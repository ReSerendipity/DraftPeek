package com.draftpeek.feature.editor.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MarkdownLinkParser")
class MarkdownLinkParserTest {

    @Nested
    @DisplayName("parse() 双链 [[...]]")
    inner class WikilinkTests {

        @Test
        @DisplayName("提取简单双链目标")
        fun simpleWikilink_extractsTitle() {
            val result = MarkdownLinkParser.parse("我在读 [[读书清单]]")
            assertEquals(listOf("读书清单"), result.linkedTitles)
        }

        @Test
        @DisplayName("提取多个双链目标并去重")
        fun multipleWikilinks_deduplicated() {
            val result = MarkdownLinkParser.parse("[[A]] 和 [[B]] 还有 [[A]]")
            assertEquals(listOf("A", "B"), result.linkedTitles)
        }

        @Test
        @DisplayName("支持 [[目标|显示文本]] 别名形式")
        fun aliasForm_usesTargetBeforePipe() {
            val result = MarkdownLinkParser.parse("见 [[项目计划|计划文档]]")
            assertEquals(listOf("项目计划"), result.linkedTitles)
        }

        @Test
        @DisplayName("忽略代码块中的双链")
        fun codeBlock_ignored() {
            val content = "```\nval x = \"[[不应提取]]\"\n```\n正文 [[应提取]]"
            val result = MarkdownLinkParser.parse(content)
            assertEquals(listOf("应提取"), result.linkedTitles)
        }

        @Test
        @DisplayName("忽略行内代码中的双链")
        fun inlineCode_ignored() {
            val content = "写代码 `fun f() { [[x]] }` 后 [[正常目标]]"
            val result = MarkdownLinkParser.parse(content)
            assertEquals(listOf("正常目标"), result.linkedTitles)
        }

        @Test
        @DisplayName("空内容返回空结果")
        fun blankContent_empty() {
            val result = MarkdownLinkParser.parse("   ")
            assertTrue(result.linkedTitles.isEmpty())
            assertTrue(result.mentions.isEmpty())
        }
    }

    @Nested
    @DisplayName("parse() @提及")
    inner class MentionTests {

        @Test
        @DisplayName("提取 @提及目标")
        fun simpleMention_extracts() {
            val result = MarkdownLinkParser.parse("合作者 @小明 和 @kate")
            assertEquals(listOf("小明", "kate"), result.mentions)
        }

        @Test
        @DisplayName("不提取邮箱地址中的 @")
        fun emailNotExtracted() {
            val result = MarkdownLinkParser.parse("联系 test@example.com 吧")
            assertTrue(result.mentions.isEmpty())
        }

        @Test
        @DisplayName("去重合并全部目标")
        fun allTargets_mergesAndDeduplicates() {
            val result = MarkdownLinkParser.parse("[[目标]] 提到 @目标")
            assertEquals(listOf("目标"), result.allTargets)
        }
    }
}