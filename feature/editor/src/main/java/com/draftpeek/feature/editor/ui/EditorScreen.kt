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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.draftpeek.core.domain.model.Snippet
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandFilterChip
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandSwitch
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.KeyboardSafeBottomSheet
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.component.rememberHapticController
import com.draftpeek.core.ui.composition.isFeatureEnabled
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
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
import com.draftpeek.feature.editor.sora.SearchCountDisplay
import com.draftpeek.feature.editor.sora.SearchMatchCounter
import com.draftpeek.feature.editor.sora.SoraEditorWrapper
import com.draftpeek.feature.editor.sora.saveUndoState
import com.draftpeek.feature.editor.sora.setDiagnostics
import com.draftpeek.feature.editor.sora.restoreUndoState
import com.draftpeek.feature.editor.treesitter.TreeSitterLanguageProvider
import com.draftpeek.feature.editor.viewmodel.CursorPosition
import com.draftpeek.feature.editor.viewmodel.EditorViewModel
import com.draftpeek.feature.settings.model.EditorSettings
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
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
    val changedLineCount by viewModel.changedLineCount.collectAsStateWithLifecycle()
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
                // 保存按钮 — 始终占位，无改动时置灰（实施指导书 §2.3 屏 15）。
                // 原实现是「无改动就不渲染」，会让按钮位置随状态跳动；改为置灰后位置恒定。
                if (successState?.isReadOnly != true) {
                    val canSave = isModified && wrapper.isUsable()
                    TooltipIconButton(
                        tooltip = stringResource(R.string.editor_save),
                        onClick = { if (canSave) viewModel.saveFile(wrapper.getContent()) },
                        modifier = Modifier.accessibilityEnhanced(
                            role = Role.Button,
                            contentDescription = stringResource(R.string.editor_save),
                            stateDescription = if (isModified) "有未保存更改" else "无未保存更改"
                        )
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Save,
                            contentDescription = stringResource(R.string.editor_save),
                            tint = if (canSave) accent else muted.copy(alpha = 0.4f)
                        )
                    }
                }
                // Quick Settings 从溢出菜单升为顶栏常驻「⚙」（实施指导书 §2.3 屏 15）
                if (uiState is EditorUiState.Success) {
                    TooltipIconButton(
                        tooltip = stringResource(R.string.editor_quick_settings),
                        onClick = { showQuickSettings = true },
                        modifier = Modifier.accessibilityEnhanced(
                            role = Role.Button,
                            contentDescription = stringResource(R.string.editor_quick_settings)
                        )
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Settings,
                            contentDescription = stringResource(R.string.editor_quick_settings),
                            tint = fgSoft
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
                            // ── 编辑 ─────────────────────────────────────────────
                            EditorMenuSectionLabel(stringResource(R.string.editor_menu_group_edit))
                            // 撤销 / 重做合并为一行双按钮（实施指导书 §2.3 屏 15–17）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EditorMenuActionButton(
                                    icon = StrokeIcons.Undo,
                                    label = stringResource(R.string.editor_undo),
                                    onClick = {
                                        showMoreMenu = false
                                        if (wrapper.isUsable()) wrapper.undo()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                EditorMenuActionButton(
                                    icon = StrokeIcons.Redo,
                                    label = stringResource(R.string.editor_redo),
                                    onClick = {
                                        showMoreMenu = false
                                        if (wrapper.isUsable()) wrapper.redo()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                            // ── 工具 ─────────────────────────────────────────────
                            HorizontalDivider(thickness = 1.dp, color = border)
                            EditorMenuSectionLabel(stringResource(R.string.editor_menu_group_tools))
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
                            // ── 文件 ─────────────────────────────────────────────
                            HorizontalDivider(thickness = 1.dp, color = border)
                            EditorMenuSectionLabel(stringResource(R.string.editor_menu_group_file))
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
                            }
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
                            // ── 危险区 ───────────────────────────────────────────
                            if (isInternalFile) {
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
                changedLineCount = changedLineCount,
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
                },
                // 编码段点击直达「选择编码重新打开」面板（实施指导书 §2.3 屏 15）
                onEncodingClick = { viewModel.showEncodingSelector() }
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
