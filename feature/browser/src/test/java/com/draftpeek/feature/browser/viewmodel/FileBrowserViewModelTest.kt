package com.draftpeek.feature.browser.viewmodel

import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.FileSortOption
import com.draftpeek.feature.browser.model.sortFiles
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FileBrowserViewModel].
 *
 * Since the ViewModel depends on Android Context, we test the core sorting
 * and filtering logic (which is pure Kotlin) directly, and verify ViewModel
 * behavior through mocked dependencies where possible.
 */
class FileBrowserViewModelTest {

    // ------------------------------------------------------------------
    // 排序逻辑测试（纯 Kotlin，可直接测试）
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("按名称排序")
    inner class SortByNameTests {

        @Test
        @DisplayName("NAME_ASC 按名称升序排列，目录优先")
        fun nameAsc_directoriesFirstThenAlphabetical() {
            val files = listOf(
                fileItem("zebra.kt", isDir = false),
                fileItem("src", isDir = true),
                fileItem("apple.md", isDir = false),
                fileItem("build", isDir = true)
            )

            val sorted = files.sortFiles(FileSortOption.NAME_ASC)

            assertEquals("build", sorted[0].name)
            assertEquals("src", sorted[1].name)
            assertEquals("apple.md", sorted[2].name)
            assertEquals("zebra.kt", sorted[3].name)
        }

        @Test
        @DisplayName("NAME_DESC 按名称降序排列，目录优先")
        fun nameDesc_directoriesFirstThenReverseAlphabetical() {
            val files = listOf(
                fileItem("zebra.kt", isDir = false),
                fileItem("src", isDir = true),
                fileItem("apple.md", isDir = false),
                fileItem("build", isDir = true)
            )

            val sorted = files.sortFiles(FileSortOption.NAME_DESC)

            assertEquals("src", sorted[0].name)
            assertEquals("build", sorted[1].name)
            assertEquals("zebra.kt", sorted[2].name)
            assertEquals("apple.md", sorted[3].name)
        }
    }

    @Nested
    @DisplayName("按时间排序")
    inner class SortByTimeTests {

        @Test
        @DisplayName("MODIFIED_DESC 最新修改的文件排前面")
        fun modifiedDesc_newestFirst() {
            val now = System.currentTimeMillis()
            val files = listOf(
                fileItem("old.txt", isDir = false, lastModified = now - 10000),
                fileItem("new.txt", isDir = false, lastModified = now),
                fileItem("mid.txt", isDir = false, lastModified = now - 5000)
            )

            val sorted = files.sortFiles(FileSortOption.MODIFIED_DESC)

            assertEquals("new.txt", sorted[0].name)
            assertEquals("mid.txt", sorted[1].name)
            assertEquals("old.txt", sorted[2].name)
        }

        @Test
        @DisplayName("MODIFIED_ASC 最早修改的文件排前面")
        fun modifiedAsc_oldestFirst() {
            val now = System.currentTimeMillis()
            val files = listOf(
                fileItem("old.txt", isDir = false, lastModified = now - 10000),
                fileItem("new.txt", isDir = false, lastModified = now),
                fileItem("mid.txt", isDir = false, lastModified = now - 5000)
            )

            val sorted = files.sortFiles(FileSortOption.MODIFIED_ASC)

            assertEquals("old.txt", sorted[0].name)
            assertEquals("mid.txt", sorted[1].name)
            assertEquals("new.txt", sorted[2].name)
        }

        @Test
        @DisplayName("按时间排序时目录始终在前")
        fun sortByTime_directoriesFirst() {
            val now = System.currentTimeMillis()
            val files = listOf(
                fileItem("file.txt", isDir = false, lastModified = now),
                fileItem("folder", isDir = true, lastModified = now - 1000)
            )

            val sorted = files.sortFiles(FileSortOption.MODIFIED_DESC)

            assertEquals("folder", sorted[0].name)
            assertEquals("file.txt", sorted[1].name)
        }
    }

