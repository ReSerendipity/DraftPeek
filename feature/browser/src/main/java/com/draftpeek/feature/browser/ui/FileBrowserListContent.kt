package com.draftpeek.feature.browser.ui

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.util.FileUtils
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.domain.model.RecentFile
import com.draftpeek.core.ui.component.BrandDirectoryCard
import com.draftpeek.core.ui.component.BrandFileCard
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.layout.LayoutMode
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
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.FileSortOption
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import java.util.regex.Pattern
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */


// ============================================================
// Section header — mono uppercase label with optional count
// ============================================================

@Composable
internal fun SectionMonoHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: @Composable (() -> Unit)? = null
) {
    val muted = PrototypeTokens.muted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MonoLabelStyle,
            color = muted
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MonoLabelStyle,
                color = muted.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        action?.invoke()
    }
}


// ============================================================
// Empty state — shown when directory has no files
// ============================================================

@Composable
internal fun EmptyFilesContent(modifier: Modifier = Modifier) {
    val muted = PrototypeTokens.muted
    val fgSoft = PrototypeTokens.fgSoft

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOff,
            contentDescription = null,
            tint = muted.copy(alpha = 0.35f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.browser_no_files_yet),
            style = DraftPeekTypography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = fgSoft
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.browser_no_files_hint),
            style = DraftPeekTypography.bodySmall,
            color = muted
        )
    }
}


private fun formatRecentFileMeta(recentFile: RecentFile): String {
    val parts = mutableListOf<String>()
    parts.add(
        DateUtils.getRelativeTimeSpanString(
            recentFile.lastOpenedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    )
    return parts.joinToString(" · ")
}


/**
 * Convert a [RecentFile] entity into a [FileItem] for display in virtual folder views.
 */
internal fun RecentFile.toFileItem(): FileItem = FileItem(
    name = fileName,
    uri = Uri.parse(uri),
    isDirectory = false,
    size = fileSize,
    lastModified = lastOpenedAt,
    mimeType = "",
    extension = fileName.substringAfterLast('.', ""),
    isPinned = isFavorite,
    isBookmarked = false
)


/**
 * Map a set of pinned URIs to [FileItem] objects by looking up matching items
 * in the internal files list first, then falling back to URI-based construction.
 */
internal fun mapPinnedUrisToFileItems(pinnedUris: Set<String>, internalFiles: List<FileItem>): List<FileItem> {
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
                isPinned = true
            )
        }
    }
}


@Composable
private fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(PrototypeTokens.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = DraftPeekTypography.titleMedium,
            color = PrototypeTokens.accent,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}


