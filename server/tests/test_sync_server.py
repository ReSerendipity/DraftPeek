"""
Tests for Sync Server improvements.

These tests verify the P1-P2 fixes applied to server/sync_server/main.py:
- P1: Structured error responses (unified format)
- P1: Rate limiting
- P2: SQLite persistence with connection pooling
- P2: Atomic batch operations with rollback

Run: python -m pytest server/tests/test_sync_server.py -v
"""

import os
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

# Add both server dir and project root to path
_SERVER_DIR = str(Path(__file__).parent.parent)
_PROJECT_ROOT = str(Path(__file__).parent.parent.parent)
sys.path.insert(0, _SERVER_DIR)
sys.path.insert(0, _PROJECT_ROOT)


@pytest.fixture
def temp_storage(tmp_path):
    """Create a temporary storage directory and database."""
    os.environ["SYNC_STORAGE_DIR"] = str(tmp_path / "storage")
    os.environ["SYNC_DB_PATH"] = str(tmp_path / "storage" / "test.db")
    os.environ["SYNC_AUTH_USERNAME"] = ""
    os.environ["SYNC_AUTH_PASSWORD"] = ""
    os.environ["SYNC_RATE_LIMIT_PER_MIN"] = "1000"
    yield tmp_path
    # Cleanup
    for key in ["SYNC_STORAGE_DIR", "SYNC_DB_PATH", "SYNC_AUTH_USERNAME",
                 "SYNC_AUTH_PASSWORD", "SYNC_RATE_LIMIT_PER_MIN"]:
        os.environ.pop(key, None)


@pytest.fixture
def client(temp_storage):
    """Create a FastAPI test client with temp storage."""
    # Re-import after setting env vars
    import importlib

    import sync_server.main as sync_module
    importlib.reload(sync_module)
    with TestClient(sync_module.app) as c:
        yield c


# ─── P1: Structured error responses ──────────────────────────────────────

