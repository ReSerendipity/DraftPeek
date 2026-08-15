/**
 * 拖拽重排控制器文件。
 *
 * 从 FileBrowserScreen.kt 中提取的拖拽重排逻辑，封装为独立的 DragDropController 类。
 * 管理拖拽状态、位置计算、自动滚动和重排回调。
 *
 * @author DraftPeek Team
 * @since 1.0.25
 */
package com.draftpeek.feature.browser.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job

/**
 * 虚拟目录区：行在视口中的边界（拖动时按需更新）
 */
data class VRowBounds(
    val index: Int,
    val top: Float,
    val bottom: Float,
    val height: Int,
)

/**
 * 虚拟目录区：绝对累计坐标（含 header），拖动开始时构建并冻结
 */
data class VAbsBounds(
    /** 在 items 中的索引（不含 header） */
    val index: Int,
    /** 相对于整个列表 header（SectionMonoHeader）顶部的绝对坐标 */
    val absTop: Float,
    val absBottom: Float,
    val height: Float,
    /** 与下一项 absTop 差 = height + spacing */
    val step: Float,
)

/**
 * 拖拽重排控制器 — 封装虚拟目录区域的拖拽重排逻辑。
 *
 * 从 FileBrowserScreen.kt 提取，管理：
 * - 拖拽状态（起始索引、插入索引、手指位置、偏移量）
 * - 位置计算（绝对坐标构建、插入索引解析、目标索引解析）
 * - 自动滚动（边缘检测、速率计算）
 * - 行边界追踪（VRowBounds 映射）
 *
 * 使用方式：
 * ```
 * val controller = remember { DragDropController(density, vListState) }
 * // 在 LaunchedEffect 中调用 onDragStart/onDragEnd
 * // 在 Modifier.pointerInput 中使用 controller 创建手势检测
 * ```
 *
 * @param density Compose Density，用于 dp→px 转换
 * @param listState LazyListState，用于查询可见项和滚动
 */
