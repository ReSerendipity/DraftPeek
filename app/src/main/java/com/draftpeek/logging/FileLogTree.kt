/**
 * DraftPeek 日志持久化工具。
 *
 * 基于 Timber 的 FileLogTree：
 * - debug 构建：仅输出到 Logcat（Timber.DebugTree）
 * - release 构建：写入应用私有目录 logs/draftpeek.log，按 1MB 轮转保留 5 个备份
 *
 * 日志格式：时间戳 + 级别 + 线程ID + 文件:行号 + 消息
 */
package com.draftpeek.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

/** 单个日志文件最大字节数（1MB） */
private const val MAX_FILE_BYTES = 1024 * 1024

/** 轮转备份数量（保留 app.log.1 ~ app.log.5） */
private const val BACKUP_COUNT = 5

/** 时间戳格式 */
private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

/**
 * 将日志写入文件，支持按文件大小轮转归档。
 *
 * [Timber.Tree] 的 [Timber.Tree.log] 回调在 debug 级别也会触发，
 * 因此这里按优先级过滤：仅记录 DEBUG 及以上（对应 Android Log.d）。
 */
class FileLogTree(context: Context, logFileName: String = "draftpeek.log") : Timber.Tree() {

    private val logDir = File(context.filesDir, "logs").apply { mkdirs() }
    private val logFile = File(logDir, logFileName)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.DEBUG) return

        val level = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }

        val timestamp = TIMESTAMP_FORMAT.format(Date())
        val threadId = Thread.currentThread().id
        val threadName = Thread.currentThread().name
        val origin = extractOrigin(t)
        val line = "[$timestamp] [$level] [TID:$threadId($threadName)] [$origin] $message"

        writeLine(line)
        if (t != null) {
            writeLine("  ${Log.getStackTraceString(t).replace("\n", "\n  ")}")
        }
    }

    /** 从 Throwable 的堆栈帧中提取调用来源（文件:行号），无堆栈时回退到 tag。 */
    private fun extractOrigin(t: Throwable?): String {
        val frames = t?.stackTrace
        if (!frames.isNullOrEmpty()) {
            val frame = frames[0]
            return "${frame.fileName}:${frame.lineNumber}"
        }
        return "unknown"
    }

    /** 追加一行日志，超出大小上限时轮转归档。 */
    private fun writeLine(line: String) {
        try {
            if (logFile.exists() && logFile.length() + line.length > MAX_FILE_BYTES) {
                rotate()
            }
            logFile.appendText(line + "\n")
        } catch (_: IOException) {
            // 忽略写入失败，避免日志 I/O 异常影响主流程
        }
    }

    /** 轮转：app.log.1 → app.log.2 → ... → app.log.N，最新的备份移到 .1。 */
    private fun rotate() {
        if (!logFile.exists()) return
        for (i in BACKUP_COUNT - 1 downTo 1) {
            val src = File(logDir, "draftpeek.log.$i")
            val dst = File(logDir, "draftpeek.log.${i + 1}")
            if (src.exists()) {
                src.copyTo(dst, overwrite = true)
                src.delete()
            }
        }
        val firstBackup = File(logDir, "draftpeek.log.1")
        logFile.copyTo(firstBackup, overwrite = true)
        logFile.delete()
    }

    /** 当前日志文件路径，供崩溃上报 / 设置页展示。 */
    fun currentLogFile(): File = logFile
}