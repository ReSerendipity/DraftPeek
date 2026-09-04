/**
 * DraftPeek Native 层安全检测模块。
 *
 * SECURITY VULN-005: C 层反检测实现。
 *
 * 通过 JNI 调用 native C 代码执行安全检测，使 Frida/Xposed 难以通过
 * Java 方法名 hook 绕过检测。Native 函数使用 hidden visibility，
 * 符号不出现在 .so 的导出符号表中，增加逆向难度。
 *
 * 检测维度：
 * - /proc/self/maps 中的 Frida 痕迹扫描
 * - /proc/self/status 中的 TracerPid 读取
 * - /proc/self/maps 中的 Zygisk/Magisk 痕迹扫描
 * - 综合检测（一次性执行所有检测，减少 JNI 调用开销）
 *
 * R5 整改（fail-open → 显式上报）：
 * 原实现在 .so 加载失败时静默吞掉 UnsatisfiedLinkError，所有 native 检测返回 false/0，
 * 调用方无法区分"检测通过"与"检测根本没跑"。现改为：
 *   1. 记录 nativeLoadFailed，并通过 [isNativeLoaded] 暴露给调用方；
 *   2. 告警一次（warn-once，避免刷屏），而非完全静默；
 *   3. [performSecurityCheck] 在不可用时置位 [THREAT_NATIVE_UNAVAILABLE]，
 *      使"检测能力降级"成为可观测信号，而不是伪装成"未检测到威胁"。
 *
 * 说明：此处**未**把 THREAT_NATIVE_UNAVAILABLE 计入 AntiDebug 的威胁分。
 * 原因是 .so 加载失败在部分设备/CI 上属合法情形，直接加分会破坏既有回归测试
 * （AntiDebugAssessTest 依赖干净环境 currentThreat == 0），并有误杀风险。
 * 若后续具备误报遥测（见评估报告 R10），再评估是否纳入计分。
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.security

import android.util.Log
import androidx.annotation.WorkerThread
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native 层安全检测器。
 *
 * 通过 JNI 调用 C 代码执行反调试检测，比纯 Java 实现更难被 Frida hook 绕过。
 *
 * 使用方式：
 * ```kotlin
 * val threats = NativeSecurityChecker.performSecurityCheck()
 * if (threats and NativeSecurityChecker.THREAT_FRIDA != 0) {
 *     // Frida detected
 * }
 * if (threats and NativeSecurityChecker.THREAT_NATIVE_UNAVAILABLE != 0) {
 *     // 检测能力降级：native 层不可用，结果不可信
 * }
 * ```
 */
object NativeSecurityChecker {

    /** 威胁掩码：Frida 检测到 */
    const val THREAT_FRIDA = 1

    /** 威胁掩码：进程被跟踪（TracerPid > 0） */
    const val THREAT_TRACED = 2

    /** 威胁掩码：Zygisk/Magisk 注入检测到 */
    const val THREAT_ZYGISK = 4

    /**
     * 威胁掩码：native 检测层不可用（.so 加载失败）。
     *
     * 置位表示"本次未执行 native 检测"，而非"检测通过"。
     * 调用方可据此判断结果可信度（R5）。
     */
    const val THREAT_NATIVE_UNAVAILABLE = 8

    private const val TAG = "NativeSecurityChecker"

    /**
     * native 库加载是否失败。必须在 init 块**之前**声明：
     * Kotlin 的属性初始化器与 init 块按源码顺序执行，若声明在 init 之后，
     * 其 `= false` 初始化器会在 init 赋值之后再跑一遍并覆盖结果。
     */
    @Volatile
    private var nativeLoadFailed: Boolean = false

    /** warn-once 闸门，避免每次检测都刷日志 */
    private val warnedOnce = AtomicBoolean(false)

    init {
        try {
            System.loadLibrary("native_security")
        } catch (e: UnsatisfiedLinkError) {
            // R5：不再静默降级——标记失败并告警一次
            markUnavailable(e)
        }
    }

    /**
     * native 检测层是否可用。false 表示 .so 未加载，native 检测全部未执行。
     */
    fun isNativeLoaded(): Boolean = !nativeLoadFailed

    /**
     * 标记 native 层不可用，并仅告警一次。
     */
    private fun markUnavailable(cause: Throwable) {
        nativeLoadFailed = true
        if (warnedOnce.compareAndSet(false, true)) {
            Log.w(
                TAG,
                "native_security.so unavailable — native anti-tamper checks are degraded. " +
                    "performSecurityCheck() will report THREAT_NATIVE_UNAVAILABLE.",
                cause
            )
        }
    }

    /**
     * 检测 /proc/self/maps 中的 Frida 痕迹。
     *
     * Native 实现，比 Java 的 File.useLines() 更难被 hook。
     *
     * @return true 如果检测到 Frida 痕迹（native 不可用时返回 false，请结合 [isNativeLoaded] 判断）
     */
    @WorkerThread
    fun isFridaDetectedNative(): Boolean = try {
        nativeCheckFridaInMaps()
    } catch (e: UnsatisfiedLinkError) {
        markUnavailable(e)
        false
    }

    /**
     * 检测进程是否被跟踪（TracerPid > 0）。
     *
     * Native 实现，直接读取 /proc/self/status，不经过 Java IO。
     *
     * @return true 如果进程正在被跟踪
     */
    @WorkerThread
    fun isTracedNative(): Boolean = try {
        nativeCheckTracerPid()
    } catch (e: UnsatisfiedLinkError) {
        markUnavailable(e)
        false
    }

    /**
     * 检测 /proc/self/maps 中的 Zygisk/Magisk 痕迹。
     *
     * @return true 如果检测到 Zygisk/Magisk 注入
     */
    @WorkerThread
    fun isZygiskDetectedNative(): Boolean = try {
        nativeCheckZygiskInMaps()
    } catch (e: UnsatisfiedLinkError) {
        markUnavailable(e)
        false
    }

    /**
     * 执行综合安全检测，返回威胁掩码。
     *
     * 一次性执行所有 native 检测，减少 JNI 调用开销。
     *
     * R5：native 层不可用时，返回值会带上 [THREAT_NATIVE_UNAVAILABLE]，
     * 使调用方能区分"未检测到威胁"与"检测未执行"。
     *
     * @return 威胁掩码（0 = 全部正常，非零值表示检测到威胁或检测能力降级）
     */
    @WorkerThread
    fun performSecurityCheck(): Int = try {
        val mask = nativePerformSecurityCheck()
        if (nativeLoadFailed) mask or THREAT_NATIVE_UNAVAILABLE else mask
    } catch (e: UnsatisfiedLinkError) {
        markUnavailable(e)
        THREAT_NATIVE_UNAVAILABLE
    }

    // ===== JNI Native 方法声明 =====

    private external fun nativeCheckFridaInMaps(): Boolean

    private external fun nativeCheckTracerPid(): Boolean

    private external fun nativeCheckZygiskInMaps(): Boolean

    private external fun nativePerformSecurityCheck(): Int
}
