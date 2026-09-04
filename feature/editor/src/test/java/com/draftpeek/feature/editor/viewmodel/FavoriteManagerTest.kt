package com.draftpeek.feature.editor.viewmodel

import com.draftpeek.core.testing.FakeRecentFilesRepository
import com.draftpeek.core.testing.TestDataFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [FavoriteManager] 行为测试 —— 本文件同时是「core:testing 不再是孤儿模块」的验收测试：
 *
 * - [FakeRecentFilesRepository]：替代 Room 数据库的内存实现，验证 FavoriteManager 与
 *   仓库的完整交互契约（订阅 → 发值 → 切换 → 委托写回）；
 * - [TestDataFactory]：统一构造期望实体，避免测试内手写 `RecentFile(...)`。
 *
 * 协程策略：`loadFavoriteStatus` 内部用 `scope.launch` 启动无限期 collect，这类
 * "后台收集器" 不能放进 `runTest` 的主作业树（否则测试永不结束）。实测本环境
 * `backgroundScope` + `advanceUntilIdle()` 不会执行后台协程，因此改用
 * [UnconfinedTestDispatcher] 独立作用域：launch 在调用线程上急切执行到首个挂起点，
 * StateFlow 的当前值同步发出，断言完全确定、不依赖虚拟时间调度。
 *
 * 注意 fake 的语义：`getRecentFile(uri)` 返回**调用时刻**的快照 StateFlow（此后不再发新值），
 * 因此「仓库内 toggle 后 FavoriteManager 不自动感知」是 fake 的既定行为而非 bug；
 * 与之对应的产品行为是「切换文件时重新调用 loadFavoriteStatus」，由
 * switchingUri_reemitsFromNewSource 覆盖。
 */
@DisplayName("FavoriteManager（复用 core:testing 的 Fake 与 TestDataFactory）")
class FavoriteManagerTest {

    private lateinit var repository: FakeRecentFilesRepository
    private lateinit var manager: FavoriteManager

    @BeforeEach
    fun setUp() {
        repository = FakeRecentFilesRepository()
        manager = FavoriteManager(repository)
    }

    /** [FavoriteManager.loadFavoriteStatus] 需要的外部作用域：急切执行，不阻塞测试结束。 */
    private fun eagerScope(scheduler: TestCoroutineScheduler): CoroutineScope =
        CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(scheduler))

    @Nested
    @DisplayName("loadFavoriteStatus — 订阅与发值")
    inner class LoadFavoriteStatus {

        @Test
        @DisplayName("空 URI 直接置 false，不发起订阅")
        fun emptyUri_setsFalse() = runTest {
            manager.loadFavoriteStatus(eagerScope(testScheduler), "")

            assertFalse(manager.isFavorite.value, "空 URI 时收藏状态应为 false")
        }

        @Test
        @DisplayName("已收藏文件加载后 isFavorite 为 true")
        fun favoriteFile_emitsTrue() = runTest {
            val uri = "content://test/fav.kt"
            repository.addRecentFile(uri, fileName = "fav.kt", language = "kotlin", fileSize = 100L)
            repository.toggleFavorite(uri)
            assertTrue(
                repository.getRecentFile(uri).first()?.isFavorite == true,
                "前置条件：fake 仓库内该文件应为已收藏"
            )

            manager.loadFavoriteStatus(eagerScope(testScheduler), uri)

            assertTrue(manager.isFavorite.value, "已收藏文件应使 isFavorite 为 true")
        }

        @Test
        @DisplayName("未收藏文件加载后 isFavorite 为 false")
        fun normalFile_emitsFalse() = runTest {
            val uri = "content://test/plain.kt"
            repository.addRecentFile(uri, fileName = "plain.kt", language = "kotlin", fileSize = 100L)

            manager.loadFavoriteStatus(eagerScope(testScheduler), uri)

            assertFalse(manager.isFavorite.value, "未收藏文件应使 isFavorite 为 false")
        }

        @Test
        @DisplayName("切换观察目标后按新文件状态重新发值（模拟切换文件）")
        fun switchingUri_reemitsFromNewSource() = runTest {
            val favUri = "content://test/a.kt"
            val plainUri = "content://test/b.kt"
            repository.addRecentFile(favUri, fileName = "a.kt", language = "kotlin", fileSize = 1L)
            repository.toggleFavorite(favUri)
            repository.addRecentFile(plainUri, fileName = "b.kt", language = "kotlin", fileSize = 1L)

            manager.loadFavoriteStatus(eagerScope(testScheduler), favUri)
            assertTrue(manager.isFavorite.value, "观察已收藏文件时应为 true")

            manager.loadFavoriteStatus(eagerScope(testScheduler), plainUri)
            assertFalse(manager.isFavorite.value, "切换到未收藏文件后应为 false")
        }
    }

    @Nested
    @DisplayName("toggleFavorite — 写回委托")
    inner class ToggleFavorite {

        @Test
        @DisplayName("委托仓库完成收藏状态翻转")
        fun delegatesToRepository() = runTest {
            val uri = "content://test/toggle.kt"
            repository.addRecentFile(uri, fileName = "toggle.kt", language = "kotlin", fileSize = 1L)

            manager.toggleFavorite(uri)
            assertEquals(true, repository.getRecentFile(uri).first()?.isFavorite, "第一次切换后应为已收藏")

            manager.toggleFavorite(uri)
            assertEquals(false, repository.getRecentFile(uri).first()?.isFavorite, "第二次切换后应回到未收藏")
        }

        @Test
        @DisplayName("空 URI 为 no-op，不抛异常")
        fun emptyUri_isNoop() = runTest {
            manager.toggleFavorite("")

            assertNull(
                repository.getRecentFile("content://test/none.kt").first(),
                "空 URI 不应产生任何仓库写入"
            )
        }
    }

    @Nested
    @DisplayName("TestDataFactory 联动")
    inner class TestDataFactoryIntegration {

        @Test
        @DisplayName("factory 构造的期望实体经 fake 存取后关键字段一致")
        fun factoryCreatedExpectations_matchFakeStorage() = runTest {
            val expected = TestDataFactory.createRecentFile(
                uri = "content://test/factory.kt",
                fileName = "factory.kt",
                language = "kotlin",
                isFavorite = false,
                fileSize = 123L
            )
            repository.addRecentFile(expected.uri, expected.fileName, expected.language, expected.fileSize)

            val stored = repository.getRecentFile(expected.uri).first()
            assertEquals(expected.uri, stored?.uri)
            assertEquals(expected.fileName, stored?.fileName)
            assertEquals(expected.language, stored?.language)
            assertEquals(expected.fileSize, stored?.fileSize)
        }

        @Test
        @DisplayName("factory 批量列表经 fake 全部可查")
        fun factoryList_roundTripsThroughFake() = runTest {
            val files = TestDataFactory.createRecentFileList(3)
            files.forEach { repository.addRecentFile(it.uri, it.fileName, it.language, it.fileSize) }

            files.forEach { expected ->
                val stored = repository.getRecentFile(expected.uri).first()
                assertEquals(expected.fileName, stored?.fileName, "uri=${expected.uri} 应可按 URI 查回")
            }
        }
    }
}
