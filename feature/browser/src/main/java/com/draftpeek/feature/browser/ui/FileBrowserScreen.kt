package com.draftpeek.feature.browser.ui

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitHubFileEntry
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.data.entity.RecentFile
import kotlin.math.roundToInt
import java.util.regex.Pattern
import com.draftpeek.core.ui.component.BrandChip
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandDirectoryCard
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.component.BrandFAB
import com.draftpeek.core.ui.component.BrandFileCard
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandSearchBar
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.FABMenuItem
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.util.FileUtils
import com.draftpeek.core.common.util.InputValidator
import com.draftpeek.core.common.util.ValidationResult
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.FileTypeColors
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.layout.FoldableState
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.layout.SplitScreenLayout
import com.draftpeek.core.ui.modifier.minimumTouchTarget
import com.draftpeek.feature.browser.model.BrowserUiState
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.GitHubImportState
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import com.draftpeek.feature.browser.viewmodel.GitEvent
import com.draftpeek.feature.browser.viewmodel.GitViewModel
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.viewmodel.RecentFilesViewModel
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.CodeTextStyle
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.MonoUppercaseTitleStyle
import com.draftpeek.core.ui.composition.LocalFeatureToggle
import com.draftpeek.core.ui.composition.isFeatureEnabled

// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */
private enum class FileFilter {
    ALL,
    RECENT,
    FAVORITES,
    BOOKMARKS,
    CODE,
    DOCUMENTS,
    MEDIA,
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
private val CODE_EXTENSIONS = setOf(
    "kt", "kts", "java", "js", "ts", "jsx", "tsx", "mjs", "cjs", "mts", "cts",
    "py", "pyw", "c", "h", "cpp", "cc", "cxx", "hpp", "cs", "go", "rs", "swift",
    "rb", "php", "html", "htm", "css", "scss", "less", "sh", "bash", "zsh", "fish",
    "lua", "dart", "groovy", "gradle", "json", "yaml", "yml", "xml", "xsl", "xsd",
    "toml", "md", "markdown", "sql", "scala", "r", "dockerfile", "cmake", "make", "proto",
)

// 文档文件扩展名集合
private val DOCUMENT_EXTENSIONS = setOf(
    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
    "txt", "rtf", "odt", "ods", "odp", "csv",
)

// 媒体文件扩展名集合
private val MEDIA_EXTENSIONS = setOf(
    "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "ico", "heic", "heif",
    "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm",
    "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma",
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
    viewModel: FileBrowserViewModel = hiltViewModel(),
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
    var showCreateSnippetDialog by rememberSaveable { mutableStateOf(false) }
    var showGitHubImportDialog by rememberSaveable { mutableStateOf(false) }
    var showGitActionSheet by rememberSaveable { mutableStateOf(false) }
    var showGitCommitDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showMoveToDialog by rememberSaveable { mutableStateOf(false) }
    var fileToMove by remember { mutableStateOf<FileItem?>(null) }

    // Search and filter state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(FileFilter.ALL) }

    // GitHub import state
    val gitHubImportState by viewModel.gitHubImportState.collectAsStateWithLifecycle()
    val gitHubSelectedFiles by viewModel.gitHubSelectedFiles.collectAsStateWithLifecycle()

    // Snippet ViewModel for creating files and snippets from this page
    val snippetViewModel: com.draftpeek.feature.browser.viewmodel.SnippetViewModel = hiltViewModel()

    // Wrap file-open callbacks so the ViewModel can record activity stats.
    val onOpenFile: (FileItem) -> Unit = { item ->
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
                    size = 0L,
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
            },
        )
    }

    // Create folder dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        BrandDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(stringResource(R.string.browser_dialog_new_folder_title)) },
            content = {
                BrandOutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.browser_hint_enter_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        if (folderName.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.browser_error_folder_name_empty), Toast.LENGTH_SHORT).show()
                            return@BrandFilledButton
                        }
                        val existing = viewModel.getInternalFolders().any { it.name == folderName }
                        if (existing) {
                            Toast.makeText(context, context.getString(R.string.browser_error_folder_exists), Toast.LENGTH_SHORT).show()
                            return@BrandFilledButton
                        }
                        val created = viewModel.createFolder(folderName)
                        if (created) {
                            Toast.makeText(context, context.getString(R.string.browser_folder_create_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.browser_folder_create_failed), Toast.LENGTH_SHORT).show()
                        }
                        showCreateFolderDialog = false
                    },
                ) {
                    Text(stringResource(R.string.browser_action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            },
        )
    }

    // Move to folder dialog
    if (showMoveToDialog) {
        val folders = viewModel.getInternalFolders()
        val isMultiSelect = isMultiSelectMode && selectedItems.isNotEmpty()
        BrandDialog(
            onDismissRequest = { showMoveToDialog = false; fileToMove = null },
            title = { Text(stringResource(R.string.browser_dialog_move_to_title)) },
            content = {
                Column {
                    // Move to internal storage root
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_move_to_internal)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = PrototypeTokens.accent,
                            )
                        },
                        onClick = {
                            val userFilesDir = com.draftpeek.core.common.util.AppFileManager.getUserFilesDir(context)
                            val rootUri = com.draftpeek.core.common.util.AppFileManager.fileToInternalUri(userFilesDir)
                            if (isMultiSelect) {
                                val count = viewModel.moveSelectedFiles(rootUri)
                                Toast.makeText(
                                    context,
                                    if (count > 0) context.getString(R.string.browser_move_success, count)
                                    else context.getString(R.string.browser_move_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                val item = fileToMove
                                if (item != null) {
                                    val moved = viewModel.moveFile(item.uri.toString(), rootUri)
                                    Toast.makeText(
                                        context,
                                        if (moved) context.getString(R.string.browser_move_success, 1)
                                        else context.getString(R.string.browser_move_failed),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                            showMoveToDialog = false
                            fileToMove = null
                        },
                    )
                    if (folders.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = PrototypeTokens.accent,
                                    )
                                },
                                onClick = {
                                    val folderUri = com.draftpeek.core.common.util.AppFileManager.fileToInternalUri(folder)
                                    if (isMultiSelect) {
                                        val count = viewModel.moveSelectedFiles(folderUri)
                                        Toast.makeText(
                                            context,
                                            if (count > 0) context.getString(R.string.browser_move_success, count)
                                            else context.getString(R.string.browser_move_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        val item = fileToMove
                                        if (item != null) {
                                            val moved = viewModel.moveFile(item.uri.toString(), folderUri)
                                            Toast.makeText(
                                                context,
                                                if (moved) context.getString(R.string.browser_move_success, 1)
                                                else context.getString(R.string.browser_move_failed),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                    showMoveToDialog = false
                                    fileToMove = null
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveToDialog = false; fileToMove = null }) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            },
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
            categories = emptyList(),
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
            },
        )
    }

    // GitHub file selection dialogs
    when (val state = gitHubImportState) {
        is GitHubImportState.Loading -> {
            GitHubLoadingDialog(
                message = stringResource(R.string.browser_github_loading),
                onDismiss = { viewModel.resetGitHubImportState() },
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
                onDismiss = { viewModel.resetGitHubImportState() },
            )
        }
        is GitHubImportState.Cloning -> {
            GitHubLoadingDialog(
                message = state.progressMessage ?: stringResource(R.string.browser_github_loading),
                onDismiss = { viewModel.resetGitHubImportState() },
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
            onCheckout = { branch -> gitViewModel.checkout(branch) },
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
            onDismiss = { showGitCommitDialog = false },
        )
    }

    // Delete file confirmation dialog
    if (showDeleteConfirmDialog && fileToDelete != null) {
        val fileItem = fileToDelete!!
        BrandDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                fileToDelete = null
            },
            title = { Text(stringResource(R.string.browser_dialog_delete_file_title)) },
            content = {
                Text(
                    text = stringResource(R.string.browser_dialog_delete_file_message, fileItem.name),
                    style = DraftPeekTypography.bodyMedium,
                )
            },
            confirmButton = {
                BrandFilledButton(
                    onClick = {
                        scope.launch {
                            val deleted = viewModel.deleteInternalFile(fileItem)
                            val msg = if (deleted) {
                                context.getString(R.string.browser_delete_success)
                            } else {
                                context.getString(R.string.browser_delete_failed)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirmDialog = false
                        fileToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.browser_action_delete_file))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        fileToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.browser_action_cancel))
                }
            }
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
                    size = info.file.length(),
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
            } else emptyList()
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
            } else emptyList()
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

    // Helper to filter files based on search + filter chip (for non-virtual-folder filters)
    fun filterFiles(files: List<FileItem>): List<FileItem> {
        var result = files
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.name.lowercase().contains(q) }
        }
        result = when (selectedFilter) {
            FileFilter.ALL -> result
            FileFilter.RECENT -> result.filter { !it.isDirectory && it.lastModified > 0 &&
                (System.currentTimeMillis() - it.lastModified) < 7 * 24 * 60 * 60 * 1000L }
            FileFilter.FAVORITES -> result.filter { it.isPinned }
            FileFilter.BOOKMARKS -> result.filter { it.isBookmarked }
            FileFilter.CODE -> result.filter { !it.isDirectory && it.extension.lowercase() in CODE_EXTENSIONS }
            FileFilter.DOCUMENTS -> result.filter { !it.isDirectory && it.extension.lowercase() in DOCUMENT_EXTENSIONS }
            FileFilter.MEDIA -> result.filter { !it.isDirectory && it.extension.lowercase() in MEDIA_EXTENSIONS }
        }
        return result
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // ---- TopBar ----
            BrandTopBar(
                title = stringResource(R.string.browser_page_title),
                titleStyle = MonoUppercaseTitleStyle,
                onBack = if (isInSubdirectory) {{ viewModel.navigateUp() }} else null,
                actions = {
                    if (isGitRepo && gitUiEnabled) {
                        IconButton(
                            onClick = {
                                showGitActionSheet = true
                            },
                            modifier = Modifier.accessibilityEnhanced(
                                contentDescription = stringResource(R.string.browser_git_action_sheet),
                                stateDescription = if (gitUiState.modifiedCount > 0) "有 ${gitUiState.modifiedCount} 个修改" else "无修改",
                            ),
                        ) {
                            StrokeIcon(
                                icon = StrokeIcons.GitBranch,
                                contentDescription = stringResource(R.string.browser_git_action_sheet),
                                tint = if (gitUiState.modifiedCount > 0) PrototypeTokens.accent else PrototypeTokens.muted,
                            )
                        }
                    }
                },
            )

            // ---- Search Bar ----
            BrandSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.browser_search_hint),
                modifier = Modifier
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                    .padding(top = 12.dp),
            )

            // ---- Filter Chips Row ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FileFilter.entries.forEach { filter ->
                    BrandChip(
                        text = fileFilterLabel(filter),
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
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
                        .padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ---- Content Area ----
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isVirtualFolderActive -> {
                        // Virtual folder view (Recent / Favourites) — overrides normal listing
                        val virtualItems = if (selectedFilter == FileFilter.RECENT) recentFileItems else favoriteFileItems
                        val sectionTitle = if (selectedFilter == FileFilter.RECENT) {
                            stringResource(R.string.browser_recent_section)
                        } else {
                            stringResource(R.string.browser_favorites_section)
                        }
                        val emptyText = if (selectedFilter == FileFilter.RECENT) {
                            stringResource(R.string.browser_no_recent_files)
                        } else {
                            stringResource(R.string.browser_no_favorite_files)
                        }

                        if (virtualItems.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = if (selectedFilter == FileFilter.RECENT) Icons.Filled.History else Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = muted.copy(alpha = 0.35f),
                                    modifier = Modifier.size(64.dp),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = emptyText,
                                    style = DraftPeekTypography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = fg.copy(alpha = 0.6f),
                                )
                            }
                        } else {
                            val scope = rememberCoroutineScope()
                            val vListState = rememberLazyListState()
                            val density = LocalDensity.current
                            // ── 拖拽控制器（逻辑已提取到 DragDropController）──
                            val dragController = remember { DragDropController(density, vListState) }
                            var vSaveJob by remember { mutableStateOf<Job?>(null) }
                            // 本地可变 state 副本（拖动过程中不修改真实顺序，仅用于同步外部 Flow）
                            var localVirtualItems by remember {
                                mutableStateOf(virtualItems.toList())
                            }

                            LaunchedEffect(virtualItems) {
                                // 只有在不处于拖动状态时才同步外部变化（避免用户拖动被重置）
                                if (dragController.dragStartIndex == -1) {
                                    localVirtualItems = virtualItems.toList()
                                }
                            }

                            fun vSaveOrder() {
                                vSaveJob?.cancel()
                                val order = localVirtualItems.map { it.uri.toString() }
                                vSaveJob = scope.launch {
                                    if (selectedFilter == FileFilter.RECENT) {
                                        viewModel.saveRecentOrder(order)
                                    } else {
                                        viewModel.savePinnedOrder(order)
                                    }
                                }
                            }

                            LaunchedEffect(dragController.isDragging) {
                                if (dragController.isDragging) {
                                    dragController.freezeBounds(localVirtualItems.size)
                                    // 启动自动滚动循环
                                    dragController.autoScrollJob?.cancel()
                                    dragController.autoScrollJob = launch {
                                        while (dragController.dragStartIndex >= 0) {
                                            val fingerInViewport = dragController.fingerInViewport()
                                            val delta = dragController.autoScrollDelta(fingerInViewport)
                                            if (delta != 0f) {
                                                val consumed = vListState.scrollBy(delta)
                                                if (consumed != 0f) {
                                                    dragController.onAutoScrollConsumed(consumed, localVirtualItems.size)
                                                }
                                            }
                                            withFrameNanos { }
                                        }
                                    }
                                } else {
                                    dragController.autoScrollJob?.cancel()
                                    dragController.autoScrollJob = null
                                    dragController.clearFrozenBounds()
                                }
                            }

                            LazyColumn(
                                state = vListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { dragController.viewportHeight = it.height.toFloat() }
                                    .pointerInput(selectedFilter) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                val localY = offset.y
                                                val hit = dragController.resolveTarget(forStart = true, localY = localY, itemCount = localVirtualItems.size)
                                                if (hit in localVirtualItems.indices) {
                                                    dragController.onDragStart(hit, localY)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                if (dragController.dragStartIndex < 0) return@detectDragGesturesAfterLongPress
                                                dragController.onDragUpdate(dragAmount.y, localVirtualItems.size)
                                            },
                                            onDragEnd = {
                                                if (dragController.dragStartIndex >= 0) {
                                                    val target = dragController.resolveTargetIndex(dragController.dragStartIndex, dragController.dragInsertionIndex)
                                                        .coerceIn(localVirtualItems.indices)
                                                    if (target != dragController.dragStartIndex) {
                                                        val newList = localVirtualItems.toMutableList()
                                                        val moved = newList.removeAt(dragController.dragStartIndex)
                                                        newList.add(target, moved)
                                                        localVirtualItems = newList
                                                        vSaveOrder()
                                                    }
                                                    dragController.markAnimateBack()
                                                }
                                                scope.launch {
                                                    delay(260)
                                                    dragController.resetDragState()
                                                }
                                            },
                                            onDragCancel = {
                                                dragController.markAnimateBack()
                                                scope.launch {
                                                    delay(260)
                                                    dragController.resetDragState()
                                                }
                                            },
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                item {
                                    SectionMonoHeader(
                                        title = sectionTitle,
                                        count = localVirtualItems.size,
                                    )
                                }
                                itemsIndexed(localVirtualItems, key = { idx, it -> "vf_${it.uri.toString()}_$idx" }) { index, item ->
                                    val isDraggingNow = index == dragController.dragStartIndex
                                    val insertionAtThis = dragController.dragInsertionIndex == index && !isDraggingNow
                                    val stepPx: Float = run {
                                        val frozen = dragController.frozenAbsBounds
                                        if (frozen.isNotEmpty()) {
                                            frozen[index]?.step
                                                ?: ((frozen[index]?.height ?: dragController.defaultRowHeightPx) + dragController.itemSpacingPx.toFloat())
                                        } else {
                                            val cur = dragController.rowBounds[index]
                                            val next = dragController.rowBounds[index + 1]
                                            if (cur != null && next != null) {
                                                val d = next.top - cur.top
                                                if (d > 0) return@run d
                                            }
                                            (cur?.height?.toFloat() ?: dragController.defaultRowHeightPx) + dragController.itemSpacingPx.toFloat()
                                        }
                                    }
                                    val rowDisplacement = when {
                                        !dragController.isDragging -> 0f
                                        dragController.dragInsertionIndex > dragController.dragStartIndex &&
                                            index > dragController.dragStartIndex &&
                                            index < dragController.dragInsertionIndex -> -stepPx
                                        dragController.dragInsertionIndex < dragController.dragStartIndex &&
                                            index >= dragController.dragInsertionIndex &&
                                            index < dragController.dragStartIndex -> stepPx
                                        else -> 0f
                                    }
                                    val transition = updateTransition(
                                        targetState = isDraggingNow,
                                        label = "vf_item_$index",
                                    )
                                    val scale by transition.animateFloat(
                                        transitionSpec = {
                                            if (targetState) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                            else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                        },
                                        label = "scale_v_$index",
                                    ) { d -> if (d) 1.06f else 1f }
                                    val cardElevPx by transition.animateFloat(
                                        transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                                        label = "elev_v_$index",
                                    ) { d -> if (d) 18f else 0f }
                                    val bgAlpha by transition.animateFloat(
                                        transitionSpec = { tween(180) },
                                        label = "alpha_v_$index",
                                    ) { d -> if (d) 0.96f else 1f }
                                    val translationY by animateFloatAsState(
                                        targetValue = when {
                                            isDraggingNow && !dragController.animateBack -> dragController.dragOffset
                                            else -> rowDisplacement
                                        },
                                        animationSpec = if (dragController.animateBack || !isDraggingNow) {
                                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                        } else {
                                            spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                                        },
                                        label = "vf_trans_$index",
                                    )
                                    StaggeredAppearance(index = index) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .zIndex(if (isDraggingNow) 1f else 0f)
                                                .onGloballyPositioned { coords ->
                                                    val h = coords.size.height
                                                    val info = vListState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == index + 1 } // +1 因为 header 占 index=0
                                                    val top = (info?.offset ?: 0).toFloat()
                                                    val bottom = top + h
                                                    dragController.rowBounds[index] = VRowBounds(index, top, bottom, h)
                                                }
                                                .graphicsLayer {
                                                    this.alpha = bgAlpha
                                                    this.scaleX = scale
                                                    this.scaleY = scale
                                                }
                                                .offset { IntOffset(0, translationY.roundToInt()) }
                                                .then(
                                                    if (isDraggingNow) Modifier.shadow(
                                                        with(density) { cardElevPx.toDp() },
                                                        shape = BrandShapes.Card,
                                                        clip = false,
                                                    ) else Modifier
                                                ),
                                        ) {
                                            if (insertionAtThis) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .padding(horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(2.dp)
                                                            .background(
                                                                color = accent,
                                                                shape = RoundedCornerShape(999.dp),
                                                            ),
                                                    )
                                                }
                                            }
                                            FileItemComposable(
                                                item = item,
                                                onClick = { clickedItem ->
                                                    if (!dragController.isDragging && !clickedItem.isDirectory) {
                                                        onOpenFile(clickedItem)
                                                    }
                                                },
                                                onTogglePin = { viewModel.togglePin(it) },
                                                onToggleBookmark = onToggleBookmark,
                                                onDelete = onDeleteFile,
                                                onMoveTo = { fileItem ->
                                                    fileToMove = fileItem
                                                    showMoveToDialog = true
                                                },
                                                onMultiSelect = {
                                                    viewModel.toggleMultiSelectMode()
                                                    viewModel.toggleItemSelected(item.uri.toString())
                                                },
                                                onToggleSelect = { uri ->
                                                    viewModel.toggleItemSelected(uri)
                                                },
                                                isMultiSelectMode = isMultiSelectMode,
                                                isItemSelected = selectedItems.contains(item.uri.toString()),
                                                currentDirectoryUri = "",
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                                // Bottom padding for FAB
                                item { Spacer(modifier = Modifier.height(96.dp)) }
                            }
                        }
                    }
                    !hasSafPermission -> {
                        if (internalFiles.isNotEmpty()) {
                            // Internal files list (app sandbox)
                            val filteredInternal0 = filterFiles(internalFiles)
                            // 应用内部文件整体顺序（文件夹和文件合并的顺序）
                            val orderedInternal = if (internalFilesOrder.isNotEmpty()) {
                                val orderMap = internalFilesOrder.withIndex().associate { it.value to it.index }
                                filteredInternal0.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
                            } else filteredInternal0

                            val dirs0 = orderedInternal.filter { it.isDirectory }
                            val files0 = orderedInternal.filter { !it.isDirectory }
                            val bookmarked0 = filteredInternal0.filter { it.isBookmarked }
                                .let { list ->
                                    if (bookmarkOrder.isNotEmpty()) {
                                        val orderMap = bookmarkOrder.withIndex().associate { it.value to it.index }
                                        list.sortedBy { orderMap[it.uri.toString()] ?: Int.MAX_VALUE }
                                    } else list
                                }

                            val scope = rememberCoroutineScope()
                            val iListState = rememberLazyListState()
                            val density = LocalDensity.current
                            // ── 新拖拽状态（分区独立拖动 + 绝对累计坐标，避免跨屏命中失效）──
                            var iDragSection by remember { mutableStateOf<String?>(null) }
                            var iDragStartIndex by remember { mutableIntStateOf(-1) }
                            var iDragInsertionIndex by remember { mutableIntStateOf(-1) }
                            var iDragStartFingerY by remember { mutableFloatStateOf(0f) }
                            var iDragOffset by remember { mutableFloatStateOf(0f) }
                            var iDragScrollOffset by remember { mutableFloatStateOf(0f) }
                            var iAnimateBack by remember { mutableStateOf(false) }
                            // 每行 top/bottom（以 section_idx 为 key，比如 "dir_0"），仅记录当前可见项
                            val iRowBounds = remember { mutableStateMapOf<String, IRowBounds>() }
                            // 全局绝对坐标（0 = 整个 LazyColumn 第一个逻辑项的顶部），拖动开始时构建，含所有不可见项
                            var iFrozenGlobalAbs by remember { mutableStateOf<Map<String, IAbsBounds>>(emptyMap()) }
                            // 拖动锚点
                            var iFirstVisibleIdxAtStart by remember { mutableIntStateOf(0) }
                            var iFirstVisibleOffAtStart by remember { mutableFloatStateOf(0f) }
                            var iOriginalViewportTopGlobalAbs by remember { mutableFloatStateOf(0f) }
                            var iViewportHeight by remember { mutableFloatStateOf(0f) }
                            var iAutoScrollJob by remember { mutableStateOf<Job?>(null) }

                            var iItemSpacingPx by remember { mutableIntStateOf(0) }
                            var iSaveJob by remember { mutableStateOf<Job?>(null) }

                            var localBookmarks by remember { mutableStateOf(bookmarked0.toList()) }
                            var localDirs by remember { mutableStateOf(dirs0.toList()) }
                            var localFiles by remember { mutableStateOf(files0.toList()) }

                            val iAutoScrollEdgePx: Float = with(density) { 80.dp.toPx() }
                            val iAutoScrollMaxStepPx: Float = with(density) { 6.dp.toPx() }
                            val iDefaultRowHeightPx: Float = with(density) { 64.dp.toPx() }
                            val iDefaultDirHeightPx: Float = with(density) { 72.dp.toPx() }
                            val iHeaderHeightPx: Float = with(density) { 48.dp.toPx() }

                            // 外部变化同步到本地（仅在不拖动时）
                            LaunchedEffect(bookmarked0) {
                                if (iDragSection != "bookmark") localBookmarks = bookmarked0.toList()
                            }
                            LaunchedEffect(dirs0) {
                                if (iDragSection != "dir") localDirs = dirs0.toList()
                            }
                            LaunchedEffect(files0) {
                                if (iDragSection != "file") localFiles = files0.toList()
                            }

                            LaunchedEffect(Unit) {
                                with(density) { iItemSpacingPx = 4.dp.toPx().toInt() }
                            }

                            fun iSaveOrder() {
                                iSaveJob?.cancel()
                                iSaveJob = scope.launch {
                                    viewModel.saveBookmarkOrder(localBookmarks.map { it.uri.toString() })
                                    val combined = (localDirs + localFiles).map { it.uri.toString() }
                                    viewModel.saveInternalFilesOrder(combined)
                                }
                            }

                            val hasBookmarks = localBookmarks.isNotEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL
                            val hasDirs = localDirs.isNotEmpty()
                            val hasFiles = localFiles.isNotEmpty()
                            val iIsDragging = iDragSection != null

                            // section + index 映射到 section 内的 list 引用
                            fun iSectionList(section: String): List<FileItem> = when (section) {
                                "bookmark" -> localBookmarks
                                "dir" -> localDirs
                                "file" -> localFiles
                                else -> emptyList()
                            }
                            fun iSectionRowHeight(section: String, idxInSection: Int): Float = when (section) {
                                "dir" -> iRowBounds["dir_$idxInSection"]?.height?.toFloat() ?: iDefaultDirHeightPx
                                else -> iRowBounds["${section}_$idxInSection"]?.height?.toFloat() ?: iDefaultRowHeightPx
                            }

                            /**
                             * 根据 firstVisible 累加 LazyColumn 逻辑项，返回命中的 (section, idxInSection)
                             * @param forInsertion true 返回插入索引 0..list.size；false 返回命中索引 list.indices
                             */
                            fun iHitTest(forInsertion: Boolean, localY: Float): Pair<String?, Int> {
                                val firstVisible = iListState.firstVisibleItemIndex
                                val firstVisibleOffset = iListState.firstVisibleItemScrollOffset
                                var currentY = -firstVisibleOffset.toFloat()
                                var logicalIdx = 0
                                fun consume(height: Float, onHit: () -> Pair<String?, Int>): Pair<String?, Int>? {
                                    val top = currentY
                                    val bottom = currentY + height + iItemSpacingPx
                                    if (localY in top..bottom) return onHit()
                                    currentY = bottom
                                    logicalIdx++
                                    return null
                                }
                                // 跳到 firstVisible：通过模拟 advance 直到达到 firstVisible
                                fun advanceIfNeeded(targetSection: String, listSize: Int, sectionHeader: Boolean) {
                                    if (logicalIdx >= firstVisible) return
                                    // header?
                                    if (sectionHeader) {
                                        logicalIdx++
                                        currentY += iHeaderHeightPx + iItemSpacingPx
                                        if (logicalIdx >= firstVisible) return
                                    }
                                    // items remaining
                                    val step = (firstVisible - logicalIdx).coerceAtMost(listSize)
                                    var hSum = 0f
                                    for (i in 0 until step) {
                                        hSum += iSectionRowHeight(targetSection, i) + iItemSpacingPx
                                        logicalIdx++
                                    }
                                    currentY += hSum
                                }
                                if (hasBookmarks) {
                                    advanceIfNeeded("bookmark", localBookmarks.size, sectionHeader = true)
                                    if (logicalIdx == firstVisible) { /* 对齐了 */ }
                                    if (hasBookmarks) {
                                        // header
                                        val r = consume(iHeaderHeightPx) { null to -1 }
                                        if (r != null) return r
                                        for (i in localBookmarks.indices) {
                                            val h = iSectionRowHeight("bookmark", i)
                                            val hit = consume(h) {
                                                if (!forInsertion) "bookmark" to i
                                                else {
                                                    val top = currentY
                                                    val bottom = currentY + h + iItemSpacingPx
                                                    val center = (top + bottom) / 2f
                                                    if (localY < center) "bookmark_insert" to i else "bookmark_insert" to (i + 1)
                                                }
                                            }
                                            if (hit != null) {
                                                return if (forInsertion && hit.first == "bookmark_insert") "bookmark" to hit.second
                                                       else hit
                                            }
                                        }
                                    }
                                }
                                if (hasDirs) {
                                    val r = consume(iHeaderHeightPx) { null to -1 }
                                    if (r != null) return r
                                    for (i in localDirs.indices) {
                                        val h = iSectionRowHeight("dir", i)
                                        val hit = consume(h) {
                                            if (!forInsertion) "dir" to i
                                            else {
                                                val top = currentY
                                                val bottom = currentY + h + iItemSpacingPx
                                                val center = (top + bottom) / 2f
                                                if (localY < center) "dir_insert" to i else "dir_insert" to (i + 1)
                                            }
                                        }
                                        if (hit != null) {
                                            return if (forInsertion && hit.first == "dir_insert") "dir" to hit.second
                                                   else hit
                                        }
                                    }
                                }
                                if (hasFiles) {
                                    val r = consume(iHeaderHeightPx) { null to -1 }
                                    if (r != null) return r
                                    for (i in localFiles.indices) {
                                        val h = iSectionRowHeight("file", i)
                                        val hit = consume(h) {
                                            if (!forInsertion) "file" to i
                                            else {
                                                val top = currentY
                                                val bottom = currentY + h + iItemSpacingPx
                                                val center = (top + bottom) / 2f
                                                if (localY < center) "file_insert" to i else "file_insert" to (i + 1)
                                            }
                                        }
                                        if (hit != null) {
                                            return if (forInsertion && hit.first == "file_insert") "file" to hit.second
                                                   else hit
                                        }
                                    }
                                }
                                return null to -1
                            }

                            fun iCurrentFingerGlobalAbsY(): Float =
                                iOriginalViewportTopGlobalAbs + iDragScrollOffset + (iDragStartFingerY + iDragOffset)

                            /**
                             * 构建所有 section + items 的全局绝对坐标 Map。
                             * 0 = 整个 LazyColumn 第一个逻辑项顶部（若有书签区，就是书签 header；否则目录 header；否则文件 header）
                             */
                            fun iBuildFullGlobalAbs(): Map<String, IAbsBounds> {
                                val result = LinkedHashMap<String, IAbsBounds>()
                                var top = 0f
                                fun walkSection(section: String, list: List<FileItem>) {
                                    // section header
                                    top += iHeaderHeightPx + iItemSpacingPx
                                    // items
                                    for (i in list.indices) {
                                        val h = iSectionRowHeight(section, i)
                                        val key = "${section}_$i"
                                        result[key] = IAbsBounds(
                                            key = key,
                                            indexInSection = i,
                                            absTop = top,
                                            absBottom = top + h,
                                            height = h,
                                            step = h + iItemSpacingPx,
                                        )
                                        top += h + iItemSpacingPx
                                    }
                                }
                                if (hasBookmarks) walkSection("bookmark", localBookmarks)
                                if (hasDirs) walkSection("dir", localDirs)
                                if (hasFiles) walkSection("file", localFiles)
                                return result
                            }

                            /**
                             * 给定当前拖动 section 和手指全局绝对 Y，找 section 内的插入线索引（0..list.size）
                             */
                            fun iResolveInsertionIndex(section: String, fingerGlobalAbsY: Float): Int {
                                val list = iSectionList(section)
                                val sorted = iFrozenGlobalAbs
                                    .filterKeys { it.startsWith("${section}_") }
                                    .entries.sortedBy { it.value.indexInSection }
                                for ((_, b) in sorted) {
                                    if (fingerGlobalAbsY <= b.absBottom) {
                                        val c = (b.absTop + b.absBottom) / 2f
                                        return if (fingerGlobalAbsY < c) b.indexInSection else b.indexInSection + 1
                                    }
                                }
                                return list.size
                            }

                            fun iAutoScrollDelta(fingerYInViewport: Float): Float {
                                if (iViewportHeight <= 0f) return 0f
                                val topThreshold = iAutoScrollEdgePx
                                val bottomThreshold = (iViewportHeight - iAutoScrollEdgePx)
                                    .coerceAtLeast(topThreshold)
                                return when {
                                    fingerYInViewport < topThreshold -> {
                                        val overflow = topThreshold - fingerYInViewport
                                        val ratio = (overflow / iAutoScrollEdgePx).coerceIn(0f, 1f)
                                        -(ratio * iAutoScrollMaxStepPx).coerceAtLeast(0.5f)
                                    }
                                    fingerYInViewport > bottomThreshold -> {
                                        val overflow = fingerYInViewport - bottomThreshold
                                        val ratio = (overflow / iAutoScrollEdgePx).coerceIn(0f, 1f)
                                        (ratio * iAutoScrollMaxStepPx).coerceAtLeast(0.5f)
                                    }
                                    else -> 0f
                                }
                            }

                            fun iResolveTargetIndex(startIndex: Int, insertionIndex: Int): Int =
                                if (insertionIndex > startIndex) insertionIndex - 1 else insertionIndex

                            // ---- 把逻辑项索引映射到 visibleItemsInfo 的 LazyList 真实 index（含 header） ----
                            fun iLogicalToLazyIndex(section: String, idxInSection: Int): Int {
                                var li = 0
                                if (hasBookmarks) {
                                    li++ // header
                                    if (section == "bookmark") return li + idxInSection
                                    li += localBookmarks.size
                                }
                                if (hasDirs) {
                                    li++
                                    if (section == "dir") return li + idxInSection
                                    li += localDirs.size
                                }
                                if (hasFiles) {
                                    li++
                                    if (section == "file") return li + idxInSection
                                }
                                return -1
                            }

                            /**
                             * 根据 firstVisibleItemIndex / firstVisibleItemScrollOffset，
                             * 逐段累加（含 header）求出「视口顶部在列表全局绝对坐标系中的 Y」。
                             * 与 LaunchedEffect(iIsDragging) 中的算法一致，消除 onDragStart 中的维度错配 bug。
                             */
                            fun iComputeViewportTopAbs(): Float {
                                val firstVisible = iListState.firstVisibleItemIndex
                                val firstOff = iListState.firstVisibleItemScrollOffset.toFloat()
                                if (firstVisible == 0) return 0f - firstOff

                                var logicalCursor = 0
                                var absCursor = 0f
                                var matched = false

                                fun advanceHeader() {
                                    if (logicalCursor >= firstVisible) { matched = true; return }
                                    logicalCursor++
                                    absCursor += iHeaderHeightPx + iItemSpacingPx
                                }
                                fun advanceItems(count: Int, section: String) {
                                    if (matched) return
                                    val stepCount = (firstVisible - logicalCursor).coerceAtMost(count)
                                    for (i in 0 until stepCount) {
                                        if (logicalCursor >= firstVisible) { matched = true; return }
                                        absCursor += iSectionRowHeight(section, i) + iItemSpacingPx
                                        logicalCursor++
                                    }
                                }

                                if (hasBookmarks) { advanceHeader(); advanceItems(localBookmarks.size, "bookmark") }
                                if (hasDirs)      { advanceHeader(); advanceItems(localDirs.size,     "dir") }
                                if (hasFiles)     { advanceHeader(); advanceItems(localFiles.size,    "file") }
                                return absCursor - firstOff
                            }

                            /** 返回当前被拖行在视口中的中心 Y；不可见时回退到视口中点 */
                            fun iDragStartCenterInViewport(section: String, idxInSection: Int): Float {
                                val lazyIndex = iLogicalToLazyIndex(section, idxInSection)
                                val info = iListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
                                return if (info != null) {
                                    info.offset.toFloat() + info.size / 2f
                                } else {
                                    iViewportHeight / 2f
                                }
                            }

                            LaunchedEffect(iIsDragging) {
                                if (iIsDragging) {
                                    // 1. 为所有 section 的所有 item（含不可见）构建完整的全局绝对坐标
                                    iFrozenGlobalAbs = iBuildFullGlobalAbs()
                                    // 2. 锚点 — 统一调用 iComputeViewportTopAbs() 消除维度错配
                                    iFirstVisibleIdxAtStart = iListState.firstVisibleItemIndex
                                    iFirstVisibleOffAtStart = iListState.firstVisibleItemScrollOffset.toFloat()
                                    iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                                    // 4. 自动滚动循环
                                    iAutoScrollJob?.cancel()
                                    iAutoScrollJob = launch {
                                        while (iDragSection != null) {
                                            val fingerInViewport = iDragStartFingerY + iDragOffset
                                            val delta = iAutoScrollDelta(fingerInViewport)
                                            if (delta != 0f) {
                                                val consumed = iListState.scrollBy(delta)
                                                if (consumed != 0f) {
                                                    iDragOffset += consumed
                                                    iDragScrollOffset += consumed
                                                    val sec = iDragSection
                                                    if (sec != null) {
                                                        iDragInsertionIndex = iResolveInsertionIndex(sec, iCurrentFingerGlobalAbsY())
                                                    }
                                                }
                                            }
                                            withFrameNanos { }
                                        }
                                    }
                                } else {
                                    iAutoScrollJob?.cancel()
                                    iAutoScrollJob = null
                                    iFrozenGlobalAbs = emptyMap()
                                }
                            }

                            if (filteredInternal0.isEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL) {
                                EmptyFilesContent(modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    state = iListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { clip = false }
                                        .onSizeChanged { iViewportHeight = it.height.toFloat() },
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    // 最近打开（首页置顶，代码预览卡片）
                                    val hasHomeRecent = homeRecentFiles.isNotEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL
                                    if (hasHomeRecent) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_recent_section),
                                                count = homeRecentFiles.size,
                                            )
                                        }
                                        itemsIndexed(homeRecentFiles, key = { idx, rf -> "home_recent_${rf.uri}_$idx" }) { _, recentFile ->
                                            val fileItem = remember(recentFile) { recentFile.toFileItem() }
                                            RecentFilePreviewCard(
                                                recentFile = recentFile,
                                                viewModel = viewModel,
                                                onClick = { onOpenFile(fileItem) },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }

                                    // Bookmarks section
                                    val hasBm = localBookmarks.isNotEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL
                                    if (hasBm) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_bookmarks_section),
                                                count = localBookmarks.size,
                                            )
                                        }
                                        itemsIndexed(localBookmarks, key = { idx, it -> "bookmark_${it.uri}_$idx" }) { index, item ->
                                            val isDraggingNow = iDragSection == "bookmark" && index == iDragStartIndex
                                            val insertionAtThis = iDragSection == "bookmark" && iDragInsertionIndex == index && !isDraggingNow
                                            val stepPx: Float = run {
                                                val frozen = iFrozenGlobalAbs
                                                if (frozen.isNotEmpty()) {
                                                    frozen["bookmark_$index"]?.step
                                                        ?: ((frozen["bookmark_$index"]?.height ?: iDefaultRowHeightPx) + iItemSpacingPx.toFloat())
                                                } else {
                                                    val bounds = iRowBounds
                                                    val cur = bounds["bookmark_$index"]
                                                    val next = bounds["bookmark_${index + 1}"]
                                                    if (cur != null && next != null) {
                                                        val d = next.top - cur.top
                                                        if (d > 0) return@run d
                                                    }
                                                    (cur?.height?.toFloat() ?: iDefaultRowHeightPx) + iItemSpacingPx.toFloat()
                                                }
                                            }
                                            val rowDisplacement = when {
                                                iDragSection != "bookmark" -> 0f
                                                iDragInsertionIndex > iDragStartIndex &&
                                                    index > iDragStartIndex && index < iDragInsertionIndex -> -stepPx
                                                iDragInsertionIndex < iDragStartIndex &&
                                                    index >= iDragInsertionIndex && index < iDragStartIndex -> stepPx
                                                else -> 0f
                                            }
                                            val transition = updateTransition(targetState = isDraggingNow, label = "bm_$index")
                                            val scale by transition.animateFloat(
                                                transitionSpec = {
                                                    if (targetState) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                    else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                                },
                                                label = "bm_scale_$index",
                                            ) { d -> if (d) 1.06f else 1f }
                                            val elevPx by transition.animateFloat(
                                                transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                                                label = "bm_elev_$index",
                                            ) { d -> if (d) 18f else 0f }
                                            val alpha by transition.animateFloat(
                                                transitionSpec = { tween(180) },
                                                label = "bm_alpha_$index",
                                            ) { d -> if (d) 0.96f else 1f }
                                            val translationY by animateFloatAsState(
                                                targetValue = when {
                                                    isDraggingNow && !iAnimateBack -> iDragOffset
                                                    else -> rowDisplacement
                                                },
                                                animationSpec = if (iAnimateBack || !isDraggingNow) {
                                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                } else {
                                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                                                },
                                                label = "bm_trans_$index",
                                            )
                                            Box(
                                                modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                                            ) {
                                                StaggeredAppearance(index = index) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .onGloballyPositioned { coords ->
                                                                val h = coords.size.height
                                                                val lazyIndex = iLogicalToLazyIndex("bookmark", index)
                                                                val info = iListState.layoutInfo.visibleItemsInfo
                                                                    .firstOrNull { it.index == lazyIndex }
                                                                val top = (info?.offset ?: 0).toFloat()
                                                                val bottom = top + h
                                                                iRowBounds["bookmark_$index"] = IRowBounds(
                                                                    key = "bookmark_$index",
                                                                    indexInSection = index,
                                                                    top = top,
                                                                    bottom = bottom,
                                                                    height = h,
                                                                )
                                                            }
                                                            .graphicsLayer {
                                                                this.alpha = alpha
                                                                this.scaleX = scale
                                                                this.scaleY = scale
                                                                if (isDraggingNow) {
                                                                    this.clip = false
                                                                }
                                                            }
                                                            .offset { IntOffset(0, translationY.roundToInt()) }
                                                            .then(
                                                                if (isDraggingNow) Modifier.shadow(
                                                                    with(density) { elevPx.toDp() },
                                                                    shape = BrandShapes.Card,
                                                                    clip = false,
                                                                ) else Modifier
                                                            ),
                                                    ) {
                                                        if (insertionAtThis) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(6.dp)
                                                                    .padding(horizontal = 12.dp),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(2.dp)
                                                                        .background(
                                                                            color = accent,
                                                                            shape = RoundedCornerShape(999.dp),
                                                                        ),
                                                                )
                                                            }
                                                        }
                                                        FileItemComposable(
                                                            item = item,
                                                            onClick = { clickedItem ->
                                                                if (!iIsDragging && !clickedItem.isDirectory) {
                                                                    onOpenFile(clickedItem)
                                                                }
                                                            },
                                                            onTogglePin = { viewModel.togglePin(it) },
                                                            onToggleBookmark = onToggleBookmark,
                                                            onDelete = onDeleteFile,
                                                            onMoveTo = { fileItem ->
                                                                fileToMove = fileItem
                                                                showMoveToDialog = true
                                                            },
                                                            onMultiSelect = {
                                                                viewModel.toggleMultiSelectMode()
                                                                viewModel.toggleItemSelected(item.uri.toString())
                                                            },
                                                            onToggleSelect = { uri ->
                                                                viewModel.toggleItemSelected(uri)
                                                            },
                                                            isMultiSelectMode = isMultiSelectMode,
                                                            isItemSelected = selectedItems.contains(item.uri.toString()),
                                                            currentDirectoryUri = "",
                                                            modifier = Modifier.fillMaxWidth(),
                                                            onDragStart = {
                                                                iDragSection = "bookmark"
                                                                iDragStartIndex = index
                                                                iDragInsertionIndex = index
                                                                iDragStartFingerY = iDragStartCenterInViewport("bookmark", index)
                                                                iDragOffset = 0f
                                                                iDragScrollOffset = 0f
                                                                iAnimateBack = false
                                                                iFrozenGlobalAbs = iBuildFullGlobalAbs()
                                                                iFirstVisibleIdxAtStart = iListState.firstVisibleItemIndex
                                                                iFirstVisibleOffAtStart = iListState.firstVisibleItemScrollOffset.toFloat()
                                                                iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                                                            },
                                                            onDrag = { dragAmountY ->
                                                                if (iDragSection != null && iDragStartIndex >= 0) {
                                                                    iDragOffset += dragAmountY
                                                                    iDragInsertionIndex = iResolveInsertionIndex(iDragSection!!, iCurrentFingerGlobalAbsY())
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                if (iDragSection != null && iDragStartIndex >= 0) {
                                                                    val list = localBookmarks
                                                                    val target = iResolveTargetIndex(iDragStartIndex, iDragInsertionIndex)
                                                                        .coerceIn(list.indices)
                                                                    if (target != iDragStartIndex) {
                                                                        val newList = list.toMutableList()
                                                                        val moved = newList.removeAt(iDragStartIndex)
                                                                        newList.add(target, moved)
                                                                        localBookmarks = newList
                                                                        iSaveOrder()
                                                                    }
                                                                }
                                                                iAnimateBack = true
                                                                scope.launch {
                                                                    delay(260)
                                                                    iDragSection = null
                                                                    iDragStartIndex = -1
                                                                    iDragInsertionIndex = -1
                                                                    iDragOffset = 0f
                                                                    iDragScrollOffset = 0f
                                                                    iDragStartFingerY = 0f
                                                                    iAnimateBack = false
                                                                }
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (localDirs.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_folders_section),
                                                count = localDirs.size,
                                            )
                                        }
                                        itemsIndexed(localDirs, key = { idx, it -> "dir_${it.uri}_$idx" }) { index, item ->
                                            val isDraggingNow = iDragSection == "dir" && index == iDragStartIndex
                                            val insertionAtThis = iDragSection == "dir" && iDragInsertionIndex == index && !isDraggingNow
                                            val stepPx: Float = run {
                                                val frozen = iFrozenGlobalAbs
                                                if (frozen.isNotEmpty()) {
                                                    frozen["dir_$index"]?.step
                                                        ?: ((frozen["dir_$index"]?.height ?: iDefaultDirHeightPx) + iItemSpacingPx.toFloat())
                                                } else {
                                                    val bounds = iRowBounds
                                                    val cur = bounds["dir_$index"]
                                                    val next = bounds["dir_${index + 1}"]
                                                    if (cur != null && next != null) {
                                                        val d = next.top - cur.top
                                                        if (d > 0) return@run d
                                                    }
                                                    (cur?.height?.toFloat() ?: iDefaultDirHeightPx) + iItemSpacingPx.toFloat()
                                                }
                                            }
                                            val rowDisplacement = when {
                                                iDragSection != "dir" -> 0f
                                                iDragInsertionIndex > iDragStartIndex &&
                                                    index > iDragStartIndex && index < iDragInsertionIndex -> -stepPx
                                                iDragInsertionIndex < iDragStartIndex &&
                                                    index >= iDragInsertionIndex && index < iDragStartIndex -> stepPx
                                                else -> 0f
                                            }
                                            val transition = updateTransition(targetState = isDraggingNow, label = "dir_$index")
                                            val scale by transition.animateFloat(
                                                transitionSpec = {
                                                    if (targetState) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                    else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                                },
                                                label = "dir_scale_$index",
                                            ) { d -> if (d) 1.05f else 1f }
                                            val elevPx by transition.animateFloat(
                                                transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                                                label = "dir_elev_$index",
                                            ) { d -> if (d) 16f else 0f }
                                            val alpha by transition.animateFloat(
                                                transitionSpec = { tween(180) },
                                                label = "dir_alpha_$index",
                                            ) { d -> if (d) 0.96f else 1f }
                                            val translationY by animateFloatAsState(
                                                targetValue = when {
                                                    isDraggingNow && !iAnimateBack -> iDragOffset
                                                    else -> rowDisplacement
                                                },
                                                animationSpec = if (iAnimateBack || !isDraggingNow) {
                                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                } else {
                                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                                                },
                                                label = "dir_trans_$index",
                                            )
                                            Box(
                                                modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                                            ) {
                                                StaggeredAppearance(index = index) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .onGloballyPositioned { coords ->
                                                                val h = coords.size.height
                                                                val lazyIndex = iLogicalToLazyIndex("dir", index)
                                                                val info = iListState.layoutInfo.visibleItemsInfo
                                                                    .firstOrNull { it.index == lazyIndex }
                                                                val top = (info?.offset ?: 0).toFloat()
                                                                val bottom = top + h
                                                                iRowBounds["dir_$index"] = IRowBounds(
                                                                    key = "dir_$index",
                                                                    indexInSection = index,
                                                                    top = top,
                                                                    bottom = bottom,
                                                                    height = h,
                                                                )
                                                            }
                                                            .graphicsLayer {
                                                                this.alpha = alpha
                                                                this.scaleX = scale
                                                                this.scaleY = scale
                                                                if (isDraggingNow) {
                                                                    this.clip = false
                                                                }
                                                            }
                                                            .offset { IntOffset(0, translationY.roundToInt()) }
                                                            .then(
                                                                if (isDraggingNow) Modifier.shadow(
                                                                    with(density) { elevPx.toDp() },
                                                                    shape = BrandShapes.Card,
                                                                    clip = false,
                                                                ) else Modifier
                                                            ),
                                                    ) {
                                                        if (insertionAtThis) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(6.dp)
                                                                    .padding(horizontal = 12.dp),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(2.dp)
                                                                        .background(
                                                                            color = accent,
                                                                            shape = RoundedCornerShape(999.dp),
                                                                        ),
                                                                )
                                                            }
                                                        }
                                                        BrandDirectoryCard(
                                                            directoryName = item.name,
                                                            onClick = {
                                                                if (!iIsDragging) {
                                                                    viewModel.navigateTo(item)
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                                                            itemCount = null,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (localFiles.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_files_section),
                                                count = localFiles.size,
                                            )
                                        }
                                        itemsIndexed(localFiles, key = { idx, it -> "file_${it.uri}_$idx" }) { index, item ->
                                            val dirOffset = localDirs.size
                                            val isDraggingNow = iDragSection == "file" && index == iDragStartIndex
                                            val insertionAtThis = iDragSection == "file" && iDragInsertionIndex == index && !isDraggingNow
                                            val stepPx: Float = run {
                                                val frozen = iFrozenGlobalAbs
                                                if (frozen.isNotEmpty()) {
                                                    frozen["file_$index"]?.step
                                                        ?: ((frozen["file_$index"]?.height ?: iDefaultRowHeightPx) + iItemSpacingPx.toFloat())
                                                } else {
                                                    val bounds = iRowBounds
                                                    val cur = bounds["file_$index"]
                                                    val next = bounds["file_${index + 1}"]
                                                    if (cur != null && next != null) {
                                                        val d = next.top - cur.top
                                                        if (d > 0) return@run d
                                                    }
                                                    (cur?.height?.toFloat() ?: iDefaultRowHeightPx) + iItemSpacingPx.toFloat()
                                                }
                                            }
                                            val rowDisplacement = when {
                                                iDragSection != "file" -> 0f
                                                iDragInsertionIndex > iDragStartIndex &&
                                                    index > iDragStartIndex && index < iDragInsertionIndex -> -stepPx
                                                iDragInsertionIndex < iDragStartIndex &&
                                                    index >= iDragInsertionIndex && index < iDragStartIndex -> stepPx
                                                else -> 0f
                                            }
                                            val transition = updateTransition(targetState = isDraggingNow, label = "fl_$index")
                                            val scale by transition.animateFloat(
                                                transitionSpec = {
                                                    if (targetState) spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                    else spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                                                },
                                                label = "fl_scale_$index",
                                            ) { d -> if (d) 1.06f else 1f }
                                            val elevPx by transition.animateFloat(
                                                transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                                                label = "fl_elev_$index",
                                            ) { d -> if (d) 18f else 0f }
                                            val alpha by transition.animateFloat(
                                                transitionSpec = { tween(180) },
                                                label = "fl_alpha_$index",
                                            ) { d -> if (d) 0.96f else 1f }
                                            val translationY by animateFloatAsState(
                                                targetValue = when {
                                                    isDraggingNow && !iAnimateBack -> iDragOffset
                                                    else -> rowDisplacement
                                                },
                                                animationSpec = if (iAnimateBack || !isDraggingNow) {
                                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                                                } else {
                                                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                                                },
                                                label = "fl_trans_$index",
                                            )
                                            Box(
                                                modifier = Modifier.zIndex(if (isDraggingNow) 10f else 0f)
                                            ) {
                                                StaggeredAppearance(index = index + dirOffset) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .onGloballyPositioned { coords ->
                                                                val h = coords.size.height
                                                                val lazyIndex = iLogicalToLazyIndex("file", index)
                                                                val info = iListState.layoutInfo.visibleItemsInfo
                                                                    .firstOrNull { it.index == lazyIndex }
                                                                val top = (info?.offset ?: 0).toFloat()
                                                                val bottom = top + h
                                                                iRowBounds["file_$index"] = IRowBounds(
                                                                    key = "file_$index",
                                                                    indexInSection = index,
                                                                    top = top,
                                                                    bottom = bottom,
                                                                    height = h,
                                                                )
                                                            }
                                                            .graphicsLayer {
                                                                this.alpha = alpha
                                                                this.scaleX = scale
                                                                this.scaleY = scale
                                                                if (isDraggingNow) {
                                                                    this.clip = false
                                                                }
                                                            }
                                                            .offset { IntOffset(0, translationY.roundToInt()) }
                                                            .then(
                                                                if (isDraggingNow) Modifier.shadow(
                                                                    with(density) { elevPx.toDp() },
                                                                    shape = BrandShapes.Card,
                                                                    clip = false,
                                                                ) else Modifier
                                                            ),
                                                    ) {
                                                        if (insertionAtThis) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(6.dp)
                                                                    .padding(horizontal = 12.dp),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(2.dp)
                                                                        .background(
                                                                            color = accent,
                                                                            shape = RoundedCornerShape(999.dp),
                                                                        ),
                                                                )
                                                            }
                                                        }
                                                        FileItemComposable(
                                                            item = item,
                                                            onClick = { clickedItem ->
                                                                if (!iIsDragging && !clickedItem.isDirectory) {
                                                                    onOpenFile(clickedItem)
                                                                }
                                                            },
                                                            onTogglePin = { viewModel.togglePin(it) },
                                                            onToggleBookmark = onToggleBookmark,
                                                            onDelete = onDeleteFile,
                                                            onMoveTo = { fileItem ->
                                                                fileToMove = fileItem
                                                                showMoveToDialog = true
                                                            },
                                                            onMultiSelect = {
                                                                viewModel.toggleMultiSelectMode()
                                                                viewModel.toggleItemSelected(item.uri.toString())
                                                            },
                                                            onToggleSelect = { uri ->
                                                                viewModel.toggleItemSelected(uri)
                                                            },
                                                            isMultiSelectMode = isMultiSelectMode,
                                                            isItemSelected = selectedItems.contains(item.uri.toString()),
                                                            currentDirectoryUri = "",
                                                            modifier = Modifier.fillMaxWidth(),
                                                            onDragStart = {
                                                                iDragSection = "file"
                                                                iDragStartIndex = index
                                                                iDragInsertionIndex = index
                                                                iDragStartFingerY = iDragStartCenterInViewport("file", index)
                                                                iDragOffset = 0f
                                                                iDragScrollOffset = 0f
                                                                iAnimateBack = false
                                                                iFrozenGlobalAbs = iBuildFullGlobalAbs()
                                                                iFirstVisibleIdxAtStart = iListState.firstVisibleItemIndex
                                                                iFirstVisibleOffAtStart = iListState.firstVisibleItemScrollOffset.toFloat()
                                                                iOriginalViewportTopGlobalAbs = iComputeViewportTopAbs()
                                                            },
                                                            onDrag = { dragAmountY ->
                                                                if (iDragSection != null && iDragStartIndex >= 0) {
                                                                    iDragOffset += dragAmountY
                                                                    iDragInsertionIndex = iResolveInsertionIndex(iDragSection!!, iCurrentFingerGlobalAbsY())
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                if (iDragSection != null && iDragStartIndex >= 0) {
                                                                    val list = localFiles
                                                                    val target = iResolveTargetIndex(iDragStartIndex, iDragInsertionIndex)
                                                                        .coerceIn(list.indices)
                                                                    if (target != iDragStartIndex) {
                                                                        val newList = list.toMutableList()
                                                                        val moved = newList.removeAt(iDragStartIndex)
                                                                        newList.add(target, moved)
                                                                        localFiles = newList
                                                                        iSaveOrder()
                                                                    }
                                                                }
                                                                iAnimateBack = true
                                                                scope.launch {
                                                                    delay(260)
                                                                    iDragSection = null
                                                                    iDragStartIndex = -1
                                                                    iDragInsertionIndex = -1
                                                                    iDragOffset = 0f
                                                                    iDragScrollOffset = 0f
                                                                    iDragStartFingerY = 0f
                                                                    iAnimateBack = false
                                                                }
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Bottom padding for FAB
                                    item { Spacer(modifier = Modifier.height(96.dp)) }
                                }
                            }
                        } else {
                            NoPermissionContent(
                                onSelectDirectory = { safLauncher.launch(null) },
                                onOpenFile = { openFileLauncher.launch(arrayOf(
                                    "text/*",
                                    "application/json",
                                    "application/xml",
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-powerpoint",
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                    "image/*",
                                    "audio/*",
                                    "video/*",
                                    "application/octet-stream",
                                )) },
                                onNavigateToSamples = onNavigateToSamples,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                            )
                        }
                    }
                    uiState is BrowserUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    uiState is BrowserUiState.Error -> {
                        ErrorContent(
                            message = (uiState as BrowserUiState.Error).message,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.fillMaxSize(),
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
                                        current[1].uri.toString(),
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
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onTogglePin = { viewModel.togglePin(it) },
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDeleteFile,
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                layoutMode = layoutMode,
                                onSaveExternalFile = onSaveExternalFile,
                                modifier = Modifier,
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
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onSaveExternalFile = onSaveExternalFile,
                                onOpenTerminal = { item ->
                                    onNavigateToTerminal(FileUtils.resolveLocalPath(item.uri.toString()))
                                },
                                modifier = Modifier,
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
                                onMoveTo = { fileItem ->
                                    fileToMove = fileItem
                                    showMoveToDialog = true
                                },
                                onMultiSelect = {
                                    viewModel.toggleMultiSelectMode()
                                },
                                onToggleSelect = { uri ->
                                    viewModel.toggleItemSelected(uri)
                                },
                                isMultiSelectMode = isMultiSelectMode,
                                selectedItems = selectedItems,
                                currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                gitFileStatuses = gitFileStatuses,
                                isGitRepo = isGitRepo,
                                onSaveExternalFile = onSaveExternalFile,
                                onOpenTerminal = { item ->
                                    onNavigateToTerminal(FileUtils.resolveLocalPath(item.uri.toString()))
                                },
                                modifier = Modifier,
                            )
                        } else {
                            // Compact list with grouped sections
                            val filtered = filterFiles(files)
                            val dirs = filtered.filter { it.isDirectory }
                            val nonDirs = filtered.filter { !it.isDirectory }
                            val isEmpty = filtered.isEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL

                            if (isEmpty) {
                                EmptyFilesContent(modifier = Modifier.fillMaxSize())
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    // Git status section
                                    if (isGitRepo && gitFileStatuses.isNotEmpty() && searchQuery.isBlank() && selectedFilter == FileFilter.ALL) {
                                        item { SectionMonoHeader(title = stringResource(R.string.browser_changes_section)) }
                                        items(gitFileStatuses, key = { "git_${it.filePath}" }) { gitStatus ->
                                            GitStatusItemComposable(
                                                gitFileStatus = gitStatus,
                                                modifier = Modifier.padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                                            )
                                        }
                                    }

                                    if (dirs.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_folders_section),
                                                count = dirs.size,
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
                                                            onLongClick = { showTerminalMenu = true },
                                                        )
                                                ) {
                                                    BrandDirectoryCard(
                                                        directoryName = item.name,
                                                        onClick = {},
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                                                        itemCount = null,
                                                    )
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = showTerminalMenu,
                                                onDismissRequest = { showTerminalMenu = false },
                                                containerColor = MaterialTheme.colorScheme.surface,
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.browser_action_open_terminal)) },
                                                    leadingIcon = {
                                                        StrokeIcon(
                                                            icon = StrokeIcons.Terminal,
                                                            contentDescription = null,
                                                            tint = if (localPath != null) PrototypeTokens.accent else PrototypeTokens.muted,
                                                        )
                                                    },
                                                    enabled = localPath != null,
                                                    onClick = {
                                                        showTerminalMenu = false
                                                        onNavigateToTerminal(localPath)
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    if (nonDirs.isNotEmpty()) {
                                        item {
                                            SectionMonoHeader(
                                                title = stringResource(R.string.browser_files_section),
                                                count = nonDirs.size,
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
                                                        modifier = Modifier.padding(horizontal = PrototypeSpacing.ScreenHorizontal),
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
                                                        onMoveTo = { fileItem ->
                                                            fileToMove = fileItem
                                                            showMoveToDialog = true
                                                        },
                                                        onMultiSelect = {
                                                            viewModel.toggleMultiSelectMode()
                                                            viewModel.toggleItemSelected(item.uri.toString())
                                                        },
                                                        onToggleSelect = { uri ->
                                                            viewModel.toggleItemSelected(uri)
                                                        },
                                                        isMultiSelectMode = isMultiSelectMode,
                                                        isItemSelected = selectedItems.contains(item.uri.toString()),
                                                        currentDirectoryUri = currentTreeUri?.toString() ?: "",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 0.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // No results for search/filter
                                    if (filtered.isEmpty() && (searchQuery.isNotBlank() || selectedFilter != FileFilter.ALL)) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 48.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.browser_no_matching_files),
                                                    style = DraftPeekTypography.bodyMedium,
                                                    color = muted,
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
                            contentAlignment = Alignment.Center,
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.browser_action_select_all),
                    color = PrototypeTokens.accent,
                    modifier = Modifier
                        .clickable { viewModel.selectAllItems() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.browser_action_deselect_all),
                    color = PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable { viewModel.clearSelection() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.browser_action_move, selectedItems.size),
                    color = if (selectedItems.isNotEmpty()) PrototypeTokens.accent else PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable(enabled = selectedItems.isNotEmpty()) {
                            showMoveToDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(R.string.browser_action_cancel),
                    color = PrototypeTokens.muted,
                    modifier = Modifier
                        .clickable { viewModel.toggleMultiSelectMode() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // ---- FAB with expandable speed-dial menu ----
        val fabItems = mutableListOf(
            FABMenuItem(
                icon = StrokeIcons.Plus,
                label = stringResource(R.string.browser_fab_new_file),
                onClick = { showCreateFileDialog = true },
            ),
            FABMenuItem(
                icon = StrokeIcons.FolderOutline,
                label = stringResource(R.string.browser_fab_new_folder),
                onClick = { showCreateFolderDialog = true },
            ),
            FABMenuItem(
                icon = StrokeIcons.Upload,
                label = stringResource(R.string.browser_fab_import_file),
                onClick = { importFileLauncher.launch(arrayOf("*/*")) },
            ),
            FABMenuItem(
                icon = StrokeIcons.FolderOutline,
                label = stringResource(R.string.browser_fab_open_file),
                onClick = { openFileLauncher.launch(arrayOf("*/*")) },
            ),
            FABMenuItem(
                icon = StrokeIcons.Download,
                label = stringResource(R.string.browser_github_import),
                onClick = { showGitHubImportDialog = true },
            ),
        )
        if (terminalEnabled) {
            fabItems.add(
                FABMenuItem(
                    icon = StrokeIcons.Terminal,
                    label = stringResource(R.string.browser_fab_terminal),
                    onClick = { onNavigateToTerminal(null) },
                )
            )
        }
        fabItems.addAll(listOf(
            FABMenuItem(
                icon = StrokeIcons.Braces,
                label = stringResource(R.string.browser_action_snippets),
                onClick = onNavigateToSnippets,
            ),
            FABMenuItem(
                icon = StrokeIcons.File,
                label = stringResource(R.string.browser_fab_samples),
                onClick = onNavigateToSamples,
            ),
            FABMenuItem(
                icon = StrokeIcons.Clock,
                label = stringResource(R.string.browser_history_content_desc),
                onClick = onNavigateToHistory,
            ),
        ))
        BrandFAB(
            items = fabItems,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = PrototypeSpacing.FABRight,
                    bottom = 16.dp,
                ),
        )
    }
}

// ============================================================
// Section header — mono uppercase label with optional count
// ============================================================

@Composable
private fun SectionMonoHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val muted = PrototypeTokens.muted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MonoLabelStyle,
            color = muted,
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MonoLabelStyle,
                color = muted.copy(alpha = 0.6f),
            )
        }
    }
}

