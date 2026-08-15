/**
 * 编辑器文件领域模型文件。
 *
 * 定义可在编辑器中打开的文件的领域层数据模型，独立于数据层实现，
 * 通过数据模块中的映射器与数据层类型进行转换。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 表示可在编辑器中打开的文件的领域模型。
 *
 * 这是一个纯净的领域层表示，独立于任何数据层关注点
 * （Room实体、SAF URI等）。它通过数据模块中的映射器
 * 与数据层类型进行双向映射。
 *
 * @property uri 文件URI字符串
 * @property fileName 文件名
 * @property language 编程语言标识（可为null）
 * @property fileSize 文件大小（字节）
 * @property isReadOnly 是否为只读文件
 * @property detectedEncoding 检测到的文件编码，默认为UTF-8
 * @property isBinary 是否为二进制文件
 */
data class EditorFile(
    val uri: String,
    val fileName: String,
    val language: String?,
    val fileSize: Long,
    val isReadOnly: Boolean = false,
    val detectedEncoding: String = "UTF-8",
    val isBinary: Boolean = false,
)
