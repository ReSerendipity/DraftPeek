#!/usr/bin/env python3
"""Check version consistency across gradle.properties / CHANGELOG / Git tag / README.

Single source of truth for the "three-place version sync" gate (evaluation P1-2).
Used by BOTH:
  * .github/workflows/docs-consistency.yml  (PR-time, tolerant)
  * .github/workflows/release.yml           (tag-time, --strict)

Rationale
---------
The project keeps the release version in three places that were previously
synchronized by hand:
  1. gradle.properties   draftpeek.version.{major,minor,patch}
  2. CHANGELOG.md        '## [x.y.z] - YYYY-MM-DD'
  3. Git tag             v x.y.z  (must equal versionName)
A fourth drifting surface was observed in README.md, which advertised AGP
8.8.0 / Gradle 8.13 while the repo actually used 8.10.1 / 8.14.3.  That class
of drift had NO gate at all, so this script covers it too.

Modes
-----
default (PR-time, tolerant):
  FAIL  gradle version <  CHANGELOG latest released   (CHANGELOG ahead / version rolled back)
  FAIL  README AGP/Gradle/Kotlin != actual catalog value
  FAIL  versionCode formula in app/build.gradle.kts != major*10000+minor*100+patch
  WARN  gradle version >  CHANGELOG latest released   (CHANGELOG not updated yet)
        -> does NOT fail, because mid-development the version is legitimately
           bumped before the changelog entry is written.  The tag-time gate
           below is what makes "forgot to update CHANGELOG" fatal.

--strict (tag-time): additionally requires
  FAIL  --tag version != gradle.properties version
  FAIL  CHANGELOG has no '## [tag]' entry

Exit codes: 0 = pass (warnings allowed), 1 = fail.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

# --------------------------------------------------------------------------
# parsing helpers
# --------------------------------------------------------------------------


def parse_gradle_version(root: Path) -> tuple[int, int, int] | None:
    """Read draftpeek.version.{major,minor,patch} from gradle.properties."""
    props = root / "gradle.properties"
    if not props.is_file():
        return None
    text = props.read_text(encoding="utf-8")
    vals: dict[str, int] = {}
    for comp in ("major", "minor", "patch"):
        m = re.search(
            rf"^\s*draftpeek\.version\.{comp}\s*=\s*(\d+)\s*$", text, re.MULTILINE
        )
        if m:
            vals[comp] = int(m.group(1))
    if len(vals) == 3:
        return vals["major"], vals["minor"], vals["patch"]
    return None


def parse_changelog_releases(root: Path) -> tuple[str | None, bool]:
    """Return (latest released version string, has_unreleased_section)."""
    changelog = root / "CHANGELOG.md"
    if not changelog.is_file():
        return None, False
    text = changelog.read_text(encoding="utf-8")
    has_unreleased = bool(
        re.search(r"^##\s*\[?Unreleased\]?", text, re.MULTILINE | re.IGNORECASE)
    )
    versions = re.findall(r"^##\s*\[(\d+\.\d+\.\d+)\]", text, re.MULTILINE)
    return (versions[0] if versions else None), has_unreleased


def parse_catalog_version(root: Path, key: str) -> str | None:
    """Read a version value from gradle/libs.versions.toml (top-level [versions])."""
    catalog = root / "gradle" / "libs.versions.toml"
    if not catalog.is_file():
        return None
    text = catalog.read_text(encoding="utf-8")
    m = re.search(rf'^\s*{re.escape(key)}\s*=\s*"([^"]+)"\s*$', text, re.MULTILINE)
    return m.group(1) if m else None


def parse_gradle_wrapper_version(root: Path) -> str | None:
    props = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if not props.is_file():
        return None
    m = re.search(r"gradle-(\d+\.\d+(?:\.\d+)?)-", props.read_text(encoding="utf-8"))
    return m.group(1) if m else None


def parse_readme_table_version(root: Path, label: str) -> str | None:
    """Read '| <label> | <value> |' from any markdown table in README.md."""
    readme = root / "README.md"
    if not readme.is_file():
        return None
    text = readme.read_text(encoding="utf-8")
    m = re.search(rf"^\|\s*{re.escape(label)}\s*\|\s*([^|]+?)\s*\|", text, re.MULTILINE)
    return m.group(1) if m else None


def _first_version(text: str) -> str | None:
    m = re.search(r"(\d+\.\d+(?:\.\d+)?)", text or "")
    return m.group(1) if m else None


def check_versioncode_formula(root: Path, version: tuple[int, int, int]) -> str | None:
    """Verify app/build.gradle.kts still uses major*10000 + minor*100 + patch."""
    build = root / "app" / "build.gradle.kts"
    if not build.is_file():
        return None  # nothing to verify; do not invent a failure
    text = build.read_text(encoding="utf-8")
    expected_code = version[0] * 10000 + version[1] * 100 + version[2]
    m = re.search(
        r"versionCode\s*=\s*vMajor\s*\*\s*(\d+)\s*\+\s*vMinor\s*\*\s*(\d+)\s*\+\s*vPatch",
        text,
    )
    if not m:
        return (
            "app/build.gradle.kts 中未找到 versionCode 公式 "
            "'vMajor * 10000 + vMinor * 100 + vPatch'，无法校验（如已改公式请同步本脚本）"
        )
    major_mul, minor_mul = int(m.group(1)), int(m.group(2))
    actual_code = version[0] * major_mul + version[1] * minor_mul + version[2]
    if (major_mul, minor_mul) != (10000, 100):
        return (
            f"versionCode 公式已变为 vMajor*{major_mul} + vMinor*{minor_mul} + vPatch，"
            f"与约定 10000/100 不符（当前 versionCode 将变为 {actual_code}）"
        )
    if actual_code != expected_code:
        return f"versionCode 计算不一致：期望 {expected_code}，公式得 {actual_code}"
    return None


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument(
        "--strict",
        action="store_true",
        help="tag-time mode: also require --tag to match gradle.properties + CHANGELOG",
    )
    ap.add_argument("--tag", help="git tag being released, e.g. v1.0.30")
    ap.add_argument("--root", default=str(REPO_ROOT), help="repository root")
    args = ap.parse_args()

    root = Path(args.root).resolve()
    failures: list[str] = []
    warnings: list[str] = []
    notes: list[str] = []

    # --- 1. gradle.properties ------------------------------------------------
    version = parse_gradle_version(root)
    if version is None:
        print("FAIL: 无法从 gradle.properties 解析 draftpeek.version.{major,minor,patch}")
        return 1
    gradle_version = f"{version[0]}.{version[1]}.{version[2]}"
    version_code = version[0] * 10000 + version[1] * 100 + version[2]
    print(f"gradle.properties : {gradle_version}  (versionCode={version_code})")

    # --- 2. CHANGELOG --------------------------------------------------------
    changelog_version, has_unreleased = parse_changelog_releases(root)
    print(
        f"CHANGELOG         : {changelog_version or '<none>'}"
        f"  (Unreleased 节: {'yes' if has_unreleased else 'no'})"
    )
    if changelog_version is None:
        warnings.append("CHANGELOG.md 中未找到任何 '## [x.y.z]' 发布条目")
    else:

        def key(s: str) -> tuple:
            return tuple(int(p) for p in s.split("."))

        if key(gradle_version) < key(changelog_version):
            failures.append(
                f"版本回退/CHANGELOG 超前：gradle.properties={gradle_version} "
                f"< CHANGELOG 最新={changelog_version}"
            )
        elif key(gradle_version) > key(changelog_version):
            msg = (
                f"CHANGELOG 未同步：gradle.properties={gradle_version} "
                f"> CHANGELOG 最新={changelog_version}"
            )
            if args.strict:
                failures.append(msg + "（--strict 模式下为硬失败）")
            else:
                warnings.append(msg + "（开发期正常，打 tag 前必须补齐）")

    # --- 3. tag (strict only) ------------------------------------------------
    if args.strict:
        if not args.tag:
            failures.append("--strict 模式必须提供 --tag")
        else:
            tag_version = args.tag[1:] if args.tag.startswith("v") else args.tag
            print(f"git tag           : {tag_version} (raw={args.tag})")
            if tag_version != gradle_version:
                failures.append(
                    f"tag 与 gradle.properties 不一致：tag={tag_version} "
                    f"!= gradle.properties={gradle_version}"
                )
            else:
                changelog = root / "CHANGELOG.md"
                text = changelog.read_text(encoding="utf-8") if changelog.is_file() else ""
                if not re.search(
                    rf"^##\s*\[{re.escape(tag_version)}\]", text, re.MULTILINE
                ):
                    failures.append(
                        f"CHANGELOG.md 缺少 '## [{tag_version}]' 条目；"
                        "请先运行 ./gradlew generateChangelog 或手动补齐"
                    )

    # --- 4. README 声明版本 vs 实际 ------------------------------------------
    actual_agp = parse_catalog_version(root, "agp")
    actual_gradle = parse_gradle_wrapper_version(root)
    actual_kotlin = parse_catalog_version(root, "kotlin")
    actual_buildtools = parse_catalog_version(root, "buildTools")

    for label, actual in (
        ("AGP", actual_agp),
        ("Gradle", actual_gradle),
        ("Kotlin", actual_kotlin),
        ("Build Tools", actual_buildtools),
    ):
        declared_raw = parse_readme_table_version(root, label)
        declared = _first_version(declared_raw)
        if declared is None or actual is None:
            notes.append(f"README/目录缺少 {label} 声明，跳过比对")
            continue
        if declared != actual:
            failures.append(
                f"README.md 声明 {label}={declared}，实际为 {actual} → 文档漂移，请同步 README.md"
            )

    # --- 5. versionCode 公式 -------------------------------------------------
    formula_err = check_versioncode_formula(root, version)
    if formula_err:
        failures.append(formula_err)

    # --- report --------------------------------------------------------------
    for n in notes:
        print(f"NOTE : {n}")
    for w in warnings:
        print(f"WARN : {w}")
    for f in failures:
        print(f"FAIL : {f}")

    if failures:
        print(f"\n版本一致性校验失败（{len(failures)} 项）")
        return 1
    print(f"\n版本一致性校验通过（告警 {len(warnings)} 项）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
