/**
 * Markdown 内容分块器。
 *
 * 将大 Markdown 文件按语义边界（标题、代码块、空行等）切分为 [MarkdownBlock] 列表，
 * 供 LazyColumn 流式渲染使用。分块策略：
 *
 * - 代码块（``` 或 ~~~）作为整体块，不拆分
 * - 标题行作为独立块
 * - 连续的非空行组成段落块
 * - 空行作为块边界
 * - 列表、引用、表格等保持完整性
 *
 * @author DraftPeek Team
 * @since 1.1.0
 */
package com.draftpeek.feature.editor.util

import com.draftpeek.feature.editor.model.MarkdownBlockType
import com.draftpeek.feature.editor.model.MarkdownBlock

/**
 * 将 Markdown 文本切分为语义块列表。
 *
 * 算法步骤：
 * 1. 按行遍历内容
 * 2. 检测代码块开始/结束（``` 或 ~~~），代码块内不拆分
 * 3. 空行作为块边界，将累积的内容创建为块
 * 4. 标题行（# 开头）作为独立块
 * 5. 列表项、引用块等连续行组成块
 *
 * @param content Markdown 文本内容
 * @return 分块列表
 */
fun chunkMarkdown(content: String): List<MarkdownBlock> {
    if (content.isEmpty()) return emptyList()

    val blocks = mutableListOf<MarkdownBlock>()
    val lines = content.split('\n')
    var currentBlock = StringBuilder()
    var blockStartIndex = 0
    var currentType = MarkdownBlockType.PARAGRAPH
    var inCodeBlock = false
    var codeBlockMarker = ""
    var charIndex = 0

    fun flushBlock() {
        if (currentBlock.isNotEmpty()) {
            val blockContent = currentBlock.toString().trimEnd('\n')
            if (blockContent.isNotEmpty()) {
                val type = if (inCodeBlock) MarkdownBlockType.CODE_BLOCK else determineMarkdownBlockType(blockContent)
                blocks.add(
                    MarkdownBlock(
                        content = blockContent,
                        type = type,
                        startIndex = blockStartIndex,
                        endIndex = blockStartIndex + blockContent.length,
                        estimatedHeightDp = MarkdownBlock.estimateHeight(blockContent, type),
                    )
                )
            }
            currentBlock.clear()
        }
    }

    for ((lineIdx, line) in lines.withIndex()) {
        val lineStart = charIndex
        val trimmed = line.trim()

        // Detect code block boundaries
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            val marker = trimmed.take(3)
            if (!inCodeBlock) {
                // Start of code block - flush current block first
                flushBlock()
                inCodeBlock = true
                codeBlockMarker = marker
                blockStartIndex = lineStart
                currentBlock.appendLine(line)
            } else if (marker == codeBlockMarker) {
                // End of code block
                currentBlock.appendLine(line)
                flushBlock()
                inCodeBlock = false
                codeBlockMarker = ""
            } else {
                // Different marker inside code block - keep as content
                currentBlock.appendLine(line)
            }
        } else if (inCodeBlock) {
            // Inside code block - collect all lines
            currentBlock.appendLine(line)
        } else if (trimmed.isEmpty()) {
            // Blank line - block boundary
            flushBlock()
            currentType = MarkdownBlockType.PARAGRAPH
        } else if (trimmed.startsWith("#")) {
            // Heading - flush current block, heading is its own block
            flushBlock()
            blockStartIndex = lineStart
            currentBlock.appendLine(line)
            flushBlock()
        } else if (trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___")) {
            // Horizontal rule
            flushBlock()
            blockStartIndex = lineStart
            currentBlock.appendLine(line)
            flushBlock()
        } else {
            // Regular content line
            if (currentBlock.isEmpty()) {
                blockStartIndex = lineStart
                currentType = determineMarkdownBlockType(trimmed)
            }
            currentBlock.appendLine(line)
        }

        charIndex = lineStart + line.length + 1 // +1 for the newline
    }

    // Flush remaining content
    flushBlock()

    return blocks
}

/**
 * 根据内容的首行判断块类型。
 *
 * @param content 块内容（已去除尾部空行）
 * @return 块类型
 */
private fun determineMarkdownBlockType(content: String): MarkdownBlockType {
    val firstLine = content.lineSequence().firstOrNull()?.trim() ?: return MarkdownBlockType.PARAGRAPH
    return when {
        firstLine.startsWith("#") -> MarkdownBlockType.HEADING
        firstLine.startsWith("```") || firstLine.startsWith("~~~") -> MarkdownBlockType.CODE_BLOCK
        firstLine.startsWith("- [ ]") || firstLine.startsWith("- [x]") ||
            firstLine.startsWith("- [X]") || firstLine.startsWith("* [ ]") ||
            firstLine.startsWith("* [x]") || firstLine.startsWith("+ [ ]") ||
            firstLine.startsWith("+ [x]") -> MarkdownBlockType.TASK_LIST
        firstLine.startsWith("- ") || firstLine.startsWith("* ") || firstLine.startsWith("+ ") -> MarkdownBlockType.UNORDERED_LIST
        Regex("^\\d+\\.\\s").containsMatchIn(firstLine) -> MarkdownBlockType.ORDERED_LIST
        firstLine.startsWith(">") -> MarkdownBlockType.BLOCKQUOTE
        firstLine.startsWith("|") -> MarkdownBlockType.TABLE
        firstLine.startsWith("---") || firstLine.startsWith("***") || firstLine.startsWith("___") -> MarkdownBlockType.HORIZONTAL_RULE
        else -> MarkdownBlockType.PARAGRAPH
    }
}
