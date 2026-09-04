/**
 * 触摸目标尺寸 Modifier。
 *
 * 遵循 Material Design 无障碍设计指南，确保可点击组件的最小触摸目标为 48×48dp。
 * 小于 48dp 的视觉元素将通过透明方式扩展其可点击区域。
 *
 * 使用时应在任何 `clickable()` Modifier **之后**、`size()` 等尺寸 Modifier **之前** 应用，
 * 即推荐顺序：`Modifier.clickable { }.minimumTouchTarget().size(40.dp)`。
 */
package com.draftpeek.core.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Material Design 推荐的最小触摸目标尺寸。 */
val MinimumTouchTargetSize: Dp = 48.dp

/**
 * 确保 Composable 具有最小 48×48dp 的触摸目标，
 * 符合 Material Design 无障碍设计指南的推荐标准。
 *
 * ## 与 `sizeIn()` 实现的区别（重要）
 * 早期实现等价于 `sizeIn(minWidth = 48.dp, minHeight = 48.dp)`，它会把**约束下发给子组件**，
 * 导致视觉尺寸被一并撑大到 48dp——例如 30dp 高的开关轨道会被拉成 48dp，破坏视觉设计。
 *
 * 本实现采用"外扩 + 居中"策略：子组件仍按自身期望尺寸测量（30dp 轨道保持 30dp），
 * 仅外层布局节点被扩展到 ≥48dp 并把子组件居中放置。因此：
 * - **视觉尺寸不变**（图标按钮仍是 40dp、开关轨道仍是 30dp）；
 * - **可点击区域扩大到 48dp**，满足无障碍触控要求。
 *
 * 为了让点击真正落在 48dp 区域内，`clickable()` 必须写在本 Modifier **之前**（即更外层），
 * 使其成为同一个 48dp 布局节点上的指针输入修饰符。
 *
 * @param minSize 最小触摸目标边长，默认 48dp
 */
fun Modifier.minimumTouchTarget(minSize: Dp = MinimumTouchTargetSize): Modifier =
    this.then(MinimumTouchTargetModifier(minSize))

/**
 * 实现方式参考 Material3 的 `MinimumInteractiveModifier`：
 * 子组件以"最小约束归零"的方式测量，保证它不会被动膨胀；外层再取
 * `max(子组件尺寸, minSize)` 作为自身尺寸。
 */
private data class MinimumTouchTargetModifier(private val minSize: Dp) : LayoutModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val minPx = minSize.roundToPx()
        // 归零最小约束：让子组件（如 40dp 图标按钮 / 30dp 开关轨道）保留自身视觉尺寸
        val relaxed = constraints.copy(minWidth = 0, minHeight = 0)
        val placeable = measurable.measure(relaxed)

        // 外扩到最小触摸目标，但不得超过父级上限
        val width = max(placeable.width, minPx).coerceAtMost(constraints.maxWidth)
        val height = max(placeable.height, minPx).coerceAtMost(constraints.maxHeight)

        return layout(width, height) {
            placeable.place(
                x = (width - placeable.width) / 2,
                y = (height - placeable.height) / 2
            )
        }
    }
}
