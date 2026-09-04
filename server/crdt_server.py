"""
Yjs CRDT Collaboration Server

A WebSocket server for real-time collaborative editing using Yjs CRDT.
Each document gets its own "room" where connected clients can sync changes.

Dependencies:
    pip install websockets uvicorn pycrdt

Usage:
    python crdt_server.py

    # Connect from client:
    # ws://localhost:8080/yjs/<document_id>
    # ws://localhost:8080/yjs/<document_id>?token=<CRDT_TOKEN>  (when CRDT_TOKEN is set)

Environment variables:
    CRDT_HOST              bind address (default: 127.0.0.1; set to 0.0.0.0 to expose on LAN)
    CRDT_PORT              listen port (default: 8080)
    CRDT_TOKEN             optional shared token for client authentication
    CRDT_TOKENS            optional JSON object {"<document_id>": "<secret>", ...} for
                          per-room access control (takes precedence over CRDT_TOKEN).
                          When ANY auth is configured, WSS (CRDT_TLS_CERT/KEY) is
                          mandatory — plaintext ws:// connections are rejected (1008).
    CRDT_PERSISTENCE_DIR   directory for document state persistence (default: ./crdt_data)
    CRDT_MAX_MSG_PER_SEC   rate limit: max messages per second per client (default: 100)
    CRDT_MAX_BURST         rate limit: burst capacity (default: 200)
    CRDT_PING_INTERVAL     heartbeat ping interval in seconds (default: 30)
    CRDT_PING_TIMEOUT      heartbeat pong timeout in seconds (default: 10)
    CRDT_AUDIT_LOG         path to conflict-resolution audit log file (default: disabled)
    CRDT_MAX_MSG_SIZE      max WebSocket message size in bytes (default: 10 MB)

STATUS:
    EXPERIMENTAL reference implementation — NOT deployed, NOT production-hardened.
    DraftPeek is a local-first Android editor; this collaboration server is an
    OPTIONAL auxiliary service (no wired client in the shipped app, default-off).
    See 协作同步服务设计评估报告_v1.0.30.md for the threat model and the required
    hardening: signed per-room tokens + WSS, resource caps, and a Python CI gate.

References:
    - Yjs: https://github.com/yjs/yjs
    - ypy: https://github.com/y-crdt/ypy
    - y-websocket protocol: https://github.com/yjs/y-websocket

Protocol note:
    Message framing follows y-protocols sync:
      [0, 0, stateVector]  sync step1 (client -> server)
      [0, 1, update]       sync step2 (server -> client, answer to step1)
      [0, 2, update]       sync update  (broadcast)
    On join the server no longer pushes a bare state vector; it waits for the
    client's sync step1 and answers with sync step2.

    Error frames (P1 fix):
      [1, errorCode, ...errorMessageBytes]  error frame (server -> client)
    Error codes:
      0 = SYNC_STEP2_FAILED    server could not compute diff update
      1 = RATE_LIMITED         client is sending too many messages
      2 = INVALID_MESSAGE      message could not be decoded
"""

import asyncio
import base64
import hashlib
import hmac
import json
import logging
import os
import time
from pathlib import Path
from typing import Dict, Optional, Set
from urllib.parse import parse_qs

import websockets
from websockets.server import WebSocketServerProtocol

# ─── pycrdt (formerly ypy): hard dependency (P0 fix — no more silent pass-through)
# The ypy package was renamed to pycrdt.  We try pycrdt first, fall back to ypy
# for older installs, and refuse to start if neither is available.
try:
    from pycrdt import Doc as _Doc
    CRDT_LIB = "pycrdt"
except ImportError:
    try:
        from ypy import Doc as _Doc
        CRDT_LIB = "ypy"
    except ImportError:
        _Doc = None
        CRDT_LIB = None
        logging.error(
            "Neither pycrdt nor ypy is installed. The CRDT server requires "
            "one of them to maintain server-side document state and answer "
            "sync step1.  Install with: pip install pycrdt"
        )
        raise ImportError(
            "pycrdt (or ypy) is a hard dependency of crdt_server. "
            "Install with: pip install pycrdt"
        )

