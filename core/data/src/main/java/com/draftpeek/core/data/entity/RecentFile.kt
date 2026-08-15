/**
 * 最近文件实体类，对应数据库 `recent_files` 表。
 *
 * 记录用户打开过的文件历史，支持收藏标记和阅读位置持久化。
 * URI 作为主键，同一文件多次打开只会更新时间戳而非重复插入。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 最近打开文件实体。
 *
 * 用于首页"最近文件"列表展示，以及重新打开文件时恢复阅读位置。
 * 索引设计优化：按收藏状态+最后打开时间、最后打开时间查询。
 *
 * @property uri 文件 URI 字符串（主键，唯一标识一个文件）
 * @property fileName 文件名，用于列表展示
 * @property language 编程语言标识（如 "kotlin"），未知时为 null
 * @property lastOpenedAt 最后打开时间戳（毫秒），用于排序
 * @property isFavorite 是否被用户收藏，收藏的文件不会被自动清理
 * @property fileSize 文件大小（字节），用于大小警告
 * @property cursorLine 光标所在行（从 1 开始），用于恢复阅读位置
 * @property cursorColumn 光标所在列（从 1 开始），用于恢复阅读位置
 * @property scrollX 水平滚动偏移像素，用于恢复阅读位置
 * @property scrollY 垂直滚动偏移像素，用于恢复阅读位置
 */
@Entity(
    tableName = "recent_files",
    indices = [
        Index(value = ["isFavorite", "lastOpenedAt"]),
        Index(value = ["lastOpenedAt"]),
    ]
)
data class RecentFile(
    @PrimaryKey val uri: String,
    val fileName: String,
    val language: String?,
    val lastOpenedAt: Long,
    val isFavorite: Boolean = false,
    val fileSize: Long = 0,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val scrollX: Int = 0,
    val scrollY: Int = 0,
)
