/**
 * DraftPeek 日志系统初始化入口。
 *
 * 基于 Timber 提供统一日志门面，并按构建类型区分日志策略：
 * - debug：Timber.DebugTree 输出到 Logcat（含 DEBUG 级别）
 * - release：FileLogTree 写入应用私有目录（仅 INFO 及以上，避免敏感调试信息落盘）
 *
 * 同时注册全局未捕获异常处理器，将崩溃堆栈写入专门的 error 日志文件。
 */
package com.draftpeek.logging

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import timber.log.Timber

/**
 * 日志初始化与全局崩溃捕获。
 *
 * 应在 [android.app.Application.onCreate] 中尽早调用。
 */
object AppLogger {

    private const val TAG = "AppLogger"

    /** 错误日志文件（单独采集崩溃堆栈，便于线上排查） */
    private lateinit var errorLogFile: File

    /** 是否已初始化，防止重复调用 */
    private var initialized = false

    /** 远程崩溃上报端点（空串 = 不启用远程上报） */
    private var crashReportUrl: String = ""

    /** 远程上报用单线程执行器（避免崩溃路径上做网络 IO 阻塞主线程） */
    private val reportExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AppLoggerCrashReport").apply { isDaemon = true }
    }

    /**
     * 初始化 Timber + 文件持久化 + 全局崩溃捕获。
     *
     * @param context 应用上下文。
     * @param isDebug 是否 debug 构建。debug 输出 DEBUG 级到 Logcat；release 仅文件记录 INFO+。
     * @param versionCode 当前版本号，用于崩溃指标按版本统计。
     * @param reportUrl 远程崩溃上报端点；非空时崩溃发生后异步上报 error 日志内容。
     */
    fun init(context: Context, isDebug: Boolean, versionCode: Int = 0, reportUrl: String = "") {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true

            val app = context.applicationContext
            val fileTree = FileLogTree(app)
            errorLogFile = File(app.filesDir, "logs/draftpeek_error.log")
            crashReportUrl = reportUrl

            if (isDebug) {
                Timber.plant(Timber.DebugTree())
                Timber.plant(ReleaseFileTree(fileTree))
            } else {
                Timber.plant(ReleaseFileTree(fileTree))
            }

            // 崩溃指标初始化（按版本统计会话数和崩溃数）
            if (versionCode > 0) {
                CrashMetrics.init(app, versionCode)
            }

            // 全局崩溃捕获：记录堆栈并写入 error 日志 + 崩溃指标
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(previous = Thread.getDefaultUncaughtExceptionHandler())
            )
        }
    }

    /** 当前主日志文件（供设置页 / 导出）。 */
    fun currentLogFile(): File? =
        if (::errorLogFile.isInitialized) errorLogFile.parentFile?.resolve("draftpeek.log") else null

    /** 当前错误日志文件。 */
    fun currentErrorLogFile(): File? = if (::errorLogFile.isInitialized) errorLogFile else null

    /**
     * 生产环境文件树：仅记录 INFO 及以上级别到文件，避免敏感调试日志落盘。
     */
    private class ReleaseFileTree(private val fileTree: FileLogTree) : Timber.Tree() {
        override fun isLoggable(priority: Int): Boolean = priority >= Log.INFO
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            fileTree.log(priority, tag, message, t)
        }
    }

    /**
     * 全局未捕获异常处理器。
     *
     * 将崩溃堆栈写入 draftpeek_error.log（含设备信息），随后透传给上一个
     * 处理器（若存在），确保不破坏原有崩溃行为。
     */
    private class CrashHandler(private val previous: Thread.UncaughtExceptionHandler?) :
        Thread.UncaughtExceptionHandler {

        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            try {
                // 记录崩溃指标（崩溃计数 + 每日记录）
                CrashMetrics.recordCrash(throwable)

                val sb = StringBuilder()
                sb.append("========== CRASH ${System.currentTimeMillis()} ==========\n")
                sb.append("Thread: ${thread.name} (id=${thread.id})\n")
                sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})\n")
                sb.append("Message: ${throwable.message}\n")
                sb.append(Log.getStackTraceString(throwable))
                sb.append("\n========== END CRASH ==========\n")
                val crashText = sb.toString()
                errorLogFile.apply {
                    parentFile?.mkdirs()
                    appendText(crashText)
                }

                // T3: 远程崩溃上报（异步，失败绝不抛出）
                if (crashReportUrl.isNotBlank() && crashReportUrl.startsWith("http")) {
                    val endpoint = crashReportUrl
                    reportExecutor.execute {
                        try {
                            postCrashReport(endpoint, thread, throwable, crashText)
                        } catch (_: Throwable) {
                            // 上报失败静默；本地 error 日志仍是兜底
                        }
                    }
                }
            } catch (_: Throwable) {
                // 崩溃日志写入失败时绝不能再次抛出
            }
            previous?.uncaughtException(thread, throwable) ?: run {
                // 无上一级处理器时，终止进程避免停留在崩溃状态
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }

        /** 将崩溃摘要 POST 到远程端点（自建 AppLogger 上报通道）。 */
        private fun postCrashReport(endpoint: String, thread: Thread, throwable: Throwable, crashText: String) {
            val payload = StringBuilder()
            payload.append("{\"thread\":\"")
            payload.append(jsonEscape(thread.name))
            payload.append("\",\"device\":\"")
            payload.append(jsonEscape("${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"))
            payload.append("\",\"exception\":\"")
            payload.append(jsonEscape(throwable.javaClass.name))
            payload.append("\",\"message\":\"")
            payload.append(jsonEscape(throwable.message ?: ""))
            payload.append("\",\"stack\":\"")
            payload.append(jsonEscape(crashText))
            payload.append("\"}")
            val conn = URL(endpoint).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setFixedLengthStreamingMode(payload.length)
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                Log.i(TAG, "Crash report posted to $endpoint -> HTTP $code")
            } finally {
                conn.disconnect()
            }
        }

        private fun jsonEscape(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
                }
            }
        }
    }
}
