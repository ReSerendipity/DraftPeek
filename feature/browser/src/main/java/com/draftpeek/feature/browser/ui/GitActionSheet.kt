package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.viewmodel.GitUiState

/**
 * Git 操作底部动作面板
 *
 * 提供 Git 状态概览、提交、推送、拉取、获取等快捷操作，
 * 以及分支切换功能。作为底部表单弹出，供文件浏览器快速访问 Git 功能。
 *
 * @param uiState Git UI 状态
 * @param branches 分支列表
 * @param onDismiss 面板关闭回调
 * @param onCommit 提交操作回调
 * @param onPush 推送操作回调
 * @param onPull 拉取操作回调
 * @param onFetch 获取操作回调
 * @param onCheckout 分支切换回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitActionSheet(
    uiState: GitUiState,
    branches: List<String>,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onFetch: () -> Unit,
    onCheckout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.branchName.ifBlank { "HEAD" },
                        style = MonoLabelStyle.copy(
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                uiState.remoteUrl?.let { remoteUrl ->
                    Text(
                        text = remoteUrl.substringAfterLast('/').removeSuffix(".git"),
                        style = DraftPeekTypography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.modifiedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrototypeTokens.elevated)
                        .border(1.dp, border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.browser_git_status_summary, uiState.modifiedCount),
                        style = DraftPeekTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = fg
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (uiState.isGitRepo) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SemanticColors.Success.copy(alpha = 0.08f))
                        .border(1.dp, SemanticColors.Success.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.browser_git_status_clean),
                        style = DraftPeekTypography.bodyMedium,
                        color = SemanticColors.Success
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            uiState.lastCommitMessage?.let { lastCommit ->
                Text(
                    text = stringResource(R.string.browser_git_last_commit),
                    style = MonoLabelStyle,
                    color = muted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastCommit,
                    style = DraftPeekTypography.bodySmall,
                    color = fg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.browser_git_actions),
                style = MonoLabelStyle,
                color = muted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GitActionButton(
                    icon = Icons.Filled.CloudUpload,
                    label = stringResource(R.string.browser_git_action_push),
                    onClick = onPush,
                    enabled = !uiState.isPushing,
                    isLoading = uiState.isPushing,
                    modifier = Modifier.weight(1f)
                )
                GitActionButton(
                    icon = Icons.Filled.CloudDownload,
                    label = stringResource(R.string.browser_git_action_pull),
                    onClick = onPull,
                    enabled = !uiState.isPulling,
                    isLoading = uiState.isPulling,
                    modifier = Modifier.weight(1f)
                )
                GitActionButton(
                    icon = Icons.Filled.Sync,
                    label = stringResource(R.string.browser_git_action_fetch),
                    onClick = onFetch,
                    enabled = !uiState.isFetching,
                    isLoading = uiState.isFetching,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GitActionButton(
                icon = null,
                label = stringResource(R.string.browser_git_action_commit),
                onClick = onCommit,
                enabled = !uiState.isCommitting && uiState.modifiedCount > 0,
                isLoading = uiState.isCommitting,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true
            )

            if (uiState.isGitRepo) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = border)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.browser_git_checkout_branch),
                    style = MonoLabelStyle,
                    color = muted
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (branches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.browser_git_no_branches),
                        style = DraftPeekTypography.bodySmall,
                        color = muted
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(branches, key = { it }) { branch ->
                            val displayName = branch.removePrefix("refs/heads/")
                            val isCurrent = displayName == uiState.branchName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) accentSoft else Color.Transparent)
                                    .clickable(enabled = !isCurrent) { onCheckout(branch) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayName,
                                    style = DraftPeekTypography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isCurrent) accent else fg
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Text(
                                        text = stringResource(R.string.browser_git_current_branch),
                                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                                        color = accent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Git 操作按钮组件
 *
 * @param icon 按钮图标（可为空）
 * @param label 按钮文本
 * @param onClick 点击回调
 * @param enabled 是否启用
 * @param isLoading 是否显示加载状态
 * @param modifier 修饰符
 * @param isPrimary 是否为主按钮样式
 */
@Composable
private fun GitActionButton(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val muted = PrototypeTokens.muted

    val bgColor = when {
        !enabled -> surface
        isPrimary -> accent
        else -> accentSoft
    }
    val fgColor = when {
        !enabled -> muted
        isPrimary -> Color.White
        else -> accent
    }

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (!isPrimary && enabled) Modifier.border(1.dp, border, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = fgColor
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(16.dp)
            )
        }
        if (icon != null || isLoading) {
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = DraftPeekTypography.bodySmall.copy(
                color = fgColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}
