#!/usr/bin/env python3
# scripts/check_strings_keys.py  —— 跨语言 strings.xml 键集合一致性校验
# 用途：在 MissingTranslation Lint 被 disable 的前提下，提供脚本化兜底，
#       防止 zh/en/ja/ko(及繁中) 翻译键集合漂移。
# 用法：python scripts/check_strings_keys.py
import sys, re, pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
BASE = "values-zh"
LANGS = ["values-en", "values-ja", "values-ko", "values-zh-rTW"]


def keys(locale: str) -> set:
    f = RES / locale / "strings.xml"
    if not f.exists():
        return set()
    return set(re.findall(r'name="([^"]+)"', f.read_text(encoding="utf-8")))


def main() -> int:
    base = keys(BASE)
    if not base:
        print(f"BASE locale {BASE} missing!");
        return 1
    total = 0
    for lang in LANGS:
        lk = keys(lang)
        missing = sorted(base - lk)
        extra = sorted(lk - base)
        if missing or extra:
            total += len(missing)
            if missing:
                print(f"[{lang}] 缺失 {len(missing)}: {missing}")
            if extra:
                print(f"[{lang}] 多余 {len(extra)}: {extra}")
    if total:
        print(f"FAIL: 共 {total} 个键缺失")
        return 1
    print(f"OK: 基准 {len(base)} 键，{', '.join(LANGS)} 全部一致")
    return 0


if __name__ == "__main__":
    sys.exit(main())
