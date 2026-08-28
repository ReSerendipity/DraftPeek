/**
 * DraftPeek 反调试与反注入保护模块。
 *
 * **文件功能**：提供多层反调试、反注入、反 Hook 检测，识别调试器、模拟器、Root、Frida、Xposed 等敌对环境，
 *               通过安全等级体系驱动功能降级，保护应用运行时安全。
 *
 * **主要类/接口**：[AntiDebug] - 单例对象，包含所有反调试检测方法。
 *
 * **模块依赖**：
 * - Android 框架：[Debug]、[Build] 系统 API
 * - Java 标准库：文件 I/O、Socket 网络检测、反射
 * - `core/common/security`：与 [SecurityGate] 联动更新环境安全状态
 *
 * 仅在 Release 构建中启用完整检测，Debug 构建跳过所有安全检查。
 */
package com.draftpeek.security

import android.annotation.SuppressLint
import android.os.Build
import android.os.Debug
import androidx.annotation.WorkerThread
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 多层反调试与反注入保护系统。
 * 仅在 Release 构建中启用。
 *
 * **设计原则**：
 * - 安全等级体系（SAFE / SUSPICIOUS / HOSTILE）替代单一 killProcess
 * - 检测结果驱动功能降级而非立即崩溃，增加攻击者排查难度
 * - 多检测维度：调试器、模拟器、ptrace、Frida、Xposed/LSPosed/EdXposed、Root、Hook框架
 * - 检测方法内聚，不在单一入口点暴露全部逻辑
 * - 使用原子累加威胁分数，多次检测结果累积使行为更难预测
 */
object AntiDebug {

    /**
     * 安全威胁等级
     */
    enum class SecurityLevel {
        /** 环境安全 */
        SAFE,

        /** 存在可疑迹象，功能应降级 */
        SUSPICIOUS,

        /** 确认处于敌对环境，应终止或严格限制 */
        HOSTILE
    }

    // ===== 累积威胁分数（跨多次检测累加，使行为更难预测） =====
    // [Review] C4 fix: 使用 AtomicInteger 替代 @Volatile var，保证原子性
    //
    // [Fix] AUDIT-2026-08：原实现只增不减，一旦 HOSTILE 永久降级（误报即锁死）。
    // 增加滑窗衰减：每 SCORE_DECAY_INTERVAL_MS 将累积分减半一次，
    // 持续性威胁（每次评估都会重新加分）仍会保持高等级，
    // 瞬态误报（模拟器测试、临时挂调试器）会在数分钟内自动恢复。
    @JvmField
    @Volatile
    internal var threatScore = AtomicInteger(0)
    @JvmField
    @Volatile
    internal var lastScoreDecayMs = AtomicLong(System.currentTimeMillis())

    private const val SCORE_DECAY_INTERVAL_MS = 60_000L // 每 60 秒衰减一次
    private const val SCORE_DECAY_DIVISOR = 2 // 每次衰减为原来的 1/2

    /**
     * 威胁分数滑窗衰减：超过衰减间隔后把累积分减半。
     * 由 [assess] 在每次评估前调用，分数只在被观察时变化。
     */
    private fun decayThreatScore() {
        val now = System.currentTimeMillis()
        val last = lastScoreDecayMs.get()
        if (now - last < SCORE_DECAY_INTERVAL_MS) return
        if (lastScoreDecayMs.compareAndSet(last, now)) {
            var current = threatScore.get()
            while (current > 0) {
                val decayed = current / SCORE_DECAY_DIVISOR
                if (threatScore.compareAndSet(current, decayed)) break
                current = threatScore.get()
            }
        }
    }

    /**
     * 当前安全等级（供外部模块查询以决定是否限制功能）
     */
    @JvmField
    @Volatile
    internal var currentLevel: SecurityLevel = SecurityLevel.SAFE

    // =================================================================
    //  检测原语（每个方法独立检测一个维度）
    // =================================================================

