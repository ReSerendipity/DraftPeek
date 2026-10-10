package com.draftpeek.feature.stats.account

import com.draftpeek.core.sync.SyncBackoff
import com.draftpeek.core.sync.SyncFailureKind
import com.draftpeek.core.sync.SyncMachineState
import com.draftpeek.core.sync.SyncPhase

/**
 * 引擎状态 → 身份区展示态的映射（设计规格 v1.1 §3.1 · B6 接线表）。
 *
 * **纯函数、零判断逻辑**：引擎已经把失败分好类（[SyncFailureKind]），
 * 这里只做「状态 → 展示态」的转换，文案选择留在 UI 层查表。
 * 阶段 B 接线时 ViewModel 订阅 `SyncMachine.state` 并调用本函数即可，
 * 展示层（阶段 A 已交付）结构不动。
 */
object SyncStatusMapping {

    /**
     * @param state 引擎状态机当前状态
     * @return 身份区同步行的展示态
     *
     * 边界口径：
     * - `SYNCING` 且处于退避等待（`pendingRetryDelayMs != null`）→ [SyncUiStatus.Retrying]，
     *   次数取 `failureCount`（语义：第 n 次重试，共 [SyncBackoff.MAX_ATTEMPTS] 次）；
     * - `SYNCED` / `IDLE` 且成功过 → [SyncUiStatus.Synced]；
     * - `IDLE` 且从未成功（首次同步启动前的短暂态）→ 按 [SyncUiStatus.Syncing] 显示；
     * - `FAILED` 缺类别（防御）→ [SyncFailureKind.OTHER]。
     */
    fun from(state: SyncMachineState): SyncUiStatus = when (state.phase) {
        SyncPhase.SYNCING -> if (state.pendingRetryDelayMs != null) {
            SyncUiStatus.Retrying(
                attempt = state.failureCount,
                maxAttempts = SyncBackoff.MAX_ATTEMPTS
            )
        } else {
            SyncUiStatus.Syncing
        }

        SyncPhase.SYNCED -> SyncUiStatus.Synced(state.lastSuccessAtMillis ?: 0L)

        SyncPhase.FAILED -> SyncUiStatus.Failed(state.failureKind ?: SyncFailureKind.OTHER)

        SyncPhase.OFFLINE -> SyncUiStatus.Offline

        SyncPhase.IDLE ->
            state.lastSuccessAtMillis
                ?.let { SyncUiStatus.Synced(it) }
                ?: SyncUiStatus.Syncing
    }
}
