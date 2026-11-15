/*
 * 真 PTY native 接口的最薄一层 Kotlin 封装。
 *
 * 只有 native 库加载成功时 [isAvailable] 才为 true；加载失败不会抛出，
 * 由 TerminalSessionManager 回退到 ProcessBuilder 并在输出区提示。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log

/**
 * PTY native 方法声明。
 *
 * 所有 fd / pid 均由 native 侧创建并返回；Kotlin 侧必须保证：
 * - [closeFd] 每个 master fd 只调用一次；
 * - [readFd] / [writeFd] 只在 fd 未关闭时调用；
 * - [waitFor] 只由每条会话的 waiter 线程调用一次。
 */
internal object TerminalPtyNative {

    private const val TAG = "TerminalPtyNative"
    private const val LIBRARY_NAME = "draftpeek_pty"

    /** 加载失败原因；null 表示加载成功。 */
    private val loadFailure: Throwable? = try {
        System.loadLibrary(LIBRARY_NAME)
        Log.i(TAG, "lib$LIBRARY_NAME.so 加载成功")
        null
    } catch (error: Throwable) {
        Log.e(TAG, "lib$LIBRARY_NAME.so 加载失败，PTY 不可用", error)
        error
    }

    /** native 库是否可用。 */
    val isAvailable: Boolean get() = loadFailure == null

    /** 返回加载失败的可读原因；加载成功时返回空串。 */
    fun unavailableReason(): String {
        val failure = loadFailure ?: return ""
        return "${failure.javaClass.simpleName}: ${failure.message.orEmpty()}"
    }

    /**
     * 创建 PTY 并 fork 子进程。
     *
     * @param argv 完整命令 argv，argv[0] 建议为绝对路径
     * @param cwd 子进程工作目录，空串表示继承
     * @param env 完整环境变量数组，格式 `KEY=VALUE`
     * @param rows 终端行数
     * @param columns 终端列数
     * @return `[masterFd, pid]`
     */
    external fun createPty(argv: Array<String>, cwd: String, env: Array<String>, rows: Int, columns: Int): IntArray

    /** 通过 TIOCSWINSZ 调整窗口大小。 */
    external fun resize(masterFd: Int, rows: Int, columns: Int): Boolean

    /** 阻塞 waitpid，返回退出码。 */
    external fun waitFor(pid: Int): Int

    /** 关闭 master fd。 */
    external fun closeFd(masterFd: Int)

    /** 阻塞读原始字节；失败返回 `-errno`。 */
    external fun readFd(masterFd: Int, buffer: ByteArray, offset: Int, length: Int): Int

    /** 写原始字节；失败返回 `-errno`。 */
    external fun writeFd(masterFd: Int, buffer: ByteArray, offset: Int, length: Int): Int

    /** 向 pid 所在进程组发送信号。 */
    external fun signalProcessGroup(pid: Int, signal: Int): Boolean
}
