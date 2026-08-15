/**
 * 清理过期URI用例文件。
 *
 * 扫描最近文件列表中指向磁盘上已不存在的内部文件的URI并删除它们，
 * 这是一个应定期调用的维护操作（如应用启动时）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 从最近文件中移除过期URI的用例。
 *
 * 扫描最近文件中指向磁盘上已不存在的内部文件的URI，并删除它们。
 * 这是一个应定期调用的维护操作（例如应用启动时）。
 */
class RemoveStaleUrisUseCase @Inject constructor(
    private val repository: RecentFilesRepository,
) {
    /**
     * 执行清理过期URI操作。
     * 获取所有过期URI，如果存在则批量删除。
     */
    suspend operator fun invoke() {
        val staleUris = repository.getStaleUris()
        if (staleUris.isNotEmpty()) {
            repository.removeRecentFiles(staleUris)
        }
    }
}
