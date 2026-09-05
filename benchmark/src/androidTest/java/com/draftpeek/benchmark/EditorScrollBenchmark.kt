/**
 * 编辑器滚动帧率基准测试（Macrobenchmark）。
 *
 * 测量**快速甩动滚动（fling）过程中的帧渲染耗时**，对应评估报告 P1③
 * 「Macrobenchmark 缺编辑器滚动指标」。
 *
 * 与 [EditorInputBenchmark] 的分工：
 * - 输入基准覆盖「IME 输入 → 文本变更 → 语法高亮重算 → 重组」链路；
 * - 滚动基准覆盖「手势 → 行号/当前行重绘 → 大文本布局复用」链路。
 * 两者卡顿的成因不同，必须分别量化。
 *
 * 指标：[FrameTimingMetric]。滚动场景的验收建议：P95 帧耗时 < 16.7ms（60Hz 单帧预算），
 * 若 P95 持续劣化说明滚动路径上出现了每帧的重复计算。
 *
 * 运行方式（需真机或模拟器，夜间档 / 手动 dispatch，不阻塞 PR）：
 * ./gradlew :benchmark:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.draftpeek.benchmark.EditorScrollBenchmark
 *
 * 前置条件见 [EditorBenchmarkSupport] 的文件头注释。
 */
package com.draftpeek.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 编辑器滚动帧率基准。
 */
@RunWith(AndroidJUnit4::class)
class EditorScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 自上而下快速甩动一次，测量期间的帧耗时分布。
     *
     * setupBlock 每轮都重新灌入文档：滚动会把视口带到文末，
     * 若不重新投喂，后续迭代将在文末原地滚动，测不到真实的滚动路径。
     */
    @Test
    fun flingDownFrameTiming() = benchmarkRule.measureRepeated(
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
            sendText(seedDocument())
            device.waitForIdle()
        }
    ) {
        device.requireEditor().fling(Direction.DOWN)
        device.waitForIdle()
    }
}
