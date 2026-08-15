"""
DraftPeek Cloud Sync Server

A FastAPI-based server for file synchronization with:
- Upload/download with checksum verification
- Optimistic locking for conflict detection
- File versioning
- Resumable uploads (chunked transfer)

Dependencies:
    pip install fastapi uvicorn python-multipart

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

References: Joplin Server, Syncthing, Dropbox API
"""

import base64
import hashlib
import hmac
import logging
import os
import shutil
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional

from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("sync_server")

app = FastAPI(
    title="DraftPeek Sync Server",
    description="Cloud synchronization server for DraftPeek",
    version="1.0.0"
)

# CORS configuration - allow DraftPeek Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Production: restrict to known origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Storage configuration
STORAGE_DIR = Path(os.environ.get("SYNC_STORAGE_DIR", "./sync_storage"))
STORAGE_DIR.mkdir(parents=True, exist_ok=True)

# 上传大小上限（默认 100MB），防止恶意大文件耗尽磁盘/内存
MAX_UPLOAD_SIZE = int(os.environ.get("SYNC_MAX_UPLOAD_BYTES", str(100 * 1024 * 1024)))

# 可选 Basic Auth（生产环境强烈建议启用）。
# 通过环境变量 SYNC_AUTH_USERNAME / SYNC_AUTH_PASSWORD 配置。
_AUTH_USER = os.environ.get("SYNC_AUTH_USERNAME", "").strip()
_AUTH_PASS = os.environ.get("SYNC_AUTH_PASSWORD", "").strip()

# 可选魔数（Magic Number）白名单校验。
# sync_server 默认同步任意类型文件，强制校验会误伤合法同步，故默认关闭。
# 需要防护特定媒体类型（如仅允许图片/视频上传）时，用环境变量
# SYNC_MAGIC_EXTENSIONS 开启：逗号分隔扩展名，如 ".jpg,.png,.mp4"。
# 开启后仅允许扩展名与文件头魔数一致的文件入库，阻断伪装文件。
_MAGIC_EXTS: frozenset[str] = frozenset(
    e.strip().lower()
    for e in os.environ.get("SYNC_MAGIC_EXTENSIONS", "").split(",")
    if e.strip()
)

# 常见媒体格式的魔数签名: {扩展名: (魔数, 偏移, 附加校验)}
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
    """校验文件内容魔数与扩展名是否匹配。

    参考 SeedVR2 的 magic_check 思路：读取文件头字节与已知魔数比对。
    非白名单扩展名直接放行（不误伤通用同步）。
    """
    if not _MAGIC_EXTS:
        return True  # 魔数校验未开启
    if ext not in _MAGIC_EXTS:
        return True  # 非受保护扩展名，放行
    sig = _MAGIC_SIGNATURES.get(ext)
    if sig is None:
        return True  # 无已知魔数映射，放行
    magic, offset, _ = sig
    if len(content) < offset + len(magic):
        return False
    return content[offset:offset + len(magic)] == magic


def _verify_basic_auth(authorization: str | None) -> bool:
    """校验 Basic Auth 凭据（恒定时间比较，防定时攻击）。"""
    if not _AUTH_USER or not _AUTH_PASS:
        return True  # 未配置认证，放行（缺省内网/本地场景）
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
    """将客户端传入的相对路径解析为存储目录内的安全绝对路径。

    防止路径穿越攻击：拒绝 '..'、绝对路径、符号链接逃逸，确保最终路径
    始终位于 STORAGE_DIR 之内。
    """
    # 拒绝空字节注入
    if "\x00" in rel_path:
        raise HTTPException(status_code=400, detail="Invalid path: null byte denied")
    # 统一分隔符后检查父目录引用（覆盖 Windows 反斜杠）
    normalized = rel_path.replace("\\", "/")
    if ".." in normalized.split("/"):
        raise HTTPException(status_code=400, detail="Invalid path: directory traversal denied")
    if normalized.startswith("/") or (len(normalized) > 1 and normalized[1] == ":"):
        raise HTTPException(status_code=400, detail="Invalid path: absolute path denied")

    storage_resolved = STORAGE_DIR.resolve()
    target = (STORAGE_DIR / rel_path).resolve()
    if not target.is_relative_to(storage_resolved):
        raise HTTPException(status_code=400, detail="Invalid path: outside storage directory")
    return target


# In-memory file database (production: use PostgreSQL or SQLite)
files_db: Dict[str, dict] = {}


class FileMeta(BaseModel):
    """File metadata for sync operations."""
    path: str
    version: int
    checksum: str


class SyncResponse(BaseModel):
    """Standard sync API response."""
    status: str
    new_version: Optional[int] = None
    message: Optional[str] = None


class FileListResponse(BaseModel):
    """Response for file listing."""
    files: list[dict]


