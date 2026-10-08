package com.draftpeek.feature.editor.sora

import com.draftpeek.core.common.util.InputValidator

/**
 * 搜索匹配计数（实施指导书 §2.3 屏 15 的「匹配计数 n/N」）。
 *
 * **为什么需要自己实现**：sora 的 `EditorSearcher` 只暴露「是否命中」的布尔值
 * （`search` / `gotoNext` / `gotoPrevious` 均无计数），拿不到匹配总数。
 * 这里在缓冲区文本上按**与 [SoraSearchManager] 完全一致的口径**复算匹配位置：
 *
 * | 选项组合 | SoraSearchManager 的做法 | 本实现 |
 * |---|---|---|
 * | `regex = false` | `InputValidator.escapeRegexSpecialChars(query)` 后按正则检索 | 同 |
 * | `regex = true` + `wholeWord` | `\b(?:query)\b` | 同 |
 * | `regex = false` + `wholeWord` | 交给 sora 的 `TYPE_WHOLE_WORD` | `\b(?:escaped)\b` |
 * | `matchCase = false` | `caseInsensitive = true` | `RegexOption.IGNORE_CASE` |
 *
 * 纯函数，完全可单元测试。
 */
internal object SearchMatchCounter {

    /**
     * 单次检索的匹配数上限。
     *
     * 用户可能输入单字符查询（如 `e`），在数万行文件上会产生海量匹配；
     * 计数只为展示 `n/N`，没必要算全量 ⇒ 到达上限即停止。
     */
    const val MAX_MATCHES = 10_000

    /**
     * 找出 [text] 中所有匹配区间（字符偏移，闭区间 `first..last`）。
     *
     * @param text 缓冲区全文
     * @param query 用户输入的查询串
     * @param regex 是否按正则解释查询
     * @param matchCase 是否区分大小写
     * @param wholeWord 是否整词匹配
     * @return 按出现顺序排列的区间；查询为空、正则非法或无匹配时返回空列表
     */
    fun findAll(
        text: String,
        query: String,
        regex: Boolean = false,
        matchCase: Boolean = false,
        wholeWord: Boolean = false
    ): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val pattern = buildPattern(query, regex, wholeWord)
        val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
        return try {
            pattern.toRegex(options).findAll(text).take(MAX_MATCHES).map { it.range }.toList()
        } catch (_: Exception) {
            // 非法正则：用户边输边改时很常见（如只敲了一个 `[`）——静默返回空，不打断输入
            emptyList()
        }
    }

    /**
     * 当前光标偏移落在第几个匹配上（**1-based**）。
     *
     * 分两步判定，**顺序不能反**：
     * 1. 先找「光标落在匹配区间内」（`first..last`）的匹配；
     * 2. 没有命中时，才接受「光标紧跟在某匹配之后」（`last + 1`，对应选区停在匹配末尾）。
     *
     * ⚠️ 不能把两步合成 `cursorOffset in it.first..(it.last + 1)`：**相邻匹配**时
     * 前一个匹配的 `last + 1` 恰好是后一个匹配的 `first`（如 `"bb"` 中两个 `b` 是
     * `0..0` 与 `1..1`），合并后 `indexOfFirst` 会先命中前一个，导致序号永远停在
     * `1/N`、↑↓ 导航看起来不生效（真机走查发现）。
     *
     * @return 命中时返回序号；光标不在任何匹配上（含其末尾）时返回 0
     */
    fun ordinalAt(matches: List<IntRange>, cursorOffset: Int): Int {
        val inside = matches.indexOfFirst { cursorOffset in it.first..it.last }
        if (inside >= 0) return inside + 1
        val justAfter = matches.indexOfFirst { cursorOffset == it.last + 1 }
        return if (justAfter >= 0) justAfter + 1 else 0
    }

    /** 与 [SoraSearchManager] 的 `buildSearchQuery` 同口径地构造正则串。 */
    private fun buildPattern(query: String, regex: Boolean, wholeWord: Boolean): String = when {
        regex && wholeWord -> "\\b(?:$query)\\b"
        regex -> query
        wholeWord -> "\\b(?:${InputValidator.escapeRegexSpecialChars(query)})\\b"
        else -> InputValidator.escapeRegexSpecialChars(query)
    }
}
