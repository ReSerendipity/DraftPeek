package com.draftpeek.feature.editor.ui

import com.draftpeek.feature.editor.sora.SoraEditorWrapper
import io.github.rosemoe.sora.widget.CodeEditor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [SoraEditorBridge] 的回归测试（纯 JVM，无需 Android 设备/模拟器）。
 *
 * 覆盖评估报告 P0② 要求的两类契约：
 * 1. **滚动阈值判定**：split / preview / 单栏三个槽位共用同一阈值逻辑，必须保持一致；
 * 2. **update 轻量化**：主题与焦点下发必须去重，避免每次重组都触发昂贵的重新着色
 *    或反复弹起软键盘。
 *
 * 之所以把这些逻辑抽成顶层函数与 [SoraBridgeState]，正是为了让它们能脱离
 * instrumentation 环境被验证——[SoraEditorBridge] 的组合体本身仍需
 * `connectedAndroidTest` 覆盖。
 */
@DisplayName("SoraEditorBridge")
class SoraEditorBridgeTest {

    @Nested
    @DisplayName("isScrolledPastThreshold() — 阈值边界")
    inner class ThresholdLogicTests {

        @Test
        @DisplayName("首行可见行 <= 5 时判定为未滚过阈值")
        fun atOrBelowThreshold_returnsFalse() {
            assertFalse(isScrolledPastThreshold(firstVisibleLine = 0))
            assertFalse(isScrolledPastThreshold(firstVisibleLine = 4))
            assertFalse(isScrolledPastThreshold(firstVisibleLine = 5))
        }

        @Test
        @DisplayName("首行可见行 > 5 时判定为已滚过阈值")
        fun aboveThreshold_returnsTrue() {
            assertTrue(isScrolledPastThreshold(firstVisibleLine = 6))
            assertTrue(isScrolledPastThreshold(firstVisibleLine = 500))
        }

        @Test
        @DisplayName("编辑器不可用时恒为 false（不因滚动位置误判）")
        fun unusableEditor_alwaysFalse() {
            assertFalse(isScrolledPastThreshold(firstVisibleLine = 100, isUsable = false))
        }

        @Test
        @DisplayName("阈值常量与报告一致（> 5 行）")
        fun thresholdConstant_isFive() {
            assertEquals(5, SCROLL_THRESHOLD_LINE)
        }
    }

    @Nested
    @DisplayName("computeScrollPastThreshold() — 包装器集成与异常降级")
    inner class WrapperIntegrationTests {

        @Test
        @DisplayName("编辑器不可用时返回 false")
        fun unusableWrapper_returnsFalse() {
            val wrapper = mockk<SoraEditorWrapper>(relaxed = true)
            every { wrapper.isUsable() } returns false

            assertFalse(computeScrollPastThreshold(wrapper))
        }

        @Test
        @DisplayName("读取 firstVisibleLine 抛异常时降级为 false，不崩溃")
        fun exceptionWhileReading_degradesToFalse() {
            val editor = mockk<CodeEditor>(relaxed = true)
            every { editor.firstVisibleLine } throws IllegalStateException("editor detached")
            val wrapper = mockk<SoraEditorWrapper>(relaxed = true)
            every { wrapper.isUsable() } returns true
            every { wrapper.editor } returns editor

            assertFalse(computeScrollPastThreshold(wrapper))
        }

        @Test
        @DisplayName("可用编辑器按 firstVisibleLine 正确判定")
        fun usableWrapper_readsFirstVisibleLine() {
            val editor = mockk<CodeEditor>(relaxed = true)
            every { editor.firstVisibleLine } returns 42
            val wrapper = mockk<SoraEditorWrapper>(relaxed = true)
            every { wrapper.isUsable() } returns true
            every { wrapper.editor } returns editor

            assertTrue(computeScrollPastThreshold(wrapper))
        }
    }

    @Nested
    @DisplayName("SoraBridgeState — update 轻量化去重")
    inner class BridgeStateTests {

        @Test
        @DisplayName("首次应用主题返回 true，同值重复调用返回 false")
        fun theme_onlyAppliedOnChange() {
            val state = SoraBridgeState()

            assertTrue(state.shouldApplyTheme(true), "首次下发暗色主题应返回 true")
            assertFalse(state.shouldApplyTheme(true), "暗色值未变化不应重复下发")
            assertFalse(state.shouldApplyTheme(true), "连续第三次仍不应重复下发")

            assertTrue(state.shouldApplyTheme(false), "切回亮色应重新下发")
            assertFalse(state.shouldApplyTheme(false), "亮色值未变化不应重复下发")
            assertTrue(state.shouldApplyTheme(true), "再次切到暗色应重新下发")
        }

        @Test
        @DisplayName("焦点请求为边沿触发：false→true 触发一次，持续 true 不再触发")
        fun focus_isEdgeTriggered() {
            val state = SoraBridgeState()

            assertFalse(state.shouldRequestFocus(false), "未开启焦点请求时不触发")
            assertTrue(state.shouldRequestFocus(true), "false→true 应触发一次")
            assertFalse(state.shouldRequestFocus(true), "持续 true 不应反复弹键盘")
            assertFalse(state.shouldRequestFocus(false), "true→false 不应触发")
            assertTrue(state.shouldRequestFocus(true), "再次 true 应重新触发")
        }

        @Test
        @DisplayName("reset 后状态回到未同步，强制下一次重新下发")
        fun reset_forcesResync() {
            val state = SoraBridgeState()
            state.shouldApplyTheme(true)
            state.shouldRequestFocus(true)
            assertFalse(state.shouldApplyTheme(true))
            assertFalse(state.shouldRequestFocus(true))

            state.reset()

            assertTrue(state.shouldApplyTheme(true), "reset 后应重新下发主题")
            assertTrue(state.shouldRequestFocus(true), "reset 后应重新请求焦点")
        }
    }

    @Nested
    @DisplayName("槽位标识")
    inner class SlotLabelTests {

        @Test
        @DisplayName("三个槽位标识互不重复，便于日志定位")
        fun slotLabels_areDistinct() {
            val labels = setOf(SORA_SLOT_EDITOR, SORA_SLOT_SPLIT, SORA_SLOT_SPLIT_STREAM)
            assertEquals(3, labels.size, "槽位标识必须唯一，否则无法从日志区分挂载来源")
        }
    }
}
