/**
 * 最近文件数据访问对象（DAO）接口。
 *
 * 提供对 `recent_files` 表的 CRUD 操作，管理用户最近打开的文件列表、收藏标记、
 * 阅读位置持久化和过期记录清理。记录按最后打开时间降序排列。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.draftpeek.core.data.entity.RecentFile
import kotlinx.coroutines.flow.Flow

/**
 * 最近文件表 DAO 接口。
 *
 * 支持收藏功能、阅读位置（光标/滚动位置）保存、批量删除和数量自动修剪。
 */
@Dao
interface RecentFileDao {

    /**
     * 获取所有最近文件，按最后打开时间降序排列。
     * @return [Flow] 最近文件列表，数据变化时自动更新
     */
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    /**
     * 获取所有已收藏的文件，按最后打开时间降序排列。
     * @return [Flow] 收藏文件列表
     */
    @Query("SELECT * FROM recent_files WHERE isFavorite = 1 ORDER BY lastOpenedAt DESC")
    fun getFavorites(): Flow<List<RecentFile>>

    /**
     * 根据 URI 获取单个最近文件的响应式流。
     * @param uri 文件 URI 字符串
     * @return [Flow] 发射匹配的文件或 null
     */
    @Query("SELECT * FROM recent_files WHERE uri = :uri")
    fun getRecentFile(uri: String): Flow<RecentFile?>

    /**
     * 插入或更新最近文件记录。
     * 如果 URI 已存在则替换（[OnConflictStrategy.REPLACE]）。
     * @param file 要插入/更新的文件实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(file: RecentFile)

    /**
     * 根据 URI 删除单个最近文件记录。
     * @param uri 要删除的文件 URI
     */
    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun delete(uri: String)

    /**
     * 批量删除多个 URI 对应的最近文件记录。
     * @param uris 要删除的 URI 列表
     */
    @Query("DELETE FROM recent_files WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    /**
     * 切换指定文件的收藏状态（取反 isFavorite 字段）。
     * @param uri 文件 URI
     */
    @Query("UPDATE recent_files SET isFavorite = NOT isFavorite WHERE uri = :uri")
    suspend fun toggleFavorite(uri: String)

    /**
     * 删除指定时间之前的所有非收藏文件。
     * 用于自动修剪过期的最近文件记录。
     * @param timestamp 时间戳阈值（毫秒），早于此时间的非收藏记录将被删除
     */
    @Query("DELETE FROM recent_files WHERE lastOpenedAt < :timestamp AND isFavorite = 0")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * 获取指定偏移位置的文件最后打开时间戳。
     * 用于实现 MAX_RECENT_FILES 数量限制修剪逻辑。
     * @param offset 偏移位置（0 表示最新，MAX-1 表示第 N 条）
     * @return 对应位置的时间戳，记录不足时返回 null
     */
    @Query("SELECT lastOpenedAt FROM recent_files ORDER BY lastOpenedAt DESC LIMIT 1 OFFSET :offset")
    suspend fun getTimestampAt(offset: Int): Long?

    /**
     * 删除所有非收藏的最近文件（清空历史但保留收藏）。
     */
    @Query("DELETE FROM recent_files WHERE isFavorite = 0")
    suspend fun deleteAllNonFavorite()

    /**
     * 仅当文件不存在时插入（[OnConflictStrategy.IGNORE]）。
     * 用于首次添加记录而不覆盖已有的元数据（如阅读位置）。
     * @param file 要插入的文件实体
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExist(file: RecentFile)

    /**
     * 更新指定文件的最后打开时间戳。
     * @param uri 文件 URI
     * @param timestamp 新的时间戳（毫秒）
     */
    @Query("UPDATE recent_files SET lastOpenedAt = :timestamp WHERE uri = :uri")
    suspend fun updateLastOpenedAt(uri: String, timestamp: Long)

    /**
     * 保存文件的阅读位置（光标位置和滚动偏移）。
     * @param uri 文件 URI
     * @param line 光标所在行（从 1 开始）
     * @param column 光标所在列（从 1 开始）
     * @param scrollX 水平滚动偏移像素
     * @param scrollY 垂直滚动偏移像素
     */
    @Query(
        "UPDATE recent_files SET cursorLine = :line, cursorColumn = :column, scrollX = :scrollX, scrollY = :scrollY WHERE uri = :uri"
    )
    suspend fun updateReadingPosition(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int)

    /**
     * 获取所有最近文件的 URI 列表（挂起函数，单次查询）。
     * @return URI 字符串列表
     */
    @Query("SELECT uri FROM recent_files")
    suspend fun getAllUris(): List<String>

    /**
     * 根据 URI 获取文件的阅读位置信息（挂起函数）。
     * @param uri 文件 URI
     * @return 匹配的 [RecentFile] 实体，包含保存的阅读位置；未找到返回 null
     */
    @Query("SELECT * FROM recent_files WHERE uri = :uri")
    suspend fun getReadingPosition(uri: String): RecentFile?

    /**
     * 一次性获取所有最近文件列表（非 Flow，用于数据回填等场景）。
     * @return 所有最近文件的实体列表
     */
    @Query("SELECT * FROM recent_files")
    suspend fun getAllRecentFilesOneShot(): List<RecentFile>

    /**
     * 原子地添加/刷新一条最近文件记录并修剪过期记录。
     *
     * 将“插入（若不存在）→ 更新打开时间 → 修剪超额记录”三步包裹在单个事务中，
     * 避免中途崩溃或并发调用导致的中间态（如插入了但时间未更新、或 trim 误删刚插入的记录）。
     * 行为与原先三步分开调用完全一致，仅增加事务原子性。
     *
     * @param file 要插入的文件实体（仅当 uri 不存在时生效）
     * @param now 当前时间戳（毫秒）
     * @param maxRecentFiles 非收藏记录的最大保留数量
     */
    @Transaction
    suspend fun addRecentFileAtomic(file: RecentFile, now: Long, maxRecentFiles: Int) {
        insertIfNotExist(file)
        updateLastOpenedAt(file.uri, now)
        val cutoff = getTimestampAt(maxRecentFiles - 1)
        if (cutoff != null) {
            deleteOlderThan(cutoff)
        }
    }
}