// ============================================================
// Breadcrumb bar for subdirectory navigation
// ============================================================

@Composable
private fun BreadcrumbBar(
    path: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StrokeIcon(
            icon = StrokeIcons.FolderFilled,
            contentDescription = null,
            tint = PrototypeTokens.folder,
            modifier = Modifier.size(14.dp),
        )
        StrokeIcon(
            icon = StrokeIcons.ChevronRight,
            contentDescription = null,
            tint = muted.copy(alpha = 0.4f),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = displayName,
            style = MonoLabelStyle.copy(fontSize = 11.sp),
            color = fgSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

// ============================================================
// Empty state — shown when directory has no files
// ============================================================

@Composable
private fun EmptyFilesContent(
    modifier: Modifier = Modifier,
) {
    val muted = PrototypeTokens.muted
    val fgSoft = PrototypeTokens.fgSoft

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOff,
            contentDescription = null,
            tint = muted.copy(alpha = 0.35f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.browser_no_files_yet),
            style = DraftPeekTypography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = fgSoft,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.browser_no_files_hint),
            style = DraftPeekTypography.bodySmall,
            color = muted,
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
    border: Color,
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
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = DraftPeekTypography.bodySmall.copy(
                color = fg,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

private fun formatRecentFileMeta(recentFile: RecentFile): String {
    val parts = mutableListOf<String>()
    parts.add(DateUtils.getRelativeTimeSpanString(recentFile.lastOpenedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString())
    return parts.joinToString(" · ")
}

/**
 * Convert a [RecentFile] entity into a [FileItem] for display in virtual folder views.
 */
private fun RecentFile.toFileItem(): FileItem = FileItem(
    name = fileName,
    uri = Uri.parse(uri),
    isDirectory = false,
    size = fileSize,
    lastModified = lastOpenedAt,
    mimeType = "",
    extension = fileName.substringAfterLast('.', ""),
    isPinned = isFavorite,
    isBookmarked = false,
)

/**
 * Map a set of pinned URIs to [FileItem] objects by looking up matching items
 * in the internal files list first, then falling back to URI-based construction.
 */
private fun mapPinnedUrisToFileItems(
    pinnedUris: Set<String>,
    internalFiles: List<FileItem>,
): List<FileItem> {
    val internalByUri = internalFiles.associateBy { it.uri.toString() }
    return pinnedUris.mapNotNull { uriString ->
        internalByUri[uriString] ?: run {
            // Fallback: construct a FileItem from the URI itself
            val parsedUri = Uri.parse(uriString)
            val name = parsedUri.lastPathSegment?.substringAfterLast('/') ?: "file"
            FileItem(
                name = name,
                uri = parsedUri,
                isDirectory = false,
                size = 0L,
                extension = name.substringAfterLast('.', ""),
                isPinned = true,
            )
        }
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
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null,
) {
    val windowSize = LocalWindowInfo.current.containerSize

    if (foldInfo.orientation == FoldingFeature.Orientation.HORIZONTAL) {
        val screenHeightPx = windowSize.height
        val splitRatio = if (screenHeightPx > 0) {
            foldInfo.bounds.top.toFloat() / screenHeightPx
        } else 0.5f

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
                onMoveTo = onMoveTo,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
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
        } else 0.4f

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
                onMoveTo = onMoveTo,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
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
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null,
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
                onMoveTo = onMoveTo,
                onMultiSelect = onMultiSelect,
                onToggleSelect = onToggleSelect,
                isMultiSelectMode = isMultiSelectMode,
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
        maxSplitRatio = 0.75f,
    )
}

@Composable
private fun FilePreviewPane(
    selectedFile: FileItem?,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedFile == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = stringResource(R.string.browser_hint_select_file_preview),
                    modifier = Modifier.size(64.dp),
                    tint = PrototypeTokens.muted.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.browser_hint_select_file_preview),
                    style = DraftPeekTypography.bodyLarge,
                    color = PrototypeTokens.muted
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (selectedFile.isDirectory) Icons.Filled.Folder
                    else Icons.Filled.Description,
                    contentDescription = if (selectedFile.isDirectory) stringResource(R.string.browser_content_desc_folder) else stringResource(R.string.browser_content_desc_file),
                    modifier = Modifier.size(48.dp),
                    tint = if (selectedFile.isDirectory) {
                        PrototypeTokens.folder
                    } else {
                        PrototypeTokens.muted
                    }
                )
                Text(
                    text = selectedFile.name,
                    style = DraftPeekTypography.headlineSmall,
                )
                HorizontalDivider()
                if (!selectedFile.isDirectory) {
                    FileInfoRow(label = stringResource(R.string.browser_label_size), value = formatFileSize(selectedFile.size))
                    if (selectedFile.lastModified > 0) {
                        FileInfoRow(
                            label = stringResource(R.string.browser_label_modified_time),
                            value = DateUtils.getRelativeTimeSpanString(
                                selectedFile.lastModified,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            ).toString()
                        )
                    }
                    if (selectedFile.mimeType.isNotEmpty()) {
                        FileInfoRow(label = stringResource(R.string.browser_label_type), value = selectedFile.mimeType)
                    }
                    if (selectedFile.extension.isNotEmpty()) {
                        FileInfoRow(label = stringResource(R.string.browser_label_extension), value = ".${selectedFile.extension}")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!selectedFile.isDirectory) {
                    BrandFilledButton(
                        text = stringResource(R.string.browser_action_open_in_editor),
                        onClick = { onOpenFile(selectedFile) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = DraftPeekTypography.bodyMedium,
            color = PrototypeTokens.muted,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = DraftPeekTypography.bodyMedium,
        )
    }
}

@Composable
private fun NoPermissionContent(
    onSelectDirectory: () -> Unit,
    onOpenFile: () -> Unit,
    onNavigateToSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elevated = PrototypeTokens.elevated
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val info = SemanticColors.Info

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PrototypeShapes.Card)
                .background(elevated)
                .border(1.dp, border, PrototypeShapes.Card)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browser_open_from_device),
                style = DraftPeekTypography.bodyLarge.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.browser_open_from_device_desc),
                style = DraftPeekTypography.bodySmall.copy(color = muted, lineHeight = 18.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(PrototypeShapes.Medium)
                    .background(accent)
                    .clickable { onSelectDirectory() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.browser_choose_folder),
                    style = DraftPeekTypography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(info.copy(alpha = 0.06f))
                .border(1.dp, info.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.browser_external_readonly_hint),
                style = DraftPeekTypography.bodySmall.copy(color = info, lineHeight = 18.sp),
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOff,
            contentDescription = null,
            tint = PrototypeTokens.error.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = DraftPeekTypography.bodyMedium,
            color = PrototypeTokens.error,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        BrandFilledButton(
            text = stringResource(R.string.browser_action_retry),
            onClick = onRetry,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(PrototypeTokens.accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = DraftPeekTypography.titleMedium,
            color = PrototypeTokens.accent,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
private fun FileListContent(
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    selectedFile: FileItem? = null,
    compareSelection: List<FileItem> = emptyList(),
    gitFileStatuses: List<GitFileStatus> = emptyList(),
    isGitRepo: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    onDelete: ((FileItem) -> Unit)? = null,
    onMoveTo: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null,
) {
    val useGrid = layoutMode != LayoutMode.COMPACT && compareSelection.isEmpty()

    if (useGrid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
            if (isGitRepo && gitFileStatuses.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.browser_section_git_changes)) }
                items(gitFileStatuses, key = { "git_${it.filePath}" }, span = { GridItemSpan(maxLineSpan) }) { gitStatus ->
                    GitStatusItemComposable(gitFileStatus = gitStatus)
                }
                item(span = { GridItemSpan(maxLineSpan) }) { HorizontalDivider() }
            }

            if (files.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyFilesContent()
                    }
                }
            } else {
                items(files, key = { it.uri.toString() }) { item ->
                    val isSelected = selectedFile?.uri == item.uri
                    FileGridItem(
                        item = item,
                        onClick = onFileClick,
                        selected = isSelected,
                        onTogglePin = onTogglePin,
                        onToggleBookmark = onToggleBookmark,
                        currentDirectoryUri = currentDirectoryUri,
                        onOpenTerminal = onOpenTerminal,
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        ) {
            if (isGitRepo && gitFileStatuses.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.browser_section_git_changes)) }
                items(gitFileStatuses, key = { "git_${it.filePath}" }) { gitStatus ->
                    GitStatusItemComposable(gitFileStatus = gitStatus)
                }
                item { HorizontalDivider() }
            }

            if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyFilesContent()
                    }
                }
            } else {
                // Group directories and files
                val dirs = files.filter { it.isDirectory }
                val nonDirs = files.filter { !it.isDirectory }

                if (dirs.isNotEmpty()) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_folders_section),
                            count = dirs.size,
                        )
                    }
                    items(dirs, key = { it.uri.toString() }) { item ->
                        var showTerminalMenu by remember { mutableStateOf(false) }
                        val localPath = remember(item.uri.toString()) {
                            FileUtils.resolveLocalPath(item.uri.toString())
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onFileClick(item) },
                                    onLongClick = { showTerminalMenu = true },
                                )
                        ) {
                            BrandDirectoryCard(
                                directoryName = item.name,
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 3.dp),
                                itemCount = null,
                            )
                        }
                        DropdownMenu(
                            expanded = showTerminalMenu,
                            onDismissRequest = { showTerminalMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_action_open_terminal)) },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Terminal,
                                        contentDescription = null,
                                        tint = if (localPath != null) PrototypeTokens.accent else PrototypeTokens.muted,
                                    )
                                },
                                enabled = localPath != null,
                                onClick = {
                                    showTerminalMenu = false
                                    onOpenTerminal?.invoke(item)
                                },
                            )
                        }
                    }
                }

                if (nonDirs.isNotEmpty()) {
                    item {
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_files_section),
                            count = nonDirs.size,
                        )
                    }
                    items(nonDirs, key = { it.uri.toString() }) { item ->
                        val isSelected = selectedFile?.uri == item.uri
                        val compareIdx = compareSelection.indexOfFirst { it.uri == item.uri }
                        if (item.isExternal) {
                            ExternalFileCard(
                                item = item,
                                onClick = onFileClick,
                                onSave = onSaveExternalFile,
                                modifier = Modifier.padding(horizontal = PrototypeSpacing.ScreenHorizontal),
                            )
                        } else {
                            FileItemComposable(
                                item = item,
                                onClick = onFileClick,
                                selected = isSelected,
                                compareIndex = if (compareIdx >= 0) compareIdx + 1 else 0,
                                isMultiSelectMode = isMultiSelectMode,
                                isItemSelected = selectedItems.contains(item.uri.toString()),
                                onTogglePin = onTogglePin,
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDelete,
                                onMoveTo = onMoveTo,
                                onMultiSelect = onMultiSelect,
                                onToggleSelect = onToggleSelect,
                                currentDirectoryUri = currentDirectoryUri,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredAppearance(
    index: Int,
    delayMillis: Int = 50,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong() * index.coerceAtMost(20))
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "stagger_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "stagger_offset",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
            this.clip = false   // 允许内部阴影/缩放溢出
        }
    ) {
        content()
    }
}

