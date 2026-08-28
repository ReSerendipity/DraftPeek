/**
 * 编辑器文件仓库实现类。
 *
 * 实现文件读写的核心逻辑，支持多种文件来源（内部存储、SAF URI、assets 资源），
 * 包含编码自动检测、二进制文件识别、Office 文档解析、大文件流式处理等功能。
 * 遵循离线优先和错误显式处理原则，所有 I/O 操作均在 Dispatchers.IO 上执行。
 *
 * ## 主要功能
 * - 多来源文件读取（内部文件 / SAF / assets）
 * - 文件编码自动检测（UTF-8/GBK 等）
 * - 二进制文件检测（null byte 扫描）
 * - Office 文档解析（Word/Excel/PPT → HTML）
 * - 大文件流式读取（>100MB 截断预览）
 * - 文件大小分级（警告阈值 / 只读阈值）
 * - 路径遍历攻击防护
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.common.util.DocumentTypeHelper
import com.draftpeek.core.common.util.EncodingDetector
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.common.util.OfficeDocumentParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [EditorFileRepository] for reading and writing files.
 *
 * This class handles:
 * - File reading from various sources (internal, SAF, assets)
 * - File writing with encoding support
 * - Binary file detection
 * - Large file handling with streaming
 * - Document type detection and parsing
 *
 * Optimization highlights:
 * - C2: Pre-allocates ByteArrayOutputStream when size is known
 * - C8: Uses use{} blocks consistently for resource management
 * - D7: Validates internal file paths against directory traversal
 * - E3: Handles edge cases for empty/corrupt files
 * - E7: All streams use use{} for guaranteed cleanup
 * - A4: All magic numbers extracted as named constants
 * - A6: readStreamChunked extracted as single-responsibility helper
 */
@Singleton
class EditorFileRepositoryImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    EditorFileRepository {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    companion object {
        // A4: All thresholds extracted as named constants
        private const val SIZE_WARN_THRESHOLD = 10L * 1024 * 1024 // 10 MB
        private const val SIZE_READONLY_THRESHOLD = 50L * 1024 * 1024 // 50 MB
        private const val HIGHLIGHT_DISABLE_THRESHOLD = 1024L * 1024 // 1 MB
        private const val BINARY_DETECT_SCAN_BYTES = 8192
        private const val OFFICE_SIZE_LIMIT = 30L * 1024 * 1024 // 30 MB
        private const val READ_CHUNK_SIZE = 8 * 1024 // 8 KB

        /** Maximum file size for full in-memory loading (100 MB). Beyond this, use streaming read. */
        private const val STREAMING_THRESHOLD = 100L * 1024 * 1024 // 100 MB

        /** Number of lines to read in streaming mode for preview (first N lines). */
        private const val STREAMING_PREVIEW_LINES = 50_000

        // A4/M3: Set constants for document type lookups (avoids per-call List allocation)
        private val MEDIA_DOC_TYPES = setOf(DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO)
        private val OFFICE_DOC_TYPES = setOf(DocumentType.WORD, DocumentType.EXCEL, DocumentType.POWERPOINT)
        private val NON_TEXT_DOC_TYPES =
            setOf(DocumentType.PDF, DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO)

