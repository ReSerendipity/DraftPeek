package com.draftpeek.feature.editor.model

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MarkdownBlock")
class MarkdownBlockTest {

    @Nested
    @DisplayName("estimateHeight()")
    inner class EstimateHeightTests {

        @Test
        @DisplayName("H1 标题高度为 48dp")
        fun h1Height_is48dp() {
            val height = MarkdownBlock.estimateHeight("# Title", MarkdownBlockType.HEADING)
            assertEquals(48.dp, height)
        }

        @Test
        @DisplayName("H2 标题高度为 40dp")
        fun h2Height_is40dp() {
            val height = MarkdownBlock.estimateHeight("## Title", MarkdownBlockType.HEADING)
            assertEquals(40.dp, height)
        }

        @Test
        @DisplayName("H3 标题高度为 34dp")
        fun h3Height_is34dp() {
            val height = MarkdownBlock.estimateHeight("### Title", MarkdownBlockType.HEADING)
            assertEquals(34.dp, height)
        }

        @Test
        @DisplayName("代码块高度基于行数计算")
        fun codeBlockHeight_basedOnLineCount() {
            val content = "line1\nline2\nline3"
            val height = MarkdownBlock.estimateHeight(content, MarkdownBlockType.CODE_BLOCK)
            // 3 lines * 20dp + 16dp = 76dp
            assertEquals(76.dp, height)
        }

        @Test
        @DisplayName("水平分隔线高度为 16dp")
        fun hrHeight_is16dp() {
            val height = MarkdownBlock.estimateHeight("---", MarkdownBlockType.HORIZONTAL_RULE)
            assertEquals(16.dp, height)
        }

        @Test
        @DisplayName("空行高度为 8dp")
        fun blankHeight_is8dp() {
            val height = MarkdownBlock.estimateHeight("", MarkdownBlockType.BLANK)
            assertEquals(8.dp, height)
        }

        @Test
        @DisplayName("段落高度至少为行数高度")
        fun paragraphHeight_atLeastLineCount() {
            val content = "a\nb\nc"
            val height = MarkdownBlock.estimateHeight(content, MarkdownBlockType.PARAGRAPH)
            // 3 chars / 40 = 0, coerced to lineCount=3 → 3*24+12 = 84
            assertEquals(84.dp, height)
        }

        @Test
        @DisplayName("列表高度基于行数计算")
        fun listHeight_basedOnLineCount() {
            val content = "- item1\n- item2"
            val height = MarkdownBlock.estimateHeight(content, MarkdownBlockType.UNORDERED_LIST)
            // 2 lines * 26dp + 8dp = 60dp
            assertEquals(60.dp, height)
        }

        @Test
        @DisplayName("表格高度基于行数计算")
        fun tableHeight_basedOnLineCount() {
            val content = "| A |\n|---|\n| 1 |"
            val height = MarkdownBlock.estimateHeight(content, MarkdownBlockType.TABLE)
            // 3 lines * 32dp + 8dp = 104dp
            assertEquals(104.dp, height)
        }
    }

    @Nested
    @DisplayName("MarkdownBlock data class")
    inner class DataClassTests {

        @Test
        @DisplayName("默认值正确设置")
        fun defaultValues() {
            val block = MarkdownBlock(content = "text", type = MarkdownBlockType.PARAGRAPH)
            assertEquals(0, block.startIndex)
            assertEquals(0, block.endIndex)
            assertEquals(0.dp, block.estimatedHeightDp)
        }

        @Test
        @DisplayName("相同内容的块相等")
        fun equality() {
            val block1 = MarkdownBlock("text", MarkdownBlockType.PARAGRAPH)
            val block2 = MarkdownBlock("text", MarkdownBlockType.PARAGRAPH)
            assertEquals(block1, block2)
        }
    }

    @Nested
    @DisplayName("MarkdownBlockType enum")
    inner class EnumTests {

        @Test
        @DisplayName("包含所有预期类型")
        fun containsAllTypes() {
            val types = MarkdownBlockType.entries
            assertTrue(types.contains(MarkdownBlockType.HEADING))
            assertTrue(types.contains(MarkdownBlockType.PARAGRAPH))
            assertTrue(types.contains(MarkdownBlockType.CODE_BLOCK))
            assertTrue(types.contains(MarkdownBlockType.UNORDERED_LIST))
            assertTrue(types.contains(MarkdownBlockType.ORDERED_LIST))
            assertTrue(types.contains(MarkdownBlockType.TASK_LIST))
            assertTrue(types.contains(MarkdownBlockType.BLOCKQUOTE))
            assertTrue(types.contains(MarkdownBlockType.TABLE))
            assertTrue(types.contains(MarkdownBlockType.HORIZONTAL_RULE))
            assertTrue(types.contains(MarkdownBlockType.BLANK))
            assertEquals(10, types.size)
        }
    }
}
