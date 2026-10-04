#!/usr/bin/env python3
"""本地化键对齐 + 字符串资源安全性的棘轮闸。

两件事：
  1) 安全性：撇号未转义、`%<n>` 缺 `$<转换符>`、XML 不可解析 —— 这些会在 aapt/运行期炸。
  2) 对齐度：每个 locale 相对默认 values/ 缺多少键。**棘轮**：只许变好不许变坏，
     基线写在 locale-baseline.json（沿用 coverage-baseline.json 的"单一事实来源"约定），
     因为 zh-rTW 的 feature/stats 目前 312 键只翻了你 1 个，
     直接硬卡会让 CI 从第一天就红成噪声 —— 那种闸等于没有闸。

刻意不把正则写成字面量里带反斜杠的形式经 shell 传递：
本机实测 shell→python 的二次求值会吃掉一层反斜杠，
`[^\\\\]` 变成 `[^\\]`（未闭合字符类），曾产出 41KB 假红。
需要反斜杠/撇号处一律用 chr() 构造。
"""
from __future__ import annotations

import json
import re
import sys
import xml.dom.minidom as minidom
from pathlib import Path

APOS = chr(39)
BS = chr(92)
STRING_TAG = re.compile(r'\s*<string\s+name="([^"]+)"([^>]*)>(.*)</string>\s*$')
BAD_FMT = re.compile(r'%\d+(?!\$)')
SKIP = {'build', '.trash', '.git', 'node_modules'}


def parse(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
        m = STRING_TAG.match(line)
        if m:
            out[m.group(1)] = m.group(3)
    return out


def unescaped_apostrophe(value: str) -> bool:
    """值里出现没有被反斜杠转义的撇号 => aapt 会报错。"""
    i = value.find(APOS)
    while i != -1:
        back = 0
        j = i - 1
        while j >= 0 and value[j] == BS:
            back += 1
            j -= 1
        if back % 2 == 0:
            return True
        i = value.find(APOS, i + 1)
    return False


def scan(root: Path) -> tuple[list[str], dict[str, int]]:
    errors: list[str] = []
    counts: dict[str, int] = {}
    for default in sorted(root.rglob('src/main/res/values/strings.xml')):
        if SKIP & set(default.parts):
            continue
        mod = default.relative_to(root).parts[0]
        try:
            dk = parse(default)
        except Exception as e:  # noqa: BLE001
            errors.append(f'{default}: 默认文件解析失败 {e}')
            continue
        for k, v in dk.items():
            if BAD_FMT.search(v):
                errors.append(f'{default} [{k}]: 占位符 {v!r} 的 %<n> 缺 $<转换符>')
        for loc in sorted(default.parent.parent.glob('values-*/strings.xml')):
            if loc.parent.name == 'values':
                continue
            try:
                minidom.parseString(loc.read_text(encoding='utf-8', errors='replace'))
            except Exception as e:  # noqa: BLE001
                errors.append(f'{loc}: XML 不可解析 {e}')
                continue
            lk = parse(loc)
            for k, v in lk.items():
                if unescaped_apostrophe(v):
                    errors.append(f'{loc} [{k}]: 撇号未转义 -> {v!r}')
                if BAD_FMT.search(v):
                    errors.append(f'{loc} [{k}]: 占位符 {v!r} 的 %<n> 缺 $<转换符>')
            missing = len(set(dk) - set(lk))
            # 键必须归一化成正斜杠：本机是 Windows，直接 str(relative) 会写出 `app\\src\\...`，
            # 而 Linux CI 上算出来的是 `app/src/...` —— 键对不上会让基线全部落空、闸门天天假红。
            counts[f'{loc.parent.name} :: {default.relative_to(root).as_posix()}'] = missing
    return errors, counts


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('.')
    baseline_path = root / 'locale-baseline.json'
    errors, counts = scan(root)
    if errors:
        print(f'FAIL: {len(errors)} 处字符串资源安全问题')
        for e in errors[:40]:
            print('  ', e)
        if len(errors) > 40:
            print(f'   ...（另有 {len(errors) - 40} 条，先修前面的：噪声会吃掉信号）')
        return 1
    baseline = {}
    if baseline_path.exists():
        baseline = json.loads(baseline_path.read_text(encoding='utf-8'))
    worse = {k: v for k, v in counts.items() if v > int(baseline.get(k, 0))}
    if not baseline_path.exists():
        print('BASELINE MISSING: 生成 locale-baseline.json 后再启用本闸')
        print(json.dumps(dict(sorted(counts.items())), ensure_ascii=False, indent=1))
        return 2
    if worse:
        print('FAIL: 以下 locale 的缺失键数比基线变差了（棘轮只许降不许升）')
        for k, v in sorted(worse.items()):
            print(f'   {k}: {baseline.get(k, 0)} -> {v}')
        return 1
    print(f'OK: {len(counts)} 个 locale 文件，安全项全通过，缺失键数未劣化')
    return 0


if __name__ == '__main__':
    sys.exit(main())
