#!/usr/bin/env python3
# scripts/check_coverage.py  —— 逐模块 JaCoCo 覆盖率汇总器
#
# 用途：汇总各模块 build/reports/jacoco/**/*.xml 的 LINE / BRANCH / INSTRUCTION
#       覆盖率，给出一张可读的清单。用于回答「逐模块覆盖率到底是多少」，
#       并作为覆盖率趋势的观测入口。
#
# 前置：先生成报告
#   ./gradlew jacocoTestReport :app:jacocoTestReport
#   （库模块的 jacocoTestReport 由根 build.gradle.kts 统一注册；
#     app 自带一份，且依赖全部 Test 任务，会解析所有变体 classpath）
#
# 用法：
#   python scripts/check_coverage.py                        # 打印汇总表（无报告只警告，退出 0）
#   python scripts/check_coverage.py --min-line 30          # 行覆盖 < 30% 的模块列出
#   python scripts/check_coverage.py --min-line=30 --strict # 未达标 / 缺报告则退出 1（门禁）
#
# 退出码：0 通过；1 门禁未达标或（--strict 下）缺报告；2 参数错误。
import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]

TYPES = ("LINE", "BRANCH", "INSTRUCTION")


def gradle_modules():
    """从 settings.gradle.kts 解析真实模块清单。

    不能改用 `glob('core/*')` 之类的目录扫描：历史上仓库里存在被 git 跟踪但**未纳入
    构建**的死目录（core/crdt、core/sync、feature/knowledge 脚手架均已于 2026-09-12 移除）。
    目录扫描会把它们当成「缺报告的模块」报出来，制造假阳性并掩盖真实缺口。
    """
    settings = ROOT / "settings.gradle.kts"
    if not settings.exists():
        return ["app"]
    text = settings.read_text(encoding="utf-8")
    mods = re.findall(r'include\(\s*"([^"]+)"', text)
    return [m.lstrip(":").replace(":", "/") for m in mods]


MODULE_DIRS = gradle_modules()


def pct(covered: int, missed: int) -> float:
    total = covered + missed
    return (covered / total * 100) if total else 100.0


def parse_report(xml_path: pathlib.Path) -> dict:
    """解析单个 jacoco XML，返回 report 级 counter。"""
    root = ET.parse(xml_path).getroot()
    out = {}
    # report 级 counter 是 <report> 的直接子元素
    for c in root.findall("counter"):
        t = c.get("type")
        out[t] = (int(c.get("covered", 0)), int(c.get("missed", 0)))
    return out


def has_test_sources(rel: str) -> bool:
    """该模块是否存在测试源集。

    没有单测源集的模块（如 core/designsystem、core/testing；benchmark 只有
    androidTest 跑宏基准、不产出 jacoco exec）本来就不会有覆盖率报告，若在
    --strict 下把它们算作「缺报告」，门禁必然恒红，反而会被人绕开。
    """
    return (ROOT / rel / "src" / "test").exists()


def find_reports(mod: pathlib.Path) -> list:
    base = mod / "build" / "reports" / "jacoco"
    if not base.exists():
        return []
    return sorted(base.rglob("*.xml"))


def main() -> int:
    ap = argparse.ArgumentParser(
        prog="check_coverage.py",
        description="汇总逐模块 JaCoCo 覆盖率（LINE / BRANCH / INSTRUCTION）",
    )
    ap.add_argument(
        "--min-line",
        type=float,
        default=None,
        metavar="PCT",
        help="行覆盖率下限；低于该值的模块会被列出（--strict 下退出码 1）",
    )
    ap.add_argument(
        "--strict",
        action="store_true",
        help="门禁模式：未达标或缺报告均以退出码 1 阻断",
    )
    args = ap.parse_args()
    min_line = args.min_line

    rows = []
    missing = []      # 有测试源却没产出报告 = 真实缺口
    no_tests = []     # 本就没有测试源 = 预期无报告
    for rel in MODULE_DIRS:
        mod = ROOT / rel
        reports = find_reports(mod)
        if not reports:
            (missing if has_test_sources(rel) else no_tests).append(rel)
            continue
        # 若同模块有多份报告，取 mtime 最新的一份
        latest = max(reports, key=lambda p: p.stat().st_mtime)
        counters = parse_report(latest)
        rows.append((rel, counters, latest.relative_to(ROOT).as_posix()))

    if not rows:
        print("未找到任何 JaCoCo 报告。请先运行：")
        print("  ./gradlew jacocoTestReport :app:jacocoTestReport")
        # 默认仅为观测（本地不一定生成过报告）；--strict 下才视为门禁失败。
        return 1 if args.strict else 0

    hdr = f"{'模块':<22}{'行覆盖':>10}{'分支覆盖':>12}{'指令覆盖':>12}   报告"
    print(hdr)
    print("-" * len(hdr))
    below = []
    for rel, c, rp in rows:
        cells = []
        for t in TYPES:
            cov, mis = c.get(t, (0, 0))
            cells.append(f"{pct(cov, mis):6.1f}%")
        line_pct = pct(*c.get("LINE", (0, 0)))
        if min_line is not None and line_pct < min_line:
            below.append((rel, line_pct))
        print(f"{rel:<22}{cells[0]:>10}{cells[1]:>12}{cells[2]:>12}   {rp}")

    if missing:
        print()
        print(f"[缺口] 有测试源但未产出报告（{len(missing)} 个模块 —— --strict 下判定失败）:")
        for m in missing:
            print(f"  {m}")

    if no_tests:
        print()
        print(f"无测试源集，预期无报告（{len(no_tests)} 个模块，不计缺口）:")
        for m in no_tests:
            print(f"  {m}")

    if min_line is not None:
        print()
        if below:
            print(f"行覆盖率低于 {min_line:.1f}% 的模块：")
            for m, p in below:
                print(f"  {m}  {p:.1f}%")
        else:
            print(f"全部模块行覆盖率均 >= {min_line:.1f}%")

    # 缺报告在 --strict 下也算未达标：否则「某模块被移出报告任务」这类回归会静默通过。
    if args.strict and (below or missing):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
