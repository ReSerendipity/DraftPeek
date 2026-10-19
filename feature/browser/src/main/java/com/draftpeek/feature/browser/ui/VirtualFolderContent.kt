package com.draftpeek.feature.browser.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.viewmodel.FileBrowserViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
/**
 * 虚拟文件夹内容（最近 / 收藏）渲染与拖拽排序。
 *
 * 从 FileBrowserScreen 拆出（2026-10-11 任务A 第一步）；拖拽状态自持于本 Composable。
 */
@Composable
internal fun VirtualFolderContent(
    selectedFilter: FileFilter,
    recentFileItems: List<FileItem>,
    favoriteFileItems: List<FileItem>,
    viewModel: FileBrowserViewModel,
    onOpenFile: (FileItem) -> Unit,
    onToggleBookmark: (String, String, String) -> Unit,
    onDeleteFile: (FileItem) -> Unit,
    onEncryptExportFile: (FileItem) -> Unit,
    onMoveTo: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    isMultiSelectMode: Boolean,
    isManualOrderMode: Boolean,
    selectedItems: Set<String>,
    modifier: Modifier = Modifier
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg
    // Virtual folder view (Recent / Favourites) — overrides normal listing
    val virtualItems = if (selectedFilter ==
        FileFilter.RECENT
    ) {
        recentFileItems
    } else {
        favoriteFileItems
    }
    val sectionTitle = if (selectedFilter == FileFilter.RECENT) {
        stringResource(R.string.browser_recent_section)
    } else {
        stringResource(R.string.browser_favorites_section)
    }
    val emptyText = if (selectedFilter == FileFilter.RECENT) {
        stringResource(R.string.browser_no_recent_files)
    } else {
        stringResource(R.string.browser_no_favorite_files)
    }

    if (virtualItems.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selectedFilter ==
                    FileFilter.RECENT
                ) {
                    Icons.Filled.History
                } else {
                    Icons.Filled.PushPin
                },
                contentDescription = null,
                tint = muted.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = emptyText,
                style = DraftPeekTypography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = fg.copy(alpha = 0.6f)
            )
        }
    } else {
        val scope = rememberCoroutineScope()
        val vListState = rememberLazyListState()
        val density = LocalDensity.current
        // ── 拖拽控制器（逻辑已提取到 DragDropController）──
        val dragController = remember { DragDropController(density, vListState) }
        var vSaveJob by remember { mutableStateOf<Job?>(null) }
        // 本地可变 state 副本（拖动过程中不修改真实顺序，仅用于同步外部 Flow）
        var localVirtualItems by remember {
            mutableStateOf(virtualItems.toList())
        }

        LaunchedEffect(virtualItems) {
            // 只有在不处于拖动状态时才同步外部变化（避免用户拖动被重置）
            if (dragController.dragStartIndex == -1) {
                localVirtualItems = virtualItems.toList()
            }
        }

        fun vSaveOrder() {
            vSaveJob?.cancel()
            val order = localVirtualItems.map { it.uri.toString() }
            vSaveJob = scope.launch {
                if (selectedFilter == FileFilter.RECENT) {
                    viewModel.saveRecentOrder(order)
                } else {
                    viewModel.savePinnedOrder(order)
                }
            }
        }

        LaunchedEffect(dragController.isDragging) {
            if (dragController.isDragging) {
                dragController.freezeBounds(localVirtualItems.size)
                // 启动自动滚动循环
                dragController.autoScrollJob?.cancel()
                dragController.autoScrollJob = launch {
                    while (dragController.dragStartIndex >= 0) {
                        val fingerInViewport = dragController.fingerInViewport()
                        val delta = dragController.autoScrollDelta(fingerInViewport)
                        if (delta != 0f) {
                            val consumed = vListState.scrollBy(delta)
                            if (consumed != 0f) {
                                dragController.onAutoScrollConsumed(
                                    consumed,
                                    localVirtualItems.size
                                )
                            }
                        }
                        withFrameNanos { }
                    }
                }
            } else {
                dragController.autoScrollJob?.cancel()
                dragController.autoScrollJob = null
                dragController.clearFrozenBounds()
            }
        }

        LazyColumn(
            state = vListState,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { dragController.viewportHeight = it.height.toFloat() }
                .pointerInput(selectedFilter) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val localY = offset.y
                            val hit = dragController.resolveTarget(
                                forStart = true,
                                localY = localY,
                                itemCount = localVirtualItems.size
                            )
                            if (hit in localVirtualItems.indices) {
                                dragController.onDragStart(hit, localY)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragController.dragStartIndex <
                                0
                            ) {
                                return@detectDragGesturesAfterLongPress
                            }
                            dragController.onDragUpdate(dragAmount.y, localVirtualItems.size)
                        },
                        onDragEnd = {
                            if (dragController.dragStartIndex >= 0) {
                                val target = dragController.resolveTargetIndex(
                                    dragController.dragStartIndex,
                                    dragController.dragInsertionIndex
                                )
                                    .coerceIn(localVirtualItems.indices)
                                if (target != dragController.dragStartIndex) {
                                    val newList = localVirtualItems.toMutableList()
                                    val moved = newList.removeAt(dragController.dragStartIndex)
                                    newList.add(target, moved)
                                    localVirtualItems = newList
                                    vSaveOrder()
                                }
                                dragController.markAnimateBack()
                            }
                            scope.launch {
                                delay(260)
                                dragController.resetDragState()
                            }
                        },
                        onDragCancel = {
                            dragController.markAnimateBack()
                            scope.launch {
                                delay(260)
                                dragController.resetDragState()
                            }
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SectionMonoHeader(
                    title = sectionTitle,
                    count = localVirtualItems.size
                )
            }
            itemsIndexed(localVirtualItems, key = { idx, it ->
                "vf_${it.uri}_$idx"
            }) { index, item ->
                val isDraggingNow = index == dragController.dragStartIndex
                val insertionAtThis = dragController.dragInsertionIndex == index && !isDraggingNow
                val stepPx: Float = run {
                    val frozen = dragController.frozenAbsBounds
                    if (frozen.isNotEmpty()) {
                        frozen[index]?.step
                            ?: (
                                (frozen[index]?.height ?: dragController.defaultRowHeightPx) +
                                    dragController.itemSpacingPx.toFloat()
                                )
                    } else {
                        val cur = dragController.rowBounds[index]
                        val next = dragController.rowBounds[index + 1]
                        if (cur != null && next != null) {
                            val d = next.top - cur.top
                            if (d > 0) return@run d
                        }
                        (cur?.height?.toFloat() ?: dragController.defaultRowHeightPx) +
                            dragController.itemSpacingPx.toFloat()
                    }
                }
                val rowDisplacement = when {
                    !dragController.isDragging -> 0f
                    dragController.dragInsertionIndex > dragController.dragStartIndex &&
                        index > dragController.dragStartIndex &&
                        index < dragController.dragInsertionIndex -> -stepPx
                    dragController.dragInsertionIndex < dragController.dragStartIndex &&
                        index >= dragController.dragInsertionIndex &&
                        index < dragController.dragStartIndex -> stepPx
                    else -> 0f
                }
                val transition = updateTransition(
                    targetState = isDraggingNow,
                    label = "vf_item_$index"
                )
                val scale by transition.animateFloat(
                    transitionSpec = {
                        if (targetState) {
                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                        } else {
                            spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                        }
                    },
                    label = "scale_v_$index"
                ) { d -> if (d) 1.06f else 1f }
                val cardElevPx by transition.animateFloat(
                    transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
                    label = "elev_v_$index"
                ) { d -> if (d) 18f else 0f }
                val bgAlpha by transition.animateFloat(
                    transitionSpec = { tween(180) },
                    label = "alpha_v_$index"
                ) { d -> if (d) 0.96f else 1f }
                val translationY by animateFloatAsState(
                    targetValue = when {
                        isDraggingNow && !dragController.animateBack -> dragController.dragOffset
                        else -> rowDisplacement
                    },
                    animationSpec = if (dragController.animateBack || !isDraggingNow) {
                        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
                    } else {
                        spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                    },
                    label = "vf_trans_$index"
                )
                StaggeredAppearance(index = index) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDraggingNow) 1f else 0f)
                            .onGloballyPositioned { coords ->
                                val h = coords.size.height
                                val info = vListState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == index + 1 } // +1 因为 header 占 index=0
                                val top = (info?.offset ?: 0).toFloat()
                                val bottom = top + h
                                dragController.rowBounds[index] = VRowBounds(index, top, bottom, h)
                            }
                            .graphicsLayer {
                                this.alpha = bgAlpha
                                this.scaleX = scale
                                this.scaleY = scale
                            }
                            .offset { IntOffset(0, translationY.roundToInt()) }
                            .then(
                                if (isDraggingNow) {
                                    Modifier.shadow(
                                        with(density) { cardElevPx.toDp() },
                                        shape = BrandShapes.Card,
                                        clip = false
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (insertionAtThis) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(
                                            color = accent,
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                )
                            }
                        }
                        FileItemComposable(
                            item = item,
                            onClick = { clickedItem ->
                                if (!dragController.isDragging && !clickedItem.isDirectory) {
                                    onOpenFile(clickedItem)
                                }
                            },
                            onTogglePin = { viewModel.togglePin(it) },
                            onToggleBookmark = onToggleBookmark,
                            onDelete = onDeleteFile,
                            onEncryptExport = onEncryptExportFile,
                            onMoveTo = onMoveTo,
                            onRename = onRename,
                            onMultiSelect = {
                                viewModel.toggleMultiSelectMode()
                                viewModel.toggleItemSelected(item.uri.toString())
                            },
                            onToggleSelect = { uri ->
                                viewModel.toggleItemSelected(uri)
                            },
                            isMultiSelectMode = isMultiSelectMode,
                            isManualOrderMode = isManualOrderMode,
                            isItemSelected = selectedItems.contains(item.uri.toString()),
                            currentDirectoryUri = "",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            // Bottom padding for FAB
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}
