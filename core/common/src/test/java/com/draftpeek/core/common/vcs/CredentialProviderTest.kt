package com.draftpeek.core.common.vcs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CredentialProvider.GitCredential 数据类单元测试。
 *
 * 验证凭据有效性判断、Token/BasicAuth 优先级、异常类型等。
 */
@DisplayName("CredentialProvider.GitCredential")
class CredentialProviderTest {

    @Nested
    @DisplayName("Token 认证")
    inner class TokenAuthTest {

        @Test
        @DisplayName("仅有 Token 时 hasToken 应为 true")
        fun should_hasTokenBeTrue_when_onlyTokenProvided() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                token = "ghp_xxxx",
            )
            assertTrue(cred.hasToken)
            assertFalse(cred.hasBasicAuth)
            assertTrue(cred.isValid)
        }

        @Test
        @DisplayName("空 Token 时 hasToken 应为 false")
        fun should_hasTokenBeFalse_when_emptyToken() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                token = "",
            )
            assertFalse(cred.hasToken)
            assertFalse(cred.isValid)
        }

        @Test
        @DisplayName("空白 Token 时 hasToken 应为 false")
        fun should_hasTokenBeFalse_when_blankToken() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                token = "   ",
            )
            assertFalse(cred.hasToken)
        }
    }

    @Nested
    @DisplayName("BasicAuth 认证")
    inner class BasicAuthTest {

        @Test
        @DisplayName("有用户名和密码时 hasBasicAuth 应为 true")
        fun should_hasBasicAuthBeTrue_when_usernameAndPasswordProvided() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                username = "user",
                password = "pass",
            )
            assertTrue(cred.hasBasicAuth)
            assertFalse(cred.hasToken)
            assertTrue(cred.isValid)
        }

        @Test
        @DisplayName("仅有用户名时 hasBasicAuth 应为 false")
        fun should_hasBasicAuthBeFalse_when_onlyUsername() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                username = "user",
            )
            assertFalse(cred.hasBasicAuth)
            assertFalse(cred.isValid)
        }

        @Test
        @DisplayName("仅有密码时 hasBasicAuth 应为 false")
        fun should_hasBasicAuthBeFalse_when_onlyPassword() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                password = "pass",
            )
            assertFalse(cred.hasBasicAuth)
            assertFalse(cred.isValid)
        }
    }

    @Nested
    @DisplayName("混合认证")
    inner class MixedAuthTest {

        @Test
        @DisplayName("同时有 Token 和 BasicAuth 时两者都为 true")
        fun should_bothBeTrue_when_bothProvided() {
            val cred = CredentialProvider.GitCredential(
                host = "github.com",
                username = "user",
                password = "pass",
                token = "ghp_xxxx",
            )
            assertTrue(cred.hasToken)
            assertTrue(cred.hasBasicAuth)
            assertTrue(cred.isValid)
        }
    }

    @Nested
    @DisplayName("CredentialLockedException")
    inner class CredentialLockedExceptionTest {

        @Test
        @DisplayName("应正确创建并携带原因异常")
        fun should_createWithCause() {
            val cause = RuntimeException("keystore error")
            val exception = CredentialProvider.CredentialLockedException(cause = cause)
            assertNotNull(exception)
            assertEquals(cause, exception.cause)
            assertTrue(exception.message!!.contains("Keystore"))
        }

        @Test
        @DisplayName("无原因时也应可创建")
        fun should_createWithoutCause() {
            val exception = CredentialProvider.CredentialLockedException()
            assertNotNull(exception)
            assertNull(exception.cause)
        }
    }
}
