/**
 * 安全事件仓库接口（领域层）。
 *
 * 提供安全事件的持久化查询和清除操作。
 *
 * 接口定义在领域层，由数据层（core:data）实现，遵循依赖倒置原则。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.domain.repository

import com.draftpeek.core.domain.model.SecurityEvent
import kotlinx.coroutines.flow.Flow

/**
 * 安全事件数据仓库接口。
 */
interface SecurityEventRepository {

    /** 所有安全事件的响应式流（按时间倒序） */
    val allEvents: Flow<List<SecurityEvent>>

    /**
     * 插入安全事件记录。
     * @param eventType 事件类型
     * @param threatLevel 威胁等级
     * @param signalsMask 信号 bitmask
     * @param responseLevel 响应级别
     */
    suspend fun record(eventType: String, threatLevel: String, signalsMask: Int, responseLevel: String)

    /**
     * 获取最近 N 条安全事件。
     * @param limit 最大返回数量
     */
    suspend fun getRecentEvents(limit: Int): List<SecurityEvent>

    /**
     * 获取安全事件总数。
     */
    suspend fun getEventCount(): Int

    /**
     * 清空所有安全事件日志。
     */
    suspend fun deleteAll()
}
