/**
 * 文件功能：编辑器核心状态管理器
 * 
 * 主要类/数据类：
 * - [CursorPosition]：结构化光标位置数据类（替代旧的 "line:column" 字符串）
 * - [EditorStateManager]：编辑器状态管理单例，负责内容、修改标记、光标、滚动位置、大纲、预览模式等状态
 * 
 * 模块依赖：
 * - core/common：ContentChecksum 提供 CRC32 校验和功能
 * - feature/editor/model：EditorUiState、MarkdownTheme、MarkdownViewMode 等 UI 状态模型
 * - feature/editor/tabs：TabManager 用于标签页状态同步
 * - kotlinx-collections-immutable：不可变集合确保线程安全
 * - kotlinx-coroutines：Flow 响应式状态管理
 * - Hilt：依赖注入
 */
package com.draftpeek.feature.editor.viewmodel

import androidx.compose.runtime.Immutable
import com.draftpeek.core.common.util.ContentChecksum
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.tabs.TabManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 结构化光标位置
 * 
 * 替代旧的 "line:column" 字符串表示，使用 @Immutable 标记让 Compose 在相同光标位置
 * 重新发射时可以跳过重组（利用数据类的身份相等性）。
 * 
 * @property line 行号（1-based）
 * @property column 列号（1-based）
 */
@Immutable
data class CursorPosition(val line: Int = 1, val column: Int = 1) {
    /**
     * 转换为显示用的字符串格式
     * 
     * @return "行:列" 格式的字符串，如 "42:15"
     */
    fun toDisplayString(): String = "$line:$column"
}

/**
 * 编辑器核心状态管理器
 * 
 * 从 EditorViewModel 中提取，作为独立可测试的状态管理器。负责管理：
 * - 编辑器 UI 状态（加载/成功/错误/带进度加载）
 * - 内容修改标记（带防抖和 CRC32 校验）
 * - 光标位置
 * - 滚动位置
 * - Markdown 大纲项
 * - Markdown 预览模式
 * - 专注模式/打字机模式
 * - Markdown 主题
 * - 实时内容流（用于分屏预览同步）
 * 
 * **优化要点**：
 * - 单一职责原则：从 ViewModel 提取，可独立测试
 * - 依赖倒置：依赖注入的 TabManager 抽象
 * - 热路径优化：onContentChanged 仅在内容实际变化时更新状态
 * - 空安全：所有 UI 状态访问都有 null 安全保护
 * - 可测试性：单例注入，无需 Android 组件即可测试
 * - 防抖机制：修改标记延迟提交，避免快速输入时状态抖动
 * - CRC32 校验：快速检测内容是否恢复到基线版本
 */
