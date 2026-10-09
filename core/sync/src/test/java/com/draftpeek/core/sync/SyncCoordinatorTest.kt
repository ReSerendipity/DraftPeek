package com.draftpeek.core.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 协调器单测：虚拟时间驱动，覆盖「成功 / 退避重试 / 失败耗尽 / 离线续传 / 取消」全流程。
 *
 * ⚠️ **scope 的选法很关键**（实测踩过）：`TestScope.backgroundScope` 里 launch 的协程
 * 在本仓库环境下**不会被 `advanceUntilIdle()` 执行**（诊断用例 `viaBackground=false`），
 * 而 `TestScope` 自身又会因长驻收集器而让 `runTest` 挂住。
 * 因此这里用一个**共享同一个测试调度器的独立 scope**（`UnconfinedTestDispatcher(testScheduler)`）：
 * 既能被执行，又不会被 `runTest` 等待；测试结束由 [Env.close] 取消。
 */
@DisplayName("SyncCoordinator · 同步流程编排")
class SyncCoordinatorTest {

    private class FakeTransport : SyncTransport {
        var pullCount = 0
        var pushCount = 0
        var pullDelayMillis = 0L
        var pullResult: () -> PullOutcome = { PullOutcome.Success(SyncSnapshot.EMPTY) }
        var pushResult: () -> TransferResult = { TransferResult.Success }

        override suspend fun pull(): PullOutcome {
            pullCount++
            if (pullDelayMillis > 0) delay(pullDelayMillis)
            return pullResult()
        }

        override suspend fun push(snapshot: SyncSnapshot): TransferResult {
            pushCount++
            return pushResult()
        }
    }

    private class FakeLocal(var content: Map<String, String> = emptyMap()) : LocalSnapshotSource {
        var writeCount = 0
        var lastWritten: SyncSnapshot? = null

        override suspend fun read(): SyncSnapshot = SyncSnapshot(content)

        override suspend fun write(snapshot: SyncSnapshot) {
            writeCount++
            lastWritten = snapshot
            content = snapshot.files
        }
    }

    private class Env(testScope: TestScope) {
        private val external = CoroutineScope(UnconfinedTestDispatcher(testScope.testScheduler))

        val connectivity = MutableSharedFlow<Boolean>(replay = 1)
        val transport = FakeTransport()
        val local = FakeLocal(mapOf("sync.json" to "local"))
        val coordinator = SyncCoordinator(
            transport = transport,
            localSource = local,
            // 合并策略用假实现（远端优先），够验证「合并结果被写回并推送」
            merger = SyncMerger { _, remote -> remote },
            connectivity = connectivity,
            scope = external,
            clock = { testScope.testScheduler.currentTime }
        )

        fun close() {
            coordinator.stop()
            external.cancel()
        }
    }

    /** 统一处理 Env 的创建与清理，避免长驻协程泄漏到下一个用例。 */
    private fun coordinatorTest(body: suspend TestScope.(Env) -> Unit) = runTest {
        val env = Env(this)
        try {
            body(env)
        } finally {
            env.close()
        }
    }

    @Test
    @DisplayName("顺利路径：读本地 → 拉远端 → 合并 → 写本地 → 推远端，最终已同步")
    fun happyPath() = coordinatorTest { env ->
        env.transport.pullResult = { PullOutcome.Success(SyncSnapshot(mapOf("sync.json" to "remote"))) }
        env.coordinator.start()

        env.coordinator.requestSync()
        advanceUntilIdle()

        assertEquals(SyncPhase.SYNCED, env.coordinator.state.value.phase)
        assertEquals(1, env.transport.pullCount)
        assertEquals(1, env.transport.pushCount)
        assertEquals(1, env.local.writeCount)
        // 合并结果（远端优先）已写回本地并推送到远端
        assertEquals("remote", env.local.lastWritten?.files?.get("sync.json"))
    }