@Composable
internal fun FileListContent(
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
    onRename: ((FileItem) -> Unit)? = null,
    onEncryptExport: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    isMultiSelectMode: Boolean = false,
    isManualOrderMode: Boolean = false,
    selectedItems: Set<String> = emptySet(),
    currentDirectoryUri: String = "",
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    onSaveExternalFile: (FileItem) -> Unit = {},
    onOpenTerminal: ((FileItem) -> Unit)? = null,
    currentSortOption: FileSortOption = FileSortOption.NAME_ASC,
    onSortOptionChange: ((FileSortOption) -> Unit)? = null
) {
    val useGrid = layoutMode != LayoutMode.COMPACT && compareSelection.isEmpty()

    if (useGrid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            if (isGitRepo && gitFileStatuses.isNotEmpty()) {
                item(span = {
                    GridItemSpan(maxLineSpan)
                }) { SectionHeader(stringResource(R.string.browser_section_git_changes)) }
                items(gitFileStatuses, key = {
                    "git_${it.filePath}"
                }, span = { GridItemSpan(maxLineSpan) }) { gitStatus ->
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
                        contentAlignment = Alignment.Center
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
                        onOpenTerminal = onOpenTerminal
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
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
                        contentAlignment = Alignment.Center
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
                            count = dirs.size
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
                                    onLongClick = { showTerminalMenu = true }
                                )
                        ) {
                            BrandDirectoryCard(
                                directoryName = item.name,
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 3.dp),
                                itemCount = null
                            )
                        }
                        DropdownMenu(
                            expanded = showTerminalMenu,
                            onDismissRequest = { showTerminalMenu = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_action_open_terminal)) },
                                leadingIcon = {
                                    StrokeIcon(
                                        icon = StrokeIcons.Terminal,
                                        contentDescription = null,
                                        tint = if (localPath != null) PrototypeTokens.accent else PrototypeTokens.muted
                                    )
                                },
                                enabled = localPath != null,
                                onClick = {
                                    showTerminalMenu = false
                                    onOpenTerminal?.invoke(item)
                                }
                            )
                        }
                    }
                }

                if (nonDirs.isNotEmpty()) {
                    item {
                        val changeSort = onSortOptionChange
                        SectionMonoHeader(
                            title = stringResource(R.string.browser_files_section),
                            count = nonDirs.size,
                            action = if (changeSort != null) {
                                { SortOptionMenu(current = currentSortOption, onSelect = changeSort) }
                            } else {
                                null
                            }
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
                                modifier = Modifier.padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                            )
                        } else {
                            FileItemComposable(
                                item = item,
                                onClick = onFileClick,
                                selected = isSelected,
                                compareIndex = if (compareIdx >= 0) compareIdx + 1 else 0,
                                isMultiSelectMode = isMultiSelectMode,
                                isManualOrderMode = isManualOrderMode,
                                isItemSelected = selectedItems.contains(item.uri.toString()),
                                onTogglePin = onTogglePin,
                                onToggleBookmark = onToggleBookmark,
                                onDelete = onDelete,
                                onEncryptExport = onEncryptExport,
                                onMoveTo = onMoveTo,
                                onRename = onRename,
                                onMultiSelect = onMultiSelect,
                                onToggleSelect = onToggleSelect,
                                currentDirectoryUri = currentDirectoryUri
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
internal fun StaggeredAppearance(index: Int, delayMillis: Int = 50, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong() * index.coerceAtMost(20))
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "stagger_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "stagger_offset"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
            this.clip = false // 允许内部阴影/缩放溢出
        }
    ) {
        content()
    }
}


private fun isCodeExtension(ext: String): Boolean = ext.lowercase() in CODE_EXTENSIONS


