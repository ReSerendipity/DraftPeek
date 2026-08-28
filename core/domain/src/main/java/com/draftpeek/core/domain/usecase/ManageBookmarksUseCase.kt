/**
 * 管理书签用例文件。
 *
 * 提供书签CRUD操作的统一API，使ViewModel与BookmarkRepository解耦。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.repository.BookmarkRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 管理文件书签的用例。
 *
 * 提供书签CRUD操作的统一API，
 * 保持ViewModel与BookmarkRepository解耦。
 */
class ManageBookmarksUseCase @Inject constructor(private val repository: BookmarkRepository) {
    /**
     * 获取所有书签的Flow。
     * @return 书签实体列表的Flow流
     */
    fun allBookmarks(): Flow<List<BookmarkEntity>> = repository.allBookmarks

    /**
     * 获取所有书签URI的Flow。
     * @return 书签URI字符串列表的Flow流
     */
    fun allBookmarkUris(): Flow<List<String>> = repository.allBookmarkUris

    /**
     * 获取指定目录下的书签。
     * @param directoryUri 目录URI字符串
     * @return 该目录下书签实体列表的Flow流
     */
    fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>> =
        repository.getBookmarksByDirectory(directoryUri)

    /**
     * 检查文件是否已被收藏。
     * @param uri 文件URI字符串
     * @return 已收藏返回true，否则返回false
     */
    suspend fun isBookmarked(uri: String): Boolean = repository.isBookmarked(uri)

    /**
     * 添加书签。
     * @param uri 文件URI字符串
     * @param fileName 文件名
     * @param directoryUri 所在目录URI字符串
     */
    suspend fun addBookmark(uri: String, fileName: String, directoryUri: String) =
        repository.addBookmark(uri, fileName, directoryUri)

    /**
     * 移除书签。
     * @param uri 文件URI字符串
     */
    suspend fun removeBookmark(uri: String) = repository.removeBookmark(uri)
}