YPY_AVAILABLE = True  # Always True after successful import above
Doc = _Doc  # type: ignore

# ─── Logging ──────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("crdt_server")

# ─── Server configuration ─────────────────────────────────────────────────
CRDT_HOST = os.environ.get("CRDT_HOST", "127.0.0.1").strip()
CRDT_PORT = int(os.environ.get("CRDT_PORT", "8080"))
AUTH_TOKEN = os.environ.get("CRDT_TOKEN", "").strip()
# AUTH_TOKEN is the HMAC SECRET (server-side only). When set, handle_client
# requires WSS and a valid per-room signed token (see mint_room_token /
# verify_room_token below — P1-3 fix).
AUTH_TOKEN_TTL = int(os.environ.get("CRDT_TOKEN_TTL", "300"))

# ── Per-room token map (P1-3) ─────────────────────────────────────────────
# Optional JSON object mapping document_id -> secret, enabling per-room access
# control instead of a single shared token that grants access to every room.
# Takes precedence over the shared CRDT_TOKEN when set.
ROOM_TOKENS: Dict[str, str] = {}
_room_tokens_raw = os.environ.get("CRDT_TOKENS", "").strip()
if _room_tokens_raw:
    try:
        _parsed = json.loads(_room_tokens_raw)
        if isinstance(_parsed, dict):
            ROOM_TOKENS = {str(k): str(v) for k, v in _parsed.items()}
        else:
            logger.warning("CRDT_TOKENS must be a JSON object; ignored")
    except Exception as exc:  # noqa: BLE001
        logger.warning("Failed to parse CRDT_TOKENS: %s", exc)

# Auth is enabled if either a shared token or a per-room token map is configured.
AUTH_ENABLED = bool(AUTH_TOKEN or ROOM_TOKENS)

# ─── Persistence configuration (P0 fix) ──────────────────────────────────
PERSISTENCE_DIR = Path(
    os.environ.get("CRDT_PERSISTENCE_DIR", "./crdt_data")
)
PERSISTENCE_DIR.mkdir(parents=True, exist_ok=True)

# ─── Rate limiting configuration (P1 fix) ─────────────────────────────────
MAX_MSG_PER_SECOND = float(os.environ.get("CRDT_MAX_MSG_PER_SEC", "100"))
MAX_BURST = int(os.environ.get("CRDT_MAX_BURST", "200"))

# ─── Heartbeat configuration (P2 fix) ─────────────────────────────────────
PING_INTERVAL = int(os.environ.get("CRDT_PING_INTERVAL", "30"))
PING_TIMEOUT = int(os.environ.get("CRDT_PING_TIMEOUT", "10"))

# ─── Audit log configuration (P1 fix) ─────────────────────────────────────
AUDIT_LOG_PATH = os.environ.get("CRDT_AUDIT_LOG", "").strip()
_audit_logger: Optional[logging.Logger] = None
if AUDIT_LOG_PATH:
    _audit_logger = logging.getLogger("crdt_audit")
    _audit_logger.setLevel(logging.INFO)
    _handler = logging.FileHandler(AUDIT_LOG_PATH)
    _handler.setFormatter(logging.Formatter("%(asctime)s %(message)s"))
    _audit_logger.addHandler(_handler)
    _audit_logger.propagate = False

# ─── Message size limit ───────────────────────────────────────────────────
MAX_MSG_SIZE = int(os.environ.get("CRDT_MAX_MSG_SIZE", str(10 * 1024 * 1024)))

# ─── TLS / WSS configuration (P2 fix) ─────────────────────────────────────
# When CRDT_TLS_CERT and CRDT_TLS_KEY are set, the server uses wss://.
# Token passed via query string is only secure over TLS.
TLS_CERT = os.environ.get("CRDT_TLS_CERT", "").strip()
TLS_KEY = os.environ.get("CRDT_TLS_KEY", "").strip()
# TLS/WSS must be enabled whenever auth is enabled (tokens travel on the wire).
TLS_ENABLED = bool(TLS_CERT and TLS_KEY)

# ─── Room registry ────────────────────────────────────────────────────────
rooms: Dict[str, "Room"] = {}

