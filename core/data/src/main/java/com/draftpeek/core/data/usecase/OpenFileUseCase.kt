/**
 * 打开/读取文件用例。
 *
 * 封装文件读取操作，包括编码检测、二进制文件处理、大文件策略（流式预览），
 * 使 ViewModel 与 Repository 内部实现解耦。
 *
 * 遵循离线优先模式：返回 [EditorFileReadOutcome]，调用方必须显式处理
 * Success 和 Error 两种路径。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileReadOutcome
import com.draftpeek.core.data.repository.EditorFileRepository
import javax.inject.Inject

/**
 * 打开文件用例。
 *
 * @property repository 编辑器文件仓库实例
 */
class OpenFileUseCase @Inject constructor(
    private val repository: EditorFileRepository,
) {
    /**
     * 读取指定 URI 的文件内容。
     * @param uri 文件 URI
     * @param encoding 可选的文件编码，为 null 时自动检测
     * @return 文件读取结果（成功或失败）
     */
    suspend operator fun invoke(
        uri: Uri,
        encoding: String? = null,
    ): EditorFileReadOutcome = repository.readFile(uri, encoding)

    /** 判断 URI 是否指向应用内部文件。 */
    fun isInternalFile(uriString: String): Boolean =
        repository.isInternalFile(uriString)

    /** 删除内部存储的文件。 */
    suspend fun deleteInternalFile(uriString: String): Boolean =
        repository.deleteInternalFile(uriString)
}
