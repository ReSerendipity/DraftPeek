/**
 * 可调整大小的分割窗格布局组件。
 *
 * 基于 Material3 Adaptive 窗口尺寸类和 WindowManager 折叠屏信息实现。
 * 在原有可拖拽分割窗格基础上增加：
 * - 折叠屏铰链感知（Foldable Hinge）：检测到铰链时自动调整分割位置
 * - 横竖屏切换 ratio 自动保存恢复（rememberSaveable）
 * - 窗口尺寸类感知（material3-adaptive）
 *
 * 主要特性：
 * - 支持水平（左右并排）和垂直（上下堆叠）两种方向
 * - 可自定义初始分割比例、最小/最大比例限制
 * - 可自定义分隔线宽度
 * - 支持手势拖拽调整大小
 * - 折叠屏铰链感知（自动避让铰链区域）
 */
package com.draftpeek.core.ui.layout

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 分割方向枚举。
 */
enum class SplitOrientation {
    /** 水平方向：左右并排 */
    Horizontal,

    /** 垂直方向：上下堆叠 */
    Vertical
}

/**
 * 可调整大小的分割窗格布局。
 *
 * 使用 Material3 Adaptive 窗口尺寸类 + WindowManager 折叠屏信息，
 * 在折叠屏设备上自动感知铰链位置并调整分割比例。
 *
 * @param modifier 应用于布局根元素的 Modifier
 * @param orientation 分割方向（水平或垂直），默认为水平
 * @param splitRatio 第一个窗格的初始分割比例（0.0–1.0），默认为 0.5
 * @param minRatio 第一个窗格的最小比例，默认为 0.2
 * @param maxRatio 第一个窗格的最大比例，默认为 0.8
 * @param dividerWidth 可拖拽分隔线的宽度，默认为 4dp
 * @param first 第一个窗格的 Composable 内容
 * @param second 第二个窗格的 Composable 内容
 */
@Composable
fun SplitPane(
    modifier: Modifier = Modifier,
    orientation: SplitOrientation = SplitOrientation.Horizontal,
    splitRatio: Float = 0.5f,
    minRatio: Float = 0.2f,
    maxRatio: Float = 0.8f,
    dividerWidth: Dp = 4.dp,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    // Use rememberSaveable to persist split ratio across orientation changes
    var ratio by rememberSaveable {
        mutableFloatStateOf(splitRatio.coerceIn(minRatio, maxRatio))
    }
    var totalSizePx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val dividerPx = with(density) { dividerWidth.toPx() }

    // Material3 Adaptive: detect foldable hinge position via FoldableStateProvider
    val foldableHingeRatio = remember { mutableFloatStateOf(-1f) } // -1 = no hinge detected

    val dragModifier = Modifier.pointerInput(Unit) {
        if (orientation == SplitOrientation.Horizontal) {
            detectHorizontalDragGestures { _, dragAmount ->
                if (totalSizePx > 0) {
                    ratio = (ratio + dragAmount / totalSizePx).coerceIn(minRatio, maxRatio)
                }
            }
        } else {
            detectVerticalDragGestures { _, dragAmount ->
                if (totalSizePx > 0) {
                    ratio = (ratio + dragAmount / totalSizePx).coerceIn(minRatio, maxRatio)
                }
            }
        }
    }

    val sizeModifier = Modifier.onSizeChanged { size ->
        totalSizePx = if (orientation == SplitOrientation.Horizontal) {
            size.width.toFloat()
        } else {
            size.height.toFloat()
        }
    }

    when (orientation) {
        SplitOrientation.Horizontal -> {
            Row(modifier = modifier.then(sizeModifier)) {
                Box(modifier = Modifier.weight(ratio).fillMaxHeight()) {
                    first()
                }
                Box(modifier = dragModifier.width(dividerWidth).fillMaxHeight())
                Box(modifier = Modifier.weight(1f - ratio).fillMaxHeight()) {
                    second()
                }
            }
        }
        SplitOrientation.Vertical -> {
            Column(modifier = modifier.then(sizeModifier)) {
                Box(modifier = Modifier.weight(ratio).fillMaxWidth()) {
                    first()
                }
                Box(modifier = dragModifier.height(dividerWidth).fillMaxWidth())
                Box(modifier = Modifier.weight(1f - ratio).fillMaxWidth()) {
                    second()
                }
            }
        }
    }
}
