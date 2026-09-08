"""
Tests for the Play Integrity verification server (server/integrity_server.py).

These lock in the P1-4 hardening from ../../docs/reports/协作同步服务设计评估报告_v1.0.30.md:
- Fail-closed: when Google credentials are NOT configured the server must return
  503 (not a mock "UNKNOWN" verdict that could be mistaken for a real check).
- Nonce freshness: an expired/out-of-window nonce_timestamp is rejected (400).

Run: python -m pytest server/tests/test_integrity_server.py -v
"""

import os
import sys
import time
from pathlib import Path

import pytest

# Ensure server dir is importable and Google creds are explicitly absent so the
# fail-closed path is exercised.
sys.path.insert(0, str(Path(__file__).parent.parent))
os.environ.pop("INTEGRITY_PROJECT_NUMBER", None)
os.environ.pop("INTEGRITY_SERVICE_ACCOUNT_EMAIL", None)

from fastapi.testclient import TestClient  # noqa: E402

import integrity_server  # noqa: E402


@pytest.fixture()
def client():
    with TestClient(integrity_server.app) as c:
        yield c


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["service"] == "integrity_server"


def test_fail_closed_without_google_credentials(client):
    """P1-4: no Google creds -> 503, never a mock verdict."""
    resp = client.post(
        "/integrity/verify",
        json={"token": "abc", "nonce": "xyz"},
    )
    assert resp.status_code == 503
    assert "Google credentials" in resp.json()["detail"]


def test_nonce_expired_rejected(client):
    """P1-4: a clearly stale nonce_timestamp must be rejected with 400."""
    # 2 hours ago — unambiguously outside the 600s TTL (avoids boundary flakiness).
    stale = int(time.time() * 1000) - 2 * 60 * 60 * 1000
    resp = client.post(
        "/integrity/verify",
        json={"token": "abc", "nonce": "xyz", "nonce_timestamp": stale},
    )
    assert resp.status_code == 400
    assert "Nonce" in resp.json()["detail"]


def test_nonce_future_rejected(client):
    """P1-4: a not-yet-valid (future) nonce_timestamp must be rejected."""
    future = int(time.time() * 1000) + 2 * 60 * 60 * 1000
    resp = client.post(
        "/integrity/verify",
        json={"token": "abc", "nonce": "xyz", "nonce_timestamp": future},
    )
    assert resp.status_code == 400


def test_missing_token_rejected(client):
    # `token` is a required request field, so FastAPI rejects with 422 before
    # the handler runs — still a rejection of an unauthenticated request.
    resp = client.post("/integrity/verify", json={"nonce": "xyz"})
    assert resp.status_code == 422


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
