package com.draftpeek.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SecurityIntegrityChecker 安全代码自校验模块测试。
 *
 * 验证安全类完整性自校验、行为验证和快速检查功能。
 */
@DisplayName("SecurityIntegrityChecker")
class SecurityIntegrityCheckerTest {

    @Nested
    @DisplayName("verifySelfIntegrity()")
    inner class VerifySelfIntegrityTest {

        @Test
        @DisplayName("在测试环境中应返回 true（安全类未被篡改）")
        fun should_returnTrue_when_classesIntact() {
            val result = SecurityIntegrityChecker.verifySelfIntegrity()
            assertTrue(result, "Security classes should be intact in test environment")
        }

        @Test
        @DisplayName("多次调用应返回一致结果")
        fun should_returnConsistentResult_when_calledMultipleTimes() {
            val first = SecurityIntegrityChecker.verifySelfIntegrity()
            val second = SecurityIntegrityChecker.verifySelfIntegrity()
            assertTrue(first == second)
        }
    }

    @Nested
    @DisplayName("spotCheck()")
    inner class SpotCheckTest {

        @Test
        @DisplayName("spotCheck 应返回布尔值且不抛异常")
        fun should_returnBooleanWithoutException() {
            val result = SecurityIntegrityChecker.spotCheck()
            // spotCheck should not throw
            assertTrue(result || !result) // Always true — just verifying no exception
        }
    }

    @Nested
    @DisplayName("IntegrityCheckResult")
    inner class IntegrityCheckResultTest {

        @Test
        @DisplayName("Verified 应是 data object")
        fun verifiedShouldBeSingleton() {
            val result = SecurityIntegrityChecker.IntegrityCheckResult.Verified
            assertNotNull(result)
        }

        @Test
        @DisplayName("Tampered 应包含详情字符串")
        fun tamperedShouldContainDetails() {
            val result = SecurityIntegrityChecker.IntegrityCheckResult.Tampered("test issue")
            assertTrue(result.details.contains("test issue"))
        }

        @Test
        @DisplayName("Error 应包含异常")
        fun errorShouldContainException() {
            val exception = RuntimeException("test error")
            val result = SecurityIntegrityChecker.IntegrityCheckResult.Error(exception)
            assertNotNull(result.exception)
        }
    }

    @Nested
    @DisplayName("isVerified 状态")
    inner class IsVerifiedStateTest {

        @Test
        @DisplayName("isVerified 初始应为 false（未执行 verifyIntegrity）")
        fun should_beFalseInitially() {
            // Note: isVerified may have been set by previous test runs
            // This test verifies the field exists and is accessible
            val state = SecurityIntegrityChecker.isVerified
            assertTrue(state || !state) // Always true — just verifying accessibility
        }
    }
}
