/**
 * 文件: ContributionLegend.kt
 * 功能: 统计模块UI组件 - 热力图图例
 * 描述: 热力图颜色强度图例组件，显示从"少"到"多"的颜色渐变指示，帮助用户理解热力图颜色含义。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.stats.R
import androidx.compose.material3.MaterialTheme

/**
 * 热力图贡献度图例组件。
 *
 * 在热力图下方显示颜色强度指示，包含"少"和"多"的文字标签，以及 5 个颜色方块表示活动强度从低到高。
 *
 * @param heatColors 热力图颜色列表，应包含 5 个等级的颜色
 * @param modifier 修饰符
 */
@Composable
fun ContributionLegend(
    heatColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_less),
            style = typography.bodySmall,
            color = PrototypeTokens.muted,
        )
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = heatColors[index.coerceIn(0, heatColors.lastIndex)],
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = stringResource(R.string.stats_more),
            style = typography.bodySmall,
            color = PrototypeTokens.muted,
        )
    }
}
