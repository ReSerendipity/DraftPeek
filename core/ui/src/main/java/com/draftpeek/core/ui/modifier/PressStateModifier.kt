/**
 * 按压状态缩放效果 Modifier。
 *
 * 提供按压时的缩放反馈效果，遵循 Material Design 3 交互反馈指南。
 * 当元素被按下时缩放到原始大小的 95%，松开后恢复，带有平滑动画过渡。
 * 适用于按钮、卡片等可交互元素。
 */
package com.draftpeek.core.ui.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * 当元素被按下时应用按压缩放效果（原始大小的 95%）。
 * 遵循 MD3 交互反馈指南。
 * 将此 Modifier 应用于按钮和卡片等可交互元素。
 *
 * @return 应用了按压缩放效果的 Modifier
 */
fun Modifier.pressScaleEffect(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )
    this.scale(scale)
}
