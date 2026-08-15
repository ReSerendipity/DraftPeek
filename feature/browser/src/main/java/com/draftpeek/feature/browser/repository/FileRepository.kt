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
    suspend fun takeUriPermission(treeUri: Uri)
    suspend fun releaseUriPermission(treeUri: Uri)
}

/**
 * Sealed result of reading a file, following offline-first pattern.
 * Forces callers to handle both success and error explicitly.
 */
sealed class FileContentResult {
    data class Success(val content: FileContent) : FileContentResult()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isFileNotFound: Boolean = false,
    ) : FileContentResult()
}
