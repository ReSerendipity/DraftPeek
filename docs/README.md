# DraftPeek — 文档与项目速览

> Android 原生代码/文本编辑器（Jetpack Compose + Kotlin 多模块）。内置终端、Markdown 预览、多语言语法高亮；`server/` 为可选的实验性 Python 协作同步服务（非默认启用、非生产部署）。
> 入口：Gradle 多模块（应用壳 `app/`）；构建与脚本见根 `README.md`。版本与发布规范见 `docs/VERSIONING.md`。

## 快速了解本项目

- **做什么**：Android 原生代码/文本编辑器（40+ 语言高亮、Markdown 编辑/预览、集成终端、FTP/SFTP 文件浏览、Git 集成、实验性 LSP）。
- **技术栈**：Kotlin · Jetpack Compose · Room(SQLCipher) · Hilt · sora-editor(TextMate + Tree-sitter)；`server/` 为 Python CRDT 同步（实验性）。
- **如何构建**：`./gradlew assembleDebug`；发布流程见 `docs/VERSIONING.md` 与 `docs/RELEASE_CHECKLIST.md`。

## 目录结构速览

| 目录 | 内容 |
|---|---|
| `app/` | 应用壳模块（MainActivity、导航、安全模块、AppWidget） |
| `core/` | 基础层（common / data / domain / designsystem / testing / ui） |
| `feature/` | 功能模块（browser / editor / settings / stats / terminal） |
| `benchmark/` | Macrobenchmark 性能基准 |
| `build-logic/` | Gradle 约定插件 |
| `gradle/` | wrapper 与版本目录（libs.versions.toml） |
| `migrations/` | 数据库迁移脚本（SQL） |
| `server/` | 可选实验性 Python 协作同步服务（crdt / integrity / sync + pytest） |
| `docs/` | 项目文档（见下方索引） |
| `scripts/` | 构建与辅助脚本 |

## docs/ 索引（本目录）

| 文件 / 子目录 | 存什么 |
|---|---|
| `VERSIONING.md` | 版本号规范、versionCode 公式、发布/灰度/回滚、hotfix 与 SLA |
| `RELEASE_CHECKLIST.md` | 发布前人工检查清单（与 CI 门禁分工） |
| `DATABASE_MIGRATION_STRATEGY.md` | Room / SQLCipher 迁移策略 |
| `release-version-management-evaluation.md` | 发布版本管理评估（版本集中化改造依据） |
| `SECURITY_AUDIT_DraftPeek.md` | 安全审计 |
| `adr/` | 架构决策记录（含 CRDT vs OT 选型） |
| `deploy-download-page/` | 下载页部署说明 |
| `icon-design/` | 应用图标设计与导出 |
| `reports/` | 测试体系 / 前端工程评估报告 |
| `screenshots/` | 界面截图 |

## 想找内容？

- 改编辑器 → `feature/editor/`；统计看板 → `feature/stats/`；设计系统 → `core/designsystem/`；数据层 → `core/data/`
- 发版 → `docs/VERSIONING.md` + `docs/RELEASE_CHECKLIST.md`

> 特别提醒：根目录 `release.jks` 是 App 签名密钥（敏感，勿提交勿外传）；`index.html` 是被源码注释引用的 HTML 设计原型，保留。所有改动遵循根 `AGENTS.md`。