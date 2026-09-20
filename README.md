# DraftPeek（撰码轻览）

> Android 原生代码 / 文本编辑器：内置终端、Markdown 预览、多语言语法高亮。采用 Jetpack Compose + 整洁架构，安全加固、离线可用。

## 界面预览

截图请见 [`docs/screenshots/`](docs/screenshots/) 目录。

## 项目简介

DraftPeek 是一款功能强大的 Android 原生文本 / 代码编辑器，主要特性：

- **多语言语法高亮**：基于 sora-editor（TextMate + Tree-sitter），支持 40+ 编程语言
- **Markdown 编辑与预览**：CommonMark 原生解析 + WebView 渲染管线，支持 KaTeX / Mermaid / 代码高亮
- **集成终端**：基于 Proot 架构的终端模拟器（proot 二进制与 rootfs 未随包分发，详见 THIRD_PARTY_NOTICES.md）
- **文件浏览**：本地 / FTP / SFTP 文件系统抽象，支持目录监听、书签、全文搜索
- **安全加固**：Native C 反调试 + Play Integrity + APK/DEX 校验 + AES-256-GCM 加密
- **多标签页编辑**：同时打开多个文件，快速切换
- **LSP 支持**：实验性多语言 LSP 客户端（Python/Kotlin/JS/Java/Go/Dart）
- **Git 集成**：GitHub 仓库导入与 Git 操作
- **可访问性**：色盲模式、高对比度主题、折叠屏 / 分屏自适应布局
- **国际化**：中文（默认）、English、日本語、한국어

## 环境要求

| 依赖 | 版本 |
|---|---|
| Android Studio | Meerkat 2024.3.2+（或任意支持 AGP 8.10.1 的版本） |
| JDK | 17 |
| Android SDK | compileSdk 36, targetSdk 36, minSdk 26 (Android 8.0+) |
| Gradle | 8.14.3（gradle-wrapper 管理，无需手动安装） |
| AGP | 8.10.1 |
| Kotlin | 2.2.21 |
| Build Tools | 36.1.0 |

> 版本一致性由 `scripts/check_version_consistency.py` 在 CI 中强制校验：上表 AGP / Gradle / Kotlin / Build Tools 必须与 `gradle/libs.versions.toml` 及 `gradle/wrapper/gradle-wrapper.properties` 实际取值一致，否则 CI 失败。本地自查：`python scripts/check_version_consistency.py`

**支持的 ABI**：`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`

## 编译步骤

### 打开项目

1. Android Studio → `File` → `Open` → 选择 `DraftPeek` 根目录
2. 等待 Gradle Sync 完成（首次同步需下载依赖，约 5–10 分钟）

### 构建 Debug / Release APK

```bash
./gradlew assembleDebug          # Linux/macOS        gradlew.bat assembleDebug          # Windows
./gradlew assembleRelease        # Release（需签名配置）   scripts\build-release.bat          # 或辅助脚本
```

- Debug APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK 输出：`app/build/outputs/apk/release/app-release.apk`
- 日常开发优先使用 `assembleRelease`，确保 R8 优化、ProGuard 混淆和安全模块均正常工作

### 签名配置

签名凭据通过 `local.properties`（git-ignored）配置，不硬编码在构建脚本中：

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
RELEASE_STORE_FILE=release.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

详见 [`local.properties.example`](local.properties.example)。

## 项目结构

```
DraftPeek/
├── app/                    # 壳模块：MainActivity、导航、安全模块、AppWidget
├── benchmark/              # 性能基准测试（Macrobenchmark）
├── build-logic/            # Gradle 约定插件（统一各模块构建配置）
├── core/                   # 基础层（整洁架构）
│   ├── common/             #   工具类、DiffEngine、GitManager、EventBus、插件系统
│   ├── data/               #   Room + SQLCipher + DataStore、DAO、Repository
│   ├── designsystem/       #   设计 Token（字体、色彩、形状、间距）
│   ├── domain/             #   UseCase + 领域模型
│   ├── testing/            #   测试 Fixtures
│   └── ui/                 #   品牌组件、主题、自适应布局
├── feature/                # 功能层（browser / editor / settings / stats / terminal）
├── docs/  scripts/  gradle/  migrations/
├── server/                 # Python 协作同步服务（实验性/可选）
├── AGENTS.md               # AI 辅助开发指南（仅本地保留、不入库；见下方「分发边界」）
└── CHANGELOG.md            # 更新日志
```

**依赖规则**：`app` → `feature/*` → `core/*`；Feature 之间允许跨模块依赖。

## 测试与覆盖率

