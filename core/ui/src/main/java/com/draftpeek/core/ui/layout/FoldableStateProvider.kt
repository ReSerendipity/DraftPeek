/**
 * 折叠屏设备状态提供器。
 *
 * 提供折叠屏设备状态的监听与适配支持，包括：
 * - [FoldableState] 枚举：表示折叠状态（展开、半开、闭合）
 * - [FoldInfo] 数据类：包含完整的折叠信息（状态、方向、边界等）
 * - [rememberFoldableState]：观察折叠状态并返回 Compose State
 * - [hingeAvoidancePadding]：为铰链区域提供避让内边距
 *
 * 基于 Jetpack WindowManager 库实现，支持各种折叠屏设备形态。
 */
package com.draftpeek.core.ui.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * 表示当前折叠设备的状态。
 */
enum class FoldableState {
    /** 设备完全展开（平板模式）— 使用双窗格布局 */
    FLAT,

    /** 设备半开（桌面/笔记本模式）— 使用分屏布局 */
    HALF_OPENED,

    /** 设备完全闭合或非折叠设备 — 使用单窗格布局 */
    CLOSED
}

/**
 * 折叠信息数据类，包含折叠状态的完整信息。
 *
 * @param state 当前折叠状态
 * @param isSeparating 折叠是否将屏幕物理分隔为两个区域
 * @param orientation 折叠方向（水平或垂直）
 * @param bounds 折叠铰链的边界矩形
 */
data class FoldInfo(
    val state: FoldableState = FoldableState.CLOSED,
    val isSeparating: Boolean = false,
    val orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.VERTICAL,
    val bounds: android.graphics.Rect = android.graphics.Rect()
)

/**
 * 观察折叠特性状态并将其作为 Compose State 返回。
 *
 * 使用 [WindowInfoTracker] 监听窗口布局信息变化，自动更新折叠状态。
 *
 * @return 包含当前 [FoldInfo] 的 State 对象
 */
@Composable
fun rememberFoldableState(): State<FoldInfo> {
    val context = LocalContext.current
    return produceState(initialValue = FoldInfo()) {
        val windowInfoTracker = WindowInfoTracker.getOrCreate(context)
        windowInfoTracker.windowLayoutInfo(context).collect { layoutInfo ->
            val foldingFeature = layoutInfo.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull()

            value = if (foldingFeature != null) {
                FoldInfo(
                    state = when (foldingFeature.state) {
                        FoldingFeature.State.FLAT -> FoldableState.FLAT
                        FoldingFeature.State.HALF_OPENED -> FoldableState.HALF_OPENED
                        else -> FoldableState.CLOSED
                    },
                    isSeparating = foldingFeature.isSeparating,
                    orientation = foldingFeature.orientation,
                    bounds = foldingFeature.bounds
                )
            } else {
                FoldInfo()
            }
        }
    }
}

/**
 * 当设备具有分隔屏幕的活动折叠特性时，应用铰链避让内边距。
 *
 * 关键交互元素（按钮、输入框）应使用此 Modifier 以确保与铰链区域保持至少 32dp 的距离。
 *
 * @param foldInfo 当前折叠信息
 * @return 应用了避让内边距的 Modifier
 */
fun Modifier.hingeAvoidancePadding(foldInfo: FoldInfo): Modifier = if (foldInfo.isSeparating) {
    val hingePadding = 32.dp
    when (foldInfo.orientation) {
        FoldingFeature.Orientation.HORIZONTAL -> this.padding(top = hingePadding, bottom = hingePadding)
        else -> this.padding(start = hingePadding, end = hingePadding)
    }
} else {
    this
}
