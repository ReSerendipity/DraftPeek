/**
 * 文件: DayDetailDialog.kt
 * 功能: 统计模块UI组件 - 单日详情对话框
 * 描述: 点击热力图日期单元格时弹出的对话框，显示该日期的详细活动数据，包括：
 *       - 日期和星期
 *       - 活动智能摘要
 *       - 活跃时段标签（凌晨/上午/下午/夜间等）
 *       - 四项核心指标（使用时长、书写字符、阅读文件、创建文件）
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.viewmodel.DayDetail

/**
 * 单日活动详情对话框。
 *
 * 展示用户点击热力图某一天后的详细统计信息，包括活动摘要、活跃时段和四项核心指标。
 *
 * @param detail 单日详情数据对象，包含日期、摘要、活跃时段和各项指标
 * @param onDismiss 对话框关闭回调
 */
@Composable
fun DayDetailDialog(detail: DayDetail, onDismiss: () -> Unit) {
    val monthDayText = stringResource(
        R.string.stats_day_detail_month_day_format,
        detail.date.monthValue,
        detail.date.dayOfMonth
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = monthDayText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = PrototypeTokens.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = detail.dayOfWeek,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = PrototypeTokens.fgSoft
                )

                Text(
                    text = detail.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = PrototypeTokens.fgSoft,
                    textAlign = TextAlign.Center
                )

                if (detail.activeTimePeriods.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detail.activeTimePeriods.forEach { period ->
                            Text(
                                text = period,
                                fontSize = 12.sp,
                                color = PrototypeTokens.accent,
                                modifier = Modifier
                                    .background(
                                        color = PrototypeTokens.accentSoft,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricItem(
                            label = stringResource(R.string.stats_card_usage_duration),
                            value = "${detail.usageDurationMinutes}min",
                            modifier = Modifier.weight(1f)
                        )
                        MetricItem(
                            label = stringResource(R.string.stats_card_chars_written),
                            value = "%,d".format(detail.charWriteCount),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricItem(
                            label = stringResource(R.string.stats_card_files_read),
                            value = "${detail.fileReadCount}${stringResource(R.string.stats_unit_files)}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricItem(
                            label = stringResource(R.string.stats_card_files_created),
                            value = "${detail.fileCreateCount}${stringResource(R.string.stats_unit_files)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                BrandOutlinedButton(
                    text = stringResource(R.string.stats_close),
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        },
        confirmButton = {}
    )
}

/**
 * 指标项组件。
 *
 * 在对话框中显示单个统计指标，包含标签和数值两部分。
 *
 * @param label 指标标签文本
 * @param value 指标数值文本
 * @param modifier 修饰符
 */
@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = PrototypeTokens.muted
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = PrototypeTokens.fg
        )
    }
}
