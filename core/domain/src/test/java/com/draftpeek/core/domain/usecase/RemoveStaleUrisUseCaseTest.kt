package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RemoveStaleUrisUseCase")
class RemoveStaleUrisUseCaseTest {

    private lateinit var repository: RecentFilesRepository
    private lateinit var useCase: RemoveStaleUrisUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = RemoveStaleUrisUseCase(repository)
    }

    @Nested
    @DisplayName("invoke")
    inner class InvokeTests {

        @Test
        @DisplayName("存在过期 URI 时批量删除")
        fun invoke_withStaleUris_removesThem() = runTest {
            val staleUris = listOf("uri1", "uri2", "uri3")
            coEvery { repository.getStaleUris() } returns staleUris

            useCase()

            coVerify { repository.removeRecentFiles(staleUris) }
        }

        @Test
        @DisplayName("无过期 URI 时不调用 removeRecentFiles")
        fun invoke_noStaleUris_doesNotRemove() = runTest {
            coEvery { repository.getStaleUris() } returns emptyList()

            useCase()

            coVerify(exactly = 0) { repository.removeRecentFiles(any()) }
        }

        @Test
        @DisplayName("单个过期 URI 也正确删除")
        fun invoke_singleStaleUri_removesIt() = runTest {
            coEvery { repository.getStaleUris() } returns listOf("single_uri")

            useCase()

            coVerify { repository.removeRecentFiles(listOf("single_uri")) }
        }
    }
}
