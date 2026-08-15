/**
 * 用户活动摘要领域模型文件。
 *
 * 定义用户活动统计的领域层数据模型，独立于Room实体结构，
 * 用于统计功能模块展示指定时间段内的用户活动汇总数据。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 表示指定时间段内用户活动摘要的领域模型。
 *
 * 这是一个纯净的领域层表示，供统计功能使用，
 * 独立于Room实体结构。
 *
 * @property date 日期字符串（格式：yyyy-MM-dd）
 * @property fileOpenCount 文件打开次数
 * @property sessionCount 会话次数
 * @property textEditCount 文本编辑次数
 * @property searchCount 搜索次数
 * @property exportCount 导出次数
 * @property snippetCount 代码片段操作次数
 * @property previewCount 预览次数
 * @property diffCount 差异对比次数
 * @property usageDurationMinutes 使用时长（分钟）
 * @property charWriteCount 字符写入数量
 * @property fileCreateCount 文件创建次数
 */
data class UserActivitySummary(
    val date: String,
    val fileOpenCount: Int = 0,
    val sessionCount: Int = 0,
    val textEditCount: Int = 0,
    val searchCount: Int = 0,
    val exportCount: Int = 0,
    val snippetCount: Int = 0,
    val previewCount: Int = 0,
    val diffCount: Int = 0,
    val usageDurationMinutes: Int = 0,
    val charWriteCount: Long = 0,
    val fileCreateCount: Int = 0,
)
