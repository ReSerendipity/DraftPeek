/**
 * 用户活动数据访问对象（DAO）接口。
 *
 * 提供对 `user_activity` 表的操作，按日期聚合记录用户的各类行为统计：
 * 文件打开、文本编辑、搜索、预览、代码片段操作、差异对比、使用时长、字符写入等。
 * 使用原子增量更新避免并发计数问题。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.draftpeek.core.data.entity.UserActivity
import kotlinx.coroutines.flow.Flow

/**
 * 用户活动表 DAO 接口。
 *
 * 所有增量操作使用 SQL `SET field = field + :value` 原子更新，
 * 在高并发场景下保证计数准确性。提供日期范围查询用于热力图展示。
 */
@Dao
interface UserActivityDao {

    /**
     * 获取指定日期范围内的活动记录，按日期升序排列（闭区间）。
     * @param start 开始日期（yyyy-MM-dd 格式，包含）
     * @param end 结束日期（yyyy-MM-dd 格式，包含）
     * @return [Flow] 日期范围内的活动记录列表
     */
    @Query("SELECT * FROM user_activity WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getActivityForDateRange(start: String, end: String): Flow<List<UserActivity>>

    /**
     * 获取单个日期的活动记录响应式流。
     * @param date 日期（yyyy-MM-dd 格式）
     * @return [Flow] 发射当日活动记录或 null
     */
    @Query("SELECT * FROM user_activity WHERE date = :date")
    fun getActivityForDate(date: String): Flow<UserActivity?>

    /**
     * 获取最新一条活动记录（按日期降序取第一条）。
     * 用于判断是否需要执行历史数据回填。
     * @return 最新的 [UserActivity]，无记录时返回 null
     */
    @Query("SELECT * FROM user_activity ORDER BY date DESC LIMIT 1")
    suspend fun getLatestActivity(): UserActivity?

    /**
     * 插入或更新活动记录（[OnConflictStrategy.REPLACE]）。
     * @param activity 要保存的活动实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: UserActivity)

    /**
     * 确保指定日期的记录存在：若不存在则插入一条全零初始记录。
     * 使用 INSERT OR IGNORE 避免覆盖已有数据。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒），默认当前时间
     */
    @Query(
        """
        INSERT OR IGNORE INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, previewCount, searchCount, snippetCount, diffCount, usageDurationMinutes, charWriteCount, fileCreateCount, updatedAt)
        VALUES (:date, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, :updatedAt)
        """
    )
    suspend fun ensureDateExists(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日文件打开计数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET fileOpenCount = fileOpenCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementFileOpenCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日文本编辑/保存计数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET textEditCount = textEditCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementTextEditCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日其他操作计数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query(
        "UPDATE user_activity SET otherOperationCount = otherOperationCount + 1, updatedAt = :updatedAt WHERE date = :date"
    )
    suspend fun incrementOtherOperationCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日会话启动计数（App 启动次数）。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET sessionCount = sessionCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementSessionCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日预览次数（Markdown/Office/PDF 预览）。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET previewCount = previewCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementPreviewCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日搜索操作次数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET searchCount = searchCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementSearchCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日代码片段创建次数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET snippetCount = snippetCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementSnippetCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日差异对比次数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET diffCount = diffCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementDiffCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子累加当日使用时长（分钟）。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param minutes 要累加的分钟数（应 > 0）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query(
        "UPDATE user_activity SET usageDurationMinutes = usageDurationMinutes + :minutes, updatedAt = :updatedAt WHERE date = :date"
    )
    suspend fun incrementUsageDurationMinutes(date: String, minutes: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子累加当日字符写入数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param count 要累加的字符数（应 > 0）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query(
        "UPDATE user_activity SET charWriteCount = charWriteCount + :count, updatedAt = :updatedAt WHERE date = :date"
    )
    suspend fun incrementCharWriteCount(date: String, count: Int, updatedAt: Long = System.currentTimeMillis())

    /**
     * 原子递增当日文件创建次数。
     * @param date 日期（yyyy-MM-dd 格式）
     * @param updatedAt 更新时间戳（毫秒）
     */
    @Query("UPDATE user_activity SET fileCreateCount = fileCreateCount + 1, updatedAt = :updatedAt WHERE date = :date")
    suspend fun incrementFileCreateCount(date: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 获取活动表总记录数。
     * 用于判断是否为空库以决定是否执行历史数据回填。
     * @return 记录总数
     */
    @Query("SELECT COUNT(*) FROM user_activity")
    suspend fun getActivityCount(): Int
}
