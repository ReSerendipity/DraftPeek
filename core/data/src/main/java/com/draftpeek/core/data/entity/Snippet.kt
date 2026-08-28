/**
 * 代码片段实体类，对应数据库 `snippets` 表。
 *
 * 存储用户保存的常用代码模板，支持按分类和编程语言组织，配合 FTS4 虚拟表实现全文搜索。
 * 创建/更新时间戳用于排序和变更追踪。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 代码片段实体。
 *
 * 索引设计：
 * - (category, updatedAt)：按分类查询并按时间排序
 * - (language, updatedAt)：按语言查询并按时间排序
 * - updatedAt：单独按更新时间排序
 * - category：单独按分类过滤
 *
 * @property id 自增主键，由数据库自动生成
 * @property title 片段标题，用于列表展示和搜索
 * @property content 代码内容，支持多行文本
 * @property language 编程语言标识（如 "kotlin", "python"），通用片段为 null
 * @property category 分类名称（如 "Kotlin", "Compose", "Web"）
 * @property createdAt 创建时间戳（毫秒）
 * @property updatedAt 最后更新时间戳（毫秒）
 */
@Entity(
    tableName = "snippets",
    indices = [
        Index(value = ["category", "updatedAt"]),
        Index(value = ["language", "updatedAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["category"])
    ]
)
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val language: String?,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)
