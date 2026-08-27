// === PLAN-C FUTURE ===
// C 层扩展：新增 TTS 播报功能（首次检测到 TAMPERED_APK 时，使用 Android TTS
//   引擎合成并播放 5 秒中文法律声明音频，需用户手动确认后方可关闭）。
//   注：TTS 引擎包名是 "com.google.android.tts" / 系统自带，不会增加 APK 体积。
// C 层扩展：在 DEX 文件头（classes.dex 第 0x24 偏移后）插入自定义版权 notice，
//   内容为 "DraftPeek (C) 2024-2026 - OFFICIAL BUILD HASH=xxx"，
//   被二次打包工具重打包时此偏移内容通常会被改写，作为额外的完整性校验源。
// C 层扩展：每个资源文件 apk 包 entry 增加 comment 字段（ZIP format 支持），
//   写入版权信息与哈希，二次打包工具（apktool）默认会丢弃 comment 字段，
//   从而触发 verifyResourceCommentHash() 校验失败。
// === END PLAN-C ===

/**
 * 法律威慑模块。
 *
 * 功能：
 *  1. 启动时展示「官方 APK 校验」状态通知栏（持续显示）
 *  2. 检测到威胁时按严重级别展示法律警告 Dialog（震动 + 提示音）
 *  3. 启动时可选展示二次打包风险提示（签名不匹配时必显）
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.security

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.draftpeek.R
import java.lang.ref.WeakReference

private const val TAG = "LegalDeterrence"

/**
 * 法律威慑模块单例。
 */
object LegalDeterrence {

    /** 通知栏常驻通知 ID */
    private const val NOTIFICATION_ID = 10001

    /** 通知渠道 ID */
    private const val CHANNEL_ID = "security_status"

    /**
     * 严重级别枚举。
     */
    enum class Severity {
        /** 初级警告 */
        WARNING,

        /** 中级锁定 */
        LOCKED,

        /** 高级：检测到篡改 APK */
        TAMPERED_APK,
    }

    /** 当前 resumed Activity 的弱引用（通过 ActivityLifecycleCallbacks 获取）。
     *  使用 WeakReference 避免单例持有 Activity 强引用造成内存泄漏。 */
    @Volatile
    private var currentActivityRef: WeakReference<android.app.Activity>? = null

    /**
     * 注册 Activity 生命周期回调以获取当前 resumed Activity。
     * 由 DraftPeekApp.onCreate 调用。
     */
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(object :
            android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: android.app.Activity) {
                if (currentActivityRef?.get() === activity) {
                    currentActivityRef = null
                }
            }

            override fun onActivityCreated(
                activity: android.app.Activity,
                savedInstanceState: android.os.Bundle?,
            ) {
            }

            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(
                activity: android.app.Activity,
                outState: android.os.Bundle,
            ) {
            }

            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    /**
     * 展示法律警告 Dialog。
     *
     * 通过 ActivityLifecycleCallbacks 获取当前 resumed Activity，
     * 使用 BrandDialog（core/ui 组件）渲染，禁止使用原生 AlertDialog。
     *
     * 实现要点：
     * - severity = TAMPERED_APK 时按钮 = "立即卸载(引导)" + "知道了"，前者跳转系统卸载页
     * - 弹窗同时触发震动和提示音
     *
     * @param reason 触发原因（信号名称列表）
     * @param severity 严重级别
     */
    fun showLegalWarningDialog(reason: String, severity: Severity) {
        val activity = currentActivityRef?.get() ?: run {
            Log.w(TAG, "Cannot show legal warning dialog: no active activity")
            return
        }

        // 触发震动
        triggerVibration(activity)

        // 播放默认通知提示音
        triggerNotificationSound(activity)

        // 使用 Compose 方式展示对话框
        // 通过 ActivityLifecycleCallbacks + Compose 实现
        // 由于安全模块无法直接使用 Compose 组件，这里通过 Notification + Toast 兜底
        val titleResId = when (severity) {
            Severity.WARNING -> R.string.security_legal_warning_title_warning
            Severity.LOCKED -> R.string.security_legal_warning_title_locked
            Severity.TAMPERED_APK -> R.string.security_legal_warning_title_tampered
        }

        val title = activity.getString(titleResId)
        val message = com.draftpeek.core.common.security.AiEthicalNotice
            .LEGAL_CONSEQUENCES_TEXT[getCurrentLanguageTag()] ?:
            com.draftpeek.core.common.security.AiEthicalNotice
                .LEGAL_CONSEQUENCES_TEXT["en-US"] ?:
            com.draftpeek.core.common.security.AiEthicalNotice
                .LEGAL_CONSEQUENCES_TEXT["zh-CN"] ?: ""

        // 简化实现：通过系统 Notification 展示法律警告
        // 正式版应由 UI 层使用 BrandDialog 渲染
        showWarningNotification(activity, title, message, severity)
    }

