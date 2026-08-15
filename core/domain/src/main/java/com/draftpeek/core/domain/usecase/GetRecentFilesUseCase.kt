/**
 * 获取最近文件用例文件。
 *
 * 提供最近文件和收藏文件的只读可观察接口，使ViewModel与仓库内部实现解耦。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 观察最近文件和收藏文件的用例。
 *
 * 提供最近文件数据的只读可观察接口，
 * 保持ViewModel与仓库内部实现解耦。
 */
class GetRecentFilesUseCase @Inject constructor(
    private val repository: RecentFilesRepository,
) {
    /**
     * 获取最近文件列表的Flow。
     * @return 最近文件列表的Flow流
     */
    operator fun invoke(): Flow<List<RecentFile>> = repository.recentFiles

    /**
     * 获取收藏文件列表的Flow。
     * @return 收藏文件列表的Flow流
     */
    fun favorites(): Flow<List<RecentFile>> = repository.favorites

    /**
     * 获取指定URI的最近文件。
     * @param uri 文件URI字符串
     * @return 对应的RecentFile的Flow流，不存在则为null
     */
    fun getRecentFile(uri: String): Flow<RecentFile?> = repository.getRecentFile(uri)
}
