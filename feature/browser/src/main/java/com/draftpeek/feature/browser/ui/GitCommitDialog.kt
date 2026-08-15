package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.browser.R

/**
 * Git 提交对话框
 *
 * 提供提交信息输入、文件选择功能。用户可以输入提交消息，
 * 选择要提交的文件，支持全选/取消全选操作。
 *
 * @param fileStatuses Git 文件状态列表
 * @param isCommitting 是否正在提交
 * @param onCommit 提交回调，参数：(提交消息, 选中文件路径集合)
 * @param onDismiss 对话框关闭回调
 * @param modifier 修饰符
 */
@Composable
fun GitCommitDialog(
    fileStatuses: List<GitFileStatus>,
    isCommitting: Boolean,
    onCommit: (message: String, selectedFiles: Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var commitMessage by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent
    val border = PrototypeTokens.border

    // Files that can be committed (non-unmodified)
    val committableFiles = fileStatuses.filter { it.status != GitStatus.UNMODIFIED }

    // Initialize selected files to all committable files on first composition
    if (selectedFiles.isEmpty() && committableFiles.isNotEmpty()) {
        selectedFiles = committableFiles.map { it.filePath }.toSet()
    }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.browser_git_commit_title)) },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BrandOutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text(stringResource(R.string.browser_git_commit_message_hint)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    enabled = !isCommitting,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Select all / deselect all
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BrandFilledButton(
                        text = stringResource(R.string.browser_git_select_all),
                        onClick = { selectedFiles = committableFiles.map { it.filePath }.toSet() },
                        enabled = !isCommitting,
                    )
                    BrandOutlinedButton(
                        text = stringResource(R.string.browser_git_deselect_all),
                        onClick = { selectedFiles = emptySet() },
                        enabled = !isCommitting,
                    )
                }

                Text(
                    text = stringResource(R.string.browser_git_commit_files, committableFiles.size),
                    style = MonoLabelStyle,
                    color = muted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                if (committableFiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.browser_git_no_changes),
                        style = DraftPeekTypography.bodySmall,
                        color = muted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(committableFiles, key = { it.filePath }) { fileStatus ->
                            val isSelected = fileStatus.filePath in selectedFiles
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isCommitting) {
                                        selectedFiles = if (isSelected) {
                                            selectedFiles - fileStatus.filePath
                                        } else {
                                            selectedFiles + fileStatus.filePath
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedFiles = if (checked) {
                                            selectedFiles + fileStatus.filePath
                                        } else {
                                            selectedFiles - fileStatus.filePath
                                        }
                                    },
                                    enabled = !isCommitting,
                                    colors = CheckboxDefaults.colors(checkedColor = PrototypeTokens.accent),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = gitStatusColor(fileStatus.status),
                                            shape = PrototypeShapes.Small,
                                        ),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fileStatus.filePath,
                                    style = DraftPeekTypography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(gitStatusLabelRes(fileStatus.status)),
                                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                                    color = gitStatusColor(fileStatus.status),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.browser_git_action_commit),
                onClick = {
                    if (commitMessage.isNotBlank() && selectedFiles.isNotEmpty()) {
                        onCommit(commitMessage, selectedFiles)
                    }
                },
                enabled = !isCommitting && commitMessage.isNotBlank() && selectedFiles.isNotEmpty(),
            )
        },
        dismissButton = {
            BrandOutlinedButton(
                text = stringResource(R.string.browser_action_cancel),
                onClick = onDismiss,
                enabled = !isCommitting,
            )
        },
    )
}

@Composable
private fun gitStatusColor(status: GitStatus) = when (status) {
    GitStatus.ADDED -> PrototypeTokens.folder
    GitStatus.MODIFIED -> androidx.compose.ui.graphics.Color(0xFFFFA000)
    GitStatus.DELETED -> PrototypeTokens.error
    GitStatus.UNTRACKED -> androidx.compose.ui.graphics.Color(0xFFB0BEC5)
    GitStatus.UNMODIFIED -> SemanticColors.Success
    GitStatus.CONFLICTED -> androidx.compose.ui.graphics.Color(0xFFFF5722)
}

/**
 * 获取 Git 状态对应的字符串资源 ID
 *
 * @param status Git 文件状态
 * @return 字符串资源 ID
 */
@androidx.annotation.StringRes
private fun gitStatusLabelRes(status: GitStatus): Int = when (status) {
    GitStatus.ADDED -> R.string.browser_git_status_added
    GitStatus.MODIFIED -> R.string.browser_git_status_modified
    GitStatus.DELETED -> R.string.browser_git_status_deleted
    GitStatus.UNTRACKED -> R.string.browser_git_status_untracked
    GitStatus.UNMODIFIED -> R.string.browser_git_status_unmodified
    GitStatus.CONFLICTED -> R.string.browser_git_status_conflicted
}
