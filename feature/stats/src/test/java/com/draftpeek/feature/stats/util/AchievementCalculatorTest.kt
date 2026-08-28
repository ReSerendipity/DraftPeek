package com.draftpeek.feature.stats.util

import com.draftpeek.core.data.entity.UserActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AchievementCalculator")
class AchievementCalculatorTest {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun activityFor(date: LocalDate, fileOpen: Int = 1): UserActivity = UserActivity(
        date = date.format(dateFormatter),
        fileOpenCount = fileOpen
    )

    @Nested
    @DisplayName("calculate() — empty input")
    inner class EmptyInputTests {

        @Test
        @DisplayName("空活动数据返回默认 AchievementStats")
        fun emptyMap_returnsDefaultStats() {
            val stats = AchievementCalculator.calculate(emptyMap())
            assertEquals(0, stats.totalRead)
            assertEquals(0, stats.totalCreate)
            assertEquals(0, stats.totalMinutes)
            assertEquals(0, stats.currentStreak)
            assertEquals(0, stats.maxStreak)
        }

        @Test
        @DisplayName("无活动的记录被忽略")
        fun noActivityEntries_ignored() {
            val today = LocalDate.now()
            val activity = UserActivity(date = today.format(dateFormatter), fileOpenCount = 0)
            val stats = AchievementCalculator.calculate(mapOf(activity.date to activity))
            assertEquals(0, stats.totalRead)
        }
    }

    @Nested
    @DisplayName("calculate() — basic aggregation")
    inner class BasicAggregationTests {

        @Test
        @DisplayName("totalRead 累加所有 fileOpenCount")
        fun totalRead_sumsFileOpenCounts() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val map = mapOf(
                today.format(dateFormatter) to activityFor(today, fileOpen = 5),
                yesterday.format(dateFormatter) to activityFor(yesterday, fileOpen = 3)
            )

            val stats = AchievementCalculator.calculate(map)
            assertEquals(8, stats.totalRead)
        }

        @Test
        @DisplayName("totalCreate 累加所有 fileCreateCount")
        fun totalCreate_sumsFileCreateCounts() {
            val today = LocalDate.now()
            val activity = UserActivity(
                date = today.format(dateFormatter),
                fileOpenCount = 1,
                fileCreateCount = 4
            )

            val stats = AchievementCalculator.calculate(mapOf(activity.date to activity))
            assertEquals(4, stats.totalCreate)
        }

        @Test
        @DisplayName("totalMinutes 累加所有 usageDurationMinutes")
        fun totalMinutes_sumsUsageDurations() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val map = mapOf(
                today.format(dateFormatter) to UserActivity(
                    date = today.format(dateFormatter),
                    fileOpenCount = 1,
                    usageDurationMinutes = 30
                ),
                yesterday.format(dateFormatter) to UserActivity(
                    date = yesterday.format(dateFormatter),
                    fileOpenCount = 1,
                    usageDurationMinutes = 60
                )
            )

            val stats = AchievementCalculator.calculate(map)
            assertEquals(90L, stats.totalMinutes)
        }

        @Test
        @DisplayName("totalChars 累加所有 charWriteCount")
        fun totalChars_sumsCharWriteCounts() {
            val today = LocalDate.now()
            val activity = UserActivity(
                date = today.format(dateFormatter),
                fileOpenCount = 1,
                charWriteCount = 500
            )

            val stats = AchievementCalculator.calculate(mapOf(activity.date to activity))
            assertEquals(500L, stats.totalChars)
        }
    }

    @Nested
    @DisplayName("calculate() — streaks")
    inner class StreakTests {

        @Test
        @DisplayName("仅今天有活动时 currentStreak = 1")
        fun onlyToday_streakIs1() {
            val today = LocalDate.now()
            val activity = activityFor(today)

            val stats = AchievementCalculator.calculate(mapOf(activity.date to activity))
            assertEquals(1, stats.currentStreak)
            assertEquals(1, stats.maxStreak)
        }

        @Test
        @DisplayName("连续3天有活动时 currentStreak = 3")
        fun threeDayStreak() {
            val today = LocalDate.now()
            val map = (0..2).associate {
                val date = today.minusDays(it.toLong())
                date.format(dateFormatter) to activityFor(date)
            }

            val stats = AchievementCalculator.calculate(map)
            assertEquals(3, stats.currentStreak)
            assertEquals(3, stats.maxStreak)
        }
    }
}
