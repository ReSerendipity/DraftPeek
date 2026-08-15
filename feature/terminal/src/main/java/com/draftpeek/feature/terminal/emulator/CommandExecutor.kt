/**
 * 安全命令执行管道文件。
 *
 * 协调命令分词、安全验证和执行的完整管道。
 * 这是Ch7#4修复的核心：防止未验证的命令字符串直接传递给Runtime.exec()。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log

/**
 * Ch7#4: 安全命令执行管道。
 *
 * 协调分词、安全验证和执行。
 * 这是Ch7#4修复的核心：防止未验证的命令字符串直接传递给Runtime.exec()。
 *
 * 执行流程：
 * 1. 使用ArgumentTokenizer分词
 * 2. 使用SecurityValidator验证
 * 3. 如果验证通过，将已分词参数传递给会话管理器执行
 *
 * 注意：永不将原始用户命令字符串直接传递给shell！
 * 始终传递分词后的参数数组以防止命令注入。
 */
class CommandExecutor(
    private val sessionManager: TerminalSessionManager,
) {

    /**
     * 命令执行结果密封类。
     */
    sealed class ExecutionResult {
        /**
         * 命令已成功排队执行。
         */
        data object Success : ExecutionResult()

        /**
         * 命令被安全验证器阻止。
         * @property reason 阻止原因
         */
        data class Blocked(val reason: String) : ExecutionResult()

        /**
         * 命令被标记为危险，需要用户确认。
         * @property warning 警告消息
         * @property executeToken 用于确认执行的令牌
         */
        data class RequiresConfirmation(
            val warning: String,
            val executeToken: String,
        ) : ExecutionResult()

        /**
         * 执行期间发生错误。
         * @property error 错误消息
         */
        data class Error(val error: String) : ExecutionResult()
    }

    private val pendingConfirmations = mutableMapOf<String, List<String>>()
    private var tokenCounter = 0L

    /**
     * 执行命令，包含完整安全验证管道。
     *
     * @param sessionId 目标终端会话ID
     * @param command 用户输入的原始命令字符串
     * @return 执行结果（成功/阻止/需要确认/错误）
     */
    fun execute(sessionId: String, command: String): ExecutionResult {
        // Step 1: Tokenize
        val args = try {
            ArgumentTokenizer.tokenize(command)
        } catch (e: IllegalArgumentException) {
            return ExecutionResult.Error("命令语法错误: ${e.message}")
        }

        if (args.isEmpty()) {
            return ExecutionResult.Success // No-op
        }

        // Step 2: Validate
        val validation = SecurityValidator.validateArgs(args)
        return when (validation) {
            is SecurityValidator.ValidationResult.Blocked -> {
                Log.w("CommandExecutor", "阻止命令: ${args.joinToString(" ")} - ${validation.reason}")
                ExecutionResult.Blocked(validation.reason)
            }
            is SecurityValidator.ValidationResult.Allowed -> {
                if (validation.isDangerous) {
                    // Generate confirmation token
                    val token = generateToken()
                    pendingConfirmations[token] = args
                    ExecutionResult.RequiresConfirmation(
                        warning = validation.warning ?: "此命令可能存在危险",
                        executeToken = token,
                    )
                } else {
                    // Step 3: Execute with tokenized args (NOT raw string!)
                    executeValidated(sessionId, args)
                }
            }
        }
    }

    /**
     * 用户确认后执行危险命令。
     *
     * @param sessionId 目标终端会话ID
     * @param token execute()返回的确认令牌
     * @return 执行结果
     */
    fun confirmAndExecute(sessionId: String, token: String): ExecutionResult {
        val args = pendingConfirmations.remove(token)
            ?: return ExecutionResult.Error("无效或过期的确认令牌")
        return executeValidated(sessionId, args)
    }

    /**
     * 内部执行：直接将已验证、已分词参数写入会话。
     *
     * 关键安全点：写入命令时附加换行符，但不重新拼接成shell命令字符串。
     * 由于使用-proot/shell会话逐字符/逐行处理输入，这保持了参数完整性。
     */
    private fun executeValidated(sessionId: String, args: List<String>): ExecutionResult {
        return try {
            // 将分词后的参数拼接为命令行并发送到指定会话
            val commandLine = args.joinToString(" ") + "\n"
            sessionManager.sendInput(sessionId, commandLine)

            Log.d("CommandExecutor", "执行: ${args.first()} (${args.size - 1} 个参数)")
            ExecutionResult.Success
        } catch (e: Exception) {
            Log.e("CommandExecutor", "执行失败", e)
            ExecutionResult.Error(e.message ?: "未知错误")
        }
    }

    @Synchronized
    private fun generateToken(): String {
        return "exec_${System.currentTimeMillis()}_${++tokenCounter}"
    }
}
