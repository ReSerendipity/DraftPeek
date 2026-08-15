# FILEMAP.md — DraftPeek 完整文件清单

> 本文件是 `AGENTS.md` 的补充文档，包含项目所有源文件的详细清单。
> 当需要查找特定文件或了解模块内部结构时参考此文件。

## App Entry (`app/`)

| 文件 | 职责 |
|------|------|
| `DraftPeekApp.kt` | Application 类，初始化安全模块、语言设置 |
| `MainActivity.kt` | 主 Activity，承载 Compose 导航 |
| `SplashActivity.kt` | 启动屏，动画后跳转主界面 |
| `navigation/DraftPeekNavHost.kt` | Compose Navigation 导航图 |
| `navigation/DraftPeekTransitions.kt` | 导航转场动画 |
| `navigation/Route.kt` | 路由定义 |
| `onboarding/OnboardingActivity.kt` | 首次启动引导页 |
| `onboarding/OnboardingScreen.kt` | 引导页 UI（Compose） |
| `onboarding/SplashAnimation.kt` | 启动动画代码 |
| `security/AntiDebug.kt` | 反调试检测（端口探测、进程检查） |
| `security/ApkIntegrityChecker.kt` | APK 签名校验 |
| `security/DexIntegrityChecker.kt` | DEX 文件完整性校验 |
| `security/NativeSecurityChecker.kt` | JNI 桥接 native_security.c（Frida/TracerPid/Zygisk 检测） |
| `security/PlayIntegrityChecker.kt` | Google Play Integrity API 设备完整性校验 |
| `security/SecurityIntegrityChecker.kt` | 安全代码自校验（反射校验安全类完整性） |
| `widget/RecentFilesWidget.kt` | Glance AppWidget 最近文件展示 |
| `widget/RecentFilesWidgetReceiver.kt` | GlanceAppWidgetReceiver 注册 |

## Native C (`app/src/main/cpp/`)

| 文件 | 职责 |
|------|------|
| `CMakeLists.txt` | CMake 构建配置，输出 `native_security.so` |
| `native_security.c` | Native C 反检测（Frida maps 扫描、TracerPid 读取、Zygisk/Magisk 检测） |

## Editor (`feature/editor/`)

### sora/

| 文件 | 职责 |
|------|------|
| `SoraEditorWrapper.kt` | sora-editor 封装，管理语法高亮、内容同步、光标 |
| `MarkdownFormatHandler.kt` | Markdown 格式化处理 |
| `ThemeLoaderExt.kt` | 主题加载扩展 |
| `SoraThemeManager.kt` | TextMate 主题管理 |
| `SoraSearchManager.kt` | 搜索操作管理 |
| `SoraEditorColors.kt` | 预计算 ARGB 编辑器颜色值 |
| `VsCodeThemeImporter.kt` | VS Code 主题格式转换器 |
| `TokenColorCache.kt` | TextMate token 颜色 LRU 缓存 |
| `ThemeMetadata.kt` | 主题元数据 data class |
| `HighlightSpanPool.kt` | 高亮 span 对象池（复用优化） |
| `EditorThemeManager.kt` | 主题管理（Flow 状态） |
| `DeferredCommandQueue.kt` | 延迟命令队列 |
| `CustomThemeRepository.kt` | 自定义主题仓库接口 |
| `CustomThemeRepositoryImpl.kt` | 自定义主题仓库实现 |

### viewmodel/

| 文件 | 职责 |
|------|------|
| `EditorViewModel.kt` | 编辑器 ViewModel，文件加载/保存/搜索 |
| `EditorStateManager.kt` | 编辑器状态管理（内容、光标、滚动位置） |
| `DiffViewModel.kt` | 差异对比 ViewModel |
| `FavoriteManager.kt` | 收藏管理 |
| `EditorModeManager.kt` | 编辑器模式切换管理（Standard/Lite/Focus） |

### ui/

