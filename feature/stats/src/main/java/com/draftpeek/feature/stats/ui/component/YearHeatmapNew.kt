/**
 * 文件: YearHeatmapNew.kt
 * 功能: 统计模块UI组件 - 年度热力图
 * 描述: GitHub 风格的年度活动热力图组件，展示全年每一天的使用活跃度。
 *       特性：
 *       - 7行（周一至周日）× 53/54列（周）网格布局
 *       - 根据使用时长分为 5 个颜色等级
 *       - 显示月份标签和星期标签
 *       - 阶梯式月份分隔线（红色竖线/灰色横线）
 *       - 支持水平滚动，初始滚动到当前季度
 *       - 响应式单元格大小（手机约13周可见，平板显示更多）
 *       - 日期数字显示在单元格内
 *       - 点击日期单元格触发回调查看详情
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 年度热力图组件。
 *
 * 展示全年活动数据的 GitHub 风格热力图，支持横向滚动查看历史月份。
 * 单元格颜色根据当日使用时长分为5级：无活动/≤10分钟/≤45分钟/≤90分钟/>90分钟。
 *
 * @param activities 活动数据映射（日期字符串 -> UserActivity）
 * @param onDayClick 日期单元格点击回调
 * @param modifier 修饰符
 * @param isDark 是否为暗色主题
 */
