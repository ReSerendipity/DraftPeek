/**
 * 文件：EditorRepositoryImpl.kt
 * 功能：编辑器文件读写仓库实现类
 * 主要类/接口：EditorRepositoryImpl
 * 模块依赖：
 *   - android.content：Context、ContentResolver
 *   - core/common/util：AppFileManager、DocumentTypeHelper、EncodingDetector、OfficeDocumentParser
 *   - feature/editor/model：LanguageMapper
 *   - kotlinx.coroutines：协程 Flow、Dispatchers
 *   - javax.inject：Hilt 依赖注入
 */
package com.draftpeek.feature.editor.repository

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.common.util.DocumentTypeHelper
import com.draftpeek.core.common.util.EncodingDetector
import com.draftpeek.core.common.util.OfficeDocumentParser
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.LanguageMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 编辑器文件读写仓库实现类。
 *
 * 负责处理三种 URI 来源的文件读写：
 * 1. Asset URI（file:///android_asset/）：应用内置资源文件，只读
 * 2. Internal File URI：应用内部存储文件，可读写
 * 3. SAF Content URI：Storage Access Framework 选择的外部文件，可读写
 *
 * 优化亮点：
 * - C2：已知文件大小时预分配 ByteArrayOutputStream 减少内存拷贝
 * - C8：统一使用 use{} 块进行资源管理
 * - D7：验证内部文件路径防止目录遍历攻击
 * - E3：处理空文件/损坏文件的边界情况
 * - E7：所有流使用 use{} 保证资源清理
 * - A4：所有魔数提取为命名常量
 * - A6：readStreamChunked 提取为单一职责辅助方法
 * - C-01：流式读取支持超大文件，避免 OOM；编码检测从 3 次扫描优化为 1 次
 *
 * @property context 应用上下文，使用 @ApplicationContext 避免内存泄漏
 */
class EditorRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : EditorRepository {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    companion object {
        /** 文件大小警告阈值（10 MB），超过此大小显示性能警告 */
        private const val SIZE_WARN_THRESHOLD = 10L * 1024 * 1024
        /** 文件只读阈值（50 MB），超过此大小强制只读模式 */
        private const val SIZE_READONLY_THRESHOLD = 50L * 1024 * 1024
        /** 语法高亮禁用阈值（1 MB），超过此大小禁用语法高亮提升性能 */
        private const val HIGHLIGHT_DISABLE_THRESHOLD = 1024L * 1024
        /** 二进制文件检测扫描字节数（8 KB），扫描前 N 字节查找空字节判断是否为二进制 */
        private const val BINARY_DETECT_SCAN_BYTES = 8192
        /** Office 文档大小限制（30 MB），防止 ZIP 炸弹/OOM */
        private const val OFFICE_SIZE_LIMIT = 30L * 1024 * 1024
        /** 分块读取缓冲区大小（8 KB） */
        private const val READ_CHUNK_SIZE = 8 * 1024
        /** 全内存加载最大文件大小（2 MB），超过此大小使用流式预览 */
        private const val STREAMING_THRESHOLD = 2L * 1024 * 1024
        /** 流式预览读取行数（前 10000 行），超大文件只读取前 N 行预览 */
        private const val STREAMING_PREVIEW_LINES = 10_000
        /** 分块进度加载阈值（5 MB），超过此大小启用进度跟踪加载 */
        private const val CHUNKED_LOADING_THRESHOLD = 5L * 1024 * 1024
        /** 进度发射间隔（1 MB），避免 Flow 事件泛滥 */
        private const val PROGRESS_EMIT_INTERVAL = 1024L * 1024

        /** 媒体文档类型集合，避免每次调用创建 List 分配 */
        private val MEDIA_DOC_TYPES = setOf(DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO)
        /** Office 文档类型集合 */
        private val OFFICE_DOC_TYPES = setOf(DocumentType.WORD, DocumentType.EXCEL, DocumentType.POWERPOINT)
        /** 非文本文档类型集合 */
        private val NON_TEXT_DOC_TYPES = setOf(DocumentType.PDF, DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO)

