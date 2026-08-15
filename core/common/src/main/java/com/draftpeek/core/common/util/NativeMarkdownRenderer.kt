/**
 * 原生Markdown渲染器模块。
 *
 * 使用CommonMark库实现原生Markdown渲染，避免依赖WebView + JavaScript。支持以下GFM扩展：
 * - 表格（Tables）
 * - 删除线（Strikethrough）
 * - 任务列表（Task List Items）
 * - 自动链接（Autolink）
 *
 * 提供更好的性能、离线能力、更可预测的渲染结果，以及通过节点访问者模式更易于自定义。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.ext.gfm.tables.TablesExtension

/**
 * 原生Markdown渲染器对象。
 *
 * 基于CommonMark库（带GFM扩展）实现Markdown到HTML的渲染，支持明暗主题CSS样式。
 * 委托CommonMarkParser提供标题提取、目录生成、词数统计、链接提取等辅助功能。
 */
object NativeMarkdownRenderer {

    private val extensions: List<Extension> = listOf(
        StrikethroughExtension.create(),
        TablesExtension.create(),
        AutolinkExtension.create(),
        TaskListItemsExtension.create(),
    )

    private val parser: Parser = Parser.builder()
        .extensions(extensions)
        .build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        .build()

    /**
     * 将Markdown文本渲染为HTML。
     *
     * @param markdown 要渲染的Markdown文本
     * @return HTML字符串
     */
    fun renderToHtml(markdown: String): String {
        val document: Node = parser.parse(markdown)
        return renderer.render(document)
    }

