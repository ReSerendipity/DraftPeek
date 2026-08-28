/**
 * 渲染性能分析工具模块。
 *
 * 提供内存使用跟踪和Compose重组性能分析功能：
 * - MemoryTracker：定期采样JVM堆内存，记录内存压力事件
 * - RenderProfiler：跟踪Composable的重组次数和耗时，检测过度重组
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 内存使用跟踪器类。
 *
 * 定期采样JVM堆内存使用情况，记录内存压力事件。可与[PerformanceBenchmark]集成以与其他指标关联分析。
 * 使用CoroutineScope在后台线程执行采样，支持开始/停止控制。
 *
 * @property sampleIntervalMs 采样间隔（毫秒），默认5秒
 * @property warningThresholdBytes 内存警告阈值（字节），默认200MB
 */
class MemoryTracker(
    private val sampleIntervalMs: Long = DEFAULT_SAMPLE_INTERVAL_MS,
    private val warningThresholdBytes: Long = DEFAULT_WARNING_THRESHOLD_BYTES
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sampleJob: Job? = null

    private val _samples = mutableListOf<MemorySample>()
    private val _pressureEvents = mutableListOf<MemoryPressureEvent>()
    private val _currentUsage = MutableStateFlow(0L)

    /** 当前内存使用量的StateFlow，可用于UI观察 */
    val currentUsage: StateFlow<Long> = _currentUsage.asStateFlow()

    companion object {
        private const val TAG = "MemoryTracker"

        /** 默认采样间隔：5秒 */
        const val DEFAULT_SAMPLE_INTERVAL_MS = 5000L

        /** 默认内存警告阈值：200MB */
        const val DEFAULT_WARNING_THRESHOLD_BYTES = 200 * 1024 * 1024L
    }

    /**
     * 内存采样数据类，记录某一时刻的内存使用快照。
     *
     * @property timestampMs 时间戳（毫秒）
     * @property totalMemoryBytes 总内存（字节）
     * @property freeMemoryBytes 空闲内存（字节）
     * @property usedMemoryBytes 已使用内存（字节）
     * @property maxMemoryBytes 最大可用内存（字节）
     */
    data class MemorySample(
        val timestampMs: Long,
        val totalMemoryBytes: Long,
        val freeMemoryBytes: Long,
        val usedMemoryBytes: Long,
        val maxMemoryBytes: Long
    )

    /**
     * 内存压力事件数据类，记录内存使用超过阈值的事件。
     *
     * @property timestampMs 时间戳（毫秒）
     * @property usedMemoryBytes 已使用内存（字节）
     * @property maxMemoryBytes 最大可用内存（字节）
     * @property usagePercent 使用百分比
     * @property message 事件消息
     */
    data class MemoryPressureEvent(
        val timestampMs: Long,
        val usedMemoryBytes: Long,
        val maxMemoryBytes: Long,
        val usagePercent: Float,
        val message: String
    )

    /**
     * 内存快照汇总数据类，包含所有采样数据和统计信息。
     *
     * @property samples 所有内存采样列表
     * @property pressureEvents 所有内存压力事件列表
     * @property peakUsageBytes 峰值内存使用（字节）
     * @property averageUsageBytes 平均内存使用（字节）
     */
    data class MemorySnapshot(
        val samples: List<MemorySample>,
        val pressureEvents: List<MemoryPressureEvent>,
        val peakUsageBytes: Long,
        val averageUsageBytes: Long
    )

    /**
     * 开始定期内存采样。
     * 如果已在运行则忽略。
     */
    fun start() {
        if (sampleJob?.isActive == true) return
        sampleJob = scope.launch {
            while (true) {
                sample()
                delay(sampleIntervalMs)
            }
        }
    }

    /**
     * 停止定期采样。
     */
    fun stop() {
        sampleJob?.cancel()
        sampleJob = null
    }

    /**
     * 立即执行一次内存采样。
     *
     * @return 本次采样结果
     */
    fun sample(): MemorySample {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val used = total - free
        val max = runtime.maxMemory()

        val sample = MemorySample(
            timestampMs = System.currentTimeMillis(),
            totalMemoryBytes = total,
            freeMemoryBytes = free,
            usedMemoryBytes = used,
            maxMemoryBytes = max
        )

        synchronized(_samples) { _samples.add(sample) }
        _currentUsage.value = used

        if (used > warningThresholdBytes) {
            val usagePercent = (used.toFloat() / max) * 100
            val event = MemoryPressureEvent(
                timestampMs = sample.timestampMs,
                usedMemoryBytes = used,
                maxMemoryBytes = max,
                usagePercent = usagePercent,
                message = "High memory usage: ${formatBytes(used)} / ${formatBytes(max)} (${usagePercent.toInt()}%)"
            )
            synchronized(_pressureEvents) { _pressureEvents.add(event) }
            Log.w(TAG, event.message)
        }

        return sample
    }

    /**
     * 获取所有已收集采样和压力事件的快照。
     *
     * @return 包含所有数据和统计信息的MemorySnapshot
     */
    fun snapshot(): MemorySnapshot {
        synchronized(_samples) {
            synchronized(_pressureEvents) {
                return MemorySnapshot(
                    samples = _samples.toList(),
                    pressureEvents = _pressureEvents.toList(),
                    peakUsageBytes = _samples.maxOfOrNull { it.usedMemoryBytes } ?: 0,
                    averageUsageBytes = if (_samples.isEmpty()) {
                        0L
                    } else {
                        _samples.map {
                            it.usedMemoryBytes
                        }.average().toLong()
                    }
                )
            }
        }
    }

    /**
     * 清除所有已收集的采样和事件。
     */
    fun clear() {
        synchronized(_samples) { _samples.clear() }
        synchronized(_pressureEvents) { _pressureEvents.clear() }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    }
}

