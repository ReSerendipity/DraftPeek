/**
 * 终端会话管理器文件。
 *
 * 每条终端会话优先使用真 PTY（native openpty + fork + TIOCSCTTY），
 * 使 shell / proot 看到真实 tty：`stty size`、`tput cols`、Ctrl-C、SIGWINCH、
 * vim/top 等全屏程序都依赖它。PTY 不可用时回退到既有 ProcessBuilder 管道，
 * 并在输出区明确提示。
 *
 * 线程模型：
 * - PTY 会话：reader / writer / waiter 三条 daemon 线程（见 TerminalPtySession）；
 * - 管道回退：reader / waiter 两条线程；
 * - 所有 StateFlow 更新在 Main 调度器，IO / 进程管理在 Dispatchers.IO。
 *
 * 作者：DraftPeek Team
 * 起始版本：1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log
import com.draftpeek.feature.terminal.model.TerminalConfig
import com.draftpeek.feature.terminal.model.TerminalSession
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "TerminalSessionManager"
private const val OUTPUT_PUBLISH_INTERVAL_MS = 32L

/**
 * 终端会话管理器。
 *
 * 管理多个终端会话，每个会话有独立 PTY / 进程和 IO 线程。
 * 线程安全：状态更新在 Main 调度器执行，IO 操作在 IO 调度器执行；
 * map 的注册 / 注销用 `@Synchronized` 保证与 destroy 的竞态安全。
 */
