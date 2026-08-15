package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Pure JVM unit tests for [AppFileManager]. Methods that require an Android
 * [android.content.Context] (e.g. `getUserFilesDir`, `createUserFile`,
 * `getInternalFileFromUri`) are covered indirectly via instrumentation tests
 * or skipped here to avoid pulling Robolectric/mockk into core/common.
 *
 * Note: [AppFileManager.isInternalUri] uses hardcoded `/` separators because
 * Android paths are always POSIX-style. Platform-specific path assertions are
 * therefore skipped on non-POSIX hosts.
 */
@DisplayName("AppFileManager")
class AppFileManagerTest {

    private val isPosixFileSystem: Boolean
        get() = File.separatorChar == '/'

    @Nested
    @DisplayName("fileToInternalUri()")
    inner class FileToInternalUriTest {

        @Test
        @DisplayName("prefixes file:// to absolute path")
        fun prefixesFileScheme(@TempDir tempDir: Path) {
            val file = tempDir.resolve("example.txt").toFile()
            file.writeText("hi")

            val uri = AppFileManager.fileToInternalUri(file)

            assertTrue(uri.startsWith("file://"), "Expected file:// prefix, got $uri")
            assertTrue(uri.endsWith("example.txt"))
        }
    }

    @Nested
    @DisplayName("isInternalUri()")
    inner class IsInternalUriTest {

        @Test
        @DisplayName("non-file:// scheme returns false")
        fun rejectsContentScheme() {
            assertFalse(AppFileManager.isInternalUri("content://com.example/123"))
            assertFalse(AppFileManager.isInternalUri("http://example.com/foo"))
        }

        @Test
        @DisplayName("file:// path under user_files returns true (POSIX only)")
        fun acceptsUserFilesPath(@TempDir tempDir: Path) {
            assumeTrue(isPosixFileSystem, "AppFileManager.isInternalUri uses POSIX separators")

            val userDir = File(tempDir.toFile(), "user_files").apply { mkdirs() }
            val target = File(userDir, "note.md").apply { writeText("data") }

            val uri = AppFileManager.fileToInternalUri(target)
            assertTrue(
                AppFileManager.isInternalUri(uri),
                "Expected true for path under user_files: $uri",
            )
        }

        @Test
        @DisplayName("file:// deep path under user_files returns true (POSIX only)")
        fun acceptsDeepUserFilesPath(@TempDir tempDir: Path) {
            assumeTrue(isPosixFileSystem, "AppFileManager.isInternalUri uses POSIX separators")

            // Simulates real Android path: /data/user/0/com.draftpeek/files/user_files/Md.md
            val deepDir = File(tempDir.toFile(), "data").apply { mkdirs() }
            val midDir = File(deepDir, "com.app.files").apply { mkdirs() }
            val userDir = File(midDir, "user_files").apply { mkdirs() }
            val target = File(userDir, "Md.md").apply { writeText("data") }

            val uri = AppFileManager.fileToInternalUri(target)
            assertTrue(
                AppFileManager.isInternalUri(uri),
                "Expected true for deep path under user_files: $uri",
            )
        }

        @Test
        @DisplayName("file:// path outside user_files returns false (POSIX only)")
        fun rejectsExternalPath(@TempDir tempDir: Path) {
            assumeTrue(isPosixFileSystem, "AppFileManager.isInternalUri uses POSIX separators")

            val outsideDir = File(tempDir.toFile(), "other_dir").apply { mkdirs() }
            val outside = File(outsideDir, "foo.txt").apply { writeText("data") }

            val uri = AppFileManager.fileToInternalUri(outside)
            assertFalse(
                AppFileManager.isInternalUri(uri),
                "Expected false for path outside user_files: $uri",
            )
        }

        @Test
        @DisplayName("malformed URI returns false (does not throw)")
        fun handlesMalformed() {
            assertFalse(AppFileManager.isInternalUri("not-a-uri"))
            assertFalse(AppFileManager.isInternalUri(""))
        }
    }

    @Nested
    @DisplayName("URI round-trip")
    inner class UriRoundTripTest {

        @Test
        @DisplayName("fileToInternalUri → isInternalUri → file path resolves back (POSIX only)")
        fun roundTrip(@TempDir tempDir: Path) {
            assumeTrue(isPosixFileSystem, "AppFileManager.isInternalUri uses POSIX separators")

            val userDir = File(tempDir.toFile(), "user_files").apply { mkdirs() }
            val original = File(userDir, "doc.md").apply { writeText("# hi") }

            val uri = AppFileManager.fileToInternalUri(original)

            assertTrue(AppFileManager.isInternalUri(uri))
            val parsed = File(uri.removePrefix("file://"))
            assertEquals(original.absolutePath, parsed.absolutePath)
        }
    }
}
