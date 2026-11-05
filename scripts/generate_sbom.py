#!/usr/bin/env python3
"""DraftPeek SBOM（CycloneDX 1.5 JSON）生成器（评估 2026-11-05 P1-S8 整改）。

从**已入库的依赖锁定文件**生成制品成分清单，随 Release 附件分发：

    数据源（全部为 git 内受版本控制的单一事实来源，无网络依赖）：
    - app/gradle.lockfile —— Gradle STRICT 依赖锁定（root build.gradle.kts
      `lockAllConfigurations()` + `LockMode.STRICT`），其中
      `productionReleaseRuntimeClasspath` 配置就是发布 APK
      （app-production-release.apk）实际打包的外部依赖闭包。
    - settings.gradle.kts —— 一方（first-party）Gradle 模块清单，
      即 APK 内DEX 的来源模块。
    - gradle.properties —— 版本位（draftpeek.version.{major,minor,patch}）。

    确定性：同一提交树重复运行输出字节一致 ——
    - serialNumber = uuid5（版本 + 组件清单摘要），不取随机数；
    - metadata.timestamp = HEAD 提交时间（可被 SOURCE_DATE_EPOCH 覆盖），不取挂钟；
    - 组件按 bom-ref 排序。

    口径说明（如实登记，不夸大）：
    - 本 SBOM 覆盖外部 Maven 依赖（release 运行时配置）+ 一方模块，不含
      native .so 内嵌的第三方库清单与资源级溯源；哈希（cryptoProperties/
      hashes）未包含 —— 锁定文件只有版本坐标，哈希留待接入
      Gradle Verification Metadata 后回填。
    - scope 规则：出现在 productionReleaseRuntimeClasspath → required；
      仅出现在测试/编译期配置的组件不入 components（计数记录在 metadata
      属性 draftpeek:excluded_* 中）。

用法：
    python scripts/generate_sbom.py --output app/build/sbom/sbom.cdx.json
        生成 + 自校验（写入后立即按 --verify 同规则复检，fail-closed）。
    python scripts/generate_sbom.py --verify app/build/sbom/sbom.cdx.json
        校验既有 SBOM 文件（结构/唯一性/purl 格式），CI 与人工复核用。

退出码：
    0  生成且自校验通过 / 校验通过
    1  自校验或校验失败（结构缺失、bom-ref 重复、purl 非法）
    2  输入错误（锁定文件/版本位缺失、解析失败）
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
APP_LOCKFILE = ROOT / "app" / "gradle.lockfile"
SETTINGS_FILE = ROOT / "settings.gradle.kts"
GRADLE_PROPS = ROOT / "gradle.properties"

# 发布制品 = production flavor × release build type；该配置即 APK 打包闭包
PACKAGED_CONFIG = "productionReleaseRuntimeClasspath"

# 测试/编译期专用配置的后缀（命中则计入 excluded 计数，不入 components）
_TEST_CONFIG_MARKERS = (
    "UnitTestCompileClasspath",
    "UnitTestRuntimeClasspath",
    "AndroidTestCompileClasspath",
    "AndroidTestRuntimeClasspath",
)

LOCK_LINE_RE = re.compile(r"^(?P<group>[^:]+):(?P<name>[^:]+):(?P<version>[^=]+)=(?P<configs>.*)$")
INCLUDE_RE = re.compile(r"""include\("(?P<path>[^"]+)"\)""")
VERSION_KEYS = ("draftpeek.version.major", "draftpeek.version.minor", "draftpeek.version.patch")

