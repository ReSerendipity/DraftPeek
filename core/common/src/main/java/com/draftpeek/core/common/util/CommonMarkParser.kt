/**
 * CommonMark Markdown解析工具模块。
 *
 * 使用CommonMark库实现原生Markdown解析，避免依赖WebView + JavaScript。提供以下优势：
 * - 更好的性能（无WebView开销）
 * - 离线能力（无JS依赖）
 * - 更可预测的渲染结果
 * - 通过节点访问者模式更易于自定义
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * CommonMark Markdown解析工具对象。
 *
 * 基于CommonMark Java库实现Markdown解析和HTML渲染，提供标题提取、目录生成、
 * 词数统计、链接提取等辅助功能。使用单例Parser和Renderer实例以提高性能。
 */
object CommonMarkParser {

    @Volatile
    var enabled: Boolean = true

    private val parser: Parser = Parser.builder().build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder().build()

    /**
     * 将Markdown文本转换为HTML。
     *
     * @param markdown 输入的Markdown文本
     * @return 转换后的HTML字符串，当CommonMark解析器被禁用时返回空字符串
     */
    fun toHtml(markdown: String): String {
        if (!enabled) return ""
        val document: Node = parser.parse(markdown)
        return renderer.render(document)
    }

    /**
     * 从Markdown文本中提取所有标题，返回(级别, 文本)对列表。
     * 用于生成文档大纲。
     *
     * @param markdown 输入的Markdown文本
     * @return 标题列表，每个元素为(标题级别1-6, 标题文本)，当解析器禁用时返回空列表
     */
    fun extractHeadings(markdown: String): List<Pair<Int, String>> {
        if (!enabled) return emptyList()
        val headings = mutableListOf<Pair<Int, String>>()
        val document: Node = parser.parse(markdown)

        document.accept(object : org.commonmark.node.AbstractVisitor() {
            override fun visit(heading: org.commonmark.node.Heading) {
                val text = buildString {
                    heading.accept(object : org.commonmark.node.AbstractVisitor() {
                        override fun visit(text: org.commonmark.node.Text) {
                            append(text.literal)
                        }
                    })
                }
                headings.add(heading.level to text)
                visitChildren(heading)
            }
        })

        return headings
    }

    /**
     * 从Markdown文本中提取目录（Table of Contents）。
     * 返回基于标题级别的缩进格式化目录字符串。
     *
     * @param markdown 输入的Markdown文本
     * @return 格式化的目录字符串
     */
    fun extractTableOfContents(markdown: String): String {
        val headings = extractHeadings(markdown)
        return buildString {
            headings.forEach { (level, text) ->
                val indent = "  ".repeat(level - 1)
                appendLine("$indent- $text")
            }
        }
    }

    /**
     * 统计Markdown文本中的单词数（忽略语法标记）。
     *
     * @param markdown 输入的Markdown文本
     * @return 单词数量
     */
    fun countWords(markdown: String): Int {
        val plainText = markdown
            .replace(Regex("#{1,6}\\s+"), "")
            .replace(Regex("\\*\\*|__"), "")
            .replace(Regex("\\*|_"), "")
            .replace(Regex("`{1,3}[^`]*`{1,3}"), "")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .replace(Regex("!\\[([^]]+)]\\([^)]+\\)"), "")
            .replace(Regex("[#\\->|\\[\\](){}]"), "")
            .trim()

        return plainText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }

    /**
     * 统计Markdown文本中的行数。
     *
     * @param markdown 输入的Markdown文本
     * @return 行数
     */
    fun countLines(markdown: String): Int = markdown.lines().size

    /**
     * 从Markdown文本中提取所有链接。
     *
     * @param markdown 输入的Markdown文本
     * @return 链接列表，每个元素为(链接文本, 链接URL)
     */
    fun extractLinks(markdown: String): List<Pair<String, String>> {
        val links = mutableListOf<Pair<String, String>>()
        val regex = Regex("\\[([^]]+)]\\(([^)]+)\\)")
        regex.findAll(markdown).forEach { match ->
            links.add(match.groupValues[1] to match.groupValues[2])
        }
        return links
    }
}
