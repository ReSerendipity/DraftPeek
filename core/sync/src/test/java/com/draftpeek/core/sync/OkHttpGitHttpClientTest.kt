package com.draftpeek.core.sync

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [OkHttpGitHttpClient] 的真实 HTTP 测试：用 MockWebServer 起本地服务，**不走外网**。
 *
 * 这层是「接缝的真实实现」，所以值得单独测 —— 请求方法/头/体是否真的按 Git Contents API
 * 的要求发出去了，是假实现测不到的部分。
 */
@DisplayName("OkHttpGitHttpClient · 真实 HTTP")
class OkHttpGitHttpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var http: OkHttpGitHttpClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        http = OkHttpGitHttpClient(OkHttpGitHttpClient.withTimeout(OkHttpClient()))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun url(path: String): String = server.url(path).toString()

    @Test
    @DisplayName("GET：返回状态码与响应体")
    fun getReturnsBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"sha":"abc"}"""))

        val response = http.get(url("/contents/x"), mapOf("Authorization" to "Bearer tok"))

        assertEquals(200, response.statusCode)
        assertEquals("""{"sha":"abc"}""", response.body)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("Bearer tok", recorded.getHeader("Authorization"))
    }

    @Test
    @DisplayName("GET：404 原样返回（由上层判定为「远端无此文件」，不是异常）")
    fun getNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(404, http.get(url("/contents/x"), emptyMap()).statusCode)
    }

    @Test
    @DisplayName("PUT：真的把 JSON 体发出去，且方法/头正确")
    fun putSendsJsonBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"sha":"new"}"""))
        val body = """{"message":"m","content":"aGVsbG8=","branch":"main"}"""

        val response = http.put(url("/contents/x"), mapOf("Authorization" to "Bearer tok"), body)

        assertEquals(201, response.statusCode)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("Bearer tok", recorded.getHeader("Authorization"))
        assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    @DisplayName("限流判定：403 + X-RateLimit-Remaining: 0 → rateLimited=true（可重试）")
    fun detectsRateLimit() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).addHeader("X-RateLimit-Remaining", "0"))

        val response = http.get(url("/contents/x"), emptyMap())

        assertEquals(403, response.statusCode)
        assertTrue(response.rateLimited)
    }

    @Test
    @DisplayName("限流判定：403 但无限流头 → rateLimited=false（视为权限不足，不可重试）")
    fun forbiddenWithoutRateLimit() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        assertFalse(http.get(url("/contents/x"), emptyMap()).rateLimited)
    }

    @Test
    @DisplayName("限流判定：Retry-After 也算限流")
    fun retryAfterIsRateLimit() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "60"))
        assertTrue(http.get(url("/contents/x"), emptyMap()).rateLimited)
    }

    @Test
    @DisplayName("连接失败：抛 IOException（交给 SyncFailureMapper 判为可重试的网络类）")
    fun connectionFailureThrowsIoException() {
        val deadUrl = server.url("/contents/x").toString()
        server.shutdown() // 先关掉服务器，制造真实连接失败

        assertThrows(IOException::class.java) {
            kotlinx.coroutines.runBlocking { http.get(deadUrl, emptyMap()) }
        }
    }

    @Test
    @DisplayName("withTimeout：不改动传入的客户端实例（避免副作用）")
    fun withTimeoutDoesNotMutateInput() {
        val base = OkHttpClient()
        val derived = OkHttpGitHttpClient.withTimeout(base, seconds = 5L)
        assertTrue(derived !== base)
        assertEquals(OkHttpClient().connectTimeoutMillis, base.connectTimeoutMillis)
    }
}
