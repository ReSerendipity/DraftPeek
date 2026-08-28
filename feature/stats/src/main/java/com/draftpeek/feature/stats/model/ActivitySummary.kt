/**
 * 文件: ActivitySummary.kt
 * 功能: 统计模块数据模型 - 活动汇总数据类
 * 描述: 定义指定时间范围内的用户活动统计汇总数据，包含各类操作计数、活跃天数、连续使用天数等核心统计指标
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

/**
 * 指定时间范围内的活动汇总数据。
 *
 * 该数据类聚合了用户在选定时间段内的各类操作统计，用于在统计页面展示核心数据指标。
 *
 * @property fileOpenCount 文件打开次数
 * @property textEditCount 文本编辑次数
 * @property otherOperationCount 其他操作次数
 * @property previewCount 预览次数
 * @property searchCount 搜索次数
 * @property snippetCount 代码片段使用次数
 * @property diffCount 差异对比次数
 * @property activeDays 活跃天数
 * @property currentStreak 当前连续使用天数
 * @property longestStreak 最长连续使用天数
 * @property peakHour 活跃度最高的小时（0-23），无数据时为 null
 * @property favoriteExtension 最常使用的文件扩展名，无数据时显示为 "—"
 */
data class ActivitySummary(
    val fileOpenCount: Int = 0,
    val textEditCount: Int = 0,
    val otherOperationCount: Int = 0,
    val previewCount: Int = 0,
    val searchCount: Int = 0,
    val snippetCount: Int = 0,
    val diffCount: Int = 0,
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val peakHour: Int? = null,
    val favoriteExtension: String = "—"
)
