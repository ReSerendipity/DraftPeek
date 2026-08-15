/**
 * DraftPeek 可拖拽分隔线组件。
 *
 * 设计层级：原子组件（Atom）— 最小可复用 UI 基元。
 *
 * 用于分屏布局的垂直可拖拽分隔线。
 * 主要特性：
 * - 细可见线（1dp）配合更宽的触摸目标（24dp），确保无障碍访问
 * - 悬停或拖拽时改变颜色提供视觉反馈
 * - 通过 [onDrag] 回调报告水平拖拽增量（像素）
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 用于分屏布局的垂直可拖拽分隔线。
 *
 * 特性：
 * - 细可见线（1dp）配合更宽的触摸目标（24dp）以实现无障碍访问
 * - 悬停或拖拽时改变颜色以提供视觉反馈
 * - 通过 [onDrag] 报告水平拖拽增量
 *
 * @param onDrag 每次拖拽步骤时调用的回调，[deltaPx] 为水平拖拽增量（像素）
 * @param modifier 分隔线的可选 [Modifier]
 */
@Composable
fun DraggableDivider(
    onDrag: (deltaPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()

    val isActive = isHovered || isDragged

    val dividerColor = if (isActive) {
        PrototypeTokens.accent
    } else {
        PrototypeTokens.border
    }

    val backgroundColor = if (isActive) {
        PrototypeTokens.accent.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(TouchTargetWidth)
            .hoverable(interactionSource)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(VisibleLineWidth)
                .background(dividerColor)
        )
    }
}

/** 触摸目标宽度，确保足够的触摸区域 */
private val TouchTargetWidth = 24.dp
/** 可见分隔线宽度 */
private val VisibleLineWidth = 1.dp