    /**
     * 检测是否处于被调试状态
     */
    fun isDebuggerConnected(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    /**
     * 检测是否运行在常见模拟器环境中
     */
    fun isRunningOnEmulator(): Boolean = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.PRODUCT.contains("sdk_google") ||
        Build.PRODUCT.contains("vbox86p") ||
        Build.PRODUCT.contains("emulator") ||
        Build.PRODUCT.contains("simulator")

    /**
     * 检测是否被 ptrace 附加（通过 /proc/self/status 的 TracerPid 字段）
     */
    fun isTraced(): Boolean = try {
        val statusFile = File("/proc/self/status")
        if (statusFile.exists()) {
            val tracerPidLine = statusFile.useLines { lines ->
                lines.find { it.startsWith("TracerPid:") }
            }
            val pid = tracerPidLine?.split("\\s+".toRegex())?.getOrNull(1)?.toIntOrNull() ?: 0
            pid != 0
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }

    /**
     * 检测 Frida 动态注入框架（多端口 + 进程 + 文件 + maps 扫描 + 库特征检测）
     *
     * [Review] C3 fix: 添加 @WorkerThread 注解 —— Socket 连接 + readText 会阻塞调用线程，
     * 禁止从主线程调用，否则可能导致 ANR。
     */
    @WorkerThread
    @SuppressLint("SdCardPath") // 安全扫描需要检查固定路径下的 Frida 特征文件（/sdcard/frida-server 等）
    fun isFridaDetected(): Boolean {
        return try {
            // 检测 Frida 常见端口（默认 27042 及其他常用端口）
            val fridaPorts = intArrayOf(27042, 27043, 4444, 6666)
            for (port in fridaPorts) {
                try {
                    // [Review] C3 fix: 使用 .use {} 确保 Socket 在任何路径下都被关闭
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress("127.0.0.1", port), 100)
                    }
                    return true
                } catch (_: Exception) {
                    // Port not open, continue
                }
            }

            // 检测 /proc/self/maps 中的 Frida 特征（包括库文件）
            try {
                val maps = File("/proc/self/maps")
                if (maps.exists()) {
                    // 增强的 Frida 特征模式列表
                    val fridaPatterns = listOf(
                        "frida", // Frida 核心
                        "gum-js-loop", // Frida GUM 引擎
                        "gmain", // Frida GLib 主循环
                        "linjector", // Frida 注入器
                        "frida-gadget", // Frida Gadget 库
                        "frida-agent", // Frida Agent
                        "libfrida", // Frida 原生库
                        "re.frida.server" // Frida Server
                    )
                    val found = maps.useLines { lines ->
                        lines.any { line ->
                            fridaPatterns.any { pattern ->
                                line.contains(pattern, ignoreCase = true)
                            }
                        }
                    }
                    if (found) return true
                }
            } catch (_: Exception) { /* ignore */ }

            // 检测 Frida 特征文件和目录
            val fridaPaths = listOf(
                "/usr/sbin/frida-server",
                "/data/local/tmp/frida-server",
                "/sdcard/frida-server",
                "/data/local/tmp/re.frida.server",
                "/data/local/tmp/frida-gadget.so",
                "/data/local/tmp/frida-agent.so",
                "/data/local/tmp/frida"
            )
            if (fridaPaths.any { File(it).exists() }) return true

            // 检测 Frida 环境变量
            val fridaEnvVars = listOf(
                "FRIDA_SERVER_PORT",
                "FRIDA_SERVER_SCRIPT"
            )
            if (fridaEnvVars.any { System.getenv(it) != null }) return true

            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 检测 Xposed / LSPosed / EdXposed 框架（类检测 + 文件/目录检测 + maps 检测）
     */
    fun isXposedDetected(): Boolean {
        return try {
            // 检测多种 Xposed 变体的特征类
            val xposedClasses = listOf(
                "de.robv.android.xposed.XposedBridge", // 原始 Xposed
                "de.robv.android.xposed.XposedHelpers", // Xposed Helpers
                "com.swift.sandhook.xposedcompat.XposedCompat", // SandHook (EdXposed)
                "me.weishu.epic.art.EpicNative", // Epic (VirtualXposed)
                "org.lsposed.lspd.nativebridge.NativeAPI", // LSPosed
                "io.github.lsposed.lspd.NativeAPI", // LSPosed 变体
                "de.robv.android.xposed.XC_MethodHook", // Xposed 方法 Hook
                "de.robv.android.xposed.callbacks.XC_LoadPackage" // Xposed 包加载回调
            )
            val classDetected = xposedClasses.any { className ->
                try {
                    Class.forName(className)
                    true
                } catch (_: ClassNotFoundException) {
                    false
                }
            }
            if (classDetected) return true

            // 检测 Xposed 特征文件和目录
            val xposedPaths = listOf(
                "/system/framework/XposedBridge.jar",
                "/system/lib/libxposed_art.so",
                "/system/lib64/libxposed_art.so",
                "/data/adb/modules/lspd",
                "/data/adb/lsposed",
                "/data/adb/edposed",
                "/data/adb/virtualxposed"
            )
            if (xposedPaths.any { File(it).exists() }) return true

            // 检测 /proc/self/maps 中的 Xposed 特征
            try {
                val maps = File("/proc/self/maps")
                if (maps.exists()) {
                    val xposedMapsPatterns = listOf(
                        "XposedBridge",
                        "xposed_art",
                        "lspd",
                        "edposed",
                        "virtualxposed"
                    )
                    val found = maps.useLines { lines ->
                        lines.any { line ->
                            xposedMapsPatterns.any { pattern ->
                                line.contains(pattern, ignoreCase = true)
                            }
                        }
                    }
                    if (found) return true
                }
            } catch (_: Exception) { /* ignore */ }

            false
        } catch (_: Exception) {
            false
        }
    }

    private const val SU_TIMEOUT_MS = 1000L

    /**
     * 检测设备是否已被 Root。
     *
     * 检测维度：
     * 1. 常见 su 二进制文件路径存在性检查
     * 2. Magisk 特有路径和文件检测
     * 3. PATH 环境变量中的 su 可执行文件
     * 4. 尝试执行 `su -c id` 命令验证 Root 权限（1秒超时防止阻塞）
     *
     * @return 若检测到 Root 迹象返回 true，否则返回 false
     */
    fun isRooted(): Boolean {
        return try {
            val rootPaths = listOf(
                "/system/app/Superuser.apk",
                "/system/xbin/su",
                "/system/bin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/data/local/su",
                "/su/bin/su",
                "/su/bin",
                "/system/xbin/daemonsu"
            )
            if (rootPaths.any { File(it).exists() }) return true

            val magiskPaths = listOf(
                "/sbin/.magisk",
                "/data/adb/magisk",
                "/data/adb/magisk.img",
                "/cache/.disable_magisk",
                "/data/adb/modules",
                "/data/adb/services.d",
                "/data/adb/post-fs-data.d"
            )
            if (magiskPaths.any { File(it).exists() }) return true

            val pathEnv = System.getenv("PATH") ?: ""
            val pathBins = pathEnv.split(":")
            for (bin in pathBins) {
                val suPath = File(bin, "su")
                if (suPath.exists()) return true
            }

            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                if (process.waitFor(SU_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    if (process.exitValue() == 0) return true
                } else {
                    process.destroyForcibly()
                }
            } catch (_: Exception) {
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 检测常见的 Hook 框架（非 Xposed 系列）
     */
    @SuppressLint("SdCardPath") // 安全扫描需要检查固定路径下的 Frida Gadget 特征文件（/sdcard/frida-gadget.so）
    fun isHookFrameworkDetected(): Boolean {
        return try {
            // 检测 Frida Gadget（即使没有 frida-server）
            val gadgetPaths = listOf(
                "/data/local/tmp/libfrida-gadget.so",
                "/data/local/tmp/frida-gadget.so",
                "/sdcard/frida-gadget.so"
            )
            if (gadgetPaths.any { File(it).exists() }) return true

            // 检测 /proc/self/maps 中的 Hook 框架特征
            try {
                val maps = File("/proc/self/maps")
                if (maps.exists()) {
                    val hookPatterns = listOf(
                        "frida-gadget",
                        "frida-agent",
                        "libfrida",
                        "xposed",
                        "substrate", // Cydia Substrate
                        "lib substrate", // Substrate 原生库
                        "TweakInject",
                        "SSLHook"
                    )
                    val found = maps.useLines { lines ->
                        lines.any { line ->
                            hookPatterns.any { pattern ->
                                line.contains(pattern, ignoreCase = true)
                            }
                        }
                    }
                    if (found) return true
                }
            } catch (_: Exception) { /* ignore */ }

            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 检测 Magisk Zygisk 注入框架和 Wrap.sh 调试包装器。
     *
     * SECURITY VULN-014: 增强 Root 检测，覆盖 Magisk Zygisk 注入痕迹。
     *
     * 检测维度：
     * 1. /proc/self/maps 中的 Zygisk 库特征（libzygisk, zygisk_*.so）
     * 2. Wrap.sh 存在性检查（Magisk 的调试包装器）
     * 3. Magisk DenyList (Shamiko) 痕迹检测
     * 4. Zygisk 配置文件和目录检测
     */
    fun isZygiskDetected(): Boolean {
        return try {
            // 检测 /proc/self/maps 中的 Zygisk 特征
            try {
                val maps = File("/proc/self/maps")
                if (maps.exists()) {
                    val zygiskPatterns = listOf(
                        "zygisk", // Zygisk 核心库
                        "libzygisk", // Zygisk 原生库
                        "zygisk_", // Zygisk 模块前缀
                        "magiskzygisk", // Magisk Zygisk
                        "riru", // Riru (Zygisk 前身)
                        "libriru", // Riru 原生库
                        "shamiko", // Shamiko (DenyList 模块)
                        "zygisk_next", // Zygisk Next (替代实现)
                        "dobby", // Dobby hook 框架 (Zygisk 依赖)
                        "ndk_translation" // 某些 Zygisk 模块依赖
                    )
                    val found = maps.useLines { lines ->
                        lines.any { line ->
                            zygiskPatterns.any { pattern ->
                                line.contains(pattern, ignoreCase = true)
                            }
                        }
                    }
                    if (found) return true
                }
            } catch (_: Exception) { /* ignore */ }

            // 检测 Zygisk 特征文件和目录
            val zygiskPaths = listOf(
                "/data/adb/modules/zygisksu", // Zygisk - SU
                "/data/adb/modules/zygisk", // Zygisk 模块目录
                "/data/adb/modules/shamiko", // Shamiko 模块
                "/data/adb/.zygisk", // Zygisk 隐藏目录
                "/debug_ramdisk/zygisk", // Zygisk 在 debug ramdisk
                "/system/bin/wrap.sh", // Wrap.sh 调试包装器
                "/system/xbin/wrap.sh", // Wrap.sh 备用路径
                "/debug_ramdisk/wrap.sh", // Wrap.sh 在 debug ramdisk
                "/data/local/tmp/wrap.sh", // Wrap.sh 临时路径
                "/data/adb/modules/riru", // Riru 模块目录
                "/data/adb/riru", // Riru 核心目录
                "/data/adb/modules/zygisk_next" // Zygisk Next 模块
            )
            if (zygiskPaths.any { File(it).exists() }) return true

            // 检测 Magisk DenyList (Shamiko) 配置
            val denyListPaths = listOf(
                "/data/adb/shamiko", // Shamiko 配置
                "/data/adb/modules/.zygisk_shamiko", // Shamiko 隐藏标记
                "/data/adb/magisk/denylist", // Magisk DenyList 配置
                "/cache/.disable_magisk" // Magisk 禁用标记（可能表示隐藏模式）
            )
            if (denyListPaths.any { File(it).exists() }) return true

            // 检测 Wrap.sh 环境变量（Magisk 调试包装器会设置此变量）
            if (System.getenv("WRAP_SCRIPT") != null) return true
            if (System.getenv("debug_wrap") != null) return true

            false
        } catch (_: Exception) {
            false
        }
    }

    // =================================================================
    //  综合评估
    // =================================================================

// 安全代码完整性校验结果缓存
@JvmField
@Volatile
internal var integrityChecked = false

@JvmField
@Volatile
internal var integrityVerified = false

    /**
     * 执行全量安全检测，返回威胁等级。
     * 每次调用会累加威胁分数；累积分按时间窗衰减（见 [decayThreatScore]），
     * 持续性威胁保持高等级，瞬态误报会在数分钟内自动恢复。
     *
     * [Review] C4 fix: 使用 AtomicInteger.addAndGet() 原子累加，
     * 保证多线程并发调用的正确性。
     *
     * [Code Integrity] 添加安全代码完整性自校验，检测安全类是否被篡改。
     */
    @WorkerThread
    fun assess(): SecurityLevel {
        var currentThreat = 0

        // ===== 安全代码完整性自校验 =====
        // 攻击者修改 assess() 时会改变类字节码，此检查可增加逆向成本
        if (!integrityChecked) {
            try {
                val result = SecurityIntegrityChecker.verifySelfIntegrity()
                integrityVerified = result
                integrityChecked = true
            } catch (_: Exception) {
                // 校验本身失败，记录为可疑
                integrityVerified = false
                integrityChecked = true
            }
        }
        // 如果完整性校验失败，显著增加威胁分数
        if (integrityChecked && !integrityVerified) {
            currentThreat += 10 // 高威胁：安全代码可能被篡改
        }

        if (isDebuggerConnected()) currentThreat += 5
        if (isRunningOnEmulator()) currentThreat += 3
        if (isTraced()) currentThreat += 5
        if (isFridaDetected()) currentThreat += 5
        if (isXposedDetected()) currentThreat += 4
        if (isRooted()) currentThreat += 2
        if (isHookFrameworkDetected()) currentThreat += 4
        if (isZygiskDetected()) currentThreat += 3 // VULN-014: Zygisk 检测

        // VULN-005: Native C 层反检测（更难被 Frida hook 绕过）
        // Native 检测作为 Java 层检测的补充，如果 Java 层检测被 hook 绕过，
        // Native 层检测仍然可以发现威胁。
        val nativeThreats = NativeSecurityChecker.performSecurityCheck()
        if (nativeThreats and NativeSecurityChecker.THREAT_FRIDA != 0) {
            currentThreat += 3 // Native Frida 检测（分数较低因为 Java 层已有检测）
        }
        if (nativeThreats and NativeSecurityChecker.THREAT_TRACED != 0) {
            currentThreat += 2 // Native TracerPid 检测
        }
        if (nativeThreats and NativeSecurityChecker.THREAT_ZYGISK != 0) {
            currentThreat += 2 // Native Zygisk 检测
        }

        // [Fix] AUDIT-2026-08：累加前先执行滑窗衰减，瞬态误报可自动恢复
        decayThreatScore()

        // [Review] C4 fix: 原子累加
        val total = threatScore.addAndGet(currentThreat)

        currentLevel = when {
            total >= 8 || currentThreat >= 5 -> SecurityLevel.HOSTILE
            total >= 3 || currentThreat >= 2 -> SecurityLevel.SUSPICIOUS
            else -> SecurityLevel.SAFE
        }

        return currentLevel
    }

    /**
     * 执行强制保护 —— 仅在确认 HOSTILE 时终止进程。
     * 不直接暴露全部检测逻辑在单一方法中，使 Frida hook 单方法无法完全绕过。
     */
    fun enforce() {
        val level = assess()
        if (level == SecurityLevel.HOSTILE) {
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }

    /**
     * 轻量级快速校验 —— 仅检测调试器连接状态。
     * 适用于高频调用场景（如周期性检查）。
     * 此方法不执行阻塞 IO 操作，可安全从主线程调用。
     */
    fun quickCheck(): Boolean {
        if (isDebuggerConnected()) {
            // [Review] C4 fix: 原子累加
            threatScore.addAndGet(5)
            currentLevel = SecurityLevel.HOSTILE
            return false
        }
        if (isTraced()) {
            threatScore.addAndGet(5)
            currentLevel = SecurityLevel.HOSTILE
            return false
        }
        return true
    }

    /**
     * 获取随机化的检查间隔（毫秒），避免固定间隔被攻击者预测。
     * 范围：3-15 秒
     * SECURITY VULN-013: 使用 SecureRandom 替代非 CSPRNG Random，
     * 防止攻击者通过预测检查间隔来规避检测。
     */
    fun nextCheckIntervalMs(): Long {
        val secureRandom = SecureRandom()
        return (3000 + secureRandom.nextInt(12001)).toLong() // 3000..15000
    }

    /**
     * 获取初始检查延迟（毫秒），增加不确定性。
     * 范围：1-5 秒
     * SECURITY VULN-013: 使用 SecureRandom 替代非 CSPRNG Random。
     */
    fun initialDelayMs(): Long {
        val secureRandom = SecureRandom()
        return (1000 + secureRandom.nextInt(4001)).toLong() // 1000..5000
    }
}
