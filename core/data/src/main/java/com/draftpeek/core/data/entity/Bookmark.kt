/**
 * 书签实体类，对应数据库 `bookmarks` 表。
 *
 * 存储用户收藏的常用文件信息，支持按目录分组展示。URI 设为唯一索引，
 * 确保同一文件不会被重复收藏。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书签文件实体。
 *
 * @property id 自增主键，由数据库自动生成
 * @property uri 文件 URI 字符串（唯一索引，不可重复收藏）
 * @property fileName 文件名，用于列表展示
 * @property directoryUri 父目录 URI，用于按目录分组收藏
 * @property addedAt 添加时间戳（毫秒），默认为当前系统时间
 */
@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["directoryUri"]),
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val directoryUri: String,
    val addedAt: Long = System.currentTimeMillis(),
)