@Composable
fun YearHeatmapNew(
    activities: Map<String, UserActivity>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalDarkTheme.current
) {
    val today = LocalDate.now()
    val currentYear = today.year
    val firstOfYear = LocalDate.of(currentYear, 1, 1)
    val startDate = firstOfYear.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastOfYear = LocalDate.of(currentYear, 12, 31)
    val endDate = lastOfYear.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val totalWeeks = ChronoUnit.WEEKS.between(startDate, endDate).toInt() + 1

    val accent = PrototypeTokens.accent
    val emptyColor = if (isDark) Color(0xFF2C2C2F) else Color(0xFFE0E0E0)
    val heatColors = remember(isDark, accent) {
        listOf(
            emptyColor,
            accent.copy(alpha = 0.15f),
            accent.copy(alpha = 0.30f),
            accent.copy(alpha = 0.55f),
            accent.copy(alpha = 0.80f)
        )
    }

    val cellYearMonths = remember(startDate, totalWeeks) {
        Array(totalWeeks) { weekIdx ->
            Array(7) { dayIdx ->
                YearMonth.from(startDate.plusDays((weekIdx * 7 + dayIdx).toLong()))
            }
        }
    }

    val dividerColor = if (isDark) Color(0xFF6E6E73) else Color(0xFFAAAAAA)

    val currentWeekIdx = remember {
        ChronoUnit.WEEKS.between(startDate, today).toInt().coerceIn(0, totalWeeks - 1)
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        val targetWeek = (currentWeekIdx - 6).coerceAtLeast(0)
        val targetPx = with(density) {
            val estimatedCellSize = 42.dp
            val estStep = estimatedCellSize + 2.dp
            (estStep * targetWeek).roundToPx()
        }
        scrollState.scrollTo(targetPx)
    }

    val monthLabels = remember(startDate, totalWeeks, currentYear) {
        val labels = mutableListOf<Pair<Int, String>>()
        for (weekIdx in 0 until totalWeeks) {
            val weekStart = startDate.plusDays((weekIdx * 7L))
            if (weekStart.year != currentYear) continue
            val ym = YearMonth.from(weekStart)
            val prevWeekMonth = if (weekIdx > 0) {
                YearMonth.from(
                    startDate.plusDays(((weekIdx - 1) * 7L))
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                )
            } else {
                null
            }
            if (ym != prevWeekMonth) {
                labels.add(weekIdx to "${ym.monthValue}月")
            }
        }
        val lastWeekStart = YearMonth.from(startDate.plusDays(((totalWeeks - 1) * 7L)))
        val lastLabelWeekIdx = totalWeeks - 1
        if (labels.isEmpty() || labels.last().second != "${lastWeekStart.monthValue}月") {
            labels.add(lastLabelWeekIdx to "${lastWeekStart.monthValue}月")
        }
        labels
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth - 48.dp

        val targetWeeksVisible = 13
        val idealCellSize = (availableWidth / targetWeeksVisible - 2.dp).coerceIn(24.dp, 48.dp)

        val weekdayLabelWidth = 28.dp
        val cellSpacing = 2.dp
        val cellSize = idealCellSize
        val cellStep = cellSize + cellSpacing
        val monthLabelHeight = 18.dp

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                WeekdayLabelsColumn(
                    cellSize = cellSize,
                    cellSpacing = cellSpacing,
                    labelWidth = weekdayLabelWidth
                )

                Box(
                    modifier = Modifier.horizontalScroll(scrollState)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(cellStep * totalWeeks)
                                .height(monthLabelHeight)
                        ) {
                            monthLabels.forEach { (weekIdx, label) ->
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = PrototypeTokens.muted,
                                    modifier = Modifier.offset(x = cellStep * weekIdx)
                                )
                            }
                        }

                        Box {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                            ) {
                                for (weekIdx in 0 until totalWeeks) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(cellSpacing)
                                    ) {
                                        for (dayIdx in 0 until 7) {
                                            val date = startDate.plusDays((weekIdx * 7 + dayIdx).toLong())
                                            val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val activity = activities[dateKey]
                                            val isFuture = date.isAfter(today)
                                            val isOtherYear = date.year != currentYear

                                            val level = when {
                                                isOtherYear || isFuture -> -1
                                                activity == null -> 0
                                                activity.usageDurationMinutes <= 0 -> 0
                                                activity.usageDurationMinutes <= 10 -> 1
                                                activity.usageDurationMinutes <= 45 -> 2
                                                activity.usageDurationMinutes <= 90 -> 3
                                                else -> 4
                                            }

                                            val cellColor = when {
                                                isOtherYear || isFuture -> heatColors[0].copy(alpha = 0.4f)
                                                else -> heatColors[level]
                                            }

                                            val textColor = when {
                                                isOtherYear || isFuture -> PrototypeTokens.muted.copy(alpha = 0.5f)
                                                cellColor.luminance() < 0.4f -> Color.White
                                                else -> Color(0xFF555555)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(cellSize)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (!isFuture && !isOtherYear) {
                                                            Modifier
                                                                .pressScaleEffect()
                                                                .clickable(
                                                                    interactionSource = remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                                    indication = null,
                                                                    onClick = { onDayClick(date) }
                                                                )
                                                        } else {
                                                            Modifier
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${date.dayOfMonth}",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 11.sp,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Canvas(modifier = Modifier.matchParentSize()) {
                                val stepPx = (cellSize + cellSpacing).toPx()
                                val halfGap = cellSpacing.toPx() / 2f

                                val vertPath = Path()
                                val horizPath = Path()

                                run {
                                    val y = -halfGap
                                    horizPath.moveTo(0f, y)
                                    horizPath.lineTo(stepPx * totalWeeks - halfGap, y)
                                }

                                run {
                                    val y = stepPx * 7 - halfGap
                                    horizPath.moveTo(0f, y)
                                    horizPath.lineTo(stepPx * totalWeeks - halfGap, y)
                                }

                                for (weekIdx in 0 until totalWeeks) {
                                    for (dayIdx in 0 until 7) {
                                        val curMonth = cellYearMonths[weekIdx][dayIdx]

                                        if (weekIdx + 1 < totalWeeks &&
                                            cellYearMonths[weekIdx + 1][dayIdx] != curMonth
                                        ) {
                                            val x = stepPx * (weekIdx + 1) - halfGap
                                            val yTop = stepPx * dayIdx
                                            val yBot = stepPx * (dayIdx + 1) - halfGap
                                            vertPath.moveTo(x, yTop)
                                            vertPath.lineTo(x, yBot)
                                        }

                                        if (dayIdx < 6 &&
                                            cellYearMonths[weekIdx][dayIdx + 1] != curMonth
                                        ) {
                                            val y = stepPx * (dayIdx + 1) - halfGap
                                            val xL = stepPx * weekIdx
                                            val xR = stepPx * (weekIdx + 1) - halfGap
                                            horizPath.moveTo(xL, y)
                                            horizPath.lineTo(xR, y)
                                        }
                                    }
                                }

                                val strokeW = 1.dp.toPx()
                                val dash = PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 3.dp.toPx())
                                )
                                val style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeW,
                                    pathEffect = dash
                                )

                                drawPath(path = vertPath, color = Color(0xFFE04050), style = style)
                                drawPath(path = horizPath, color = dividerColor, style = style)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.stats_less),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = PrototypeTokens.muted
                )
                Spacer(modifier = Modifier.width(6.dp))
                repeat(5) { idx ->
                    Box(
                        modifier = Modifier
                            .size(cellSize.coerceAtMost(18.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatColors[idx])
                    )
                    if (idx < 4) Spacer(modifier = Modifier.width(3.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.stats_more),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = PrototypeTokens.muted
                )
            }
        }
    }
}

/**
 * 星期标签列组件。
 *
 * 在热力图左侧显示固定的星期标签（一/三/五/日），与热力图行对齐。
 *
 * @param cellSize 单元格大小
 * @param cellSpacing 单元格间距
 * @param labelWidth 标签列宽度
 */
@Composable
private fun WeekdayLabelsColumn(cellSize: Dp, cellSpacing: Dp, labelWidth: Dp) {
    val labels = listOf("一", "", "三", "", "五", "", "日")

    Column(
        verticalArrangement = Arrangement.spacedBy(cellSpacing)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .size(labelWidth, cellSize),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = PrototypeTokens.muted
                    )
                }
            }
        }
    }
}
