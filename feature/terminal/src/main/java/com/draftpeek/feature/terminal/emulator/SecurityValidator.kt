/**
 * 终端命令安全验证器文件。
 *
 * 提供命令执行前的安全验证，防止意外或恶意操作造成系统损坏。
 * 参考Termux Tasker的PluginUtils安全模式独立实现。
 *
 * 安全模型：
 * - 阻止可能造成不可逆系统损坏的命令
 * - 对潜在危险操作发出警告
 * - 支持自定义阻止/允许命令列表
 * - 永不阻止用户安全逃生通道（如Ctrl+C等）
 *
 * 线程安全：所有方法都是纯函数，可安全并发使用。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.emulator

import android.util.Log

/**
 * Ch7#4: 终端命令安全验证器。
 *
 * 在执行前验证用户输入的命令，防止意外或恶意损坏。
 * 参考Termux Tasker的PluginUtils安全模式，独立重新实现。
 *
 * 安全模型：
 * - 阻止可能造成不可逆系统损坏的命令
 * - 对潜在危险操作发出警告
 * - 支持自定义阻止/允许命令
 * - 永不阻止用户安全逃生通道（Ctrl+C等）
 *
 * 线程安全：所有方法都是纯函数，可安全并发使用。
 */
object SecurityValidator {

    private const val TAG = "SecurityValidator"

    /**
     * 无论配置如何始终被阻止的命令。
     * 这些命令可能导致设备变砖或数据丢失且无法恢复。
     */
    private val BLOCKED_COMMANDS = setOf(
        "reboot",
        "reboot -p", // 设备重启/关机
        "shutdown", // 系统关机
        "recovery", // 启动到恢复模式
        "bootloader", // 启动到bootloader
        "fastboot", // Fastboot模式
        "format" // 磁盘格式化
    )

    /**
     * 有条件危险的命令 - 允许执行但应标记以提醒用户。
     */
    private val DANGEROUS_PATTERNS = listOf(
        Regex("""\brm\s+(-[a-zA-Z]*f[a-zA-Z]*\s+|.*--no-preserve-root.*)"""), // rm -rf 强制递归删除
        Regex("""\bdd\s+.*of=/dev/"""), // dd写入设备（可能损坏分区）
        Regex("""\biptables\s+-F"""), // 清空防火墙规则
        Regex("""\bsystemctl\s+(stop|disable)\s+"""), // 停止/禁用系统服务
        Regex("""\bchmod\s+(-R\s+)?0{3,4}\s+/"""), // 根目录权限设为000
        Regex("""\bchown\s+(-R\s+)?\S+\s+/""") // 修改根目录所有者
    )

    /**
     * 命令执行应受限制的目录。
     * 针对这些路径的命令需谨慎处理。
     */
    private val RESTRICTED_PATHS = setOf(
        "/system",
        "/proc",
        "/sys",
        "/dev"
    )

    /**
     * 命令验证结果密封类。
     */
    sealed class ValidationResult {
        /**
         * 命令可以安全执行。
         * @property isDangerous 命令是否被认为有危险（用户可能需要确认）
         * @property warning 如果命令有危险，提供人类可读的警告信息
         */
        data class Allowed(val isDangerous: Boolean = false, val warning: String? = null) : ValidationResult()

        /**
         * 命令被阻止，不得执行。
         * @property reason 命令被阻止的原因
         */
        data class Blocked(val reason: String) : ValidationResult()
    }

    /**
     * 验证命令字符串的执行安全性。
     *
     * 安全验证流程：
     * 1. 空命令直接允许（无操作）
     * 2. 提取基础命令（第一个单词）
     * 3. 检查是否在阻止列表中（包括路径绕过检测）
     * 4. 检查危险模式匹配
     * 5. 检查是否访问受限系统路径
     *
     * @param command 用户输入的原始命令字符串
     * @return 验证结果，指示命令是否可以继续执行
     */
    fun validate(command: String): ValidationResult {
        val trimmed = command.trim()

        // 空命令允许执行（无操作）
        if (trimmed.isEmpty()) {
            return ValidationResult.Allowed()
        }

        // 提取基础命令（第一个单词）
        val baseCommand = trimmed.substringBefore(' ').lowercase()

        // 检查阻止命令列表
        if (isBlockedCommand(baseCommand, trimmed)) {
            Log.w(TAG, "阻止命令: $trimmed")
            return ValidationResult.Blocked(
                reason = "命令 '$baseCommand' 因安全原因被阻止。它可能导致不可逆的系统损坏。"
            )
        }

        // 检查危险模式匹配
        val dangerousMatch = DANGEROUS_PATTERNS.find { it.containsMatchIn(trimmed) }
        if (dangerousMatch != null) {
            return ValidationResult.Allowed(
                isDangerous = true,
                warning = "此命令可能导致数据丢失或系统损坏，请谨慎操作。"
            )
        }

        // 检查受限路径访问
        if (targetsRestrictedPath(trimmed)) {
            return ValidationResult.Allowed(
                isDangerous = true,
                warning = "此命令操作受限系统目录。"
            )
        }

        return ValidationResult.Allowed()
    }

    /**
     * 验证解析后的参数列表。
     *
     * @param args 分词后的命令参数
     * @return 验证结果
     */
    fun validateArgs(args: List<String>): ValidationResult {
        if (args.isEmpty()) return ValidationResult.Allowed()
        return validate(args.joinToString(" "))
    }

    /**
     * 检查命令是否在阻止列表中。
     * 包含路径绕过检测：即使通过完整路径调用被阻止的命令也会被检测。
     *
     * @param baseCommand 基础命令名
     * @param fullCommand 完整命令字符串
     * @return 如果命令被阻止返回true
     */
    private fun isBlockedCommand(baseCommand: String, fullCommand: String): Boolean {
        // 检查精确匹配的阻止命令
        if (BLOCKED_COMMANDS.contains(baseCommand)) return true
        if (BLOCKED_COMMANDS.contains(fullCommand.lowercase())) return true

        // 阻止通过路径调用被阻止命令的尝试（如 /system/bin/reboot）
        val withoutPath = baseCommand.substringAfterLast('/')
        if (BLOCKED_COMMANDS.contains(withoutPath)) return true

        return false
    }

    /**
     * 检查命令是否操作受限系统路径。
     *
     * @param command 命令字符串
     * @return 如果命令访问受限路径返回true
     */
    private fun targetsRestrictedPath(command: String): Boolean = RESTRICTED_PATHS.any { prefix ->
        command.contains(Regex("""\s${Regex.escape(prefix)}/"""))
    }
}
