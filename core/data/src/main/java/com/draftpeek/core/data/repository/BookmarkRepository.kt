/**
 * 书签仓库接口。
 *
 * 遵循 Clean Architecture 原则，定义书签功能的数据层契约。
 * 提供收藏/取消收藏文件、查询收藏列表、检查收藏状态等操作，
 * 实现类负责将业务逻辑与 DAO 层解耦。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签数据仓库接口。
 *
 * 所有返回 [Flow] 的方法支持响应式更新，当数据库变化时自动发射新值。
 */
interface BookmarkRepository {

    /**
     * 获取所有书签的响应式流，按文件名排序。
     */
    val allBookmarks: Flow<List<BookmarkEntity>>

    /**
     * 获取指定目录下的书签响应式流。
     * @param directoryUri 父目录 URI
     */
    fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>>

    /**
     * 获取所有已收藏文件 URI 的响应式流。
     */
    val allBookmarkUris: Flow<List<String>>

    /**
     * 检查指定 URI 是否已被收藏（挂起函数）。
     * @param uri 文件 URI
     * @return true 表示已收藏
     */
    suspend fun isBookmarked(uri: String): Boolean

    /**
     * 添加文件到收藏。
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param directoryUri 父目录 URI
     */
    suspend fun addBookmark(uri: String, fileName: String, directoryUri: String)

    /**
     * 从收藏中移除文件。
     * @param uri 文件 URI
     */
    suspend fun removeBookmark(uri: String)
}
