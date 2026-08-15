/**
 * 文件: TimeRangeSelector.kt
 * 功能: 统计模块UI组件 - 时间范围选择器
 * 描述: 活动记录筛选的时间范围选择器，提供全部/30天/7天三个选项，使用 FilterChip 样式。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.model.TimeRange
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 时间范围选择器组件。
 *
 * 提供三个预设时间范围选项（全部/最近30天/最近7天），用于筛选活动记录数据。
 *
 * @param selectedRange 当前选中的时间范围
 * @param onRangeSelected 范围选择回调
 * @param modifier 修饰符
 */
@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimeRange.entries.forEach { range ->
            val labelRes = when (range) {
                TimeRange.ALL -> R.string.stats_filter_all
                TimeRange.D30 -> R.string.stats_filter_30d
                TimeRange.D7 -> R.string.stats_filter_7d
            }
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        text = stringResource(labelRes),
                        style = typography.labelLarge,
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrototypeTokens.accentSoft,
                    selectedLabelColor = PrototypeTokens.accent,
                ),
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}
