# DraftPeek 图标设计档案

本目录是应用图标优化的**完整历史记录**,请勿删除。

## 内容

| 路径 | 说明 |
|---|---|
| `DraftPeek图标评审板-v11.6.html` | **历史评审板**：文件名为 v11.6，但内部标题/页脚实际为 v10/v9，最终裁定仍是早期 G4R，不是当前 G34O 定稿。仅作设计过程归档，浏览器可查看历史候选。 |
| `archive/README.md` | **历史图标归档索引（HISTORICAL / DO NOT APPLY）**：G34R 导出包、G34R 备份、初版备份、G34R 商店 PNG 与 G34R 脚本的路径、哈希、日期。 |
| `icon-export-g34o/` | **当前 G34O 定稿导出包**：前景/单色层 vector XML、adaptive icon 定义、背景色、G34O 商店 PNG（48/72/96/144/192/512/圆角 512）。 |
| `archive/g34r/icon-export/` | 历史 G34R·红弦导出包（含 `store-png/` 与 README）；不参与构建，不要复制进 `app/src/main/res/`。 |
| `archive/initial-2026-08-15/backup-originals/` | 2026-08-15 初版 PNG/adaptive XML/背景色备份。 |
| `scripts/` | 生成与验证脚本：`gen8.js`（评审板生成器，含全部记号几何定义）、`patch*.js`（各轮单变量修改记录）、`audit*.js`（66dp 安全区审计器）、`apply_export.js`（仅 G34O 应用，使用当前仓库相对路径）、`export_g34o.js`/`gen_pngs_g34o.py`（G34O 导出）。G34R 专用 `gen_export.js`/`gen_pngs.py` 已归档至 `archive/g34r/scripts/`。 |

## 定稿状态（2026-11-02 复核）

- **工程内生效（定稿）**：**G34O 偏心窥圆**——九弦裁入 r27 圆、近眼三弦雾墨色阶（#33475F）、强调弦电光蓝（#3178C6，红弦版经对比落选）、朱砂红眼（#C41E3A）悬浮偏心孔心；背景 #F5F7FA 日夜一致。资源位于 `icon-export-g34o/`，已应用至 `app/src/main/res/`；6 个资源哈希与 `icon-export-g34o/res` 一致。
- **历史定稿**：G34R·红弦（前一版，已应用后被 G34O 替换），其资源与工程备份已归档至 `archive/g34r/icon-export/` 与 `archive/g34r/backup-previous-g34r/`。
- 复现定稿：`node scripts/export_g34o.js && python scripts/gen_pngs_g34o.py`。

## 历史资产归档

G34R 导出包、G34R 备份、G34R 商店 PNG、G34R 专用脚本与 2026-08-15 初版备份已移入 `archive/`，详见 `archive/README.md`。归档内容不参与构建，禁止复制进 `app/src/main/res/`。

## 夜间配色例外（2026-11-02 确认）

| 主题 | Primary | Secondary | 说明 |
|---|---|---|---|
| 浅色 | `#C41E3A`（朱砂红） | `#3178C6`（电光蓝） | 与 G34O 图标一致；应用强调色 |
| 深色 | `#E8A838`（暖金） | `#5A9BD5`（浅蓝） | 夜间专属配色；应用强调色、启动动画 accent |

- 代码事实：`core/designsystem/src/main/java/com/draftpeek/core/designsystem/theme/Color.kt` 的 `PrimaryLight`/`SecondaryLight`/`PrimaryDark`/`SecondaryDark`；`app/src/main/res/values-night/colors.xml` 与 `values-night` 主题同步。
- 这是**有意设计，不是漏改**：夜间在深色底上使用暖金强调，形成暖冷对比并保持可读性；朱砂红/电光蓝继续用于浅色主题和 G34O 图标本体。
- 适用场景：Material 强调色、`PrototypeTokens.accent`、启动动画 `SplashAnimation` 的强调色。
- 不适用于启动图标静态资源：G34O 图标仍使用 `#C41E3A`/`#3178C6`/`#33475F`/`#1C2B3A`，背景 `#F5F7FA`。

## 复现

```bash
node scripts/gen8.js        # 重新生成评审板(输出路径需按环境调整)
node scripts/audit18.js     # 66dp 安全区审计
```
