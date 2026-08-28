/**
 * Markdown 双链语法解析器。
 *
 * 从文本中提取双链引用 `[[目标标题]]`（DraftPeek 的双向链接语法）与
 * `@提及`（@后跟一个词），供双向链接索引与反向链接展示使用。
 *
 * 规则（与 Logseq 双链语义一致，保持轻量）：
 * - `[[目标标题]]`：提取括号内内容作为目标标题，支持 `目标标题|显示文本` 别名形式
 * - 内嵌的代码块 / `反引号` 内部的双链会被忽略，避免把代码里的 `[[...]]` 误当链接
 * - `@提及`：@后连续的中英文/数字/下划线视为提及目标（可与 [[标题]] 并存、去重）
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.util

/**
 * 解析结果：文档中的全部引用目标。
 *
 * @property linkedTitles `[[...]]` 双链提取出的目标标题列表
 * @property mentions `@提及` 提取出的目标列表
 */
data class MarkdownLinkParseResult(val linkedTitles: List<String>, val mentions: List<String>) {
    /** 去重合并后的全部引用目标（双链 + 提及） */
    val allTargets: List<String>
        get() = (linkedTitles + mentions).distinct()
}

/**
 * Markdown 双链 / @提及 解析器单例对象。
 */
object MarkdownLinkParser {

    // 匹配 [[目标]] 或 [[目标|显示文本]]
    private val WIKILINK_REGEX = Regex("""\[\[([^\[\]\n]+)]]""")

    // 匹配 @提及（@后跟中英文/数字/下划线），排除 @ 位于 URL / 邮箱中被误抓的场景
    private val MENTION_REGEX = Regex("""(?<![\w@])@([\p{L}\p{N}_]+)""")

    /**
     * 解析文本中的全部引用目标。
     *
     * 忽略被反引号包裹（代码/行内代码）的内容，确保代码示例中的
     * `[[...]]` 或 `@...` 不会被误判为链接。
     *
     * @param content 源文本（Markdown 正文）
     * @return 解析结果，包含双链目标与 @提及
     */
    fun parse(content: String): MarkdownLinkParseResult {
        if (content.isBlank()) return MarkdownLinkParseResult(emptyList(), emptyList())

        // 先剔除反引号包裹的代码片段（含行内 code 与 ``` fenced block ```）
        val withoutCode = removeCodeSpans(content)

        val wikilinks = WIKILINK_REGEX
            .findAll(withoutCode)
            .map { match ->
                val inner = match.groupValues[1].trim()
                // 支持 [[目标|显示文本]]：取 | 前的目标标题
                if (inner.contains('|')) inner.substringBefore('|').trim() else inner
            }
            .filter { it.isNotBlank() }
            .toList()

        val mentions = MENTION_REGEX
            .findAll(withoutCode)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()

        return MarkdownLinkParseResult(
            linkedTitles = wikilinks.distinct(),
            mentions = mentions.distinct()
        )
    }

    /**
     * 移除代码片段（反引号包裹的内容），避免把代码中的括号/@当链接解析。
     *
     * 同时处理：
     * - 行内代码：`...`
     * - 围栏代码块：``` ... ```（含语言标识）
     */
    private fun removeCodeSpans(content: String): String {
        // 先去除围栏代码块（``` ... ```，可跨行）
        val noFenced = FENCED_CODE_BLOCK_REGEX.replace(content) { "" }
        // 再去除行内代码（`...`，不跨行）
        return INLINE_CODE_REGEX.replace(noFenced) { "" }
    }

    private val FENCED_CODE_BLOCK_REGEX = Regex("""```[\s\S]*?```""")
    private val INLINE_CODE_REGEX = Regex("""`[^`\n]*`""")
}
