package com.draftpeek.feature.browser.vfs

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.draftpeek.core.common.util.EncodingDetector
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.feature.browser.model.FileContent
import com.draftpeek.feature.browser.model.FileItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 本地文件系统提供者，使用 Android 的存储访问框架（SAF）
 *
 * 这是所有本地和基于 SAF 的文件访问的默认提供者。
 * 通过 DocumentFile API 处理 content://、file:// 和 tree URI。
 *
 * ## 异常处理
 * - [java.io.FileNotFoundException]: 文件不存在
 * - [SecurityException]: 没有文件访问权限
 * - [IllegalArgumentException]: URI 无法解析
 * - 其他异常统一包装为 [FileSystemResult.Error]
 */
class LocalFileSystemProvider @Inject constructor(@param:ApplicationContext private val context: Context) :
    FileSystemProvider {

    override val scheme: String = "file"
    override val displayName: String = "本地存储"

    private val contentResolver
        get() = context.contentResolver

    override fun supportsUri(uri: String): Boolean = uri.startsWith("content://") ||
        uri.startsWith("file://") ||
        uri.startsWith("/")

    override fun listFiles(uri: String): Flow<List<FileItem>> = flow {
        val documentFile = resolveDocumentFile(uri)
            ?: throw IllegalArgumentException("无法解析 URI: $uri")

        if (!documentFile.isDirectory) {
            emit(emptyList())
            return@flow
        }

        val files = documentFile.listFiles()
            .filter { it.exists() }
            .map { doc ->
                FileItem(
                    uri = doc.uri,
                    name = doc.name ?: "未知",
                    isDirectory = doc.isDirectory,
                    size = if (doc.isFile) doc.length() else 0L,
                    lastModified = if (doc.isFile) doc.lastModified() else 0L,
                    mimeType = doc.type ?: "",
                    extension = doc.name?.substringAfterLast('.', "") ?: "",
                    isReadOnly = !doc.canWrite()
                )
            }
            .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })

        emit(files)
    }.flowOn(Dispatchers.IO)

    override suspend fun readFile(uri: String): FileSystemResult<FileContent> = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            val fileName = DocumentFile.fromSingleUri(context, parsedUri)?.name ?: ""

            val bytes = contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                BufferedInputStream(inputStream).use { bis ->
                    bis.readBytes()
                }
            } ?: return@withContext FileSystemResult.Error(
                message = "无法打开输入流",
                errorCode = FileSystemResult.ErrorCode.NOT_FOUND
            )

            val encoding = EncodingDetector.detectEncoding(bytes, fileName)
            val content = EncodingDetector.decodeWithEncoding(bytes, encoding)
            val extension = fileName.substringAfterLast('.', "")
            val language = LanguageConfig.extensionToLanguage(extension) ?: ""

            FileSystemResult.Success(
                FileContent(content = content, encoding = encoding, language = language)
            )
        } catch (e: java.io.FileNotFoundException) {
            FileSystemResult.Error(
                message = e.message ?: "文件未找到",
                cause = e,
                errorCode = FileSystemResult.ErrorCode.NOT_FOUND
            )
        } catch (e: SecurityException) {
            FileSystemResult.Error(
                message = e.message ?: "权限被拒绝",
                cause = e,
                errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = e.message ?: "读取文件失败",
                cause = e
            )
        }
    }

    override suspend fun writeFile(uri: String, content: String): FileSystemResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val parsedUri = Uri.parse(uri)
            contentResolver.openOutputStream(parsedUri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            } ?: return@withContext FileSystemResult.Error(
                message = "无法打开输出流",
                errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED
            )
            FileSystemResult.Success(Unit)
        } catch (e: SecurityException) {
            FileSystemResult.Error(
                message = e.message ?: "权限被拒绝",
                cause = e,
                errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = e.message ?: "写入文件失败",
                cause = e
            )
        }
    }

    override suspend fun createDirectory(parentUri: String, dirName: String): FileSystemResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val parentDoc = resolveDocumentFile(parentUri)
                    ?: return@withContext FileSystemResult.Error(
                        message = "父目录未找到",
                        errorCode = FileSystemResult.ErrorCode.NOT_FOUND
                    )
                val newDir = parentDoc.createDirectory(dirName)
                    ?: return@withContext FileSystemResult.Error(
                        message = "创建目录失败",
                        errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED
                    )
                FileSystemResult.Success(newDir.uri.toString())
            } catch (e: Exception) {
                FileSystemResult.Error(
                    message = e.message ?: "创建目录失败",
                    cause = e
                )
            }
        }

    override suspend fun delete(uri: String): FileSystemResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val doc = resolveDocumentFile(uri)
                ?: return@withContext FileSystemResult.Error(
                    message = "文件未找到",
                    errorCode = FileSystemResult.ErrorCode.NOT_FOUND
                )
            if (doc.delete()) {
                FileSystemResult.Success(Unit)
            } else {
                FileSystemResult.Error(
                    message = "删除失败",
                    errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED
                )
            }
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = e.message ?: "删除失败",
                cause = e
            )
        }
    }

    override suspend fun exists(uri: String): Boolean = withContext(Dispatchers.IO) {
        resolveDocumentFile(uri)?.exists() ?: false
    }

    override suspend fun getFileInfo(uri: String): FileSystemResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val doc = resolveDocumentFile(uri)
                ?: return@withContext FileSystemResult.Error(
                    message = "文件未找到",
                    errorCode = FileSystemResult.ErrorCode.NOT_FOUND
                )
            FileSystemResult.Success(
                FileItem(
                    uri = doc.uri,
                    name = doc.name ?: "未知",
                    isDirectory = doc.isDirectory,
                    size = if (doc.isFile) doc.length() else 0L,
                    lastModified = if (doc.isFile) doc.lastModified() else 0L,
                    mimeType = doc.type ?: "",
                    extension = doc.name?.substringAfterLast('.', "") ?: "",
                    isReadOnly = !doc.canWrite()
                )
            )
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = e.message ?: "获取文件信息失败",
                cause = e
            )
        }
    }

    override suspend fun testConnection(): Boolean = true

    override fun close() {
        // 本地文件系统无需释放资源
    }

    /**
     * 将 URI 解析为 DocumentFile
     *
     * @param uri 要解析的 URI
     * @return 解析后的 DocumentFile，无法解析时返回 null
     */
    private fun resolveDocumentFile(uri: String): DocumentFile? {
        val parsedUri = Uri.parse(uri)
        return when {
            uri.contains("/tree/") -> DocumentFile.fromTreeUri(context, parsedUri)
            parsedUri.scheme == "file" -> {
                val path = parsedUri.path ?: return null
                DocumentFile.fromFile(java.io.File(path))
            }
            else -> DocumentFile.fromSingleUri(context, parsedUri)
        }
    }
}
