package com.draftpeek.core.sync

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 传输层单测：用假 [GitHttpClient] 覆盖成功 / 失败 / 冲突全部分支，**不需要网络**。
 */
@DisplayName("GitSyncTransport · 方案 C 载体")
class GitSyncTransportTest {

    private class FakeHttp(
        private val getResponses: MutableList<GitHttpResponse> = mutableListOf(),
        private val putResponses: MutableList<GitHttpResponse> = mutableListOf(),
        private val throwOnGet: Throwable? = null
    ) : GitHttpClient {

        val getUrls = mutableListOf<String>()
        val putBodies = mutableListOf<String>()
        val putUrls = mutableListOf<String>()

        override suspend fun get(url: String, headers: Map<String, String>): GitHttpResponse {
            getUrls += url
            throwOnGet?.let { throw it }
            return getResponses.removeFirstOrNull() ?: GitHttpResponse(404)
        }

        override suspend fun put(url: String, headers: Map<String, String>, jsonBody: String): GitHttpResponse {
            putUrls += url
            putBodies += jsonBody
            return putResponses.removeFirstOrNull() ?: GitHttpResponse(200)
        }
    }

    private fun transport(http: GitHttpClient, files: List<String> = listOf("sync.json")) = GitSyncTransport(
        host = GitHost.GITHUB,
        owner = "owner",
        repo = "repo",
        branch = "main",
        token = "tok",
        http = http,
        fileNames = files
    )

    // ── pull ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("pull：远端 404 → 成功且快照为空（首次同步不是错误）")
    fun pullMissingIsEmpty() = runTest {
        val outcome = transport(FakeHttp()).pull()
        assertEquals(PullOutcome.Success(SyncSnapshot.EMPTY), outcome)
    }

    @Test
    @DisplayName("pull：200 → 解码 base64 内容")
    fun pullDecodesContent() = runTest {
        val body = """{"sha":"abc","content":"aGVsbG8="}"""
        val outcome = transport(FakeHttp(mutableListOf(GitHttpResponse(200, body)))).pull()
        assertEquals(PullOutcome.Success(SyncSnapshot(mapOf("sync.json" to "hello"))), outcome)
    }

    @Test
    @DisplayName("pull：请求 URL 带 ref 指向配置的分支")
    fun pullUsesBranch() = runTest {
        val http = FakeHttp()
        transport(http).pull()
        assertEquals(
            "https://api.github.com/repos/owner/repo/contents/.draftpeek/sync.json?ref=main",
            http.getUrls.single()
        )
    }

    @Test
    @DisplayName("pull：401 → AUTH（需重新授权，不重试）")
    fun pullUnauthorized() = runTest {
        val outcome = transport(FakeHttp(mutableListOf(GitHttpResponse(401)))).pull()
        // Failure 是数据类且带 cause，故只比 kind
        assertEquals(SyncFailureKind.AUTH, (outcome as PullOutcome.Failure).kind)
    }

    @Test
    @DisplayName("pull：网络异常 → NETWORK（可重试）")
    fun pullNetworkFailure() = runTest {
        val outcome = transport(FakeHttp(throwOnGet = IOException("no network"))).pull()
        assertEquals(SyncFailureKind.NETWORK, (outcome as PullOutcome.Failure).kind)
    }

    // ── push ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("push：远端无此文件 → 请求体不带 sha（新建）")
    fun pushCreatesFile() = runTest {
        val http = FakeHttp()
        val result = transport(http).push(SyncSnapshot(mapOf("sync.json" to "hello")))
        assertEquals(TransferResult.Success, result)
        assertFalse(http.putBodies.single().contains("\"sha\""))
        assertEquals(
            "https://api.github.com/repos/owner/repo/contents/.draftpeek/sync.json",
            http.putUrls.single()
        )
    }

    @Test
    @DisplayName("push：远端已有文件 → 先取 sha 再带上（更新）")
    fun pushUpdatesFile() = runTest {
        val http = FakeHttp(mutableListOf(GitHttpResponse(200, """{"sha":"abc123"}""")))
        val result = transport(http).push(SyncSnapshot(mapOf("sync.json" to "hello")))
        assertEquals(TransferResult.Success, result)
        assertTrue(http.putBodies.single().contains("\"sha\":\"abc123\""))
    }

    @Test
    @DisplayName("push：409 → CONFLICT（取 sha 到写入之间远端被改动）")
    fun pushConflict() = runTest {
        val http = FakeHttp(
            getResponses = mutableListOf(GitHttpResponse(200, """{"sha":"abc"}""")),
            putResponses = mutableListOf(GitHttpResponse(409))
        )
        val result = transport(http).push(SyncSnapshot(mapOf("sync.json" to "hello")))
        assertEquals(TransferResult.Failure(SyncFailureKind.CONFLICT), result)
    }

    @Test
    @DisplayName("push：401 → AUTH；网络异常 → NETWORK")
    fun pushFailures() = runTest {
        val unauthorized = FakeHttp(putResponses = mutableListOf(GitHttpResponse(401)))
        assertEquals(
            TransferResult.Failure(SyncFailureKind.AUTH),
            transport(unauthorized).push(SyncSnapshot(mapOf("sync.json" to "x")))
        )

        val offline = FakeHttp(throwOnGet = IOException("offline"))
        val offlineResult = transport(offline).push(SyncSnapshot(mapOf("sync.json" to "x")))
        assertEquals(SyncFailureKind.NETWORK, (offlineResult as TransferResult.Failure).kind)
    }

    @Test
    @DisplayName("push：快照里没有的文件名被跳过（不产生多余请求）")
    fun pushSkipsAbsentFiles() = runTest {
        val http = FakeHttp()
        transport(http, files = listOf("a.json", "b.json"))
            .push(SyncSnapshot(mapOf("a.json" to "A")))
        assertEquals(1, http.putUrls.size)
        assertTrue(http.putUrls.single().endsWith("/a.json"))
    }

    @Test
    @DisplayName("多文件配置：逐个读写")
    fun multiFile() = runTest {
        val http = FakeHttp(
            mutableListOf(
                GitHttpResponse(200, """{"sha":"s1","content":"QQ=="}"""),
                GitHttpResponse(404)
            )
        )
        val outcome = transport(http, files = listOf("a.json", "b.json")).pull()
        assertEquals(
            PullOutcome.Success(SyncSnapshot(mapOf("a.json" to "A"))),
            outcome
        )
        assertEquals(2, http.getUrls.size)
    }
}
