/**
 * 双向链接仓库实现类。
 *
 * 通过 [LinkDao] 实现链接数据的持久化操作，由 Hilt 依赖注入提供单例实例。
 * 所有数据库操作通过 DAO 层委托执行。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.LinkDao
import com.draftpeek.core.data.entity.LinkEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * [LinkRepository] 的 Room 实现。
 *
 * @property dao 链接 DAO 实例，由 Hilt 注入
 */
class LinkRepositoryImpl @Inject constructor(private val dao: LinkDao) : LinkRepository {

    override fun getOutgoingTitles(sourceUri: String): Flow<List<String>> = dao.getOutgoingTitles(sourceUri)

    override fun getBacklinks(targetTitle: String): Flow<List<String>> = dao.getBacklinks(targetTitle)

    override suspend fun replaceLinks(sourceUri: String, targetTitles: List<String>) {
        val now = System.currentTimeMillis()
        val newLinks = targetTitles
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { LinkEntity(sourceUri = sourceUri, targetTitle = it, updatedAt = now) }
        dao.deleteForSource(sourceUri)
        if (newLinks.isNotEmpty()) {
            dao.insertAll(newLinks)
        }
    }

    override suspend fun removeSource(sourceUri: String) {
        dao.deleteForSource(sourceUri)
    }
}