| 文件 | 职责 |
|------|------|
| `EditorScreen.kt` | 编辑器主界面（Compose） |
| `MarkdownWebViewPreview.kt` | Markdown WebView 预览 |
| `MarkdownPreviewScreen.kt` | Markdown 预览包装 |
| `MarkdownToolbar.kt` | Markdown 格式工具栏 |
| `MarkdownContextBar.kt` | Markdown 上下文栏 |
| `HtmlPreview.kt` | HTML 预览 |
| `PdfDocumentScreen.kt` | PDF 阅读器 |
| `OfficeDocumentScreen.kt` | Office 文档阅读器 |
| `MediaViewerScreen.kt` | 媒体文件查看器 |
| `BinaryFilePlaceholder.kt` | 二进制文件占位符 |
| `DiffScreen.kt` | 差异对比界面 |
| `CrossFileSearchScreen.kt` | 跨文件搜索 |
| `TabBar.kt` | 标签栏 |
| `OutlineDrawer.kt` | 大纲抽屉 |
| `EditorStatusBar.kt` | 编辑器状态栏 |
| `EditorScrollComponents.kt` | 滚动组件 |
| `CommandPalette.kt` | 命令面板 |
| `EmojiPicker.kt` | Emoji 选择器 |
| `EncodingSelectorDialog.kt` | 编码选择对话框 |
| `SaveToAppSheet.kt` | 保存到应用底部表单 |
| `WysiwYGWebViewPreview.kt` | WYSIWYG Markdown WebView 编辑器 |
| `SymbolPanel.kt` | 特殊符号面板（CJK、数学、箭头） |
| `MarkdownRichEditor.kt` | 原生 Compose WYSIWYG Markdown 编辑器（richeditor-compose） |
| `MarkdownModeSwitcher.kt` | Markdown 视图模式分段切换 |
| `MarkdownCheatSheet.kt` | Markdown 语法速查对话框 |
| `LazyMarkdownPreview.kt` | LazyColumn 流式 Markdown 预览 |

### repository/

| 文件 | 职责 |
|------|------|
| `EditorRepository.kt` | 文件读写接口 |
| `EditorRepositoryImpl.kt` | 文件读写实现 |
| `FileContentResult.kt` | 离线优先内容状态密封类 |
| `EditorFileReadOutcome.kt` | 离线优先文件读取结果密封类 |

### model/

| 文件 | 职责 |
|------|------|
| `EditorUiState.kt` | UI 状态密封类 |
| `EditorMessage.kt` | 编辑器消息 |
| `EditorTab.kt` | 标签页模型 |
| `EditorMode.kt` | 编辑器模式枚举（Standard/Lite/Focus） |
| `MarkdownTheme.kt` | Markdown 主题 |
| `MarkdownViewMode.kt` | Markdown 视图模式 |
| `MarkdownOutline.kt` | 大纲项 |
| `MarkdownBlock.kt` | Markdown 块模型（流式/LazyColumn） |
| `RichDocumentModel.kt` | 富文档树模型（parser/writer） |
| `DocumentStats.kt` | 文档统计 |
| `ExportConfig.kt` | 导出配置 |
| `LanguageMapper.kt` | 语言映射 |

### 其他子目录

| 文件 | 职责 |
|------|------|
| `tabs/TabManager.kt` | 多标签页管理（Singleton） |
| `tabs/TabStateManager.kt` | 标签页状态管理 |
| `tabs/SessionManager.kt` | 会话管理 |
| `treesitter/TreeSitterLanguageProvider.kt` | Tree-sitter 语言支持 |
| `lsp/LspManager.kt` | LSP 管理器 |
| `lsp/LspClient.kt` | LSP 客户端 |
| `lsp/LspModels.kt` | LSP 数据模型 |
| `lsp/LspTransport.kt` | LSP 传输层抽象（stdio/socket/pipe） |
| `lsp/Lsp4jConverter.kt` | LSP4J ↔ DraftPeek 类型转换器 |
| `lsp/PythonLspProvider.kt` | Python LSP 提供者 |
| `lsp/KotlinLspProvider.kt` | Kotlin LSP 提供者（stub） |
| `lsp/JavaScriptLspProvider.kt` | JavaScript/TypeScript LSP 提供者（stub） |
| `lsp/JavaLspProvider.kt` | Java LSP 提供者（stub） |
| `lsp/GoLspProvider.kt` | Go LSP 提供者（stub） |
| `lsp/DartLspProvider.kt` | Dart LSP 提供者（stub） |
| `lsp/SoraLspBridge.kt` | DraftPeek LSP 与 sora-editor LSP 桥接 |
| `diagnostics/SimpleDiagnosticProvider.kt` | 简单诊断提供者 |
| `diagnostics/DiagnosticNavigator.kt` | 诊断导航状态管理 |
| `input/KeyboardShortcutHandler.kt` | 键盘快捷键处理 |
| `util/MarkdownExporter.kt` | Markdown 导出 |
| `util/MarkdownDocxExporter.kt` | Markdown 转 DOCX 导出 |
| `util/MarkdownChunker.kt` | Markdown 内容分块器（流式） |
| `util/HtmlMarkdownConverter.kt` | HTML ↔ Markdown 转换器 |
| `markdown/MarkdownRendererPipeline.kt` | 基于插件的 Markdown 渲染管线接口和实现 |
| `markdown/BuiltinMarkdownPlugins.kt` | 内置插件：KaTeX、Mermaid、TOC 提取、Frontmatter 解析 |
| `data/CacheManager.kt` | 缓存管理 |
| `di/EditorModule.kt` | Hilt DI 模块 |

