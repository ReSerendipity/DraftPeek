/**
 * 保存文件内容用例。
 *
 * 封装文件写入操作与结果处理，使 ViewModel 与 Repository 实现解耦。
 * 返回 [Result] 强制调用方在调用处显式处理错误。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileRepository
import javax.inject.Inject

/**
 * 保存文件用例。
 *
 * @property repository 编辑器文件仓库实例
 */
class SaveFileUseCase @Inject constructor(
    private val repository: EditorFileRepository,
) {
    /**
     * 将内容写入指定 URI 的文件。
     * @param uri 目标文件 URI
     * @param content 要写入的文本内容
     * @param encoding 可选的文件编码，为 null 时使用 UTF-8
     * @return 成功返回 Unit，失败返回带异常的 Result
     */
    suspend operator fun invoke(
        uri: Uri,
        content: String,
        encoding: String? = null,
    ): Result<Unit> = repository.writeFile(uri, content, encoding)
}