/**
 * Compose渲染性能分析器类。
 *
 * 跟踪特定Composable的重组次数和耗时，通过追踪钩子包装Composable实现。结果可导出用于分析，
 * 检测过度重组（重组次数超过阈值时输出警告日志）。
 */
class RenderProfiler {

    private val counters = mutableMapOf<String, RecompositionCounter>()
    private var enabled = true

    /**
     * 重组计数器数据类，记录单个Composable的重组统计。
     *
     * @property key Composable标识符
     * @property count 重组次数
     * @property totalDurationNs 总耗时（纳秒）
     * @property maxDurationNs 最大单次耗时（纳秒）
     * @property lastTimestampMs 最后一次重组时间戳（毫秒）
     */
    data class RecompositionCounter(
        val key: String,
        var count: Int = 0,
        var totalDurationNs: Long = 0,
        var maxDurationNs: Long = 0,
        var lastTimestampMs: Long = 0
    )

    companion object {
        private const val TAG = "RenderProfiler"

        /** 过度重组警告阈值：重组次数超过此值时输出警告 */
        const val EXCESSIVE_RECOMPOSITION_THRESHOLD = 10
    }

    /**
     * 启用或禁用性能分析。禁用时所有方法为空操作。
     *
     * @param enabled 是否启用
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * 在Composable重组开始时调用。
     *
     * @param key Composable标识符（如组件名称）
     * @return 传递给[onRecomposeEnd]的追踪键
     */
    fun onRecompose(key: String): String {
        if (!enabled) return ""
        val counter = counters.getOrPut(key) { RecompositionCounter(key) }
        counter.count++
        counter.lastTimestampMs = System.currentTimeMillis()
        if (counter.count == EXCESSIVE_RECOMPOSITION_THRESHOLD) {
            Log.w(TAG, "Excessive recomposition detected for '$key': ${counter.count} times")
        }
        return key
    }

    /**
     * 在Composable重组结束时调用。
     *
     * @param traceKey [onRecompose]返回的追踪键
     * @param durationNs 本次重组耗时（纳秒），默认0
     */
    fun onRecomposeEnd(traceKey: String, durationNs: Long = 0) {
        if (!enabled || traceKey.isEmpty()) return
        val counter = counters[traceKey] ?: return
        if (durationNs > 0) {
            counter.totalDurationNs += durationNs
            counter.maxDurationNs = maxOf(counter.maxDurationNs, durationNs)
        }
    }

    /**
     * 获取所有已跟踪Composable的重组统计，按重组次数降序排序。
     *
     * @return 重组计数器列表
     */
    fun getStats(): List<RecompositionCounter> = counters.values.sortedByDescending { it.count }

    /**
     * 获取指定键的重组次数。
     *
     * @param key Composable标识符
     * @return 重组次数，未找到返回0
     */
    fun getCount(key: String): Int = counters[key]?.count ?: 0

    /**
     * 重置所有计数器。
     */
    fun reset() {
        counters.clear()
    }

    /**
     * 将所有重组计数摘要输出到Logcat。
     */
    fun logSummary() {
        if (!enabled) return
        val stats = getStats()
        if (stats.isEmpty()) return
        Log.d(TAG, "=== Render Profiler Summary ===")
        for (counter in stats) {
            val avgNs = if (counter.count > 0) counter.totalDurationNs / counter.count else 0
            Log.d(
                TAG,
                "  ${counter.key}: ${counter.count} recompositions, " +
                    "avg=${avgNs / 1_000_000.0}ms, max=${counter.maxDurationNs / 1_000_000.0}ms"
            )
        }
        val excessive = stats.filter { it.count >= EXCESSIVE_RECOMPOSITION_THRESHOLD }
        if (excessive.isNotEmpty()) {
            Log.w(TAG, "Excessive recomposition: ${excessive.joinToString { "${it.key}(${it.count}x" }}")
        }
    }
}
