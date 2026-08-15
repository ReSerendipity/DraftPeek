@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

/**
 * 文件: StatsViewModel.kt
 * 功能: 统计模块 ViewModel - 统计数据管理与业务逻辑
 * 描述: 负责统计页面的数据加载、计算和状态管理。核心功能包括：
 *       1. 从 UserActivityRepository 和 RecentFilesRepository 加载用户活动数据
 *       2. 按选定时间周期（摘要/年/月/周/今日/昨日）计算统计指标
 *       3. 计算连续使用天数（currentStreak、maxStreak）
 *       4. 计算成就解锁状态
 *       5. 提供单日详情查询功能
 *       6. 管理热力图颜色主题设置
 *       7. 处理缓存清理等操作
 *
 * 统计数据计算逻辑说明：
 * - 周期统计 (calculatePeriodStats): 根据选定的 StatsPeriod 确定日期范围，遍历活动数据累加使用时长、书写字符数、文件打开/创建数
 * - 连续天数: 当前连续天数从今天向前数，遇到无活动日期停止；最长连续天数遍历所有日期计算最长连续序列
 * - 单日详情 (getDayDetail): 根据活动时长和操作类型生成智能摘要文案，识别活跃时段（凌晨/上午/下午/夜间）
 * - 成就计算: 委托给 AchievementCalculator 计算 AchievementStats，再通过 AchievementDefinitions 过滤已解锁成就
 *
 * 创建: 2024
 */
package com.draftpeek.feature.stats.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.util.AppCacheManager
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.data.repository.RecentFilesRepository
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.usecase.GetRecentFilesUseCase
import com.draftpeek.core.ui.theme.RainbowColor
import com.draftpeek.feature.settings.repository.SettingsRepository
import com.draftpeek.feature.stats.model.Achievement
import com.draftpeek.feature.stats.util.AchievementCalculator
import com.draftpeek.feature.stats.util.AchievementDefinitions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 统计时间周期枚举。
 *
 * 定义统计页面支持的时间筛选周期，用于控制统计数据的时间范围。
 */
enum class StatsPeriod {
    /** 摘要 - 显示全部历史数据汇总 */
    SUMMARY,
    /** 本年 - 显示本年度 1月1日至今的数据 */
    YEAR,
    /** 本月 - 显示本月 1日至今的数据 */
    MONTH,
    /** 最近7天 - 显示最近 7 天（含今天）的数据 */
    WEEK,
    /** 今日 - 仅显示今天的数据 */
    TODAY,
    /** 昨日 - 仅显示昨天的数据 */
    YESTERDAY
}

/**
 * 周期统计数据。
 *
 * 包含选定时间周期内的核心统计指标，用于在 StatCardGrid 中展示。
 *
 * @property usageDurationMinutes 使用时长（分钟）
 * @property charWriteCount 书写字符数
 * @property fileReadCount 文件打开/阅读数量
 * @property fileCreateCount 文件创建数量
 */
data class PeriodStats(
    val usageDurationMinutes: Long = 0,
    val charWriteCount: Long = 0,
    val fileReadCount: Int = 0,
    val fileCreateCount: Int = 0
)

/**
 * 单日活动详情数据。
 *
 * 包含某一天的详细活动信息，用于 DayDetailDialog 展示。
 *
 * @property date 日期
 * @property usageDurationMinutes 使用时长（分钟）
 * @property charWriteCount 书写字符数
 * @property fileReadCount 文件打开/阅读数量
 * @property fileCreateCount 文件创建数量
 * @property dayOfWeek 星期几的中文显示名称（如"星期一"）
 * @property summary 智能生成的当日活动摘要文案
 * @property activeTimePeriods 活跃时段标签列表（如["上午", "下午"]）
 */
data class DayDetail(
    val date: LocalDate,
    val usageDurationMinutes: Int,
    val charWriteCount: Int,
    val fileReadCount: Int,
    val fileCreateCount: Int,
    val dayOfWeek: String,
    val summary: String,
    val activeTimePeriods: List<String>,
)

/**
 * 统计页面一次性消息事件密封类。
 *
 * 用于通过 SharedFlow 向 UI 层发送一次性事件（如 Toast 提示）。
 */
