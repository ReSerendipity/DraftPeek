/**
 * 文件: ProfileScreen.kt
 * 功能: 统计模块UI - 个人资料/统计/设置主页面
 * 描述: DraftPeek 应用的"我的"页面，整合了以下功能模块：
 *       1. 用户资料展示（头像、用户名、版本号）
 *       2. 统计数据展示（周期选择器、统计卡片网格、年度热力图）
 *       3. 编辑器设置（字体大小、主题、字体、自动换行、行号等）
 *       4. Markdown 预览设置（主题、自定义 CSS）
 *       5. 无障碍设置入口
 *       6. 功能开关（Feature Flags）
 *       7. 网络设置（GitHub 镜像）
 *       8. 反馈功能（带截图附件的邮件反馈）
 *       9. 社交平台链接
 *       10. 应用验证与版本信息
 *       11. 危险操作（清除缓存、重置设置）
 *
 * 页面结构：使用垂直滚动布局，按分区组织设置项，每个分区有 SectionHeader 标识。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.sync.SyncFailureKind
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandPill
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.account.AccountTimeFormat
import com.draftpeek.feature.stats.account.AccountUiState
import com.draftpeek.feature.stats.account.RelativeUnit
import com.draftpeek.feature.stats.account.SyncUiStatus
import kotlinx.coroutines.delay


/**
 * 「我的」页身份区（设计回函 v3 · 阶段 A 五态）。
 *
 * 完全由 [AccountUiState] 驱动：未登录（占位 + 价值主张 + 登录按钮）／已登录（昵称 + 邮箱 +
 * 同步状态行）。阶段 B 接入真实账号与同步后本组件无需改动。
 *
 */
