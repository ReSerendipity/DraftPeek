/**
 * 跨文件搜索界面组件。
 *
 * 提供搜索框输入关键词，在多个文件中搜索匹配内容，显示匹配的文件名和行片段。
 * 支持搜索结果点击跳转到对应文件，使用 filePath 作为稳定 key 避免并发搜索导致的列表错乱。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SearchBarHintStyle
import com.draftpeek.core.common.util.RequestCanceller
import com.draftpeek.feature.editor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 文件搜索结果数据类。
 *
 * @property fileName 匹配的文件名
 * @property filePath 文件路径（作为列表稳定 key）
 * @property lineNumbers 匹配的行号列表
 * @property lineSnippets 匹配行的内容片段列表
 */
data class FileSearchResult(
    val fileName: String,
    val filePath: String,
    val lineNumbers: List<Int>,
    val lineSnippets: List<String>,
)

/**
 * 跨文件搜索界面 Composable。
 *
 * 提供搜索框，用户输入关键词后在多个文件中搜索，显示匹配结果列表。
 * 支持搜索加载状态、无结果状态显示，点击结果项可跳转到对应文件。
 *
 * @param onBack 返回回调
 * @param onFileClick 文件点击回调，参数为文件路径
 * @param searchInFiles 挂起函数，执行实际的跨文件搜索，返回搜索结果列表
 * @param modifier 修饰符
 */
@Composable
fun CrossFileSearchScreen(
    onBack: () -> Unit,
    onFileClick: (String) -> Unit,
    searchInFiles: suspend (String) -> List<FileSearchResult>,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FileSearchResult>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val searchCanceller = remember { RequestCanceller(scope) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun performSearch() {
        if (query.isNotBlank()) {
            searchCanceller.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { isSearching = true }
                try {
                    val searchResults = searchInFiles(query)
                    withContext(Dispatchers.Main) {
                        results = searchResults
                        hasSearched = true
                    }
                } finally {
                    withContext(Dispatchers.Main) { isSearching = false }
                }
            }
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PrototypeSpacing.ScreenHorizontal),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(PrototypeSpacing.HistoryButtonSizeB)
                        .clip(PrototypeShapes.Medium)
                        .border(1.dp, border, PrototypeShapes.Medium)
                        .background(surface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.editor_back),
                        tint = fgSoft,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.editor_cross_file_search),
                    style = H2Style.copy(color = fg),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PrototypeSpacing.SearchBarHeight)
                    .clip(PrototypeShapes.InputField)
                    .background(surface)
                    .border(1.dp, border, PrototypeShapes.InputField)
                    .padding(horizontal = PrototypeSpacing.InputFieldPaddingH),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.editor_find),
                    tint = muted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { if (it.length <= 500) query = it },
                    singleLine = true,
                    textStyle = DraftPeekTypography.bodyMedium.copy(color = fg),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch() },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_search_content),
                                style = SearchBarHintStyle.copy(color = muted),
                            )
                        }
                        innerTextField()
                    },
                )
                if (query.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(PrototypeShapes.Pill)
                            .background(accent)
                            .clickable(enabled = !isSearching) { performSearch() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = surface,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.editor_search),
                                style = DraftPeekTypography.labelMedium.copy(
                                    color = surface,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    hasSearched && results.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.editor_no_match_found),
                                    style = DraftPeekTypography.bodyMedium.copy(color = muted),
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // OPTIMIZE: [F4] - 使用 filePath 作为稳定 key，避免并发搜索结果
                            // （C-05 改造后 awaitAll 顺序受调度影响）顺序变化导致的：
                            // 1) LazyColumn 重组时丢失滚动位置；
                            // 2) 列表项的进入/退出动画错乱（item 复用错位）。
                            // filePath 在同一查询内唯一稳定，是 Compose 推荐的 key 选型。
                            items(results, key = { it.filePath }) { result ->
                                FileSearchResultItem(
                                    result = result,
                                    onClick = { onFileClick(result.filePath) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 文件搜索结果项 Composable。
 *
 * 显示单个文件的搜索结果：文件名、最多 3 个匹配行的行号和片段预览。
 *
 * @param result 搜索结果数据
 * @param onClick 点击回调
 */
@Composable
private fun FileSearchResultItem(
    result: FileSearchResult,
    onClick: () -> Unit,
) {
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PrototypeShapes.Card)
            .background(surface)
            .border(1.dp, border, PrototypeShapes.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = result.fileName,
            style = DraftPeekTypography.bodyMedium.copy(
                color = fg,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        result.lineSnippets.take(3).forEachIndexed { idx, snippet ->
            Text(
                text = stringResource(
                    R.string.editor_line_snippet,
                    (result.lineNumbers.getOrNull(idx) ?: "").toString(),
                    snippet.take(80),
                ),
                style = DraftPeekTypography.bodySmall.copy(color = muted),
                maxLines = 1,
            )
        }
    }
}