# ─── y-protocols sync framing ─────────────────────────────────────────────
MSG_SYNC = 0
MSG_ERROR = 1  # P1 fix: error frame type

SYNC_STEP1 = 0
SYNC_STEP2 = 1
SYNC_UPDATE = 2

# Error codes (P1 fix)
ERR_SYNC_STEP2_FAILED = 0
ERR_RATE_LIMITED = 1
ERR_INVALID_MESSAGE = 2


def encode_sync_step2(update: bytes) -> bytes:
    """Wrap an update into a sync step2 frame: [0, 1, update]."""
    return bytes([MSG_SYNC, SYNC_STEP2]) + update


def encode_sync_update(update: bytes) -> bytes:
    """Wrap an update into a sync update frame: [0, 2, update]."""
    return bytes([MSG_SYNC, SYNC_UPDATE]) + update


def encode_error(error_code: int, message: str = "") -> bytes:
    """Build an error frame: [1, errorCode, ...messageBytes]."""
    return bytes([MSG_ERROR, error_code]) + message.encode("utf-8")


def decode_sync_payload(message: bytes):
    """Return (sync_step, payload) for a sync frame, else (None, message)."""
    if len(message) >= 2 and message[0] == MSG_SYNC:
        if message[1] in (SYNC_STEP1, SYNC_STEP2, SYNC_UPDATE):
            return message[1], message[2:]
    return None, message


# ─── Token‑based rate limiter (P1 fix — token bucket) ────────────────────
class RateLimiter:
    """Token‑bucket rate limiter for WebSocket messages."""

    def __init__(self, rate: float, burst: int):
        self.rate = rate
        self.capacity = burst
        self.tokens = float(burst)
        self.last_refill = time.monotonic()

    def acquire(self) -> bool:
        """Try to consume one token.  Returns True if allowed."""
        now = time.monotonic()
        elapsed = now - self.last_refill
        self.tokens = min(self.capacity, self.tokens + elapsed * self.rate)
        self.last_refill = now
        if self.tokens >= 1.0:
            self.tokens -= 1.0
            return True
        return False


