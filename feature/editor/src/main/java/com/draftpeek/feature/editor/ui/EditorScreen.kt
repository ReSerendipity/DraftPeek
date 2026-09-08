/**
 * 编辑器主界面模块。
 *
 * 提供代码编辑、Markdown预览、多文档查看（PDF/Office/媒体）、多标签页管理、
 * 搜索替换、快捷设置、代码片段等核心编辑功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.model.TabId
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.common.util.FileUtils
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandFilterChip
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandSwitch
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.component.rememberHapticController
import com.draftpeek.core.ui.composition.isFeatureEnabled
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.layout.SplitOrientation
import com.draftpeek.core.ui.layout.SplitPane
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.EditorStatusBarStyle
import com.draftpeek.core.ui.theme.FontOptions
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.input.KeyboardShortcutHandler
import com.draftpeek.feature.editor.model.EditorMessage
import com.draftpeek.feature.editor.model.EditorTab
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.sora.SoraEditorWrapper
import com.draftpeek.feature.editor.treesitter.TreeSitterLanguageProvider
import com.draftpeek.feature.editor.viewmodel.CursorPosition
import com.draftpeek.feature.editor.viewmodel.EditorViewModel
import com.draftpeek.feature.settings.model.EditorSettings
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private const val TAG = "EditorScreen"

/**
 * 编辑器显示模式。
 *
 * - CODE: 代码编辑模式，使用 sora-editor 进行文本编辑
 * - DOCUMENT: 文档查看模式，用于 PDF/Office/媒体等非文本文件的预览
 */
enum class EditorMode { CODE, DOCUMENT }

/**
 * 搜索面板页签模式。
 *
 * 统一管理查找 / 替换 / 跳转到行三种搜索操作，
 * 替代原来三个独立对话框的布尔状态。
 */
enum class SearchPanelMode { FIND, REPLACE, GOTO }

