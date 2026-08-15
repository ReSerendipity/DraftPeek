// === PLAN-C FUTURE ===
// C 层扩展：将以下 3 个检测函数迁移到 Native C（native_security.c）实现，
//   分别增加：
//     native_check_memory_dump()  —— 扫描 /proc/self/mem 权限、检测 ptrace 读内存
//     native_check_dex_region_integrity() —— 计算 DEX 内存页哈希与磁盘版本对比
//     native_trigger_sigsegv_if_hostile() —— HOSTILE 时直接触发 SIGSEGV 而非 killProcess，
//       让崩溃栈看起来像普通 native crash，不暴露安全代码位置
// C 层扩展：新增 /proc/net/unix 扫描检测 Frida Gadget Unix socket
// === END PLAN-C ===

/**
 * AI 逆向检测模块。
 *
 * 单例对象，被 DraftPeekApp 启动时调用。
 * 内部调用 AntiDebug.quickCheck() 复用结果并累加 AI 专项检测信号。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.security

import android.content.Context
import android.content.pm.PackageManager
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.WorkerThread
import com.draftpeek.core.common.security.AiDetectionSignal
import com.draftpeek.core.common.security.AiDetectionSignal.*
import com.draftpeek.core.common.security.AiProtectionState
import com.draftpeek.core.common.security.AiProtectionStateHolder
import com.draftpeek.core.common.security.AiThreatLevel
import com.draftpeek.core.common.security.SecurityEventRecorder
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.BuildConfig
import java.io.File
import java.util.ArrayDeque

/**
 * AI 逆向行为检测器。
 *
 * 检测维度：
 * - 增强模拟器指纹（20+ 项特征）
 * - 类加载爆发（沙箱批量分析）
 * - 反射调用时间异常（脚本驱动）
 * - 沙箱触控时间规律性
 * - DEX 运行时 CRC 校验
 * - 签名 SHA-256 校验
 */
object AiDetector {

    // ========== A 层检测（全部实现） ==========

    /**
     * 增强模拟器指纹检测（A 层）：在 AntiDebug.isRunningOnEmulator() 基础上 +20 项特征。
     * 每项命中 +1 分，>= 5 分判定 EMULATOR_ENHANCED_FINGERPRINT。
     */
    @SuppressLint("HardwareIds") // 模拟器指纹检测：仅与已知模拟器序列号常量比对，不采集真实设备标识
    @WorkerThread
    fun detectEnhancedEmulator(): Set<AiDetectionSignal> {
        val signals = mutableSetOf<AiDetectionSignal>()
        var score = 0
        val checks = listOf<() -> Boolean>(
            { Build.RADIO.contains("generic") },
            { Build.BOARD == "unknown" },
            { Build.BOOTLOADER == "unknown" || Build.BOOTLOADER.startsWith("U-Boot") },
            { !File("/dev/block/vold").exists() && !File("/dev/fuse").exists() },
            { File("/system/lib/libc_malloc_debug_qemu.so").exists() },
            { File("/sys/qemu_trace").exists() },
            { System.getProperty("ro.kernel.qemu") != null },
            { Build.FINGERPRINT.contains("test-keys") || Build.FINGERPRINT.contains("dev-keys") },
            { Build.SERIAL == "android_id" || Build.SERIAL == "0123456789ABCDEF" },
            { Build.HARDWARE == "ttVM_x86" || Build.HARDWARE == "vbox86" },
            // 额外 10 项特征
            { Build.MODEL.contains("Android SDK", ignoreCase = true) },
            { Build.MANUFACTURER.equals("Genymotion", ignoreCase = true) },
            { Build.PRODUCT.contains("sdk_google", ignoreCase = true) },
            { Build.PRODUCT.contains("vbox86p", ignoreCase = true) },
            { Build.PRODUCT.contains("emulator", ignoreCase = true) },
            { Build.PRODUCT.contains("simulator", ignoreCase = true) },
            { File("/system/bin/qemu-props").exists() },
            { File("/proc/cpuinfo").readText().runCatching {
                contains("hypervisor", ignoreCase = true)
            }.getOrDefault(false) },
            { Build.HARDWARE.contains("goldfish", ignoreCase = true) },
            { Build.HARDWARE.contains("ranchu", ignoreCase = true) },
        )
        checks.forEach { check ->
            if (runCatching { check() }.getOrDefault(false)) score++
        }
        if (score >= 5) signals += EMULATOR_ENHANCED_FINGERPRINT
        return signals
    }

