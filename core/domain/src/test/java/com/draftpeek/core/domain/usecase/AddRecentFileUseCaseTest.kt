package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.RecentFilesRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AddRecentFileUseCase")
class AddRecentFileUseCaseTest {

    private lateinit var repository: RecentFilesRepository
    private lateinit var useCase: AddRecentFileUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = AddRecentFileUseCase(repository)
    }

    @Nested
    @DisplayName("invoke")
    inner class InvokeTests {

        @Test
        @DisplayName("调用 repository.addRecentFile 传入正确参数")
        fun invoke_callsRepositoryWithCorrectArgs() = runTest {
            useCase.invoke(
                uri = "content://test/file.kt",
                fileName = "file.kt",
                language = "kotlin",
                fileSize = 1024L,
            )

            coVerify {
                repository.addRecentFile("content://test/file.kt", "file.kt", "kotlin", 1024L)
            }
        }

        @Test
        @DisplayName("language 为 null 时正确传递")
        fun invoke_nullLanguage_passedCorrectly() = runTest {
            useCase.invoke(
                uri = "content://test/file.txt",
                fileName = "file.txt",
                language = null,
                fileSize = 0L,
            )

            coVerify {
                repository.addRecentFile("content://test/file.txt", "file.txt", null, 0L)
            }
        }
    }
}
