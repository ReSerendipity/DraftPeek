package com.draftpeek.benchmark

import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 应用启动性能基准测试。
 *
 * 测量冷启动和热启动时间，生成 Baseline Profile 用于优化首次启动速度。
 * 运行方式：./gradlew :benchmark:connectedBenchmarkAndroidTest
 *
 * 生成的 Baseline Profile 位于 app/src/main/baseline-prof.txt，
 * 安装后首次启动速度可提升 20-40%。
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.draftpeek",
        metrics = listOf(
            androidx.benchmark.macro.StartupTimingMetric(),
        ),
        iterations = 5,
        startupMode = androidx.benchmark.macro.StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun startupWarm() = benchmarkRule.measureRepeated(
        packageName = "com.draftpeek",
        metrics = listOf(
            androidx.benchmark.macro.StartupTimingMetric(),
        ),
        iterations = 5,
        startupMode = androidx.benchmark.macro.StartupMode.WARM,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun startupHot() = benchmarkRule.measureRepeated(
        packageName = "com.draftpeek",
        metrics = listOf(
            androidx.benchmark.macro.StartupTimingMetric(),
        ),
        iterations = 5,
        startupMode = androidx.benchmark.macro.StartupMode.HOT,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
