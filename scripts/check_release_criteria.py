#!/usr/bin/env python3
"""DraftPeek 灰度发布判据的机器可执行清单（评估 2026-11-05 P1-4 整改）。

背景：
    `docs/release-governance.md` §3.1 的灰度四阶段（Internal / Closed Beta /
    Open Beta / Production）判据此前只有 Markdown 表格，且 §3.2 自认崩溃观测
    「仅本地」——crash rate 尚无远端聚合数据源。本脚本把该表变成**可执行判据**：
    每一条判据都带 machine-readable 的阈值与「数据来源 / 数据源缺口」标注，
    输入一份实测指标 JSON 即可逐条判定 PASS / FAIL / SKIP（数据源缺口）。

用法：
    python scripts/check_release_criteria.py --list
        打印全部判据（含阈值、数据来源与缺口状态）。不输入数据，退出码恒为 0。
    python scripts/check_release_criteria.py --check --input metrics.json --stage closed_beta
        按 metrics.json 中的实测值判定 closed_beta 阶段的全部判据。
    python scripts/check_release_criteria.py --self-test
        内置用例自检（判定逻辑回归验证，不读外部文件）。

指标输入格式（metrics.json，与 --input 搭配）：
    {
      "version": "1.0.33",                 // 可选，仅用于展示
      "metrics": {                          // 只需提供本轮真有的指标
        "crash_rate_pct": 0.4,              // 崩溃率（百分数，如 0.4 表示 0.4%）
        "p0_bugs": 0,                       // 未修复 P0 bug 数
        "p1_bugs": 1,                       // 未修复 P1 bug 数
        "data_loss": false,                 // 是否发生用户数据丢失
        "feedback_positive": true,          // 用户反馈是否正面（人工判定）
        "key_feature_outage": false         // 关键功能是否不可用
      }
    }

退出码：
    0  本阶段全部判据 PASS（或 SKIP：判据有定义但本轮无数据源，已逐条列出）
    1  任一判据 FAIL（含回滚触发判据命中）
    2  用法/输入错误（stage 不存在、JSON 解析失败、必填缺失）
    --strict 下：SKIP 视为失败（返回 1）——供远端数据源落地后切换为硬门禁。

数据源缺口口径（与 release-governance.md §3.2 一致，2026-10-19 成文）：
    crash_rate 类判据当前**无远端聚合数据源**：客户端本地 CrashMetrics（会话/崩溃
    计数）+ 人工导出抽样近似；`CRASH_REPORT_URL` 默认空串即关闭，无 ANR/native
    采集，无上报接收端。其余人工判据（bug 数、数据丢失、反馈、关键功能）为人工
    台账口径。接入远端聚合前，本脚本的 crash rate 判定只能消费「人工导出」值，
    逐条 SKIP/标注如实呈现，不得据此宣称已具备远端灰度门禁。
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

# ── 数据源状态枚举 ────────────────────────────────────────────────────────
# missing_remote：判据需要远端聚合数据，当前无采集/无接收端（§3.2 缺口）
# local_only    ：数据只存在于设备本地（CrashMetrics / 本地日志），须人工导出
# manual        ：人工台账口径（issue / CHANGELOG / 测试报告 / 用户反馈）
SOURCE_MISSING_REMOTE = "missing_remote"
SOURCE_LOCAL_ONLY = "local_only"
SOURCE_MANUAL = "manual"

_SOURCE_LABEL = {
    SOURCE_MISSING_REMOTE: "无远端聚合数据源（缺口）",
    SOURCE_LOCAL_ONLY: "仅设备本地数据，需人工导出",
    SOURCE_MANUAL: "人工台账（issue/CHANGELOG/反馈记录）",
}

_CRASH_SOURCE_NOTE = (
    "crash rate 当前仅本地 CrashMetrics 统计 + 人工导出抽样（§3.2）；"
    "CRASH_REPORT_URL 默认关闭、无 ANR/native 采集、无远端聚合看板"
)

# ── 判据清单（逐字对应 release-governance.md §3.1 灰度四阶段表） ────────────
# 每条判据：id / stage / kind(pass|rollback) / metric / op / threshold / 说明
# 数值单位与 §3.1 表一致：crash rate 用百分数（0.1 表示 0.1%）。
CRITERIA: list[dict[str, Any]] = [
    # ── Internal（开发团队 5-10 人）──
    {
        "id": "internal.pass.no_p0",
        "stage": "internal", "kind": "pass",
        "metric": "p0_bugs", "op": "==", "threshold": 0,
        "desc": "无 P0 bug",
        "source": SOURCE_MANUAL, "source_note": "缺陷台账（issue/CHANGELOG）",
    },
    {
        "id": "internal.pass.no_p1",
        "stage": "internal", "kind": "pass",
        "metric": "p1_bugs", "op": "==", "threshold": 0,
        "desc": "无 P1 bug",
        "source": SOURCE_MANUAL, "source_note": "缺陷台账（issue/CHANGELOG）",
    },
    {
        "id": "internal.pass.crash_rate",
        "stage": "internal", "kind": "pass",
        "metric": "crash_rate_pct", "op": "<", "threshold": 0.1,
        "desc": "crash rate < 0.1%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "internal.rollback.p0",
        "stage": "internal", "kind": "rollback",
        "metric": "p0_bugs", "op": ">=", "threshold": 1,
        "desc": "回滚触发：任一 P0 bug",
        "source": SOURCE_MANUAL, "source_note": "缺陷台账（issue/CHANGELOG）",
    },
    # ── Closed Beta（封闭测试群 50-100 人）──
    {
        "id": "closed_beta.pass.crash_rate",
        "stage": "closed_beta", "kind": "pass",
        "metric": "crash_rate_pct", "op": "<", "threshold": 0.5,
        "desc": "crash rate < 0.5%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "closed_beta.pass.no_data_loss",
        "stage": "closed_beta", "kind": "pass",
        "metric": "data_loss", "op": "==", "threshold": False,
        "desc": "无数据丢失",
        "source": SOURCE_MANUAL, "source_note": "用户反馈 + 复现记录",
    },
    {
        "id": "closed_beta.rollback.crash_rate",
        "stage": "closed_beta", "kind": "rollback",
        "metric": "crash_rate_pct", "op": ">", "threshold": 1.0,
        "desc": "回滚触发：crash rate > 1%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "closed_beta.rollback.data_loss",
        "stage": "closed_beta", "kind": "rollback",
        "metric": "data_loss", "op": "==", "threshold": True,
        "desc": "回滚触发：数据丢失",
        "source": SOURCE_MANUAL, "source_note": "用户反馈 + 复现记录",
    },
    # ── Open Beta（公开测试 1000+ 人）──
    {
        "id": "open_beta.pass.crash_rate",
        "stage": "open_beta", "kind": "pass",
        "metric": "crash_rate_pct", "op": "<", "threshold": 0.3,
        "desc": "crash rate < 0.3%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "open_beta.pass.feedback",
        "stage": "open_beta", "kind": "pass",
        "metric": "feedback_positive", "op": "==", "threshold": True,
        "desc": "用户反馈正面",
        "source": SOURCE_MANUAL, "source_note": "反馈渠道人工归类",
    },
    {
        "id": "open_beta.rollback.crash_rate",
        "stage": "open_beta", "kind": "rollback",
        "metric": "crash_rate_pct", "op": ">", "threshold": 0.5,
        "desc": "回滚触发：crash rate > 0.5%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    # ── Production（全量发布）──
    {
        "id": "production.pass.crash_rate",
        "stage": "production", "kind": "pass",
        "metric": "crash_rate_pct", "op": "<", "threshold": 0.2,
        "desc": "crash rate < 0.2%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "production.rollback.crash_rate",
        "stage": "production", "kind": "rollback",
        "metric": "crash_rate_pct", "op": ">", "threshold": 0.5,
        "desc": "回滚触发：crash rate > 0.5%",
        "source": SOURCE_MISSING_REMOTE, "source_note": _CRASH_SOURCE_NOTE,
    },
    {
        "id": "production.rollback.key_feature_outage",
        "stage": "production", "kind": "rollback",
        "metric": "key_feature_outage", "op": "==", "threshold": True,
        "desc": "回滚触发：关键功能不可用",
        "source": SOURCE_MANUAL, "source_note": "工单/反馈人工判定",
    },
]

STAGES = ["internal", "closed_beta", "open_beta", "production"]


def _evaluate(op: str, actual: Any, threshold: Any) -> bool:
    """对单条判据做比较运算；类型不符按 False 处理（fail-closed）。"""
    try:
        if op == "<":
            return actual < threshold
        if op == ">":
            return actual > threshold
        if op == ">=":
            return actual >= threshold
        if op == "==":
            return actual == threshold
    except TypeError:
        return False
    raise ValueError(f"未知比较符：{op}")


def criteria_for(stage: str) -> list[dict[str, Any]]:
    return [c for c in CRITERIA if c["stage"] == stage]


def check_stage(stage: str, metrics: dict[str, Any], strict: bool) -> tuple[int, list[str]]:
    """判定一个阶段的全部判据。返回 (exit_code, 输出行)。"""
    lines: list[str] = []
    has_fail = False
    has_skip = False
    stage_criteria = criteria_for(stage)
    if not stage_criteria:
        return 2, [f"[ERROR] 未知阶段：{stage}（可选：{', '.join(STAGES)}）"]

    lines.append(f"=== 灰度判据判定：stage={stage}（判据 {len(stage_criteria)} 条） ===")
    for c in stage_criteria:
        cid = c["id"]
        if c["metric"] not in metrics:
            has_skip = True
            lines.append(
                f"  [SKIP] {cid}  {c['desc']}"
                f"\n         ↳ 数据源缺口：{_SOURCE_LABEL[c['source']]} — {c['source_note']}"
            )
            continue
        actual = metrics[c["metric"]]
        ok = _evaluate(c["op"], actual, c["threshold"])
        if c["kind"] == "rollback":
            # 回滚判据：命中（比较为真）即 FAIL
            status = "FAIL(触发回滚)" if ok else "PASS"
            has_fail = has_fail or ok
        else:
            status = "PASS" if ok else "FAIL"
            has_fail = has_fail or not ok
        lines.append(f"  [{status}] {cid}  {c['desc']}（实测 {actual!r} {c['op']} 阈值 {c['threshold']!r}）")

    if has_fail:
        lines.append(f"结论：FAIL —— stage={stage} 有未通过判据（回滚判据命中或通过标准未满足）")
        return 1, lines
    if has_skip:
        lines.append("结论：PASS with gaps —— 无失败判据，但存在数据源缺口（SKIP 逐条见上）")
        if strict:
            lines.append("--strict：SKIP 视为失败")
            return 1, lines
        return 0, lines
    lines.append(f"结论：PASS —— stage={stage} 全部判据通过")
    return 0, lines


def print_list() -> list[str]:
    lines = ["=== 灰度发布判据清单（来源：docs/release-governance.md §3.1，目标态） ==="]
    for stage in STAGES:
        lines.append(f"\n[{stage}]")
        for c in criteria_for(stage):
            kind = "回滚" if c["kind"] == "rollback" else "通过"
            lines.append(
                f"  {c['id']}  ({kind}) {c['desc']}"
                f"\n      数据来源：{_SOURCE_LABEL[c['source']]} — {c['source_note']}"
            )
    lines.append(
        "\n口径提示：crash rate 判据为目标态（无远端聚合数据源）；"
        "接入前按 §3.2 以本地抽样与人工导出近似，不得宣称已具备远端口径。"
    )
    return lines


def _self_test() -> tuple[int, list[str]]:
    """内置用例回归：验证判定逻辑（含回滚分支与 SKIP 分支）行为正确。"""
    lines = ["=== 自检：判定逻辑内置用例 ==="]
    cases: list[tuple[str, int, int]] = []  # (case 名, 期望 rc, 实际 rc)

    # 用例 1：internal 全指标缺失 → 全 SKIP，非 strict rc=0
    rc, out = check_stage("internal", {}, strict=False)
    cases.append(("internal_no_data_non_strict", 0, rc))
    lines += ["  " + l for l in out[-1:]]

    # 用例 2：--strict 下同输入 → rc=1
    rc, out = check_stage("internal", {}, strict=True)
    cases.append(("internal_no_data_strict", 1, rc))

    # 用例 3：internal 全绿 → rc=0，无 SKIP
    rc, out = check_stage(
        "internal", {"p0_bugs": 0, "p1_bugs": 0, "crash_rate_pct": 0.05}, strict=False
    )
    cases.append(("internal_all_green", 0, rc))
    lines += ["  " + l for l in out[-1:]]

    # 用例 4：internal crash rate 0.2% 超阈值 0.1% → rc=1
    rc, _ = check_stage(
        "internal", {"p0_bugs": 0, "p1_bugs": 0, "crash_rate_pct": 0.2}, strict=False
    )
    cases.append(("internal_crash_rate_over", 1, rc))

    # 用例 5：closed_beta 回滚判据命中（crash rate 1.1% > 1.0%）→ rc=1
    rc, _ = check_stage(
        "closed_beta", {"crash_rate_pct": 1.1, "data_loss": False}, strict=False
    )
    cases.append(("closed_beta_rollback_hit", 1, rc))

    # 用例 6：closed_beta 通过（crash 0.4% < 0.5%，无数据丢失）→ rc=0
    rc, out = check_stage(
        "closed_beta", {"crash_rate_pct": 0.4, "data_loss": False}, strict=False
    )
    cases.append(("closed_beta_green", 0, rc))
    lines += ["  " + l for l in out[-1:]]

    # 用例 7：production 数据丢失类型错误（str 比较布尔）→ fail-closed 判 False，
    #         回滚判据「命中」为假 → 不触发；pass 判据缺失 → SKIP → rc=0
    rc, out = check_stage(
        "production", {"crash_rate_pct": 0.15, "key_feature_outage": "unknown"}, strict=False
    )
    cases.append(("production_type_mismatch_fail_closed", 0, rc))
    lines += ["  " + l for l in out[-1:]]

    # 用例 8：未知阶段 → rc=2
    rc, _ = check_stage("nope", {}, strict=False)
    cases.append(("unknown_stage", 2, rc))

    failed = [(n, e, a) for (n, e, a) in cases if e != a]
    for n, e, a in cases:
        status = "PASS" if e == a else "FAIL"
        lines.append(f"  [{status}] {n}（期望 rc={e}，实际 rc={a}）")
    if failed:
        lines.append(f"结论：自检失败 {len(failed)} 例")
        return 1, lines
    lines.append(f"结论：自检通过（{len(cases)} 例）")
    return 0, lines


def main() -> int:
    parser = argparse.ArgumentParser(description="灰度发布判据可执行清单（§3.1）")
    parser.add_argument("--list", action="store_true", help="打印全部判据与数据源缺口标注")
    parser.add_argument("--check", action="store_true", help="按输入指标判定指定阶段")
    parser.add_argument("--input", type=Path, default=None, help="指标 JSON 文件（--check 必填）")
    parser.add_argument("--stage", choices=STAGES, default=None, help="要判定的阶段（--check 必填）")
    parser.add_argument(
        "--strict", action="store_true",
        help="数据源缺口（SKIP）视为失败——远端数据源落地后切硬门禁用",
    )
    parser.add_argument("--self-test", action="store_true", help="内置判定逻辑自检")
    args = parser.parse_args()

    if args.self_test:
        rc, lines = _self_test()
        print("\n".join(lines))
        return rc

    if args.list:
        print("\n".join(print_list()))
        return 0

    if args.check:
        if args.input is None or args.stage is None:
            parser.error("--check 需要 --input <metrics.json> 与 --stage <stage>")
        try:
            data = json.loads(args.input.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            print(f"[ERROR] 指标文件读取/解析失败：{exc}", file=sys.stderr)
            return 2
        metrics = data.get("metrics") if isinstance(data, dict) else None
        if not isinstance(metrics, dict):
            print('[ERROR] 指标文件缺少 "metrics" 对象', file=sys.stderr)
            return 2
        rc, lines = check_stage(args.stage, metrics, strict=args.strict)
        print("\n".join(lines))
        return rc

    parser.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())