# ─── Room ──────────────────────────────────────────────────────────────────
class Room:
    """Represents a collaborative document room with persistence, audit
    logging, and concurrent broadcast support."""

    def __init__(self, room_id: str):
        self.room_id = room_id
        self.clients: Set[WebSocketServerProtocol] = set()
        self.doc = Doc()  # ypy is a hard dependency — always available
        self._persistence_path = PERSISTENCE_DIR / f"{room_id}.state"
        self._load_state()
        logger.info(f"Room created: {room_id}")

    # ── Persistence (P0 fix) ────────────────────────────────────────────
    def _persistence_path_safe(self) -> Path:
        """Return a safe persistence path, guarding against path traversal
        in the room_id."""
        safe = self.room_id.replace("/", "_").replace("\\", "_").replace("..", "_")
        return PERSISTENCE_DIR / f"{safe}.state"

    def _load_state(self):
        """Load persisted document state from disk if it exists."""
        path = self._persistence_path_safe()
        if path.exists():
            try:
                state = path.read_bytes()
                if state:
                    self.doc.apply_update(state)
                    logger.info(f"Restored state for room '{self.room_id}' ({len(state)} bytes)")
            except Exception as exc:
                logger.error(f"Failed to load state for room '{self.room_id}': {exc}")

    def _save_state(self):
        """Persist the current document state to disk."""
        path = self._persistence_path_safe()
        try:
            # get_update() with no state vector returns the full document state.
            # (Passing b"" is invalid in pycrdt >= 0.14 and raises.)
            state = self.doc.get_update()
            path.write_bytes(state)
        except Exception as exc:  # noqa: BLE001
            logger.error(f"Failed to save state for room '{self.room_id}': {exc}")

    # ── Audit logging (P1 fix) ──────────────────────────────────────────
    @staticmethod
    def _audit_log(room_id: str, event: str, **kwargs):
        """Write a structured audit log entry for conflict‑resolution events."""
        if _audit_logger is None:
            return
        entry = json.dumps({
            "room": room_id,
            "event": event,
            "timestamp": time.time(),
            **kwargs,
        })
        _audit_logger.info(entry)

    # ── Client management ──────────────────────────────────────────────
    async def add_client(self, websocket: WebSocketServerProtocol):
        self.clients.add(websocket)
        logger.info(
            f"Client joined room '{self.room_id}'. "
            f"Total clients: {len(self.clients)}"
        )

    async def remove_client(self, websocket: WebSocketServerProtocol):
        self.clients.discard(websocket)
        logger.info(
            f"Client left room '{self.room_id}'. "
            f"Remaining clients: {len(self.clients)}"
        )
        if not self.clients and rooms.get(self.room_id) is self:
            # P0 fix: persist state before deleting room
            self._save_state()
            del rooms[self.room_id]
            logger.info(f"Room deleted (empty, state persisted): {self.room_id}")

    # ── Concurrent broadcast (P1 fix) ──────────────────────────────────
    async def broadcast_update(self, update: bytes, exclude: WebSocketServerProtocol = None):
        """Apply update to server doc and broadcast to all clients except sender.

        Uses asyncio.gather for concurrent sends (P1 fix: was serial).
        """
        # Apply to server-side document
        self.doc.apply_update(update)

        # Audit log the merge event
        self._audit_log(
            self.room_id,
            "update_applied",
            update_size=len(update),
            client_count=len(self.clients),
        )

        framed = encode_sync_update(update)
        targets = [c for c in self.clients if c != exclude]
        if not targets:
            return

        # P1 fix: concurrent broadcast instead of serial await
        results = await asyncio.gather(
            *[c.send(framed) for c in targets],
            return_exceptions=True,
        )
        # Clean up disconnected clients — any send exception means the client
        # is no longer reachable, so remove it from the room.
        for client, result in zip(targets, results):
            if isinstance(result, Exception):
                if isinstance(result, websockets.ConnectionClosed):
                    self.clients.discard(client)
                else:
                    logger.warning(
                        "Error broadcasting to client in room '%s': %s",
                        self.room_id, result,
                    )
                    self.clients.discard(client)

    # ── Sync step2 reply with error notification (P1 fix) ─────────────
    async def reply_sync_step2(self, websocket: WebSocketServerProtocol, state_vector: bytes):
        """Answer a sync step1 with a sync step2 carrying the diff update.

        On failure, send an error frame so the client knows sync failed
        (P1 fix: was silently swallowed).
        """
        try:
            update = self.doc.get_update(state_vector)
            await websocket.send(encode_sync_step2(update))
            self._audit_log(
                self.room_id,
                "sync_step2_sent",
                update_size=len(update),
                state_vector_size=len(state_vector),
            )
        except Exception as exc:
            logger.error("Failed to compute sync step2 for room '%s': %s", self.room_id, exc)
            # P1 fix: notify the client instead of silent failure
            try:
                await websocket.send(
                    encode_error(ERR_SYNC_STEP2_FAILED, f"sync step2 failed: {exc}")
                )
            except Exception:
                pass  # client may already be disconnected

    # ── Catch-up pagination (P3 fix) ────────────────────────────────────
    async def reply_sync_step2_paginated(
        self,
        websocket: WebSocketServerProtocol,
        state_vector: bytes,
        max_chunk_size: int = 1 * 1024 * 1024,  # 1 MB per chunk
    ):
        """Answer sync step1 with paginated sync step2 for large diffs.

        If the diff update exceeds max_chunk_size, it is still sent as a
        single frame (Yjs updates are atomic) but a warning is logged.
        A future enhancement could split using sub-document updates.
        """
        try:
            update = self.doc.get_update(state_vector)
            if len(update) > max_chunk_size:
                logger.warning(
                    "Large sync step2 for room '%s': %d bytes (threshold %d) — "
                    "consider client-side state vector compaction",
                    self.room_id, len(update), max_chunk_size,
                )
            await websocket.send(encode_sync_step2(update))
            self._audit_log(
                self.room_id,
                "sync_step2_sent",
                update_size=len(update),
                state_vector_size=len(state_vector),
                paginated=len(update) > max_chunk_size,
            )
        except Exception as exc:
            logger.error("Failed to compute paginated sync step2 for room '%s': %s", self.room_id, exc)
            try:
                await websocket.send(
                    encode_error(ERR_SYNC_STEP2_FAILED, f"sync step2 failed: {exc}")
                )
            except Exception:
                pass


