package com.draftpeek.feature.browser.model

import android.net.Uri
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FileSortOption — sortFiles()")
class FileSortOptionTest {

    private val mockUri: Uri = mockk(relaxed = true)

    private fun createFile(
        name: String,
        isDirectory: Boolean = false,
        size: Long = 0,
        lastModified: Long = 0,
        extension: String = "",
        isPinned: Boolean = false,
    ) = FileItem(
        name = name,
        uri = mockUri,
        isDirectory = isDirectory,
        size = size,
        lastModified = lastModified,
        extension = extension,
        isPinned = isPinned,
    )

    private val testFiles = listOf(
        createFile("zfile.kt", size = 100, lastModified = 3000, extension = "kt"),
        createFile("afile.kt", size = 200, lastModified = 1000, extension = "kt"),
        createFile("mfile.kt", size = 300, lastModified = 2000, extension = "kt"),
    )

    @Nested
    @DisplayName("NAME_ASC")
    inner class NameAscTests {

        @Test
        @DisplayName("按名称升序排列")
        fun sortedByNameAsc() {
            val sorted = testFiles.sortFiles(FileSortOption.NAME_ASC)
            assertEquals("afile.kt", sorted[0].name)
            assertEquals("mfile.kt", sorted[1].name)
            assertEquals("zfile.kt", sorted[2].name)
        }

        @Test
        @DisplayName("目录排在文件之前")
        fun directoriesBeforeFiles() {
            val files = listOf(
                createFile("file.kt"),
                createFile("dir", isDirectory = true),
            )
            val sorted = files.sortFiles(FileSortOption.NAME_ASC)
            assertEquals("dir", sorted[0].name)
            assertEquals("file.kt", sorted[1].name)
        }
    }

    @Nested
    @DisplayName("NAME_DESC")
    inner class NameDescTests {

        @Test
        @DisplayName("按名称降序排列")
        fun sortedByNameDesc() {
            val sorted = testFiles.sortFiles(FileSortOption.NAME_DESC)
            assertEquals("zfile.kt", sorted[0].name)
            assertEquals("mfile.kt", sorted[1].name)
            assertEquals("afile.kt", sorted[2].name)
        }
    }

    @Nested
    @DisplayName("SIZE_DESC")
    inner class SizeDescTests {

        @Test
        @DisplayName("大文件在前")
        fun largestFirst() {
            val sorted = testFiles.sortFiles(FileSortOption.SIZE_DESC)
            assertEquals(300L, sorted[0].size)
            assertEquals(200L, sorted[1].size)
            assertEquals(100L, sorted[2].size)
        }
    }

    @Nested
    @DisplayName("SIZE_ASC")
    inner class SizeAscTests {

        @Test
        @DisplayName("小文件在前")
        fun smallestFirst() {
            val sorted = testFiles.sortFiles(FileSortOption.SIZE_ASC)
            assertEquals(100L, sorted[0].size)
            assertEquals(200L, sorted[1].size)
            assertEquals(300L, sorted[2].size)
        }
    }

    @Nested
    @DisplayName("MODIFIED_DESC")
    inner class ModifiedDescTests {

        @Test
        @DisplayName("最近修改在前")
        fun mostRecentFirst() {
            val sorted = testFiles.sortFiles(FileSortOption.MODIFIED_DESC)
            assertEquals(3000L, sorted[0].lastModified)
            assertEquals(2000L, sorted[1].lastModified)
            assertEquals(1000L, sorted[2].lastModified)
        }
    }

    @Nested
    @DisplayName("MODIFIED_ASC")
    inner class ModifiedAscTests {

        @Test
        @DisplayName("最早修改在前")
        fun oldestFirst() {
            val sorted = testFiles.sortFiles(FileSortOption.MODIFIED_ASC)
            assertEquals(1000L, sorted[0].lastModified)
            assertEquals(2000L, sorted[1].lastModified)
            assertEquals(3000L, sorted[2].lastModified)
        }
    }

    @Nested
    @DisplayName("Pinned files")
    inner class PinnedFilesTests {

        @Test
        @DisplayName("置顶文件始终排在最前")
        fun pinnedFileAlwaysFirst() {
            val files = listOf(
                createFile("afile.kt"),
                createFile("zfile.kt", isPinned = true),
                createFile("mfile.kt"),
            )
            val sorted = files.sortFiles(FileSortOption.NAME_ASC)
            assertEquals("zfile.kt", sorted[0].name)
        }

        @Test
        @DisplayName("多个置顶文件间仍按排序规则")
        fun multiplePinned_sortedAmongThemselves() {
            val files = listOf(
                createFile("zpin.kt", isPinned = true),
                createFile("apin.kt", isPinned = true),
                createFile("normal.kt"),
            )
            val sorted = files.sortFiles(FileSortOption.NAME_ASC)
            assertEquals("apin.kt", sorted[0].name)
            assertEquals("zpin.kt", sorted[1].name)
            assertEquals("normal.kt", sorted[2].name)
        }
    }

    @Nested
    @DisplayName("TYPE_ASC")
    inner class TypeAscTests {

        @Test
        @DisplayName("按扩展名排序")
        fun sortByExtension() {
            val files = listOf(
                createFile("file.kt", extension = "kt"),
                createFile("file.py", extension = "py"),
                createFile("file.go", extension = "go"),
            )
            val sorted = files.sortFiles(FileSortOption.TYPE_ASC)
            assertEquals("go", sorted[0].extension)
            assertEquals("kt", sorted[1].extension)
            assertEquals("py", sorted[2].extension)
        }
    }

    @Nested
    @DisplayName("Preservation")
    inner class PreservationTests {

        @Test
        @DisplayName("排序不丢失文件")
        fun sortPreservesAllFiles() {
            val sorted = testFiles.sortFiles(FileSortOption.NAME_ASC)
            assertEquals(testFiles.size, sorted.size)
        }

        @Test
        @DisplayName("空列表排序返回空列表")
        fun emptyList_returnsEmpty() {
            val sorted = emptyList<FileItem>().sortFiles(FileSortOption.NAME_ASC)
            assertTrue(sorted.isEmpty())
        }
    }
}