        /**
         * 格式化文件大小为人类可读字符串。
         *
         * 防御性处理负值情况。
         *
         * @param bytes 字节数
         * @return 格式化后的字符串（B/KB/MB/GB）
         */
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 0) return "Unknown"
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
            if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            return "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }

        /**
         * 启发式二进制文件检测：扫描前 [BINARY_DETECT_SCAN_BYTES] 字节查找空字节（0x00）。
         *
         * 算法：O(n) 扫描，遇到第一个空字节提前退出。
         *
         * @param bytes 文件字节数据
         * @return 如果检测到空字节返回 true（可能是二进制文件），否则返回 false
         */
        private fun isLikelyBinary(bytes: ByteArray): Boolean {
            val scanLen = bytes.size.coerceAtMost(BINARY_DETECT_SCAN_BYTES)
            for (i in 0 until scanLen) {
                if (bytes[i] == 0.toByte()) return true
            }
            return false
        }

        /**
         * 使用分块读取将 InputStream 读入 ByteArray。
         *
         * 已知预期大小时预分配缓冲区减少内存拷贝。
         * 使用 BufferedInputStream 和 use{} 保证资源清理。
         *
         * @param stream 输入流
         * @param expectedSize 预期文件大小，未知时为 -1
         * @return 读取的字节数组
         */
        private fun readStreamChunked(stream: InputStream, expectedSize: Long = -1L): ByteArray {
            return BufferedInputStream(stream).use { bis ->
                val buffer = if (expectedSize > 0 && expectedSize <= Int.MAX_VALUE.toLong()) {
                    ByteArrayOutputStream(expectedSize.toInt())
                } else {
                    ByteArrayOutputStream()
                }
                val chunk = ByteArray(READ_CHUNK_SIZE)
                while (true) {
                    val read = bis.read(chunk)
                    if (read == -1) break
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }
        }

        /**
         * 带进度发射的分块读取 InputStream。
         *
         * 每读取 [PROGRESS_EMIT_INTERVAL] 字节发射一次 [FileReadProgress.Loading]，
         * 允许 UI 显示实时加载进度条。
         *
         * @param stream 输入流
         * @param expectedSize 预期文件总大小（字节），未知时为 -1
         * @param emitProgress 进度更新回调
         * @return 读取完成的完整字节数组
         */
        private suspend fun readStreamChunkedWithProgress(
            stream: InputStream,
            expectedSize: Long,
            emitProgress: suspend (FileReadProgress.Loading) -> Unit,
        ): ByteArray {
            return BufferedInputStream(stream).use { bis ->
                val buffer = if (expectedSize > 0 && expectedSize <= Int.MAX_VALUE.toLong()) {
                    ByteArrayOutputStream(expectedSize.toInt())
                } else {
                    ByteArrayOutputStream()
                }
                val chunk = ByteArray(READ_CHUNK_SIZE)
                var totalRead = 0L
                var lastEmitAt = 0L

                while (true) {
                    val read = bis.read(chunk)
                    if (read == -1) break
                    buffer.write(chunk, 0, read)
                    totalRead += read

                    // 按间隔发射进度，避免 Flow 泛滥
                    if (totalRead - lastEmitAt >= PROGRESS_EMIT_INTERVAL) {
                        emitProgress(FileReadProgress.Loading(totalRead, expectedSize))
                        lastEmitAt = totalRead
                    }
                }

                // 最后一次进度发射报告完成
                if (totalRead > 0 && totalRead != lastEmitAt) {
                    emitProgress(FileReadProgress.Loading(totalRead, expectedSize))
                }

                buffer.toByteArray()
            }
        }

        /**
         * 超大文件（>100MB）的流式预览读取。
         *
         * 仅读取前 [STREAMING_PREVIEW_LINES] 行以避免 OOM，
         * 并追加截断提示，允许用户在不崩溃的情况下查看超大文件开头。
         *
         * @param stream 输入流
         * @param expectedSize 预期文件大小（用于提示文本）
         * @return 前 N 行文本 + 截断提示
         */
        private fun readStreamPreview(stream: InputStream, expectedSize: Long): String {
            return BufferedInputStream(stream).use { bis ->
                val sb = StringBuilder()
                var lineCount = 0
                val bufferedReader = bis.bufferedReader(Charsets.UTF_8)
                bufferedReader.use { reader ->
                    reader.lineSequence().take(STREAMING_PREVIEW_LINES).forEach { line ->
                        if (lineCount > 0) sb.append('\n')
                        sb.append(line)
                        lineCount++
                    }
                }
                if (lineCount >= STREAMING_PREVIEW_LINES) {
                    sb.append("\n\n--- \n")
                    sb.append("[File truncated: showing first $STREAMING_PREVIEW_LINES lines of ${formatFileSize(expectedSize)}]")
                }
                sb.toString()
            }
        }
    }

