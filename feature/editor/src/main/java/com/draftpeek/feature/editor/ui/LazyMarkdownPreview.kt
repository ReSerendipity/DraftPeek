/**
 * 流式 Markdown 预览组件（LazyColumn 虚拟滚动）。
 *
 * 将大 Markdown 文件分块后，使用 LazyColumn 只渲染可见区域内的块，
 * 实现 O(1) 内存占用和流畅滚动。适用于超大文件（>500KB）的预览。
 *
 * 每个块使用原生 Compose Text + AnnotatedString 渲染，避免 WebView 开销。
 * 支持标题、段落、代码块、列表、引用、表格、分隔线等基本 Markdown 语法。
 *
 * @author DraftPeek Team
 * @since 1.1.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.designsystem.theme.PrototypeTokens
import com.draftpeek.feature.editor.model.MarkdownBlockType
import com.draftpeek.feature.editor.model.MarkdownBlock
import com.draftpeek.feature.editor.util.chunkMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 触发流式渲染的字符阈值（超过此大小使用 LazyMarkdownPreview 替代 WebView） */
const val STREAMING_PREVIEW_THRESHOLD = 500_000

/** 单个块的最大字符数，超过则进一步拆分 */
private const val MAX_BLOCK_CHARS = 50_000

/** 预渲染额外块数（可见区域前后各预渲染这么多块） */
private const val PRELOAD_EXTRA_BLOCKS = 3

/**
 * 流式 Markdown 预览 Composable。
 *
 * 在后台线程将 Markdown 分块，然后使用 LazyColumn 虚拟滚动只渲染可见块。
 * 每个块使用原生 Compose Text + AnnotatedString 渲染，无 WebView 开销。
 *
 * @param markdownContent 要预览的 Markdown 文本
 * @param isDarkTheme 是否使用深色主题
 * @param modifier 修饰符
 */
@Composable
fun LazyMarkdownPreview(
    markdownContent: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
) {
    val listState = rememberLazyListState()

    // Chunk markdown on a background thread to avoid jank
    val blocks by produceState(initialValue = emptyList<MarkdownBlock>(), markdownContent) {
        value = withContext(Dispatchers.Default) {
            chunkMarkdown(markdownContent).flatMap { block ->
                if (block.content.length > MAX_BLOCK_CHARS) {
                    splitLargeBlock(block)
                } else {
                    listOf(block)
                }
            }
        }
    }

    val fgColor = PrototypeTokens.fg
    val fgSoftColor = PrototypeTokens.fgSoft
    val accentColor = PrototypeTokens.accent
    val codeBgColor = if (isDarkTheme) Color(0xFF1E2228) else Color(0xFFF6F8FA)
    val quoteColor = if (isDarkTheme) Color(0xFF30363D) else Color(0xFFDFE2E5)
    val dividerColor = if (isDarkTheme) Color(0xFF30363D) else Color(0xFFE1E4E8)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = blocks,
            key = { it.startIndex },
        ) { block ->
            MarkdownBlockRenderer(
                block = block,
                fgColor = fgColor,
                fgSoftColor = fgSoftColor,
                accentColor = accentColor,
                codeBgColor = codeBgColor,
                quoteColor = quoteColor,
                dividerColor = dividerColor,
                isDarkTheme = isDarkTheme,
            )
        }
    }
}

/**
 * 渲染单个 Markdown 块。
 *
 * 根据块类型选择不同的渲染方式：
 * - HEADING: 大号粗体文本
 * - CODE_BLOCK: 等宽字体 + 背景色
 * - PARAGRAPH: 带行内格式的文本
 * - LIST: 带项目符号/编号的文本
 * - BLOCKQUOTE: 左边框引用样式
 * - TABLE: 简单表格文本
 * - HORIZONTAL_RULE: 分隔线
 */
