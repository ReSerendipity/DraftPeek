/**
 * 文件通用工具模块。
 *
 * 提供文件大小格式化、文件扩展名提取、文本文件类型判断、大文件检测等通用文件操作工具方法。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import java.util.Locale

/**
 * 文件通用工具对象。
 *
 * 提供文件大小格式化、文件扩展名提取、文本文件MIME类型判断等静态工具方法。
 */
object FileUtils {

    private val FILE_SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

    /**
     * 将字节数格式化为人类可读的文件大小字符串。
     *
     * 自动选择合适的单位（B/KB/MB/GB/TB），对于非整数大小保留一位小数。
     *
     * @param size 文件大小（字节数）
     * @return 格式化后的字符串，如 "1.5 KB" 或 "3.2 MB"
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        var bytes = size.toDouble()
        var unitIndex = 0

        while (bytes >= 1024.0 && unitIndex < FILE_SIZE_UNITS.lastIndex) {
            bytes /= 1024.0
            unitIndex++
        }

        return if (bytes == bytes.toLong().toDouble()) {
            String.format(Locale.getDefault(), "%d %s", bytes.toLong(), FILE_SIZE_UNITS[unitIndex])
        } else {
            String.format(Locale.getDefault(), "%.1f %s", bytes, FILE_SIZE_UNITS[unitIndex])
        }
    }

    /**
     * 从文件名中提取扩展名（不含前导点）。
     *
     * @param fileName 文件名，例如 "MainActivity.kt"
     * @return 小写的扩展名，例如 "kt"；无扩展名时返回空字符串
     */
    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase(Locale.getDefault())
        } else {
            ""
        }
    }

    /**
     * 判断给定MIME类型是否为DraftPeek可在编辑器中打开的文本类文件。
     *
     * 支持 `text/` 前缀的MIME类型，以及常见的文本格式如JSON、XML、JavaScript、YAML等。
     *
     * @param mimeType 待检查的MIME类型
     * @return 如果是文本类文件返回 `true`
     */
    fun isTextFile(mimeType: String): Boolean {
        if (mimeType.isBlank()) return false
        return mimeType.startsWith("text/", ignoreCase = true) ||
            mimeType.equals("application/json", ignoreCase = true) ||
            mimeType.equals("application/xml", ignoreCase = true) ||
            mimeType.equals("application/javascript", ignoreCase = true) ||
            mimeType.equals("application/x-javascript", ignoreCase = true) ||
            mimeType.equals("application/xhtml+xml", ignoreCase = true) ||
            mimeType.equals("application/x-yaml", ignoreCase = true) ||
            mimeType.equals("application/yaml", ignoreCase = true)
    }

    /**
     * 判断文件是否超过指定大小阈值。
     *
     * @param size 文件大小（字节数）
     * @param threshold 阈值（字节数），默认50MB
     * @return 如果文件大小超过阈值返回 `true`
     */
    fun isLargeFile(size: Long, threshold: Long = 50 * 1024 * 1024): Boolean = size > threshold

    /**
     * 将 URI 字符串解析为真实本地文件系统路径。
     *
     * 终端工作目录必须是真实本地文件系统路径。此方法用于判断文件页/编辑器中的
     * 条目是否可在终端中打开：
     * - `file://` URI 或以 `/` 开头的路径 → 返回真实路径
     * - `content://` (SAF)、`ftp://`、`sftp://` 等无真实路径 → 返回 `null`
     *
     * @param uri URI 字符串
     * @return 真实本地路径，或 `null`（如果无法解析为本地路径）
     */
    fun resolveLocalPath(uri: String): String? {
        if (uri.isBlank()) return null

        // 已经是文件系统绝对路径
        if (uri.startsWith("/")) return uri

        // file:// URI
        if (uri.startsWith("file://")) {
            return uri.removePrefix("file://").let { path ->
                // 处理 file:/// 三斜杠情况
                if (path.startsWith("/")) path else "/$path"
            }
        }

        // content:// (SAF)、ftp://、sftp:// 等无真实路径
        // 无法在终端中 cd 到这些位置
        return null
    }
}