@Composable
internal fun AccountIdentityHeader(
    state: AccountUiState,
    nowMillis: Long,
    versionName: String,
    avatarBitmap: ImageBitmap?,
    localName: String,
    onPickAvatar: () -> Unit,
    onEditName: () -> Unit,
    onSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit,
    onCancelSync: () -> Unit
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val signedIn = state as? AccountUiState.SignedIn

    // 设计回函 v4 §1：**可编辑的是本地资料**（DataStore），「未登录」描述的是**账户/同步状态**，
    // 两者各司其职。因此本地资料的编辑入口在「未登录」态保留（这是设计侧的底线要求）；
    // 已登录态的资料合并/迁移策略由阶段 B 设计稿给出，本批**不预支**，只读展示。
    val localProfileEditable = signedIn == null
    val displayName = if (localProfileEditable) localName else signedIn?.displayName ?: localName

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(PrototypeSpacing.AvatarSize + 8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(PrototypeSpacing.AvatarSize)
                    .clip(CircleShape)
                    .background(PrototypeTokens.accentSoft)
                    .then(
                        if (localProfileEditable) {
                            Modifier.clickable(onClick = onPickAvatar)
                        } else {
                            Modifier
                        }
                    )
                    .accessibilityEnhanced(
                        // 未登录态头像可点换 → 播报操作名；已登录态头像是装饰（昵称紧邻下方）→ 不播报
                        contentDescription = if (localProfileEditable) {
                            stringResource(R.string.profile_select_avatar)
                        } else {
                            null
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(PrototypeSpacing.AvatarSize)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = getInitials(displayName),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = PrototypeTokens.accent
                    )
                }
            }
            if (localProfileEditable) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrototypeTokens.accent)
                        .clickable(onClick = onPickAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.profile_select_avatar),
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 本地资料昵称（未登录态可点改名；编辑徽标在头像上）
        Text(
            text = displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = fg,
            lineHeight = 24.sp,
            modifier = if (localProfileEditable) {
                Modifier.clickable(onClick = onEditName)
            } else {
                Modifier
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 分隔：把「本地资料」与「账户状态」两块分开（设计回函 v4 §1）
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 1.dp)
                .background(border)
        )

        Spacer(modifier = Modifier.height(10.dp))

        when (state) {
            AccountUiState.SignedOut -> {
                // 账户状态行：状态 + 价值主张同一行（v4 §1：「未登录」不再是身份区标题）
                // v6 §2：改单条独立词条（拼接是 i18n 反模式，各语言语序无法靠拼接保证）
                Text(
                    text = stringResource(R.string.account_signed_out_status),
                    fontSize = 13.sp,
                    color = muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                BrandFilledButton(
                    text = stringResource(R.string.account_sign_in),
                    onClick = onSignIn
                )
            }

            is AccountUiState.SignedIn -> {
                Text(
                    text = state.email,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = muted
                )
                Spacer(modifier = Modifier.height(10.dp))
                SyncStatusRow(
                    sync = state.sync,
                    nowMillis = nowMillis,
                    onSyncNow = onSyncNow,
                    onRetry = onRetry,
                    onCancelSync = onCancelSync
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        BrandPill(
            text = versionName,
            dotColor = SemanticColors.Success
        )
    }
}


/**
 * 同步状态行（设计回函 v3 §3）：**一律行内提示，不弹窗**。
 *
 * 「正在同步…」超过 [SyncUiStatus.SYNC_CANCEL_AFTER_MS] 后行内出现「取消」。
 */
@Composable
private fun SyncStatusRow(
    sync: SyncUiStatus,
    nowMillis: Long,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit,
    onCancelSync: () -> Unit
) {
    // 同步中超 30 秒才给「取消」；用独立计时器而非 60 秒的页面 ticker，保证阈值准确
    var cancelAvailable by remember(sync) { mutableStateOf(false) }
    LaunchedEffect(sync) {
        cancelAvailable = false
        if (sync is SyncUiStatus.Syncing) {
            delay(SyncUiStatus.SYNC_CANCEL_AFTER_MS)
            cancelAvailable = true
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (sync) {
            is SyncUiStatus.Synced -> {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SemanticColors.Success
                )
                Text(
                    text = syncedAgoText(sync.lastSuccessAtMillis, nowMillis),
                    style = MonoLabelStyle,
                    color = PrototypeTokens.fgSoft
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSyncNow) {
                    Text(text = stringResource(R.string.account_sync_now))
                }
            }

            SyncUiStatus.Syncing -> {
                Text(
                    text = stringResource(R.string.account_syncing),
                    style = MonoLabelStyle,
                    color = PrototypeTokens.fgSoft
                )
                if (cancelAvailable) {
                    TextButton(onClick = onCancelSync) {
                        Text(text = stringResource(R.string.account_sync_cancel))
                    }
                }
            }

            is SyncUiStatus.Retrying -> Text(
                text = stringResource(R.string.account_sync_retrying, sync.attempt, sync.maxAttempts),
                style = MonoLabelStyle,
                color = PrototypeTokens.fgSoft
            )

            is SyncUiStatus.Failed -> {
                // 类别 → 文案查表（规格 v1.1 §3.1）；点击目标屏未实现前，仅网络/其他保持可点重试，
                // 其余（重新授权 / 重新选择 / 查看）待 B1/B4/B7/B8 落位后再接线，不给死入口。
                val textRes = when (sync.kind) {
                    SyncFailureKind.NETWORK, SyncFailureKind.OTHER -> R.string.account_sync_failed
                    SyncFailureKind.AUTH -> R.string.account_sync_auth_expired
                    SyncFailureKind.REPOSITORY -> R.string.account_sync_repo_unavailable
                    SyncFailureKind.CONFLICT, SyncFailureKind.CORRUPT -> R.string.account_sync_failed_detail
                }
                val clickableRetry =
                    sync.kind == SyncFailureKind.NETWORK || sync.kind == SyncFailureKind.OTHER
                Text(
                    text = stringResource(textRes),
                    style = MonoLabelStyle,
                    color = SemanticColors.Danger,
                    modifier = if (clickableRetry) Modifier.clickable(onClick = onRetry) else Modifier
                )
            }

            SyncUiStatus.Offline -> Text(
                text = stringResource(R.string.account_offline),
                style = MonoLabelStyle,
                color = PrototypeTokens.muted
            )
        }
    }
}


/** 「已同步 · N 分钟前」；不足 1 分钟显示「刚刚同步」。 */
@Composable
private fun syncedAgoText(lastSuccessAtMillis: Long, nowMillis: Long): String {
    val relative = AccountTimeFormat.relativeTime(nowMillis, lastSuccessAtMillis)
    return when (relative.unit) {
        RelativeUnit.JUST_NOW -> stringResource(R.string.account_synced_just_now)
        RelativeUnit.MINUTES -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_minutes, relative.value)
        )

        RelativeUnit.HOURS -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_hours, relative.value)
        )

        RelativeUnit.DAYS -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_days, relative.value)
        )
    }
}


private fun getInitials(name: String): String {
    if (name.isEmpty()) return "用"
    val firstChar = name.trim().first()
    return if (firstChar.isLetter()) {
        firstChar.uppercaseChar().toString()
    } else {
        firstChar.toString()
    }
}
