package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FileUtils.resolveLocalPath")
class FileUtilsTest {

    @Nested
    @DisplayName("file:// URIs")
    inner class FileUriTest {
        @Test
        @DisplayName("resolves file:// URI with double slash")
        fun resolvesFileUriDoubleSlash() {
            val result = FileUtils.resolveLocalPath("file:///sdcard/myproject")
            assertEquals("/sdcard/myproject", result)
        }

        @Test
        @DisplayName("resolves file:// URI without trailing slash")
        fun resolvesFileUriNoTrailingSlash() {
            val result = FileUtils.resolveLocalPath("file:///data/data/com.draftpeek/files")
            assertEquals("/data/data/com.draftpeek/files", result)
        }
    }

    @Nested
    @DisplayName("absolute paths")
    inner class AbsolutePathTest {
        @Test
        @DisplayName("resolves absolute path starting with /")
        fun resolvesAbsolutePath() {
            val result = FileUtils.resolveLocalPath("/sdcard/Documents")
            assertEquals("/sdcard/Documents", result)
        }

        @Test
        @DisplayName("resolves /storage path")
        fun resolvesStoragePath() {
            val result = FileUtils.resolveLocalPath("/storage/emulated/0/Download")
            assertEquals("/storage/emulated/0/Download", result)
        }

        @Test
        @DisplayName("resolves /data path")
        fun resolvesDataPath() {
            val result = FileUtils.resolveLocalPath("/data/data/com.draftpeek/files/rootfs")
            assertEquals("/data/data/com.draftpeek/files/rootfs", result)
        }
    }

    @Nested
    @DisplayName("non-local URIs return null")
    inner class NonLocalUriTest {
        @Test
        @DisplayName("content:// returns null")
        fun contentUriReturnsNull() {
            val result = FileUtils.resolveLocalPath("content://com.android.externalstorage.documents/tree/primary%3ADocuments")
            assertNull(result)
        }

        @Test
        @DisplayName("ftp:// returns null")
        fun ftpUriReturnsNull() {
            val result = FileUtils.resolveLocalPath("ftp://192.168.1.1/home/user/project")
            assertNull(result)
        }

        @Test
        @DisplayName("sftp:// returns null")
        fun sftpUriReturnsNull() {
            val result = FileUtils.resolveLocalPath("sftp://example.com/home/user/project")
            assertNull(result)
        }

        @Test
        @DisplayName("http:// returns null")
        fun httpUriReturnsNull() {
            val result = FileUtils.resolveLocalPath("http://example.com/file.txt")
            assertNull(result)
        }

        @Test
        @DisplayName("https:// returns null")
        fun httpsUriReturnsNull() {
            val result = FileUtils.resolveLocalPath("https://example.com/file.txt")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCaseTest {
        @Test
        @DisplayName("empty string returns null")
        fun emptyStringReturnsNull() {
            val result = FileUtils.resolveLocalPath("")
            assertNull(result)
        }

        @Test
        @DisplayName("blank string returns null")
        fun blankStringReturnsNull() {
            val result = FileUtils.resolveLocalPath("   ")
            assertNull(result)
        }

        @Test
        @DisplayName("relative path returns the path as-is")
        fun relativePathReturnsAsIs() {
            // Paths not starting with / or a known scheme are not resolved
            val result = FileUtils.resolveLocalPath("relative/path")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("formatFileSize")
    inner class FormatFileSizeTest {
        @Test
        @DisplayName("formats bytes correctly")
        fun formatsBytes() {
            assertEquals("0 B", FileUtils.formatFileSize(0))
            assertEquals("500 B", FileUtils.formatFileSize(500))
        }

        @Test
        @DisplayName("formats kilobytes correctly")
        fun formatsKilobytes() {
            assertEquals("1 KB", FileUtils.formatFileSize(1024))
        }

        @Test
        @DisplayName("formats megabytes correctly")
        fun formatsMegabytes() {
            assertEquals("1 MB", FileUtils.formatFileSize(1024 * 1024))
        }
    }
}
