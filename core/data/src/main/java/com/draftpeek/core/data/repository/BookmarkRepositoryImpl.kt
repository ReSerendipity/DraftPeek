/**
 * 书签仓库实现类。
 *
 * 通过 [BookmarkDao] 实现书签数据的持久化操作，由 Hilt 依赖注入提供单例实例。
 * 所有数据库操作通过 DAO 层委托执行，保持仓库层的简洁性。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.BookmarkDao
import com.draftpeek.core.data.entity.BookmarkEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * [BookmarkRepository] 的 Room 实现。
 *
 * @property dao 书签 DAO 实例，由 Hilt 注入
 */
class BookmarkRepositoryImpl @Inject constructor(private val dao: BookmarkDao) : BookmarkRepository {

    override val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    override fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>> =
        dao.getBookmarksByDirectory(directoryUri)

    override val allBookmarkUris: Flow<List<String>> = dao.getAllBookmarkUris()

    override suspend fun isBookmarked(uri: String): Boolean = dao.isBookmarked(uri)

    override suspend fun addBookmark(uri: String, fileName: String, directoryUri: String) {
        dao.insert(
            BookmarkEntity(
                uri = uri,
                fileName = fileName,
                directoryUri = directoryUri
            )
        )
    }

    override suspend fun removeBookmark(uri: String) {
        dao.deleteByUri(uri)
    }
}
