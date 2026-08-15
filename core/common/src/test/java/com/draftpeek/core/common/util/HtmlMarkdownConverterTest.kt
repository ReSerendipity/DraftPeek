package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("HtmlMarkdownConverter")
class HtmlMarkdownConverterTest {

    @Nested
    @DisplayName("htmlToMarkdown()")
    inner class HtmlToMarkdownTest {

        @Test
        @DisplayName("converts h1-h6 headers")
        fun convertsHeaders() {
            assertEquals("# Heading 1", HtmlMarkdownConverter.htmlToMarkdown("<h1>Heading 1</h1>"))
            assertEquals("## Heading 2", HtmlMarkdownConverter.htmlToMarkdown("<h2>Heading 2</h2>"))
            assertEquals("### Heading 3", HtmlMarkdownConverter.htmlToMarkdown("<h3>Heading 3</h3>"))
            assertEquals("#### Heading 4", HtmlMarkdownConverter.htmlToMarkdown("<h4>Heading 4</h4>"))
            assertEquals("##### Heading 5", HtmlMarkdownConverter.htmlToMarkdown("<h5>Heading 5</h5>"))
            assertEquals("###### Heading 6", HtmlMarkdownConverter.htmlToMarkdown("<h6>Heading 6</h6>"))
        }

        @Test
        @DisplayName("converts bold tags")
        fun convertsBold() {
            assertEquals("**bold text**", HtmlMarkdownConverter.htmlToMarkdown("<strong>bold text</strong>"))
            assertEquals("**bold text**", HtmlMarkdownConverter.htmlToMarkdown("<b>bold text</b>"))
        }

        @Test
        @DisplayName("converts italic tags")
        fun convertsItalic() {
            assertEquals("*italic text*", HtmlMarkdownConverter.htmlToMarkdown("<em>italic text</em>"))
            assertEquals("*italic text*", HtmlMarkdownConverter.htmlToMarkdown("<i>italic text</i>"))
        }

        @Test
        @DisplayName("converts inline code")
        fun convertsInlineCode() {
            assertEquals("`code`", HtmlMarkdownConverter.htmlToMarkdown("<code>code</code>"))
        }

        @Test
        @DisplayName("converts pre>code blocks")
        fun convertsCodeBlock() {
            val html = "<pre><code>val x = 1\nval y = 2</code></pre>"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertTrue(result.startsWith("```"))
            assertTrue(result.contains("val x = 1"))
            assertTrue(result.endsWith("```"))
        }

        @Test
        @DisplayName("converts anchor tags to links")
        fun convertsLinks() {
            val html = """<a href="https://example.com">Example</a>"""
            assertEquals("[Example](https://example.com)", HtmlMarkdownConverter.htmlToMarkdown(html))
        }

        @Test
        @DisplayName("converts img tags")
        fun convertsImages() {
            val html = """<img src="image.png" alt="An image"/>"""
            assertEquals("![An image](image.png)", HtmlMarkdownConverter.htmlToMarkdown(html))
        }

        @Test
        @DisplayName("converts list items")
        fun convertsListItems() {
            val html = "<li>Item 1</li><li>Item 2</li>"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertTrue(result.contains("- Item 1"))
            assertTrue(result.contains("- Item 2"))
        }

        @Test
        @DisplayName("converts blockquotes")
        fun convertsBlockquotes() {
            val html = "<blockquote>Quoted text</blockquote>"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertTrue(result.contains("> Quoted text"))
        }

        @Test
        @DisplayName("converts horizontal rules")
        fun convertsHorizontalRules() {
            assertEquals("---", HtmlMarkdownConverter.htmlToMarkdown("<hr/>"))
            assertEquals("---", HtmlMarkdownConverter.htmlToMarkdown("<hr>"))
        }

        @Test
        @DisplayName("converts paragraphs with spacing")
        fun convertsParagraphs() {
            val html = "<p>First</p><p>Second</p>"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertTrue(result.contains("First"))
            assertTrue(result.contains("Second"))
            assertTrue(result.contains("\n\n"))
        }

        @Test
        @DisplayName("converts line breaks")
        fun convertsLineBreaks() {
            val html = "Line 1<br/>Line 2"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertTrue(result.contains("  \n"))
        }

        @Test
        @DisplayName("strips unknown tags")
        fun stripsUnknownTags() {
            val html = "<div><span>Text</span></div>"
            assertEquals("Text", HtmlMarkdownConverter.htmlToMarkdown(html))
        }

        @Test
        @DisplayName("decodes HTML entities")
        fun decodesEntities() {
            val html = "&amp; &lt; &gt; &quot; &#39;"
            assertEquals("& < > \" '", HtmlMarkdownConverter.htmlToMarkdown(html))
        }

        @Test
        @DisplayName("cleans up excessive blank lines")
        fun cleansUpBlankLines() {
            val html = "<p>A</p><p>B</p><p>C</p>"
            val result = HtmlMarkdownConverter.htmlToMarkdown(html)
            assertFalse(result.contains("\n\n\n"))
        }
    }

    @Nested
    @DisplayName("markdownToHtml()")
    inner class MarkdownToHtmlTest {

        @Test
        @DisplayName("wraps markdown in HTML structure")
        fun wrapsInHtml() {
            val md = "# Hello"
            val result = HtmlMarkdownConverter.markdownToHtml(md)
            assertTrue(result.startsWith("<!DOCTYPE html>"))
            assertTrue(result.contains("<html>"))
            assertTrue(result.contains("</html>"))
            assertTrue(result.contains("<pre>"))
            assertTrue(result.contains("</pre>"))
        }

        @Test
        @DisplayName("escapes HTML special characters in markdown")
        fun escapesSpecialChars() {
            val md = "if (x < 0 && y > 0)"
            val result = HtmlMarkdownConverter.markdownToHtml(md)
            assertTrue(result.contains("&lt;"))
            assertTrue(result.contains("&gt;"))
            assertTrue(result.contains("&amp;"))
            assertFalse(result.contains("if (x < 0 && y > 0)"))
        }

        @Test
        @DisplayName("includes rendering hint")
        fun includesRenderingHint() {
            val result = HtmlMarkdownConverter.markdownToHtml("test")
            assertTrue(result.contains("Open in DraftPeek"))
        }
    }
}
