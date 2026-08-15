/**
 * 文件: StatCard.kt
 * 功能: 统计模块UI组件 - 统计卡片
 * 描述: 基础统计卡片组件，用于展示单个统计指标（标题+数值），使用品牌卡片样式。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 统计卡片组件。
 *
 * 展示一个统计指标，包含标签标题和数值，使用 Material3 Card 组件和品牌圆角样式。
 *
 * @param title 指标标签文本
 * @param value 指标数值文本
 * @param modifier 修饰符
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = PrototypeTokens.elevated,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                style = typography.labelMedium,
                color = PrototypeTokens.fgSoft,
            )
            Text(
                text = value,
                style = typography.headlineSmall,
                color = PrototypeTokens.fg,
            )
        }
    }
}
