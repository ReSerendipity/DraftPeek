#!/usr/bin/env python3
# scripts/check_a11y.py  —— 无障碍（a11y）量化扫描器
#
# 用途（评估报告 P2⑩「contentDescription/48dp 触摸目标未量化」）：
#   对全部 Compose main 源码做静态扫描，量化两类 a11y 契约：
#   1. 图标/图片可感知性：Icon()/Image()/AsyncImage()/StrokeIcon() 调用点必须
#      显式携带 contentDescription 参数（值为 null 表示「装饰性、对读屏隐藏」，
#      同样是合规声明；完全缺失参数才算违规）。
#   2. 触控目标外扩：clickable 密集屏是否使用 minimumTouchTarget 兜底（仅报告
#      统计，不作违规——Brand 组件已在包装层内置 48dp 外扩）。
#
# 用法：
#   python scripts/check_a11y.py            # 报告模式，始终退出码 0
#   python scripts/check_a11y.py --strict   # 违规时退出码 1（供 CI 门禁，先观察一轮再启用）
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]

SCAN_DIRS = (
    ["app/src/main"]
    + [str(p.relative_to(ROOT)) for p in ROOT.glob("feature/*/src/main")]
    + [str(p.relative_to(ROOT)) for p in ROOT.glob("core/*/src/main")]
)

# 调用点检测正则（\b 排除 BrandIconButton( 这类前后缀粘连；排除定义行）
CALL_RE = re.compile(r"(?<![A-Za-z0-9_])(Icon|Image|AsyncImage|StrokeIcon)\(")
DEF_RE = re.compile(
    r"^\s*(internal\s+|public\s+|private\s+)?(data\s+|sealed\s+|value\s+)*"
    r"(fun|class)\s+(Icon|Image|AsyncImage|StrokeIcon)\b"
)
# 预览文件豁免：@Preview 合成器不需要真实语义
PREVIEW_HINT = "Preview"


def find_matching_close(text: str, open_idx: int) -> int:
    """从 open_idx 指向的 '(' 起做括号配对，跳过字符串字面量。返回匹配 ')' 下标。"""
    depth = 0
    i = open_idx
    in_str = in_char = in_line_comment = False
    while i < len(text):
        c = text[i]
        if in_line_comment:
            if c == "\n":
                in_line_comment = False
        elif in_str:
            if c == "\\":
                i += 1
            elif c == '"':
                in_str = False
        elif in_char:
            if c == "\\":
                i += 1
            elif c == "'":
                in_char = False
        else:
            if c == "/" and i + 1 < len(text) and text[i + 1] == "/":
                in_line_comment = True
            elif c == '"':
                in_str = True
            elif c == "'":
                in_char = True
            elif c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return len(text) - 1


def scan_file(path: pathlib.Path):
    """返回 (违规调用列表[(line_no, call_name)], 总调用数)。"""
    text = path.read_text(encoding="utf-8")
    if PREVIEW_HINT in path.name:
        return [], 0
    violations = []
    total = 0
    lines = text.splitlines()
    for m in CALL_RE.finditer(text):
        call = m.group(1)
        line_no = text.count("\n", 0, m.start()) + 1
        # 排除函数定义（如本项目自身封装的 StrokeIcon 定义处）
        if DEF_RE.match(lines[line_no - 1]):
            continue
        total += 1
        close = find_matching_close(text, m.end() - 1)
        args = text[m.end():close]
        # 多行参数里的变量转发（contentDescription = cd 等）都算显式声明
        if not re.search(r"contentDescription\s*=", args):
            violations.append((line_no, call))
    return violations, total


def main() -> int:
    strict = "--strict" in sys.argv
    all_violations = []
    grand_total = 0
    for rel in SCAN_DIRS:
        d = ROOT / rel
        if not d.exists():
            continue
        for kt in sorted(d.rglob("*.kt")):
            v, total = scan_file(kt)
            grand_total += total
            for line_no, call in v:
                all_violations.append(f"{kt.relative_to(ROOT).as_posix()}:{line_no}: {call}() 缺 contentDescription")

    covered = grand_total - len(all_violations)
    rate = (covered / grand_total * 100) if grand_total else 100.0
    print(f"a11y 图标/图片可感知性：共 {grand_total} 个调用点，"
          f"显式声明 {covered} 个，覆盖率 {rate:.1f}%")
    if all_violations:
        print(f"违规 {len(all_violations)} 处：")
        for v in all_violations:
            print(f"  {v}")
    else:
        print("OK: 无缺失 contentDescription 的图标/图片调用")

    # 第二维度（仅统计不门禁）：minimumTouchTarget 使用情况
    mt = 0
    for rel in SCAN_DIRS:
        d = ROOT / rel
        if d.exists():
            for kt in d.rglob("*.kt"):
                mt += len(re.findall(r"minimumTouchTarget\b", kt.read_text(encoding="utf-8")))
    print(f"minimumTouchTarget 引用次数：{mt}（Brand 包装层内置外扩，业务层无需重复声明）")

    if strict and all_violations:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
