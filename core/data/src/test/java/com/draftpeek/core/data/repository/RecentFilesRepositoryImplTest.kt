package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.entity.RecentFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RecentFilesRepositoryImpl")
class RecentFilesRepositoryImplTest {

    @Nested
    @DisplayName("recentFiles & favorites")
    inner class FlowTests {

        @Test
        @DisplayName("recentFiles 委托给 dao.getAllRecentFiles")
        fun recentFiles_delegatesToDao() = runTest {
            val files = listOf(RecentFile(uri = "uri1", fileName = "f1.kt", language = "kotlin", lastOpenedAt = 0L))
            val dao = mockk<RecentFileDao>()
            every { dao.getAllRecentFiles() } returns flowOf(files)
            every { dao.getFavorites() } returns flowOf(emptyList())
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            val result = repository.recentFiles.first()
            assertEquals(1, result.size)
        }

        @Test
        @DisplayName("favorites 委托给 dao.getFavorites")
        fun favorites_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>()
            every { dao.getAllRecentFiles() } returns flowOf(emptyList())
            every { dao.getFavorites() } returns flowOf(emptyList())
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            val result = repository.favorites.first()
            assertEquals(0, result.size)
        }
    }

    @Nested
    @DisplayName("removeRecentFile & removeRecentFiles")
    inner class RemoveTests {

        @Test
        @DisplayName("removeRecentFile 委托给 dao.delete")
        fun removeRecentFile_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            repository.removeRecentFile("uri1")
            coVerify { dao.delete("uri1") }
        }

        @Test
        @DisplayName("removeRecentFiles 委托给 dao.deleteByUris")
        fun removeRecentFiles_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            repository.removeRecentFiles(listOf("uri1", "uri2"))
            coVerify { dao.deleteByUris(listOf("uri1", "uri2")) }
        }
    }

    @Nested
    @DisplayName("toggleFavorite & clearAllRecentFiles")
    inner class FavoriteAndClearTests {

        @Test
        @DisplayName("toggleFavorite 委托给 dao")
        fun toggleFavorite_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            repository.toggleFavorite("uri1")
            coVerify { dao.toggleFavorite("uri1") }
        }

        @Test
        @DisplayName("clearAllRecentFiles 委托给 dao.deleteAllNonFavorite")
        fun clearAllRecentFiles_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            repository.clearAllRecentFiles()
            coVerify { dao.deleteAllNonFavorite() }
        }
    }

    @Nested
    @DisplayName("saveReadingPosition & getReadingPosition")
    inner class ReadingPositionTests {

        @Test
        @DisplayName("saveReadingPosition 委托给 dao.updateReadingPosition")
        fun saveReadingPosition_delegatesToDao() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())

            repository.saveReadingPosition("uri1", 10, 5, 100, 200)
            coVerify { dao.updateReadingPosition("uri1", 10, 5, 100, 200) }
        }

        @Test
        @DisplayName("getReadingPosition 返回 dao 结果")
        fun getReadingPosition_returnsDaoResult() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())
            val file = RecentFile(uri = "uri1", fileName = "f.kt", language = null, lastOpenedAt = 0L)
            coEvery { dao.getReadingPosition("uri1") } returns file

            val result = repository.getReadingPosition("uri1")
            assertEquals("f.kt", result?.fileName)
        }

        @Test
        @DisplayName("getReadingPosition 不存在时返回 null")
        fun getReadingPosition_nonExistent_returnsNull() = runTest {
            val dao = mockk<RecentFileDao>(relaxed = true)
            val repository = RecentFilesRepositoryImpl(dao, mockk())
            coEvery { dao.getReadingPosition("nonexistent") } returns null

            val result = repository.getReadingPosition("nonexistent")
            assertNull(result)
        }
    }
}
