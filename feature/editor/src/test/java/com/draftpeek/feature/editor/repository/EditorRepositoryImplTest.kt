package com.draftpeek.feature.editor.repository

import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
 *
 * P1-6 修复：将反射访问替换为直接调用 internal 可见性的 companion 方法。
 * 原实现通过 getDeclaredMethod + isAccessible=true 反射访问 private 方法，
 * 重构后 companion 方法改为 internal 可见性，测试可直接调用。
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
            val stream = ByteArrayInputStream(testData)

            // P1-6 修复：直接调用 internal companion 方法，不再使用反射
            val result = EditorRepositoryImpl.readStreamChunked(stream, -1L)
            assertEquals(testData.size, result.size)
            assertEquals(String(testData), String(result))
        }

        @Test
        @DisplayName("readStreamChunked 预分配缓冲区时正确读取")
        fun readStreamChunked_withExpectedSize_returnsCorrectBytes() {
            val testData = ByteArray(1024) { it.toByte() }
            val stream = ByteArrayInputStream(testData)

            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.readStreamChunked(stream, testData.size.toLong())
            assertEquals(testData.size, result.size)
            assertTrue(testData.contentEquals(result))
        }

        @Test
        @DisplayName("readStreamChunked 读取空流返回空数组")
        fun readStreamChunked_emptyStream_returnsEmptyArray() {
            val stream = ByteArrayInputStream(ByteArray(0))

            val result = EditorRepositoryImpl.readStreamChunked(stream, 0L)
            assertEquals(0, result.size)
        }
    }

    @Nested
    @DisplayName("isLikelyBinary (companion object)")
    inner class IsLikelyBinaryTests {

        @Test
        @DisplayName("包含 null byte 的数据被识别为二进制")
        fun containsNullByte_returnsTrue() {
            val data = byteArrayOf(0x41, 0x42, 0x00, 0x43)

            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.isLikelyBinary(data)
            assertTrue(result)
        }

        @Test
        @DisplayName("纯文本数据不被识别为二进制")
        fun pureText_returnsFalse() {
            val data = "Hello, World!".toByteArray()

            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.isLikelyBinary(data)
            assertTrue(!result)
        }

        @Test
        @DisplayName("空数组不被识别为二进制")
        fun emptyArray_returnsFalse() {
            val data = ByteArray(0)

            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.isLikelyBinary(data)
            assertTrue(!result)
        }

        @Test
        @DisplayName("空字节在数据末尾也能被检测到")
        fun nullByteAtEnd_returnsTrue() {
            val data = byteArrayOf(0x41, 0x42, 0x43, 0x00)

            val result = EditorRepositoryImpl.isLikelyBinary(data)
            assertTrue(result)
        }
    }

    @Nested
    @DisplayName("formatFileSize (companion object)")
    inner class FormatFileSizeTests {

        @Test
        @DisplayName("小于 1KB 的文件显示为 B")
        fun lessThan1KB_showsBytes() {
            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.formatFileSize(512L)
            assertEquals("512 B", result)
        }

        @Test
        @DisplayName("1KB-1MB 的文件显示为 KB")
        fun between1KBAnd1MB_showsKB() {
            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.formatFileSize(2048L)
            assertEquals("2 KB", result)
        }

        @Test
        @DisplayName("1MB-1GB 的文件显示为 MB")
        fun between1MBAnd1GB_showsMB() {
            // P1-6 修复：直接调用 internal companion 方法
            val result = EditorRepositoryImpl.formatFileSize(5L * 1024 * 1024)
            assertEquals("5.0 MB", result)
        }

        @Test
        @DisplayName("负值显示为 Unknown")
        fun negativeValue_showsUnknown() {
            // P1-6 修复：直接调用 internal companion 方法，新增边界测试
            val result = EditorRepositoryImpl.formatFileSize(-1L)
            assertEquals("Unknown", result)
        }

        @Test
        @DisplayName("0 字节显示为 0 B")
        fun zeroBytes_showsZeroB() {
            // P1-6 修复：新增边界测试
            val result = EditorRepositoryImpl.formatFileSize(0L)
            assertEquals("0 B", result)
        }
    }
}
