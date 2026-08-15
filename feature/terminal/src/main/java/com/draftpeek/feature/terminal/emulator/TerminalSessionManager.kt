/**
 * 终端会话管理器文件。
 *
 * 使用Android Process API管理终端会话，每个会话拥有独立的shell进程、IO流和读取线程。
 * 所有MutableStateFlow状态更新限制在Main调度器以保证线程安全，IO和进程管理在Dispatchers.IO上运行。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log
import com.draftpeek.feature.terminal.model.TerminalConfig
import com.draftpeek.feature.terminal.model.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TerminalSessionManager"
private const val OUTPUT_BUFFER_MAX = 50_000
private const val OUTPUT_BUFFER_KEEP = 40_000

/**
 * 终端会话管理器。
 *
 * 管理多个终端会话，每个会话有独立的shell进程和IO流。
 * 提供基础的Android原生shell会话支持，不依赖proot。
 *
 * 线程安全：所有状态更新在Main调度器执行，IO操作在IO调度器执行。
 */
@Singleton
class TerminalSessionManager @Inject constructor(
    private val prootSessionManager: ProotSessionManager,
) {

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

    /**
     * 进程会话内部数据类，持有进程和相关流/线程。
     */
    private data class ProcessSession(
        val process: Process,
        val outputStream: DataOutputStream,
        val readerThread: Thread,
        val stdoutThread: Thread,
        val stderrThread: Thread,
    )

    private val processSessions = ConcurrentHashMap<String, ProcessSession>()

    /**
     * 获取会话的输出缓冲区流。
     * @param sessionId 会话ID
     * @return 输出内容的StateFlow
     */
    fun getOutputBuffer(sessionId: String): StateFlow<String> {
        return sessionOutputBuffers.getOrPut(sessionId) { MutableStateFlow("") }.asStateFlow()
    }

    /**
     * 创建新的终端会话。
     * @param config 终端配置，使用默认配置
     * @param title 会话标题
     * @return 新创建的终端会话
     */
    fun createSession(config: TerminalConfig = TerminalConfig(), title: String? = null): TerminalSession {
        val sessionTitle = title ?: "Shell #${_sessions.value.size + 1}"
        val session = TerminalSession(
            id = UUID.randomUUID().toString(),
            config = config,
            title = sessionTitle,
            workingDirectory = config.workingDirectory,
        )

        sessionOutputBuffers[session.id] = MutableStateFlow("")

        scope.launch(Dispatchers.Main.immediate) {
            _sessions.value = _sessions.value + session
            _activeSession.value = session
        }

        startProcess(session.id, config)
        return session
    }

    /**
     * 向指定会话发送输入。
     * @param sessionId 会话ID
     * @param input 输入字符串
     */
    fun sendInput(sessionId: String, input: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val ps = processSessions[sessionId] ?: return@launch
                ps.outputStream.writeBytes(input)
                ps.outputStream.flush()
            } catch (e: Exception) {
                Log.w(TAG, "向会话 $sessionId 发送输入失败", e)
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
     * 调整终端大小（当前未实现）。
     */
    fun resize(columns: Int, rows: Int) {
    }

    /**
     * 销毁指定会话。
     * @param sessionId 要销毁的会话ID
     */
    fun destroySession(sessionId: String) {
        destroyProcess(sessionId)

        scope.launch(Dispatchers.Main.immediate) {
            val currentList = _sessions.value.filter { it.id != sessionId }
            _sessions.value = currentList

            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = currentList.lastOrNull()
            }
        }

        sessionOutputBuffers.remove(sessionId)
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
     * 修复：原实现调用 `scope.cancel()` 会永久取消该 CoroutineScope。由于本类为 @Singleton，
     * 当 TerminalService.onDestroy 调用后，若用户重新打开终端，已死的 scope 会使后续
     * createSession 中的 launch 静默失败（进程启动但无人管理，可能产生僵尸进程）。
     * 改为 `cancelChildren()` 仅取消当前运行中的子协程（进程已在上方销毁），
     * 保留 scope 供未来会话复用。
     */
    fun destroyAll() {
        processSessions.keys.toList().forEach { destroyProcess(it) }
        processSessions.clear()
        sessionOutputBuffers.clear()
        _sessions.value = emptyList()
        _activeSession.value = null
        scope.coroutineContext.cancelChildren()
    }

    /**
     * 启动shell进程。
     *
     * 当 [TerminalConfig.useProot] 为 true 且 proot 可用时，使用 proot 包装的 Linux shell；
     * 否则回退到 Android 原生 shell 并在输出区提示。
     */
    private fun startProcess(sessionId: String, config: TerminalConfig) {
        destroyProcess(sessionId)

        scope.launch(Dispatchers.IO) {
            try {
                val workDir = File(config.workingDirectory).takeIf { it.isDirectory }
                    ?: File("/sdcard")

                // 尝试使用 proot 模式
                var process: Process? = null
                var prootFallback = false

                if (config.useProot) {
                    try {
                        val availability = prootSessionManager.checkProotAvailable()
                        if (availability.isReady) {
                            val prootPath = availability.prootPath!!
                            val rootfsPath = availability.rootfsPath!!
                            val prootCmd = buildProotCommand(
                                prootPath = prootPath,
                                rootfsPath = rootfsPath,
                                cwd = config.workingDirectory,
                                shell = "/bin/bash",
                            )

                            val processBuilder = ProcessBuilder(prootCmd)
                                .directory(File(rootfsPath))
                                .redirectErrorStream(true)

                            val processEnv = processBuilder.environment()
                            processEnv["HOME"] = "/root"
                            processEnv["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                            processEnv["TERM"] = "xterm-256color"
                            processEnv["LANG"] = "en_US.UTF-8"
                            processEnv["PROOT_NO_SECCOMP"] = "1"
                            config.environment.forEach { (key, value) -> processEnv[key] = value }

                            process = processBuilder.start()
                            Log.d(TAG, "会话 $sessionId 使用 proot 模式启动")
                        } else {
                            prootFallback = true
                            Log.w(TAG, "Proot 未就绪，回退到原生 shell")
                        }
                    } catch (e: Exception) {
                        prootFallback = true
                        Log.w(TAG, "Proot 启动失败，回退到原生 shell", e)
                    }
                }

                // 回退到原生 shell
                if (process == null) {
                    val envArray = config.environment.map { "${it.key}=${it.value}" }.toTypedArray()
                    process = Runtime.getRuntime().exec(
                        arrayOf(config.shell),
                        envArray,
                        workDir
                    )
                    if (prootFallback) {
                        val buffer = sessionOutputBuffers.getOrPut(sessionId) { MutableStateFlow("") }
                        scope.launch(Dispatchers.Main.immediate) {
                            buffer.value = buffer.value + "[Proot 不可用，已回退到原生 shell]\n"
                        }
                    }
                }

                val outputStream = DataOutputStream(process!!.outputStream)

                val buffer = sessionOutputBuffers.getOrPut(sessionId) { MutableStateFlow("") }

                fun appendOutput(text: String) {
                    scope.launch(Dispatchers.Main.immediate) {
                        val current = buffer.value
                        val newBuffer = if (current.length > OUTPUT_BUFFER_MAX) {
                            current.substring(current.length - OUTPUT_BUFFER_KEEP) + text
                        } else {
                            current + text
                        }
                        buffer.value = newBuffer
                    }
                }

                val stdoutThread = Thread {
                    try {
                        BufferedReader(InputStreamReader(process!!.inputStream)).use { reader ->
                            reader.forEachLine { line ->
                                appendOutput(line + "\n")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "会话 $sessionId stdout读取结束: ${e.message}")
                    }
                }.apply { name = "stdout-$sessionId"; isDaemon = true }

                val stderrThread = Thread {
                    try {
                        BufferedReader(InputStreamReader(process!!.errorStream)).use { reader ->
                            reader.forEachLine { line ->
                                appendOutput(line + "\n")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "会话 $sessionId stderr读取结束: ${e.message}")
                    }
                }.apply { name = "stderr-$sessionId"; isDaemon = true }

                val readerThread = Thread {
                    stdoutThread.start()
                    stderrThread.start()
                    try {
                        val exitCode = process!!.waitFor()
                        appendOutput("\n[进程退出，代码 $exitCode]\n")
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "会话 $sessionId 读取线程被中断")
                    } finally {
                        stdoutThread.interrupt()
                        stderrThread.interrupt()
                        process!!.destroyForcibly()
                    }
                }.apply { name = "waiter-$sessionId"; isDaemon = true }

                processSessions[sessionId] = ProcessSession(
                    process = process!!,
                    outputStream = outputStream,
                    readerThread = readerThread,
                    stdoutThread = stdoutThread,
                    stderrThread = stderrThread,
                )

                readerThread.start()

            } catch (e: Exception) {
                Log.e(TAG, "会话 $sessionId 启动进程失败", e)
                val buffer = sessionOutputBuffers.getOrPut(sessionId) { MutableStateFlow("") }
                scope.launch(Dispatchers.Main.immediate) {
                    buffer.value = buffer.value + "启动shell错误: ${e.message}\n"
                }
            }
        }
    }

    /**
     * 构建 proot 命令行。
     *
     * proot 命令格式：
     * ```
     * proot -0 -r <rootfs> --link2symlink --kill-on-exit -b /dev -b /proc -b /sys -b /sdcard [-b <cwd>] /bin/bash
     * ```
     */
    private fun buildProotCommand(
        prootPath: String,
        rootfsPath: String,
        cwd: String,
        shell: String,
    ): List<String> {
        val cmd = mutableListOf(
            prootPath,
            "-0",
            "-r", rootfsPath,
            "--link2symlink",
            "--kill-on-exit",
        )

        // 绑定挂载必要的 Android 目录
        cmd.add("-b"); cmd.add("/dev")
        cmd.add("-b"); cmd.add("/proc")
        cmd.add("-b"); cmd.add("/sys")

        // 绑定挂载 /sdcard
        if (File("/sdcard").exists()) {
            cmd.add("-b"); cmd.add("/sdcard")
        }

        // 绑定挂载工作目录（如果不在 /sdcard 内）
        val cwdFile = File(cwd)
        if (cwdFile.exists() && cwdFile.isDirectory && !cwd.startsWith("/sdcard")) {
            cmd.add("-b"); cmd.add(cwd)
        }

        // Shell 命令
        cmd.add(shell)

        Log.d(TAG, "Proot命令: ${cmd.joinToString(" ")}")
        return cmd
    }

    /**
     * 销毁进程并清理资源。
     */
    private fun destroyProcess(sessionId: String) {
        val ps = processSessions.remove(sessionId) ?: return

        try {
            ps.outputStream.close()
        } catch (_: Exception) {}

        ps.readerThread.interrupt()
        ps.stdoutThread.interrupt()
        ps.stderrThread.interrupt()

        try {
            ps.process.destroy()
            val exited = ps.process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!exited) {
                ps.process.destroyForcibly()
            }
        } catch (e: Exception) {
            Log.w(TAG, "销毁会话 $sessionId 进程错误", e)
            try { ps.process.destroyForcibly() } catch (_: Exception) {}
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
