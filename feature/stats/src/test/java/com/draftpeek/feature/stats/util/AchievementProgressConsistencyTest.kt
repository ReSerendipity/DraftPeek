package com.draftpeek.feature.stats.util

import com.draftpeek.feature.stats.model.AchievementCategory
import com.draftpeek.feature.stats.model.AchievementStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [AchievementProgress] 与 [AchievementDefinitions] 的**一致性反向验证**。
 *
 * 对每个成就断言：
 * - 数值化分类：`target-1` 未解锁、`target` 已解锁 —— 证明映射表的阈值与定义
 *   的 check 逐格一致（定义改阈值而映射未跟随 → 本测试红灯）；
 * - 非数值化分类：`targetOf == null`（UI 不展示进度条）。
 */
@DisplayName("成就进度映射一致性（防定义/映射两处漂移）")
class AchievementProgressConsistencyTest {

    private val numericCategories = setOf(
        AchievementCategory.READ,
        AchievementCategory.CREATE,
        AchievementCategory.DURATION,
        AchievementCategory.CHARS,
        AchievementCategory.STREAK,
        AchievementCategory.MILESTONE
    )

    @Test
    @DisplayName("逐成就反向验证：target-1 未解锁 / target 已解锁；非数值化分类为 null")
    fun mappingMatchesDefinitionsForEveryAchievement() {
        AchievementDefinitions.allAchievements.forEach { achievement ->
            val target = AchievementProgress.targetOf(achievement)
            if (achievement.category !in numericCategories) {
                assertNull(target, "${achievement.id}（${achievement.category}）应无数值化目标")
                return@forEach
            }
            assertNotNull(target, "${achievement.id}（${achievement.category} T${achievement.tier}）缺目标值")
            assertFalse(
                achievement.check(statsAt(achievement.category, target!! - 1)),
                "${achievement.id} 在 target-1=${target - 1} 时不应解锁（映射阈值与定义不一致？）"
            )
            assertTrue(
                achievement.check(statsAt(achievement.category, target)),
                "${achievement.id} 在 target=$target 时应解锁（映射阈值与定义不一致？）"
            )
        }
    }

    @Test
    @DisplayName("progressOf 读取对应统计字段（含 DURATION 的分钟→小时换算）")
    fun progressReadsMatchingFields() {
        val stats = AchievementStats(
            totalRead = 7,
            totalCreate = 3,
            totalMinutes = 150, // 2.5 小时 → 2
            totalChars = 12_345,
            maxStreak = 9,
            totalDays = 45
        )
        val byId = AchievementDefinitions.allAchievements.associateBy { it.id }

        assertEquals(7, AchievementProgress.progressOf(byId.getValue("read_1"), stats))
        assertEquals(3, AchievementProgress.progressOf(byId.getValue("create_1"), stats))
        assertEquals(2, AchievementProgress.progressOf(byId.getValue("duration_1"), stats))
        assertEquals(12_345, AchievementProgress.progressOf(byId.getValue("chars_1"), stats))
        assertEquals(9, AchievementProgress.progressOf(byId.getValue("streak_1"), stats))
        assertEquals(45, AchievementProgress.progressOf(byId.getValue("milestone_1"), stats))
    }

    private fun statsAt(category: AchievementCategory, value: Int): AchievementStats = when (category) {
        AchievementCategory.READ -> AchievementStats(totalRead = value)
        AchievementCategory.CREATE -> AchievementStats(totalCreate = value)
        AchievementCategory.DURATION -> AchievementStats(totalMinutes = value * 60L)
        AchievementCategory.CHARS -> AchievementStats(totalChars = value.toLong())
        AchievementCategory.STREAK -> AchievementStats(maxStreak = value)
        AchievementCategory.MILESTONE -> AchievementStats(totalDays = value)
        else -> error("非数值化分类不构造统计：$category")
    }
}
