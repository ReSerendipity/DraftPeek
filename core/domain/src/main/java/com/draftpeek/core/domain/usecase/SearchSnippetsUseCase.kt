/**
 * 搜索代码片段用例文件。
 *
 * 封装代码片段搜索逻辑，包括FTS4全文搜索和LIKE子字符串回退搜索，
 * 以处理FTS分词器可能遗漏的部分匹配。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 搜索代码片段的用例。
 *
 * 封装代码片段搜索逻辑，包括FTS4全文搜索和
 * LIKE子字符串回退，用于FTS分词器可能遗漏的部分匹配。
 */
class SearchSnippetsUseCase @Inject constructor(private val repository: SnippetRepository) {
    /**
     * 使用FTS4进行全文搜索。
     * @param query 搜索查询字符串
     * @return 匹配的代码片段列表Flow流
     */
    operator fun invoke(query: String): Flow<List<Snippet>> = repository.searchSnippets(query)

    /**
     * 限定编程语言的FTS4搜索。
     * @param query 搜索查询字符串
     * @param language 编程语言标识
     * @return 匹配的代码片段列表Flow流
     */
    fun byLanguage(query: String, language: String): Flow<List<Snippet>> =
        repository.searchSnippetsByLanguage(query, language)

    /**
     * 限定分类的FTS4搜索。
     * @param query 搜索查询字符串
     * @param category 分类名称
     * @return 匹配的代码片段列表Flow流
     */
    fun byCategory(query: String, category: String): Flow<List<Snippet>> =
        repository.searchSnippetsByCategory(query, category)

    /**
     * LIKE子字符串回退搜索，用于部分单词匹配。
     * @param query 搜索查询字符串
     * @return 匹配的代码片段列表Flow流
     */
    fun substring(query: String): Flow<List<Snippet>> = repository.searchSnippetsSubstring(query)
}
