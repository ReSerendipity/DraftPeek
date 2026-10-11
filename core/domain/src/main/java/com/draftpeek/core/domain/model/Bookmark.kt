/**
 * 书签领域模型。
 *
 * 领域层纯 Kotlin 表示，与 `core:data` 的 Room 实体字段一一对应，
 * 由数据层实现负责 entity↔model 映射。存储用户收藏的常用文件信息，
 * 支持按目录分组展示。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 书签文件领域模型。
 *
 * @property id 自增主键（领域层仅透传）
 * @property uri 文件 URI 字符串（唯一索引，不可重复收藏）
 * @property fileName 文件名，用于列表展示
 * @property directoryUri 父目录 URI，用于按目录分组收藏
 * @property addedAt 添加时间戳（毫秒），默认为当前系统时间
 */
data class Bookmark(
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val directoryUri: String,
    val addedAt: Long = System.currentTimeMillis()
)
