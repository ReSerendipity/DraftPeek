package com.draftpeek.feature.browser.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.util.FileUtils
import com.draftpeek.core.common.util.InputValidator
import com.draftpeek.core.common.util.ValidationResult
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitHubFileEntry
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.domain.model.RecentFile
import com.draftpeek.core.data.security.FileCipher
import com.draftpeek.core.ui.component.BrandChip
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandDirectoryCard
import com.draftpeek.core.ui.component.BrandFAB
import com.draftpeek.core.ui.component.BrandFileCard
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandSearchBar
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.FABMenuItem
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.composition.isFeatureEnabled
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.layout.FoldableState
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.layout.SplitScreenLayout
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.CodeTextStyle
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.FileTypeColors
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.MonoUppercaseTitleStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.BrowserUiState
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.FileSortOption
import com.draftpeek.feature.browser.model.GitHubImportState
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import com.draftpeek.feature.browser.viewmodel.GitEvent
import com.draftpeek.feature.browser.viewmodel.GitViewModel
import com.draftpeek.feature.browser.viewmodel.RecentFilesViewModel
import java.util.regex.Pattern
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */


// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */
internal enum class FileFilter {
    ALL,
    RECENT,
    FAVORITES,
    BOOKMARKS,
    CODE,
    DOCUMENTS,
    MEDIA
}


/**
 * 获取文件筛选选项的显示标签
 *
 * @param filter 文件筛选枚举
 * @return 对应的本地化字符串
 */
@Composable
private fun fileFilterLabel(filter: FileFilter): String = when (filter) {
    FileFilter.ALL -> stringResource(R.string.browser_filter_all)
    FileFilter.RECENT -> stringResource(R.string.browser_filter_recent)
    FileFilter.FAVORITES -> stringResource(R.string.browser_filter_favorites)
    FileFilter.BOOKMARKS -> stringResource(R.string.browser_filter_bookmarks)
    FileFilter.CODE -> stringResource(R.string.browser_filter_code)
    FileFilter.DOCUMENTS -> stringResource(R.string.browser_filter_documents)
    FileFilter.MEDIA -> stringResource(R.string.browser_filter_media)
}


// 代码文件扩展名集合
internal val CODE_EXTENSIONS = setOf(
    "kt", "kts", "java", "js", "ts", "jsx", "tsx", "mjs", "cjs", "mts", "cts",
    "py", "pyw", "c", "h", "cpp", "cc", "cxx", "hpp", "cs", "go", "rs", "swift",
    "rb", "php", "html", "htm", "css", "scss", "less", "sh", "bash", "zsh", "fish",
    "lua", "dart", "groovy", "gradle", "json", "yaml", "yml", "xml", "xsl", "xsd",
    "toml", "md", "markdown", "sql", "scala", "r", "dockerfile", "cmake", "make", "proto"
)


// 文档文件扩展名集合
internal val DOCUMENT_EXTENSIONS = setOf(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "rtf", "odt", "ods", "odp", "csv"
)


// 媒体文件扩展名集合
internal val MEDIA_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico", "heic", "heif",
    "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
    "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma"
)