@Singleton
class TerminalSessionManager @Inject constructor(private val prootSessionManager: ProotSessionManager) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeSession = MutableStateFlow<TerminalSession?>(null)

    /** 当前活动终端会话流 */
    val activeSession: StateFlow<TerminalSession?> = _activeSession.asStateFlow()

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())

    /** 所有终端会话列表流 */
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    /** Proot 会话管理器，用于可用性检查和 rootfs 解压 */
    val prootManager: ProotSessionManager get() = prootSessionManager

    private val sessionOutputBuffers = ConcurrentHashMap<String, MutableStateFlow<String>>()

    private val textBuffers = ConcurrentHashMap<String, TerminalTextBuffer>()

    private val ptySessions = ConcurrentHashMap<String, TerminalPtySession>()

    private val processSessions = ConcurrentHashMap<String, ProcessSession>()

    private val publishJobs = ConcurrentHashMap<String, Job>()
    private val publishLock = Any()

    /** 最近一次 UI 尺寸，用于新建会话时直接以正确行列启动。 */
    @Volatile
    private var lastColumns: Int = 0

    @Volatile
    private var lastRows: Int = 0

    /**
     * PTY 会话内部数据类，持有 native 会话句柄。
     */
    private data class ProcessSession(
        val process: Process,
        val outputStream: DataOutputStream,
        val readerThread: Thread,
        val waiterThread: Thread
    )

    /**
     * 一次启动的完整命令描述。
     *
     * @property argv 完整 argv（proot 模式含 proot 参数）
     * @property cwd 子进程工作目录
     * @property environment 完整环境变量
     * @property notices 启动前要写入输出区的提示（降级原因等）
     */
    private data class LaunchSpec(
        val argv: List<String>,
        val cwd: String,
        val environment: Map<String, String>,
        val notices: List<String>
    )

    /**
     * 获取会话的输出缓冲区流。
     * @param sessionId 会话ID
     * @return 输出内容的StateFlow
     */
    fun getOutputBuffer(sessionId: String): StateFlow<String> =
        sessionOutputBuffers.getOrPut(sessionId) { MutableStateFlow("") }.asStateFlow()

    /**
     * 创建新的终端会话。
     * @param config 终端配置
     * @param title 会话标题
     * @return 新创建的终端会话
     */
    fun createSession(config: TerminalConfig = TerminalConfig(), title: String? = null): TerminalSession {
        val sessionTitle = title ?: "Shell #${_sessions.value.size + 1}"
        val columns = lastColumns.takeIf { it > 0 } ?: config.columns
        val rows = lastRows.takeIf { it > 0 } ?: config.rows
        val effectiveConfig = config.copy(columns = columns, rows = rows)
        val session = TerminalSession(
            id = UUID.randomUUID().toString(),
            config = effectiveConfig,
            title = sessionTitle,
            workingDirectory = config.workingDirectory
        )

        sessionOutputBuffers[session.id] = MutableStateFlow("")
        textBuffers[session.id] = TerminalTextBuffer()

        scope.launch(Dispatchers.Main.immediate) {
            _sessions.value = _sessions.value + session
            _activeSession.value = session
        }

        startSession(session.id, effectiveConfig)
        return session
    }

    /**
     * 向指定会话发送输入。
     * @param sessionId 会话ID
     * @param input 输入字符串（UTF-8 编码后按原始字节写入）
     */
    fun sendInput(sessionId: String, input: String) {
        val bytes = input.toByteArray(Charsets.UTF_8)
        scope.launch(Dispatchers.IO) {
            ptySessions[sessionId]?.let { session ->
                session.write(bytes)
                return@launch
            }

            try {
                val ps = processSessions[sessionId] ?: return@launch
                ps.outputStream.write(bytes)
                ps.outputStream.flush()
            } catch (error: Exception) {
                Log.w(TAG, "向会话 $sessionId 发送输入失败", error)
            }
        }
    }

    /**
     * 向当前活动会话发送输入。
     * @param input 输入字符串
     */
    fun sendInput(input: String) {
        val activeId = _activeSession.value?.id ?: return
        sendInput(activeId, input)
    }

    /**
     * 向当前活动会话发送特殊按键事件。
     * @param keyCode 终端按键枚举
     */
    fun sendKeyEvent(keyCode: TerminalKey) {
        val bytes = when (keyCode) {
            TerminalKey.ENTER -> "\r"
            TerminalKey.TAB -> "\t"
            TerminalKey.BACKSPACE -> "\b"
            TerminalKey.ESC -> "\u001B"
            TerminalKey.CTRL_C -> "\u0003"
            TerminalKey.CTRL_D -> "\u0004"
            TerminalKey.CTRL_Z -> "\u001A"
            TerminalKey.ARROW_UP -> "\u001B[A"
            TerminalKey.ARROW_DOWN -> "\u001B[B"
            TerminalKey.ARROW_RIGHT -> "\u001B[C"
            TerminalKey.ARROW_LEFT -> "\u001B[D"
        }
        sendInput(bytes)
    }

    /**
     * 调整当前活动会话的终端大小。
     *
     * 由 UI 在旋转 / 分屏 / 键盘弹出导致输出区尺寸变化时调用；
     * PTY 会话走 `ioctl(TIOCSWINSZ)`，内核自动向前台进程组发 SIGWINCH。
     */
    fun resize(columns: Int, rows: Int) {
        _activeSession.value?.id?.let { resize(it, columns, rows) }
    }

    /**
     * 调整指定会话的终端大小。
     */
    fun resize(sessionId: String, columns: Int, rows: Int) {
        if (columns <= 0 || rows <= 0) return
        lastColumns = columns
        lastRows = rows
        ptySessions[sessionId]?.resize(columns, rows)
    }

    /**
     * 销毁指定会话。幂等：重复调用不会二次关闭 fd / 二次 kill。
     * @param sessionId 要销毁的会话ID
     */
    @Synchronized
    fun destroySession(sessionId: String) {
        val pty = ptySessions.remove(sessionId)
        val processSession = processSessions.remove(sessionId)

        pty?.destroy()
        if (processSession != null) {
            scope.launch(Dispatchers.IO) { destroyPipe(processSession) }
        }

        publishJobs.remove(sessionId)?.cancel()
        sessionOutputBuffers.remove(sessionId)
        textBuffers.remove(sessionId)

        scope.launch(Dispatchers.Main.immediate) {
            val currentList = _sessions.value.filter { it.id != sessionId }
            _sessions.value = currentList

            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = currentList.lastOrNull()
            }
        }
    }

    /**
     * 切换到指定会话。
     * @param sessionId 要切换到的会话ID
     */
    fun switchSession(sessionId: String) {
        val session = _sessions.value.find { it.id == sessionId } ?: return
        _activeSession.value = session
    }

    /**
     * 销毁所有会话并清理资源。
     *
     * `scope.cancelChildren()` 只取消当前子协程，保留 scope 供服务重启后再次 createSession；
     * 这是原实现的既有修复，PTY 改造保持不变。destroy 幂等，可重复调用。
     */
    @Synchronized
    fun destroyAll() {
        ptySessions.keys.toList().forEach { ptySessions.remove(it)?.destroy() }
        processSessions.keys.toList().forEach { key ->
            processSessions.remove(key)?.let { destroyPipe(it) }
        }
        publishJobs.values.forEach { it.cancel() }
        publishJobs.clear()
        sessionOutputBuffers.clear()
        textBuffers.clear()
        _sessions.value = emptyList()
        _activeSession.value = null
        scope.coroutineContext.cancelChildren()
    }

    /**
     * 启动会话：优先 PTY，失败则回退 ProcessBuilder。
     */
    private fun startSession(sessionId: String, config: TerminalConfig) {
        if (!isSessionAlive(sessionId)) return

        scope.launch(Dispatchers.IO) {
            try {
                val spec = buildLaunchSpec(config)
                spec.notices.forEach { feedText(sessionId, it) }

                if (TerminalPtyNative.isAvailable) {
                    try {
                        val pty = TerminalPtySession(
                            sessionId = sessionId,
                            argv = spec.argv,
                            cwd = spec.cwd,
                            environment = spec.environment,
                            rows = config.rows,
                            columns = config.columns,
                            onOutput = { bytes -> feedOutput(sessionId, bytes) },
                            onExit = { exitCode -> handleExit(sessionId, exitCode) }
                        )
                        if (registerPty(sessionId, pty)) return@launch
                        pty.destroy()
                        return@launch
                    } catch (error: Throwable) {
                        Log.w(TAG, "会话 $sessionId PTY 启动失败，回退管道", error)
                        feedText(
                            sessionId,
                            "[PTY 启动失败：${error.message.orEmpty()}，已回退到管道模式]\n"
                        )
                    }
                } else {
                    feedText(
                        sessionId,
                        "[PTY 不可用：${TerminalPtyNative.unavailableReason()}，已回退到管道模式]\n"
                    )
                }

                if (!isSessionAlive(sessionId)) return@launch
                startPipeProcess(sessionId, spec)
            } catch (error: Throwable) {
                Log.e(TAG, "会话 $sessionId 启动失败", error)
                feedText(sessionId, "启动shell错误: ${error.message.orEmpty()}\n")
            }
        }
    }

    /**
     * 构造启动命令：proot 可用时把 proot 作为 PTY 子进程执行；
     * 否则执行 Android 原生 shell。
     */
    private suspend fun buildLaunchSpec(config: TerminalConfig): LaunchSpec {
        val workDir = File(config.workingDirectory).takeIf { it.isDirectory } ?: File("/sdcard")
        val notices = mutableListOf<String>()

        if (config.useProot) {
            try {
                val availability = prootSessionManager.checkProotAvailable()
                if (availability.isReady) {
                    val prootPath = availability.prootPath!!
                    val rootfsPath = availability.rootfsPath!!
                    return LaunchSpec(
                        argv = buildProotCommand(
                            prootPath = prootPath,
                            rootfsPath = rootfsPath,
                            cwd = config.workingDirectory,
                            shell = "/bin/bash"
                        ),
                        cwd = rootfsPath,
                        environment = prootEnvironment(config),
                        notices = emptyList()
                    )
                }
                notices.add("[Proot 未就绪，已回退到原生 shell]\n")
            } catch (error: Exception) {
                notices.add("[Proot 启动失败，已回退到原生 shell：${error.message.orEmpty()}]\n")
                Log.w(TAG, "Proot 不可用，回退原生 shell", error)
            }
        }

        return LaunchSpec(
            argv = listOf(config.shell),
            cwd = workDir.absolutePath,
            environment = nativeEnvironment(config, workDir),
            notices = notices
        )
    }

    /** 原生 shell 环境：继承配置，补 HOME / PWD / TERM / LANG / PATH。 */
    private fun nativeEnvironment(config: TerminalConfig, workDir: File): Map<String, String> {
        val environment = LinkedHashMap<String, String>()
        environment.putAll(config.environment)
        environment.putIfAbsent("TERM", "xterm-256color")
        environment.putIfAbsent("LANG", "en_US.UTF-8")
        environment.putIfAbsent("PATH", "/system/bin:/system/xbin")
        environment["HOME"] = workDir.absolutePath
        environment["PWD"] = workDir.absolutePath
        return environment
    }

    /**
     * Proot 环境：PATH / HOME 必须指向 rootfs 语义，不能被默认配置里的
     * Android PATH 覆盖（否则 proot 内的 shell 找不到 /usr/bin 工具）。
     */
    private fun prootEnvironment(config: TerminalConfig): Map<String, String> {
        val environment = LinkedHashMap<String, String>()
        environment["HOME"] = "/root"
        environment["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        environment["TERM"] = "xterm-256color"
        environment["LANG"] = "en_US.UTF-8"
        environment["PROOT_NO_SECCOMP"] = "1"
        config.environment.forEach { (key, value) ->
            if (key != "PATH" && key != "HOME") {
                environment[key] = value
            }
        }
        return environment
    }

    /**
     * 注册 PTY 会话；若期间会话已被销毁则返回 false。
     */
    @Synchronized
    private fun registerPty(sessionId: String, pty: TerminalPtySession): Boolean {
        if (!isSessionAlive(sessionId)) return false
        ptySessions[sessionId] = pty
        return true
    }

    /**
     * 启动 ProcessBuilder 管道回退（无 PTY 语义，仅保证旧功能可用）。
     */
    @Synchronized
    private fun startPipeProcess(sessionId: String, spec: LaunchSpec) {
        if (!isSessionAlive(sessionId)) return

        try {
            val builder = ProcessBuilder(spec.argv)
                .directory(File(spec.cwd))
                .redirectErrorStream(true)
            builder.environment().apply {
                clear()
                putAll(spec.environment)
            }
            val process = builder.start()
            val outputStream = DataOutputStream(process.outputStream)

            val readerThread = Thread({
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        reader.forEachLine { line -> feedText(sessionId, line + "\n") }
                    }
                } catch (error: Exception) {
                    Log.d(TAG, "会话 $sessionId 管道输出结束: ${error.message}")
                }
            }, "pipe-reader-$sessionId").apply { isDaemon = true }

            val waiterThread = Thread({
                try {
                    val exitCode = process.waitFor()
                    feedText(sessionId, "\n[进程退出，代码 $exitCode]\n")
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }, "pipe-waiter-$sessionId").apply { isDaemon = true }

            val processSession = ProcessSession(process, outputStream, readerThread, waiterThread)
            processSessions[sessionId] = processSession

            if (!isSessionAlive(sessionId)) {
                processSessions.remove(sessionId)
                destroyPipe(processSession)
                return
            }

            readerThread.start()
            waiterThread.start()
            Log.i(TAG, "会话 $sessionId 使用管道回退启动 argv=${spec.argv.joinToString(" ")}")
        } catch (error: Exception) {
            Log.e(TAG, "会话 $sessionId 启动管道进程失败", error)
            feedText(sessionId, "启动shell错误: ${error.message.orEmpty()}\n")
        }
    }

    /** 子进程退出：冲刷残留输出，追加退出码，移除 PTY 句柄。 */
    private fun handleExit(sessionId: String, exitCode: Int) {
        textBuffers[sessionId]?.flush()
        feedText(sessionId, "\n[进程退出，代码 $exitCode]\n")
        ptySessions.remove(sessionId)
        Log.d(TAG, "会话 $sessionId 已退出，code=$exitCode")
    }

    /**
     * 构建 proot 命令行。
     *
     * proot 命令格式：
     * ```
     * proot -0 -r <rootfs> --link2symlink --kill-on-exit -b /dev -b /proc -b /sys -b /sdcard [-b <cwd>] /bin/bash
     * ```
     */
    private fun buildProotCommand(prootPath: String, rootfsPath: String, cwd: String, shell: String): List<String> {
        val cmd = mutableListOf(
            prootPath,
            "-0",
            "-r",
            rootfsPath,
            "--link2symlink",
            "--kill-on-exit"
        )

        // 绑定挂载必要的 Android 目录
        cmd.add("-b")
        cmd.add("/dev")
        cmd.add("-b")
        cmd.add("/proc")
        cmd.add("-b")
        cmd.add("/sys")

        // 绑定挂载 /sdcard
        if (File("/sdcard").exists()) {
            cmd.add("-b")
            cmd.add("/sdcard")
        }

        // 绑定挂载工作目录（如果不在 /sdcard 内）
        val cwdFile = File(cwd)
        if (cwdFile.exists() && cwdFile.isDirectory && !cwd.startsWith("/sdcard")) {
            cmd.add("-b")
            cmd.add(cwd)
        }

        // Shell 命令
        cmd.add(shell)

        Log.d(TAG, "Proot命令: ${cmd.joinToString(" ")}")
        return cmd
    }

    /**
     * 销毁管道回退进程并清理资源。
     */
    private fun destroyPipe(ps: ProcessSession) {
        try {
            ps.outputStream.close()
        } catch (_: Exception) {
        }

        ps.readerThread.interrupt()
        ps.waiterThread.interrupt()

        try {
            ps.process.destroy()
            val exited = ps.process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!exited) {
                ps.process.destroyForcibly()
            }
        } catch (error: Exception) {
            Log.w(TAG, "销毁管道会话进程错误", error)
            try {
                ps.process.destroyForcibly()
            } catch (_: Exception) {
            }
        }
    }

    /** 会话是否仍然存在（未被 destroy）。 */
    private fun isSessionAlive(sessionId: String): Boolean = sessionOutputBuffers.containsKey(sessionId)

    /** 追加原始字节到会话文本缓冲。 */
    private fun feedOutput(sessionId: String, bytes: ByteArray) {
        val buffer = textBuffers[sessionId] ?: return
        buffer.feed(bytes)
        schedulePublish(sessionId)
    }

    /** 追加纯文本（用于提示 / 退出码）。 */
    private fun feedText(sessionId: String, text: String) {
        feedOutput(sessionId, text.toByteArray(Charsets.UTF_8))
    }

    /**
     * 32ms 合并发布一次快照，避免逐字节输出触发 O(n²) 字符串拼接与高频重组。
     */
    private fun schedulePublish(sessionId: String) {
        synchronized(publishLock) {
            if (publishJobs[sessionId]?.isActive == true) return
            val job = scope.launch(Dispatchers.Main.immediate) {
                delay(OUTPUT_PUBLISH_INTERVAL_MS)
                val text = textBuffers[sessionId]?.snapshot()
                if (text != null) {
                    sessionOutputBuffers[sessionId]?.value = text
                }
                synchronized(publishLock) { publishJobs.remove(sessionId) }
            }
            publishJobs[sessionId] = job
        }
    }
}

/**
 * 终端特殊按键枚举。
 */
enum class TerminalKey {
    /** 回车键 */
    ENTER,

    /** Tab键 */
    TAB,

    /** 退格键 */
    BACKSPACE,

    /** ESC键 */
    ESC,

    /** Ctrl+C中断 */
    CTRL_C,

    /** Ctrl+D EOF */
    CTRL_D,

    /** Ctrl+Z挂起 */
    CTRL_Z,

    /** 上箭头 */
    ARROW_UP,

    /** 下箭头 */
    ARROW_DOWN,

    /** 左箭头 */
    ARROW_LEFT,

    /** 右箭头 */
    ARROW_RIGHT
}
