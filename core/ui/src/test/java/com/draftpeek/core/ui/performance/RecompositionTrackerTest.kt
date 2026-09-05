/**
 * [RecompositionTracker] 单元测试。
 *
 * 只覆盖**纯 Kotlin 的注册表逻辑**（记账 / 聚合 / 排序 / 清零 / 全局开关）。
 * [trackRecomposition] 依赖 Compose 运行时，其行为由
 * `core/ui/src/androidTest/.../RecompositionCounterBehaviorTest.kt` 在真机/模拟器上验证。
 *
 * 运行方式：./gradlew :core:ui:testDebugUnitTest --tests "*RecompositionTrackerTest"
 *
 * 注意：本模块同时存在 Paparazzi 截图测试（JUnit4），因此本文件也使用 JUnit4，
 * 不加 `useJUnitPlatform()` —— 否则 JUnit Platform 会在没有 vintage 引擎的情况下
 * 静默跳过全部 JUnit4 截图用例（详见 docs/agents/GOTCHAS.md）。
 */
package com.draftpeek.core.ui.performance

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 重组计数注册表行为验证。
 */
class RecompositionTrackerTest {

    @After
    fun tearDown() {
        RecompositionTracker.resetAll()
    }

    @Test
    fun record_accumulatesPerTag() {
        RecompositionTracker.record("a")
        RecompositionTracker.record("a")
        RecompositionTracker.record("b")

        assertEquals(2L, RecompositionTracker.countOf("a"))
        assertEquals(1L, RecompositionTracker.countOf("b"))
    }

    @Test
    fun record_honoursDelta() {
        RecompositionTracker.record("a", delta = 5)

        assertEquals(5L, RecompositionTracker.countOf("a"))
    }

    @Test
    fun countOf_returnsZeroForUnknownTag() {
        assertEquals(0L, RecompositionTracker.countOf("never-recorded"))
    }

    @Test
    fun record_returnsUpdatedTotal() {
        assertEquals(1L, RecompositionTracker.record("a"))
        assertEquals(2L, RecompositionTracker.record("a"))
    }

    @Test
    fun globalSwitch_disabledShortCircuitsWithoutCreatingEntry() {
        RecompositionTracker.isGloballyEnabled = false

        assertEquals(0L, RecompositionTracker.record("a"))
        assertEquals(0L, RecompositionTracker.countOf("a"))
        assertTrue(RecompositionTracker.snapshot().isEmpty())
    }

    @Test
    fun globalSwitch_canBeTurnedBackOn() {
        RecompositionTracker.isGloballyEnabled = false
        RecompositionTracker.record("a")
        RecompositionTracker.isGloballyEnabled = true
        RecompositionTracker.record("a")

        assertEquals(1L, RecompositionTracker.countOf("a"))
    }

    @Test
    fun snapshot_isIndependentCopy() {
        RecompositionTracker.record("a")
        val snapshot = RecompositionTracker.snapshot()
        RecompositionTracker.record("a")

        assertEquals(1L, snapshot["a"])
        assertEquals(2L, RecompositionTracker.countOf("a"))
    }

    @Test
    fun report_sortsByCountDescending() {
        RecompositionTracker.record("slow", delta = 50)
        RecompositionTracker.record("medium", delta = 10)
        RecompositionTracker.record("fast", delta = 1)

        val report = RecompositionTracker.report()

        assertEquals(listOf("slow", "medium", "fast"), report.map { it.tag })
        assertEquals(listOf(50L, 10L, 1L), report.map { it.count })
    }

    @Test
    fun report_breaksTiesByTagName() {
        RecompositionTracker.record("zeta")
        RecompositionTracker.record("alpha")

        assertEquals(listOf("alpha", "zeta"), RecompositionTracker.report().map { it.tag })
    }

    @Test
    fun report_respectsLimit() {
        RecompositionTracker.record("a", delta = 3)
        RecompositionTracker.record("b", delta = 2)
        RecompositionTracker.record("c", delta = 1)

        assertEquals(2, RecompositionTracker.report(limit = 2).size)
    }

    @Test
    fun report_negativeLimitReturnsEmpty() {
        RecompositionTracker.record("a")

        assertTrue(RecompositionTracker.report(limit = -1).isEmpty())
    }

    @Test
    fun dump_reportsTagCountAndTotal() {
        RecompositionTracker.record("Editor:toolbar", delta = 3)
        RecompositionTracker.record("Editor:body", delta = 7)

        val dumped = RecompositionTracker.dump()

        assertTrue(dumped.contains("2 tags"))
        assertTrue(dumped.contains("10 total"))
        assertTrue(dumped.contains("Editor:toolbar"))
    }

    @Test
    fun dump_emptyTrackerIsExplicit() {
        assertEquals("Recomposition report: <empty>", RecompositionTracker.dump())
    }

    @Test
    fun reset_clearsEverything() {
        RecompositionTracker.record("a")
        RecompositionTracker.record("b")
        RecompositionTracker.reset()

        assertTrue(RecompositionTracker.snapshot().isEmpty())
    }

    @Test
    fun reset_singleTagKeepsOthers() {
        RecompositionTracker.record("a")
        RecompositionTracker.record("b")
        RecompositionTracker.reset("a")

        assertEquals(0L, RecompositionTracker.countOf("a"))
        assertEquals(1L, RecompositionTracker.countOf("b"))
    }

    @Test
    fun reset_isIdempotentForUnknownTag() {
        RecompositionTracker.reset("never-recorded")

        assertTrue(RecompositionTracker.snapshot().isEmpty())
    }

    @Test
    fun concurrentRecordingDoesNotLoseIncrements() {
        val threads = (1..8).map {
            Thread {
                repeat(200) { RecompositionTracker.record("hot") }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(8 * 200L, RecompositionTracker.countOf("hot"))
    }

    @Test
    fun defaultState_trackingGloballyEnabled() {
        assertTrue(RecompositionTracker.isGloballyEnabled)
    }
}
