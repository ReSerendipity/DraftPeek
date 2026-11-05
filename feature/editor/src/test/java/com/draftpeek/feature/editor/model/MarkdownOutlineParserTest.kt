package com.draftpeek.feature.editor.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * [MarkdownOutlineParser] 单元测试（评估 2026-11-05 P1-3：核心 UI 模块补实质断言）。
 *
 * 锁定大纲导航（OutlineDrawer）的解析契约——该解析器此前无任何测试：
 * 1. ATX 风格标题（# 到 ######，锚定行首、# 后必须有空格）；
 * 2. Setext 风格标题（下一行 `===` 一级 / `---` 二级，下划线长度 ≥ 2）；
 * 3. 结果按文档顺序排列，lineIndex 指向标题所在行（0 基）；
 * 4. 非标题行（无空格的 #、7 个以上 #、缩进 #、空行）不产生条目。
 *
 * 运行方式：./gradlew :feature:editor:testDebugUnitTest --tests "*MarkdownOutlineParserTest"
 */
@DisplayName("MarkdownOutlineParser")
class MarkdownOutlineParserTest {

    @Nested
    @DisplayName("parse() — 空输入")
    inner class EmptyInputTests {

        @Test
        @DisplayName("空字符串返回空大纲")
        fun emptyString_returnsEmptyList() {
            assertTrue(MarkdownOutlineParser.parse("").isEmpty())
        }

        @Test
        @DisplayName("无标题的普通文本返回空大纲")
        fun plainText_returnsEmptyList() {
            val doc = "这是一段普通文字。\n第二行也没有标题。\n"
            assertTrue(MarkdownOutlineParser.parse(doc).isEmpty())
        }
    }

    @Nested
    @DisplayName("parse() — ATX 风格标题")
    inner class AtxTests {

        @ParameterizedTest(name = "一级到六级标题：{0}")
        @ValueSource(ints = [1, 2, 3, 4, 5, 6])
        fun atx_levels1To6(level: Int) {
            val line = "#".repeat(level) + " 标题"
            val headings = MarkdownOutlineParser.parse(line)
            assertEquals(1, headings.size)
            assertEquals(level, headings[0].level)
            assertEquals("标题", headings[0].text)
            assertEquals(0, headings[0].lineIndex)
        }

        @Test
        @DisplayName("七个 # 不是标题（正则上限六级）")
        fun atx_sevenHashes_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("####### 七个井号").isEmpty())
        }

        @Test
        @DisplayName("# 后无空格不是标题")
        fun atx_noSpaceAfterHashes_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("#NoSpace").isEmpty())
        }

        @Test
        @DisplayName("缩进的 # 不是标题（行首锚定）")
        fun atx_indented_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("  # 缩进标题").isEmpty())
        }

        @Test
        @DisplayName("行尾闭合 # 被剥离")
        fun atx_trailingClosingHash_removed() {
            val headings = MarkdownOutlineParser.parse("# 标题 #")
            assertEquals(1, headings.size)
            assertEquals("标题", headings[0].text)
        }

        @Test
        @DisplayName("只有 # 没有文字不是标题")
        fun atx_noText_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("# ").isEmpty())
            assertTrue(MarkdownOutlineParser.parse("##").isEmpty())
        }
    }

    @Nested
    @DisplayName("parse() — Setext 风格标题")
    inner class SetextTests {

        @Test
        @DisplayName("=== 下划线 → 一级标题，lineIndex 指向文字行")
        fun setext_equals_level1() {
            val doc = "引言\n\n大标题\n======\n正文"
            val headings = MarkdownOutlineParser.parse(doc)
            assertEquals(1, headings.size)
            assertEquals(OutlineHeading(1, "大标题", 2), headings[0])
        }

        @Test
        @DisplayName("--- 下划线 → 二级标题")
        fun setext_dashes_level2() {
            val doc = "小节\n--"
            val headings = MarkdownOutlineParser.parse(doc)
            assertEquals(1, headings.size)
            assertEquals(OutlineHeading(2, "小节", 0), headings[0])
        }

        @Test
        @DisplayName("下划线长度为 1 不构成 Setext 标题")
        fun setext_singleCharUnderline_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("标题\n=").isEmpty())
            assertTrue(MarkdownOutlineParser.parse("标题\n-").isEmpty())
        }

        @Test
        @DisplayName("文字行为空行时不构成 Setext 标题")
        fun setext_blankTextLine_notHeading() {
            assertTrue(MarkdownOutlineParser.parse("\n====").isEmpty())
        }

        @Test
        @DisplayName("Setext 文字行两侧空白被裁剪")
        fun setext_textTrimmed() {
            val headings = MarkdownOutlineParser.parse("  带空白标题  \n====")
            assertEquals(1, headings.size)
            assertEquals("带空白标题", headings[0].text)
        }
    }

    @Nested
    @DisplayName("parse() — 混合文档")
    inner class MixedDocumentTests {

        @Test
        @DisplayName("多级标题按文档顺序排列且 lineIndex 正确")
        fun mixedDocument_orderAndLineIndex() {
            val doc = """
                # 第一章
                段落文字。
                ## 第一节
                更多文字。
                ### 小点
            """.trimIndent()
            val headings = MarkdownOutlineParser.parse(doc)
            assertEquals(
                listOf(
                    OutlineHeading(1, "第一章", 0),
                    OutlineHeading(2, "第一节", 2),
                    OutlineHeading(3, "小点", 4)
                ),
                headings
            )
        }

        @Test
        @DisplayName("ATX 与 Setext 混用互不干扰")
        fun mixedStyles_bothParsed() {
            val doc = "ATX 一级\n====\n# ATX 二级\n"
            val headings = MarkdownOutlineParser.parse(doc)
            assertEquals(2, headings.size)
            assertEquals(OutlineHeading(1, "ATX 一级", 0), headings[0])
            assertEquals(OutlineHeading(1, "ATX 二级", 2), headings[1])
        }
    }
}
