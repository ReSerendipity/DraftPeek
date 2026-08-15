/**
 * 性能基准测试工具模块。
 *
 * 轻量级性能基准测试工具，用于在Debug构建中测量关键操作的性能指标：操作响应时间、帧率、内存使用、
 * 文件I/O吞吐量、启动时间、性能回归自动检测等。使用ConcurrentHashMap保证线程安全，
 * 提供JSON导出功能用于CI/CD集成和历史追踪。
 *
 * 注意：此工具仅应在Debug构建中使用，Release构建中应避免调用。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.os.Build
import android.os.Debug
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

private const val TAG = "PerfBenchmark"

/**
 * 性能基准测试工具对象。
 *
 * 提供操作计时、内存快照、文件I/O基准测试、性能回归检测、JSON导出等功能。
 * 使用线程安全的ConcurrentHashMap存储计时和结果数据，支持同步和挂起函数的性能测量。
 * 预定义了关键操作的性能阈值，超过阈值时输出警告日志。
 */
object PerformanceBenchmark {

    private val timers = ConcurrentHashMap<String, Long>()
    private val results = ConcurrentHashMap<String, MutableList<Measurement>>()
    private val memorySnapshots = ConcurrentHashMap<String, MutableList<MemorySnapshot>>()
    private val regressionLog = mutableListOf<RegressionEntry>()
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val successCounts = ConcurrentHashMap<String, AtomicInteger>()

    /**
     * 单次测量结果数据类。
     *
     * @property name 操作名称
     * @property durationNanos 耗时（纳秒）
     * @property timestamp 时间戳（毫秒）
     */
    data class Measurement(
        val name: String,
        val durationNanos: Long,
        val timestamp: Long,
    )

    /**
     * 内存快照数据类，捕获某一时刻的堆内存使用情况。
     *
     * @property tag 快照标签
     * @property heapUsedBytes 已使用堆内存（字节）
     * @property heapMaxBytes 最大堆内存（字节）
     * @property nativeHeapBytes 原生堆内存（字节）
     * @property timestamp 时间戳（毫秒）
     */
    data class MemorySnapshot(
        val tag: String,
        val heapUsedBytes: Long,
        val heapMaxBytes: Long,
        val nativeHeapBytes: Long,
        val timestamp: Long,
    ) {
        /** 已使用堆内存（MB） */
        val heapUsedMB: Float get() = heapUsedBytes / (1024f * 1024f)
        /** 最大堆内存（MB） */
        val heapMaxMB: Float get() = heapMaxBytes / (1024f * 1024f)
        /** 原生堆内存（MB） */
        val nativeHeapMB: Float get() = nativeHeapBytes / (1024f * 1024f)
    }

    /**
     * 检测到的性能回归条目数据类。
     *
     * @property name 操作名称
     * @property currentMs 当前平均耗时（毫秒）
     * @property baselineMs 基线耗时（毫秒）
     * @property regressionPercent 回归百分比
     * @property timestamp 时间戳（毫秒）
     */
    data class RegressionEntry(
        val name: String,
        val currentMs: Long,
        val baselineMs: Long,
        val regressionPercent: Double,
        val timestamp: Long,
    )

    /**
     * 开始一次计时操作。
     *
     * @param name 计时操作名称
     */
    fun startTimer(name: String) {
        timers[name] = System.nanoTime()
    }

    /**
     * 结束一次计时操作并记录结果。
     *
     * @param name 计时操作名称
     * @param logResult 是否输出日志结果
     * @return 耗时（毫秒），未找到对应开始计时返回-1
     */
    @JvmOverloads
    fun endTimer(name: String, logResult: Boolean = true): Long {
        val startTime = timers.remove(name) ?: return -1L
        val durationNanos = System.nanoTime() - startTime
        val durationMs = durationNanos / 1_000_000L

        results.computeIfAbsent(name) { mutableListOf() }
            .add(Measurement(name, durationNanos, System.currentTimeMillis()))

        if (logResult) {
            Log.d(TAG, "[$name] ${durationMs}ms")
        }

        checkThresholds(name, durationMs)

        return durationMs
    }

