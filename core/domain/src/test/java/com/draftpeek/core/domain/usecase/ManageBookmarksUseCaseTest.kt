package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.repository.BookmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ManageBookmarksUseCase")
class ManageBookmarksUseCaseTest {

    private lateinit var repository: BookmarkRepository
    private lateinit var useCase: ManageBookmarksUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = ManageBookmarksUseCase(repository)
    }

    @Nested
    @DisplayName("allBookmarks")
    inner class AllBookmarksTests {

        @Test
        @DisplayName("返回 repository.allBookmarks 流")
        fun allBookmarks_delegatesToRepository() = runTest {
            val bookmarks = listOf(BookmarkEntity(uri = "uri1", fileName = "f1.kt", directoryUri = "dir1"))
            every { repository.allBookmarks } returns flowOf(bookmarks)

            val result = useCase.allBookmarks().first()
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("allBookmarkUris")
    inner class AllBookmarkUrisTests {

        @Test
        @DisplayName("返回 repository.allBookmarkUris 流")
        fun allBookmarkUris_delegatesToRepository() = runTest {
            every { repository.allBookmarkUris } returns flowOf(listOf("uri1", "uri2"))

            val result = useCase.allBookmarkUris().first()
            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("isBookmarked")
    inner class IsBookmarkedTests {

        @Test
        @DisplayName("已收藏返回 true")
        fun isBookmarked_existing_returnsTrue() = runTest {
            coEvery { repository.isBookmarked("uri1") } returns true

            assertTrue(useCase.isBookmarked("uri1"))
        }
    }

    @Nested
    @DisplayName("addBookmark & removeBookmark")
    inner class AddAndRemoveTests {

        @Test
        @DisplayName("addBookmark 委托给 repository")
        fun addBookmark_delegatesToRepository() = runTest {
            useCase.addBookmark("uri1", "file.kt", "dir1")

            coVerify { repository.addBookmark("uri1", "file.kt", "dir1") }
        }

        @Test
        @DisplayName("removeBookmark 委托给 repository")
        fun removeBookmark_delegatesToRepository() = runTest {
            useCase.removeBookmark("uri1")

            coVerify { repository.removeBookmark("uri1") }
        }
    }
}
