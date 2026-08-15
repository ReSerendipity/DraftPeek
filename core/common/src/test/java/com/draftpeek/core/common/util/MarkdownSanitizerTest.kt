package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MarkdownSanitizer")
class MarkdownSanitizerTest {

    @Nested
    @DisplayName("sanitize()")
    inner class SanitizeTest {

        @Test
        @DisplayName("removes <script> tags with content")
        fun removesScriptTags() {
            val input = "<p>hello</p><script>alert('xss')</script><p>world</p>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("<script"), "Script tag should be removed")
            assertFalse(result.contains("alert"), "Script content should be removed")
            assertTrue(result.contains("hello"))
            assertTrue(result.contains("world"))
        }

        @Test
        @DisplayName("removes <script> tags spanning multiple lines")
        fun removesMultilineScriptTags() {
            val input = "<script>\nfunction evil() {\n  alert('xss');\n}\n</script>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("<script"))
            assertFalse(result.contains("evil"))
        }

        @Test
        @DisplayName("removes <iframe> tags")
        fun removesIframeTags() {
            val input = "<iframe src=\"evil.com\"></iframe>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("iframe"))
        }

        @Test
        @DisplayName("removes <object> and <embed> tags")
        fun removesObjectAndEmbedTags() {
            val input = "<object data=\"evil.swf\"></object><embed src=\"evil.swf\"></embed>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("object"))
            assertFalse(result.contains("embed"))
        }

        @Test
        @DisplayName("removes <svg> tags entirely (more secure than preserving)")
        fun removesSvgTags() {
            val input = "<svg onload=\"alert('xss')\"><circle/></svg>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("<svg"), "SVG tag should be removed entirely")
            assertFalse(result.contains("onload"), "onload handler should be removed")
        }

        @Test
        @DisplayName("removes safe SVG tags too (regex strips all dangerous tags)")
        fun removesSafeSvg() {
            val input = "<svg width=\"100\" height=\"100\"><circle cx=\"50\" cy=\"50\" r=\"40\"/></svg>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("<svg"), "SVG tags are removed by dangerous tag regex")
        }

        @Test
        @DisplayName("removes javascript: URI scheme")
        fun removesJavascriptUri() {
            val input = "<a href=\"javascript:alert('xss')\">click</a>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("javascript:"))
        }

        @Test
        @DisplayName("removes data: URI scheme")
        fun removesDataUri() {
            val input = "<img src=\"data:text/html,<script>alert(1)</script>\">"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("data:"))
        }

        @Test
        @DisplayName("removes on* event handlers with double quotes")
        fun removesOnEventHandlersDoubleQuote() {
            val input = "<div onclick=\"alert('xss')\" class=\"test\">content</div>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("onclick"))
        }

        @Test
        @DisplayName("removes on* event handlers with single quotes")
        fun removesOnEventHandlersSingleQuote() {
            val input = "<div onclick='alert(\"xss\")' class=\"test\">content</div>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("onclick"))
        }

        @Test
        @DisplayName("removes <style> tags with content")
        fun removesStyleTags() {
            val input = "<style>body { background: red; }</style>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("style"))
            assertFalse(result.contains("background"))
        }

        @Test
        @DisplayName("removes <base>, <link>, <meta>, <applet>, <form> tags")
        fun removesDangerousTags() {
            val input = "<base href=\"evil.com/\"><link rel=\"stylesheet\" href=\"evil.css\">" +
                "<meta http-equiv=\"refresh\" content=\"0;url=evil.com\"><applet code=\"evil.class\"></applet>" +
                "<form action=\"evil.com\"><button>submit</button></form>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("base"))
            assertFalse(result.contains("link"))
            assertFalse(result.contains("meta"))
            assertFalse(result.contains("applet"))
            assertFalse(result.contains("form"))
        }

        @Test
        @DisplayName("preserves HTML hex entity encodings (not decoded by regex)")
        fun preservesHexEntityEncodings() {
            val input = "javascript&#x3a;alert(1)"
            val result = MarkdownSanitizer.sanitize(input)
            assertTrue(result.contains("&#x"), "Hex entity encodings are not decoded by regex approach")
        }

        @Test
        @DisplayName("preserves HTML decimal entity encodings (not decoded by regex)")
        fun preservesDecimalEntityEncodings() {
            val input = "javascript&#58;alert(1)"
            val result = MarkdownSanitizer.sanitize(input)
            assertTrue(result.contains("&#58"), "Decimal entity encodings are not decoded by regex approach")
        }

        @Test
        @DisplayName("preserves CSS url() calls (only sanitizeCss handles these)")
        fun preservesCssUrlCalls() {
            val input = "background: url(evil.png)"
            val result = MarkdownSanitizer.sanitize(input)
            assertTrue(result.contains("url("), "CSS url() is not sanitized by sanitize()")
        }

