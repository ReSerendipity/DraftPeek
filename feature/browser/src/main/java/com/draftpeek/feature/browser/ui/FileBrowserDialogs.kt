package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.util.InputValidator
import com.draftpeek.core.common.util.ValidationResult
import com.draftpeek.core.common.vcs.GitHubFileEntry
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.FileTypeIcon
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.browser.R

// ---- 文件筛选选项 ----
/**
 * 文件筛选枚举
 *
 * 定义文件列表的筛选方式：全部、最近、收藏、书签、代码、文档、媒体
 */


// ============================================================
// Import actions sheet — 「导入…」二级弹层（屏 08）
// ============================================================

/**
 * 「导入…」二级底部弹层。
 *
 * FAB 只保留创建类动作（新建文件 / 新建文件夹 / 导入…），四类导入入口收进这里，
 * 避免一钮展开八项混排（实施指导书 §2.2）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportActionsSheet(
    onDismiss: () -> Unit,
    onSamples: () -> Unit,
    onImportFromGitHub: () -> Unit,
    onOpenExternalFile: () -> Unit,
    onImportToInternal: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrototypeTokens.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.browser_fab_import),
                style = DraftPeekTypography.titleMedium.copy(
                    color = PrototypeTokens.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            ImportActionRow(
                icon = StrokeIcons.File,
                label = stringResource(R.string.browser_fab_samples),
                onClick = onSamples
            )
            ImportActionRow(
                icon = StrokeIcons.Download,
                label = stringResource(R.string.browser_github_import),
                onClick = onImportFromGitHub
            )
            ImportActionRow(
                icon = StrokeIcons.FolderOutline,
                label = stringResource(R.string.browser_fab_open_file),
                onClick = onOpenExternalFile
            )
            ImportActionRow(
                icon = StrokeIcons.Upload,
                label = stringResource(R.string.browser_fab_import_file),
                onClick = onImportToInternal
            )
        }
    }
}


/** 「导入…」弹层中的单行动作。 */
@Composable
private fun ImportActionRow(icon: StrokeIconDef, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PrototypeShapes.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StrokeIcon(
            icon = icon,
            modifier = Modifier.size(20.dp),
            tint = PrototypeTokens.accent,
            contentDescription = null
        )
        Text(
            text = label,
            style = DraftPeekTypography.bodyMedium.copy(color = PrototypeTokens.fg)
        )
    }
}


@Composable
internal fun NoPermissionContent(
    onSelectDirectory: () -> Unit,
    onImportFromGitHub: () -> Unit,
    onNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevated = PrototypeTokens.elevated
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PrototypeShapes.Card)
                .background(elevated)
                .border(1.dp, border, PrototypeShapes.Card)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browser_no_files_yet),
                style = DraftPeekTypography.bodyLarge.copy(
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.browser_open_from_device_desc),
                style = DraftPeekTypography.bodySmall.copy(color = muted, lineHeight = 18.sp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(PrototypeShapes.Medium)
                    .background(accent)
                    .clickable { onSelectDirectory() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.browser_choose_folder),
                    style = DraftPeekTypography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 只读说明并入卡内小字（实施指导书 §2.8）
            Text(
                text = stringResource(R.string.browser_external_readonly_hint),
                style = DraftPeekTypography.bodySmall.copy(color = info, lineHeight = 18.sp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        // 次级入口降为文字链（实施指导书 §2.8）：不再与主 CTA / FAB 并列争入口
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.browser_github_import),
                style = DraftPeekTypography.bodySmall.copy(color = accent),
                modifier = Modifier.clickable { onImportFromGitHub() }
            )
            Text(
                text = stringResource(R.string.browser_fab_new_file),
                style = DraftPeekTypography.bodySmall.copy(color = accent),
                modifier = Modifier.clickable { onNewFile() }
            )
        }
    }
}


@Composable
internal fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = DraftPeekTypography.bodyMedium,
            color = PrototypeTokens.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        BrandFilledButton(
            text = stringResource(R.string.browser_action_retry),
            onClick = onRetry
        )
    }
}


@Composable
internal fun GitHubImportUrlDialog(onDismiss: () -> Unit, onBrowse: (String) -> Unit) {
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
                        tint = PrototypeTokens.accent
                    )
                },
                isError = urlError.isNotEmpty(),
                supportingText = if (urlError.isNotEmpty()) {
                    { Text(urlError) }
                } else {
                    null
                }
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
                enabled = url.isNotBlank()
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
internal fun GitHubLoadingDialog(message: String, onDismiss: () -> Unit) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_github_import)) },
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
internal fun GitHubFileSelectionDialog(
    owner: String,
    repo: String,
    files: List<GitHubFileEntry>,
    selectedFiles: Set<String>,
    onToggleFile: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.browser_github_select_files))
                Text(
                    text = "$owner/$repo",
                    style = DraftPeekTypography.bodySmall,
                    color = PrototypeTokens.accent
                )
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedFiles.contains(file.path),
                                onCheckedChange = { onToggleFile(file.path) },
                                colors = CheckboxDefaults.colors(checkedColor = PrototypeTokens.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FileTypeIcon(
                                extension = file.path.substringAfterLast('.', ""),
                                modifier = Modifier.size(20.dp),
                                showExtension = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = file.path,
                                style = DraftPeekTypography.bodySmall,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedFiles.isNotEmpty()
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


internal data class IRowBounds(
    val key: String, // 'dir_N' | 'file_N' | 'bookmark_N'
    val indexInSection: Int,
    val top: Float,
    val bottom: Float,
    val height: Int
)


/** 内部文件列表区：每个 section 内部独立的绝对累计坐标（0 = section header 之后） */
internal data class IAbsBounds(
    val key: String, // 'dir_N' | 'file_N' | 'bookmark_N'
    val indexInSection: Int,
    /** 相对于本 section header 顶部的绝对坐标（注意：不包含 section header 高度） */
    val absTop: Float,
    val absBottom: Float,
    val height: Float,
    val step: Float
)
