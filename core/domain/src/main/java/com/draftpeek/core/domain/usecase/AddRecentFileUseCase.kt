/**
 * 添加最近文件用例文件。
 *
 * 封装将文件添加到最近文件列表的业务逻辑，包括自动修剪旧条目
 * （当列表超过最大容量50个文件时）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import javax.inject.Inject

/**
 * 将文件添加到最近文件列表的用例。
 *
 * 封装将文件记录为最近打开的业务逻辑，包括当列表超过
 * 最大容量（50个文件）时自动修剪旧条目。
 */
class AddRecentFileUseCase @Inject constructor(
    private val repository: RecentFilesRepository,
) {
    /**
     * 执行添加最近文件操作。
     * @param uri 文件URI字符串
     * @param fileName 文件名
     * @param language 编程语言标识（可为null）
     * @param fileSize 文件大小（字节）
     */
    suspend operator fun invoke(
        uri: String,
        fileName: String,
        language: String?,
        fileSize: Long,
    ) {
        repository.addRecentFile(uri, fileName, language, fileSize)
    }
}
