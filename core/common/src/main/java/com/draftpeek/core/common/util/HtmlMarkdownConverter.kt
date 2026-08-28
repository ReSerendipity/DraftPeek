/**
 * HTML与Markdown转换工具模块。
 *
 * 轻量级HTML ↔ Markdown转换器，处理最常见的HTML元素：标题、粗体/斜体、代码、链接、图片、
 * 列表、引用块、水平分割线、段落、换行等。对于复杂HTML，未来可考虑使用flexmark-java等专用库。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * HTML ↔ Markdown转换工具对象。
 *
 * 提供双向转换功能：htmlToMarkdown使用正则表达式替换处理常见HTML标签，
 * markdownToHtml提供简单的HTML包装（完整渲染应使用WebView-based Markdown预览）。
 * 自动解码常见HTML实体（&amp;、&lt;、&gt;等）。
 */
object HtmlMarkdownConverter {

    /**
     * 将HTML字符串转换为Markdown。
     *
     * 支持的元素：h1-h6标题、strong/b粗体、em/i斜体、code行内代码、pre>code代码块、
     * a链接、img图片、li列表项、blockquote引用、hr水平分割线、p段落、br换行。
     * 剥离剩余标签，解码常见HTML实体，清理过多空行。
     *
     * @param html 输入的HTML字符串
     * @return 转换后的Markdown字符串
     */
    fun htmlToMarkdown(html: String): String {
        var md = html
        md = md.replace(Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL)) { "# ${it.groupValues[1].trim()}" }
        md = md.replace(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL)) { "## ${it.groupValues[1].trim()}" }
        md = md.replace(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL)) { "### ${it.groupValues[1].trim()}" }
        md =
            md.replace(Regex("<h4[^>]*>(.*?)</h4>", RegexOption.DOT_MATCHES_ALL)) { "#### ${it.groupValues[1].trim()}" }
        md =
            md.replace(Regex("<h5[^>]*>(.*?)</h5>", RegexOption.DOT_MATCHES_ALL)) {
                "##### ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h6[^>]*>(.*?)</h6>", RegexOption.DOT_MATCHES_ALL)) {
                "###### ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<(strong|b)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)) {
                "**${it.groupValues[2].trim()}**"
            }
        md = md.replace(Regex("<(em|i)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)) { "*${it.groupValues[2].trim()}*" }
        md = md.replace(Regex("<code>(.*?)</code>")) { "`${it.groupValues[1]}`" }
        md = md.replace(Regex("<pre[^>]*><code[^>]*>(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL)) {
            "```\n${it.groupValues[1].trim()}\n```"
        }
        md = md.replace(Regex("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)) {
            "[${it.groupValues[2]}](${it.groupValues[1]})"
        }
        md = md.replace(Regex("<img[^>]*src=\"([^\"]+)\"[^>]*alt=\"([^\"]*)\"[^>]*/?\\s*>")) {
            "![${it.groupValues[2]}](${it.groupValues[1]})"
        }
        md = md.replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)) { "- ${it.groupValues[1].trim()}" }
        md = md.replace(Regex("<blockquote[^>]*>(.*?)</blockquote>", RegexOption.DOT_MATCHES_ALL)) {
            it.groupValues[1].trim().lines().joinToString("\n") { "> $it" }
        }
        md = md.replace(Regex("<hr\\s*/?>"), "---")
        md = md.replace(Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)) { "${it.groupValues[1].trim()}\n\n" }
        md = md.replace(Regex("<br\\s*/?>"), "  \n")
        md = md.replace(Regex("<[^>]+>"), "")
        md =
            md.replace(
                "&amp;",
                "&"
            ).replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        md = md.replace(Regex("\n{3,}"), "\n\n")
        return md.trim()
    }

    /**
     * 将Markdown字符串转换为HTML。
     *
     * 提供简单包装，转义Markdown中的HTML特殊字符并包装在基本HTML中。
     * 完整渲染应使用WebView-based Markdown预览。
     *
     * @param markdown 输入的Markdown字符串
     * @return 简单的HTML包装字符串
     */
    fun markdownToHtml(markdown: String): String {
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return """<!DOCTYPE html>
            |<html><head><meta charset="UTF-8">
            |<style>body{font-family:sans-serif;padding:16px;line-height:1.6}</style>
            |</head><body>
            |<pre>$escaped</pre>
            |<p><em>Open in DraftPeek for full Markdown rendering</em></p>
            |</body></html>
        """.trimMargin()
    }
}