/**
 * 文件浏览器主界面
 *
 * ## 功能概述
 * 提供文件系统浏览功能，支持：
 * - SAF 目录选择和导航
 * - 文件/文件夹列表展示（列表/网格视图）
 * - 文件筛选（全部、最近、收藏、书签、代码、文档、媒体）
 * - 文件搜索、排序
 * - Git 仓库状态显示和操作
 * - GitHub 仓库文件导入
 * - 文件比较选择模式
 * - 折叠屏适配
 *
 * ## 主要交互
 * - 点击文件：打开文件或进入文件夹
 * - 长按文件：显示操作菜单
 * - 下拉刷新：重新加载当前目录
 * - FAB 菜单：快速创建文件、导入文件、GitHub 导入
 *
 * @param onFileClick 文件点击回调
 * @param onCompareFiles 文件比较回调，参数为两个文件的 URI
 * @param onNavigateToHistory 导航到历史记录回调
 * @param onNavigateToSnippets 导航到代码片段回调
 * @param onNavigateToSamples 导航到示例文件回调
 * @param viewModel 文件浏览器 ViewModel
 * @param recentFilesViewModel 最近文件 ViewModel
 * @param gitViewModel Git 操作 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onFileClick: (FileItem) -> Unit = {},
    onCompareFiles: (leftUri: String, rightUri: String) -> Unit = { _, _ -> },
    onNavigateToSamples: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToTerminal: (String?) -> Unit = {},
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    foldInfo: FoldInfo = FoldInfo(),
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentTreeUri by viewModel.currentTreeUri.collectAsStateWithLifecycle()
    val gitRepoInfo by viewModel.gitRepoInfo.collectAsStateWithLifecycle()
    val gitFileStatuses by viewModel.gitFileStatuses.collectAsStateWithLifecycle()
    val isGitRepo by viewModel.isGitRepo.collectAsStateWithLifecycle()
    val internalFiles by viewModel.internalFiles.collectAsStateWithLifecycle()
    val currentSortOption by viewModel.currentSortOption.collectAsStateWithLifecycle()
    val pinnedFiles by viewModel.pinnedFiles.collectAsStateWithLifecycle()
    val bookmarkedUris by viewModel.bookmarkedUris.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    // 「手动排序」模式（屏 07 收敛）：默认隐藏每行拖拽手柄，进模式后才显示。
    var isManualOrderMode by rememberSaveable { mutableStateOf(false) }
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()

    // Feature flags
    val gitUiEnabled = isFeatureEnabled(FeatureFlag.GIT_UI)
    val terminalEnabled = isFeatureEnabled(FeatureFlag.TERMINAL)

    // Recent files ViewModel
    val recentFilesViewModel: RecentFilesViewModel = hiltViewModel()
    val recentFiles by recentFilesViewModel.recentFiles.collectAsStateWithLifecycle()
    val favoriteRecentFiles by recentFilesViewModel.favorites.collectAsStateWithLifecycle()

    // Git ViewModel
    val gitViewModel: GitViewModel = hiltViewModel()
    val gitUiState by gitViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onSaveExternalFile: (FileItem) -> Unit = { fileItem ->
        scope.launch {
            val saved = viewModel.saveExternalFileToInternal(fileItem)
            val msg = if (saved != null) {
                context.getString(R.string.browser_save_external_success)
            } else {
                context.getString(R.string.browser_save_external_failed)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    val onToggleBookmark: (String, String, String) -> Unit = { uri, fileName, directoryUri ->
        viewModel.toggleBookmark(uri, fileName, directoryUri)
    }
    // Delete file callback
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val onDeleteFile: (FileItem) -> Unit = { item ->
        fileToDelete = item
        showDeleteConfirmDialog = true
    }
    var hasSafPermission by rememberSaveable { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var showCreateFileDialog by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateSnippetDialog by rememberSaveable { mutableStateOf(false) }
    var showGitHubImportDialog by rememberSaveable { mutableStateOf(false) }
    var showGitActionSheet by rememberSaveable { mutableStateOf(false) }
    var showGitCommitDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showMoveToDialog by rememberSaveable { mutableStateOf(false) }
    var fileToMove by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileItem?>(null) }
    var fileToEncrypt by remember { mutableStateOf<FileItem?>(null) }
    var showEncryptExportDialog by rememberSaveable { mutableStateOf(false) }
    var fileToDecrypt by remember { mutableStateOf<FileItem?>(null) }
    var showDecryptDialog by rememberSaveable { mutableStateOf(false) }
    val onEncryptExportFile: (FileItem) -> Unit = { item ->
        fileToEncrypt = item
        showEncryptExportDialog = true
    }

    // Search and filter state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FileFilter.ALL) }

    // GitHub import state
    val gitHubImportState by viewModel.gitHubImportState.collectAsStateWithLifecycle()
    val gitHubSelectedFiles by viewModel.gitHubSelectedFiles.collectAsStateWithLifecycle()

    // Snippet ViewModel for creating files and snippets from this page
    val snippetViewModel: com.draftpeek.feature.browser.viewmodel.SnippetViewModel = hiltViewModel()

    // Wrap file-open callbacks so the ViewModel can record activity stats.
    val onOpenFile: (FileItem) -> Unit = onOpenFileLabel@{ item ->
        if (FileCipher.isEncryptedExport(item.name)) {
            fileToDecrypt = item
            showDecryptDialog = true
            return@onOpenFileLabel
        }
        viewModel.recordFileOpen()
        onFileClick(item)
    }

    // Refresh internal files list whenever this screen is composed
    LaunchedEffect(Unit) {
        viewModel.refreshInternalFiles()
    }

    // Clear the preview selection whenever the user switches to a different directory
    LaunchedEffect(currentTreeUri) {
        selectedFile = null
    }

    // Compare mode state
    var isCompareMode by rememberSaveable { mutableStateOf(false) }
    var compareSelection by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            hasSafPermission = true
            viewModel.onTreeUriSelected(uri)
        }
    }

    // File picker for opening external files directly (without copying)
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist read permission so the file can be reopened from history
            // after process death. Some ROMs reject this — ignore and continue.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Persistable permission not supported in this mode; proceed anyway.
            }
            onOpenFile(
                FileItem(
                    name = uri.lastPathSegment?.substringAfterLast('/') ?: "file",
                    uri = uri,
                    isDirectory = false,
                    size = 0L
                )
            )
        }
    }

    // File picker for importing files to internal storage (copy then open) - supports multi-select
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var successCount = 0
                var failCount = 0
                for (uri in uris) {
                    val imported = viewModel.importExternalFileToInternal(uri)
                    if (imported != null) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
                val message = when {
                    failCount == 0 -> context.getString(R.string.browser_import_multiple_success, successCount)
                    successCount == 0 -> context.getString(R.string.browser_import_multiple_failed)
                    else -> context.getString(R.string.browser_import_multiple_partial, successCount, failCount)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Sync permission state with current tree URI
    if (currentTreeUri != null && !hasSafPermission) {
        hasSafPermission = true
    }

    // Create file dialog
    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onCreate = { filename, language, initialContent ->
                showCreateFileDialog = false
                snippetViewModel.createFileInInternalStorage(filename, language, initialContent)
            }
        )
    }

    // Create folder dialog
    // Create folder dialog
    if (showCreateFolderDialog) {
        NewFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            viewModel = viewModel
        )
    }

    // Move to folder dialog
    if (showRenameDialog && fileToRename != null) {
        val renameTarget = fileToRename!!
        RenameFileDialog(
            fileToRename = renameTarget,
            onDismiss = {
                showRenameDialog = false
                fileToRename = null
            },
            viewModel = viewModel
        )
    }

    if (showMoveToDialog) {
        MoveFileDialog(
            fileToMove = fileToMove,
            isMultiSelectMode = isMultiSelectMode,
            selectedItems = selectedItems,
            onDismiss = {
                showMoveToDialog = false
                fileToMove = null
            },
            viewModel = viewModel
        )
    }
    // Create snippet dialog
    if (showCreateSnippetDialog) {
        com.draftpeek.feature.browser.ui.SnippetDetailDialog(
            onDismiss = { showCreateSnippetDialog = false },
            onSave = { title, content, language, category ->
                snippetViewModel.addSnippet(title, content, language, category)
                showCreateSnippetDialog = false
            },
            categories = emptyList()
        )
    }

    // GitHub import URL dialog
    if (showGitHubImportDialog) {
        GitHubImportUrlDialog(
            onDismiss = {
                showGitHubImportDialog = false
                viewModel.resetGitHubImportState()
            },
            onBrowse = { url ->
                showGitHubImportDialog = false
                viewModel.importFromGitHub(url)
            }
        )
    }

    // GitHub file selection dialogs
    when (val state = gitHubImportState) {
        is GitHubImportState.Loading -> {
            GitHubLoadingDialog(
                message = stringResource(R.string.browser_github_loading),
                onDismiss = { viewModel.resetGitHubImportState() }
            )
        }
        is GitHubImportState.FileList -> {
            GitHubFileSelectionDialog(
                owner = state.owner,
                repo = state.repo,
                files = state.files,
                selectedFiles = gitHubSelectedFiles,
                onToggleFile = { viewModel.toggleGitHubFileSelection(it) },
                onSelectAll = { viewModel.selectAllGitHubFiles() },
                onClearAll = { viewModel.clearGitHubFileSelection() },
                onConfirm = { viewModel.confirmGitHubImport() },
                onDismiss = { viewModel.resetGitHubImportState() }
            )
        }
        is GitHubImportState.Cloning -> {
            GitHubLoadingDialog(
                message = state.progressMessage ?: stringResource(R.string.browser_github_loading),
                onDismiss = { viewModel.resetGitHubImportState() }
            )
        }
        is GitHubImportState.Success -> {
            BrandDialog(
                onDismissRequest = {
                    viewModel.resetGitHubImportState()
                    viewModel.refreshInternalFiles()
                },
                title = { Text(stringResource(R.string.browser_github_import)) },
                content = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetGitHubImportState()
                        viewModel.refreshInternalFiles()
                    }) {
                        Text(stringResource(R.string.browser_action_got_it))
                    }
                }
            )
        }
        is GitHubImportState.Error -> {
            BrandDialog(
                onDismissRequest = { viewModel.resetGitHubImportState() },
                title = { Text(stringResource(R.string.browser_github_import)) },
                content = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetGitHubImportState() }) {
                        Text(stringResource(R.string.browser_action_retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetGitHubImportState() }) {
                        Text(stringResource(R.string.browser_action_cancel))
                    }
                }
            )
        }
        else -> { /* Idle */ }
    }

    // Git action sheet
    if (gitUiEnabled && showGitActionSheet && isGitRepo) {
        val branches = gitViewModel.getBranches()
        GitActionSheet(
            uiState = gitUiState,
            branches = branches,
            onDismiss = { showGitActionSheet = false },
            onCommit = {
                showGitActionSheet = false
                showGitCommitDialog = true
            },
            onPush = { gitViewModel.push() },
            onPull = { gitViewModel.pull() },
            onFetch = { gitViewModel.fetch() },
            onCheckout = { branch -> gitViewModel.checkout(branch) }
        )
    }

    // Git commit dialog
    if (gitUiEnabled && showGitCommitDialog) {
        GitCommitDialog(
            fileStatuses = gitUiState.fileStatuses,
            isCommitting = gitUiState.isCommitting,
            onCommit = { message, selectedFiles ->
                gitViewModel.commit(message, selectedFiles)
            },
            onDismiss = { showGitCommitDialog = false }
        )
    }

    // 「导入…」二级弹层（实施指导书 §2.2：FAB 只留创建类，导入类收进二级表）
    if (showImportSheet) {
        ImportActionsSheet(
            onDismiss = { showImportSheet = false },
            onSamples = {
                showImportSheet = false
                onNavigateToSamples()
            },
            onImportFromGitHub = {
                showImportSheet = false
                showGitHubImportDialog = true
            },
            onOpenExternalFile = {
                showImportSheet = false
                openFileLauncher.launch(arrayOf("*/*"))
            },
            onImportToInternal = {
                showImportSheet = false
                importFileLauncher.launch(arrayOf("*/*"))
            }
        )
    }

    // Delete file confirmation dialog
    if (showDeleteConfirmDialog && fileToDelete != null) {
        val deleteTarget = fileToDelete!!
        DeleteFileConfirmDialog(
            fileToDelete = deleteTarget,
            onDismiss = {
                showDeleteConfirmDialog = false
                fileToDelete = null
            },
            viewModel = viewModel
        )
    }

    // Encrypt export password dialog
    if (showEncryptExportDialog && fileToEncrypt != null) {
        val encryptTarget = fileToEncrypt!!
        EncryptExportDialog(
            fileToEncrypt = encryptTarget,
            onDismiss = {
                showEncryptExportDialog = false
                fileToEncrypt = null
            },
            viewModel = viewModel
        )
    }

    // Decrypt-open password dialog
    if (showDecryptDialog && fileToDecrypt != null) {
        val decryptTarget = fileToDecrypt!!
        DecryptOpenDialog(
            fileToDecrypt = decryptTarget,
            onDismiss = {
                showDecryptDialog = false
                fileToDecrypt = null
            },
            viewModel = viewModel,
            onFileClick = onFileClick
        )
    }

    // Collect Git events (success/error messages)
    LaunchedEffect(Unit) {
        gitViewModel.events.collect { event ->
            val message = when (event) {
                is GitEvent.ShowMessage -> event.message
                is GitEvent.ShowError -> event.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (event is GitEvent.ShowMessage) {
                showGitCommitDialog = false
                viewModel.refresh()
            }
        }
    }

    LaunchedEffect(currentTreeUri) {
        currentTreeUri?.let { uri ->
            gitViewModel.loadGitStatus(uri)
        }
    }

    // Listen for file creation event and navigate to editor
    LaunchedEffect(Unit) {
        snippetViewModel.createInternalFileEvent.collect { info ->
            val uri = "file://${info.file.absolutePath}"
            onOpenFile(
                FileItem(
                    name = info.file.name,
                    uri = android.net.Uri.parse(uri),
                    isDirectory = false,
                    size = info.file.length()
                )
            )
        }
    }

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    // NOTE: status bar padding is already handled by CustomScaffold via
    // DraftPeekNavHost's modifier – do NOT add extra topPadding here.

    // Derived: only recomputes when currentTreeUri changes, not on every
    // unrelated recomposition trigger.
    val isInSubdirectory by remember {
        derivedStateOf { currentTreeUri != null }
    }
    val currentPath by remember {
        derivedStateOf { (uiState as? BrowserUiState.Success)?.currentPath ?: "" }
    }

    // Reset SAF permission flag when returning to root
    LaunchedEffect(isInSubdirectory) {
        if (!isInSubdirectory) {
            hasSafPermission = false
        }
    }

    // Handle system back button to navigate up when in a subdirectory
    androidx.activity.compose.BackHandler(enabled = isInSubdirectory) {
        viewModel.navigateUp()
    }

    // ---- Virtual folder items (Recent / Favourites) ----
    // These are computed from data sources, not from the current directory listing.
    val recentOrder by viewModel.recentOrder.collectAsStateWithLifecycle()
    val pinnedOrder by viewModel.pinnedOrder.collectAsStateWithLifecycle()
    val internalFilesOrder by viewModel.internalFilesOrder.collectAsStateWithLifecycle()
    val bookmarkOrder by viewModel.bookmarkOrder.collectAsStateWithLifecycle()
    val recentFilesLimit by viewModel.recentFilesLimit.collectAsStateWithLifecycle()

    val recentFileItems by remember(selectedFilter, recentFiles, searchQuery, recentOrder) {
        mutableStateOf(
            if (selectedFilter == FileFilter.RECENT) {
                var items = recentFiles.map { it.toFileItem() }
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.lowercase()
                    items = items.filter { it.name.lowercase().contains(q) }
                }
                if (recentOrder.isNotEmpty()) {
                    val orderMap = recentOrder.withIndex().associate { it.value to it.index }
                    items = items.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
                }
                items
            } else {
                emptyList()
            }
        )
    }
    val favoriteFileItems by remember(selectedFilter, pinnedFiles, internalFiles, searchQuery, pinnedOrder) {
        mutableStateOf(
            if (selectedFilter == FileFilter.FAVORITES) {
                var items = mapPinnedUrisToFileItems(pinnedFiles, internalFiles)
                if (searchQuery.isNotBlank()) {
                    val q = searchQuery.lowercase()
                    items = items.filter { it.name.lowercase().contains(q) }
                }
                if (pinnedOrder.isNotEmpty()) {
                    val orderMap = pinnedOrder.withIndex().associate { it.value to it.index }
                    items = items.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
                }
                items
            } else {
                emptyList()
            }
        )
    }
    // 首页"最近打开"区块数据（数量由设置 recentFilesLimit 控制，独立于筛选/搜索状态）
    val homeRecentFiles by remember(recentFiles, recentFilesLimit) {
        mutableStateOf(recentFiles.take(recentFilesLimit))
    }
    // Derived: only recomputes when selectedFilter changes, avoiding
    // recomposition on unrelated state changes.
    val isVirtualFolderActive by remember {
        derivedStateOf { selectedFilter == FileFilter.RECENT || selectedFilter == FileFilter.FAVORITES }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- TopBar ----
            BrandTopBar(
                title = stringResource(R.string.browser_page_title),
                titleStyle = MonoUppercaseTitleStyle,
                onBack = if (isInSubdirectory) {
                    { viewModel.navigateUp() }
                } else {
                    null
                },
                actions = {
                    if (isGitRepo && gitUiEnabled) {
                        BrandIconButton(
                            onClick = {
                                showGitActionSheet = true
                            },
                            modifier = Modifier.accessibilityEnhanced(
                                contentDescription = stringResource(R.string.browser_git_action_sheet),
                                stateDescription = if (gitUiState.modifiedCount >
                                    0
                                ) {
                                    stringResource(
                                        R.string.browser_git_state_modified_count,
                                        gitUiState.modifiedCount
                                    )
                                } else {
                                    stringResource(R.string.browser_git_state_no_modification)
                                }
                            )
                        ) {
                            StrokeIcon(
                                icon = StrokeIcons.GitBranch,
                                contentDescription = stringResource(R.string.browser_git_action_sheet),
                                tint = if (gitUiState.modifiedCount >
                                    0
                                ) {
                                    PrototypeTokens.accent
                                } else {
                                    PrototypeTokens.muted
                                }
                            )
                        }
                    }

                    // 「手动排序」模式开关（实施指导书 §2.9 屏 07）：手柄由常驻改为按需显示。
                    BrandIconButton(
                        onClick = { isManualOrderMode = !isManualOrderMode },
                        modifier = Modifier.accessibilityEnhanced(
                            contentDescription = stringResource(R.string.browser_action_manual_order),
                            stateDescription = if (isManualOrderMode) {
                                stringResource(R.string.browser_manual_order_state_on)
                            } else {
                                stringResource(R.string.browser_manual_order_state_off)
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = null,
                            tint = if (isManualOrderMode) {
                                PrototypeTokens.accent
                            } else {
                                PrototypeTokens.muted
                            }
                        )
                    }

                    // 浏览类入口升为顶栏常驻图标（实施指导书 §2.2）：History / Snippets 不再藏在 FAB 里
                    BrandIconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.accessibilityEnhanced(
                            contentDescription = stringResource(R.string.browser_history_content_desc)
                        )
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Clock,
                            contentDescription = stringResource(R.string.browser_history_content_desc),
                            tint = PrototypeTokens.muted
                        )
                    }
                    BrandIconButton(
                        onClick = onNavigateToSnippets,
                        modifier = Modifier.accessibilityEnhanced(
                            contentDescription = stringResource(R.string.browser_action_snippets)
                        )
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Braces,
                            contentDescription = stringResource(R.string.browser_action_snippets),
                            tint = PrototypeTokens.muted
                        )
                    }
                    // 终端从 FAB 迁出后落在这里（仅 flag 打开时出现），保证 FAB 只有 3 项
                    if (terminalEnabled) {
                        BrandIconButton(
                            onClick = { onNavigateToTerminal(null) },
                            modifier = Modifier.accessibilityEnhanced(
                                contentDescription = stringResource(R.string.browser_fab_terminal)
                            )
                        ) {
                            StrokeIcon(
                                icon = StrokeIcons.Terminal,
                                contentDescription = stringResource(R.string.browser_fab_terminal),
                                tint = PrototypeTokens.muted
                            )
                        }
                    }
                }
            )

            // ---- Search Bar ----
            BrandSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.browser_search_hint),
                modifier = Modifier
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                    .padding(top = 12.dp)
            )

            // ---- Filter Chips Row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileFilter.entries.forEach { filter ->
                    BrandChip(
                        text = fileFilterLabel(filter),
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            // ---- Breadcrumb (when in subdirectory) ----
            if (isInSubdirectory && currentPath.isNotEmpty()) {
                BreadcrumbBar(
                    path = currentPath,
                    onNavigateUp = { viewModel.navigateUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ---- Content Area ----
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isVirtualFolderActive -> {
                        VirtualFolderContent(
                            selectedFilter = selectedFilter,
                            recentFileItems = recentFileItems,
                            favoriteFileItems = favoriteFileItems,
                            viewModel = viewModel,
                            onOpenFile = onOpenFile,
                            onToggleBookmark = onToggleBookmark,
                            onDeleteFile = onDeleteFile,
                            onEncryptExportFile = onEncryptExportFile,
                            onMoveTo = { fileItem ->
                                fileToMove = fileItem
                                showMoveToDialog = true
                            },
                            onRename = { fileItem ->
                                fileToRename = fileItem
                                showRenameDialog = true
                            },
                            isMultiSelectMode = isMultiSelectMode,
                            isManualOrderMode = isManualOrderMode,
                            selectedItems = selectedItems
                        )
                    }
                    !hasSafPermission -> {
                        InternalFileListContent(
                            internalFiles = internalFiles,
                            internalFilesOrder = internalFilesOrder,
                            bookmarkOrder = bookmarkOrder,
                            homeRecentFiles = homeRecentFiles,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            viewModel = viewModel,
                            onOpenFile = onOpenFile,
                            onToggleBookmark = onToggleBookmark,
                            onDeleteFile = onDeleteFile,
                            onEncryptExportFile = onEncryptExportFile,
                            onMoveTo = { fileItem ->
                                fileToMove = fileItem
                                showMoveToDialog = true
                            },
                            onRename = { fileItem ->
                                fileToRename = fileItem
                                showRenameDialog = true
                            },
                            isMultiSelectMode = isMultiSelectMode,
                            isManualOrderMode = isManualOrderMode,
                            selectedItems = selectedItems,
                            onSelectDirectory = { safLauncher.launch(null) },
                            onShowGitHubImport = { showGitHubImportDialog = true },
                            onShowCreateFile = { showCreateFileDialog = true }
                        )
                    }
                    uiState is BrowserUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    uiState is BrowserUiState.Error -> {
                        ErrorContent(
                            message = (uiState as BrowserUiState.Error).message,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    uiState is BrowserUiState.Success -> {
                        val files = (uiState as BrowserUiState.Success).files
                        val isHalfOpened = foldInfo.state == FoldableState.HALF_OPENED && foldInfo.isSeparating
                        val isFlatExpanded = foldInfo.state == FoldableState.FLAT || layoutMode == LayoutMode.EXPANDED

                        val onCompareFileClick: (FileItem) -> Unit = { item ->
                            if (item.isDirectory) {
                                viewModel.navigateTo(item)
                            } else {
                                val current = compareSelection.toMutableList()
                                val existingIdx = current.indexOfFirst { it.uri == item.uri }
                                if (existingIdx >= 0) {
                                    current.removeAt(existingIdx)
                                } else if (current.size < 2) {
                                    current.add(item)
                                }
                                compareSelection = current
                                if (current.size == 2) {
                                    onCompareFiles(
                                        current[0].uri.toString(),
                                        current[1].uri.toString()
                                    )
                                    isCompareMode = false
                                    compareSelection = emptyList()
                                }
                            }
                        }

                        if (isCompareMode) {
                            FileListContent(
                                files = files,
                                onFileClick = onCompareFileClick,
                                compareSelection = compareSelection,
                                currentSortOption = currentSortOption,
                                onSortOptionChange = { viewModel.changeSortOption(it) },
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDeleteFile,
                                onEncryptExport = onEncryptExportFile,
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onRename = { fileItem ->
                                    fileToRename = fileItem
                                    showRenameDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                isManualOrderMode = isManualOrderMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                layoutMode = layoutMode,
                                onSaveExternalFile = onSaveExternalFile,
                                modifier = Modifier
                            )
                        } else if (isHalfOpened) {
                            FoldableBrowserLayout(
                                files = files,
                                selectedFile = selectedFile,
                                onFileClick = { item ->
                                    if (item.isDirectory) {
                                        selectedFile = null
                                        viewModel.navigateTo(item)
                                    } else {
                                        selectedFile = item
                                    }
                                },
                                onOpenFile = onOpenFile,
                                foldInfo = foldInfo,
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDeleteFile,
                                onEncryptExport = onEncryptExportFile,
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onRename = { fileItem ->
                                    fileToRename = fileItem
                                    showRenameDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                isManualOrderMode = isManualOrderMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onSaveExternalFile = onSaveExternalFile,
                                onOpenTerminal = { item ->
                                    onNavigateToTerminal(FileUtils.resolveLocalPath(item.uri.toString()))
                                },
                                modifier = Modifier
                            )
                        } else if (isFlatExpanded) {
                            TwoPaneFileBrowser(
                                files = files,
                                selectedFile = selectedFile,
                                onFileClick = { item ->
                                    if (item.isDirectory) {
                                        selectedFile = null
                                        viewModel.navigateTo(item)
                                    } else {
                                        selectedFile = item
                                    }
                                },
                                onOpenFile = onOpenFile,
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDeleteFile,
                                onEncryptExport = onEncryptExportFile,
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onRename = { fileItem ->
                                    fileToRename = fileItem
                                    showRenameDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                isManualOrderMode = isManualOrderMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onSaveExternalFile = onSaveExternalFile,
                                onOpenTerminal = { item ->
                                    onNavigateToTerminal(FileUtils.resolveLocalPath(item.uri.toString()))
                                },
                                modifier = Modifier
                            )
                        } else {
                            // Compact list with grouped sections
                            val filtered = filterFiles(files, searchQuery, selectedFilter)
                            val dirs = filtered.filter { it.isDirectory }
                            val nonDirs = filtered.filter { !it.isDirectory }
                            val isEmpty =
                                filtered.isEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL

                            if (isEmpty) {
                                EmptyFilesContent(modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Git status section
                                    if (isGitRepo &&
                                        gitFileStatuses.isNotEmpty() &&
                                        searchQuery.isBlank() &&
                                        selectedFilter == FileFilter.ALL
                                    ) {
                                        item {
                                            SectionMonoHeader(title = stringResource(R.string.browser_changes_section))
                                        }
                                        items(gitFileStatuses, key = { "git_${it.filePath}" }) { gitStatus ->
                                            GitStatusItemComposable(
                                                gitFileStatus = gitStatus,
                                                modifier = Modifier.padding(
                                                    horizontal = PrototypeSpacing.ScreenHorizontal
                                                )
                                            )
                                        }
                                    }

                                    if (dirs.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_folders_section),
                                                count = dirs.size
                                            )
                                        }
                                        items(dirs, key = { it.uri.toString() }) { item ->
                                            val idx = dirs.indexOf(item)
                                            var showTerminalMenu by remember { mutableStateOf(false) }
                                            val localPath = remember(item.uri.toString()) {
                                                FileUtils.resolveLocalPath(item.uri.toString())
                                            }
                                            StaggeredAppearance(index = idx) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = { viewModel.navigateTo(item) },
                                                            onLongClick = { showTerminalMenu = true }
                                                        )
                                                ) {
                                                    BrandDirectoryCard(
                                                        directoryName = item.name,
                                                        onClick = {},
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                                                        itemCount = null
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = showTerminalMenu,
                                                onDismissRequest = { showTerminalMenu = false },
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(stringResource(R.string.browser_action_open_terminal))
                                                    },
                                                    leadingIcon = {
                                                        StrokeIcon(
                                                            icon = StrokeIcons.Terminal,
                                                            contentDescription = null,
                                                            tint = if (localPath !=
                                                                null
                                                            ) {
                                                                PrototypeTokens.accent
                                                            } else {
                                                                PrototypeTokens.muted
                                                            }
                                                        )
                                                    },
                                                    enabled = localPath != null,
                                                    onClick = {
                                                        showTerminalMenu = false
                                                        onNavigateToTerminal(localPath)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (nonDirs.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_files_section),
                                                count = nonDirs.size
                                            )
                                        }
                                        items(nonDirs, key = { it.uri.toString() }) { item ->
                                            val idx = nonDirs.indexOf(item) + dirs.size
                                            StaggeredAppearance(index = idx) {
                                                if (item.isExternal) {
                                                    ExternalFileCard(
                                                        item = item,
                                                        onClick = { clicked ->
                                                            if (clicked.isDirectory) {
                                                                viewModel.navigateTo(clicked)
                                                            } else {
                                                                onOpenFile(clicked)
                                                            }
                                                        },
                                                        onSave = onSaveExternalFile,
                                                        modifier = Modifier.padding(
                                                            horizontal = PrototypeSpacing.ScreenHorizontal
                                                        )
                                                    )
                                                } else {
                                                    FileItemComposable(
                                                        item = item,
                                                        onClick = { clicked ->
                                                            if (clicked.isDirectory) {
                                                                viewModel.navigateTo(clicked)
                                                            } else {
                                                                onOpenFile(clicked)
                                                            }
                                                        },
                                                        onTogglePin = { viewModel.togglePin(it) },
                                                        onToggleBookmark = onToggleBookmark,
                                                        onDelete = onDeleteFile,
                                                        onEncryptExport = onEncryptExportFile,
                                                        onMoveTo = { fileItem ->
                                                            fileToMove = fileItem
                                                            showMoveToDialog = true
                                                        },
                                                        onRename = { fileItem ->
                                                            fileToRename = fileItem
                                                            showRenameDialog = true
                                                        },
                                                        onMultiSelect = {
                                                            viewModel.toggleMultiSelectMode()
                                                            viewModel.toggleItemSelected(item.uri.toString())
                                                        },
                                                        onToggleSelect = { uri ->
                                                            viewModel.toggleItemSelected(uri)
                                                        },
                                                        isMultiSelectMode = isMultiSelectMode,
                                                        isManualOrderMode = isManualOrderMode,
                                                        isItemSelected = selectedItems.contains(item.uri.toString()),
                                                        currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 0.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // No results for search/filter
                                    if (filtered.isEmpty() &&
                                        (searchQuery.isNotBlank() || selectedFilter != FileFilter.ALL)
                                    ) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 48.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.browser_no_matching_files),
                                                    style = DraftPeekTypography.bodyMedium,
                                                    color = muted
                                                )
                                            }
                                        }
                                    }

                                    // Bottom padding for FAB
                                    item { Spacer(modifier = Modifier.height(96.dp)) }
                                }
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                }
            }
        }

        // ---- Multi-select bottom action bar ----
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(PrototypeTokens.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.browser_action_select_all),
                    color = PrototypeTokens.accent,
                    modifier = Modifier
                        .clickable { viewModel.selectAllItems() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.browser_action_deselect_all),
                    color = PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable { viewModel.clearSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.browser_action_move, selectedItems.size),
                    color = if (selectedItems.isNotEmpty()) PrototypeTokens.accent else PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable(enabled = selectedItems.isNotEmpty()) {
                            showMoveToDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.browser_action_cancel),
                    color = PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable { viewModel.toggleMultiSelectMode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // ---- FAB：仅创建（新建文件 / 新建文件夹 / 导入…）----
        // 导入类入口收进「导入…」二级弹层；History / Snippets 升为顶栏常驻图标；
        // 未授权目录时（空态授权卡）隐藏 FAB，避免与卡片上的「选择文件夹」主 CTA 重复
        // （实施指导书 §2.2 / §2.8 与待决策项 D1-A）。
        val showFab = currentTreeUri != null

        if (showFab) {
            val fabItems = listOf(
                FABMenuItem(
                    icon = StrokeIcons.Plus,
                    label = stringResource(R.string.browser_fab_new_file),
                    onClick = { showCreateFileDialog = true }
                ),
                FABMenuItem(
                    icon = StrokeIcons.FolderOutline,
                    label = stringResource(R.string.browser_fab_new_folder),
                    onClick = { showCreateFolderDialog = true }
                ),
                FABMenuItem(
                    icon = StrokeIcons.Download,
                    label = stringResource(R.string.browser_fab_import),
                    onClick = { showImportSheet = true }
                )
            )
            BrandFAB(
                items = fabItems,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(
                        end = PrototypeSpacing.FABRight,
                        bottom = 16.dp
                    )
            )
        }
    }
}


// ============================================================
// Breadcrumb bar for subdirectory navigation
// ============================================================
// Sort control — 文件分组行右侧的排序入口（屏 07）
// ============================================================

/**
 * 排序控件。
 *
 * 复用 ViewModel 中早已实现、此前从未接到 UI 的 `changeSortOption`，以及仓库里既有的
 * `browser_sort_*` 词条（原先全为未引用状态）。
 */
@Composable
internal fun SortOptionMenu(current: FileSortOption, onSelect: (FileSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted

    Box {
        Row(
            modifier = Modifier
                .clip(PrototypeShapes.Pill)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.browser_action_sort_by),
                style = MonoLabelStyle,
                color = muted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "▾", style = MonoLabelStyle, color = accent)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FileSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(fileSortOptionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}


/** [FileSortOption] 的本地化标签。 */
@Composable
private fun fileSortOptionLabel(option: FileSortOption): String = stringResource(
    when (option) {
        FileSortOption.NAME_ASC -> R.string.browser_sort_name_asc
        FileSortOption.NAME_DESC -> R.string.browser_sort_name_desc
        FileSortOption.SIZE_DESC -> R.string.browser_sort_size_desc
        FileSortOption.SIZE_ASC -> R.string.browser_sort_size_asc
        FileSortOption.MODIFIED_DESC -> R.string.browser_sort_modified_desc
        FileSortOption.MODIFIED_ASC -> R.string.browser_sort_modified_asc
        FileSortOption.TYPE_ASC -> R.string.browser_sort_type
    }
)


// ============================================================
// Breadcrumb bar for subdirectory navigation
// ============================================================
@Composable
private fun BreadcrumbBar(path: String, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val muted = PrototypeTokens.muted
    val fgSoft = PrototypeTokens.fgSoft
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border

    // Extract a readable display name from the path
    val displayName = path.substringAfterLast('/').substringAfterLast(':')
        .ifBlank { path.substringAfterLast(':').ifBlank { stringResource(R.string.browser_content_desc_folder) } }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onNavigateUp() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StrokeIcon(
            icon = StrokeIcons.FolderFilled,
            contentDescription = null,
            tint = PrototypeTokens.folder,
            modifier = Modifier.size(14.dp)
        )
        StrokeIcon(
            icon = StrokeIcons.ChevronRight,
            contentDescription = null,
            tint = muted.copy(alpha = 0.4f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = displayName,
            style = MonoLabelStyle.copy(fontSize = 11.sp),
            color = fgSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}


// ============================================================
// Small FAB menu item (kept for backward compatibility but not used
// in the new layout — BrandFAB replaces it)
// ============================================================

@Composable
private fun SmallFloatingActionMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    surface: Color,
    fg: Color,
    border: Color
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = DraftPeekTypography.bodySmall.copy(
                color = fg,
                fontWeight = FontWeight.Medium
            )
        )
    }
}


@Composable
private fun FoldableBrowserLayout(
    files: List<FileItem>,
    selectedFile: FileItem?,
    onFileClick: (FileItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    foldInfo: FoldInfo,
    modifier: Modifier = Modifier,
    gitFileStatuses: List<GitFileStatus> = emptyList(),
    isGitRepo: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    onDelete: ((FileItem) -> Unit)? = null,
    onMoveTo: ((FileItem) -> Unit)? = null,
    onRename: ((FileItem) -> Unit)? = null,
    onEncryptExport: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    isManualOrderMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null
) {
    val windowSize = LocalWindowInfo.current.containerSize

    if (foldInfo.orientation == FoldingFeature.Orientation.HORIZONTAL) {
        val screenHeightPx = windowSize.height
        val splitRatio = if (screenHeightPx > 0) {
            foldInfo.bounds.top.toFloat() / screenHeightPx
        } else {
            0.5f
        }

        Column(modifier = modifier.fillMaxSize()) {
            FileListContent(
                files = files,
                onFileClick = onFileClick,
                selectedFile = selectedFile,
                gitFileStatuses = gitFileStatuses,
                isGitRepo = isGitRepo,
                onTogglePin = onTogglePin,
                onToggleBookmark = onToggleBookmark,
                onDelete = onDelete,
                onEncryptExport = onEncryptExport,
                onMoveTo = onMoveTo,
                onRename = onRename,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
                isManualOrderMode = isManualOrderMode,
                selectedItems = selectedItems,
                currentDirectoryUri = currentDirectoryUri,
                onSaveExternalFile = onSaveExternalFile,
                onOpenTerminal = onOpenTerminal,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(splitRatio)
            )
            HorizontalDivider()
            FilePreviewPane(
                selectedFile = selectedFile,
                onOpenFile = onOpenFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - splitRatio)
            )
        }
    } else {
        val screenWidthPx = windowSize.width
        val splitRatio = if (screenWidthPx > 0) {
            foldInfo.bounds.left.toFloat() / screenWidthPx
        } else {
            0.4f
        }

        Row(modifier = modifier.fillMaxSize()) {
            FileListContent(
                files = files,
                onFileClick = onFileClick,
                selectedFile = selectedFile,
                gitFileStatuses = gitFileStatuses,
                isGitRepo = isGitRepo,
                onTogglePin = onTogglePin,
                onToggleBookmark = onToggleBookmark,
                onDelete = onDelete,
                onEncryptExport = onEncryptExport,
                onMoveTo = onMoveTo,
                onRename = onRename,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
                isManualOrderMode = isManualOrderMode,
                selectedItems = selectedItems,
                currentDirectoryUri = currentDirectoryUri,
                onSaveExternalFile = onSaveExternalFile,
                onOpenTerminal = onOpenTerminal,
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxHeight()
            )
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )
            FilePreviewPane(
                selectedFile = selectedFile,
                onOpenFile = onOpenFile,
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxHeight()
            )
        }
    }
}


@Composable
private fun TwoPaneFileBrowser(
    files: List<FileItem>,
    selectedFile: FileItem?,
    onFileClick: (FileItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    gitFileStatuses: List<GitFileStatus> = emptyList(),
    isGitRepo: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    onDelete: ((FileItem) -> Unit)? = null,
    onMoveTo: ((FileItem) -> Unit)? = null,
    onRename: ((FileItem) -> Unit)? = null,
    onEncryptExport: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    isManualOrderMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null
) {
    SplitScreenLayout(
        startContent = {
            FileListContent(
                files = files,
                onFileClick = onFileClick,
                selectedFile = selectedFile,
                gitFileStatuses = gitFileStatuses,
                isGitRepo = isGitRepo,
                onTogglePin = onTogglePin,
                onToggleBookmark = onToggleBookmark,
                onDelete = onDelete,
                onEncryptExport = onEncryptExport,
                onMoveTo = onMoveTo,
                onRename = onRename,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
                isManualOrderMode = isManualOrderMode,
                selectedItems = selectedItems,
                currentDirectoryUri = currentDirectoryUri,
                onSaveExternalFile = onSaveExternalFile,
                onOpenTerminal = onOpenTerminal,
                modifier = Modifier.fillMaxSize()
            )
        },
        endContent = {
            FilePreviewPane(
                selectedFile = selectedFile,
                onOpenFile = onOpenFile,
                modifier = Modifier.fillMaxSize()
            )
        },
        modifier = modifier,
        initialSplitRatio = 0.4f,
        minSplitRatio = 0.25f,
        maxSplitRatio = 0.75f
    )
}
