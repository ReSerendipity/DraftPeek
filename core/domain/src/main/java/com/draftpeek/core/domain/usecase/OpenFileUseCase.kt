/**
 * 打开文件用例文件。
 *
 * 封装文件读取操作，包括编码检测、二进制文件处理和大文件策略（流式预览），
 * 遵循离线优先模式，返回EditorFileReadOutcome要求调用方显式处理成功和错误路径。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileReadOutcome
import com.draftpeek.core.data.repository.EditorFileRepository
import javax.inject.Inject

/**
 * 在编辑器中打开/读取文件的用例。
 *
 * 封装文件读取操作，包括编码检测、二进制文件处理和
 * 大文件策略（流式预览），保持ViewModel与仓库内部实现解耦。
 *
 * 返回[EditorFileReadOutcome]遵循离线优先模式：
 * 调用方必须显式处理Success和Error路径。
 */
class OpenFileUseCase @Inject constructor(private val repository: EditorFileRepository) {
    /**
     * 执行文件读取操作。
     * @param uri 文件URI
     * @param encoding 指定编码（可选，为null时自动检测）
     * @return 文件读取结果
     */
    suspend operator fun invoke(uri: Uri, encoding: String? = null): EditorFileReadOutcome =
        repository.readFile(uri, encoding)

    /**
     * 检查是否为应用内部文件。
     * @param uriString 文件URI字符串
     * @return 是内部文件返回true
     */
    fun isInternalFile(uriString: String): Boolean = repository.isInternalFile(uriString)

    /**
     * 删除内部文件。
     * @param uriString 文件URI字符串
     * @return 删除成功返回true
     */
    suspend fun deleteInternalFile(uriString: String): Boolean = repository.deleteInternalFile(uriString)
}
