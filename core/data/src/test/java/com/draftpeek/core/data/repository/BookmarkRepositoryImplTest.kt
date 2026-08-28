package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.BookmarkDao
import com.draftpeek.core.data.entity.BookmarkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BookmarkRepositoryImpl")
class BookmarkRepositoryImplTest {

    @Nested
    @DisplayName("allBookmarks")
    inner class AllBookmarksTests {

        @Test
        @DisplayName("委托给 dao.getAllBookmarks")
        fun allBookmarks_delegatesToDao() = runTest {
            val bookmarks = listOf(BookmarkEntity(uri = "uri1", fileName = "f1", directoryUri = "d1"))
            val dao = mockk<BookmarkDao>()
            every { dao.getAllBookmarks() } returns flowOf(bookmarks)
            every { dao.getAllBookmarkUris() } returns flowOf(emptyList())
            val repository = BookmarkRepositoryImpl(dao)

            val result = repository.allBookmarks.first()
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("addBookmark")
    inner class AddBookmarkTests {

        @Test
        @DisplayName("创建 BookmarkEntity 并插入 DAO")
        fun addBookmark_insertsEntityIntoDao() = runTest {
            val dao = mockk<BookmarkDao>(relaxed = true)
            val repository = BookmarkRepositoryImpl(dao)

            repository.addBookmark("uri1", "file.kt", "dir1")

            coVerify {
                dao.insert(
                    match {
                        it.uri == "uri1" && it.fileName == "file.kt" && it.directoryUri == "dir1"
                    }
                )
            }
        }
    }

    @Nested
    @DisplayName("removeBookmark")
    inner class RemoveBookmarkTests {

        @Test
        @DisplayName("委托给 dao.deleteByUri")
        fun removeBookmark_delegatesToDao() = runTest {
            val dao = mockk<BookmarkDao>(relaxed = true)
            val repository = BookmarkRepositoryImpl(dao)

            repository.removeBookmark("uri1")

            coVerify { dao.deleteByUri("uri1") }
        }
    }

    @Nested
    @DisplayName("isBookmarked")
    inner class IsBookmarkedTests {

        @Test
        @DisplayName("委托给 dao.isBookmarked")
        fun isBookmarked_delegatesToDao() = runTest {
            val dao = mockk<BookmarkDao>(relaxed = true)
            val repository = BookmarkRepositoryImpl(dao)
            coEvery { dao.isBookmarked("uri1") } returns true

            assertTrue(repository.isBookmarked("uri1"))
        }
    }
}
