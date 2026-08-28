package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetRecentFilesUseCase")
class GetRecentFilesUseCaseTest {

    private lateinit var repository: RecentFilesRepository
    private lateinit var useCase: GetRecentFilesUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = GetRecentFilesUseCase(repository)
    }

    @Nested
    @DisplayName("invoke")
    inner class InvokeTests {

        @Test
        @DisplayName("返回 repository.recentFiles 流")
        fun invoke_returnsRecentFilesFlow() = runTest {
            val files = listOf(RecentFile(uri = "uri1", fileName = "f1.kt", language = "kotlin", lastOpenedAt = 0L))
            every { repository.recentFiles } returns flowOf(files)

            val result = useCase().first()
            assertEquals(1, result.size)
            assertEquals("uri1", result[0].uri)
        }
    }

    @Nested
    @DisplayName("favorites")
    inner class FavoritesTests {

        @Test
        @DisplayName("返回 repository.favorites 流")
        fun favorites_returnsFavoritesFlow() = runTest {
            val favs =
                listOf(
                    RecentFile(uri = "fav1", fileName = "fav.kt", language = null, lastOpenedAt = 0L, isFavorite = true)
                )
            every { repository.favorites } returns flowOf(favs)

            val result = useCase.favorites().first()
            assertEquals(1, result.size)
            assertEquals("fav1", result[0].uri)
        }
    }

    @Nested
    @DisplayName("getRecentFile")
    inner class GetRecentFileTests {

        @Test
        @DisplayName("委托给 repository.getRecentFile")
        fun getRecentFile_delegatesToRepository() = runTest {
            val file = RecentFile(uri = "uri1", fileName = "f1.kt", language = "kotlin", lastOpenedAt = 0L)
            every { repository.getRecentFile("uri1") } returns flowOf(file)

            val result = useCase.getRecentFile("uri1").first()
            assertEquals("f1.kt", result?.fileName)
        }

        @Test
        @DisplayName("不存在的 URI 返回 null")
        fun getRecentFile_nonExistent_returnsNull() = runTest {
            every { repository.getRecentFile("nonexistent") } returns flowOf(null)

            val result = useCase.getRecentFile("nonexistent").first()
            assertEquals(null, result)
        }
    }
}
