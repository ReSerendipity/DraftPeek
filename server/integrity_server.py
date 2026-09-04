"""
DraftPeek Play Integrity Verification Server

A lightweight FastAPI endpoint that receives Play Integrity tokens from
the Android client and forwards them to Google's Play Integrity API for
server-side decryption and verification.

This implements the "server-side verification" half of the Play Integrity
flow that was missing (see Security Assessment P0-2).

Dependencies:
    pip install fastapi uvicorn httpx

Usage:
    uvicorn server.integrity_server:app --reload --port 8001

    # Or run directly:
    python server/integrity_server.py

Environment variables:
    INTEGRITY_HOST          - Host to bind (default 127.0.0.1; non-loopback requires a token)
    INTEGRITY_PORT          - Port to listen on (default 8001)
    INTEGRITY_PROJECT_NUMBER- Google Cloud project number
    INTEGRITY_SERVICE_ACCOUNT_EMAIL - Service account email for auth
    INTEGRITY_AUTH_TOKEN    - Bearer token for client auth (optional but recommended)

References:
    https://developer.android.com/google/play/integrity/overview
    https://cloud.google.com/play-integrity/docs/verify-integrity

Status: Implemented (2026-08-27), EXPERIMENTAL — NOT production-hardened.
        DraftPeek is a local-first Android editor; this server is an OPTIONAL
        auxiliary service (no wired client in the shipped app, default-off).
        See 协作同步服务设计评估报告_v1.0.30.md for the threat model and the
        required hardening: server-side nonce freshness/timestamp checks and
        returning 5xx (not a mock verdict) when Google credentials are missing.
"""

import logging
import os
import time
from typing import Optional

import httpx
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel

logger = logging.getLogger("integrity_server")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

app = FastAPI(
    title="DraftPeek Integrity Server",
    description="Play Integrity token server-side verification",
    version="1.0.0",
)

# ── Configuration ──────────────────────────────────────────────
PORT = int(os.environ.get("INTEGRITY_PORT", "8001"))
PROJECT_NUMBER = os.environ.get("INTEGRITY_PROJECT_NUMBER", "")
SERVICE_ACCOUNT_EMAIL = os.environ.get("INTEGRITY_SERVICE_ACCOUNT_EMAIL", "")
AUTH_TOKEN = os.environ.get("INTEGRITY_AUTH_TOKEN", "").strip()

# R7 整改：绑定地址默认改为回环。原实现 `__main__` 硬编码 host="0.0.0.0"，
# 叠加"未配置 token 即放行"的 fail-open 鉴权，可被当作开放中继滥用。
# 现改为：默认仅监听 127.0.0.1；监听非回环地址必须配置 INTEGRITY_AUTH_TOKEN。
HOST = os.environ.get("INTEGRITY_HOST", "127.0.0.1").strip()
_LOOPBACK_HOSTS = frozenset({"127.0.0.1", "localhost", "::1"})
INTEGRITY_REQUIRE_AUTH = os.environ.get(
    "INTEGRITY_REQUIRE_AUTH", ""
).lower() in ("1", "true", "yes")


def _is_loopback_bind() -> bool:
    """是否仅监听回环地址。"""
    return HOST in _LOOPBACK_HOSTS


def _auth_required() -> bool:
    """是否强制鉴权：非回环地址一律强制；回环地址可用 INTEGRITY_REQUIRE_AUTH 强制。"""
    return INTEGRITY_REQUIRE_AUTH or not _is_loopback_bind()

# Google Play Integrity API endpoint
GOOGLE_INTEGRITY_API = (
    "https://playintegrity.googleapis.com"
    f"/v1/projects/{PROJECT_NUMBER}:verifyIntegrityToken"
    if PROJECT_NUMBER
    else ""
)

# ── Nonce freshness (P1-4) ────────────────────────────────────────────────
# The client may attach an epoch-millisecond `nonce_timestamp`. When present we
# reject replays outside the TTL window. Set INTEGRITY_REQUIRE_NONCE_TIMESTAMP
# to force clients to always supply it.
INTEGRITY_NONCE_TTL_SECONDS = int(os.environ.get("INTEGRITY_NONCE_TTL_SECONDS", "600"))
INTEGRITY_REQUIRE_NONCE_TIMESTAMP = os.environ.get(
    "INTEGRITY_REQUIRE_NONCE_TIMESTAMP", "false"
).lower() in ("1", "true", "yes")


class IntegrityRequest(BaseModel):
    """Client request payload."""
    token: str
    nonce: str
    package_name: str = "com.draftpeek"
    # Optional epoch-millisecond timestamp used for server-side freshness checks.
    nonce_timestamp: Optional[int] = None


class IntegrityResponse(BaseModel):
    """Response sent back to the client."""
    verified: bool
    device_integrity: str = "UNKNOWN"
    app_integrity: str = "UNKNOWN"
    account_lesson: str = "UNKNOWN"
    error: Optional[str] = None


