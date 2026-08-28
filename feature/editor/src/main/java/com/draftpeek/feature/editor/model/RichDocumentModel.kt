/**
 * 文件：RichDocumentModel.kt
 * 功能：富文本文档树结构模型与解析/写入器
 * 主要类/接口：RichDocument、RichParagraph、BlockType、RichSpan、RichDocumentParser、RichDocumentWriter
 * 模块依赖：
 *   - androidx.compose.runtime：Compose 不可变注解
 *   - kotlinx.collections.immutable：持久化不可变集合
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 富文档树结构模型，灵感来源于 compose-rich-editor 的
 * RichSpan → RichParagraph → RichTextState 架构。
 *
 * 该模型提供文档结构的轻量级表示，可用于：
 * - 大纲导航（标题、章节）
 * - 格式感知操作（粗体/斜体/代码片段）
 * - Markdown↔树双向转换
 * - 增量更新而无需完整重新解析
 *
 * 与完整的 compose-rich-editor 模型（处理富文本渲染和编辑）不同，
 * 此模型专注于用于分析和导航的结构表示。
 */

/**
 * 文档树根节点。
 *
 * 包含有序的段落列表，是文档结构的入口点。
 *
 * @property paragraphs 不可变段落列表，按文档顺序排列
 */
@Immutable
data class RichDocument(
    val paragraphs: ImmutableList<RichParagraph> = kotlinx.collections.immutable.persistentListOf()
) {
    /** 所有段落的总字符数。 */
    val charCount: Int get() = paragraphs.sumOf { it.charCount }

    /** 总行数（段落数）。 */
    val lineCount: Int get() = paragraphs.size

    /**
     * 按文档顺序提取所有标题。
     *
     * 用于构建文档大纲导航。
     */
    val headings: List<RichSpan.Heading>
        get() = paragraphs.flatMap { p ->
            p.spans.filterIsInstance<RichSpan.Heading>()
        }
}

/**
 * 文档中的段落（块级元素）。
 *
 * 每个段落对应源文档中的一行或一个块级结构（如代码块）。
 *
 * @property spans 不可变行内片段列表
 * @property blockType 块类型分类
 * @property lineNumber 源文档中基于 0 的行索引
 */
@Immutable
data class RichParagraph(
    val spans: ImmutableList<RichSpan> = kotlinx.collections.immutable.persistentListOf(),
    val blockType: BlockType = BlockType.Paragraph,
    val lineNumber: Int = 0
) {
    /** 该段落的总字符数。 */
    val charCount: Int get() = spans.sumOf { it.text.length }

    /** 该段落的纯文本内容（拼接所有片段）。 */
    val text: String get() = spans.joinToString("") { it.text }
}

/**
 * 块级类型分类枚举。
 *
 * 标识段落的 Markdown 块级语义类型。
 */
@Immutable
enum class BlockType {
    /** 普通段落 */
    Paragraph,

    /** 标题块 */
    Heading,

    /** 代码块 */
    CodeBlock,

    /** 引用块 */
    Blockquote,

    /** 列表项 */
    List,

    /** 分隔线 */
    ThematicBreak,

    /** 表格 */
    Table,

    /** HTML 块 */
    HtmlBlock
}

/**
 * 段落内的行内片段密封类。
 *
 * 表示带有格式信息的文本片段，是富文本结构的最小单元。
 *
 * @property text 片段文本内容
 * @property startIndex 在段落文本中的起始索引
 * @property endIndex 在段落文本中的结束索引（不含）
 */
@Immutable
sealed class RichSpan {
    abstract val text: String
    abstract val startIndex: Int
    abstract val endIndex: Int

