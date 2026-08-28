/**
 * 文件: PeriodChipRow.kt
 * 功能: 统计模块UI组件 - 时间周期选择器
 * 描述: 统计页面的时间周期筛选栏，提供横向滚动的筛选芯片，支持选择摘要/年/月/周/今日/昨日等统计周期。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandFilterChip
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.viewmodel.StatsPeriod

/**
 * 统计周期选择芯片行。
 *
 * 显示横向可滚动的筛选芯片列表，用户可以选择不同的统计时间周期来查看对应的数据。
 *
 * @param selectedPeriod 当前选中的统计周期
 * @param onPeriodSelected 周期选择回调
 * @param modifier 修饰符
 */
@Composable
fun PeriodChipRow(selectedPeriod: StatsPeriod, onPeriodSelected: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    val periods = listOf(
        StatsPeriod.SUMMARY to stringResource(R.string.stats_period_summary),
        StatsPeriod.YEAR to stringResource(R.string.stats_period_year),
        StatsPeriod.MONTH to stringResource(R.string.stats_period_month),
        StatsPeriod.WEEK to stringResource(R.string.stats_period_week),
        StatsPeriod.TODAY to stringResource(R.string.stats_period_today),
        StatsPeriod.YESTERDAY to stringResource(R.string.stats_period_yesterday)
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (period, label) ->
            BrandFilterChip(
                text = label,
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}
