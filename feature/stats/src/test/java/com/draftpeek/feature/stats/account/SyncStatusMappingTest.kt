package com.draftpeek.feature.stats.account

import com.draftpeek.core.sync.SyncBackoff
import com.draftpeek.core.sync.SyncFailureKind
import com.draftpeek.core.sync.SyncMachineState
import com.draftpeek.core.sync.SyncPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [SyncStatusMapping] 的逐格验证（设计规格 v1.1 §3.1 · B6 接线表）。
 *
 * 覆盖：五相 × 关键字段组合、退避等待的 Retrying(n/max)、失败类别直通、
 * IDLE 两个分支（成功过 / 从未成功）。
 */
@DisplayName("SyncStatusMapping 引擎态 → 展示态")
class SyncStatusMappingTest {

    private fun state(
        phase: SyncPhase,
        failureCount: Int = 0,
        lastSuccessAtMillis: Long? = null,
        pendingRetryDelayMs: Long? = null,
        failureKind: SyncFailureKind? = null
    ) = SyncMachineState(
        phase = phase,
        failureCount = failureCount,
        lastSuccessAtMillis = lastSuccessAtMillis,
        pendingRetryDelayMs = pendingRetryDelayMs,
        failureKind = failureKind
    )

    @Nested
    @DisplayName("SYNCING")
    inner class SyncingTest {

        @Test
        @DisplayName("普通同步中 → Syncing")
        fun plainSyncing() {
            assertEquals(SyncUiStatus.Syncing, SyncStatusMapping.from(state(SyncPhase.SYNCING)))
        }

        @Test
        @DisplayName("退避等待（pendingRetryDelayMs 非空）→ Retrying(n / MAX_ATTEMPTS)")
        fun backoffWaitingIsRetrying() {
            val mapped = SyncStatusMapping.from(
                state(SyncPhase.SYNCING, failureCount = 2, pendingRetryDelayMs = 4_000L)
            )
            assertEquals(SyncUiStatus.Retrying(attempt = 2, maxAttempts = SyncBackoff.MAX_ATTEMPTS), mapped)
        }
    }

    @Nested
    @DisplayName("SYNCED / IDLE")
    inner class SyncedAndIdleTest {

        @Test
        @DisplayName("SYNCED → Synced(时间戳)")
        fun synced() {
            assertEquals(
                SyncUiStatus.Synced(1_730_000_000_000L),
                SyncStatusMapping.from(state(SyncPhase.SYNCED, lastSuccessAtMillis = 1_730_000_000_000L))
            )
        }

        @Test
        @DisplayName("IDLE 且成功过 → Synced（上次时间戳）")
        fun idleWithHistory() {
            assertEquals(
                SyncUiStatus.Synced(123L),
                SyncStatusMapping.from(state(SyncPhase.IDLE, lastSuccessAtMillis = 123L))
            )
        }

        @Test
        @DisplayName("IDLE 且从未成功（首次同步启动前）→ Syncing")
        fun idleWithoutHistory() {
            assertEquals(SyncUiStatus.Syncing, SyncStatusMapping.from(state(SyncPhase.IDLE)))
        }
    }

    @Nested
    @DisplayName("FAILED")
    inner class FailedTest {

        @Test
        @DisplayName("五类失败类别逐一直通（UI 只查表）")
        fun kindsPassThrough() {
            SyncFailureKind.entries.forEach { kind ->
                val mapped = SyncStatusMapping.from(
                    state(SyncPhase.FAILED, failureKind = kind)
                )
                assertEquals(SyncUiStatus.Failed(kind), mapped, "类别 $kind 未直通")
            }
        }

        @Test
        @DisplayName("防御：FAILED 缺类别 → Failed(OTHER)，不崩溃")
        fun missingKindFallsBackToOther() {
            val mapped = SyncStatusMapping.from(state(SyncPhase.FAILED, failureKind = null))
            assertEquals(SyncUiStatus.Failed(SyncFailureKind.OTHER), mapped)
        }
    }

    @Test
    @DisplayName("OFFLINE → Offline")
    fun offline() {
        assertTrue(SyncStatusMapping.from(state(SyncPhase.OFFLINE)) is SyncUiStatus.Offline)
    }
}