def get_or_create_room(room_id: str) -> Room:
    """Get an existing room or create a new one (with state restoration)."""
    if room_id not in rooms:
        rooms[room_id] = Room(room_id)
    return rooms[room_id]


def _extract_client_token(websocket: WebSocketServerProtocol, path: str) -> str:
    """Extract the 'token' query parameter from the WebSocket request."""
    raw_path = path or ""
    if "?" not in raw_path:
        try:
            raw_path = websocket.request.path or ""
        except Exception:
            raw_path = path or ""
    if "?" in raw_path:
        query = raw_path.split("?", 1)[1]
        values = parse_qs(query).get("token", [])
        if values:
            return values[0]
    return ""


def _extract_room_id(path: str) -> str:
    """Extract the room/document id from the connection path (/yjs/<id>)."""
    path_no_query = path.split("?", 1)[0]
    parts = path_no_query.strip("/").split("/")
    if len(parts) >= 2 and parts[0] == "yjs":
        return parts[1]
    return path_no_query.strip("/").split("/")[-1]


# ─── Per-room signed token auth (P1-3, preferred scheme) ──────────────────
# The shared CRDT_TOKEN is treated as an HMAC SECRET (server-side only). The
# operator / a control plane mints short-lived, room-bound tokens with
# mint_room_token(); clients present them as ?token=<SIGNED>. This closes the
# original threat-model gap ("one token reads/writes any document") AND adds
# expiry, because a token is cryptographically bound to a room_id and a TTL.
def _b64url_encode(data: bytes) -> str:
    """URL-safe base64 without padding."""
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64url_decode(data: str) -> bytes:
    """Inverse of _b64url_encode (re-pads)."""
    padded = data + "=" * (-len(data) % 4)
    return base64.urlsafe_b64decode(padded)


def mint_room_token(room_id: str, secret: str, ttl: int = 300, now: Optional[float] = None) -> str:
    """Mint an HMAC-signed, short-lived token bound to ``room_id``.

    Token format: ``<payload>.<signature>`` where
      payload   = base64url( ``f"{room_id}|{exp}"`` )
      signature = HMAC-SHA256(secret, payload)  (base64url)

    Returns a string the client passes as ``?token=<signed>``.
    """
    if not secret:
        raise ValueError("a non-empty secret is required to mint a room token")
    now = time.time() if now is None else float(now)
    exp = int(now) + int(ttl)
    payload = _b64url_encode(f"{room_id}|{exp}".encode("utf-8"))
    signature = hmac.new(
        secret.encode("utf-8"), payload.encode("ascii"), hashlib.sha256
    ).digest()
    return f"{payload}.{_b64url_encode(signature)}"


def verify_room_token(token: str, room_id: str, secret: str, now: Optional[float] = None):
    """Verify a per-room signed token.

    Returns ``(ok: bool, reason: str)``. Fails when the token is missing,
    malformed, has a bad signature, is bound to a different room, or expired.
    """
    if not secret:
        return False, "server misconfiguration: no secret set"
    if not token:
        return False, "missing token"
    try:
        payload_b64, signature_b64 = token.split(".", 1)
    except ValueError:
        return False, "malformed token"
    expected = hmac.new(
        secret.encode("utf-8"), payload_b64.encode("ascii"), hashlib.sha256
    ).digest()
    try:
        provided = _b64url_decode(signature_b64)
    except Exception:
        return False, "malformed signature"
    if not hmac.compare_digest(expected, provided):
        return False, "bad signature"
    try:
        decoded = _b64url_decode(payload_b64).decode("utf-8")
        token_room, exp_str = decoded.split("|", 1)
    except Exception:
        return False, "malformed payload"
    if token_room != room_id:
        return False, "room mismatch"
    now = time.time() if now is None else float(now)
    if int(exp_str) < int(now):
        return False, "expired"
    return True, "ok"


