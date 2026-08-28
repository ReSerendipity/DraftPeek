/**
 * 阅读位置用例文件。
 *
 * 封装编辑器阅读位置（光标/滚动状态）的保存和获取逻辑，
 * 使ViewModel无需了解底层存储机制。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 保存和获取阅读位置（光标/滚动状态）的用例。
 *
 * 封装编辑器阅读位置的持久化逻辑，使ViewModel无需了解
 * 底层存储机制。
 */
class ReadingPositionUseCase @Inject constructor(private val repository: RecentFilesRepository) {
    /**
     * 保存阅读位置。
     * @param uri 文件URI字符串
     * @param line 光标所在行号
     * @param column 光标所在列号
     * @param scrollX 水平滚动偏移
     * @param scrollY 垂直滚动偏移
     */
    suspend fun save(uri: String, line: Int, column: Int, scrollX: Int, scrollY: Int) {
        repository.saveReadingPosition(uri, line, column, scrollX, scrollY)
    }

    /**
     * 获取保存的阅读位置。
     * @param uri 文件URI字符串
     * @return 包含阅读位置的RecentFile对象，未找到返回null
     */
    suspend fun get(uri: String): RecentFile? = repository.getReadingPosition(uri)
}
