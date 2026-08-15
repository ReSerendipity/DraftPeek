package com.draftpeek.feature.editor.sora

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.annotation.UiThread
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.Content
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.draftpeek.feature.editor.ui.MarkdownFormatAction
import com.draftpeek.feature.editor.diagnostics.DiagnosticItem
import com.draftpeek.feature.editor.diagnostics.DiagnosticNavigationState
import com.draftpeek.feature.editor.diagnostics.moveToNext
import com.draftpeek.feature.editor.diagnostics.moveToPrevious
import com.draftpeek.feature.editor.data.CacheManager
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer

/**
 * Facade for the sora-editor ([CodeEditor]) that manages editor lifecycle, content loading,
 * cursor/scroll tracking, editor settings, diagnostics, and undo state.
 *
 * **Architecture**: This class delegates to specialized managers extracted for modularity:
 * - [themeManager] ([SoraThemeManager]): TextMate/TreeSitter initialization, theme management,
 *   color scheme application, and language selection.
 * - [searchManager] ([SoraSearchManager]): Text search, replace, word highlight, and
 *   cursor-based word retrieval.
 *
 * All public APIs remain unchanged — callers interact with [SoraEditorWrapper] directly,
 * which forwards to the appropriate manager internally.
 *
 * @param context The context used to create the [CodeEditor]. Typically an Activity Context
 *                for correct theme/window configuration. The wrapper manages its own lifecycle
 *                and does not leak the Activity.
 */
class SoraEditorWrapper(context: Context) {

    private val appContext = context.applicationContext

    /**
     * Ch10 Item 16 (P2): Token color cache — avoids repeated TextMate scope-chain
     * resolution on each render pass. Cleared automatically when the theme changes.
     */
    val tokenColorCache = TokenColorCache()

    @Volatile
    private var _released = false

    /**
     * Flag to suppress content-change callbacks during programmatic setText/loadContent
     * so that loading a file or restoring state does not falsely mark the document as
     * "modified" when the user has not actually edited anything.
     */
    @Volatile
    private var isProgrammaticLoad = false

    /**
     * Ch1 Item 18 (P3): Last edit location — stores the (line, column) position
     * (1-based) of the most recent content edit. Used by [goToLastEditLocation]
     * to jump the cursor back to the last edit point.
     */
    @Volatile
    private var lastEditPosition: Pair<Int, Int>? = null

    /**
     * Callback invoked when the editor content changes (debounced).
     * Set by the hosting screen to replace polling with event-driven updates.
     *
     * 使用 debounce 策略，避免每次按键都创建新字符串造成 GC 压力。
     */
    @Volatile
    var onContentChanged: ((String) -> Unit)? = null

    /**
     * Callback invoked when the editor content length changes (debounced).
     * Called with the new content length after [CONTENT_CHANGE_DEBOUNCE_MS] of inactivity.
     */
    @Volatile
    var onContentLengthChanged: ((Int) -> Unit)? = null

    /**
     * Callback invoked when the cursor position changes.
     * Called with (line, column) where both are 1-based.
     */
    @Volatile
    var onCursorChanged: ((Int, Int) -> Unit)? = null

    /**
     * Callback invoked when the editor scroll position changes.
     * Called with (scrollX, scrollY) in pixels.
     */
    @Volatile
    var onScrollChanged: ((Int, Int) -> Unit)? = null

    /** Debounce job for content change notifications */
    @Volatile
    private var contentSyncJob: kotlinx.coroutines.Job? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /** Tracks all pending postDelayed callbacks for cleanup on release */
    private val pendingRunnables = mutableListOf<Runnable>()

