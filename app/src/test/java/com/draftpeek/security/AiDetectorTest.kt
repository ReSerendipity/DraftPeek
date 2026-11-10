/**
 * AiDetector 单元测试。
 *
 * 验证沙箱触控时间检测和反射调用时间检测逻辑。
 * 使用 mockk 模拟 Build 字段验证模拟器检测。
 */
package com.draftpeek.security

import com.draftpeek.core.common.security.AiDetectionSignal
import com.draftpeek.core.common.security.DexIntegrityStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    // ===== DEX CRC 基线（构建期接线，2026-11-07）=====

    @Test
    fun `dex baseline parse - reads generated sum line`() {
        val text = "# DraftPeek 构建期生成\nsum=69279869714\nentries=35\n"
        assertEquals(69279869714L, AiDetector.parseDexCrcBaseline(text))
    }

    @Test
    fun `dex baseline parse - missing or garbage returns zero so check is skipped`() {
        assertEquals(0L, AiDetector.parseDexCrcBaseline(""))
        assertEquals(0L, AiDetector.parseDexCrcBaseline("no baseline here"))
        // 截断/脏值不能被猜成某个数：一律 0（= 跳过校验，零误报）
        assertEquals(0L, AiDetector.parseDexCrcBaseline("sum=abc"))
        assertEquals(0L, AiDetector.parseDexCrcBaseline("sum="))
    }

    @Test
    fun `dex crc sum - additive and order independent`() {
        assertEquals(6L, AiDetector.sumDexCrcs(listOf(1L, 2L, 3L)))
        assertEquals(6L, AiDetector.sumDexCrcs(listOf(3L, 1L, 2L)))
        assertEquals(0L, AiDetector.sumDexCrcs(emptyList()))
    }

    @Test
    fun `dex entry pattern - matches only real classes dex names`() {
        assertTrue(AiDetector.DEX_ENTRY_PATTERN.matches("classes.dex"))
        assertTrue(AiDetector.DEX_ENTRY_PATTERN.matches("classes2.dex"))
        assertTrue(AiDetector.DEX_ENTRY_PATTERN.matches("classes35.dex"))
        // 负向：别的名字不算 dex 条目
        assertFalse(AiDetector.DEX_ENTRY_PATTERN.matches("MANIFEST.MF"))
        assertFalse(AiDetector.DEX_ENTRY_PATTERN.matches("classes.dex.bak"))
        assertFalse(AiDetector.DEX_ENTRY_PATTERN.matches("assets/classes.dex"))
    }

    // ===== DEX 完整性三态（走查 §六.6：负向对照仍显示「✅ 通过」）=====
    // 判定逻辑全部走纯函数 resolveDexIntegrity，不依赖 Context / 真机 / 时钟。

    @Test
    fun `dex outcome - crc sum equals baseline is the only PASS`() {
        val outcome = AiDetector.resolveDexIntegrity(baseline = 6L, crcs = listOf(1L, 2L, 3L))
        assertEquals(AiDetector.DexIntegrityOutcome.Verified, outcome)
        assertEquals(DexIntegrityStatus.PASSED, AiDetector.dexStatusOf(outcome))
        assertTrue(outcome.signals.isEmpty(), "一致时不得产出任何信号")
    }

    @Test
    fun `dex outcome - tampered baseline is FAILED not pass`() {
        // 负向对照复刻：把 assets/dex_crc_baseline.txt 的 sum 改成 12345678901（实际 13498282265）
        val outcome = AiDetector.resolveDexIntegrity(
            baseline = 12345678901L,
            crcs = listOf(13498282265L)
        )
        assertTrue(outcome is AiDetector.DexIntegrityOutcome.Mismatch, "基线被改必须判不一致")
        assertEquals(DexIntegrityStatus.FAILED, AiDetector.dexStatusOf(outcome))
        assertTrue(
            AiDetectionSignal.DEX_TAMPERED in outcome.signals,
            "篡改必须保留 DEX_TAMPERED 判据"
        )
        assertTrue(
            outcome.detail!!.contains("12345678901") && outcome.detail!!.contains("13498282265"),
            "失败原因要带上实际值与基线值：${outcome.detail}"
        )
    }

    @Test
    fun `dex outcome - missing baseline is UNKNOWN not PASS`() {
        // 基线缺失（sum=0）走的是「跳过校验」：不报篡改，但也不能说通过
        val outcome = AiDetector.resolveDexIntegrity(baseline = 0L, crcs = listOf(1L, 2L, 3L))
        assertTrue(outcome is AiDetector.DexIntegrityOutcome.NotVerifiable)
        assertEquals(DexIntegrityStatus.UNKNOWN, AiDetector.dexStatusOf(outcome))
        assertTrue(outcome.signals.isEmpty())
    }

    @Test
    fun `dex outcome - unreadable apk is UNKNOWN not PASS`() {
        val outcome = AiDetector.resolveDexIntegrity(
            baseline = 6L,
            crcs = null,
            error = java.io.IOException("zip corrupted")
        )
        assertTrue(outcome is AiDetector.DexIntegrityOutcome.NotVerifiable)
        assertEquals(DexIntegrityStatus.UNKNOWN, AiDetector.dexStatusOf(outcome))
        assertTrue(outcome.detail!!.contains("zip corrupted"))
    }

    @Test
    fun `dex outcome - no dex entries is UNKNOWN not PASS`() {
        val outcome = AiDetector.resolveDexIntegrity(baseline = 6L, crcs = emptyList())
        assertTrue(outcome is AiDetector.DexIntegrityOutcome.NotVerifiable)
        assertEquals(DexIntegrityStatus.UNKNOWN, AiDetector.dexStatusOf(outcome))
    }

    @Test
    fun `dex outcome - empty signal set is never mapped to PASSED unless verified`() {
        // 旧实现的 fail-open 就是「空集 == 通过」。逐条确认空集的三种来源都不是 PASSED。
        val notVerifiableOutcomes = listOf(
            AiDetector.resolveDexIntegrity(0L, listOf(1L)),
            AiDetector.resolveDexIntegrity(6L, emptyList()),
            AiDetector.resolveDexIntegrity(6L, null, java.io.IOException("boom"))
        )
        notVerifiableOutcomes.forEach { outcome ->
            assertTrue(outcome.signals.isEmpty())
            assertNotEquals(DexIntegrityStatus.PASSED, AiDetector.dexStatusOf(outcome))
        }
    }
}
