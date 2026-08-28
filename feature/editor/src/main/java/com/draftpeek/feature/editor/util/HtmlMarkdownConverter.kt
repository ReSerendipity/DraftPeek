/**
 * 文件功能：HTML ↔ Markdown 双向转换器（轻量级实现）
 *
 * 主要对象：
 * - [HtmlMarkdownConverter]：转换器单例对象，处理常见 HTML 元素与 Markdown 的互转
 *
 * 模块依赖：
 * - 无外部依赖，仅使用 Kotlin 标准库正则表达式
 *
 * 支持的 HTML 元素：h1-h6、strong/b、em/i、code、pre>code、a、img、li、blockquote、hr、p、br。
 * 对于复杂 HTML，建议使用 flexmark-java 等专用库。
 */
package com.draftpeek.feature.editor.util

/**
 * HTML ↔ Markdown 双向转换器（单例对象）
 *
 * 轻量级转换器，处理最常见的 HTML 元素。
 * 转换顺序很重要：从最具体的标签（code）到最通用的标签（p）。
 */
object HtmlMarkdownConverter {

    /**
     * 将 HTML 字符串转换为 Markdown
     *
     * 转换顺序：
     * 1. 标题 h1-h6 → #...######
     * 2. 粗体 strong/b → **...**
     * 3. 斜体 em/i → *...*
     * 4. 行内代码 code → `...`
     * 5. 代码块 pre>code → ```...```
     * 6. 链接 a → [text](url)
     * 7. 图片 img → ![alt](src)
     * 8. 列表项 li → - ...
     * 9. 引用 blockquote → > ...
     * 10. 水平分隔线 hr → ---
     * 11. 表格标签：剥离为纯文本行
     * 12. 段落 p → 文本 + 两个换行
     * 13. 换行 br → 两个空格 + 换行
     * 14. 剥离剩余 HTML 标签
     * 15. 解码 HTML 实体
     * 16. 清理过多空行
     *
     * @param html 输入 HTML 字符串
     * @return 转换后的 Markdown 字符串
     */
    fun convertHtmlToMarkdown(html: String): String {
        var md = html

        // 标题
        md =
            md.replace(Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "# ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "## ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h3[^>]*>(.*?)</h3>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "### ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h4[^>]*>(.*?)</h4>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "#### ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h5[^>]*>(.*?)</h5>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "##### ${it.groupValues[1].trim()}"
            }
        md =
            md.replace(Regex("<h6[^>]*>(.*?)</h6>", setOf(RegexOption.DOT_MATCHES_ALL))) {
                "###### ${it.groupValues[1].trim()}"
            }

        // 粗体
        md = md.replace(Regex("<(strong|b)>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "**${it.groupValues[2].trim()}**"
        }

        // 斜体
        md = md.replace(Regex("<(em|i)>(.*?)</\\1>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "*${it.groupValues[2].trim()}*"
        }

        // 行内代码（必须在 pre>code 之前）
        md = md.replace(Regex("<code>(.*?)</code>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "`${it.groupValues[1]}`"
        }

        // 代码块
        md = md.replace(Regex("<pre[^>]*><code[^>]*>(.*?)</code></pre>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "```\n${it.groupValues[1].trim()}\n```"
        }
        // 裸 pre 标签（回退）
        md = md.replace(Regex("<pre[^>]*>(.*?)</pre>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "```\n${it.groupValues[1].trim()}\n```"
        }

        // 链接
        md = md.replace(Regex("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "[${it.groupValues[2]}](${it.groupValues[1]})"
        }

        // 图片（alt 在 src 之前或之后）
        md = md.replace(Regex("<img[^>]*src=\"([^\"]+)\"[^>]*alt=\"([^\"]*)\"[^>]*/?\\s*>")) {
            "![${it.groupValues[2]}](${it.groupValues[1]})"
        }
        md = md.replace(Regex("<img[^>]*alt=\"([^\"]*)\"[^>]*src=\"([^\"]+)\"[^>]*/?\\s*>")) {
            "![${it.groupValues[1]}](${it.groupValues[2]})"
        }

        // 列表项
        md = md.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "- ${it.groupValues[1].trim()}"
        }

        // 引用块
        md = md.replace(Regex("<blockquote[^>]*>(.*?)</blockquote>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            it.groupValues[1].trim().lines().joinToString("\n") { line -> "> $line" }
        }

        // 水平分隔线
        md = md.replace(Regex("<hr\\s*/?>"), "---")

        // 表格行——剥离表格标记为纯文本行
        md = md.replace(Regex("</?t(?:able|head|body|foot|r|h|d)[^>]*>", setOf(RegexOption.DOT_MATCHES_ALL)), "")

        // 段落
        md = md.replace(Regex("<p[^>]*>(.*?)</p>", setOf(RegexOption.DOT_MATCHES_ALL))) {
            "${it.groupValues[1].trim()}\n\n"
        }

        // 换行
        md = md.replace(Regex("<br\\s*/?>"), "  \n")

        // 剥离剩余标签
        md = stripHtmlTags(md)

        // 解码 HTML 实体
        md = unescapeHtml(md)

        // 清理过多空行
        md = md.replace(Regex("\n{3,}"), "\n\n")
        return md.trim()
    }

    /**
     * 将 Markdown 字符串转换为 HTML 文档
     *
     * 转义 Markdown 源中的 HTML 特殊字符并包装在基本 HTML 骨架中。
     * 注意：这是一个简单的转义包装，不是完整渲染。完整渲染应使用基于 WebView 的 Markdown 预览。
     *
     * @param markdown 输入 Markdown 字符串
     * @return 基本 HTML 文档字符串
     */
    fun convertMarkdownToHtml(markdown: String): String {
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<style>body{font-family:sans-serif;padding:16px;line-height:1.6}</style>
</head><body>
<pre>$escaped</pre>
<p><em>在 DraftPeek 中打开以获得完整 Markdown 渲染</em></p>
</body></html>"""
    }

    /**
     * 解码给定文本中的常见 HTML 实体
     *
     * 支持的实体：&amp; &lt; &gt; &quot; &#39; &#x27; &#x2F; &nbsp;
     * 以及任何数字字符引用（&#NNN; 和 &#xHHH;）。
     *
     * @param text 输入文本
     * @return 解码后的文本
     */
    fun unescapeHtml(text: String): String {
        var result = text
        result = result.replace("&amp;", "&")
        result = result.replace("&lt;", "<")
        result = result.replace("&gt;", ">")
        result = result.replace("&quot;", "\"")
        result = result.replace("&#39;", "'")
        result = result.replace("&#x27;", "'")
        result = result.replace("&#x2F;", "/")
        result = result.replace("&nbsp;", " ")
        // 十进制数字实体：&#NNN;
        result = result.replace(Regex("&#(\\d+);")) {
            it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: it.value
        }
        // 十六进制数字实体：&#xHHH;
        result = result.replace(Regex("&#x([0-9a-fA-F]+);")) {
            it.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: it.value
        }
        return result
    }

    /**
     * 从给定字符串中移除所有 HTML 标签，仅保留文本内容
     *
     * @param html 输入 HTML 字符串
     * @return 纯文本字符串
     */
    fun stripHtmlTags(html: String): String = html.replace(Regex("<[^>]+>"), "")
}
