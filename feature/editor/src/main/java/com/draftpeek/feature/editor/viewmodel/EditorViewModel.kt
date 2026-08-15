/**
 * 文件功能：编辑器主界面 ViewModel
 * 
 * 主要类/数据类：
 * - [EditorSavedState]：编辑器保存状态数据类，用于配置变更或进程死亡后恢复
 * - [OutlineItem]：文件大纲项数据类（函数、类、标题等结构）
 * - [EditorViewModel]：编辑器主 ViewModel，作为协调层委托给各专用管理器
 * 
 * 模块依赖：
 * - core/common：AppError/ErrorHandler 错误处理、SecurityGate 安全检查、PerformanceBenchmark、NetworkConnectivityChecker 等
 * - core/common/event：AppEventBus 用于跨模块事件通知
 * - core/data：RecentFilesRepository、SnippetRepository、UserActivityRepository 数据仓库及 UseCase
 * - feature/editor/repository：EditorRepository 文件读写
 * - feature/editor/tabs：TabManager、TabStateManager、SessionManager 标签页和会话管理
 * - feature/editor/diagnostics：SimpleDiagnosticProvider 代码诊断
 * - feature/editor/data：CacheManager 缓存管理
 * - Hilt：依赖注入
 */
package com.draftpeek.feature.editor.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.error.AppError
import com.draftpeek.core.common.error.ErrorHandler
import com.draftpeek.core.common.error.FileOp
import com.draftpeek.core.common.error.FileType
import com.draftpeek.core.common.event.AppEventBus
import com.draftpeek.core.common.event.EditorEvent
import com.draftpeek.core.common.model.TabId
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.common.util.NetworkConnectivityChecker
import com.draftpeek.core.common.util.PerformanceBenchmark
import com.draftpeek.core.data.repository.RecentFilesRepository
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.usecase.AddRecentFileUseCase
import com.draftpeek.core.data.usecase.ReadingPositionUseCase
import com.draftpeek.core.domain.usecase.RecordUserActivityUseCase
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.EditorMessage
import com.draftpeek.feature.editor.model.EditorTab
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.repository.EditorRepository
import com.draftpeek.feature.editor.repository.FileReadResult
import com.draftpeek.feature.editor.tabs.RestoredPosition
import com.draftpeek.feature.editor.tabs.TabManager
import com.draftpeek.feature.editor.tabs.TabStateManager
import com.draftpeek.feature.editor.tabs.TabStateManager.SwitchResult
import com.draftpeek.feature.editor.tabs.SessionManager
import com.draftpeek.feature.editor.diagnostics.DiagnosticItem
import com.draftpeek.feature.editor.diagnostics.DiagnosticNavigationState
import com.draftpeek.feature.editor.diagnostics.moveToNext
import com.draftpeek.feature.editor.diagnostics.moveToPrevious
import com.draftpeek.feature.editor.diagnostics.SimpleDiagnosticProvider
import com.draftpeek.feature.editor.diagnostics.fromDiagnostics
import com.draftpeek.feature.editor.ui.FileSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.draftpeek.feature.editor.repository.FileReadProgress
import javax.inject.Inject

/**
 * 编辑器保存状态
 * 
 * 用于配置变更（如屏幕旋转）或进程死亡后恢复编辑器状态，包括内容、光标位置、滚动位置等。
 * 
 * @property content 文件文本内容
 * @property cursorLine 光标行号（1-based）
 * @property cursorColumn 光标列号（1-based）
 * @property scrollX 水平滚动偏移
 * @property scrollY 垂直滚动偏移
 * @property language 语言标识符
 * @property fileName 文件名
 * @property isReadOnly 是否只读
 * @property documentType 文档类型（PDF/Word/Excel 等）
 */
data class EditorSavedState(
    val content: String,
    val cursorLine: Int,
    val cursorColumn: Int,
    val scrollX: Int,
    val scrollY: Int,
    val language: String?,
    val fileName: String,
    val isReadOnly: Boolean = false,
    val documentType: com.draftpeek.core.common.util.DocumentType? = null,
)

/**
 * 文件大纲项
 * 
 * 表示文件大纲中的单个条目（函数、类、Markdown 标题等）。
 * 使用 @Immutable 标记，因为所有属性都是 val，允许 Compose 在重新发射相同实例时
 * 跳过重组（利用数据类相等性）。
 * 
 * @property name 大纲项名称（如函数名、标题文本）
 * @property line 所在行号（1-based）
 * @property type 条目类型（如 "function"、"class"、"h1"、"h2"）
 */
@Immutable
data class OutlineItem(
    val name: String,
    val line: Int,
    val type: String,
)