/**
 * 编辑器主界面 Composable。
 *
 * 提供完整的代码编辑体验，包括：
 * - 多标签页管理与切换
 * - 代码编辑、撤销/重做、搜索替换
 * - Markdown 预览（编辑/预览/分屏/富文本模式）
 * - PDF/Office/媒体文件预览
 * - 快捷键支持、命令面板
 * - 编码选择、快速设置
 * - 代码片段保存与插入
 *
 * @param onNavigateUp 返回上一级界面的回调
 * @param layoutMode 布局模式（紧凑/展开）
 * @param foldInfo 折叠屏设备信息
 * @param darkTheme 是否使用深色主题
 * @param fileUri 要打开的文件 URI（可选）
 * @param viewModel 编辑器 ViewModel 实例
 * @param settingsViewModel 设置 ViewModel 实例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun EditorScreen(
    onNavigateUp: () -> Unit,
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    foldInfo: FoldInfo = FoldInfo(),
    darkTheme: Boolean = false,
    fileUri: String? = null,
    onNavigateToTerminal: (String?) -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var retryKey by remember { mutableIntStateOf(0) }
    val wrapper = remember(retryKey) { SoraEditorWrapper(context) }
    val wrapperError by wrapper.error.collectAsStateWithLifecycle()
    val wrapperReady by wrapper.isReady.collectAsStateWithLifecycle()
    val hapticController = rememberHapticController()

    var loadedFileKey by remember { mutableStateOf<String?>(null) }

    // Reset loaded file key when retry is triggered so content reloads into new wrapper
    LaunchedEffect(retryKey) {
        if (retryKey > 0) {
            loadedFileKey = null
        }
    }

    // When a fileUri is provided, load it and reset the loaded-file key so
    // the editor content is always refreshed on file switches.
    LaunchedEffect(fileUri) {
        if (fileUri != null) {
            loadedFileKey = null
            viewModel.loadFileFromUri(fileUri)
        }
    }

    // Sync Tree-sitter feature flag to the language provider
    val treeSitterEnabled = isFeatureEnabled(FeatureFlag.TREE_SITTER)
    LaunchedEffect(treeSitterEnabled) {
        TreeSitterLanguageProvider.enabled = treeSitterEnabled
    }

    // Markdown feature flags
    val markdownWysiwygEnabled = isFeatureEnabled(FeatureFlag.MARKDOWN_WYSIWYG)
    val markdownEditorEnabled = isFeatureEnabled(FeatureFlag.MARKDOWN_EDITOR)
    val commonmarkParserEnabled = isFeatureEnabled(FeatureFlag.COMMONMARK_PARSER)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isModified by viewModel.isModified.collectAsStateWithLifecycle()
    val cursorPosition by viewModel.cursorPosition.collectAsStateWithLifecycle()
    val markdownViewMode by viewModel.markdownViewMode.collectAsStateWithLifecycle()
    val isLargeMarkdownFile by viewModel.isLargeMarkdownFile.collectAsStateWithLifecycle()

    // Auto-exit WYSIWYG mode when feature flag is disabled
    LaunchedEffect(markdownWysiwygEnabled, markdownViewMode) {
        if (!markdownWysiwygEnabled && markdownViewMode == MarkdownViewMode.WYSIWYG) {
            viewModel.setMarkdownViewMode(MarkdownViewMode.EDIT)
        }
    }

    // 安全网：大 Markdown 文件绝不能进入 WYSIWYG 模式（会因 Constraints 溢出崩溃）
    LaunchedEffect(isLargeMarkdownFile, markdownViewMode) {
        if (isLargeMarkdownFile && markdownViewMode == MarkdownViewMode.WYSIWYG) {
            viewModel.setMarkdownViewMode(MarkdownViewMode.EDIT)
        }
    }

    // Sync CommonMark parser flag
    LaunchedEffect(commonmarkParserEnabled) {
        com.draftpeek.core.common.util.CommonMarkParser.enabled = commonmarkParserEnabled
    }

    // Derived: only triggers recomposition when the boolean result changes,
    // not on every markdownViewMode emission that stays in the same state.
    val isPreviewMode by remember {
        derivedStateOf { markdownViewMode != MarkdownViewMode.EDIT }
    }
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val showEncodingDialog by viewModel.showEncodingDialog.collectAsStateWithLifecycle()
    val showSaveEncodingDialog by viewModel.showSaveEncodingDialog.collectAsStateWithLifecycle()
    val detectedEncoding by viewModel.detectedEncoding.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var goToLineInput by remember { mutableStateOf("") }

    // Unified search panel state (replaces showSearchDialog/showReplaceDialog/showGoToLineDialog)
    var searchPanelMode by remember { mutableStateOf<SearchPanelMode?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var replaceText by rememberSaveable { mutableStateOf("") }
    var searchRegex by rememberSaveable { mutableStateOf(false) }
    var searchMatchCase by rememberSaveable { mutableStateOf(false) }
    var searchWholeWord by rememberSaveable { mutableStateOf(false) }

    // Status bar state (§3.4: VSCode-style status bar)
    var selectionCount by remember { mutableIntStateOf(0) }
    var isReadOnlyOverride by remember { mutableStateOf(false) }
    var lineEndingOverride by remember { mutableStateOf<String?>(null) }

    // Command palette state
    var showCommandPalette by remember { mutableStateOf(false) }

    // Ch1 Item 19 (P3): Symbol panel state
    var showSymbolPanel by remember { mutableStateOf(false) }

    // Ch6 Item 16 (P3): Markdown cheat sheet state
    var showMarkdownCheatSheet by remember { mutableStateOf(false) }

    // Ch1 Item 21 (P3): Word highlight state
    var highlightedWord by remember { mutableStateOf("") }

    // Editor mode state (CODE vs DOCUMENT)
    var editorMode by remember { mutableStateOf(EditorMode.CODE) }

    // 合并多个 LaunchedEffect(uiState) 块以减少冗余重组

    // Snippet-related dialog state
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSaveSnippetDialog by remember { mutableStateOf(false) }
    var showInsertSnippetDialog by remember { mutableStateOf(false) }
    var snippetList by remember { mutableStateOf<List<Snippet>>(emptyList()) }

    // Quick settings state
    var showQuickSettings by remember { mutableStateOf(false) }
    val quickSettingsState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Delete file state
    var showDeleteFileDialog by remember { mutableStateOf(false) }
    var showBacklinksDialog by remember { mutableStateOf(false) }

    // Export file state
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (!SecurityGate.isOperationAllowed()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.security_operation_restricted),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@let
                }
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(wrapper.getContent().toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.editor_export_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.editor_export_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val isInternalFile by remember(uiState, viewModel.currentUriString) {
        derivedStateOf {
            val state = uiState as? EditorUiState.Success
            state?.let { AppFileManager.isInternalUri(viewModel.currentUriString) } ?: false
        }
    }

    // Keyboard shortcut handler for external keyboards
    val shortcutHandler = remember(wrapper) {
        KeyboardShortcutHandler(
            onSave = { if (wrapper.isUsable()) viewModel.saveFile(wrapper.getContent()) },
            onUndo = { if (wrapper.isUsable()) wrapper.undo() },
            onRedo = { if (wrapper.isUsable()) wrapper.redo() },
            onOpenSearch = { searchPanelMode = SearchPanelMode.FIND },
            onOpenReplace = { searchPanelMode = SearchPanelMode.REPLACE },
            onGoToLine = { searchPanelMode = SearchPanelMode.GOTO },
            onSelectAll = { if (wrapper.isUsable()) wrapper.selectAll() },
            onOpenCommandPalette = { showCommandPalette = true },
            onGoToLastEditLocation = { if (wrapper.isUsable()) wrapper.goToLastEditLocation() },
            onFindNext = { if (wrapper.isUsable()) wrapper.gotoNext() },
            onFindPrevious = { if (wrapper.isUsable()) wrapper.gotoPrevious() }
        )
    }

    // Pager state for HorizontalPager tab switching
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0),
        pageCount = { tabs.size.coerceAtLeast(1) }
    )
    val pagerScope = rememberCoroutineScope()

    // Sync pager page with active tab changes from ViewModel
    LaunchedEffect(activeTabId, tabs) {
        val targetPage = tabs.indexOfFirst { it.id == activeTabId }
        if (targetPage >= 0 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync ViewModel active tab when pager settles on a new page
    LaunchedEffect(pagerState.currentPage) {
        val currentTabId = tabs.getOrNull(pagerState.currentPage)?.id
        if (currentTabId != null && currentTabId != activeTabId) {
            loadedFileKey = null
            // Force-sync content before switching tabs to capture any unsaved edits
            // that haven't been reported through the debounced callback yet.
            if (wrapper.isUsable()) {
                wrapper.forceSyncContent()
            }
            viewModel.switchToTab(currentTabId)
        }
    }

    // Release the editor when the composable leaves composition OR when wrapper
    // is recreated (e.g., on retry). Also clears callbacks to prevent leaks.
    DisposableEffect(wrapper) {
        onDispose {
            wrapper.onContentChanged = null
            wrapper.onContentLengthChanged = null
            wrapper.onCursorChanged = null
            wrapper.onScrollChanged = null
            wrapper.release()
        }
    }

    // If wrapper has a fatal error, show error UI instead of normal editor.
    // This check takes precedence over other UI rendering.
    if (wrapperError != null) {
        EditorInitErrorScreen(
            error = wrapperError!!,
            onNavigateUp = onNavigateUp,
            onRetry = { retryKey++ }
        )
        return
    }

    // Collect one-shot messages from ViewModel and display as Toast.
    // The ViewModel emits structured EditorMessage types; the UI layer resolves
    // them to localized strings using LocalContext.
    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { message ->
            val displayText = when (message) {
                is EditorMessage.SaveSuccess -> {
                    hapticController.confirm()
                    context.getString(R.string.editor_save_success)
                }
                is EditorMessage.SaveInProgress -> context.getString(R.string.editor_saving_please_wait)
                is EditorMessage.SaveFailed -> {
                    hapticController.reject()
                    val appError = message.error
                    context.getString(appError.messageResId)
                }
                is EditorMessage.LoadFailed -> {
                    hapticController.reject()
                    val appError = message.error
                    context.getString(appError.messageResId)
                }
                is EditorMessage.Info -> context.getString(message.messageResId)
            }
            Toast.makeText(context, displayText, Toast.LENGTH_SHORT).show()
        }
    }

    // Sync theme with the app — guarded by isUsable() to avoid crashes during init failure.
    LaunchedEffect(darkTheme) {
        if (wrapper.isUsable()) {
            try {
                wrapper.setTheme(darkTheme)
            } catch (_: Exception) {}
        }
    }

    // Sync settings to wrapper — guarded by isUsable().
    LaunchedEffect(settings.fontSize, settings.lineWrapping, settings.showLineNumbers, settings.tabWidth, settings.autoIndent, settings.highlightCurrentLine, settings.showIndentGuides, settings.showMinimap, settings.stickyScroll, settings.autoPairCompletion, settings.codeFontFamilyId) {
        if (wrapper.isUsable()) {
            try {
                wrapper.setFontSize(settings.fontSize * context.resources.displayMetrics.density)
                wrapper.setWordWrap(settings.lineWrapping)
                wrapper.setLineNumbers(settings.showLineNumbers)
                wrapper.setTabWidth(settings.tabWidth)
                wrapper.setAutoIndent(settings.autoIndent)
                wrapper.setHighlightCurrentLine(settings.highlightCurrentLine)
                wrapper.setShowIndentGuides(settings.showIndentGuides)
                wrapper.setShowMinimap(settings.showMinimap)
                wrapper.setStickyScroll(settings.stickyScroll)
                wrapper.setAutoPairCompletion(settings.autoPairCompletion)
                val codeFont = FontOptions.getCodeFontById(settings.codeFontFamilyId)
                wrapper.setFontFamily(codeFont.typefaceName)
            } catch (_: Exception) {}
        }
    }

    // Sync content changes + auto-switch mode + load content — all in one LaunchedEffect
    // Keyed on both uiState and retryKey so content reloads when wrapper is recreated (retry)
    LaunchedEffect(uiState, retryKey) {
        val successState = uiState as? EditorUiState.Success ?: return@LaunchedEffect
        if (!wrapper.isUsable()) return@LaunchedEffect
        // 1. Capture restore positions BEFORE setting up callbacks and loading content,
        //    to avoid race conditions where initial scroll-to-(0,0) overwrites saved state.
        val restoreLine = cursorPosition.line
        val (restoreScrollX, restoreScrollY) = viewModel.scrollPosition.value
        // 2. Set up event-driven callbacks (replaces polling)
        wrapper.onContentChanged = { content ->
            viewModel.onContentChanged(content)
        }
        wrapper.onContentLengthChanged = { length ->
            viewModel.onContentLengthChanged(length)
        }
        wrapper.onCursorChanged = { line, column ->
            viewModel.onCursorChanged(line, column)
            selectionCount = wrapper.getSelectionCount()
        }
        wrapper.onScrollChanged = { scrollX, scrollY ->
            viewModel.onScrollChanged(scrollX, scrollY)
        }
        // 3. Auto-switch to DOCUMENT mode when opening Office/PDF/Media files
        val isNonTextFile = successState.isPdf ||
            successState.isOfficeDocument ||
            successState.isMediaFile ||
            successState.fileName.lowercase().let {
                it.endsWith(".pdf") ||
                    it.endsWith(".doc") ||
                    it.endsWith(".docx") ||
                    it.endsWith(".xls") ||
                    it.endsWith(".xlsx") ||
                    it.endsWith(".ppt") ||
                    it.endsWith(".pptx")
            }
        if (isNonTextFile) {
            editorMode = EditorMode.DOCUMENT
        } else {
            editorMode = EditorMode.CODE
        }
        // 4. Load file content into the native editor (skip for non-text files to avoid garbled display)
        val key = "${viewModel.currentUriString}\u0000${successState.content.length}\u0000${successState.isReadOnly}"
        if (loadedFileKey != key) {
            try {
                wrapper.setReadOnly(successState.isReadOnly)
                if (!isNonTextFile) {
                    wrapper.loadContent(successState.content, successState.language, restoreLine)
                    // Restore scroll position (uses editor.post internally to run after layout pass,
                    // overriding the initial scroll-to-(0,0) triggered by setText).
                    if (restoreScrollY > 0 || restoreScrollX > 0) {
                        try {
                            wrapper.restoreScrollPosition(restoreScrollX, restoreScrollY)
                        } catch (_: Exception) {}
                    }
                }
                loadedFileKey = key
            } catch (_: Exception) {}
        }
    }

    // P1-7: Collect diagnostic items from ViewModel and apply wavy underlines
    val diagnosticItems by viewModel.diagnostics.collectAsStateWithLifecycle()
    LaunchedEffect(diagnosticItems) {
        if (wrapper.isUsable()) {
            try {
                wrapper.setDiagnostics(diagnosticItems)
            } catch (_: Exception) {}
        }
    }

    // Persist / restore undo stack on lifecycle pause / resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.onEditorShown()
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onEditorHidden()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    val uri = viewModel.currentUriString
                    if (uri.isNotEmpty() && wrapper.isUsable()) {
                        try {
                            wrapper.saveUndoState(viewModel.cacheManager, uri)
                        } catch (_: Exception) {}
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    val uri = viewModel.currentUriString
                    if (uri.isNotEmpty() && wrapper.isUsable()) {
                        try {
                            wrapper.restoreUndoState(viewModel.cacheManager, uri)
                        } catch (_: Exception) {}
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Cursor position is now updated via event-driven callback (onCursorChanged in SoraEditorWrapper)
    // instead of polling every 300ms. This eliminates unnecessary wake-ups and GC pressure.

    // Ch1 Item 21 (P3): Word highlight on double-tap / selection.
    // When the cursor position changes and the editor has a selection,
    // highlight all occurrences of the selected word. When the selection
    // is cleared, clear the highlight.
    LaunchedEffect(uiState, cursorPosition) {
        if (uiState !is EditorUiState.Success) return@LaunchedEffect
        if (!wrapper.isUsable()) return@LaunchedEffect
        try {
            val cursor = wrapper.editor.cursor
            if (cursor != null && cursor.isSelected) {
                val selectedText = wrapper.editor.text?.substring(
                    cursor.left,
                    cursor.right
                )?.toString()
                if (!selectedText.isNullOrBlank() && selectedText.all { it.isLetterOrDigit() || it == '_' }) {
                    if (selectedText != highlightedWord) {
                        highlightedWord = selectedText
                        wrapper.highlightWord(selectedText)
                    }
                } else {
                    if (highlightedWord.isNotEmpty()) {
                        highlightedWord = ""
                        wrapper.clearWordHighlight()
                    }
                }
            } else if (highlightedWord.isNotEmpty()) {
                highlightedWord = ""
                wrapper.clearWordHighlight()
            }
        } catch (_: Exception) {}
    }

    // Intercept back button: modified -> confirm dialog; multi-tab -> close current tab; else exit.
    BackHandler(enabled = true) {
        when {
            isModified -> showExitDialog = true
            tabs.size > 1 -> {
                val currentTabId = activeTabId ?: return@BackHandler
                val closed = viewModel.closeTab(currentTabId)
                if (closed) {
                    loadedFileKey = null
                } else {
                    onNavigateUp()
                }
            }
            else -> onNavigateUp()
        }
    }

    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    // Unsaved changes dialog.
    if (showExitDialog) {
        BrandDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = stringResource(R.string.editor_unsaved_changes)) },
            content = {
                if (isSaving) {
                    Text(text = stringResource(R.string.editor_saving_file))
                } else {
                    Text(text = stringResource(R.string.editor_unsaved_exit_message))
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = if (isSaving) {
                        stringResource(
                            R.string.editor_wait_save
                        )
                    } else {
                        stringResource(R.string.editor_discard_changes)
                    },
                    onClick = {
                        if (isSaving) {
                            // Wait for save to complete then exit
                            showExitDialog = false
                        } else {
                            showExitDialog = false
                            if (tabs.size > 1) {
                                val currentTabId = activeTabId ?: return@BrandFilledButton
                                val closed = viewModel.closeTab(currentTabId)
                                if (closed) {
                                    loadedFileKey = null
                                } else {
                                    onNavigateUp()
                                }
                            } else {
                                onNavigateUp()
                            }
                        }
                    },
                    enabled = !isSaving
                )
            },
            dismissButton = {
                if (!isSaving) {
                    BrandOutlinedButton(text = stringResource(R.string.editor_cancel), onClick = {
                        showExitDialog =
                            false
                    })
                }
            }
        )
    }

    // Unified search panel (Find / Replace / Go to Line)
    val currentSearchMode = searchPanelMode
    if (currentSearchMode != null) {
        EditorSearchPanel(
            mode = currentSearchMode,
            onModeChange = { searchPanelMode = it },
            onDismiss = {
                wrapper.stopSearch()
                searchPanelMode = null
                searchQuery = ""
                replaceText = ""
                searchRegex = false
                searchMatchCase = false
                searchWholeWord = false
                goToLineInput = ""
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            replaceText = replaceText,
            onReplaceTextChange = { replaceText = it },
            searchRegex = searchRegex,
            onSearchRegexChange = { searchRegex = it },
            searchMatchCase = searchMatchCase,
            onSearchMatchCaseChange = { searchMatchCase = it },
            searchWholeWord = searchWholeWord,
            onSearchWholeWordChange = { searchWholeWord = it },
            goToLineInput = goToLineInput,
            onGoToLineInputChange = { goToLineInput = it.filter { c -> c.isDigit() } },
            wrapper = wrapper,
            context = context
        )
    }

    // Save as Snippet dialog
    if (showSaveSnippetDialog) {
        var snippetTitle by remember { mutableStateOf(viewModel.getFileName() ?: "") }
        var snippetContent by remember { mutableStateOf("") }
        var snippetCategory by remember { mutableStateOf("") }
        val uncategorizedLabel = stringResource(R.string.editor_uncategorized)

        LaunchedEffect(Unit) {
            snippetContent = wrapper.getContent()
        }

        BrandDialog(
            onDismissRequest = { showSaveSnippetDialog = false },
            title = { Text(text = stringResource(R.string.editor_save_as_snippet)) },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandOutlinedTextField(
                        value = snippetTitle,
                        onValueChange = { snippetTitle = it },
                        label = { Text(text = stringResource(R.string.editor_snippet_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BrandOutlinedTextField(
                        value = snippetContent,
                        onValueChange = { snippetContent = it },
                        label = { Text(text = stringResource(R.string.editor_snippet_content)) },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BrandOutlinedTextField(
                        value = snippetCategory,
                        onValueChange = { snippetCategory = it },
                        label = { Text(text = stringResource(R.string.editor_snippet_category)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.editor_save),
                    onClick = {
                        if (snippetTitle.isNotBlank() && snippetContent.isNotBlank()) {
                            viewModel.saveAsSnippet(
                                snippetTitle.trim(),
                                snippetContent,
                                viewModel.getLanguage(),
                                snippetCategory.ifBlank { uncategorizedLabel }
                            )
                        }
                        showSaveSnippetDialog = false
                    },
                    enabled = snippetTitle.isNotBlank() && snippetContent.isNotBlank()
                )
            },
            dismissButton = {
                BrandOutlinedButton(text = stringResource(R.string.editor_cancel), onClick = {
                    showSaveSnippetDialog =
                        false
                })
            }
        )
    }

    // Insert Snippet dialog
    if (showInsertSnippetDialog) {
        LaunchedEffect(Unit) {
            viewModel.getAllSnippets().collect { snippetList = it }
        }
        BrandDialog(
            onDismissRequest = { showInsertSnippetDialog = false },
            title = { Text(text = stringResource(R.string.editor_insert_snippet)) },
            content = {
                if (snippetList.isEmpty()) {
                    Text(text = stringResource(R.string.editor_no_snippets))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snippetList.forEach { snippet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentContent = wrapper.getContent()
                                        val newContent = currentContent + "\n" + snippet.content
                                        wrapper.loadContent(
                                            newContent,
                                            (uiState as? EditorUiState.Success)?.language
                                        )
                                        showInsertSnippetDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = PrototypeTokens.elevated
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = snippet.title,
                                            style = DraftPeekTypography.titleSmall.copy(color = PrototypeTokens.fg)
                                        )
                                        snippet.language?.let { lang ->
                                            Text(
                                                text = lang,
                                                style = DraftPeekTypography.labelSmall,
                                                color = PrototypeTokens.accent
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = snippet.content,
                                        style = DraftPeekTypography.bodySmall,
                                        color = PrototypeTokens.muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                BrandFilledButton(text = stringResource(R.string.editor_close), onClick = {
                    showInsertSnippetDialog =
                        false
                })
            }
        )
    }

    // Command Palette
    if (showCommandPalette) {
        val commandActions = EditorCommandActions(
            onSave = { if (wrapper.isUsable()) viewModel.saveFile(wrapper.getContent()) },
            onNewFile = { /* new file not directly available from editor */ },
            onOpenFile = { /* open file not directly available from editor */ },
            onExport = {
                val successState = uiState as? EditorUiState.Success
                val fileName = successState?.fileName ?: "file"
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                exportLauncher.launch(intent)
            },
            onUndo = { if (wrapper.isUsable()) wrapper.undo() },
            onRedo = { if (wrapper.isUsable()) wrapper.redo() },
            onFind = {
                showCommandPalette = false
                searchPanelMode = SearchPanelMode.FIND
            },
            onReplace = {
                showCommandPalette = false
                searchPanelMode = SearchPanelMode.REPLACE
            },
            onSelectAll = { if (wrapper.isUsable()) wrapper.selectAll() },
            onTogglePreview = { viewModel.togglePreviewMode() },
            onToggleFocusMode = { /* focus mode toggle */ },
            onToggleLineNumbers = { settingsViewModel.updateShowLineNumbers(!settings.showLineNumbers) },
            onToggleWordWrap = { settingsViewModel.updateLineWrapping(!settings.lineWrapping) },
            onToggleStickyScroll = { settingsViewModel.updateStickyScroll(!settings.stickyScroll) },
            onGoToLine = {
                showCommandPalette = false
                searchPanelMode = SearchPanelMode.GOTO
            },
            onToggleOutline = { /* outline toggle */ },
            onNextTab = {
                val idx = tabs.indexOfFirst { it.id == activeTabId }
                if (idx >= 0 && idx < tabs.lastIndex) {
                    if (wrapper.isUsable()) wrapper.forceSyncContent()
                    loadedFileKey = null
                    pagerScope.launch { pagerState.animateScrollToPage(idx + 1) }
                }
            },
            onPreviousTab = {
                val idx = tabs.indexOfFirst { it.id == activeTabId }
                if (idx > 0) {
                    if (wrapper.isUsable()) wrapper.forceSyncContent()
                    loadedFileKey = null
                    pagerScope.launch { pagerState.animateScrollToPage(idx - 1) }
                }
            },
            onDiff = { /* diff toggle */ },
            onSnippet = {
                showCommandPalette = false
                showInsertSnippetDialog = true
            },
            onToggleTheme = { /* theme toggle handled at app level */ },
            onCloseTab = {
                val currentTabId = activeTabId ?: return@EditorCommandActions
                viewModel.closeTab(currentTabId)
            },
            onCloseAllTabs = {
                viewModel.closeAllTabs()
            },
            onToggleMinimap = {
                settingsViewModel.updateShowMinimap(!settings.showMinimap)
            },
            onGoToLastEditLocation = {
                showCommandPalette = false
                if (wrapper.isUsable()) wrapper.goToLastEditLocation()
            },
            onToggleSymbolPanel = {
                showCommandPalette = false
                showSymbolPanel = true
            },
            onShowMarkdownCheatSheet = {
                showCommandPalette = false
                showMarkdownCheatSheet = true
            }
        )
        val commands = buildEditorCommands(commandActions)
        CommandPalette(
            commands = commands,
            onDismiss = { showCommandPalette = false }
        )
    }

    // Ch6 Item 16 (P3): Markdown Cheat Sheet
    if (showMarkdownCheatSheet) {
        MarkdownCheatSheet(
            onDismiss = { showMarkdownCheatSheet = false }
        )
    }

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .pointerInput(tabs, activeTabId) {
                val edgeWidth = with(density) { 32.dp.toPx() }
                val threshold = with(density) { 80.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val x = down.position.x
                    if (x >= edgeWidth && x <= size.width - edgeWidth) return@awaitEachGesture
                    val drag = awaitHorizontalDragOrCancellation(down.id)
                    if (drag != null) {
                        val delta = drag.position.x - down.position.x
                        if (kotlin.math.abs(delta) > threshold) {
                            val tabsList = tabs
                            val idx = tabsList.indexOfFirst { it.id == activeTabId }
                            if (delta < 0 && idx < tabsList.lastIndex) {
                                viewModel.switchToTab(tabsList[idx + 1].id)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (delta > 0 && idx > 0) {
                                viewModel.switchToTab(tabsList[idx - 1].id)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Top App Bar (BrandTopBar sub-page variant with mono filename) ----
            // Derived: avoids recomputing the title string on every uiState emission
            // when the fileName hasn't actually changed.
            val fileName by remember {
                derivedStateOf {
                    when (uiState) {
                        is EditorUiState.Success -> (uiState as EditorUiState.Success).fileName
                        else -> "" // fallback handled by stringResource below
                    }
                }
            }
            val displayTitle = fileName.ifBlank { stringResource(R.string.editor_title) }
            val successState = uiState as? EditorUiState.Success

            BrandTopBar(
                onBack = onNavigateUp,
                title = displayTitle,
                titleStyle = MonoFileNameStyle.copy(color = fg)
            ) {
                // Modified indicator dot
                if (isModified) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(PrototypeSpacing.EditorModifiedDot)
                            .clip(PrototypeShapes.StatusCircle)
                            .background(accent)
                    )
                }

                // Mode switch button (CODE / DOCUMENT) for PDF/Office/Media
                if (successState != null &&
                    (successState.isPdf || successState.isOfficeDocument || successState.isMediaFile)
                ) {
                    TooltipIconButton(
                        tooltip = if (editorMode ==
                            EditorMode.CODE
                        ) {
                            stringResource(R.string.editor_document_mode)
                        } else {
                            stringResource(R.string.editor_code_mode)
                        },
                        onClick = {
                            editorMode = if (editorMode == EditorMode.CODE) {
                                EditorMode.DOCUMENT
                            } else {
                                EditorMode.CODE
                            }
                        },
                        modifier = Modifier.accessibilityEnhanced(
                            role = Role.Button,
                            contentDescription = stringResource(R.string.editor_mode_switch),
                            stateDescription = if (editorMode == EditorMode.CODE) "代码模式" else "文档模式"
                        )
                    ) {
                        StrokeIcon(
                            icon = if (editorMode == EditorMode.CODE) {
                                StrokeIcons.Eye
                            } else {
                                StrokeIcons.Code
                            },
                            contentDescription = stringResource(R.string.editor_mode_switch),
                            tint = fgSoft
                        )
                    }
                }
                // Markdown 模式切换控件（仅 .md 可预览文件显示，紧凑四段）
                if (successState?.isPreviewable == true) {
                    MarkdownModeSwitcher(
                        currentMode = markdownViewMode,
                        showWysiwyg = !isLargeMarkdownFile,
                        onSelect = { viewModel.setMarkdownViewMode(it) },
                        compact = true
                    )
                }
                // 保存按钮 — 仅内容变更时显示，紧贴三点左侧（§3.8）
                if (isModified && successState?.isReadOnly != true) {
                    TooltipIconButton(
                        tooltip = stringResource(R.string.editor_save),
                        onClick = { if (wrapper.isUsable()) viewModel.saveFile(wrapper.getContent()) },
                        modifier = Modifier.accessibilityEnhanced(
                            role = Role.Button,
                            contentDescription = stringResource(R.string.editor_save),
                            stateDescription = "有未保存更改"
                        )
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Save,
                            contentDescription = stringResource(R.string.editor_save),
                            tint = accent
                        )
                    }
                }
                if (uiState is EditorUiState.Success) {
                    Box {
                        TooltipIconButton(
                            tooltip = stringResource(R.string.editor_more_actions),
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.accessibilityEnhanced(
                                role = Role.Button,
                                contentDescription = stringResource(R.string.editor_more_actions),
                                stateDescription = if (showMoreMenu) "菜单已展开" else "更多操作"
                            )
                        ) {
                            StrokeIcon(
                                icon = StrokeIcons.MoreVert,
                                contentDescription = stringResource(R.string.editor_more_actions),
                                tint = fgSoft
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.editor_quick_settings), color = fg) },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Settings,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showQuickSettings = true
                                }
                            )
                            // Undo (with subtitle)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = stringResource(R.string.editor_undo), color = fg)
                                        Text(
                                            text = stringResource(R.string.editor_menu_undo_desc),
                                            style = DraftPeekTypography.labelSmall,
                                            color = muted
                                        )
                                    }
                                },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Undo,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    if (wrapper.isUsable()) wrapper.undo()
                                }
                            )
                            // Redo (with subtitle)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = stringResource(R.string.editor_redo), color = fg)
                                        Text(
                                            text = stringResource(R.string.editor_menu_redo_desc),
                                            style = DraftPeekTypography.labelSmall,
                                            color = muted
                                        )
                                    }
                                },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Redo,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    if (wrapper.isUsable()) wrapper.redo()
                                }
                            )
                            // Search (with subtitle, opens search panel)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = stringResource(R.string.editor_menu_search), color = fg)
                                        Text(
                                            text = stringResource(R.string.editor_menu_search_desc),
                                            style = DraftPeekTypography.labelSmall,
                                            color = muted
                                        )
                                    }
                                },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Search,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    searchPanelMode = SearchPanelMode.FIND
                                }
                            )
                            // Terminal (always shown, with subtitle)
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = stringResource(R.string.editor_action_open_terminal), color = fg)
                                        Text(
                                            text = stringResource(R.string.editor_menu_terminal_desc),
                                            style = DraftPeekTypography.labelSmall,
                                            color = muted
                                        )
                                    }
                                },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Terminal,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val terminalCwd = fileUri?.let { uri ->
                                        FileUtils.resolveLocalPath(uri)?.let { path ->
                                            java.io.File(path).parent
                                        }
                                    }
                                    onNavigateToTerminal(terminalCwd)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.editor_save_as_snippet), color = fg) },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Save,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showSaveSnippetDialog = true
                                }
                            )
                            if ((uiState as? EditorUiState.Success)?.isMarkdownFile == true) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.editor_backlinks), color = fg) },
                                    leadingIcon = {
                                        StrokeIcon(
                                            icon = StrokeIcons.Link,
                                            contentDescription = null,
                                            tint = fgSoft
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showBacklinksDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.editor_insert_snippet), color = fg) },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Code,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showInsertSnippetDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(R.string.editor_reopen_with_encoding), color = fg)
                                },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Refresh,
                                        contentDescription = null,
                                        tint = fgSoft
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showEncodingSelector()
                                }
                            )
                            if (isInternalFile) {
                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(R.string.editor_save_with_encoding), color = fg)
                                    },
                                    leadingIcon = {
                                        StrokeIcon(
                                            icon = StrokeIcons.Save,
                                            contentDescription = null,
                                            tint = fgSoft
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.showSaveEncodingSelector()
                                    }
                                )
                            }
                            if (isInternalFile) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.editor_export_file), color = fg) },
                                    onClick = {
                                        showMoreMenu = false
                                        val successState2 = uiState as? EditorUiState.Success
                                        val fileName2 = successState2?.fileName ?: "file"
                                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "*/*"
                                            putExtra(Intent.EXTRA_TITLE, fileName2)
                                        }
                                        exportLauncher.launch(intent)
                                    }
                                )
                                HorizontalDivider(thickness = 1.dp, color = border)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.editor_delete_file),
                                            color = PrototypeTokens.error
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteFileDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ---- Tab Bar (visible when 2+ tabs are open) ----
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { tabId ->
                    if (tabId != activeTabId) {
                        loadedFileKey = null
                        // Force-sync content before switching to capture any unsaved edits
                        if (wrapper.isUsable()) {
                            wrapper.forceSyncContent()
                        }
                        val targetPage = tabs.indexOfFirst { it.id == tabId }
                        if (targetPage >= 0) {
                            pagerScope.launch { pagerState.animateScrollToPage(targetPage) }
                        }
                    }
                },
                onTabClose = { tabId ->
                    val hasTabsLeft = viewModel.closeTab(tabId)
                    if (hasTabsLeft) {
                        loadedFileKey = null
                    } else {
                        onNavigateUp()
                    }
                },
                onTabReorder = { tabId, toIndex ->
                    viewModel.reorderTab(tabId, toIndex)
                }
            )

            // ---- Content Area ----
            EditorContentArea(
                uiState = uiState,
                tabs = tabs,
                activeTabId = activeTabId,
                pagerState = pagerState,
                wrapper = wrapper,
                viewModel = viewModel,
                darkTheme = darkTheme,
                editorMode = editorMode,
                markdownViewMode = markdownViewMode,
                isPreviewMode = isPreviewMode,
                isLargeMarkdownFile = isLargeMarkdownFile,
                shortcutHandler = shortcutHandler,
                onNavigateUp = onNavigateUp,
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // ---- Bottom Status Bar ----
            EditorBottomStatusBar(
                cursorPosition = cursorPosition,
                detectedEncoding = detectedEncoding,
                uiState = uiState,
                isModified = isModified,
                tabWidth = settings.tabWidth,
                selectionCount = selectionCount,
                isReadOnlyOverride = isReadOnlyOverride,
                onLanguageClick = {
                    val lang = (uiState as? EditorUiState.Success)?.language ?: "TEXT"
                    val displayLang = if (lang.equals("markdown", ignoreCase = true)) "Markdown" else lang.uppercase()
                    Toast.makeText(
                        context,
                        context.getString(R.string.editor_status_language_switch, displayLang),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onReadOnlyToggle = {
                    isReadOnlyOverride = !isReadOnlyOverride
                    wrapper.setReadOnly(isReadOnlyOverride || (uiState as? EditorUiState.Success)?.isReadOnly == true)
                },
                onLineEndingClick = {
                    val currentContent = (uiState as? EditorUiState.Success)?.content ?: ""
                    val currentEnding = lineEndingOverride ?: when {
                        currentContent.contains("\r\n") -> "CRLF"
                        currentContent.contains("\n") -> "LF"
                        else -> "LF"
                    }
                    lineEndingOverride = if (currentEnding == "LF") "CRLF" else "LF"
                    Toast.makeText(context, lineEndingOverride, Toast.LENGTH_SHORT).show()
                }
            )

            // Delete File Confirmation Dialog
            if (showDeleteFileDialog) {
                val successState = uiState as? EditorUiState.Success
                BrandDialog(
                    onDismissRequest = { showDeleteFileDialog = false },
                    title = { Text(text = stringResource(R.string.editor_delete_file)) },
                    content = {
                        Text(
                            text = stringResource(R.string.editor_delete_confirm_message, successState?.fileName ?: "")
                        )
                    },
                    confirmButton = {
                        BrandFilledButton(
                            text = stringResource(R.string.editor_delete),
                            onClick = {
                                hapticController.longPress()
                                viewModel.deleteCurrentFile()
                                showDeleteFileDialog = false
                                onNavigateUp()
                            }
                        )
                    },
                    dismissButton = {
                        BrandOutlinedButton(text = stringResource(R.string.editor_cancel), onClick = {
                            showDeleteFileDialog =
                                false
                        })
                    }
                )
            }

            // Backlinks Dialog (当前文档的反向链接)
            if (showBacklinksDialog) {
                val backlinks = viewModel.backlinks.collectAsStateWithLifecycle().value
                val context = LocalContext.current
                BacklinksDialog(
                    backlinks = backlinks,
                    onBacklinkClick = { source ->
                        showBacklinksDialog = false
                        // 用系统打开方式尝试展示引用来源文档（SAF URI 或内部文件路径）
                        try {
                            val uri = android.net.Uri.parse(source)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.editor_backlinks_open_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDismiss = { showBacklinksDialog = false }
                )
            }

            // Encoding Selector Dialog
            if (showEncodingDialog) {
                EncodingSelectorDialog(
                    defaultEncoding = detectedEncoding ?: "UTF-8",
                    onDismissRequest = { viewModel.dismissEncodingSelector() },
                    onEncodingSelected = { encoding ->
                        viewModel.reloadWithEncoding(encoding)
                    }
                )
            }

            // Save Encoding Selector Dialog
            if (showSaveEncodingDialog) {
                EncodingSelectorDialog(
                    defaultEncoding = detectedEncoding ?: "UTF-8",
                    onDismissRequest = { viewModel.dismissSaveEncodingSelector() },
                    onEncodingSelected = { encoding ->
                        viewModel.saveWithEncoding(encoding)
                    }
                )
            }

            // Quick Settings Bottom Sheet
            if (showQuickSettings) {
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showQuickSettings = false },
                    sheetState = quickSettingsState,
                    containerColor = surface
                ) {
                    QuickSettingsPanel(
                        settings = settings,
                        onFontSizeChange = { settingsViewModel.updateFontSize(it) },
                        onWordWrapChange = { settingsViewModel.updateLineWrapping(it) },
                        onLineNumbersChange = { settingsViewModel.updateShowLineNumbers(it) },
                        onTabWidthChange = { settingsViewModel.updateTabWidth(it) }
                    )
                }
            }

            // Ch1 Item 19 (P3): Symbol Panel Bottom Sheet
            if (showSymbolPanel) {
                SymbolPanel(
                    onSymbolClick = { symbol ->
                        try {
                            if (wrapper.isUsable()) {
                                val cursor = wrapper.editor.cursor
                                if (cursor != null) {
                                    val left = cursor.left
                                    wrapper.editor.insertText(symbol, left)
                                    wrapper.editor.setSelection(left + symbol.length, left + symbol.length)
                                }
                            }
                        } catch (_: Exception) {}
                        showSymbolPanel = false
                    },
                    onDismiss = { showSymbolPanel = false }
                )
            }
        }
    }
}

