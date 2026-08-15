/**
 * 用户每日活动统计实体类，对应数据库 `user_activity` 表。
 *
 * 按日期（yyyy-MM-dd）聚合用户的各类行为统计数据，用于 GitHub 风格的热力图展示
 * 和使用情况分析。日期作为主键，每天一条记录。
 *
 * ## 加权强度计算
 * 不同操作类型具有不同权重，用于热力图颜色分级：
 * - 文本编辑（×5）：最高权重，代表核心创作行为
 * - 文件打开/创建（×3）：次高权重
 * - 预览/搜索/片段/差异对比（×2）：中等权重
 * - 其他操作/会话/时长/字符写入（×1）：基础权重
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日用户活动统计实体。
 *
 * 所有计数字段默认为 0，通过 DAO 的原子增量操作更新。
 * 提供多个计算属性用于热力图渲染和活动等级评估。
 *
 * @property date 日期字符串，格式 yyyy-MM-dd（主键，唯一索引）
 * @property fileOpenCount 文件打开次数（权重 ×3）
 * @property textEditCount 文本编辑/保存次数（权重 ×5，核心创作行为）
 * @property otherOperationCount 其他操作次数（权重 ×1）
 * @property sessionCount App 会话启动次数（权重 ×1）
 * @property previewCount Markdown/Office/PDF 预览次数（权重 ×2）
 * @property searchCount 搜索操作次数（权重 ×2）
 * @property snippetCount 代码片段创建/使用次数（权重 ×2）
 * @property diffCount 差异对比次数（权重 ×2）
 * @property usageDurationMinutes 使用时长（分钟，权重 ×1）
 * @property charWriteCount 写入字符数（权重 ×1）
 * @property fileCreateCount 文件创建次数（权重 ×3）
 * @property updatedAt 记录最后更新时间戳（毫秒）
 */
@Entity(
    tableName = "user_activity",
    indices = [Index(value = ["date"], unique = true)],
)
data class UserActivity(
    @PrimaryKey
    val date: String,
    val fileOpenCount: Int = 0,
    val textEditCount: Int = 0,
    val otherOperationCount: Int = 0,
    val sessionCount: Int = 0,
    val previewCount: Int = 0,
    val searchCount: Int = 0,
    val snippetCount: Int = 0,
    val diffCount: Int = 0,
    val usageDurationMinutes: Int = 0,
    val charWriteCount: Int = 0,
    val fileCreateCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * 计算加权活动总强度，用于热力图颜色深度渲染。
     * 不同操作类型按业务重要性赋予不同权重。
     * @return 加权后的总强度值
     */
    fun totalIntensity(): Int =
        fileOpenCount * 3 +
            textEditCount * 5 +
            otherOperationCount * 1 +
            sessionCount * 1 +
            previewCount * 2 +
            searchCount * 2 +
            snippetCount * 2 +
            diffCount * 2 +
            usageDurationMinutes * 1 +
            charWriteCount * 1 +
            fileCreateCount * 3

    /**
     * 计算未加权的总操作次数，用于简单计数和活动等级划分。
     * @return 所有操作类型的简单总和
     */
    fun totalOperations(): Int =
        fileOpenCount + textEditCount + otherOperationCount +
            sessionCount + previewCount + searchCount +
            snippetCount + diffCount +
            usageDurationMinutes + charWriteCount + fileCreateCount

    /**
     * 判断当天是否有任何用户活动。
     * @return true 表示有活动，false 表示无活动
     */
    fun hasActivity(): Boolean = totalIntensity() > 0

    /**
     * 根据未加权总操作数计算活动等级（0-4），用于热力图颜色分级。
     * 等级划分：
     * - 0: 0 次操作（无活动）
     * - 1: 1-5 次（低活跃）
     * - 2: 6-15 次（中等活跃）
     * - 3: 16-30 次（较高活跃）
     * - 4: 30+ 次（高活跃）
     * @return 活动等级 0-4
     */
    fun activityLevel(): Int {
        val ops = totalOperations()
        return when {
            ops == 0 -> 0
            ops <= 5 -> 1
            ops <= 15 -> 2
            ops <= 30 -> 3
            else -> 4
        }
    }
}
