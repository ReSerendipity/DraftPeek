package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.repository.UserActivityRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RecordUserActivityUseCase")
class RecordUserActivityUseCaseTest {

    private lateinit var repository: UserActivityRepository
    private lateinit var useCase: RecordUserActivityUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = RecordUserActivityUseCase(repository)
    }

    @Nested
    @DisplayName("record methods")
    inner class RecordMethodsTests {

        @Test
        @DisplayName("recordFileOpen 委托给 repository")
        fun recordFileOpen_delegatesToRepository() = runTest {
            useCase.recordFileOpen()
            coVerify { repository.recordFileOpen() }
        }

        @Test
        @DisplayName("recordTextEdit 委托给 repository")
        fun recordTextEdit_delegatesToRepository() = runTest {
            useCase.recordTextEdit()
            coVerify { repository.recordTextEdit() }
        }

        @Test
        @DisplayName("recordSearch 委托给 repository")
        fun recordSearch_delegatesToRepository() = runTest {
            useCase.recordSearch()
            coVerify { repository.recordSearch() }
        }

        @Test
        @DisplayName("recordExport 委托给 repository")
        fun recordExport_delegatesToRepository() = runTest {
            useCase.recordExport()
            coVerify { repository.recordExport() }
        }

        @Test
        @DisplayName("recordSnippetCreated 委托给 repository")
        fun recordSnippetCreated_delegatesToRepository() = runTest {
            useCase.recordSnippetCreated()
            coVerify { repository.recordSnippetCreated() }
        }

        @Test
        @DisplayName("recordFileManagement 委托给 repository")
        fun recordFileManagement_delegatesToRepository() = runTest {
            useCase.recordFileManagement()
            coVerify { repository.recordFileManagement() }
        }

        @Test
        @DisplayName("recordPreview 委托给 repository")
        fun recordPreview_delegatesToRepository() = runTest {
            useCase.recordPreview()
            coVerify { repository.recordPreview() }
        }

        @Test
        @DisplayName("recordDiff 委托给 repository")
        fun recordDiff_delegatesToRepository() = runTest {
            useCase.recordDiff()
            coVerify { repository.recordDiff() }
        }

        @Test
        @DisplayName("recordUsageDuration 传递分钟数")
        fun recordUsageDuration_passesMinutes() = runTest {
            useCase.recordUsageDuration(30)
            coVerify { repository.recordUsageDuration(30) }
        }

        @Test
        @DisplayName("recordCharWrite 传递字符数")
        fun recordCharWrite_passesCount() = runTest {
            useCase.recordCharWrite(500)
            coVerify { repository.recordCharWrite(500) }
        }

        @Test
        @DisplayName("recordFileCreate 委托给 repository")
        fun recordFileCreate_delegatesToRepository() = runTest {
            useCase.recordFileCreate()
            coVerify { repository.recordFileCreate() }
        }

        @Test
        @DisplayName("recordAppLaunch 委托给 repository")
        fun recordAppLaunch_delegatesToRepository() = runTest {
            useCase.recordAppLaunch()
            coVerify { repository.recordAppLaunch() }
        }
    }
}
