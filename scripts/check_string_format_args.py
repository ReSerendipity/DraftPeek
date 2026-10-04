#!/usr/bin/env python3
"""静态回归闸：Android 字符串资源的格式化占位符必须合法。

起因（2026-10-04 真机审计）：feature/stats/.../values/strings.xml 的
`profile_theme_import_count` 写成 `已导入 %1 个`，缺少转换符。调用点
`ProfileScreen.kt` 用 stringResource(id, args) 传参，触发
java.util.UnknownFormatConversionException: Conversion = ' '，
一进「Me / 个人资料」主标签就 FATAL 崩溃 —— 两个底部主标签之一完全不可用。

规则：`%<数字>` 之后必须紧跟 `$`（位置参数写法），否则非法。
  合法：`%1$d`、`%2$s`、`%1$02d`（flag/width 在 $ 之后）、`%%`、`%s`、`%10$`
  非法：`%1 `、`%1个`、`%1<`、`%2` 结尾

为什么不靠编译期：aapt 对缺转换符只给 lint 警告，不阻断构建；
而这条路径只要资源被带参读取就是必崩，属于该在 CI 上就拦住的类别。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

BAD = re.compile(r'%\d+(?!\$)')
STRING_TAG = re.compile(r'\s*<string\s+name=(["\'])([^"\']+)\1(.*)')
SKIP_PARTS = {'build', '.trash', '.git', 'node_modules'}


def check(root: Path) -> list[tuple[str, int, str, str]]:
    hits: list[tuple[str, int, str, str]] = []
    for path in sorted(root.rglob('src/main/res/values*/strings.xml')):
        if SKIP_PARTS & set(path.parts):
            continue
        # 用 errors=replace 而不是 strict：坏字节要报出来，但不能让编码问题掩盖真缺陷
        text = path.read_text(encoding='utf-8', errors='replace')
        for lineno, line in enumerate(text.splitlines(), 1):
            m = STRING_TAG.match(line)
            if not m:
                continue
            if 'formatted="false"' in line:      # 显式声明不参与格式化 => 合法豁免
                continue
            bad = BAD.search(line)
            if bad:
                hits.append((path.relative_to(root).as_posix(), lineno, m.group(2), line.strip()))
    return hits


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('.')
    if not root.is_dir():
        print(f'FAIL: 目录不存在 {root}', file=sys.stderr)
        return 2
    hits = check(root)
    if not hits:
        print('OK: 未发现缺转换符的格式化占位符')
        return 0
    print(f'FAIL: {len(hits)} 处字符串资源的 %<n> 缺少 $<转换符>（带参读取会抛 UnknownFormatConversionException）')
    for f, ln, name, raw in hits:
        print(f'  {f}:{ln}  {name}')
        print(f'      {raw[:120]}')
    print('修法：`%1` -> `%1$d`（或按实参类型选 %s/%f）；确实不需要格式化则加 formatted="false"。')
    return 1


if __name__ == '__main__':
    sys.exit(main())
