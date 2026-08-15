package com.draftpeek.feature.editor.util

import com.draftpeek.feature.editor.model.MarkdownBlockType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MarkdownChunker")
class MarkdownChunkerTest {

    @Nested
    @DisplayName("chunkMarkdown() — empty input")
    inner class EmptyInputTests {

        @Test
        @DisplayName("空字符串返回空列表")
        fun emptyString_returnsEmptyList() {
            assertTrue(chunkMarkdown("").isEmpty())
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — headings")
    inner class HeadingTests {

        @Test
        @DisplayName("单个标题作为独立块")
        fun singleHeading_ownBlock() {
            val blocks = chunkMarkdown("# Title")
            assertEquals(1, blocks.size)
            assertEquals(MarkdownBlockType.HEADING, blocks[0].type)
            assertTrue(blocks[0].content.contains("# Title"))
        }

        @Test
        @DisplayName("多个标题各自独立分块")
        fun multipleHeadings_separateBlocks() {
            val blocks = chunkMarkdown("# H1\n## H2\n### H3")
            assertEquals(3, blocks.size)
            blocks.forEach { assertEquals(MarkdownBlockType.HEADING, it.type) }
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — code blocks")
    inner class CodeBlockTests {

        @Test
        @DisplayName("代码块作为整体不拆分")
        fun codeBlock_singleBlock() {
            val content = """
                ```kotlin
                fun main() {
                    println("hello")
                }
                ```
            """.trimIndent()
            val blocks = chunkMarkdown(content)
            // Code block should be a single block
            val codeBlocks = blocks.filter { it.type == MarkdownBlockType.CODE_BLOCK }
            assertEquals(1, codeBlocks.size)
        }

        @Test
        @DisplayName("波浪号代码块也正确识别")
        fun tildeCodeBlock_recognized() {
            val content = """
                ~~~python
                print("hello")
                ~~~
            """.trimIndent()
            val blocks = chunkMarkdown(content)
            val codeBlocks = blocks.filter { it.type == MarkdownBlockType.CODE_BLOCK }
            assertEquals(1, codeBlocks.size)
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — paragraphs")
    inner class ParagraphTests {

        @Test
        @DisplayName("连续文本行组成段落块")
        fun continuousLines_formParagraph() {
            val blocks = chunkMarkdown("This is\na paragraph.")
            assertEquals(1, blocks.size)
            assertEquals(MarkdownBlockType.PARAGRAPH, blocks[0].type)
        }

        @Test
        @DisplayName("空行分隔不同段落")
        fun blankLine_separatesParagraphs() {
            val blocks = chunkMarkdown("First paragraph.\n\nSecond paragraph.")
            assertEquals(2, blocks.size)
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — lists")
    inner class ListTests {

        @Test
        @DisplayName("无序列表正确识别")
        fun unorderedList_recognized() {
            val blocks = chunkMarkdown("- item1\n- item2\n- item3")
            val listBlocks = blocks.filter { it.type == MarkdownBlockType.UNORDERED_LIST }
            assertTrue(listBlocks.isNotEmpty())
        }

        @Test
        @DisplayName("有序列表正确识别")
        fun orderedList_recognized() {
            val blocks = chunkMarkdown("1. first\n2. second\n3. third")
            val listBlocks = blocks.filter { it.type == MarkdownBlockType.ORDERED_LIST }
            assertTrue(listBlocks.isNotEmpty())
        }

        @Test
        @DisplayName("任务列表正确识别")
        fun taskList_recognized() {
            val blocks = chunkMarkdown("- [x] done\n- [ ] todo")
            val taskBlocks = blocks.filter { it.type == MarkdownBlockType.TASK_LIST }
            assertTrue(taskBlocks.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — blockquotes and tables")
    inner class BlockquoteAndTableTests {

        @Test
        @DisplayName("引用块正确识别")
        fun blockquote_recognized() {
            val blocks = chunkMarkdown("> This is a quote\n> Second line")
            val quoteBlocks = blocks.filter { it.type == MarkdownBlockType.BLOCKQUOTE }
            assertTrue(quoteBlocks.isNotEmpty())
        }

        @Test
        @DisplayName("表格正确识别")
        fun table_recognized() {
            val blocks = chunkMarkdown("| A | B |\n|---|---|\n| 1 | 2 |")
            val tableBlocks = blocks.filter { it.type == MarkdownBlockType.TABLE }
            assertTrue(tableBlocks.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — horizontal rules")
    inner class HorizontalRuleTests {

        @Test
        @DisplayName("--- 分隔线识别")
        fun dashHr_recognized() {
            val blocks = chunkMarkdown("---")
            val hrBlocks = blocks.filter { it.type == MarkdownBlockType.HORIZONTAL_RULE }
            assertTrue(hrBlocks.isNotEmpty())
        }

        @Test
        @DisplayName("*** 分隔线识别")
        fun asteriskHr_recognized() {
            val blocks = chunkMarkdown("***")
            val hrBlocks = blocks.filter { it.type == MarkdownBlockType.HORIZONTAL_RULE }
            assertTrue(hrBlocks.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("chunkMarkdown() — mixed content")
    inner class MixedContentTests {

        @Test
        @DisplayName("标题 + 段落 + 代码块正确分块")
        fun mixedContent_correctBlocks() {
            val content = """
                # Title

                Some paragraph text.

                ```
                code here
                ```

                Another paragraph.
            """.trimIndent()
            val blocks = chunkMarkdown(content)
            assertTrue(blocks.size >= 4)
        }
    }
}
