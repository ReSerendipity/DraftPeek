package com.draftpeek.core.sync

/**
 * 同步引擎的内部状态。
 *
 * 只保留驱动 UI 与重试策略所必需的信息；时间一律由调用方传入（**不在此读系统时钟**），
 * 因此整个状态机是纯函数，可完全单测。
 */
data class SyncMachineState(
    val phase: SyncPhase = SyncPhase.IDLE,
    /**
     * 本轮已失败次数（用于退避决策；成功或取消后归零）。
     *
     * 注意语义：**3 次重试 = 4 次失败**（首次失败 + 3 次重试各失败一次），
     * 因此进入失败态时该值为 4，与 [SyncBackoff.MAX_ATTEMPTS] 不相等。
     */
    val failureCount: Int = 0,
    /** 上次**全量成功**的时间戳；规格：仅全量成功后更新。 */
    val lastSuccessAtMillis: Long? = null,
    /** 本轮同步开始时间（用于判定「超 30 秒可取消」）。 */
    val syncingStartedAtMillis: Long? = null,
    /** 「正在同步…」是否已超过取消阈值。 */
    val cancelAvailable: Boolean = false,
    /** 退避等待中的下次重试延迟；非退避态为 null。 */
    val pendingRetryDelayMs: Long? = null,
    /** 最近一次失败的类别；进入 FAILED 前写入，`Requested`/`Succeeded`/取消/断网时清空。 */
    val failureKind: SyncFailureKind? = null
)

/** 驱动状态机的事件。 */
sealed interface SyncEvent {

    /** 触发同步（自动变更触发或用户点「立即同步」）。 */
    data class Requested(val nowMillis: Long) : SyncEvent

    /** 同步成功。 */
    data class Succeeded(val nowMillis: Long) : SyncEvent

    /** 同步失败。 */
    data class Failed(val kind: SyncFailureKind) : SyncEvent

    /** 检测到断网（即时转「离线，恢复联网后继续」）。 */
    data object WentOffline : SyncEvent

    /** 恢复联网（离线态下**自动续传**）。 */
    data class WentOnline(val nowMillis: Long) : SyncEvent

    /** 用户点「取消」（仅在超过阈值后有效）。 */
    data object CancelRequested : SyncEvent

    /**
     * 退避时间到点，可以重试了。
     *
     * **为什么需要单独一个事件**：退避期间状态仍是 [SyncPhase.SYNCING]，
     * 而 [Requested] 会被「同步中禁用重复触发」的守卫挡掉；这里需要的是
     * **调度器到点后**重新发起传输，所以单列一个事件，语义更清晰、也可单测。
     */
    data object RetryDue : SyncEvent

    /** 时间推进（用于判定取消阈值）。 */
    data class Tick(val nowMillis: Long) : SyncEvent
}

/** 状态机要求外部执行的动作。 */
sealed interface SyncEffect {

    /** 开始一次传输。 */
    data object StartTransfer : SyncEffect

    /** 在 [delayMillis] 之后重试。 */
    data class ScheduleRetry(val delayMillis: Long) : SyncEffect

    /** 中止进行中的传输（离线或用户取消）。 */
    data object CancelTransfer : SyncEffect
}

/**
 * 同步状态机（设计回函 v3 §3 的行为规格）。
 *
 * 规格要点，逐条对应实现：
 * - 触发后进入 [SyncPhase.SYNCING] 并**禁用重复触发**；
 * - 超过 [CANCEL_AFTER_MS]（30 秒）后允许「取消」；
 * - **网络类**失败自动退避 [SyncBackoff.DELAYS_MS]（1s → 4s → 16s，最多 3 次），
 *   仍失败转 [SyncPhase.FAILED]；
 * - **授权类 / 仓库类**失败不重试，直接 [SyncPhase.FAILED]（重试无意义）；
 * - 断网即时转 [SyncPhase.OFFLINE]，恢复联网后**自动续传**；
 * - 「已同步」时间戳**仅在成功后**更新。
 *
 * 纯函数：[reduce] 不产生副作用，只返回新状态与需要执行的动作。
 */
object SyncMachine {

    /** 「正在同步…」超过该时长后行内出现「取消」。 */
    const val CANCEL_AFTER_MS: Long = 30_000L

    /** 状态转移。 */
    fun reduce(state: SyncMachineState, event: SyncEvent): Pair<SyncMachineState, List<SyncEffect>> = when (event) {
        is SyncEvent.Requested -> onRequested(state, event.nowMillis)
        is SyncEvent.Succeeded -> state.copy(
            phase = SyncPhase.SYNCED,
            failureCount = 0,
            lastSuccessAtMillis = event.nowMillis,
            syncingStartedAtMillis = null,
            cancelAvailable = false,
            pendingRetryDelayMs = null,
            failureKind = null
        ) to emptyList()

        is SyncEvent.Failed -> onFailed(state, event.kind)
        SyncEvent.WentOffline -> onOffline(state)
        is SyncEvent.WentOnline -> onWentOnline(state, event.nowMillis)
        SyncEvent.CancelRequested -> onCancel(state)
        SyncEvent.RetryDue -> onRetryDue(state)
        is SyncEvent.Tick -> onTick(state, event.nowMillis)
    }