private fun isCodeExtension(ext: String): Boolean {
    return ext.lowercase() in CODE_EXTENSIONS
}

@Composable
private fun ExternalFileCard(
    item: FileItem,
    onClick: (FileItem) -> Unit,
    onSave: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extension = item.extension
    val isDark = LocalDarkTheme.current
    val typeColor = FileTypeColors.forExtension(extension, isDark)
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val warning = SemanticColors.Warning

    Card(
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(containerColor = PrototypeTokens.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(typeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = extension.uppercase().take(2),
                    style = MonoLabelStyle.copy(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.02.sp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.name,
                        style = DraftPeekTypography.bodyMedium.copy(color = fg, fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(warning.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "EXT",
                            style = CodeTextStyle.copy(color = warning, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.sp),
                        )
                    }
                }
                Text(
                    text = buildString {
                        append(item.uri.path?.substringBeforeLast("/")?.substringAfterLast("/") ?: "")
                        append(" · ")
                        append(formatFileSize(item.size))
                    },
                    style = FileMetaStyle,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentSoft)
                    .clickable { onSave(item) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.browser_action_save),
                    style = CodeTextStyle.copy(color = accent, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemComposable(
    item: FileItem,
    onClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    compareIndex: Int = 0,
    isMultiSelectMode: Boolean = false,
    isItemSelected: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    onDelete: ((FileItem) -> Unit)? = null,
    onMoveTo: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    currentDirectoryUri: String = "",
    onDragStart: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val extension = item.extension
    val isInternalFile = !item.isExternal && !item.isDirectory

    val displayName = if (compareIndex > 0) "${compareIndex}. ${item.name}" else item.name
    val metaText = if (!item.isDirectory) {
        buildString {
            append(formatFileSize(item.size))
            if (item.lastModified > 0) {
                append("  ")
                append(DateUtils.getRelativeTimeSpanString(item.lastModified, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS))
            }
        }
    } else ""

    if (item.isDirectory) {
        BrandDirectoryCard(
            directoryName = item.name,
            onClick = { onClick(item) },
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 3.dp),
            itemCount = null,
        )
    } else {
        BrandFileCard(
            fileName = displayName,
            extension = extension,
            metaText = metaText,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 3.dp),
            onClick = if (isMultiSelectMode && onToggleSelect != null) {
                { onToggleSelect.invoke(item.uri.toString()) }
            } else {
                { onClick(item) }
            },
            onLongClick = if (isMultiSelectMode && onToggleSelect != null) {
                { onToggleSelect.invoke(item.uri.toString()) }
            } else if (onTogglePin != null || onToggleBookmark != null || (onDelete != null && isInternalFile) || onMoveTo != null || onMultiSelect != null) {
                { showContextMenu = true }
            } else null,
            isPinned = item.isPinned,
            isSelected = selected || compareIndex > 0 || isItemSelected,
            leadingContent = if (isMultiSelectMode) {
                {
                    Checkbox(
                        checked = isItemSelected,
                        onCheckedChange = { onToggleSelect?.invoke(item.uri.toString()) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrototypeTokens.accent,
                            uncheckedColor = PrototypeTokens.muted.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else if (item.isPinned || item.isBookmarked) {
                {
                    if (item.isBookmarked) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.browser_content_desc_bookmarked),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (item.isPinned) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.browser_content_desc_pinned),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            } else null,
            trailingContent = {
                if (item.isReadOnly) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.browser_content_desc_read_only),
                        tint = PrototypeTokens.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                // 拖动手柄 — 使用 clickable 拦截点击，阻止父级长按菜单
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                // 点击时触发拖动开始，长按时也会触发
                                onDragStart?.invoke()
                            }
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    onDragStart?.invoke()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag?.invoke(dragAmount.y)
                                },
                                onDragEnd = {
                                    onDragEnd?.invoke()
                                },
                                onDragCancel = {
                                    onDragEnd?.invoke()
                                },
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.browser_action_drag_reorder),
                        tint = PrototypeTokens.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                StrokeIcon(
                    icon = StrokeIcons.ChevronRight,
                    contentDescription = stringResource(R.string.browser_action_open_in_editor),
                    tint = PrototypeTokens.muted.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }

    if (!item.isDirectory && (onTogglePin != null || onToggleBookmark != null || (onDelete != null && isInternalFile) || onMoveTo != null || onMultiSelect != null)) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            if (onTogglePin != null) {
                DropdownMenuItem(
                    text = { Text(if (item.isPinned) stringResource(R.string.browser_action_unpin) else stringResource(R.string.browser_action_pin)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                            contentDescription = if (item.isPinned) stringResource(R.string.browser_action_unpin) else stringResource(R.string.browser_action_pin),
                            tint = PrototypeTokens.accent,
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onTogglePin.invoke(item.uri.toString())
                    },
                )
            }
            if (onToggleBookmark != null) {
                DropdownMenuItem(
                    text = { Text(if (item.isBookmarked) stringResource(R.string.browser_action_unbookmark) else stringResource(R.string.browser_action_bookmark)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (item.isBookmarked) stringResource(R.string.browser_action_unbookmark) else stringResource(R.string.browser_action_bookmark),
                            tint = PrototypeTokens.accent,
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onToggleBookmark.invoke(item.uri.toString(), item.name, currentDirectoryUri)
                    },
                )
            }
            if (onMoveTo != null && isInternalFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_move_to)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = stringResource(R.string.browser_action_move_to),
                            tint = PrototypeTokens.accent,
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onMoveTo.invoke(item)
                    },
                )
            }
            if (onMultiSelect != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_multi_select)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.browser_action_multi_select),
                            tint = PrototypeTokens.accent,
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onMultiSelect.invoke()
                    },
                )
            }
            if (onDelete != null && isInternalFile) {
                if (onTogglePin != null || onToggleBookmark != null || onMoveTo != null || onMultiSelect != null) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_delete_file), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.browser_action_delete_file),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onDelete.invoke(item)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    item: FileItem,
    onClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    currentDirectoryUri: String = "",
    onOpenTerminal: ((FileItem) -> Unit)? = null,
) {
    val extension = item.extension
    var showContextMenu by remember { mutableStateOf(false) }

    val cardContainerColor = when {
        selected -> PrototypeTokens.accentSoft
        else -> PrototypeTokens.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, PrototypeTokens.border.copy(alpha = 0.5f)),
                RoundedCornerShape(10.dp),
            )
            .combinedClickable(
                onClick = { onClick(item) },
                onLongClick = {
                    if (item.isDirectory) {
                        if (onOpenTerminal != null) {
                            showContextMenu = true
                        }
                    } else if (onTogglePin != null || onToggleBookmark != null) {
                        showContextMenu = true
                    }
                },
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (item.isBookmarked || item.isPinned) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (item.isBookmarked) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.browser_content_desc_bookmarked),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    if (item.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.browser_content_desc_pinned),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Box {
                if (item.isDirectory) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = stringResource(R.string.browser_content_desc_folder),
                        tint = PrototypeTokens.folder,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    FileTypeIcon(
                        extension = extension,
                        showExtension = true,
                    )
                }
                if (item.isReadOnly) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.browser_content_desc_read_only),
                        tint = PrototypeTokens.muted.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = (-4).dp, y = 18.dp),
                    )
                }
                if (item.gitStatus != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = 18.dp, y = 18.dp)
                            .background(
                                color = gitStatusColor(item.gitStatus),
                                shape = PrototypeShapes.Small
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.name,
                style = if (isCodeExtension(extension)) {
                    DraftPeekTypography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                } else {
                    DraftPeekTypography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            if (!item.isDirectory) {
                Text(
                    text = formatFileSize(item.size),
                    style = DraftPeekTypography.labelSmall.copy(fontSize = 10.sp),
                    color = PrototypeTokens.muted,
                    maxLines = 1,
                )
            }
        }
    }

    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        if (item.isDirectory && onOpenTerminal != null) {
            val localPath = FileUtils.resolveLocalPath(item.uri.toString())
            DropdownMenuItem(
                text = { Text(stringResource(R.string.browser_action_open_terminal)) },
                leadingIcon = {
                    StrokeIcon(
                        icon = StrokeIcons.Terminal,
                        contentDescription = null,
                        tint = if (localPath != null) PrototypeTokens.accent else PrototypeTokens.muted,
                    )
                },
                enabled = localPath != null,
                onClick = {
                    showContextMenu = false
                    onOpenTerminal.invoke(item)
                },
            )
        }
        if (onTogglePin != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isPinned) stringResource(R.string.browser_action_unpin)
                        else stringResource(R.string.browser_action_pin)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        contentDescription = if (item.isPinned) stringResource(R.string.browser_action_unpin) else stringResource(R.string.browser_action_pin),
                        tint = PrototypeTokens.accent,
                    )
                },
                onClick = {
                    showContextMenu = false
                    onTogglePin?.invoke(item.uri.toString())
                },
            )
        }
        if (onToggleBookmark != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isBookmarked) stringResource(R.string.browser_action_unbookmark)
                        else stringResource(R.string.browser_action_bookmark)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (item.isBookmarked) stringResource(R.string.browser_action_unbookmark) else stringResource(R.string.browser_action_bookmark),
                        tint = PrototypeTokens.accent,
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleBookmark.invoke(item.uri.toString(), item.name, currentDirectoryUri)
                },
            )
        }
    }
}