        @Test
        @DisplayName("removes srcdoc attributes")
        fun removesSrcdocAttributes() {
            val input = "<iframe srcdoc=\"<script>alert(1)</script>\"></iframe>"
            val result = MarkdownSanitizer.sanitize(input)
            assertFalse(result.contains("srcdoc"))
        }

        @Test
        @DisplayName("preserves safe content")
        fun preservesSafeContent() {
            val input = "<h1>Title</h1><p>This is <strong>safe</strong> content.</p>"
            val result = MarkdownSanitizer.sanitize(input)
            assertEquals(input, result)
        }

        @Test
        @DisplayName("handles empty string")
        fun handlesEmptyString() {
            assertEquals("", MarkdownSanitizer.sanitize(""))
        }

        @Test
        @DisplayName("handles plain text without HTML")
        fun handlesPlainText() {
            val input = "Just some plain text without any HTML."
            assertEquals(input, MarkdownSanitizer.sanitize(input))
        }
    }

    @Nested
    @DisplayName("escapeForJsString()")
    inner class EscapeForJsStringTest {

        @Test
        @DisplayName("escapes backslashes")
        fun escapesBackslashes() {
            val input = "path\\to\\file"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("path\\\\to\\\\file", result)
        }

        @Test
        @DisplayName("escapes backticks")
        fun escapesBackticks() {
            val input = "code `inline`"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("code \\`inline\\`", result)
        }

        @Test
        @DisplayName("escapes single quotes")
        fun escapesSingleQuotes() {
            val input = "it's a test"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("it\\'s a test", result)
        }

        @Test
        @DisplayName("escapes double quotes")
        fun escapesDoubleQuotes() {
            val input = "say \"hello\""
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("say \\\"hello\\\"", result)
        }

        @Test
        @DisplayName("escapes newlines")
        fun escapesNewlines() {
            val input = "line1\nline2"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("line1\\nline2", result)
        }

        @Test
        @DisplayName("escapes carriage returns")
        fun escapesCarriageReturns() {
            val input = "line1\rline2"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("line1\\rline2", result)
        }

        @Test
        @DisplayName("escapes dollar signs")
        fun escapesDollarSigns() {
            val input = "cost: \$100"
            val result = MarkdownSanitizer.escapeForJsString(input)
            assertEquals("cost: \\$100", result)
        }

        @Test
        @DisplayName("handles empty string")
        fun handlesEmptyString() {
            assertEquals("", MarkdownSanitizer.escapeForJsString(""))
        }

        @Test
        @DisplayName("handles string with no special characters")
        fun handlesNoSpecialChars() {
            val input = "plain text 123"
            assertEquals(input, MarkdownSanitizer.escapeForJsString(input))
        }
    }

    @Nested
    @DisplayName("sanitizeCss()")
    inner class SanitizeCssTest {

        @Test
        @DisplayName("preserves legitimate url() unlike sanitize()")
        fun preservesUrl() {
            val css = "body { background-image: url(bg.png); }"
            val result = MarkdownSanitizer.sanitizeCss(css)
            assertTrue(result.contains("url(bg.png)"), "CSS url() must be preserved for legitimate styling")
        }

        @Test
        @DisplayName("removes <script> and <style> tags")
        fun removesScriptAndStyle() {
            val css = ".a{}<script>alert(1)</script><style>x</style>.b{}"
            val result = MarkdownSanitizer.sanitizeCss(css)
            assertFalse(result.contains("<script"))
            assertFalse(result.contains("alert"))
            assertFalse(result.contains("<style"))
        }

        @Test
        @DisplayName("removes javascript: and data: URIs")
        fun removesDangerousUris() {
            val css = "a{background:url(javascript:evil())}b{c:data:text/html}"
            val result = MarkdownSanitizer.sanitizeCss(css)
            assertFalse(result.contains("javascript:"))
            assertFalse(result.contains("data:"))
        }

        @Test
        @DisplayName("removes on* event handlers and srcdoc attributes")
        fun removesEventHandlers() {
            val css = "<div onload=\"evil()\" srcdoc='x'>"
            val result = MarkdownSanitizer.sanitizeCss(css)
            assertFalse(result.contains("onload"))
            assertFalse(result.contains("srcdoc"))
        }

        @Test
        @DisplayName("removes iframe/object/embed/base tags")
        fun removesEmbedTags() {
            val css = "<iframe></iframe><object></object><embed></embed><base>"
            val result = MarkdownSanitizer.sanitizeCss(css)
            assertFalse(result.contains("<iframe"))
            assertFalse(result.contains("<object"))
            assertFalse(result.contains("<embed"))
            assertFalse(result.contains("<base"))
        }

        @Test
        @DisplayName("leaves clean CSS unchanged")
        fun leavesCleanCssUnchanged() {
            val css = "h1 { color: #333; margin: 0 auto; font-size: 14px; }"
            assertEquals(css, MarkdownSanitizer.sanitizeCss(css))
        }

        @Test
        @DisplayName("handles empty string")
        fun handlesEmpty() {
            assertEquals("", MarkdownSanitizer.sanitizeCss(""))
        }
    }
}