    /**
     * 构建"文件过大只读"结果，消除 SAF 预检查和内部文件检查中的重复代码。
     */
    private fun createTooLargeResult(fileName: String, fileSize: Long): FileReadResult {
        return FileReadResult(
            content = "",
            language = null,
            fileName = fileName,
            fileSize = fileSize,
            isReadOnly = true,
            detectedEncoding = "binary",
            documentType = null,
            fileSizeWarning = context.getString(R.string.editor_file_too_large_readonly, formatFileSize(fileSize)),
            isBinaryFile = true,
        )
    }

    /**
     * 字节读取后的通用处理逻辑：文件大小检查、文档类型检测、编码检测、语言映射等。
     *
     * 从 [readFile] 和 [readFileWithProgress] 中提取的公共后处理流程，
     * 消除约 120 行重复代码。
     *
     * @param bytes 文件字节数据
     * @param fileName 文件名
     * @param isReadOnly 是否只读（asset 文件为 true）
     * @param encoding 用户指定编码，null 时自动检测
     * @param onProgress 进度回调，no-op 用于 [readFile]，channel send 用于 [readFileWithProgress]
     * @return 完整的文件读取结果
     */
    private suspend fun buildFileReadResult(
        bytes: ByteArray,
        fileName: String,
        isReadOnly: Boolean,
        encoding: String?,
        onProgress: suspend (FileReadProgress) -> Unit = {},
    ): FileReadResult {
        val fileSize = bytes.size.toLong()
        val highlightDisabled = fileSize >= HIGHLIGHT_DISABLE_THRESHOLD
        val fileSizeWarning = buildString {
            when {
                fileSize >= SIZE_READONLY_THRESHOLD ->
                    append(context.getString(R.string.editor_file_too_large_readonly, formatFileSize(fileSize)))
                fileSize >= SIZE_WARN_THRESHOLD ->
                    append(context.getString(R.string.editor_file_large_slow, formatFileSize(fileSize)))
            }
            if (highlightDisabled) {
                if (isNotEmpty()) append("\n")
                append(context.getString(R.string.editor_highlight_disabled_large_file))
            }
        }.ifEmpty { null }
        val forcedReadOnly = fileSize >= SIZE_READONLY_THRESHOLD || isReadOnly

        var documentType = DocumentTypeHelper.getDocumentType(fileName)

        // PDF 检测防御性回退
        if (documentType == null && fileName.lowercase().endsWith(".pdf")) {
            documentType = DocumentType.PDF
        }

        if (documentType == DocumentType.PDF) {
            return FileReadResult(
                content = "",
                language = null,
                fileName = fileName,
                fileSize = fileSize,
                isReadOnly = true,
                detectedEncoding = "binary",
                documentType = DocumentType.PDF,
                fileSizeWarning = fileSizeWarning,
            )
        }

        if (documentType in MEDIA_DOC_TYPES) {
            return FileReadResult(
                content = "",
                language = null,
                fileName = fileName,
                fileSize = fileSize,
                isReadOnly = true,
                detectedEncoding = "binary",
                documentType = documentType,
                fileSizeWarning = fileSizeWarning,
            )
        }

        // Office 文档大小限制防止 ZIP 炸弹/OOM
        if (documentType in OFFICE_DOC_TYPES && fileSize > OFFICE_SIZE_LIMIT) {
            return FileReadResult(
                content = "",
                language = null,
                fileName = fileName,
                fileSize = fileSize,
                isReadOnly = true,
                detectedEncoding = "binary",
                documentType = documentType,
                fileSizeWarning = context.getString(R.string.editor_file_too_large_readonly, formatFileSize(fileSize)),
            )
        }

        if (documentType != null && documentType !in NON_TEXT_DOC_TYPES) {
            val html = when (documentType) {
                DocumentType.WORD -> OfficeDocumentParser.parseWord(bytes.inputStream())
                DocumentType.EXCEL -> OfficeDocumentParser.parseExcel(bytes.inputStream())
                DocumentType.POWERPOINT -> OfficeDocumentParser.parsePowerPoint(bytes.inputStream())
                else -> ""
            }
            return FileReadResult(
                content = "",
                language = null,
                fileName = fileName,
                fileSize = fileSize,
                isReadOnly = true,
                detectedEncoding = "binary",
                documentType = documentType,
                renderedHtml = html,
                fileSizeWarning = fileSizeWarning,
            )
        }

        // 二进制检测：扩展名 + 空字节扫描
        if (DocumentTypeHelper.isNonTextExtension(fileName) || isLikelyBinary(bytes)) {
            return FileReadResult(
                content = "",
                language = null,
                fileName = fileName,
                fileSize = fileSize,
                isReadOnly = true,
                detectedEncoding = "binary",
                documentType = null,
                fileSizeWarning = fileSizeWarning,
                isBinaryFile = true,
            )
        }

        // 编码检测优化：从原来的 3 次扫描优化为 1 次扫描
        val (content, detectedEncodingName) = if (encoding != null) {
            // 用户指定编码时直接解码，跳过检测
            String(bytes, Charset.forName(encoding)) to encoding
        } else {
            val detected = EncodingDetector.detectEncoding(bytes, fileName)
            EncodingDetector.decodeWithEncoding(bytes, detected) to detected
        }
        val extension = fileName.substringAfterLast('.', "")
        val language = LanguageMapper.fromExtension(extension)

        return FileReadResult(
            content = content,
            language = if (highlightDisabled) null else language,
            fileName = fileName,
            fileSize = fileSize,
            isReadOnly = forcedReadOnly,
            detectedEncoding = detectedEncodingName,
            fileSizeWarning = fileSizeWarning,
        )
    }

