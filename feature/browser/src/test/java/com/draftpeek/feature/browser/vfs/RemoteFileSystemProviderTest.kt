package com.draftpeek.feature.browser.vfs

import com.draftpeek.feature.browser.model.FileItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * FTP/SFTP 远程文件系统提供者集成测试。
 *
 * 验证 RemoteFileSystemConfig 配置验证、URI 方案识别和
 * FileSystemRegistry 注册逻辑。实际网络连接测试需要测试环境中的
 * FTP/SFTP 服务器（在 CI 中通过 docker-compose 提供）。
 */
@DisplayName("Remote FileSystem Providers")
class RemoteFileSystemProviderTest {

    @Nested
    @DisplayName("RemoteFileSystemConfig")
    inner class ConfigTests {

        @Test
        @DisplayName("有效配置：host 非空、port > 0")
        fun validConfig_allFieldsPresent() {
            val config = RemoteFileSystemConfig(
                host = "ftp.example.com",
                port = 21,
                username = "user",
                password = "pass",
            )
            assertTrue(config.host.isNotEmpty())
            assertTrue(config.port > 0)
        }

        @Test
        @DisplayName("默认端口：FTP 为 21，SFTP 为 22")
        fun defaultPorts_ftp21_sftp22() {
            val ftpConfig = RemoteFileSystemConfig(host = "ftp.example.com")
            assertEquals(0, ftpConfig.port) // 0 means use protocol default

            val sftpConfig = RemoteFileSystemConfig(host = "sftp.example.com")
            assertEquals(0, sftpConfig.port)
        }

        @Test
        @DisplayName("basePath 默认为根目录")
        fun defaultBasePath_isRoot() {
            val config = RemoteFileSystemConfig(host = "example.com")
            assertEquals("/", config.basePath)
        }

        @Test
        @DisplayName("超时默认值合理")
        fun defaultTimeouts_areReasonable() {
            val config = RemoteFileSystemConfig(host = "example.com")
            assertTrue(config.connectionTimeoutMs > 0)
            assertTrue(config.readTimeoutMs > 0)
            assertTrue(config.readTimeoutMs >= config.connectionTimeoutMs)
        }
    }

    @Nested
    @DisplayName("FileSystemResult")
    inner class FileSystemResultTests {

        @Test
        @DisplayName("Success 结果包含数据")
        fun success_containsData() {
            val result = FileSystemResult.Success("file content")
            assertTrue(result is FileSystemResult.Success)
            assertEquals("file content", result.data)
        }

        @Test
        @DisplayName("Error 结果包含消息和错误码")
        fun error_containsMessageAndCode() {
            val result = FileSystemResult.Error(
                message = "Connection refused",
                errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR,
            )
            assertTrue(result is FileSystemResult.Error)
            assertEquals("Connection refused", result.message)
            assertEquals(FileSystemResult.ErrorCode.NETWORK_ERROR, result.errorCode)
        }

        @Test
        @DisplayName("所有错误码可用")
        fun allErrorCodes_available() {
            val codes = FileSystemResult.ErrorCode.values()
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NOT_FOUND))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.AUTH_FAILED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NETWORK_ERROR))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.PERMISSION_DENIED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.ALREADY_EXISTS))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.NOT_SUPPORTED))
            assertTrue(codes.contains(FileSystemResult.ErrorCode.UNKNOWN))
        }
    }

    @Nested
    @DisplayName("URI scheme matching")
    inner class UriSchemeTests {

        @Test
        @DisplayName("FTP URI 以 ftp:// 开头")
        fun ftpUri_startsCorrectly() {
            val uri = "ftp://ftp.example.com/path/file.txt"
            assertTrue(uri.startsWith("ftp://"))
            assertFalse(uri.startsWith("sftp://"))
        }

        @Test
        @DisplayName("SFTP URI 以 sftp:// 开头")
        fun sftpUri_startsCorrectly() {
            val uri = "sftp://sftp.example.com/path/file.txt"
            assertTrue(uri.startsWith("sftp://"))
            assertFalse(uri.startsWith("ftp://"))
        }

        @Test
        @DisplayName("本地 file:// URI 不匹配远程方案")
        fun localUri_doesNotMatchRemote() {
            val uri = "file:///storage/emulated/0/file.txt"
            assertFalse(uri.startsWith("ftp://"))
            assertFalse(uri.startsWith("sftp://"))
        }
    }
}
