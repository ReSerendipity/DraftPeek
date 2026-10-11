"""T1-g: 依赖倒置边界守卫（domain ←→ data）。

强制 core:domain 不得依赖 core:data。

背景（85 分评估报告 T1 / 第二轮落地指令 T1-g）：
  - 依赖倒置前：core:domain 直接 implementation(project(":core:data"))，
    domain 的 usecase 直接 import data 接口/实体，9 组完全同名的 usecase
    分居两处，架构边界名存实亡。
  - 落地后：domain 定义 7 个 repository 接口 + 6 个纯 Kotlin model + 9 个
    usecase；core:data 反向依赖 core:domain 并实现接口、做 entity↔model 映射。

本守卫读取 core/domain/build.gradle.kts，扫描其中对 :core:data 的 project
依赖声明（implementation/api/compileOnly 等均拦），任何匹配立即以非零退出。

用法：python .github/scripts/check_domain_boundary.py
  0 = OK；1 = 边界被破坏。
"""

import re
import sys

REPO_ROOT = __file__.rsplit("/.github", 1)[0] if "/.github" in __file__ else "."
DOMAIN_BUILD = REPO_ROOT + "/core/domain/build.gradle.kts"

# 匹配任意 Gradle 依赖配置声明对 :core:data 的项目依赖：
#   implementation(project(":core:data"))
#   api(project(":core:data"))
#   compileOnly(project(":core:data"))
#   implementation(project(":core:data")) 等（允许单双引号、空白差异）
PATTERNS = [
    re.compile(r'\b(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|ksp|kapt)\s*\(\s*project\s*\(\s*["\']:core:data["\']\s*\)\s*\)'),
    re.compile(r'\bproject\s*\(\s*["\']:core:data["\']\s*\)'),
]

IGNORE_LINE_PREFIX = ("//", "/*", "*", "#")


def main():
    try:
        with open(DOMAIN_BUILD, encoding="utf-8") as f:
            lines = f.readlines()
    except OSError as e:
        print(f"FAIL: 无法读取 {DOMAIN_BUILD}: {e}")
        return 1

    violations = []
    for idx, raw in enumerate(lines, start=1):
        line = raw.rstrip("\n")
        stripped = line.strip()
        if not stripped or stripped.startswith(IGNORE_LINE_PREFIX):
            continue
        if any(p.search(stripped) for p in PATTERNS):
            violations.append(f"  {DOMAIN_BUILD}:{idx}: {stripped}")

    if violations:
        print("FAIL: 依赖倒置边界被破坏 —— core:domain 不得依赖 core:data：")
        print("\n".join(violations))
        print("接口/model 已上移至 core:domain，实现类在 core:data 且反向依赖 domain。")
        return 1

    print("check_domain_boundary: OK —— core:domain 不依赖 core:data")
    return 0


if __name__ == "__main__":
    sys.exit(main())
