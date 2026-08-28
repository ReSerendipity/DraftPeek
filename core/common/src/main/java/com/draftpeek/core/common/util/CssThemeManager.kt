/**
 * Markdown预览CSS主题管理模块。
 *
 * 提供可自定义的CSS注入功能，用于WebView-based Markdown渲染，支持无需重新加载页面即可切换主题。
 * 每个主题定义一组CSS变量，注入到WebView中，使相同的HTML模板可以渲染出不同的视觉样式。
 * 提供4种内置主题：github、dark、academic、night。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * CSS主题管理工具对象。
 *
 * 管理Markdown预览的CSS主题，提供CSS变量声明生成和完整样式表生成功能。
 * 使用CSS自定义属性（CSS Variables）实现主题切换，避免重新生成HTML内容。
 */
object CssThemeManager {

    /**
     * Markdown预览可用的CSS主题集合。
     * 每个主题是CSS变量名到值的映射表，定义了背景色、文本色、链接色、代码背景色等。
     */
    val themes = mapOf(
        "github" to mapOf(
            "--bg-color" to "#ffffff",
            "--text-color" to "#24292e",
            "--heading-color" to "#24292e",
            "--link-color" to "#0366d6",
            "--code-bg" to "#f6f8fa",
            "--code-text" to "#e36209",
            "--border-color" to "#e1e4e8",
            "--blockquote-border" to "#6a737d",
            "--blockquote-text" to "#6a737d"
        ),
        "dark" to mapOf(
            "--bg-color" to "#0d1117",
            "--text-color" to "#c9d1d9",
            "--heading-color" to "#c9d1d9",
            "--link-color" to "#58a6ff",
            "--code-bg" to "#161b22",
            "--code-text" to "#79c0ff",
            "--border-color" to "#30363d",
            "--blockquote-border" to "#6e7681",
            "--blockquote-text" to "#8b949e"
        ),
        "academic" to mapOf(
            "--bg-color" to "#fafafa",
            "--text-color" to "#333333",
            "--heading-color" to "#1a1a1a",
            "--link-color" to "#2563eb",
            "--code-bg" to "#f0f0f0",
            "--code-text" to "#c7254e",
            "--border-color" to "#dddddd",
            "--blockquote-border" to "#999999",
            "--blockquote-text" to "#666666"
        ),
        "night" to mapOf(
            "--bg-color" to "#1e1e2e",
            "--text-color" to "#cdd6f4",
            "--heading-color" to "#cdd6f4",
            "--link-color" to "#89b4fa",
            "--code-bg" to "#313244",
            "--code-text" to "#fab387",
            "--border-color" to "#45475a",
            "--blockquote-border" to "#6c7086",
            "--blockquote-text" to "#a6adc8"
        )
    )

    /**
     * 为指定主题生成CSS变量声明。
     *
     * @param themeName 主题名称
     * @return CSS :root选择器下的变量声明字符串，主题不存在时返回空字符串
     */
    fun generateCssVariables(themeName: String): String {
        val theme = themes[themeName] ?: themes["github"] ?: return ""
        return buildString {
            appendLine(":root {")
            theme.forEach { (variable, value) ->
                appendLine("  $variable: $value;")
            }
            appendLine("}")
        }
    }

    /**
     * 为Markdown预览生成完整的CSS样式表。
     * 包含CSS变量声明和所有Markdown元素的样式规则（标题、链接、代码、引用、表格、图片、分割线等）。
     *
     * @param themeName 主题名称
     * @return 完整的CSS样式表字符串
     */
    fun generateStylesheet(themeName: String): String {
        val variables = generateCssVariables(themeName)
        return """
            |$variables
            |
            |body {
            |  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
            |  background-color: var(--bg-color);
            |  color: var(--text-color);
            |  line-height: 1.6;
            |  padding: 20px;
            |  max-width: 800px;
            |  margin: 0 auto;
            |}
            |
            |h1, h2, h3, h4, h5, h6 {
            |  color: var(--heading-color);
            |  margin-top: 24px;
            |  margin-bottom: 16px;
            |  font-weight: 600;
            |  line-height: 1.25;
            |}
            |
            |h1 { font-size: 2em; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
            |h2 { font-size: 1.5em; border-bottom: 1px solid var(--border-color); padding-bottom: 0.3em; }
            |h3 { font-size: 1.25em; }
            |
            |a { color: var(--link-color); text-decoration: none; }
            |a:hover { text-decoration: underline; }
            |
            |code {
            |  background-color: var(--code-bg);
            |  color: var(--code-text);
            |  padding: 0.2em 0.4em;
            |  border-radius: 3px;
            |  font-size: 85%;
            |}
            |
            |pre {
            |  background-color: var(--code-bg);
            |  padding: 16px;
            |  border-radius: 6px;
            |  overflow-x: auto;
            |  line-height: 1.45;
            |}
            |
            |pre code {
            |  background: none;
            |  padding: 0;
            |  font-size: 100%;
            |}
            |
            |blockquote {
            |  border-left: 4px solid var(--blockquote-border);
            |  color: var(--blockquote-text);
            |  margin: 0;
            |  padding: 0 16px;
            |}
            |
            |table {
            |  border-collapse: collapse;
            |  width: 100%;
            |}
            |
            |th, td {
            |  border: 1px solid var(--border-color);
            |  padding: 8px 12px;
            |  text-align: left;
            |}
            |
            |th { font-weight: 600; }
            |
            |img { max-width: 100%; border-radius: 4px; }
            |
            |hr {
            |  border: none;
            |  border-top: 1px solid var(--border-color);
            |  margin: 24px 0;
            |}
        """.trimMargin()
    }
}