    /**
     * 读取指定 URI 的文件文本内容。
     *
     * 文件读取流程：
     * 1. 判断 URI 类型（Asset/Internal/SAF）
     * 2. 预检查文件大小，超 50MB 直接返回只读二进制结果
     * 3. 根据 URI 类型选择对应的输入流打开方式
     * 4. 超 100MB 文件使用流式预览读取前 50000 行
     * 5. 委托 [buildFileReadResult] 完成后续文档类型检测、编码检测、语言映射
     *
     * @param uri 文件 URI
     * @param encoding 可选的编码覆盖，null 时自动检测
     * @return 文件读取结果，包含内容、语言、文件名等信息
     * @throws SecurityException 权限过期时抛出
     * @throws FileNotFoundException 文件不存在时抛出
     */
    override suspend fun readFile(uri: Uri, encoding: String?): FileReadResult =
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            val isAssetUri = uriString.startsWith("file:///android_asset/")
            val isInternalFile = AppFileManager.isInternalUri(uriString) && !isAssetUri

            // SAF URI 预检查文件大小
            if (!isAssetUri && !isInternalFile) {
                try {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    val fileSize = docFile?.length() ?: -1L
                    if (fileSize > SIZE_READONLY_THRESHOLD) {
                        val fileName = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                        return@withContext createTooLargeResult(fileName, fileSize)
                    }
                } catch (_: Exception) {
                    // DocumentFile.length() 可能抛异常，继续正常读取
                }
            }

