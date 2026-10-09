package com.draftpeek.core.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SyncBackoff · 退避策略（设计回函 v3 §3：1s → 4s → 16s，最多 3 次）")
class SyncBackoffTest {

    @Test
    @DisplayName("延迟序列是 1s / 4s / 16s")
    fun delays() {
        assertEquals(listOf(1_000L, 4_000L, 16_000L), SyncBackoff.DELAYS_MS)
        assertEquals(3, SyncBackoff.MAX_ATTEMPTS)
    }

    @Test
    @DisplayName("第 n 次重试取对应延迟；超出上限返回 null（不再重试）")
    fun delayForAttempt() {
        assertEquals(1_000L, SyncBackoff.delayForAttempt(1))
        assertEquals(4_000L, SyncBackoff.delayForAttempt(2))
        assertEquals(16_000L, SyncBackoff.delayForAttempt(3))
        assertNull(SyncBackoff.delayForAttempt(4))
    }

    @Test
    @DisplayName("非法入参（0 或负数）返回 null，不抛异常")
    fun invalidAttempt() {
        assertNull(SyncBackoff.delayForAttempt(0))
        assertNull(SyncBackoff.delayForAttempt(-1))
    }
}

@DisplayName("SyncMachine · 同步状态机")
class SyncMachineTest {

    private val t0 = 1_700_000_000_000L

    private fun reduce(state: SyncMachineState, event: SyncEvent): Pair<SyncMachineState, List<SyncEffect>> =
        SyncMachine.reduce(state, event)

    /** 走一遍「触发 → 成功」，得到一个已同步的稳定态。 */
    private fun synced(nowMillis: Long = t0): SyncMachineState = reduce(
        reduce(SyncMachineState(), SyncEvent.Requested(nowMillis)).first,
        SyncEvent.Succeeded(nowMillis + 5_000L)
    ).first

    @Nested
    @DisplayName("触发与去重")
    inner class Trigger {

        @Test
        @DisplayName("空闲态触发 → 进入同步中并开始传输")
        fun requestStartsSync() {
            val (next, effects) = reduce(SyncMachineState(), SyncEvent.Requested(t0))
            assertEquals(SyncPhase.SYNCING, next.phase)
            assertEquals(listOf(SyncEffect.StartTransfer), effects)
            assertEquals(t0, next.syncingStartedAtMillis)
        }

        @Test
        @DisplayName("同步中重复触发被忽略（规格：禁用重复触发）")
        fun duplicateRequestIgnored() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.Requested(t0 + 1_000L))
            assertEquals(syncing, next)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("退避等待期间仍属同步中 → 重复触发同样被忽略")
        fun requestDuringBackoffIgnored() {
            var state = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            state = reduce(state, SyncEvent.Failed(SyncFailureKind.NETWORK)).first
            assertEquals(1_000L, state.pendingRetryDelayMs)

            val (next, effects) = reduce(state, SyncEvent.Requested(t0 + 500L))
            assertEquals(state, next)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("失败态手动重试会重置失败计数（3 次重试耗尽 = 4 次失败）")
        fun manualRetryResetsFailureCount() {
            var state = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            repeat(4) { state = reduce(state, SyncEvent.Failed(SyncFailureKind.NETWORK)).first }
            assertEquals(SyncPhase.FAILED, state.phase)
            assertEquals(4, state.failureCount)

            val (retried, effects) = reduce(state, SyncEvent.Requested(t0 + 90_000L))
            assertEquals(SyncPhase.SYNCING, retried.phase)
            assertEquals(0, retried.failureCount)
            assertEquals(listOf(SyncEffect.StartTransfer), effects)
        }
    }

    @Nested
    @DisplayName("成功路径")
    inner class Success {

        @Test
        @DisplayName("成功后进入已同步并更新时间戳、清零退避")
        fun successUpdatesTimestamp() {
            val (next, effects) = reduce(
                reduce(SyncMachineState(), SyncEvent.Requested(t0)).first,
                SyncEvent.Succeeded(t0 + 3_000L)
            )
            assertEquals(SyncPhase.SYNCED, next.phase)
            assertEquals(t0 + 3_000L, next.lastSuccessAtMillis)
            assertEquals(0, next.failureCount)
            assertNull(next.syncingStartedAtMillis)
            assertTrue(effects.isEmpty())
        }
    }

