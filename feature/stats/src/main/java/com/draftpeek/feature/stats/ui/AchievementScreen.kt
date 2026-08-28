/**
 * 文件: AchievementScreen.kt
 * 功能: 统计模块UI - 成就展示页面
 * 描述: 成就系统展示页面，按分类展示所有成就项，包含：
 *       - 顶部统计卡片：已解锁/未解锁数量、完成百分比
 *       - 按成就分类分组展示（阅读文件、创建文件、使用时长、书写字符等9大分类）
 *       - 每个分类显示分类标题、图标和进度（已解锁/总数）
 *       - 2列网格布局展示成就卡片，已解锁成就高亮显示，未解锁成就置灰
 *       - 成就卡片显示图标、名称、描述和等级标签（T1-T7）
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.model.Achievement
import com.draftpeek.feature.stats.model.AchievementCategory
import com.draftpeek.feature.stats.util.AchievementDefinitions
import com.draftpeek.feature.stats.viewmodel.StatsViewModel

/**
 * 成就展示页面。
 *
 * 按分类展示所有成就项，顶部显示解锁统计概览，下方以2列网格展示每个分类下的成就卡片。
 *
 * @param onNavigateUp 返回导航回调
 * @param viewModel 统计 ViewModel
 */
@Composable
fun AchievementScreen(onNavigateUp: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val allAchievements = AchievementDefinitions.allAchievements
    val unlockedIds = unlockedAchievements.map { it.id }.toSet()

    val unlockedCount = unlockedAchievements.size
    val totalCount = allAchievements.size
    val lockedCount = totalCount - unlockedCount
    val completionPercent = if (totalCount > 0) (unlockedCount * 100) / totalCount else 0

    val categories = AchievementCategory.entries.toList()
    val categoryMap = allAchievements.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        BrandTopBar(
            title = stringResource(R.string.stats_achievements_title),
            onBack = onNavigateUp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        value = unlockedCount.toString(),
                        label = stringResource(R.string.stats_achievement_unlocked_count),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        value = lockedCount.toString(),
                        label = stringResource(R.string.stats_achievement_locked_count),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        value = "$completionPercent%",
                        label = stringResource(R.string.stats_achievement_completion),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            categories.forEach { category ->
                val achievements = categoryMap[category] ?: emptyList()
                val unlockedInCategory = achievements.count { it.id in unlockedIds }

                item {
                    CategoryHeader(
                        category = category,
                        unlocked = unlockedInCategory,
                        total = achievements.size
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(((achievements.size + 1) / 2 * 100).dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(achievements, key = { it.id }) { achievement ->
                            val isUnlocked = achievement.id in unlockedIds
                            AchievementItem(
                                achievement = achievement,
                                isUnlocked = isUnlocked
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 成就统计摘要卡片。
 *
 * 页面顶部的统计概览卡片，显示一个数值和对应的标签。
 *
 * @param value 统计数值
 * @param label 标签文本
 * @param modifier 修饰符
 */
@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .background(PrototypeTokens.surface, shape)
            .border(1.dp, PrototypeTokens.border, shape)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = PrototypeTokens.fg
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = PrototypeTokens.muted
            )
        }
    }
}

/**
 * 成就分类标题组件。
 *
 * 显示分类图标、分类名称和该分类的解锁进度（已解锁/总数）。
 *
 * @param category 成就分类
 * @param unlocked 该分类已解锁数量
 * @param total 该分类成就总数
 */
@Composable
private fun CategoryHeader(category: AchievementCategory, unlocked: Int, total: Int) {
    val icon = when (category) {
        AchievementCategory.READ -> Icons.AutoMirrored.Filled.MenuBook
        AchievementCategory.CREATE -> Icons.AutoMirrored.Filled.NoteAdd
        AchievementCategory.DURATION -> Icons.Default.Schedule
        AchievementCategory.CHARS -> Icons.Default.Edit
        AchievementCategory.STREAK -> Icons.Default.LocalFireDepartment
        AchievementCategory.TIME_PERIOD -> Icons.Default.WbSunny
        AchievementCategory.ATTENDANCE -> Icons.Default.CalendarMonth
        AchievementCategory.MILESTONE -> Icons.Default.Stars
        AchievementCategory.HOLIDAY -> Icons.Default.CardGiftcard
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrototypeTokens.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrototypeTokens.accent,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = category.displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = PrototypeTokens.fg,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$unlocked/$total",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            color = PrototypeTokens.muted
        )
    }
}

/**
 * 单个成就项卡片。
 *
 * 2列网格中的成就卡片，显示成就图标、名称、描述和等级标签。
 * 已解锁成就高亮显示（accent 色调），未解锁成就置灰显示。
 *
 * @param achievement 成就数据
 * @param isUnlocked 是否已解锁
 */
@Composable
private fun AchievementItem(achievement: Achievement, isUnlocked: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (isUnlocked) {
        PrototypeTokens.accent.copy(alpha = 0.2f)
    } else {
        PrototypeTokens.border
    }
    val bgColor = if (isUnlocked) {
        PrototypeTokens.accent.copy(alpha = 0.03f)
    } else {
        PrototypeTokens.surface
    }
    val iconTint = if (isUnlocked) {
        PrototypeTokens.accent
    } else {
        PrototypeTokens.muted
    }
    val alpha = if (isUnlocked) 1f else 0.4f

    Box(
        modifier = Modifier
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .padding(10.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrototypeTokens.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = achievement.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = PrototypeTokens.fg.copy(alpha = alpha),
                    maxLines = 1
                )
                Text(
                    text = achievement.description,
                    fontSize = 10.sp,
                    color = PrototypeTokens.muted.copy(alpha = alpha),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isUnlocked) PrototypeTokens.accentSoft else PrototypeTokens.mutedSoft)
        ) {
            Text(
                text = "T${achievement.tier}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = if (isUnlocked) PrototypeTokens.accent else PrototypeTokens.muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
