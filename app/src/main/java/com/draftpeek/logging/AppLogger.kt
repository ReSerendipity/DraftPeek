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

    /**
     * 初始化 Timber + 文件持久化 + 全局崩溃捕获。
     *
     * @param context 应用上下文。
     * @param isDebug 是否 debug 构建。debug 输出 DEBUG 级到 Logcat；release 仅文件记录 INFO+。
     * @param versionCode 当前版本号，用于崩溃指标按版本统计。
     */
    fun init(context: Context, isDebug: Boolean, versionCode: Int = 0) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true

            val app = context.applicationContext
            val fileTree = FileLogTree(app)
            errorLogFile = File(app.filesDir, "logs/draftpeek_error.log")

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
    fun currentErrorLogFile(): File? =
        if (::errorLogFile.isInitialized) errorLogFile else null

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
                errorLogFile.apply {
                    parentFile?.mkdirs()
                    appendText(sb.toString())
                }
            } catch (_: Throwable) {
                // 崩溃日志写入失败时绝不能再次抛出
            }
            previous?.uncaughtException(thread, throwable) ?: run {
                // 无上一级处理器时，终止进程避免停留在崩溃状态
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}