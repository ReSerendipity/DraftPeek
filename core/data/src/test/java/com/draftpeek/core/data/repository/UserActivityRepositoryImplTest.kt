package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.dao.UserActivityDao
import com.draftpeek.core.data.entity.RecentFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserActivityRepositoryImpl")
class UserActivityRepositoryImplTest {

    private lateinit var userActivityDao: UserActivityDao
    private lateinit var recentFileDao: RecentFileDao
    private lateinit var repository: UserActivityRepositoryImpl

    @BeforeEach
    fun setUp() {
        userActivityDao = mockk(relaxed = true)
        recentFileDao = mockk(relaxed = true)
        repository = UserActivityRepositoryImpl(userActivityDao, recentFileDao)
    }

    @Nested
    @DisplayName("getActivityForDateRange & getActivityForDate")
    inner class QueryTests {

        @Test
        @DisplayName("getActivityForDateRange 委托给 dao")
        fun getActivityForDateRange_delegatesToDao() = runTest {
            every { userActivityDao.getActivityForDateRange("2026-01-01", "2026-01-31") } returns flowOf(emptyList())

            val result = repository.getActivityForDateRange("2026-01-01", "2026-01-31").first()
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("getActivityForDate 委托给 dao")
        fun getActivityForDate_delegatesToDao() = runTest {
            every { userActivityDao.getActivityForDate("2026-01-01") } returns flowOf(null)

            val result = repository.getActivityForDate("2026-01-01").first()
            assertEquals(null, result)
        }
    }

    @Nested
    @DisplayName("record methods")
    inner class RecordMethodTests {

        @Test
        @DisplayName("recordFileOpen 调用 ensureDateExists 和 incrementFileOpenCount")
        fun recordFileOpen_callsCorrectDaoMethods() = runTest {
            repository.recordFileOpen()

            coVerify(atLeast = 1) { userActivityDao.ensureDateExists(any(), any()) }
            coVerify(atLeast = 1) { userActivityDao.incrementFileOpenCount(any(), any()) }
        }

        @Test
        @DisplayName("recordTextEdit 调用 ensureDateExists 和 incrementTextEditCount")
        fun recordTextEdit_callsCorrectDaoMethods() = runTest {
            repository.recordTextEdit()

            coVerify(atLeast = 1) { userActivityDao.incrementTextEditCount(any(), any()) }
        }

        @Test
        @DisplayName("recordSearch 调用 incrementSearchCount")
        fun recordSearch_callsCorrectDaoMethods() = runTest {
            repository.recordSearch()

            coVerify(atLeast = 1) { userActivityDao.incrementSearchCount(any(), any()) }
        }

        @Test
        @DisplayName("recordPreview 调用 incrementPreviewCount")
        fun recordPreview_callsCorrectDaoMethods() = runTest {
            repository.recordPreview()

            coVerify(atLeast = 1) { userActivityDao.incrementPreviewCount(any(), any()) }
        }

        @Test
        @DisplayName("recordDiff 调用 incrementDiffCount")
        fun recordDiff_callsCorrectDaoMethods() = runTest {
            repository.recordDiff()

            coVerify(atLeast = 1) { userActivityDao.incrementDiffCount(any(), any()) }
        }

        @Test
        @DisplayName("recordSnippetCreated 调用 incrementSnippetCount")
        fun recordSnippetCreated_callsCorrectDaoMethods() = runTest {
            repository.recordSnippetCreated()

            coVerify(atLeast = 1) { userActivityDao.incrementSnippetCount(any(), any()) }
        }

        @Test
        @DisplayName("recordFileCreate 调用 incrementFileCreateCount")
        fun recordFileCreate_callsCorrectDaoMethods() = runTest {
            repository.recordFileCreate()

            coVerify(atLeast = 1) { userActivityDao.incrementFileCreateCount(any(), any()) }
        }

        @Test
        @DisplayName("recordAppLaunch 调用 incrementSessionCount")
        fun recordAppLaunch_callsCorrectDaoMethods() = runTest {
            repository.recordAppLaunch()

            coVerify(atLeast = 1) { userActivityDao.incrementSessionCount(any(), any()) }
        }

        @Test
        @DisplayName("recordUsageDuration 正值时调用 incrementUsageDurationMinutes")
        fun recordUsageDuration_positiveValue_callsDao() = runTest {
            repository.recordUsageDuration(30)

            coVerify(atLeast = 1) { userActivityDao.incrementUsageDurationMinutes(any(), 30, any()) }
        }

        @Test
        @DisplayName("recordUsageDuration 零或负值时不调用 DAO")
        fun recordUsageDuration_zeroOrNegative_doesNotCallDao() = runTest {
            repository.recordUsageDuration(0)
            repository.recordUsageDuration(-1)

            coVerify(exactly = 0) { userActivityDao.incrementUsageDurationMinutes(any(), any(), any()) }
        }

        @Test
        @DisplayName("recordCharWrite 正值时调用 incrementCharWriteCount")
        fun recordCharWrite_positiveValue_callsDao() = runTest {
            repository.recordCharWrite(100)

            coVerify(atLeast = 1) { userActivityDao.incrementCharWriteCount(any(), 100, any()) }
        }

        @Test
        @DisplayName("recordCharWrite 零或负值时不调用 DAO")
        fun recordCharWrite_zeroOrNegative_doesNotCallDao() = runTest {
            repository.recordCharWrite(0)

            coVerify(exactly = 0) { userActivityDao.incrementCharWriteCount(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("backfillFromRecentFiles")
    inner class BackfillTests {

        @Test
        @DisplayName("已有活动数据时不执行回填")
        fun backfill_existingData_skipsBackfill() = runTest {
            coEvery { userActivityDao.getActivityCount() } returns 5

            repository.backfillFromRecentFiles()

            coVerify(exactly = 0) { recentFileDao.getAllRecentFilesOneShot() }
        }

        @Test
        @DisplayName("无活动数据时从最近文件回填")
        fun backfill_noData_backfillsFromRecentFiles() = runTest {
            coEvery { userActivityDao.getActivityCount() } returns 0
            coEvery { recentFileDao.getAllRecentFilesOneShot() } returns listOf(
                RecentFile(uri = "uri1", fileName = "f1.kt", language = "kotlin", lastOpenedAt = System.currentTimeMillis()),
            )

            repository.backfillFromRecentFiles()

            coVerify(atLeast = 1) { userActivityDao.upsert(any()) }
        }
    }
}
