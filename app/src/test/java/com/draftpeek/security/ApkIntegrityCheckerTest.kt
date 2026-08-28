package com.draftpeek.security

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ApkIntegrityChecker")
class ApkIntegrityCheckerTest {

    @Nested
    @DisplayName("IntegrityResult")
    inner class IntegrityResultTest {

        @Test
        @DisplayName("Verified is a data object")
        fun verifiedIsDataObject() {
            val result = ApkIntegrityChecker.IntegrityResult.Verified
            assertNotNull(result)
            assertEquals(ApkIntegrityChecker.IntegrityResult.Verified, result)
        }

        @Test
        @DisplayName("Tampered holds currentHash")
        fun tamperedHoldsHash() {
            val result = ApkIntegrityChecker.IntegrityResult.Tampered("ABC123")
            assertEquals("ABC123", result.currentHash)
        }

        @Test
        @DisplayName("Error holds exception")
        fun errorHoldsException() {
            val exception = IllegalStateException("test error")
            val result = ApkIntegrityChecker.IntegrityResult.Error(exception)
            assertEquals(exception, result.exception)
        }
    }

    @Nested
    @DisplayName("isVerified")
    inner class IsVerifiedTest {

        @Test
        @DisplayName("is a Boolean")
        fun isBoolean() {
            assertNotNull(ApkIntegrityChecker.isVerified)
        }
    }

    @Nested
    @DisplayName("spotCheck()")
    inner class SpotCheckTest {

        @Test
        @DisplayName("returns false when not verified and context returns defaults")
        fun returnsFalseWhenNotVerified() {
            val context = createMockContext()
            val result = ApkIntegrityChecker.spotCheck(context)
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("verify()")
    inner class VerifyTest {

        @Test
        @DisplayName("returns Error or Tampered in test environment")
        fun returnsErrorOrTamperedInTest() {
            val context = createMockContext()
            val result = ApkIntegrityChecker.verify(context)
            // In test environment, verify should not return Verified
            assertFalse(result is ApkIntegrityChecker.IntegrityResult.Verified)
        }
    }

    /**
     * Create a mock Context for unit testing.
     * Uses MockK with relaxed=true so all methods return default values.
     */
    private fun createMockContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val appInfo = ApplicationInfo()
        // sourceDir is null by default in ApplicationInfo
        every { context.applicationInfo } returns appInfo
        every { context.packageName } returns "com.draftpeek"
        return context
    }
}
