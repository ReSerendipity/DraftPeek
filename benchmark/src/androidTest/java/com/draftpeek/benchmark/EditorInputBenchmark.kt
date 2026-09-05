/**
 * 编辑器输入帧率基准测试（Macrobenchmark）。
 *
 * 测量**连续键入过程中的帧渲染耗时**，对应评估报告 P1③「Macrobenchmark 缺编辑器输入指标」。
 *
 * 为什么必须测这一项：编辑器是本应用唯一的高频重组 + 高亮重算 + IME 交互叠加场景，
 * 启动基准（[StartupBenchmark]）与文件 IO 基准（[FileReadBenchmark]）都覆盖不到。
 * 键入卡顿是代码编辑器最影响体感的性能问题，而它此前没有任何量化基线。
 *
 * 指标：[FrameTimingMetric] —— 输出 P50/P90/P95/P99 帧耗时，
 * 用于判定「输入时掉帧率」是否劣化（阈值建议：P99 < 32ms，即不连续丢两帧）。
 *
 * 运行方式（需真机或模拟器，夜间档 / 手动 dispatch，不阻塞 PR）：
 * ./gradlew :benchmark:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.draftpeek.benchmark.EditorInputBenchmark
 *
 * 前置条件见 [EditorBenchmarkSupport] 的文件头注释。
 */
package com.draftpeek.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 编辑器键入帧率基准。
 */
@RunWith(AndroidJUnit4::class)
class EditorInputBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 连续键入一整行 Kotlin 代码，测量期间的帧耗时分布。
     *
     * 使用 WARM 启动：冷启动的类加载与 DEX 校验耗时会淹没键入本身的帧数据。
     * 每轮迭代都会重新拉起 Activity 并重新聚焦编辑器，因此迭代之间互不影响。
     */
    @Test
    fun typingFrameTiming() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.ensureEditorOpen()
            device.requireEditor().click()
            device.waitForIdle()
        }
    ) {
        sendText(INPUT_BURST)
        device.waitForIdle()
    }
}
