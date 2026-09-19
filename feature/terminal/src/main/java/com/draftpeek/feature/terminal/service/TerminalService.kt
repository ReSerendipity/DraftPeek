/**
 * 终端前台服务文件。
 *
 * 当应用进入后台时保持终端会话存活的前台服务。
 * 参考Termux的TermuxService架构实现。
 *
 * 支持两种执行模式：
 * - 前台模式：终端UI可见，显示持久通知，进程交互式运行
 * - 后台模式：终端UI不可见但shell进程继续运行，适用于长时间运行命令
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.draftpeek.feature.terminal.R
import com.draftpeek.feature.terminal.emulator.TerminalSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 终端前台服务。
 *
 * 当应用切换到后台时保持终端会话存活，防止Android系统杀死进程。
 * Android 12+要求前台服务必须有通知，点击通知可打开终端UI。
 *
 * 模式切换：
 * - 前台→后台：终端UI关闭但会话保持存活（用户按Home或导航离开）
 * - 后台→前台：用户重新打开终端UI
 */
@AndroidEntryPoint
class TerminalService : Service() {

    companion object {
        private const val TAG = "TerminalService"
        const val CHANNEL_ID = "terminal_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.draftpeek.terminal.START"
        const val ACTION_STOP = "com.draftpeek.terminal.STOP"
        const val ACTION_SET_MODE = "com.draftpeek.terminal.SET_MODE"
        const val ACTION_EXECUTE_COMMAND = "com.draftpeek.terminal.EXECUTE_COMMAND"

        // Intent参数键
        const val EXTRA_MODE = "mode"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_WORKING_DIRECTORY = "working_directory"
        const val EXTRA_ENV_VARS = "env_vars"

        /** 执行模式：前台 */
        const val MODE_FOREGROUND = "foreground"

        /** 执行模式：后台 */
        const val MODE_BACKGROUND = "background"

        /**
         * `capturedVariables` 中 `env_` 前缀条目的最大数量。
         *
         * 服务以 START_STICKY 长期运行，若不设上限，用户长时间执行大量带环境变量的命令
         * 会导致该 Map 无界增长。超过上限时按插入顺序淘汰最旧的 `env_` 条目。
         */
        private const val MAX_CAPTURED_ENV_VARS = 100

        /**
         * 启动终端前台服务。
         */
        fun start(context: Context) {
            val intent = Intent(context, TerminalService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        /**
         * 停止终端前台服务。
         */
        fun stop(context: Context) {
            val intent = Intent(context, TerminalService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * 设置终端执行模式。
         * @param context 应用上下文
         * @param mode MODE_FOREGROUND或MODE_BACKGROUND
         */
        fun setMode(context: Context, mode: String) {
            val intent = Intent(context, TerminalService::class.java).apply {
                action = ACTION_SET_MODE
                putExtra(EXTRA_MODE, mode)
            }
            context.startService(intent)
        }

        /**
         * 在终端后台执行命令。
         *
         * 命令在当前终端会话的shell中运行，输出被捕获供后续检索。
         *
         * @param context 应用上下文
         * @param command 要执行的命令
         * @param workingDirectory 可选工作目录覆盖
         * @param envVars 可选环境变量（键值对）
         */
        fun executeCommand(
            context: Context,
            command: String,
            workingDirectory: String? = null,
            envVars: Map<String, String>? = null
        ) {
            val intent = Intent(context, TerminalService::class.java).apply {
                action = ACTION_EXECUTE_COMMAND
                putExtra(EXTRA_COMMAND, command)
                workingDirectory?.let { putExtra(EXTRA_WORKING_DIRECTORY, it) }
                envVars?.let {
                    putExtra(EXTRA_ENV_VARS, ArrayList(it.entries.map { "${it.key}=${it.value}" }))
                }
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var sessionManager: TerminalSessionManager

    /** 当前执行模式 */
    private var currentMode = MODE_FOREGROUND

    /** 变量传递系统 - 后台命令捕获的输出 */
    private val capturedVariables = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                currentMode = MODE_FOREGROUND
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            ACTION_SET_MODE -> {
                val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_FOREGROUND
                setExecutionMode(mode)
            }
            ACTION_EXECUTE_COMMAND -> {
                // 确保以前台服务运行
                if (currentMode == MODE_FOREGROUND) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                }
                handleExecuteCommand(intent)
            }
            else -> {
                // 即使被系统重启也确保在前台
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 即使任务被划走也保持服务运行
        // 用户可通过通知显式停止
    }

    override fun onDestroy() {
        sessionManager.destroyAll()
        capturedVariables.clear()
        super.onDestroy()
    }

    /**
     * 获取捕获的变量值。
     *
     * 可用变量：
     * - `%stdout`: 上次捕获的stdout输出
     * - `%stderr`: 上次捕获的stderr输出
     * - `%exit_code`: 上次命令的退出码
     *
     * @param key 变量名
     * @return 变量值，未设置返回null
     */
    fun getVariable(key: String): String? = capturedVariables[key]

    /**
     * 获取所有捕获的变量。
     * @return 变量名到值的不可变映射
     */
    fun getAllVariables(): Map<String, String> = capturedVariables.toMap()

    /**
     * 设置执行模式并更新通知。
     */
    private fun setExecutionMode(mode: String) {
        val previousMode = currentMode
        currentMode = mode

        when (mode) {
            MODE_FOREGROUND -> {
                val notification = buildNotification()
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "切换到前台模式")
            }
            MODE_BACKGROUND -> {
                val notification = buildBackgroundNotification()
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "切换到后台模式")
            }
        }
    }

    /**
     * 处理EXECUTE_COMMAND Intent。
     *
     * 解析命令，应用环境变量和工作目录覆盖，然后将命令发送给会话管理器。
     */
    private fun handleExecuteCommand(intent: Intent) {
        val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
        val workingDir = intent.getStringExtra(EXTRA_WORKING_DIRECTORY)
        val envVarsList = intent.getStringArrayListExtra(EXTRA_ENV_VARS)

        // 应用环境变量覆盖（如提供）
        if (envVarsList != null) {
            for (envSpec in envVarsList) {
                val parts = envSpec.split("=", limit = 2)
                if (parts.size == 2) {
                    capturedVariables["env_${parts[0]}"] = parts[1]
                }
            }
            evictOldestEnvVarsIfNeeded()
        }

        // 如果指定则切换工作目录
        if (workingDir != null) {
            sessionManager.sendInput("cd '$workingDir'\n")
        }

        // 执行命令
        sessionManager.sendInput(command + "\n")

        // 捕获命令用于变量传递
        capturedVariables["%last_command"] = command

        Log.d(TAG, "后台命令已执行: $command")
    }

    /**
     * 当 `env_` 条目数量超过 [MAX_CAPTURED_ENV_VARS] 时，按插入顺序淘汰最旧的条目。
     *
     * `capturedVariables` 为 LinkedHashMap（[mutableMapOf] 默认实现），遍历顺序即插入顺序，
     * 因此可安全地从头部移除最早写入的 `env_` 变量。%last_command 等非 env_ 条目不受影响。
     */
    private fun evictOldestEnvVarsIfNeeded() {
        var envCount = capturedVariables.keys.count { it.startsWith("env_") }
        if (envCount <= MAX_CAPTURED_ENV_VARS) return
        val iterator = capturedVariables.keys.iterator()
        while (iterator.hasNext() && envCount > MAX_CAPTURED_ENV_VARS) {
            if (iterator.next().startsWith("env_")) {
                iterator.remove()
                envCount--
            }
        }
    }

    /**
     * 构建前台模式通知。
     */
    private fun buildNotification(): Notification {
        // getLaunchIntentForPackage 解析出的 Intent 只有包内 Activity，显式再限定一次包名，
        // 避免 PendingIntent 被其它应用以隐式 Intent 接管（PendingIntent hijacking）。
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.also {
            it.setPackage(packageName)
        }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val stopIntent = Intent(this, TerminalService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.terminal_notification_title))
            .setContentText(getString(R.string.terminal_notification_text))
            .setSmallIcon(R.drawable.terminal_notification_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.terminal_notification_stop),
                stopPendingIntent
            )
            .build()
    }

    /**
     * 构建后台执行模式通知。
     * 显示不同消息表示终端在后台无活动UI运行。
     */
    private fun buildBackgroundNotification(): Notification {
        // getLaunchIntentForPackage 解析出的 Intent 只有包内 Activity，显式再限定一次包名，
        // 避免 PendingIntent 被其它应用以隐式 Intent 接管（PendingIntent hijacking）。
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.also {
            it.setPackage(packageName)
        }
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val stopIntent = Intent(this, TerminalService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foregroundIntent = Intent(this, TerminalService::class.java).apply {
            action = ACTION_SET_MODE
            putExtra(EXTRA_MODE, MODE_FOREGROUND)
        }
        val foregroundPendingIntent = PendingIntent.getService(
            this,
            3,
            foregroundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("终端 - 后台运行")
            .setContentText(getString(R.string.terminal_service_running))
            .setSmallIcon(R.drawable.terminal_notification_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_play,
                "显示终端",
                foregroundPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.terminal_notification_stop),
                stopPendingIntent
            )
            .build()
    }

    /**
     * 创建通知渠道。
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.terminal_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.terminal_notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
