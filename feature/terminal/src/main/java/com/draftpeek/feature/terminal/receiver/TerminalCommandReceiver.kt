/**
 * 终端命令广播接收器文件。
 *
 * 允许通过广播Intent执行终端命令。接收器受签名级权限保护，
 * 仅限与DraftPeek使用相同签名密钥的应用发送广播。
 * 所有命令必须通过安全验证管道，无法跳过。
 *
 * SECURITY VULN-001 fix:
 * - 接收器添加 signature 级权限保护（com.draftpeek.permission.TERMINAL_COMMAND）
 * - 删除 EXTRA_FORCE 参数，所有命令必须经过 SecurityValidator 验证
 *
 * SECURITY VULN-012 fix:
 * - 移除所有命令明文日志输出，防止 logcat 泄露终端命令内容
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.draftpeek.feature.terminal.emulator.ArgumentTokenizer
import com.draftpeek.feature.terminal.emulator.SecurityValidator
import com.draftpeek.feature.terminal.service.TerminalService

/**
 * 终端命令广播接收器。
 *
 * 接收外部应用发送的命令执行广播，经过分词和安全验证后转发给TerminalService执行。
 *
 * 安全模型：
 * - **签名级权限保护**：接收器要求 `com.draftpeek.permission.TERMINAL_COMMAND` 权限，
 *   该权限 protectionLevel=signature，仅同签名应用可发送
 * - **强制安全验证**：所有命令必须经过 SecurityValidator 验证，无跳过选项
 * - 命令执行前通过 ArgumentTokenizer 分词
 *
 * Intent参数：
 * - `command` (String, 必需): 要执行的命令
 * - `working_directory` (String, 可选): 工作目录覆盖
 * - `background` (Boolean, 可选): 后台模式运行（默认false）
 */
class TerminalCommandReceiver : BroadcastReceiver() {

    companion object {
        /** 执行命令广播Action */
        const val ACTION_RUN_COMMAND = "com.draftpeek.terminal.RUN_COMMAND"

        /** 命令结果广播Action */
        const val ACTION_COMMAND_RESULT = "com.draftpeek.terminal.COMMAND_RESULT"

        // Intent参数键
        const val EXTRA_COMMAND = "command"
        const val EXTRA_WORKING_DIRECTORY = "working_directory"
        const val EXTRA_BACKGROUND = "background"

        // 结果参数键
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_MESSAGE = "result_message"
        const val EXTRA_RESULT_COMMAND = "result_command"

        // 结果代码
        const val RESULT_SUCCESS = 0
        const val RESULT_BLOCKED = 1
        const val RESULT_PARSE_ERROR = 2
        const val RESULT_EXECUTION_ERROR = 3
        const val RESULT_NO_COMMAND = 4

        private const val TAG = "TerminalCmdReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RUN_COMMAND) return

        val command = intent.getStringExtra(EXTRA_COMMAND)
        if (command.isNullOrBlank()) {
            sendResult(context, RESULT_NO_COMMAND, "未提供命令", null)
            return
        }

        val workingDir = intent.getStringExtra(EXTRA_WORKING_DIRECTORY)
        val runInBackground = intent.getBooleanExtra(EXTRA_BACKGROUND, false)

        // SECURITY VULN-012: 不记录命令明文，仅记录安全相关的元数据
        Log.d(TAG, "Command received (bg=$runInBackground, len=${command.length})")

        // 第一步：分词
        val args = ArgumentTokenizer.tokenizeOrNull(command)
        if (args == null) {
            sendResult(context, RESULT_PARSE_ERROR, "命令中引号未闭合", command)
            return
        }

        // 第二步：安全验证 —— 强制执行，无法跳过
        // SECURITY VULN-001: 删除了 EXTRA_FORCE / skipValidation 逻辑
        val validation = SecurityValidator.validateArgs(args)
        if (validation is SecurityValidator.ValidationResult.Blocked) {
            // VULN-012: 不记录被阻止的命令明文，仅记录原因
            Log.w(TAG, "Command blocked by SecurityValidator: ${validation.reason}")
            sendResult(context, RESULT_BLOCKED, validation.reason, command)
            return
        }

        // 第三步：执行命令
        try {
            if (runInBackground) {
                // 如果未运行则以后台模式启动终端服务
                TerminalService.start(context)
                TerminalService.setMode(context, TerminalService.MODE_BACKGROUND)
                TerminalService.executeCommand(context, command, workingDir)
            } else {
                // 前台模式启动
                TerminalService.start(context)
                TerminalService.setMode(context, TerminalService.MODE_FOREGROUND)
                TerminalService.executeCommand(context, command, workingDir)
            }

            sendResult(context, RESULT_SUCCESS, "命令已分发", command)
            // VULN-012: 不记录命令内容
            Log.d(TAG, "Command dispatched successfully")
        } catch (e: Exception) {
            // VULN-012: 不记录命令内容，仅记录异常信息
            Log.e(TAG, "Command dispatch failed: ${e.message}", e)
            sendResult(context, RESULT_EXECUTION_ERROR, e.message ?: "未知错误", command)
        }
    }

    /**
     * 向调用方发送结果广播。
     *
     * 结果广播包含：
     * - `result_code`: RESULT_*常量之一
     * - `result_message`: 人类可读的结果描述
     * - `result_command`: 原始命令字符串
     */
    private fun sendResult(context: Context, resultCode: Int, message: String, command: String?) {
        val resultIntent = Intent(ACTION_COMMAND_RESULT).apply {
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_RESULT_MESSAGE, message)
            command?.let { putExtra(EXTRA_RESULT_COMMAND, it) }
            // 使结果广播仅对发送应用可见
            setPackage(context.packageName)
        }
        context.sendBroadcast(resultIntent)
    }
}
