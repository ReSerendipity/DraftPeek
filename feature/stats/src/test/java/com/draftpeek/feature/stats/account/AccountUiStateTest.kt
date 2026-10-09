package com.draftpeek.feature.stats.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AccountUiState · 阶段 A 身份区")
class AccountUiStateTest {

    private val now = 1_700_000_000_000L

    @Nested
    @DisplayName("AccountTimeFormat.relativeTime（已同步 · N 分钟前）")
    inner class RelativeTimeTest {

        @Test
        @DisplayName("不足 1 分钟 → JUST_NOW（避免显示「0 分钟前」）")
        fun justNow() {
            assertEquals(RelativeUnit.JUST_NOW, AccountTimeFormat.relativeTime(now, now).unit)
            assertEquals(
                RelativeUnit.JUST_NOW,
                AccountTimeFormat.relativeTime(now, now - 59_000L).unit
            )
        }

        @Test
        @DisplayName("1 分钟 ~ 59 分钟 → MINUTES")
        fun minutes() {
            val r = AccountTimeFormat.relativeTime(now, now - 2 * 60_000L)
            assertEquals(RelativeUnit.MINUTES, r.unit)
            assertEquals(2, r.value)
            assertEquals(59, AccountTimeFormat.relativeTime(now, now - 59 * 60_000L).value)
        }

        @Test
        @DisplayName("60 分钟 → 进到 HOURS")
        fun hours() {
            val r = AccountTimeFormat.relativeTime(now, now - 60 * 60_000L)
            assertEquals(RelativeUnit.HOURS, r.unit)
            assertEquals(1, r.value)
            assertEquals(
                RelativeUnit.HOURS,
                AccountTimeFormat.relativeTime(now, now - 23 * 3_600_000L).unit
            )
        }

        @Test
        @DisplayName("24 小时 → 进到 DAYS")
        fun days() {
            val r = AccountTimeFormat.relativeTime(now, now - 24 * 3_600_000L)
            assertEquals(RelativeUnit.DAYS, r.unit)
            assertEquals(1, r.value)
        }

        @Test
        @DisplayName("时钟回拨（上次同步时间在未来）按 JUST_NOW 处理，不出现负数")
        fun clockSkew() {
            assertEquals(
                RelativeUnit.JUST_NOW,
                AccountTimeFormat.relativeTime(now, now + 60_000L).unit
            )
        }
    }

    @Nested
    @DisplayName("AccountStateSimulation.next（debug 验收轮转）")
    inner class SimulationTest {

        @Test
        @DisplayName("未登录 → 已同步，且时间戳为「2 分钟前」")
        fun signedOutGoesToSynced() {
            val next = AccountStateSimulation.next(AccountUiState.SignedOut, now)
            val signedIn = next as AccountUiState.SignedIn
            val synced = signedIn.sync as SyncUiStatus.Synced
            assertEquals(
                now - AccountStateSimulation.SIMULATED_SYNCED_AGO_MS,
                synced.lastSuccessAtMillis
            )
            // 该时间戳应落在 MINUTES 量级，便于核对「已同步 · 2 分钟前」
            assertEquals(
                RelativeUnit.MINUTES,
                AccountTimeFormat.relativeTime(now, synced.lastSuccessAtMillis).unit
            )
        }

        @Test
        @DisplayName("轮转顺序：未登录 → 已同步 → 同步中 → 失败 → 离线 → 未登录")
        fun cycleOrder() {
            var state: AccountUiState = AccountUiState.SignedOut
            state = AccountStateSimulation.next(state, now)
            assertTrue((state as AccountUiState.SignedIn).sync is SyncUiStatus.Synced)

            state = AccountStateSimulation.next(state, now)
            assertTrue((state as AccountUiState.SignedIn).sync is SyncUiStatus.Syncing)

            state = AccountStateSimulation.next(state, now)
            assertTrue((state as AccountUiState.SignedIn).sync is SyncUiStatus.Failed)

            state = AccountStateSimulation.next(state, now)
            assertTrue((state as AccountUiState.SignedIn).sync is SyncUiStatus.Offline)

            state = AccountStateSimulation.next(state, now)
            assertEquals(AccountUiState.SignedOut, state)
        }

        @Test
        @DisplayName("已登录资料在轮转中保持不变（邮箱与昵称不随同步状态变化）")
        fun profileStableAcrossSyncStates() {
            val first = AccountStateSimulation.next(AccountUiState.SignedOut, now)
            val second = AccountStateSimulation.next(first, now) as AccountUiState.SignedIn
            assertEquals((first as AccountUiState.SignedIn).email, second.email)
            assertEquals(first.displayName, second.displayName)
        }
    }

    @Nested
    @DisplayName("SyncUiStatus 常量")
    inner class SyncStatusTest {

        @Test
        @DisplayName("「正在同步…」的取消阈值是 30 秒（设计回函 v3 §3）")
        fun cancelThreshold() {
            assertEquals(30_000L, SyncUiStatus.SYNC_CANCEL_AFTER_MS)
        }
    }
}
