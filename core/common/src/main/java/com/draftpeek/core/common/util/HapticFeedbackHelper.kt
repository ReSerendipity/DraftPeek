/**
 * 触觉反馈（振动）帮助模块。
 *
 * 为听障用户和需要触觉反馈的场景提供振动提示，根据操作类型提供不同模式的振动（点击、成功、错误、警告、重要通知）。
 * 兼容Android 8.0以上的VibrationEffect API和旧版API，支持预定义效果和自定义振动模式。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 振动反馈帮助类。
 *
 * 为听障用户和需要触觉反馈的场景提供振动提示。根据操作类型提供不同模式的振动，
 * 确保用户可以通过触觉感知操作结果。兼容新旧API版本，自动选择Vibrator或VibratorManager。
 *
 * 使用方式：
 * ```
 * val helper = HapticFeedbackHelper(context)
 * if (settings.vibrationFeedback) {
 *     helper.success()    // 操作成功
 *     helper.error()      // 操作失败
 *     helper.click()      // 按钮点击
 *     helper.warning()    // 警告提示
 * }
 * ```
 *
 * @property context 应用上下文
 */
class HapticFeedbackHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** 设备是否支持振动 */
    val hasVibrator: Boolean
        get() = vibrator?.hasVibrator() == true

    /**
     * 轻微点击反馈 — 用于按钮点击、切换开关等常规交互。
     */
    fun click() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.EFFECT_TICK, 20)
        } else {
            // EFFECT_TICK 需要 API 29，低版本退化为短促振动
            vibrate(longArrayOf(0, 20), -1)
        }
    }

    /**
     * 操作成功反馈 — 短促单次振动，表示操作成功完成。
     */
    fun success() {
        vibrate(longArrayOf(0, 50, 50, 50), -1)
    }

    /**
     * 操作失败/错误反馈 — 较长时间的双振动模式。
     */
    fun error() {
        vibrate(longArrayOf(0, 100, 80, 100, 80, 100), -1)
    }

    /**
     * 警告提示反馈 — 中等强度的连续短振动。
     */
    fun warning() {
        vibrate(longArrayOf(0, 80, 60, 80), -1)
    }

    /**
     * 重要通知反馈 — 强振动模式，用于需要用户立即注意的事件。
     */
    fun important() {
        vibrate(longArrayOf(0, 200, 100, 200), -1)
    }

    /**
     * 自定义振动模式。
     *
     * @param timings 振动时间模式数组（毫秒），偶数索引为等待，奇数索引为振动
     * @param repeat 重复索引（-1 表示不重复）
     */
    fun vibrate(timings: LongArray, repeat: Int = -1) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        // minSdk 26（O）起 VibrationEffect 一直可用
        vib.vibrate(VibrationEffect.createWaveform(timings, repeat))
    }

    /**
     * 使用预设效果 ID 振动。
     *
     * @param effectId VibrationEffect 预设效果 ID（如EFFECT_TICK、EFFECT_CLICK等）
     * @param duration 振动持续时间（毫秒），仅在预设效果不可用时作为后备
     */
    fun vibrate(effectId: Int, duration: Long) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vib.vibrate(VibrationEffect.createPredefined(effectId))
                return
            } catch (_: Exception) {
            }
        }
        // minSdk 26（O）起 createOneShot 一直可用
        vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * 取消正在进行的振动。
     */
    fun cancel() {
        vibrator?.cancel()
    }
}
