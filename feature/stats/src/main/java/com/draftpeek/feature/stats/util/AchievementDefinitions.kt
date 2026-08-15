/**
 * 文件: AchievementDefinitions.kt
 * 功能: 统计模块工具类 - 成就定义列表
 * 描述: 定义应用中所有成就项的静态列表，包含 9 大分类共 70+ 个成就。
 *       每个成就包含唯一 ID、分类、等级、中文名称、描述、图标和解锁条件 lambda。
 *
 * 成就分类：
 * - READ: 阅读文件 (5级: 10/50/200/500/1000 个)
 * - CREATE: 创建文件 (5级: 5/20/50/200/500 个)
 * - DURATION: 使用时长 (5级: 1/10/50/200/500 小时)
 * - CHARS: 书写字符 (5级: 1k/10k/100k/500k/1M 字)
 * - STREAK: 连续天数 (7级: 3/7/14/30/60/100/365 天)
 * - TIME_PERIOD: 时间段活跃 (6个时段，各2-4级不等)
 * - ATTENDANCE: 考勤 (周勤4级/月勤4级/完美月4级)
 * - MILESTONE: 里程碑 (6级: 30/90/180/365/730/1095 天)
 * - HOLIDAY: 节假日 (10个节假日成就)
 * 创建: 2024
 */
package com.draftpeek.feature.stats.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HourglassFull
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import com.draftpeek.feature.stats.model.Achievement
import com.draftpeek.feature.stats.model.AchievementCategory

/**
 * 成就定义对象。
 *
 * 包含 allAchievements 静态列表，定义了应用中所有可用成就。
 */
object AchievementDefinitions {

