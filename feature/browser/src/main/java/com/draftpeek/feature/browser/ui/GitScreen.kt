package com.draftpeek.feature.browser.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.viewmodel.GitUiState
import com.draftpeek.feature.browser.viewmodel.GitViewModel

/**
 * Git 管理专用界面
 *
 * 提供完整的 Git 管理功能，采用 5 个标签页的界面设计：
 * - 状态：显示工作区变更文件列表和状态摘要
 * - 提交：输入提交消息，选择文件进行提交
 * - 远程：推送、拉取、获取远程更新
 * - 分支：查看和切换分支
 * - 日志：查看提交历史
 *
 * 替代了之前仅使用底部动作面板的方案，提供更完整的 Git 操作体验。
 *
 * @param treeUri 当前目录的 SAF URI
 * @param onNavigateBack 返回导航回调
 * @param viewModel Git 操作 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(
    treeUri: Uri,
    onNavigateBack: () -> Unit,
    viewModel: GitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val branches = remember { viewModel.getBranches() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val elevated = PrototypeTokens.elevated

    // Load Git status on first composition
    LaunchedEffect(treeUri) {
        viewModel.loadGitStatus(treeUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.browser_git_screen_title),
                    style = DraftPeekTypography.titleMedium,
                    color = fg,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(android.R.string.cancel),
                        tint = fg,
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.browser_git_action_fetch),
                        tint = muted,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = surface),
        )

        // Branch info bar
        if (uiState.isGitRepo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(elevated)
                    .padding(horizontal = PrototypeSpacing.ScreenHorizontal, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = uiState.branchName.ifBlank { "HEAD" },
                        style = MonoLabelStyle.copy(
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        ),
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
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Tab row
        val tabs = listOf(
            stringResource(R.string.browser_git_tab_status),
            stringResource(R.string.browser_git_tab_commit),
            stringResource(R.string.browser_git_tab_remote),
            stringResource(R.string.browser_git_tab_branches),
            stringResource(R.string.browser_git_tab_log),
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = surface,
            contentColor = accent,
            divider = { HorizontalDivider(color = border) },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = DraftPeekTypography.labelSmall,
                        )
                    },
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> StatusTab(uiState, fg, muted, accent, accentSoft, surface, border, elevated)
            1 -> CommitTab(uiState, viewModel, fg, muted, accent, accentSoft, surface, border, elevated)
            2 -> RemoteTab(uiState, viewModel, fg, muted, accent, accentSoft, surface, border, elevated)
            3 -> BranchesTab(branches, uiState, viewModel, fg, muted, accent, accentSoft, surface, border, elevated)
            4 -> LogTab(uiState, fg, muted, accent, accentSoft, surface, border, elevated)
        }
    }
}

// ── Tab 1: Status ──────────────────────────────────────────────────────

/**
 * Git 状态标签页
 *
 * 显示工作区变更摘要和变更文件列表，文件按状态（M/A/D/?/C）标记颜色。
 *
 * @param uiState Git UI 状态
 * @param fg 前景色
 * @param muted 次要文本颜色
 * @param accent 强调色
 * @param accentSoft 柔和强调色
 * @param surface 表面色
 * @param border 边框色
 * @param elevated 抬高卡片色
 */
@Composable
private fun StatusTab(
    uiState: GitUiState,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    if (!uiState.isGitRepo) {
        EmptyState(
            icon = Icons.Filled.Source,
            message = stringResource(R.string.browser_git_no_repo),
            muted = muted,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Summary
        item {
            Spacer(modifier = Modifier.height(12.dp))
            StatusSummaryCard(uiState, fg, muted, accent, accentSoft, border, elevated)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browser_git_changed_files),
                style = MonoLabelStyle,
                color = muted,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Changed files
        val changedFiles = uiState.fileStatuses.filter { it.status != GitStatus.UNMODIFIED }
        if (changedFiles.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.browser_git_status_clean),
                    style = DraftPeekTypography.bodySmall,
                    color = SemanticColors.Success,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(changedFiles, key = { it.filePath }) { file ->
                FileStatusRow(file, fg, muted, accent, accentSoft, border)
            }
        }
    }
}

