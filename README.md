# DraftPeek (撰码轻览)

> Android 原生代码/文本编辑器，内置终端、Markdown 预览、多语言语法高亮，采用 Jetpack Compose + 整洁架构。

## 界面预览

> 界面截图存放于本地 `docs/screenshots/` 目录（本地保留、未随公开仓库发布）。

## 项目简介

DraftPeek 是一款功能强大的 Android 原生文本/代码编辑器，主要特性包括：

- **多语言语法高亮**：基于 sora-editor (TextMate + Tree-sitter)，支持 40+ 编程语言
- **Markdown 编辑与预览**：CommonMark 原生解析 + WebView 渲染管线，支持 KaTeX/Mermaid/代码高亮
- **集成终端**：基于 Proot 的终端模拟器，支持命令执行与会话管理
- **文件浏览**：本地/FTP/SFTP 文件系统抽象，支持目录监听、书签、全文搜索
- **安全加固**：Native C 反调试 + Play Integrity + APK/DEX 校验 + AES-256-GCM 加密
- **多标签页编辑**：支持同时打开多个文件，快速切换
- **LSP 支持**：实验性多语言 LSP 客户端 (Python/Kotlin/JS/Java/Go/Dart)
- **Git 集成**：GitHub 仓库导入与 Git 操作
- **可访问性**：色盲模式、高对比度主题、折叠屏/分屏自适应布局
- **国际化**：支持中文（默认）、English、日本語、한국어

## 环境要求

| 依赖 | 版本 |
|------|------|
| Android Studio | Ladybug 2024.2.1+ (或任意支持 AGP 8.8.0 的版本) |
| JDK | 17 |
| Android SDK | compileSdk 36, targetSdk 36, minSdk 26 (Android 8.0+) |
| Gradle | 8.13 (由 gradle-wrapper 管理，无需手动安装) |
| AGP | 8.8.0 |
| Kotlin | 2.2.21 |
| Build Tools | 36.1.0 |

**支持的 ABI**：`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`

## 编译步骤

### 打开项目

1. 打开 Android Studio → `File` → `Open` → 选择 `DraftPeek` 根目录
2. 等待 Gradle Sync 完成（首次同步需要下载依赖，可能需要 5-10 分钟）

### 构建 Debug APK

```bash
# 命令行
./gradlew assembleDebug          # Linux/macOS
gradlew.bat assembleDebug         # Windows

# 或使用辅助脚本（Windows）
scripts\build-debug.bat
```

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 构建 Release APK

Release 构建需要签名配置。请先准备签名凭据：

1. 复制 `local.properties.example` 为 `local.properties`
2. 填入 SDK 路径和签名配置（keystore 路径、密码等）
3. 执行构建：

```bash
./gradlew assembleRelease        # Linux/macOS
gradlew.bat assembleRelease       # Windows

# 或使用辅助脚本（Windows）
scripts\build-release.bat
```

Release APK 输出路径：`app/build/outputs/apk/release/app-release.apk`

> **注意**：日常开发优先使用 `assembleRelease`，以确保 R8 优化、ProGuard 混淆和安全模块均正常工作。

### 签名配置

签名凭据通过 `local.properties`（git-ignored）配置，不硬编码在构建脚本中：

```properties
# local.properties
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
│   └── ui/                 #   品牌组件（BrandButton 等）、主题、自适应布局
├── feature/                # 功能层（按业务领域拆分）
│   ├── browser/            #   文件浏览器（VFS 抽象：local/FTP/SFTP）
│   ├── editor/             #   编辑器核心（sora-editor、Markdown、LSP、多标签页）
│   ├── settings/           #   设置数据层
│   ├── stats/              #   统计 UI、成就系统
│   └── terminal/           #   终端模拟器（Proot 会话/命令执行）
├── docs/                   # 文档目录
├── scripts/                # 辅助脚本（构建、测试、安装）
├── gradle/                 # Gradle 版本目录（libs.versions.toml）
├── repos/                  # 竞品/参考仓库源码（调研用，不参与编译）
├── AGENTS.md               # AI 辅助开发指南
├── FILEMAP.md              # 完整文件清单
└── CHANGELOG.md            # 更新日志
```

## 模块依赖关系

