/**
 * 获取最近文件和收藏文件用例。
 *
 * 提供最近文件数据的只读可观察接口，使 ViewModel 与 Repository 内部实现解耦。
 * 支持获取全部最近文件、仅收藏文件，以及查询单个文件的最新状态。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 获取最近文件用例。
 *
 * @property repository 最近文件仓库实例
 */
class GetRecentFilesUseCase @Inject constructor(private val repository: RecentFilesRepository) {
    /**
     * 获取所有最近文件（含收藏）的 Flow。
     * @return 最近文件列表流，按最后打开时间倒序排列
     */
    operator fun invoke(): Flow<List<RecentFile>> = repository.recentFiles

    /**
     * 获取收藏文件的 Flow。
     * @return 收藏文件列表流，按文件名排序
     */
    fun favorites(): Flow<List<RecentFile>> = repository.favorites

    /**
     * 获取指定 URI 的最近文件记录。
     * @param uri 文件 URI
     * @return 最近文件实体流，不存在时发射 null
     */
    fun getRecentFile(uri: String): Flow<RecentFile?> = repository.getRecentFile(uri)
}
