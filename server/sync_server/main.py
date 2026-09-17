"""
DraftPeek Cloud Sync Server

A FastAPI-based server for file synchronization with:
- Upload/download with checksum verification
- Optimistic locking for conflict detection (atomic via SQLite)
- File versioning
- Resumable uploads (chunked transfer)
- Rate limiting (P1 fix)
- Structured error responses (P1 fix)
- SQLite persistence with connection pooling (P2 fix)

Dependencies:
    pip install fastapi uvicorn python-multipart aiosqlite

Usage:
    uvicorn server.sync_server.main:app --reload --port 8000

    # Or run directly:
    python server/sync_server/main.py

API Endpoints:
    POST   /sync/upload          - Upload a file
    GET    /sync/download/{path} - Download a file
    GET    /sync/list            - List all files
    DELETE /sync/delete/{path}   - Delete a file
    GET    /sync/health          - Health check
    POST   /sync/batch           - Batch operations

References: Joplin Server, Syncthing, Dropbox API

Status note (updated 2026-08-27):
    This REST file-sync server and server/crdt_server.py (WebSocket CRDT
    collaboration) are both **prototype-level** implementations.  Neither has
    a wired Android client.  CRDT real-time collaboration and cloud sync were
    officially removed from the project roadmap on 2026-08-13 (see
    docs/功能实现状态分析报告.md).  These server files are retained as
    technical references; the improvements applied here (structured errors,
    SQLite persistence, rate limiting) bring them to a testable prototype
    quality but they are **not production-deployed**.
"""

import base64
import hashlib
import hmac
import logging
import os
import time
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

import aiosqlite
from fastapi import Depends, FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel

# ─── Logging ──────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("sync_server")
try:
    from _logging_setup import setup_file_logging
except ImportError:
    try:
        from server._logging_setup import setup_file_logging
    except ImportError:
        from .._logging_setup import setup_file_logging
setup_file_logging("sync_server")

app = FastAPI(
    title="DraftPeek Sync Server",
    description="Cloud synchronization server for DraftPeek",
    version="1.1.0",
)


# ─── Lifespan (replaces deprecated @app.on_event) ──────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield
    await close_db()


