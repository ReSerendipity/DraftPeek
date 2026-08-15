/**
 * 保存文件用例文件。
 *
 * 封装文件写入操作及结果处理，返回Result以强制调用点显式错误处理，
 * 使ViewModel与仓库实现解耦。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileRepository
import javax.inject.Inject

/**
 * 保存文件内容的用例。
 *
 * 封装带结果处理的文件写入操作，保持ViewModel与仓库实现解耦。
 * 返回Result以强制调用点显式错误处理。
 */
class SaveFileUseCase @Inject constructor(
    private val repository: EditorFileRepository,
) {
    /**
     * 执行文件保存操作。
     * @param uri 文件URI
     * @param content 文件内容
     * @param encoding 文件编码（可选，为null时使用默认编码）
     * @return 保存结果，成功为Unit，失败包含异常信息
     */
    suspend operator fun invoke(
        uri: Uri,
        content: String,
        encoding: String? = null,
    ): Result<Unit> = repository.writeFile(uri, content, encoding)
}
