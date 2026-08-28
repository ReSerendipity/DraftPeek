package com.draftpeek.security

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DexIntegrityChecker")
class DexIntegrityCheckerTest {

    @Nested
    @DisplayName("DexResult")
    inner class DexResultTest {

        @Test
        @DisplayName("Verified is a data object")
        fun verifiedIsDataObject() {
            val result = DexIntegrityChecker.DexResult.Verified
            assertNotNull(result)
            assertEquals(DexIntegrityChecker.DexResult.Verified, result)
        }

        @Test
        @DisplayName("Tampered holds details")
        fun tamperedHoldsDetails() {
            val result = DexIntegrityChecker.DexResult.Tampered("test details")
            assertEquals("test details", result.details)
        }

        @Test
        @DisplayName("Error holds exception")
        fun errorHoldsException() {
            val exception = IllegalStateException("test error")
            val result = DexIntegrityChecker.DexResult.Error(exception)
            assertEquals(exception, result.exception)
        }
    }

    @Nested
    @DisplayName("DexFileInfo")
    inner class DexFileInfoTest {

        @Test
        @DisplayName("holds all fields correctly")
        fun holdsAllFields() {
            val info = DexIntegrityChecker.DexFileInfo(
                name = "classes.dex",
                crc32 = 12345L,
                sha256 = "ABCDEF",
                size = 1024L
            )
            assertEquals("classes.dex", info.name)
            assertEquals(12345L, info.crc32)
            assertEquals("ABCDEF", info.sha256)
            assertEquals(1024L, info.size)
        }
    }

    @Nested
    @DisplayName("isVerified")
    inner class IsVerifiedTest {

        @Test
        @DisplayName("is a Boolean")
        fun isBoolean() {
            assertNotNull(DexIntegrityChecker.isVerified)
        }
    }

    @Nested
    @DisplayName("verify()")
    inner class VerifyTest {

        @Test
        @DisplayName("returns Error when APK path is null")
        fun returnsErrorWhenApkPathNull() {
            val context = createMockContext()
            val result = DexIntegrityChecker.verify(context)
            // sourceDir is null, so verify should return Error
            assertTrue(result is DexIntegrityChecker.DexResult.Error)
        }
    }

    @Nested
    @DisplayName("quickVerify()")
    inner class QuickVerifyTest {

        @Test
        @DisplayName("returns false in test environment")
        fun returnsFalseInTest() {
            val context = createMockContext()
            assertFalse(DexIntegrityChecker.quickVerify(context))
        }
    }

    @Nested
    @DisplayName("getDexFileInfo()")
    inner class GetDexFileInfoTest {

        @Test
        @DisplayName("returns empty list when APK path is null")
        fun returnsEmptyWhenApkPathNull() {
            val context = createMockContext()
            val result = DexIntegrityChecker.getDexFileInfo(context)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("computeDexSha256()")
    inner class ComputeDexSha256Test {

        @Test
        @DisplayName("returns null when APK path is null")
        fun returnsNullWhenApkPathNull() {
            val context = createMockContext()
            val result = DexIntegrityChecker.computeDexSha256(context)
            assertEquals(null, result)
        }
    }

    /**
     * Create a mock Context with null sourceDir for unit testing.
     */
    private fun createMockContext(): Context {
        val context = mockk<Context>(relaxed = true)
        val appInfo = ApplicationInfo()
        // sourceDir is null by default in ApplicationInfo
        every { context.applicationInfo } returns appInfo
        return context
    }
}