        /**
         * Formats file size into human-readable string.
         * E3: Handles negative values defensively.
         */
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 0) return "Unknown"
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
            if (bytes < 1024 * 1024 * 1024) return "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            return "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }

        /**
         * Heuristic binary-file detection: scan the first [BINARY_DETECT_SCAN_BYTES]
         * bytes for null bytes (0x00).
         *
         * C1: O(n) scan with early exit on first null byte.
         */
        private fun isLikelyBinary(bytes: ByteArray): Boolean {
            val scanLen = bytes.size.coerceAtMost(BINARY_DETECT_SCAN_BYTES)
            for (i in 0 until scanLen) {
                if (bytes[i] == 0.toByte()) return true
            }
            return false
        }

        /**
         * Reads an InputStream into a ByteArray using chunked reads.
         * C2: Pre-allocates buffer when expected size is known.
         * E7: Uses BufferedInputStream with use{} for guaranteed cleanup.
         */
        private fun readStreamChunked(stream: InputStream, expectedSize: Long = -1L): ByteArray =
            BufferedInputStream(stream).use { bis ->
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

        /**
         * Streaming read for very large files (>100MB).
         * Reads only the first [STREAMING_PREVIEW_LINES] lines to avoid OOM,
         * appending a notice about truncation. This allows the user to view
         * the beginning of very large files without crashing.
         */
        private fun readStreamPreview(stream: InputStream, expectedSize: Long): String =
            BufferedInputStream(stream).use { bis ->
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
                    sb.append(
                        "[File truncated: showing first $STREAMING_PREVIEW_LINES lines of ${formatFileSize(
                            expectedSize
                        )}]"
                    )
                }
                sb.toString()
            }
    }

    override suspend fun readFile(uri: Uri, encoding: String?): EditorFileReadOutcome = withContext(Dispatchers.IO) {
        try {
            readFileInternal(uri, encoding)
        } catch (e: SecurityException) {
            EditorFileReadOutcome.Error(
                message = e.message ?: "Permission expired or revoked",
                isSecurityException = true,
                cause = e
            )
        } catch (e: FileNotFoundException) {
            EditorFileReadOutcome.Error(
                message = e.message ?: "File not found",
                isFileNotFound = true,
                cause = e
            )
        } catch (e: Exception) {
            EditorFileReadOutcome.Error(
                message = e.message ?: "Failed to read file",
                cause = e
            )
        }
    }

    private suspend fun readFileInternal(uri: Uri, encoding: String?): EditorFileReadOutcome =
        withContext(Dispatchers.IO) {
            val uriString = uri.toString()
            val isAssetUri = uriString.startsWith("file:///android_asset/")
            val isInternalFile = AppFileManager.isInternalUri(uriString) && !isAssetUri

            // Pre-check file size for SAF URIs
            if (!isAssetUri && !isInternalFile) {
                try {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    val fileSize = docFile?.length() ?: -1L
                    if (fileSize > SIZE_READONLY_THRESHOLD) {
                        val fileName = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                        return@withContext EditorFileReadOutcome.Success(
                            EditorFileReadResult(
                                content = "",
                                language = null,
                                fileName = fileName,
                                fileSize = fileSize,
                                isReadOnly = true,
                                detectedEncoding = "binary",
                                documentType = null,
                                fileSizeWarning = "File is too large (${formatFileSize(fileSize)}) and is read-only",
                                isBinaryFile = true
                            )
                        )
                    }
                } catch (_: Exception) {
                    // DocumentFile.length() may throw, continue with normal read
                }
            }

            val (bytes, fileName, isReadOnly) = when {
                isAssetUri -> {
                    val assetPath = uriString.removePrefix("file:///android_asset/")
                    // D7: Validate asset path to prevent traversal
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
                        return@withContext EditorFileReadOutcome.Success(
                            EditorFileReadResult(
                                content = "",
                                language = null,
                                fileName = file.name,
                                fileSize = file.length(),
                                isReadOnly = true,
                                detectedEncoding = "binary",
                                documentType = null,
                                fileSizeWarning = "File is too large (${formatFileSize(
                                    file.length()
                                )}) and is read-only",
                                isBinaryFile = true
                            )
                        )
                    }
                    // Streaming read for very large files to reduce memory pressure
                    val content: ByteArray = if (file.length() > STREAMING_THRESHOLD) {
                        file.inputStream().use { readStreamPreview(it, file.length()) }.toByteArray(Charsets.UTF_8)
                    } else {
                        file.inputStream().use { readStreamChunked(it, file.length()) }
                    }
                    Triple(content, file.name, false)
                }
                else -> {
                    val docFile = DocumentFile.fromSingleUri(context, uri)
                    val name = docFile?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
                    val expectedSize = docFile?.length() ?: -1L
                    // E3: Wrap openInputStream so permission expiry / missing file
                    // surface as actionable, localized messages instead of generic
                    // SecurityException / FileNotFoundException stacks.
                    val content = try {
                        contentResolver.openInputStream(uri)
                    } catch (e: SecurityException) {
                        throw SecurityException("Permission expired or revoked")
                    } catch (e: FileNotFoundException) {
                        throw FileNotFoundException("External file not found")
                    }?.use {
                        // Streaming read for very large files
                        if (expectedSize > STREAMING_THRESHOLD) {
                            readStreamPreview(it, expectedSize).toByteArray(Charsets.UTF_8)
                        } else {
                            readStreamChunked(it, expectedSize)
                        }
                    } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
                    Triple(content, name, false)
                }
            }

            val fileSize = bytes.size.toLong()
            val highlightDisabled = fileSize >= HIGHLIGHT_DISABLE_THRESHOLD
            val fileSizeWarning = buildString {
                when {
                    fileSize >= SIZE_READONLY_THRESHOLD ->
                        append("File is too large (${formatFileSize(fileSize)}) and is read-only")
                    fileSize >= SIZE_WARN_THRESHOLD ->
                        append("File is large (${formatFileSize(fileSize)}). Editing may be slow.")
                }
                if (highlightDisabled) {
                    if (isNotEmpty()) append("\n")
                    append("Syntax highlighting disabled for large files")
                }
            }.ifEmpty { null }
            val forcedReadOnly = fileSize >= SIZE_READONLY_THRESHOLD || isReadOnly

            var documentType = DocumentTypeHelper.getDocumentType(fileName)

            // E3: Defensive fallback for PDF detection
            if (documentType == null && fileName.lowercase().endsWith(".pdf")) {
                documentType = DocumentType.PDF
            }

            if (documentType == DocumentType.PDF) {
                return@withContext EditorFileReadOutcome.Success(
                    EditorFileReadResult(
                        content = "",
                        language = null,
                        fileName = fileName,
                        fileSize = fileSize,
                        isReadOnly = true,
                        detectedEncoding = "binary",
                        documentType = DocumentType.PDF,
                        fileSizeWarning = fileSizeWarning
                    )
                )
            }

            if (documentType in MEDIA_DOC_TYPES) {
                return@withContext EditorFileReadOutcome.Success(
                    EditorFileReadResult(
                        content = "",
                        language = null,
                        fileName = fileName,
                        fileSize = fileSize,
                        isReadOnly = true,
                        detectedEncoding = "binary",
                        documentType = documentType,
                        fileSizeWarning = fileSizeWarning
                    )
                )
            }

            // D7/E3: Office document size limit to prevent ZIP bomb / OOM
            if (documentType in OFFICE_DOC_TYPES && fileSize > OFFICE_SIZE_LIMIT) {
                return@withContext EditorFileReadOutcome.Success(
                    EditorFileReadResult(
                        content = "",
                        language = null,
                        fileName = fileName,
                        fileSize = fileSize,
                        isReadOnly = true,
                        detectedEncoding = "binary",
                        documentType = documentType,
                        fileSizeWarning = "File is too large (${formatFileSize(fileSize)}) and is read-only"
                    )
                )
            }

            if (documentType != null && documentType !in NON_TEXT_DOC_TYPES) {
                val html = when (documentType) {
                    DocumentType.WORD -> OfficeDocumentParser.parseWord(bytes.inputStream())
                    DocumentType.EXCEL -> OfficeDocumentParser.parseExcel(bytes.inputStream())
                    DocumentType.POWERPOINT -> OfficeDocumentParser.parsePowerPoint(bytes.inputStream())
                    else -> ""
                }
                return@withContext EditorFileReadOutcome.Success(
                    EditorFileReadResult(
                        content = "",
                        language = null,
                        fileName = fileName,
                        fileSize = fileSize,
                        isReadOnly = true,
                        detectedEncoding = "binary",
                        documentType = documentType,
                        renderedHtml = html,
                        fileSizeWarning = fileSizeWarning
                    )
                )
            }

            // Binary detection: extension-based + null-byte scan
            if (DocumentTypeHelper.isNonTextExtension(fileName) || isLikelyBinary(bytes)) {
                return@withContext EditorFileReadOutcome.Success(
                    EditorFileReadResult(
                        content = "",
                        language = null,
                        fileName = fileName,
                        fileSize = fileSize,
                        isReadOnly = true,
                        detectedEncoding = "binary",
                        documentType = null,
                        fileSizeWarning = fileSizeWarning,
                        isBinaryFile = true
                    )
                )
            }

            // OPTIMIZE: [C-01] - 修复三次重复扫描：原实现 decodeBytes 内部检测 (1x) +
            // detectedEncoding 字段再次 detectEncoding (1x) + 可能 fallback (1x) = 3x。
            // 优化后：detectEncoding (1x) + decodeWithEncoding (0x 扫描，仅 BOM strip) = 1x 扫描。
            val (content, detectedEncodingName) = if (encoding != null) {
                // 用户指定编码时直接解码，跳过检测
                String(bytes, Charset.forName(encoding)) to encoding
            } else {
                val detected = EncodingDetector.detectEncoding(bytes, fileName)
                EncodingDetector.decodeWithEncoding(bytes, detected) to detected
            }
            val extension = fileName.substringAfterLast('.', "")
            val language = LanguageConfig.extensionToLanguage(extension)

            EditorFileReadOutcome.Success(
                EditorFileReadResult(
                    content = content,
                    language = if (highlightDisabled) null else language,
                    fileName = fileName,
                    fileSize = fileSize,
                    isReadOnly = forcedReadOnly,
                    detectedEncoding = detectedEncodingName,
                    fileSizeWarning = fileSizeWarning
                )
            )
        }

    override suspend fun writeFile(uri: Uri, content: String, encoding: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uriString = uri.toString()
                val charset = Charset.forName(encoding ?: "UTF-8")
                if (AppFileManager.isInternalUri(uriString) && !uriString.startsWith("file:///android_asset/")) {
                    val file = AppFileManager.getInternalFileFromUri(context, uriString)
                        ?: throw IllegalStateException("Cannot find internal file: $uriString")
                    // D7: Path traversal already validated in getInternalFileFromUri
                    AppFileManager.writeUserFile(file, content)
                } else {
                    // C2: Use OutputStreamWriter for streaming, avoids byte array doubling
                    contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        OutputStreamWriter(outputStream, charset).use { writer ->
                            writer.write(content)
                        }
                    } ?: throw IllegalStateException("Cannot open output stream for URI: $uri")
                }
            }
        }

    override fun isInternalFile(uriString: String): Boolean = AppFileManager.isInternalUri(uriString) &&
        !uriString.startsWith("file:///android_asset/")

    override suspend fun deleteInternalFile(uriString: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInternalFile(uriString)) return@withContext false
        val file = AppFileManager.getInternalFileFromUri(context, uriString)
            ?: return@withContext false
        AppFileManager.deleteUserFile(file)
        true
    }

    /**
     * C-01: 流式打开文件用于全文搜索，避免将整个文件加载到内存。
     *
     * 三类 URI 的处理对齐 readFile()：
     * - asset URI → AssetManager.open()
     * - internal file URI → File.inputStream()
     * - SAF content URI → ContentResolver.openInputStream()
     *
     * 返回 null 的情形：URI 失效、文件不存在、权限过期、asset 路径越界。
     * 调用方负责在使用后关闭流。
     */
    override suspend fun openInputStreamForSearch(uri: Uri): InputStream? = withContext(Dispatchers.IO) {
        val uriString = uri.toString()
        try {
            when {
                uriString.startsWith("file:///android_asset/") -> {
                    val assetPath = uriString.removePrefix("file:///android_asset/")
                    // D7: 防 asset 路径遍历
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
            // E1: 文件不存在 / 权限过期 / IO 错误统一返回 null，调用方优雅跳过
            null
        }
    }
}
