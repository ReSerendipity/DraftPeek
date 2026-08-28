/**
 * 多标签页标签栏组件。
 *
 * 提供水平滚动的标签栏，每个标签显示文件类型徽标（彩色方块+扩展名缩写）、文件名、关闭按钮。
 * 激活标签显示底部 accent 色指示条。单标签时隐藏标签栏（顶栏已展示文件名，避免重复行）。
 * 关闭已修改的标签时弹出确认对话框。激活标签切换时自动滚动到可见区域。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.common.model.TabId
import com.draftpeek.core.ui.component.ConfirmDialog
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.CodeTextStyle
import com.draftpeek.core.ui.theme.FileTypeColors
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.EditorTab
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 多标签页标签栏 Composable。
 *
 * 水平可滚动的紧凑标签栏，匹配代码编辑器原型设计，包含文件类型徽标、accent 指示条、边框分隔线。
 * 单标签时隐藏标签栏；多标签时支持水平滚动和自动定位到激活标签。
 * 关闭已修改标签时弹出确认对话框。
 *
 * @param tabs 当前打开的标签页列表
 * @param activeTabId 当前激活标签页的 ID
 * @param onTabClick 标签点击回调（切换到该标签）
 * @param onTabClose 标签关闭按钮点击回调
 * @param onTabReorder 标签拖拽重排回调（(拖拽的标签 ID, 目标索引)），非空时启用长按拖拽重排
 * @param modifier 修饰符
 */