            val (bytes, fileName, isReadOnly) = when {
                isAssetUri -> {
                    val assetPath = uriString.removePrefix("file:///android_asset/")
                    // 验证 asset 路径防止遍历攻击
                    if (assetPath.contains("..")) {
                        throw SecurityException("Invalid asset path")
                    }
                    val name = assetPath.substringAfterLast('/')
                    val content = context.assets.open(assetPath).use { readStreamChunked(it) }
                    Triple(content, name, true)
                }
                isInternalFile -> {
                    val file = AppFileManager.getInternalFileFromUri(context, uriString)
                        ?: throw FileNotFoundException("Cannot find internal file: $uriString")
                    if (file.length() > SIZE_READONLY_THRESHOLD) {
                        return@withContext createTooLargeResult(file.name, file.length())
                    }
                    // 超大文件使用流式读取降低内存压力
                    val content = if (file.length() > STREAMING_THRESHOLD) {
                        file.inputStream().use { readStreamPreview(it, file.length()).toByteArray(Charsets.UTF_8) }
                    } else {
                        file.inputStream().use { readStreamChunked(it, file.length()) }
                    }
                    Triple(content, file.name, false)
                }
                else -> {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    val name = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                    val expectedSize = docFile?.length() ?: -1L
                    val content = try {
                        contentResolver.openInputStream(uri)
                    } catch (e: SecurityException) {
                        throw SecurityException(
                            context.getString(R.string.editor_error_permission_expired)
                        )
                    } catch (e: FileNotFoundException) {
                        throw FileNotFoundException(
                            context.getString(R.string.editor_error_external_file_not_found)
                        )
                    }?.use {
                        // 超大文件使用流式预览
                        if (expectedSize > STREAMING_THRESHOLD) {
                            readStreamPreview(it, expectedSize).toByteArray(Charsets.UTF_8)
                        } else {
                            readStreamChunked(it, expectedSize)
                        }
                    } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
                    Triple(content, name, false)
                }
            }

