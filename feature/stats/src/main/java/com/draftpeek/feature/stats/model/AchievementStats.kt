/**
 * 文件: AchievementStats.kt
 * 功能: 统计模块数据模型 - 成就统计数据类
 * 描述: 定义用于成就解锁判定的统计数据聚合类，包含各类成就所需的统计指标
 * 创建: 2024
 */
package com.draftpeek.feature.stats.model

/**
 * 成就统计数据类。
 *
 * 聚合所有成就判定所需的统计指标，由 AchievementCalculator 计算生成，
 * 作为成就解锁条件检查函数 (Achievement.check) 的输入参数。
 *
 * @property totalRead 累计打开/阅读文件总数
 * @property totalCreate 累计创建文件总数
 * @property totalMinutes 累计使用时长（分钟）
 * @property totalChars 累计书写字符总数
 * @property totalDays 累计使用总天数（从首次使用到今天）
 * @property maxStreak 历史最长连续使用天数
 * @property currentStreak 当前连续使用天数
 * @property midnightSessions 凌晨（00:00-05:59）活跃天数
 * @property morningSessions 清晨（06:00-08:59）活跃天数
 * @property forenoonSessions 上午（09:00-11:59）活跃天数
 * @property noonSessions 午后（12:00-13:59）活跃天数
 * @property afternoonSessions 下午（14:00-17:59）活跃天数
 * @property nightSessions 夜间（18:00-23:59）活跃天数
 * @property perfectWeeks 完美周数量（周一至周日每天都活跃）
 * @property perfectMonths 达标月数量（单月活跃天数 ≥ 20 天）
 * @property fullMonths 完美月数量（整月每天都活跃且月份已结束）
 * @property holidayNewYear 元旦（1月1日）是否有活动
 * @property holidayNewYearEve 除夕是否有活动（暂未实现）
 * @property holidaySpring 春节是否有活动（暂未实现）
 * @property holidayLantern 元宵节（2月15日）是否有活动
 * @property holidayLabor 劳动节（5月1日）是否有活动
 * @property holidayDragon 端午节（6月5日）是否有活动
 * @property holidayQixi 七夕节（8月7日）是否有活动
 * @property holidayMidAutumn 中秋节（9月15日）是否有活动
 * @property holidayNational 国庆节（10月1日）是否有活动
 * @property holidayChristmas 圣诞节（12月25日）是否有活动
 */
data class AchievementStats(
    val totalRead: Int = 0,
    val totalCreate: Int = 0,
    val totalMinutes: Long = 0L,
    val totalChars: Long = 0L,
    val totalDays: Int = 0,
    val maxStreak: Int = 0,
    val currentStreak: Int = 0,
    val midnightSessions: Int = 0,
    val morningSessions: Int = 0,
    val forenoonSessions: Int = 0,
    val noonSessions: Int = 0,
    val afternoonSessions: Int = 0,
    val nightSessions: Int = 0,
    val perfectWeeks: Int = 0,
    val perfectMonths: Int = 0,
    val fullMonths: Int = 0,
    val holidayNewYear: Boolean = false,
    val holidayNewYearEve: Boolean = false,
    val holidaySpring: Boolean = false,
    val holidayLantern: Boolean = false,
    val holidayLabor: Boolean = false,
    val holidayDragon: Boolean = false,
    val holidayQixi: Boolean = false,
    val holidayMidAutumn: Boolean = false,
    val holidayNational: Boolean = false,
    val holidayChristmas: Boolean = false
)
