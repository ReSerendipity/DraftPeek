package com.draftpeek.core.sync

/**
 * 把载体侧的失败翻译成 [SyncFailureKind]。
 *
 * 这是「方案 C」相对 BaaS/自建最不一样的一层：Git 载体引入了
 * **授权过期**（token 失效）、**仓库不可用**（无权限/配额/仓库被删）与
 * **冲突**（远端被别人改动）三类 BaaS 里不存在的失败形态。
 *
 * 纯函数，完全可单测。
 */
object SyncFailureMapper {

    /**
     * 按 HTTP 状态码分类。
     *
     * @param statusCode HTTP 状态码
     * @param rateLimited 是否因限流被拒（GitHub 用 `X-RateLimit-Remaining: 0` 判定）——
     *   限流是**可重试**的，而 403 通常代表权限不足（不可重试），必须区分开
     */
    fun fromHttpStatus(statusCode: Int, rateLimited: Boolean = false): SyncFailureKind = when {
        // 401：token 失效/被撤销 —— 需重新授权，重试无意义
        statusCode == 401 -> SyncFailureKind.AUTH
        // 403：限流可重试；否则视为仓库权限不足
        statusCode == 403 -> if (rateLimited) SyncFailureKind.NETWORK else SyncFailureKind.REPOSITORY
        // 404：仓库不存在 / 无权限访问（GitHub 对无权限的私有仓库也返回 404）
        statusCode == 404 -> SyncFailureKind.REPOSITORY
        // 409：Contents API 的「sha 不匹配」= 远端已被改动
        statusCode == 409 -> SyncFailureKind.CONFLICT
        // 429：显式限流
        statusCode == 429 -> SyncFailureKind.NETWORK
        // 5xx：服务端临时故障
        statusCode in 500..599 -> SyncFailureKind.NETWORK
        else -> SyncFailureKind.OTHER
    }

    /**
     * 按异常类型分类。
     *
     * 注意判定顺序：`SSLException` 是 `IOException` 的子类，
     * 必须先判**证书校验失败**（`SSLPeerUnverifiedException`，含证书锁定不匹配）
     * —— 那属于**不该重试**的安全问题，而不是网络抖动。
     */
    fun fromThrowable(throwable: Throwable): SyncFailureKind = when (throwable) {
        // 数据损坏：重试必然再败，必须走人工修复路径（设计 G7 · B8）
        is SyncDataCorruptException -> SyncFailureKind.CORRUPT
        is javax.net.ssl.SSLPeerUnverifiedException -> SyncFailureKind.OTHER
        is java.io.IOException -> SyncFailureKind.NETWORK
        else -> SyncFailureKind.OTHER
    }

    /** 该失败是否值得退避重试（与 [SyncMachine] 的判定保持一致）。 */
    fun isRetryable(kind: SyncFailureKind): Boolean = kind == SyncFailureKind.NETWORK
}