/**
 * 编辑器快速设置面板。
 *
 * 提供常用编辑器配置的快捷访问，包括字体大小、自动换行、行号显示、Tab宽度等。
 *
 * @param settings 当前编辑器设置
 * @param onFontSizeChange 字体大小变更回调
 * @param onWordWrapChange 自动换行开关变更回调
 * @param onLineNumbersChange 行号显示开关变更回调
 * @param onTabWidthChange Tab宽度变更回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSettingsPanel(
    settings: EditorSettings,
    onFontSizeChange: (Int) -> Unit,
    onWordWrapChange: (Boolean) -> Unit,
    onLineNumbersChange: (Boolean) -> Unit,
    onTabWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PrototypeSpacing.BottomSheetPadding)
    ) {
        Text(
            text = stringResource(R.string.editor_quick_settings_title),
            style = H2Style.copy(color = fg),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Font size slider
        Text(
            stringResource(R.string.editor_font_size, settings.fontSize),
            style = DraftPeekTypography.bodyMedium.copy(color = fg)
        )
        androidx.compose.material3.Slider(
            value = settings.fontSize.toFloat(),
            onValueChange = { onFontSizeChange(it.toInt()) },
            valueRange = 10f..24f,
            steps = 13,
            colors = SliderDefaults.colors(
                activeTrackColor = PrototypeTokens.accent,
                thumbColor = PrototypeTokens.accent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Word wrap switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.editor_word_wrap), style = DraftPeekTypography.bodyMedium.copy(color = fg))
            BrandSwitch(
                checked = settings.lineWrapping,
                onCheckedChange = onWordWrapChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Line numbers switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.editor_show_line_numbers),
                style = DraftPeekTypography.bodyMedium.copy(color = fg)
            )
            BrandSwitch(
                checked = settings.showLineNumbers,
                onCheckedChange = onLineNumbersChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab width chips
        Text(stringResource(R.string.editor_tab_width), style = DraftPeekTypography.bodyMedium.copy(color = fg))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 4, 8).forEach { width ->
                androidx.compose.material3.FilterChip(
                    selected = settings.tabWidth == width,
                    onClick = { onTabWidthChange(width) },
                    label = { Text("$width", style = DraftPeekTypography.labelMedium) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 大文件警告横幅。
 *
 * 当打开的文件过大时，在编辑器内容区域顶部显示警告信息，
 * 提示用户可能存在性能问题。
 *
 * @param message 警告消息文本
 * @param modifier 修饰符
 * @param onDismiss 关闭横幅的回调
 */