```bash
./gradlew testDebugUnitTest                       # 全部单元测试
./gradlew :core:common:testDebugUnitTest          # 指定模块
./gradlew connectedAndroidTest                    # UI/集成测试（需设备/模拟器）
./gradlew jacocoTestReport                        # 覆盖率报告（HTML: build/reports/jacoco/html/index.html）
scripts\coverage.bat                              # Windows 一键覆盖率
```

**覆盖率阈值**（详见 [`codecov.yml`](codecov.yml)）：

| 模块 | 覆盖率要求 |
|---|---|
| 项目整体 | 60% |
| core-common | 70% |
| core-data | 75% |
| feature-editor / feature-browser | 60% |
| app | 50% |

## 技术栈

| 领域 | 技术 |
|---|---|
| UI 框架 | Jetpack Compose (BOM 2025.04.01) + Material Design 3 |
| 代码编辑器 | sora-editor（TextMate + Tree-sitter 语法高亮） |
| Markdown | CommonMark 0.24.0 (GFM 扩展) + WebView 预览（marked.js + KaTeX + Mermaid + highlight.js）+ richeditor-compose（WYSIWYG） |
| Office 文档 | Apache POI 5.3.0（Word/Excel/PPT → HTML → WebView） |
| PDF | Android PdfRenderer + android-pdf-viewer |
| Diff | java-diff-utils 4.12 |
| DI | Hilt 2.58 + KSP |
| 数据层 | Room 2.7.1 (SQLCipher 4.6.0 加密) + DataStore |
| 安全 | Native C 反调试 + Play Integrity + APK/DEX 校验 + AES-256-GCM |
| 远程文件系统 | FTP (commons-net) + SFTP (jsch) |
| 插件系统 | PF4J 3.11.0 |
| LSP | Eclipse LSP4J 0.22.0 |

## CI/CD

- [`.github/workflows/android.yml`](.github/workflows/android.yml)：主 CI（Lint → Debug Build → 单元测试含覆盖率 → 仪器化测试矩阵 → Release 构建验证）
- [`.github/workflows/release.yml`](.github/workflows/release.yml)：推送 `v*` tag 时自动构建 Release APK 并创建 GitHub Release

## 安全说明

- 所有安全校验仅在 Release 构建中启用（`!BuildConfig.DEBUG`）
- 签名凭据通过 `local.properties` 管理，绝不硬编码；`release.jks` 已通过 `.gitignore` 排除
- 数据库使用 SQLCipher 加密；敏感文件支持 AES-256-GCM 密码加密

## 相关文档

- `docs/VERSIONING.md` — 版本管理与发布规范
- `docs/RELEASE_CHECKLIST.md` — 发布前检查清单
- [CHANGELOG.md](CHANGELOG.md) — 更新日志

### 分发边界（AI 辅助开发文档仅本地保留）

根 `AGENTS.md`（AI 辅助开发指南）及其家族——`feature/editor/AGENTS.md`、`core/common/AGENTS.md`、`docs/agents/**`（GOTCHAS.md 等）、`FIX_LOG.md`、`LOCAL_RULES.md`——为 **仅本地保留、有意不入库** 的维护者上下文（经 `.gitignore` 的 `*.md` 通配规则忽略），**不会出现在 fresh clone 中**。新环境下的 agent 请退回以下**已入库的可分发入口**：本 `README.md`、[`docs/README.md`](docs/README.md)（目录结构 + docs 索引）、[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)。缺少 `AGENTS.md` 属预期行为，非错误。

## 开源模式说明

本仓库为**公开仓库**，以 Apache License 2.0 发布（Copyright 2026 ReSerendipity）。原「main 私有开发 / public 公开演示」双仓双分支体系已废止：私有仓已改名为本仓库并全量公开（含完整 main 历史）。

- `main` 为唯一主分支，push 即发布；禁止 force push
- 敏感文件永不入库：`release.jks` / `keystore/` / `local.properties` / `.env`（.gitignore 已覆盖，全历史已扫描核验）
- `server/` 为可选的实验性协作同步服务（非默认启用、非生产部署）
- 安全漏洞请通过 [SECURITY.md](.github/SECURITY.md) 的私密披露渠道报告，勿直接提公开 issue

## 贡献

参与贡献请遵循 [组织级贡献指南](https://github.com/ReSerendipity/.github/blob/main/CONTRIBUTING.md)（Conventional Commits + DCO 签名）。

## 许可证

本项目以 **Apache License 2.0** 开源发布（Copyright 2026 ReSerendipity）。详见 [LICENSE](LICENSE) · [NOTICE](NOTICE) · [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
