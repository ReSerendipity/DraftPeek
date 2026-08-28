/**
 * .editorconfig解析工具模块。
 *
 * 轻量级.editorconfig文件解析器，支持最常用的配置属性：indent_style、indent_size、end_of_line、
 * charset、trim_trailing_whitespace、insert_final_newline、max_line_length等。
 * 使用Glob模式匹配节（section），支持按文件名解析对应的编辑器配置。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * EditorConfig配置数据类，表示解析后的.editorconfig属性。
 *
 * @property indentStyle 缩进风格："space"或"tab"
 * @property indentSize 缩进大小（空格数）
 * @property tabWidth Tab宽度
 * @property endOfLine 行尾符："lf"、"crlf"、"native"
 * @property charset 字符编码
 * @property trimTrailingWhitespace 是否自动去除行尾空白
 * @property insertFinalNewline 是否在文件末尾插入换行符
 * @property maxLineLength 最大行长度
 */
data class EditorConfig(
    val indentStyle: String? = null,
    val indentSize: Int? = null,
    val tabWidth: Int? = null,
    val endOfLine: String? = null,
    val charset: String? = null,
    val trimTrailingWhitespace: Boolean? = null,
    val insertFinalNewline: Boolean? = null,
    val maxLineLength: Int? = null
)

/**
 * EditorConfig解析器对象。
 *
 * 解析.editorconfig文件内容，根据文件名匹配对应的Glob模式节，合并属性后返回EditorConfig。
 * 节按定义顺序应用，后面匹配的节会覆盖前面的属性。支持简单的Glob模式（*和?通配符）。
 */
object EditorConfigParser {
    private val SECTION_REGEX = Regex("^\\[(.+)\\]$")
    private val PROPERTY_REGEX = Regex("^\\s*(\\S+)\\s*=\\s*(.+)\\s*$")

    /**
     * 解析.editorconfig内容，返回给定文件名对应的解析后[EditorConfig]。
     *
     * 节按顺序匹配；当多个模式匹配同一文件名时，后面的节覆盖前面的节。
     *
     * @param content .editorconfig文件内容
     * @param fileName 要匹配的文件名
     * @return 合并后的EditorConfig对象
     */
    fun parse(content: String, fileName: String): EditorConfig {
        var currentSection = "*"
        val sections = mutableMapOf<String, MutableMap<String, String>>()
        var currentProps = mutableMapOf<String, String>()
        sections["*"] = currentProps

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue

            val sectionMatch = SECTION_REGEX.find(trimmed)
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1]
                currentProps = sections.getOrPut(currentSection) { mutableMapOf() }
                continue
            }

            val propMatch = PROPERTY_REGEX.find(trimmed)
            if (propMatch != null) {
                currentProps[propMatch.groupValues[1]] = propMatch.groupValues[2].trim()
            }
        }

        val matchingProps = mutableMapOf<String, String>()
        val sectionOrder = mutableListOf<String>()

        var tempSection = "*"
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) continue
            val sectionMatch = SECTION_REGEX.find(trimmed)
            if (sectionMatch != null) {
                tempSection = sectionMatch.groupValues[1]
                if (tempSection !in sectionOrder) {
                    sectionOrder.add(tempSection)
                }
                continue
            }
        }
        if ("*" !in sectionOrder) {
            sectionOrder.add(0, "*")
        }

        for (pattern in sectionOrder) {
            val props = sections[pattern] ?: continue
            if (matchesGlob(fileName, pattern)) {
                matchingProps.putAll(props)
            }
        }

        return EditorConfig(
            indentStyle = matchingProps["indent_style"],
            indentSize = matchingProps["indent_size"]?.toIntOrNull(),
            tabWidth = matchingProps["tab_width"]?.toIntOrNull(),
            endOfLine = matchingProps["end_of_line"],
            charset = matchingProps["charset"],
            trimTrailingWhitespace = matchingProps["trim_trailing_whitespace"]?.toBooleanStrictOrNull(),
            insertFinalNewline = matchingProps["insert_final_newline"]?.toBooleanStrictOrNull(),
            maxLineLength = matchingProps["max_line_length"]?.toIntOrNull()
        )
    }

    /**
     * 简单的Glob匹配，支持`*`和`?`通配符。
     * 对于EditorConfig，`*`匹配任意字符串（在完整路径中不匹配路径分隔符，但对于简单文件名匹配则匹配所有内容）。
     *
     * @param fileName 要匹配的文件名
     * @param pattern Glob模式
     * @return 如果文件名匹配模式返回true
     */
    private fun matchesGlob(fileName: String, pattern: String): Boolean {
        if (pattern == "*") return true
        val regexStr = buildString {
            for (ch in pattern) {
                when (ch) {
                    '*' -> append(".*")
                    '?' -> append(".")
                    '.', '(', ')', '+', '^', '$', '|', '\\', '{', '}' -> {
                        append('\\')
                        append(ch)
                    }
                    else -> append(ch)
                }
            }
        }
        return regexStr.toRegex().matches(fileName)
    }
}