## Browser (`feature/browser/`)

| 文件 | 职责 |
|------|------|
| `viewmodel/FileBrowserViewModel.kt` | 文件列表、导航栈、Git 状态 |
| `viewmodel/SnippetViewModel.kt` | 代码片段 CRUD |
| `viewmodel/GitViewModel.kt` | Git 操作 |
| `viewmodel/GitStatusViewModel.kt` | Git 状态监控 |
| `viewmodel/RecentFilesViewModel.kt` | 最近文件展示 |
| `ui/FileBrowserScreen.kt` | 文件浏览主界面 |
| `ui/BrowseHistoryScreen.kt` | 浏览历史 |
| `ui/SnippetScreen.kt` | 代码片段管理 |
| `ui/SampleFilesScreen.kt` | 内置示例文件 |
| `ui/CreateFileDialog.kt` | 新建文件对话框 |
| `ui/SnippetDetailDialog.kt` | 代码片段详情对话框 |
| `ui/GitCommitDialog.kt` | Git 提交对话框 |
| `ui/GitActionSheet.kt` | Git 操作底部表单 |
| `ui/GitScreen.kt` | Git 完整操作界面（commit/diff/branch/remote） |
| `ui/DragDropController.kt` | 拖拽排序控制器 |
| `vfs/FileSystemProvider.kt` | 文件系统提供者接口（local/FTP/SFTP） |
| `vfs/FileSystemRegistry.kt` | 文件系统提供者注册表（URI scheme 查找） |
| `vfs/LocalFileSystemProvider.kt` | 本地 SAF 文件系统提供者 |
| `vfs/FtpFileSystemProvider.kt` | FTP 文件系统提供者（commons-net） |
| `vfs/SftpFileSystemProvider.kt` | SFTP 文件系统提供者（JSch） |
| `observer/DirectoryObserver.kt` | 基于 FileObserver 的目录变更监听（debounced Flow） |
| `model/FileItem.kt` | 文件项数据模型 |
| `model/FileSortOption.kt` | 排序选项 |
| `model/FileContent.kt` | 文件内容模型 |
| `model/BrowserUiState.kt` | 浏览器 UI 状态 |
| `model/GitHubImportState.kt` | GitHub 导入状态 |
| `repository/FileRepository.kt` | 文件仓库接口 |
| `repository/FileRepositoryImpl.kt` | 文件仓库实现 |
| `sample/SampleFileManager.kt` | 示例文件管理 |
| `util/FileTemplateProvider.kt` | 文件模板 |
| `di/BrowserModule.kt` | Hilt DI 模块 |
| `di/VcsModule.kt` | VCS DI 模块 |

## Settings (`feature/settings/`)

| 文件 | 职责 |
|------|------|
| `viewmodel/SettingsViewModel.kt` | 设置 ViewModel |
| `repository/SettingsRepository.kt` | 设置仓库接口 |
| `repository/SettingsRepositoryImpl.kt` | 设置仓库实现（DataStore） |
| `model/EditorSettings.kt` | 编辑器设置 data class |
| `cache/CachedPreference.kt` | 响应式缓存偏好工具（同步读取 + Flow 观察） |
| `di/SettingsModule.kt` | Hilt DI 模块 |

