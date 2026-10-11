#!/usr/bin/env python3
"""Structure-guard: enforce a per-file line-count ceiling on UI source files.

T2 (P1): 巨型屏幕文件拆分后接入的行数上限门禁。

扫描 feature/*/src/main/java/com/draftpeek/feature/*/ui/ 与
feature/*/src/main/java/com/draftpeek/feature/*/sora/ 下的 .kt 源文件，
单文件行数超过 LIMIT（默认 2000）即 FAIL。

已知超限豁免（显式声明，不得静默新增）：
  （无 —— FileBrowserScreen.kt 已于 2026-10-11 任务A 拆分至 1714 行，豁免移除；
    拆分产物 VirtualFolderContent.kt / InternalFileListContent.kt /
    FileBrowserActionDialogs.kt 均已 < 1500 行。）

退出码：0 = OK，1 = hard failure（CI 直接失败，不静默放行）。
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))          # .../.github/scripts
REPO_ROOT = os.path.dirname(os.path.dirname(HERE))         # 仓库根
LIMIT = 2000

# 豁免清单：相对仓库根路径。添加必须写明原因（更新上方 docstring）。
EXEMPT: set[str] = set()


def main():
    failures = []
    scanned = 0
    for dirpath, dirnames, filenames in os.walk(REPO_ROOT):
        # 跳过构建产物与 .git
        dirnames[:] = [d for d in dirnames if d not in (".git", "build", ".gradle", ".trash")]
        if not dirpath.endswith(("\\ui", "/ui", "\\sora", "/sora")):
            continue
        if "src\\main\\java" not in dirpath and "src/main/java" not in dirpath:
            continue
        for fn in filenames:
            if not fn.endswith(".kt"):
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, REPO_ROOT).replace("\\", "/")
            if rel in EXEMPT:
                continue
            with open(full, "rb") as f:
                n = sum(1 for _ in f)
            scanned += 1
            if n > LIMIT:
                failures.append((rel, n))
    for rel, n in failures:
        print(f"FAIL: {rel}: {n} lines > {LIMIT}")
    print(f"scanned {scanned} files under ui/sora dirs, limit={LIMIT}")
    if failures:
        print("line-count ceiling violated; see T2 ledger for exempt rationale")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
