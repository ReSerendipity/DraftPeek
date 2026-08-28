package com.draftpeek.feature.editor.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DocumentStatsCalculator")
class DocumentStatsCalculatorTest {

    @Nested
    @DisplayName("calculate() — empty and blank input")
    inner class EmptyInputTests {

        @Test
        @DisplayName("空字符串返回零统计")
        fun emptyString_returnsZeroStats() {
            val stats = DocumentStatsCalculator.calculate("")
            assertEquals(0, stats.wordCount)
            assertEquals(0, stats.charCount)
            assertEquals(0, stats.lineCount)
        }

        @Test
        @DisplayName("纯空格字符串统计正确")
        fun spacesOnly_correctStats() {
            val stats = DocumentStatsCalculator.calculate("   ")
            assertEquals(0, stats.wordCount)
            assertEquals(3, stats.charCount)
            assertEquals(1, stats.lineCount)
        }
    }

    @Nested
    @DisplayName("calculate() — English text")
    inner class EnglishTextTests {

        @Test
        @DisplayName("单个英文单词")
        fun singleWord() {
            val stats = DocumentStatsCalculator.calculate("hello")
            assertEquals(1, stats.wordCount)
            assertEquals(5, stats.charCount)
            assertEquals(1, stats.lineCount)
        }

        @Test
        @DisplayName("多行英文文本正确计数字数和行数")
        fun multiLineEnglish() {
            val text = "hello world\nfoo bar baz"
            val stats = DocumentStatsCalculator.calculate(text)
            assertEquals(5, stats.wordCount)
            assertEquals(2, stats.lineCount)
        }

        @Test
        @DisplayName("数字序列作为单词计数")
        fun numbersAsWords() {
            val stats = DocumentStatsCalculator.calculate("123 456 789")
            assertEquals(3, stats.wordCount)
        }

        @Test
        @DisplayName("混合字母数字作为一个单词")
        fun mixedAlphaNumeric() {
            val stats = DocumentStatsCalculator.calculate("abc123 def456")
            assertEquals(2, stats.wordCount)
        }
    }

    @Nested
    @DisplayName("calculate() — CJK text")
    inner class CjkTextTests {

        @Test
        @DisplayName("中文字符每个算一个单词")
        fun chineseChars_eachCountsAsWord() {
            val stats = DocumentStatsCalculator.calculate("你好世界")
            assertEquals(4, stats.wordCount)
            assertEquals(4, stats.charCount)
        }

        @Test
        @DisplayName("中文混合文本中 CJK 字符每个算一个单词")
        fun mixedCjk() {
            // 仅 CJK 统一表意文字 (U+4E00..U+9FFF) 被检测
            // 日文假名和韩文 Hangul 不在检测范围内
            val stats = DocumentStatsCalculator.calculate("你好世界代码")
            assertEquals(6, stats.wordCount)
        }

        @Test
        @DisplayName("中英文混合文本")
        fun mixedChineseEnglish() {
            val stats = DocumentStatsCalculator.calculate("hello 你好 world")
            // "hello" = 1, "你好" = 2, "world" = 1 → 4
            assertEquals(4, stats.wordCount)
        }

        @Test
        @DisplayName("全角符号计入CJK")
        fun fullwidthSymbols() {
            val stats = DocumentStatsCalculator.calculate("ＡＢＣ")
            assertEquals(3, stats.wordCount)
        }
    }

    @Nested
    @DisplayName("calculate() — line counting")
    inner class LineCountTests {

        @Test
        @DisplayName("单行文本")
        fun singleLine() {
            val stats = DocumentStatsCalculator.calculate("single line")
            assertEquals(1, stats.lineCount)
        }

        @Test
        @DisplayName("多行文本含空行")
        fun multiLineWithEmptyLines() {
            val text = "line1\n\nline3\n"
            val stats = DocumentStatsCalculator.calculate(text)
            assertEquals(4, stats.lineCount) // "line1", "", "line3", ""
        }

        @Test
        @DisplayName("仅换行符")
        fun newlinesOnly() {
            val stats = DocumentStatsCalculator.calculate("\n\n\n")
            assertEquals(4, stats.lineCount)
        }
    }

    @Nested
    @DisplayName("calculate() — character count")
    inner class CharCountTests {

        @Test
        @DisplayName("字符数包含空格和换行")
        fun charCount_includesSpacesAndNewlines() {
            val text = "a b\nc"
            val stats = DocumentStatsCalculator.calculate(text)
            assertEquals(5, stats.charCount) // a, space, b, newline, c
        }
    }
}
