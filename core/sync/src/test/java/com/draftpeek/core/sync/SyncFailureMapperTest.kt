package com.draftpeek.core.sync

import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SyncFailureMapper · 载体失败分类（方案 C 特有形态）")
class SyncFailureMapperTest {

    @Nested
    @DisplayName("HTTP 状态码")
    inner class HttpStatus {

        @Test
        @DisplayName("401 → AUTH（token 失效/被撤销，重试无意义）")
        fun unauthorized() {
            assertEquals(SyncFailureKind.AUTH, SyncFailureMapper.fromHttpStatus(401))
        }

        @Test
        @DisplayName("403 非限流 → REPOSITORY（权限不足，重试无意义）")
        fun forbidden() {
            assertEquals(SyncFailureKind.REPOSITORY, SyncFailureMapper.fromHttpStatus(403))
        }

        @Test
        @DisplayName("403 限流 → NETWORK（可重试）—— 与权限不足必须区分")
        fun forbiddenByRateLimit() {
            assertEquals(
                SyncFailureKind.NETWORK,
                SyncFailureMapper.fromHttpStatus(403, rateLimited = true)
            )
        }

        @Test
        @DisplayName("404 → REPOSITORY（仓库不存在 / 无权限访问私有仓库）")
        fun notFound() {
            assertEquals(SyncFailureKind.REPOSITORY, SyncFailureMapper.fromHttpStatus(404))
        }

        @Test
        @DisplayName("409 → CONFLICT（Contents API 的 sha 不匹配 = 远端已被改动）")
        fun conflict() {
            assertEquals(SyncFailureKind.CONFLICT, SyncFailureMapper.fromHttpStatus(409))
        }

        @Test
        @DisplayName("429 与 5xx → NETWORK（可重试）")
        fun retryableServerSide() {
            assertEquals(SyncFailureKind.NETWORK, SyncFailureMapper.fromHttpStatus(429))
            assertEquals(SyncFailureKind.NETWORK, SyncFailureMapper.fromHttpStatus(500))
            assertEquals(SyncFailureKind.NETWORK, SyncFailureMapper.fromHttpStatus(503))
        }

        @Test
        @DisplayName("其他状态码 → OTHER")
        fun other() {
            assertEquals(SyncFailureKind.OTHER, SyncFailureMapper.fromHttpStatus(418))
        }
    }

    @Nested
    @DisplayName("异常类型")
    inner class ThrowableMapping {

        @Test
        @DisplayName("普通 IOException → NETWORK（可重试）")
        fun ioException() {
            assertEquals(
                SyncFailureKind.NETWORK,
                SyncFailureMapper.fromThrowable(IOException("connection reset"))
            )
            assertEquals(
                SyncFailureKind.NETWORK,
                SyncFailureMapper.fromThrowable(SocketTimeoutException("timeout"))
            )
        }

        @Test
        @DisplayName("证书校验失败 → OTHER（安全问题，**不该重试**）")
        fun certificateFailure() {
            assertEquals(
                SyncFailureKind.OTHER,
                SyncFailureMapper.fromThrowable(SSLPeerUnverifiedException("pin mismatch"))
            )
        }

        @Test
        @DisplayName("非 IO 异常 → OTHER")
        fun other() {
            assertEquals(
                SyncFailureKind.OTHER,
                SyncFailureMapper.fromThrowable(IllegalStateException("boom"))
            )
        }

        @Test
        @DisplayName("数据损坏异常 → CORRUPT（重试必然再败，不做退避）")
        fun corruptData() {
            assertEquals(
                SyncFailureKind.CORRUPT,
                SyncFailureMapper.fromThrowable(SyncDataCorruptException("bad json"))
            )
            // CORRUPT 不可重试（与 isRetryable 口径一致）
            assertEquals(false, SyncFailureMapper.isRetryable(SyncFailureKind.CORRUPT))
        }
    }

    @Test
    @DisplayName("只有 NETWORK 可退避重试")
    fun retryable() {
        assertTrue(SyncFailureMapper.isRetryable(SyncFailureKind.NETWORK))
        assertFalse(SyncFailureMapper.isRetryable(SyncFailureKind.AUTH))
        assertFalse(SyncFailureMapper.isRetryable(SyncFailureKind.REPOSITORY))
        assertFalse(SyncFailureMapper.isRetryable(SyncFailureKind.CONFLICT))
        assertFalse(SyncFailureMapper.isRetryable(SyncFailureKind.OTHER))
    }
}

