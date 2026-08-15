/**
 * 用户活动统计仓库接口。
 *
 * 负责记录和查询用户每日活动数据，包括文件打开、文本编辑、搜索、预览、
 * 代码片段操作、差异对比、使用时长、字符写入等行为统计。数据用于热力图展示。
 *
 * 所有 record* 方法使用 Mutex 保证并发安全，日期格式统一为 yyyy-MM-dd。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.entity.UserActivity
import kotlinx.coroutines.flow.Flow

/**
 * 用户活动数据仓库接口。
 */
interface UserActivityRepository {

    /**
     * 观察指定日期范围内的活动记录（闭区间）。
     * @param start 开始日期（yyyy-MM-dd，包含）
     * @param end 结束日期（yyyy-MM-dd，包含）
     * @return [Flow] 活动记录列表，按日期升序
     */
    fun getActivityForDateRange(start: String, end: String): Flow<List<UserActivity>>

    /**
     * 观察单个日期的活动记录。
     * @param date 日期（yyyy-MM-dd）
     * @return [Flow] 发射当日活动或 null
     */
    fun getActivityForDate(date: String): Flow<UserActivity?>

    /** 记录一次 App 启动/会话（当日 sessionCount +1） */
    suspend fun recordAppLaunch()

    /** 记录一次文件打开（当日 fileOpenCount +1） */
    suspend fun recordFileOpen()

    /** 记录一次文本编辑/保存（当日 textEditCount +1） */
    suspend fun recordTextEdit()

    /** 记录一次其他通用操作（当日 otherOperationCount +1） */
    suspend fun recordOtherOperation()

    /** 记录一次搜索操作（当日 searchCount +1） */
    suspend fun recordSearch()

    /** 记录一次导出操作（归类为 otherOperation） */
    suspend fun recordExport()

    /** 记录一次代码片段创建（当日 snippetCount +1） */
    suspend fun recordSnippetCreated()

    /** 记录一次文件创建/删除操作（归类为 otherOperation） */
    suspend fun recordFileManagement()

    /** 记录一次预览（Markdown/Office/PDF）（当日 previewCount +1） */
    suspend fun recordPreview()

    /** 记录一次差异对比（当日 diffCount +1） */
    suspend fun recordDiff()

    /**
     * 累加使用时长（分钟）。
     * @param minutes 要累加的分钟数（应 > 0）
     */
    suspend fun recordUsageDuration(minutes: Int)

    /**
     * 累加字符写入数。
     * @param count 要累加的字符数（应 > 0）
     */
    suspend fun recordCharWrite(count: Int)

    /** 记录一次文件创建（当日 fileCreateCount +1） */
    suspend fun recordFileCreate()

    /**
     * 从现有 `recent_files.lastOpenedAt` 数据回填历史活动记录。
     * 安全可重复调用：已有活动数据时会跳过，避免覆盖用户数据。
     */
    suspend fun backfillFromRecentFiles()
}
