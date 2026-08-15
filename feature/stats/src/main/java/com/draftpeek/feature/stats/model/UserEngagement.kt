/**
 * 文件: UserEngagement.kt
 * 功能: 统计模块数据模型 - 用户活跃度评估
 * 描述: 定义用户活跃度等级枚举和活跃度评估结果数据类，用于分析用户近期使用模式
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

/**
 * 用户活跃度等级枚举。
 *
 * 根据用户最近 7 天和 14 天的活动模式评估用户的活跃程度。
 */
enum class UserEngagementLevel {
    /** 高活跃度：最近 7 天内有 4 天及以上活跃 */
    HIGH,
    /** 中等活跃度：最近 7 天内有 1-3 天活跃 */
    MEDIUM,
    /** 休眠状态：最近 14 天内无任何活动 */
    DORMANT,
}

/**
 * 用户活跃度评估结果。
 *
 * 包含活跃度等级及相关统计指标，用于在 UI 层展示用户活跃状态。
 *
 * @property level 评估得出的活跃度等级
 * @property weeklyActiveDays 最近 7 天内的活跃天数
 * @property totalSessions 最近 30 天内的应用会话总数
 * @property daysSinceLastActivity 距离上次活动的天数，无活动记录时为 -1
 */
data class UserEngagement(
    val level: UserEngagementLevel,
    val weeklyActiveDays: Int,
    val totalSessions: Int,
    val daysSinceLastActivity: Int,
)
