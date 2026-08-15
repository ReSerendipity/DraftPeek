/**
 * 安全事件日志实体（纯本地 SQLCipher 加密存储，不上传任何服务器）。
 *
 * 字段设计严格无隐私信息：仅存事件类型、级别、时间戳、设备匿名 ID（每次重装随机）。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 安全事件日志实体，对应数据库 `security_events` 表。
 *
 * @property id 自增主键
 * @property eventType 事件类型：DETECTION / RESPONSE / INTEGRITY_VERIFY / USER_ACK
 * @property threatLevel 威胁等级：SAFE / SUSPICIOUS / HOSTILE
 * @property signalsMask AiDetectionSignal.ordinal bitmask
 * @property responseLevel 响应级别：NONE / WARNING / LOCKED / SELF_DEFEND
 * @property timestampEpochMs UTC 毫秒时间戳
 * @property anonymizedDeviceId 匿名设备 ID（每次重装随机生成的 UUID）
 * @property appVersionCode BuildConfig.VERSION_CODE
 * @property extra 可选 JSON 扩展（未来使用）
 */
@Entity(
    tableName = "security_events",
    indices = [
        Index(value = ["timestampEpochMs"]),
        Index(value = ["eventType"]),
    ],
)
data class SecurityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val threatLevel: String,
    val signalsMask: Int,
    val responseLevel: String,
    val timestampEpochMs: Long,
    val anonymizedDeviceId: String,
    val appVersionCode: Int,
    val extra: String = "",
)