def test_health_check(client):
    """Health endpoint should return healthy status."""
    response = client.get("/sync/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "timestamp" in data


def test_download_not_found_returns_structured_error(client):
    """P1 fix: 404 should return unified error format."""
    response = client.get("/sync/download/nonexistent.txt")
    assert response.status_code == 404
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "NOT_FOUND"
    assert "message" in data["error"]


def test_delete_not_found_returns_structured_error(client):
    """P1 fix: DELETE on missing file should return structured error."""
    response = client.delete("/sync/delete/missing.txt")
    assert response.status_code == 404
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "NOT_FOUND"


def test_path_traversal_returns_structured_error(client):
    """P1 fix: path traversal should return structured error."""
    response = client.get("/sync/download/../../etc/passwd")
    assert response.status_code in (400, 404)
    if response.status_code == 400:
        data = response.json()
        assert "error" in data
        assert data["error"]["code"] == "INVALID_PATH"


# ─── P1: Rate limiting ────────────────────────────────────────────────────

def test_rate_limiting_blocks_excess_requests(client):
    """P1 fix: rate limiter should block requests exceeding the limit."""
    # Set a very low rate limit
    import sync_server.main as sync_module
    sync_module.RATE_LIMIT_PER_MINUTE = 3

    # Make requests exceeding the limit
    responses = []
    for _ in range(10):
        r = client.get("/sync/list")
        responses.append(r)

    # Some should be 429
    status_codes = [r.status_code for r in responses]
    assert 429 in status_codes, "Should have rate-limited responses"

    # The 429 response should have structured error
    rate_limited = [r for r in responses if r.status_code == 429][0]
    data = rate_limited.json()
    assert data["error"]["code"] == "RATE_LIMITED"


# ─── P2: SQLite persistence ──────────────────────────────────────────────

def test_upload_and_download_roundtrip(client):
    """P2 fix: file uploaded should be retrievable via download (SQLite-backed)."""
    import hashlib
    content = b"Hello, DraftPeek!"
    checksum = hashlib.sha256(content).hexdigest()

    # Upload
    response = client.post(
        "/sync/upload",
        data={"meta_path": "test.txt", "meta_version": 1, "meta_checksum": checksum},
        files={"file": ("test.txt", content, "application/octet-stream")},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["new_version"] == 2

    # Download
    response = client.get("/sync/download/test.txt")
    assert response.status_code == 200
    assert response.content == content
    assert response.headers["X-File-Version"] == "2"
    assert response.headers["X-File-Checksum"] == checksum


def test_upload_conflict_detection(client):
    """P2 fix: optimistic locking should detect version conflicts via SQLite."""
    import hashlib
    content = b"v1 content"
    checksum = hashlib.sha256(content).hexdigest()

    # Upload v1
    client.post(
        "/sync/upload",
        data={"meta_path": "conflict.txt", "meta_version": 1, "meta_checksum": checksum},
        files={"file": ("conflict.txt", content, "application/octet-stream")},
    )

    # Try to upload v1 again (should conflict — server version is now 2)
    response = client.post(
        "/sync/upload",
        data={"meta_path": "conflict.txt", "meta_version": 1, "meta_checksum": checksum},
        files={"file": ("conflict.txt", content, "application/octet-stream")},
    )
    assert response.status_code == 409
    data = response.json()
    assert data["error"]["code"] == "CONFLICT"


def test_list_files(client):
    """P2 fix: list should return files from SQLite database."""
    import hashlib
    content = b"list test"
    checksum = hashlib.sha256(content).hexdigest()

    client.post(
        "/sync/upload",
        data={"meta_path": "listable.txt", "meta_version": 1, "meta_checksum": checksum},
        files={"file": ("listable.txt", content, "application/octet-stream")},
    )

    response = client.get("/sync/list")
    assert response.status_code == 200
    data = response.json()
    assert len(data["files"]) >= 1
    paths = [f["path"] for f in data["files"]]
    assert "listable.txt" in paths


def test_delete_file(client):
    """P2 fix: delete should remove from both disk and SQLite."""
    import hashlib
    content = b"to be deleted"
    checksum = hashlib.sha256(content).hexdigest()

    client.post(
        "/sync/upload",
        data={"meta_path": "deletable.txt", "meta_version": 1, "meta_checksum": checksum},
        files={"file": ("deletable.txt", content, "application/octet-stream")},
    )

    response = client.delete("/sync/delete/deletable.txt")
    assert response.status_code == 200

    # Confirm gone
    response = client.get("/sync/download/deletable.txt")
    assert response.status_code == 404


# ─── P2: Batch operations ────────────────────────────────────────────────

def test_batch_delete(client):
    """P2 fix: batch delete should work atomically."""
    import hashlib
    for name in ["batch1.txt", "batch2.txt"]:
        content = name.encode()
        checksum = hashlib.sha256(content).hexdigest()
        client.post(
            "/sync/upload",
            data={"meta_path": name, "meta_version": 1, "meta_checksum": checksum},
            files={"file": (name, content, "application/octet-stream")},
        )

    response = client.post("/sync/batch", json=[
        {"type": "delete", "path": "batch1.txt"},
        {"type": "delete", "path": "batch2.txt"},
    ])
    assert response.status_code == 200
    data = response.json()
    assert all(r["status"] == "deleted" for r in data["results"])


# ─── P1: Checksum validation ────────────────────────────────────────────

def test_checksum_mismatch_returns_structured_error(client):
    """P1 fix: checksum mismatch should return structured error."""
    response = client.post(
        "/sync/upload",
        data={"meta_path": "bad.txt", "meta_version": 1, "meta_checksum": "0000"},
        files={"file": ("bad.txt", b"content", "application/octet-stream")},
    )
    assert response.status_code == 400
    data = response.json()
    assert data["error"]["code"] == "CHECKSUM_MISMATCH"


# ─── 安全加固回归（路径校验 / 元数据不可信） ─────────────────────────────

def test_upload_rejects_control_char_path(client):
    """路径含控制字符必须在任何文件系统写入之前被拒绝。"""
    response = client.post(
        "/sync/upload",
        data={"meta_path": "evil\r\n.txt", "meta_version": 1, "meta_checksum": "x"},
        files={"file": ("evil.txt", b"payload", "application/octet-stream")},
    )
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_PATH"


def test_upload_rejects_backslash_traversal(client):
    """Windows 分隔符形式的 .. 同样不得绕过目录校验。"""
    response = client.post(
        "/sync/upload",
        data={"meta_path": "a\\..\\..\\escape.txt", "meta_version": 1, "meta_checksum": "x"},
        files={"file": ("escape.txt", b"payload", "application/octet-stream")},
    )
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_PATH"


def test_download_refuses_tampered_storage_path(client, temp_storage):
    """DB 的 storage_path 只用于一致性核对，被篡改时不得据此读取其它位置。"""
    import hashlib
    import sqlite3

    import sync_server.main as sync_module

    content = b"integrity probe"
    client.post(
        "/sync/upload",
        data={
            "meta_path": "tampered.txt",
            "meta_version": 1,
            "meta_checksum": hashlib.sha256(content).hexdigest(),
        },
        files={"file": ("tampered.txt", content, "application/octet-stream")},
    )

    secret = temp_storage / "should-not-be-served.txt"
    secret.write_bytes(b"SHOULD NOT BE SERVED")
    conn = sqlite3.connect(sync_module.DB_PATH)
    conn.execute("UPDATE files SET storage_path = ? WHERE path = ?", (str(secret), "tampered.txt"))
    conn.commit()
    conn.close()

    response = client.get("/sync/download/tampered.txt")
    assert response.status_code == 404
    assert response.content != b"SHOULD NOT BE SERVED"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
