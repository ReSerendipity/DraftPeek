/**
 * Markdown/HTML内容安全清洗模块。
 *
 * SECURITY VULN-004: 使用正则表达式移除危险HTML标签和XSS向量。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * Markdown/HTML内容安全清洗工具对象。
 */
object MarkdownSanitizer {

    private val RE_SCRIPT = "<script[^>]*>.*?</script>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_STYLE = "<style[^>]*>.*?</style>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_IFRAME = "<iframe[^>]*>.*?</iframe>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_OBJECT = "<object[^>]*>.*?</object>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_EMBED = "<embed[^>]*>.*?</embed>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_APPLET = "<applet[^>]*>.*?</applet>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_FORM = "<form[^>]*>.*?</form>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_SVG = "<svg[^>]*>.*?</svg>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_MATH = "<math[^>]*>.*?</math>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    private val RE_SCRIPT_OPEN = "<script[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_STYLE_OPEN = "<style[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_IFRAME_OPEN = "<iframe[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_IFRAME_CLOSE = "</iframe>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_OBJECT_OPEN = "<object[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_OBJECT_CLOSE = "</object>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_EMBED_OPEN = "<embed[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_EMBED_CLOSE = "</embed>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_APPLET_OPEN = "<applet[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_APPLET_CLOSE = "</applet>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_BASE = "<base[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_LINK = "<link[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_META = "<meta[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_INPUT = "<input[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_TEXTAREA = "<textarea[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_SELECT = "<select[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_BUTTON = "<button[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_SVG_OPEN = "<svg[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val RE_MATH_OPEN = "<math[^>]*>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

    private val RE_EVENT_HANDLER_DQ = """\s+on\w+\s*=\s*"[^"]*"""".toRegex(setOf(RegexOption.IGNORE_CASE))
    private val RE_EVENT_HANDLER_SQ = """\s+on\w+\s*=\s*'[^']*'""".toRegex(setOf(RegexOption.IGNORE_CASE))
    private val RE_JAVASCRIPT_URI = "javascript\\s*:".toRegex(setOf(RegexOption.IGNORE_CASE))
    private val RE_DATA_URI = "data\\s*:".toRegex(setOf(RegexOption.IGNORE_CASE))
    private val RE_SRCDOC_DQ = """\s+srcdoc\s*=\s*"[^"]*"""".toRegex(setOf(RegexOption.IGNORE_CASE))
    private val RE_SRCDOC_SQ = """\s+srcdoc\s*=\s*'[^']*'""".toRegex(setOf(RegexOption.IGNORE_CASE))

    /**
     * 清洗Markdown/HTML内容，移除危险HTML标签和XSS向量，同时保留Markdown语法。
     *
     * @param content 待清洗的原始Markdown内容
     * @return 可安全用于WebView渲染的清洗后内容
     */
    fun sanitize(content: String): String {
        return try {
            var cleaned = content
            cleaned = cleaned.replace(RE_SCRIPT, "")
            cleaned = cleaned.replace(RE_STYLE, "")
            cleaned = cleaned.replace(RE_IFRAME, "")
            cleaned = cleaned.replace(RE_OBJECT, "")
            cleaned = cleaned.replace(RE_EMBED, "")
            cleaned = cleaned.replace(RE_APPLET, "")
            cleaned = cleaned.replace(RE_FORM, "")
            cleaned = cleaned.replace(RE_SVG, "")
            cleaned = cleaned.replace(RE_MATH, "")
            cleaned = cleaned.replace(RE_SCRIPT_OPEN, "")
            cleaned = cleaned.replace(RE_STYLE_OPEN, "")
            cleaned = cleaned.replace(RE_IFRAME_OPEN, "")
            cleaned = cleaned.replace(RE_IFRAME_CLOSE, "")
            cleaned = cleaned.replace(RE_OBJECT_OPEN, "")
            cleaned = cleaned.replace(RE_OBJECT_CLOSE, "")
            cleaned = cleaned.replace(RE_EMBED_OPEN, "")
            cleaned = cleaned.replace(RE_EMBED_CLOSE, "")
            cleaned = cleaned.replace(RE_APPLET_OPEN, "")
            cleaned = cleaned.replace(RE_APPLET_CLOSE, "")
            cleaned = cleaned.replace(RE_BASE, "")
            cleaned = cleaned.replace(RE_LINK, "")
            cleaned = cleaned.replace(RE_META, "")
            cleaned = cleaned.replace(RE_INPUT, "")
            cleaned = cleaned.replace(RE_TEXTAREA, "")
            cleaned = cleaned.replace(RE_SELECT, "")
            cleaned = cleaned.replace(RE_BUTTON, "")
            cleaned = cleaned.replace(RE_SVG_OPEN, "")
            cleaned = cleaned.replace(RE_MATH_OPEN, "")
            cleaned = cleaned.replace(RE_EVENT_HANDLER_DQ, "")
            cleaned = cleaned.replace(RE_EVENT_HANDLER_SQ, "")
            cleaned = cleaned.replace(RE_JAVASCRIPT_URI, "")
            cleaned = cleaned.replace(RE_DATA_URI, "")
            cleaned = cleaned.replace(RE_SRCDOC_DQ, "")
            cleaned = cleaned.replace(RE_SRCDOC_SQ, "")
            cleaned
        } catch (_: Exception) {
            content
        }
    }

    /**
     * 清洗用户自定义 CSS 中可能夹带的 XSS 向量。
     *
     * @param css 用户提供的原始 CSS
     * @return 清洗后可安全注入 WebView 的 CSS
     */
    fun sanitizeCss(css: String): String {
        return css
            .replace(RE_SCRIPT, "")
            .replace(RE_IFRAME_OPEN, "")
            .replace(RE_IFRAME_CLOSE, "")
            .replace(RE_OBJECT_OPEN, "")
            .replace(RE_OBJECT_CLOSE, "")
            .replace(RE_EMBED_OPEN, "")
            .replace(RE_EMBED_CLOSE, "")
            .replace(RE_BASE, "")
            .replace(RE_JAVASCRIPT_URI, "")
            .replace(RE_EVENT_HANDLER_DQ, "")
            .replace(RE_EVENT_HANDLER_SQ, "")
            .replace(RE_SRCDOC_DQ, "")
            .replace(RE_SRCDOC_SQ, "")
            .replace(RE_STYLE, "")
            .replace(RE_DATA_URI, "")
    }

    /**
     * 转义特殊字符以便安全嵌入JavaScript字符串字面量。
     */
    fun escapeForJsString(content: String): String {
        return content
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
    }
}