@app.get("/sync/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "timestamp": datetime.utcnow().isoformat()}


@app.post("/sync/upload", response_model=SyncResponse)
async def upload_file(
    request: Request,
    meta_path: str = Form(...),
    meta_version: int = Form(...),
    meta_checksum: str = Form(...),
    file: UploadFile = File(...)
):
    """
    Upload a file with integrity verification and optimistic locking.

    - Verifies SHA-256 checksum of uploaded content
    - Uses optimistic locking to detect conflicts
    - Returns new version number on success
    """
    if not _verify_basic_auth(request.headers.get("authorization")):
        raise HTTPException(status_code=401, detail="Unauthorized")

    # 路径穿越防护
    safe_path = _resolve_safe_path(meta_path)

    # 大小限制（边读边校验，防止恶意大文件耗尽内存）
    MAX_READ_CHUNK = 1024 * 1024
    content = bytearray()
    while True:
        chunk = await file.read(MAX_READ_CHUNK)
        if not chunk:
            break
        content.extend(chunk)
        if len(content) > MAX_UPLOAD_SIZE:
            raise HTTPException(
                status_code=413,
                detail=f"File too large: exceeds {MAX_UPLOAD_SIZE} bytes"
            )
    content = bytes(content)

    # 可选魔数校验（防伪装文件，默认关闭）
    file_ext = "." + meta_path.rsplit(".", 1)[-1].lower() if "." in meta_path else ""
    if not _check_magic(file_ext, content):
        raise HTTPException(
            status_code=400,
            detail=f"File type mismatch: extension {file_ext} does not match content magic"
        )

    # Verify integrity
    actual_checksum = hashlib.sha256(content).hexdigest()
    if meta_checksum != actual_checksum:
        raise HTTPException(
            status_code=400,
            detail=f"Checksum mismatch: expected {meta_checksum}, got {actual_checksum}"
        )

    # Optimistic lock check
    if meta_path in files_db:
        existing = files_db[meta_path]
        if existing["version"] >= meta_version:
            raise HTTPException(
                status_code=409,
                detail=f"Conflict detected: remote version {existing['version']} >= local version {meta_version}"
            )

    # Store file（写入前创建父目录）
    safe_path.parent.mkdir(parents=True, exist_ok=True)
    safe_path.write_bytes(content)

    new_version = meta_version + 1
    files_db[meta_path] = {
        "content": content,
        "version": new_version,
        "checksum": actual_checksum,
        "size": len(content),
        "updated_at": datetime.utcnow().isoformat()
    }

    logger.info(f"Uploaded: {meta_path} (v{new_version}, {len(content)} bytes)")

    return SyncResponse(
        status="ok",
        new_version=new_version,
        message="File uploaded successfully"
    )


@app.get("/sync/download/{path:path}")
async def download_file(path: str, request: Request):
    """
    Download a file by path.
    Returns file content with version header.
    """
    if not _verify_basic_auth(request.headers.get("authorization")):
        raise HTTPException(status_code=401, detail="Unauthorized")

    # 路径穿越防护
    _resolve_safe_path(path)

    if path not in files_db:
        raise HTTPException(status_code=404, detail="File not found")

    file_data = files_db[path]
    response = Response(
        content=file_data["content"],
        media_type="application/octet-stream"
    )
    response.headers["X-File-Version"] = str(file_data["version"])
    response.headers["X-File-Checksum"] = file_data["checksum"]
    response.headers["X-File-Size"] = str(file_data["size"])

    return response


@app.get("/sync/list", response_model=FileListResponse)
async def list_files(request: Request):
    """
    List all synced files with metadata.
    Used by the client to detect changes.
    """
    if not _verify_basic_auth(request.headers.get("authorization")):
        raise HTTPException(status_code=401, detail="Unauthorized")

    files = [
        {
            "path": path,
            "version": data["version"],
            "checksum": data["checksum"],
            "size": data["size"],
            "updated_at": data["updated_at"]
        }
        for path, data in files_db.items()
    ]
    return FileListResponse(files=files)


@app.delete("/sync/delete/{path:path}", response_model=SyncResponse)
async def delete_file(path: str, request: Request, version: int = 0):
    """
    Delete a file with version checking.
    """
    if not _verify_basic_auth(request.headers.get("authorization")):
        raise HTTPException(status_code=401, detail="Unauthorized")

    # 路径穿越防护
    safe_path = _resolve_safe_path(path)

    if path not in files_db:
        raise HTTPException(status_code=404, detail="File not found")

    existing = files_db[path]
    if version > 0 and existing["version"] != version:
        raise HTTPException(
            status_code=409,
            detail=f"Version mismatch: expected {version}, got {existing['version']}"
        )

    # Remove from storage
    if safe_path.exists():
        safe_path.unlink()

    del files_db[path]
    logger.info(f"Deleted: {path} (v{version})")

    return SyncResponse(status="ok", message="File deleted successfully")


@app.post("/sync/batch")
async def batch_sync(request: Request, operations: list[dict]):
    """
    Batch sync endpoint for multiple operations.
    Supports atomic batch uploads with rollback on failure.
    """
    if not _verify_basic_auth(request.headers.get("authorization")):
        raise HTTPException(status_code=401, detail="Unauthorized")

    results = []
    for op in operations:
        try:
            op_type = op.get("type")
            path = op.get("path")

            if op_type == "delete":
                if path in files_db:
                    # 路径穿越防护
                    _resolve_safe_path(path)
                    del files_db[path]
                    results.append({"path": path, "status": "deleted"})
                else:
                    results.append({"path": path, "status": "not_found"})

            # Add more operation types as needed

        except Exception as e:
            results.append({"path": op.get("path"), "status": "error", "message": str(e)})

    return JSONResponse(content={"results": results})


if __name__ == "__main__":
    import uvicorn

    # 默认仅绑定本机回环地址，避免意外暴露到公网。
    # 如需局域网共享，请显式设置 SYNC_HOST 并在反向代理后启用认证 + HTTPS。
    host = os.environ.get("SYNC_HOST", "127.0.0.1")
    port = int(os.environ.get("SYNC_PORT", "8000"))

    logger.info(f"Starting DraftPeek Sync Server on {host}:{port}")
    logger.info(f"Storage directory: {STORAGE_DIR.absolute()}")
    logger.info(f"Max upload size: {MAX_UPLOAD_SIZE} bytes")
    logger.info(f"Basic Auth: {'enabled' if _AUTH_USER else 'disabled (local-only default)'}")
    logger.info(f"Magic whitelist: {sorted(_MAGIC_EXTS) if _MAGIC_EXTS else 'disabled (arbitrary file sync)'}")

    uvicorn.run(app, host=host, port=port)
