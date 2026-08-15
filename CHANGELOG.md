# Changelog

本项目所有重要变更均记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [Unreleased] (开发中)

### Added
- 新增 `README.md` 项目说明文档
- 新增 `local.properties.example` 配置模板
- 新增 `CHANGELOG.md` 更新日志
- 新增 `docs/ARCHITECTURE.md` 架构设计文档
- 新增 `scripts/` 辅助脚本目录（build-debug、build-release、run-unit-tests、install-on-device、clean-project、package-release）
- 新增 `.env.example` 环境变量模板
- 清理根目录 40+ 临时日志文件
- 更新 `.gitignore` 排除临时日志文件

## [1.0.30] - 2026-08-10

### Added
- 新增 `values-zh-rTW` 繁体中文资源（所有模块）
- 新增 `BookmarkDaoTest`、`SecurityEventDaoTest`、`SnippetDaoTest` 单元测试
- 新增 `core/data` 测试 DI 模块和 Repository 测试
- 新增 `core/domain` 测试目录
- 新增 `core/ui` 测试目录
- 新增 `StartupBenchmark` 启动基准测试

### Changed
- versionCode 29 → 30, versionName 1.0.29 → 1.0.30
- 更新启动器图标（前景图、单色图标）
- 优化 GitHub Actions CI 工作流

## [1.0.29] - 2026-07

### Added
- 集成 sora-editor 语法高亮引擎 (TextMate + Tree-sitter)
- Tree-sitter Java 语法支持 (sora-language-treesitter 0.24.6 + tree-sitter 4.3.2)
- 实验性 LSP 客户端 (Eclipse LSP4J 0.22.0)，支持 Python/Kotlin/JS/Java/Go/Dart
- Markdown 原生解析管线 (CommonMark 0.24.0 + GFM 扩展)
- Markdown WYSIWYG 编辑 (richeditor-compose 1.0.0-rc13)
- Office 文档解析 (Apache POI 5.3.0：Word/Excel/PPT → HTML → WebView)
- PDF 渲染 (Android PdfRenderer + android-pdf-viewer)
- Diff 引擎 (java-diff-utils 4.12 替换自研 Myers diff)
- Glance AppWidget 最近文件快捷访问
- 首次启动引导流程 (OnboardingActivity)
- 插件系统 (PF4J 3.11.0)

### Changed
- Markwon 移除，替换为 CommonMark 原生解析 + WebView 管线
- DiffEngine 使用 java-diff-utils 替换自研 Myers diff
- 安全-加密库 security-crypto 移除，替换为自建 SecureFileStorage + SecurePreferences
- SQLCipher 迁移至 net.zetetic:sqlcipher-android (4.6.0)
- R8 Full Mode 禁用 (兼容 Apache POI/JGit 反射链)

### Security
- Native C 反检测 (native_security.so：Frida/TracerPid/Zygisk 检测)
- Play Integrity API 设备完整性校验 (VULN-009)
- APK Signature Scheme v3 启用
- jsoup HTML 消毒 (VULN-004)
- 字符串加密 IR 插件 + 假类生成 (防逆向)

## [1.0.28] - 2026-06

### Added
- 整洁架构分层：core/* (common/data/designsystem/domain/testing/ui) + feature/* (browser/editor/settings/stats/terminal)
- build-logic Convention Plugins 统一构建配置
- Hilt 2.58 依赖注入
- Room 2.7.1 + SQLCipher 加密数据库
- 终端模块 (Proot 会话管理 + 终端模拟器)
- Git 集成 (GitHub 仓库导入 + JGit 操作)
- VFS 抽象层 (local/FTP/SFTP 文件系统)
- 可访问性设计 (色盲模式、高对比度、自适应布局)
