/**
 * 最近文件仓库实现类。
 *
 * 通过 [RecentFileDao] 实现最近文件的持久化操作，包含自动修剪逻辑（保持最多 MAX_RECENT_FILES 条非收藏记录）。
 * 提供失效 URI 检测功能，用于清理指向已删除文件的过期记录。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import android.content.Context
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.entity.RecentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * [RecentFilesRepository] 的 Room 实现。
 *
 * @property dao 最近文件 DAO 实例
 * @property context 应用上下文，用于检查内部文件是否存在
 */
class RecentFilesRepositoryImpl @Inject constructor(
    private val dao: RecentFileDao,
    @param:ApplicationContext private val context: Context
) : RecentFilesRepository {

    companion object {
        /** 最大保留的最近文件数量（不含收藏） */
        private const val MAX_RECENT_FILES = 50
    }

    override val recentFiles: Flow<List<RecentFile>> = dao.getAllRecentFiles()

    override val favorites: Flow<List<RecentFile>> = dao.getFavorites()

    override fun getRecentFile(uri: String): Flow<RecentFile?> = dao.getRecentFile(uri)

    override suspend fun addRecentFile(uri: String, fileName: String, language: String?, fileSize: Long) {
        val now = System.currentTimeMillis()
        // 三步操作（插入 / 更新时间 / 修剪）在单个事务内原子执行，避免并发中间态。
        dao.addRecentFileAtomic(
            RecentFile(
                uri = uri,
                fileName = fileName,
                language = language,
                lastOpenedAt = now,
                fileSize = fileSize
            ),
            now = now,
            maxRecentFiles = MAX_RECENT_FILES
        )
    }

    override suspend fun removeRecentFile(uri: String) {
        dao.delete(uri)
    }

    override suspend fun removeRecentFiles(uris: List<String>) {
        dao.deleteByUris(uris)
    }

    override suspend fun toggleFavorite(uri: String) {
        dao.toggleFavorite(uri)
    }

    override suspend fun clearAllRecentFiles() {
        dao.deleteAllNonFavorite()
    }

    override suspend fun getStaleUris(): List<String> {
        val allUris = dao.getAllUris()
        return allUris.filter { uri ->
            if (AppFileManager.isInternalUri(uri)) {
                val file = AppFileManager.getInternalFileFromUri(context, uri)
                file == null
            } else {
                false
            }
        }
    }

    override suspend fun saveReadingPosition(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int) {
        dao.updateReadingPosition(uri, line, column, scrollX, scrollY)
    }

    override suspend fun getReadingPosition(uri: String): RecentFile? = dao.getReadingPosition(uri)
}