## Stats (`feature/stats/`)

| 文件 | 职责 |
|------|------|
| `viewmodel/StatsViewModel.kt` | 统计 ViewModel |
| `ui/ProfileScreen.kt` | 个人资料 / 设置 / 统计混合界面 |
| `ui/AccessibilityScreen.kt` | 无障碍设置界面 |
| `ui/AchievementScreen.kt` | 成就展示页面（9 类别、70+ 成就） |
| `ui/component/ActivityCalendarGrid.kt` | GitHub 风格热力图 |
| `ui/component/StatCard.kt` | 统计卡片 |
| `ui/component/TimeRangeSelector.kt` | 时间范围选择 |
| `ui/component/YearSelector.kt` | 年份选择 |
| `ui/component/DayDetailDialog.kt` | 日期详情对话框 |
| `ui/component/ContributionLegend.kt` | 贡献图例 |
| `ui/component/RainbowColorPicker.kt` | 彩虹颜色选择器 |
| `ui/component/YearHeatmapNew.kt` | 年热力图组件（GitHub 风格） |
| `ui/component/StatCardGrid.kt` | 2x2 统计卡片网格 |
| `ui/component/PeriodChipRow.kt` | 时间段标签选择器 |
| `ui/component/GreetingSection.kt` | 欢迎/问候区域（含成就徽章） |
| `ui/component/AchievementBadgeChip.kt` | 成就徽章标签组件 |
| `ui/style/StatsStyle.kt` | 统计视觉样式 |
| `model/ActivitySummary.kt` | 活动摘要 |
| `model/UserEngagement.kt` | 用户参与度 |
| `model/OperationType.kt` | 操作类型 |
| `model/TimeRange.kt` | 时间范围 |
| `model/Achievement.kt` | 成就 data class |
| `model/AchievementCategory.kt` | 成就类别枚举（READ/CREATE/DURATION/CHARS/STREAK 等） |
| `model/AchievementStats.kt` | 成就统计聚合 data class |
| `util/StatsExporter.kt` | 统计导出工具 |
| `util/AchievementDefinitions.kt` | 成就定义（9 类别、70+ 项） |
| `util/AchievementCalculator.kt` | 成就统计计算器 |

## Terminal (`feature/terminal/`)

| 文件 | 职责 |
|------|------|
| `ui/TerminalScreen.kt` | 终端主界面（Compose） |
| `ui/FloatingTerminal.kt` | 悬浮终端窗口 |
| `viewmodel/TerminalViewModel.kt` | 终端 ViewModel |
| `service/TerminalService.kt` | 终端前台服务 |
| `emulator/TerminalSessionManager.kt` | 终端会话管理 |
| `emulator/ProotSessionManager.kt` | Proot 会话管理 |
| `emulator/CommandExecutor.kt` | 命令执行 |
| `emulator/SecurityValidator.kt` | 命令安全校验 |
| `emulator/ArgumentTokenizer.kt` | 命令参数分词 |
| `receiver/TerminalCommandReceiver.kt` | 终端命令广播接收器 |
| `model/TerminalModels.kt` | 终端数据模型 |
| `di/TerminalModule.kt` | Hilt DI 模块 |

## Data Layer (`core/data/`)