    @Test
    @DisplayName("网络类失败：按 1s → 4s 退避自动重试，重试成功则转已同步")
    fun retriesWithBackoff() = coordinatorTest { env ->
        var attempts = 0
        env.transport.pullResult = {
            attempts++
            if (attempts <= 2) {
                PullOutcome.Failure(SyncFailureKind.NETWORK)
            } else {
                PullOutcome.Success(SyncSnapshot.EMPTY)
            }
        }
        env.coordinator.start()

        env.coordinator.requestSync()
        advanceUntilIdle()

        assertEquals(3, env.transport.pullCount)
        assertEquals(SyncPhase.SYNCED, env.coordinator.state.value.phase)
    }

    @Test
    @DisplayName("网络类失败耗尽 3 次重试后转失败态，不再重试")
    fun exhaustsRetries() = coordinatorTest { env ->
        env.transport.pullResult = { PullOutcome.Failure(SyncFailureKind.NETWORK) }
        env.coordinator.start()

        env.coordinator.requestSync()
        advanceUntilIdle()

        // 首次 + 3 次重试 = 4 次尝试
        assertEquals(4, env.transport.pullCount)
        assertEquals(SyncPhase.FAILED, env.coordinator.state.value.phase)
    }

    @Test
    @DisplayName("授权类失败：不重试，直接失败")
    fun authFailureDoesNotRetry() = coordinatorTest { env ->
        env.transport.pullResult = { PullOutcome.Failure(SyncFailureKind.AUTH) }
        env.coordinator.start()

        env.coordinator.requestSync()
        advanceUntilIdle()

        assertEquals(1, env.transport.pullCount)
        assertEquals(SyncPhase.FAILED, env.coordinator.state.value.phase)
    }

    @Test
    @DisplayName("断网 → 离线态；恢复联网 → 自动续传")
    fun offlineThenResume() = coordinatorTest { env ->
        env.coordinator.start()

        env.connectivity.emit(false)
        advanceUntilIdle()
        assertEquals(SyncPhase.OFFLINE, env.coordinator.state.value.phase)

        val before = env.transport.pullCount
        env.connectivity.emit(true)
        advanceUntilIdle()

        assertEquals(before + 1, env.transport.pullCount)
        assertEquals(SyncPhase.SYNCED, env.coordinator.state.value.phase)
    }

    @Test
    @DisplayName("同步中重复触发被忽略（传输只跑一次）")
    fun duplicateRequestIgnored() = coordinatorTest { env ->
        env.transport.pullDelayMillis = 5_000L
        env.coordinator.start()

        env.coordinator.requestSync()
        runCurrent()
        env.coordinator.requestSync()
        env.coordinator.requestSync()
        advanceUntilIdle()

        assertEquals(1, env.transport.pullCount)
    }

    @Test
    @DisplayName("超过 30 秒才可取消；取消后回到已同步并中止传输")
    fun cancelAfterThreshold() = coordinatorTest { env ->
        env.coordinator.start()
        // 先成功一次，让「上一次稳定态」是已同步
        env.coordinator.requestSync()
        advanceUntilIdle()
        assertEquals(SyncPhase.SYNCED, env.coordinator.state.value.phase)

        // 再发起一次慢同步
        env.transport.pullDelayMillis = 120_000L
        env.coordinator.requestSync()
        runCurrent()

        advanceTimeBy(SyncMachine.CANCEL_AFTER_MS - 2_000L)
        runCurrent()
        assertFalse(env.coordinator.state.value.cancelAvailable)

        advanceTimeBy(3_000L)
        runCurrent()
        assertTrue(env.coordinator.state.value.cancelAvailable)

        env.coordinator.cancel()
        advanceUntilIdle()

        assertEquals(SyncPhase.SYNCED, env.coordinator.state.value.phase)
        assertFalse(env.coordinator.state.value.cancelAvailable)
    }

    @Test
    @DisplayName("stop() 后不再发起新的传输")
    fun stopCancelsJobs() = coordinatorTest { env ->
        env.coordinator.start()
        env.coordinator.requestSync()
        runCurrent()

        env.coordinator.stop()
        advanceUntilIdle()

        val after = env.transport.pullCount
        advanceTimeBy(120_000L)
        advanceUntilIdle()
        assertEquals(after, env.transport.pullCount)
    }
}
