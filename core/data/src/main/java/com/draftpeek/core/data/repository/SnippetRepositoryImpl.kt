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
import com.draftpeek.core.data.mapper.toDomain
import com.draftpeek.core.data.mapper.toEntity
import com.draftpeek.core.domain.model.Snippet as SnippetModel
import com.draftpeek.core.domain.repository.SnippetRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [SnippetRepository] 的 Room 实现。
 *
 * @property dao 代码片段 DAO 实例
 */
class SnippetRepositoryImpl @Inject constructor(private val dao: SnippetDao) : SnippetRepository {

    override fun getAllSnippets(): Flow<List<SnippetModel>> =
        dao.getAllSnippets().map { list -> list.map { it.toDomain() } }

    override fun getSnippetsByCategory(category: String): Flow<List<SnippetModel>> =
        dao.getSnippetsByCategory(category).map { list -> list.map { it.toDomain() } }

    override fun getSnippetsByLanguage(language: String): Flow<List<SnippetModel>> =
        dao.getSnippetsByLanguage(language).map { list -> list.map { it.toDomain() } }

    override fun searchSnippets(query: String): Flow<List<SnippetModel>> =
        dao.searchSnippets(query).map { list -> list.map { it.toDomain() } }

    override fun searchSnippetsByLanguage(query: String, language: String): Flow<List<SnippetModel>> =
        dao.searchSnippetsByLanguage(query, language).map { list -> list.map { it.toDomain() } }

    override fun searchSnippetsByCategory(query: String, category: String): Flow<List<SnippetModel>> =
        dao.searchSnippetsByCategory(query, category).map { list -> list.map { it.toDomain() } }

    override fun searchSnippetsSubstring(query: String): Flow<List<SnippetModel>> =
        dao.searchSnippetsSubstring(query).map { list -> list.map { it.toDomain() } }

    override fun getSnippetById(id: Long): Flow<SnippetModel?> = dao.getSnippetById(id).map { it?.toDomain() }

    override fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()

    /**
     * 新增片段。
     *
     * 若未显式提供 [SnippetModel.syncId]，这里**兜底生成** UUID —— syncId 必须「创建时生成、
     * 终身不变」，不能指望每个调用方都记得；漏了会得到空键，同步时所有空键片段互相覆盖。
     */
    override suspend fun addSnippet(snippet: SnippetModel): Long {
        val toInsert = if (snippet.syncId.isEmpty()) {
            snippet.copy(syncId = UUID.randomUUID().toString())
        } else {
            snippet
        }
        return dao.insert(toInsert.toEntity())
    }

    override suspend fun updateSnippet(snippet: SnippetModel) = dao.update(snippet.toEntity())

    override suspend fun deleteSnippet(snippet: SnippetModel) = dao.delete(snippet.toEntity())
}