| 文件 | 职责 |
|------|------|
| `db/AppDatabase.kt` | Room 数据库定义（version=10, exportSchema=true） |
| `dao/RecentFileDao.kt` | 最近文件 DAO |
| `dao/SnippetDao.kt` | 代码片段 DAO |
| `dao/UserActivityDao.kt` | 用户活动 DAO |
| `dao/BookmarkDao.kt` | 书签 DAO（CRUD、URI 唯一性、Flow 查询） |
| `entity/RecentFile.kt` | 最近文件实体 |
| `entity/Snippet.kt` | 代码片段实体 |
| `entity/SnippetFts.kt` | 代码片段全文搜索（FTS4 虚拟表） |
| `entity/UserActivity.kt` | 用户活动实体 |
| `entity/Bookmark.kt` | 书签实体（BookmarkEntity） |
| `repository/RecentFilesRepository.kt` | 最近文件仓库接口 |
| `repository/RecentFilesRepositoryImpl.kt` | 最近文件仓库实现 |
| `repository/SnippetRepository.kt` | 代码片段仓库接口 |
| `repository/SnippetRepositoryImpl.kt` | 代码片段仓库实现 |
| `repository/UserActivityRepository.kt` | 用户活动仓库接口 |
| `repository/UserActivityRepositoryImpl.kt` | 用户活动仓库实现 |
| `repository/EditorFileRepository.kt` | 编辑器文件操作仓库接口 |
| `repository/EditorFileRepositoryImpl.kt` | 编辑器文件操作仓库实现 |
| `repository/BookmarkRepository.kt` | 书签仓库接口 |
| `repository/BookmarkRepositoryImpl.kt` | 书签仓库实现 |
| `security/DatabaseKeyManager.kt` | SQLCipher 密钥管理（Keystore） |
| `security/FileCipher.kt` | AES-256-GCM 文件加密（密码加密） |
| `security/CredentialManager.kt` | Git 凭证安全存储（Keystore + EncryptedSharedPreferences） |
| `security/SecureFileStorage.kt` | 安全文件存储（AES-256-GCM、Keystore-based） |
| `usecase/AddRecentFileUseCase.kt` | 添加最近文件用例 |
| `usecase/GetRecentFilesUseCase.kt` | 获取最近文件用例 |
| `usecase/ManageBookmarksUseCase.kt` | 管理书签用例 |
| `usecase/OpenFileUseCase.kt` | 打开文件用例 |
| `usecase/ReadingPositionUseCase.kt` | 阅读位置用例 |
| `usecase/RecordUserActivityUseCase.kt` | 记录用户活动用例 |
| `usecase/RemoveStaleUrisUseCase.kt` | 移除过期 URI 用例 |
| `usecase/SaveFileUseCase.kt` | 保存文件用例 |
| `usecase/SearchSnippetsUseCase.kt` | 搜索代码片段用例 |
| `util/FtsQueryBuilder.kt` | FTS4 搜索查询构建器（AND/OR/NOT、短语匹配、列限定） |
| `DataStoreProvider.kt` | DataStore 实例提供者 |
| `di/DataModule.kt` | Hilt DI 模块 |

## Domain (`core/domain/`)

| 文件 | 职责 |
|------|------|
| `model/EditorFile.kt` | 编辑器文件领域模型 |
| `model/ReadingPosition.kt` | 阅读位置领域模型 |
| `model/UserActivitySummary.kt` | 用户活动摘要领域模型 |
| `usecase/AddRecentFileUseCase.kt` | 添加最近文件用例 |
| `usecase/GetRecentFilesUseCase.kt` | 获取最近文件用例 |
| `usecase/ManageBookmarksUseCase.kt` | 管理书签用例 |
| `usecase/OpenFileUseCase.kt` | 打开文件用例 |
| `usecase/ReadingPositionUseCase.kt` | 阅读位置用例 |
| `usecase/RecordUserActivityUseCase.kt` | 记录用户活动用例 |
| `usecase/RemoveStaleUrisUseCase.kt` | 移除过期 URI 用例 |
| `usecase/SaveFileUseCase.kt` | 保存文件用例 |
| `usecase/SearchSnippetsUseCase.kt` | 搜索代码片段用例 |

## Design System (`core/designsystem/`)

| 文件 | 职责 |
|------|------|
| `theme/Theme.kt` | 主题定义 |
| `theme/Color.kt` | 颜色定义（TokenColors, SemanticColors, FileTypeColors） |
| `theme/Type.kt` | 字体排版（AppFonts） |
| `theme/Shape.kt` | 形状定义（BrandShapes） |
| `theme/Spacing.kt` | 间距常量（DraftPeekSpacing） |
| `theme/Elevation.kt` | 海拔/阴影定义（BrandElevation） |
| `theme/FontOptions.kt` | 字体选项配置（FontOption） |
| `theme/ColorBlindnessHelper.kt` | 色盲模拟 |
| `theme/ColorBlindMode.kt` | 色盲模式枚举 |
| `theme/AccessibilityState.kt` | 无障碍状态管理 |
| `theme/HighContrastScheme.kt` | 高对比度配色方案 |
| `res/font/jetbrains_mono_*.ttf` | JetBrains Mono 字体（semibold/medium/regular） |
| `res/font/inter_*.ttf` | Inter 字体（semibold/medium/regular/bold） |

