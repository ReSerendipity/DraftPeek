/**
 * 文件差异对比界面组件。
 *
 * 提供两种视图模式：
 * - 展开模式（平板/桌面）：左右并排显示原始文件和修改后文件，滚动同步
 * - 紧凑模式（手机）：标签页切换显示原始/修改文件
 *
 * 支持差异处高亮、上一处/下一处差异导航、差异数量统计。
 * 使用颜色编码标记不同类型的差异：插入（绿色）、删除（红色）、修改（黄色）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.util.DiffLine
import com.draftpeek.core.common.util.DiffResult
import com.draftpeek.core.common.util.DiffType
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.viewmodel.DiffViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 文件差异对比主界面 Composable。
 *
 * 根据布局模式自动选择并排视图或标签页视图，提供返回按钮、差异导航按钮、
 * 加载状态/错误状态显示，以及底部差异计数指示器。
 *
 * @param onNavigateUp 返回上一级界面的回调
 * @param layoutMode 布局模式（COMPACT/MEDIUM/EXPANDED）
 * @param foldInfo 折叠屏设备信息（暂未使用）
 * @param viewModel 差异对比 ViewModel 实例
 */
@Composable
fun DiffScreen(
    onNavigateUp: () -> Unit,
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    foldInfo: FoldInfo = FoldInfo(),
    viewModel: DiffViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentDiffIndex by viewModel.currentDiffIndex.collectAsStateWithLifecycle()

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(PrototypeSpacing.HistoryButtonSizeB)
                        .clip(PrototypeShapes.Medium)
                        .border(1.dp, border, PrototypeShapes.Medium)
                        .background(surface)
                        .clickable { onNavigateUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.editor_back),
                        tint = fgSoft,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "文件对比",
                        style = H2Style.copy(color = fg),
                        maxLines = 1
                    )
                    val diffCount = uiState.diffResult?.diffCount ?: 0
                    if (diffCount > 0) {
                        Text(
                            text = "${uiState.leftFileName} → ${uiState.rightFileName} · $diffCount 处差异",
                            style = DraftPeekTypography.bodySmall.copy(color = muted),
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "${uiState.leftFileName} → ${uiState.rightFileName}",
                            style = DraftPeekTypography.bodySmall.copy(color = muted),
                            maxLines = 1
                        )
                    }
                }
                val diffCount = uiState.diffResult?.diffCount ?: 0
                if (diffCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(PrototypeSpacing.HistoryButtonSizeB)
                            .clip(PrototypeShapes.Medium)
                            .border(1.dp, border, PrototypeShapes.Medium)
                            .background(surface)
                            .clickable { viewModel.prevDiff() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.editor_prev_diff),
                            tint = fgSoft,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(PrototypeSpacing.HistoryButtonSizeB)
                            .clip(PrototypeShapes.Medium)
                            .border(1.dp, border, PrototypeShapes.Medium)
                            .background(surface)
                            .clickable { viewModel.nextDiff() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.editor_next_diff),
                            tint = fgSoft,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "",
                                style = DraftPeekTypography.bodyLarge.copy(color = PrototypeTokens.error)
                            )
                        }
                    }
                    uiState.diffResult != null -> {
                        val isExpanded = layoutMode == LayoutMode.EXPANDED ||
                            layoutMode == LayoutMode.MEDIUM

                        if (isExpanded) {
                            SideBySideDiffContent(
                                diffResult = requireNotNull(uiState.diffResult),
                                currentDiffIndex = currentDiffIndex,
                                viewModel = viewModel,
                                border = border
                            )
                        } else {
                            TabDiffContent(
                                diffResult = requireNotNull(uiState.diffResult),
                                currentDiffIndex = currentDiffIndex,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            val diffResult = uiState.diffResult
            if (diffResult != null && diffResult.diffCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PrototypeShapes.Medium)
                        .background(surface)
                        .border(1.dp, border, PrototypeShapes.Medium)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentDiffIndex + 1}/${diffResult.diffCount} 处差异",
                        style = DraftPeekTypography.bodySmall.copy(color = fgSoft)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 左右并排差异视图 Composable。
 *
 * 在宽屏设备上使用，左右两侧分别显示原始文件和修改后文件，滚动位置同步。
 * 自动滚动到当前选中的差异处，并高亮显示。
 *
 * @param diffResult 差异计算结果
 * @param currentDiffIndex 当前选中的差异索引
 * @param viewModel 差异 ViewModel，用于获取滚动行号
 * @param border 边框颜色
 * @param modifier 修饰符
 */
@Composable
private fun SideBySideDiffContent(
    diffResult: DiffResult,
    currentDiffIndex: Int,
    viewModel: DiffViewModel,
    border: Color,
    modifier: Modifier = Modifier
) {
    val leftListState = rememberLazyListState()
    val rightListState = rememberLazyListState()

    LaunchedEffect(leftListState) {
        snapshotFlow { leftListState.firstVisibleItemIndex to leftListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                if (rightListState.firstVisibleItemIndex != index ||
                    rightListState.firstVisibleItemScrollOffset != offset
                ) {
                    rightListState.scrollToItem(index, offset)
                }
            }
    }

    LaunchedEffect(currentDiffIndex) {
        val targetLine = viewModel.getScrollLineForCurrentDiff()
        leftListState.animateScrollToItem(targetLine)
        rightListState.animateScrollToItem(targetLine)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(PrototypeShapes.Card)
            .border(1.dp, border, PrototypeShapes.Card)
    ) {
        LazyColumn(
            state = leftListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            itemsIndexed(diffResult.leftLines, key = { _, line ->
                "left_${line.lineNumber}_${line.type}"
            }) { index, line ->
                DiffLineItem(
                    line = line,
                    isHighlighted = isDiffHighlighted(diffResult, index, currentDiffIndex)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = border
        )

        LazyColumn(
            state = rightListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            itemsIndexed(diffResult.rightLines, key = { _, line ->
                "right_${line.lineNumber}_${line.type}"
            }) { index, line ->
                DiffLineItem(
                    line = line,
                    isHighlighted = isDiffHighlighted(diffResult, index, currentDiffIndex)
                )
            }
        }
    }
}

/**
 * 标签页切换差异视图 Composable。
 *
 * 在窄屏设备上使用，通过标签页切换显示原始文件或修改后文件。
 * 自动滚动到当前选中的差异处，并高亮显示。
 *
 * @param diffResult 差异计算结果
 * @param currentDiffIndex 当前选中的差异索引
 * @param viewModel 差异 ViewModel，用于获取滚动行号
 * @param modifier 修饰符
 */
@Composable
private fun TabDiffContent(
    diffResult: DiffResult,
    currentDiffIndex: Int,
    viewModel: DiffViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent

    LaunchedEffect(currentDiffIndex) {
        val targetLine = viewModel.getScrollLineForCurrentDiff()
        listState.animateScrollToItem(targetLine)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(PrototypeShapes.Card)
            .border(1.dp, border, PrototypeShapes.Card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surface)
        ) {
            DiffTabItem(
                label = stringResource(R.string.editor_diff_original),
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(border)
            )
            DiffTabItem(
                label = stringResource(R.string.editor_diff_modified),
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(thickness = 1.dp, color = border)

        val lines = if (selectedTab == 0) diffResult.leftLines else diffResult.rightLines

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines, key = { _, line -> "${line.lineNumber}_${line.type}" }) { index, line ->
                DiffLineItem(
                    line = line,
                    isHighlighted = isDiffHighlighted(diffResult, index, currentDiffIndex)
                )
            }
        }
    }
}

/**
 * 差异视图标签项 Composable。
 *
 * 标签页切换按钮，选中时显示底部 accent 色指示条和加粗文字。
 *
 * @param label 标签显示文本
 * @param isSelected 是否选中
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
private fun DiffTabItem(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrototypeTokens.bg else PrototypeTokens.surface,
        animationSpec = tween(200),
        label = "diff_tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) PrototypeTokens.fg else PrototypeTokens.muted,
        animationSpec = tween(200),
        label = "diff_tab_content"
    )
    val accent = PrototypeTokens.accent

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = DraftPeekTypography.bodySmall.copy(
                    color = contentColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) accent else Color.Transparent)
        )
    }
}

/**
 * 单行差异显示 Composable。
 *
 * 根据差异类型（插入/删除/修改/相同）显示不同的背景色和文字色，
 * 当前选中的差异行会额外叠加 accent 色高亮。
 *
 * @param line 差异行数据
 * @param isHighlighted 是否为当前选中的差异行
 */
@Composable
private fun DiffLineItem(line: DiffLine, isHighlighted: Boolean) {
    val isDark = LocalDarkTheme.current
    val backgroundColor = when (line.type) {
        DiffType.EQUAL -> Color.Transparent
        DiffType.INSERT -> if (isDark) InsertColorDark else InsertColorLight
        DiffType.DELETE -> if (isDark) DeleteColorDark else DeleteColorLight
        DiffType.MODIFY -> if (isDark) ModifyColorDark else ModifyColorLight
    }

    val accent = PrototypeTokens.accent
    val highlightAlpha = if (isHighlighted) 0.15f else 0f
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (isHighlighted) {
                    Modifier.background(accent.copy(alpha = highlightAlpha))
                } else {
                    Modifier
                }
            )
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = if (line.lineNumber > 0) line.lineNumber.toString() else "",
            color = fgSoft,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .width(44.dp)
                .padding(start = 8.dp, top = 2.dp)
        )

        val scrollState = rememberScrollState()
        val contentColor = when (line.type) {
            DiffType.INSERT -> if (isDark) InsertTextColorDark else InsertTextColorLight
            DiffType.DELETE -> if (isDark) DeleteTextColorDark else DeleteTextColorLight
            DiffType.MODIFY -> if (isDark) ModifyTextColorDark else ModifyTextColorLight
            DiffType.EQUAL -> fg
        }

        Text(
            text = line.content,
            color = contentColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .padding(end = 8.dp, top = 2.dp)
        )
    }
}

