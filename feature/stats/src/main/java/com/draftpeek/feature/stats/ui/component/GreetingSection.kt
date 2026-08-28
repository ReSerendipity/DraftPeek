/**
 * 文件: GreetingSection.kt
 * 功能: 统计模块UI组件 - 欢迎语区域
 * 描述: 个人资料页顶部的欢迎区域，显示个性化问候语、使用天数统计和最近解锁的成就徽章横向滚动列表。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.model.Achievement

/**
 * 欢迎语区域组件。
 *
 * 显示用户名问候、使用天数统计（DraftPeek 和天数高亮显示），以及最近解锁成就的横向滚动徽章列表。
 * 成就区域最多显示 8 个最近解锁的成就，无成就时显示空状态提示。
 *
 * @param userName 用户名
 * @param daysSinceFirstUse 首次使用至今的天数
 * @param unlockedAchievements 已解锁成就列表
 * @param onAchievementClick 成就徽章点击回调（通常跳转到成就页面）
 * @param modifier 修饰符
 */
@Composable
fun GreetingSection(
    userName: String,
    daysSinceFirstUse: Int,
    unlockedAchievements: List<Achievement>,
    onAchievementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_greeting_hello, userName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = PrototypeTokens.fg
        )

        Text(
            text = buildAnnotatedString {
                val fullText = stringResource(R.string.stats_greeting_days, daysSinceFirstUse)
                val draftpeek = "DraftPeek"
                val daysStr = daysSinceFirstUse.toString()

                val startDraftPeek = fullText.indexOf(draftpeek)
                val startDays = fullText.indexOf(daysStr)

                if (startDraftPeek >= 0) {
                    append(fullText.substring(0, startDraftPeek))
                    withStyle(
                        SpanStyle(
                            color = PrototypeTokens.accent,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(draftpeek)
                    }
                    val afterDraftPeek = startDraftPeek + draftpeek.length
                    if (startDays >= afterDraftPeek) {
                        append(fullText.substring(afterDraftPeek, startDays))
                        withStyle(
                            SpanStyle(
                                color = PrototypeTokens.accent,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(daysStr)
                        }
                        append(fullText.substring(startDays + daysStr.length))
                    } else {
                        append(fullText.substring(afterDraftPeek))
                    }
                } else {
                    append(fullText)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = PrototypeTokens.fgSoft,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (unlockedAchievements.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_achievements_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = PrototypeTokens.muted
                )
            } else {
                unlockedAchievements.take(8).forEach { achievement ->
                    AchievementBadgeChip(
                        icon = achievement.icon,
                        name = achievement.name,
                        onClick = onAchievementClick
                    )
                }
            }
        }
    }
}
