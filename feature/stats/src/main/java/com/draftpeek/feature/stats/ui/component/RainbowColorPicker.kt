/**
 * 文件: RainbowColorPicker.kt
 * 功能: 统计模块UI组件 - 彩虹主题色选择器
 * 描述: 圆形主题色选择器组件，展示一组彩虹色选项供用户选择应用主题色，选中项有放大和边框动画效果。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.RainbowColor

/**
 * 彩虹主题色选择器组件。
 *
 * 显示一行圆形颜色按钮，选中的颜色会放大并添加边框，带有平滑的动画过渡效果。
 * 根据当前明暗主题自动选择对应的亮色/暗色变体。
 *
 * @param selectedColor 当前选中的主题色
 * @param onColorSelected 颜色选择回调
 * @param modifier 修饰符
 */
@Composable
fun RainbowColorPicker(
    selectedColor: RainbowColor,
    onColorSelected: (RainbowColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "主题色",
            style = MaterialTheme.typography.labelLarge,
            color = PrototypeTokens.fgSoft
        )
        Spacer(modifier = Modifier.width(16.dp))
        RainbowColor.entries.forEach { color ->
            val isSelected = color == selectedColor
            val size = animateDpAsState(
                targetValue = if (isSelected) 32.dp else 28.dp,
                label = "colorSize"
            )
            val borderWidth = animateDpAsState(
                targetValue = if (isSelected) 3.dp else 0.dp,
                label = "colorBorder"
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                label = "colorScale"
            )

            Box(
                modifier = Modifier
                    .size(size.value)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(if (isDark) color.baseDark else color.baseLight)
                    .border(
                        width = borderWidth.value,
                        color = PrototypeTokens.fg,
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onColorSelected(color) }
                    )
                    .semantics {
                        contentDescription = color.displayName
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}
