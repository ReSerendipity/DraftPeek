/*
 * PTY 原始字节 → 可读文本的最小清洗层（阶段 B 用）。
 *
 * 注意：这不是终端模拟器。阶段 C 的 cell buffer / 光标 / 滚动区 / 备用屏幕
 * 尚未实现，这里只做：
 * - UTF-8 增量解码；
 * - 去掉 CSI / OSC 转义（颜色、光标定位等）；
 * - 处理 \r、\b、\t、\n，使 shell 输出不出现控制字符乱码；
 * - 识别 2J/3J、1049h/l 等清屏序列，避免 vim 退出后残留整屏旧帧。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** 一次 feed 的结果。 */
internal data class TerminalOutputUpdate(
    val completedLines: List<String>,
    val currentLine: String,
    val clearScreen: Boolean
)

/**
 * 增量终端输出清洗器。
 *
 * 非线程安全：每条会话只由 reader 线程调用；TerminalTextBuffer 负责加锁。
 */
internal class TerminalOutputSanitizer {

    private enum class State { NORMAL, ESCAPE, CSI, OSC }

    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private var pendingBytes = ByteArray(0)
    private val current = StringBuilder()
    private val completed = ArrayList<String>()
    private var state = State.NORMAL
    private val csi = StringBuilder()
    private val osc = StringBuilder()
    private var oscEscape = false
    private var pendingCarriageReturn = false
    private var clearScreen = false

    fun feed(bytes: ByteArray): TerminalOutputUpdate {
        clearScreen = false
        completed.clear()
        val text = decode(bytes)
        consume(text)
        return snapshotUpdate()
    }

    /** 会话结束时冲刷残留半行 / 不完整 UTF-8。 */
    fun flush(): TerminalOutputUpdate {
        clearScreen = false
        completed.clear()
        if (pendingBytes.isNotEmpty()) {
            val text = pendingBytes.toString(Charsets.UTF_8)
            pendingBytes = ByteArray(0)
            decoder.reset()
            consume(text)
        }
        if (pendingCarriageReturn) {
            pendingCarriageReturn = false
        }
        state = State.NORMAL
        return snapshotUpdate()
    }

    private fun snapshotUpdate(): TerminalOutputUpdate =
        TerminalOutputUpdate(completed.toList(), current.toString(), clearScreen)

    private fun decode(bytes: ByteArray): String {
        val all = if (pendingBytes.isEmpty()) bytes else pendingBytes + bytes
        val byteBuffer = ByteBuffer.wrap(all)
        val charBuffer = CharBuffer.allocate(all.size * 2 + 16)
        decoder.decode(byteBuffer, charBuffer, false)
        charBuffer.flip()
        val text = charBuffer.toString()
        pendingBytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(pendingBytes)
        return text
    }

    private fun consume(text: String) {
        var index = 0
        while (index < text.length) {
            val char = text[index]

            if (state == State.NORMAL && pendingCarriageReturn) {
                pendingCarriageReturn = false
                if (char == '\n') {
                    commitLine()
                    index++
                    continue
                }
                current.setLength(0)
            }

            when (state) {
                State.NORMAL -> when {
                    char == ESCAPE -> state = State.ESCAPE
                    char == '\n' -> commitLine()
                    char == '\r' -> pendingCarriageReturn = true
                    char == '\b' -> if (current.isNotEmpty()) {
                        current.deleteCharAt(current.length - 1)
                    }
                    char == '\t' -> appendTab()
                    char.code < 0x20 || char.code == 0x7F -> Unit
                    else -> appendChar(char)
                }
                State.ESCAPE -> when (char) {
                    '[' -> {
                        csi.setLength(0)
                        state = State.CSI
                    }
                    ']' -> {
                        osc.setLength(0)
                        oscEscape = false
                        state = State.OSC
                    }
                    else -> state = State.NORMAL
                }
                State.CSI -> {
                    if (char in '@'..'~') {
                        handleCsi(csi.toString(), char)
                        state = State.NORMAL
                    } else if (csi.length < MAX_ESCAPE_LENGTH) {
                        csi.append(char)
                    } else {
                        state = State.NORMAL
                    }
                }
                State.OSC -> when {
                    char == '\u0007' -> state = State.NORMAL
                    char == ESCAPE -> oscEscape = true
                    oscEscape && char == '\\' -> state = State.NORMAL
                    else -> {
                        oscEscape = false
                        if (osc.length < MAX_ESCAPE_LENGTH) osc.append(char)
                    }
                }
            }
            index++
        }
    }

    private fun appendChar(char: Char) {
        if (current.length >= MAX_CURRENT_LINE_LENGTH) {
            current.delete(0, current.length - MAX_CURRENT_LINE_LENGTH + 1)
        }
        current.append(char)
    }

    private fun appendTab() {
        val spaces = 8 - (current.length % 8)
        repeat(spaces) { appendChar(' ') }
    }

    private fun commitLine() {
        completed.add(current.toString())
        current.setLength(0)
    }

    /**
     * 处理 CSI：
     * - 2J / 3J 清屏；
     * - 1049h / 1049l（备用屏幕进出）清屏，保证 vim 退出后回到干净的 shell 画面；
     * - K 清当前行；其余（SGR、光标移动、滚动区等）忽略。
     */
    private fun handleCsi(parameters: String, finalChar: Char) {
        when (finalChar) {
            'J' -> if (parameters == "2" || parameters == "3") clearScreen = true
            'K' -> if (parameters.isEmpty() || parameters == "0" || parameters == "1" || parameters == "2") {
                current.setLength(0)
            }
            'h', 'l' -> if (parameters.contains("1049") ||
                parameters.contains("1047") ||
                parameters.contains("47")
            ) {
                clearScreen = true
            }
        }
    }

    private companion object {
        const val MAX_ESCAPE_LENGTH = 256
        const val MAX_CURRENT_LINE_LENGTH = 16_384
        const val ESCAPE = '\u001B'
    }
}