PURL_RE = re.compile(r"^pkg:maven/[^@\s]+@[^@\s]+$")
URN_RE = re.compile(r"^urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

REPO_URL = "https://github.com/ReSerendipity/DraftPeek"


def read_app_version() -> str:
    props: dict[str, str] = {}
    for line in GRADLE_PROPS.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        props[key.strip()] = value.strip()
    missing = [k for k in VERSION_KEYS if k not in props]
    if missing:
        raise SystemExit(f"[ERROR] gradle.properties 缺少版本位：{missing}")
    return f"{props[VERSION_KEYS[0]]}.{props[VERSION_KEYS[1]]}.{props[VERSION_KEYS[2]]}"


def parse_lockfile() -> tuple[list[dict[str, str]], int, int]:
    """解析 app/gradle.lockfile。

    返回 (打包组件列表, 一方外 excluded 计数, 不同坐标总数)。
    打包判定：该坐标的配置列表精确含 `productionReleaseRuntimeClasspath`。
    """
    if not APP_LOCKFILE.exists():
        raise SystemExit(f"[ERROR] 依赖锁定文件不存在：{APP_LOCKFILE.relative_to(ROOT)}")
    components: list[dict[str, str]] = []
    excluded = 0
    seen: set[tuple[str, str, str]] = set()
    for raw in APP_LOCKFILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        match = LOCK_LINE_RE.match(line)
        if not match:
            if line.startswith("empty="):
                # Gradle 锁定文件的「无依赖配置」行（empty=<config,...>），不含坐标，跳过
                continue
            raise SystemExit(f"[ERROR] 锁定文件行解析失败：{line[:80]}")
        key = (match.group("group"), match.group("name"), match.group("version"))
        seen.add(key)
        configs = [c.strip() for c in match.group("configs").split(",") if c.strip()]
        if PACKAGED_CONFIG in configs:
            components.append(
                {
                    "group": key[0],
                    "name": key[1],
                    "version": key[2],
                    "configs": ",".join(sorted(configs)),
                }
            )
        elif any(marker in config for config in configs for marker in _TEST_CONFIG_MARKERS):
            excluded += 1
    if not components:
        raise SystemExit(
            f"[ERROR] 锁定文件中没有任何组件落在 {PACKAGED_CONFIG} 配置上——"
            "锁定文件过期或口径漂移，拒绝生成空 SBOM"
        )
    components.sort(key=lambda c: (c["group"], c["name"], c["version"]))
    return components, excluded, len(seen)


def parse_first_party_modules() -> list[str]:
    if not SETTINGS_FILE.exists():
        raise SystemExit("[ERROR] settings.gradle.kts 不存在")
    modules = [m.group("path") for m in INCLUDE_RE.finditer(SETTINGS_FILE.read_text(encoding="utf-8"))]
    if not modules:
        raise SystemExit("[ERROR] settings.gradle.kts 未解析到任何 include() 模块")
    return sorted(modules)


def head_timestamp() -> str:
    """SBOM 时间戳：SOURCE_DATE_EPOCH 优先，否则 HEAD 提交时间（确定性，不取挂钟）。"""
    epoch = os.environ.get("SOURCE_DATE_EPOCH")
    if epoch:
        dt = datetime.fromtimestamp(int(epoch), tz=timezone.utc)
    else:
        raw = subprocess.run(
            ["git", "log", "-1", "--format=%cI"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout.strip()
        dt = datetime.fromisoformat(raw)
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def build_bom(version: str) -> dict[str, Any]:
    ext_components, excluded_test_only, total_coords = parse_lockfile()
    first_party = parse_first_party_modules()

    components: list[dict[str, Any]] = []
    for c in ext_components:
        purl = f"pkg:maven/{c['group']}/{c['name']}@{c['version']}"
        components.append(
            {
                "type": "library",
                "bom-ref": purl,
                "group": c["group"],
                "name": c["name"],
                "version": c["version"],
                "scope": "required",
                "purl": purl,
                "properties": [
                    {"name": "draftpeek:origin", "value": "external"},
                    {"name": "draftpeek:locked-configs", "value": c["configs"]},
                ],
            }
        )
    for module in first_party:
        ref = f"draftpeek:gradle-module:{module}"
        components.append(
            {
                "type": "library",
                "bom-ref": ref,
                "name": module,
                "version": version,
                "scope": "required",
                "properties": [
                    {"name": "draftpeek:origin", "value": "first-party"},
                    {"name": "draftpeek:build-system", "value": "gradle"},
                ],
            }
        )
    components.sort(key=lambda comp: comp["bom-ref"])

    # 确定性 serialNumber：uuid5(版本 + 组件 bom-ref 摘要)
    digest = hashlib.sha256(
        "\n".join([version] + [comp["bom-ref"] for comp in components]).encode("utf-8")
    ).hexdigest()
    serial = f"urn:uuid:{uuid.uuid5(uuid.NAMESPACE_URL, f'{REPO_URL}/sbom/{version}/{digest}')}"

    bom: dict[str, Any] = {
        "bomFormat": "CycloneDX",
        "specVersion": "1.5",
        "serialNumber": serial,
        "version": 1,
        "metadata": {
            "timestamp": head_timestamp(),
            "tools": {
                "components": [
                    {
                        "type": "application",
                        "name": "scripts/generate_sbom.py",
                        "version": "1.0",
                    }
                ]
            },
            "component": {
                "type": "application",
                "bom-ref": f"pkg:github/ReSerendipity/DraftPeek@v{version}",
                "name": "DraftPeek",
                "version": version,
                "purl": f"pkg:github/ReSerendipity/DraftPeek@v{version}",
                "properties": [
                    {"name": "draftpeek:artifact", "value": "app-production-release.apk"},
                    {"name": "draftpeek:lockfile", "value": "app/gradle.lockfile"},
                    {"name": "draftpeek:packaged-config", "value": PACKAGED_CONFIG},
                    {"name": "draftpeek:external-components", "value": str(len(ext_components))},
                    {"name": "draftpeek:first-party-modules", "value": str(len(first_party))},
                    {"name": "draftpeek:excluded-test-only-coords", "value": str(excluded_test_only)},
                    {"name": "draftpeek:locked-coords-total", "value": str(total_coords)},
                    {
                        "name": "draftpeek:known-gaps",
                        "value": "no dependency hashes; no native .so subcomponents; "
                        "scope=release runtime classpath only",
                    },
                ],
            },
        },
        "components": components,
    }
    return bom


def validate_bom(data: dict[str, Any]) -> list[str]:
    """结构校验（fail-closed）。返回错误列表，空列表 = 通过。"""
    errors: list[str] = []
    if data.get("bomFormat") != "CycloneDX":
        errors.append("bomFormat 不是 CycloneDX")
    if data.get("specVersion") != "1.5":
        errors.append("specVersion 不是 1.5")
    serial = data.get("serialNumber", "")
    if not URN_RE.match(serial):
        errors.append(f"serialNumber 非法：{serial!r}")
    components = data.get("components")
    if not isinstance(components, list) or not components:
        errors.append("components 缺失或为空")
        return errors
    refs = [c.get("bom-ref") for c in components]
    if len(set(refs)) != len(refs):
        errors.append("bom-ref 存在重复")
    for comp in components:
        for field in ("type", "name", "version", "bom-ref"):
            if not comp.get(field):
                errors.append(f"组件缺字段 {field}：{comp.get('name', '?')}")
        purl = comp.get("purl")
        if purl is not None and not PURL_RE.match(purl):
            errors.append(f"purl 非法：{purl!r}")
    metadata = data.get("metadata", {})
    if "component" not in metadata:
        errors.append("metadata.component 缺失（主组件未登记）")
    if "timestamp" not in metadata:
        errors.append("metadata.timestamp 缺失")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="DraftPeek SBOM 生成器（CycloneDX 1.5）")
    parser.add_argument("--output", type=Path, default=ROOT / "app/build/sbom/sbom.cdx.json",
                        help="输出路径（默认 app/build/sbom/sbom.cdx.json）")
    parser.add_argument("--verify", type=Path, default=None,
                        help="只校验既有 SBOM 文件，不生成")
    args = parser.parse_args()

    if args.verify:
        try:
            data = json.loads(args.verify.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            print(f"[ERROR] SBOM 文件读取/解析失败：{exc}", file=sys.stderr)
            return 2
        errors = validate_bom(data)
        if errors:
            print("[FAIL] SBOM 校验失败：", file=sys.stderr)
            for err in errors:
                print(f"  - {err}", file=sys.stderr)
            return 1
        components = data["components"]
        external = sum(
            1 for comp in components
            if any(p.get("value") == "external" for p in comp.get("properties", []))
        )
        print(
            f"[OK] SBOM 校验通过：{len(components)} 组件"
            f"（external {external} / first-party {len(components) - external}）"
            f"，serial={data['serialNumber']}"
        )
        return 0

    version = read_app_version()
    bom = build_bom(version)
    errors = validate_bom(bom)
    if errors:
        print("[FAIL] 生成结果未通过自校验（未写出文件）：", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(bom, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    external = sum(1 for comp in bom["components"] if comp.get("purl"))
    print(
        f"[OK] SBOM 已生成：{args.output}\n"
        f"  版本 {version}；组件 {len(bom['components'])}"
        f"（external {external} / first-party {len(bom['components']) - external}）；"
        f"serial={bom['serialNumber']}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
