/**
 * 可拖拽分隔的分屏布局组件。
 *
 * 提供水平分屏布局，中间带有可拖拽的分隔线，用户可以拖动调整左右窗格的宽度比例。
 * 主要特性：
 * - 支持初始分割比例设置
 * - 支持最小/最大分割比例限制
 * - 强制最小窗格宽度（200dp），防止窗格缩放到不可用大小
 * - 分割比例通过 [rememberSaveable] 保存，配置变更后恢复
 *
 * 适用于编辑器/预览分屏、文件列表/详情等双窗格场景。
 */
package com.draftpeek.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.DraggableDivider

/** 最小窗格宽度，防止窗格缩放到不可用大小 */
private val MinPaneWidth = 200.dp

/**
 * 带有可拖拽分隔线的分屏布局。
 *
 * [initialSplitRatio] 控制初始分割位置
 * （0.0 = 全部在左侧，1.0 = 全部在右侧）。
 * 可以拖动分隔线在 [minSplitRatio] 和 [maxSplitRatio] 范围内调整分割比例。
 *
 * 强制最小窗格宽度为 200dp，确保两个窗格都不会缩放到不可用大小。
 *
 * @param startContent 左侧（起始）窗格的 Composable 内容
 * @param endContent 右侧（结束）窗格的 Composable 内容
 * @param modifier 应用于布局根元素的 Modifier
 * @param initialSplitRatio 初始分割比例（0.0-1.0），默认为 0.4
 * @param minSplitRatio 最小分割比例，默认为 0.25
 * @param maxSplitRatio 最大分割比例，默认为 0.75
 */
@Composable
fun SplitScreenLayout(
    startContent: @Composable () -> Unit,
    endContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialSplitRatio: Float = 0.4f,
    minSplitRatio: Float = 0.25f,
    maxSplitRatio: Float = 0.75f
) {
    var splitRatio by rememberSaveable { mutableFloatStateOf(initialSplitRatio) }
    var totalWidthPx by rememberSaveable { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val minPaneWidthPx = with(density) { MinPaneWidth.roundToPx() }

    Row(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { totalWidthPx = it.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(splitRatio)
        ) {
            startContent()
        }

        DraggableDivider(
            onDrag = { deltaPx ->
                if (totalWidthPx > 0) {
                    val deltaRatio = deltaPx / totalWidthPx
                    val newRatio = (splitRatio + deltaRatio)
                        .coerceIn(minSplitRatio, maxSplitRatio)

                    val startWidthPx = newRatio * totalWidthPx
                    val endWidthPx = (1f - newRatio) * totalWidthPx
                    if (startWidthPx >= minPaneWidthPx && endWidthPx >= minPaneWidthPx) {
                        splitRatio = newRatio
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f - splitRatio)
        ) {
            endContent()
        }
    }
}