    /** 类加载爆发检测时间戳队列（最大 100 条） */
    private val classLoadTimestamps = ArrayDeque<Long>()

    /**
     * 记录一次类加载事件（被 B 层 ClassLoader 埋点回调调用）。
     */
    fun recordClassLoad() {
        synchronized(classLoadTimestamps) {
            if (classLoadTimestamps.size >= 100) {
                classLoadTimestamps.removeFirst()
            }
            classLoadTimestamps.addLast(System.nanoTime())
        }
    }

    /**
     * 类加载爆发检测（A 层）：100ms 窗口内 loadClass 次数 >= 20 视为沙箱批量扫描。
     */
    fun detectClassloadingBurst(): Set<AiDetectionSignal> {
        synchronized(classLoadTimestamps) {
            if (classLoadTimestamps.size < 20) return emptySet()
            val now = System.nanoTime()
            val count = classLoadTimestamps.count { now - it <= 100_000_000L }
            return if (count >= 20) setOf(CLASSLOADING_BURST) else emptySet()
        }
    }

    /** 反射调用时间戳队列（最大 50 条） */
    private val reflectionTimestamps = ArrayDeque<Long>()

    /**
     * 记录一次反射调用事件（被 B 层精选反射埋点调用）。
     */
    fun recordReflectionCall() {
        synchronized(reflectionTimestamps) {
            if (reflectionTimestamps.size >= 50) {
                reflectionTimestamps.removeFirst()
            }
            reflectionTimestamps.addLast(System.nanoTime())
        }
    }

    /**
     * 反射调用时间异常检测（A 层）：反射间隔标准差 < 0.5ms 视为脚本驱动。
     */
    fun detectReflectionTimingAnomaly(): Set<AiDetectionSignal> {
        synchronized(reflectionTimestamps) {
            if (reflectionTimestamps.size < 10) return emptySet()
            val timestamps = reflectionTimestamps.toList()
            val intervals = timestamps.zipWithNext { a, b -> (b - a) / 1_000_000.0 }
            val mean = intervals.average()
            if (mean > 500) return emptySet() // 间隔太大就不是批量
            val variance = intervals.map { (it - mean) * (it - mean) }.average()
            val stddev = kotlin.math.sqrt(variance)
            return if (stddev < 0.5) setOf(REFLECTION_TIMING_ANOMALY) else emptySet()
        }
    }

    /**
     * 沙箱触控/操作时间规律性检测（A 层）。
     * 连续 15 次点击间隔标准差 < 5ms 则标记 SANDBOX_TOUCH_TIMING。
     *
     * @param touchDownTimestamps 触控 ACTION_DOWN 的 epoch 毫秒时间戳列表
     */
    fun detectSandboxTouchTiming(touchDownTimestamps: List<Long>): Set<AiDetectionSignal> {
        if (touchDownTimestamps.size < 15) return emptySet()
        val intervals = touchDownTimestamps.zipWithNext { a, b -> (b - a).toDouble() }
        val mean = intervals.average()
        val variance = intervals.map { (it - mean) * (it - mean) }.average()
        return if (kotlin.math.sqrt(variance) < 5.0) setOf(SANDBOX_TOUCH_TIMING) else emptySet()
    }

