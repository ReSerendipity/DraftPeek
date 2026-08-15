/**
 * 文件：EditorUiState.kt
 * 功能：编辑器界面 UI 状态密封类定义
 * 主要类/接口：EditorUiState（Loading、LoadingWithProgress、Success、Error）
 * 模块依赖：
 *   - androidx.compose.runtime：Compose 状态注解
 *   - core/common/util/DocumentType：文档类型枚举
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.draftpeek.core.common.util.DocumentType

/**
 * 编辑器界面的 UI 状态密封类。
 *
 * 使用 @Stable 注解标记，以便 Compose 在重新发射相同的密封类实例时
 * （通过引用相等性判断）可以跳过重组，提升性能。
 *
 * 状态流转：
 * Loading/LoadingWithProgress → Success（加载成功）或 Error（加载失败）
 */
@Stable
sealed class EditorUiState {

    /**
     * 文件内容正在加载中（无进度显示）。
     *
     * 使用场景：小文件快速加载时显示简单加载指示器。
     */
    data object Loading : EditorUiState()

    /**
     * 文件内容正在加载中（带进度跟踪）。
     *
     * 使用场景：加载大文件（大于 [CHUNKED_LOADING_THRESHOLD]）时，
     * 用户可以看到进度指示器而非冻结的加载圈，提升用户体验。
     *
     * @property loadedBytes 已读取的字节数
     * @property totalBytes 文件总字节数，未知时为 -1
     * @property progress 进度分数 0f..1f，总大小未知时为 -1f
     */
    @Immutable
    data class LoadingWithProgress(
        val loadedBytes: Long = 0,
        val totalBytes: Long = -1,
        val progress: Float = -1f,
    ) : EditorUiState() {
        /**
         * 人类可读的进度文本，例如 "12.4 MB / 50 MB"。
         */
        val progressText: String
            get() = buildString {
                append(formatBytes(loadedBytes))
                if (totalBytes > 0) {
                    append(" / ")
                    append(formatBytes(totalBytes))
                }
            }

        /**
         * 百分比文本，例如 "24%"。总大小未知时返回 null。
         */
        val percentageText: String?
            get() = if (progress in 0f..1f) "${(progress * 100).toInt()}%" else null

        companion object {
            /**
             * 格式化字节数为人类可读格式。
             *
             * @param bytes 字节数
             * @return 格式化后的字符串（B/KB/MB/GB）
             */
            private fun formatBytes(bytes: Long): String = when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
                else -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
            }
        }
    }

    /**
     * 文件内容加载成功状态。
     *
     * 所有属性均为不可变；计算属性派生自创建后不会改变的 [fileName] 和 [documentType]。
     *
     * @property content 文件文本内容
     * @property language 编程语言标识符，null 表示纯文本
     * @property fileName 文件名（含扩展名）
     * @property isReadOnly 是否为只读模式
     * @property documentType 文档类型枚举
     * @property renderedHtml 预渲染的 HTML 内容（用于预览模式）
     * @property fileSizeWarning 文件大小警告信息
     * @property fileSize 文件大小（字节）
     * @property isBinaryFile 是否为二进制文件
     * @property isTruncated 内容是否被截断（用于大于100MB文件的流式预览）
     */
    @Immutable
    data class Success(
        val content: String,
        val language: String?,
        val fileName: String,
        val isReadOnly: Boolean = false,
        val documentType: DocumentType? = null,
        val renderedHtml: String? = null,
        val fileSizeWarning: String? = null,
        val fileSize: Long = 0,
        val isBinaryFile: Boolean = false,
        val isTruncated: Boolean = false,
    ) : EditorUiState() {

        private val lowerFileName: String get() = fileName.lowercase()

        /**
         * 是否为 Markdown 文件（.md 或 .markdown 扩展名）。
         */
        val isMarkdownFile: Boolean
            get() = lowerFileName.let { name ->
                name.endsWith(".md") || name.endsWith(".markdown")
            }

        /**
         * 是否为 HTML 文件（.html 或 .htm 扩展名）。
         */
        val isHtmlFile: Boolean
            get() = lowerFileName.let { name ->
                name.endsWith(".html") || name.endsWith(".htm")
            }

        /**
         * 是否可预览（Markdown 或 HTML 文件）。
         */
        val isPreviewable: Boolean
            get() = isMarkdownFile || isHtmlFile

        /**
         * 是否为 PDF 文档（需要内嵌 PDF 渲染器）。
         */
        val isPdf: Boolean
            get() = documentType == DocumentType.PDF || lowerFileName.endsWith(".pdf")

        /**
         * 是否为 Office 文档（Word/Excel/PPT）。
         */
        val isOfficeDocument: Boolean
            get() = documentType in listOf(
                DocumentType.WORD, DocumentType.EXCEL, DocumentType.POWERPOINT,
            ) || lowerFileName.let {
                it.endsWith(".doc") || it.endsWith(".docx") ||
                it.endsWith(".xls") || it.endsWith(".xlsx") ||
                it.endsWith(".ppt") || it.endsWith(".pptx")
            }

        /**
         * 是否为媒体文件（图片、音频或视频）。
         */
        val isMediaFile: Boolean
            get() = documentType in listOf(
                DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO,
            ) || lowerFileName.let {
                it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") ||
                it.endsWith(".gif") || it.endsWith(".webp") || it.endsWith(".bmp") ||
                it.endsWith(".mp3") || it.endsWith(".wav") || it.endsWith(".ogg") ||
                it.endsWith(".flac") || it.endsWith(".aac") || it.endsWith(".m4a") ||
                it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                it.endsWith(".avi") || it.endsWith(".mov")
            }

        /**
         * 是否应显示"无法显示"消息而非编辑器。
         *
         * 使用场景：二进制文件或媒体文件无法在文本编辑器中显示。
         */
        val isNonDisplayable: Boolean
            get() = isBinaryFile || isMediaFile
    }

    /**
     * 加载文件时发生错误。
     *
     * @property message 错误消息文本，用于 UI 显示给用户
     */
    @Immutable
    data class Error(val message: String) : EditorUiState()
}
