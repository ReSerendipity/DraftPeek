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
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.security

import androidx.annotation.WorkerThread

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
 * ```
 */
object NativeSecurityChecker {

    /** 威胁掩码：Frida 检测到 */
    const val THREAT_FRIDA = 1

    /** 威胁掩码：进程被跟踪（TracerPid > 0） */
    const val THREAT_TRACED = 2

    /** 威胁掩码：Zygisk/Magisk 注入检测到 */
    const val THREAT_ZYGISK = 4

    init {
        // 加载 native 库
        // 库加载失败不抛异常，相关检测会返回 false（降级处理）
        try {
            System.loadLibrary("native_security")
        } catch (_: UnsatisfiedLinkError) {
            // 库加载失败，可能是设备不支持或库被移除
            // 安全检测降级为纯 Java 实现
        }
    }

    /**
     * 检测 /proc/self/maps 中的 Frida 痕迹。
     *
     * Native 实现，比 Java 的 File.useLines() 更难被 hook。
     *
     * @return true 如果检测到 Frida 痕迹
     */
    @WorkerThread
    fun isFridaDetectedNative(): Boolean = try {
        nativeCheckFridaInMaps()
    } catch (_: UnsatisfiedLinkError) {
        false // 库未加载，降级处理
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
    } catch (_: UnsatisfiedLinkError) {
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
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    /**
     * 执行综合安全检测，返回威胁掩码。
     *
     * 一次性执行所有 native 检测，减少 JNI 调用开销。
     *
     * @return 威胁掩码（0 = 全部正常，非零值表示检测到威胁）
     */
    @WorkerThread
    fun performSecurityCheck(): Int = try {
        nativePerformSecurityCheck()
    } catch (_: UnsatisfiedLinkError) {
        0 // 库未加载，返回 0（降级处理）
    }

    // ===== JNI Native 方法声明 =====

    private external fun nativeCheckFridaInMaps(): Boolean

    private external fun nativeCheckTracerPid(): Boolean

    private external fun nativeCheckZygiskInMaps(): Boolean

    private external fun nativePerformSecurityCheck(): Int
}
