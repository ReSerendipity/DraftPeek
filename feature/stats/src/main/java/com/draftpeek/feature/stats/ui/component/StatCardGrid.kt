/**
 * 文件: StatCardGrid.kt
 * 功能: 统计模块UI组件 - 统计卡片网格
 * 描述: 2×2 网格布局的统计卡片组，展示四项核心统计指标：使用时长、书写字符、阅读文件、创建文件。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.viewmodel.PeriodStats

/**
 * 统计卡片网格组件。
 *
 * 以 2×2 网格布局展示四项核心统计指标，使用等宽字体显示数值，支持数值格式化回调。
 *
 * @param stats 周期统计数据
 * @param formatDuration 使用时长格式化函数（分钟转友好显示）
 * @param formatNumber 数字格式化函数（大数转简写如 1.2k）
 * @param modifier 修饰符
 */
@Composable
fun StatCardGrid(
    stats: PeriodStats,
    formatDuration: (Long) -> String,
    formatNumber: (Long) -> String,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCardItem(
                label = stringResource(R.string.stats_card_usage_duration),
                value = formatDuration(stats.usageDurationMinutes),
                unit = "",
                modifier = Modifier.weight(1f),
                cardShape = cardShape
            )
            StatCardItem(
                label = stringResource(R.string.stats_card_chars_written),
                value = formatNumber(stats.charWriteCount),
                unit = stringResource(R.string.stats_unit_chars),
                modifier = Modifier.weight(1f),
                cardShape = cardShape
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCardItem(
                label = stringResource(R.string.stats_card_files_read),
                value = stats.fileReadCount.toString(),
                unit = stringResource(R.string.stats_unit_files),
                modifier = Modifier.weight(1f),
                cardShape = cardShape
            )
            StatCardItem(
                label = stringResource(R.string.stats_card_files_created),
                value = stats.fileCreateCount.toString(),
                unit = stringResource(R.string.stats_unit_files),
                modifier = Modifier.weight(1f),
                cardShape = cardShape
            )
        }
    }
}

/**
 * 单个统计卡片项组件。
 *
 * 网格中的单个卡片，显示标签、数值和单位，数值使用等宽字体加粗显示。
 *
 * @param label 指标标签
 * @param value 指标数值文本
 * @param unit 单位文本（如"字"、"个"）
 * @param cardShape 卡片圆角形状
 * @param modifier 修饰符
 */
@Composable
private fun StatCardItem(
    label: String,
    value: String,
    unit: String,
    cardShape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PrototypeTokens.surface, cardShape)
            .border(1.dp, PrototypeTokens.border, cardShape)
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                color = PrototypeTokens.muted
            )
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = PrototypeTokens.fg
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted,
                        modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}