    /**
     * 显示/更新通知栏「官方 APK 校验」状态指示器（常驻 Notification）。
     *
     * @param context 应用上下文
     * @param isOfficial true 表示官方版本已验证，false 表示非官方
     */
    fun updateSignatureVerificationNotification(context: Context, isOfficial: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        // minSdk 26（O）起通知渠道一直可用
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.security_notification_channel_security_status),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.security_notification_channel_security_status)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)

        val titleRes = if (isOfficial) {
            R.string.security_notification_official_title
        } else {
            R.string.security_notification_unofficial_title
        }

        val textRes = if (isOfficial) {
            R.string.security_notification_official_text
        } else {
            R.string.security_notification_unofficial_text
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isOfficial) android.R.drawable.ic_dialog_info else android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 启动页二次打包风险提示（若签名不匹配）。
     * 在 SplashActivity 动画结束后、跳转 MainActivity 之前额外插入风险提示。
     *
     * @param activity SplashActivity 实例
     */
    fun showSplashTamperWarningIfNeeded(activity: android.app.Activity) {
        // 签名不匹配时在启动动画结束后、跳转 MainActivity 之前，
        // 额外插入 3 秒风险提示：
        //   - 红色背景大标题 "检测到非官方 APK"
        //   - 列表展示 3 条风险：数据窃取 / 广告注入 / 恶意代码植入
        //   - 按钮 "前往官方下载" + "我已知晓风险（不推荐）"
        // 简化实现：通过通知展示
        val title = activity.getString(R.string.security_splash_tamper_title)
        val message = "${activity.getString(R.string.security_splash_tamper_risk_1)}\n" +
            "${activity.getString(R.string.security_splash_tamper_risk_2)}\n" +
            "${activity.getString(R.string.security_splash_tamper_risk_3)}"

        showWarningNotification(activity, title, message, Severity.TAMPERED_APK)
    }

    // ---- 私有辅助方法 ----

    private fun triggerVibration(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                // minSdk 26（O）起 VibrationEffect 一直可用
                it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
            // 震动失败不影响功能
        }
    }

    private fun triggerNotificationSound(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri)?.play()
        } catch (_: Exception) {
            // 播放失败不影响功能
        }
    }

    private fun showWarningNotification(
        context: Context,
        title: String,
        message: String,
        severity: Severity,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        // minSdk 26（O）起通知渠道一直可用
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.security_notification_channel_security_status),
            NotificationManager.IMPORTANCE_HIGH,
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // 使用不同通知 ID 避免与常驻通知冲突
        notificationManager.notify(NOTIFICATION_ID + (severity.ordinal + 1), notification)
    }

    /**
     * 获取当前应用语言的 BCP-47 标签。
     * 跟随系统/应用语言设置，回退到英语。
     */
    private fun getCurrentLanguageTag(): String {
        return try {
            val currentActivity = currentActivityRef?.get()
            val context = currentActivity ?: return "en-US"
            val config = context.resources.configuration
            val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.locales[0]
            } else {
                @Suppress("DEPRECATION")
                config.locale
            }
            val tag = locale.toLanguageTag()
            // AiEthicalNotice 支持的标签：zh-CN, en-US, ja-JP, ko-KR
            when {
                tag.startsWith("zh") -> "zh-CN"
                tag.startsWith("ja") -> "ja-JP"
                tag.startsWith("ko") -> "ko-KR"
                else -> "en-US"
            }
        } catch (_: Exception) {
            "en-US"
        }
    }
}
