/**
 * 文档类型识别工具模块。
 *
 * 根据文件扩展名识别文档类型（PDF/Word/Excel/PowerPoint/图片/音频/视频），
 * 判断是否为Office文档、媒体文件、非文本二进制文件，并提供对应MIME类型映射。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 文档类型工具对象。
 *
 * 提供文档类型识别、Office文档判断、媒体文件判断、非文本扩展名判断、MIME类型映射等功能。
 * 非文本扩展名列表作为第一道防线（第二道防线是原始字节空字节扫描）。
 */
object DocumentTypeHelper {

    /** Android BitmapFactory支持的图片文件扩展名集合 */
    private val IMAGE_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "gif", "webp", "bmp",
    )

    /** 音频文件扩展名集合 */
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "opus",
    )

    /** 视频文件扩展名集合 */
    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v", "flv",
    )

    /**
     * 已知的非文本（二进制）文件扩展名集合，不应作为文本解码。
     * 这是第一道防线；第二道防线是原始字节空字节扫描。
     */
    private val NON_TEXT_EXTENSIONS = setOf(
        "exe", "dll", "so", "class", "dex", "apk", "aab", "wasm", "o", "a", "lib",
        "dylib", "sys", "bin", "elf", "msi",
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lz", "lz4", "zst", "arj",
        "cab", "iso", "dmg", "pkg", "deb", "rpm",
        "db", "sqlite", "sqlite3", "mdb", "accdb", "frm", "myd", "myi", "ibd",
        "ttf", "otf", "woff", "woff2", "eot",
        "dat", "ico", "psd", "ai", "blend", "sketch", "xd", "fig",
        "keystore", "jks", "p12", "pfx", "cer", "der", "pub", "key",
        "nib", "storyboard", "xib", "plist", "mobileprovision",
        "proto", "pb",
        "thmx", "fnt", "fon", "suit", "sdf",
    )

    /**
     * 根据文件名获取文档类型。
     *
     * @param fileName 文件名
     * @return 识别到的DocumentType，无法识别返回null
     */
    fun getDocumentType(fileName: String): DocumentType? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> DocumentType.PDF
            "doc", "docx" -> DocumentType.WORD
            "xls", "xlsx" -> DocumentType.EXCEL
            "ppt", "pptx" -> DocumentType.POWERPOINT
            in IMAGE_EXTENSIONS -> DocumentType.IMAGE
            in AUDIO_EXTENSIONS -> DocumentType.AUDIO
            in VIDEO_EXTENSIONS -> DocumentType.VIDEO
            else -> null
        }
    }

    /**
     * 判断文件是否为Office文档（PDF/Word/Excel/PowerPoint）。
     *
     * @param fileName 文件名
     * @return 如果是Office文档返回 `true`
     */
    fun isOfficeDocument(fileName: String): Boolean {
        val type = getDocumentType(fileName)
        return type in listOf(DocumentType.PDF, DocumentType.WORD, DocumentType.EXCEL, DocumentType.POWERPOINT)
    }

    /**
     * 判断文件是否为媒体类型（图片、音频或视频）。
     *
     * @param fileName 文件名
     * @return 如果是媒体文件返回 `true`
     */
    fun isMediaFile(fileName: String): Boolean {
        val type = getDocumentType(fileName)
        return type in listOf(DocumentType.IMAGE, DocumentType.AUDIO, DocumentType.VIDEO)
    }

    /**
     * 判断文件扩展名是否属于已知的非文本（二进制）格式。
     *
     * @param fileName 文件名
     * @return 如果是非文本扩展名返回 `true`
     */
    fun isNonTextExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in NON_TEXT_EXTENSIONS
    }

    /**
     * 获取文档类型对应的MIME类型字符串。
     *
     * @param type 文档类型
     * @return MIME类型字符串
     */
    fun getMimeType(type: DocumentType): String = when (type) {
        DocumentType.PDF -> "application/pdf"
        DocumentType.WORD -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        DocumentType.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        DocumentType.POWERPOINT -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        DocumentType.IMAGE -> "image/*"
        DocumentType.AUDIO -> "audio/*"
        DocumentType.VIDEO -> "video/*"
    }
}

/**
 * 文档类型枚举。
 */
enum class DocumentType {
    /** PDF文档 */
    PDF,
    /** Word文档（.doc/.docx） */
    WORD,
    /** Excel表格（.xls/.xlsx） */
    EXCEL,
    /** PowerPoint演示文稿（.ppt/.pptx） */
    POWERPOINT,
    /** 图片文件 */
    IMAGE,
    /** 音频文件 */
    AUDIO,
    /** 视频文件 */
    VIDEO,
}
