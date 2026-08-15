/**
 * 文件：MarkdownOutline.kt
 * 功能：Markdown 大纲数据模型与解析器
 * 主要类/接口：OutlineHeading、MarkdownOutlineParser
 * 模块依赖：无外部依赖
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable

/**
 * 表示 Markdown 文档大纲中的一个标题项。
 *
 * 用于 OutlineDrawer 侧边大纲导航。
 *
 * @property level 标题级别（1-6，对应 Markdown 的 # 到 ######）
 * @property text 标题文本（不包含 # 标记）
 * @property lineIndex 源文档中基于 0 的行索引，用于点击跳转
 */
@Immutable
data class OutlineHeading(
    val level: Int,
    val text: String,
    val lineIndex: Int,
)

/**
 * 从 Markdown 源文本解析标题的工具对象。
 *
 * 支持两种 Markdown 标题风格：
 * 1. ATX 风格：以 # 开头（# 标题、## 二级标题 等）
 * 2. Setext 风格：标题文本下一行用 ===（一级）或 ---（二级）下划线
 *
 * 解析算法：
 * 1. 逐行扫描文本
 * 2. 用正则匹配 ATX 风格标题
 * 3. 检查下一行是否为 Setext 风格下划线
 * 4. 收集所有标题并返回列表
 */
object MarkdownOutlineParser {
    private val atxRegex = Regex("^(#{1,6})\\s+(.+)$")

    /**
     * 从给定 Markdown 源文本提取所有标题。
     *
     * 支持 ATX 风格和 Setext 风格标题。
     *
     * @param markdown Markdown 源文本
     * @return 按文档顺序排列的标题列表
     */
    fun parse(markdown: String): List<OutlineHeading> {
        val headings = mutableListOf<OutlineHeading>()
        val lines = markdown.lines()

        for ((index, line) in lines.withIndex()) {
            // 步骤1：匹配 ATX 风格标题（# 开头）
            val atxMatch = atxRegex.find(line)
            if (atxMatch != null) {
                val level = atxMatch.groupValues[1].length
                val text = atxMatch.groupValues[2].removeSuffix("#").trim()
                headings.add(OutlineHeading(level, text, index))
                continue
            }

            // 步骤2：检查 Setext 风格标题（下一行用 === 或 --- 下划线）
            if (index + 1 < lines.size) {
                val nextLine = lines[index + 1].trim()
                if (nextLine.isNotEmpty() && line.isNotBlank()) {
                    if (nextLine.all { it == '=' } && nextLine.length >= 2) {
                        // === 下划线表示一级标题
                        headings.add(OutlineHeading(1, line.trim(), index))
                    } else if (nextLine.all { it == '-' } && nextLine.length >= 2) {
                        // --- 下划线表示二级标题
                        headings.add(OutlineHeading(2, line.trim(), index))
                    }
                }
            }
        }

        return headings
    }
}