## Common (`core/common/`)

### Util (`core/common/.../util/`)

| 文件 | 职责 |
|------|------|
| `AppFileManager.kt` | 内部文件管理 |
| `AppCacheManager.kt` | 应用缓存管理 |
| `EncodingDetector.kt` | 文件编码检测 |
| `OfficeDocumentParser.kt` | Office 文档解析（Apache POI → HTML） |
| `DiffEngine.kt` | 文本差异比较（java-diff-utils 4.12，含 OOM 防护） |
| `LanguageConfig.kt` | 编程语言配置 |
| `MarkdownSanitizer.kt` | Markdown 输入清洗 |
| `InputValidator.kt` | 输入验证工具 |
| `NetworkConnectivityChecker.kt` | 网络连接检查 |
| `HapticFeedbackHelper.kt` | 触觉反馈工具 |
| `WebViewStyleHelper.kt` | WebView 样式注入 |
| `PerformanceBenchmark.kt` | 性能基准测试 |
| `FpsMonitor.kt` | FPS 监控 |
| `DocumentTypeHelper.kt` | 文档类型识别 |
| `FileUtils.kt` | 文件工具函数 |
| `RenderProfiler.kt` | 渲染性能分析器（内存跟踪、Compose recomposition） |
| `OutputThrottler.kt` | 高频流输出节流器（Git 输出、日志） |
| `NativeMarkdownRenderer.kt` | CommonMark 原生 Markdown 渲染器 |
| `HtmlMarkdownConverter.kt` | HTML ↔ Markdown 转换器 |
| `EditorConfigParser.kt` | .editorconfig 文件解析器 |
| `DeferredInitializer.kt` | 延迟初始化器（后台预加载） |
| `CssThemeManager.kt` | WebView Markdown CSS 主题管理器 |
| `ContentChecksum.kt` | CRC32 内容校验和（变更检测） |
| `CommonMarkParser.kt` | CommonMark Markdown 解析器 |

### VCS (`core/common/.../vcs/`)

| 文件 | 职责 |
|------|------|
| `GitManager.kt` | Git 操作（JGit 6.10.1） |
| `GitHubApiClient.kt` | GitHub API 客户端 |
| `CredentialProvider.kt` | Git 凭证存储抽象接口 |

### Plugin (`core/common/.../plugin/`)

| 文件 | 职责 |
|------|------|
| `PluginManager.kt` | 插件生命周期管理（ConcurrentHashMap） |
| `DraftPeekPlugin.kt` | 插件接口 + ExtensionRegistry + LanguageExtensionProvider + ThemeExtensionProvider |
| `PluginInfo.kt` | 插件元数据 data class + ExtensionPoint 枚举 |
| `Pf4jPluginLoader.kt` | PF4J 插件框架适配器 |

### Error (`core/common/.../error/`)

| 文件 | 职责 |
|------|------|
| `AppError.kt` | 密封类：Network / FileOperation / Validation / Parse / Unknown |
| `ErrorEvent.kt` | 一次性错误事件（UI 消费） |
| `ErrorHandler.kt` | 异常 → AppError 转换器 |

### Event (`core/common/.../event/`)

| 文件 | 职责 |
|------|------|
| `AppEvent.kt` | 应用事件标记接口 |
| `AppEventBus.kt` | 全局事件总线（SharedFlow, replay=0） |
| `EditorEvents.kt` | 编辑器模块事件定义 |
| `BrowserEvents.kt` | 浏览器模块事件定义 |

### Feature (`core/common/.../feature/`)

| 文件 | 职责 |
|------|------|
| `FeatureToggleManager.kt` | 运行时功能开关管理器（SharedPreferences + StateFlow） |
| `FeatureFlag.kt` | 功能标志枚举定义 |

### Model (`core/common/.../model/`)

