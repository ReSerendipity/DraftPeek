"""版本一致性基础测试（家族简化版）

验证：
- 版本号符合 SemVer 格式（x.y.z）
- 不存在硬编码的旧版本号（0.1.0 / 0.0.1 等）
"""
from __future__ import annotations

import json
import re
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).resolve().parent.parent


def _read_version():
    """读取版本权威源，返回 (文件名, 版本号)；无可用来源时返回 (None, "")。

    逐个候选尝试：文件存在但读不出版本号（例如外围 Node 工具的 package.json
    没有 version 字段）时继续往下找，不要把「文件存在」当成「版本可读」。
    """
    candidates = [
        "package.json",
        "pyproject.toml",
        "version.json",
        "gradle.properties",
    ]
    for c in candidates:
        path = PROJECT_ROOT / c
        if not path.exists():
            continue
        content = path.read_text(encoding="utf-8", errors="ignore")

        # package.json
        if c == "package.json":
            version = str(json.loads(content).get("version", "")).strip()
        # pyproject.toml
        elif c == "pyproject.toml":
            m = re.search(r'version\s*=\s*["\']([^"\']+)["\']', content)
            version = m.group(1) if m else ""
        # version.json
        elif c == "version.json":
            version = str(json.loads(content).get("version", "")).strip()
        # gradle.properties：版本位集中在 draftpeek.version.{major,minor,patch}
        elif c == "gradle.properties":
            parts = [
                re.search(rf"^{key}\s*=\s*(\S+)", content, re.MULTILINE)
                for key in (
                    "draftpeek.version.major",
                    "draftpeek.version.minor",
                    "draftpeek.version.patch",
                )
            ]
            version = ".".join(p.group(1) for p in parts) if all(parts) else ""
        else:
            version = ""

        if version:
            return c, version
    return None, ""


def test_version_file_exists():
    vfile, _ = _read_version()
    assert vfile is not None, "未找到版本权威源文件"


def test_version_is_semver():
    vfile, version = _read_version()
    assert vfile, "未找到版本文件"

    assert re.fullmatch(r"\d+\.\d+\.\d+", version), f"{vfile} 的版本号不符合 SemVer: {version!r}"


def test_no_hardcoded_old_version():
    """代码中不应硬编码 0.1.0 / 0.0.1 等占位版本号"""
    old_patterns = [r'version\s*=\s*["\']0\.1\.0["\']', r'version\s*=\s*["\']0\.0\.1["\']']

    offenders = []
    for pyfile in PROJECT_ROOT.rglob("*.py"):
        if any(skip in str(pyfile) for skip in [".venv", "__pycache__", "node_modules"]):
            continue
        try:
            content = pyfile.read_text(encoding="utf-8", errors="ignore")
            for pat in old_patterns:
                if re.search(pat, content):
                    offenders.append(str(pyfile.relative_to(PROJECT_ROOT)))
                    break
        except:
            pass

    assert offenders == [], f"发现硬编码旧版本号: {offenders}"
