/*
 * 单条真 PTY 会话。
 *
 * 每条会话持有 master fd、子进程 pid，以及三条 daemon 线程：
 * - reader：阻塞 readFd，把原始 ByteArray 交给上层；
 * - writer：串行消费输入队列，按字节写 fd；
 * - waiter：阻塞 waitFor(pid)，回收子进程并上报退出码。
 *
 * 生命周期约定：
 * - fd 由 [closeMasterOnce] 保证只关闭一次，避免 fd 复用后误关其它文件；
 * - [destroy] 幂等：close(fd) -> SIGHUP -> 200ms 后 SIGKILL，waitpid 始终由 waiter 完成；
 * - 子进程退出后 readFd 返回 EIO，reader 视为正常结束，不抛出、不刷错误。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "TerminalPtySession"

/** Linux errno：EIO，子进程退出后读 master fd 的常见返回。 */
private const val ERRNO_EIO = 5

/** Linux errno：EBADF，fd 已关闭。 */
private const val ERRNO_EBADF = 9

/** Linux errno：EINTR，被信号打断，可重试。 */
private const val ERRNO_EINTR = 4

/** Linux 信号：SIGHUP。 */
private const val SIGNAL_HUP = 1

/** Linux 信号：SIGKILL。 */
private const val SIGNAL_KILL = 9

/**
 * 真 PTY 会话。
 *
 * @param sessionId 会话 ID，仅用于日志与线程命名
 * @param argv 子进程 argv
 * @param cwd 子进程工作目录
 * @param environment 完整环境变量
 * @param rows 初始行数
 * @param columns 初始列数
 * @param onOutput 原始字节回调（由 reader 线程调用，不得阻塞）
 * @param onExit 子进程退出回调（由 waiter 线程调用，只调用一次）
 */
