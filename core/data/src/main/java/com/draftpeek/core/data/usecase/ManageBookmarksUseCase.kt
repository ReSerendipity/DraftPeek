/**
 * 书签管理用例。
 *
 * 提供书签 CRUD 操作的统一 API，使 ViewModel 与 BookmarkRepository 实现解耦。
 * 支持按目录查询、检查是否已收藏等便捷方法。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.repository.BookmarkRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 书签管理用例。
 *
 * @property repository 书签仓库实例
 */
class ManageBookmarksUseCase @Inject constructor(private val repository: BookmarkRepository) {
    /** 获取所有书签，按文件名排序。 */
    fun allBookmarks(): Flow<List<BookmarkEntity>> = repository.allBookmarks

    /** 获取所有书签的 URI 集合，用于快速判断是否收藏。 */
    fun allBookmarkUris(): Flow<List<String>> = repository.allBookmarkUris

    /** 按目录查询书签。 */
    fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>> =
        repository.getBookmarksByDirectory(directoryUri)

    /** 检查文件是否已收藏。 */
    suspend fun isBookmarked(uri: String): Boolean = repository.isBookmarked(uri)

    /** 添加书签到收藏夹。 */
    suspend fun addBookmark(uri: String, fileName: String, directoryUri: String) =
        repository.addBookmark(uri, fileName, directoryUri)

    /** 从收藏夹移除书签。 */
    suspend fun removeBookmark(uri: String) = repository.removeBookmark(uri)
}
