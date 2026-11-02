# DraftPeek 图标历史归档

> **HISTORICAL / DO NOT APPLY**
>
> 本目录只保存历史设计版本，**不参与构建**，也**不要复制进 `app/src/main/res/`**。
> 当前唯一生效的启动图标是 **G34O「偏心窥圆」**，资源位于 `app/src/main/res/` 与 `docs/icon-design/icon-export-g34o/`。
> 本目录中的 G34R、初版位图、旧 adaptive XML、旧背景色与 G34R 商店 PNG 均已被 G34O 取代。

## 归档清单

| 归档路径 | 内容 | 来源 / 日期 | 关键哈希（git blob SHA-1 前 8 位） |
|---|---|---|---|
| `g34r/icon-export/` | G34R「红弦」完整导出包：`README.md`、`geometry.json`、`res/`（前景、单色层、adaptive XML、背景色）、`store-png/`（7 张） | 工程生效期 2026-09-09（`aa433ce`）→ 2026-09-13（`c6cb379`）被 G34O 替换；导出包快照提交 `c6cb379`（2026-09-13） | 前景 `8dce0eb1`；单色层 `0e12a731`；adaptive `10bc7a5d`；背景色 `235e2fad`；README `99a93e20`；geometry `77a35648` |
| `g34r/backup-previous-g34r/` | G34R 被替换前的工程备份：`ic_launcher_foreground-g34r.xml`、`ic_launcher_monochrome-g34r.xml` | 提交 `c6cb379`（2026-09-13） | 前景 `8dce0eb1`；单色层 `0e12a731` |
| `g34r/scripts/` | G34R 专用生成脚本：`gen_export.js`（G34R XML 生成）、`gen_pngs.py`（G34R PNG 渲染） | 提交 `c6cb379`（2026-09-13） | `gen_export.js` `dab9c882`；`gen_pngs.py` `b1d8857d` |
| `initial-2026-08-15/backup-originals/` | 初版图标备份：`drawable-nodpi__ic_launcher_foreground.png`（512×512、不透明白底）、初版 adaptive XML、初版日/夜背景色 | 提交 `9560934`（2026-08-15）；2026-09-09（`aa433ce`）矢量化时移出构建 | PNG `53797306`；adaptive `a8a8fa55`（launcher / round 同哈希）；浅色背景 `a069564c`；夜间背景 `98c2f44d` |

## G34R 商店 PNG 明细

| 文件 | 尺寸 | git blob SHA-1 前 8 位 |
|---|---|---|
| `g34r/icon-export/store-png/ic_launcher_48.png` | 48×48 | `98dd2569` |
| `g34r/icon-export/store-png/ic_launcher_72.png` | 72×72 | `f2ec4741` |
| `g34r/icon-export/store-png/ic_launcher_96.png` | 96×96 | `60ccdcbd` |
| `g34r/icon-export/store-png/ic_launcher_144.png` | 144×144 | `109eda38` |
| `g34r/icon-export/store-png/ic_launcher_192.png` | 192×192 | `e1dc75a3` |
| `g34r/icon-export/store-png/ic_launcher-playstore.png` | 512×512 | `98086475` |
| `g34r/icon-export/store-png/ic_launcher-rounded-512.png` | 512×512 圆角 | `aa5c0083` |

## 使用说明

- 所有归档文件均通过 `git mv` 保留历史；未删除文件。`g34r/icon-export/README.md` 仅增加了历史警示与路径修正。
- `g34r/icon-export/README.md` 与 `g34r/scripts/` 仅作为当时设计过程记录；其中的替换步骤和脚本内硬编码的 `.qwenworkcn` 输出路径均已废弃，不要执行，也不保证在新环境可运行。
- 旧背景色为浅色 `#0E0E14` / 夜间 `#1B2838`；当前 G34O 背景色为 `#F5F7FA`，日夜一致。
- 旧 adaptive XML 不包含 `<monochrome>`；当前 G34O 两个 adaptive XML 均包含前景、单色层与背景。
- 如需恢复或重建图标，请使用 `docs/icon-design/icon-export-g34o/` 与 `docs/icon-design/scripts/` 中的 G34O 流程，不要使用本归档目录。
