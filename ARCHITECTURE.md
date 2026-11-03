# 架构 — DraftPeek

> 治理定位：`delivery=release`、`lifecycle=maintenance`（`governance/desired-state.json`）。
> 本文按 2026-11-03 的实际目录结构生成；数据流与边界须由维护者在实现变更时同步复核。

## 1. 定位

Android 原生代码 / 文本编辑器：内置终端、Markdown 预览、40+ 语言语法高亮。Jetpack Compose + 整洁架构，安全加固、离线可用。

## 2. 组件与依赖方向

```text
应用层：core/ (328)、app/ (191)
服务层：server/ (15)
工具层：scripts/ (27)、build-logic/ (10)
测试层：benchmark/ (11)、tests/ (1)
支撑层：docs/ (143)、.github/ (32)
其他层：feature/ (326)、.githooks/ (10)、gradle/ (4)、migrations/ (1)
根级层：根级文件 (26)
```

| 组件 | 目录 | 文件数 | 代表文件 |
|---|---|---|---|
| `core` | `core/` | 328 | `core/common/build.gradle.kts`, `core/common/gradle.lockfile`, `core/common/src/main/AndroidManifest.xml` |
| `feature` | `feature/` | 326 | `feature/browser/build.gradle.kts`, `feature/browser/gradle.lockfile`, `feature/browser/src/androidTest/java/com/draftpeek/feature/browser/ui/CreateFileDialogFlowTest.kt` |
| `app` | `app/` | 191 | `app/build.gradle.kts`, `app/gradle.lockfile`, `app/lint-baseline.xml` |
| `docs` | `docs/` | 143 | `docs/CODING_STANDARDS.md`, `docs/DATABASE_MIGRATION_STRATEGY.md`, `docs/HANDOVER_GUIDE.md` |
| `scripts` | `scripts/` | 27 | `scripts/apply_branch_protection.py`, `scripts/build-debug.bat`, `scripts/build-release.bat` |
| 根级文件 | `/` | 26 | `.editorconfig`, `.env.example`, `.gitattributes` |
| `server` | `server/` | 15 | `server/_logging_setup.py`, `server/crdt_server.py`, `server/integrity_server.py` |
| `benchmark` | `benchmark/` | 11 | `benchmark/build.gradle.kts`, `benchmark/check_performance_gate.py`, `benchmark/ci/preseed-onboarding-gate.sh` |
| `.githooks` | `.githooks/` | 10 | `.githooks/README.md`, `.githooks/commit-msg`, `.githooks/install.sh` |
| `build-logic` | `build-logic/` | 10 | `build-logic/convention/build.gradle.kts`, `build-logic/convention/src/main/kotlin/com/draftpeek/buildlogic/AndroidApplicationConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/com/draftpeek/buildlogic/AndroidComposeConventionPlugin.kt` |
| `gradle` | `gradle/` | 4 | `gradle/gradle-daemon-jvm.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.jar` |
| `migrations` | `migrations/` | 1 | `migrations/001_create_knowledge_graph.sql` |
| `tests` | `tests/` | 1 | `tests/test_version_consistency.py` |

依赖方向：入口层（应用 / 页面）调用领域层（业务模块），领域层依赖基础设施层（脚本、配置、模型资产）；反向引用不允许。

## 3. 数据流

Android 客户端打开文件 → 编辑内核处理缓冲区 → 终端 / 预览模块渲染 → 本地持久化。

## 4. 外部依赖

| 依赖 | 用途 | 不可用时 |
|---|---|---|
| Gradle / Android SDK | 构建 APK | 无法出包 |
| server/ 的 Python 依赖 | 本地服务与同步 | 客户端功能降级 |

## 5. 边界与非目标

- 发布链只走 §10：远端优先、tag 驱动、资产附 `SHA256SUMS.txt`。
- 分发产物由 CI 生成，不入库。
- 不入库：密钥 / token / 口令 / 连接串、本机绝对路径、`local/` 与 `AGENTS.md`、构建产物。
- 治理正文只在 `ReSerendipity/.github/governance`，本仓不复制规则正文。

## 6. 变更影响面

| 改什么 | 必须同步 | 由谁校验 |
|---|---|---|
| 目录结构 / 入口 | 本文「组件与依赖方向」与「数据流」 | 维护者 |
| CI 与门禁 | 本仓 `README.md` 的测试命令；治理仓 §20 远端载体口径 | `audit-drift.py` |
| 依赖清单 | 锁文件与 `.github/dependabot.yml` 的生态段 | Dependabot / `lock-consistency` |
| README 展示图 | §8.1 真实性红线（图必须来自真实运行） | 治理仓 `readme-image-provenance` |
