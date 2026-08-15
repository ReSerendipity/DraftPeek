/**
 * 最近文件仓库接口。
 *
 * 管理用户最近打开的文件历史列表，支持收藏、阅读位置持久化、过期清理等功能。
 * 收藏的文件不会被自动修剪，非收藏文件保留最近 MAX_RECENT_FILES 条记录。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.entity.RecentFile
import kotlinx.coroutines.flow.Flow

/**
 * 最近文件数据仓库接口。
 */
interface RecentFilesRepository {

    /** 所有最近文件的响应式流，按最后打开时间降序排列 */
    val recentFiles: Flow<List<RecentFile>>

    /** 所有收藏文件的响应式流 */
    val favorites: Flow<List<RecentFile>>

    /**
     * 获取指定 URI 文件的响应式流。
     * @param uri 文件 URI
     */
    fun getRecentFile(uri: String): Flow<RecentFile?>

    /**
     * 添加文件到最近文件列表，如已存在则更新打开时间。
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param language 编程语言标识
     * @param fileSize 文件大小（字节）
     */
    suspend fun addRecentFile(uri: String, fileName: String, language: String?, fileSize: Long)

    /**
     * 从最近文件列表中移除指定文件。
     * @param uri 文件 URI
     */
    suspend fun removeRecentFile(uri: String)

    /**
     * 批量移除多个文件。
     * @param uris 要移除的 URI 列表
     */
    suspend fun removeRecentFiles(uris: List<String>)

    /**
     * 切换指定文件的收藏状态。
     * @param uri 文件 URI
     */
    suspend fun toggleFavorite(uri: String)

    /** 清除所有非收藏的最近文件记录 */
    suspend fun clearAllRecentFiles()

    /**
     * 获取已失效（文件不存在）的内部文件 URI 列表。
     * 用于清理指向已删除文件的过期记录。
     * @return 失效的 URI 列表
     */
    suspend fun getStaleUris(): List<String>

    /**
     * 保存文件的阅读位置（光标行/列、滚动偏移）。
     * @param uri 文件 URI
     * @param line 光标行号（从 1 开始）
     * @param column 光标列号（从 1 开始）
     * @param scrollX 水平滚动偏移
     * @param scrollY 垂直滚动偏移
     */
    suspend fun saveReadingPosition(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int)

    /**
     * 获取文件保存的阅读位置。
     * @param uri 文件 URI
     * @return 包含阅读位置的 [RecentFile]，未找到返回 null
     */
    suspend fun getReadingPosition(uri: String): RecentFile?
}
