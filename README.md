# DraftPeek (撰码轻览)

> Android 原生代码/文本编辑器，内置终端、Markdown 预览、多语言语法高亮，采用 Jetpack Compose + 整洁架构。

## 界面预览

> 截图请见 [`docs/screenshots/`](docs/screenshots/) 目录。

## 项目简介

DraftPeek 是一款功能强大的 Android 原生文本/代码编辑器，主要特性包括：

- **多语言语法高亮**：基于 sora-editor (TextMate + Tree-sitter)，支持 40+ 编程语言
- **Markdown 编辑与预览**：CommonMark 原生解析 + WebView 渲染管线，支持 KaTeX/Mermaid/代码高亮
- **集成终端**：基于 Proot 架构的终端模拟器（proot 二进制与 rootfs 未随包分发，详见 THIRD_PARTY_NOTICES.md）
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

## 测试与覆盖率

### 运行测试

```bash
# 运行所有单元测试
./gradlew testDebugUnitTest

# 运行特定模块测试
./gradlew :core:common:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew :feature:editor:testDebugUnitTest

# 运行 UI/集成测试（需要模拟器或真机）
./gradlew connectedAndroidTest

# 运行特定测试类
./gradlew :core:common:testDebugUnitTest --tests "*GitHubApiContractTest*"
./gradlew :core:data:testDebugUnitTest --tests "*SnippetRepositoryDaoIntegrationTest*"
./gradlew :core:common:testDebugUnitTest --tests "*ChaosEngineeringTest*"
```

### 生成覆盖率报告

```bash
# Windows
scripts\coverage.bat

# Linux/macOS
./scripts/coverage.sh

# 手动生成
./gradlew jacocoTestReport
```

报告输出位置：
- HTML: `build/reports/jacoco/html/index.html`
- XML: `build/reports/jacoco/report.xml`

### 测试金字塔

DraftPeek 遵循测试金字塔模型：

```
        /\
       /  \      UI/E2E 测试 (Android Instrumentation)
      /----\    - 用户旅程测试
     /      \   - 可访问性测试
    /--------\
   /          \  集成测试
  /            \ - Repository + Room 集成
 /--------------\ - 跨模块事件总线
/                \ - API 契约测试 (MockWebServer)
------------------
单元测试 (Unit Tests)
- 业务逻辑验证
- 混沌工程测试
- 工具类测试
```

### 覆盖率阈值

| 模块 | 覆盖率要求 |
|------|-----------|
| 项目整体 | 60% |
| core-common | 70% |
| core-data | 75% |
| core-network | 80% |
| feature-editor | 60% |
| feature-browser | 60% |
| app | 50% |

详见 [`codecov.yml`](codecov.yml) 配置。

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

- [AGENTS.md](AGENTS.md) — AI 辅助开发指南与快速参考
- [docs/FILEMAP.md](docs/FILEMAP.md) — 完整文件清单
- [CHANGELOG.md](CHANGELOG.md) — 更新日志

## ⚠️ Git 分支与仓库安全策略（🚨 **必读，防止源代码泄露**）

本项目采用 **本地私有主分支 + 远程公开展示分支** 的双分支隔离架构。这是本项目的核心安全机制，所有开发者（包括 AI Assistant）**必须严格遵守**。

### 📋 分支定义

| 分支名称 | 位置 | 用途 | 能否推送到 GitHub | 包含内容 |
|---------|------|------|------------------|---------|
| **main** | 仅本地存在 | 日常开发与私有工作的主分支 | ❌ **禁止推送** | 完整源代码、加密密钥、敏感配置、所有功能实现代码 |
| **public** | 本地 + 远程 (`origin/public`) | GitHub 仓库可见内容（`https://github.com/ReSerendipity/DraftPeek`） | ✅ **唯一允许推送的分支** | README、截图、构建脚本、示例配置、文档（**不包含任何 `.kt/.java` 源代码**） |

### 🛑 绝对禁止的行为（会导致源代码或密钥泄露）

- ❌ `git push origin main` —— **会立即将私有源代码上传到 GitHub！**
- ❌ 在 `public` 分支提交 `.kt` / `.java` / `.cpp` 等源代码文件
- ❌ 将 `local.properties`（含签名密钥）、`keystore/` 目录添加到 public 分支
- ❌ 把 secret/key/password 硬编码提交到 Git 历史中

### 🔒 安全工作流程

#### 日常开发（始终在 main 分支）

```bash
# 1. 确保在 main 分支进行开发
git checkout main

# 2. 正常开发、提交
git add .
git commit -m "feat(editor): 实现 xx 功能"

# ⚠️ 注意：只执行 add+commit，不要执行 git push！
# 你的代码会保留在本地，不会被上传到 GitHub
```

#### 同步公共内容到 GitHub（手动操作，谨慎执行）

```bash
# 1. 切换到 public 分支
git checkout public

# 2. 清理当前状态（回到远端最新状态）
git reset --hard origin/public

# 3. 只添加非敏感文件（README、截图、文档、构建脚本等）
git add README.md docs/screenshots/ .gitignore *.gradle.kts ...

# 4. 选择性 cherry-pick 公共提交（如 AGENTS.md 的公开部分、CHANGELOG 等）
git cherry-pick <commit-hash>  # 只选择非源代码的提交

# 5. 验证是否包含敏感文件
git status   # 确认只有非源代码文件被修改
git diff --stat  # 确认没有源代码变更

# 6. 推送到远程（这是唯一允许的 push 操作）
git push origin public

# 7. 切回 main 分支继续开发
git checkout main
```

### 🧪 AI 开发时的安全检查清单

每次提交前，AI Agent **必须** 确认：

- [ ] **当前分支是 `main`**（日常开发用）
- [ ] 没有 `.kt` / `.java` / `.cpp` 源代码文件要被推送到 remote
- [ ] 没有 `local.properties` / `*.jks` / `keystore/` 等敏感文件
- [ ] **不会执行 `git push origin main`**
- [ ] 如果要推送到 GitHub，已经先在 `public` 分支上验证过

**如果不确定某个文件是否应该公开 → 默认视为不可公开 → 先问用户！**

### 📖 详细说明文档

更多关于双分支安全策略的详细说明，请参阅 [`AGENTS.md`](AGENTS.md) 的第 8 节「Git / 提交规范 & 分支策略」。
