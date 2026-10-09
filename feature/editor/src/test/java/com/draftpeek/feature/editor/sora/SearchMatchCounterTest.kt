package com.draftpeek.feature.editor.sora

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SearchMatchCounter（屏 15 匹配计数 n/N）")
class SearchMatchCounterTest {

    @Nested
    @DisplayName("字面量检索（regex = false）")
    inner class LiteralTest {

        @Test
        @DisplayName("默认大小写不敏感")
        fun caseInsensitiveByDefault() {
            val matches = SearchMatchCounter.findAll("Foo foo FOO", "foo")
            assertEquals(3, matches.size)
        }

        @Test
        @DisplayName("matchCase = true 时区分大小写")
        fun matchCaseRespected() {
            val matches = SearchMatchCounter.findAll("Foo foo FOO", "foo", matchCase = true)
            assertEquals(1, matches.size)
            assertEquals(4..6, matches.first())
        }

        @Test
        @DisplayName("正则元字符按字面量处理（a.b 不应匹配 axb）")
        fun regexMetacharsAreEscaped() {
            val matches = SearchMatchCounter.findAll("a.b axb a.b", "a.b")
            assertEquals(2, matches.size)
        }

        @Test
        @DisplayName("空查询返回空列表")
        fun emptyQuery() {
            assertTrue(SearchMatchCounter.findAll("abc", "").isEmpty())
        }

        @Test
        @DisplayName("无匹配返回空列表")
        fun noMatch() {
            assertTrue(SearchMatchCounter.findAll("abc", "zzz").isEmpty())
        }
    }

    @Nested
    @DisplayName("整词匹配（wholeWord）")
    inner class WholeWordTest {

        @Test
        @DisplayName("只匹配独立单词")
        fun onlyStandaloneWord() {
            val matches = SearchMatchCounter.findAll("cat category cat", "cat", wholeWord = true)
            assertEquals(2, matches.size)
        }

        @Test
        @DisplayName("整词 + 正则：用 \\b(?:...)\\b 包裹")
        fun wholeWordWithRegex() {
            val matches = SearchMatchCounter.findAll("a1 a22 a333", "a\\d+", regex = true, wholeWord = true)
            assertEquals(3, matches.size)
        }
    }

    @Nested
    @DisplayName("正则检索（regex = true）")
    inner class RegexTest {

        @Test
        @DisplayName("按正则解释查询")
        fun regexQuery() {
            val matches = SearchMatchCounter.findAll("a1 b2 c3", "\\w\\d")
            assertEquals(0, matches.size, "regex=false 时 \\w\\d 应按字面量处理，故无匹配")
            assertEquals(3, SearchMatchCounter.findAll("a1 b2 c3", "\\w\\d", regex = true).size)
        }

        @Test
        @DisplayName("非法正则返回空列表而不是抛异常")
        fun invalidRegexDoesNotThrow() {
            assertTrue(SearchMatchCounter.findAll("abc", "[", regex = true).isEmpty())
        }
    }

