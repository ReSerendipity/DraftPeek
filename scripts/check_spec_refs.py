#!/usr/bin/env python3
"""Thin wrapper -> shared family auditor. Fail if NEW phantom refs appear.

The auditor lives OUTSIDE this repo (a sibling .spec_audit directory next to
the repository). On a developer machine it is found and the check is authoritative;
in a fresh CI checkout it is absent and the check degrades to "skip" — exit 0 so
CI stays green, but the skip must be reported as an unrun check, never as a pass.
"""
from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
AUDITORS = [
    HERE / ".spec_audit" / "audit_spec_refs.py",
    HERE.parent / ".spec_audit" / "audit_spec_refs.py",
]
auditor = next((p for p in AUDITORS if p.is_file()), None)
if auditor is None:
    # 退出码保持 0（CI 侧这是设计内的降级），但措辞不能谎报成「已通过」或「CI green」：
    # 本轮 0 个引用被校验过，必须让读日志的人和 CI 界面看见「没跑」这件事。
    msg = ("[spec-refs] SKIPPED — 未找到 family auditor（期望路径："
           f"{', '.join(str(p) for p in AUDITORS)}）；本轮 0 个规范引用被校验。"
           "这是设计内的降级（退出码 0），不是通过。")
    if os.environ.get("GITHUB_ACTIONS") == "true":
        print("::warning::" + msg)
    print(msg, file=sys.stderr)
    sys.exit(0)

with tempfile.TemporaryDirectory(prefix="spec_audit_") as td:
    out = Path(td) / "current.json"
    out_md = Path(td) / "current.md"
    subprocess.run([sys.executable, str(auditor), "--project", HERE.name,
                    "--json", str(out), "--md", str(out_md)], check=True)
    data = json.loads(out.read_text(encoding="utf-8"))[0]

hard = [f for f in data["findings"] if f["status"] == "PHANTOM" and f["tier"] == "ASSERTIVE"]
dl = data["dead_links"]
wf = data["workflows"]["missing"]
pc = data["precommit"]["declared_not_configured"]
print(f"phantom={len(hard)} dead_links={len(dl)} bad_workflow={len(wf)} bad_hook={len(pc)}")
for x in hard:
    print(f"  PHANTOM {x['ref']}  in {', '.join(x['specs'])}")
for d in dl:
    print(f"  DEAD    {d['spec']}:{d['line']} -> {d['link']}")
sys.exit(1 if (hard or dl or wf or pc) else 0)
