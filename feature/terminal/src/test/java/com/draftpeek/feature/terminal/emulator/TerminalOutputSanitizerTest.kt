package com.draftpeek.feature.terminal.emulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TerminalOutputSanitizer")
class TerminalOutputSanitizerTest {

    private fun bytes(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    @Test
    @DisplayName("剥离 SGR 颜色序列，保留文本")
    fun stripsSgr() {
        val update = TerminalOutputSanitizer().feed(bytes("\u001B[31mred\u001B[0m\n"))
        assertEquals(listOf("red"), update.completedLines)
        assertEquals("", update.currentLine)
        assertFalse(update.clearScreen)
    }

    @Test
    @DisplayName("CRLF 只提交一行，不产生空行")
    fun handlesCrLf() {
        val update = TerminalOutputSanitizer().feed(bytes("abc\r\n"))
        assertEquals(listOf("abc"), update.completedLines)
        assertEquals("", update.currentLine)
    }

    @Test
    @DisplayName("单独 CR 覆盖当前行")
    fun carriageReturnOverwritesCurrentLine() {
        val update = TerminalOutputSanitizer().feed(bytes("progress 10%\rprogress 20%"))
        assertEquals("progress 20%", update.currentLine)
        assertTrue(update.completedLines.isEmpty())
    }

    @Test
    @DisplayName("退格删除当前行最后一个字符")
    fun backspaceDeletesLastChar() {
        val update = TerminalOutputSanitizer().feed(bytes("ab\b"))
        assertEquals("a", update.currentLine)
    }

    @Test
    @DisplayName("TAB 展开到 8 列制表位")
    fun tabExpandsToTabStop() {
        val update = TerminalOutputSanitizer().feed(bytes("a\t"))
        assertEquals("a       ", update.currentLine)
    }

    @Test
    @DisplayName("UTF-8 跨 chunk 增量解码")
    fun decodesSplitUtf8() {
        val sanitizer = TerminalOutputSanitizer()
        val chineseBytes = "中".toByteArray(Charsets.UTF_8)
        val first = sanitizer.feed(chineseBytes.copyOfRange(0, 2))
        assertEquals("", first.currentLine)
        val second = sanitizer.feed(chineseBytes.copyOfRange(2, 3))
        assertEquals("中", second.currentLine)
    }

    @Test
    @DisplayName("OSC 标题序列不进入输出")
    fun stripsOscTitle() {
        val update = TerminalOutputSanitizer().feed(bytes("\u001B]0;draftpeek\u0007hello"))
        assertEquals("hello", update.currentLine)
    }

    @Test
    @DisplayName("CSI 2J 触发清屏标记")
    fun csi2JClearsScreen() {
        val sanitizer = TerminalOutputSanitizer()
        sanitizer.feed(bytes("old\n"))
        val update = sanitizer.feed(bytes("\u001B[2Jnew"))
        assertTrue(update.clearScreen)
        assertEquals("new", update.currentLine)
    }

    @Test
    @DisplayName("备用屏幕 1049h/l 触发清屏，vim 退出不残留旧帧")
    fun alternateScreenClears() {
        val sanitizer = TerminalOutputSanitizer()
        sanitizer.feed(bytes("prompt\n"))
        assertTrue(sanitizer.feed(bytes("\u001B[?1049h")).clearScreen)
        assertTrue(sanitizer.feed(bytes("vim-frame\u001B[?1049l")).clearScreen)
    }

    @Test
    @DisplayName("跨 chunk 的 CSI 序列被完整丢弃")
    fun csiSplitAcrossChunks() {
        val sanitizer = TerminalOutputSanitizer()
        val first = sanitizer.feed(bytes("abc\u001B["))
        assertEquals("abc", first.currentLine)
        val second = sanitizer.feed(bytes("31m"))
        assertEquals("abc", second.currentLine)
    }
}
