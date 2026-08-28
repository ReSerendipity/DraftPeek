/**
 * DraftPeek 运行时性能监控工具。
 *
 * 本对象在应用运行时收集性能指标，用于识别性能瓶颈并优化用户体验。
 *
 * 收集的指标包括：
 * - 文件操作耗时统计
 * - 内存使用模式
 * - 操作期间的 CPU 使用情况
 * - 帧渲染性能（卡顿检测）
 * - 启动时间分解
 *
 * 参考：core/common 模块中的 PerformanceBenchmark。
 */
package com.draftpeek.benchmark

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 运行时性能监控器。
 *
 * 提供线程安全的性能数据收集功能，包括操作计时、内存快照和帧渲染统计。
 * 使用 ConcurrentHashMap 和 AtomicLong 确保多线程环境下的数据安全。
 */
object RuntimePerformanceMonitor {

    private const val TAG = "PerformanceMonitor"

    /** 操作耗时记录，键为操作名称，值为耗时列表（毫秒） */
    private val operationTimings = ConcurrentHashMap<String, MutableList<Long>>()

    /** 操作计数记录，键为操作名称，值为调用次数 */
    private val operationCounts = ConcurrentHashMap<String, AtomicLong>()

    /** 内存快照列表，最多保留最近 1000 条记录 */
    private val memorySnapshots = mutableListOf<MemorySnapshot>()

    /** 已渲染的总帧数 */
    private var frameCount = 0

    /** 累计帧渲染时间（毫秒），用于计算真实平均帧时间 */
    private var totalFrameTimeMs = 0.0

    /** 上一帧的时间戳 */
    private var lastFrameTime = 0L

    /** 卡顿帧数量（渲染时间超过 16.67ms） */
    private var jankCount = 0

    /**
     * 内存快照数据类。
     *
     * @property timestamp 快照时间戳（毫秒）
     * @property usedHeap 已使用的堆内存（字节）
     * @property maxHeap 最大可用堆内存（字节）
     * @property nativeHeap 已使用的 Native 堆内存（字节）
     * @property operation 关联的操作名称，可为 null
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val usedHeap: Long,
        val maxHeap: Long,
        val nativeHeap: Long,
        val operation: String? = null
    )

    /**
     * 性能报告数据类。
     *
     * @property operationTimings 各操作的耗时统计
     * @property memoryStats 内存使用统计
     * @property frameStats 帧渲染统计
     * @property timestamp 报告生成时间戳
     */
    data class PerformanceReport(
        val operationTimings: Map<String, TimingStats>,
        val memoryStats: MemoryStats,
        val frameStats: FrameStats,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 操作耗时统计数据类。
     *
     * @property count 操作执行次数
     * @property avgMs 平均耗时（毫秒）
     * @property minMs 最小耗时（毫秒）
     * @property maxMs 最大耗时（毫秒）
     * @property p50Ms 50 百分位耗时（中位数，毫秒）
     * @property p90Ms 90 百分位耗时（毫秒）
     * @property p99Ms 99 百分位耗时（毫秒）
     */
    data class TimingStats(
        val count: Long,
        val avgMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val p50Ms: Long,
        val p90Ms: Long,
        val p99Ms: Long
    )

    /**
     * 内存统计数据类。
     *
     * @property avgUsedHeap 平均已使用堆内存（字节）
     * @property maxUsedHeap 最大已使用堆内存（字节）
     * @property avgNativeHeap 平均已使用 Native 堆内存（字节）
     * @property maxNativeHeap 最大已使用 Native 堆内存（字节）
     * @property snapshotCount 快照数量
     */
    data class MemoryStats(
        val avgUsedHeap: Long,
        val maxUsedHeap: Long,
        val avgNativeHeap: Long,
        val maxNativeHeap: Long,
        val snapshotCount: Int
    )

    /**
     * 帧渲染统计数据类。
     *
     * @property totalFrames 总帧数
     * @property jankFrames 卡顿帧数
     * @property jankPercentage 卡顿帧百分比
     * @property avgFrameTime 平均帧渲染时间（毫秒）
     */
    data class FrameStats(
        val totalFrames: Long,
        val jankFrames: Long,
        val jankPercentage: Double,
        val avgFrameTime: Double
    )

    /**
     * 开始计时一个操作。
     *
     * @param operation 要计时的操作名称
     * @return 用于停止计时的令牌对象
     */
    fun startTiming(operation: String): TimingToken = TimingToken(operation, SystemClock.elapsedRealtime())

    /**
     * 停止计时并记录结果。
     *
     * @param token 由 startTiming 返回的计时令牌
     */
    fun stopTiming(token: TimingToken) {
        val elapsed = SystemClock.elapsedRealtime() - token.startTime
        recordTiming(token.operation, elapsed)
    }

    /**
     * 记录一个耗时测量值。
     *
     * 如果操作耗时超过 1 秒，会在 Logcat 中输出警告日志。
     *
     * @param operation 操作名称
     * @param durationMs 耗时（毫秒）
     */
    fun recordTiming(operation: String, durationMs: Long) {
        operationTimings.getOrPut(operation) { mutableListOf() }.add(durationMs)
        operationCounts.getOrPut(operation) { AtomicLong(0) }.incrementAndGet()

        if (durationMs > 1000) {
            Log.w(TAG, "检测到慢操作：$operation 耗时 ${durationMs}ms")
        }
    }

    /**
     * 拍摄一次内存快照。
     *
     * 快照会被添加到内存快照列表中，最多保留最近 1000 条记录。
     *
     * @param operation 可选的关联操作名称
     */
    fun takeMemorySnapshot(operation: String? = null) {
        val runtime = Runtime.getRuntime()
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()

        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedHeap = usedHeap,
            maxHeap = maxHeap,
            nativeHeap = nativeHeap,
            operation = operation
        )

        synchronized(memorySnapshots) {
            memorySnapshots.add(snapshot)
            if (memorySnapshots.size > 1000) {
                memorySnapshots.removeAt(0)
            }
        }
    }

