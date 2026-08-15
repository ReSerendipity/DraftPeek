/**
 * 文件: ActivityCalendarGrid.kt
 * 功能: 统计模块UI组件 - 通用活动日历网格
 * 描述: 通用的7行活动热力图网格组件，是年度热力图的基础实现。
 *       性能优化：使用 `key()` Compose 最小化重组，记忆最大强度和日期格式化器避免每个单元格分配。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.ui.theme.ContributionLevel
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.accentHeatMapColors
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * 通用的7行活动热力图网格组件。
 *
 * 性能优化：使用 `key()` Compose 最小化重组，记忆最大强度值和日期格式化器以避免每个单元格的重复分配。
 * 根据活动强度相对于最大值的比例，将单元格分为5个贡献等级。
 *
 * @param startDate 网格起始日期（应为周一）
 * @param weeks 显示的周数
 * @param activities 活动数据映射（日期字符串 -> UserActivity）
 * @param heatColors 热力图颜色列表，应包含5个等级颜色
 * @param modifier 修饰符
 * @param cellSize 单元格大小
 * @param cellSpacing 单元格间距
 * @param showMonthLabels 是否显示月份标签
 * @param showWeekdayLabels 是否显示星期标签
 * @param onDayClick 日期单元格点击回调，为 null 时不可点击
 */
@Composable
fun ActivityCalendarGrid(
    startDate: LocalDate,
    weeks: Int,
    activities: Map<String, UserActivity>,
    heatColors: List<Color> = accentHeatMapColors(LocalDarkTheme.current),
    modifier: Modifier = Modifier,
    cellSize: Dp = 12.dp,
    cellSpacing: Dp = 3.dp,
    showMonthLabels: Boolean = false,
    showWeekdayLabels: Boolean = false,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val isDark = LocalDarkTheme.current
    val emptyCellColor = PrototypeTokens.bg
    val maxIntensity = remember(activities) {
        activities.values.maxOfOrNull { it.totalIntensity() } ?: 0
    }
    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val weekdayLabelWidth = 24.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showMonthLabels) {
            MonthLabelsRow(
                startDate = startDate,
                weeks = weeks,
                cellSize = cellSize,
                cellSpacing = cellSpacing,
                showWeekdayLabels = showWeekdayLabels,
                weekdayLabelWidth = weekdayLabelWidth,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            if (showWeekdayLabels) {
                WeekdayLabelsColumn(
                    startDate = startDate,
                    cellSize = cellSize,
                    cellSpacing = cellSpacing,
                    labelWidth = weekdayLabelWidth,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
            ) {
                repeat(7) { dayIndex ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                    ) {
                        repeat(weeks) { weekIndex ->
                            val cellIndex = weekIndex * 7 + dayIndex
                            key(cellIndex) {
                                val date = startDate.plusDays(cellIndex.toLong())
                                val dateKey = date.format(dateFormatter)
                                val activity = activities[dateKey]
                                val cellColor = if (activity == null || !activity.hasActivity()) {
                                    emptyCellColor
                                } else {
                                    val level = contributionLevel(
                                        activity.totalIntensity(),
                                        maxIntensity,
                                    )
                                    heatColors[level.ordinal.coerceIn(0, heatColors.lastIndex)]
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(cellColor)
                                        .border(
                                            width = 0.5.dp,
                                            color = PrototypeTokens.border.copy(
                                                alpha = if (isDark) 0.2f else 0.1f,
                                            ),
                                            shape = RoundedCornerShape(2.dp),
                                        )
                                        .then(
                                            if (onDayClick != null) {
                                                Modifier.clickable { onDayClick(date) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .semantics {
                                            contentDescription = buildContentDescription(
                                                date,
                                                activity,
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 月份标签行组件。
 *
 * 在热力图顶部显示月份缩写标签，仅在月份变化时显示标签。
 *
 * @param startDate 网格起始日期
 * @param weeks 显示的周数
 * @param cellSize 单元格大小
 * @param cellSpacing 单元格间距
 * @param showWeekdayLabels 是否显示星期标签（用于计算左边距）
 * @param weekdayLabelWidth 星期标签列宽度
 */
@Composable
private fun MonthLabelsRow(
    startDate: LocalDate,
    weeks: Int,
    cellSize: Dp,
    cellSpacing: Dp,
    showWeekdayLabels: Boolean,
    weekdayLabelWidth: Dp,
) {
    Row(
        modifier = Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (showWeekdayLabels) {
            Spacer(modifier = Modifier.width(weekdayLabelWidth))
        }

        var previousMonth: Month? = null
        repeat(weeks) { weekIndex ->
            val weekStart = startDate.plusWeeks(weekIndex.toLong())
            val currentMonth = weekStart.month
            val showLabel = previousMonth != currentMonth
            previousMonth = currentMonth

            Box(
                modifier = Modifier.width(cellSize),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (showLabel) {
                    Text(
                        text = currentMonth.getDisplayName(
                            TextStyle.SHORT,
                            Locale.ENGLISH,
                        ),
                        color = PrototypeTokens.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 星期标签列组件。
 *
 * 在热力图左侧显示星期缩写标签（周一、周三、周五）。
 *
 * @param startDate 网格起始日期
 * @param cellSize 单元格大小
 * @param cellSpacing 单元格间距
 * @param labelWidth 标签列宽度
 */
@Composable
private fun WeekdayLabelsColumn(
    startDate: LocalDate,
    cellSize: Dp,
    cellSpacing: Dp,
    labelWidth: Dp,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(cellSpacing),
    ) {
        repeat(7) { dayIndex ->
            val dayOfWeek = startDate.plusDays(dayIndex.toLong()).dayOfWeek
            val label = when (dayIndex) {
                0, 2, 4 -> dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                else -> ""
            }

            Box(
                modifier = Modifier.size(labelWidth, cellSize),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        color = PrototypeTokens.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * 根据活动强度计算贡献等级。
 *
 * 将活动强度与最大强度的比值映射到5个贡献等级：
 * - NONE: 无活动
 * - LOW: ≤20%
 * - MEDIUM: ≤40%
 * - HIGH: ≤70%
 * - VERY_HIGH: >70%
 *
 * @param intensity 当前活动强度
 * @param maxIntensity 最大活动强度
 * @return 贡献等级
 */
private fun contributionLevel(intensity: Int, maxIntensity: Int): ContributionLevel {
    if (intensity <= 0 || maxIntensity <= 0) return ContributionLevel.NONE
    val ratio = intensity.toFloat() / maxIntensity
    return when {
        ratio <= 0.2f -> ContributionLevel.LOW
        ratio <= 0.4f -> ContributionLevel.MEDIUM
        ratio <= 0.7f -> ContributionLevel.HIGH
        else -> ContributionLevel.VERY_HIGH
    }
}

/**
 * 构建无障碍内容描述文本。
 *
 * 为屏幕阅读器提供日期和活动数据的中文描述。
 *
 * @param date 日期
 * @param activity 活动数据，为 null 或无活动时显示"无活动"
 * @return 无障碍描述文本
 */
private fun buildContentDescription(date: LocalDate, activity: UserActivity?): String {
    val dateText = date.format(DateTimeFormatter.ofPattern("M月d日"))
    return if (activity == null || !activity.hasActivity()) {
        "$dateText，无活动"
    } else {
        "$dateText，文件打开 ${activity.fileOpenCount} 次，文本编辑 ${activity.textEditCount} 次，其他操作 ${activity.otherOperationCount} 次"
    }
}