/**
 * 判断指定行是否为当前选中的差异行。
 *
 * 遍历差异行列表，找到第 currentDiffIndex 处差异，返回该行是否匹配。
 *
 * @param diffResult 差异计算结果
 * @param lineIndex 要检查的行索引
 * @param currentDiffIndex 当前选中的差异序号
 * @return 如果该行是当前选中的差异行则返回 true
 */
private fun isDiffHighlighted(diffResult: DiffResult, lineIndex: Int, currentDiffIndex: Int): Boolean {
    if (diffResult.diffCount == 0) return false
    var count = 0
    for (i in diffResult.leftLines.indices) {
        if (diffResult.leftLines[i].type != DiffType.EQUAL) {
            if (count == currentDiffIndex) return i == lineIndex
            count++
        }
    }
    return false
}

private val InsertColorLight = Color(0xFFD4EDDA)
private val DeleteColorLight = Color(0xFFF8D7DA)
private val ModifyColorLight = Color(0xFFFFF3CD)
private val InsertTextColorLight = Color(0xFF155724)
private val DeleteTextColorLight = Color(0xFF721C24)
private val ModifyTextColorLight = Color(0xFF856404)

private val InsertColorDark = Color(0xFF1B3D28)
private val DeleteColorDark = Color(0xFF3D1B1F)
private val ModifyColorDark = Color(0xFF3D3A1B)
private val InsertTextColorDark = Color(0xFF6BCF8C)
private val DeleteTextColorDark = Color(0xFFCF6B72)
private val ModifyTextColorDark = Color(0xFFCFC56B)
