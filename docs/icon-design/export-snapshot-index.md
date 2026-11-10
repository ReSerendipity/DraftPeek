# DraftPeek 应用图标索引（仅保留当前 G34O 版本）

> **迁移说明（2026-11-05）**：本索引原存于仓库外 `C:\Users\Doro\APP\repository\_app-icons\DraftPeek\`，现已迁入仓库。
> 迁移时经 `git hash-object` 复验，13 个图标文件与仓库内正式位置字节一致（构建资源在 `app/src/main/res/`，商店 PNG 在 `docs/icon-design/icon-export-g34o/`），仓库外的冗余副本已删除。下表「导出文件名」一列仅为历史快照命名记录，磁盘上已无对应文件。

## 基线（只读）

- 仓库：`C:\Users\Doro\APP\repository\DraftPeek`
- 当前分支：`main`（跟踪 `origin/main`）
- HEAD：`e18f4d0bba8fb69d9cf522d520b39a19daae9c74`
- `git status`：仅存在未跟踪目录 `docs/screenshots/`；未 checkout、未切换分支、未改动仓库跟踪文件
- 导出内容经 `git hash-object` 与历史 blob 逐一比对，保留的 13 个文件全部字节一致

## 保留范围与清理说明

本目录按用户要求只保留**当前生效的 G34O「偏心窥圆」这一套图标**，共 **13 个文件**：

- 构建资源：6 个 XML（adaptive icon 定义 ×2、前景 ×1、单色层 ×1、背景色 ×2）
- 商店/设计导出：7 张 G34O PNG（48/72/96/144/192/512/圆角 512）

历史版本已按用户要求清理，本索引不再保留旧条目。已删除的 14 个历史版本导出文件：

- 2026-08-15 初版（9560934）：5 个（1 张 PNG + 4 个 XML）
- 2026-09-09 G34R「红弦」构建资源（aa433ce）：2 个 XML
- 2026-09-13 G34R「红弦」商店 PNG（c6cb379）：7 张

历史版本条目已从本索引移除，不再逐条保留。

## 当前生效链路

[AndroidManifest.xml](file:///C:/Users/Doro/APP/repository/DraftPeek/app/src/main/AndroidManifest.xml#L36-L37)：

- `android:icon="@mipmap/ic_launcher"`
- `android:roundIcon="@mipmap/ic_launcher_round"`

资源解析链：

1. `mipmap-anydpi/ic_launcher.xml`、`mipmap-anydpi/ic_launcher_round.xml`（adaptive icon 定义，均含 `background` / `foreground` / `monochrome` 三层）
2. 前景 → `drawable/ic_launcher_foreground.xml`（G34O「偏心窥圆」矢量）
3. 单色层 → `drawable/ic_launcher_monochrome.xml`（G34O 矢量）
4. 背景 → `@color/ic_launcher_background`，`values` 与 `values-night` 当前均为 `#F5F7FA`

当前构建中没有任何 PNG/WEBP 启动图标；`mipmap-anydpi` 下也没有 `-v26` 变体。G34O 商店 PNG 仅用于商店/设计展示，不参与 APK 构建。

## 清单

| # | 提交短哈希 | 提交日期 | 提交说明 | 原始仓库路径 | 导出文件名 | 角色 |
|---|---|---|---|---|---|---|
| 1 | `aa433ce` | 2026-09-09 | chore(branding): 应用图标矢量化刷新（adaptive icon 前景改 vector drawable，新增 monochrome 主题图标层） | `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | `2026-09-09_aa433ce_ic_launcher.xml` | 当前生效（构建资源） |
| 2 | `aa433ce` | 2026-09-09 | chore(branding): 应用图标矢量化刷新（adaptive icon 前景改 vector drawable，新增 monochrome 主题图标层） | `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | `2026-09-09_aa433ce_ic_launcher_round.xml` | 当前生效（构建资源） |
| 3 | `aa433ce` | 2026-09-09 | chore(branding): 应用图标矢量化刷新（adaptive icon 前景改 vector drawable，新增 monochrome 主题图标层） | `app/src/main/res/values/ic_launcher_background.xml` | `2026-09-09_aa433ce_values_ic_launcher_background.xml` | 当前生效（构建资源，`#F5F7FA`） |
| 4 | `aa433ce` | 2026-09-09 | chore(branding): 应用图标矢量化刷新（adaptive icon 前景改 vector drawable，新增 monochrome 主题图标层） | `app/src/main/res/values-night/ic_launcher_background.xml` | `2026-09-09_aa433ce_values-night_ic_launcher_background.xml` | 当前生效（构建资源，`#F5F7FA`） |
| 5 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `app/src/main/res/drawable/ic_launcher_foreground.xml` | `2026-09-13_c6cb379_ic_launcher_foreground.xml` | 当前生效（构建资源，G34O） |
| 6 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `app/src/main/res/drawable/ic_launcher_monochrome.xml` | `2026-09-13_c6cb379_ic_launcher_monochrome.xml` | 当前生效（构建资源，G34O） |
| 7 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher_48.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher_48.png` | 当前商店导出（非构建资源） |
| 8 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher_72.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher_72.png` | 当前商店导出（非构建资源） |
| 9 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher_96.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher_96.png` | 当前商店导出（非构建资源） |
| 10 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher_144.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher_144.png` | 当前商店导出（非构建资源） |
| 11 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher_192.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher_192.png` | 当前商店导出（非构建资源） |
| 12 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher-playstore.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher-playstore.png` | 当前商店导出（非构建资源，512×512） |
| 13 | `c6cb379` | 2026-09-13 | feat: app icon redesign and design documentation | `docs/icon-design/icon-export-g34o/store-png/ic_launcher-rounded-512.png` | `2026-09-13_c6cb379_icon-export-g34o_ic_launcher-rounded-512.png` | 当前商店导出（非构建资源，512×512 圆角） |

## 数量

- 保留：**13 个文件**（6 个 XML + 7 张 PNG）
- 已清理：**14 个历史导出文件**（初版 5、G34R 构建资源 2、G34R 商店 PNG 7）
- 当前构建链：6 个 XML 全部生效；7 张 PNG 为商店/设计资产，不进入构建


