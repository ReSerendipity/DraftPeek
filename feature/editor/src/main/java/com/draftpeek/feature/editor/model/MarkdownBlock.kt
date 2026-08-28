/**
 * Markdown 分块模型，用于流式渲染。
 *
 * 将大 Markdown 文件按结构化块（标题、段落、代码块、列表等）切分，
 * 使 LazyColumn 可以只渲染可见区域内的块，实现虚拟滚动。
 *
 * @author DraftPeek Team
 * @since 1.1.0
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Markdown 块类型枚举。
 *
 * 每种类型对应不同的渲染方式和高度估算。
 */
enum class MarkdownBlockType {
    /** 标题（H1-H6） */
    HEADING,

    /** 段落 */
    PARAGRAPH,

    /** 代码块（``` 或 ~~~） */
    CODE_BLOCK,

    /** 无序列表 */
    UNORDERED_LIST,

    /** 有序列表 */
    ORDERED_LIST,

    /** 任务列表 */
    TASK_LIST,

    /** 引用块 */
    BLOCKQUOTE,

    /** 表格 */
    TABLE,

    /** 水平分隔线 */
    HORIZONTAL_RULE,

    /** 空行（用于块间分隔） */
    BLANK
}

/**
 * Markdown 块数据模型。
 *
 * 表示 Markdown 内容中一个独立的语义块，包含原始文本、类型和高度估算。
 * 在流式渲染中，只有可见区域附近的块会被实际渲染，其余使用占位符。
 *
 * @property content 块的原始 Markdown 文本
 * @property type 块类型
 * @property startIndex 块在原始文档中的起始字符索引
 * @property endIndex 块在原始文档中的结束字符索引
 * @property estimatedHeightDp 估算的渲染高度（dp），用于占位符
 */
@Immutable
data class MarkdownBlock(
    val content: String,
    val type: MarkdownBlockType,
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    val estimatedHeightDp: Dp = 0.dp
) {
    companion object {
        /**
         * 根据块类型和内容行数估算渲染高度。
         *
         * @param content 块内容
         * @param type 块类型
         * @return 估算高度（dp）
         */
        fun estimateHeight(content: String, type: MarkdownBlockType): Dp {
            val lineCount = content.count { it == '\n' } + 1
            val baseHeight = when (type) {
                MarkdownBlockType.HEADING -> {
                    // 标题高度取决于级别（通过 # 数量判断）
                    val level = content.takeWhile { it == '#' }.length.coerceAtMost(6)
                    when (level) {
                        1 -> 48f
                        2 -> 40f
                        3 -> 34f
                        4 -> 30f
                        5 -> 28f
                        else -> 26f
                    }
                }
                MarkdownBlockType.CODE_BLOCK -> {
                    // 代码块：每行 20dp + 16dp padding
                    (lineCount * 20f + 16f)
                }
                MarkdownBlockType.PARAGRAPH -> {
                    // 段落：每行约 24dp + 12dp margin
                    val charsPerLine = 40f
                    val wrappedLines = (content.length / charsPerLine).toInt().coerceAtLeast(lineCount)
                    (wrappedLines * 24f + 12f)
                }
                MarkdownBlockType.UNORDERED_LIST, MarkdownBlockType.ORDERED_LIST, MarkdownBlockType.TASK_LIST -> {
                    // 列表项：每行约 26dp
                    (lineCount * 26f + 8f)
                }
                MarkdownBlockType.BLOCKQUOTE -> {
                    (lineCount * 24f + 12f)
                }
                MarkdownBlockType.TABLE -> {
                    // 表格：行数 * 32dp + header
                    (lineCount * 32f + 8f)
                }
                MarkdownBlockType.HORIZONTAL_RULE -> 16f
                MarkdownBlockType.BLANK -> 8f
            }
            return baseHeight.dp
        }
    }
}