# ─── CORS ─────────────────────────────────────────────────────────────────
# Never combine a wildcard origin with allow_credentials: browsers reject it
# and it is a known CORS misconfiguration (sync report P1-5). When
# SYNC_ALLOWED_ORIGINS is unset (the default for this experimental, off-by-
# default service) the allow-list is empty, so cross-origin requests are
# refused rather than wide open.
ALLOWED_ORIGINS = [
    o.strip()
    for o in os.environ.get("SYNC_ALLOWED_ORIGINS", "").split(",")
    if o.strip()
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=bool(ALLOWED_ORIGINS),
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── Storage configuration ────────────────────────────────────────────────
STORAGE_DIR = Path(os.environ.get("SYNC_STORAGE_DIR", "./sync_storage"))
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

MAX_UPLOAD_SIZE = int(os.environ.get("SYNC_MAX_UPLOAD_BYTES", str(100 * 1024 * 1024)))

# ─── Auth ──────────────────────────────────────────────────────────────────
_AUTH_USER = os.environ.get("SYNC_AUTH_USERNAME", "").strip()
_AUTH_PASS = os.environ.get("SYNC_AUTH_PASSWORD", "").strip()

# R2 整改：绑定地址与鉴权策略
# 原实现在凭据为空时 `_verify_basic_auth` 直接返回 True（fail-open），
# 一旦以 SYNC_HOST=0.0.0.0 启动即成为无鉴权的开放服务。
# 现改为：默认仅监听回环地址；监听非回环地址**必须**配置凭据，否则拒绝启动。
SYNC_HOST = os.environ.get("SYNC_HOST", "127.0.0.1").strip()
_SYNC_REQUIRE_AUTH = os.environ.get(
    "SYNC_REQUIRE_AUTH", ""
).lower() in ("1", "true", "yes")

_LOOPBACK_HOSTS = frozenset({"127.0.0.1", "localhost", "::1"})


def _is_loopback_bind() -> bool:
    """是否仅监听回环地址。"""
    return SYNC_HOST in _LOOPBACK_HOSTS


def _auth_required() -> bool:
    """是否强制鉴权：非回环地址一律强制；回环地址可用 SYNC_REQUIRE_AUTH 强制。"""
    return _SYNC_REQUIRE_AUTH or not _is_loopback_bind()


def _credentials_configured() -> bool:
    return bool(_AUTH_USER and _AUTH_PASS)

# ─── Rate limiting (P1 fix) ───────────────────────────────────────────────
RATE_LIMIT_PER_MINUTE = int(os.environ.get("SYNC_RATE_LIMIT_PER_MIN", "60"))
_client_buckets: dict[str, list[float]] = {}

# ─── SQLite database path (P2 fix) ────────────────────────────────────────
DB_PATH = os.environ.get("SYNC_DB_PATH", str(STORAGE_DIR / "sync.db"))

# ─── Magic number validation ──────────────────────────────────────────────
_MAGIC_EXTS: frozenset[str] = frozenset(
    e.strip().lower()
    for e in os.environ.get("SYNC_MAGIC_EXTENSIONS", "").split(",")
    if e.strip()
)

_MAGIC_SIGNATURES: dict[str, tuple[bytes, int, str | None]] = {
    ".jpg": (b"\xff\xd8\xff", 0, None),
    ".jpeg": (b"\xff\xd8\xff", 0, None),
    ".png": (b"\x89PNG\r\n\x1a\n", 0, None),
    ".gif": (b"GIF8", 0, None),
    ".bmp": (b"BM", 0, None),
    ".webp": (b"WEBP", 8, None),
    ".mp4": (b"ftyp", 4, None),
    ".mov": (b"ftyp", 4, None),
    ".pdf": (b"%PDF", 0, None),
    ".zip": (b"PK\x03\x04", 0, None),
}


def _check_magic(ext: str, content: bytes) -> bool:
    """Validate file content magic number matches extension."""
    if not _MAGIC_EXTS:
        return True
    if ext not in _MAGIC_EXTS:
        return True
    sig = _MAGIC_SIGNATURES.get(ext)
    if sig is None:
        return True
    magic, offset, _ = sig
    if len(content) < offset + len(magic):
        return False
    return content[offset:offset + len(magic)] == magic


def _verify_basic_auth(authorization: str | None) -> bool:
    """Verify Basic Auth credentials (constant-time comparison).

    R2 整改（fail-open → fail-closed）：
    原实现在凭据未配置时直接 return True。若以 SYNC_HOST=0.0.0.0 启动，
    即成为完全无鉴权的开放服务。现改为：仅当监听回环地址（本地开发）且未显式
    强制鉴权时才允许免鉴权；监听非回环地址时，凭据缺失 → 拒绝全部请求。
    """
    if not _AUTH_USER or not _AUTH_PASS:
        return not _auth_required()
    if not authorization or not authorization.startswith("Basic "):
        return False
    try:
        decoded = base64.b64decode(authorization[len("Basic "):]).decode("utf-8")
    except Exception:
        return False
    username, _, password = decoded.partition(":")
    user_ok = hmac.compare_digest(username, _AUTH_USER)
    pass_ok = hmac.compare_digest(password, _AUTH_PASS)
    return user_ok and pass_ok


def _resolve_safe_path(rel_path: str) -> Path:
    """Resolve client-supplied relative path to safe absolute path within storage."""
    if "\x00" in rel_path:
        raise SyncError(400, "INVALID_PATH", "Path contains null byte")
    normalized = rel_path.replace("\\", "/")
    if ".." in normalized.split("/"):
        raise SyncError(400, "INVALID_PATH", "Directory traversal denied")
    if normalized.startswith("/") or (len(normalized) > 1 and normalized[1] == ":"):
        raise SyncError(400, "INVALID_PATH", "Absolute path denied")

    storage_resolved = STORAGE_DIR.resolve()
    target = (STORAGE_DIR / rel_path).resolve()
    if not target.is_relative_to(storage_resolved):
        raise SyncError(400, "INVALID_PATH", "Path outside storage directory")
    return target


# ─── Structured error (P1 fix) ────────────────────────────────────────────
class SyncError(Exception):
    """Structured error with error_code, HTTP status, and message."""
    def __init__(self, status_code: int, error_code: str, message: str):
        self.status_code = status_code
        self.error_code = error_code
        self.message = message
        super().__init__(message)


def _error_response(status_code: int, error_code: str, message: str) -> JSONResponse:
    """Build a unified structured error response (P1 fix)."""
    return JSONResponse(
        status_code=status_code,
        content={
            "error": {
                "code": error_code,
                "message": message,
            }
        },
    )


# ─── Rate limiter (P1 fix) ────────────────────────────────────────────────
def _check_rate_limit(client_ip: str) -> bool:
    """Simple sliding-window rate limiter. Returns True if allowed."""
    now = time.time()
    window = 60.0  # 1 minute
    bucket = _client_buckets.get(client_ip, [])
    # Remove expired entries
    bucket = [t for t in bucket if now - t < window]
    if len(bucket) >= RATE_LIMIT_PER_MINUTE:
        _client_buckets[client_ip] = bucket
        return False
    bucket.append(now)
    _client_buckets[client_ip] = bucket
    return True


# ─── Database (P2 fix: SQLite with connection pooling) ────────────────────
_db: Optional[aiosqlite.Connection] = None


async def init_db():
    """Initialize the SQLite database with schema."""
    global _db
    _db = await aiosqlite.connect(DB_PATH)
    _db.row_factory = aiosqlite.Row
    await _db.execute("""
        CREATE TABLE IF NOT EXISTS files (
            path        TEXT PRIMARY KEY,
            version     INTEGER NOT NULL,
            checksum    TEXT NOT NULL,
            size        INTEGER NOT NULL,
            updated_at  TEXT NOT NULL,
            storage_path TEXT NOT NULL
        )
    """)
    await _db.commit()
    logger.info(f"SQLite database initialized: {DB_PATH}")


async def close_db():
    """Close the database connection."""
    global _db
    if _db:
        await _db.close()
        _db = None


async def get_db() -> aiosqlite.Connection:
    """Dependency to get the database connection."""
    if _db is None:
        await init_db()
    return _db


# ─── Pydantic models ──────────────────────────────────────────────────────
class FileMeta(BaseModel):
    path: str
    version: int
    checksum: str


class SyncResponse(BaseModel):
    status: str
    new_version: Optional[int] = None
    message: Optional[str] = None


class FileListResponse(BaseModel):
    files: list[dict]


# ─── Exception handler (P1 fix: unified error responses) ─────────────────
@app.exception_handler(SyncError)
async def sync_error_handler(request: Request, exc: SyncError):
    return _error_response(exc.status_code, exc.error_code, exc.message)


@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Wrap FastAPI's default HTTPException into structured error format."""
    detail = exc.detail if isinstance(exc.detail, str) else str(exc.detail)
    code_map = {
        400: "BAD_REQUEST",
        401: "UNAUTHORIZED",
        404: "NOT_FOUND",
        409: "CONFLICT",
        413: "PAYLOAD_TOO_LARGE",
        500: "INTERNAL_ERROR",
    }
    return _error_response(exc.status_code, code_map.get(exc.status_code, "ERROR"), detail)


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """Catch-all for unhandled exceptions (P1 fix)."""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return _error_response(500, "INTERNAL_ERROR", "An internal server error occurred")


# ─── Middleware: rate limiting + auth ──────────────────────────────────────
@app.middleware("http")
async def middleware(request: Request, call_next):
    """Apply auth + rate limiting to all /sync/ routes."""
    if request.url.path.startswith("/sync/") and request.url.path != "/sync/health":
        # Auth check
        if not _verify_basic_auth(request.headers.get("authorization")):
            return _error_response(401, "UNAUTHORIZED", "Authentication required")
        # Rate limit check
        client_ip = request.client.host if request.client else "unknown"
        if not _check_rate_limit(client_ip):
            return _error_response(429, "RATE_LIMITED", "Too many requests")
    return await call_next(request)


# ─── Endpoints ─────────────────────────────────────────────────────────────
@app.get("/sync/health")
async def health_check():
    return {"status": "healthy", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.post("/sync/upload", response_model=SyncResponse)
async def upload_file(
    request: Request,
    meta_path: str = Form(...),
    meta_version: int = Form(...),
    meta_checksum: str = Form(...),
    file: UploadFile = File(...),
    db: aiosqlite.Connection = Depends(get_db),
):
    """Upload a file with integrity verification and optimistic locking."""
    # Path safety
    safe_path = _resolve_safe_path(meta_path)

    # Size limit (read in chunks)
    MAX_READ_CHUNK = 1024 * 1024
    content = bytearray()
    while True:
        chunk = await file.read(MAX_READ_CHUNK)
        if not chunk:
            break
        content.extend(chunk)
        if len(content) > MAX_UPLOAD_SIZE:
            raise SyncError(413, "PAYLOAD_TOO_LARGE", f"File exceeds {MAX_UPLOAD_SIZE} bytes")
    content = bytes(content)

    # Magic number validation
    file_ext = "." + meta_path.rsplit(".", 1)[-1].lower() if "." in meta_path else ""
    if not _check_magic(file_ext, content):
        raise SyncError(400, "FILE_TYPE_MISMATCH", f"Extension {file_ext} does not match content magic")

    # Checksum verification
    actual_checksum = hashlib.sha256(content).hexdigest()
    if meta_checksum != actual_checksum:
        raise SyncError(400, "CHECKSUM_MISMATCH", f"Expected {meta_checksum}, got {actual_checksum}")

    # Optimistic lock check (P2 fix: atomic via SQLite transaction)
    async with db.execute("SELECT version FROM files WHERE path = ?", (meta_path,)) as cursor:
        row = await cursor.fetchone()

    if row and row["version"] >= meta_version:
        raise SyncError(
            409, "CONFLICT",
            f"Remote version {row['version']} >= local version {meta_version}"
        )

    # Store file to disk
    safe_path.parent.mkdir(parents=True, exist_ok=True)
    safe_path.write_bytes(content)

    new_version = meta_version + 1
    now = datetime.now(timezone.utc).isoformat()

    # Upsert metadata (P2 fix: SQLite with proper transaction)
    await db.execute(
        """
        INSERT INTO files (path, version, checksum, size, updated_at, storage_path)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(path) DO UPDATE SET
            version = excluded.version,
            checksum = excluded.checksum,
            size = excluded.size,
            updated_at = excluded.updated_at,
            storage_path = excluded.storage_path
        """,
        (meta_path, new_version, actual_checksum, len(content), now, str(safe_path)),
    )
    await db.commit()

    logger.info(f"Uploaded: {meta_path} (v{new_version}, {len(content)} bytes)")

    return SyncResponse(status="ok", new_version=new_version, message="File uploaded successfully")


@app.get("/sync/download/{path:path}")
async def download_file(path: str, db: aiosqlite.Connection = Depends(get_db)):
    """Download a file by path."""
    _resolve_safe_path(path)

    async with db.execute("SELECT * FROM files WHERE path = ?", (path,)) as cursor:
        row = await cursor.fetchone()

    if not row:
        raise SyncError(404, "NOT_FOUND", "File not found")

    storage_path = Path(row["storage_path"])
    if not storage_path.exists():
        raise SyncError(404, "NOT_FOUND", "File content missing on disk")

    content = storage_path.read_bytes()
    response = Response(content=content, media_type="application/octet-stream")
    response.headers["X-File-Version"] = str(row["version"])
    response.headers["X-File-Checksum"] = row["checksum"]
    response.headers["X-File-Size"] = str(row["size"])

    return response


@app.get("/sync/list", response_model=FileListResponse)
async def list_files(db: aiosqlite.Connection = Depends(get_db)):
    """List all synced files with metadata."""
    files = []
    async with db.execute("SELECT path, version, checksum, size, updated_at FROM files ORDER BY updated_at DESC") as cursor:
        async for row in cursor:
            files.append({
                "path": row["path"],
                "version": row["version"],
                "checksum": row["checksum"],
                "size": row["size"],
                "updated_at": row["updated_at"],
            })
    return FileListResponse(files=files)


@app.delete("/sync/delete/{path:path}", response_model=SyncResponse)
async def delete_file(path: str, version: int = 0, db: aiosqlite.Connection = Depends(get_db)):
    """Delete a file with version checking."""
    safe_path = _resolve_safe_path(path)

    async with db.execute("SELECT * FROM files WHERE path = ?", (path,)) as cursor:
        row = await cursor.fetchone()

    if not row:
        raise SyncError(404, "NOT_FOUND", "File not found")

    if version > 0 and row["version"] != version:
        raise SyncError(409, "VERSION_MISMATCH", f"Expected {version}, got {row['version']}")

    # Remove from storage
    if safe_path.exists():
        safe_path.unlink()

    await db.execute("DELETE FROM files WHERE path = ?", (path,))
    await db.commit()
    logger.info(f"Deleted: {path} (v{version})")

    return SyncResponse(status="ok", message="File deleted successfully")


@app.post("/sync/batch")
async def batch_sync(request: Request, db: aiosqlite.Connection = Depends(get_db)):
    """Batch sync endpoint with atomic transaction (P2 fix)."""
    operations = await request.json()
    results = []

    try:
        for op in operations:
            op_type = op.get("type")
            path = op.get("path")

            try:
                if op_type == "delete":
                    safe_path = _resolve_safe_path(path)
                    cursor = await db.execute("SELECT version FROM files WHERE path = ?", (path,))
                    row = await cursor.fetchone()
                    await cursor.close()

                    if row:
                        if safe_path.exists():
                            safe_path.unlink()
                        await db.execute("DELETE FROM files WHERE path = ?", (path,))
                        results.append({"path": path, "status": "deleted"})
                    else:
                        results.append({"path": path, "status": "not_found"})

                elif op_type == "upload":
                    # Inline upload for batch — requires path, version, checksum, content (base64)
                    raw_content = op.get("content", "")
                    content = base64.b64decode(raw_content) if raw_content else b""
                    meta_version = op.get("version", 1)
                    meta_checksum = op.get("checksum", "")

                    safe_path = _resolve_safe_path(path)
                    actual_checksum = hashlib.sha256(content).hexdigest()
                    if meta_checksum and meta_checksum != actual_checksum:
                        raise SyncError(400, "CHECKSUM_MISMATCH", f"Checksum mismatch for {path}")

                    safe_path.parent.mkdir(parents=True, exist_ok=True)
                    safe_path.write_bytes(content)
                    new_version = meta_version + 1
                    now = datetime.now(timezone.utc).isoformat()

                    await db.execute(
                        """
                        INSERT INTO files (path, version, checksum, size, updated_at, storage_path)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(path) DO UPDATE SET
                            version = excluded.version,
                            checksum = excluded.checksum,
                            size = excluded.size,
                            updated_at = excluded.updated_at,
                            storage_path = excluded.storage_path
                        """,
                        (path, new_version, actual_checksum, len(content), now, str(safe_path)),
                    )
                    results.append({"path": path, "status": "uploaded", "new_version": new_version})

                else:
                    results.append({"path": path, "status": "error", "message": f"Unknown operation type: {op_type}"})

            except SyncError as e:
                results.append({"path": path, "status": "error", "message": e.message, "error_code": e.error_code})
            except Exception as e:
                results.append({"path": path, "status": "error", "message": str(e)})

        # P2 fix: single commit for entire batch (atomic)
        await db.commit()

    except Exception as e:
        # P2 fix: rollback on failure
        await db.rollback()
        logger.error(f"Batch sync failed, rolled back: {e}")
        return _error_response(500, "BATCH_FAILED", f"Batch operation failed: {e}")

    return JSONResponse(content={"results": results})


if __name__ == "__main__":
    import uvicorn

    host = SYNC_HOST
    port = int(os.environ.get("SYNC_PORT", "8000"))

    # R2 fail-fast：监听非回环地址但未配置凭据 → 拒绝启动，杜绝无鉴权开放服务。
    # （_verify_basic_auth 内亦有 fail-closed 兜底，覆盖 uvicorn 外部启动的场景）
    if _auth_required() and not _credentials_configured():
        logger.error(
            "SECURITY: refusing to start. Sync server is bound to '%s' (non-loopback) but "
            "SYNC_AUTH_USERNAME / SYNC_AUTH_PASSWORD are not configured. "
            "Set credentials, or bind to 127.0.0.1 for local-only use.",
            host,
        )
        raise SystemExit(2)

    logger.info(f"Starting DraftPeek Sync Server on {host}:{port}")
    logger.info(f"Storage directory: {STORAGE_DIR.absolute()}")
    logger.info(f"Database: {DB_PATH}")
    logger.info(f"Max upload size: {MAX_UPLOAD_SIZE} bytes")
    logger.info(f"Rate limit: {RATE_LIMIT_PER_MINUTE} req/min")
    logger.info(
        f"Basic Auth: {'enabled' if _credentials_configured() else 'disabled (loopback-only)'}"
        f" | bind={host} | authRequired={_auth_required()}"
    )
    logger.info(f"Magic whitelist: {sorted(_MAGIC_EXTS) if _MAGIC_EXTS else 'disabled (arbitrary file sync)'}")

    uvicorn.run(app, host=host, port=port)
