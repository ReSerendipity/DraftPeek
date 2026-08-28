/**
 * 代码片段仓库实现类。
 *
 * 通过 [SnippetDao] 实现代码片段的持久化操作，由 Hilt 依赖注入提供实例。
 * 所有操作直接委托给 DAO 层执行。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.SnippetDao
import com.draftpeek.core.data.entity.Snippet
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * [SnippetRepository] 的 Room 实现。
 *
 * @property dao 代码片段 DAO 实例
 */
class SnippetRepositoryImpl @Inject constructor(private val dao: SnippetDao) : SnippetRepository {

    override fun getAllSnippets(): Flow<List<Snippet>> = dao.getAllSnippets()

    override fun getSnippetsByCategory(category: String): Flow<List<Snippet>> = dao.getSnippetsByCategory(category)

    override fun getSnippetsByLanguage(language: String): Flow<List<Snippet>> = dao.getSnippetsByLanguage(language)

    override fun searchSnippets(query: String): Flow<List<Snippet>> = dao.searchSnippets(query)

    override fun searchSnippetsByLanguage(query: String, language: String): Flow<List<Snippet>> =
        dao.searchSnippetsByLanguage(query, language)

    override fun searchSnippetsByCategory(query: String, category: String): Flow<List<Snippet>> =
        dao.searchSnippetsByCategory(query, category)

    override fun searchSnippetsSubstring(query: String): Flow<List<Snippet>> = dao.searchSnippetsSubstring(query)

    override fun getSnippetById(id: Long): Flow<Snippet?> = dao.getSnippetById(id)

    override fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()

    override suspend fun addSnippet(snippet: Snippet): Long = dao.insert(snippet)

    override suspend fun updateSnippet(snippet: Snippet) = dao.update(snippet)

    override suspend fun deleteSnippet(snippet: Snippet) = dao.delete(snippet)
}
