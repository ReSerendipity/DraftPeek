/**
 * 编辑器文件操作仓库接口。
 *
 * 遵循 Clean Architecture 和离线优先原则：
 * - 接口位于数据层（core/data），定义文件读写操作契约
 * - 读操作返回结构化结果类型，显式处理成功/错误路径
 * - 写操作返回 Result<Unit> 统一错误处理
 * - 实现类可替换而不影响领域逻辑
 *
 * 支持多种文件来源：内部存储、SAF（Storage Access Framework）、assets 资源。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import android.net.Uri
import com.draftpeek.core.common.util.DocumentType
import java.io.InputStream

/**
 * 编辑器文件数据仓库接口。
 *
 * 定义文件读写、删除、输入流打开等操作的契约。
 */
interface EditorFileRepository {

    /**
     * 读取指定 URI 文件的文本内容。
     *
     * 离线优先：返回结构化的 [EditorFileReadOutcome]，调用方必须显式处理
     * 成功和错误路径，无需使用 try/catch。
     *
     * @param uri 要读取的文件 URI
     * @param encoding 可选的文件编码（如 "UTF-8", "GBK"），为 null 时自动检测
     * @return [EditorFileReadOutcome] 包含文件内容或错误信息
     */
    suspend fun readFile(uri: Uri, encoding: String? = null): EditorFileReadOutcome

    /**
     * 将内容写入指定 URI 文件，替换原有内容。
     *
     * @param uri 要写入的文件 URI
     * @param content 要写入的文本内容
     * @param encoding 可选的文件编码，默认为 UTF-8
     * @return Result 表示成功或失败
     */
    suspend fun writeFile(uri: Uri, content: String, encoding: String? = null): Result<Unit>

    /**
     * 检查指定 URI 字符串是否引用应用内部文件。
     *
     * @param uriString 要检查的 URI 字符串
     * @return true 表示是内部文件，false 表示外部文件
     */
    fun isInternalFile(uriString: String): Boolean

    /**
     * 删除 URI 字符串引用的内部文件。
     *
     * @param uriString 内部文件的 URI 字符串
     * @return true 表示文件已删除，false 表示不是内部文件或删除失败
     */
    suspend fun deleteInternalFile(uriString: String): Boolean

    /**
     * 打开文件输入流用于流式读取操作（如全文搜索）。
     *
     * @param uri 要打开的文件 URI
     * @return 成功时返回 [InputStream]，无法打开时返回 null；调用方负责关闭流
     */
    suspend fun openInputStreamForSearch(uri: Uri): InputStream?
}

/**
 * 文件读取结果密封类，遵循离线优先模式。
 *
 * 强制调用方显式处理成功和错误两种情况，消除调用处 try/catch 的需要。
 */
sealed class EditorFileReadOutcome {
    /**
     * 文件读取成功。
     * @property result 包含文件内容和元数据的结果对象
     */
    data class Success(val result: EditorFileReadResult) : EditorFileReadOutcome()

    /**
     * 文件读取失败。
     * @property message 错误消息
     * @property cause 原始异常（可为 null）
     * @property isFileNotFound 是否为文件不存在错误
     * @property isSecurityException 是否为权限/安全错误
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isFileNotFound: Boolean = false,
        val isSecurityException: Boolean = false,
    ) : EditorFileReadOutcome()
}

/**
 * 编辑器文件读取结果数据类。
 *
 * 包含编辑器层正确显示和处理文件内容所需的所有元数据。
 *
 * @property content 文件文本内容（二进制/特殊文档类型为空字符串）
 * @property language 编程语言标识（如 "kotlin"）
 * @property fileName 文件名
 * @property fileSize 文件大小（字节）
 * @property isReadOnly 是否为只读模式（大文件或二进制文件）
 * @property detectedEncoding 检测到的文件编码
 * @property documentType 特殊文档类型（PDF/Office/媒体等）
 * @property renderedHtml Office 文档渲染后的 HTML 内容
 * @property fileSizeWarning 文件大小警告提示（大文件时显示）
 * @property isBinaryFile 是否为二进制文件
 * @property isTruncated 大文件流式读取时是否被截断
 */
data class EditorFileReadResult(
    val content: String,
    val language: String?,
    val fileName: String,
    val fileSize: Long = 0,
    val isReadOnly: Boolean = false,
    val detectedEncoding: String = "UTF-8",
    val documentType: DocumentType? = null,
    val renderedHtml: String? = null,
    val fileSizeWarning: String? = null,
    val isBinaryFile: Boolean = false,
    val isTruncated: Boolean = false,
)