@Composable
fun TabBar(
    tabs: List<EditorTab>,
    activeTabId: TabId?,
    onTabClick: (TabId) -> Unit,
    onTabClose: (TabId) -> Unit,
    onTabReorder: ((TabId, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // State for the close-confirmation dialog on modified tabs.
    var pendingCloseTabId by remember { mutableStateOf<TabId?>(null) }

    if (tabs.size <= 1) return

    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val haptic = LocalHapticFeedback.current

    // Dragging state for tab reorder (long-press drag).
    // dragOffsetX accumulates the horizontal drag delta since the last swap so the
    // dragged item visually follows the finger; on crossing an adjacent tab's midpoint
    // we perform a single adjacent swap and reset the baseline.
    var dragTabId by remember { mutableStateOf<TabId?>(null) }
    var dragStartIndex by remember { mutableStateOf(0) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(activeTabId, tabs) {
        val activeIndex = tabs.indexOfFirst { it.id == activeTabId }
        if (activeIndex <= 0) return@LaunchedEffect
        var offsetPx = 0
        for (i in 0 until activeIndex) {
            val estimatedWidth = ((tabs[i].fileName.length * 7).dp + 40.dp).coerceIn(80.dp, 200.dp)
            offsetPx += with(density) { estimatedWidth.roundToPx() }
        }
        scope.launch { scrollState.animateScrollTo(offsetPx) }
    }

    /**
     * Estimate the pixel width of the given tab, matching the prototype metric.
     */
    fun estimatedTabWidthPx(tab: EditorTab): Int {
        val w = ((tab.fileName.length * 7).dp + 40.dp).coerceIn(80.dp, 200.dp)
        return with(density) { w.roundToPx() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(surface)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isActive = tab.id == activeTabId
            val isDraggingThis = dragTabId == tab.id
            val currentReorder = onTabReorder
            // 实时重排阶段需要知道被拖标签的"当前"位置，用于乒乓交换判定。
            val reorderHandler = currentReorder
            // 水平偏移仅作用于被拖拽的标签，实现跟随手指的视觉效果。
            val dragModifier = if (isDraggingThis && dragOffsetX != 0f) {
                Modifier.offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            } else {
                Modifier
            }
            val reorderModifier = if (reorderHandler != null && tabs.size > 1) {
                Modifier.pointerInput(tab.id, tabs, reorderHandler) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            dragTabId = tab.id
                            dragStartIndex = tabs.indexOfFirst { it.id == tab.id }.coerceAtLeast(0)
                            dragOffsetX = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            // 乒乓交换：累计位移越过左侧/右侧相邻标签一半宽度即交换一次，并重置参照。
                            val oldIndex = tabs.indexOfFirst { it.id == tab.id }.coerceAtLeast(0)
                            val neighborIndex = if (dragOffsetX > 0f) oldIndex + 1 else oldIndex - 1
                            if (neighborIndex in 0 until tabs.size) {
                                val neighborWidth = estimatedTabWidthPx(tabs[neighborIndex])
                                val threshold = neighborWidth / 2f
                                if (dragOffsetX > threshold || dragOffsetX < -threshold) {
                                    reorderHandler(tab.id, neighborIndex)
                                    dragStartIndex = neighborIndex
                                    dragOffsetX = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragEnd = {
                            dragTabId = null
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            dragTabId = null
                            dragOffsetX = 0f
                        }
                    )
                }
            } else {
                Modifier
            }
            TabItem(
                tab = tab,
                isActive = isActive,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabClick(tab.id)
                },
                onClose = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (tab.isModified) {
                        pendingCloseTabId = tab.id
                    } else {
                        onTabClose(tab.id)
                    }
                },
                showRightBorder = index < tabs.lastIndex,
                modifier = dragModifier.then(reorderModifier)
            )
        }
    }

    HorizontalDivider(thickness = 1.dp, color = border)

    // Confirmation dialog for closing a modified tab.
    if (pendingCloseTabId != null) {
        val tabToClose = tabs.find { it.id == pendingCloseTabId }
        ConfirmDialog(
            title = stringResource(R.string.editor_unsaved_changes),
            message = stringResource(
                R.string.editor_tab_close_modified_message,
                tabToClose?.fileName ?: stringResource(R.string.editor_tab_file)
            ),
            confirmLabel = stringResource(R.string.editor_close),
            isDestructive = true,
            onConfirm = {
                pendingCloseTabId?.let { onTabClose(it) }
                pendingCloseTabId = null
            },
            onDismiss = { pendingCloseTabId = null }
        )
    }
}

/**
 * 单个标签项 Composable。
 *
 * 显示文件类型徽标（彩色方块+2字母扩展名缩写）、文件名、关闭按钮（激活标签显示），
 * 以及底部 accent 色激活指示条。非最后一个标签右侧显示边框分隔线。
 *
 * @param tab 标签页数据
 * @param isActive 是否为当前激活标签
 * @param onClick 点击回调
 * @param onClose 关闭按钮点击回调
 * @param showRightBorder 是否显示右侧边框分隔线
 * @param modifier 附加修饰符（拖拽手势、位移等）
 */
@Composable
private fun TabItem(
    tab: EditorTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    showRightBorder: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bgColor by animateColorAsState(
        targetValue = if (isActive) PrototypeTokens.bg else PrototypeTokens.surface,
        animationSpec = tween(200),
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) PrototypeTokens.fg else PrototypeTokens.muted,
        animationSpec = tween(200),
        label = "tab_content"
    )
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    // Derive file extension and type code/color from the file name
    val extension = tab.fileName.substringAfterLast('.', "")
    val typeCode = extension.uppercase().take(2)
    val typeColor = FileTypeColors.forExtension(extension, isDark)

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = PrototypeSpacing.EditorTabPaddingH, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // File type badge (HTML prototype: border-radius 4px for mini)
            Box(
                modifier = Modifier
                    .size(PrototypeSpacing.EditorTabMiniBadge)
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = typeCode,
                    style = MonoLabelStyle.copy(
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // File name (HTML prototype: 12px mono font)
            Text(
                text = tab.fileName,
                style = CodeTextStyle.copy(
                    color = contentColor,
                    fontSize = PrototypeSpacing.EditorTabFontSize
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Close button — shown for each tab in the multi-tab bar
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(14.dp)
            ) {
                StrokeIcon(
                    icon = StrokeIcons.Close,
                    contentDescription = stringResource(R.string.editor_close_tab),
                    modifier = Modifier.size(10.dp),
                    tint = contentColor
                )
            }
        }

        // Active indicator line (2dp accent-colored for active, transparent for inactive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isActive) accent else Color.Transparent)
        )

        // Right border separator between tabs
        if (showRightBorder) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(38.dp)
                    .background(PrototypeTokens.borderSoft)
            )
        }
    }
}

/**
 * 迷你文件类型徽标已移除：单标签时不再显示迷你标签栏（顶栏已展示文件名，避免重复行）。
 */
