/**
 * 文件: AchievementCalculator.kt
 * 功能: 统计模块工具类 - 成就统计计算器
 * 描述: 根据用户活动数据和最近文件记录，计算成就系统所需的各类统计指标。
 *       计算结果封装为 AchievementStats 对象，用于成就解锁条件判定。
 *
 * 计算逻辑说明：
 * 1. 基础统计: 累加总阅读数、总创建数、总使用时长、总书写字符数
 * 2. 连续天数: 从今天向前数计算 currentStreak，遍历所有日期计算 maxStreak
 * 3. 时段统计: 根据 RecentFile.lastOpenedAt 统计各时段（凌晨/清晨/上午/午后/下午/夜间）活跃天数
 * 4. 考勤统计: 计算完美周（周一至周日全活跃）、达标月（≥20天活跃）、完美月（全月每天活跃）
 * 5. 节假日检测: 检查各节假日当天是否有活动记录
 * 创建: 2024
 */
package com.draftpeek.feature.stats.util

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.feature.stats.model.AchievementStats
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 成就统计计算器对象。
 *
 * 提供静态方法 calculate()，根据活动数据和最近文件记录计算成就统计数据。
 */
object AchievementCalculator {

    /** ISO 日期格式化器 */
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** 系统默认时区 */
    private val zoneId = ZoneId.systemDefault()

    /**
     * 计算成就统计数据。
     *
     * @param activityMap 活动数据映射（日期字符串 -> UserActivity）
     * @param recentFiles 最近打开的文件列表，用于时段统计
     * @return 成就统计数据 AchievementStats 对象
     */
    fun calculate(
        activityMap: Map<String, UserActivity>,
        recentFiles: List<RecentFile> = emptyList()
    ): AchievementStats {
        val activeDates = activityMap.values
            .filter { it.hasActivity() }
            .mapNotNull { parseDate(it.date) }
            .toSortedSet()

        if (activeDates.isEmpty()) {
            return AchievementStats()
        }

        val totalRead = activityMap.values.sumOf { it.fileOpenCount }
        val totalCreate = activityMap.values.sumOf { it.fileCreateCount }
        val totalMinutes = activityMap.values.sumOf { it.usageDurationMinutes.toLong() }
        val totalChars = activityMap.values.sumOf { it.charWriteCount.toLong() }

        val firstDate = activeDates.first()
        val today = LocalDate.now(zoneId)
        val totalDays = ChronoUnit.DAYS.between(firstDate, today).toInt() + 1

        val (currentStreak, maxStreak) = calculateStreaks(activeDates, today)

        val timeSessions = calculateTimeSessions(activeDates, recentFiles)

        val perfectWeeks = calculatePerfectWeeks(activeDates, firstDate, today)
        val (perfectMonths, fullMonths) = calculateMonths(activeDates, firstDate, today)

        val holidays = calculateHolidays(activeDates)

        return AchievementStats(
            totalRead = totalRead,
            totalCreate = totalCreate,
            totalMinutes = totalMinutes,
            totalChars = totalChars,
            totalDays = totalDays,
            maxStreak = maxStreak,
            currentStreak = currentStreak,
            midnightSessions = timeSessions.midnight,
            morningSessions = timeSessions.morning,
            forenoonSessions = timeSessions.forenoon,
            noonSessions = timeSessions.noon,
            afternoonSessions = timeSessions.afternoon,
            nightSessions = timeSessions.night,
            perfectWeeks = perfectWeeks,
            perfectMonths = perfectMonths,
            fullMonths = fullMonths,
            holidayNewYear = holidays.newYear,
            holidayNewYearEve = holidays.newYearEve,
            holidaySpring = holidays.spring,
            holidayLantern = holidays.lantern,
            holidayLabor = holidays.labor,
            holidayDragon = holidays.dragon,
            holidayQixi = holidays.qixi,
            holidayMidAutumn = holidays.midAutumn,
            holidayNational = holidays.national,
            holidayChristmas = holidays.christmas
        )
    }

    /**
     * 解析日期字符串。
     */
    private fun parseDate(dateStr: String): LocalDate? = try {
        LocalDate.parse(dateStr, dateFormatter)
    } catch (e: Exception) {
        null
    }

