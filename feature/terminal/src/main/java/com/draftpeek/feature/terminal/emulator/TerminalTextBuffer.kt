/*
 * 会话输出文本缓冲。
 *
 * 维护已提交行 + 当前未提交行，按字符数封顶（默认 50,000，与原 OUTPUT_BUFFER_MAX 一致）。
 * reader 线程 feed，Main 线程 snapshot；所有公共方法加锁。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */
package com.draftpeek.feature.terminal.emulator

/**
 * 有界终端文本缓冲。
 *
 * @param maxChars 最大字符数，超出后从最旧的行开始丢弃
 */
internal class TerminalTextBuffer(private val maxChars: Int = DEFAULT_MAX_CHARS) {

    private val sanitizer = TerminalOutputSanitizer()
    private val lines = ArrayDeque<String>()
    private var currentLine = ""
    private var totalChars = 0
    private var cachedSnapshot: String? = null

    /** 追加一块 PTY 原始字节。 */
    @Synchronized
    fun feed(bytes: ByteArray) {
        apply(sanitizer.feed(bytes))
    }

    /** 会话结束时冲刷残留字节。 */
    @Synchronized
    fun flush() {
        apply(sanitizer.flush())
    }

    /** 当前可见文本快照（只读）。 */
    @Synchronized
    fun snapshot(): String {
        cachedSnapshot?.let { return it }

        val builder = StringBuilder(totalChars + lines.size)
        for (line in lines) {
            builder.append(line).append('\n')
        }
        builder.append(currentLine)
        val text = builder.toString()
        val bounded = if (text.length > maxChars) {
            text.substring(text.length - maxChars)
        } else {
            text
        }
        cachedSnapshot = bounded
        return bounded
    }

    private fun apply(update: TerminalOutputUpdate) {
        if (update.clearScreen) {
            lines.clear()
            totalChars = 0
            currentLine = ""
        }

        for (line in update.completedLines) {
            lines.addLast(line)
            totalChars += line.length + 1
        }

        totalChars -= currentLine.length
        currentLine = update.currentLine
        totalChars += currentLine.length

        trimLines()
        if (totalChars > maxChars && currentLine.length > maxChars) {
            currentLine = currentLine.takeLast(maxChars)
            totalChars = currentLine.length
        }
        cachedSnapshot = null
    }

    private fun trimLines() {
        while (totalChars > maxChars && lines.isNotEmpty()) {
            val removed = lines.removeFirst()
            totalChars -= removed.length + 1
        }
        while (lines.size > MAX_LINES) {
            val removed = lines.removeFirst()
            totalChars -= removed.length + 1
        }
    }

    private companion object {
        const val DEFAULT_MAX_CHARS = 50_000
        const val MAX_LINES = 4_000
    }
}
