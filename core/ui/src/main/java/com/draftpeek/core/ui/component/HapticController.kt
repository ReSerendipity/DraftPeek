/**
 * DraftPeek 触觉反馈控制器。
 *
 * 提供统一的触觉反馈（振动）接口，根据无障碍设置控制是否触发振动。
 * 使用系统 [View.performHapticFeedback] API 而非直接调用 Vibrator，
 * 以自动尊重用户系统触觉反馈设置且无需额外权限。
 *
 * 支持三种反馈类型：
 * - [HapticController.confirm]：操作成功反馈
 * - [HapticController.reject]：操作失败/错误反馈
 * - [HapticController.longPress]：长按/重要操作反馈
 *
 * 通过 [rememberHapticController] 在 Composable 中获取实例。
 */
package com.draftpeek.core.ui.component

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.draftpeek.core.ui.theme.LocalAccessibilityState

/**
 * 触觉反馈控制器。
 *
 * 根据 [AccessibilityState.vibrationFeedback] 控制是否触发振动反馈。
 * 使用 [View.performHapticFeedback] 而非直接调用 Vibrator，这样可以：
 * - 自动尊重用户系统的触觉反馈设置（用户关闭系统触感时不强制振动）
 * - 无需额外权限（VIBRATE 权限）
 * - 与系统 UI 行为一致
 *
 * 在关键交互（保存成功、错误、删除等）时调用对应方法，让听障用户或
 * 需要触觉反馈的场景通过振动感知操作结果。
 *
 * 使用方式：
 * ```
 * val hapticController = rememberHapticController()
 * hapticController.confirm()   // 操作成功
 * hapticController.reject()    // 操作失败
 * hapticController.longPress() // 长按 / 删除
 * ```
 */
class HapticController(
    private val view: View,
    private val enabled: Boolean,
) {
    /** 操作成功反馈 — 使用 CONFIRM（API 30+），低版本回退到 KEYBOARD_TAP。 */
    fun confirm() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        view.performHapticFeedback(constant)
    }

    /** 操作失败/错误反馈 — 使用 REJECT（API 30+），低版本回退到 LONG_PRESS。 */
    fun reject() {
        if (!enabled) return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }

    /** 长按 / 重要操作反馈 — 使用 LONG_PRESS。 */
    fun longPress() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/**
 * 在 Composable 作用域内记住一个 [HapticController] 实例。
 *
 * 当 [AccessibilityState.vibrationFeedback] 切换时，控制器会自动重建，
 * 确保后续调用使用最新的启用状态。
 */
@Composable
fun rememberHapticController(): HapticController {
    val view = LocalView.current
    val state = LocalAccessibilityState.current
    return remember(view, state.vibrationFeedback) {
        HapticController(view, state.vibrationFeedback)
    }
}