@Composable
private fun MarkdownBlockRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    fgSoftColor: Color,
    accentColor: Color,
    codeBgColor: Color,
    quoteColor: Color,
    dividerColor: Color,
    isDarkTheme: Boolean,
) {
    when (block.type) {
MarkdownBlockType.HEADING -> HeadingBlock(block, fgColor, isDarkTheme)
MarkdownBlockType.CODE_BLOCK -> CodeBlockRenderer(block, fgColor, codeBgColor)
MarkdownBlockType.PARAGRAPH -> Text(
            text = parseInlineMarkdown(block.content, fgColor, accentColor),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = fgColor,
            modifier = Modifier.fillMaxWidth(),
        )
MarkdownBlockType.UNORDERED_LIST -> ListBlockRenderer(block, fgColor, accentColor, ordered = false)
MarkdownBlockType.ORDERED_LIST -> ListBlockRenderer(block, fgColor, accentColor, ordered = true)
MarkdownBlockType.TASK_LIST -> TaskListBlockRenderer(block, fgColor, accentColor)
MarkdownBlockType.BLOCKQUOTE -> BlockquoteRenderer(block, fgColor, fgSoftColor, quoteColor)
MarkdownBlockType.TABLE -> TableBlockRenderer(block, fgColor, fgSoftColor, dividerColor)
MarkdownBlockType.HORIZONTAL_RULE -> HorizontalDivider(
            thickness = 1.dp,
            color = dividerColor,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        MarkdownBlockType.BLANK -> Box(modifier = Modifier.height(8.dp))
    }
}

/**
 * 渲染标题块。
 */
@Composable
private fun HeadingBlock(
    block: MarkdownBlock,
    fgColor: Color,
    isDarkTheme: Boolean,
) {
    val level = block.content.takeWhile { it == '#' }.length.coerceAtMost(6)
    val text = block.content.dropWhile { it == '#' || it == ' ' }.trimEnd()
    val (fontSize, fontWeight) = when (level) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 20.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.SemiBold
        4 -> 16.sp to FontWeight.SemiBold
        5 -> 14.sp to FontWeight.Medium
        else -> 13.sp to FontWeight.Medium
    }
    val headingColor = if (isDarkTheme) Color(0xFFF0F6FC) else fgColor
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = headingColor,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (level <= 2) 8.dp else 4.dp, bottom = 4.dp),
    )
}

/**
 * 渲染代码块。
 */
