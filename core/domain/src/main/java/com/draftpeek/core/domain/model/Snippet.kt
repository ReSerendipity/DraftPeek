/**
 * 代码片段领域模型。
 *
 * 领域层纯 Kotlin 表示，与 `core:data` 的 Room 实体字段一一对应，
 * 由数据层实现负责 entity↔model 映射。存储用户保存的常用代码模板，
 * 支持按分类和编程语言组织，配合 FTS 全文搜索。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 代码片段领域模型。
 *
 * @property id 自增主键（领域层仅透传，不依赖数据库生成）
 * @property syncId 跨设备同步标识（UUID），**同步键用 `syncId`，绝不用自增 [id]**
 * @property title 片段标题，用于列表展示和搜索
 * @property content 代码内容，支持多行文本
 * @property language 编程语言标识（如 "kotlin", "python"），通用片段为 null
 * @property category 分类名称（如 "Kotlin", "Compose", "Web"）
 * @property createdAt 创建时间戳（毫秒）
 * @property updatedAt 最后更新时间戳（毫秒）
 */
data class Snippet(
    val id: Long = 0,
    val syncId: String = "",
    val title: String,
    val content: String,
    val language: String?,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)
