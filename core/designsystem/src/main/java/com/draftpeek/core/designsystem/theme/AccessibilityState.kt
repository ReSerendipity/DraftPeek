/**
 * 无障碍状态管理文件。
 *
 * 定义全局无障碍状态数据类和CompositionLocal，支持色盲模式、高对比度模式、
 * 文字缩放、屏幕阅读器优化、振动反馈等无障碍功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 全局无障碍状态，通过 CompositionLocal 在 Compose 树中传播。
 *
 * 各字段含义：
 * - [colorBlindMode] 色盲模式，影响色彩转换
 * - [highContrastMode] 高对比度模式，增强前景/背景对比度
 * - [textScale] UI 文字缩放倍数 (0.85 ~ 1.5)
 * - [screenReaderOptimized] 屏幕阅读器优化，增强语义信息
 * - [vibrationFeedback] 振动反馈开关
 * - [nonColorIndicators] 非色彩标识，为依赖颜色的元素添加图标/形状/文本标签
 */
@Immutable
data class AccessibilityState(
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    val highContrastMode: Boolean = false,
    val textScale: Float = 1.0f,
    val screenReaderOptimized: Boolean = false,
    val vibrationFeedback: Boolean = false,
    val nonColorIndicators: Boolean = false
) {
    /** 是否有任何无障碍功能被启用 */
    val anyEnabled: Boolean
        get() = colorBlindMode != ColorBlindMode.NONE ||
            highContrastMode ||
            textScale != 1.0f ||
            screenReaderOptimized ||
            vibrationFeedback ||
            nonColorIndicators

    companion object {
        val Default = AccessibilityState()
    }
}

/**
 * 全局无障碍状态 CompositionLocal。
 * 在 [DraftPeekTheme] 中通过 [AccessibilityStateProvider] 提供。
 */
val LocalAccessibilityState = staticCompositionLocalOf { AccessibilityState.Default }

/**
 * 便捷访问当前无障碍状态。
 */
val accessibilityState: AccessibilityState
    @Composable get() = LocalAccessibilityState.current
