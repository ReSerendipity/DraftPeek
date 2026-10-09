package com.draftpeek.core.sync

/** 最小 HTTP 响应 —— 只保留传输层需要的部分。 */
data class GitHttpResponse(
    val statusCode: Int,
    val body: String = "",
    /**
     * 是否因限流被拒。
     *
     * GitHub 用 `X-RateLimit-Remaining: 0` 判定；Gitee 无此头时恒为 false。
     * 需要区分它是因为 **403 可能是限流（可重试）也可能是权限不足（不可重试）**。
     */
    val rateLimited: Boolean = false
)

/**
 * 最小 HTTP 接缝。
 *
 * 把 OkHttp / 证书锁定（`RemotePolicyManager`）/ 超时与重试策略留给基础设施层注入，
 * 好处是**传输逻辑可完全单测**——用假实现即可覆盖全部成功/失败/冲突分支，不需要网络。
 */
interface GitHttpClient {

    suspend fun get(url: String, headers: Map<String, String>): GitHttpResponse

    suspend fun put(url: String, headers: Map<String, String>, jsonBody: String): GitHttpResponse
}
