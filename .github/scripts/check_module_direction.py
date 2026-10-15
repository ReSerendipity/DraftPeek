#!/usr/bin/env python3
"""Module dependency-direction guard.

对应报告：《架构技术债 Flash 报告》P1-3（依赖方向只守了一条边）。
与 check_domain_boundary.py（domain<-data 单边守卫）互补，覆盖全模块：
  R1 core/* 模块禁止依赖 :feature:* 与 :app
  R2 feature/* 模块禁止依赖 :app
  R3 任何模块禁止依赖 :app；allowlist = {:benchmark}（macrobenchmark 量测宿主，
     若未来真依赖 :app 须在此登记理由；当前实况为零依赖 :app）
  R4 feature/* 之间互依允许（README「依赖规则」明文允许）
  R5 core/* 之间互依允许
  R6 禁止自依赖
  R7 core/* 依赖子图内禁止环（DFS）

用法：python .github/scripts/check_module_direction.py   # 0=OK，1=违规
解析范围：根 settings.gradle.kts 的 include(...) 模块清单 + 各模块 build.gradle.kts
中的 project("...") 声明；build-logic（独立 includeBuild）与 settings 本身不参与。
纯标准库；由 structure-guard.yml 的 `layout` 必需检查执行，脚本缺失/失败即 job 失败。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SETTINGS = ROOT / "settings.gradle.kts"
ALLOW_APP_DEPENDENTS = {":benchmark"}

PROJ_RE = re.compile(r'project\(\s*["\']([^"\']+)["\']\s*\)')
INCLUDE_RE = re.compile(r'include\(\s*["\']([^"\']+)["\']\s*\)')


def collect_modules(settings_text: str) -> list[str]:
    return sorted(set(INCLUDE_RE.findall(settings_text)))


def collect_deps(build_text: str) -> list[str]:
    """Extract project(":...") coordinates from one build.gradle.kts body."""
    deps: list[str] = []
    for line in build_text.splitlines():
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*"):
            continue
        deps.extend(PROJ_RE.findall(line))
    return deps


def evaluate(modules: list[str], deps: dict[str, list[str]]) -> list[str]:
    """Pure rule evaluation -> violation strings (R1-R7)."""
    violations: list[str] = []
    module_set = set(modules)
    for m in sorted(deps):
        for d in deps[m]:
            if d == m:
                violations.append(f"R6 self-dependency: {m} -> {d}")
            if m.startswith(":core:") and (d.startswith(":feature:") or d == ":app"):
                violations.append(f"R1 core must not depend on feature/app: {m} -> {d}")
            if m.startswith(":feature:") and d == ":app":
                violations.append(f"R2 feature must not depend on app: {m} -> {d}")
            if d == ":app" and m != ":app" and m not in ALLOW_APP_DEPENDENTS:
                violations.append(f"R3 no module may depend on :app (allowlist={sorted(ALLOW_APP_DEPENDENTS)}): {m} -> {d}")
    core = [m for m in modules if m.startswith(":core:")]
    graph = {
        m: [d for d in deps.get(m, []) if d in module_set and d.startswith(":core:")]
        for m in core
    }
    state: dict[str, int] = {}

    def dfs(node: str, stack: list[str]) -> None:
        state[node] = 1
        for nxt in graph.get(node, []):
            if state.get(nxt) == 1:
                violations.append(f"R7 cycle in core graph: {' -> '.join(stack + [node, nxt])}")
            elif state.get(nxt, 0) == 0:
                dfs(nxt, stack + [node])
        state[node] = 2

    for m in core:
        if state.get(m, 0) == 0:
            dfs(m, [])
    return violations


def main() -> int:
    if not SETTINGS.is_file():
        print(f"FAIL: settings not found: {SETTINGS}")
        return 1
    modules = collect_modules(SETTINGS.read_text(encoding="utf-8"))
    deps: dict[str, list[str]] = {}
    for m in modules:
        build = ROOT / m.lstrip(":").replace(":", "/") / "build.gradle.kts"
        if not build.is_file():
            print(f"FAIL: module build file missing: {build}")
            return 1
        deps[m] = collect_deps(build.read_text(encoding="utf-8"))
    violations = evaluate(modules, deps)
    edges = sum(len(v) for v in deps.values())
    if violations:
        print(f"FAIL: module dependency direction violations ({len(modules)} modules, {edges} edges):")
        for v in violations:
            print(f"  {v}")
        return 1
    print(f"check_module_direction: OK - {len(modules)} modules, {edges} project() edges, rules R1-R7 all pass")
    return 0


if __name__ == "__main__":
    sys.exit(main())