    /**
     * 纯文本片段（无格式）。
     *
     * @property text 文本内容
     * @property startIndex 起始索引，默认为 0
     * @property endIndex 结束索引，默认为文本长度
     */
    @Immutable
    data class Text(
        override val text: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 标题片段，带级别（1-6）。
     *
     * @property text 标题文本
     * @property level 标题级别（1-6，对应 Markdown 的 # 到 ######）
     * @property startIndex 起始索引
     * @property endIndex 结束索引
     */
    @Immutable
    data class Heading(
        override val text: String,
        val level: Int,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 粗体文本片段。
     */
    @Immutable
    data class Bold(
        override val text: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 斜体文本片段。
     */
    @Immutable
    data class Italic(
        override val text: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 行内代码片段。
     */
    @Immutable
    data class Code(
        override val text: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 链接片段，包含 URL。
     *
     * @property url 链接目标 URL
     */
    @Immutable
    data class Link(
        override val text: String,
        val url: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 图片片段，包含替代文本和 URL。
     *
     * @property url 图片源 URL
     */
    @Immutable
    data class Image(
        override val text: String,
        val url: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()

    /**
     * 删除线文本片段。
     */
    @Immutable
    data class Strikethrough(
        override val text: String,
        override val startIndex: Int = 0,
        override val endIndex: Int = text.length
    ) : RichSpan()
}

/**
 * Markdown 文本到 RichDocument 树的解析器。
 *
 * 使用简单的正则表达式解析标题和常见行内格式。
 * 虽然不是完整的 CommonMark 兼容解析器，但对于大纲导航和格式分析已足够。
 *
 * 解析算法步骤：
 * 1. 逐行扫描源文本
 * 2. 检测代码块边界（```）并整体处理
 * 3. 检测 ATX 风格标题（# 开头）
 * 4. 识别块级类型（引用、列表、分隔线）
 * 5. 对普通段落解析行内格式（粗体、斜体、代码、链接、删除线）
 */
object RichDocumentParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)
    private val codeBlockStartRegex = Regex("^```[\\w]*$", RegexOption.MULTILINE)
    private val codeBlockEndRegex = Regex("^```$", RegexOption.MULTILINE)

    /**
     * 将 Markdown 文本解析为 RichDocument 树。
     *
     * @param content Markdown 源文本
     * @return 包含结构化段落和片段的 RichDocument
     */
    fun parse(content: String): RichDocument {
        if (content.isBlank()) return RichDocument()

        val lines = content.lines()
        val paragraphs = mutableListOf<RichParagraph>()
        var inCodeBlock = false
        var codeBlockBuffer = StringBuilder()
        var codeBlockStartLine = 0

        for ((index, line) in lines.withIndex()) {
            // 步骤1：处理代码块
            if (codeBlockStartRegex.matches(line)) {
                if (!inCodeBlock) {
                    // 进入代码块
                    inCodeBlock = true
                    codeBlockStartLine = index
                    codeBlockBuffer = StringBuilder()
                } else {
                    // 退出代码块，创建 CodeBlock 段落
                    inCodeBlock = false
                    paragraphs.add(
                        RichParagraph(
                            spans = kotlinx.collections.immutable.persistentListOf(
                                RichSpan.Code(
                                    text = codeBlockBuffer.toString().trimEnd(),
                                    startIndex = 0
                                )
                            ),
                            blockType = BlockType.CodeBlock,
                            lineNumber = codeBlockStartLine
                        )
                    )
                }
                continue
            }

            if (inCodeBlock) {
                // 代码块内，累积内容
                if (codeBlockBuffer.isNotEmpty()) codeBlockBuffer.append('\n')
                codeBlockBuffer.append(line)
                continue
            }

            // 步骤2：解析 ATX 标题
            val headingMatch = headingRegex.find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val headingText = headingMatch.groupValues[2].trim()
                paragraphs.add(
                    RichParagraph(
                        spans = kotlinx.collections.immutable.persistentListOf(
                            RichSpan.Heading(
                                text = headingText,
                                level = level,
                                startIndex = headingMatch.range.first
                            )
                        ),
                        blockType = BlockType.Heading,
                        lineNumber = index
                    )
                )
                continue
            }

            // 步骤3：解析普通段落及行内格式
            val spans = parseInlineSpans(line)
            paragraphs.add(
                RichParagraph(
                    spans = spans,
                    blockType = when {
                        line.startsWith("> ") -> BlockType.Blockquote
                        line.startsWith(
                            "- "
                        ) ||
                            line.startsWith("* ") ||
                            Regex("^\\d+\\.\\s").matches(line) -> BlockType.List
                        line.trim() == "---" || line.trim() == "***" -> BlockType.ThematicBreak
                        else -> BlockType.Paragraph
                    },
                    lineNumber = index
                )
            )
        }

        return RichDocument(
            paragraphs = paragraphs.toImmutableList()
        )
    }

    /**
     * 解析单行文本中的行内格式片段。
     *
     * 支持的格式：粗体(****)、斜体(**)、行内代码(``)、链接([]())、删除线(~~~~)
     *
     * @param line 单行文本
     * @return 解析后的不可变片段列表
     */
    private fun parseInlineSpans(line: String): kotlinx.collections.immutable.ImmutableList<RichSpan> {
        val spans = mutableListOf<RichSpan>()
        var pos = 0

        // 行内格式正则：按优先级匹配（粗斜体 > 粗体 > 斜体 > 代码 > 链接 > 删除线）
        val inlinePattern = Regex(
            """(\*\*\*(.+?)\*\*\*)|""" +
                """(\*\*(.+?)\*\*)|""" +
                """(\*(.+?)\*)|""" +
                """(`(.+?)`)|""" +
                """(\[(.+?)\]\((.+?)\))|""" +
                """(~~(.+?)~~)"""
        )

        var lastEnd = 0
        for (match in inlinePattern.findAll(line)) {
            // 添加匹配前的纯文本
            if (match.range.first > lastEnd) {
                val plainText = line.substring(lastEnd, match.range.first)
                if (plainText.isNotEmpty()) {
                    spans.add(RichSpan.Text(text = plainText, startIndex = lastEnd))
                }
            }

            // 根据捕获组判断匹配的格式类型
            when {
                match.groupValues[2].isNotEmpty() -> {
                    spans.add(RichSpan.Bold(text = match.groupValues[2], startIndex = match.range.first))
                }
                match.groupValues[4].isNotEmpty() -> {
                    spans.add(RichSpan.Bold(text = match.groupValues[4], startIndex = match.range.first))
                }
                match.groupValues[6].isNotEmpty() -> {
                    spans.add(RichSpan.Italic(text = match.groupValues[6], startIndex = match.range.first))
                }
                match.groupValues[8].isNotEmpty() -> {
                    spans.add(RichSpan.Code(text = match.groupValues[8], startIndex = match.range.first))
                }
                match.groupValues[10].isNotEmpty() -> {
                    spans.add(
                        RichSpan.Link(
                            text = match.groupValues[10],
                            url = match.groupValues[11],
                            startIndex = match.range.first
                        )
                    )
                }
                match.groupValues[13].isNotEmpty() -> {
                    spans.add(
                        RichSpan.Strikethrough(
                            text = match.groupValues[13],
                            startIndex = match.range.first
                        )
                    )
                }
            }

            lastEnd = match.range.last + 1
        }

        // 添加剩余的纯文本
        if (lastEnd < line.length) {
            val remaining = line.substring(lastEnd)
            if (remaining.isNotEmpty()) {
                spans.add(RichSpan.Text(text = remaining, startIndex = lastEnd))
            }
        }

        // 如果没有任何格式，整行作为纯文本
        if (spans.isEmpty() && line.isNotEmpty()) {
            spans.add(RichSpan.Text(text = line, startIndex = 0))
        }

        return spans.toImmutableList()
    }

    /**
     * 将可变 List 转换为 kotlinx 不可变 ImmutableList。
     */
    private fun <T> List<T>.toImmutableList(): kotlinx.collections.immutable.ImmutableList<T> =
        persistentListOf<T>().addAll(this)
}

/**
 * RichDocument 转换回 Markdown 文本的工具类。
 *
 * 根据块类型和行内片段类型，将文档树序列化为 Markdown 源码。
 */
object RichDocumentWriter {

    /**
     * 将 RichDocument 转换为 Markdown 文本。
     *
     * @param document 富文档树
     * @return Markdown 格式的文本字符串
     */
    fun toMarkdown(document: RichDocument): String = document.paragraphs.joinToString("\n\n") { paragraph ->
        when (paragraph.blockType) {
            BlockType.Heading -> {
                val heading = paragraph.spans.firstOrNull() as? RichSpan.Heading
                if (heading != null) {
                    "${"#".repeat(heading.level)} ${heading.text}"
                } else {
                    paragraph.text
                }
            }
            BlockType.CodeBlock -> {
                "```\n${paragraph.text}\n```"
            }
            BlockType.Blockquote -> {
                "> ${paragraph.spans.joinToString("") { spanToMarkdown(it) }}"
            }
            BlockType.List -> {
                "- ${paragraph.spans.joinToString("") { spanToMarkdown(it) }}"
            }
            else -> {
                paragraph.spans.joinToString("") { spanToMarkdown(it) }
            }
        }
    }

    /**
     * 将单个 RichSpan 转换为对应的 Markdown 标记文本。
     *
     * @param span 行内片段
     * @return Markdown 格式的文本
     */
    private fun spanToMarkdown(span: RichSpan): String = when (span) {
        is RichSpan.Text -> span.text
        is RichSpan.Heading -> "${"#".repeat(span.level)} ${span.text}"
        is RichSpan.Bold -> "**${span.text}**"
        is RichSpan.Italic -> "*${span.text}*"
        is RichSpan.Code -> "`${span.text}`"
        is RichSpan.Link -> "[${span.text}](${span.url})"
        is RichSpan.Image -> "![${span.text}](${span.url})"
        is RichSpan.Strikethrough -> "~~${span.text}~~"
    }
}
