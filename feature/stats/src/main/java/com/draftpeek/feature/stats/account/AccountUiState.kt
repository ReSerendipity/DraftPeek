package com.draftpeek.feature.stats.account

/**
 * 「我的」页身份区的展示状态（设计回函 v3 · 阶段 A）。
 *
 * **为什么单独建模**：阶段 A 要求身份区呈现五态（未登录 / 已登录 / 同步中 / 失败离线 / 退出确认），
 * 但账号体系与同步引擎属**阶段 B**（`core/sync` 目前只有模块骨架、未接入构建）。
 * 因此这里只定义**展示状态**：UI 完全由 [AccountUiState] 驱动，阶段 B 到位时**只换数据源、UI 不动**。
 *
 * 真机验收五态靠 debug 构建的模拟开关（见 [AccountStateSimulation]），不伪造后端。
 */
sealed interface AccountUiState {

    /** 未登录：头像占位 + 「未登录」+ 价值主张 + 「登录 / 注册」主按钮。 */
    data object SignedOut : AccountUiState

    /**
     * 已登录：头像 + 昵称 + 邮箱 + 同步状态行。
     *
     * 同步状态行随 [sync] 变化，覆盖「已同步 / 同步中 / 失败 / 离线」四态。
     */
    data class SignedIn(val email: String, val displayName: String, val sync: SyncUiStatus) : AccountUiState
}

/** 同步状态行的四态（设计回函 v3 §3：**一律行内提示，不弹窗**）。 */
sealed interface SyncUiStatus {

    /** 已同步 · N 分钟前。仅全量成功后更新 [lastSuccessAtMillis]。 */
    data class Synced(val lastSuccessAtMillis: Long) : SyncUiStatus

    /** 正在同步…（行内）；超过 [SYNC_CANCEL_AFTER_MS] 后行内出现「取消」。 */
    data object Syncing : SyncUiStatus

    /** 同步失败 · 重试（网络类失败退避重试后仍失败）。 */
    data object Failed : SyncUiStatus

    /** 离线，恢复联网后继续。 */
    data object Offline : SyncUiStatus

    companion object {
        /** 「正在同步…」超过该时长后行内出现「取消」（设计回函 v3 §3）。 */
        const val SYNC_CANCEL_AFTER_MS = 30_000L
    }
}

/** 相对时间的量级。UI 层按它选词条，避免把本地化文案写进纯逻辑。 */
enum class RelativeUnit { JUST_NOW, MINUTES, HOURS, DAYS }

/** 相对时间（[value] 仅对 MINUTES / HOURS / DAYS 有意义）。 */
data class RelativeTime(val unit: RelativeUnit, val value: Int)

/**
 * 「已同步 · N 分钟前」的相对时间换算（纯函数，可单测）。
 *
 * 设计回函给的模板是 `已同步 · %1$s前`，其中 `%1$s` 需要**已本地化的量词**；
 * 量词词条（分钟/小时/天 + 刚刚）由实现侧补齐并回传设计侧并入词条表。
 */
object AccountTimeFormat {

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    /**
     * @param nowMillis 当前时间
     * @param thenMillis 上次同步成功时间
     * @return 相对量级；[thenMillis] 晚于 [nowMillis]（时钟回拨）时按「刚刚」处理
     */
    fun relativeTime(nowMillis: Long, thenMillis: Long): RelativeTime {
        val delta = (nowMillis - thenMillis).coerceAtLeast(0L)
        return when {
            delta < MINUTE_MS -> RelativeTime(RelativeUnit.JUST_NOW, 0)
            delta < HOUR_MS -> RelativeTime(RelativeUnit.MINUTES, (delta / MINUTE_MS).toInt())
            delta < DAY_MS -> RelativeTime(RelativeUnit.HOURS, (delta / HOUR_MS).toInt())
            else -> RelativeTime(RelativeUnit.DAYS, (delta / DAY_MS).toInt())
        }
    }
}

/**
 * 阶段 A 的**验收用**状态模拟（debug 构建专用）。
 *
 * 账号体系在阶段 B 才落地，五态无法自然产生；这里按固定顺序轮转，使五态**可真机验收**，
 * 且不引入任何假装存在的后端。release 构建不暴露该入口（由 UI 层的 debuggable 判断把关）。
 */
object AccountStateSimulation {

    /** 模拟用的上次同步成功时间：固定「2 分钟前」，便于核对 `已同步 · 2 分钟前`。 */
    const val SIMULATED_SYNCED_AGO_MS = 2 * 60_000L

    private const val SIMULATED_EMAIL = "draftpeek@example.com"

    /**
     * 模拟昵称刻意用**语言中立**的假数据：它只在 debug 构建的验收路径上出现，
     * 不值得为它往 6 套 locale 词条表里塞一条（也会污染设计侧的词条表）。
     */
    private const val SIMULATED_NAME = "DraftPeek User"

    /** 轮转顺序：未登录 → 已同步 → 同步中 → 失败 → 离线 → 未登录。 */
    fun next(current: AccountUiState, nowMillis: Long): AccountUiState = when {
        current !is AccountUiState.SignedIn -> signedIn(SyncUiStatus.Synced(nowMillis - SIMULATED_SYNCED_AGO_MS))
        current.sync is SyncUiStatus.Synced -> signedIn(SyncUiStatus.Syncing)
        current.sync is SyncUiStatus.Syncing -> signedIn(SyncUiStatus.Failed)
        current.sync is SyncUiStatus.Failed -> signedIn(SyncUiStatus.Offline)
        else -> AccountUiState.SignedOut
    }

    /** 模拟账号的展示资料（阶段 B 由真实账号数据替换）。 */
    fun signedIn(sync: SyncUiStatus): AccountUiState.SignedIn = AccountUiState.SignedIn(
        email = SIMULATED_EMAIL,
        displayName = SIMULATED_NAME,
        sync = sync
    )
}