// ============================================================
// Recent file code-preview card — 对应 HTML 原型 recent-card
// ============================================================

/**
 * 首页"最近打开"代码预览大卡片。
 *
 * 结构（与 HTML 原型一致）：文件类型徽章 + 等宽文件名 + 元信息，
 * 下方为带语法高亮的等宽代码预览块。
 *
 * @param recentFile 最近文件实体（用于元信息展示与 URI）
 * @param viewModel 文件浏览 ViewModel（提供异步预览读取）
 * @param onClick 点击卡片（打开文件）
 */
@Composable
private fun RecentFilePreviewCard(
    recentFile: RecentFile,
    viewModel: FileBrowserViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = PrototypeTokens.surface
    val codeBg = PrototypeTokens.bg
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val borderSoft = PrototypeTokens.borderSoft
    val accent = PrototypeTokens.accent
    val warning = PrototypeTokens.warning
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    var preview by remember(recentFile.uri) { mutableStateOf<String?>(null) }
    LaunchedEffect(recentFile.uri) {
        preview = viewModel.readFilePreview(recentFile.uri)
    }

    Card(
        onClick = onClick,
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(containerColor = surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 4.dp)
            .pressScaleEffect(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hero row：文件类型徽章 + 等宽文件名 + 元信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileTypeIcon(
                    extension = recentFile.fileName.substringAfterLast('.', ""),
                    modifier = Modifier.size(DraftPeekSpacing.FileTypeBadgeSize),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentFile.fileName,
                        style = MonoFileNameStyle,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRecentFileMeta(recentFile),
                        style = FileMetaStyle,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 代码预览块
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(codeBg)
                    .border(1.dp, borderSoft, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val previewText = preview
                if (previewText.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.browser_recent_no_preview),
                        style = CodeTextStyle.copy(fontSize = 11.sp, lineHeight = 18.sp),
                        color = muted.copy(alpha = 0.7f),
                    )
                } else {
                    Text(
                        text = buildCodePreviewAnnotatedString(
                            code = previewText,
                            fgSoft = fgSoft,
                            accent = accent,
                            secondary = secondary,
                            tertiary = tertiary,
                            muted = muted,
                            warning = warning,
                        ),
                        style = CodeTextStyle.copy(fontSize = 11.sp, lineHeight = 18.sp),
                        color = fgSoft,
                        maxLines = 5,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/** 代码高亮关键字集合（覆盖 Kotlin 常见语法与 Gradle DSL）。 */
private val CodePreviewKeywords = setOf(
    "fun", "val", "var", "class", "object", "interface", "private", "public", "internal",
    "protected", "import", "package", "return", "if", "else", "when", "for", "while", "do",
    "try", "catch", "finally", "throw", "data", "sealed", "enum", "companion", "override",
    "suspend", "inline", "tailrec", "constructor", "init", "by", "as", "is", "in", "null",
    "true", "false", "this", "super", "typealias", "where", "get", "set", "abstract", "open",
    "final", "const", "lateinit", "lazy", "launch", "withContext", "remember",
    "mutableStateOf", "collect", "repeat", "require", "check", "let", "apply", "also", "run",
    "with", "implementation", "api", "kapt", "ksp", "apply", "plugin", "alias", "libs",
)

/** 分词正则：注释 / 字符串 / 数字 / 标识符 / 其他符号。 */
private val CodePreviewTokenPattern = Pattern.compile(
    "(//[^\\n]*" +
        "|\"[^\"\\n]*\"|'[^'\\n]*'" +
        "|\\b\\d[\\d_]*\\.?\\d*\\b" +
        "|\\b[A-Za-z_][A-Za-z0-9_]*\\b" +
        "|\\S)"
)

/**
 * 把代码预览文本渲染为带语法高亮的 [AnnotatedString]。
 * 配色对齐原型：关键字=朱砂红、函数=secondary、字符串=tertiary、注释=muted、类型=secondary 混合。
 */
private fun buildCodePreviewAnnotatedString(
    code: String,
    fgSoft: Color,
    accent: Color,
    secondary: Color,
    tertiary: Color,
    muted: Color,
    warning: Color,
): AnnotatedString {
    val typeColor = lerp(secondary, fgSoft, 0.35f)
    return buildAnnotatedString {
        val matcher = CodePreviewTokenPattern.matcher(code)
        while (matcher.find()) {
            val token = matcher.group()
            val span: SpanStyle = when {
                token.startsWith("//") -> SpanStyle(color = muted, fontStyle = FontStyle.Italic)
                token.startsWith("\"") || token.startsWith("'") -> SpanStyle(color = tertiary)
                token.firstOrNull()?.isDigit() == true -> SpanStyle(color = warning)
                token in CodePreviewKeywords -> SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)
                token.firstOrNull()?.isLetter() == true -> {
                    val next = code.getOrNull(matcher.end())
                    when {
                        next == '(' -> SpanStyle(color = secondary)
                        token.first().isUpperCase() -> SpanStyle(color = typeColor)
                        else -> SpanStyle(color = fgSoft)
                    }
                }
                else -> SpanStyle(color = fgSoft)
            }
            withStyle(span) { append(token) }
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        .coerceAtMost(units.size - 1)
    val value = size / Math.pow(1024.0, digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}

@Composable
private fun GitStatusItemComposable(
    gitFileStatus: GitFileStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = gitStatusColor(gitFileStatus.status),
                    shape = PrototypeShapes.Small
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gitFileStatus.filePath,
                style = DraftPeekTypography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(gitStatusLabelRes(gitFileStatus.status)),
            style = DraftPeekTypography.labelSmall,
            color = gitStatusColor(gitFileStatus.status),
        )
    }
}

@Composable
private fun gitStatusColor(status: GitStatus) = when (status) {
    GitStatus.ADDED -> PrototypeTokens.folder
    GitStatus.MODIFIED -> Color(0xFFFFA000)
    GitStatus.DELETED -> PrototypeTokens.error
    GitStatus.UNTRACKED -> Color(0xFFB0BEC5)
    GitStatus.UNMODIFIED -> SemanticColors.Success
    GitStatus.CONFLICTED -> Color(0xFFFF5722)
}

@androidx.annotation.StringRes
private fun gitStatusLabelRes(status: GitStatus): Int = when (status) {
    GitStatus.ADDED -> R.string.browser_git_status_added
    GitStatus.MODIFIED -> R.string.browser_git_status_modified
    GitStatus.DELETED -> R.string.browser_git_status_deleted
    GitStatus.UNTRACKED -> R.string.browser_git_status_untracked
    GitStatus.UNMODIFIED -> R.string.browser_git_status_unmodified
    GitStatus.CONFLICTED -> R.string.browser_git_status_conflicted
}

@Composable
private fun GitHubImportUrlDialog(
    onDismiss: () -> Unit,
    onBrowse: (String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf("") }
    val context = LocalContext.current

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_github_import)) },
        content = {
            BrandOutlinedTextField(
                value = url,
                onValueChange = {
                    if (it.length <= 2048) {
                        url = InputValidator.sanitizeControlChars(it)
                        urlError = ""
                    }
                },
                label = { Text(stringResource(R.string.browser_github_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = stringResource(R.string.browser_github_import),
                        tint = PrototypeTokens.accent,
                    )
                },
                isError = urlError.isNotEmpty(),
                supportingText = if (urlError.isNotEmpty()) {
                    { Text(urlError) }
                } else null,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sanitizedUrl = InputValidator.sanitizeControlChars(url.trim())
                    val validation = InputValidator.validateGitHubUrl(sanitizedUrl)
                    if (validation is ValidationResult.Invalid) {
                        urlError = try {
                            context.getString(validation.messageResId)
                        } catch (_: android.content.res.Resources.NotFoundException) {
                            validation.messageResId.toString()
                        }
                        return@TextButton
                    }
                    onBrowse(sanitizedUrl)
                },
                enabled = url.isNotBlank(),
            ) {
                Text(stringResource(R.string.browser_github_browse))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.browser_action_cancel))
            }
        }
    )
}