    /**
     * 执行一个代码块并自动测量其执行时间。
     *
     * @param name 计时名称
     * @param block 要测量的代码块
     * @return 代码块的返回值和执行时间（毫秒）的Pair
     */
    inline fun <T> measure(name: String, block: () -> T): Pair<T, Long> {
        startTimer(name)
        val result = block()
        val elapsed = endTimer(name)
        return result to elapsed
    }

    /**
     * 使用协程友好的方式测量挂起函数执行时间。
     *
     * @param name 计时名称
     * @param block 要测量的挂起代码块
     * @return 代码块的返回值和执行时间（毫秒）的Pair
     */
    inline suspend fun <T> measureSuspend(
        name: String,
        crossinline block: suspend () -> T,
    ): Pair<T, Long> {
        startTimer(name)
        val result = block()
        val elapsed = endTimer(name)
        return result to elapsed
    }

    /**
     * 获取某个操作的统计摘要。
     *
     * @param name 操作名称
     * @return 包含(平均, 最大, 最小)耗时（毫秒）的Triple，无数据返回null
     */
    fun getStats(name: String): Triple<Long, Long, Long>? {
        val measurements = results[name] ?: return null
        if (measurements.isEmpty()) return null

        var total = 0L
        var minVal = Long.MAX_VALUE
        var maxVal = Long.MIN_VALUE

        for (m in measurements) {
            val ms = m.durationNanos / 1_000_000L
            total += ms
            minVal = min(minVal, ms)
            maxVal = max(maxVal, ms)
        }

        return Triple(total / measurements.size, maxVal, minVal)
    }

    /**
     * 打印所有已收集指标的完整报告到Logcat。
     */
    fun printReport() {
        if (results.isEmpty() && errorCounts.isEmpty()) {
            Log.d(TAG, "No benchmark data collected.")
            return
        }

        Log.i(TAG, "===== Performance Benchmark Report =====")
        for ((name, measurements) in results) {
            val stats = getStats(name) ?: continue
            val (avg, maxVal, minVal) = stats
            val errors = errorCounts[name]?.get() ?: 0
            val successes = successCounts[name]?.get() ?: 0
            val total = errors + successes
            val errorRate = if (total > 0) "errors=$errors (${"%.1f".format(errors.toDouble()/total*100)}%)" else ""
            Log.i(
                TAG,
                "[$name] avg=${avg}ms, max=${maxVal}ms, min=${minVal}ms (n=${measurements.size}) $errorRate"
            )
        }
        // 打印只有错误计数但没有耗时测量的操作
        for ((name, counter) in errorCounts) {
            if (!results.containsKey(name)) {
                val errors = counter.get()
                val successes = successCounts[name]?.get() ?: 0
                Log.i(TAG, "[$name] errors=$errors, successes=$successes")
            }
        }
        Log.i(TAG, "=========================================")
    }

    /**
     * 清除所有已收集的数据。
     */
    fun clear() {
        timers.clear()
        results.clear()
        errorCounts.clear()
        successCounts.clear()
        memorySnapshots.clear()
        regressionLog.clear()
    }

    /**
     * 记录操作成功完成。
     *
     * 与 [recordError] 配合使用可计算操作错误率。
     *
     * @param name 操作名称
     */
    fun recordSuccess(name: String) {
        successCounts.computeIfAbsent(name) { AtomicInteger(0) }.incrementAndGet()
    }

    /**
     * 记录操作失败（错误）。
     *
     * @param name 操作名称
     * @param reason 失败原因（可为null），仅用于日志
     */
    fun recordError(name: String, reason: String? = null) {
        errorCounts.computeIfAbsent(name) { AtomicInteger(0) }.incrementAndGet()
        if (reason != null) {
            Log.w(TAG, "[$name] Error: $reason")
        }
    }

    /**
     * 获取指定操作的错误率。
     *
     * @param name 操作名称
     * @return 错误率（0.0-1.0），无数据时返回 null
     */
    fun getErrorRate(name: String): Double? {
        val errors = errorCounts[name]?.get() ?: 0
        val successes = successCounts[name]?.get() ?: 0
        val total = errors + successes
        if (total == 0) return null
        return errors.toDouble() / total
    }

