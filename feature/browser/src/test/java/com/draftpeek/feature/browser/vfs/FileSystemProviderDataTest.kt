package com.draftpeek.feature.browser.vfs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * FileSystemResult 和 RemoteFileSystemConfig 单元测试。
 *
 * 验证密封结果类、错误码、远程文件系统配置等核心数据结构。
 */
@DisplayName("FileSystemProvider — 数据结构")
class FileSystemProviderDataTest {

    @Nested
    @DisplayName("FileSystemResult")
    inner class FileSystemResultTest {

        @Test
        @DisplayName("Success 应包含数据")
        fun should_containData_inSuccess() {
            val result = FileSystemResult.Success("file content")
            assertEquals("file content", result.data)
        }

        @Test
        @DisplayName("Error 应包含消息和错误码")
        fun should_containMessageAndErrorCode_inError() {
            val result = FileSystemResult.Error(
                message = "File not found",
                errorCode = FileSystemResult.ErrorCode.NOT_FOUND
            )
            assertEquals("File not found", result.message)
            assertEquals(FileSystemResult.ErrorCode.NOT_FOUND, result.errorCode)
            assertNull(result.cause)
        }

        @Test
        @DisplayName("Error 应可选包含异常原因")
        fun should_optionallyContainCause_inError() {
            val cause = RuntimeException("underlying error")
            val result = FileSystemResult.Error(
                message = "Operation failed",
                cause = cause,
                errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR
            )
            assertNotNull(result.cause)
            assertEquals(cause, result.cause)
        }

        @Test
        @DisplayName("Error 默认错误码应为 UNKNOWN")
        fun should_defaultToUnknownErrorCode() {
            val result = FileSystemResult.Error(message = "something went wrong")
            assertEquals(FileSystemResult.ErrorCode.UNKNOWN, result.errorCode)
        }
    }

    @Nested
    @DisplayName("ErrorCode")
    inner class ErrorCodeTest {

        @Test
        @DisplayName("应包含所有预期的错误码值")
        fun should_containAllExpectedErrorCodes() {
            val codes = FileSystemResult.ErrorCode.entries
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NOT_FOUND))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.AUTH_FAILED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NETWORK_ERROR))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.PERMISSION_DENIED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.ALREADY_EXISTS))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NOT_SUPPORTED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.UNKNOWN))
            assertEquals(7, codes.size)
        }
    }

    @Nested
    @DisplayName("RemoteFileSystemConfig")
    inner class RemoteFileSystemConfigTest {

        @Test
        @DisplayName("默认值应正确设置")
        fun should_haveCorrectDefaults() {
            val config = RemoteFileSystemConfig(host = "example.com")
            assertEquals("example.com", config.host)
            assertEquals(0, config.port)
            assertEquals("", config.username)
            assertEquals("", config.password)
            assertEquals("/", config.basePath)
            assertTrue(config.useEncryption)
            assertEquals(10_000, config.connectionTimeoutMs)
            assertEquals(30_000, config.readTimeoutMs)
        }

        @Test
        @DisplayName("自定义值应正确设置")
        fun should_setCustomValues() {
            val config = RemoteFileSystemConfig(
                host = "ftp.example.com",
                port = 2121,
                username = "user",
                password = "pass",
                basePath = "/home/user",
                useEncryption = false,
                connectionTimeoutMs = 5_000,
                readTimeoutMs = 15_000
            )
            assertEquals("ftp.example.com", config.host)
            assertEquals(2121, config.port)
            assertEquals("user", config.username)
            assertEquals("pass", config.password)
            assertEquals("/home/user", config.basePath)
            assertFalse(config.useEncryption)
            assertEquals(5_000, config.connectionTimeoutMs)
            assertEquals(15_000, config.readTimeoutMs)
        }

        @Test
        @DisplayName("相同配置应相等")
        fun should_beEqual_when_sameConfig() {
            val config1 = RemoteFileSystemConfig(host = "example.com", port = 21)
            val config2 = RemoteFileSystemConfig(host = "example.com", port = 21)
            assertEquals(config1, config2)
        }
    }
}
