package com.draftpeek.feature.terminal.emulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TerminalTextBuffer")
class TerminalTextBufferTest {

    private fun bytes(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    @Test
    @DisplayName("已提交行与当前行拼接为快照")
    fun joinsCommittedAndCurrentLine() {
        val buffer = TerminalTextBuffer()
        buffer.feed(bytes("line1\nline2"))
        assertEquals("line1\nline2", buffer.snapshot())
    }

    @Test
    @DisplayName("清屏序列丢弃旧内容")
    fun clearScreenDropsOldContent() {
        val buffer = TerminalTextBuffer()
        buffer.feed(bytes("old\n"))
        buffer.feed(bytes("\u001B[2Jnew"))
        assertEquals("new", buffer.snapshot())
    }

    @Test
    @DisplayName("超过 maxChars 时保留最新尾部")
    fun keepsBoundedTail() {
        val buffer = TerminalTextBuffer(maxChars = 100)
        buffer.feed(bytes("x".repeat(200)))
        val snapshot = buffer.snapshot()
        assertEquals(100, snapshot.length)
        assertTrue(snapshot.all { it == 'x' })
    }

    @Test
    @DisplayName("flush 输出残留半行")
    fun flushOutputsPendingLine() {
        val buffer = TerminalTextBuffer()
        buffer.feed(bytes("pending"))
        buffer.flush()
        assertEquals("pending", buffer.snapshot())
    }
}
