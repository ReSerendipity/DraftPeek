package com.draftpeek.feature.browser.viewmodel

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.draftpeek.core.common.event.AppEvent
import com.draftpeek.core.common.event.AppEventBus
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitHubApiClient
import com.draftpeek.core.common.vcs.GitManager
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.domain.repository.BookmarkRepository
import com.draftpeek.core.domain.repository.UserActivityRepository
import com.draftpeek.core.domain.usecase.ManageBookmarksUseCase
import com.draftpeek.core.domain.usecase.RecordUserActivityUseCase
import com.draftpeek.feature.browser.model.BrowserUiState
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.FileSortOption
import com.draftpeek.feature.browser.model.GitHubImportState
import com.draftpeek.feature.browser.model.SafAccessState
import com.draftpeek.feature.browser.model.sortFiles
import com.draftpeek.feature.browser.observer.DirectoryObserver
import com.draftpeek.feature.browser.repository.FileRepository
import com.draftpeek.feature.settings.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [FileBrowserViewModel].
 *
 * 两层测试策略：
 * 1. 纯 Kotlin 排序/过滤逻辑（不依赖 Android）—— 直接对 [sortFiles] 与 Git 状态映射断言。
 * 2. ViewModel 真实实例 + Turbine —— 对纯内存 StateFlow 做事件序列断言（A-P1-2）。
 *    与 Terminal/Settings/Editor 测试一致：用 [UnconfinedTestDispatcher] 替换 Main
 *    （viewModelScope 构造时即捕获 Main），并 mock 掉 init 中触发真实文件系统的
 *    [AppFileManager.listUserFiles]，使 JVM 单测可断言 StateFlow 而无需 Robolectric。
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
    // ViewModel 真实实例 + Turbine：StateFlow 事件序列断言（A-P1-2）
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("ViewModel StateFlow（Turbine 序列断言）")
    inner class ViewModelStateFlowTests {

        private lateinit var repository: FileRepository
        private lateinit var settingsRepository: SettingsRepository
        private lateinit var userActivityRepository: UserActivityRepository
        private lateinit var bookmarkRepository: BookmarkRepository
        private lateinit var manageBookmarks: ManageBookmarksUseCase
        private lateinit var recordUserActivity: RecordUserActivityUseCase
        private lateinit var appEventBus: AppEventBus
        private lateinit var gitManager: GitManager
        private lateinit var gitHubApiClient: GitHubApiClient
        private lateinit var directoryObserver: DirectoryObserver
        private lateinit var context: Context
        private lateinit var viewModel: FileBrowserViewModel

        // viewModelScope 依赖 Dispatchers.Main：必须在 VM 构造之前 setMain（构造时即捕获），
        // 用 UnconfinedTestDispatcher 让 viewModelScope 内的派发急切执行，JVM 单测无需 Looper。
        private val mainDispatcher = UnconfinedTestDispatcher()

        @BeforeEach
        fun setUp() {
            Dispatchers.setMain(mainDispatcher)

            // init 会调用 AppFileManager.listUserFiles(context) 触达真实文件系统，
            // 用 mockkObject 截获并返回空列表，避免 JVM 单测中 NPE / 真机 IO。
            mockkObject(AppFileManager)
            every { AppFileManager.listUserFiles(any()) } returns emptyList()

            repository = mockk(relaxed = true)
            userActivityRepository = mockk(relaxed = true)
            bookmarkRepository = mockk(relaxed = true)
            gitManager = mockk(relaxed = true)
            gitHubApiClient = mockk(relaxed = true)
            recordUserActivity = mockk(relaxed = true)
            context = mockk(relaxed = true)

            settingsRepository = mockk(relaxed = true)
            every { settingsRepository.settings } returns emptyFlow()
            every { settingsRepository.getPinnedFiles() } returns MutableStateFlow(emptySet())
            every { settingsRepository.getPinnedOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getRecentOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getInternalFilesOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getBookmarkOrder() } returns MutableStateFlow(emptyList())

            manageBookmarks = mockk(relaxed = true)
            every { manageBookmarks.allBookmarkUris() } returns MutableStateFlow(emptyList())

            appEventBus = mockk(relaxed = true)
            every { appEventBus.events } returns MutableSharedFlow<AppEvent>()

            directoryObserver = mockk(relaxed = true)
            every { directoryObserver.refreshEvents } returns MutableSharedFlow<Unit>()

            viewModel = FileBrowserViewModel(
                repository,
                settingsRepository,
                userActivityRepository,
                bookmarkRepository,
                manageBookmarks,
                recordUserActivity,
                appEventBus,
                gitManager,
                gitHubApiClient,
                directoryObserver,
                context
            )
        }

        @AfterEach
        fun tearDown() {
            Dispatchers.resetMain()
            unmockkObject(AppFileManager)
        }

        @Test
        @DisplayName("currentSortOption 初始 NAME_ASC，changeSortOption 透传新值（Turbine）")
        fun currentSortOption_emitsOnChange() = runTest {
            viewModel.currentSortOption.test {
                assertEquals(FileSortOption.NAME_ASC, awaitItem())
                viewModel.changeSortOption(FileSortOption.SIZE_DESC)
                assertEquals(FileSortOption.SIZE_DESC, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isMultiSelectMode 初始 false，toggle 翻转（Turbine）")
        fun isMultiSelectMode_toggles() = runTest {
            viewModel.isMultiSelectMode.test {
                assertFalse(awaitItem())
                viewModel.toggleMultiSelectMode()
                assertTrue(awaitItem())
                viewModel.toggleMultiSelectMode()
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("selectedItems 随 toggleItemSelected 增减（Turbine）")
        fun selectedItems_emitsOnToggle() = runTest {
            viewModel.selectedItems.test {
                assertEquals(emptySet<String>(), awaitItem())
                viewModel.toggleItemSelected("u1")
                assertEquals(setOf("u1"), awaitItem())
                viewModel.toggleItemSelected("u2")
                assertEquals(setOf("u1", "u2"), awaitItem())
                viewModel.toggleItemSelected("u1")
                assertEquals(setOf("u2"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("pinnedFiles 随 togglePin 增减，currentTreeUri 为空时不触发 loadFiles（Turbine）")
        fun pinnedFiles_emitsOnTogglePin() = runTest {
            viewModel.pinnedFiles.test {
                assertEquals(emptySet<String>(), awaitItem())
                viewModel.togglePin("u1")
                assertEquals(setOf("u1"), awaitItem())
                viewModel.togglePin("u1")
                assertEquals(emptySet<String>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("gitHubSelectedFiles 随 toggleGitHubFileSelection 增减（Turbine）")
        fun gitHubSelectedFiles_emitsOnToggle() = runTest {
            viewModel.gitHubSelectedFiles.test {
                assertEquals(emptySet<String>(), awaitItem())
                viewModel.toggleGitHubFileSelection("a/b.txt")
                assertEquals(setOf("a/b.txt"), awaitItem())
                viewModel.toggleGitHubFileSelection("a/b.txt")
                assertEquals(emptySet<String>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("gitHubImportState 初始为 Idle；resetGitHubImportState 维持 Idle（Turbine）")
        fun gitHubImportState_initialIdle() = runTest {
            viewModel.gitHubImportState.test {
                assertEquals(GitHubImportState.Idle, awaitItem())
                viewModel.resetGitHubImportState()
                // Idle → Idle 为同值，StateFlow 不重复发射，无需再 await
                assertEquals(GitHubImportState.Idle, viewModel.gitHubImportState.value)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ------------------------------------------------------------------
    // SAF 目录授权 → 列表加载（WO-B22「文件页列表恒空」）
    // ------------------------------------------------------------------

    /**
     * 覆盖 S13 定位的根因：授权态在 UI 组合期置真、与 `uiState` 的发布时机错位，
     * 授权后首次 `listFiles` 返回空即被渲染成「暂无文件」，且没有重试路径。
     *
     * 断言点全部落在 ViewModel 的公开状态（`safAccessState` / `uiState`）与仓储交互
     * （`listFiles` 调用次数、调用瞬间的授权态），可在 JVM 上跑，不需要 Robolectric。
     */
    @Nested
    @DisplayName("SAF 目录授权（WO-B22）")
    inner class SafDirectoryAccessTests {

        private lateinit var repository: FileRepository
        private lateinit var settingsRepository: SettingsRepository
        private lateinit var userActivityRepository: UserActivityRepository
        private lateinit var bookmarkRepository: BookmarkRepository
        private lateinit var manageBookmarks: ManageBookmarksUseCase
        private lateinit var recordUserActivity: RecordUserActivityUseCase
        private lateinit var appEventBus: AppEventBus
        private lateinit var gitManager: GitManager
        private lateinit var gitHubApiClient: GitHubApiClient
        private lateinit var directoryObserver: DirectoryObserver
        private lateinit var context: Context
        private lateinit var viewModel: FileBrowserViewModel

        private val mainDispatcher = UnconfinedTestDispatcher()
        private val treeUri = mockk<Uri>(relaxed = true)

        /** `listFiles` 被调用次数（空结果会触发一次补偿加载，故非空 ≠ 1）。 */
        private var listFilesCalls = 0

        /** `listFiles` 被调用那一刻的授权态 —— 负控判据：必须是 GRANTED。 */
        private var accessStateAtLoad: SafAccessState? = null

        @BeforeEach
        fun setUp() {
            Dispatchers.setMain(mainDispatcher)
            mockkObject(AppFileManager)
            every { AppFileManager.listUserFiles(any()) } returns emptyList()

            every { treeUri.toString() } returns "file:///storage/emulated/0/DraftPeekAudit"
            every { treeUri.scheme } returns "file"
            every { treeUri.path } returns "/storage/emulated/0/DraftPeekAudit"

            repository = mockk(relaxed = true)
            coEvery { repository.takeUriPermission(any()) } returns Unit

            settingsRepository = mockk(relaxed = true)
            every { settingsRepository.settings } returns emptyFlow()
            every { settingsRepository.getPinnedFiles() } returns MutableStateFlow(emptySet())
            every { settingsRepository.getPinnedOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getRecentOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getInternalFilesOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getBookmarkOrder() } returns MutableStateFlow(emptyList())
            every { settingsRepository.getDirectorySortOption(any()) } returns flowOf(null)

            manageBookmarks = mockk(relaxed = true)
            every { manageBookmarks.allBookmarkUris() } returns MutableStateFlow(emptyList())

            appEventBus = mockk(relaxed = true)
            every { appEventBus.events } returns MutableSharedFlow<AppEvent>()

            directoryObserver = mockk(relaxed = true)
            every { directoryObserver.refreshEvents } returns MutableSharedFlow<Unit>()

            gitManager = mockk(relaxed = true)
            every { gitManager.findRepository(any()) } returns null

            gitHubApiClient = mockk(relaxed = true)
            userActivityRepository = mockk(relaxed = true)
            bookmarkRepository = mockk(relaxed = true)
            recordUserActivity = mockk(relaxed = true)
            context = mockk(relaxed = true)

            stubListFiles(listOf())

            viewModel = FileBrowserViewModel(
                repository,
                settingsRepository,
                userActivityRepository,
                bookmarkRepository,
                manageBookmarks,
                recordUserActivity,
                appEventBus,
                gitManager,
                gitHubApiClient,
                directoryObserver,
                context
            )
        }

        @AfterEach
        fun tearDown() {
            Dispatchers.resetMain()
            unmockkObject(AppFileManager)
        }

        /**
         * 按批次返回 `listFiles` 结果；批次用尽后一直返回最后一组。
         * 顺带记录调用次数与调用瞬间的授权态。
         */
        private fun stubListFiles(vararg batches: List<FileItem>) {
            val pending = batches.toList().toMutableList()
            every { repository.listFiles(any()) } answers {
                listFilesCalls++
                accessStateAtLoad = viewModel.safAccessState.value
                val batch = if (pending.isEmpty()) batches.last() else pending.removeAt(0)
                flowOf(batch)
            }
        }

        private fun safFile(name: String, isDir: Boolean = false): FileItem = FileItem(
            name = name,
            uri = mockk(relaxed = true),
            isDirectory = isDir
        )

        @Test
        @DisplayName("授权前：停在首页（NONE + Idle），不会把空结果发布成 Success")
        fun beforeGrant_staysOnHomeWithoutLoad() {
            assertEquals(SafAccessState.NONE, viewModel.safAccessState.value)
            assertEquals(BrowserUiState.Idle, viewModel.uiState.value)
            assertEquals(0, listFilesCalls)
            coVerify(exactly = 0) { repository.takeUriPermission(any()) }
        }

        @Test
        @DisplayName("授权后：先发布 GRANTED 再加载；非空列表发布 Success（负控：授权晚于加载即红）")
        fun afterGrant_publishesGrantedBeforeLoad() {
            stubListFiles(listOf(safFile("a.md"), safFile("src", isDir = true)))

            viewModel.onTreeUriSelected(treeUri)

            assertEquals(SafAccessState.GRANTED, viewModel.safAccessState.value)
            val state = viewModel.uiState.value
            assertTrue(state is BrowserUiState.Success, "授权后应有加载结果，实际 $state")
            assertEquals(2, (state as BrowserUiState.Success).files.size)
            // 负控判据：加载发生时授权态必须已经发布，否则「空列表」就无法被区分为已加载空
            assertEquals(SafAccessState.GRANTED, accessStateAtLoad)
            assertEquals(1, listFilesCalls)
            coVerify(exactly = 1) { repository.takeUriPermission(treeUri) }
        }

        @Test
        @DisplayName("授权后首次为空：显式补一次加载，仍空则作为「已加载空」收敛")
        fun afterGrant_emptyDirectoryRetriesOnceThenAcceptsEmpty() {
            stubListFiles(emptyList())

            viewModel.onTreeUriSelected(treeUri)

            assertEquals(SafAccessState.GRANTED, viewModel.safAccessState.value)
            val state = viewModel.uiState.value
            assertTrue(state is BrowserUiState.Success, "空目录也应落到 Success，实际 $state")
            assertTrue((state as BrowserUiState.Success).files.isEmpty())
            assertEquals(2, listFilesCalls, "首次授权后应显式补一次加载")
        }

        @Test
        @DisplayName("首次空、补偿加载取到文件：最终 Success 非空（复现「授权后停在空列表」的症状）")
        fun firstEmptyThenFilled_surfacesFiles() {
            stubListFiles(emptyList(), listOf(safFile("qa-walkthrough-16.md")))

            viewModel.onTreeUriSelected(treeUri)

            val state = viewModel.uiState.value
            assertTrue(state is BrowserUiState.Success, "应落到 Success，实际 $state")
            assertEquals(1, (state as BrowserUiState.Success).files.size)
            assertEquals("qa-walkthrough-16.md", state.files.first().name)
            assertEquals(2, listFilesCalls)
        }

        @Test
        @DisplayName("授权成功但查询抛错：落到 Error，不静默变成「暂无文件」")
        fun loadFailure_surfacesErrorInsteadOfEmpty() {
            every { repository.listFiles(any()) } answers {
                listFilesCalls++
                accessStateAtLoad = viewModel.safAccessState.value
                flow { throw IllegalStateException("Cannot resolve directory tree") }
            }

            viewModel.onTreeUriSelected(treeUri)

            val state = viewModel.uiState.value
            assertTrue(state is BrowserUiState.Error, "加载失败应曝光为错误态，实际 $state")
            assertEquals(1, listFilesCalls)
        }

        @Test
        @DisplayName("回到根目录：授权态与目录会话一起复位（NONE + Idle）")
        fun navigateUpToRoot_resetsAccessState() {
            stubListFiles(listOf(safFile("a.md")))

            viewModel.onTreeUriSelected(treeUri)
            assertEquals(SafAccessState.GRANTED, viewModel.safAccessState.value)

            assertTrue(viewModel.navigateUp())

            assertEquals(SafAccessState.NONE, viewModel.safAccessState.value)
            assertEquals(null, viewModel.currentTreeUri.value)
            assertEquals(BrowserUiState.Idle, viewModel.uiState.value)
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
