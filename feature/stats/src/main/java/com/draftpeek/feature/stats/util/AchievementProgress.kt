package com.draftpeek.feature.stats.util

import com.draftpeek.feature.stats.model.Achievement
import com.draftpeek.feature.stats.model.AchievementCategory
import com.draftpeek.feature.stats.model.AchievementStats

/**
 * 成就的「卡级进度」推导（设计画布 s21 标注 1）。
 *
 * 现状：`Achievement` 只存解锁判定 lambda（`check`），没有可直接展示的目标值/当前值。
 * 本对象按「分类 + 等级（tier）」推导 `(target, 当前进度)`：
 *
 * - 仅覆盖**可数值化**的分类：阅读 / 创建 / 时长 / 字符 / 连续天数 / 里程碑；
 *   时段活跃、考勤、节假日等不适用分类返回 null，UI 不展示进度条（诚实留白，不造假数）。
 * - `target` 必须与 `AchievementDefinitions` 的 check 阈值一致 ——
 *   由 `AchievementProgressConsistencyTest` 用「target-1 未解锁 / target 已解锁」
 *   对全部成就逐格反向验证；定义改阈值而此处未跟随时测试立即红灯，防两处漂移。
 * - 进度显示为纯数字 `N / M`（单位语义由成就描述承担），不引入新词条。
 */
object AchievementProgress {

    /** 该成就是否有可展示的数值目标和进度；不可数值化返回 null。 */
    fun targetOf(achievement: Achievement): Int? = targets(achievement.category)?.getOrNull(achievement.tier - 1)

    /**
     * 当前进度值（与 [targetOf] 成对；分类不可数值化时为 null）。
     *
     * 口径与解锁判定保持一致：STREAK 用 [AchievementStats.maxStreak]
     * （历史最长连续天数，避免「已解锁但进度条不满」的自相矛盾）。
     */
    fun progressOf(achievement: Achievement, stats: AchievementStats): Int? {
        if (targets(achievement.category) == null) return null
        val raw: Long = when (achievement.category) {
            AchievementCategory.READ -> stats.totalRead.toLong()
            AchievementCategory.CREATE -> stats.totalCreate.toLong()
            AchievementCategory.DURATION -> stats.totalMinutes / 60
            AchievementCategory.CHARS -> stats.totalChars
            AchievementCategory.STREAK -> stats.maxStreak.toLong()
            AchievementCategory.MILESTONE -> stats.totalDays.toLong()
            else -> return null
        }
        return raw.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /** 各数值化分类的阈值（按 tier 升序）；与 definitions 中 check 的 `>=` 边界一致。 */
    private fun targets(category: AchievementCategory): List<Int>? = when (category) {
        AchievementCategory.READ -> listOf(10, 50, 200, 500, 1000)
        AchievementCategory.CREATE -> listOf(5, 20, 50, 200, 500)
        AchievementCategory.DURATION -> listOf(1, 10, 50, 200, 500)
        AchievementCategory.CHARS -> listOf(1_000, 10_000, 100_000, 500_000, 1_000_000)
        AchievementCategory.STREAK -> listOf(3, 7, 14, 30, 60, 100, 365)
        AchievementCategory.MILESTONE -> listOf(30, 90, 180, 365, 730, 1095)
        else -> null
    }
}
