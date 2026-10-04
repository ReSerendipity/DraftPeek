#!/usr/bin/env python3
"""DraftPeek 覆盖率质量门禁校验脚本（ratchet 策略）。

用法：
    python scripts/check_coverage_gate.py            # 检查所有模块当前覆盖率
    python scripts/check_coverage_gate.py --ratchet  # 额外执行 ratchet 检查
                                                     #   （对比 main 分支的 coverage-baseline.json）

退出码：
    0  所有模块通过阈值 + ratchet 未违反
    1  任一模块低于阈值 / 报告缺失 / 解析失败 / ratchet 违反

设计原则（对应 AGENTS.md「覆盖率门禁 = 质量门禁」）：
    1. 阈值单一来源：`coverage-baseline.json.modules[*].threshold_pct`。
    2. 精度到小数点后 2 位：避开整数除法对低覆盖率模块（如 core/ui 0.92%）
       的截断失真。
    3. Ratchet：只允许 threshold_pct 相对 main 上升；若下降则本次检查失败。
    4. 报告缺失即失败：不允许「无数据即绿」的历史陷阱重演（GOTCHAS #37）。
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
BASELINE = ROOT / "coverage-baseline.json"
COUNTER_RE = re.compile(
    r'<counter\s+type="INSTRUCTION"\s+missed="(?P<missed>\d+)"\s+covered="(?P<covered>\d+)"\s*/>'
)


def parse_coverage(xml_path: Path) -> tuple[int, int]:
    """从 JaCoCo XML 报告提取 (covered, missed) 指令数。取 report-level 最后一个 counter。"""
    text = xml_path.read_text(encoding="utf-8", errors="replace")
    matches = list(COUNTER_RE.finditer(text))
    if not matches:
        raise ValueError(f"未找到 INSTRUCTION counter：{xml_path}")
    last = matches[-1]
    return int(last.group("covered")), int(last.group("missed"))


def load_baseline(path: Path) -> dict[str, Any]:
    if not path.exists():
        print(f"[FAIL] 基线文件不存在：{path}", file=sys.stderr)
        sys.exit(2)
    return json.loads(path.read_text(encoding="utf-8"))


def check_modules(baseline: dict[str, Any]) -> list[str]:
    """逐模块校验：报告存在 + 解析成功 + 覆盖率 ≥ threshold_pct。返回失败原因列表。"""
    failures: list[str] = []
    warnings: list[str] = []
    for name, cfg in baseline["modules"].items():
        report = ROOT / cfg["report_path"]
        threshold = float(cfg["threshold_pct"])
        if not report.exists():
            failures.append(
                f"[{name}] JaCoCo 报告缺失：{cfg['report_path']}（门禁拒绝「无数据即绿」）"
            )
            continue
        try:
            covered, missed = parse_coverage(report)
        except Exception as exc:  # noqa: BLE001
            failures.append(f"[{name}] 报告解析失败：{exc}")
            continue
        total = covered + missed
        if total == 0:
            failures.append(f"[{name}] 报告无指令数据（total=0）")
            continue
        pct = covered * 100.0 / total
        status = "PASS" if pct >= threshold else "FAIL"
        print(f"  [{status}] {name:<20} 实测 {pct:6.2f}%  阈值 {threshold:5.2f}%  (cov={covered}/{total})")
        if pct < threshold:
            failures.append(
                f"[{name}] 覆盖率 {pct:.2f}% 低于阈值 {threshold:.2f}%"
            )
        # 债务累积软警告：实测显著高于基线中记录的 measured_pct
        # 且与 threshold 拉开 ≥ 3pt → 提示应同步上调基线。
        measured = float(cfg.get("measured_pct", 0))
        if pct - threshold >= 3.0 and pct - measured >= 1.0:
            warnings.append(
                f"[{name}] 实测 {pct:.2f}% 已高于基线 measured_pct {measured:.2f}% ≥1pt，"
                f"且距阈值 {threshold:.2f}% ≥ 3pt；建议上调 threshold_pct 到新实测的 90%（约 {pct * 0.9:.2f}%）"
            )
    for w in warnings:
        print(f"  [WARN] {w}", file=sys.stderr)
    return failures


def check_ratchet(baseline: dict[str, Any]) -> list[str]:
    """Ratchet：本 PR 的 threshold_pct 相对 main 分支同文件任何下降 → 失败。

    若无法访问 main（浅克隆 / 分离 HEAD / 首次提交），跳过并给出明确警告，
    不视为失败——避免因 CI 环境瞬时问题阻塞合法变更。
    """
    try:
        raw = subprocess.check_output(
            ["git", "show", "origin/main:coverage-baseline.json"],
            cwd=ROOT,
            stderr=subprocess.STDOUT,
        )
    except subprocess.CalledProcessError as exc:
        print(
            f"  [SKIP] 无法读取 origin/main:coverage-baseline.json："
            f"{exc.output.decode(errors='replace').strip() if exc.output else exc}",
            file=sys.stderr,
        )
        return []
    prev = json.loads(raw.decode("utf-8"))
    failures: list[str] = []
    prev_modules = prev.get("modules", {})
    for name, cfg in baseline["modules"].items():
        new_thr = float(cfg["threshold_pct"])
        old_cfg = prev_modules.get(name)
        if old_cfg is None:
            # 新增模块：允许，但必须 ≥ 已测出的当前覆盖率 90%
            continue
        old_thr = float(old_cfg["threshold_pct"])
        if new_thr < old_thr:
            failures.append(
                f"[{name}] ratchet 违反：threshold_pct 从 {old_thr:.2f}% "
                f"下降到 {new_thr:.2f}%（只允许上升；如需下降请走 ADR）"
            )
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--ratchet",
        action="store_true",
        help="额外检查 threshold_pct 相对 origin/main 未下降",
    )
    args = parser.parse_args()

    baseline = load_baseline(BASELINE)

    print("=== DraftPeek 覆盖率质量门禁 ===")
    print(f"基线：{BASELINE.name}  ·  指标：{baseline.get('metric', 'jacoco.instruction.covered_ratio')}")
    failures = check_modules(baseline)

    if args.ratchet:
        print("\n=== Ratchet 检查（threshold_pct 只升不降） ===")
        failures.extend(check_ratchet(baseline))

    if failures:
        print("\n=== 门禁失败 ===", file=sys.stderr)
        for f in failures:
            print(f"  {f}", file=sys.stderr)
        return 1

    print("\n=== 门禁通过 ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
