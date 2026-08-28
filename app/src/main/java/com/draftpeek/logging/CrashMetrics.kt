/**
 * 崩溃率监控与发布健康度指标模块。
 *
 * 基于本地崩溃日志和会话计数，计算发布健康度指标：
 * - 崩溃率（崩溃次数 / 会话数 × 100%）
 * - 崩溃趋势（按天统计）
 * - 严重崩溃分类（按异常类型分组）
 *
 * 设计为离线优先，所有指标存储在本地 SharedPreferences 中，
 * 可通过设置页导出或在未来接入远程上报通道。
 *
 * @author DraftPeek Team
 * @since 1.0.31
 */
package com.draftpeek.logging

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.json.JSONArray
import org.json.JSONObject

/**
 * 崩溃指标采集器（单例）。
 *
 * 在每次应用启动时递增会话计数，在崩溃发生时递增崩溃计数，
 * 并按天记录崩溃事件摘要。提供 [getHealthReport] 方法输出
 * 当前版本的健康度报告，用于判断发布是否需要暂停或回滚。
 *
 * **崩溃率阈值**：
 * - < 0.1% → HEALTHY（健康，继续发布）
 * - 0.1% ~ 1% → WATCH（观察，降低灰度比例）
 * - > 1% → CRITICAL（危险，暂停发布并评估回滚）
 */
object CrashMetrics {

    private const val TAG = "CrashMetrics"

    private const val PREFS_NAME = "draftpeek_crash_metrics"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_CURRENT_VERSION_CODE = "current_version_code"
    private const val KEY_DAILY_RECORDS = "daily_records"

    /** 崩溃率阈值（百分比） */
    private const val THRESHOLD_WATCH = 0.1
    private const val THRESHOLD_CRITICAL = 1.0

    /** 每日最多保留记录数（防止无限增长） */
    private const val MAX_DAILY_RECORDS = 90

    private lateinit var prefs: SharedPreferences

    /** 当前会话的崩溃计数（进程级，每次启动重置） */
    private val sessionCrashCount = AtomicInteger(0)

    /** 当前会话 ID（基于启动时间戳） */
    private val sessionId = AtomicLong(0)

    /**
     * 初始化崩溃指标采集器。
     *
     * 应在 [DraftPeekApp.onCreate] 中 [AppLogger.init] 之后调用。
     * 每次调用递增版本级会话计数。
     *
     * @param context 应用上下文
     * @param versionCode 当前版本号（用于区分不同版本的指标）
     */
    fun init(context: Context, versionCode: Int) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sessionId.set(System.currentTimeMillis())

        // 版本切换时重置计数
        val storedVersionCode = prefs.getInt(KEY_CURRENT_VERSION_CODE, -1)
        if (storedVersionCode != versionCode) {
            prefs.edit()
                .putInt(KEY_CURRENT_VERSION_CODE, versionCode)
                .putInt(KEY_SESSION_COUNT, 0)
                .putInt(KEY_CRASH_COUNT, 0)
                .putString(KEY_DAILY_RECORDS, JSONArray().toString())
                .apply()
        }