            buildFileReadResult(bytes, fileName, isReadOnly, encoding)
        }

    /**
     * 将文本内容写入指定 URI 的文件。
     *
     * @param uri 目标文件 URI
     * @param content 要写入的文本内容
     * @param encoding 文本编码，默认 UTF-8
     * @return 写入成功返回 Result.success(Unit)，失败返回 Result.failure(exception)
     */
    override suspend fun writeFile(uri: Uri, content: String, encoding: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uriString = uri.toString()
                val charset = Charset.forName(encoding ?: "UTF-8")
                if (AppFileManager.isInternalUri(uriString) && !uriString.startsWith("file:///android_asset/")) {
                    val file = AppFileManager.getInternalFileFromUri(context, uriString)
                        ?: throw IllegalStateException("Cannot find internal file: $uriString")
                    // getInternalFileFromUri 已验证路径遍历
                    AppFileManager.writeUserFile(file, content)
                } else {
                    // 使用 OutputStreamWriter 流式写入，避免字节数组翻倍
                    contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        OutputStreamWriter(outputStream, charset).use { writer ->
                            writer.write(content)
                        }
                    } ?: throw IllegalStateException("Cannot open output stream for URI: $uri")
                }
            }
        }

    /**
     * 检查给定 URI 字符串是否指向应用内部文件。
     *
     * @param uriString URI 字符串
     * @return 如果是内部可读写文件返回 true，否则返回 false
     */
    override fun isInternalFile(uriString: String): Boolean {
        return AppFileManager.isInternalUri(uriString) &&
            !uriString.startsWith("file:///android_asset/")
    }

    /**
     * 删除 URI 字符串引用的内部文件。
     *
     * @param uriString 内部文件 URI 字符串
     * @return 删除成功返回 true，如果不是内部文件或删除失败返回 false
     */
    override suspend fun deleteInternalFile(uriString: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!isInternalFile(uriString)) return@withContext false
            val file = AppFileManager.getInternalFileFromUri(context, uriString)
                ?: return@withContext false
            AppFileManager.deleteUserFile(file)
            true
        }

    /**
     * 流式打开文件输入流用于全文搜索，避免将整个文件加载到内存。
     *
     * 三类 URI 处理与 readFile() 对齐：
     * - Asset URI → AssetManager.open()
     * - Internal File URI → File.inputStream()
     * - SAF Content URI → ContentResolver.openInputStream()
     *
     * 返回 null 的情形：URI 失效、文件不存在、权限过期、asset 路径越界。
     * 调用方负责在使用后关闭流（推荐 use{}）。
     *
     * @param uri 文件 URI
     * @return 输入流，打开失败时返回 null
     */
    @SuppressLint("Recycle") // 返回给调用方的流由调用方负责关闭（EditorViewModel 使用 use{}）
    override suspend fun openInputStreamForSearch(uri: Uri): InputStream? =
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            try {
                when {
                    uriString.startsWith("file:///android_asset/") -> {
                        val assetPath = uriString.removePrefix("file:///android_asset/")
                        // 防 asset 路径遍历
                        if (assetPath.contains("..")) return@withContext null
                        context.assets.open(assetPath)
                    }
                    isInternalFile(uriString) -> {
                        val file = AppFileManager.getInternalFileFromUri(context, uriString)
                            ?: return@withContext null
                        file.inputStream()
                    }
                    else -> {
                        contentResolver.openInputStream(uri)
                    }
                }
            } catch (e: Exception) {
                // 文件不存在/权限过期/IO 错误统一返回 null，调用方优雅跳过
                null
            }
        }

    /**
     * 带实时进度跟踪的文件读取（通过 Flow）。
     *
     * Flow 在读取过程中（约每 1 MB）发射 [FileReadProgress.Loading] 事件，
     * 最后发射 [FileReadProgress.Done] 携带完整结果。
     *
     * 对于小于 [CHUNKED_LOADING_THRESHOLD]（5 MB）的文件，读取速度很快，
     * 进度事件可能观察不到——但 API 保持一致。
     *
     * 该方法复用与 [readFile] 相同的后处理逻辑（通过 [buildFileReadResult]），
     * 仅在字节读取步骤包装了进度发射。
     * 所有基于大小的保护措施（只读、流式预览、二进制检测、编码检测）均适用。
     *
     * @param uri 文件 URI
     * @param encoding 可选编码覆盖
     * @return 进度事件 Flow
     */
    override fun readFileWithProgress(uri: Uri, encoding: String?): Flow<FileReadProgress> =
        channelFlow {
            withContext(Dispatchers.IO) {
                val uriString = uri.toString()
                val isAssetUri = uriString.startsWith("file:///android_asset/")
                val isInternalFile = AppFileManager.isInternalUri(uriString) && !isAssetUri

                // SAF URI 预检查文件大小（与 readFile 相同）
                if (!isAssetUri && !isInternalFile) {
                    try {
                        val docFile = DocumentFile.fromSingleUri(context, uri)
                        val fileSize = docFile?.length() ?: -1L
                        if (fileSize > SIZE_READONLY_THRESHOLD) {
                            val fileName = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                            send(FileReadProgress.Done(createTooLargeResult(fileName, fileSize)))
                            return@withContext
                        }
                    } catch (_: Exception) {
                        // DocumentFile.length() 可能抛异常，继续正常读取
                    }
                }

                // 带进度跟踪读取字节
                val (bytes, fileName, isReadOnly) = when {
                    isAssetUri -> {
                        val assetPath = uriString.removePrefix("file:///android_asset/")
                        if (assetPath.contains("..")) throw SecurityException("Invalid asset path")
                        val name = assetPath.substringAfterLast('/')
                        val content = context.assets.open(assetPath).use {
                            readStreamChunkedWithProgress(it, -1L) { progress ->
                                send(progress)
                            }
                        }
                        Triple(content, name, true)
                    }
                    isInternalFile -> {
                        val file = AppFileManager.getInternalFileFromUri(context, uriString)
                            ?: throw FileNotFoundException("Cannot find internal file: $uriString")
                        if (file.length() > SIZE_READONLY_THRESHOLD) {
                            send(FileReadProgress.Done(createTooLargeResult(file.name, file.length())))
                            return@withContext
                        }
                        val content = if (file.length() > STREAMING_THRESHOLD) {
                            file.inputStream().use { readStreamPreview(it, file.length()).toByteArray(Charsets.UTF_8) }
                        } else {
                            file.inputStream().use {
                                readStreamChunkedWithProgress(it, file.length()) { progress ->
                                    send(progress)
                                }
                            }
                        }
                        Triple(content, file.name, false)
                    }
                    else -> {
                        val docFile = DocumentFile.fromSingleUri(context, uri)
                        val name = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                        val expectedSize = docFile?.length() ?: -1L
                        val content = try {
                            contentResolver.openInputStream(uri)
                        } catch (e: SecurityException) {
                            throw SecurityException(context.getString(R.string.editor_error_permission_expired))
                        } catch (e: FileNotFoundException) {
                            throw FileNotFoundException(context.getString(R.string.editor_error_external_file_not_found))
                        }?.use {
                            if (expectedSize > STREAMING_THRESHOLD) {
                                readStreamPreview(it, expectedSize).toByteArray(Charsets.UTF_8)
                            } else {
                                readStreamChunkedWithProgress(it, expectedSize) { progress ->
                                    send(progress)
                                }
                            }
                        } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
                        Triple(content, name, false)
                    }
                }

                val result = buildFileReadResult(bytes, fileName, isReadOnly, encoding) { progress ->
                    send(progress)
                }

                val fileSize = bytes.size.toLong()
                val isTruncated = fileSize > STREAMING_THRESHOLD

                send(FileReadProgress.Done(
                    result.copy(
                        isTruncated = isTruncated,
                        loadMoreUri = if (isTruncated) uri else null,
                    ),
                ))
            }
        }

    /**
     * 离线优先文件读取（Ch5#3）。
     *
     * 离线优先模式流程：
     * 1. 如果有缓存内容，立即发射 [EditorFileReadOutcome.Cached]
     * 2. 然后从源获取最新内容
     * 3. 如果源不可用但有缓存数据，保留缓存版本——UI 可显示"过期"指示器
     * 4. 如果既无缓存也无源可用，发射 [EditorFileReadOutcome.Unavailable]
     *
     * @param uri 要读取的文件 URI
     * @param encoding 可选编码覆盖
     * @param cachedContent 可选的缓存内容，立即提供服务
     * @param cachedTimestamp 缓存内容保存时的纪元毫秒数
     * @return 离线优先结果 Flow
     */
    override fun readFileOfflineFirst(
        uri: Uri,
        encoding: String?,
        cachedContent: String?,
        cachedTimestamp: Long,
    ): Flow<EditorFileReadOutcome> = flow {
        // 步骤1：如果有缓存，立即发射缓存内容
        if (cachedContent != null) {
            val ageMs = if (cachedTimestamp > 0L) {
                System.currentTimeMillis() - cachedTimestamp
            } else {
                -1L
            }
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
            val extension = fileName.substringAfterLast('.', "")
            val language = LanguageMapper.fromExtension(extension)
            emit(
                EditorFileReadOutcome.Cached(
                    result = FileReadResult(
                        content = cachedContent,
                        language = language,
                        fileName = fileName,
                        fileSize = cachedContent.length.toLong(),
                        detectedEncoding = encoding ?: "UTF-8",
                    ),
                    ageMs = ageMs.coerceAtLeast(-1L),
                ),
            )
        }

        // 步骤2：从源获取最新内容
        try {
            val freshResult = readFile(uri, encoding)
            emit(EditorFileReadOutcome.Fresh(freshResult))
        } catch (e: Exception) {
            if (cachedContent == null) {
                emit(
                    EditorFileReadOutcome.Unavailable(
                        error = e,
                        message = e.message ?: "Failed to read file",
                    ),
                )
            }
        }
    }
}