@Composable
internal fun ExternalFileCard(
    item: FileItem,
    onClick: (FileItem) -> Unit,
    onSave: (FileItem) -> Unit,
    modifier: Modifier = Modifier
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
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(typeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = extension.uppercase().take(2),
                    style = MonoLabelStyle.copy(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.02.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = DraftPeekTypography.bodyMedium.copy(
                            color = fg,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(warning.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXT",
                            style = CodeTextStyle.copy(
                                color = warning,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.04.sp
                            )
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
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentSoft)
                    .clickable { onSave(item) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.browser_action_save),
                    style = CodeTextStyle.copy(color = accent, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
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
    isManualOrderMode: Boolean = false,
    isItemSelected: Boolean = false,
    onTogglePin: ((String) -> Unit)? = null,
    onToggleBookmark: ((String, String, String) -> Unit)? = null,
    onDelete: ((FileItem) -> Unit)? = null,
    onMoveTo: ((FileItem) -> Unit)? = null,
    onRename: ((FileItem) -> Unit)? = null,
    onEncryptExport: ((FileItem) -> Unit)? = null,
    onMultiSelect: (() -> Unit)? = null,
    onToggleSelect: ((String) -> Unit)? = null,
    currentDirectoryUri: String = "",
    onDragStart: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val extension = item.extension
    val isInternalFile = !item.isExternal && !item.isDirectory
    // 行内「⋮」与长按共用同一套动作；没有可用动作时不渲染入口，避免开出空菜单。
    val hasContextActions = !item.isDirectory &&
        (
            onTogglePin != null ||
                onToggleBookmark != null ||
                (onDelete != null && isInternalFile) ||
                onMoveTo != null ||
                onEncryptExport != null ||
                onMultiSelect != null
            )

    val displayName = if (compareIndex > 0) "$compareIndex. ${item.name}" else item.name
    val metaText = if (!item.isDirectory) {
        buildString {
            append(formatFileSize(item.size))
            if (item.lastModified > 0) {
                append("  ")
                append(
                    DateUtils.getRelativeTimeSpanString(
                        item.lastModified,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                )
            }
        }
    } else {
        ""
    }

    if (item.isDirectory) {
        BrandDirectoryCard(
            directoryName = item.name,
            onClick = { onClick(item) },
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 3.dp),
            itemCount = null
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
            } else if (onTogglePin != null ||
                onToggleBookmark != null ||
                (onDelete != null && isInternalFile) ||
                onMoveTo != null ||
                onEncryptExport != null ||
                onMultiSelect != null
            ) {
                { showContextMenu = true }
            } else {
                null
            },
            isPinned = item.isPinned,
            isSelected = selected || compareIndex > 0 || isItemSelected,
            leadingContent = if (isMultiSelectMode) {
                {
                    Checkbox(
                        checked = isItemSelected,
                        onCheckedChange = { onToggleSelect?.invoke(item.uri.toString()) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrototypeTokens.accent,
                            uncheckedColor = PrototypeTokens.muted.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (item.isPinned || item.isBookmarked) {
                {
                    if (item.isBookmarked) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.browser_content_desc_bookmarked),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (item.isPinned) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.browser_content_desc_pinned),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                null
            },
            trailingContent = {
                if (item.isReadOnly) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.browser_content_desc_read_only),
                        tint = PrototypeTokens.muted.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                // 拖动手柄 — 仅「手动排序」模式下显示（屏 07 收敛）。
                // 拖拽逻辑本身未改动，只是默认不再常驻，避免与行内 ⋮ 菜单争夺点击。
                if (isManualOrderMode) {
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
                                    }
                                )
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.browser_action_drag_reorder),
                            tint = PrototypeTokens.muted.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                // 尾部收敛为单一「⋮」行内菜单（实施指导书 §2.9 屏 07）：点它即可看到
                // 原本只藏在长按里的动作，取代原先语义不明的「›」。
                if (hasContextActions) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showContextMenu = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.MoreVert,
                            contentDescription = stringResource(R.string.browser_action_more),
                            tint = PrototypeTokens.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }

    if (hasContextActions) {
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            if (onTogglePin != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (item.isPinned) {
                                stringResource(
                                    R.string.browser_action_unpin
                                )
                            } else {
                                stringResource(R.string.browser_action_pin)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                            contentDescription = if (item.isPinned) {
                                stringResource(
                                    R.string.browser_action_unpin
                                )
                            } else {
                                stringResource(R.string.browser_action_pin)
                            },
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onTogglePin.invoke(item.uri.toString())
                    }
                )
            }
            if (onToggleBookmark != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (item.isBookmarked) {
                                stringResource(
                                    R.string.browser_action_unbookmark
                                )
                            } else {
                                stringResource(R.string.browser_action_bookmark)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (item.isBookmarked) {
                                stringResource(
                                    R.string.browser_action_unbookmark
                                )
                            } else {
                                stringResource(R.string.browser_action_bookmark)
                            },
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onToggleBookmark.invoke(item.uri.toString(), item.name, currentDirectoryUri)
                    }
                )
            }
            if (onMoveTo != null && isInternalFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_move_to)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = stringResource(R.string.browser_action_move_to),
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onMoveTo.invoke(item)
                    }
                )
            }
            if (onRename != null && isInternalFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_rename)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.browser_action_rename),
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onRename.invoke(item)
                    }
                )
            }
            if (onEncryptExport != null && isInternalFile) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_encrypt_export)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.browser_action_encrypt_export),
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onEncryptExport.invoke(item)
                    }
                )
            }
            if (onMultiSelect != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.browser_action_multi_select)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.browser_action_multi_select),
                            tint = PrototypeTokens.accent
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onMultiSelect.invoke()
                    }
                )
            }
            if (onDelete != null && isInternalFile) {
                if (onTogglePin != null || onToggleBookmark != null || onMoveTo != null || onMultiSelect != null) {
                    HorizontalDivider()
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.browser_action_delete_file),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.browser_action_delete_file),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onDelete.invoke(item)
                    }
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
    onOpenTerminal: ((FileItem) -> Unit)? = null
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
                RoundedCornerShape(10.dp)
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
                }
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (item.isBookmarked || item.isPinned) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (item.isBookmarked) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.browser_content_desc_bookmarked),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    if (item.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.browser_content_desc_pinned),
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(12.dp)
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
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    FileTypeIcon(
                        extension = extension,
                        showExtension = true
                    )
                }
                if (item.isReadOnly) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.browser_content_desc_read_only),
                        tint = PrototypeTokens.muted.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = (-4).dp, y = 18.dp)
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
                        fontSize = 11.sp
                    )
                } else {
                    DraftPeekTypography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (!item.isDirectory) {
                Text(
                    text = formatFileSize(item.size),
                    style = DraftPeekTypography.labelSmall.copy(fontSize = 10.sp),
                    color = PrototypeTokens.muted,
                    maxLines = 1
                )
            }
        }
    }

    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (item.isDirectory && onOpenTerminal != null) {
            val localPath = FileUtils.resolveLocalPath(item.uri.toString())
            DropdownMenuItem(
                text = { Text(stringResource(R.string.browser_action_open_terminal)) },
                leadingIcon = {
                    StrokeIcon(
                        icon = StrokeIcons.Terminal,
                        contentDescription = null,
                        tint = if (localPath != null) PrototypeTokens.accent else PrototypeTokens.muted
                    )
                },
                enabled = localPath != null,
                onClick = {
                    showContextMenu = false
                    onOpenTerminal.invoke(item)
                }
            )
        }
        if (onTogglePin != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isPinned) {
                            stringResource(R.string.browser_action_unpin)
                        } else {
                            stringResource(R.string.browser_action_pin)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        contentDescription = if (item.isPinned) {
                            stringResource(
                                R.string.browser_action_unpin
                            )
                        } else {
                            stringResource(R.string.browser_action_pin)
                        },
                        tint = PrototypeTokens.accent
                    )
                },
                onClick = {
                    showContextMenu = false
                    onTogglePin?.invoke(item.uri.toString())
                }
            )
        }
        if (onToggleBookmark != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isBookmarked) {
                            stringResource(R.string.browser_action_unbookmark)
                        } else {
                            stringResource(R.string.browser_action_bookmark)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (item.isBookmarked) {
                            stringResource(
                                R.string.browser_action_unbookmark
                            )
                        } else {
                            stringResource(R.string.browser_action_bookmark)
                        },
                        tint = PrototypeTokens.accent
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleBookmark.invoke(item.uri.toString(), item.name, currentDirectoryUri)
                }
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
internal fun RecentFilePreviewCard(
    recentFile: RecentFile,
    viewModel: FileBrowserViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            .pressScaleEffect()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hero row：文件类型徽章 + 等宽文件名 + 元信息
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileTypeIcon(
                    extension = recentFile.fileName.substringAfterLast('.', ""),
                    modifier = Modifier.size(DraftPeekSpacing.FileTypeBadgeSize)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recentFile.fileName,
                        style = MonoFileNameStyle,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRecentFileMeta(recentFile),
                        style = FileMetaStyle,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                val previewText = preview
                if (previewText.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.browser_recent_no_preview),
                        style = CodeTextStyle.copy(fontSize = 11.sp, lineHeight = 18.sp),
                        color = muted.copy(alpha = 0.7f)
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
                            warning = warning
                        ),
                        style = CodeTextStyle.copy(fontSize = 11.sp, lineHeight = 18.sp),
                        color = fgSoft,
                        maxLines = 5,
                        overflow = TextOverflow.Clip
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
    "with", "implementation", "api", "kapt", "ksp", "apply", "plugin", "alias", "libs"
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
    warning: Color
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


internal fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        .coerceAtMost(units.size - 1)
    val value = size / Math.pow(1024.0, digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}


@Composable
internal fun GitStatusItemComposable(gitFileStatus: GitFileStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
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
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = stringResource(gitStatusLabelRes(gitFileStatus.status)),
            style = DraftPeekTypography.labelSmall,
            color = gitStatusColor(gitFileStatus.status)
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
