/**
 * 终端数据模型定义文件。
 *
 * 定义终端功能模块使用的所有数据类和枚举，包括终端配置、会话、输出行、
 * ANSI样式跨度、ANSI颜色和终端主题。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.model

/**
 * 终端会话配置。
 *
 * @property shell Shell路径，默认为/system/bin/sh
 * @property workingDirectory 工作目录，默认为用户主目录或/sdcard
 * @property columns 终端列数，默认为80
 * @property rows 终端行数，默认为24
 * @property environment 环境变量映射
 */
data class TerminalConfig(
    val shell: String = "/system/bin/sh",
    val workingDirectory: String = System.getProperty("user.home") ?: "/sdcard",
    val columns: Int = 80,
    val rows: Int = 24,
    val useProot: Boolean = true,
    val rootfsPath: String? = null,
    val environment: Map<String, String> = mapOf(
        "TERM" to "xterm-256color",
        "LANG" to "en_US.UTF-8",
        "PATH" to "/system/bin:/system/xbin"
    )
)

/**
 * 表示单个终端会话。
 *
 * @property id 会话唯一标识符
 * @property config 终端配置
 * @property isActive 会话是否处于活动状态
 * @property title 会话标题
 */
data class TerminalSession(
    val id: String,
    val config: TerminalConfig,
    val isActive: Boolean = true,
    val title: String = "Shell",
    val workingDirectory: String = config.workingDirectory
)

/**
 * 带有可选ANSI样式的终端输出行。
 *
 * @property text 行文本内容
 * @property spans ANSI样式跨度列表
 */
data class TerminalLine(val text: String, val spans: List<AnsiSpan> = emptyList())

/**
 * 终端行内的样式跨度（ANSI颜色/样式）。
 *
 * @property start 起始位置（字符索引）
 * @property end 结束位置（字符索引）
 * @property foreground 前景色
 * @property background 背景色
 * @property bold 是否粗体
 * @property italic 是否斜体
 * @property underline 是否下划线
 */
data class AnsiSpan(
    val start: Int,
    val end: Int,
    val foreground: AnsiColor = AnsiColor.DEFAULT,
    val background: AnsiColor = AnsiColor.DEFAULT,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)

/**
 * 终端渲染用的ANSI颜色代码枚举。
 *
 * @property code ANSI颜色代码值
 */
enum class AnsiColor(val code: Int) {
    DEFAULT(-1),
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    BRIGHT_BLACK(8),
    BRIGHT_RED(9),
    BRIGHT_GREEN(10),
    BRIGHT_YELLOW(11),
    BRIGHT_BLUE(12),
    BRIGHT_MAGENTA(13),
    BRIGHT_CYAN(14),
    BRIGHT_WHITE(15);

    companion object {
        /**
         * 根据ANSI颜色代码查找对应的枚举值。
         * @param code ANSI颜色代码
         * @return 对应的AnsiColor枚举，未找到返回DEFAULT
         */
        fun fromCode(code: Int): AnsiColor = entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}

/**
 * 终端渲染颜色主题。
 *
 * 使用Catppuccin Mocha配色方案，提供深色背景和高对比度文字，
 * 适合长时间代码编辑使用。
 *
 * @property background 背景色（ARGB格式Long值）
 * @property foreground 前景文字色
 * @property cursor 光标颜色
 * @property selectionBackground 选中区域背景色
 * @property colors 16色ANSI调色板
 */
data class TerminalTheme(
    val background: Long = 0xFF1E1E2E, // Dark background
    val foreground: Long = 0xFFCDD6F4, // Light text
    val cursor: Long = 0xFFF5E0DC, // Cursor color
    val selectionBackground: Long = 0xFF45475A,
    val colors: List<Long> = listOf(
        0xFF1E1E2E, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
        0xFF89B4FA, 0xFFF5C2E7, 0xFF94E2D5, 0xFFCDD6F4,
        0xFF585B70, 0xFFF38BA8, 0xFFA6E3A1, 0xFFF9E2AF,
        0xFF89B4FA, 0xFFF5C2E7, 0xFF94E2D5, 0xFFCDD6F4
    )
)
