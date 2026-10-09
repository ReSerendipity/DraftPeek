package com.draftpeek.core.sync

/**
 * 网络类失败的退避策略。
 *
 * 规格（设计回函 v3 §3）：**1s → 4s → 16s，最多 3 次**；仍失败则转「同步失败 · 重试」行内态。
 *
 * 纯数据 + 纯函数，便于单测与后续替换策略（如加抖动）。
 */
object SyncBackoff {

    /** 每次重试前的等待时长；下标 = 已失败次数 - 1。 */
    val DELAYS_MS: List<Long> = listOf(1_000L, 4_000L, 16_000L)

    /** 最多自动重试次数。 */
    val MAX_ATTEMPTS: Int = DELAYS_MS.size

    /**
     * 第 [attempt] 次重试前应等待的毫秒数（[attempt] 从 1 开始计）。
     *
     * @return 等待时长；已超出 [MAX_ATTEMPTS] 时返回 null（表示不再重试）
     */
    fun delayForAttempt(attempt: Int): Long? = DELAYS_MS.getOrNull(attempt - 1)
}
