package com.draftpeek.core.sync

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * [GitHttpClient] 的 OkHttp 实现 —— 方案 C 里真正发请求的那一层。
 *
 * **客户端由外部注入，本类不自己构造**。原因有两条：
 * 1. 仓库的证书锁定配置在 `core/common` 的 `RemotePolicyManager` / `GitHubApiClient`
 *    内部（`CertificatePinner`），**生产环境应注入那个带锁定的客户端**；
 *    若本类自建客户端就会绕过锁定，属于安全回退；
 * 2. 注入后本类可用 `MockWebServer` 做**真实 HTTP** 单测，不需要外网。
 *
 * 只做两件事：发请求、把响应规整成 [GitHttpResponse]（含限流判定）。
 * 错误分类交给 [SyncFailureMapper]，业务语义交给 [GitSyncTransport]。
 */
class OkHttpGitHttpClient(private val client: OkHttpClient) : GitHttpClient {

    override suspend fun get(url: String, headers: Map<String, String>): GitHttpResponse =
        execute(Request.Builder().url(url).headers(headers.toHeaders()).get().build())

    override suspend fun put(url: String, headers: Map<String, String>, jsonBody: String): GitHttpResponse = execute(
        Request.Builder()
            .url(url)
            .headers(headers.toHeaders())
            .put(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()
    )

    private fun execute(request: Request): GitHttpResponse {
        // 用 withTimeout 而不是给 OkHttpClient 加拦截器：超时策略对调用方可见、可单测
        val call = client.newCall(request)
        return try {
            call.execute().use { response ->
                GitHttpResponse(
                    statusCode = response.code,
                    body = response.body?.string().orEmpty(),
                    rateLimited = response.isRateLimited()
                )
            }
        } catch (io: IOException) {
            // 交给上层按类型分类（网络类可重试、证书类不可重试）
            throw io
        }
    }

    /**
     * 判定限流。
     *
     * GitHub 用 `X-RateLimit-Remaining: 0` 表示已用尽；**403 既可能是限流也可能是权限不足**，
     * 两者处置完全不同（前者可重试、后者不可），所以必须把限流单独识别出来。
     */
    private fun Response.isRateLimited(): Boolean = header("X-RateLimit-Remaining") == "0" ||
        header("Retry-After") != null

    private fun Map<String, String>.toHeaders(): okhttp3.Headers =
        okhttp3.Headers.Builder().apply { forEach { (name, value) -> add(name, value) } }.build()

    companion object {
        /** 单次请求超时。同步是后台行为，宁可早失败让状态机退避重试，也不要长时间挂住。 */
        const val DEFAULT_TIMEOUT_SECONDS: Long = 30L

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** 便捷构造：给注入的客户端加上本模块期望的超时（不改动调用方传入的实例）。 */
        fun withTimeout(base: OkHttpClient, seconds: Long = DEFAULT_TIMEOUT_SECONDS): OkHttpClient = base.newBuilder()
            .connectTimeout(seconds, TimeUnit.SECONDS)
            .readTimeout(seconds, TimeUnit.SECONDS)
            .writeTimeout(seconds, TimeUnit.SECONDS)
            .build()
    }
}