@Composable
private fun FileSizeWarningBanner(message: String, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val warning = PrototypeTokens.warning
    val warningContainer = warning.copy(alpha = 0.1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(warningContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = DraftPeekTypography.bodyMedium,
                color = warning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 大文件预览提示横幅。
 *
 * 在编辑模式下对大 Markdown 文件显示，提示用户可以切换到预览模式查看渲染效果。
 * 使用品牌强调色而非警告色，因为这不是错误状态，而是功能提示。
 *
 * @param onPreviewClick 点击"预览"按钮的回调
 * @param modifier 修饰符
 */
@Composable
private fun LargeFilePreviewHintBanner(onPreviewClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = PrototypeTokens.accent
    val accentContainer = accent.copy(alpha = 0.1f)
    val fg = PrototypeTokens.fg
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(accentContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.editor_large_file_preview_hint),
                style = DraftPeekTypography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onPreviewClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.editor_preview),
                    style = DraftPeekTypography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 只读文件提示横幅。
 *
 * 在编辑器内容区域顶部显示，告知用户当前文件为只读状态无法编辑，
 * 并提示如何通过文件浏览器打开可编辑文件。用户可点击关闭按钮隐藏此提示。
 *
 * @param modifier 修饰符
 * @param onDismiss 关闭横幅的回调
 */
@Composable
private fun ReadOnlyBanner(modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val info = PrototypeTokens.info
    val infoContainer = info.copy(alpha = 0.1f)
    val fg = PrototypeTokens.fg
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(infoContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "此文件为只读，不能修改。\n如需编辑，请复制内容后新建文件。",
                style = DraftPeekTypography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(PrototypeShapes.Medium)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                StrokeIcon(
                    icon = StrokeIcons.Close,
                    contentDescription = stringResource(R.string.editor_close_hint),
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 带工具提示的图标按钮。
 *
 * 对 [IconButton] 的封装，在长按（或支持悬停的设备上鼠标悬停）时显示纯文本提示，
 * 用于帮助用户理解工具栏图标的功能含义。
 *
 * @param tooltip 提示文本内容
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param content 按钮内的图标内容 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        BrandIconButton(onClick = onClick, modifier = modifier) {
            content()
        }
    }
}

/**
 * 统一搜索面板（查找 / 替换 / 跳转到行三页签）。
 *
 * 替代原来三个独立的 BrandDialog，在单一对话框内通过页签切换。
 * 引擎侧复用 SoraSearchManager 已有的 search/replace/replaceAll/goToLine 能力。
 */
@Composable
private fun EditorSearchPanel(
    mode: SearchPanelMode,
    onModeChange: (SearchPanelMode) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceText: String,
    onReplaceTextChange: (String) -> Unit,
    searchRegex: Boolean,
    onSearchRegexChange: (Boolean) -> Unit,
    searchMatchCase: Boolean,
    onSearchMatchCaseChange: (Boolean) -> Unit,
    searchWholeWord: Boolean,
    onSearchWholeWordChange: (Boolean) -> Unit,
    goToLineInput: String,
    onGoToLineInputChange: (String) -> Unit,
    wrapper: SoraEditorWrapper,
    context: android.content.Context
) {
    // Shared state for go-to-line error (must be visible in both content and confirmButton)
    var goToLineError by remember { mutableStateOf("") }

    // Live search: re-run search when query or options change (for Find and Replace tabs)
    LaunchedEffect(searchQuery, searchRegex, searchMatchCase, searchWholeWord, mode) {
        if (mode == SearchPanelMode.FIND || mode == SearchPanelMode.REPLACE) {
            if (searchQuery.isNotBlank()) {
                wrapper.search(
                    searchQuery,
                    regex = searchRegex,
                    matchCase = searchMatchCase,
                    wholeWord = searchWholeWord
                )
            }
        }
    }

    val tabs = listOf(
        SearchPanelMode.FIND to stringResource(R.string.editor_search_tab_find),
        SearchPanelMode.REPLACE to stringResource(R.string.editor_search_tab_replace),
        SearchPanelMode.GOTO to stringResource(R.string.editor_search_tab_goto_line)
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEach { (tabMode, tabLabel) ->
                    val isSelected = tabMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrototypeTokens.surface else Color.Transparent)
                            .clickable { onModeChange(tabMode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            style = DraftPeekTypography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) PrototypeTokens.accent else PrototypeTokens.muted
                            )
                        )
                    }
                }
            }
        },
        content = {
            when (mode) {
                SearchPanelMode.FIND -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text(text = stringResource(R.string.editor_find_content)) },
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandFilterChip(
                                text = stringResource(R.string.editor_regex),
                                selected = searchRegex,
                                onClick = { onSearchRegexChange(!searchRegex) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_match_case),
                                selected = searchMatchCase,
                                onClick = { onSearchMatchCaseChange(!searchMatchCase) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_whole_word),
                                selected = searchWholeWord,
                                onClick = { onSearchWholeWordChange(!searchWholeWord) }
                            )
                        }
                    }
                }
                SearchPanelMode.REPLACE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text(text = stringResource(R.string.editor_find_content)) },
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandFilterChip(
                                text = stringResource(R.string.editor_regex),
                                selected = searchRegex,
                                onClick = { onSearchRegexChange(!searchRegex) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_match_case),
                                selected = searchMatchCase,
                                onClick = { onSearchMatchCaseChange(!searchMatchCase) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_whole_word),
                                selected = searchWholeWord,
                                onClick = { onSearchWholeWordChange(!searchWholeWord) }
                            )
                        }
                        BrandOutlinedTextField(
                            value = replaceText,
                            onValueChange = onReplaceTextChange,
                            label = { Text(text = stringResource(R.string.editor_replace_with)) },
                            singleLine = true
                        )
                    }
                }
                SearchPanelMode.GOTO -> {
                    val totalLines = wrapper.getTotalLines()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = goToLineInput,
                            onValueChange = {
                                onGoToLineInputChange(it)
                                goToLineError = ""
                            },
                            label = { Text(text = stringResource(R.string.editor_line_number_range, totalLines)) },
                            singleLine = true,
                            isError = goToLineError.isNotEmpty(),
                            supportingText = if (goToLineError.isNotEmpty()) {
                                { Text(text = goToLineError) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (mode) {
                SearchPanelMode.FIND -> {
                    BrandFilledButton(text = stringResource(R.string.editor_find), onClick = {
                        onDismiss()
                    })
                }
                SearchPanelMode.REPLACE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedButton(text = stringResource(R.string.editor_replace), onClick = {
                            if (searchQuery.isNotBlank()) {
                                wrapper.replaceCurrent(replaceText)
                            }
                        })
                        BrandFilledButton(text = stringResource(R.string.editor_replace_all), onClick = {
                            wrapper.replaceAll(
                                searchQuery,
                                replaceText,
                                regex = searchRegex,
                                matchCase = searchMatchCase,
                                wholeWord = searchWholeWord
                            )
                            onDismiss()
                        })
                    }
                }
                SearchPanelMode.GOTO -> {
                    val totalLines = wrapper.getTotalLines()
                    BrandFilledButton(text = stringResource(R.string.editor_go), onClick = {
                        val line = goToLineInput.toIntOrNull()
                        when {
                            goToLineInput.isBlank() -> {
                                goToLineError = context.getString(R.string.editor_enter_line_number)
                            }
                            line == null || line <= 0 -> {
                                goToLineError = context.getString(R.string.editor_enter_valid_line)
                            }
                            line > totalLines -> {
                                goToLineError = context.getString(R.string.editor_out_of_range, totalLines)
                            }
                            else -> {
                                wrapper.goToLine(line)
                                onDismiss()
                            }
                        }
                    })
                }
            }
        },
        dismissButton = {
            BrandOutlinedButton(text = stringResource(R.string.editor_cancel), onClick = onDismiss)
        }
    )
}

