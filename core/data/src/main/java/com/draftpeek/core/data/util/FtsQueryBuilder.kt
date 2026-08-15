/**
 * SQLite FTS4 搜索查询构建工具。
 *
 * 构建兼容 SQLite FTS4 MATCH 语法的搜索查询字符串，支持：
 * - 隐式 AND："hello world" → 两个词都必须出现
 * - OR："hello OR world" → 任意一个词出现
 * - NOT："hello NOT world" → 包含 hello 但不包含 world
 * - 短语：双引号包裹的精确短语匹配
 * - 列限定：title:hello content:world → 搜索指定列
 *
 * 自动转义用户输入中的 FTS4 特殊字符，避免查询语法错误。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.util

/**
 * FTS4 查询构建器（单例对象）。
 */
object FtsQueryBuilder {

    /** FTS4 特殊字符，需要在 token 中转义 */
    private val FTS_SPECIAL_CHARS = charArrayOf('"', '*', ':', '^', '-', '(', ')')

    /**
     * 构建 AND 查询（所有词必须匹配）。
     * @param terms 搜索词列表
     * @return FTS4 查询字符串
     */
    fun buildAndQuery(terms: List<String>): String {
        if (terms.isEmpty()) return ""
        return terms.map { escapeToken(it) }.joinToString(" ")
    }

    /**
     * 构建 OR 查询（任意词匹配）。
     * @param terms 搜索词列表
     * @return FTS4 查询字符串
     */
    fun buildOrQuery(terms: List<String>): String {
        if (terms.isEmpty()) return ""
        if (terms.size == 1) return escapeToken(terms[0])
        return terms.map { escapeToken(it) }.joinToString(" OR ")
    }

    /**
     * 构建短语查询（精确短语匹配）。
     * @param phrase 短语文本
     * @return 双引号包裹的 FTS4 查询字符串
     */
    fun buildPhraseQuery(phrase: String): String {
        val cleaned = phrase.replace("\"", "")
        if (cleaned.isBlank()) return ""
        return "\"$cleaned\""
    }

    /**
     * 构建 NOT 查询（包含 include，排除 exclude）。
     * @param include 必须包含的词
     * @param exclude 必须排除的词
     * @return FTS4 查询字符串
     */
    fun buildNotQuery(include: String, exclude: String): String {
        val inc = escapeToken(include)
        val exc = escapeToken(exclude)
        if (inc.isBlank()) return ""
        if (exc.isBlank()) return inc
        return "$inc NOT $exc"
    }

    /**
     * 构建列限定查询。
     * @param column 列名
     * @param query 搜索词
     * @return 带列限定前缀的 FTS4 查询字符串
     */
    fun buildColumnQuery(column: String, query: String): String {
        val escaped = escapeToken(query)
        if (escaped.isBlank()) return ""
        return "$column:$escaped"
    }

    /**
     * 根据用户原始输入构建最优的 FTS4 查询。
     *
     * 解析规则：
     * - 双引号文本 → 短语查询
     * - 大写 OR → OR 查询
     * - 减号前缀 → NOT 查询
     * - 空格分隔 → AND 查询（默认）
     *
     * @param rawInput 用户原始输入
     * @return 优化后的 FTS4 查询字符串
     */
    fun buildFromRawInput(rawInput: String): String {
        if (rawInput.isBlank()) return ""
        val input = rawInput.trim()

        if (input.startsWith("\"") && input.endsWith("\"") && input.length > 2) {
            return buildPhraseQuery(input.substring(1, input.length - 1))
        }

        val tokens = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        val result = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]

            if (token.equals("OR", ignoreCase = true) && result.isNotEmpty() && i + 1 < tokens.size) {
                val last = result.removeAt(result.lastIndex)
                val next = escapeToken(tokens[i + 1])
                result.add("$last OR $next")
                i += 2
                continue
            }

            if (token.startsWith("-") && token.length > 1) {
                if (result.isNotEmpty()) {
                    val last = result.removeAt(result.lastIndex)
                    val negated = escapeToken(token.substring(1))
                    result.add("$last NOT $negated")
                }
                i++
                continue
            }

            result.add(escapeToken(token))
            i++
        }

        return result.joinToString(" ")
    }

    /**
     * 转义 token 中的 FTS4 特殊字符。
     * @param token 原始搜索词
     * @return 转义后的安全 token
     */
    private fun escapeToken(token: String): String {
        var escaped = token
        for (c in FTS_SPECIAL_CHARS) {
            escaped = escaped.replace(c, ' ')
        }
        escaped = escaped.trim().replace(Regex("\\s+"), " ")
        return escaped
    }
}
