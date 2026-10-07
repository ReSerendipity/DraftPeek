package com.draftpeek.feature.browser.repository

import android.net.Uri
import com.draftpeek.feature.browser.model.FileContent
import com.draftpeek.feature.browser.model.FileItem
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun listFiles(treeUri: Uri): Flow<List<FileItem>>

    /**
     * Read the text content of the file at [uri].
     *
     * Offline-first: returns [FileContentResult] sealed class so callers
     * explicitly handle both Success and Error paths without try/catch.
     */
    suspend fun readFile(uri: Uri): FileContentResult

    suspend fun writeFile(uri: Uri, content: String): Result<Unit>

    /**
     * 重命名 SAF 文档（`DocumentsContract.renameDocument`）。
     *
     * 仅对声明了 `FLAG_SUPPORTS_RENAME` 的 provider 有效；不支持时会抛异常或返回 null，
     * 两种情况统一归入 [Result.failure] 由调用方提示。**内部存储文件不走这里**
     * （见 `AppFileManager.renameInternalFile`）。
     *
     * @param newName 新文件名（含扩展名）
     * @return 成功时返回 provider 回读到的显示名（可能被规范化，未必等于入参）
     */
    suspend fun renameFile(uri: Uri, newName: String): Result<String>
    suspend fun takeUriPermission(treeUri: Uri)
    suspend fun releaseUriPermission(treeUri: Uri)
}

/**
 * Sealed result of reading a file, following offline-first pattern.
 * Forces callers to handle both success and error explicitly.
 */
sealed class FileContentResult {
    data class Success(val content: FileContent) : FileContentResult()
    data class Error(val message: String, val cause: Throwable? = null, val isFileNotFound: Boolean = false) :
        FileContentResult()
}