    // ------------------------------------------------------------------
    // 文件名过滤测试
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("文件名过滤")
    inner class FilterTests {

        @Test
        @DisplayName("按名称关键词过滤文件列表")
        fun filterByName_returnsMatchingFiles() {
            val files = listOf(
                fileItem("MainActivity.kt", isDir = false),
                fileItem("build.gradle.kts", isDir = false),
                fileItem("MainFragment.kt", isDir = false),
                fileItem("settings.json", isDir = false),
                fileItem("src", isDir = true)
            )

            val filtered = files.filter { it.name.contains("Main", ignoreCase = true) }

            assertEquals(2, filtered.size)
            assertTrue(filtered.all { it.name.contains("Main", ignoreCase = true) })
        }

        @Test
        @DisplayName("按扩展名过滤文件列表")
        fun filterByExtension_returnsMatchingFiles() {
            val files = listOf(
                fileItem("MainActivity.kt", isDir = false, extension = "kt"),
                fileItem("build.gradle.kts", isDir = false, extension = "kts"),
                fileItem("Main.java", isDir = false, extension = "java"),
                fileItem("config.json", isDir = false, extension = "json")
            )

            val filtered = files.filter { it.extension == "kt" }

            assertEquals(1, filtered.size)
            assertEquals("MainActivity.kt", filtered[0].name)
        }

        @Test
        @DisplayName("空关键词返回全部文件")
        fun filterEmptyKeyword_returnsAllFiles() {
            val files = listOf(
                fileItem("a.kt", isDir = false),
                fileItem("b.java", isDir = false)
            )

            val filtered = files.filter { it.name.contains("", ignoreCase = true) }

            assertEquals(2, filtered.size)
        }
    }

    // ------------------------------------------------------------------
    // 按大小排序
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("按大小排序")
    inner class SortBySizeTests {

        @Test
        @DisplayName("SIZE_DESC 最大文件排前面")
        fun sizeDesc_largestFirst() {
            val files = listOf(
                fileItem("small.txt", isDir = false, size = 100),
                fileItem("large.bin", isDir = false, size = 10000),
                fileItem("medium.log", isDir = false, size = 1000)
            )

            val sorted = files.sortFiles(FileSortOption.SIZE_DESC)

            assertEquals("large.bin", sorted[0].name)
            assertEquals("medium.log", sorted[1].name)
            assertEquals("small.txt", sorted[2].name)
        }

        @Test
        @DisplayName("SIZE_ASC 最小文件排前面")
        fun sizeAsc_smallestFirst() {
            val files = listOf(
                fileItem("small.txt", isDir = false, size = 100),
                fileItem("large.bin", isDir = false, size = 10000),
                fileItem("medium.log", isDir = false, size = 1000)
            )

            val sorted = files.sortFiles(FileSortOption.SIZE_ASC)

            assertEquals("small.txt", sorted[0].name)
            assertEquals("medium.log", sorted[1].name)
            assertEquals("large.bin", sorted[2].name)
        }
    }

    // ------------------------------------------------------------------
    // 按类型排序
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("按类型排序")
    inner class SortByTypeTests {

        @Test
        @DisplayName("TYPE_ASC 按扩展名排序，目录优先")
        fun typeAsc_byExtensionDirectoriesFirst() {
            val files = listOf(
                fileItem("main.py", isDir = false, extension = "py"),
                fileItem("src", isDir = true, extension = ""),
                fileItem("app.kt", isDir = false, extension = "kt"),
                fileItem("readme.md", isDir = false, extension = "md")
            )

            val sorted = files.sortFiles(FileSortOption.TYPE_ASC)

            assertEquals("src", sorted[0].name) // directory first
            assertEquals("app.kt", sorted[1].name) // kt before md
            assertEquals("readme.md", sorted[2].name) // md before py
            assertEquals("main.py", sorted[3].name)
        }
    }

    // ------------------------------------------------------------------
    // Git 状态模拟测试
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Git 状态查询")
    inner class GitStatusTests {

        @Test
        @DisplayName("GitFileStatus 可正确映射到文件项")
        fun gitFileStatus_mappedToFileItem() {
            val statuses = listOf(
                GitFileStatus("Main.kt", GitStatus.MODIFIED),
                GitFileStatus("new_file.txt", GitStatus.ADDED),
                GitFileStatus("deleted.txt", GitStatus.DELETED)
            )

            // Simulate the enrichment logic from FileBrowserViewModel
            val file = fileItem("Main.kt", isDir = false)
            val gitStatus = statuses.find { it.filePath == file.name }?.status

            assertEquals(GitStatus.MODIFIED, gitStatus)
        }

        @Test
        @DisplayName("不在 Git 状态列表中的文件 gitStatus 为 null")
        fun gitFileStatus_notTracked_returnsNull() {
            val statuses = listOf(
                GitFileStatus("Main.kt", GitStatus.MODIFIED)
            )

            val file = fileItem("OtherFile.java", isDir = false)
            val gitStatus = statuses.find { it.filePath == file.name }?.status

            assertEquals(null, gitStatus)
        }
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    private fun fileItem(
        name: String,
        isDir: Boolean,
        size: Long = 0,
        lastModified: Long = 0,
        extension: String = name.substringAfterLast(".", "")
    ): FileItem = FileItem(
        name = name,
        uri = mockk(),
        isDirectory = isDir,
        size = size,
        lastModified = lastModified,
        extension = extension
    )
}