| 文件 | 职责 |
|------|------|
| `Ids.kt` | 类型安全 ID 值类（TabId, SnippetId 等） |

### Security (`core/common/.../security/`)

| 文件 | 职责 |
|------|------|
| `SecurityGate.kt` | 敏感操作安全门控（保存、导出、删除、创建） |

## UI Components (`core/ui/`)

### Theme (`core/ui/.../theme/`)

| 文件 | 职责 |
|------|------|
| `Theme.kt` | 主题定义 |
| `Color.kt` | 颜色定义 |
| `Type.kt` | 字体排版 |
| `Shape.kt` | 形状定义 |
| `Spacing.kt` | 间距常量 |
| `Elevation.kt` | 海拔（阴影）定义 |
| `FontOptions.kt` | 字体选项配置 |
| `ThemeLoader.kt` | JSON 主题加载 |
| `ColorBlindnessHelper.kt` | 色盲模拟 |
| `ColorBlindMode.kt` | 色盲模式枚举 |
| `AccessibilityState.kt` | 无障碍状态管理 |
| `HighContrastScheme.kt` | 高对比度配色方案 |

### Components (`core/ui/.../component/`)

| 文件 | 职责 |
|------|------|
| `BrandButton.kt` | BrandFilledButton / BrandOutlinedButton / BrandTonalButton |
| `BrandSwitch.kt` | 品牌开关 |
| `BrandDialog.kt` | 品牌对话框 |
| `BrandOutlinedTextField.kt` | 品牌输入框 |
| `BrandTopBar.kt` | 品牌顶栏 |
| `BrandFAB.kt` | 品牌浮动操作按钮 |
| `BrandChip.kt` | 品牌标签 |
| `BrandSearchBar.kt` | 品牌搜索栏 |
| `BrandFileCard.kt` | 品牌文件卡片 |
| `BrandToast.kt` | 品牌 Toast 通知 |
| `BrandSettingRow.kt` | 品牌设置行 |
| `BrandPill.kt` | 品牌药丸组件 |
| `BrandIconButton.kt` | 品牌图标按钮 |
| `FileTypeIcon.kt` | 文件类型图标映射 |
| `CustomTabBar.kt` | 自定义标签栏 |
| `CustomScaffold.kt` | 自定义脚手架 |
| `QuickSettingsBottomSheet.kt` | 快捷设置底部表单 |
| `ConfirmDialog.kt` | 确认对话框 |
| `SectionHeader.kt` | 分区标题 |
| `EmptyStateView.kt` | 空状态视图 |
| `DraggableDivider.kt` | 可拖拽分隔线 |
| `AccessibilitySemantics.kt` | 无障碍语义 |
| `HapticController.kt` | 触觉反馈控制器 |

### Modifier (`core/ui/.../modifier/`)

| 文件 | 职责 |
|------|------|
| `BlurModifier.kt` | 模糊效果修饰符 |
| `PressStateModifier.kt` | 按压状态修饰符 |
| `TouchTargetModifier.kt` | 触摸目标修饰符 |

### Layout (`core/ui/.../layout/`)

| 文件 | 职责 |
|------|------|
| `FoldableStateProvider.kt` | 折叠屏适配 |
| `SplitScreenLayout.kt` | 分屏布局 |
| `SplitPane.kt` | 可调整大小的分栏布局（折叠屏/铰链感知） |
| `AdaptiveScaffold.kt` | 自适应脚手架 |
| `AdaptiveListDetailScaffold.kt` | 自适应列表-详情布局（Material3 SupportingPaneScaffold） |
| `WindowSize.kt` | 窗口尺寸检测 |

### Icon (`core/ui/.../icon/`)

| 文件 | 职责 |
|------|------|
| `DraftPeekIcons.kt` | 自定义图标定义 |
| `StrokeIcons.kt` | 线条风格图标定义（Lucide-style） |
| `StrokeIcon.kt` | 线条图标渲染组件（Canvas-based） |

### Composition (`core/ui/.../composition/`)

| 文件 | 职责 |
|------|------|
| `DraftPeekLocals.kt` | CompositionLocal 提供者（FeatureToggle、EditorSettings、IsLandscape、IsWideScreen、FoldInfo） |
