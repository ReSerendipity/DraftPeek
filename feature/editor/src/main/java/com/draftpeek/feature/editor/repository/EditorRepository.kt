/**
 * 文件：EditorRepository.kt
 * 功能：编辑器文件读写仓库接口定义
 * 主要类/接口：EditorRepository、FileReadResult、FileReadProgress
 * 模块依赖：
 *   - android.net：Uri
 *   - core/common/util：DocumentType
 *   - kotlinx.coroutines.flow：Flow
 */
package com.draftpeek.feature.editor.repository

import android.net.Uri
import com.draftpeek.core.common.util.DocumentType
import java.io.InputStream
import kotlinx.coroutines.flow.Flow

/**
 * 编辑器文件读写的数据层接口。
 *
 * 定义文件读取、写入、删除、流式打开等操作，
 * 由 [EditorRepositoryImpl] 提供具体实现。
 *
 * 职责：
 * - 文件内容的读取（支持普通读取、带进度读取、离线优先读取）
 * - 文件内容的写入
 * - 内部文件的识别和删除
 * - 全文搜索用的流式输入流打开
 */
interface EditorRepository {

    /**
     * 读取 [uri] 指向文件的文本内容。
     *
     * @param uri 文件 URI
     * @param encoding 可选的文件编码，null 时自动检测
     * @return 文件读取结果，包含内容、语言、文件名等信息
     */
    suspend fun readFile(uri: Uri, encoding: String? = null): FileReadResult

    /**
     * 带进度跟踪读取 [uri] 指向文件的文本内容（P1-14 功能）。
     *
     * 返回的 Flow 发射：
     * - 读取过程中的 [FileReadProgress.Loading]（带字节计数和总数）
     * - 读取成功完成时的 [FileReadProgress.Done]
     *
     * 调用方应收集 Flow 并相应更新 UI。
     * 如果调用方不需要进度，请改用 [readFile]。
     *
     * @param uri 文件 URI
     * @param encoding 可选的文件编码，null 时自动检测
     * @return 进度事件 Flow
     */
    fun readFileWithProgress(uri: Uri, encoding: String? = null): Flow<FileReadProgress>

    /**
     * 将 [content] 写入 [uri] 指向的文件，替换现有内容。
     *
     * @param uri 目标文件 URI
     * @param content 要写入的文本内容
     * @param encoding 文本编码，默认 UTF-8
     * @return 写入成功返回 Result.success(Unit)，失败返回 Result.failure
     */
    suspend fun writeFile(uri: Uri, content: String, encoding: String? = null): Result<Unit>

    /**
     * 检查给定 [uriString] 是否引用应用内部文件。
     *
     * @param uriString URI 字符串
     * @return 如果是内部可读写文件返回 true
     */
    fun isInternalFile(uriString: String): Boolean

    /**
     * 删除 [uriString] 引用的内部文件。
     *
     * @param uriString 内部文件 URI 字符串
     * @return 文件删除成功返回 true；如果不是内部文件返回 false
     */
    suspend fun deleteInternalFile(uriString: String): Boolean

    /**
     * 打开 [uri] 对应的输入流用于流式扫描（如全文搜索）。
     *
     * C-01：调用方可按行读取而无需将整个文件加载到内存。
     * 返回 null 表示无法打开（URI 失效、文件不存在等），调用方应优雅跳过。
     *
     * 调用方**必须**在读取完毕后调用 `InputStream.close()`（推荐使用 `use{}` 块）。
     *
     * @param uri 文件 URI
     * @return 输入流，打开失败时返回 null
     */
    suspend fun openInputStreamForSearch(uri: Uri): InputStream?

    /**
     * 离线优先文件读取（Ch5#3 功能）。
     *
     * 返回的 Flow 发射：
     * 1. 如果存在本地缓存（如最近文件快照或会话恢复），立即发射 [EditorFileReadOutcome.Cached]
     * 2. 源文件读取成功后发射 [EditorFileReadOutcome.Fresh]
     * 3. 如果缓存和源都无法访问，发射 [EditorFileReadOutcome.Unavailable]
     *
     * 遵循离线优先模式：始终先显示缓存数据，然后从源刷新。
     * UI 在加载新数据时可显示"过期"指示器。
     *
     * @param uri 要读取的文件 URI
     * @param encoding 可选编码覆盖
     * @param cachedContent 可选的缓存内容，立即提供服务
     * @param cachedTimestamp 缓存内容保存时的纪元毫秒数
     * @return 离线优先结果 Flow
     */
    fun readFileOfflineFirst(
        uri: Uri,
        encoding: String? = null,
        cachedContent: String? = null,
        cachedTimestamp: Long = 0L,
    ): Flow<EditorFileReadOutcome>
}

/**
 * 文件读取结果数据类。
 *
 * @property content 文件文本内容
 * @property language 编程语言标识符，null 表示纯文本或特殊文档类型
 * @property fileName 文件名（含扩展名）
 * @property fileSize 文件大小（字节）
 * @property isReadOnly 是否为只读模式
 * @property detectedEncoding 检测到的文件编码
 * @property documentType 文档类型枚举（PDF/Office/媒体等）
 * @property renderedHtml 预渲染的 HTML 内容（用于 Office 文档预览）
 * @property fileSizeWarning 文件大小警告信息
 * @property isBinaryFile 是否为二进制文件
 * @property isTruncated 内容是否被截断（超大文件流式预览）
 * @property loadMoreUri 加载更多内容的 URI（用于截断文件）
 */
data class FileReadResult(
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
    val loadMoreUri: Uri? = null,
)

/**
 * 文件读取进度事件密封类（P1-14 功能）。
 */
sealed class FileReadProgress {
    /**
     * 文件正在读取中。
     *
     * @property loadedBytes 已读取字节数
     * @property totalBytes 总字节数，总大小未知时为 -1
     */
    data class Loading(
        val loadedBytes: Long,
        val totalBytes: Long = -1,
    ) : FileReadProgress() {
        /** 进度分数 0f..1f，总大小未知时为 -1f */
        val progress: Float
            get() = if (totalBytes > 0) (loadedBytes.toFloat() / totalBytes) else -1f
    }

    /**
     * 文件读取成功完成。
     *
     * @property result 完整的文件读取结果
     */
    data class Done(val result: FileReadResult) : FileReadProgress()
}
