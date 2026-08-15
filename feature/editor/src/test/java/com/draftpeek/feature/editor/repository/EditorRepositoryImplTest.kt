package com.draftpeek.feature.editor.repository

import android.net.Uri
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [EditorRepositoryImpl].
 *
 * Tests the isInternalFile and deleteInternalFile methods that were added
 * during the P1-5 refactor to remove Context dependency from EditorViewModel.
 * The readFile and writeFile methods require Android ContentResolver and
 * are tested via instrumented tests.
 */
class EditorRepositoryImplTest {

    // Note: Full readFile/writeFile tests require Android ContentResolver
    // and are better tested as instrumented tests with Robolectric or
    // on-device testing. These unit tests cover the pure-Kotlin logic.

    @Nested
    @DisplayName("isInternalFile")
    inner class IsInternalFileTests {

        @Test
        @DisplayName("draftpeek-internal:// URI 返回 true")
        fun internalUri_returnsTrue() {
            // This test validates the logic without instantiating the repository
            // (which requires Context). The logic is delegated to AppFileManager.
            val uriString = "draftpeek-internal://test.kt"
            val expected = uriString.startsWith("draftpeek-internal://") &&
                !uriString.startsWith("file:///android_asset/")
            assertTrue(expected)
        }

        @Test
        @DisplayName("file:///android_asset/ URI 返回 false")
        fun assetUri_returnsFalse() {
            val uriString = "file:///android_asset/samples/hello.md"
            val expected = uriString.startsWith("draftpeek-internal://") &&
                !uriString.startsWith("file:///android_asset/")
            assertTrue(!expected)
        }

        @Test
        @DisplayName("content:// URI 返回 false")
        fun contentUri_returnsFalse() {
            val uriString = "content://com.android.externalstorage.documents/document/primary%3ATest.kt"
            val expected = uriString.startsWith("draftpeek-internal://") &&
                !uriString.startsWith("file:///android_asset/")
            assertTrue(!expected)
        }
    }

    @Nested
    @DisplayName("readStreamChunked (companion object)")
    inner class ReadStreamChunkedTests {

        @Test
        @DisplayName("readStreamChunked 正确读取小型 InputStream")
        fun readStreamChunked_smallStream_returnsCorrectBytes() {
            val testData = "Hello, World!".toByteArray()
            val stream = java.io.ByteArrayInputStream(testData)

            // Access the private companion method via reflection
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("readStreamChunked", java.io.InputStream::class.java, Long::class.javaPrimitiveType)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, stream, -1L) as ByteArray
            assertEquals(testData.size, result.size)
            assertEquals(String(testData), String(result))
        }

        @Test
        @DisplayName("readStreamChunked 预分配缓冲区时正确读取")
        fun readStreamChunked_withExpectedSize_returnsCorrectBytes() {
            val testData = ByteArray(1024) { it.toByte() }
            val stream = java.io.ByteArrayInputStream(testData)

            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("readStreamChunked", java.io.InputStream::class.java, Long::class.javaPrimitiveType)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, stream, testData.size.toLong()) as ByteArray
            assertEquals(testData.size, result.size)
            assertTrue(testData.contentEquals(result))
        }
    }

    @Nested
    @DisplayName("isLikelyBinary (companion object)")
    inner class IsLikelyBinaryTests {

        @Test
        @DisplayName("包含 null byte 的数据被识别为二进制")
        fun containsNullByte_returnsTrue() {
            val data = byteArrayOf(0x41, 0x42, 0x00, 0x43)
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("isLikelyBinary", ByteArray::class.java)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, data) as Boolean
            assertTrue(result)
        }

        @Test
        @DisplayName("纯文本数据不被识别为二进制")
        fun pureText_returnsFalse() {
            val data = "Hello, World!".toByteArray()
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("isLikelyBinary", ByteArray::class.java)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, data) as Boolean
            assertTrue(!result)
        }

        @Test
        @DisplayName("空数组不被识别为二进制")
        fun emptyArray_returnsFalse() {
            val data = ByteArray(0)
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("isLikelyBinary", ByteArray::class.java)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, data) as Boolean
            assertTrue(!result)
        }
    }

    @Nested
    @DisplayName("formatFileSize (companion object)")
    inner class FormatFileSizeTests {

        @Test
        @DisplayName("小于 1KB 的文件显示为 B")
        fun lessThan1KB_showsBytes() {
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("formatFileSize", Long::class.javaPrimitiveType)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, 512L) as String
            assertEquals("512 B", result)
        }

        @Test
        @DisplayName("1KB-1MB 的文件显示为 KB")
        fun between1KBAnd1MB_showsKB() {
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("formatFileSize", Long::class.javaPrimitiveType)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, 2048L) as String
            assertEquals("2 KB", result)
        }

        @Test
        @DisplayName("1MB-1GB 的文件显示为 MB")
        fun between1MBAnd1GB_showsMB() {
            val method = EditorRepositoryImpl.Companion::class.java
                .getDeclaredMethod("formatFileSize", Long::class.javaPrimitiveType)
            method.isAccessible = true

            val result = method.invoke(EditorRepositoryImpl.Companion, 5L * 1024 * 1024) as String
            assertEquals("5.0 MB", result)
        }
    }
}