    /**
     * DEX 运行时 CRC32 校验（A 层）：检测二次打包 / DEX 被 patch。
     * 基线值由 Gradle 构建期注入到 BuildConfig.DEX_CRC_BASELINE。
     * 基线为 0（CI 无签名占位）时跳过校验，零误报。
     */
    @WorkerThread
    fun verifyDexIntegrity(context: Context): Set<AiDetectionSignal> {
        val baseline = try {
            BuildConfig.DEX_CRC_BASELINE
        } catch (_: Exception) {
            0L
        }
        if (baseline == 0L) return emptySet()
        return try {
            val apkPath = context.packageCodePath
            val zipFile = java.util.zip.ZipFile(apkPath)
            var crc = 0L
            for (idx in 1..4) {
                val entryName = if (idx == 1) "classes.dex" else "classes${idx}.dex"
                val entry = zipFile.getEntry(entryName) ?: continue
                crc += entry.crc
            }
            if (crc != baseline) setOf(DEX_TAMPERED) else emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * 签名 SHA-256 校验（A 层）：检测二次打包。
     * 基线值由 Gradle 构建期注入到 BuildConfig.OFFICIAL_SIGNATURE_SHA256。
     * 基线为空（CI 无签名占位）时跳过校验，零误报。
     */
    @WorkerThread
    fun verifyOfficialSignature(context: Context): Set<AiDetectionSignal> {
        val baseline = try {
            BuildConfig.OFFICIAL_SIGNATURE_SHA256
        } catch (_: Exception) {
            ""
        }
        if (baseline.isEmpty()) return emptySet()
        return try {
            val pm = context.packageManager
            val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                    )
                ).signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
            } ?: return setOf(SIGNATURE_MISMATCH)
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val actual = md.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            if (actual != baseline.lowercase()) setOf(SIGNATURE_MISMATCH) else emptySet()
        } catch (_: Exception) {
            setOf(SIGNATURE_MISMATCH)
        }
    }

    // ========== B 层精选检测（IR 埋点后才触发，未埋点时永远返回空） ==========

    /**
     * B 层埋点检测：2 秒窗口内 >= 50 次 Class.forName。
     */
    fun detectAsmClassFornameBurst(): Set<AiDetectionSignal> {
        synchronized(classLoadTimestamps) {
            if (classLoadTimestamps.size < 50) return emptySet()
            val now = System.nanoTime()
            val recent = classLoadTimestamps.toList().takeLast(50)
            val count = recent.count { now - it <= 2_000_000_000L }
            return if (count >= 50) setOf(ASM_CLASS_FORNAME_BURST) else emptySet()
        }
    }

    /**
     * B 层埋点检测：Method.invoke 超阈值。
     */
    fun detectAsmMethodInvokeBurst(): Set<AiDetectionSignal> = emptySet()

    /**
     * B 层埋点检测：安全类异常实例化时机。
     */
    fun detectAsmSensitiveClassCreation(): Set<AiDetectionSignal> = emptySet()

    // ========== 综合评估 ==========

    /**
     * 综合评估：合并 AntiDebug 结果 + 所有 AI 检测信号。
     *
     * @param context 应用上下文
     * @return 当前 AI 防护状态
     */
    @WorkerThread
    fun assess(context: Context): AiProtectionState {
        val allSignals = mutableSetOf<AiDetectionSignal>()
        allSignals += detectEnhancedEmulator()
        allSignals += detectClassloadingBurst()
        allSignals += detectReflectionTimingAnomaly()
        allSignals += verifyDexIntegrity(context)
        allSignals += verifyOfficialSignature(context)
        allSignals += detectAsmClassFornameBurst()
        allSignals += detectAsmMethodInvokeBurst()
        allSignals += detectAsmSensitiveClassCreation()

        // 复用 AntiDebug 结果
        val antiSafe = AntiDebug.quickCheck()
        if (!antiSafe) allSignals += HOSTILE_FRIDA_XPOSED

        val threatLevel = when {
            allSignals.any { it in setOf(SIGNATURE_MISMATCH, DEX_TAMPERED) } -> AiThreatLevel.HOSTILE
            allSignals.size >= 2 -> AiThreatLevel.HOSTILE
            allSignals.size == 1 -> AiThreatLevel.SUSPICIOUS
            else -> AiThreatLevel.SAFE
        }

        val responseLevel = when (threatLevel) {
            AiThreatLevel.SAFE -> AiProtectionState.ResponseLevel.NONE
            AiThreatLevel.SUSPICIOUS -> AiProtectionState.ResponseLevel.WARNING
            AiThreatLevel.HOSTILE -> when {
                SIGNATURE_MISMATCH in allSignals || DEX_TAMPERED in allSignals
                -> AiProtectionState.ResponseLevel.SELF_DEFEND
                else -> AiProtectionState.ResponseLevel.LOCKED
            }
        }

        val state = AiProtectionState(threatLevel, allSignals.toSet(), responseLevel)
        AiProtectionStateHolder.update(state)
        return state
    }
}
