/**
 * 阅读位置保存/获取用例。
 *
 * 封装编辑器光标位置和滚动位置的持久化逻辑，
 * 使 ViewModel 不需要了解底层存储机制。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 阅读位置用例。
 *
 * @property repository 最近文件仓库实例
 */
class ReadingPositionUseCase @Inject constructor(
    private val repository: RecentFilesRepository,
) {
    /**
     * 保存当前阅读位置。
     * @param uri 文件 URI
     * @param line 光标所在行（从 1 开始）
     * @param column 光标所在列（从 1 开始）
     * @param scrollX 水平滚动偏移
     * @param scrollY 垂直滚动偏移
     */
    suspend fun save(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int) {
        repository.saveReadingPosition(uri, line, column, scrollX, scrollY)
    }

    /**
     * 获取上次保存的阅读位置。
     * @param uri 文件 URI
     * @return 包含阅读位置的最近文件实体，不存在时返回 null
     */
    suspend fun get(uri: String): RecentFile? = repository.getReadingPosition(uri)
}