/**
 * 编辑器主界面 ViewModel
 * 
 * 作为薄协调层，将具体职责委托给：
 * - [EditorStateManager]：编辑器核心状态（内容、光标、滚动等）
 * - [TabStateManager]：每个标签页的状态持久化
 * - [FavoriteManager]：收藏状态管理
 * - [TabManager]：标签列表管理（MRU 驱逐）
 * - [SessionManager]：会话持久化和恢复
 * 
 * **状态管理**：
 * - UI 状态通过 StateFlow 暴露给 Compose 层
 * - 一次性消息（如保存成功/失败）通过 SharedFlow 发送
 * - 所有文件 I/O 和 CPU 密集型操作在 Dispatchers.IO/Default 上执行
 * 
 * **数据流**：
 * 1. 文件打开 → loadFileFromUri → repository.readFileWithProgress → 状态更新
 * 2. 内容变更 → onContentChanged → EditorStateManager（防抖）→ 诊断分析（防抖）
 * 3. 保存 → saveFile → repository.writeFile → 状态更新
 * 4. 标签切换 → switchToTab → TabStateManager 保存/恢复状态
 * 5. 跨文件搜索 → searchInFiles（并发）→ FileSearchResult 列表
 * 
 * **优化要点**：
 * - 跨文件搜索使用 async + awaitAll 并发执行，总耗时 ≈ max(单文件时间)
 * - 单文件搜索使用流式按行读取，避免将整个文件加载到内存
 * - 诊断分析和阅读位置保存均采用防抖策略，减少不必要的计算和 I/O
 * - CRC32 校验和快速检测内容是否恢复到基线版本
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: EditorRepository,
    private val recentFilesRepository: RecentFilesRepository,
    private val snippetRepository: SnippetRepository,
    private val tabManager: TabManager,
    private val tabStateManager: TabStateManager,
    private val editorStateManager: EditorStateManager,
    private val favoriteManager: FavoriteManager,
    private val userActivityRepository: UserActivityRepository,
    private val recordUserActivity: RecordUserActivityUseCase,
    private val addRecentFile: AddRecentFileUseCase,
    private val readingPosition: ReadingPositionUseCase,
    private val networkChecker: NetworkConnectivityChecker,
    private val sessionManager: SessionManager,
    private val appEventBus: AppEventBus,
    val cacheManager: com.draftpeek.feature.editor.data.CacheManager,
    @param:ApplicationContext private val appContext: android.content.Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    @Volatile
    private var uriString: String = savedStateHandle["uri"] ?: ""

    /** 当前打开文件的 URI 字符串 */
    val currentUriString: String get() = uriString

    // ------------------------------------------------------------------
    // 委托状态流（来自各 Manager）
    // ------------------------------------------------------------------

    val uiState: StateFlow<EditorUiState> = editorStateManager.uiState
    val isModified: StateFlow<Boolean> = editorStateManager.isModified
    val cursorPosition: StateFlow<CursorPosition> = editorStateManager.cursorPosition
    val scrollPosition: StateFlow<Pair<Int, Int>> = editorStateManager.scrollPosition
    val outlineItems: StateFlow<ImmutableList<OutlineItem>> = editorStateManager.outlineItems
    val markdownViewMode: StateFlow<MarkdownViewMode> = editorStateManager.markdownViewMode
    val isFocusMode: StateFlow<Boolean> = editorStateManager.isFocusMode
    val isTypewriterMode: StateFlow<Boolean> = editorStateManager.isTypewriterMode
    val isLargeMarkdownFile: StateFlow<Boolean> = editorStateManager.isLargeMarkdownFile
    val markdownTheme: StateFlow<MarkdownTheme> = editorStateManager.markdownTheme
    val liveContent: StateFlow<String> = editorStateManager.liveContent
    val isPreviewMode: Boolean get() = editorStateManager.isPreviewMode
    val isFavorite: StateFlow<Boolean> = favoriteManager.isFavorite

    val isMarkdownFile: Boolean
        get() = editorStateManager.isMarkdownFile

    val isPreviewable: Boolean
        get() = editorStateManager.isPreviewable

    /** 当前文件是否需要流式预览（超大 Markdown 文件） */
    val needsStreamingPreview: Boolean
        get() = editorStateManager.needsStreamingPreview

    val tabs: StateFlow<ImmutableList<EditorTab>> = tabManager.tabs
    val activeTabId: StateFlow<TabId?> = tabManager.activeTabId

    private val _showEncodingDialog = MutableStateFlow(false)
    /** 是否显示编码选择对话框 */
    val showEncodingDialog: StateFlow<Boolean> = _showEncodingDialog.asStateFlow()

    private val _detectedEncoding = MutableStateFlow<String?>(null)
    /** 自动检测到的文件编码 */
    val detectedEncoding: StateFlow<String?> = _detectedEncoding.asStateFlow()

    private val _showSaveEncodingDialog = MutableStateFlow(false)
    /** 是否显示保存编码选择对话框 */
    val showSaveEncodingDialog: StateFlow<Boolean> = _showSaveEncodingDialog.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    /** 是否正在保存文件 */
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _messageEvent = MutableSharedFlow<EditorMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** 一次性消息事件流（保存成功/失败、加载失败等） */
    val messageEvent: SharedFlow<EditorMessage> = _messageEvent.asSharedFlow()

    private var saveJob: Job? = null
    private var savePositionJob: Job? = null

    private var loadingTimeoutJob: Job? = null

    /** 防抖的诊断分析任务 */
    private var diagnosticJob: Job? = null

    /** 当前诊断项列表，暴露给 UI 渲染波浪下划线 */
    private val _diagnostics = MutableStateFlow<ImmutableList<DiagnosticItem>>(persistentListOf())
    val diagnostics: StateFlow<ImmutableList<DiagnosticItem>> = _diagnostics.asStateFlow()

    /** 诊断导航状态（上一个/下一个跳转） */
    private val _diagnosticNavigation = MutableStateFlow(DiagnosticNavigationState())
    val diagnosticNavigation: StateFlow<DiagnosticNavigationState> = _diagnosticNavigation.asStateFlow()

    @Volatile
    private var usageStartTimeMs: Long = 0L
    private var usageTrackingJob: Job? = null
    @Volatile
    private var isUsageTracking = false

    @Volatile
    private var lastContentLength: Int = 0
    private var charWriteJob: Job? = null
    private val pendingCharCountAtomic = java.util.concurrent.atomic.AtomicInteger(0)

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Log.e(TAG, "Uncaught exception in ViewModel coroutine", throwable)
        }
    }

    /**
     * Launch a fire-and-forget IO coroutine with exception handling.
     * Used for user activity recording and other non-critical background tasks.
     */
    private fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            block()
        }
    }

    init {
        sessionManager.initialize(appContext, viewModelScope)

        viewModelScope.launch {
            while (true) {
                delay(DIRTY_FLUSH_TICK_MS)
                editorStateManager.flushDirtyState()
            }
        }

        viewModelScope.launch {
            uiState.collect { state ->
                when (state) {
                    is EditorUiState.Loading, is EditorUiState.LoadingWithProgress -> {
                        startLoadingTimeout()
                    }
                    is EditorUiState.Success, is EditorUiState.Error -> {
                        cancelLoadingTimeout()
                    }
                }
            }
        }

        viewModelScope.launch {
            val navigatedUri = savedStateHandle["uri"] as? String ?: ""
            if (navigatedUri.isNotBlank()) {
                uriString = navigatedUri
                editorStateManager.setLoading()
                loadFile()
                loadFavoriteStatus()
                return@launch
            }

            val sessionData = sessionManager.restoreSession()
            if (sessionData != null && sessionData.tabs.isNotEmpty()) {
                val activeTab = sessionData.tabs.find { it.id == sessionData.activeTabId }
                if (activeTab != null) {
                    uriString = activeTab.uri
                    val restoredPos = tabStateManager.getRestoredPosition(activeTab.id)
                    loadFile(restoredPosition = restoredPos)
                    loadFavoriteStatus()
                }
            } else {
                loadFile()
                loadFavoriteStatus()
            }
        }

        viewModelScope.launch {
            tabManager.tabs.collect {
                saveActiveTabStateToTabStateManager()
                sessionManager.saveSession()
            }
        }
        viewModelScope.launch {
            tabManager.activeTabId.collect {
                saveActiveTabStateToTabStateManager()
                sessionManager.saveSession()
            }
        }
    }

    /**
     * 从指定 URI 加载文件
     * 
     * @param uri 文件 URI 字符串
     */
    fun loadFileFromUri(uri: String) {
        if (uri.isBlank()) return
        if (uri == uriString) return
        uriString = uri
        editorStateManager.setLoading()
        loadFile()
        loadFavoriteStatus()
    }

    // ------------------------------------------------------------------
    // 标签页管理
    // ------------------------------------------------------------------

    /**
     * 切换到指定标签页
     * 
     * 算法步骤：
     * 1. 保存当前活动标签的状态到 TabStateManager
     * 2. 调用 tabStateManager.switchTab(tabId) 获取切换结果
     * 3. 根据结果类型处理：
     *    - RestoreState：从保存的状态恢复（内容已缓存）
     *    - LoadFromDisk：从磁盘重新加载文件
     *    - Noop：标签未找到，不做处理
     * 
     * @param tabId 目标标签页 ID
     */
    fun switchToTab(tabId: TabId) {
        PerformanceBenchmark.startTimer("tab_switch")
        val currentTab = tabManager.getActiveTab()
        if (currentTab != null) {
            editorStateManager.saveState()?.let { state ->
                tabStateManager.saveTabState(currentTab.id, state)
            }
        }

        when (val result = tabStateManager.switchTab(tabId)) {
            is SwitchResult.RestoreState -> {
                tabStateManager.removeRestoredPosition(tabId)
                editorStateManager.restoreState(result.state)
                PerformanceBenchmark.endTimer("tab_switch")
                PerformanceBenchmark.recordSuccess("tab_switch")
            }
            is SwitchResult.LoadFromDisk -> {
                PerformanceBenchmark.endTimer("tab_switch", logResult = false)
                uriString = result.uri
                editorStateManager.setLoading()
                loadFile(restoredPosition = result.restoredPosition)
                loadFavoriteStatus()
            }
            is SwitchResult.Noop -> {
                PerformanceBenchmark.endTimer("tab_switch", logResult = false)
            }
        }
    }

    /**
     * 关闭指定标签页
     * 
     * @param tabId 要关闭的标签页 ID
     * @return 关闭后是否还有剩余标签页
     */
    fun closeTab(tabId: TabId): Boolean {
        val wasActive = tabManager.activeTabId.value == tabId
        val hasTabsLeft = tabStateManager.closeTab(tabId)

        if (wasActive) {
            val newActive = tabManager.getActiveTab()
            if (newActive != null) {
                switchToTab(newActive.id)
            } else {
                editorStateManager.setLoading()
            }
        }

        return hasTabsLeft
    }

    /**
     * 关闭所有打开的标签页并重置为空状态
     */
    fun closeAllTabs() {
        tabManager.closeAllTabs()
        editorStateManager.setLoading()
    }

    // ------------------------------------------------------------------
    // 公共 API – 委托给 EditorStateManager
    // ------------------------------------------------------------------

    /**
     * 编辑器内容变更时调用（由 SoraEditorWrapper 以 150ms 防抖）
     * 
     * @param content 新的文本内容
     */
    fun onContentChanged(content: String) {
        editorStateManager.onContentChanged(content)
        editorStateManager.flushDirtyState()
        scheduleDiagnosticAnalysis(content)
    }

    /** 编辑器显示时调用，开始使用时长追踪 */
    fun onEditorShown() {
        startUsageTracking()
    }

    /** 编辑器隐藏时调用，停止使用时长追踪并记录 */
    fun onEditorHidden() {
        stopUsageTracking()
    }

    // ------------------------------------------------------------------
    // 诊断（波浪下划线 + 工具提示）
    // ------------------------------------------------------------------

    /**
     * 调度防抖的诊断分析任务
     * 
     * 在 [DIAGNOSTIC_DEBOUNCE_MS] 无活动后，在 [Dispatchers.Default] 上运行
     * [SimpleDiagnosticProvider.analyze]，然后更新 [_diagnostics]。
     * 
     * 防抖防止快速输入时过度使用 CPU。对于超过 500KB 的超大文件也会跳过分析，
     * 因为正则扫描开销太大。
     * 
     * @param content 要分析的文本内容
     */
    private fun scheduleDiagnosticAnalysis(content: String) {
        if (content.length > DIAGNOSTIC_MAX_CONTENT_LENGTH) {
            if (_diagnostics.value.isNotEmpty()) {
                _diagnostics.value = persistentListOf()
                _diagnosticNavigation.value = DiagnosticNavigationState()
            }
            return
        }
        diagnosticJob?.cancel()
        diagnosticJob = viewModelScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
            delay(DIAGNOSTIC_DEBOUNCE_MS)
            PerformanceBenchmark.startTimer("diagnostic_analysis")
            val language = editorStateManager.getLanguage()
            val items = SimpleDiagnosticProvider.analyze(content, language)
            PerformanceBenchmark.endTimer("diagnostic_analysis")
            PerformanceBenchmark.recordSuccess("diagnostic_analysis")
            _diagnostics.value = items.toImmutableList()
            _diagnosticNavigation.value = _diagnosticNavigation.value.fromDiagnostics(items)
        }
    }

    /** 清除所有诊断标记（如加载新文件时） */
    fun clearDiagnostics() {
        diagnosticJob?.cancel()
        _diagnostics.value = persistentListOf()
        _diagnosticNavigation.value = DiagnosticNavigationState()
    }

    /**
     * 导航到当前文件中的下一个诊断项
     * 
     * UI 应调用此方法，然后使用导航状态将编辑器光标跳转到聚焦的诊断位置。
     */
    fun navigateToNextDiagnostic() {
        val current = _diagnosticNavigation.value
        _diagnosticNavigation.value = current.moveToNext()
    }

    /**
     * 导航到当前文件中的上一个诊断项
     */
    fun navigateToPreviousDiagnostic() {
        val current = _diagnosticNavigation.value
        _diagnosticNavigation.value = current.moveToPrevious()
    }

    /**
     * 内容长度变化时调用，用于追踪字符写入统计
     * 
     * @param newLength 新的内容长度
     */
    fun onContentLengthChanged(newLength: Int) {
        if (lastContentLength == 0) {
            lastContentLength = newLength
            return
        }
        val delta = newLength - lastContentLength
        if (delta > 0) {
            pendingCharCountAtomic.addAndGet(delta)
            scheduleCharWrite()
        }
        lastContentLength = newLength
    }

    /**
     * 重置内容长度追踪（如加载新文件时）
     * 
     * @param length 当前内容长度
     */
    fun resetContentLength(length: Int) {
        lastContentLength = length
        pendingCharCountAtomic.set(0)
        charWriteJob?.cancel()
        charWriteJob = null
    }

    private fun startUsageTracking() {
        if (isUsageTracking) return
        isUsageTracking = true
        usageStartTimeMs = System.currentTimeMillis()
        startMinuteTracking()
    }

    private fun stopUsageTracking() {
        if (!isUsageTracking) return
        isUsageTracking = false
        stopMinuteTracking()
        recordUsageDuration()
    }

    private fun startMinuteTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(60_000L)
                recordUsageDuration()
                usageStartTimeMs = System.currentTimeMillis()
            }
        }
    }

    private fun stopMinuteTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = null
    }

    private fun recordUsageDuration() {
        if (usageStartTimeMs == 0L) return
        val elapsedMs = System.currentTimeMillis() - usageStartTimeMs
        val minutes = (elapsedMs / 60_000L).toInt()
        if (minutes > 0) {
            launchIo { recordUserActivity.recordUsageDuration(minutes) }
        }
    }

    private fun scheduleCharWrite() {
        charWriteJob?.cancel()
        charWriteJob = viewModelScope.launch {
            delay(CHAR_WRITE_DEBOUNCE_MS)
            flushCharCount()
        }
    }

    private fun flushCharCount() {
        val count = pendingCharCountAtomic.getAndSet(0)
        if (count > 0) {
            launchIo { recordUserActivity.recordCharWrite(count) }
        }
    }

    /**
     * 光标位置变化时调用
     * 
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     */
    fun onCursorChanged(line: Int, column: Int) {
        editorStateManager.onCursorChanged(line, column)
        debounceSaveReadingPosition(line, column)
    }

    /**
     * 滚动位置变化时调用
     * 
     * @param scrollX 水平滚动偏移
     * @param scrollY 垂直滚动偏移
     */
    fun onScrollChanged(scrollX: Int, scrollY: Int) {
        editorStateManager.onScrollChanged(scrollX, scrollY)
        val pos = editorStateManager.cursorPosition.value
        debounceSaveReadingPosition(pos.line, pos.column, scrollX, scrollY)
    }

    /**
     * 大纲项更新时调用
     * 
     * @param json JSON 格式的大纲数组
     */
    fun onOutlineItems(json: String) {
        editorStateManager.onOutlineItems(json)
    }

    /**
     * 切换 Markdown 预览模式（编辑 → 预览 → 分屏 → WYSIWYG 循环）
     */
    fun togglePreviewMode() {
        editorStateManager.togglePreviewMode()
        if (markdownViewMode.value != MarkdownViewMode.EDIT) {
            launchIo { recordUserActivity.recordPreview() }
        }
    }

    /**
     * 设置指定的 Markdown 视图模式
     * 
     * @param mode 目标视图模式
     */
    fun setMarkdownViewMode(mode: MarkdownViewMode) {
        editorStateManager.setMarkdownViewMode(mode)
    }

    /** 切换专注模式 */
    fun toggleFocusMode() = editorStateManager.toggleFocusMode()
    
    /** 切换打字机模式 */
    fun toggleTypewriterMode() = editorStateManager.toggleTypewriterMode()
    
    /**
     * 设置 Markdown 预览主题
     * 
     * @param theme 要应用的主题
     */
    fun setMarkdownTheme(theme: MarkdownTheme) = editorStateManager.setMarkdownTheme(theme)

    /**
     * 保存当前编辑器内容回文件
     * 
     * @param content 要保存的内容，为 null 时使用状态管理器的当前内容
     * @param encoding 可选的文件编码，为 null 时默认 UTF-8
     * @throws SecurityException 安全检查不通过时抛出（通过 messageEvent 通知用户）
     */
    fun saveFile(content: String? = null, encoding: String? = null) {
        if (uriString.isEmpty()) return
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch { _messageEvent.emit(EditorMessage.Info(R.string.security_operation_restricted)) }
            return
        }
        if (_isSaving.value) {
            viewModelScope.launch { _messageEvent.emit(EditorMessage.SaveInProgress) }
            return
        }
        val contentToSave = content ?: editorStateManager.getCurrentContent()
        saveJob = viewModelScope.launch {
            _isSaving.value = true
            PerformanceBenchmark.startTimer("file_save")
            try {
                repository.writeFile(Uri.parse(uriString), contentToSave, encoding)
                    .onSuccess {
                        PerformanceBenchmark.endTimer("file_save")
                        PerformanceBenchmark.recordSuccess("file_save")
                        editorStateManager.onContentSaved(contentToSave)
                        recordUserActivity.recordTextEdit()
                        if (encoding != null) {
                            _detectedEncoding.value = encoding
                        }
                        _messageEvent.emit(EditorMessage.SaveSuccess)
                        appEventBus.emit(EditorEvent.FileSaved(uriString, editorStateManager.getFileName() ?: ""))
                    }
                    .onFailure { e ->
                        PerformanceBenchmark.endTimer("file_save", logResult = false)
                        PerformanceBenchmark.recordError("file_save", e.message)
                        val appError = ErrorHandler.fromException(e, networkChecker.isNetworkAvailable())
                        _messageEvent.emit(EditorMessage.SaveFailed(appError))
                    }
            } finally {
                _isSaving.value = false
                saveJob = null
            }
        }
    }

    /**
     * 等待当前保存操作完成（用于在关闭文件前确保保存完成）
     */
    suspend fun awaitSaveCompletion() {
        saveJob?.join()
    }

    /**
     * 删除当前文件（仅支持内部应用文件）
     * 
     * @return 删除操作已启动返回 true
     */
    fun deleteCurrentFile(): Boolean {
        if (uriString.isEmpty()) return false
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch { _messageEvent.emit(EditorMessage.Info(R.string.security_operation_restricted)) }
            return false
        }
        if (!repository.isInternalFile(uriString)) return false
        launchIo {
            repository.deleteInternalFile(uriString)
            recentFilesRepository.removeRecentFile(uriString)
            recordUserActivity.recordFileManagement()
        }
        return true
    }

    /**
     * 同步内容（外部调用入口，委托给 onContentChanged 触发诊断）
     * 
     * @param content 要同步的内容
     */
    fun syncContent(content: String) {
        onContentChanged(content)
    }

    // ------------------------------------------------------------------
    // 收藏
    // ------------------------------------------------------------------

    /** 切换当前文件的收藏状态 */
    fun toggleFavorite() {
        if (uriString.isEmpty()) return
        launchIo {
            favoriteManager.toggleFavorite(uriString)
        }
    }

    // ------------------------------------------------------------------
    // 代码片段
    // ------------------------------------------------------------------

    /**
     * 将当前内容保存为代码片段
     * 
     * @param title 片段标题
     * @param content 片段内容
     * @param language 编程语言
     * @param category 分类名称
     */
    fun saveAsSnippet(title: String, content: String, language: String?, category: String) {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch { _messageEvent.emit(EditorMessage.Info(R.string.security_operation_restricted)) }
            return
        }
        val now = System.currentTimeMillis()
        launchIo {
            snippetRepository.addSnippet(
                Snippet(
                    title = title,
                    content = content,
                    language = language,
                    category = category.ifBlank { DEFAULT_SNIPPET_CATEGORY },
                    createdAt = now,
                    updatedAt = now,
                )
            )
            recordUserActivity.recordSnippetCreated()
            recordUserActivity.recordFileCreate()
        }
    }

    /** 获取所有代码片段的 Flow */
    fun getAllSnippets() = snippetRepository.getAllSnippets()
    
    /**
     * 获取当前文件名
     * 
     * @return 文件名
     */
    fun getFileName(): String? = editorStateManager.getFileName()
    
    /**
     * 获取当前语言
     * 
     * @return 语言标识符
     */
    fun getLanguage(): String? = editorStateManager.getLanguage()
    
    /** 记录搜索操作 */
    fun recordSearch() { launchIo { recordUserActivity.recordSearch() } }
    
    /** 记录导出操作 */
    fun recordExport() { launchIo { recordUserActivity.recordExport() } }
    
    /** 记录文件管理操作 */
    fun recordFileManagement() { launchIo { recordUserActivity.recordFileManagement() } }

    /**
     * 将当前 HTML 内容转换为 Markdown 并更新编辑器
     * 
     * 使用户可以打开 HTML 文件，转换为 Markdown，编辑 Markdown 源码，然后保存结果。
     * 转换由 HtmlMarkdownConverter 执行，在 [Dispatchers.Default] 上运行以避免大文件阻塞主线程。
     */
    fun convertHtmlToMarkdown() {
        val currentState = editorStateManager.uiState.value
        if (currentState !is EditorUiState.Success || !currentState.isHtmlFile) return

        val htmlContent = currentState.content
        viewModelScope.launch {
            val markdown = withContext(Dispatchers.Default) {
                com.draftpeek.core.common.util.HtmlMarkdownConverter.htmlToMarkdown(htmlContent)
            }
            editorStateManager.loadContent(
                content = markdown,
                language = "markdown",
                fileName = currentState.fileName.replace(Regex("\\.(html?|htm)$", RegexOption.IGNORE_CASE), ".md"),
                isReadOnly = currentState.isReadOnly,
                documentType = null,
                renderedHtml = null,
                fileSizeWarning = null,
                fileSize = markdown.length.toLong(),
            )
        }
    }

    // ------------------------------------------------------------------
    // 状态快照
    // ------------------------------------------------------------------

    /**
     * 保存当前编辑器状态
     * 
     * @return 状态快照，UI 状态不是 Success 时返回 null
     */
    fun saveState(): EditorSavedState? = editorStateManager.saveState()

    /**
     * 从保存的状态恢复
     * 
     * @param savedState 要恢复的状态快照
     */
    fun restoreState(savedState: EditorSavedState) {
        editorStateManager.restoreState(savedState)
    }

    // ------------------------------------------------------------------
    // 阅读位置持久化
    // ------------------------------------------------------------------

    /**
     * 防抖保存阅读位置到数据库
     * 
     * 等待 [READING_POSITION_DEBOUNCE_MS] 无活动后持久化，避免频繁写数据库。
     * 
     * @param line 光标行号
     * @param column 光标列号
     * @param scrollX 水平滚动位置
     * @param scrollY 垂直滚动位置
     */
    private fun debounceSaveReadingPosition(
        line: Int = 1,
        column: Int = 1,
        scrollX: Int = 0,
        scrollY: Int = 0,
    ) {
        if (uriString.isEmpty()) return
        savePositionJob?.cancel()
        savePositionJob = viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            delay(READING_POSITION_DEBOUNCE_MS)
            readingPosition.save(uriString, line, column, scrollX, scrollY)
        }
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /**
     * 加载文件（内部实现）
     * 
     * 算法步骤：
     * 1. 验证 URI 不为空
     * 2. 使用 readFileWithProgress 流式加载，实时更新进度条
     * 3. 加载完成后调用 onFileReadComplete 处理后续逻辑
     * 4. 处理各种异常情况：SecurityException（权限过期）、FileNotFoundException（文件不存在）
     * 
     * @param encoding 可选的文件编码
     * @param restoredPosition 会话恢复的光标/滚动位置
     */
    private fun loadFile(encoding: String? = null, restoredPosition: RestoredPosition? = null) {
        if (uriString.isBlank()) {
            editorStateManager.setError("No file specified")
            return
        }
        viewModelScope.launch {
            try {
                PerformanceBenchmark.startTimer("file_open")

                repository.readFileWithProgress(Uri.parse(uriString), encoding)
                    .collect { progress ->
                        when (progress) {
                            is FileReadProgress.Loading -> {
                                editorStateManager.setLoadingProgress(
                                    loadedBytes = progress.loadedBytes,
                                    totalBytes = progress.totalBytes,
                                )
                            }
                            is FileReadProgress.Done -> {
                                onFileReadComplete(progress.result, restoredPosition)
                            }
                        }
                    }
                PerformanceBenchmark.endTimer("file_open")
                PerformanceBenchmark.recordSuccess("file_open")
            } catch (e: CancellationException) {
                PerformanceBenchmark.endTimer("file_open", logResult = false)
                throw e
            } catch (e: SecurityException) {
                PerformanceBenchmark.endTimer("file_open", logResult = false)
                PerformanceBenchmark.recordError("file_open", "SecurityException: ${e.message}")
                editorStateManager.setError(e.message ?: "Failed to load file")
                _messageEvent.emit(
                    EditorMessage.LoadFailed(
                        AppError.FileOperation(
                            FileType.OTHER,
                            FileOp.READ,
                            R.string.editor_error_permission_expired
                        )
                    )
                )
            } catch (e: Exception) {
                PerformanceBenchmark.endTimer("file_open", logResult = false)
                PerformanceBenchmark.recordError("file_open", e.message)
                editorStateManager.setError(e.message ?: "Failed to load file")
                if (e is java.io.FileNotFoundException && repository.isInternalFile(uriString)) {
                    recentFilesRepository.removeRecentFile(uriString)
                } else if (e is java.io.FileNotFoundException) {
                    _messageEvent.emit(
                        EditorMessage.LoadFailed(
                            AppError.FileOperation(
                                FileType.OTHER,
                                FileOp.READ,
                                R.string.editor_error_external_file_not_found
                            )
                        )
                    )
                }
            }
        }
    }

    /** 显示编码选择对话框 */
    fun showEncodingSelector() { _showEncodingDialog.value = true }
    
    /** 隐藏编码选择对话框 */
    fun dismissEncodingSelector() { _showEncodingDialog.value = false }

    /**
     * 文件读取完成后的处理
     * 
     * 算法步骤：
     * 1. 更新检测到的编码
     * 2. 确定要恢复的光标/滚动位置（优先级：会话恢复 > 最近文件记录 > 默认值）
     * 3. 调用 editorStateManager.loadContent 更新 UI 状态
     * 4. 清除并重新调度诊断分析
     * 5. 添加到最近文件记录、打开标签页、记录用户活动
     * 
     * @param result 文件读取结果
     * @param restoredPosition 会话恢复的位置，为 null 时从最近文件读取
     */
    private suspend fun onFileReadComplete(
        result: FileReadResult,
        restoredPosition: RestoredPosition? = null,
    ) {
        _detectedEncoding.value = result.detectedEncoding

        val restoreLine: Int
        val restoreColumn: Int
        val restoreScrollX: Int
        val restoreScrollY: Int
        if (restoredPosition != null) {
            val activeId = tabManager.activeTabId.value
            if (activeId != null) {
                tabStateManager.removeRestoredPosition(activeId)
            }
            restoreLine = restoredPosition.cursorLine
            restoreColumn = restoredPosition.cursorColumn
            restoreScrollX = restoredPosition.scrollX
            restoreScrollY = restoredPosition.scrollY
        } else {
            val savedPosition = readingPosition.get(uriString)
            restoreLine = savedPosition?.cursorLine ?: 1
            restoreColumn = savedPosition?.cursorColumn ?: 1
            restoreScrollX = savedPosition?.scrollX ?: 0
            restoreScrollY = savedPosition?.scrollY ?: 0
        }

        // 大文件保护:超大 Markdown 文件强制使用纯文本编辑模式,避免 RichTextEditor 崩溃
        // 必须在 loadContent 之前设置,否则 WYSIWYG 编辑器可能已经开始渲染大内容
        val isMarkdownFile = result.fileName.endsWith(".md", ignoreCase = true) ||
            result.fileName.endsWith(".markdown", ignoreCase = true)
        if (result.fileSize > MAX_WYSIWYG_FILE_SIZE && isMarkdownFile) {
            editorStateManager.setMarkdownViewMode(MarkdownViewMode.EDIT)
            Log.w(TAG, "Large file (${result.fileSize} bytes) forced to EDIT mode to prevent RichTextEditor crash")
        }

        editorStateManager.loadContent(
            content = result.content,
            language = result.language,
            fileName = result.fileName,
            isReadOnly = result.isReadOnly,
            documentType = result.documentType,
            renderedHtml = result.renderedHtml,
            fileSizeWarning = result.fileSizeWarning,
            fileSize = result.fileSize,
            restoreCursorLine = restoreLine,
            restoreCursorColumn = restoreColumn,
            restoreScrollX = restoreScrollX,
            restoreScrollY = restoreScrollY,
            isBinaryFile = result.isBinaryFile,
            isTruncated = result.isTruncated,
        )

        clearDiagnostics()
        scheduleDiagnosticAnalysis(result.content)
        addRecentFile(
            uri = uriString,
            fileName = result.fileName,
            language = result.language,
            fileSize = result.fileSize,
        )
        tabManager.openTab(uriString, result.fileName, result.language)
        recordUserActivity.recordFileOpen()
        resetContentLength(result.content.length)
        if (result.documentType in PREVIEWABLE_DOC_TYPES) {
            recordUserActivity.recordPreview()
        }
    }

    /**
     * 使用指定编码重新加载文件
     * 
     * @param encoding 要使用的文件编码
     */
    fun reloadWithEncoding(encoding: String) {
        dismissEncodingSelector()
        loadFile(encoding)
    }

    /** 显示保存编码选择对话框 */
    fun showSaveEncodingSelector() { _showSaveEncodingDialog.value = true }
    
    /** 隐藏保存编码选择对话框 */
    fun dismissSaveEncodingSelector() { _showSaveEncodingDialog.value = false }

    /**
     * 使用指定编码保存文件
     * 
     * @param encoding 保存时使用的编码
     */
    fun saveWithEncoding(encoding: String) {
        dismissSaveEncodingSelector()
        saveFile(encoding = encoding)
    }

    /**
     * 在最近打开的文件和当前标签页中搜索查询字符串
     * 
     * **算法优化**：
     * - 使用 async + awaitAll 并发扫描多个标签，原实现串行 await 总耗时 = N × 单文件时间，
     *   优化后总耗时 ≈ max(单文件时间)。对 5 个各 50ms 的搜索场景，从 250ms → 50ms（约 5 倍加速）。
     * - 单文件搜索使用流式按行读取，避免将整个文件加载到内存。
     * - 命中达 [MAX_SEARCH_RESULTS_PER_FILE] 即提前退出。
     * 
     * @param query 搜索查询字符串
     * @return 匹配的文件搜索结果列表
     */
    suspend fun searchInFiles(query: String): List<FileSearchResult> {
        if (query.isBlank()) return emptyList()
        recordSearch()

        return withContext(Dispatchers.Default) {
            val seenUris = mutableSetOf<String>()

            val deferredResults = tabManager.tabs.value
                .filter { seenUris.add(it.uri) }
                .map { tab ->
                    async(Dispatchers.Default) {
                        searchSingleFile(tab.uri, tab.fileName, query)
                    }
                }

            deferredResults.awaitAll().filterNotNull()
        }
    }

    /**
     * 搜索单个文件中的查询字符串，返回结果（无匹配或错误时返回 null）
     * 
     * 从 searchInFiles 提取，符合单一职责原则并便于测试。
     * 使用流式按行读取，避免将整个文件作为 String 加载到内存。
     * 
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param query 搜索查询（不区分大小写）
     * @return 搜索结果，无匹配或错误时返回 null
     */
    private suspend fun searchSingleFile(
        uri: String,
        fileName: String,
        query: String,
    ): FileSearchResult? {
        return try {
            val lineNumbers = mutableListOf<Int>()
            val lineSnippets = mutableListOf<String>()

            val stream = repository.openInputStreamForSearch(Uri.parse(uri)) ?: return null
            stream.use { input ->
                java.io.BufferedReader(java.io.InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var lineNum = 0
                    var line = reader.readLine()
                    while (line != null) {
                        lineNum++
                        // Use Kotlin's ignoreCase parameter which avoids creating
                        // intermediate lowercase strings for every line (reduces GC)
                        if (line.contains(query, ignoreCase = true)) {
                            lineNumbers.add(lineNum)
                            lineSnippets.add(line.trim())
                            if (lineNumbers.size >= MAX_SEARCH_RESULTS_PER_FILE) break
                        }
                        line = reader.readLine()
                    }
                }
            }

            if (lineNumbers.isNotEmpty()) {
                FileSearchResult(fileName, uri, lineNumbers, lineSnippets)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "searchInFiles: failed to read $fileName", e)
            null
        }
    }

    private fun loadFavoriteStatus() {
        favoriteManager.loadFavoriteStatus(viewModelScope, uriString)
    }

    /**
     * 将活动标签的编辑器状态保存到 TabStateManager
     * 
     * 必须在 [sessionManager.saveSession()] 之前调用，以便光标/滚动位置包含在持久化会话数据中。
     */
    private fun saveActiveTabStateToTabStateManager() {
        val activeTab = tabManager.getActiveTab() ?: return
        editorStateManager.saveState()?.let { state ->
            tabStateManager.saveTabState(activeTab.id, state)
        }
    }

    /**
     * ViewModel 清除时取消所有任务，防止泄漏，并持久化当前会话
     */
    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
        savePositionJob?.cancel()
        loadingTimeoutJob?.cancel()
        diagnosticJob?.cancel()
        usageTrackingJob?.cancel()
        charWriteJob?.cancel()
        stopUsageTracking()
        flushCharCount()
        saveActiveTabStateToTabStateManager()
        sessionManager.saveSession()
    }

    private fun startLoadingTimeout() {
        cancelLoadingTimeout()
        loadingTimeoutJob = viewModelScope.launch {
            delay(LOADING_TIMEOUT_MS)
            if (uiState.value is EditorUiState.Loading || uiState.value is EditorUiState.LoadingWithProgress) {
                editorStateManager.setError(appContext.getString(R.string.editor_init_timeout))
            }
        }
    }

    private fun cancelLoadingTimeout() {
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = null
    }

    companion object {
        private const val TAG = "EditorViewModel"

        /** 阅读位置保存防抖时长（毫秒） */
        private const val READING_POSITION_DEBOUNCE_MS = 500L
        /** 字符写入统计防抖时长（毫秒） */
        private const val CHAR_WRITE_DEBOUNCE_MS = 150L
        /** 每个文件最大搜索结果数 */
        private const val MAX_SEARCH_RESULTS_PER_FILE = 100
        /** 默认代码片段分类名称 */
        private const val DEFAULT_SNIPPET_CATEGORY = "未分类"

        /** 刷新防抖脏状态的 tick 间隔（毫秒） */
        private const val DIRTY_FLUSH_TICK_MS = 100L

        /** 运行诊断分析前的防抖时长（毫秒） */
        private const val DIAGNOSTIC_DEBOUNCE_MS = 800L

        /** 跳过诊断分析的文件大小阈值（字符数 ≈ 字节数） */
        private const val DIAGNOSTIC_MAX_CONTENT_LENGTH = 500_000

        /** WYSIWYG 编辑器最大文件大小 (500KB) - 超过此大小强制使用纯文本模式 */
        private const val MAX_WYSIWYG_FILE_SIZE = 500 * 1024L

        /** 加载超时：如果文件在此时间内未加载完成，显示错误 */
        private const val LOADING_TIMEOUT_MS = 10_000L

        /** 支持预览的文档类型集合 */
        private val PREVIEWABLE_DOC_TYPES = setOf(
            DocumentType.PDF, DocumentType.WORD, DocumentType.EXCEL, DocumentType.POWERPOINT
        )
    }
}
