#!/usr/bin/env python3
"""机器比对 docs/CODING_STANDARDS.md 第 2 节 与 docs/ci/branch-protection.json。

为什么要有这个脚本：同一条漂移已两次靠人工回填收口（见 FIX_LOG.md 2026-10-14 与
2026-10-17，同一签名 sig:branch-protection-doc-live-drift 第二次出现）。按根
AGENTS.md「同一失败签名第二次出现禁止同方向再试」，把它变成可重复执行的检查。
期望值全部从 JSON 派生，不在本脚本里写死条数或名字。

判据（任一不满足即退出码 1）：
  1. 第 2 节必须点名单一来源文件 docs/ci/branch-protection.json；
  2. 第 2 节必须把必需检查的名单交给 JSON 的 `contexts` 键（出现 contexts 这个键名即视为已委托）；
  3. 第 2 节禁止复述计数——不得出现「N 项必需检查 / N 个必需检查」这类写法。本节曾因把
     「三项必需检查」写进文档、而脚下这份 JSON 只登记 2 项，当场制造了新的漂移，故用负面断言堵死；
  4. policy.enforce_admins 为 false 时，第 2 节必须显式写出 `enforce_admins=false`（承认豁免存在）；
     为 true 时反过来禁止仍写着 false（否则文档描述的保护强度低于实况）；
  5. 第 2 节必须引用实测标记字段名（_measured_at 或 _meets_reality），且该字段在 JSON 里真实存在——
     防止文档指向一个并不存在的「实测口径」。
本脚本只管「文档 vs 本 JSON」的形式一致性；「本 JSON vs 远端实况」由只读的
scripts/apply_branch_protection.py 负责，两者互补，别把后者的期望写死进文档。
退出码 2 保留给「文件/节缺失、JSON 解析失败」这类工具或数据故障——与「检查不过」
区分开，避免把读不到当成通过或当成违规。
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SECTION_HEADING = re.compile(r"^## 2\..*Git.*$", re.MULTILINE)
NEXT_HEADING = re.compile(r"^## ", re.MULTILINE)
# 「三项必需检查」「3 个必需检查」等复述写法：一律禁止（值属于 JSON，不属于散文）
RESTATED_COUNT = re.compile(r"[0-9〇一二三四五六七八九十]+\s*[项个]\s*(?:必需的?|登记的?)?\s*检查")


def _fail_hard(msg: str) -> "int":
    sys.stderr.write("[bp-doc] 故障：%s（检查中止，非通过）\n" % msg)
    return 2


def section_2(text: str) -> str | None:
    """取第 2 节正文：从本节的 ## 2. 标题起，到下一个 ## 标题前。"""
    m = SECTION_HEADING.search(text)
    if not m:
        return None
    rest = text[m.end():]
    nxt = NEXT_HEADING.search(rest)
    return rest if not nxt else rest[:nxt.start()]


def main() -> int:
    ap = argparse.ArgumentParser(description="比对 CODING_STANDARDS 第 2 节与 branch-protection.json")
    ap.add_argument("--doc", default=str(ROOT / "docs" / "CODING_STANDARDS.md"))
    ap.add_argument("--json", dest="json_path", default=str(ROOT / "docs" / "ci" / "branch-protection.json"))
    args = ap.parse_args()

    doc_path, json_path = Path(args.doc), Path(args.json_path)
    for p, kind in ((doc_path, "规范文档"), (json_path, "分支保护单一来源")):
        if not p.is_file():
            return _fail_hard("%s 不存在：%s" % (kind, p.name))
    try:
        doc_text = doc_path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        return _fail_hard("规范文档读不出：%s" % exc)
    try:
        policy_json = json.loads(json_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        return _fail_hard("分支保护 JSON 解析失败：%s" % exc)

    sec = section_2(doc_text)
    if sec is None:
        return _fail_hard("在 %s 里找不到「## 2. …Git…」小节，判据无法定位（不是通过）" % doc_path.name)

    contexts = policy_json.get("contexts")
    policy = policy_json.get("policy") or {}
    if not isinstance(contexts, list):
        return _fail_hard("JSON 缺少 contexts 列表，无法派生期望值")
    enforce_admins = policy.get("enforce_admins")

    sec_nospace = sec.replace(" ", "")
    problems: list[str] = []

    if "docs/ci/branch-protection.json" not in sec:
        problems.append("第 2 节没有点名单一来源 docs/ci/branch-protection.json")

    if "contexts" not in sec_nospace:
        problems.append("第 2 节没把必需检查名单交给 JSON 的 `contexts` 键（应委托，不要在本节列举）")

    hit = RESTATED_COUNT.search(sec)
    if hit:
        problems.append("第 2 节复述了必需检查的条数「%s」——条数属于 JSON 的 contexts（现登记 %d 项），"
                        "本节写死就会漂" % (hit.group(0).strip(), len(contexts)))

    if enforce_admins is False and "enforce_admins=false" not in sec_nospace:
        problems.append("policy.enforce_admins=false（owner/admin 可免 PR 直推），但第 2 节未写明该豁免")
    if enforce_admins is True and "enforce_admins=false" in sec_nospace:
        problems.append("policy.enforce_admins=true（直推也会被拦），但第 2 节仍写着 enforce_admins=false 豁免")

    cited = [k for k in ("_measured_at", "_meets_reality") if k in sec]
    if not cited:
        problems.append("第 2 节未引用 JSON 的实测标记字段（_measured_at / _meets_reality）")
    else:
        absent = [k for k in cited if k not in policy_json]
        if absent:
            problems.append("第 2 节引用了 %s，但该字段不在 JSON 里——实测口径无处可查" % ", ".join(absent))

    print("[bp-doc] 判据来源：%s（contexts=%d 项，enforce_admins=%s，实测标记=%s）"
          % (json_path.name, len(contexts), enforce_admins,
             ",".join(k for k in ("_measured_at", "_meets_reality") if k in policy_json) or "无"))
    if problems:
        print("[bp-doc] 第 2 节与分支保护单一来源不一致，%d 项：" % len(problems))
        for p in problems:
            print("  - " + p)
        print("修法：按 %s 改写 docs/CODING_STANDARDS.md 第 2 节（委托值、写明豁免、引用实测标记）；"
              "文档与本 JSON 之外的「本 JSON vs 远端实况」用只读命令 python scripts/apply_branch_protection.py（不加 --apply）判。"
              % json_path.name)
        return 1
    print("[bp-doc] 一致：第 2 节已点名单一来源、把名单委托给 contexts、未复述条数、豁免与实测标记均对得上")
    return 0


if __name__ == "__main__":
    sys.exit(main())