@Singleton
class EditorStateManager @Inject constructor(
    private val tabManager: TabManager,
) {

    companion object {
        /** 修改状态防抖窗口（毫秒） */
        const val DIRTY_DEBOUNCE_MS = 500L

        /** 流式预览的文件大小阈值（字节），超过此大小使用 LazyMarkdownPreview */
        const val STREAMING_PREVIEW_FILE_THRESHOLD = 2L * 1024 * 1024 // 2MB

        /** WYSIWYG 编辑器最大文件大小（字节），超过此大小强制纯文本模式 */
        const val WYSIWYG_FILE_SIZE_LIMIT = 500L * 1024 // 500KB
    }

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    /** 编辑器 UI 状态流 */
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    /** 内容是否已修改（未保存）状态流 */
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _cursorPosition = MutableStateFlow(CursorPosition())
    /** 光标位置状态流 */
    val cursorPosition: StateFlow<CursorPosition> = _cursorPosition.asStateFlow()

    private val _scrollPosition = MutableStateFlow(Pair(0, 0))
    /** 滚动位置状态流（scrollX, scrollY） */
    val scrollPosition: StateFlow<Pair<Int, Int>> = _scrollPosition.asStateFlow()

    private val _outlineItems = MutableStateFlow<ImmutableList<OutlineItem>>(persistentListOf())
    /** Markdown 大纲项列表状态流 */
    val outlineItems: StateFlow<ImmutableList<OutlineItem>> = _outlineItems.asStateFlow()

    private val _markdownViewMode = MutableStateFlow(MarkdownViewMode.WYSIWYG)
    /** Markdown 视图模式状态流 */
    val markdownViewMode: StateFlow<MarkdownViewMode> = _markdownViewMode.asStateFlow()

    private val _isFocusMode = MutableStateFlow(false)
    /** 是否处于专注模式状态流 */
    val isFocusMode: StateFlow<Boolean> = _isFocusMode.asStateFlow()

    private val _isTypewriterMode = MutableStateFlow(false)
    /** 是否处于打字机模式状态流 */
    val isTypewriterMode: StateFlow<Boolean> = _isTypewriterMode.asStateFlow()

    private val _isLargeMarkdownFile = MutableStateFlow(false)
    /** 当前是否为大 Markdown 文件（超过 WYSIWYG 阈值） */
    val isLargeMarkdownFile: StateFlow<Boolean> = _isLargeMarkdownFile.asStateFlow()

    private val _markdownTheme = MutableStateFlow(MarkdownTheme.DEFAULT)
    /** Markdown 预览主题状态流 */
    val markdownTheme: StateFlow<MarkdownTheme> = _markdownTheme.asStateFlow()

    /**
     * 设置 Markdown 预览主题
     * 
     * @param theme 要应用的 Markdown 主题
     */
    fun setMarkdownTheme(theme: MarkdownTheme) {
        _markdownTheme.value = theme
    }

    /** 当前是否为预览模式（非纯编辑模式） */
    val isPreviewMode: Boolean get() = _markdownViewMode.value != MarkdownViewMode.EDIT

    @Volatile
    private var currentContent: String = ""
    @Volatile
    private var baselineContent: String = ""

    /**
     * 基线内容的 CRC32 校验和
     * 
     * 与字符串比较配合用于高效变更检测——当内容被编辑后又撤销回原始内容时，
     * 校验和可以快速确认内容与已保存基线匹配，无需先进行完整字符串比较即可清除修改标记。
     */
    @Volatile
    private var contentChecksum: Long = 0L

    /**
     * 上次实际与基线不同的内容变更时间戳
     * 
     * 用于修改标记防抖：我们等待 [DIRTY_DEBOUNCE_MS] 后才将 [isModified] 置为 true，
     * 避免用户短暂输入后撤销时修改标记快速切换。
     */
    @Volatile
    private var lastDirtyChangeMs: Long = 0L

    /**
     * 尚未提交到 [_isModified] 的待处理修改状态
     * 
     * 因为防抖窗口尚未到期。null 表示没有待处理更新。
     */
    @Volatile
    private var pendingDirty: Boolean? = null

    /**
     * 实际防抖时长。默认为 [DIRTY_DEBOUNCE_MS]。
     * 对测试可见——单元测试中设置为 0 使 [flushDirtyState] 立即提交，无需依赖时间等待。
     */
    @Volatile
    var dirtyDebounceMs: Long = DIRTY_DEBOUNCE_MS

    /**
     * 实时内容流，用于分屏预览订阅
     * 
     * 与 [uiState]（仅在加载/保存时重新发射）不同，此 Flow 跟踪每个防抖后的内容变更，
     * 以便 Markdown 预览可以实时同步，而无需强制全屏重组。
     */
    private val _liveContent = MutableStateFlow("")
    val liveContent: StateFlow<String> = _liveContent.asStateFlow()

    /** 当前文件是否为 Markdown 文件 */
    val isMarkdownFile: Boolean
        get() = (_uiState.value as? EditorUiState.Success)?.isMarkdownFile == true

    /** 当前文件是否支持预览（Markdown/HTML/PDF/Office 等） */
    val isPreviewable: Boolean
        get() = (_uiState.value as? EditorUiState.Success)?.isPreviewable == true

    /** 当前文件是否需要流式预览（超大 Markdown 文件） */
    val needsStreamingPreview: Boolean
        get() = (_uiState.value as? EditorUiState.Success)?.let {
            it.isMarkdownFile && it.fileSize > STREAMING_PREVIEW_FILE_THRESHOLD
        } ?: false

    // ------------------------------------------------------------------
    // 内容与修改标记
    // ------------------------------------------------------------------

    /**
     * 编辑器报告内容变更时调用（由 SoraEditorWrapper 以 150ms 防抖）
     * 
     * **算法步骤**：
     * 1. 快速路径：如果内容与当前内容相同，直接返回（减少不必要的 Flow 发射）
     * 2. 更新 currentContent 和 _liveContent（实时更新，不分防抖）
     * 3. 使用 CRC32 校验和判断内容是否恢复到已保存基线
     *    - CRC32 匹配：内容未实际改变（撤销回原始状态），修改标记应为 false
     *    - CRC32 不同：内容实际已改变，修改标记应为 true
     * 4. 设置 pendingDirty 为计算出的修改状态，记录时间戳
     * 5. 调用方应定期调用 [flushDirtyState] 提交待处理状态
     * 
     * **注意**：内容和 liveContent 立即更新（无防抖），确保分屏预览响应及时。
     * 仅修改标记采用防抖策略。
     */
    fun onContentChanged(content: String) {
        if (currentContent == content) return
        currentContent = content
        _liveContent.value = content

        val modified = if (!ContentChecksum.hasChanged(content, contentChecksum)) {
            false
        } else {
            true
        }

        pendingDirty = modified
        lastDirtyChangeMs = System.currentTimeMillis()
    }

    /**
     * 如果防抖窗口已到期，刷新任何待处理的修改状态
     * 
     * 应定期调用（例如从协程的短延迟循环或 UI 帧回调中）。
     * 如果没有待处理项或防抖窗口尚未到期，则提前返回。
     */
    fun flushDirtyState() {
        val pending = pendingDirty ?: return
        val elapsed = System.currentTimeMillis() - lastDirtyChangeMs
        if (elapsed < dirtyDebounceMs) return

        pendingDirty = null
        if (_isModified.value != pending) {
            _isModified.value = pending
            tabManager.getActiveTab()?.id?.let { tabId ->
                tabManager.updateTabModified(tabId, pending)
            }
        }
    }

    /**
     * 标记当前内容已保存（用于手动保存标记更新）
     */
    fun markSaved() {
        baselineContent = currentContent
        contentChecksum = ContentChecksum.crc32(currentContent)
        pendingDirty = null
        _isModified.value = false
        tabManager.getActiveTab()?.id?.let { tabId ->
            tabManager.updateTabModified(tabId, false)
        }
    }

    /**
     * 内容保存成功后调用，更新基线内容和校验和
     * 
     * @param savedContent 已保存的内容
     */
    fun onContentSaved(savedContent: String) {
        currentContent = savedContent
        _liveContent.value = savedContent
        baselineContent = savedContent
        contentChecksum = ContentChecksum.crc32(savedContent)
        pendingDirty = null
        _isModified.value = false
        tabManager.getActiveTab()?.id?.let { tabId ->
            tabManager.updateTabModified(tabId, false)
        }
    }

    /**
     * 获取当前编辑器内容
     * 
     * @return 当前文本内容
     */
    fun getCurrentContent(): String = currentContent

    // ------------------------------------------------------------------
    // 光标与滚动
    // ------------------------------------------------------------------

    /**
     * 光标位置变化时调用
     * 
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     */
    fun onCursorChanged(line: Int, column: Int) {
        _cursorPosition.value = CursorPosition(line, column)
    }

    /**
     * 滚动位置变化时调用
     * 
     * @param scrollX 水平滚动偏移
     * @param scrollY 垂直滚动偏移
     */
    fun onScrollChanged(scrollX: Int, scrollY: Int) {
        _scrollPosition.value = Pair(scrollX, scrollY)
    }

    // ------------------------------------------------------------------
    // 大纲
    // ------------------------------------------------------------------

    /**
     * 解析编辑器返回的大纲 JSON 数组
     * 
     * 使用 org.json 进行结构化安全解析（替代脆弱的正则表达式）。
     * 
     * @param json JSON 格式的大纲数组字符串
     */
    fun onOutlineItems(json: String) {
        try {
            val array = JSONArray(json)
            val items = mutableListOf<OutlineItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(OutlineItem(
                    name = obj.getString("name"),
                    line = obj.optInt("line", 1),
                    type = obj.optString("type", ""),
                ))
            }
            _outlineItems.value = items.toImmutableList()
        } catch (e: Exception) {
            android.util.Log.w("EditorStateManager", "Failed to parse outline JSON", e)
        }
    }

    // ------------------------------------------------------------------
    // 预览模式
    // ------------------------------------------------------------------

    /**
     * 切换 Markdown 预览模式
     * 
     * 循环顺序：所见即所得 → 编辑 → 预览 → 分屏 → 所见即所得
     * 仅在文件可预览时生效。
     */
    fun togglePreviewMode() {
        if (isPreviewable) {
            _markdownViewMode.value = when (_markdownViewMode.value) {
                MarkdownViewMode.WYSIWYG -> MarkdownViewMode.EDIT
                MarkdownViewMode.EDIT -> MarkdownViewMode.PREVIEW
                MarkdownViewMode.PREVIEW -> MarkdownViewMode.SPLIT
                // 大文件跳过 WYSIWYG 模式（会崩溃），直接回到 EDIT
                MarkdownViewMode.SPLIT -> if (_isLargeMarkdownFile.value) MarkdownViewMode.EDIT else MarkdownViewMode.WYSIWYG
            }
        }
    }

    /**
     * 设置指定的 Markdown 视图模式
     * 
     * @param mode 要设置的视图模式
     */
    fun setMarkdownViewMode(mode: MarkdownViewMode) {
        // 始终允许设置 EDIT 模式（大文件保护需要在 Loading 状态时设置）
        if (mode == MarkdownViewMode.EDIT || isPreviewable) {
            _markdownViewMode.value = mode
        }
    }

    /** 切换专注模式 */
    fun toggleFocusMode() { _isFocusMode.value = !_isFocusMode.value }
    
    /** 切换打字机模式 */
    fun toggleTypewriterMode() { _isTypewriterMode.value = !_isTypewriterMode.value }

    // ------------------------------------------------------------------
    // 状态快照
    // ------------------------------------------------------------------

    /**
     * 保存当前编辑器状态为可恢复的快照
     * 
     * @return 当前状态快照，UI 状态不是 Success 时返回 null
     */
    fun saveState(): EditorSavedState? {
        val state = _uiState.value
        if (state !is EditorUiState.Success) return null
        val pos = _cursorPosition.value
        return EditorSavedState(
            content = currentContent,
            cursorLine = pos.line,
            cursorColumn = pos.column,
            scrollX = _scrollPosition.value.first,
            scrollY = _scrollPosition.value.second,
            language = state.language,
            fileName = state.fileName,
            isReadOnly = state.isReadOnly,
            documentType = state.documentType,
        )
    }

    /**
     * 从保存的状态快照恢复编辑器状态
     * 
     * @param savedState 要恢复的状态快照
     */
    fun restoreState(savedState: EditorSavedState) {
        currentContent = savedState.content
        _liveContent.value = savedState.content
        baselineContent = savedState.content
        contentChecksum = ContentChecksum.crc32(savedState.content)
        pendingDirty = null
        _cursorPosition.value = CursorPosition(savedState.cursorLine, savedState.cursorColumn)
        _scrollPosition.value = Pair(savedState.scrollX, savedState.scrollY)
        _isModified.value = false
        val successState = EditorUiState.Success(
            content = savedState.content,
            language = savedState.language,
            fileName = savedState.fileName,
            isReadOnly = savedState.isReadOnly,
            documentType = savedState.documentType,
        )
        _uiState.value = successState
        // 恢复大文件标记：使用内容长度估算（字符数 ≈ 字节数）
        _isLargeMarkdownFile.value = successState.isMarkdownFile &&
            savedState.content.length.toLong() > WYSIWYG_FILE_SIZE_LIMIT
        _markdownViewMode.value = when {
            successState.isMarkdownFile && !_isLargeMarkdownFile.value -> MarkdownViewMode.WYSIWYG
            else -> MarkdownViewMode.EDIT
        }
    }

    /** 设置 UI 状态为加载中 */
    fun setLoading() { _uiState.value = EditorUiState.Loading }
    
    /**
     * 设置 UI 状态为错误
     * 
     * @param message 错误消息
     */
    fun setError(message: String) { _uiState.value = EditorUiState.Error(message) }

    /**
     * 加载内容到编辑器，更新所有相关状态
     * 
     * @param content 文件文本内容
     * @param language 语言标识符
     * @param fileName 文件名
     * @param isReadOnly 是否只读
     * @param documentType 文档类型（PDF/Word/Excel 等）
     * @param renderedHtml 预渲染的 HTML（用于 Office 文档）
     * @param fileSizeWarning 文件大小警告信息
     * @param fileSize 文件大小（字节）
     * @param restoreCursorLine 恢复光标的行号（1-based）
     * @param restoreCursorColumn 恢复光标的列号（1-based）
     * @param restoreScrollX 恢复的水平滚动位置
     * @param restoreScrollY 恢复的垂直滚动位置
     * @param isBinaryFile 是否为二进制文件
     * @param isTruncated 文件内容是否被截断
     */
    fun loadContent(
        content: String,
        language: String?,
        fileName: String,
        isReadOnly: Boolean = false,
        documentType: com.draftpeek.core.common.util.DocumentType? = null,
        renderedHtml: String? = null,
        fileSizeWarning: String? = null,
        fileSize: Long = 0,
        restoreCursorLine: Int = 1,
        restoreCursorColumn: Int = 1,
        restoreScrollX: Int = 0,
        restoreScrollY: Int = 0,
        isBinaryFile: Boolean = false,
        isTruncated: Boolean = false,
    ) {
        currentContent = content
        _liveContent.value = content
        baselineContent = content
        contentChecksum = ContentChecksum.crc32(content)
        pendingDirty = null
        _isModified.value = false
        _cursorPosition.value = CursorPosition(restoreCursorLine, restoreCursorColumn)
        _scrollPosition.value = Pair(restoreScrollX, restoreScrollY)
        val successState = EditorUiState.Success(
            content = content,
            language = language,
            fileName = fileName,
            isReadOnly = isReadOnly,
            documentType = documentType,
            renderedHtml = renderedHtml,
            fileSizeWarning = fileSizeWarning,
            fileSize = fileSize,
            isBinaryFile = isBinaryFile,
            isTruncated = isTruncated,
        )
        // 关键：先设置大文件标记和视图模式，再设置 UI 状态。
        // 如果先设 Success，Compose 可能在中间状态观察到 Success + WYSIWYG，
        // 从而渲染 MarkdownRichEditor 导致大文件崩溃。
        _isLargeMarkdownFile.value = successState.isMarkdownFile &&
            fileSize > WYSIWYG_FILE_SIZE_LIMIT
        if (_isLargeMarkdownFile.value) {
            _markdownViewMode.value = MarkdownViewMode.EDIT
        } else if (_markdownViewMode.value == MarkdownViewMode.WYSIWYG && !successState.isMarkdownFile) {
            _markdownViewMode.value = MarkdownViewMode.EDIT
        }
        // 最后设置 UI 状态，触发重组（此时大文件标记和模式已正确）
        _uiState.value = successState
    }

    /**
     * 更新 UI 状态以显示大文件加载进度
     * 
     * 用 [LoadingWithProgress] 替代通用的 [Loading] 状态，使用户能看到实时进度条，
     * 而不是冻结的加载指示器。
     * 
     * @param loadedBytes 已加载字节数
     * @param totalBytes 总字节数，-1 表示未知
     */
    fun setLoadingProgress(loadedBytes: Long, totalBytes: Long = -1) {
        _uiState.value = EditorUiState.LoadingWithProgress(
            loadedBytes = loadedBytes,
            totalBytes = totalBytes,
            progress = if (totalBytes > 0) loadedBytes.toFloat() / totalBytes else -1f,
        )
    }

    /**
     * 获取当前文件名
     * 
     * @return 文件名，UI 状态不是 Success 时返回 null
     */
    fun getFileName(): String? = (_uiState.value as? EditorUiState.Success)?.fileName
    
    /**
     * 获取当前语言
     * 
     * @return 语言标识符，UI 状态不是 Success 时返回 null
     */
    fun getLanguage(): String? = (_uiState.value as? EditorUiState.Success)?.language
}