internal class TerminalPtySession(
    private val sessionId: String,
    argv: List<String>,
    cwd: String,
    environment: Map<String, String>,
    rows: Int,
    columns: Int,
    private val onOutput: (ByteArray) -> Unit,
    private val onExit: (Int) -> Unit
) {

    private val masterFd: Int
    private val pid: Int

    private val closed = AtomicBoolean(false)
    private val destroyStarted = AtomicBoolean(false)
    private val masterClosed = AtomicBoolean(false)
    private val exitNotified = AtomicBoolean(false)

    private val writeQueue = LinkedBlockingQueue<ByteArray>(WRITE_QUEUE_CAPACITY)

    private val readerThread: Thread
    private val writerThread: Thread
    private val waiterThread: Thread

    init {
        val result = try {
            TerminalPtyNative.createPty(
                argv = argv.toTypedArray(),
                cwd = cwd,
                env = environment.map { "${it.key}=${it.value}" }.toTypedArray(),
                rows = rows,
                columns = columns
            )
        } catch (error: Throwable) {
            throw IOException("createPty 失败: ${error.message}", error)
        }

        if (result.size < 2 || result[0] < 0 || result[1] <= 0) {
            throw IOException("createPty 返回非法 fd/pid: ${result.joinToString(",")}")
        }
        masterFd = result[0]
        pid = result[1]

        readerThread = Thread({ runReader() }, "pty-reader-$sessionId").apply { isDaemon = true }
        writerThread = Thread({ runWriter() }, "pty-writer-$sessionId").apply { isDaemon = true }
        waiterThread = Thread({ runWaiter() }, "pty-waiter-$sessionId").apply { isDaemon = true }

        readerThread.start()
        writerThread.start()
        waiterThread.start()

        Log.i(TAG, "PTY 会话已启动 session=$sessionId fd=$masterFd pid=$pid size=${columns}x$rows")
    }

    /** 当前是否已关闭（销毁或子进程退出后读端关闭）。 */
    val isClosed: Boolean get() = closed.get()

    /**
     * 写入原始字节。队列满时丢弃本次输入并返回 false，避免 UI 线程被阻塞。
     */
    fun write(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        if (closed.get()) {
            Log.w(TAG, "会话 $sessionId 已关闭，丢弃输入 ${bytes.size}B")
            return false
        }
        val accepted = writeQueue.offer(bytes)
        if (!accepted) {
            Log.w(TAG, "会话 $sessionId 输入队列已满，丢弃 ${bytes.size}B")
        }
        return accepted
    }

    /**
     * 调整 PTY 窗口大小。失败只记日志；UI 尺寸变化频繁，不向用户弹错。
     */
    fun resize(columns: Int, rows: Int) {
        if (closed.get() || columns <= 0 || rows <= 0) return
        if (!TerminalPtyNative.resize(masterFd, rows, columns)) {
            Log.w(TAG, "会话 $sessionId resize(${columns}x$rows) 失败")
        }
    }

    /**
     * 销毁会话。幂等、非阻塞：先 close(fd)，再 SIGHUP，200ms 后仍未退出则 SIGKILL。
     * waitpid 由 waiter 线程完成，调用方（可能是主线程）不会被阻塞。
     */
    fun destroy() {
        if (!destroyStarted.compareAndSet(false, true)) {
            Log.d(TAG, "会话 $sessionId 重复 destroy，已忽略")
            return
        }

        closed.set(true)
        closeMasterOnce()

        val hungUp = TerminalPtyNative.signalProcessGroup(pid, SIGNAL_HUP)
        Log.d(TAG, "会话 $sessionId 销毁: close(fd) + SIGHUP=$hungUp pid=$pid")

        Thread({
            try {
                waiterThread.join(DESTROY_GRACE_MILLIS)
                if (waiterThread.isAlive) {
                    TerminalPtyNative.signalProcessGroup(pid, SIGNAL_KILL)
                    Log.w(TAG, "会话 $sessionId SIGHUP 后未退出，已 SIGKILL")
                }
                waiterThread.join(WAIT_RECLAIM_MILLIS)
                if (waiterThread.isAlive) {
                    Log.e(TAG, "会话 $sessionId waiter 未能回收 pid=$pid，可能残留僵尸进程")
                }
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }, "pty-kill-$sessionId").apply { isDaemon = true }.start()
    }

    /** reader：原始字节 → 上层；EIO / EBADF 视为正常结束。 */
    private fun runReader() {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        try {
            while (!closed.get()) {
                val count = TerminalPtyNative.readFd(masterFd, buffer, 0, buffer.size)
                when {
                    count > 0 -> onOutput(buffer.copyOf(count))
                    count == 0 -> {
                        Log.d(TAG, "会话 $sessionId read 返回 0，读端结束")
                        break
                    }
                    else -> {
                        val errno = -count
                        if (errno == ERRNO_EINTR) continue
                        if (errno == ERRNO_EIO || errno == ERRNO_EBADF) {
                            Log.d(TAG, "会话 $sessionId read 结束 errno=$errno")
                        } else {
                            Log.w(TAG, "会话 $sessionId read 失败 errno=$errno")
                        }
                        break
                    }
                }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "会话 $sessionId reader 异常", error)
        } finally {
            closeMasterOnce()
        }
    }

    /** writer：串行写 fd，保证多线程 sendInput 的顺序与字节完整性。 */
    private fun runWriter() {
        try {
            while (!closed.get()) {
                val bytes = writeQueue.poll(WRITE_POLL_MILLIS, TimeUnit.MILLISECONDS) ?: continue
                var offset = 0
                while (offset < bytes.size) {
                    if (closed.get()) return
                    val written = TerminalPtyNative.writeFd(
                        masterFd,
                        bytes,
                        offset,
                        bytes.size - offset
                    )
                    if (written <= 0) {
                        Log.w(TAG, "会话 $sessionId write 失败 errno=${-written}")
                        return
                    }
                    offset += written
                }
            }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Throwable) {
            Log.w(TAG, "会话 $sessionId writer 异常", error)
        }
    }

    /** waiter：waitpid 回收子进程，单次上报退出码。 */
    private fun runWaiter() {
        val exitCode = try {
            TerminalPtyNative.waitFor(pid)
        } catch (error: Throwable) {
            Log.w(TAG, "会话 $sessionId waitFor 异常", error)
            -1
        }
        closeMasterOnce()
        closed.set(true)
        if (exitNotified.compareAndSet(false, true)) {
            Log.i(TAG, "会话 $sessionId 进程退出 code=$exitCode")
            onExit(exitCode)
        }
    }

    /**
     * 关闭 master fd 一次。close 与 reader 的阻塞 read 可能并发：
     * reader 的 in-flight read 持有旧文件引用，返回后循环会因 closed=true 退出，
     * 不会用已复用的 fd 号继续读。
     */
    private fun closeMasterOnce() {
        if (masterClosed.compareAndSet(false, true)) {
            TerminalPtyNative.closeFd(masterFd)
        }
    }

    private companion object {
        const val READ_BUFFER_SIZE = 8192
        const val WRITE_QUEUE_CAPACITY = 64
        const val WRITE_POLL_MILLIS = 100L
        const val DESTROY_GRACE_MILLIS = 200L
        const val WAIT_RECLAIM_MILLIS = 1000L
    }
}