@Composable
private fun StatusSummaryCard(
    uiState: GitUiState,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    border: Color, elevated: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(elevated)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.browser_git_status_summary, uiState.modifiedCount),
            style = DraftPeekTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = fg,
        )
    }
}

@Composable
private fun FileStatusRow(
    file: GitFileStatus,
    fg: Color, muted: Color, accent: Color, accentSoft: Color, border: Color,
) {
    val (statusLabel, statusColor) = when (file.status) {
        GitStatus.MODIFIED -> "M" to SemanticColors.Warning
        GitStatus.ADDED -> "A" to SemanticColors.Success
        GitStatus.DELETED -> "D" to SemanticColors.Danger
        GitStatus.UNTRACKED -> "?" to muted
        GitStatus.CONFLICTED -> "C" to SemanticColors.Danger
        GitStatus.UNMODIFIED -> " " to muted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Transparent)
            .border(1.dp, border.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = statusLabel,
                style = MonoLabelStyle.copy(
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = file.filePath.substringAfterLast('/'),
            style = DraftPeekTypography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = file.filePath.substringBeforeLast('/', ""),
            style = MonoLabelStyle.copy(fontSize = 10.sp),
            color = muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Tab 2: Commit ──────────────────────────────────────────────────────

@Composable
private fun CommitTab(
    uiState: GitUiState,
    viewModel: GitViewModel,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    var commitMessage by rememberSaveable { mutableStateOf("") }
    val changedFiles = uiState.fileStatuses.filter { it.status != GitStatus.UNMODIFIED }
    val selectedFiles = remember { mutableStateOf(setOf<String>()) }

    // Auto-select all changed files
    LaunchedEffect(changedFiles) {
        selectedFiles.value = changedFiles.map { it.filePath }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
            .imePadding(),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Commit message input
        Text(
            text = stringResource(R.string.browser_git_commit_message_label),
            style = MonoLabelStyle,
            color = muted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        BrandOutlinedTextField(
            value = commitMessage,
            onValueChange = { commitMessage = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.browser_git_commit_message_hint)) },
            singleLine = false,
            maxLines = 5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Select files to stage
        Text(
            text = stringResource(R.string.browser_git_stage_files),
            style = MonoLabelStyle,
            color = muted,
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (changedFiles.isEmpty()) {
            Text(
                text = stringResource(R.string.browser_git_no_changes),
                style = DraftPeekTypography.bodySmall,
                color = muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(changedFiles, key = { it.filePath }) { file ->
                    val isSelected = selectedFiles.value.contains(file.filePath)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) accentSoft.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable {
                                selectedFiles.value = if (isSelected) {
                                    selectedFiles.value - file.filePath
                                } else {
                                    selectedFiles.value + file.filePath
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.CreateNewFolder,
                            contentDescription = null,
                            tint = if (isSelected) accent else muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file.filePath.substringAfterLast('/'),
                            style = DraftPeekTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (isSelected) fg else muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Commit button
        BrandFilledButton(
            onClick = {
                viewModel.commit(commitMessage, selectedFiles.value)
                commitMessage = ""
            },
            enabled = commitMessage.isNotBlank() && selectedFiles.value.isNotEmpty() && !uiState.isCommitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isCommitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = stringResource(R.string.browser_git_action_commit))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Tab 3: Remote (Push/Pull/Fetch) ────────────────────────────────────

@Composable
private fun RemoteTab(
    uiState: GitUiState,
    viewModel: GitViewModel,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Remote info
        if (uiState.remoteUrl != null) {
            RemoteInfoCard(uiState, fg, muted, accent, accentSoft, border, elevated)
        } else {
            Text(
                text = stringResource(R.string.browser_git_no_remote),
                style = DraftPeekTypography.bodySmall,
                color = muted,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Push button
        RemoteActionCard(
            icon = Icons.Filled.CloudUpload,
            title = stringResource(R.string.browser_git_action_push),
            description = stringResource(R.string.browser_git_push_desc),
            isLoading = uiState.isPushing,
            enabled = uiState.isGitRepo && !uiState.isPushing,
            onClick = { viewModel.push() },
            fg = fg, muted = muted, accent = accent, accentSoft = accentSoft,
            surface = surface, border = border, elevated = elevated,
        )

        // Pull button
        RemoteActionCard(
            icon = Icons.Filled.CloudDownload,
            title = stringResource(R.string.browser_git_action_pull),
            description = stringResource(R.string.browser_git_pull_desc),
            isLoading = uiState.isPulling,
            enabled = uiState.isGitRepo && !uiState.isPulling,
            onClick = { viewModel.pull() },
            fg = fg, muted = muted, accent = accent, accentSoft = accentSoft,
            surface = surface, border = border, elevated = elevated,
        )

        // Fetch button
        RemoteActionCard(
            icon = Icons.Filled.Sync,
            title = stringResource(R.string.browser_git_action_fetch),
            description = stringResource(R.string.browser_git_fetch_desc),
            isLoading = uiState.isFetching,
            enabled = uiState.isGitRepo && !uiState.isFetching,
            onClick = { viewModel.fetch() },
            fg = fg, muted = muted, accent = accent, accentSoft = accentSoft,
            surface = surface, border = border, elevated = elevated,
        )
    }
}

@Composable
private fun RemoteInfoCard(
    uiState: GitUiState,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    border: Color, elevated: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(elevated)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.browser_git_remote_info),
            style = MonoLabelStyle,
            color = muted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = uiState.remoteUrl ?: "",
            style = DraftPeekTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = fg,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RemoteActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(elevated)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) accent else muted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DraftPeekTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) fg else muted,
            )
            Text(
                text = description,
                style = DraftPeekTypography.bodySmall,
                color = muted,
            )
        }
    }
}

// ── Tab 4: Branches ────────────────────────────────────────────────────

@Composable
private fun BranchesTab(
    branches: List<String>,
    uiState: GitUiState,
    viewModel: GitViewModel,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.browser_git_checkout_branch),
                style = MonoLabelStyle,
                color = muted,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (branches.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.browser_git_no_branches),
                    style = DraftPeekTypography.bodySmall,
                    color = muted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(branches, key = { it }) { branch ->
                val displayName = branch.removePrefix("refs/heads/")
                val isCurrent = displayName == uiState.branchName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCurrent) accentSoft else Color.Transparent)
                        .border(1.dp, if (isCurrent) accent.copy(alpha = 0.3f) else border, RoundedCornerShape(6.dp))
                        .clickable(enabled = !isCurrent) { viewModel.checkout(branch) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Source,
                        contentDescription = null,
                        tint = if (isCurrent) accent else muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayName,
                        style = DraftPeekTypography.bodyMedium.copy(
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCurrent) accent else fg,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Text(
                            text = stringResource(R.string.browser_git_current_branch),
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

// ── Tab 5: Log ─────────────────────────────────────────────────────────

@Composable
private fun LogTab(
    uiState: GitUiState,
    fg: Color, muted: Color, accent: Color, accentSoft: Color,
    surface: Color, border: Color, elevated: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        uiState.lastCommitMessage?.let { lastCommit ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(elevated)
                    .border(1.dp, border, RoundedCornerShape(8.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.browser_git_last_commit),
                    style = MonoLabelStyle,
                    color = muted,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastCommit,
                    style = DraftPeekTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = fg,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } ?: run {
            EmptyState(
                icon = Icons.Filled.History,
                message = stringResource(R.string.browser_git_no_commits),
                muted = muted,
            )
        }
    }
}

// ── Shared ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(icon: ImageVector, message: String, muted: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = muted.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = DraftPeekTypography.bodyMedium,
            color = muted,
        )
    }
}
