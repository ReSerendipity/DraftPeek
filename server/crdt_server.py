"""
Yjs CRDT Collaboration Server

A simple WebSocket server for real-time collaborative editing using Yjs CRDT.
Each document gets its own "room" where connected clients can sync changes.

Dependencies:
    pip install websockets uvicorn ypy

Usage:
    python crdt_server.py

    # Connect from client:
    # ws://localhost:8080/yjs/<document_id>
    # ws://localhost:8080/yjs/<document_id>?token=<CRDT_TOKEN>  (when CRDT_TOKEN is set)

Environment variables:
    CRDT_HOST   bind address (default: 127.0.0.1; set to 0.0.0.0 to expose on LAN/network)
    CRDT_PORT   listen port (default: 8080)
    CRDT_TOKEN  optional shared token; when set, clients MUST pass it as the
                ?token=<CRDT_TOKEN> query parameter, otherwise the connection
                is rejected with close code 1008 (policy violation).

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
"""

import asyncio
import logging
import os
from typing import Dict, Set
from urllib.parse import parse_qs

import websockets
from websockets.server import WebSocketServerProtocol

try:
    from ypy import Doc
    YPY_AVAILABLE = True
except ImportError:
    YPY_AVAILABLE = False
    logging.warning("ypy not installed. Running in pass-through mode.")

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
logger = logging.getLogger("crdt_server")

# --- Server configuration (default: loopback only; explicit opt-in to expose) ---
# 默认仅绑定本机回环地址，避免无认证的协作服务暴露到局域网/公网；
# 需要对外提供协作服务时，显式设置 CRDT_HOST=0.0.0.0。
CRDT_HOST = os.environ.get("CRDT_HOST", "127.0.0.1").strip()
CRDT_PORT = int(os.environ.get("CRDT_PORT", "8080"))
# 可选共享令牌认证：设置 CRDT_TOKEN 后，客户端必须携带 ?token=<CRDT_TOKEN>，
# 否则连接被拒绝（关闭码 1008）。未设置时保持无认证（兼容旧客户端）。
AUTH_TOKEN = os.environ.get("CRDT_TOKEN", "").strip()

# Room management: each room is a Yjs document shared among connected clients
rooms: Dict[str, "Room"] = {}

# --- y-protocols sync framing (see module docstring) ---
MSG_SYNC = 0
SYNC_STEP1 = 0
SYNC_STEP2 = 1
SYNC_UPDATE = 2


def encode_sync_step2(update: bytes) -> bytes:
    """Wrap an update into a sync step2 frame: [0, 1, update]."""
    return bytes([MSG_SYNC, SYNC_STEP2]) + update


def encode_sync_update(update: bytes) -> bytes:
    """Wrap an update into a sync update frame: [0, 2, update]."""
    return bytes([MSG_SYNC, SYNC_UPDATE]) + update


def decode_sync_payload(message: bytes):
    """Return (sync_step, payload) for a sync frame, else (None, message)."""
    if len(message) >= 2 and message[0] == MSG_SYNC:
        if message[1] in (SYNC_STEP1, SYNC_STEP2, SYNC_UPDATE):
            return message[1], message[2:]
    return None, message


class Room:
    """Represents a collaborative document room."""

    def __init__(self, room_id: str):
        self.room_id = room_id
        self.clients: Set[WebSocketServerProtocol] = set()
        if YPY_AVAILABLE:
            self.doc = Doc()
        else:
            self.doc = None
        logger.info(f"Room created: {room_id}")

    async def add_client(self, websocket: WebSocketServerProtocol):
        """Add a new client to the room."""
        self.clients.add(websocket)
        logger.info(
            f"Client joined room '{self.room_id}'. "
            f"Total clients: {len(self.clients)}"
        )

        # 按 y-websocket 协议，服务端不主动推送裸 state vector；
        # 新客户端发送 sync step1（state vector），由 handle_client 以
        # sync step2（update）应答，完成 step1/step2 握手。

    async def remove_client(self, websocket: WebSocketServerProtocol):
        """Remove a client from the room."""
        self.clients.discard(websocket)
        logger.info(
            f"Client left room '{self.room_id}'. "
            f"Remaining clients: {len(self.clients)}"
        )
        # Clean up empty rooms.
        # 竞态防护：仅当 rooms 字典仍指向本房间时才删除，避免误删
        # 在 add_client 挂起期间被新客户端重新引用的房间。
        if not self.clients and rooms.get(self.room_id) is self:
            del rooms[self.room_id]
            logger.info(f"Room deleted (empty): {self.room_id}")

    async def broadcast_update(self, update: bytes, exclude: WebSocketServerProtocol = None):
        """Broadcast an update to all clients in the room except the sender.

        Outgoing updates are wrapped in a sync update frame ([0, 2, update])
        per y-protocols.
        """
        if self.doc and YPY_AVAILABLE:
            self.doc.apply_update(update)

        framed = encode_sync_update(update)
        disconnected = set()
        for client in self.clients:
            if client != exclude:
                try:
                    await client.send(framed)
                except websockets.ConnectionClosed:
                    disconnected.add(client)

        # Clean up disconnected clients
        for client in disconnected:
            self.clients.discard(client)


    async def reply_sync_step2(self, websocket: WebSocketServerProtocol, state_vector: bytes):
        """Answer a sync step1 with a sync step2 carrying the diff update.

        The update transforms a document whose state equals ``state_vector``
        into the current server-side document state.
        """
        if not (self.doc and YPY_AVAILABLE):
            logger.warning("ypy not available; cannot answer sync step1 for room '%s'", self.room_id)
            return
        try:
            update = self.doc.get_update(state_vector)
            await websocket.send(encode_sync_step2(update))
        except Exception as exc:
            logger.error("Failed to compute sync step2 for room '%s': %s", self.room_id, exc)