    /** Resets the programmatic-load flag after a short delay, allowing user edits to flow. */
    private val programmaticLoadResetRunnable = Runnable {
        isProgrammaticLoad = false
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught exception in editor coroutine", throwable)
    }

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + coroutineExceptionHandler)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    /**
     * 使用传入的Context创建CodeEditor。
     * 注意：这里使用传入的Context（通常是Activity Context）而非ApplicationContext，
     * 因为CodeEditor需要正确的主题、Window配置和LayoutInflater才能正常渲染。
     * wrapper通过remember(tab.id)管理生命周期，Composable离开composition时会被GC，
     * 不会造成内存泄漏。
     */
    @Volatile
    private var _editor: CodeEditor? = null
    val editor: CodeEditor
        get() = _editor ?: throw IllegalStateException(
            "SoraEditorWrapper failed to initialize. Check error StateFlow for details.",
            _error.value
        )

    private lateinit var formatHandler: MarkdownFormatHandler

    /** Theme/syntax highlight manager — handles TextMate, TreeSitter, themes, and color schemes. */
    private lateinit var themeManager: SoraThemeManager

    /** Search manager — handles text search, replace, and word highlighting. */
    private val searchManager: SoraSearchManager by lazy {
        SoraSearchManager(
            editorProvider = { if (_released) null else _editor },
            isReleased = { _released },
        )
    }

    @Volatile
    private var _lastLoadStartMs: Long = 0L

    init {
        val editorCreated = try {
            _editor = CodeEditor(context)
            formatHandler = MarkdownFormatHandler(editor)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal: Failed to create CodeEditor instance", t)
            _error.value = t
            _isReady.value = true
            false
        }

        if (editorCreated) {
            // Initialize the theme manager with editor access
            themeManager = SoraThemeManager(
                appContext = appContext,
                editorProvider = { if (_released) null else _editor },
                isReleased = { _released },
                initScope = initScope,
            )

            try {
                editor.apply {
                    isLineNumberEnabled = true
                    isWordwrap = false
                    isEditable = true
                    setTextSizePx(14 * appContext.resources.displayMetrics.density)

                    try {
                        props.stickyScroll = true
                        props.stickyScrollMaxLines = 3
                        props.stickyScrollAutoCollapse = true
                    } catch (e: IllegalAccessError) {
                        Log.e(TAG, "Failed to set initial sticky scroll props due to R8 field access issue", e)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set initial sticky scroll props", e)
                    }

                    // Set sensible defaults for editor behavior props
                    try {
                        props.autoIndent = true
                        props.enhancedHomeAndEnd = true
                        props.rowBasedHomeEnd = true
                        props.boldMatchingDelimiters = true
                        props.highlightMatchingDelimiters = true
                        props.deleteEmptyLineFast = true
                        props.deleteMultiSpaces = -1 // follow tab size
                        props.cursorLineBgOverlapBehavior = io.github.rosemoe.sora.widget.DirectAccessProps.CURSOR_LINE_BG_OVERLAP_MIXED
                        // Hard-wrap guide at column 120 (matches long-line diagnostic threshold).
                        props.hardwrapColumn = 120
                        // Better scroll behavior: single-axis fling/drag prevents diagonal scrolling
                        props.singleDirectionFling = true
                        props.singleDirectionDragging = true
                        props.scrollFling = true
                        // Mouse/trackpad support improvements
                        props.mouseWheelScrollFactor = 1.2f
                        props.fastScrollSensitivity = 5f
                        // Line number click places cursor at line start
                        props.actionWhenLineNumberClicked = io.github.rosemoe.sora.widget.DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set default editor props", e)
                    }

                    subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
                        if (_released) return@subscribeEvent
                        // Suppress callbacks during programmatic setText/loadContent to avoid
                        // false "modified" signals from line-ending normalization or initial load.
                        val isSetNewText = event.action == ContentChangeEvent.ACTION_SET_NEW_TEXT
                        if (isProgrammaticLoad && isSetNewText) {
                            return@subscribeEvent
                        }
                        try {
                            val newLength = _editor?.text?.length ?: -1
                            val action = when (event.action) {
                                ContentChangeEvent.ACTION_INSERT -> "INSERT"
                                ContentChangeEvent.ACTION_DELETE -> "DELETE"
                                ContentChangeEvent.ACTION_SET_NEW_TEXT -> "SET_NEW_TEXT"
                                else -> "UNKNOWN(${event.action})"
                            }
                            if (newLength == 0) {
                                Log.w(TAG, "ContentChangeEvent: action=$action, content became EMPTY! (changedLength=${event.changedText.length})")
                            } else {
                                val elapsed = System.currentTimeMillis() - _lastLoadStartMs
                                if (elapsed < 3000) {
                                    Log.d(TAG, "ContentChangeEvent: action=$action, newLength=$newLength (elapsed=${elapsed}ms after load)")
                                }
                            }
                        } catch (_: Throwable) {}
                        try {
                            val cursor = _editor?.cursor
                            if (cursor != null) {
                                lastEditPosition = (cursor.leftLine + 1) to (cursor.leftColumn + 1)
                            }
                        } catch (_: Exception) {
                        }
                        contentSyncJob?.cancel()
                        contentSyncJob = mainScope.launch {
                            delay(CONTENT_CHANGE_DEBOUNCE_MS)
                            if (!_released) {
                                val text = _editor?.text?.toString() ?: ""
                                onContentChanged?.invoke(text)
                                onContentLengthChanged?.invoke(text.length)
                            }
                        }
                    }

                    // Event-driven cursor position tracking (replaces polling in EditorScreen)
                    subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                        if (_released) return@subscribeEvent
                        try {
                            val left = event.left
                            val line = left.line + 1
                            val column = left.column + 1
                            onCursorChanged?.invoke(line, column)
                        } catch (_: Throwable) {}
                    }

                    // Event-driven scroll position tracking
                    subscribeEvent(ScrollEvent::class.java) { event, _ ->
                        if (_released) return@subscribeEvent
                        try {
                            onScrollChanged?.invoke(event.endX, event.endY)
                        } catch (_: Throwable) {}
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Non-fatal error during editor configuration, continuing with defaults", t)
            }

            initScope.launch {
                try {
                    themeManager.initialize()
                } catch (t: Throwable) {
                    Log.e(TAG, "TextMate initialization failed, falling back to plain text mode", t)
                } finally {
                    themeManager.completeInit()
                    _isReady.value = true
                }
            }
        }
    }

    /** Internal access to TextMate init state — callers use [loadContent] which guards access. */
    private val textMateInitialized: Boolean get() = ::themeManager.isInitialized && themeManager.textMateInitialized
    private val textMateInitFailed: Boolean get() = ::themeManager.isInitialized && themeManager.textMateInitFailed

    /**
     * 加载内容到编辑器并应用语法高亮。
     *
     * 如果 TextMate 尚未初始化，会先显示纯文本，后台初始化完成后异步应用高亮。
     * 对于超过 [LARGE_FILE_THRESHOLD_BYTES] 的文件，自动禁用语法高亮以防止 OOM 和卡顿。
     *
     * @param content 文本内容
     * @param language 语言标识（如 "kotlin"、"java"），null 表示纯文本
     * @param restoreLine 恢复光标到的行号（1-based），默认为 1
     */
    @UiThread
    fun loadContent(content: String, language: String?, restoreLine: Int = 1) {
        if (_released || _editor == null) {
            Log.w(TAG, "loadContent called on released/uninitialized wrapper")
            return
        }
        // Suppress onContentChanged callbacks during programmatic load to prevent false
        // "modified" state from initial setText / line-ending normalization / content guards.
        isProgrammaticLoad = true
        mainHandler.removeCallbacks(programmaticLoadResetRunnable)
        mainHandler.postDelayed(programmaticLoadResetRunnable, 600L)

        val fileSizeBytes = content.length.toLong()
        val effectiveLanguage = if (fileSizeBytes > LARGE_FILE_THRESHOLD_BYTES) {
            Log.w(TAG, "Large file detected (${fileSizeBytes / 1024}KB > ${LARGE_FILE_THRESHOLD_BYTES / 1024}KB), disabling syntax highlighting")
            null
        } else {
            language
        }

        val expectedContent = content
        _lastLoadStartMs = System.currentTimeMillis()

        fun verifyAndRestoreContent(phase: String): Boolean {
            return try {
                val currentText = editor.text?.toString()
                val currentLen = currentText?.length ?: -1
                val expectedLen = expectedContent.length
                if (currentText == null || (currentText.isEmpty() && expectedContent.isNotEmpty())) {
                    Log.w(TAG, "[$phase] Content EMPTY! (currentLength=$currentLen, expectedLength=$expectedLen), restoring...")
                    editor.setText(expectedContent)
                    postLayoutAndInvalidate()
                    false
                } else if (currentLen != expectedLen) {
                    Log.w(TAG, "[$phase] Content length mismatch: current=$currentLen, expected=$expectedLen (not restoring)")
                    true
                } else {
                    val elapsed = System.currentTimeMillis() - _lastLoadStartMs
                    if (elapsed < 3000) {
                        Log.d(TAG, "[$phase] Content OK: length=$currentLen (elapsed=${elapsed}ms)")
                    }
                    true
                }
            } catch (t: Throwable) {
                Log.e(TAG, "[$phase] Failed to verify content integrity", t)
                false
            }
        }

        fun scheduleContentGuard(delayMs: Long) {
            val runnable = object : Runnable {
                override fun run() {
                    try {
                        if (_released || _editor == null) return
                        verifyAndRestoreContent("Guard+${delayMs}ms")
                    } finally {
                        synchronized(pendingRunnables) {
                            pendingRunnables.remove(this)
                        }
                    }
                }
            }
            synchronized(pendingRunnables) {
                pendingRunnables.add(runnable)
            }
            mainHandler.postDelayed(runnable, delayMs)
        }

        try {
            editor.setText(expectedContent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to set initial text content", t)
            return
        }

        try {
            updateUndoLimit()
        } catch (_: Throwable) {}

        try {
            themeManager.applyColorScheme()
        } catch (t: Throwable) {
            Log.w(TAG, "applyColorScheme failed during loadContent", t)
        }

        postLayoutAndInvalidate()

        if (!themeManager.textMateInitialized) {
            if (themeManager.textMateInitFailed) {
                Log.w(TAG, "TextMate init previously failed, using plain text mode")
                try {
                    editor.setEditorLanguage(null)
                } catch (_: Throwable) {}
                verifyAndRestoreContent("TextMateFailed")
                postLayoutAndInvalidate()
                scheduleContentGuard(50)
                scheduleContentGuard(200)
                scheduleContentGuard(500)
                restoreCursorToLine(restoreLine)
                return
            }
            initScope.launch {
                themeManager.awaitInitAndApplyLanguage(
                    effectiveLanguage = effectiveLanguage,
onReady = {
postLayoutAndInvalidate()
try {
themeManager.applyColorScheme()
                        } catch (t: Throwable) {
                            Log.e(TAG, "applyColorScheme failed after TextMate init", t)
                        }
                        verifyAndRestoreContent("AfterInitLang")
                        scheduleContentGuard(50)
                        scheduleContentGuard(200)
                        scheduleContentGuard(500)
                        scheduleContentGuard(1000)
                        restoreCursorToLine(restoreLine)
                    },
                    onTimeout = {
                        verifyAndRestoreContent("InitTimeout")
                        postLayoutAndInvalidate()
                        scheduleContentGuard(50)
                        scheduleContentGuard(200)
                        scheduleContentGuard(500)
                        restoreCursorToLine(restoreLine)
                    },
                )
            }
        } else {
            try {
themeManager.setLanguageForContent(effectiveLanguage)
postLayoutAndInvalidate()
} catch (t: Throwable) {
Log.w(TAG, "setLanguageForContent failed", t)
try { editor.setEditorLanguage(null) } catch (_: Throwable) {}
}
try {
themeManager.applyColorScheme()
            } catch (t: Throwable) {
                Log.e(TAG, "applyColorScheme failed after setLanguage (2nd call)", t)
            }
            verifyAndRestoreContent("SyncLang")
            scheduleContentGuard(50)
            scheduleContentGuard(200)
            scheduleContentGuard(500)
            scheduleContentGuard(1000)
            restoreCursorToLine(restoreLine)
        }
    }

    /**
     * Restore the cursor to a specific line after content is loaded.
     * Uses a post to ensure the editor layout is complete before jumping.
     */
    private fun restoreCursorToLine(line: Int) {
        if (line > 1) {
            val zeroBasedLine = (line - 1).coerceAtLeast(0)
            val runnable = object : Runnable {
                override fun run() {
                    try {
                        if (_released || _editor == null) return
                        try {
                            val totalLines = editor.text?.lineCount ?: 1
                            val targetLine = zeroBasedLine.coerceAtMost(totalLines - 1)
                            editor.jumpToLine(targetLine)
                            requestLayoutAndInvalidate()
                        } catch (e: Exception) {
                            Log.w(TAG, "restoreCursorToLine failed: ${e.message}")
                        }
                    } finally {
                        synchronized(pendingRunnables) {
                            pendingRunnables.remove(this)
                        }
                    }
                }
            }
            synchronized(pendingRunnables) {
                pendingRunnables.add(runnable)
            }
            mainHandler.post(runnable)
        }
    }

    /** 获取编辑器当前文本内容。必须在主线程调用。 */
    @UiThread
    fun getContent(): String {
        if (_released || _editor == null) return ""
        return try {
            editor.text?.toString() ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "getContent failed", e)
            ""
        }
    }

    /** 设置编辑器主题（深色/浅色）。必须在主线程调用。 */
    @UiThread
    fun setTheme(dark: Boolean) {
        if (_released || _editor == null) return
        try {
            themeManager.setTheme(dark, tokenColorCache)
        } catch (e: Exception) {
            Log.w(TAG, "setTheme failed", e)
        }
    }

    /**
     * 设置编辑器主题为指定的自定义主题。必须在主线程调用。
     *
     * Supports both bundled themes (from assets) and custom themes
     * (imported by the user, stored in internal files directory).
     *
     * @param themeId The ID of the theme to apply (e.g., "nord", "dracula",
     *                or a custom theme ID like "custom-my-theme").
     *                If the theme is not yet loaded, it will be loaded from
     *                assets (bundled) or file (custom).
     */
    @UiThread
    fun setCustomTheme(themeId: String) {
        if (_released || _editor == null) return
        try {
            themeManager.setCustomTheme(themeId, tokenColorCache)
        } catch (e: Exception) {
            Log.w(TAG, "setCustomTheme failed", e)
        }
    }

    /** 设置字体大小（像素）。必须在主线程调用。 */
    @UiThread
    fun setFontSize(sizePx: Float) {
        try {
            editor.setTextSizePx(sizePx)
        } catch (t: Throwable) {
            Log.w(TAG, "setFontSize failed", t)
        }
    }

    /** 启用/禁用行号显示。必须在主线程调用。 */
    @UiThread
    fun setLineNumbers(enabled: Boolean) {
        try {
            editor.isLineNumberEnabled = enabled
        } catch (t: Throwable) {
            Log.w(TAG, "setLineNumbers failed", t)
        }
    }

    /** 启用/禁用自动换行。必须在主线程调用。 */
    @UiThread
    fun setWordWrap(wrap: Boolean) {
        try {
            editor.isWordwrap = wrap
        } catch (t: Throwable) {
            Log.w(TAG, "setWordWrap failed", t)
        }
    }

    /** 设置只读模式。必须在主线程调用。 */
    @UiThread
    fun setReadOnly(readOnly: Boolean) {
        try {
            editor.isEditable = !readOnly
        } catch (t: Throwable) {
            Log.w(TAG, "setReadOnly failed", t)
        }
    }

    /** 设置 Tab 宽度。必须在主线程调用。 */
    @UiThread
    fun setTabWidth(width: Int) {
        try {
            editor.tabWidth = width
        } catch (t: Throwable) {
            Log.w(TAG, "setTabWidth failed", t)
        }
    }

    fun setAutoIndent(enabled: Boolean) {
        try {
            editor.props.autoIndent = enabled
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "setAutoIndent failed due to R8 field access issue", e)
        } catch (t: Throwable) {
            Log.w(TAG, "setAutoIndent failed", t)
        }
    }

    fun setHighlightCurrentLine(enabled: Boolean) {
        try {
            editor.isHighlightCurrentLine = enabled
        } catch (t: Throwable) {
            Log.w(TAG, "setHighlightCurrentLine failed", t)
        }
    }

    fun setShowIndentGuides(enabled: Boolean) {
        // sora-editor 0.24.6 does not expose a public API for indent guide lines.
        // The editor renders block lines (structural indent guides) via internal
        // TextMate styling when the theme defines them. No direct toggle exists.
        // This setting is preserved for future sora-editor versions.
    }

    /**
     * Set the hard wrap marker column (a vertical guideline at the given column).
     * Set to 0 or negative to disable.
     */
    fun setHardWrapColumn(column: Int) {
        try {
            editor.props.hardwrapColumn = column.coerceAtLeast(0)
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "setHardWrapColumn failed due to R8 field access issue", e)
        } catch (t: Throwable) {
            Log.w(TAG, "setHardWrapColumn failed", t)
        }
    }

    /**
     * Enable/disable bold rendering for matching delimiter pairs.
     */
    fun setBoldMatchingDelimiters(enabled: Boolean) {
        try {
            editor.props.boldMatchingDelimiters = enabled
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "setBoldMatchingDelimiters failed due to R8 field access issue", e)
        } catch (t: Throwable) {
            Log.w(TAG, "setBoldMatchingDelimiters failed", t)
        }
    }

    fun setStickyScroll(enabled: Boolean) {
        try {
            editor.props.stickyScroll = enabled
            editor.props.stickyScrollMaxLines = 3
            editor.props.stickyScrollAutoCollapse = true
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "setStickyScroll failed due to R8 field access issue", e)
        } catch (t: Throwable) {
            Log.w(TAG, "setStickyScroll failed", t)
        }
    }

    /**
     * 启用/禁用缩略图（minimap）。
     *
     * Note: [io.github.rosemoe.sora.widget.CodeEditor.props.showMinimap] is
     * marked @Experimental in sora-editor. The feature may change in future versions.
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun setShowMinimap(enabled: Boolean) {
        try {
            editor.props.showMinimap = enabled
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "setShowMinimap failed due to R8 field access issue", e)
        } catch (t: Throwable) {
            Log.w(TAG, "setShowMinimap failed", t)
        }
    }

    /**
     * 启用/禁用自动配对补全。
     *
     * 当启用时，设置 SymbolPairMatch 使得输入开括号/引号时自动插入闭括号/引号，
     * 并在光标位于闭括号/引号前方时跳过而非重复插入。
     * 支持的配对: (), [], {}, "", '', ``。
     *
     * 当禁用时，清除 SymbolPairMatch，恢复为无自动配对行为。
     */
    @UiThread
    fun setAutoPairCompletion(enabled: Boolean) {
        try {
            val overridePairs = editor.props.overrideSymbolPairs
            overridePairs.removeAllPairs()
            if (enabled) {
                overridePairs.putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
                overridePairs.putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
                overridePairs.putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
                overridePairs.putPair('"', SymbolPairMatch.SymbolPair("\"", "\"",
                    object : SymbolPairMatch.SymbolPair.SymbolPairEx {
                        override fun shouldDoAutoSurround(content: Content): Boolean {
                            return content.cursor.isSelected
                        }
                    }
                ))
                overridePairs.putPair('\'', SymbolPairMatch.SymbolPair("'", "'",
                    object : SymbolPairMatch.SymbolPair.SymbolPairEx {
                        override fun shouldDoAutoSurround(content: Content): Boolean {
                            return content.cursor.isSelected
                        }
                    }
                ))
                overridePairs.putPair('`', SymbolPairMatch.SymbolPair("`", "`",
                    object : SymbolPairMatch.SymbolPair.SymbolPairEx {
                        override fun shouldDoAutoSurround(content: Content): Boolean {
                            return content.cursor.isSelected
                        }
                    }
                ))
                editor.props.symbolPairAutoCompletion = true
                Log.d(TAG, "Auto pair completion enabled")
            } else {
                editor.props.symbolPairAutoCompletion = false
                Log.d(TAG, "Auto pair completion disabled")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "setAutoPairCompletion failed", t)
            try {
                editor.props.symbolPairAutoCompletion = false
            } catch (_: Throwable) {}
        }
    }

    /** 设置字体族。必须在主线程调用。 */
    @UiThread
    fun setFontFamily(typefaceName: String?) {
        try {
            val typeface = when {
                typefaceName.isNullOrBlank() -> Typeface.MONOSPACE
                typefaceName.equals("serif", ignoreCase = true) -> Typeface.SERIF
                typefaceName.equals("sans-serif", ignoreCase = true) -> Typeface.SANS_SERIF
                typefaceName.equals("monospace", ignoreCase = true) -> Typeface.MONOSPACE
                else -> try {
                    Typeface.create(typefaceName, Typeface.NORMAL) ?: Typeface.MONOSPACE
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to create typeface for '$typefaceName', falling back to monospace", t)
                    Typeface.MONOSPACE
                }
            }
            editor.typefaceText = typeface
        } catch (t: Throwable) {
            Log.w(TAG, "setFontFamily failed", t)
        }
    }

    /**
     * Apply a Markdown format action to the editor content.
     * Handles wrapping selected text, inserting templates, and line prefixes.
     */
    @UiThread
    fun applyFormatAction(action: MarkdownFormatAction) {
        if (_released || _editor == null) return
        try {
            formatHandler.applyFormatAction(action)
        } catch (e: Exception) {
            Log.w(TAG, "applyFormatAction failed", e)
        }
    }

    /**
     * Get the text content of the current line (the line where the cursor is).
     *
     * @return 当前行的文本，失败时返回空字符串
     */
    @UiThread
    fun getCurrentLineText(): String {
        if (_released || _editor == null) return ""
        return try {
            formatHandler.getCurrentLineText()
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLineText failed", e)
            ""
        }
    }

    /**
     * Decrease indentation of the current line by removing [removeCount] leading spaces.
     */
    @UiThread
    fun decreaseIndent(removeCount: Int) {
        if (_released || _editor == null) return
        try {
            formatHandler.decreaseIndent(removeCount)
        } catch (e: Exception) {
            Log.w(TAG, "decreaseIndent failed", e)
        }
    }

    /**
     * Toggle the current line's list type:
     * - Unordered (- ) → Ordered (1. )
     * - Ordered (1. ) → Task (- [ ] )
     * - Task (- [ ] / - [x]) → Unordered (- )
     */
    @UiThread
    fun toggleListType() {
        if (_released || _editor == null) return
        try {
            formatHandler.toggleListType()
        } catch (e: Exception) {
            Log.w(TAG, "toggleListType failed", e)
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Safely request layout and invalidate the editor view.
     * Centralized to avoid repeated try/catch blocks.
     */
    private fun requestLayoutAndInvalidate() {
        try {
            _editor?.let { ed ->
                ed.requestLayout()
                ed.postInvalidate()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Execute a block on the editor only if it's usable (not released, not null).
     * Any exceptions are caught and logged.
     */
    private inline fun safeEditorCall(block: (CodeEditor) -> Unit) {
        if (_released || _editor == null) return
        try {
            block(editor)
        } catch (t: Throwable) {
            Log.w(TAG, "Editor operation failed", t)
        }
    }

    /**
     * Post a requestLayout + invalidate to the main thread.
     * Used after language/theme changes that require a re-render.
     * The Runnable is tracked in pendingRunnables to ensure cleanup on release().
     */
    private fun postLayoutAndInvalidate() {
        if (_released) return
        val runnable = object : Runnable {
            override fun run() {
                try {
                    requestLayoutAndInvalidate()
                } finally {
                    synchronized(pendingRunnables) {
                        pendingRunnables.remove(this)
                    }
                }
            }
        }
        synchronized(pendingRunnables) {
            if (_released) return
            pendingRunnables.add(runnable)
        }
        mainHandler.post(runnable)
    }

    /** 释放编辑器资源，取消所有后台协程。必须在主线程调用。 */
    @UiThread
    fun release() {
        if (_released) return
        _released = true
        try {
            contentSyncJob?.cancel()
            contentSyncJob = null
        } catch (_: Exception) {}
        try {
            mainScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel mainScope", e)
        }
        try {
            initScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel initScope", e)
        }
        // Remove all tracked pending Handler callbacks
        try {
            synchronized(pendingRunnables) {
                for (r in pendingRunnables) {
                    mainHandler.removeCallbacks(r)
                }
                pendingRunnables.clear()
            }
        } catch (_: Exception) {}
        // Safety net: remove ALL remaining callbacks and messages from the handler
        // to catch any untracked posts (e.g., postLayoutAndInvalidate)
        try {
            mainHandler.removeCallbacksAndMessages(null)
        } catch (_: Exception) {}
        try {
            _editor?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release editor", e)
        }
        _editor = null
        onContentChanged = null
        onContentLengthChanged = null
        onCursorChanged = null
        onScrollChanged = null
        lastEditPosition = null
    }

    /**
     * Alias for [release]. Provides semantic naming for lifecycle-aware callers.
     * Must be called on the UI thread.
     */
    @UiThread
    fun destroy() = release()

    /**
     * Force an immediate content sync, bypassing the debounce window.
     *
     * Call this before tab switches, saves, or app backgrounding to ensure
     * the latest editor content is reported to [onContentChanged] without
     * waiting for the 150ms debounce. This prevents data loss when the user
     * types and immediately switches tabs.
     *
     * Must be called on the main thread.
     */
    @UiThread
    fun forceSyncContent() {
        if (_released) return
        contentSyncJob?.cancel()
        try {
            val text = _editor?.text?.toString() ?: return
            onContentChanged?.invoke(text)
            onContentLengthChanged?.invoke(text.length)
        } catch (e: Exception) {
            Log.w(TAG, "forceSyncContent failed", e)
        }
    }

    /**
     * Get the current editor text synchronously, or null if the editor is not usable.
     * Must be called on the main thread.
     */
    @UiThread
    fun getCurrentText(): String? {
        if (_released) return null
        return try {
            _editor?.text?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the current cursor position (1-based line, 1-based column), or null if not available.
     * Must be called on the main thread.
     */
    @UiThread
    fun getCursorPosition(): Pair<Int, Int>? {
        if (_released) return null
        return try {
            val cursor = _editor?.cursor ?: return null
            (cursor.leftLine + 1) to (cursor.leftColumn + 1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if the editor is in a usable state (not released, not fatally errored).
     */
    fun isUsable(): Boolean = !_released && _editor != null

    /**
     * Release TreeSitter native resources.
     * Call when the application is terminating to free native memory.
     */
    fun releaseTreeSitter() {
        com.draftpeek.feature.editor.treesitter.TreeSitterLanguageProvider.release()
    }

    /** 撤销上一次操作。必须在主线程调用。 */
    @UiThread
    fun undo() {
        try {
            editor.undo()
        } catch (e: Exception) {
            Log.w(TAG, "undo failed", e)
        }
    }

    /** 重做上一次撤销的操作。必须在主线程调用。 */
    @UiThread
    fun redo() {
        try {
            editor.redo()
        } catch (e: Exception) {
            Log.w(TAG, "redo failed", e)
        }
    }

    /** 跳转到指定行（1-based）。必须在主线程调用。 */
    @UiThread
    fun goToLine(line: Int) {
        try {
            val zeroBasedLine = (line - 1).coerceAtLeast(0)
            editor.jumpToLine(zeroBasedLine)
        } catch (e: Exception) {
            Log.w(TAG, "goToLine failed", e)
        }
    }

    /**
     * Ch1 Item 18 (P3): Jump the cursor back to the last edit location.
     *
     * Navigates to the (line, column) stored in [lastEditPosition], which is
     * updated on every content change event. If no edit has been recorded yet,
     * this is a no-op.
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun goToLastEditLocation() {
        val position = lastEditPosition ?: run {
            Log.d(TAG, "goToLastEditLocation: no last edit position recorded")
            return
        }
        val (line, column) = position
        safeEditorCall { ed ->
            val totalLines = ed.text?.lineCount ?: 1
            val targetLine = (line - 1).coerceIn(0, totalLines - 1)
            ed.jumpToLine(targetLine)
            val lineEnd = ed.text?.getLine(targetLine)?.length ?: 0
            val targetColumn = (column - 1).coerceIn(0, lineEnd)
            ed.setSelection(targetLine, targetColumn)
            requestLayoutAndInvalidate()
            Log.d(TAG, "goToLastEditLocation: jumped to line=$line, column=$column")
        }
    }

    // ------------------------------------------------------------------
    // Search — delegates to [SoraSearchManager]
    // ------------------------------------------------------------------

    /**
     * 在编辑器中搜索 [query]。
     *
     * @param query 搜索文本
     * @param regex 是否将 query 作为正则表达式
     * @param matchCase 是否区分大小写（true = 区分大小写，false = 忽略大小写）
     * @param wholeWord 是否仅匹配完整单词
     * @return true 表示搜索成功启动
     */
    @UiThread
    fun search(query: String, regex: Boolean = false, matchCase: Boolean = false, wholeWord: Boolean = false): Boolean {
        return searchManager.search(query, regex, matchCase, wholeWord)
    }

    /**
     * 替换当前搜索匹配项。必须在主线程调用。
     */
    @UiThread
    fun replaceCurrent(replacement: String) {
        searchManager.replaceCurrent(replacement)
    }

    /**
     * 替换所有搜索匹配项。必须在主线程调用。
     */
    @UiThread
    fun replaceAll(query: String, replacement: String, regex: Boolean = false, matchCase: Boolean = false, wholeWord: Boolean = false) {
        searchManager.replaceAll(query, replacement, regex, matchCase, wholeWord)
    }

    /** 停止当前搜索。必须在主线程调用。 */
    @UiThread
    fun stopSearch() {
        searchManager.stopSearch()
    }

    /**
     * Jump to the next search match.
     * @return true if successfully moved to next match, false if no matches or at end
     */
    @UiThread
    fun gotoNext(): Boolean {
        return searchManager.gotoNext()
    }

    /**
     * Jump to the previous search match.
     * @return true if successfully moved to previous match, false if no matches or at start
     */
    @UiThread
    fun gotoPrevious(): Boolean {
        return searchManager.gotoPrevious()
    }

    // ------------------------------------------------------------------
    // Word highlight — delegates to [SoraSearchManager]
    // ------------------------------------------------------------------

    /**
     * Ch1 Item 21 (P3): Highlight all occurrences of [word] in the editor.
     *
     * Uses the sora-editor searcher API (the same engine used for Find) to
     * search for the word with case-insensitive, whole-word matching.
     * The matched ranges are rendered as highlighted backgrounds.
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun highlightWord(word: String) {
        searchManager.highlightWord(word)
    }

    /**
     * Ch1 Item 21 (P3): Clear the word highlight created by [highlightWord].
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun clearWordHighlight() {
        searchManager.clearWordHighlight()
    }

    /**
     * Ch1 Item 21 (P3): Get the word at the current cursor position.
     *
     * Returns null if the cursor is not on a word character.
     * Must be called on the UI thread.
     */
    @UiThread
    fun getWordAtCursor(): String? {
        return searchManager.getWordAtCursor()
    }

    /** 全选编辑器内容。必须在主线程调用。 */
    @UiThread
    fun selectAll() {
        try {
            editor.selectAll()
        } catch (e: Exception) {
            Log.w(TAG, "selectAll failed", e)
        }
    }

    /**
     * Get the current scroll position in pixels.
     * Must be called on the UI thread.
     */
    @UiThread
    fun getScrollPosition(): Pair<Int, Int> {
        return try {
            (editor.scrollX to editor.scrollY)
        } catch (e: Exception) {
            Log.w(TAG, "getScrollPosition failed", e)
            (0 to 0)
        }
    }

    /**
     * Restore scroll position to the given pixel coordinates.
     * Posts the scroll to the UI thread to ensure layout is complete.
     */
    @UiThread
    fun restoreScrollPosition(scrollX: Int, scrollY: Int) {
        if (scrollX == 0 && scrollY == 0) return
        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (_released || _editor == null) return
                    try {
                        editor.scrollTo(scrollX, scrollY)
                    } catch (e: Exception) {
                        Log.w(TAG, "restoreScrollPosition failed", e)
                    }
                } finally {
                    synchronized(pendingRunnables) {
                        pendingRunnables.remove(this)
                    }
                }
            }
        }
        synchronized(pendingRunnables) {
            pendingRunnables.add(runnable)
        }
        mainHandler.post(runnable)
    }

    /** 获取编辑器总行数。必须在主线程调用。 */
    @UiThread
    fun getTotalLines(): Int {
        return try {
            editor.text?.lineCount ?: 1
        } catch (e: Exception) {
            Log.w(TAG, "getTotalLines failed", e)
            1
        }
    }

    /**
     * 获取当前选区字符数。无选区时返回 0。
     * 必须在主线程调用。
     */
    @UiThread
    fun getSelectionCount(): Int {
        return try {
            val cursor = editor.cursor ?: return 0
            val text = editor.text ?: return 0
            if (cursor.leftLine == cursor.rightLine && cursor.leftColumn == cursor.rightColumn) return 0
            // Calculate character count of the selection
            val startIndex = text.getIndexer().getCharPosition(cursor.leftLine, cursor.leftColumn).index
            val endIndex = text.getIndexer().getCharPosition(cursor.rightLine, cursor.rightColumn).index
            kotlin.math.abs(endIndex - startIndex)
        } catch (e: Exception) {
            Log.w(TAG, "getSelectionCount failed", e)
            0
        }
    }

    // ------------------------------------------------------------------
    // Diagnostics (wavy underlines + tooltip)
    // ------------------------------------------------------------------

    /**
     * Apply diagnostic markers to the editor.
     *
     * Each [DiagnosticItem] produces a wavy underline at the given character range
     * with the corresponding severity colour (error / warning / typo).
     * Hovering / clicking the marker shows [DiagnosticItem.message] in a tooltip.
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun setDiagnostics(diagnostics: List<DiagnosticItem>) {
        val container = DiagnosticsContainer()
        for ((index, item) in diagnostics.withIndex()) {
            val region = DiagnosticRegion(
                item.startIndex,
                item.endIndex,
                item.severity,
                index.toLong(),
                DiagnosticDetail(item.message)
            )
            container.addDiagnostic(region)
        }
        editor.setDiagnostics(container)
    }

    /** Clear all diagnostic markers. Must be called on the UI thread. */
    @UiThread
    fun clearDiagnostics() {
        editor.setDiagnostics(null)
    }

    /**
     * Ch2#5: Jump the cursor to the diagnostic at [startIndex].
     *
     * Moves the selection cursor to the beginning of the diagnostic
     * range and centers the viewport on that position.
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun jumpToDiagnostic(startIndex: Int, endIndex: Int) {
        val content = editor.text ?: return
        if (startIndex < 0 || startIndex >= content.length) return
        // Convert character index to line/column position
        val position = content.indexer.getCharPosition(startIndex)
        val line = position.line
        val column = position.column
        // Set cursor to the start of the diagnostic
        editor.setSelection(line, column)
        // Ensure the position is visible
        editor.ensurePositionVisible(line, column)
    }

    /**
     * Ch2#5: Jump to the next diagnostic in the navigation state.
     *
     * Returns the updated [DiagnosticNavigationState] after moving.
     * The caller is responsible for calling [jumpToDiagnostic] with
     * the focused item's position.
     */
    fun navigateToNextDiagnostic(state: DiagnosticNavigationState): DiagnosticNavigationState {
        val next = state.moveToNext()
        val current: DiagnosticItem? = next.current
        if (current != null) {
            jumpToDiagnostic(current.startIndex, current.endIndex)
        }
        return next
    }

    /**
     * Ch2#5: Jump to the previous diagnostic in the navigation state.
     */
    fun navigateToPreviousDiagnostic(state: DiagnosticNavigationState): DiagnosticNavigationState {
        val prev = state.moveToPrevious()
        val current: DiagnosticItem? = prev.current
        if (current != null) {
            jumpToDiagnostic(current.startIndex, current.endIndex)
        }
        return prev
    }

    // ------------------------------------------------------------------
    // Undo stack persistence
    // ------------------------------------------------------------------

    /**
     * Save the undo stack to the cache directory via [CacheManager].
     *
     * @return true if saved successfully
     */
    fun saveUndoState(cacheManager: CacheManager, uri: String): Boolean {
        return try {
            val content = editor.text ?: return false
            val undoManager = content.undoManager
            cacheManager.saveUndoState(uri, undoManager)
        } catch (e: Exception) {
            Log.w(TAG, "saveUndoState failed", e)
            false
        }
    }

    /**
     * Restore the undo stack from the cache directory via [CacheManager].
     *
     * @return true if restored successfully
     */
    fun restoreUndoState(cacheManager: CacheManager, uri: String): Boolean {
        return try {
            val content = editor.text ?: return false
            val undoManager = cacheManager.restoreUndoState(uri) ?: return false
            content.undoManager = undoManager
            true
        } catch (e: Exception) {
            Log.w(TAG, "restoreUndoState failed", e)
            false
        }
    }

    /**
     * P0-6: Dynamically set the undo stack size limit based on current content size.
     *
     * Since sora-editor's [setMaxUndoStackSize] only supports count-based limits,
     * we compute an approximate operation count that corresponds to the 512 KB
     * size budget for the current file size.
     *
     * Heuristic: each undo entry stores the changed text region plus metadata.
     * For a file of size N bytes, the average change is roughly N/50 bytes,
     * so 512 KB / (N/50 + 100) gives a reasonable operation count, clamped
     * between 50 and [UNDO_MAX_OPERATIONS].
     */
    private fun updateUndoLimit() {
        val editor = this.editor
        val text = editor.text ?: return
        val contentBytes = text.length.toLong() * 2L // UTF-16 chars = 2 bytes each
        val estimatedOps = if (contentBytes > 0) {
            (UNDO_SIZE_LIMIT_BYTES / (contentBytes / 50 + 100)).toInt()
                .coerceIn(50, UNDO_MAX_OPERATIONS)
        } else {
            500
        }
        text.setMaxUndoStackSize(estimatedOps)
        Log.d(TAG, "Undo limit set to $estimatedOps operations (contentBytes=$contentBytes)")
    }

    companion object {
        private const val TAG = "SoraEditorWrapper"
        /** P0-6: Size-based undo stack limit (512 KB). Approximate tracking via content length. */
        private const val UNDO_SIZE_LIMIT_BYTES = 512L * 1024 // 512 KB
        /** P0-6: Approximate overhead factor per undo operation (content + metadata). */
        private const val UNDO_OVERHEAD_FACTOR = 1.5
        /** P0-6: Maximum number of undo operations (fallback cap). */
        private const val UNDO_MAX_OPERATIONS = 1000
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 150L
        /** File size threshold for disabling syntax highlighting (1 MB). */
        private const val LARGE_FILE_THRESHOLD_BYTES = 1_000_000L
    }
}
