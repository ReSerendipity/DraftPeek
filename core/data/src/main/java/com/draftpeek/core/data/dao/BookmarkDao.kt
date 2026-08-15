/**
 * 书签数据访问对象（DAO）接口。
 *
 * 提供对 `bookmarks` 表的 CRUD 操作，支持按目录分组查询、URI 唯一性检查和 Flow 响应式查询。
 * 书签用于用户收藏常用文件，按文件名字母序排序展示。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.draftpeek.core.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书签表 DAO 接口。
 *
 * 定义书签文件的增删查操作，所有查询方法返回 [Flow] 以支持数据库变化的自动响应式更新。
 */
@Dao
interface BookmarkDao {

    /**
     * 获取所有书签，按文件名字母序升序排列。
     * @return [Flow] 包含所有书签实体的列表，数据变化时自动发射新值
     */
    @Query("SELECT * FROM bookmarks ORDER BY fileName ASC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    /**
     * 获取指定目录下的所有书签，按文件名字母序升序排列。
     * @param directoryUri 父目录 URI 字符串
     * @return [Flow] 该目录下的书签列表
     */
    @Query("SELECT * FROM bookmarks WHERE directoryUri = :directoryUri ORDER BY fileName ASC")
    fun getBookmarksByDirectory(directoryUri: String): Flow<List<BookmarkEntity>>

    /**
     * 根据 URI 查找书签记录（挂起函数，单次查询）。
     * @param uri 文件 URI 字符串
     * @return 匹配的 [BookmarkEntity]，未找到返回 null
     */
    @Query("SELECT * FROM bookmarks WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): BookmarkEntity?

    /**
     * 插入书签到数据库。
     * 如果 URI 已存在（冲突），则替换旧记录（[OnConflictStrategy.REPLACE]）。
     * @param bookmark 要插入的书签实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    /**
     * 根据 URI 删除书签记录。
     * @param uri 要删除的文件 URI
     */
    @Query("DELETE FROM bookmarks WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    /**
     * 检查指定 URI 是否已被收藏。
     * @param uri 文件 URI 字符串
     * @return true 表示已收藏，false 表示未收藏
     */
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE uri = :uri)")
    suspend fun isBookmarked(uri: String): Boolean

    /**
     * 获取所有已收藏文件的 URI 列表。
     * @return [Flow] 包含所有书签 URI 字符串的列表
     */
    @Query("SELECT uri FROM bookmarks")
    fun getAllBookmarkUris(): Flow<List<String>>
}