    /**
     * 将Markdown文本渲染为带主题样式的HTML。
     *
     * @param markdown 要渲染的Markdown文本
     * @param isDarkTheme 是否使用深色主题样式
     * @return 嵌入CSS的完整HTML字符串
     */
    fun renderToHtmlWithTheme(markdown: String, isDarkTheme: Boolean = false): String {
        val htmlContent = renderToHtml(markdown)
        val css = if (isDarkTheme) DARK_THEME_CSS else LIGHT_THEME_CSS

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    $css
                </style>
            </head>
            <body>
                <div class="markdown-body">
                    $htmlContent
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * 从Markdown文本中提取标题，返回(级别, 文本)对列表。
     * 用于生成文档大纲。
     *
     * @param markdown 要解析的Markdown文本
     * @return (级别, 文本)对列表
     */
    fun extractHeadings(markdown: String): List<Pair<Int, String>> {
        return CommonMarkParser.extractHeadings(markdown)
    }

    /**
     * 从Markdown文本中提取目录。
     *
     * @param markdown 要解析的Markdown文本
     * @return 格式化的目录字符串
     */
    fun extractTableOfContents(markdown: String): String {
        return CommonMarkParser.extractTableOfContents(markdown)
    }

    /**
     * 统计Markdown文本中的单词数（忽略语法标记）。
     *
     * @param markdown 要分析的Markdown文本
     * @return 单词数量
     */
    fun countWords(markdown: String): Int {
        return CommonMarkParser.countWords(markdown)
    }

    /**
     * 统计Markdown文本中的行数。
     *
     * @param markdown 要分析的Markdown文本
     * @return 行数
     */
    fun countLines(markdown: String): Int {
        return CommonMarkParser.countLines(markdown)
    }

    /**
     * 从Markdown文本中提取所有链接。
     *
     * @param markdown 要解析的Markdown文本
     * @return (文本, URL)对列表
     */
    fun extractLinks(markdown: String): List<Pair<String, String>> {
        return CommonMarkParser.extractLinks(markdown)
    }

    private const val LIGHT_THEME_CSS = """
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            background: #fff;
            padding: 16px;
            max-width: 800px;
            margin: 0 auto;
        }
        h1, h2, h3, h4, h5, h6 {
            margin-top: 24px;
            margin-bottom: 16px;
            font-weight: 600;
            line-height: 1.25;
        }
        h1 { font-size: 2em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        h2 { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        h3 { font-size: 1.25em; }
        h4 { font-size: 1em; }
        h5 { font-size: 0.875em; }
        h6 { font-size: 0.85em; color: #6a737d; }
        p { margin-top: 0; margin-bottom: 16px; }
        a { color: #0366d6; text-decoration: none; }
        a:hover { text-decoration: underline; }
        img { max-width: 100%; height: auto; border-radius: 4px; }
        blockquote {
            margin: 0 0 16px 0;
            padding: 0 1em;
            color: #6a737d;
            border-left: 0.25em solid #dfe2e5;
        }
        pre {
            background: #f6f8fa;
            border-radius: 6px;
            padding: 16px;
            overflow: auto;
            font-size: 85%;
            line-height: 1.45;
            background-color: #f6f8fa;
            border-radius: 6px;
        }
        code {
            font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
            font-size: 85%;
            background-color: rgba(27,31,35,0.05);
            padding: 0.2em 0.4em;
            border-radius: 3px;
        }
        pre code {
            background-color: transparent;
            padding: 0;
            border-radius: 0;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            margin-bottom: 16px;
        }
        table, th, td {
            border: 1px solid #dfe2e5;
        }
        th, td {
            padding: 6px 13px;
        }
        tr:nth-child(2n) {
            background-color: #f6f8fa;
        }
        hr {
            height: 0.25em;
            padding: 0;
            margin: 24px 0;
            background-color: #e1e4e8;
            border: 0;
        }
        ul, ol {
            padding-left: 2em;
            margin-top: 0;
            margin-bottom: 16px;
        }
        li + li {
            margin-top: 0.25em;
        }
        .task-list-item {
            list-style-type: none;
            margin-left: -1.5em;
        }
        .task-list-item input[type="checkbox"] {
            margin-right: 0.5em;
        }
    """

    private const val DARK_THEME_CSS = """
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #c9d1d9;
            background: #0d1117;
            padding: 16px;
            max-width: 800px;
            margin: 0 auto;
        }
        h1, h2, h3, h4, h5, h6 {
            margin-top: 24px;
            margin-bottom: 16px;
            font-weight: 600;
            line-height: 1.25;
            color: #f0f6fc;
        }
        h1 { font-size: 2em; border-bottom: 1px solid #21262d; padding-bottom: 0.3em; }
        h2 { font-size: 1.5em; border-bottom: 1px solid #21262d; padding-bottom: 0.3em; }
        h3 { font-size: 1.25em; }
        h4 { font-size: 1em; }
        h5 { font-size: 0.875em; }
        h6 { font-size: 0.85em; color: #8b949e; }
        p { margin-top: 0; margin-bottom: 16px; }
        a { color: #58a6ff; text-decoration: none; }
        a:hover { text-decoration: underline; }
        img { max-width: 100%; height: auto; border-radius: 4px; }
        blockquote {
            margin: 0 0 16px 0;
            padding: 0 1em;
            color: #8b949e;
            border-left: 0.25em solid #30363d;
        }
        pre {
            background: #161b22;
            border-radius: 6px;
            padding: 16px;
            overflow: auto;
            font-size: 85%;
            line-height: 1.45;
            border-radius: 6px;
        }
        code {
            font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
            font-size: 85%;
            background-color: rgba(110,118,129,0.4);
            padding: 0.2em 0.4em;
            border-radius: 3px;
        }
        pre code {
            background-color: transparent;
            padding: 0;
            border-radius: 0;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            margin-bottom: 16px;
        }
        table, th, td {
            border: 1px solid #30363d;
        }
        th, td {
            padding: 6px 13px;
        }
        tr:nth-child(2n) {
            background-color: #161b22;
        }
        hr {
            height: 0.25em;
            padding: 0;
            margin: 24px 0;
            background-color: #30363d;
            border: 0;
        }
        ul, ol {
            padding-left: 2em;
            margin-top: 0;
            margin-bottom: 16px;
        }
        li + li {
            margin-top: 0.25em;
        }
        .task-list-item {
            list-style-type: none;
            margin-left: -1.5em;
        }
        .task-list-item input[type="checkbox"] {
            margin-right: 0.5em;
        }
    """
}