def _is_token_valid(client_token: str, room_id: str) -> bool:
    """Validate a presented token against the configured auth policy (P1-3).

    A token is accepted if ANY holds:
    - No auth configured -> always allowed.
    - It is a valid HMAC-signed short-lived token bound to ``room_id``
      (minted with the shared CRDT_TOKEN secret via mint_room_token). [preferred]
    - Per-room static secrets (CRDT_TOKENS) are configured and the token equals
      the secret bound to ``room_id``.
    - Only a shared token (CRDT_TOKEN) is configured and the token equals it
      (legacy, non-expiring fallback).
    """
    if not AUTH_ENABLED:
        return True
    # (1) Signed, short-lived, room-bound token — preferred scheme.
    if AUTH_TOKEN and verify_room_token(client_token, room_id, AUTH_TOKEN)[0]:
        return True
    # (2) Per-room static secrets.
    if ROOM_TOKENS:
        expected = ROOM_TOKENS.get(room_id)
        return expected is not None and client_token == expected
    # (3) Legacy shared token (no per-room map).
    return client_token == AUTH_TOKEN


# ─── Connection handler (with rate limiting + heartbeat + error frames) ──
async def handle_client(websocket: WebSocketServerProtocol, path: str = ""):
    """Handle a WebSocket client connection.

    Path format: /yjs/<document_id>[?token=<CRDT_TOKEN>]
    """
    # ── Extract room ID ────────────────────────────────────────────────
    room_id = _extract_room_id(path)

    # ── Authentication (P1-3: per-room tokens + forced WSS) ────────────
    if AUTH_ENABLED:
        # A token on a plaintext ws:// connection is visible to network observers.
        # WSS is mandatory whenever authentication is enabled.
        if not TLS_ENABLED:
            logger.warning(
                "Rejected connection to room '%s': auth enabled but WSS not configured",
                room_id[:120],
            )
            await websocket.close(code=1008, reason="wss-required")
            return
        client_token = _extract_client_token(websocket, path)
        if not _is_token_valid(client_token, room_id):
            logger.warning(
                "Rejected client with invalid token (room='%s')", room_id[:120]
            )
            await websocket.close(code=1008, reason="unauthorized")
            return

    room = get_or_create_room(room_id)
    await room.add_client(websocket)

    # ── Race-condition guard (existing) ────────────────────────────────
    if rooms.get(room_id) is not room:
        await room.remove_client(websocket)
        room = get_or_create_room(room_id)
        await room.add_client(websocket)

    # ── Per-connection rate limiter (P1 fix) ───────────────────────────
    limiter = RateLimiter(MAX_MSG_PER_SECOND, MAX_BURST)

    # ── Heartbeat task (P2 fix) ────────────────────────────────────────
    async def heartbeat():
        """Send periodic pings; close if pong doesn't arrive in time."""
        try:
            while True:
                await asyncio.sleep(PING_INTERVAL)
                pong_waiter = await websocket.ping()
                await asyncio.wait_for(pong_waiter, timeout=PING_TIMEOUT)
        except asyncio.TimeoutError:
            logger.warning("Client heartbeat timeout in room '%s', closing", room_id)
            await websocket.close(code=1001, reason="heartbeat timeout")
        except websockets.ConnectionClosed:
            pass
        except Exception as exc:
            logger.debug("Heartbeat task ended for room '%s': %s", room_id, exc)

    heartbeat_task = asyncio.create_task(heartbeat())

    try:
        async for message in websocket:
            # ── Rate limiting check (P1 fix) ──────────────────────────
            if not limiter.acquire():
                logger.warning("Rate limit exceeded for client in room '%s'", room_id)
                try:
                    await websocket.send(
                        encode_error(ERR_RATE_LIMITED, "rate limit exceeded")
                    )
                except Exception:
                    pass
                continue

            if isinstance(message, bytes):
                # ── Message size check ────────────────────────────────
                if len(message) > MAX_MSG_SIZE:
                    logger.warning(
                        "Oversized message (%d bytes) in room '%s'", len(message), room_id
                    )
                    await websocket.close(code=1009, reason="message too big")
                    break

                sync_step, payload = decode_sync_payload(message)
                if sync_step == SYNC_STEP1:
                    await room.reply_sync_step2(websocket, payload)
                elif sync_step in (SYNC_STEP2, SYNC_UPDATE):
                    await room.broadcast_update(payload, exclude=websocket)
                elif sync_step is None:
                    # Unframed binary — treat as update for backwards compat
                    await room.broadcast_update(message, exclude=websocket)
                else:
                    # P1 fix: notify invalid message instead of silent ignore
                    try:
                        await websocket.send(
                            encode_error(ERR_INVALID_MESSAGE, "unknown sync step")
                        )
                    except Exception:
                        pass

            elif isinstance(message, str):
                logger.debug(f"Received text message in room '{room_id}': {message[:100]}")
                # Broadcast to other clients
                targets = [c for c in room.clients if c != websocket]
                if targets:
                    await asyncio.gather(
                        *[c.send(message) for c in targets],
                        return_exceptions=True,
                    )

    except websockets.ConnectionClosed:
        pass
    finally:
        heartbeat_task.cancel()
        try:
            await heartbeat_task
        except asyncio.CancelledError:
            pass
        await room.remove_client(websocket)


