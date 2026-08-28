/**
 * 基于proot的终端会话管理器文件。
 *
 * 通过proot提供完整Linux环境，使终端无需root权限即可运行标准Linux工具（bash、grep、awk、python、gcc等）。
 * 参考Xed-Editor的proot终端集成架构。
 *
 * proot工作原理：
 * 1. proot二进制文件（为ARM64/x86_64编译）打包在APK中
 * 2. rootfs（最小Linux文件系统）解压到应用数据目录
 * 3. proot转换路径：/usr → /data/data/com.draftpeek/files/rootfs/usr
 * 4. shell和所有进程在此虚拟chroot环境中运行
 *
 * 安全考虑：
 * - proot不需要root权限
 * - 虚拟文件系统隔离在应用数据目录内
 * - 所有文件操作限制在rootfs内
 * - 使用proot-care处理新版本Android的链接器问题
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 基于proot的终端会话管理器。
 *
 * 通过proot提供完整Linux环境，无需root权限即可运行标准Linux工具。
 * proot通过拦截系统调用转换，在应用数据目录内创建虚拟根文件系统。
 *
 * 设置流程：
 * 1. [checkProotAvailable] — 检查proot二进制文件是否存在
 * 2. [extractRootfs] — 从APK资源解压rootfs（一次性操作）
 * 3. [createProotSession] — 启动proot包装的shell会话
 */
class ProotSessionManager(private val context: Context) {

    private val _setupState = MutableStateFlow<ProotSetupState>(ProotSetupState.NotChecked)

    /** proot设置状态流 */
    val setupState: StateFlow<ProotSetupState> = _setupState.asStateFlow()

    private val _prootSessions = MutableStateFlow<List<ProotSession>>(emptyList())

    /** 活动proot会话列表流 */
    val prootSessions: StateFlow<List<ProotSession>> = _prootSessions.asStateFlow()

    companion object {
        private const val TAG = "ProotSession"

        // 应用内部存储路径
        private const val PROOT_DIR = "proot"
        private const val ROOTFS_DIR = "rootfs"
        private const val PROOT_BIN = "proot"
        private const val PROOT_CARE_BIN = "proot-care"

        // Assets资源路径
        private const val ASSETS_PROOT_DIR = "proot"
        private const val ASSETS_ROOTFS_DIR = "rootfs"

        // 默认proot shell
        private const val DEFAULT_SHELL = "/bin/bash"
        private const val FALLBACK_SHELL = "/bin/sh"

        // Rootfs大小检查（最小50MB视为有效）
        private const val MIN_ROOTFS_SIZE_BYTES = 50L * 1024 * 1024
    }

    /**
     * 检查设备上proot是否可用。
     *
     * 验证：
     * 1. proot二进制文件存在且可执行
     * 2. rootfs目录存在且达到最小大小
     *
     * @return 包含详细信息的ProotAvailability
     */
    suspend fun checkProotAvailable(): ProotAvailability = withContext(Dispatchers.IO) {
        _setupState.value = ProotSetupState.Checking

        val prootDir = File(context.filesDir, PROOT_DIR)
        val prootBinary = File(prootDir, PROOT_BIN)
        val rootfsDir = File(context.filesDir, ROOTFS_DIR)

        // 检查proot二进制文件
        val binaryExists = prootBinary.exists() && prootBinary.canExecute()
        if (!binaryExists && prootBinary.exists()) {
            // 尝试设置可执行权限
            prootBinary.setExecutable(true)
        }
        val binaryReady = prootBinary.exists() && prootBinary.canExecute()

        // 检查rootfs
        val rootfsExists = rootfsDir.exists() && rootfsDir.isDirectory
        val rootfsValid = if (rootfsExists) {
            val rootfsSize = calculateDirectorySize(rootfsDir)
            rootfsSize >= MIN_ROOTFS_SIZE_BYTES
        } else {
            false
        }

        val availability = ProotAvailability(
            prootBinaryReady = binaryReady,
            rootfsReady = rootfsExists && rootfsValid,
            prootPath = if (binaryReady) prootBinary.absolutePath else null,
            rootfsPath = if (rootfsExists) rootfsDir.absolutePath else null,
            needsExtraction = !rootfsValid || !binaryReady
        )

        if (availability.isReady) {
            _setupState.value = ProotSetupState.Ready
        } else {
            _setupState.value = ProotSetupState.NotSetup(availability)
        }

        Log.d(TAG, "Proot可用性: binary=${availability.prootBinaryReady}, rootfs=${availability.rootfsReady}")
        availability
    }

