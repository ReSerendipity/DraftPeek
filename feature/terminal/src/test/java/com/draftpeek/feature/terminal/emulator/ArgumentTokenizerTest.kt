package com.draftpeek.feature.terminal.emulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ArgumentTokenizer")
class ArgumentTokenizerTest {

    @Nested
    @DisplayName("tokenize() — simple commands")
    inner class SimpleCommandTests {

        @Test
        @DisplayName("空命令返回空列表")
        fun emptyCommand_returnsEmptyList() {
            assertTrue(ArgumentTokenizer.tokenize("").isEmpty())
        }

        @Test
        @DisplayName("纯空白命令返回空列表")
        fun whitespaceOnly_returnsEmptyList() {
            assertTrue(ArgumentTokenizer.tokenize("   ").isEmpty())
        }

        @Test
        @DisplayName("单个命令无参数")
        fun singleCommand() {
            val args = ArgumentTokenizer.tokenize("ls")
            assertEquals(listOf("ls"), args)
        }

        @Test
        @DisplayName("命令带多个参数")
        fun commandWithArgs() {
            val args = ArgumentTokenizer.tokenize("echo hello world")
            assertEquals(listOf("echo", "hello", "world"), args)
        }

        @Test
        @DisplayName("多个空格分隔参数正确处理")
        fun multipleSpaces() {
            val args = ArgumentTokenizer.tokenize("echo    hello     world")
            assertEquals(listOf("echo", "hello", "world"), args)
        }
    }

    @Nested
    @DisplayName("tokenize() — single quotes")
    inner class SingleQuoteTests {

        @Test
        @DisplayName("单引号内容作为字面量")
        fun singleQuote_literal() {
            val args = ArgumentTokenizer.tokenize("echo 'hello world'")
            assertEquals(listOf("echo", "hello world"), args)
        }

        @Test
        @DisplayName("单引号内特殊字符不转义")
        fun singleQuote_noEscape() {
            val args = ArgumentTokenizer.tokenize("echo 'hello \$world'")
            assertEquals(listOf("echo", "hello \$world"), args)
        }

        @Test
        @DisplayName("未闭合单引号抛出异常")
        fun unclosedSingleQuote_throws() {
            assertThrows(IllegalArgumentException::class.java) {
                ArgumentTokenizer.tokenize("echo 'unclosed")
            }
        }
    }

    @Nested
    @DisplayName("tokenize() — double quotes")
    inner class DoubleQuoteTests {

        @Test
        @DisplayName("双引号内容保留空格")
        fun doubleQuote_preservesSpaces() {
            val args = ArgumentTokenizer.tokenize("echo \"hello world\"")
            assertEquals(listOf("echo", "hello world"), args)
        }

        @Test
        @DisplayName("双引号内反斜杠转义")
        fun doubleQuote_escapeSequences() {
            val args = ArgumentTokenizer.tokenize("echo \"hello \\\"world\\\"\"")
            assertEquals(listOf("echo", "hello \"world\""), args)
        }

        @Test
        @DisplayName("未闭合双引号抛出异常")
        fun unclosedDoubleQuote_throws() {
            assertThrows(IllegalArgumentException::class.java) {
                ArgumentTokenizer.tokenize("echo \"unclosed")
            }
        }
    }

    @Nested
    @DisplayName("tokenize() — backslash escape")
    inner class BackslashTests {

        @Test
        @DisplayName("引号外反斜杠转义下一字符")
        fun backslashOutsideQuotes() {
            val args = ArgumentTokenizer.tokenize("echo hello\\ world")
            assertEquals(listOf("echo", "hello world"), args)
        }

        @Test
        @DisplayName("末尾反斜杠作为字面量")
        fun trailingBackslash_literal() {
            val args = ArgumentTokenizer.tokenize("echo hello\\")
            assertEquals(listOf("echo", "hello\\"), args)
        }
    }

    @Nested
    @DisplayName("tokenize() — mixed quotes")
    inner class MixedQuoteTests {

        @Test
        @DisplayName("单双引号混合使用")
        fun mixedQuotes() {
            val args = ArgumentTokenizer.tokenize("echo 'hello' \"world\"")
            assertEquals(listOf("echo", "hello", "world"), args)
        }

        @Test
        @DisplayName("嵌套引号（单引号内双引号）")
        fun nestedQuotes() {
            val args = ArgumentTokenizer.tokenize("echo 'say \"hi\"'")
            assertEquals(listOf("echo", "say \"hi\""), args)
        }
    }

    @Nested
    @DisplayName("tokenizeOrNull()")
    inner class TokenizeOrNullTests {

        @Test
        @DisplayName("有效命令返回参数列表")
        fun validCommand_returnsArgs() {
            val args = ArgumentTokenizer.tokenizeOrNull("echo hello")
            assertEquals(listOf("echo", "hello"), args)
        }

        @Test
        @DisplayName("无效命令返回 null")
        fun invalidCommand_returnsNull() {
            val args = ArgumentTokenizer.tokenizeOrNull("echo 'unclosed")
            assertTrue(args == null)
        }
    }
}
