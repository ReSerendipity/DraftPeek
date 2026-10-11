/**
 * 双向链接领域模型。
 *
 * 领域层纯 Kotlin 表示，与 `core:data` 的 Room 实体字段一一对应，
 * 由数据层实现负责 entity↔model 映射。表示 Markdown 双链语法
 * `[[目标标题]]` 产生的单向引用关系。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 链接关系领域模型。
 *
 * @property id 自增主键（领域层仅透传）
 * @property sourceUri 引用来源文件的 URI
 * @property targetTitle 被引用目标的标题（`[[目标标题]]` 中的目标标题）
 * @property updatedAt 该链接关系最后更新的时间戳（毫秒）
 */
data class Link(
    val id: Long = 0,
    val sourceUri: String,
    val targetTitle: String,
    val updatedAt: Long = System.currentTimeMillis()
)