    @Nested
    @DisplayName("失败与退避")
    inner class Failure {

        @Test
        @DisplayName("网络类失败 → 退避 1s 后重试，期间仍显示同步中")
        fun networkFailureSchedulesRetry() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.Failed(SyncFailureKind.NETWORK))
            assertEquals(SyncPhase.SYNCING, next.phase)
            assertEquals(1, next.failureCount)
            assertEquals(1_000L, next.pendingRetryDelayMs)
            assertEquals(listOf(SyncEffect.ScheduleRetry(1_000L)), effects)
        }

        @Test
        @DisplayName("连续三次网络失败（1s/4s/16s）后转失败态，不再重试")
        fun networkFailureExhaustsRetries() {
            var state = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val delays = mutableListOf<Long>()
            repeat(3) {
                val (next, effects) = reduce(state, SyncEvent.Failed(SyncFailureKind.NETWORK))
                state = next
                delays += (effects.single() as SyncEffect.ScheduleRetry).delayMillis
            }
            assertEquals(listOf(1_000L, 4_000L, 16_000L), delays)
            assertEquals(3, state.failureCount)

            val (failed, effects) = reduce(state, SyncEvent.Failed(SyncFailureKind.NETWORK))
            assertEquals(SyncPhase.FAILED, failed.phase)
            assertTrue(effects.isEmpty())
            assertNull(failed.pendingRetryDelayMs)
        }

        @Test
        @DisplayName("授权类失败不重试，直接失败（方案 C：token 失效需重新授权）")
        fun authFailureFailsImmediately() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.Failed(SyncFailureKind.AUTH))
            assertEquals(SyncPhase.FAILED, next.phase)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("仓库类失败不重试（无权限 / 配额 / 仓库不存在）")
        fun repositoryFailureFailsImmediately() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.Failed(SyncFailureKind.REPOSITORY))
            assertEquals(SyncPhase.FAILED, next.phase)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("冲突类失败不重试（需先重新拉取并合并，交给上层处理）")
        fun conflictFailsImmediately() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.Failed(SyncFailureKind.CONFLICT))
            assertEquals(SyncPhase.FAILED, next.phase)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("退避到点（RetryDue）→ 重新发起传输；已离开同步中则忽略")
        fun retryDueRestartsTransfer() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val backing = reduce(syncing, SyncEvent.Failed(SyncFailureKind.NETWORK)).first
            assertEquals(1_000L, backing.pendingRetryDelayMs)

            val (retried, effects) = reduce(backing, SyncEvent.RetryDue)
            assertEquals(SyncPhase.SYNCING, retried.phase)
            assertNull(retried.pendingRetryDelayMs)
            assertEquals(listOf(SyncEffect.StartTransfer), effects)

            // 期间已转失败（如授权类失败）时，到点的重试应被忽略
            val failed = reduce(backing, SyncEvent.Failed(SyncFailureKind.AUTH)).first
            val (ignored, noEffects) = reduce(failed, SyncEvent.RetryDue)
            assertEquals(failed, ignored)
            assertTrue(noEffects.isEmpty())
        }

        @Test
        @DisplayName("失败不会污染「已同步」时间戳（规格：仅成功后更新）")
        fun failureKeepsLastSuccess() {
            val successAt = t0 + 5_000L
            var state = synced()
            assertEquals(successAt, state.lastSuccessAtMillis)

            state = reduce(state, SyncEvent.Requested(t0 + 60_000L)).first
            state = reduce(state, SyncEvent.Failed(SyncFailureKind.AUTH)).first
            assertEquals(SyncPhase.FAILED, state.phase)
            assertEquals(successAt, state.lastSuccessAtMillis)
        }
    }

    @Nested
    @DisplayName("离线与续传")
    inner class Offline {

        @Test
        @DisplayName("同步中断网 → 立即转离线，并中止进行中的传输")
        fun offlineWhileSyncing() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, effects) = reduce(syncing, SyncEvent.WentOffline)
            assertEquals(SyncPhase.OFFLINE, next.phase)
            assertEquals(listOf(SyncEffect.CancelTransfer), effects)
        }

        @Test
        @DisplayName("空闲态断网 → 转离线但不产生动作")
        fun offlineWhileIdle() {
            val (next, effects) = reduce(SyncMachineState(), SyncEvent.WentOffline)
            assertEquals(SyncPhase.OFFLINE, next.phase)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("离线恢复联网 → 自动续传")
        fun onlineResumesAutomatically() {
            val offline = reduce(SyncMachineState(), SyncEvent.WentOffline).first
            val (next, effects) = reduce(offline, SyncEvent.WentOnline(t0 + 30_000L))
            assertEquals(SyncPhase.SYNCING, next.phase)
            assertEquals(listOf(SyncEffect.StartTransfer), effects)
        }

        @Test
        @DisplayName("非离线态收到「恢复联网」不产生任何变化")
        fun onlineWithoutOfflineIsNoop() {
            val state = synced()
            val (next, effects) = reduce(state, SyncEvent.WentOnline(t0 + 90_000L))
            assertEquals(state, next)
            assertTrue(effects.isEmpty())
        }
    }

    @Nested
    @DisplayName("取消（超 30 秒）")
    inner class Cancel {

        @Test
        @DisplayName("未到 30 秒：Tick 不开放取消，取消请求无效")
        fun cancelUnavailableBeforeThreshold() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (before, _) = reduce(syncing, SyncEvent.Tick(t0 + 29_999L))
            assertTrue(!before.cancelAvailable)

            val (next, effects) = reduce(before, SyncEvent.CancelRequested)
            assertEquals(before, next)
            assertTrue(effects.isEmpty())
        }

        @Test
        @DisplayName("满 30 秒：Tick 开放取消")
        fun cancelAvailableAtThreshold() {
            val syncing = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            val (next, _) = reduce(syncing, SyncEvent.Tick(t0 + SyncMachine.CANCEL_AFTER_MS))
            assertTrue(next.cancelAvailable)
        }

        @Test
        @DisplayName("取消后回到上一次稳定态：同步过 → 已同步（时间戳不变）")
        fun cancelReturnsToSynced() {
            var state = synced()
            val successAt = state.lastSuccessAtMillis
            state = reduce(state, SyncEvent.Requested(t0 + 120_000L)).first
            state = reduce(state, SyncEvent.Tick(t0 + 120_000L + SyncMachine.CANCEL_AFTER_MS)).first

            val (next, effects) = reduce(state, SyncEvent.CancelRequested)
            assertEquals(SyncPhase.SYNCED, next.phase)
            assertEquals(successAt, next.lastSuccessAtMillis)
            assertEquals(listOf(SyncEffect.CancelTransfer), effects)
        }

        @Test
        @DisplayName("取消后回到上一次稳定态：从未同步过 → 空闲")
        fun cancelReturnsToIdle() {
            var state = reduce(SyncMachineState(), SyncEvent.Requested(t0)).first
            state = reduce(state, SyncEvent.Tick(t0 + SyncMachine.CANCEL_AFTER_MS)).first

            val (next, _) = reduce(state, SyncEvent.CancelRequested)
            assertEquals(SyncPhase.IDLE, next.phase)
        }
    }
}