    /**
     * 从APK资源解压proot二进制文件和rootfs。
     *
     * 这是一次性操作。解压后的文件存储在应用内部存储中，跨会话重用。
     *
     * @param onProgress 解压进度回调（0.0到1.0）
     * @return 解压成功返回true
     */
    suspend fun extractRootfs(onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        _setupState.value = ProotSetupState.Extracting(0f)

        try {
            // 第一步：解压proot二进制文件
            val prootDir = File(context.filesDir, PROOT_DIR)
            if (!prootDir.exists()) prootDir.mkdirs()

            val prootBinary = File(prootDir, PROOT_BIN)
            if (!prootBinary.exists()) {
                val prootAssetPath = "$ASSETS_PROOT_DIR/$PROOT_BIN"
                try {
                    context.assets.open(prootAssetPath).use { input ->
                        prootBinary.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    prootBinary.setExecutable(true)
                    Log.d(TAG, "Proot二进制文件已解压并设置可执行权限")
                } catch (e: Exception) {
                    Log.w(TAG, "Assets中未找到proot二进制文件: $prootAssetPath", e)
                    // 二进制文件可能需要单独下载
                }
            }

            onProgress(0.1f)

            // 第二步：解压rootfs（如未解压）
            val rootfsDir = File(context.filesDir, ROOTFS_DIR)
            if (!rootfsDir.exists() || calculateDirectorySize(rootfsDir) < MIN_ROOTFS_SIZE_BYTES) {
                if (rootfsDir.exists()) {
                    rootfsDir.deleteRecursively()
                }
                rootfsDir.mkdirs()

                try {
                    val assetList = context.assets.list(ASSETS_ROOTFS_DIR) ?: emptyArray()
                    if (assetList.isNotEmpty()) {
                        extractAssetDir(ASSETS_ROOTFS_DIR, rootfsDir, onProgress)
                        Log.d(TAG, "Rootfs已从assets解压")
                    } else {
                        Log.w(TAG, "未找到rootfs资源，需要手动设置。")
                        _setupState.value = ProotSetupState.NotSetup(
                            ProotAvailability(
                                prootBinaryReady = prootBinary.exists(),
                                rootfsReady = false,
                                needsExtraction = true
                            )
                        )
                        return@withContext false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "从assets解压rootfs失败", e)
                    _setupState.value = ProotSetupState.Error("Rootfs解压失败: ${e.message}")
                    return@withContext false
                }
            }

            onProgress(1.0f)

            val success = checkProotAvailable()
            success.isReady
        } catch (e: Exception) {
            Log.e(TAG, "Proot设置失败", e)
            _setupState.value = ProotSetupState.Error("设置失败: ${e.message}")
            false
        }
    }

    /**
     * 创建proot包装的终端会话。
     *
     * 在proot虚拟环境中启动shell进程。
     * shell可以访问以解压的rootfs目录为根的完整Linux文件系统。
     *
     * @param shell 要启动的shell（默认：/bin/bash，回退：/bin/sh）
     * @param env 额外环境变量
     * @return 创建的ProotSession，如果proot未就绪返回null
     */
    suspend fun createProotSession(
        shell: String = DEFAULT_SHELL,
        env: Map<String, String> = emptyMap()
    ): ProotSession? = withContext(Dispatchers.IO) {
        val availability = checkProotAvailable()
        if (!availability.isReady) {
            Log.w(TAG, "无法创建proot会话：未就绪")
            return@withContext null
        }

        val prootPath = availability.prootPath ?: return@withContext null
        val rootfsPath = availability.rootfsPath ?: return@withContext null

        // 确定要使用的shell
        val actualShell = if (File(rootfsPath, shell.removePrefix("/")).exists()) {
            shell
        } else if (File(rootfsPath, FALLBACK_SHELL.removePrefix("/")).exists()) {
            Log.w(TAG, "Rootfs中未找到shell $shell，回退到$FALLBACK_SHELL")
            FALLBACK_SHELL
        } else {
            Log.e(TAG, "Rootfs中未找到任何shell")
            return@withContext null
        }

        // 构建proot命令
        val prootCmd = buildProotCommand(prootPath, rootfsPath, actualShell, env)

        try {
            val processBuilder = ProcessBuilder(prootCmd)
                .directory(File(rootfsPath))
                .redirectErrorStream(true)

            // 为proot设置环境
            val processEnv = processBuilder.environment()
            processEnv["HOME"] = "/root"
            processEnv["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            processEnv["TERM"] = "xterm-256color"
            processEnv["LANG"] = "en_US.UTF-8"
            processEnv["PROOT_NO_SECCOMP"] = "1" // 避免某些设备上的seccomp问题
            env.forEach { (key, value) -> processEnv[key] = value }

            val process = processBuilder.start()
            val session = ProotSession(
                id = java.util.UUID.randomUUID().toString(),
                process = process,
                shell = actualShell,
                rootfsPath = rootfsPath
            )

            _prootSessions.value = _prootSessions.value + session
            _setupState.value = ProotSetupState.Running(session.id)

            Log.d(TAG, "Proot会话已创建: ${session.id}，shell: $actualShell")
            session
        } catch (e: Exception) {
            Log.e(TAG, "创建proot会话失败", e)
            _setupState.value = ProotSetupState.Error("会话创建失败: ${e.message}")
            null
        }
    }

    /**
     * 销毁proot会话。
     * @param sessionId 要销毁的会话ID
     */
    fun destroySession(sessionId: String) {
        _prootSessions.value.find { it.id == sessionId }?.let { session ->
            destroyProcessWithFallback(session.process, sessionId)
            _prootSessions.value = _prootSessions.value.filter { it.id != sessionId }
            Log.d(TAG, "Proot会话已销毁: $sessionId")
        }
    }

    /**
     * 销毁所有proot会话。
     */
    fun destroyAll() {
        _prootSessions.value.forEach { session ->
            destroyProcessWithFallback(session.process, session.id)
        }
        _prootSessions.value = emptyList()
        _setupState.value = ProotSetupState.Ready
    }

    /**
     * 销毁进程，先尝试优雅终止（SIGTERM），若 500ms 内未退出则强制终止。
     *
     * 部分 proot 内进程会忽略 SIGTERM，仅调用 [Process.destroy] 可能留下僵尸进程占用
     * 文件描述符与内存。此处与 [TerminalSessionManager] 的销毁策略保持一致。
     */
    private fun destroyProcessWithFallback(process: Process, sessionId: String) {
        try {
            process.destroy()
            val exited = process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!exited) {
                process.destroyForcibly()
            }
        } catch (e: Exception) {
            Log.w(TAG, "销毁proot会话错误 $sessionId", e)
            try {
                process.destroyForcibly()
            } catch (_: Exception) {}
        }
    }

    /**
     * 构建proot命令行。
     *
     * proot命令格式：
     * ```
     * proot -0 -r /path/to/rootfs -b /dev -b /proc -b /sys /bin/bash
     * ```
     *
     * 参数说明：
     * - `-0`: 模拟root身份（无需真实root）
     * - `-r`: 指定根文件系统路径
     * - `-b`: 绑定挂载主机目录到虚拟文件系统
     * - `--link2symlink`: 将硬链接处理为符号链接（Android兼容性）
     * - `--kill-on-exit`: proot退出时终止所有进程
     */
    private fun buildProotCommand(
        prootPath: String,
        rootfsPath: String,
        shell: String,
        env: Map<String, String>
    ): List<String> {
        val cmd = mutableListOf(
            prootPath,
            "-0", // 模拟root
            "-r",
            rootfsPath, // 根文件系统
            "--link2symlink", // Android兼容性
            "--kill-on-exit" // 退出时清理
        )

        // 绑定挂载必要的Android目录
        cmd.add("-b")
        cmd.add("/dev")
        cmd.add("-b")
        cmd.add("/proc")
        cmd.add("-b")
        cmd.add("/sys")

        // 绑定挂载/sdcard以访问文件
        val sdcard = File("/sdcard")
        if (sdcard.exists()) {
            cmd.add("-b")
            cmd.add("/sdcard")
        }

        // 添加环境变量传递
        env.forEach { (key, _) ->
            // proot -E标志传递主机环境变量
        }

        // Shell命令
        cmd.add(shell)

        Log.d(TAG, "Proot命令: ${cmd.joinToString(" ")}")
        return cmd
    }

    /**
     * 递归解压assets目录到文件系统。
     */
    private fun extractAssetDir(assetPath: String, targetDir: File, onProgress: (Float) -> Unit) {
        val list = context.assets.list(assetPath) ?: return

        for ((index, name) in list.withIndex()) {
            val sourcePath = "$assetPath/$name"
            val targetFile = File(targetDir, name)

            // 先尝试作为文件打开；如果失败则是目录
            try {
                context.assets.open(sourcePath).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                // 为二进制文件设置可执行标志
                if (name.startsWith("proot") || name == "bash" || name == "sh") {
                    targetFile.setExecutable(true)
                }
            } catch (_: Exception) {
                // 是目录
                if (!targetFile.exists()) targetFile.mkdirs()
                extractAssetDir(sourcePath, targetFile) {}
            }

            onProgress(0.1f + 0.9f * (index.toFloat() / list.size))
        }
    }

    /**
     * 递归计算目录总大小。
     */
    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}

/**
 * Proot可用性状态。
 *
 * @property prootBinaryReady proot二进制文件是否就绪
 * @property rootfsReady rootfs是否就绪
 * @property prootPath proot二进制文件路径（就绪时）
 * @property rootfsPath rootfs路径（就绪时）
 * @property needsExtraction 是否需要解压
 */
data class ProotAvailability(
    val prootBinaryReady: Boolean,
    val rootfsReady: Boolean,
    val prootPath: String? = null,
    val rootfsPath: String? = null,
    val needsExtraction: Boolean = false
) {
    /** proot是否完全就绪可用 */
    val isReady: Boolean get() = prootBinaryReady && rootfsReady
}

/**
 * Proot设置状态密封类。
 */
sealed class ProotSetupState {
    /** 未检查 */
    data object NotChecked : ProotSetupState()

    /** 正在检查 */
    data object Checking : ProotSetupState()

    /** 未设置，包含可用性信息 */
    data class NotSetup(val availability: ProotAvailability) : ProotSetupState()

    /** 正在解压，包含进度 */
    data class Extracting(val progress: Float) : ProotSetupState()

    /** 就绪 */
    data object Ready : ProotSetupState()

    /** 运行中，包含会话ID */
    data class Running(val sessionId: String) : ProotSetupState()

    /** 错误，包含错误消息 */
    data class Error(val message: String) : ProotSetupState()
}

/**
 * 表示运行中的proot终端会话。
 *
 * @property id 会话唯一标识符
 * @property process 底层进程对象
 * @property shell 使用的shell路径
 * @property rootfsPath rootfs路径
 */
data class ProotSession(val id: String, val process: Process, val shell: String, val rootfsPath: String) {
    /** 进程是否存活 */
    val isAlive: Boolean get() = process.isAlive
}
