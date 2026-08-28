/**
 * 代码片段搜索用例。
 *
 * 封装片段搜索逻辑，包括 FTS4 全文搜索以及 FTS 分词器可能遗漏的
 * LIKE 子串匹配回退方案。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 代码片段搜索用例。
 *
 * @property repository 代码片段仓库实例
 */
class SearchSnippetsUseCase @Inject constructor(private val repository: SnippetRepository) {
    /**
     * 使用 FTS4 进行全文搜索。
     * @param query 搜索关键词
     * @return 匹配的片段列表流
     */
    operator fun invoke(query: String): Flow<List<Snippet>> = repository.searchSnippets(query)

    /**
     * 限定编程语言的 FTS4 搜索。
     * @param query 搜索关键词
     * @param language 编程语言标识（如 "kotlin", "python"）
     */
    fun byLanguage(query: String, language: String): Flow<List<Snippet>> =
        repository.searchSnippetsByLanguage(query, language)

    /**
     * 限定分类的 FTS4 搜索。
     * @param query 搜索关键词
     * @param category 片段分类
     */
    fun byCategory(query: String, category: String): Flow<List<Snippet>> =
        repository.searchSnippetsByCategory(query, category)

    /**
     * LIKE 子串匹配回退，用于部分单词匹配。
     * @param query 搜索子串
     */
    fun substring(query: String): Flow<List<Snippet>> = repository.searchSnippetsSubstring(query)
}
