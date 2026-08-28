/**
 * 文件：DocumentStats.kt
 * 功能：文档统计数据模型与计算器
 * 主要类/接口：DocumentStats、DocumentStatsCalculator
 * 模块依赖：无外部依赖
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable

/**
 * 文本文件的文档统计数据。
 *
 * @property wordCount 单词数（中文字符每个算1个单词，英文单词按空格分隔）
 * @property charCount 字符总数（包括空格和换行）
 * @property lineCount 行数
 */
@Immutable
data class DocumentStats(val wordCount: Int, val charCount: Int, val lineCount: Int)

/**
 * 从文本内容计算文档统计数据的工具对象。
 *
 * 单词计数规则：
 * - 中日韩（CJK）字符每个算作 1 个单词
 * - 英文单词按空白字符分隔，连续的字母数字序列算作一个单词
 *
 * 算法说明：
 * 单次线性遍历文本字符，同时完成 CJK 字符计数与英文单词计数。
 * 相比原先的两次全文正则匹配（`cjkRegex.findAll` + `wordRegex.findAll`），
 * 避免了为每个匹配生成 MatchResult/Sequence 中间对象带来的 GC 压力，
 * 对大文件（数十 KB 以上）尤为明显。计数结果与原正则实现完全等价。
 */
object DocumentStatsCalculator {

    /**
     * 计算文本的文档统计数据。
     *
     * 单词数 = CJK 字符数 + 英文单词数，其中：
     * - CJK 字符：码点落在 [U+4E00,U+9FFF]、[U+3400,U+4DBF]、[U+3000,U+303F]、[U+FF00,U+FFEF]
     *   （CJK 统一表意文字、扩展 A 区、CJK 标点、全角符号）——与原 `cjkRegex` 单字符类等价。
     * - 英文单词：连续的 ASCII 字母或数字 `[a-zA-Z0-9]+` 序列，每段算 1 个——与原 `wordRegex` 等价。
     *
     * 所有 CJK 范围均位于 BMP（< U+10000），因此按 UTF-16 code unit 逐字符判断与原正则行为一致。
     *
     * @param text 要统计的文本内容
     * @return 包含单词数、字符数、行数的 DocumentStats 对象
     */
    fun calculate(text: String): DocumentStats {
        val lines = if (text.isEmpty()) 0 else text.lines().size
        val chars = text.length

        var cjkCount = 0
        var wordCount = 0
        var inWord = false
        for (ch in text) {
            val code = ch.code
            val isCjk = code in 0x4E00..0x9FFF ||
                code in 0x3400..0x4DBF ||
                code in 0x3000..0x303F ||
                code in 0xFF00..0xFFEF
            if (isCjk) cjkCount++

            val isWordChar = ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9'
            if (isWordChar) {
                if (!inWord) {
                    wordCount++
                    inWord = true
                }
            } else {
                inWord = false
            }
        }

        return DocumentStats(
            wordCount = cjkCount + wordCount,
            charCount = chars,
            lineCount = lines
        )
    }
}
