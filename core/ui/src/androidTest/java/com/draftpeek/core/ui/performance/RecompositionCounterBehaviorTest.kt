/**
 * [trackRecomposition] 行为测试（Compose UI Test）。
 *
 * 验证重组计数在**真实 Compose 运行时**下的语义，这是 JVM 单元测试覆盖不到的部分：
 * - 关闭开关时零记账（埋点可安全常驻）；
 * - 首次组合记为 1，随后每次状态驱动的重组 +1；
 * - 返回值是「本次组合之前」的累计值，因此读取它不会引发额外重组；
 * - tag 变化会启用新的计数器，且全局注册表按 tag 分别聚合。
 *
 * 对应评估报告 P1③（重组次数监控）。
 *
 * 运行方式（需 Android 设备 / 模拟器）：
 * ./gradlew :core:ui:connectedDebugAndroidTest
 * 仅编译校验：./gradlew :core:ui:compileDebugAndroidTestKotlin
 */
package com.draftpeek.core.ui.performance

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Collections

/**
 * 重组计数埋点的运行时行为验证。
 */
class RecompositionCounterBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * 每次组合时由被测 Composable 写入的观测值（组合期调用，非 Compose State，无自激风险）。
     */
    private lateinit var observed: MutableList<Int>

    @Before
    fun setUp() {
        observed = Collections.synchronizedList(mutableListOf())
        RecompositionTracker.reset()
        RecompositionTracker.isGloballyEnabled = true
    }

    @After
    fun tearDown() {
        RecompositionTracker.reset()
        RecompositionTracker.isGloballyEnabled = true
    }

    @Test
    fun trackingDisabled_recordsNothing() {
        val trigger = mutableStateOf(0)

        composeRule.setContent {
            Probe(tag = "disabled", trigger = trigger.value, sink = observed)
        }
        composeRule.waitForIdle()
        trigger.value = 1
        composeRule.waitForIdle()
        trigger.value = 2
        composeRule.waitForIdle()

        assertEquals(3, observed.size)
        assertEquals(listOf(0, 0, 0), observed.toList())
        assertEquals(0L, RecompositionTracker.countOf("disabled"))
    }

    @Test
    fun trackingEnabled_countsInitialCompositionOnce() {
        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = "enabled", trigger = 0, sink = observed)
            }
        }
        composeRule.waitForIdle()

        // 首次组合：SideEffect 自增一次 → 全局计数 1；返回值是自增前的值 → 0
        assertEquals(listOf(0), observed.toList())
        assertEquals(1L, RecompositionTracker.countOf("enabled"))
    }

    @Test
    fun trackingEnabled_countsEveryStateDrivenRecomposition() {
        val trigger = mutableStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = "enabled", trigger = trigger.value, sink = observed)
            }
        }
        composeRule.waitForIdle()
        trigger.value = 1
        composeRule.waitForIdle()
        trigger.value = 2
        composeRule.waitForIdle()

        assertEquals(listOf(0, 1, 2), observed.toList())
        assertEquals(3L, RecompositionTracker.countOf("enabled"))
    }

    @Test
    fun trackingEnabled_readingReturnValueDoesNotCauseExtraRecomposition() {
        val trigger = mutableStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = "no-self-excite", trigger = trigger.value, sink = observed)
            }
        }
        composeRule.waitForIdle()
        // 静置：若返回值的读取会写入 Compose State，此处将出现持续自增（自激重组）
        Thread.sleep(300)
        composeRule.waitForIdle()

        assertEquals(1L, RecompositionTracker.countOf("no-self-excite"))
        assertEquals(1, observed.size)
    }

    @Test
    fun tagChange_startsFreshCounterAndSeparatesAggregation() {
        val tag = mutableStateOf("first")

        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = tag.value, trigger = 0, sink = observed)
            }
        }
        composeRule.waitForIdle()
        tag.value = "second"
        composeRule.waitForIdle()

        assertEquals(1L, RecompositionTracker.countOf("first"))
        assertEquals(1L, RecompositionTracker.countOf("second"))
        // 换 tag 后 remember 重建 → 计数器归零重新开始
        assertEquals(listOf(0, 0), observed.toList())
    }

    @Test
    fun globalSwitch_disabledSuppressesEvenLocallyEnabledTracking() {
        RecompositionTracker.isGloballyEnabled = false
        val trigger = mutableStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = "global-off", trigger = trigger.value, sink = observed)
            }
        }
        composeRule.waitForIdle()
        trigger.value = 1
        composeRule.waitForIdle()

        assertEquals(0L, RecompositionTracker.countOf("global-off"))
        assertEquals(listOf(0, 0), observed.toList())
    }

    @Test
    fun report_aggregatesAcrossCompositionScopes() {
        composeRule.setContent {
            CompositionLocalProvider(LocalRecompositionTrackingEnabled provides true) {
                Probe(tag = "shared", trigger = 0, sink = observed)
                Probe(tag = "shared", trigger = 0, sink = observed)
            }
        }
        composeRule.waitForIdle()

        assertEquals(2L, RecompositionTracker.countOf("shared"))
        assertEquals(1, RecompositionTracker.report().size)
    }

    /**
     * 被测探针：读取 [trigger] 以建立重组依赖，把 [trackRecomposition] 的返回值写入 [sink]。
     *
     * [sink] 是普通可变列表而非 Compose State，因此组合期写入不会触发新的重组。
     */
    @Composable
    private fun Probe(
        tag: String,
        trigger: Int,
        sink: MutableList<Int>
    ) {
        sink.add(trackRecomposition(tag))
        Text("trigger=$trigger")
    }
}
