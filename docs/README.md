# DraftPeek — 文档与项目速览

> Android 富文本/代码/笔记编辑器（KMP + Jetpack Compose）。支持多语言(i18n)、Markdown、终端、统计、知识图谱、文件同步。
> 入口：Gradle 多模块（应用外壳 `app/`）；启动见 `scripts/` 下的 bat 或 Android Studio。
> 详细目录放置规则见 `AGENTS.md` 末尾「文件归档与放置规范」。

## 快速了解本项目
- **做什么**：移动端富文本编辑器（Markdown WYSIWYG/源码双模式），内置文件管理、收藏/最近、统计看板、终端、知识图谱、端到端同步。
- **技术栈**：Kotlin · Jetpack Compose · KMP(Core/FE 多模块) · Room · Hilt · 自研 CRDT 同步。
- **如何构建**：Gradle（`settings.gradle.kts`）；桌面安装脚本见 `scripts/`。

## 目录结构速览
| 目录 | 内容 |
|---|---|
| `app/` | **应用主模块**（UI/入口/资源/安全） |
| `core/` | 跨模块共享库（common/ data/ domain/ designsystem/ testing/ ui/） |
| `feature/` | 功能模块（browser/ editor/ settings/ stats/ terminal/ knowledge/） |
| `benchmark/` | 基准测试（启动/文件读取） |
| `build-logic/` | Gradle 约定插件 |
| `gradle/` | Gradle wrapper / 版本目录(libs.versions.toml) |
| `migrations/` | 数据库迁移（知识图谱 SQL） |
| `server/` | Python 同步/CRDT 服务 |
| `tests/` `benchmark/` | 单元/E2E/基准测试 |
| `docs/` | 项目文档（见下方索引） |

## docs/ 索引（本目录）
| 子目录/文件 | 存什么 |
|---|---|
| `repo-analysis/` | 参考仓库学习报告（editor/termux/笔记协作，约35篇） |
| `reports/` | 测试摘要(TESTING_SUMMARY)等报告 |
| `_devarchive/` | 历史/一次性产物（icons/ logs/ trae-documents/ qoder/） |
| `FILEMAP.md` | 文件结构地图 |
| `开源合规说明.md` 等 | 合规（根目录） |

## 想找内容？
- 想改编辑器 → `feature/editor/`
- 想改统计看板 → `feature/stats/`
- 想改统一 UI/设计系统 → `core/designsystem/`
- 想改数据层/数据库 → `core/data/`、`app/src/main/java/.../core/data/db/`
- 想了解功能范围 → `docs/功能实现状态分析报告.md`

> ⚠️ 特别提醒：根目录 `release.jks` 是 **App 签名密钥（敏感，勿删勿外传勿提交）**；
> `index.html` 是被源码注释引用的 HTML 设计原型，保留。所有改动请遵循 `AGENTS.md` 的归档规则。