def _verify_auth(authorization: Optional[str]) -> None:
    """Verify the bearer token from the client, if configured.

    R7 整改（fail-open → fail-closed）：
    原实现在未配置 token 时直接 return（放行全部请求），叠加硬编码 0.0.0.0，
    可被当作 Play Integrity 开放中继滥用。现改为：仅当监听回环地址且未显式
    强制鉴权时允许免鉴权；非回环地址缺 token → 拒绝请求。
    """
    if not AUTH_TOKEN:
        if _auth_required():
            # 正常启动路径已在 __main__ 拒绝启动；
            # 此处为 `uvicorn ... --host 0.0.0.0` 外部启动时的纵深防御。
            raise HTTPException(
                status_code=503,
                detail=(
                    "Integrity server misconfigured: bound to a non-loopback address "
                    "without INTEGRITY_AUTH_TOKEN. Refusing to serve (fail-closed)."
                ),
            )
        return
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing Authorization header")
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(status_code=401, detail="Invalid Authorization header")
    if parts[1] != AUTH_TOKEN:
        raise HTTPException(status_code=403, detail="Invalid auth token")


def _verify_nonce_freshness(nonce_timestamp: Optional[int]) -> None:
    """Server-side nonce freshness check (P1-4).

    Rejects replays / stale nonces outside the TTL window. A client that does
    not send a timestamp is accepted only when enforcement is off (default), so
    existing clients keep working while new clients can opt into strict mode.
    """
    if nonce_timestamp is None:
        if INTEGRITY_REQUIRE_NONCE_TIMESTAMP:
            raise HTTPException(status_code=400, detail="Missing nonce_timestamp")
        return
    now_ms = int(time.time() * 1000)
    age_ms = now_ms - nonce_timestamp
    if age_ms < 0 or age_ms > INTEGRITY_NONCE_TTL_SECONDS * 1000:
        raise HTTPException(
            status_code=400,
            detail=f"Nonce expired or not yet valid (TTL={INTEGRITY_NONCE_TTL_SECONDS}s)",
        )


async def _call_google_api(token: str, nonce: str, package_name: str) -> dict:
    """
    Forward the integrity token to Google's Play Integrity API for decryption.

    Returns the decoded verdict from Google.

    Fail-closed (P1-4): when Google credentials are not configured we MUST NOT
    return a mock "UNKNOWN" verdict that could be mistaken for a real check. We
    surface a 503 so the client treats verification as unavailable rather than
    silently trusted.
    """
    if not GOOGLE_INTEGRITY_API:
        logger.error("Google credentials not configured; cannot verify integrity token")
        raise HTTPException(
            status_code=503,
            detail="Play Integrity verification unavailable: Google credentials not configured",
        )

    payload = {
        "integrityToken": token,
        "nonce": nonce,
        "applicationPackageName": package_name,
    }

    async with httpx.AsyncClient(timeout=10.0) as client:
        resp = await client.post(
            GOOGLE_INTEGRITY_API,
            json=payload,
            headers={"Content-Type": "application/json"},
        )
        resp.raise_for_status()
        return resp.json()


@app.post("/integrity/verify", response_model=IntegrityResponse)
async def verify_integrity(
    req: IntegrityRequest,
    authorization: Optional[str] = Header(None),
):
    """
    Receive a Play Integrity token from the Android client and verify it
    server-side via Google's Play Integrity API.

    Returns a structured verdict the client can use to make trust decisions.
    """
    _verify_auth(authorization)

    if not req.token:
        raise HTTPException(status_code=400, detail="Missing integrity token")

    # Server-side nonce freshness check (P1-4)
    _verify_nonce_freshness(req.nonce_timestamp)

    try:
        verdict = await _call_google_api(req.token, req.nonce, req.package_name)
    except httpx.HTTPStatusError as e:
        logger.error("Google API returned error: %s", e)
        return IntegrityResponse(
            verified=False,
            error=f"Google API error: {e.response.status_code}",
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error("Failed to call Google API: %s", e)
        return IntegrityResponse(verified=False, error=str(e))

    payload = verdict.get("tokenPayloadExternal", {})

    device_integrity = (
        payload.get("deviceIntegrity", {})
        .get("deviceRecency", "UNKNOWN")
    )
    app_integrity = (
        payload.get("appIntegrity", {})
        .get("appRecognitionVerdict", "UNKNOWN")
    )
    account_lesson = (
        payload.get("accountDetails", {})
        .get("accountVerdict", "UNKNOWN")
    )

    # A device is "trusted" if it passes both device and app integrity
    verified = (
        device_integrity in ("UPTODATE", "LAST_90_DAYS")
        and app_integrity == "PLAY_RECOGNIZED"
    )

    logger.info(
        "Integrity verdict: device=%s app=%s account=%s verified=%s",
        device_integrity, app_integrity, account_lesson, verified,
    )

    return IntegrityResponse(
        verified=verified,
        device_integrity=device_integrity,
        app_integrity=app_integrity,
        account_lesson=account_lesson,
    )


@app.get("/health")
async def health():
    return {"status": "ok", "service": "integrity_server"}


if __name__ == "__main__":
    import uvicorn
    # R7 fail-fast：非回环绑定但未配置 token → 拒绝启动，杜绝开放中继。
    # （_verify_auth 内亦有 fail-closed 兜底，覆盖 uvicorn 外部启动的场景）
    if _auth_required() and not AUTH_TOKEN:
        logger.error(
            "SECURITY: refusing to start. Integrity server is bound to '%s' (non-loopback) "
            "but INTEGRITY_AUTH_TOKEN is not configured. "
            "Set the token, or bind to 127.0.0.1 for local-only use.",
            HOST,
        )
        raise SystemExit(2)

    logger.info(
        "Starting DraftPeek Integrity Server on %s:%d (auth=%s)",
        HOST,
        PORT,
        "enabled" if AUTH_TOKEN else "disabled (loopback only)",
    )
    uvicorn.run(app, host=HOST, port=PORT)
