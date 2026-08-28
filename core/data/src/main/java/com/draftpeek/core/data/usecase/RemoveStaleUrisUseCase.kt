/**
 * 清理失效 URI 用例。
 *
 * 扫描最近文件列表，找出指向磁盘上已不存在的内部文件的 URI，并将其移除。
 * 这是一个维护操作，应定期调用（如应用启动时）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 清理失效 URI 用例。
 *
 * @property repository 最近文件仓库实例
 */
class RemoveStaleUrisUseCase @Inject constructor(private val repository: RecentFilesRepository) {
    /**
     * 执行清理操作。
     *
     * 首先查找所有磁盘上已不存在的内部文件 URI，然后批量从数据库移除。
     * 如果没有失效 URI，则不执行任何操作。
     */
    suspend operator fun invoke() {
        val staleUris = repository.getStaleUris()
        if (staleUris.isNotEmpty()) {
            repository.removeRecentFiles(staleUris)
        }
    }
}