@Composable
private fun CodeBlockRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    codeBgColor: Color,
) {
    // Strip the ``` or ~~~ markers
    val codeContent = block.content.lines()
        .filterNot { it.trimStart().startsWith("```") || it.trimStart().startsWith("~~~") }
        .joinToString("\n")
    val codeColor = if (codeBgColor == Color(0xFF1E2228)) Color(0xFFC9D1D9) else Color(0xFF24292E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(codeBgColor)
            .padding(12.dp),
    ) {
        Text(
            text = codeContent,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = codeColor,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 渲染列表块。
 */
@Composable
private fun ListBlockRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    accentColor: Color,
    ordered: Boolean,
) {
    val lines = block.content.lines().filter { it.isNotBlank() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEachIndexed { index, line ->
            val prefix = if (ordered) {
                "${index + 1}. "
            } else {
                "• "
            }
            val content = line.trimStart()
                .removePrefix("- ").removePrefix("* ").removePrefix("+ ")
                .removePrefix("${index + 1}. ")
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = prefix,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                )
                Text(
                    text = parseInlineMarkdown(content, fgColor, accentColor),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = fgColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 渲染任务列表块。
 */
@Composable
private fun TaskListBlockRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    accentColor: Color,
) {
    val lines = block.content.lines().filter { it.isNotBlank() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line ->
            val content = line.trimStart()
                .removePrefix("- [ ] ").removePrefix("- [x] ").removePrefix("- [X] ")
                .removePrefix("* [ ] ").removePrefix("* [x] ").removePrefix("* [X] ")
                .removePrefix("+ [ ] ").removePrefix("+ [x] ").removePrefix("+ [X] ")
            val isChecked = line.contains("[x]", ignoreCase = true)
            val checkbox = if (isChecked) "☑ " else "☐ "
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = checkbox,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                )
                Text(
                    text = parseInlineMarkdown(content, fgColor, accentColor),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (isChecked) fgColor.copy(alpha = 0.6f) else fgColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 渲染引用块。
 */
@Composable
private fun BlockquoteRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    fgSoftColor: Color,
    quoteColor: Color,
) {
    val content = block.content.lines()
        .joinToString("\n") { it.removePrefix(">").trimStart() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 3.dp,
                color = quoteColor,
                shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp),
            )
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Text(
            text = parseInlineMarkdown(content, fgSoftColor, fgColor),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
            ),
            color = fgSoftColor,
        )
    }
}

/**
 * 渲染表格块（简化版，直接显示原始文本）。
 */
@Composable
private fun TableBlockRenderer(
    block: MarkdownBlock,
    fgColor: Color,
    fgSoftColor: Color,
    dividerColor: Color,
) {
    val lines = block.content.lines().filter { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, dividerColor, RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEachIndexed { index, line ->
            val cells = line.split("|").filter { it.isNotBlank() }.map { it.trim() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                cells.forEach { cell ->
                    Text(
                        text = cell,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = if (index == 0) fgColor else fgSoftColor,
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                }
            }
            if (index == 0) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = dividerColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// 行内 Markdown 解析（AnnotatedString）
// ------------------------------------------------------------------

/** 正则表达式：匹配行内 Markdown 格式 */
private val BOLD_REGEX = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
private val ITALIC_REGEX = Regex("""\*(.+?)\*|_(.+?)_""")
private val CODE_REGEX = Regex("""`([^`]+)`""")
private val STRIKE_REGEX = Regex("""~~(.+?)~~""")
private val LINK_REGEX = Regex("""\[([^\]]+)]\(([^)]+)\)""")
private val COMBINED_REGEX = Regex(
    """\*\*(.+?)\*\*|__(.+?)__|`([^`]+)`|~~(.+?)~~|\*([^*]+?)\*|_([^_]+?)_|\[([^\]]+)]\(([^)]+)\)""",
)

/**
 * 将行内 Markdown 解析为 AnnotatedString。
 *
 * 支持：粗体、斜体、行内代码、删除线、链接。
 *
 * @param text Markdown 文本
 * @param fgColor 前景色
 * @param accentColor 强调色（用于链接、代码等）
 * @return 带格式的 AnnotatedString
 */
private fun parseInlineMarkdown(
    text: String,
    fgColor: Color,
    accentColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        COMBINED_REGEX.findAll(text).forEach { match ->
            // Append plain text before this match
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }

            val fullMatch = match.value
            when {
                fullMatch.startsWith("**") || fullMatch.startsWith("__") -> {
                    val content = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = fgColor)) {
                        append(content)
                    }
                }
                fullMatch.startsWith("`") -> {
                    val content = match.groupValues[3]
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = accentColor)) {
                        append(content)
                    }
                }
                fullMatch.startsWith("~~") -> {
                    val content = match.groupValues[4]
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = fgColor)) {
                        append(content)
                    }
                }
                fullMatch.startsWith("*") -> {
                    val content = match.groupValues[5]
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = fgColor)) {
                        append(content)
                    }
                }
                fullMatch.startsWith("_") -> {
                    val content = match.groupValues[6]
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = fgColor)) {
                        append(content)
                    }
                }
                fullMatch.startsWith("[") -> {
                    val linkText = match.groupValues[7]
                    val linkUrl = match.groupValues[8]
                    withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)) {
                        append(linkText)
                    }
                }
                else -> {
                    append(fullMatch)
                }
            }

            lastIndex = match.range.last + 1
        }

        // Append remaining plain text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

/**
 * 将超大块进一步拆分为更小的块。
 *
 * 按行拆分，每 MAX_BLOCK_CHARS 字符一个子块。
 *
 * @param block 超大块
 * @return 拆分后的块列表
 */
private fun splitLargeBlock(block: MarkdownBlock): List<MarkdownBlock> {
    val lines = block.content.split('\n')
    val result = mutableListOf<MarkdownBlock>()
    val currentChunk = StringBuilder()
    var chunkStart = block.startIndex
    var currentSize = 0

    for (line in lines) {
        val lineSize = line.length + 1
        if (currentSize + lineSize > MAX_BLOCK_CHARS && currentChunk.isNotEmpty()) {
            val content = currentChunk.toString().trimEnd('\n')
            result.add(
                MarkdownBlock(
                    content = content,
                    type = block.type,
                    startIndex = chunkStart,
                    endIndex = chunkStart + content.length,
                    estimatedHeightDp = MarkdownBlock.estimateHeight(content, block.type),
                )
            )
            chunkStart += currentSize
            currentChunk.clear()
            currentSize = 0
        }
        currentChunk.appendLine(line)
        currentSize += lineSize
    }

    if (currentChunk.isNotEmpty()) {
        val content = currentChunk.toString().trimEnd('\n')
        result.add(
            MarkdownBlock(
                content = content,
                type = block.type,
                startIndex = chunkStart,
                endIndex = chunkStart + content.length,
                estimatedHeightDp = MarkdownBlock.estimateHeight(content, block.type),
            )
        )
    }

    return result
}