# ─── Server entry point ────────────────────────────────────────────────────
async def main():
    """Start the WebSocket server (with optional TLS/WSS)."""
    host = CRDT_HOST
    port = CRDT_PORT

    # P2 fix: TLS/WSS support
    use_tls = bool(TLS_CERT and TLS_KEY)
    scheme = "wss" if use_tls else "ws"
    logger.info(f"Starting Yjs CRDT server on {scheme}://{host}:{port}")
    logger.info(f"CRDT library: {CRDT_LIB} (YPY_AVAILABLE={YPY_AVAILABLE})")
    logger.info(f"Persistence dir: {PERSISTENCE_DIR.absolute()}")
    logger.info(f"Rate limit: {MAX_MSG_PER_SECOND} msg/s, burst {MAX_BURST}")
    logger.info(f"Heartbeat: ping every {PING_INTERVAL}s, timeout {PING_TIMEOUT}s")
    logger.info(f"Max message size: {MAX_MSG_SIZE} bytes")
    if use_tls:
        logger.info(f"TLS cert: {TLS_CERT}")
        if AUTH_TOKEN:
            logger.info("Token authentication: ENABLED (secure over WSS)")
        else:
            logger.warning("WSS enabled but no CRDT_TOKEN set — unauthenticated access")
    elif host == "0.0.0.0" and not AUTH_TOKEN:
        logger.warning(
            "Binding to 0.0.0.0 WITHOUT TLS or token authentication — "
            "server is exposed to the network!"
        )
    elif AUTH_ENABLED and not use_tls:
        logger.warning(
            "Authentication is ENABLED but WSS is NOT configured — all client "
            "connections will be rejected (code 1008, 'wss-required'). "
            "Set CRDT_TLS_CERT/CRDT_TLS_KEY to serve authenticated rooms over wss://."
        )
    if AUDIT_LOG_PATH:
        logger.info(f"Audit log: {AUDIT_LOG_PATH}")
    if AUTH_ENABLED:
        if ROOM_TOKENS:
            logger.info(
                "Token authentication: ENABLED (per-room tokens for %d room(s); WSS required)",
                len(ROOM_TOKENS),
            )
        else:
            logger.info("Token authentication: ENABLED (shared CRDT_TOKEN; WSS required)")
    else:
        logger.info("Token authentication: disabled (CRDT_TOKEN / CRDT_TOKENS not set)")
    logger.info(f"Connect with: {scheme}://{host}:{port}/yjs/<document_id>")

    serve_kwargs = dict(
        max_size=MAX_MSG_SIZE,
        ping_interval=PING_INTERVAL,
        ping_timeout=PING_TIMEOUT,
    )
    if use_tls:
        import ssl
        ssl_ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ssl_ctx.load_cert_chain(TLS_CERT, TLS_KEY)
        serve_kwargs["ssl"] = ssl_ctx

    async with websockets.serve(handle_client, host, port, **serve_kwargs):
        await asyncio.Future()  # Run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Server shutting down...")