@DisplayName("GitHost · 托管商差异")
class GitHostTest {

    @Test
    @DisplayName("GitHub：Bearer + API 版本头")
    fun githubHeaders() {
        val headers = GitHost.GITHUB.headers("tok")
        assertEquals("Bearer tok", headers["Authorization"])
        assertEquals("application/vnd.github+json", headers["Accept"])
        assertEquals("2022-11-28", headers["X-GitHub-Api-Version"])
    }

    @Test
    @DisplayName("Gitee：token 方案 + 无 API 版本头")
    fun giteeHeaders() {
        val headers = GitHost.GITEE.headers("tok")
        assertEquals("token tok", headers["Authorization"])
        assertFalse(headers.containsKey("X-GitHub-Api-Version"))
    }

    @Test
    @DisplayName("contents URL：含 owner/repo/path，带 ref 时追加查询参数")
    fun contentsUrl() {
        assertEquals(
            "https://api.github.com/repos/o/r/contents/.draftpeek/sync.json",
            GitHost.GITHUB.contentsUrl("o", "r", ".draftpeek/sync.json")
        )
        assertEquals(
            "https://gitee.com/api/v5/repos/o/r/contents/a.json?ref=main",
            GitHost.GITEE.contentsUrlWithRef("o", "r", "a.json", "main")
        )
    }
}

@DisplayName("GitPayload · Contents API 载荷编解码")
class GitPayloadTest {

    @Test
    @DisplayName("base64 往返：中文与换行都能还原")
    fun base64RoundTrip() {
        val content = "{\"note\":\"中文内容\"}\n第二行"
        assertEquals(content, GitPayload.decodeBase64(GitPayload.encodeBase64(content)))
    }

    @Test
    @DisplayName("解码时容忍 GitHub 返回的换行（经典坑）")
    fun decodeToleratesNewlines() {
        // "hello" 的 base64 是 aGVsbG8=，GitHub 会折行成 "aGVs\nbG8="
        assertEquals("hello", GitPayload.decodeBase64("aGVs\nbG8="))
        assertEquals("hello", GitPayload.decodeBase64("aGVs\r\nbG8=\n"))
    }

    @Test
    @DisplayName("从响应体里取 content 与 sha（content 是**转义形态**的 base64）")
    fun extractFields() {
        val body = """{"name":"sync.json","sha":"abc123","content":"aGVsbG8=\n","encoding":"base64"}"""
        // 原始 JSON 里换行是字面量 \n（反斜杠 + n），捕获到的也是这个形态
        assertEquals("aGVsbG8=\\n", GitPayload.contentOf(body))
        assertEquals("abc123", GitPayload.shaOf(body))
    }

    @Test
    @DisplayName("端到端：从响应体直接解出原文（覆盖「转义 base64 必须先反转义」这个真 bug）")
    fun decodeFromResponseBody() {
        val body = """{"sha":"abc","content":"aGVsbG8=\n"}"""
        assertEquals("hello", GitPayload.decodeBase64(GitPayload.contentOf(body)!!))
    }

    @Test
    @DisplayName("反转义：\\n 与 \\\\ 都能还原")
    fun unescape() {
        assertEquals("a\nb", GitPayload.unescape("a\\nb"))
        assertEquals("a\\b", GitPayload.unescape("a\\\\b"))
    }

    @Test
    @DisplayName("字段缺失时返回 null（调用方按「远端无此文件」处理）")
    fun missingFields() {
        assertEquals(null, GitPayload.contentOf("""{"name":"x"}"""))
        assertEquals(null, GitPayload.shaOf("""{"name":"x"}"""))
    }

    @Test
    @DisplayName("putBody：新建文件**不带 sha**，更新文件带 sha")
    fun putBody() {
        val created = GitPayload.putBody("msg", "hello", "main", sha = null)
        assertFalse(created.contains("\"sha\""))
        assertTrue(created.contains("\"content\":\"aGVsbG8=\""))
        assertTrue(created.contains("\"branch\":\"main\""))

        val updated = GitPayload.putBody("msg", "hello", "main", sha = "abc")
        assertTrue(updated.contains("\"sha\":\"abc\""))
    }

    @Test
    @DisplayName("putBody：message 里的引号与换行被正确转义")
    fun putBodyEscapes() {
        val body = GitPayload.putBody("say \"hi\"\nnext", "x", "main")
        assertTrue(body.contains("say \\\"hi\\\"\\nnext"))
    }
}