@Composable
private fun GitHubLoadingDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_github_import)) },
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text(message)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.browser_action_cancel))
            }
        }
    )
}

@Composable
private fun GitHubFileSelectionDialog(
    owner: String,
    repo: String,
    files: List<GitHubFileEntry>,
    selectedFiles: Set<String>,
    onToggleFile: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.browser_github_select_files))
                Text(
                    text = "$owner/$repo",
                    style = DraftPeekTypography.bodySmall,
                    color = PrototypeTokens.accent,
                )
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.browser_action_select_all))
                    }
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(R.string.browser_action_clear))
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(files, key = { it.path }) { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFile(file.path) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selectedFiles.contains(file.path),
                                onCheckedChange = { onToggleFile(file.path) },
                                colors = CheckboxDefaults.colors(checkedColor = PrototypeTokens.accent),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FileTypeIcon(
                                extension = file.path.substringAfterLast('.', ""),
                                modifier = Modifier.size(20.dp),
                                showExtension = false,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.path,
                                style = DraftPeekTypography.bodySmall,
                                maxLines = 2,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedFiles.isNotEmpty(),
            ) {
                Text(stringResource(R.string.browser_github_browse))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.browser_action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = modifier.minimumTouchTarget()) {
            content()
        }
    }
}

private data class IRowBounds(
    val key: String, // 'dir_N' | 'file_N' | 'bookmark_N'
    val indexInSection: Int,
    val top: Float,
    val bottom: Float,
    val height: Int,
)

/** 内部文件列表区：每个 section 内部独立的绝对累计坐标（0 = section header 之后） */
private data class IAbsBounds(
    val key: String, // 'dir_N' | 'file_N' | 'bookmark_N'
    val indexInSection: Int,
    /** 相对于本 section header 顶部的绝对坐标（注意：不包含 section header 高度） */
    val absTop: Float,
    val absBottom: Float,
    val height: Float,
    val step: Float,
)