    /**
     * 计算连续使用天数。
     *
     * - currentStreak: 从今天开始向前数，直到遇到无活动日期为止
     * - maxStreak: 遍历所有日期，找出最长的连续活跃序列
     *
     * @param activeDates 活跃日期集合（已排序）
     * @param today 当前日期
     * @return Pair(当前连续天数, 最长连续天数)
     */
    private fun calculateStreaks(activeDates: Set<LocalDate>, today: LocalDate): Pair<Int, Int> {
        val sortedDates = activeDates.toSortedSet()

        var currentStreak = 0
        var date = today
        while (sortedDates.contains(date)) {
            currentStreak++
            date = date.minusDays(1)
        }

        var maxStreak = 0
        var currentRun = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate != null && ChronoUnit.DAYS.between(prevDate, d) == 1L) {
                currentRun++
            } else {
                currentRun = 1
            }
            maxStreak = maxOf(maxStreak, currentRun)
            prevDate = d
        }

        return currentStreak to maxStreak
    }

    /**
     * 各时段活跃天数数据类。
     */
    private data class TimeSessions(
        val midnight: Int,
        val morning: Int,
        val forenoon: Int,
        val noon: Int,
        val afternoon: Int,
        val night: Int
    )

    /**
     * 计算各时段活跃天数。
     *
     * 时段划分：
     * - 凌晨: 00:00-05:59
     * - 清晨: 06:00-08:59
     * - 上午: 09:00-11:59
     * - 午后: 12:00-13:59
     * - 下午: 14:00-17:59
     * - 夜间: 18:00-23:59
     *
     * 根据 RecentFile.lastOpenedAt 时间戳统计每个活跃日期在哪些时段有活动。
     *
     * @param activeDates 活跃日期集合
     * @param recentFiles 最近文件列表
     * @return 各时段活跃天数
     */
    private fun calculateTimeSessions(activeDates: Set<LocalDate>, recentFiles: List<RecentFile>): TimeSessions {
        if (recentFiles.isEmpty()) {
            return TimeSessions(0, 0, 0, 0, 0, 0)
        }

        val dateToHours = mutableMapOf<LocalDate, MutableSet<Int>>()

        for (file in recentFiles) {
            try {
                val instant = Instant.ofEpochMilli(file.lastOpenedAt)
                val dateTime = instant.atZone(zoneId)
                val date = dateTime.toLocalDate()
                val hour = dateTime.hour
                dateToHours.getOrPut(date) { mutableSetOf() }.add(hour)
            } catch (e: Exception) {
                continue
            }
        }

        var midnight = 0
        var morning = 0
        var forenoon = 0
        var noon = 0
        var afternoon = 0
        var night = 0

        for ((date, hours) in dateToHours) {
            if (!activeDates.contains(date)) continue

            if (hours.any { it in 0..5 }) midnight++
            if (hours.any { it in 6..8 }) morning++
            if (hours.any { it in 9..11 }) forenoon++
            if (hours.any { it in 12..13 }) noon++
            if (hours.any { it in 14..17 }) afternoon++
            if (hours.any { it in 18..23 }) night++
        }

        return TimeSessions(midnight, morning, forenoon, noon, afternoon, night)
    }

    /**
     * 计算完美周数量。
     *
     * 完美周定义：周一至周日连续 7 天每天都有活动。
     * 不完整的当前周（尚未结束）不计入。
     *
     * @param activeDates 活跃日期集合
     * @param firstDate 首次活跃日期
     * @param today 当前日期
     * @return 完美周数量
     */
    private fun calculatePerfectWeeks(activeDates: Set<LocalDate>, firstDate: LocalDate, today: LocalDate): Int {
        var count = 0
        var current = firstDate.with(java.time.DayOfWeek.MONDAY)

        while (!current.isAfter(today)) {
            val weekEnd = current.plusDays(6)
            if (!weekEnd.isAfter(today)) {
                var allActive = true
                var d = current
                while (!d.isAfter(weekEnd)) {
                    if (!activeDates.contains(d)) {
                        allActive = false
                        break
                    }
                    d = d.plusDays(1)
                }
                if (allActive) count++
            }
            current = current.plusWeeks(1)
        }

        return count
    }

    /**
     * 计算达标月和完美月数量。
     *
     * - 达标月 (perfectMonths): 单月活跃天数 ≥ 20 天
     * - 完美月 (fullMonths): 整月已结束且该月每一天都有活动
     *
     * @param activeDates 活跃日期集合
     * @param firstDate 首次活跃日期
     * @param today 当前日期
     * @return Pair(达标月数量, 完美月数量)
     */
    private fun calculateMonths(activeDates: Set<LocalDate>, firstDate: LocalDate, today: LocalDate): Pair<Int, Int> {
        var perfectMonths = 0
        var fullMonths = 0

        var currentMonth = YearMonth.from(firstDate)
        val endMonth = YearMonth.from(today)

        while (!currentMonth.isAfter(endMonth)) {
            val monthStart = currentMonth.atDay(1)
            val monthEnd = currentMonth.atEndOfMonth()
            val effectiveEnd = if (monthEnd.isAfter(today)) today else monthEnd

            var activeDaysInMonth = 0
            var d = monthStart
            while (!d.isAfter(effectiveEnd)) {
                if (activeDates.contains(d)) activeDaysInMonth++
                d = d.plusDays(1)
            }

            val daysInMonth = ChronoUnit.DAYS.between(monthStart, effectiveEnd).toInt() + 1
            val isFullMonth = monthEnd.isBefore(today) || monthEnd == today

            if (activeDaysInMonth >= 20) perfectMonths++
            if (isFullMonth && activeDaysInMonth == currentMonth.lengthOfMonth()) fullMonths++

            currentMonth = currentMonth.plusMonths(1)
        }

        return perfectMonths to fullMonths
    }

    /**
     * 节假日活动检测结果数据类。
     */
    private data class Holidays(
        val newYear: Boolean,
        val newYearEve: Boolean,
        val spring: Boolean,
        val lantern: Boolean,
        val labor: Boolean,
        val dragon: Boolean,
        val qixi: Boolean,
        val midAutumn: Boolean,
        val national: Boolean,
        val christmas: Boolean
    )

    /**
     * 检测各节假日是否有活动记录。
     *
     * 检查活动记录的所有年份中，各固定日期节假日是否有活动。
     * 注意：春节、除夕等农历节日暂使用固定公历日期或未实现。
     *
     * @param activeDates 活跃日期集合
     * @return 各节假日活动状态
     */
    private fun calculateHolidays(activeDates: Set<LocalDate>): Holidays {
        val years = activeDates.map { it.year }.toSet()

        fun hasHoliday(month: Int, day: Int): Boolean = years.any { year ->
            try {
                activeDates.contains(LocalDate.of(year, month, day))
            } catch (e: Exception) {
                false
            }
        }

        return Holidays(
            newYear = hasHoliday(1, 1),
            newYearEve = false,
            spring = false,
            lantern = hasHoliday(2, 15),
            labor = hasHoliday(5, 1),
            dragon = hasHoliday(6, 5),
            qixi = hasHoliday(8, 7),
            midAutumn = hasHoliday(9, 15),
            national = hasHoliday(10, 1),
            christmas = hasHoliday(12, 25)
        )
    }
}
