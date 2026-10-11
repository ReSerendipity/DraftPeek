/**
 * 编辑器主界面模块。
 *
 * 提供代码编辑、Markdown预览、多文档查看（PDF/Office/媒体）、多标签页管理、
 * 搜索替换、快捷设置、代码片段等核心编辑功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandFilterChip
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.KeyboardSafeBottomSheet
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.EditorStatusBarStyle
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.sora.SearchCountDisplay
import com.draftpeek.feature.editor.sora.SearchMatchCounter
import com.draftpeek.feature.editor.sora.SoraEditorWrapper
import kotlinx.coroutines.delay

private const val TAG = "EditorScreen"


@Composable
internal fun EditorSearchPanel(
    mode: SearchPanelMode,
    onModeChange: (SearchPanelMode) -> Unit,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceText: String,
    onReplaceTextChange: (String) -> Unit,
    searchRegex: Boolean,
    onSearchRegexChange: (Boolean) -> Unit,
    searchMatchCase: Boolean,
    onSearchMatchCaseChange: (Boolean) -> Unit,
    searchWholeWord: Boolean,
    onSearchWholeWordChange: (Boolean) -> Unit,
    goToLineInput: String,
    onGoToLineInputChange: (String) -> Unit,
    wrapper: SoraEditorWrapper,
    context: android.content.Context
) {
    // Shared state for go-to-line error (must be visible in both content and confirmButton)
    var goToLineError by remember { mutableStateOf("") }
    // 「全部替换」二次确认
    var showReplaceAllConfirm by remember { mutableStateOf(false) }

    // 匹配计数 n/N（屏 15）。sora 的 searcher 只暴露布尔值，这里在缓冲区文本上
    // 按同口径复算（见 SearchMatchCounter）。searchNavTick 用于「↑↓ 导航后重新定位序号」。
    var searchNavTick by remember { mutableStateOf(0) }
    var matchTotal by remember { mutableStateOf(0) }
    var matchOrdinal by remember { mutableStateOf(0) }
    // 供 ↑↓ 导航复用（避免再算一遍）
    var matchRanges by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    LaunchedEffect(searchQuery, searchRegex, searchMatchCase, searchWholeWord, searchNavTick) {
        if (searchQuery.isEmpty()) {
            matchTotal = 0
            matchOrdinal = 0
            matchRanges = emptyList()
            return@LaunchedEffect
        }
        // 防抖：边输边全量计数会很吵，也让大文件不至于每敲一个字就扫一遍
        delay(SEARCH_COUNT_DEBOUNCE_MS)
        val matches = SearchMatchCounter.findAll(
            text = wrapper.currentTextForSearch(),
            query = searchQuery,
            regex = searchRegex,
            matchCase = searchMatchCase,
            wholeWord = searchWholeWord
        )
        matchTotal = matches.size
        matchRanges = matches
        matchOrdinal = SearchMatchCounter.ordinalAt(matches, wrapper.currentCursorOffset())
    }

    // ↑↓ 导航：**自管移动**，不走 sora 的 gotoNext/gotoPrevious —— 真机走查发现它们在
    // 「同一行内的相邻匹配」上不推进选区（如 `bb` 里搜 `b`，序号会永远停在 1/N）。
    // 这里直接用面板自己算出的匹配表定位，与显示的 n/N 天然一致。
    val moveToAdjacentMatch: (Int) -> Unit = { delta ->
        val ranges = matchRanges
        if (ranges.isNotEmpty()) {
            val current = SearchMatchCounter.ordinalAt(ranges, wrapper.currentCursorOffset())
            // 光标不在任何匹配上时：下一个 → 第 1 个；上一个 → 最后一个（与多数编辑器一致）
            val currentIndex = if (current == 0) {
                if (delta > 0) -1 else 0
            } else {
                current - 1
            }
            val next = ((currentIndex + delta) % ranges.size + ranges.size) % ranges.size
            wrapper.selectMatchRange(ranges[next].first, ranges[next].last + 1)
        }
        searchNavTick++
    }

    // Live search: re-run search when query or options change (for Find and Replace tabs)
    LaunchedEffect(searchQuery, searchRegex, searchMatchCase, searchWholeWord, mode) {
        if (mode == SearchPanelMode.FIND || mode == SearchPanelMode.REPLACE) {
            if (searchQuery.isNotBlank()) {
                wrapper.search(
                    searchQuery,
                    regex = searchRegex,
                    matchCase = searchMatchCase,
                    wholeWord = searchWholeWord
                )
            }
        }
    }

    val tabs = listOf(
        SearchPanelMode.FIND to stringResource(R.string.editor_search_tab_find),
        SearchPanelMode.REPLACE to stringResource(R.string.editor_search_tab_replace),
        SearchPanelMode.GOTO to stringResource(R.string.editor_search_tab_goto_line)
    )

    // 由居中模态改为停靠键盘上方的底部弹层（实施指导书 §2.3 屏 18，复用 §3.1 键盘规范）
    KeyboardSafeBottomSheet(
        onDismissRequest = onDismiss,
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEach { (tabMode, tabLabel) ->
                    val isSelected = tabMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrototypeTokens.surface else Color.Transparent)
                            .clickable { onModeChange(tabMode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            style = DraftPeekTypography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) PrototypeTokens.accent else PrototypeTokens.muted
                            )
                        )
                    }
                }
            }
        },
        body = {
            when (mode) {
                SearchPanelMode.FIND -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text(text = stringResource(R.string.editor_find_content)) },
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandFilterChip(
                                text = stringResource(R.string.editor_regex),
                                selected = searchRegex,
                                onClick = { onSearchRegexChange(!searchRegex) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_match_case),
                                selected = searchMatchCase,
                                onClick = { onSearchMatchCaseChange(!searchMatchCase) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_whole_word),
                                selected = searchWholeWord,
                                onClick = { onSearchWholeWordChange(!searchWholeWord) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // ↑↓ 匹配导航（实施指导书 §2.3 屏 18）
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    // 设计回函 v2 · Q1：未定位显示「N 个结果」而非 0/N；
                                    // 超过 999 显示「999+ 个结果」；已定位才是 i / N。
                                    text = when (
                                        SearchMatchCounter.countDisplay(matchOrdinal, matchTotal)
                                    ) {
                                        SearchCountDisplay.NONE ->
                                            stringResource(R.string.editor_search_no_match)

                                        SearchCountDisplay.LOCATED ->
                                            stringResource(
                                                R.string.editor_search_match_count,
                                                matchOrdinal,
                                                matchTotal
                                            )

                                        SearchCountDisplay.TOTAL ->
                                            stringResource(
                                                R.string.editor_search_match_total,
                                                matchTotal
                                            )

                                        SearchCountDisplay.TOTAL_CAPPED ->
                                            stringResource(R.string.editor_search_match_total_capped)
                                    },
                                    style = EditorStatusBarStyle,
                                    color = PrototypeTokens.muted
                                )
                            }
                            SearchNavButtons(
                                onPrevious = { moveToAdjacentMatch(-1) },
                                onNext = { moveToAdjacentMatch(1) }
                            )
                        }
                    }
                }
                SearchPanelMode.REPLACE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text(text = stringResource(R.string.editor_find_content)) },
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BrandFilterChip(
                                text = stringResource(R.string.editor_regex),
                                selected = searchRegex,
                                onClick = { onSearchRegexChange(!searchRegex) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_match_case),
                                selected = searchMatchCase,
                                onClick = { onSearchMatchCaseChange(!searchMatchCase) }
                            )
                            BrandFilterChip(
                                text = stringResource(R.string.editor_whole_word),
                                selected = searchWholeWord,
                                onClick = { onSearchWholeWordChange(!searchWholeWord) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // ↑↓ 匹配导航（实施指导书 §2.3 屏 18）
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    // 设计回函 v2 · Q1：未定位显示「N 个结果」而非 0/N；
                                    // 超过 999 显示「999+ 个结果」；已定位才是 i / N。
                                    text = when (
                                        SearchMatchCounter.countDisplay(matchOrdinal, matchTotal)
                                    ) {
                                        SearchCountDisplay.NONE ->
                                            stringResource(R.string.editor_search_no_match)

                                        SearchCountDisplay.LOCATED ->
                                            stringResource(
                                                R.string.editor_search_match_count,
                                                matchOrdinal,
                                                matchTotal
                                            )

                                        SearchCountDisplay.TOTAL ->
                                            stringResource(
                                                R.string.editor_search_match_total,
                                                matchTotal
                                            )

                                        SearchCountDisplay.TOTAL_CAPPED ->
                                            stringResource(R.string.editor_search_match_total_capped)
                                    },
                                    style = EditorStatusBarStyle,
                                    color = PrototypeTokens.muted
                                )
                            }
                            SearchNavButtons(
                                onPrevious = { moveToAdjacentMatch(-1) },
                                onNext = { moveToAdjacentMatch(1) }
                            )
                        }
                        BrandOutlinedTextField(
                            value = replaceText,
                            onValueChange = onReplaceTextChange,
                            label = { Text(text = stringResource(R.string.editor_replace_with)) },
                            singleLine = true
                        )
                    }
                }
                SearchPanelMode.GOTO -> {
                    val totalLines = wrapper.getTotalLines()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandOutlinedTextField(
                            value = goToLineInput,
                            onValueChange = {
                                onGoToLineInputChange(it)
                                goToLineError = ""
                            },
                            label = { Text(text = stringResource(R.string.editor_line_number_range, totalLines)) },
                            singleLine = true,
                            isError = goToLineError.isNotEmpty(),
                            supportingText = if (goToLineError.isNotEmpty()) {
                                { Text(text = goToLineError) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandOutlinedButton(text = stringResource(R.string.editor_cancel), onClick = onDismiss)
                Spacer(modifier = Modifier.weight(1f))
                when (mode) {
                    SearchPanelMode.FIND -> {
                        BrandFilledButton(text = stringResource(R.string.editor_find), onClick = {
                            onDismiss()
                        })
                    }
                    SearchPanelMode.REPLACE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BrandOutlinedButton(text = stringResource(R.string.editor_replace), onClick = {
                                if (searchQuery.isNotBlank()) {
                                    wrapper.replaceCurrent(replaceText)
                                }
                            })
                            BrandFilledButton(text = stringResource(R.string.editor_replace_all), onClick = {
                                // 批量替换属破坏性操作，先二次确认（实施指导书 §3.2 高危操作隔离）
                                showReplaceAllConfirm = true
                            })
                        }
                    }
                    SearchPanelMode.GOTO -> {
                        val totalLines = wrapper.getTotalLines()
                        BrandFilledButton(text = stringResource(R.string.editor_go), onClick = {
                            val line = goToLineInput.toIntOrNull()
                            when {
                                goToLineInput.isBlank() -> {
                                    goToLineError = context.getString(R.string.editor_enter_line_number)
                                }
                                line == null || line <= 0 -> {
                                    goToLineError = context.getString(R.string.editor_enter_valid_line)
                                }
                                line > totalLines -> {
                                    goToLineError = context.getString(R.string.editor_out_of_range, totalLines)
                                }
                                else -> {
                                    wrapper.goToLine(line)
                                    onDismiss()
                                }
                            }
                        })
                    }
                }
            }
        }
    )

    // 「全部替换」二次确认（实施指导书 §2.3 屏 18）
    if (showReplaceAllConfirm) {
        BrandDialog(
            onDismissRequest = { showReplaceAllConfirm = false },
            title = { Text(text = stringResource(R.string.editor_replace_all)) },
            content = {
                Text(
                    text = stringResource(R.string.editor_replace_all_confirm, searchQuery),
                    style = DraftPeekTypography.bodyMedium
                )
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.editor_replace_all),
                    onClick = {
                        showReplaceAllConfirm = false
                        wrapper.replaceAll(
                            searchQuery,
                            replaceText,
                            regex = searchRegex,
                            matchCase = searchMatchCase,
                            wholeWord = searchWholeWord
                        )
                        onDismiss()
                    }
                )
            },
            dismissButton = {
                BrandOutlinedButton(
                    text = stringResource(R.string.editor_cancel),
                    onClick = { showReplaceAllConfirm = false }
                )
            }
        )
    }
}


/**
 * 查找面板的 ↑↓ 匹配导航（实施指导书 §2.3 屏 18）。
 *
 * 注意：sora 的搜索 API 只返回「是否命中」布尔值，**不暴露匹配总数**，
 * 故此处只提供上下导航，暂不做「n/N」计数（那需要新的搜索结果统计能力）。
 */
@Composable
private fun SearchNavButtons(onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TooltipIconButton(tooltip = stringResource(R.string.editor_find_previous), onClick = onPrevious) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.editor_find_previous),
                tint = PrototypeTokens.fgSoft
            )
        }
        TooltipIconButton(tooltip = stringResource(R.string.editor_find_next), onClick = onNext) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.editor_find_next),
                tint = PrototypeTokens.fgSoft
            )
        }
    }
}
