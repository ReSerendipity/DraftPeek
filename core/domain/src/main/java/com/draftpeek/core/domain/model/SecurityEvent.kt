/**
 * 安全事件日志领域模型。
 *
 * 领域层纯 Kotlin 表示，与 `core:data` 的 Room 实体字段一一对应，
 * 由数据层实现负责 entity↔model 映射。纯本地 SQLCipher 加密存储，
 * 不上传任何服务器。字段设计严格无隐私信息。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.domain.model

/**
 * 安全事件日志领域模型。
 *
 * @property id 自增主键（领域层仅透传）
 * @property eventType 事件类型：DETECTION / RESPONSE / INTEGRITY_VERIFY / USER_ACK
 * @property threatLevel 威胁等级：SAFE / SUSPICIOUS / HOSTILE
 * @property signalsMask 检测信号 ordinal bitmask
 * @property responseLevel 响应级别：NONE / WARNING / LOCKED / SELF_DEFEND
 * @property timestampEpochMs UTC 毫秒时间戳
 * @property anonymizedDeviceId 匿名设备 ID（每次重装随机生成的 UUID）
 * @property appVersionCode 应用版本号
 * @property extra 可选 JSON 扩展（未来使用）
 */
data class SecurityEvent(
    val id: Long = 0,
    val eventType: String,
    val threatLevel: String,
    val signalsMask: Int,
    val responseLevel: String,
    val timestampEpochMs: Long,
    val anonymizedDeviceId: String,
    val appVersionCode: Int,
    val extra: String = ""
)