    private fun onRetryDue(state: SyncMachineState): Pair<SyncMachineState, List<SyncEffect>> =
        // 仅当仍处于「同步中（退避等待）」才真正重试；期间被取消或断网则忽略
        if (state.phase == SyncPhase.SYNCING) {
            state.copy(pendingRetryDelayMs = null) to listOf(SyncEffect.StartTransfer)
        } else {
            state to emptyList()
        }

    private fun onRequested(state: SyncMachineState, nowMillis: Long): Pair<SyncMachineState, List<SyncEffect>> =
        if (state.phase == SyncPhase.SYNCING) {
            // 规格：同步中禁用重复触发
            state to emptyList()
        } else {
            state.copy(
                phase = SyncPhase.SYNCING,
                failureCount = 0,
                syncingStartedAtMillis = nowMillis,
                cancelAvailable = false,
                pendingRetryDelayMs = null,
                failureKind = null
            ) to listOf(SyncEffect.StartTransfer)
        }

    private fun onFailed(state: SyncMachineState, kind: SyncFailureKind): Pair<SyncMachineState, List<SyncEffect>> {
        if (kind != SyncFailureKind.NETWORK) {
            // 授权 / 仓库 / 其他：重试无意义，直接进入失败态
            return state.copy(
                phase = SyncPhase.FAILED,
                syncingStartedAtMillis = null,
                cancelAvailable = false,
                pendingRetryDelayMs = null,
                failureKind = kind
            ) to emptyList()
        }
        val attempts = state.failureCount + 1
        val delay = SyncBackoff.delayForAttempt(attempts)
        return if (delay != null) {
            // 退避等待期间仍显示「正在同步…」
            state.copy(
                phase = SyncPhase.SYNCING,
                failureCount = attempts,
                pendingRetryDelayMs = delay,
                failureKind = kind
            ) to listOf(SyncEffect.ScheduleRetry(delay))
        } else {
            state.copy(
                phase = SyncPhase.FAILED,
                failureCount = attempts,
                syncingStartedAtMillis = null,
                cancelAvailable = false,
                pendingRetryDelayMs = null,
                failureKind = kind
            ) to emptyList()
        }
    }

    private fun onOffline(state: SyncMachineState): Pair<SyncMachineState, List<SyncEffect>> = state.copy(
        phase = SyncPhase.OFFLINE,
        failureCount = 0,
        syncingStartedAtMillis = null,
        cancelAvailable = false,
        pendingRetryDelayMs = null,
        failureKind = null
    ) to if (state.phase == SyncPhase.SYNCING) listOf(SyncEffect.CancelTransfer) else emptyList()

    private fun onWentOnline(state: SyncMachineState, nowMillis: Long): Pair<SyncMachineState, List<SyncEffect>> =
        if (state.phase == SyncPhase.OFFLINE) {
            // 规格：恢复联网后自动续传
            state.copy(
                phase = SyncPhase.SYNCING,
                failureCount = 0,
                syncingStartedAtMillis = nowMillis,
                cancelAvailable = false,
                pendingRetryDelayMs = null
            ) to listOf(SyncEffect.StartTransfer)
        } else {
            state to emptyList()
        }

    private fun onCancel(state: SyncMachineState): Pair<SyncMachineState, List<SyncEffect>> =
        if (state.phase == SyncPhase.SYNCING && state.cancelAvailable) {
            state.copy(
                // 取消回到「上一次稳定态」：同步过就是已同步，否则回到空闲
                phase = if (state.lastSuccessAtMillis != null) SyncPhase.SYNCED else SyncPhase.IDLE,
                failureCount = 0,
                syncingStartedAtMillis = null,
                cancelAvailable = false,
                pendingRetryDelayMs = null,
                failureKind = null
            ) to listOf(SyncEffect.CancelTransfer)
        } else {
            // 未到 30 秒阈值：不允许取消
            state to emptyList()
        }

    private fun onTick(state: SyncMachineState, nowMillis: Long): Pair<SyncMachineState, List<SyncEffect>> {
        val startedAt = state.syncingStartedAtMillis
        val reachedThreshold = state.phase == SyncPhase.SYNCING &&
            !state.cancelAvailable &&
            startedAt != null &&
            nowMillis - startedAt >= CANCEL_AFTER_MS
        return if (reachedThreshold) state.copy(cancelAvailable = true) to emptyList() else state to emptyList()
    }
}
