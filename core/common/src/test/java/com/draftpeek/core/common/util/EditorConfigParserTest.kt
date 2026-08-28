package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EditorConfigParser")
class EditorConfigParserTest {

    @Nested
    @DisplayName("parse() — root-only config")
    inner class RootOnlyConfig {

        @Test
        @DisplayName("parses basic root properties")
        fun parsesBasicRootProperties() {
            val content = """
                root = true

                indent_style = space
                indent_size = 4
                end_of_line = lf
                charset = utf-8
                trim_trailing_whitespace = true
                insert_final_newline = true
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "example.kt")

            assertEquals("space", result.indentStyle)
            assertEquals(4, result.indentSize)
            assertEquals("lf", result.endOfLine)
            assertEquals("utf-8", result.charset)
            assertTrue(result.trimTrailingWhitespace!!)
            assertTrue(result.insertFinalNewline!!)
        }

        @Test
        @DisplayName("returns all nulls for empty config")
        fun returnsAllNullsForEmptyConfig() {
            val result = EditorConfigParser.parse("", "any.txt")

            assertNull(result.indentStyle)
            assertNull(result.indentSize)
            assertNull(result.endOfLine)
            assertNull(result.charset)
            assertNull(result.trimTrailingWhitespace)
            assertNull(result.insertFinalNewline)
            assertNull(result.maxLineLength)
        }
    }

    @Nested
    @DisplayName("parse() — section matching")
    inner class SectionMatching {

        @Test
        @DisplayName("matches section by exact file extension")
        fun matchesSectionByExtension() {
            val content = """
                root = true

                [*]
                indent_style = space
                indent_size = 4

                [*.kt]
                indent_size = 2
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "Example.kt")

            assertEquals("space", result.indentStyle)
            assertEquals(2, result.indentSize) // overridden by [*.kt]
        }

        @Test
        @DisplayName("wildcard * matches all files")
        fun wildcardMatchesAllFiles() {
            val content = """
                [*]
                indent_style = space
                indent_size = 4
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "anything.txt")

            assertEquals("space", result.indentStyle)
            assertEquals(4, result.indentSize)
        }

        @Test
        @DisplayName("non-matching section does not apply")
        fun nonMatchingSectionDoesNotApply() {
            val content = """
                [*]
                indent_style = space
                indent_size = 4

                [*.java]
                indent_size = 8
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "script.py")

            assertEquals(4, result.indentSize) // [*.java] does not match
        }

        @Test
        @DisplayName("later matching section overrides earlier one")
        fun laterSectionOverridesEarlier() {
            val content = """
                [*]
                indent_style = tab
                indent_size = 4
                end_of_line = crlf

                [*.md]
                indent_style = space
                indent_size = 2
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "README.md")

            assertEquals("space", result.indentStyle) // overridden
            assertEquals(2, result.indentSize) // overridden
            assertEquals("crlf", result.endOfLine) // inherited from [*]
        }
    }

    @Nested
    @DisplayName("parse() — comments and blank lines")
    inner class CommentsAndBlankLines {

        @Test
        @DisplayName("ignores # comments")
        fun ignoresHashComments() {
            val content = """
                # This is a comment
                indent_style = space
                # Another comment
                indent_size = 2
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "test.txt")

            assertEquals("space", result.indentStyle)
            assertEquals(2, result.indentSize)
        }

        @Test
        @DisplayName("ignores ; comments")
        fun ignoresSemicolonComments() {
            val content = """
                ; This is a comment
                indent_style = tab
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "test.txt")

            assertEquals("tab", result.indentStyle)
        }

        @Test
        @DisplayName("ignores blank lines")
        fun ignoresBlankLines() {
            val content = """
                indent_style = space

                indent_size = 4
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "test.txt")

            assertEquals("space", result.indentStyle)
            assertEquals(4, result.indentSize)
        }
    }

    @Nested
    @DisplayName("parse() — default values")
    inner class DefaultValues {

        @Test
        @DisplayName("all fields are null when not specified")
        fun allFieldsNullWhenNotSpecified() {
            val content = """
                root = true
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "test.txt")

            assertNull(result.indentStyle)
            assertNull(result.indentSize)
            assertNull(result.tabWidth)
            assertNull(result.endOfLine)
            assertNull(result.charset)
            assertNull(result.trimTrailingWhitespace)
            assertNull(result.insertFinalNewline)
            assertNull(result.maxLineLength)
        }
    }

    @Nested
    @DisplayName("parse() — all supported properties")
    inner class AllSupportedProperties {

        @Test
        @DisplayName("parses tab_width and max_line_length")
        fun parsesTabWidthAndMaxLineLength() {
            val content = """
                [*]
                indent_style = tab
                indent_size = 4
                tab_width = 2
                max_line_length = 120
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "file.java")

            assertEquals("tab", result.indentStyle)
            assertEquals(4, result.indentSize)
            assertEquals(2, result.tabWidth)
            assertEquals(120, result.maxLineLength)
        }

        @Test
        @DisplayName("parses false boolean values")
        fun parsesFalseBooleanValues() {
            val content = """
                [*]
                trim_trailing_whitespace = false
                insert_final_newline = false
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "file.txt")

            assertFalse(result.trimTrailingWhitespace!!)
            assertFalse(result.insertFinalNewline!!)
        }
    }

    @Nested
    @DisplayName("parse() — glob patterns")
    inner class GlobPatterns {

        @Test
        @DisplayName("? wildcard matches single character")
        fun questionMarkWildcard() {
            val content = """
                [*]
                indent_size = 4

                [?.txt]
                indent_size = 1
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "a.txt")

            assertEquals(1, result.indentSize)
        }

        @Test
        @DisplayName("? wildcard does not match multiple characters")
        fun questionMarkWildcardNoMultiMatch() {
            val content = """
                [*]
                indent_size = 4

                [?.txt]
                indent_size = 1
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "abc.txt")

            assertEquals(4, result.indentSize) // ? does not match "abc"
        }

        @Test
        @DisplayName("* wildcard in middle of pattern")
        fun wildcardInMiddle() {
            val content = """
                [*]
                indent_size = 4

                [Test*.kt]
                indent_size = 2
            """.trimIndent()

            val result = EditorConfigParser.parse(content, "TestHelper.kt")

            assertEquals(2, result.indentSize)
        }
    }
}