    /**
     * 获取指定操作的总调用次数。
     *
     * @param name 操作名称
     * @return 成功+失败的总次数
     */
    fun getTotalCount(name: String): Int {
        val errors = errorCounts[name]?.get() ?: 0
        val successes = successCounts[name]?.get() ?: 0
        return errors + successes
    }

    /**
     * 检查是否超过目标阈值并输出警告。
     */
    private fun checkThresholds(name: String, durationMs: Long) {
        val threshold = TARGET_THRESHOLDS[name]
        if (threshold != null && durationMs > threshold) {
            Log.w(
                TAG,
                "[$name] EXCEEDED threshold: ${durationMs}ms > ${threshold}ms"
            )
        }
    }

    /**
     * 预定义的目标阈值（毫秒），用于检测性能回归。
     */
    val TARGET_THRESHOLDS: Map<String, Long> = mapOf(
        "file_open_small" to 500,
        "file_open_medium" to 1000,
        "file_open_large" to 3000,
        "file_save" to 1000,
        "navigation_switch" to 200,
        "tab_switch" to 200,
        "editor_init" to 500,
        "search_in_files" to 3000,
        "diagnostic_analysis" to 1000,
        "markdown_render" to 500,
        "diff_compute" to 2000,
    )

    /**
     * 获取当前设备信息摘要。
     *
     * @param context 应用上下文
     * @return 设备信息字符串
     */
    fun getDeviceInfo(context: android.content.Context): String {
        return buildString {
            append("Build.MODEL=").append(Build.MODEL).append(", ")
            append("SDK_INT=").append(Build.VERSION.SDK_INT).append(", ")
            append("maxRefreshRate=").append(FpsMonitor.getMaxRefreshRate(context)).append("Hz")
        }
    }

    /**
     * 拍摄内存快照并关联标签。
     * 用于跟踪操作前后的内存变化。
     *
     * @param tag 快照标签
     * @return 内存快照对象
     */
    fun snapshotMemory(tag: String): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapMax = runtime.maxMemory()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()

        val snapshot = MemorySnapshot(
            tag = tag,
            heapUsedBytes = heapUsed,
            heapMaxBytes = heapMax,
            nativeHeapBytes = nativeHeap,
            timestamp = System.currentTimeMillis(),
        )

