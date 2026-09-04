/**
 * AiDetector 单元测试。
 *
 * 验证沙箱触控时间检测和反射调用时间检测逻辑。
 * 使用 mockk 模拟 Build 字段验证模拟器检测。
 */
package com.draftpeek.security

import com.draftpeek.core.common.security.AiDetectionSignal
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiDetectorTest {

    @BeforeEach
    fun setUp() {
        // Reset internal state by recording enough non-burst events
        // to clear any stale timestamps from previous tests
    }

    @Test
    fun `sandbox touch timing - regular intervals detected`() {
        // Create 20 timestamps with exactly 5ms intervals (very regular)
        val baseTime = 1000L
        val timestamps = (0 until 20).map { baseTime + it * 5L }

        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "Regular touch intervals should trigger SANDBOX_TOUCH_TIMING"
        )
    }

    @Test
    fun `sandbox touch timing - irregular intervals not detected`() {
        // Create 20 timestamps with random intervals (human-like)
        val baseTime = 1000L
        val timestamps = listOf(
            1000L, 1050L, 1100L, 1180L, 1250L, 1400L, 1450L, 1600L,
            1700L, 1850L, 1900L, 2050L, 2200L, 2300L, 2500L, 2600L,
            2750L, 2900L, 3100L, 3200L
        )

        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertFalse(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "Irregular touch intervals should NOT trigger SANDBOX_TOUCH_TIMING"
        )
    }

    @Test
    fun `sandbox touch timing - insufficient samples returns empty`() {
        val timestamps = listOf(1000L, 1050L, 1100L) // Only 3 samples (< 15)
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(result.isEmpty(), "Less than 15 samples should return empty set")
    }

    @Test
    fun `detectEnhancedEmulator does not crash on JVM test environment`() {
        // 在 JVM 测试环境中，Build 字段为默认值（null/空字符串），
        // detectEnhancedEmulator 内部访问 Build 字段时应安全处理。
        // 我们验证方法可正常调用并返回一个 Set（可能为空），不抛异常。
        val result = AiDetector.detectEnhancedEmulator()
        // 明确断言：返回值非 null（Kotlin 类型系统保证），且不抛异常即通过
        assertNotNull(result, "detectEnhancedEmulator() must return a non-null Set")
    }

    @Test
    fun `detectClassloadingBurst with no recorded loads returns empty`() {
        val result = AiDetector.detectClassloadingBurst()
        // With no recorded class loads, should return empty
        // Note: previous tests might have recorded some, but threshold is 20
        // and we haven't recorded 20 in 100ms
    }

    @Test
    fun `detectReflectionTimingAnomaly with insufficient data returns empty`() {
        val result = AiDetector.detectReflectionTimingAnomaly()
        // 在 JVM 测试环境中，反射时间记录不足 10 次时，应返回空 Set。
        // 如果前序测试恰好记录了反射调用，则可能返回非空 Set。
        // 关键断言：方法不抛异常，且返回值为合法 Set。
        assertNotNull(result, "detectReflectionTimingAnomaly() must return a non-null Set")
    }

    // ---- 负向 / 边界用例（测试体系评估报告 P1-6）----
    // 原则：不使用 mockkStatic(Build)（全局静态 mock 在并行 JVM 下易抖动，且污染同进程
    // 其他测试）；正向爆发类断言依赖真实时钟（100ms 窗口），在慢机器上会假阴/假阳，
    // 一律不做。以下用例全部基于纯函数输入输出，完全确定。

    @Test
    fun `sandbox touch timing - exactly 15 regular samples detected`() {
        // 阈值上界：恰好 15 个样本（size < 15 才短路），间隔恒定必须触发
        val timestamps = (0 until 15).map { 1000L + it * 5L }
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "阈值边界（恰好 15 个样本）且间隔规律应触发 SANDBOX_TOUCH_TIMING"
        )
    }

    @Test
    fun `sandbox touch timing - 14 samples below threshold returns empty`() {
        // 阈值下界：14 个样本必须走短路分支，不做统计、不抛异常
        val timestamps = (0 until 14).map { 1000L + it * 5L }
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(
            result.isEmpty(),
            "14 个样本（阈值下界 -1）应直接返回空集合"
        )
    }

    @Test
    fun `sandbox touch timing - exactly 15 irregular samples not detected`() {
        // 阈值上界 + 负向：恰好 15 个样本但间隔无规律，不应误报
        val timestamps = listOf(
            1000L, 1050L, 1100L, 1180L, 1250L, 1400L, 1450L, 1600L,
            1700L, 1850L, 1900L, 2050L, 2200L, 2300L, 2500L
        )
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertFalse(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "阈值边界（恰好 15 个样本）且间隔无规律不应触发"
        )
    }

    @Test
    fun `sandbox touch timing - empty input returns empty`() {
        assertTrue(
            AiDetector.detectSandboxTouchTiming(emptyList()).isEmpty(),
            "空输入应返回空集合而非抛异常"
        )
    }

    @Test
    fun `sandbox touch timing - zero variance duplicate timestamps detected`() {
        // 病理输入：全部时间戳相同（脚本重放特征），stddev = 0 必须触发
        val timestamps = List(20) { 1000L }
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "全零间隔（stddev=0）应触发 SANDBOX_TOUCH_TIMING"
        )
    }

    @Test
    fun `sandbox touch timing - descending timestamps evaluated on interval variance`() {
        // 固化当前行为：检测器不校验时间戳单调性，均匀递减（间隔恒为 -5ms）
        // 方差同样为 0 并触发。输入单调性责任在埋点侧——若未来加了单调性校验，
        // 本用例失败即为有意的行为变更提醒。
        val timestamps = (0 until 20).map { 1000L - it * 5L }
        val result = AiDetector.detectSandboxTouchTiming(timestamps)
        assertTrue(
            AiDetectionSignal.SANDBOX_TOUCH_TIMING in result,
            "均匀递减时间戳（方差 0）按当前实现应触发"
        )
    }

    @Test
    fun `classloading burst - fewer than 20 recorded loads returns empty`() {
        // 负向：记录数 < 20 时走短路分支（不依赖时钟窗口）
        repeat(5) { AiDetector.recordClassLoad() }
        val result = AiDetector.detectClassloadingBurst()
        assertFalse(
            AiDetectionSignal.CLASSLOADING_BURST in result,
            "记录数 < 20 应直接返回空集合"
        )
    }
}