class DragDropController(
    val density: Density,
    val listState: LazyListState,
) {
    // ── 拖拽状态 ──
    var dragStartIndex by mutableIntStateOf(-1)
    var dragInsertionIndex by mutableIntStateOf(-1)
    var dragStartFingerY by mutableFloatStateOf(0f)
    var dragOffset by mutableFloatStateOf(0f)
    var dragScrollOffset by mutableFloatStateOf(0f)
    var animateBack by mutableStateOf(false)

    /** 行边界映射（index → bounds in viewport） */
    val rowBounds = mutableStateMapOf<Int, VRowBounds>()

    /** 绝对累计坐标（含 header）：拖动开始时为所有行构建并冻结 */
    var frozenAbsBounds by mutableStateOf<Map<Int, VAbsBounds>>(emptyMap())

    // ── 拖动锚点 ──
    var firstVisibleIdxAtStart by mutableIntStateOf(0)
    var firstVisibleOffAtStart by mutableFloatStateOf(0f)
    var originalViewportTopAbs by mutableFloatStateOf(0f)
    var viewportHeight by mutableFloatStateOf(0f)
    var autoScrollJob: Job? = null

    // ── 常量（延迟初始化，需要 density）──
    var itemSpacingPx by mutableIntStateOf(0)

    val autoScrollEdgePx: Float = with(density) { 80.dp.toPx() }
    val autoScrollMaxStepPx: Float = with(density) { 6.dp.toPx() }
    val defaultRowHeightPx: Float = with(density) { 64.dp.toPx() }
    val headerHeightPx: Float = with(density) { 48.dp.toPx() }

    init {
        itemSpacingPx = with(density) { 4.dp.toPx().toInt() }
    }

    /** 是否正在拖拽 */
    val isDragging: Boolean get() = dragStartIndex >= 0

    /**
     * onDragStart 命中：根据手指在视口的 Y 与 firstVisible/firstVisibleOffset
     * 累加模拟找命中的 item 索引（0..itemCount-1），未命中返回 -1。
     * header 命中也返回 -1。
     *
     * @param forStart true=拖拽起始命中（返回 item index），false=插入位置命中
     * @param localY 手指在视口中的 Y 坐标
     * @param itemCount 列表项总数
     */
    fun resolveTarget(forStart: Boolean, localY: Float, itemCount: Int): Int {
        val firstVisible = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        var currentY = -firstVisibleOffset.toFloat()
        val totalLogical = 1 + itemCount
        for (li in firstVisible until totalLogical) {
            val h = if (li == 0) headerHeightPx
            else {
                val itemIdx = li - 1
                rowBounds[itemIdx]?.height?.toFloat() ?: defaultRowHeightPx
            }
            val top = currentY
            val bottom = currentY + h + itemSpacingPx
            if (localY in top..bottom) {
                return if (li == 0) -1 else {
                    if (forStart) li - 1
                    else {
                        val center = (top + bottom) / 2f
                        if (localY < center) li - 1 else li
                    }
                }
            }
            currentY = bottom
        }
        return -1
    }

    /**
     * 构建所有 items（不含 header）的绝对累计坐标。
     * abs=0 在整个 LazyColumn 的"第 0 个逻辑项顶部"（即 header 顶部）。
     */
    fun buildFullAbsBounds(itemCount: Int): Map<Int, VAbsBounds> {
        val result = LinkedHashMap<Int, VAbsBounds>(itemCount)
        var top = 0f
        top += headerHeightPx + itemSpacingPx
        for (i in 0 until itemCount) {
            val h = rowBounds[i]?.height?.toFloat() ?: defaultRowHeightPx
            result[i] = VAbsBounds(
                index = i,
                absTop = top,
                absBottom = top + h,
                height = h,
                step = h + itemSpacingPx,
            )
            top += h + itemSpacingPx
        }
        return result
    }

    /** 计算当前手指的绝对 Y 坐标 */
    fun currentFingerAbsY(): Float =
        originalViewportTopAbs + dragScrollOffset + (dragStartFingerY + dragOffset)

    /**
     * 用绝对坐标找插入线（0..itemCount）
     */
    fun resolveInsertionIndex(fingerAbsY: Float, itemCount: Int): Int {
        val sorted = frozenAbsBounds.entries.sortedBy { it.key }
        for ((idx, b) in sorted) {
            if (fingerAbsY <= b.absBottom) {
                val center = (b.absTop + b.absBottom) / 2f
                return if (fingerAbsY < center) idx else idx + 1
            }
        }
        return (sorted.lastOrNull()?.key?.plus(1))
            ?.coerceAtMost(itemCount)
            ?: itemCount
    }

    /** 自动滚动速率计算 */
    fun autoScrollDelta(fingerYInViewport: Float): Float {
        if (viewportHeight <= 0f) return 0f
        val topThreshold = autoScrollEdgePx
        val bottomThreshold = (viewportHeight - autoScrollEdgePx)
            .coerceAtLeast(topThreshold)
        return when {
            fingerYInViewport < topThreshold -> {
                val overflow = topThreshold - fingerYInViewport
                val ratio = (overflow / autoScrollEdgePx).coerceIn(0f, 1f)
                -(ratio * autoScrollMaxStepPx).coerceAtLeast(0.5f)
            }
            fingerYInViewport > bottomThreshold -> {
                val overflow = fingerYInViewport - bottomThreshold
                val ratio = (overflow / autoScrollEdgePx).coerceIn(0f, 1f)
                (ratio * autoScrollMaxStepPx).coerceAtLeast(0.5f)
            }
            else -> 0f
        }
    }

    /** 解析目标索引（startIndex → insertionIndex 转换为实际目标位置） */
    fun resolveTargetIndex(startIndex: Int, insertionIndex: Int): Int =
        if (insertionIndex > startIndex) insertionIndex - 1 else insertionIndex

    // ── 状态更新方法（由外部手势回调调用）──

    /** 开始拖拽 */
    fun onDragStart(hitIndex: Int, localY: Float) {
        dragStartIndex = hitIndex
        dragInsertionIndex = hitIndex
        dragStartFingerY = localY
        dragOffset = 0f
        dragScrollOffset = 0f
        animateBack = false
    }

    /** 拖拽中更新偏移 */
    fun onDragUpdate(dragAmountY: Float, itemCount: Int) {
        if (dragStartIndex < 0) return
        dragOffset += dragAmountY
        dragInsertionIndex = resolveInsertionIndex(currentFingerAbsY(), itemCount)
    }

    /** 拖拽中自动滚动消费 */
    fun onAutoScrollConsumed(consumed: Float, itemCount: Int) {
        dragOffset += consumed
        dragScrollOffset += consumed
        dragInsertionIndex = resolveInsertionIndex(currentFingerAbsY(), itemCount)
    }

    /** 冻结绝对边界（拖拽开始时调用） */
    fun freezeBounds(itemCount: Int) {
        frozenAbsBounds = buildFullAbsBounds(itemCount)
        firstVisibleIdxAtStart = listState.firstVisibleItemIndex
        firstVisibleOffAtStart = listState.firstVisibleItemScrollOffset.toFloat()
        originalViewportTopAbs = if (firstVisibleIdxAtStart == 0) {
            0f - firstVisibleOffAtStart
        } else {
            val itemAtFirst = firstVisibleIdxAtStart - 1
            val b = frozenAbsBounds[itemAtFirst]
            if (b != null) b.absTop - firstVisibleOffAtStart
            else {
                (headerHeightPx + itemSpacingPx) + itemAtFirst * (defaultRowHeightPx + itemSpacingPx) - firstVisibleOffAtStart
            }
        }
    }

    /** 清除冻结的边界 */
    fun clearFrozenBounds() {
        frozenAbsBounds = emptyMap()
    }

    /** 拖拽结束 — 重置状态（延迟调用以允许动画） */
    fun resetDragState() {
        dragStartIndex = -1
        dragInsertionIndex = -1
        dragOffset = 0f
        dragScrollOffset = 0f
        dragStartFingerY = 0f
        animateBack = false
    }

    /** 标记动画回退 */
    fun markAnimateBack() {
        animateBack = true
    }

    /** 手指在视口中的 Y 坐标（用于自动滚动计算） */
    fun fingerInViewport(): Float = dragStartFingerY + dragOffset
}
