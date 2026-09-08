/**
 * 重组（Recomposition）次数统计工具。
 *
 * 解决评估报告 P1③「无重组次数监控」：Jetpack Compose 的重组是隐式的，
 * 过度重组（同一 Composable 被无谓地反复执行）是 Compose 性能退化的首要成因，
 * 但在 Layout Inspector 之外没有可量化的运行时指标。
 *
 * 本文件提供两层能力：
 * 1. [trackRecomposition] —— Composable 内一行接入，统计该位置的重组次数；
 * 2. [RecompositionTracker] —— 全局注册表，按 tag 聚合并可导出报告 / 清零。
 *
 * ## 设计取舍：为什么不用 `mutableStateOf` 计数
 * 若在 Composable 内 `remember { mutableStateOf(0) }` 并在 `SideEffect` 中自增，
 * 该 state 会在同一次组合内被读取 → 写入立即失效读取作用域 → 触发新的重组 →
 * 再次自增，形成**自激无限重组**。故计数器使用普通类 [RecompositionCounter]
 * （非 Compose State），写它不会让任何快照失效，因此**零自激风险**。
 *
 * ## 开销
 * [LocalRecompositionTrackingEnabled] 默认 `false`。关闭时 [trackRecomposition]
 * 在首次组合即 `return 0`，不注册 SideEffect、不查表，运行时开销为零，
 * 因此可以安全地把埋点常驻在代码里，仅在需要排查时打开开关。
 */
package com.draftpeek.core.ui.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 重组统计开关（CompositionLocal 粒度）。
 *
 * 默认 `false`。在需要排查的界面局部提供 `true` 即可开启：
 * ```
 * CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
 *     EditorScreen(...)
 * }
 * ```
 */
val LocalRecompositionTrackingEnabled = staticCompositionLocalOf { false }

/**
 * 单次组合位置的重组计数器。
 *
 * **故意不实现为 Compose State** —— 详见文件头「设计取舍」。
 *
 * @property count 已发生的组合次数，首次组合记为 1
 */
class RecompositionCounter internal constructor() {

    private var value = 0

    /** 已发生的组合次数（含首次组合）。 */
    val count: Int
        get() = value

    /**
     * 自增并返回新值。
     *
     * 仅在 [SideEffect] 中调用，因此「一次成功的组合 / 重组」恰好对应一次自增。
     */
    internal fun increment(): Int = ++value
}

/**
 * 重组计数全局注册表。
 *
 * 线程安全（[ConcurrentHashMap] + [AtomicLong]），且为**纯 Kotlin 实现**，
 * 不依赖 Android 框架，因此可直接在 JVM 单元测试中验证（见 RecompositionTrackerTest）。
 */
object RecompositionTracker {

    private val globalEnabled = AtomicBoolean(true)
    private val counts = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 全局总开关。
     *
     * 与 [LocalRecompositionTrackingEnabled] 是**与**的关系：两者都开启才记账。
     * 建议在 Release 构建的应用入口显式置 `false`，避免任何残留埋点产生开销。
     */
    var isGloballyEnabled: Boolean
        get() = globalEnabled.get()
        set(value) = globalEnabled.set(value)

    /**
     * 为 [tag] 累加 [delta] 次重组。
     *
     * @param tag 埋点标识，建议使用「文件名:行号」或语义名（如 `EditorScreen:toolbar`）
     * @param delta 增量，默认 1
     * @return 记账后的累计次数；全局开关关闭时返回 0 且不创建条目
     */
    fun record(tag: String, delta: Long = 1): Long {
        if (!globalEnabled.get()) return 0L
        val counter = counts.computeIfAbsent(tag) { AtomicLong(0L) }
        return counter.addAndGet(delta)
    }

    /**
     * 查询 [tag] 的累计重组次数，未记账过返回 0。
     */
    fun countOf(tag: String): Long = counts[tag]?.get() ?: 0L

    /**
     * 导出当前全部计数快照。
     *
     * @return tag 到累计次数的不可变映射
     */
    fun snapshot(): Map<String, Long> = counts.entries.associate { it.key to it.value.get() }

    /**
     * 导出按重组次数降序排列的报告，便于直接落到看板或 CI 产物。
     *
     * @param limit 最多返回的条目数，默认全部
     * @return 次数降序的 [RecompositionEntry] 列表
     */
    fun report(limit: Int = Int.MAX_VALUE): List<RecompositionEntry> = snapshot()
        .map { (tag, count) -> RecompositionEntry(tag, count) }
        .sortedWith(compareByDescending<RecompositionEntry> { it.count }.thenBy { it.tag })
        .take(limit.coerceAtLeast(0))

    /**
     * 生成人类可读的多行报告，用于 logcat / `adb shell` 现场排查。
     *
     * 输出示例：
     * ```
     * Recomposition report (2 tags, 156 total)
     *   120  EditorScreen:toolbar
     *    36  EditorScreen:content
     * ```
     */
    fun dump(limit: Int = 20): String {
        val entries = report(limit)
        if (entries.isEmpty()) return "Recomposition report: <empty>"
        val total = entries.sumOf { it.count }
        val body = entries.joinToString(separator = "\n") { "  ${it.count}\t${it.tag}" }
        return "Recomposition report (${entries.size} tags, $total total)\n$body"
    }

    /** 清零全部计数。测试之间必须调用，否则用例相互污染。 */
    fun reset() {
        counts.clear()
    }

    /**
     * 仅清零指定 [tag]。
     *
     * 用于「先跑 N 帧预热 → 清零 → 再测 M 帧」这类需要排除首次组合噪声的场景。
     */
    fun reset(tag: String) {
        counts.remove(tag)
    }

    /** 置回初始状态（计数清零 + 全局开关恢复默认）。仅供测试使用。 */
    internal fun resetAll() {
        counts.clear()
        globalEnabled.set(true)
    }

    /**
     * 报告条目。
     *
     * @property tag 埋点标识
     * @property count 累计重组次数
     */
    data class RecompositionEntry(val tag: String, val count: Long)
}

/**
 * 统计当前组合位置的重组次数，并同步记入全局 [RecompositionTracker]。
 *
 * 典型用法（埋点常驻，靠开关控制是否生效）：
 * ```
 * @Composable
 * fun EditorToolbar(...) {
 *     trackRecomposition("EditorScreen:toolbar")
 *     ...
 * }
 * ```
 *
 * 排查时在界面外层打开开关，操作若干次后取报告：
 * ```
 * Log.d("Perf", RecompositionTracker.dump())
 * ```
 *
 * 若一次用户操作后某 tag 的计数远大于预期（例如点一下按钮增长几十次），
 * 即说明该处存在过度重组，优先检查：State 读取位置过深、不稳定的 lambda、
 * 未加 `key` 的 LazyColumn 项、以及每次重组都新建的对象参数。
 *
 * @param tag 埋点标识，同一 tag 在全局聚合
 * @param enabled 是否启用，默认取 [LocalRecompositionTrackingEnabled]
 * @return **本次组合之前**的累计次数（返回值滞后 1 是刻意的：读取发生在自增之前，
 *   这样调用方即使直接渲染该值也不会引发额外重组）
 */
@Composable
fun trackRecomposition(tag: String, enabled: Boolean = LocalRecompositionTrackingEnabled.current): Int {
    if (!enabled) return 0
    val counter = remember(tag) { RecompositionCounter() }
    SideEffect {
        counter.increment()
        RecompositionTracker.record(tag)
    }
    return counter.count
}