    @Nested
    @DisplayName("ordinalAt（当前序号）")
    inner class OrdinalTest {

        @Test
        @DisplayName("光标落在第 2 个匹配上返回 2")
        fun cursorInsideSecondMatch() {
            val matches = SearchMatchCounter.findAll("foo bar foo", "foo")
            assertEquals(2, matches.size)
            assertEquals(2, SearchMatchCounter.ordinalAt(matches, matches[1].first))
        }

        @Test
        @DisplayName("光标在匹配末尾（半开边界）也算命中该匹配")
        fun cursorAtMatchEnd() {
            val matches = SearchMatchCounter.findAll("foo bar", "foo")
            assertEquals(1, SearchMatchCounter.ordinalAt(matches, matches[0].last + 1))
        }

        @Test
        @DisplayName("光标不在任何匹配内返回 0")
        fun cursorOutsideMatches() {
            val matches = SearchMatchCounter.findAll("foo bar", "foo")
            assertEquals(0, SearchMatchCounter.ordinalAt(matches, 6))
        }

        @Test
        @DisplayName("无匹配时返回 0")
        fun noMatches() {
            assertEquals(0, SearchMatchCounter.ordinalAt(emptyList(), 0))
        }

        // ── 回归：相邻匹配（真机走查发现的「序号永远停在 1/N」） ──────────────

        @Test
        @DisplayName("相邻匹配：光标在前一个匹配上 → 1（不是 2）")
        fun adjacentMatchesCursorOnFirst() {
            val matches = SearchMatchCounter.findAll("bb", "b")
            assertEquals(2, matches.size)
            assertEquals(1, SearchMatchCounter.ordinalAt(matches, 0))
        }

        @Test
        @DisplayName("相邻匹配：光标在后一个匹配上 → 2（旧实现会误判为 1）")
        fun adjacentMatchesCursorOnSecond() {
            val matches = SearchMatchCounter.findAll("bb", "b")
            assertEquals(2, SearchMatchCounter.ordinalAt(matches, 1))
        }

        @Test
        @DisplayName("相邻匹配：光标紧跟最后一个匹配之后 → 仍算最后一个")
        fun adjacentMatchesCursorJustAfterLast() {
            val matches = SearchMatchCounter.findAll("bb", "b")
            assertEquals(2, SearchMatchCounter.ordinalAt(matches, 2))
        }

        @Test
        @DisplayName("相邻匹配：再往后一格 → 0")
        fun adjacentMatchesCursorBeyond() {
            val matches = SearchMatchCounter.findAll("bb", "b")
            assertEquals(0, SearchMatchCounter.ordinalAt(matches, 3))
        }

        @Test
        @DisplayName("三个连续匹配逐个命中 1/2/3（对应 b1b2b3 的场景）")
        fun threeAdjacentMatches() {
            val matches = SearchMatchCounter.findAll("bbb", "b")
            assertEquals(3, matches.size)
            assertEquals(1, SearchMatchCounter.ordinalAt(matches, 0))
            assertEquals(2, SearchMatchCounter.ordinalAt(matches, 1))
            assertEquals(3, SearchMatchCounter.ordinalAt(matches, 2))
        }
    }

    @Nested
    @DisplayName("countDisplay（设计回函 v2 · Q1 显示口径）")
    inner class CountDisplayTest {

        @Test
        @DisplayName("零匹配 → NONE（无匹配）")
        fun zeroMatches() {
            assertEquals(SearchCountDisplay.NONE, SearchMatchCounter.countDisplay(0, 0))
        }

        @Test
        @DisplayName("已定位（ordinal ≥ 1）→ LOCATED（i / N）")
        fun located() {
            assertEquals(SearchCountDisplay.LOCATED, SearchMatchCounter.countDisplay(1, 3))
            assertEquals(SearchCountDisplay.LOCATED, SearchMatchCounter.countDisplay(3, 3))
        }

        @Test
        @DisplayName("有匹配但未定位 → TOTAL（N 个结果），绝不显示 0/N")
        fun unlocatedShowsTotalNotZero() {
            assertEquals(SearchCountDisplay.TOTAL, SearchMatchCounter.countDisplay(0, 3))
        }

        @Test
        @DisplayName("未定位且总数 > 999 → TOTAL_CAPPED（999+ 个结果）")
        fun capped() {
            assertEquals(
                SearchCountDisplay.TOTAL_CAPPED,
                SearchMatchCounter.countDisplay(0, SearchMatchCounter.MAX_DISPLAY_TOTAL + 1)
            )
        }

        @Test
        @DisplayName("恰好 999 个且未定位 → 仍是 TOTAL（边界：999 不算超限）")
        fun exactlyAtCapIsNotCapped() {
            assertEquals(
                SearchCountDisplay.TOTAL,
                SearchMatchCounter.countDisplay(0, SearchMatchCounter.MAX_DISPLAY_TOTAL)
            )
        }

        @Test
        @DisplayName("已定位时即使总数超限也走 LOCATED（能定位即总数未过 10000 上限，序号信息更准确）")
        fun locatedWinsOverCap() {
            assertEquals(
                SearchCountDisplay.LOCATED,
                SearchMatchCounter.countDisplay(2, SearchMatchCounter.MAX_DISPLAY_TOTAL + 1)
            )
        }
    }
}