    /**
     * 所有成就的完整列表。
     *
     * 按分类组织，每个成就包含唯一 ID、分类、等级、名称、描述、图标和解锁条件检查函数。
     */
    val allAchievements: List<Achievement> = listOf(
        // 阅读文件
        Achievement(
            id = "read_1",
            category = AchievementCategory.READ,
            tier = 1,
            name = "初窥门径",
            description = "阅读文件10个",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            check = { it.totalRead >= 10 }
        ),
        Achievement(
            id = "read_2",
            category = AchievementCategory.READ,
            tier = 2,
            name = "书海拾贝",
            description = "阅读文件50个",
            icon = Icons.Filled.AutoStories,
            check = { it.totalRead >= 50 }
        ),
        Achievement(
            id = "read_3",
            category = AchievementCategory.READ,
            tier = 3,
            name = "博览群书",
            description = "阅读文件200个",
            icon = Icons.Filled.Book,
            check = { it.totalRead >= 200 }
        ),
        Achievement(
            id = "read_4",
            category = AchievementCategory.READ,
            tier = 4,
            name = "学富五车",
            description = "阅读文件500个",
            icon = Icons.AutoMirrored.Filled.LibraryBooks,
            check = { it.totalRead >= 500 }
        ),
        Achievement(
            id = "read_5",
            category = AchievementCategory.READ,
            tier = 5,
            name = "汗牛充栋",
            description = "阅读文件1000个",
            icon = Icons.Filled.CollectionsBookmark,
            check = { it.totalRead >= 1000 }
        ),

        // 创建文件
        Achievement(
            id = "create_1",
            category = AchievementCategory.CREATE,
            tier = 1,
            name = "初出茅庐",
            description = "创建文件5个",
            icon = Icons.AutoMirrored.Filled.NoteAdd,
            check = { it.totalCreate >= 5 }
        ),
        Achievement(
            id = "create_2",
            category = AchievementCategory.CREATE,
            tier = 2,
            name = "笔耕不辍",
            description = "创建文件20个",
            icon = Icons.Filled.PostAdd,
            check = { it.totalCreate >= 20 }
        ),
        Achievement(
            id = "create_3",
            category = AchievementCategory.CREATE,
            tier = 3,
            name = "著作等身",
            description = "创建文件50个",
            icon = Icons.Filled.CreateNewFolder,
            check = { it.totalCreate >= 50 }
        ),
        Achievement(
            id = "create_4",
            category = AchievementCategory.CREATE,
            tier = 4,
            name = "著书立说",
            description = "创建文件200个",
            icon = Icons.Filled.AddBox,
            check = { it.totalCreate >= 200 }
        ),
        Achievement(
            id = "create_5",
            category = AchievementCategory.CREATE,
            tier = 5,
            name = "开宗立派",
            description = "创建文件500个",
            icon = Icons.Filled.EmojiEvents,
            check = { it.totalCreate >= 500 }
        ),

        // 使用时长（小时转分钟）
        Achievement(
            id = "duration_1",
            category = AchievementCategory.DURATION,
            tier = 1,
            name = "浅尝辄止",
            description = "使用时长1小时",
            icon = Icons.Filled.Schedule,
            check = { it.totalMinutes >= 60 }
        ),
        Achievement(
            id = "duration_2",
            category = AchievementCategory.DURATION,
            tier = 2,
            name = "渐入佳境",
            description = "使用时长10小时",
            icon = Icons.Filled.Timelapse,
            check = { it.totalMinutes >= 600 }
        ),
        Achievement(
            id = "duration_3",
            category = AchievementCategory.DURATION,
            tier = 3,
            name = "如鱼得水",
            description = "使用时长50小时",
            icon = Icons.Filled.HourglassEmpty,
            check = { it.totalMinutes >= 3000 }
        ),
        Achievement(
            id = "duration_4",
            category = AchievementCategory.DURATION,
            tier = 4,
            name = "炉火纯青",
            description = "使用时长200小时",
            icon = Icons.Filled.HourglassFull,
            check = { it.totalMinutes >= 12000 }
        ),
        Achievement(
            id = "duration_5",
            category = AchievementCategory.DURATION,
            tier = 5,
            name = "登峰造极",
            description = "使用时长500小时",
            icon = Icons.Filled.Timer,
            check = { it.totalMinutes >= 30000 }
        ),

        // 书写字符
        Achievement(
            id = "chars_1",
            category = AchievementCategory.CHARS,
            tier = 1,
            name = "初试锋芒",
            description = "书写字符1000个",
            icon = Icons.Filled.Edit,
            check = { it.totalChars >= 1000 }
        ),
        Achievement(
            id = "chars_2",
            category = AchievementCategory.CHARS,
            tier = 2,
            name = "妙笔生花",
            description = "书写字符10000个",
            icon = Icons.Filled.EditNote,
            check = { it.totalChars >= 10000 }
        ),
        Achievement(
            id = "chars_3",
            category = AchievementCategory.CHARS,
            tier = 3,
            name = "下笔千言",
            description = "书写字符100000个",
            icon = Icons.Filled.TextSnippet,
            check = { it.totalChars >= 100000 }
        ),
        Achievement(
            id = "chars_4",
            category = AchievementCategory.CHARS,
            tier = 4,
            name = "笔走龙蛇",
            description = "书写字符500000个",
            icon = Icons.Filled.Article,
            check = { it.totalChars >= 500000 }
        ),
        Achievement(
            id = "chars_5",
            category = AchievementCategory.CHARS,
            tier = 5,
            name = "万古流芳",
            description = "书写字符1000000个",
            icon = Icons.AutoMirrored.Filled.Notes,
            check = { it.totalChars >= 1000000 }
        ),

        // 连续天数
        Achievement(
            id = "streak_1",
            category = AchievementCategory.STREAK,
            tier = 1,
            name = "薪火初传",
            description = "连续使用3天",
            icon = Icons.Filled.LocalFireDepartment,
            check = { it.maxStreak >= 3 }
        ),
        Achievement(
            id = "streak_2",
            category = AchievementCategory.STREAK,
            tier = 2,
            name = "星星之火",
            description = "连续使用7天",
            icon = Icons.Filled.Whatshot,
            check = { it.maxStreak >= 7 }
        ),
        Achievement(
            id = "streak_3",
            category = AchievementCategory.STREAK,
            tier = 3,
            name = "熊熊烈焰",
            description = "连续使用14天",
            icon = Icons.Filled.Flare,
            check = { it.maxStreak >= 14 }
        ),
        Achievement(
            id = "streak_4",
            category = AchievementCategory.STREAK,
            tier = 4,
            name = "烈火真金",
            description = "连续使用30天",
            icon = Icons.Filled.Bolt,
            check = { it.maxStreak >= 30 }
        ),
        Achievement(
            id = "streak_5",
            category = AchievementCategory.STREAK,
            tier = 5,
            name = "浴火重生",
            description = "连续使用60天",
            icon = Icons.Filled.AutoGraph,
            check = { it.maxStreak >= 60 }
        ),
        Achievement(
            id = "streak_6",
            category = AchievementCategory.STREAK,
            tier = 6,
            name = "凤凰涅槃",
            description = "连续使用100天",
            icon = Icons.Filled.MilitaryTech,
            check = { it.maxStreak >= 100 }
        ),
        Achievement(
            id = "streak_7",
            category = AchievementCategory.STREAK,
            tier = 7,
            name = "天长地久",
            description = "连续使用365天",
            icon = Icons.Filled.WorkspacePremium,
            check = { it.maxStreak >= 365 }
        ),

        // 时间段 - 凌晨 (00-06)
        Achievement(
            id = "midnight_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "月落乌啼",
            description = "凌晨活跃10天",
            icon = Icons.Filled.DarkMode,
            check = { it.midnightSessions >= 10 }
        ),
        Achievement(
            id = "midnight_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "夜阑人静",
            description = "凌晨活跃30天",
            icon = Icons.Filled.Nightlight,
            check = { it.midnightSessions >= 30 }
        ),
        Achievement(
            id = "midnight_3",
            category = AchievementCategory.TIME_PERIOD,
            tier = 3,
            name = "挑灯夜战",
            description = "凌晨活跃60天",
            icon = Icons.Filled.Bedtime,
            check = { it.midnightSessions >= 60 }
        ),

        // 时间段 - 清晨 (06-09)
        Achievement(
            id = "morning_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "闻鸡起舞",
            description = "清晨活跃5天",
            icon = Icons.Filled.WbSunny,
            check = { it.morningSessions >= 5 }
        ),
        Achievement(
            id = "morning_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "晨钟暮鼓",
            description = "清晨活跃20天",
            icon = Icons.Filled.LightMode,
            check = { it.morningSessions >= 20 }
        ),
        Achievement(
            id = "morning_3",
            category = AchievementCategory.TIME_PERIOD,
            tier = 3,
            name = "鸡鸣而起",
            description = "清晨活跃50天",
            icon = Icons.Filled.WbTwilight,
            check = { it.morningSessions >= 50 }
        ),
        Achievement(
            id = "morning_4",
            category = AchievementCategory.TIME_PERIOD,
            tier = 4,
            name = "披星戴月",
            description = "清晨活跃100天",
            icon = Icons.Filled.Brightness2,
            check = { it.morningSessions >= 100 }
        ),

        // 时间段 - 上午 (09-12)
        Achievement(
            id = "forenoon_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "日上三竿",
            description = "上午活跃50天",
            icon = Icons.Filled.WbTwilight,
            check = { it.forenoonSessions >= 50 }
        ),
        Achievement(
            id = "forenoon_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "废寝忘食",
            description = "上午活跃200天",
            icon = Icons.Filled.WbSunny,
            check = { it.forenoonSessions >= 200 }
        ),

        // 时间段 - 午后 (12-14)
        Achievement(
            id = "noon_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "午后清风",
            description = "午后活跃30天",
            icon = Icons.Filled.Coffee,
            check = { it.noonSessions >= 30 }
        ),
        Achievement(
            id = "noon_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "日暖风和",
            description = "午后活跃100天",
            icon = Icons.Filled.LocalCafe,
            check = { it.noonSessions >= 100 }
        ),

        // 时间段 - 下午 (14-18)
        Achievement(
            id = "afternoon_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "日昃忘疲",
            description = "下午活跃50天",
            icon = Icons.Filled.LocalCafe,
            check = { it.afternoonSessions >= 50 }
        ),
        Achievement(
            id = "afternoon_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "夕食未央",
            description = "下午活跃200天",
            icon = Icons.Filled.EmojiFoodBeverage,
            check = { it.afternoonSessions >= 200 }
        ),

        // 时间段 - 夜间 (18-00)
        Achievement(
            id = "night_1",
            category = AchievementCategory.TIME_PERIOD,
            tier = 1,
            name = "夜以继日",
            description = "夜间活跃5天",
            icon = Icons.Filled.Brightness2,
            check = { it.nightSessions >= 5 }
        ),
        Achievement(
            id = "night_2",
            category = AchievementCategory.TIME_PERIOD,
            tier = 2,
            name = "焚膏继晷",
            description = "夜间活跃20天",
            icon = Icons.Filled.Nightlight,
            check = { it.nightSessions >= 20 }
        ),
        Achievement(
            id = "night_3",
            category = AchievementCategory.TIME_PERIOD,
            tier = 3,
            name = "秉烛夜游",
            description = "夜间活跃50天",
            icon = Icons.Filled.Bedtime,
            check = { it.nightSessions >= 50 }
        ),
        Achievement(
            id = "night_4",
            category = AchievementCategory.TIME_PERIOD,
            tier = 4,
            name = "通宵达旦",
            description = "夜间活跃100天",
            icon = Icons.Filled.DarkMode,
            check = { it.nightSessions >= 100 }
        ),

        // 考勤 - 周勤（完美周）
        Achievement(
            id = "week_1",
            category = AchievementCategory.ATTENDANCE,
            tier = 1,
            name = "一周之计",
            description = "完整周打卡1周",
            icon = Icons.Filled.CalendarViewWeek,
            check = { it.perfectWeeks >= 1 }
        ),
        Achievement(
            id = "week_2",
            category = AchievementCategory.ATTENDANCE,
            tier = 2,
            name = "四季如一",
            description = "完整周打卡4周",
            icon = Icons.Filled.CalendarMonth,
            check = { it.perfectWeeks >= 4 }
        ),
        Achievement(
            id = "week_3",
            category = AchievementCategory.ATTENDANCE,
            tier = 3,
            name = "持之以恒",
            description = "完整周打卡12周",
            icon = Icons.Filled.EventRepeat,
            check = { it.perfectWeeks >= 12 }
        ),
        Achievement(
            id = "week_4",
            category = AchievementCategory.ATTENDANCE,
            tier = 4,
            name = "岁月如歌",
            description = "完整周打卡48周",
            icon = Icons.Filled.EventAvailable,
            check = { it.perfectWeeks >= 48 }
        ),

        // 考勤 - 月勤（达标月：单月活跃≥20天）
        Achievement(
            id = "month_1",
            category = AchievementCategory.ATTENDANCE,
            tier = 1,
            name = "月度之星",
            description = "达标月1个月",
            icon = Icons.Filled.CalendarViewMonth,
            check = { it.perfectMonths >= 1 }
        ),
        Achievement(
            id = "month_2",
            category = AchievementCategory.ATTENDANCE,
            tier = 2,
            name = "三月如磐",
            description = "达标月3个月",
            icon = Icons.Filled.CalendarViewWeek,
            check = { it.perfectMonths >= 3 }
        ),
        Achievement(
            id = "month_3",
            category = AchievementCategory.ATTENDANCE,
            tier = 3,
            name = "半载光阴",
            description = "达标月6个月",
            icon = Icons.Filled.CalendarMonth,
            check = { it.perfectMonths >= 6 }
        ),
        Achievement(
            id = "month_4",
            category = AchievementCategory.ATTENDANCE,
            tier = 4,
            name = "年年如一",
            description = "达标月12个月",
            icon = Icons.Filled.EventAvailable,
            check = { it.perfectMonths >= 12 }
        ),

        // 考勤 - 完美月（全月每天活跃）
        Achievement(
            id = "fullmonth_1",
            category = AchievementCategory.ATTENDANCE,
            tier = 1,
            name = "璧月无瑕",
            description = "完美月1个月",
            icon = Icons.Filled.GpsFixed,
            check = { it.fullMonths >= 1 }
        ),
        Achievement(
            id = "fullmonth_2",
            category = AchievementCategory.ATTENDANCE,
            tier = 2,
            name = "季季皆圆",
            description = "完美月3个月",
            icon = Icons.Filled.CalendarViewMonth,
            check = { it.fullMonths >= 3 }
        ),
        Achievement(
            id = "fullmonth_3",
            category = AchievementCategory.ATTENDANCE,
            tier = 3,
            name = "半壁河山",
            description = "完美月6个月",
            icon = Icons.Filled.CalendarViewWeek,
            check = { it.fullMonths >= 6 }
        ),
        Achievement(
            id = "fullmonth_4",
            category = AchievementCategory.ATTENDANCE,
            tier = 4,
            name = "十全十美",
            description = "完美月12个月",
            icon = Icons.Filled.CalendarMonth,
            check = { it.fullMonths >= 12 }
        ),

        // 里程碑（总使用天数）
        Achievement(
            id = "milestone_1",
            category = AchievementCategory.MILESTONE,
            tier = 1,
            name = "一月有成",
            description = "使用满30天",
            icon = Icons.Filled.Grade,
            check = { it.totalDays >= 30 }
        ),
        Achievement(
            id = "milestone_2",
            category = AchievementCategory.MILESTONE,
            tier = 2,
            name = "季度之约",
            description = "使用满90天",
            icon = Icons.Filled.Stars,
            check = { it.totalDays >= 90 }
        ),
        Achievement(
            id = "milestone_3",
            category = AchievementCategory.MILESTONE,
            tier = 3,
            name = "半载光阴",
            description = "使用满180天",
            icon = Icons.Filled.Diamond,
            check = { it.totalDays >= 180 }
        ),
        Achievement(
            id = "milestone_4",
            category = AchievementCategory.MILESTONE,
            tier = 4,
            name = "一年之期",
            description = "使用满365天",
            icon = Icons.Filled.MilitaryTech,
            check = { it.totalDays >= 365 }
        ),
        Achievement(
            id = "milestone_5",
            category = AchievementCategory.MILESTONE,
            tier = 5,
            name = "两载春秋",
            description = "使用满730天",
            icon = Icons.Filled.WorkspacePremium,
            check = { it.totalDays >= 730 }
        ),
        Achievement(
            id = "milestone_6",
            category = AchievementCategory.MILESTONE,
            tier = 6,
            name = "三载风雨",
            description = "使用满1095天",
            icon = Icons.Filled.EmojiEvents,
            check = { it.totalDays >= 1095 }
        ),

        // 节假日
        Achievement(
            id = "holiday_newyear",
            category = AchievementCategory.HOLIDAY,
            tier = 1,
            name = "岁序更新",
            description = "元旦当天有活动",
            icon = Icons.Filled.CardGiftcard,
            check = { it.holidayNewYear }
        ),
        Achievement(
            id = "holiday_newyeareve",
            category = AchievementCategory.HOLIDAY,
            tier = 2,
            name = "除旧布新",
            description = "除夕当天有活动",
            icon = Icons.Filled.Celebration,
            check = { it.holidayNewYearEve }
        ),
        Achievement(
            id = "holiday_spring",
            category = AchievementCategory.HOLIDAY,
            tier = 3,
            name = "春回大地",
            description = "春节当天有活动",
            icon = Icons.Filled.Cake,
            check = { it.holidaySpring }
        ),
        Achievement(
            id = "holiday_lantern",
            category = AchievementCategory.HOLIDAY,
            tier = 4,
            name = "花好月圆",
            description = "元宵节当天有活动",
            icon = Icons.Filled.Redeem,
            check = { it.holidayLantern }
        ),
        Achievement(
            id = "holiday_labor",
            category = AchievementCategory.HOLIDAY,
            tier = 5,
            name = "春华秋实",
            description = "劳动节当天有活动",
            icon = Icons.Filled.EmojiEvents,
            check = { it.holidayLabor }
        ),
        Achievement(
            id = "holiday_dragon",
            category = AchievementCategory.HOLIDAY,
            tier = 6,
            name = "龙舟竞渡",
            description = "端午节当天有活动",
            icon = Icons.Filled.Celebration,
            check = { it.holidayDragon }
        ),
        Achievement(
            id = "holiday_qixi",
            category = AchievementCategory.HOLIDAY,
            tier = 7,
            name = "金风玉露",
            description = "七夕节当天有活动",
            icon = Icons.Filled.Cake,
            check = { it.holidayQixi }
        ),
        Achievement(
            id = "holiday_midautumn",
            category = AchievementCategory.HOLIDAY,
            tier = 8,
            name = "皓月当空",
            description = "中秋节当天有活动",
            icon = Icons.Filled.Redeem,
            check = { it.holidayMidAutumn }
        ),
        Achievement(
            id = "holiday_national",
            category = AchievementCategory.HOLIDAY,
            tier = 9,
            name = "山河锦绣",
            description = "国庆节当天有活动",
            icon = Icons.Filled.EmojiEvents,
            check = { it.holidayNational }
        ),
        Achievement(
            id = "holiday_christmas",
            category = AchievementCategory.HOLIDAY,
            tier = 10,
            name = "铃响平安",
            description = "圣诞节当天有活动",
            icon = Icons.Filled.CardGiftcard,
            check = { it.holidayChristmas }
        )
    )
}