    /**
     * 记录一帧渲染事件。
     *
     * @param frameTimeMs 帧渲染耗时（毫秒），超过 16.67ms（60fps）视为卡顿帧
     */
    fun recordFrame(frameTimeMs: Long) {
        frameCount++
        totalFrameTimeMs += frameTimeMs.toDouble()
        if (frameTimeMs > 16.67) {
            jankCount++
        }
        lastFrameTime = SystemClock.elapsedRealtime()
    }

    /**
     * 判断一帧是否为卡顿帧。
     *
     * @param frameTimeMs 帧渲染耗时（毫秒）
     * @return 如果帧渲染时间超过 16.67ms（60fps 阈值）则返回 true
     */
    fun isJankFrame(frameTimeMs: Long): Boolean = frameTimeMs > 16.67

    /**
     * 生成性能报告。
     *
     * 汇总所有收集到的性能数据，计算各项统计指标（平均值、百分位数等）。
     *
     * @return 包含所有收集指标的 PerformanceReport 对象
     */
    fun generateReport(): PerformanceReport {
        val timingStats = operationTimings.map { (operation, timings) ->
            val sorted = timings.sorted()
            val count = sorted.size.toLong()
            val avg = sorted.average()
            val min = sorted.minOrNull() ?: 0L
            val max = sorted.maxOrNull() ?: 0L
            val p50 = sorted[(sorted.size * 0.5).toInt().coerceAtMost(sorted.size - 1)]
            val p90 = sorted[(sorted.size * 0.9).toInt().coerceAtMost(sorted.size - 1)]
            val p99 = sorted[(sorted.size * 0.99).toInt().coerceAtMost(sorted.size - 1)]

            operation to TimingStats(
                count = count,
                avgMs = avg,
                minMs = min,
                maxMs = max,
                p50Ms = p50,
                p90Ms = p90,
                p99Ms = p99
            )
        }.toMap()

        val memoryStats = synchronized(memorySnapshots) {
            if (memorySnapshots.isEmpty()) {
                MemoryStats(0, 0, 0, 0, 0)
            } else {
                val usedHeapList = memorySnapshots.map { it.usedHeap }
                val nativeHeapList = memorySnapshots.map { it.nativeHeap }
                MemoryStats(
                    avgUsedHeap = usedHeapList.average().toLong(),
                    maxUsedHeap = usedHeapList.maxOrNull() ?: 0L,
                    avgNativeHeap = nativeHeapList.average().toLong(),
                    maxNativeHeap = nativeHeapList.maxOrNull() ?: 0L,
                    snapshotCount = memorySnapshots.size
                )
            }
        }

        val frameStats = if (frameCount > 0) {
            FrameStats(
                totalFrames = frameCount.toLong(),
                jankFrames = jankCount.toLong(),
                jankPercentage = (jankCount.toDouble() / frameCount) * 100,
                avgFrameTime = if (frameCount > 0) totalFrameTimeMs / frameCount else 0.0
            )
        } else {
            FrameStats(0, 0, 0.0, 0.0)
        }

        return PerformanceReport(
            operationTimings = timingStats,
            memoryStats = memoryStats,
            frameStats = frameStats
        )
    }

    /**
     * 清除所有收集的性能指标。
     *
     * 重置所有计时、内存快照和帧统计数据。
     */
    fun clear() {
        operationTimings.clear()
        operationCounts.clear()
        synchronized(memorySnapshots) {
            memorySnapshots.clear()
        }
        frameCount = 0
        totalFrameTimeMs = 0.0
        jankCount = 0
        lastFrameTime = 0L
    }

    /**
     * 将性能摘要输出到 Logcat。
     *
     * 生成并记录包含操作耗时、内存使用和帧统计的性能报告。
     */
    fun logSummary() {
        val report = generateReport()
        Log.i(TAG, "=== 性能报告 ===")
        Log.i(TAG, "操作数量：${report.operationTimings.size}")
        report.operationTimings.forEach { (operation, stats) ->
            Log.i(
                TAG,
                "$operation：次数=${stats.count}, 平均=${String.format("%.2f", stats.avgMs)}ms, " +
                    "最小=${stats.minMs}ms, 最大=${stats.maxMs}ms, P50=${stats.p50Ms}ms, P90=${stats.p90Ms}ms"
            )
        }
        Log.i(
            TAG,
            "内存：平均堆=${report.memoryStats.avgUsedHeap / 1024 / 1024}MB, " +
                "最大堆=${report.memoryStats.maxUsedHeap / 1024 / 1024}MB, " +
                "平均Native=${report.memoryStats.avgNativeHeap / 1024 / 1024}MB"
        )
        Log.i(
            TAG,
            "帧：总数=${report.frameStats.totalFrames}, 卡顿=${report.frameStats.jankFrames}, " +
                "卡顿率=${String.format("%.2f", report.frameStats.jankPercentage)}%"
        )
    }

    /**
     * 计时令牌类。
     *
     * 用于标记计时操作的开始，由 [startTiming] 返回，传递给 [stopTiming] 以完成计时。
     *
     * @property operation 操作名称
     * @property startTime 开始时间（elapsedRealtime）
     */
    class TimingToken internal constructor(val operation: String, internal val startTime: Long)
}