```
app/ ──→ feature/* ──→ core/*
                    │
                    ├── feature/stats    depends on feature/settings + core/data
                    ├── feature/browser  depends on core/data + core/common + feature/settings
                    ├── feature/editor   depends on core/common + core/data + feature/settings
                    ├── feature/settings depends on core/common + core/ui
                    └── feature/terminal depends on core/common + core/ui
```

**依赖规则**：`app` → `feature/*` → `core/*`。Feature 之间允许跨模块依赖。

## 技术栈

| 领域 | 技术 |
|------|------|
| UI 框架 | Jetpack Compose (BOM 2025.04.01) + Material Design 3 |
| 代码编辑器 | sora-editor (TextMate + Tree-sitter 语法高亮) |
| Markdown 解析 | CommonMark 0.24.0 (GFM 扩展) |
| Markdown 预览 | WebView + marked.js + KaTeX + Mermaid + highlight.js |
| Markdown 编辑 | richeditor-compose (WYSIWYG) |
| Office 文档 | Apache POI 5.3.0 (Word/Excel/PPT → HTML → WebView) |
| PDF | Android PdfRenderer + android-pdf-viewer |
| Diff | java-diff-utils 4.12 |
| DI | Hilt 2.58 + KSP |
| 数据层 | Room 2.7.1 (SQLCipher 4.6.0 加密) + DataStore |
| 安全 | Native C 反调试 + Play Integrity + APK/DEX 校验 + AES-256-GCM |
| 远程文件系统 | FTP (commons-net) + SFTP (jsch) |
| 插件系统 | PF4J 3.11.0 |
| LSP | Eclipse LSP4J 0.22.0 |

## 运行测试

### 单元测试

```bash
# 所有模块
./gradlew test

# 单个模块
./gradlew :feature:editor:test
./gradlew :core:common:test

# 或使用辅助脚本（Windows）
scripts\run-unit-tests.bat
```

测试报告：`<module>/build/reports/tests/testDebugUnitTest/index.html`

### 仪器化测试（需要连接设备/模拟器）

```bash
./gradlew connectedDebugAndroidTest
```

### 基准测试

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

### 代码格式化检查

```bash
./gradlew spotlessCheck
```

### Lint 检查

```bash
./gradlew lint
```

## 辅助脚本

| 脚本 | 功能 |
|------|------|
| `scripts/build-debug.bat` | 构建 Debug APK |
| `scripts/build-release.bat` | 构建 Release APK（需配置签名） |
| `scripts/run-unit-tests.bat` | 运行单元测试 |
| `scripts/install-on-device.bat` | 编译并安装到连接的设备 |
| `scripts/clean-project.bat` | 清理构建缓存 |
| `scripts/package-release.bat` | 打包发布（含校验和、版本号重命名） |

## CI/CD

项目已配置 GitHub Actions CI：

- [`.github/workflows/android.yml`](.github/workflows/android.yml) — 主 CI 流程：Lint → Debug Build → 单元测试（含覆盖率）→ 仪器化测试（多 API 级别矩阵）→ Release 构建验证
- [`.github/workflows/release.yml`](.github/workflows/release.yml) — 发布流程：推送 `v*` tag 时自动构建 Release APK 并创建 GitHub Release

## 安全说明

- 所有安全校验仅在 Release 构建中启用（`!BuildConfig.DEBUG`）
- 签名凭据通过 `local.properties` 管理，绝不硬编码
- `release.jks` 签名文件已通过 `.gitignore` 排除
- 数据库使用 SQLCipher 加密
- 敏感文件支持 AES-256-GCM 密码加密

## 许可证

闭源项目（私有）。

## 相关文档

- `AGENTS.md`（本地文档，未随仓库发布）— AI 辅助开发指南与快速参考
- [FILEMAP.md](FILEMAP.md) — 完整文件清单
- [CHANGELOG.md](CHANGELOG.md) — 更新日志
- `docs/ARCHITECTURE.md`（本地文档，未随仓库发布）— 架构设计文档
- `docs/CODE_QUALITY_REPORT.md`（本地文档，未随仓库发布）— 代码质量报告
- `docs/LARGE_FILE_SUPPORT.md`（本地文档，未随仓库发布）— 大文件支持方案
