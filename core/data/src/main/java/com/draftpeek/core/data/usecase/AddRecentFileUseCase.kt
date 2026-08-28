/**
 * 添加文件到最近文件列表的用例。
 *
 * 封装将文件记录为最近打开的业务逻辑，Repository 层自动处理旧记录修剪
 * （保持最多 50 条非收藏记录）。
 *
 * 遵循 Clean Architecture 用例模式：通过 operator fun invoke() 提供简洁的调用语法。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 添加最近文件用例。
 *
 * @property repository 最近文件仓库实例
 */
class AddRecentFileUseCase @Inject constructor(private val repository: RecentFilesRepository) {
    /**
     * 执行添加最近文件操作。
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param language 编程语言标识
     * @param fileSize 文件大小（字节）
     */
    suspend operator fun invoke(uri: String, fileName: String, language: String?, fileSize: Long) {
        repository.addRecentFile(uri, fileName, language, fileSize)
    }
}