/**
 * 编辑器底部状态栏。
 *
 * 显示当前光标位置（行:列）、文件编码、编程语言、只读状态、修改标记等信息。
 *
 * @param cursorPosition 当前光标位置
 * @param detectedEncoding 检测到的文件编码
 * @param uiState 编辑器 UI 状态
 * @param isModified 文件是否已修改
 * @param tabWidth 缩进宽度（来自设置）
 * @param selectionCount 选中字符数（0 表示无选区）
 * @param isReadOnlyOverride 手动只读覆盖状态
 * @param onLanguageClick 语言点击回调
 * @param onReadOnlyToggle 只读/编辑切换回调
 * @param onLineEndingClick 行尾符点击回调
 * @param modifier 修饰符
 */
@Composable
private fun EditorBottomStatusBar(
    cursorPosition: CursorPosition,
    detectedEncoding: String?,
    uiState: EditorUiState,
    isModified: Boolean,
    tabWidth: Int = 4,
    selectionCount: Int = 0,
    isReadOnlyOverride: Boolean = false,
    onLanguageClick: () -> Unit = {},
    onReadOnlyToggle: () -> Unit = {},
    onLineEndingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent

    // Detect line ending style from content (CRLF vs LF)
    val detectedLineEnding = remember(uiState) {
        val content = (uiState as? EditorUiState.Success)?.content ?: ""
        when {
            content.contains("\r\n") -> "CRLF"
            content.contains("\n") -> "LF"
            content.contains("\r") -> "CR"
            else -> "LF"
        }
    }

    // Effective read-only state (file property OR manual override)
    val fileReadOnly = (uiState as? EditorUiState.Success)?.isReadOnly == true
    val effectiveReadOnly = fileReadOnly || isReadOnlyOverride

    val positionText = "Ln ${cursorPosition.line}, Col ${cursorPosition.column}"
    val encodingText = detectedEncoding ?: "UTF-8"
    val indentText = stringResource(R.string.editor_status_indent_spaces, tabWidth)

    HorizontalDivider(thickness = 1.dp, color = border)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surface)
            .padding(horizontal = PrototypeSpacing.EditorStatusBarPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Read-only / Edit toggle (clickable)
        val readOnlyLabel = if (effectiveReadOnly) {
            stringResource(R.string.editor_read_only)
        } else {
            stringResource(R.string.editor_status_editable)
        }
        Text(
            text = readOnlyLabel,
            style = EditorStatusBarStyle,
            color = if (effectiveReadOnly) muted else accent,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReadOnlyToggle
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Modified indicator
        if (isModified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(PrototypeSpacing.EditorModifiedDot)
                        .clip(PrototypeShapes.StatusCircle)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.editor_modified),
                    style = EditorStatusBarStyle,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        // Selection count (only when there is a selection)
        if (selectionCount > 0) {
            Text(
                text = stringResource(R.string.editor_status_selection_count, selectionCount),
                style = EditorStatusBarStyle,
                color = muted
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        // Language (clickable)
        if (uiState is EditorUiState.Success) {
            val lang = uiState.language
            if (lang != null) {
                val displayLang = if (lang.equals("markdown", ignoreCase = true)) "Markdown" else lang.uppercase()
                Text(
                    text = displayLang,
                    style = EditorStatusBarStyle,
                    color = accent,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLanguageClick
                    )
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        // Indent (Spaces: N)
        Text(
            text = indentText,
            style = EditorStatusBarStyle,
            color = muted
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Encoding (UTF-8, etc.)
        Text(
            text = encodingText,
            style = EditorStatusBarStyle,
            color = muted
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Line ending (LF / CRLF) — clickable
        Text(
            text = detectedLineEnding,
            style = EditorStatusBarStyle,
            color = muted,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLineEndingClick
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Cursor position (Ln X, Col Y) — rightmost in VSCode style
        Text(
            text = positionText,
            style = EditorStatusBarStyle,
            color = muted
        )
    }
}

/** Small pill-style label used in the status bar (e.g. "Read Only"). */
@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = EditorStatusBarStyle,
        color = color,
        modifier = Modifier
            .background(
                PrototypeTokens.elevated,
                PrototypeShapes.Small
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * 编辑器内容区域。
 *
 * 使用 HorizontalPager 实现多标签页切换，支持相邻标签页预加载。
 * 根据 UI 状态渲染不同类型的内容：代码编辑器、PDF预览、媒体查看器、Office文档预览、
 * Markdown/HTML预览等。
 *
 * @param uiState 编辑器 UI 状态
 * @param tabs 标签页列表
 * @param activeTabId 当前活动标签页 ID
 * @param pagerState 分页器状态
 * @param wrapper sora-editor 封装实例
 * @param viewModel 编辑器 ViewModel
 * @param darkTheme 是否深色主题
 * @param editorMode 编辑器模式（代码/文档）
 * @param markdownViewMode Markdown视图模式
 * @param isPreviewMode 是否处于预览模式
 * @param shortcutHandler 键盘快捷键处理器
 * @param onNavigateUp 返回上一级回调
 * @param modifier 修饰符
 * @param settings 编辑器设置
 */
@Composable
private fun EditorContentArea(
    uiState: EditorUiState,
    tabs: List<EditorTab>,
    activeTabId: TabId?,
    pagerState: androidx.compose.foundation.pager.PagerState,
    wrapper: SoraEditorWrapper,
    viewModel: EditorViewModel,
    darkTheme: Boolean,
    editorMode: EditorMode,
    markdownViewMode: MarkdownViewMode,
    isPreviewMode: Boolean,
    isLargeMarkdownFile: Boolean,
    shortcutHandler: KeyboardShortcutHandler,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    settings: EditorSettings = EditorSettings()
) {
    val context = LocalContext.current
    var hasScrolledPastThreshold by remember { mutableStateOf(false) }

    if (tabs.isEmpty()) {
        // No tabs open — show loading/empty state
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is EditorUiState.Loading -> CircularProgressIndicator()
                is EditorUiState.LoadingWithProgress -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.progressText,
                            style = DraftPeekTypography.bodySmall,
                            color = PrototypeTokens.muted
                        )
                    }
                }
                is EditorUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.editor_load_failed),
                            style = DraftPeekTypography.headlineMedium.copy(color = PrototypeTokens.error)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = DraftPeekTypography.bodyMedium,
                            color = PrototypeTokens.muted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BrandFilledButton(text = stringResource(R.string.editor_back), onClick = onNavigateUp)
                    }
                }
                else -> { /* idle / no file */ }
            }
        }
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = false,
        beyondViewportPageCount = 1,
        key = { page -> tabs[page].id.value }
    ) { page ->
        val tab = tabs[page]
        // Only render the active tab's content; others show a placeholder to keep alive
        val isCurrentPage = tab.id == activeTabId

        if (!isCurrentPage) {
            // Inactive tab — lightweight placeholder to keep pager slot alive
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.fileName,
                    style = DraftPeekTypography.bodyMedium.copy(color = PrototypeTokens.muted)
                )
            }
            return@HorizontalPager
        }

        when (uiState) {
            is EditorUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EditorUiState.LoadingWithProgress -> {
                // P1-14: Show loading progress for large files
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        if (uiState.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                            )
                        } else {
                            // Unknown total size: show indeterminate bar
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val percentage = uiState.percentageText
                        if (percentage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = percentage,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            is EditorUiState.Success -> {
                val successState = uiState
                val liveContent by viewModel.liveContent.collectAsStateWithLifecycle()
                val prefs =
                    remember { context.getSharedPreferences("draftpeek_prefs", android.content.Context.MODE_PRIVATE) }
                var bannerDismissed by rememberSaveable {
                    mutableStateOf(prefs.getBoolean("sample_banner_dismissed", false))
                }
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (successState.isReadOnly && !bannerDismissed) {
                        ReadOnlyBanner(onDismiss = {
                            bannerDismissed = true
                            prefs.edit().putBoolean("sample_banner_dismissed", true).apply()
                        })
                    }
                    if (successState.fileSizeWarning != null) {
                        FileSizeWarningBanner(
                            message = successState.fileSizeWarning,
                            onDismiss = {}
                        )
                    }
                    // Stage 2: 大 Markdown 文件在编辑模式下提示预览
                    if (isLargeMarkdownFile && markdownViewMode == MarkdownViewMode.EDIT) {
                        LargeFilePreviewHintBanner(
                            onPreviewClick = { viewModel.setMarkdownViewMode(MarkdownViewMode.PREVIEW) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val resolvedCss: String? = if (settings.customMarkdownCss.isBlank()) null else settings.customMarkdownCss
                        when {
                            successState.isPdf || successState.fileName.lowercase().endsWith(".pdf") -> {
                                PdfDocumentScreen(
                                    fileUri = Uri.parse(viewModel.currentUriString),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isBinaryFile -> {
                                BinaryFilePlaceholder(
                                    fileName = successState.fileName,
                                    fileSize = successState.fileSize,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isMediaFile -> {
                                MediaViewerScreen(
                                    fileUri = Uri.parse(viewModel.currentUriString),
                                    documentType = successState.documentType ?: DocumentType.IMAGE,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            successState.isOfficeDocument && editorMode == EditorMode.DOCUMENT -> {
                                OfficeDocumentScreen(
                                    htmlContent = successState.renderedHtml ?: "",
                                    documentType = successState.documentType ?: DocumentType.WORD,
                                    darkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Stage 3: 流式预览——超大 Markdown 文件使用 LazyMarkdownPreview
                            markdownViewMode == MarkdownViewMode.SPLIT &&
                                successState.isMarkdownFile &&
                                viewModel.needsStreamingPreview -> {
                                SplitPane(
                                    modifier = Modifier.fillMaxSize(),
                                    orientation = SplitOrientation.Horizontal,
                                    splitRatio = 0.5f,
                                    minRatio = 0.2f,
                                    maxRatio = 0.8f,
                                    first = {
                                        // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SoraEditorBridge(
                                                wrapper = wrapper,
                                                shortcutHandler = shortcutHandler,
                                                darkTheme = darkTheme,
                                                errorLabel = SORA_SLOT_SPLIT_STREAM,
                                                modifier = Modifier.fillMaxSize(),
                                                onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                            )
                                        }
                                    },
                                    second = {
                                        LazyMarkdownPreview(
                                            markdownContent = liveContent,
                                            isDarkTheme = darkTheme,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }
                            isPreviewMode && successState.isMarkdownFile && viewModel.needsStreamingPreview -> {
                                LazyMarkdownPreview(
                                    markdownContent = liveContent,
                                    isDarkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            markdownViewMode == MarkdownViewMode.WYSIWYG &&
                                successState.isMarkdownFile &&
                                !isLargeMarkdownFile -> {
                                MarkdownRichEditor(
                                    markdownContent = successState.content,
                                    onContentChanged = { newContent ->
                                        viewModel.onContentChanged(newContent)
                                    },
                                    isDarkTheme = darkTheme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            markdownViewMode == MarkdownViewMode.SPLIT && successState.isMarkdownFile -> {
                                SplitPane(
                                    modifier = Modifier.fillMaxSize(),
                                    orientation = SplitOrientation.Horizontal,
                                    splitRatio = 0.5f,
                                    minRatio = 0.2f,
                                    maxRatio = 0.8f,
                                    first = {
                                        // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            SoraEditorBridge(
                                                wrapper = wrapper,
                                                shortcutHandler = shortcutHandler,
                                                darkTheme = darkTheme,
                                                errorLabel = SORA_SLOT_SPLIT,
                                                modifier = Modifier.fillMaxSize(),
                                                onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                            )
                                        }
                                    },
                                    second = {
                                        MarkdownWebViewPreview(
                                            markdownContent = liveContent,
                                            isDarkTheme = darkTheme,
                                            theme = try {
                                                MarkdownTheme.valueOf(settings.markdownThemeName)
                                            } catch (
                                                _: Exception
                                            ) {
                                                MarkdownTheme.DEFAULT
                                            },
                                            customCss = resolvedCss,
                                            onHeadingClick = { lineIndex ->
                                                try {
                                                    if (wrapper.isUsable()) wrapper.goToLine(lineIndex + 1)
                                                } catch (
                                                    _: Exception
                                                ) {}
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                )
                            }
                            isPreviewMode && successState.isMarkdownFile -> {
                                MarkdownWebViewPreview(
                                    markdownContent = liveContent,
                                    isDarkTheme = darkTheme,
                                    theme = try {
                                        MarkdownTheme.valueOf(settings.markdownThemeName)
                                    } catch (
                                        _: Exception
                                    ) {
                                        MarkdownTheme.DEFAULT
                                    },
                                    customCss = resolvedCss,
                                    onHeadingClick = { lineIndex ->
                                        if (markdownViewMode == MarkdownViewMode.PREVIEW) {
                                            viewModel.setMarkdownViewMode(MarkdownViewMode.SPLIT)
                                        }
                                        try {
                                            if (wrapper.isUsable()) wrapper.goToLine(lineIndex + 1)
                                        } catch (
                                            _: Exception
                                        ) {}
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            isPreviewMode && successState.isHtmlFile -> {
                                HtmlPreview(
                                    htmlContent = successState.content,
                                    darkTheme = darkTheme,
                                    onConvertToMarkdown = { viewModel.convertHtmlToMarkdown() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // 统一桥接（SoraEditorBridge）：收敛复用挂载 / 主题同步 / IME 焦点 / 滚动阈值
                                    SoraEditorBridge(
                                        wrapper = wrapper,
                                        shortcutHandler = shortcutHandler,
                                        darkTheme = darkTheme,
                                        errorLabel = SORA_SLOT_EDITOR,
                                        modifier = Modifier.fillMaxSize(),
                                        onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
                                    )
                                    if (wrapper.isUsable()) {
                                        ScrollToTopButton(
                                            editor = wrapper.editor,
                                            hasScrolled = hasScrolledPastThreshold,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is EditorUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.editor_load_failed),
                            style = DraftPeekTypography.headlineMedium.copy(color = PrototypeTokens.error)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = DraftPeekTypography.bodyMedium,
                            color = PrototypeTokens.muted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BrandFilledButton(text = stringResource(R.string.editor_back), onClick = onNavigateUp)
                    }
                }
            }
        }
    }
}

/**
 * 编辑器初始化失败错误界面。
 *
 * 当 sora-editor 初始化发生致命错误时显示，提供错误信息、返回按钮和重试按钮。
 *
 * @param error 初始化时发生的异常
 * @param onNavigateUp 返回上一级界面的回调
 * @param onRetry 重试初始化编辑器的回调
 */
@Composable
private fun EditorInitErrorScreen(error: Throwable, onNavigateUp: () -> Unit, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = PrototypeTokens.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.editor_init_failed),
                style = DraftPeekTypography.headlineMedium,
                color = PrototypeTokens.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error.message ?: stringResource(R.string.editor_load_failed),
                style = DraftPeekTypography.bodyMedium,
                color = PrototypeTokens.muted
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandOutlinedButton(
                    text = stringResource(R.string.editor_back),
                    onClick = onNavigateUp
                )
                BrandFilledButton(
                    text = stringResource(R.string.editor_retry),
                    onClick = onRetry
                )
            }
        }
    }
}
