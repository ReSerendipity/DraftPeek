#!/usr/bin/env python3
# scripts/check_junit_platform.py  —— JUnit「平台 / 注解」一致性静态门禁
#
# 来源（2026-09-05 实测事故，GOTCHAS #36）：
#   core/common 的 ChaosEngineeringTest 用 JUnit4 的 org.junit.Test，而模块配的是
#   useJUnitPlatform() 且只挂 junit5-engine（无 vintage 引擎）。结果：该类
#   **能编译、但在 Gradle 与 CI 里从未被测试引擎发现**，12 个用例长期静默跳过，
#   无任何报错、无任何信号。启用后首次真跑即暴露 4 个潜伏缺陷（其中 2 个
#   必抛 ConcurrentModificationException / 断言与用例名相反）。
#
# 这类失效最危险的地方在于「假绿」：CI 全绿、覆盖率不变，但测试根本没跑。
# 本脚本用纯静态方式在提交前拦住它，无需执行测试。
#
# 判定规则（双向）：
#   1. 模块 useJUnitPlatform()=true 且无 junit-vintage-engine，但测试源码
#      import 了 JUnit4 注解 → JUnit4 用例会被静默跳过（违规）。
#   2. 模块未开 useJUnitPlatform()（即 JUnit4 runner），但测试源码 import 了
#      JUnit5 (org.junit.jupiter.api.*) → JUnit5 用例同样被静默跳过（违规）。
#
# 用法：
#   python scripts/check_junit_platform.py            # 报告模式，始终退出码 0
#   python scripts/check_junit_platform.py --strict    # 违规时退出码 1（供 precheck 门禁）
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

# 参与扫描的 Gradle 模块（app + core/* + feature/* + benchmark）
MODULE_GLOBS = (
    ["app"]
    + [str(p.relative_to(ROOT)) for p in ROOT.glob("core/*")]
    + [str(p.relative_to(ROOT)) for p in ROOT.glob("feature/*")]
    + ["benchmark"]
)

# JUnit4 注解/断言/Runner 的 import 特征
JUNIT4_IMPORT_RE = re.compile(
    r"^\s*import\s+org\.junit\.(Test|Ignore|Before|After|BeforeClass|AfterClass|"
    r"Assert|Assume|Rule|ClassRule|FixMethodOrder|rules\..*|runner\..*)\s*$",
    re.M,
)
# JUnit5 编程模型（jupiter）。junit-platform-* 不算，那是引擎/启动器。
JUNIT5_IMPORT_RE = re.compile(r"^\s*import\s+org\.junit\.jupiter\.api\..*\s*$", re.M)


def strip_kts_comments(text: str) -> str:
    """去掉 // 行注释与 /* */ 块注释，避免把注释里的说明文字误判为配置。

    core/ui 就是在注释里出现 useJUnitPlatform() 来说明『本模块刻意不开它』，
    不剥离注释会直接误报。
    """
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
        elif c in ('"', "'"):
            q = c
            out.append(c)
            i += 1
            while i < n:
                if text[i] == "\\":
                    out.append(text[i: i + 2])
                    i += 2
                    continue
                out.append(text[i])
                if text[i] == q:
                    i += 1
                    break
                i += 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


def module_test_sources(mod: pathlib.Path) -> list:
    roots = [mod / "src" / "test", mod / "src" / "testJava"]
    found = []
    for r in roots:
        if r.exists():
            found.extend(sorted(r.rglob("*.kt")))
    return found


def main() -> int:
    strict = "--strict" in sys.argv
    violations = []
    scanned_modules = 0
    scanned_files = 0
    j4_files = j5_files = 0

    for rel in MODULE_GLOBS:
        mod = ROOT / rel
        bf = mod / "build.gradle.kts"
        if not bf.exists():
            continue
        cfg = strip_kts_comments(bf.read_text(encoding="utf-8", errors="ignore"))
        platform5 = "useJUnitPlatform()" in cfg
        vintage = "junit-vintage-engine" in cfg
        sources = module_test_sources(mod)
        if not sources:
            continue
        scanned_modules += 1

        for kt in sources:
            txt = kt.read_text(encoding="utf-8", errors="ignore")
            has4 = bool(JUNIT4_IMPORT_RE.search(txt))
            has5 = bool(JUNIT5_IMPORT_RE.search(txt))
            scanned_files += 1
            if has4:
                j4_files += 1
            if has5:
                j5_files += 1
            relposix = kt.relative_to(ROOT).as_posix()

            if platform5 and has4 and not vintage:
                violations.append(
                    f"{relposix}: 使用 JUnit4 注解，但模块 {rel} 开了 useJUnitPlatform() "
                    f"且未挂 junit-vintage-engine → 这些用例会被静默跳过（能编译、不执行）"
                )
            if (not platform5) and has5:
                violations.append(
                    f"{relposix}: 使用 JUnit5 (jupiter) 注解，但模块 {rel} 未开 "
                    f"useJUnitPlatform()（JUnit4 runner）→ 这些用例会被静默跳过"
                )

    print(
        f"JUnit 平台/注解一致性：扫描 {scanned_modules} 个模块 / {scanned_files} 个单测文件"
        f"（JUnit4 风格 {j4_files} 个，JUnit5 风格 {j5_files} 个）"
    )
    if violations:
        print(f"违规 {len(violations)} 处（静默跳过风险）：")
        for v in violations:
            print(f"  {v}")
    else:
        print("OK: 未发现『测试平台与注解不匹配』导致的静默跳过")

    if strict and violations:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