def get_or_create_room(room_id: str) -> Room:
    """Get an existing room or create a new one."""
    if room_id not in rooms:
        rooms[room_id] = Room(room_id)
    return rooms[room_id]


def _extract_client_token(websocket: WebSocketServerProtocol, path: str) -> str:
    """Extract the 'token' query parameter from the WebSocket request.

    Works across websockets API versions: the legacy handler ``path`` argument
    (websockets < 14) and ``websocket.request.path`` (websockets >= 14) both
    carry the full request target including the query string.
    """
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


async def handle_client(websocket: WebSocketServerProtocol, path: str):
    """
    Handle a WebSocket client connection.

    The path should be in the format: /yjs/<document_id>

    When CRDT_TOKEN is configured, clients must connect with
    ?token=<CRDT_TOKEN>; connections without a valid token are rejected.
    """
    # Optional shared-token authentication (enabled via CRDT_TOKEN env var)
    if AUTH_TOKEN:
        client_token = _extract_client_token(websocket, path)
        if client_token != AUTH_TOKEN:
            logger.warning(
                "Rejected client without valid token (path='%s')", path[:120]
            )
            await websocket.close(code=1008, reason="unauthorized")
            return

    # Extract room ID from path (query string excluded)
    path_no_query = path.split("?", 1)[0]
    parts = path_no_query.strip("/").split("/")
    if len(parts) >= 2 and parts[0] == "yjs":
        room_id = parts[1]
    else:
        room_id = path_no_query.strip("/").split("/")[-1]

    room = get_or_create_room(room_id)
    await room.add_client(websocket)

    # 竞态防护：add_client 挂起期间，若最后一个旧客户端离开导致房间被删除，
    # 本客户端会挂在已脱离 rooms 字典的孤儿房间上；此处校验并重新加入当前房间。
    if rooms.get(room_id) is not room:
        await room.remove_client(websocket)
        room = get_or_create_room(room_id)
        await room.add_client(websocket)

    try:
        async for message in websocket:
            if isinstance(message, bytes):
                sync_step, payload = decode_sync_payload(message)
                if sync_step == SYNC_STEP1:
                    # sync step1：客户端发来 state vector，服务端应答 step2
                    await room.reply_sync_step2(websocket, payload)
                elif sync_step in (SYNC_STEP2, SYNC_UPDATE):
                    # sync step2 / update：应用到文档并广播给其他客户端
                    await room.broadcast_update(payload, exclude=websocket)
                else:
                    # 未带 sync 帧头的裸二进制：按裸 update 兼容处理
                    await room.broadcast_update(message, exclude=websocket)
            elif isinstance(message, str):
                # Text message: could be sync protocol step
                logger.debug(f"Received text message in room '{room_id}': {message[:100]}")
                # Broadcast to other clients
                for client in room.clients:
                    if client != websocket:
                        try:
                            await client.send(message)
                        except websockets.ConnectionClosed:
                            pass

    except websockets.ConnectionClosed:
        pass
    finally:
        await room.remove_client(websocket)


async def main():
    """Start the WebSocket server."""
    host = CRDT_HOST
    port = CRDT_PORT

    logger.info(f"Starting Yjs CRDT server on ws://{host}:{port}")
    logger.info(f"ypy available: {YPY_AVAILABLE}")
    if AUTH_TOKEN:
        logger.info("Token authentication: ENABLED (clients must pass ?token=<CRDT_TOKEN>)")
    else:
        logger.info("Token authentication: disabled (CRDT_TOKEN not set)")
    if host == "0.0.0.0" and not AUTH_TOKEN:
        logger.warning(
            "Binding to 0.0.0.0 WITHOUT token authentication - "
            "server is exposed to the network!"
        )
    logger.info(f"Connect with: ws://{host}:{port}/yjs/<document_id>")

    async with websockets.serve(handle_client, host, port):
        await asyncio.Future()  # Run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Server shutting down...")