        // 递增会话计数
        val sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, sessionCount).apply()

        Log.i(TAG, "Session #$sessionCount started for versionCode=$versionCode")
    }

    /**
     * 记录一次崩溃事件。
     *
     * 由 [AppLogger.CrashHandler] 在崩溃发生时调用。
     * 递增崩溃计数并写入每日记录摘要。
     *
     * @param throwable 导致崩溃的异常
     */
    fun recordCrash(throwable: Throwable) {
        if (!::prefs.isInitialized) return
        try {
            sessionCrashCount.incrementAndGet()

            // 递增版本级崩溃计数
            val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
            prefs.edit().putInt(KEY_CRASH_COUNT, crashCount).apply()

            // 写入每日记录
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val crashType = throwable.javaClass.simpleName.ifEmpty { "Unknown" }
            appendDailyRecord(today, crashType, throwable.message ?: "")

            Log.w(TAG, "Crash recorded: total=$crashCount, type=$crashType")
        } catch (_: Throwable) {
            // 指标记录失败绝不能影响崩溃处理流程
        }
    }

    /**
     * 获取当前版本的发布健康度报告。
     *
     * @return 健康度报告 JSON 字符串，包含会话数、崩溃数、崩溃率、
     *         健康等级和最近 N 天的崩溃趋势
     */
    fun getHealthReport(): String {
        if (!::prefs.isInitialized) return "{}"
        try {
            val sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0)
            val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
            val crashRate = if (sessionCount > 0) {
                (crashCount.toDouble() / sessionCount.toDouble()) * 100.0
            } else {
                0.0
            }

            val healthLevel = when {
                crashRate > THRESHOLD_CRITICAL -> "CRITICAL"
                crashRate > THRESHOLD_WATCH -> "WATCH"
                else -> "HEALTHY"
            }

            val report = JSONObject().apply {
                put("versionCode", prefs.getInt(KEY_CURRENT_VERSION_CODE, -1))
                put("sessionCount", sessionCount)
                put("crashCount", crashCount)
                put("crashRatePercent", String.format("%.3f", crashRate))
                put("healthLevel", healthLevel)
                put("thresholdWatch", THRESHOLD_WATCH)
                put("thresholdCritical", THRESHOLD_CRITICAL)
                put("dailyRecords", getDailyRecordsArray())
                put(
                    "deviceInfo",
                    JSONObject().apply {
                        put("manufacturer", Build.MANUFACTURER)
                        put("model", Build.MODEL)
                        put("apiLevel", Build.VERSION.SDK_INT)
                        put("osVersion", Build.VERSION.RELEASE ?: "unknown")
                    }
                )
                put("reportTime", System.currentTimeMillis())
            }
            return report.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate health report", e)
            return "{}"
        }
    }

    /**
     * 判断当前版本是否处于健康状态。
     *
     * 用于灰度发布决策：若返回 false，应暂停或回滚发布。
     */
    fun isHealthy(): Boolean {
        if (!::prefs.isInitialized) return true
        val sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0)
        val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
        if (sessionCount < 10) return true // 样本不足，不阻断
        val crashRate = (crashCount.toDouble() / sessionCount.toDouble()) * 100.0
        return crashRate <= THRESHOLD_CRITICAL
    }

    /**
     * 获取当前会话的崩溃次数（进程级）。
     */
    fun sessionCrashCount(): Int = sessionCrashCount.get()

    // ===== 内部方法 =====

    private fun appendDailyRecord(date: String, crashType: String, message: String) {
        try {
            val recordsArray = getDailyRecordsArray()
            val todayRecord: JSONObject = (0 until recordsArray.length()).map { recordsArray.getJSONObject(it) }
                .find { it.optString("date") == date }
                ?: JSONObject().apply {
                    put("date", date)
                    put("crashes", JSONArray())
                }

            val crashesArray = todayRecord.optJSONArray("crashes") ?: JSONArray()
            val crashEntry = JSONObject().apply {
                put("type", crashType)
                put("message", message.take(200)) // 截断长消息
                put("time", System.currentTimeMillis())
            }
            crashesArray.put(crashEntry)
            todayRecord.put("crashes", crashesArray)

            // 更新或插入当天记录
            val updatedArray = JSONArray()
            var found = false
            for (i in 0 until recordsArray.length()) {
                val record = recordsArray.getJSONObject(i)
                if (record.optString("date") == date) {
                    updatedArray.put(todayRecord)
                    found = true
                } else {
                    updatedArray.put(record)
                }
            }
            if (!found) {
                updatedArray.put(todayRecord)
            }

            // 限制记录数量，超出时删除最旧的
            val finalArray = if (updatedArray.length() > MAX_DAILY_RECORDS) {
                val sorted = (0 until updatedArray.length())
                    .map { updatedArray.getJSONObject(it) }
                    .sortedByDescending { it.optString("date") }
                    .take(MAX_DAILY_RECORDS)
                JSONArray().apply { sorted.forEach { put(it) } }
            } else {
                updatedArray
            }

            prefs.edit().putString(KEY_DAILY_RECORDS, finalArray.toString()).apply()
        } catch (_: Throwable) {
            // 指标记录失败不影响主流程
        }
    }

    private fun getDailyRecordsArray(): JSONArray {
        val raw = prefs.getString(KEY_DAILY_RECORDS, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Throwable) {
            JSONArray()
        }
    }
}
