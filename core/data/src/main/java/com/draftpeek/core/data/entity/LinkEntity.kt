/**
 * 双向链接实体类，对应数据库 `links` 表。
 *
 * 存储 Markdown 双链语法 `[[目标标题]]` 产生的单向引用关系：
 * 某文件（sourceUri）引用了某个目标（targetTitle）。
 * 通过反向查询即可得到"哪些文件引用了某标题"（反向链接 / backlink）。
 *
 * 采用"先删后插"（replace）策略维护文档的链接索引：保存文件时清空
 * 该 sourceUri 的全部旧链接，再写入本次解析出的全部链接，保证索引与正文一致。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 链接关系实体。
 *
 * @property id 自增主键，由数据库自动生成
 * @property sourceUri 引用来源文件的 URI（唯一性由 sourceUri+targetTitle 保证）
 * @property targetTitle 被引用目标的标题（`[[目标标题]]` 中的目标标题）
 * @property updatedAt 该链接关系最后更新的时间戳（毫秒）
 */
@Entity(
    tableName = "links",
    indices = [
        Index(value = ["sourceUri", "targetTitle"], unique = true),
        Index(value = ["targetTitle"]),
        Index(value = ["sourceUri"])
    ]
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUri: String,
    val targetTitle: String,
    val updatedAt: Long = System.currentTimeMillis()
)