sealed class StatsMessage {
    /** 缓存清理成功事件 */
    data object CacheCleared : StatsMessage()
    /**
     * 缓存清理失败事件
     * @param error 错误信息
     */
    data class CacheClearFailed(val error: String) : StatsMessage()
}

/**
 * 统计页面 ViewModel。
 *
 * 使用 Hilt 依赖注入，负责统计页面的所有数据管理和业务逻辑。
 * 通过多个 StateFlow 向 UI 层提供响应式数据流，使用 viewModelScope 管理协程生命周期。
 *
 * @param context 应用程序上下文（使用 @ApplicationContext 避免内存泄漏）
 * @param userActivityRepository 用户活动数据仓库
 * @param recentFilesRepository 最近文件仓库
 * @param getRecentFiles 获取最近文件用例
 * @param settingsRepository 设置数据仓库，用于读取/保存热力图颜色等设置
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userActivityRepository: UserActivityRepository,
    private val recentFilesRepository: RecentFilesRepository,
    private val getRecentFiles: GetRecentFilesUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** 一次性消息事件流，用于向 UI 发送 Toast 等通知 */
    private val _messageEvent = MutableSharedFlow<StatsMessage>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messageEvent: SharedFlow<StatsMessage> = _messageEvent.asSharedFlow()

    /** UI 状态 */
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    /** 最近打开的文件列表 */
    val recentFiles: StateFlow<ImmutableList<RecentFile>> = getRecentFiles()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    /** ISO 日期格式化器（yyyy-MM-dd） */
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    /** 系统默认时区 */
    private val systemZone: ZoneId = ZoneId.systemDefault()
    /** 当前日期（动态获取，避免应用长时间运行跨天导致日期错误） */
    private val today: LocalDate get() = LocalDate.now(systemZone)
    /** 两年前的日期，用于加载历史活动数据（动态计算） */
    private val twoYearsAgo: LocalDate get() = today.minusYears(2)

    /** 当前选中的统计周期 */
    private val _selectedPeriod = MutableStateFlow(StatsPeriod.YEAR)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    /** 当前选中周期的统计数据 */
    private val _periodStats = MutableStateFlow(PeriodStats())
    val periodStats: StateFlow<PeriodStats> = _periodStats.asStateFlow()

    /** 用户首次使用日期 */
    private val _firstUseDate = MutableStateFlow<LocalDate?>(null)
    val firstUseDate: StateFlow<LocalDate?> = _firstUseDate.asStateFlow()

    /** 已解锁的成就列表 */
    private val _unlockedAchievements = MutableStateFlow<ImmutableList<Achievement>>(persistentListOf())
    val unlockedAchievements: StateFlow<ImmutableList<Achievement>> = _unlockedAchievements.asStateFlow()

    /** 本年度活动数据（日期字符串 -> UserActivity） */
    val yearActivities: StateFlow<Map<String, UserActivity>> =
        userActivityRepository.getActivityForDateRange(
            today.withDayOfYear(1).format(dateFormatter),
            today.format(dateFormatter)
        )
            .map { list -> list.associateBy { it.date } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** 近两年活动数据（日期字符串 -> UserActivity） */
    val allActivities: StateFlow<Map<String, UserActivity>> =
        userActivityRepository.getActivityForDateRange(
            twoYearsAgo.format(dateFormatter),
            today.format(dateFormatter)
        )
            .map { list -> list.associateBy { it.date } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            userActivityRepository.backfillFromRecentFiles()
        }

        viewModelScope.launch {
            settingsRepository.getActivityColor().collect { color ->
                _uiState.value = _uiState.value.copy(selectedColor = color)
            }
        }

        viewModelScope.launch {
            allActivities.collect { activities ->
                updateFirstUseDate(activities)
                updateUnlockedAchievements(activities)
            }
        }

        viewModelScope.launch {
            _selectedPeriod
                .flatMapLatest { period ->
                    combine(yearActivities, allActivities) { yearAct, allAct ->
                        calculatePeriodStats(period, yearAct, allAct)
                    }
                }
                .collect { stats ->
                    _periodStats.value = stats
                }
        }
    }

    /**
     * 更新首次使用日期。
     *
     * 从所有活动数据中找出最早的有活动日期。
     *
     * @param activities 活动数据映射
     */
    private fun updateFirstUseDate(activities: Map<String, UserActivity>) {
        val dates = activities.values
            .filter { it.hasActivity() }
            .mapNotNull { parseDate(it.date) }
            .sorted()
        _firstUseDate.value = dates.firstOrNull()
    }

    /**
     * 更新已解锁成就列表。
     *
     * 使用 AchievementCalculator 计算成就统计数据，
     * 然后过滤出满足解锁条件的成就。
     *
     * @param activities 活动数据映射
     */
    private suspend fun updateUnlockedAchievements(activities: Map<String, UserActivity>) {
        val files = recentFiles.first()
        val stats = AchievementCalculator.calculate(activities, files)
        val unlocked = AchievementDefinitions.allAchievements.filter { it.check(stats) }
        _unlockedAchievements.value = unlocked.toImmutableList()
    }

    /**
     * 计算指定周期的统计数据。
     *
     * 根据选定的 StatsPeriod 确定日期范围：
     * - SUMMARY: null（全部时间）
     * - YEAR: 本年 1月1日 至 今天
     * - MONTH: 本月 1日 至 今天
     * - WEEK: 6天前 至 今天（共7天）
     * - TODAY: 今天
     * - YESTERDAY: 昨天
     *
     * 遍历活动数据，累加日期范围内的使用时长、字符数、文件打开/创建数。
     *
     * @param period 统计周期
     * @param yearActivities 本年度活动数据
     * @param allActivities 全部活动数据
     * @return 计算得出的周期统计数据
     */
    private fun calculatePeriodStats(
        period: StatsPeriod,
        yearActivities: Map<String, UserActivity>,
        allActivities: Map<String, UserActivity>
    ): PeriodStats {
        val (startDate, endDate) = when (period) {
            StatsPeriod.SUMMARY -> null to today
            StatsPeriod.YEAR -> today.withDayOfYear(1) to today
            StatsPeriod.MONTH -> today.withDayOfMonth(1) to today
            StatsPeriod.WEEK -> today.minusDays(6) to today
            StatsPeriod.TODAY -> today to today
            StatsPeriod.YESTERDAY -> today.minusDays(1) to today.minusDays(1)
        }

        val activities = when (period) {
            StatsPeriod.SUMMARY, StatsPeriod.YEAR -> {
                if (period == StatsPeriod.SUMMARY) allActivities else yearActivities
            }
            else -> allActivities
        }

        var usageDurationMinutes = 0L
        var charWriteCount = 0L
        var fileReadCount = 0
        var fileCreateCount = 0

        for ((dateStr, activity) in activities) {
            val date = parseDate(dateStr) ?: continue
            val inRange = when {
                startDate == null -> true
                date in startDate..endDate -> true
                else -> false
            }
            if (inRange) {
                usageDurationMinutes += activity.usageDurationMinutes
                charWriteCount += activity.charWriteCount
                fileReadCount += activity.fileOpenCount
                fileCreateCount += activity.fileCreateCount
            }
        }

        return PeriodStats(
            usageDurationMinutes = usageDurationMinutes,
            charWriteCount = charWriteCount,
            fileReadCount = fileReadCount,
            fileCreateCount = fileCreateCount
        )
    }

    /**
     * 解析日期字符串为 LocalDate。
     *
     * @param dateStr ISO 格式日期字符串（yyyy-MM-dd）
     * @return LocalDate 对象，解析失败返回 null
     */
    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 选择统计周期。
     *
     * @param period 要选择的统计周期
     */
    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    /**
     * 获取指定日期的详细活动信息。
     *
     * 根据活动数据生成智能摘要文案：
     * - 使用时长 ≥ 120分钟: "充实的一天"
     * - 使用时长 ≥ 60分钟: "高效的一天"
     * - 书写字符 > 500: "专注的一天"
     * - 文件打开 ≥ 5个: "探索的一天"
     * - 创建文件 > 0: "创造的一天"
     * - 其他: "安静的一天"
     *
     * 同时根据最后活动时间识别活跃时段标签。
     *
     * @param date 要查询的日期
     * @return DayDetail 对象，该日期无活动数据时返回 null
     */
    fun getDayDetail(date: LocalDate): DayDetail? {
        val dateStr = date.format(dateFormatter)
        val activity = allActivities.value[dateStr] ?: return null

        val dayOfWeekLabel = date.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.CHINA,
        )

        val summary = when {
            activity.usageDurationMinutes >= 120 -> "充实的一天，书写了 ${activity.charWriteCount} 字"
            activity.usageDurationMinutes >= 60 -> "高效的一天，书写了 ${activity.charWriteCount} 字"
            activity.charWriteCount > 500 -> "专注的一天，书写了 ${activity.charWriteCount} 字"
            activity.fileOpenCount >= 5 -> "探索的一天，阅读了 ${activity.fileOpenCount} 个文件"
            activity.fileCreateCount > 0 -> "创造的一天，新建了 ${activity.fileCreateCount} 个文件"
            else -> "安静的一天"
        }

        val activePeriods = mutableListOf<String>()
        val hour = Instant.ofEpochMilli(activity.updatedAt)
            .atZone(ZoneId.systemDefault()).hour
        when {
            hour in 5..11 -> activePeriods += "上午"
            hour in 12..17 -> activePeriods += "下午"
            hour in 18..23 -> activePeriods += "夜间"
            else -> activePeriods += "凌晨"
        }
        if (activity.usageDurationMinutes > 120) {
            if ("上午" !in activePeriods) activePeriods.add(0, "上午")
            if ("下午" !in activePeriods) activePeriods.add("下午")
        }

        return DayDetail(
            date = date,
            usageDurationMinutes = activity.usageDurationMinutes,
            charWriteCount = activity.charWriteCount,
            fileReadCount = activity.fileOpenCount,
            fileCreateCount = activity.fileCreateCount,
            dayOfWeek = dayOfWeekLabel,
            summary = summary,
            activeTimePeriods = activePeriods,
        )
    }

    /**
     * 格式化时长显示。
     *
     * - < 60分钟: 显示 "Xmin"
     * - < 600分钟（10小时）: 显示 "X.Xh"（一位小数）
     * - ≥ 600分钟: 显示 "Xh"（整数小时）
     *
     * @param minutes 时长（分钟）
     * @return 格式化后的时长字符串
     */
    fun formatDuration(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}min"
            minutes < 600 -> "%.1fh".format(minutes / 60f)
            else -> "${minutes / 60}h"
        }
    }

    /**
     * 格式化数字显示，添加千位分隔符。
     *
     * @param n 要格式化的数字
     * @return 带千位分隔符的数字字符串（如 "1,234,567"）
     */
    fun formatNumber(n: Long): String {
        return "%,d".format(n)
    }

    /**
     * 获取自首次使用以来的总天数。
     *
     * @return 总天数，无首次使用日期时返回 0
     */
    fun getTotalDaysSinceFirstUse(): Int {
        val firstDate = _firstUseDate.value ?: return 0
        return ChronoUnit.DAYS.between(firstDate, today).toInt() + 1
    }

    /**
     * 选择热力图主题颜色，并保存到设置。
     *
     * @param color 选中的彩虹色
     */
    fun selectColor(color: RainbowColor) {
        viewModelScope.launch {
            settingsRepository.setActivityColor(color)
        }
    }

    /**
     * 清除应用缓存。
     *
     * 在 IO 线程执行，通过 messageEvent 发送成功/失败通知。
     */
    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting cache clearing...")
                AppCacheManager.clearAllAppCaches(context)
                Log.d(TAG, "Cache clearing completed successfully")
                _messageEvent.emit(StatsMessage.CacheCleared)
            } catch (e: Exception) {
                Log.e(TAG, "Cache clearing failed", e)
                _messageEvent.emit(StatsMessage.CacheClearFailed(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * 选中热力图上的某一天，打开日期详情弹窗。
     *
     * @param date 选中的日期
     */
    fun onDaySelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDay = date)
    }

    /**
     * 关闭日期详情弹窗。
     */
    fun dismissDayDetail() {
        _uiState.value = _uiState.value.copy(selectedDay = null)
    }

    /**
     * 统计页面 UI 状态数据类。
     *
     * @property selectedColor 当前选中的热力图主题颜色
     * @property selectedDay 当前选中查看详情的日期，null 表示未选中
     */
    data class StatsUiState(
        val selectedColor: RainbowColor = RainbowColor.RED,
        val selectedDay: LocalDate? = null,
    )

    companion object {
        private const val TAG = "StatsViewModel"
    }
}