        memorySnapshots.computeIfAbsent(tag) { mutableListOf() }.add(snapshot)
        Log.d(TAG, "[Memory:$tag] heap=${snapshot.heapUsedMB}MB/${snapshot.heapMaxMB}MB, native=${snapshot.nativeHeapMB}MB")
        return snapshot
    }

    /**
     * 比较两个内存快照，返回内存差异（MB）。
     *
     * @param tagBefore 操作前快照标签
     * @param tagAfter 操作后快照标签
     * @return (before, after)快照对
     */
    fun compareMemory(tagBefore: String, tagAfter: String): Pair<MemorySnapshot?, MemorySnapshot?> {
        val before = memorySnapshots[tagBefore]?.lastOrNull()
        val after = memorySnapshots[tagAfter]?.lastOrNull()
        if (before != null && after != null) {
            val heapDiff = after.heapUsedMB - before.heapUsedMB
            val nativeDiff = after.nativeHeapMB - before.nativeHeapMB
            Log.d(TAG, "[Memory:Delta] heap=${"%+.1f".format(heapDiff)}MB, native=${"%+.1f".format(nativeDiff)}MB")
        }
        return before to after
    }

    /**
     * 基准测试文件读取吞吐量。
     *
     * @param file 要读取的文件
     * @param iterations 平均迭代次数
     * @return 读取速度（MB/s）
     */
    fun benchmarkFileRead(file: java.io.File, iterations: Int = 3): Double {
        if (!file.exists()) return -1.0
        val fileSize = file.length()
        var totalBytes = 0L
        val durations = mutableListOf<Long>()

        repeat(iterations) { i ->
            val startNanos = System.nanoTime()
            file.inputStream().use { it.readBytes() }
            val elapsed = System.nanoTime() - startNanos
            durations.add(elapsed)
            totalBytes += fileSize
        }

        val avgDurationNanos = durations.average()
        val throughputBytesPerSec = totalBytes / (avgDurationNanos / 1_000_000_000.0)
        val throughputMBps = throughputBytesPerSec / (1024.0 * 1024.0)

        Log.d(TAG, "[IO:Read] ${file.name}: ${"%.1f".format(throughputMBps)}MB/s (${iterations} iterations)")
        return throughputMBps
    }

    /**
     * 基准测试文件写入吞吐量。
     *
     * @param file 要写入的文件（将被覆盖）
     * @param sizeBytes 每次迭代写入的数据大小
     * @param iterations 平均迭代次数
     * @return 写入速度（MB/s）
     */
    fun benchmarkFileWrite(file: java.io.File, sizeBytes: Int = 1024 * 1024, iterations: Int = 3): Double {
        val data = ByteArray(sizeBytes) { (it % 256).toByte() }
        var totalBytes = 0L
        val durations = mutableListOf<Long>()

        repeat(iterations) {
            val startNanos = System.nanoTime()
            file.outputStream().use { it.write(data) }
            val elapsed = System.nanoTime() - startNanos
            durations.add(elapsed)
            totalBytes += sizeBytes
        }

        val avgDurationNanos = durations.average()
        val throughputBytesPerSec = totalBytes / (avgDurationNanos / 1_000_000_000.0)
        val throughputMBps = throughputBytesPerSec / (1024.0 * 1024.0)

        Log.d(TAG, "[IO:Write] ${file.name}: ${"%.1f".format(throughputMBps)}MB/s (${iterations} iterations)")
        return throughputMBps
    }

    /**
     * 检查测量结果是否相对于基线表示性能回归。
     *
     * @param name 操作名称
     * @param baselineMs 基线（预期）时间（毫秒）
     * @param tolerancePercent 允许的回归容差（默认20%）
     * @return 如果检测到回归返回true
     */
    fun checkRegression(name: String, baselineMs: Long, tolerancePercent: Double = 20.0): Boolean {
        val stats = getStats(name) ?: return false
        val avgMs = stats.first
        if (baselineMs <= 0) return false

        val regressionPercent = ((avgMs - baselineMs).toDouble() / baselineMs) * 100.0
        if (regressionPercent > tolerancePercent) {
            val entry = RegressionEntry(
                name = name,
                currentMs = avgMs,
                baselineMs = baselineMs,
                regressionPercent = regressionPercent,
                timestamp = System.currentTimeMillis(),
            )
            regressionLog.add(entry)
            Log.w(TAG, "[REGRESSION] $name: ${avgMs}ms vs baseline ${baselineMs}ms (+${"%.1f".format(regressionPercent)}%)")
            return true
        }
        return false
    }

    /**
     * 获取所有检测到的性能回归。
     *
     * @return 回归条目列表
     */
    fun getRegressions(): List<RegressionEntry> = regressionLog.toList()

    /**
     * 清除回归日志。
     */
    fun clearRegressions() {
        regressionLog.clear()
    }

    /**
     * 将所有收集的基准测试数据导出为JSON字符串。
     * 用于CI/CD集成和历史追踪。
     *
     * @return JSON格式字符串
     */
    fun exportJson(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"device\": \"${Build.MODEL}\",\n")
        sb.append("  \"sdk\": ${Build.VERSION.SDK_INT},\n")
        sb.append("  \"measurements\": [\n")

        results.entries.forEachIndexed { idx, (name, measurements) ->
            val stats = getStats(name)
            sb.append("    {\n")
            sb.append("      \"name\": \"$name\",\n")
            sb.append("      \"count\": ${measurements.size},\n")
            if (stats != null) {
                sb.append("      \"avgMs\": ${stats.first},\n")
                sb.append("      \"maxMs\": ${stats.second},\n")
                sb.append("      \"minMs\": ${stats.third}\n")
            }
            sb.append("    }${if (idx < results.size - 1) "," else ""}\n")
        }

        sb.append("  ],\n")
        sb.append("  \"regressions\": ${regressionLog.size}\n")
        sb.append("}")
        return sb.toString()
    }
}
