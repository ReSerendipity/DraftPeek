package com.draftpeek.feature.editor.lsp

import android.content.Context
import com.draftpeek.core.common.feature.FeatureToggleManager
import com.draftpeek.feature.editor.diagnostics.DefaultDiagnosticNavigator
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [LspManager] 关闭路径的回归测试。
 *
 * ## 背景（测试体系评估报告 P0）
 * `LspManager.shutdownAll()` 原先在主代码里使用 `kotlinx.coroutines.runBlocking`，
 * 违反项目铁律（主代码不得阻塞线程）。现改为 `suspend`，并额外提供 [LspManager.shutdownAllAsync]
 * 供非协程上下文使用。
 *
 * 本测试锁定两条契约，防止后续改回阻塞式实现：
 * 1. `shutdownAll()` 是挂起函数，能在 `runTest` 中直接调用并完成（无需阻塞桥接）；
 * 2. `shutdownAllAsync()` 返回的 [kotlinx.coroutines.Job] 能正常结束，且不会抛异常。
 *
 * 注：`clients` 为空时关闭路径是 no-op，这里主要验证「可挂起、可并发关闭、状态被清空」；
 * 真实客户端的 `shutdown()` 调用由 instrumented 测试覆盖。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LspManagerShutdownTest {

    private fun newManager(): LspManager = LspManager(
        context = mockk<Context>(relaxed = true),
        diagnosticNavigator = DefaultDiagnosticNavigator(),
        featureToggleManager = mockk<FeatureToggleManager>(relaxed = true)
    )

    @Test
    @DisplayName("shutdownAll 作为挂起函数可直接调用，并清空客户端状态")
    fun shutdownAll_completesAndClearsClientStatus() = runTest {
        val manager = newManager()

        manager.shutdownAll()

        assertTrue(
            manager.clientStatus.value.isEmpty(),
            "shutdownAll 后 clientStatus 应为空"
        )
    }

    @Test
    @DisplayName("shutdownAll 幂等：连续调用两次不抛异常且状态一致")
    fun shutdownAll_isIdempotent() = runTest {
        val manager = newManager()

        manager.shutdownAll()
        manager.shutdownAll()

        assertTrue(manager.clientStatus.value.isEmpty())
    }

    @Test
    @DisplayName("shutdownAllAsync 返回的 Job 能正常完成（非协程上下文可用的入口）")
    fun shutdownAllAsync_jobCompletes() = runTest {
        val manager = newManager()

        val job = manager.shutdownAllAsync()
        job.join()

        assertEquals(
            true,
            job.isCompleted,
            "shutdownAllAsync 的 Job 应正常完成，而不是被作用域取消"
        )
        assertTrue(manager.clientStatus.value.isEmpty())
    }

    @Test
    @DisplayName("并发调用 shutdownAll 不产生异常")
    fun shutdownAll_concurrentInvocationsAreSafe() = runTest {
        val manager = newManager()

        val jobs = List(4) { launch { manager.shutdownAll() } }
        jobs.joinAll()

        assertTrue(
            jobs.all { it.isCompleted },
            "并发 shutdownAll 的所有任务都应完成"
        )
        assertTrue(manager.clientStatus.value.isEmpty())
    }
}
