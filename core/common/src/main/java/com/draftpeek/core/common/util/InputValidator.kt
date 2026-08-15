/**
 * 输入验证工具模块。
 *
 * 提供用户输入数据的验证功能，包括搜索查询、替换文本、跨文件搜索、代码片段标题/内容/分类、GitHub URL等。
 * 使用白名单正则验证URL、控制字符清洗防止注入，预编译正则表达式提升性能，各验证方法单一职责。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import java.util.regex.Pattern

/**
 * 输入验证结果密封类。
 */
sealed class ValidationResult {
    /** 验证通过 */
    data object Valid : ValidationResult()

    /**
     * 验证失败。
     *
     * @property field 字段名称
     * @property messageResId 错误消息字符串资源ID
     */
    data class Invalid(val field: String, val messageResId: Int) : ValidationResult()
}

/**
 * 输入验证工具对象。
 *
 * 提供各类用户输入的验证方法，包括：搜索查询验证、替换文本验证、跨文件搜索验证、
 * 代码片段标题/内容/分类验证、GitHub URL验证。同时提供正则特殊字符转义和控制字符清洗功能。
 */
object InputValidator {

    private const val MAX_SEARCH_QUERY_LENGTH = 1000
    private const val MAX_REPLACE_TEXT_LENGTH = 5000
    private const val MAX_CROSS_FILE_SEARCH_LENGTH = 500
    private const val MAX_SNIPPET_TITLE_LENGTH = 200
    private const val MAX_SNIPPET_CONTENT_LENGTH = 100_000
    private const val MAX_SNIPPET_CATEGORY_LENGTH = 100
    private const val MAX_GITHUB_URL_LENGTH = 2048

    private val GITHUB_URL_PATTERN = Pattern.compile(
        "^https://github\\.com/[\\w.-]+/[\\w.-]+(/.*)?$"
    )

    @JvmStatic val MSG_TOO_LONG = com.draftpeek.core.common.R.string.error_url_too_long
    @JvmStatic val MSG_EMPTY = com.draftpeek.core.common.R.string.error_url_empty
    @JvmStatic val MSG_INVALID_CHARS = com.draftpeek.core.common.R.string.error_url_invalid_chars
    @JvmStatic val MSG_INVALID_URL = com.draftpeek.core.common.R.string.error_url_invalid_format

    /**
     * 验证搜索查询字符串。
     *
     * @param query 搜索查询字符串
     * @param maxLength 最大长度限制
     * @param isRegex 是否为正则表达式模式
     * @return 验证结果
     */
    fun validateSearchQuery(
        query: String,
        maxLength: Int = MAX_SEARCH_QUERY_LENGTH,
        isRegex: Boolean
    ): ValidationResult {
        if (query.isEmpty()) {
            return ValidationResult.Invalid("query", MSG_EMPTY)
        }
        val actualQuery = if (!isRegex) escapeRegexSpecialChars(query) else query
        if (actualQuery.length > maxLength) {
            return ValidationResult.Invalid("query", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证替换文本长度。
     *
     * @param text 替换文本
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateReplaceText(text: String, maxLength: Int = MAX_REPLACE_TEXT_LENGTH): ValidationResult {
        if (text.length > maxLength) {
            return ValidationResult.Invalid("replaceText", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证跨文件搜索查询。
     *
     * @param query 搜索查询字符串
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateCrossFileSearchQuery(query: String, maxLength: Int = MAX_CROSS_FILE_SEARCH_LENGTH): ValidationResult {
        if (query.isEmpty()) {
            return ValidationResult.Invalid("query", MSG_EMPTY)
        }
        if (query.length > maxLength) {
            return ValidationResult.Invalid("query", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证代码片段标题。
     *
     * 验证前先去除首尾空白并过滤控制字符。
     *
     * @param title 代码片段标题
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateSnippetTitle(title: String, maxLength: Int = MAX_SNIPPET_TITLE_LENGTH): ValidationResult {
        val sanitized = sanitizeControlChars(title.trim())
        if (sanitized.isEmpty()) {
            return ValidationResult.Invalid("title", MSG_EMPTY)
        }
        if (sanitized.length > maxLength) {
            return ValidationResult.Invalid("title", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证代码片段内容。
     *
     * @param content 代码片段内容
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateSnippetContent(content: String, maxLength: Int = MAX_SNIPPET_CONTENT_LENGTH): ValidationResult {
        if (content.isEmpty()) {
            return ValidationResult.Invalid("content", MSG_EMPTY)
        }
        if (content.length > maxLength) {
            return ValidationResult.Invalid("content", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证代码片段分类。
     *
     * @param category 分类名称
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateSnippetCategory(category: String, maxLength: Int = MAX_SNIPPET_CATEGORY_LENGTH): ValidationResult {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("category", MSG_EMPTY)
        }
        if (trimmed.length > maxLength) {
            return ValidationResult.Invalid("category", MSG_TOO_LONG)
        }
        return ValidationResult.Valid
    }

    /**
     * 验证GitHub URL。
     *
     * 使用白名单正则检查，验证前先进行控制字符清洗。
     *
     * @param url GitHub URL字符串
     * @param maxLength 最大长度限制
     * @return 验证结果
     */
    fun validateGitHubUrl(url: String, maxLength: Int = MAX_GITHUB_URL_LENGTH): ValidationResult {
        val sanitized = sanitizeControlChars(url.trim())
        if (sanitized.isEmpty()) {
            return ValidationResult.Invalid("url", MSG_EMPTY)
        }
        if (sanitized.length > maxLength) {
            return ValidationResult.Invalid("url", MSG_TOO_LONG)
        }
        if (!GITHUB_URL_PATTERN.matcher(sanitized).matches()) {
            return ValidationResult.Invalid("url", MSG_INVALID_URL)
        }
        return ValidationResult.Valid
    }

    /**
     * 转义输入字符串中的正则特殊字符。
     *
     * 转义字符包括：. * + ? ^ $ { } ( ) | [ ] \
     *
     * @param input 待转义的输入字符串
     * @return 转义后的字符串
     */
    fun escapeRegexSpecialChars(input: String): String {
        val specialChars = charArrayOf(
            '.', '*', '+', '?', '^', '$', '{', '}', '(', ')', '|', '[', ']', '\\'
        )
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            if (ch in specialChars) {
                sb.append('\\')
            }
            sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * 清洗输入字符串中的控制字符。
     *
     * 移除\x00-\x1f范围内的字符，但保留\n、\r、\t。
     *
     * @param input 待清洗的输入字符串
     * @return 清洗后的字符串
     */
    fun sanitizeControlChars(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val code = ch.code
            if (code in 0x00..0x1F) {
                if (ch == '\n' || ch == '\r' || ch == '\t') {
                    sb.append(ch)
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
