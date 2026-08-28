package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ReadingPositionUseCase")
class ReadingPositionUseCaseTest {

    private lateinit var repository: RecentFilesRepository
    private lateinit var useCase: ReadingPositionUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = ReadingPositionUseCase(repository)
    }

    @Nested
    @DisplayName("save")
    inner class SaveTests {

        @Test
        @DisplayName("保存阅读位置委托给 repository")
        fun save_delegatesToRepository() = runTest {
            useCase.save("uri1", line = 10, column = 5, scrollX = 100, scrollY = 200)

            coVerify { repository.saveReadingPosition("uri1", 10, 5, 100, 200) }
        }
    }

    @Nested
    @DisplayName("get")
    inner class GetTests {

        @Test
        @DisplayName("返回保存的阅读位置")
        fun get_returnsReadingPosition() = runTest {
            val file = RecentFile(
                uri = "uri1",
                fileName = "file.kt",
                language = "kotlin",
                lastOpenedAt = 0L,
                cursorLine = 42,
                cursorColumn = 7,
                scrollX = 50,
                scrollY = 100
            )
            coEvery { repository.getReadingPosition("uri1") } returns file

            val result = useCase.get("uri1")

            assertEquals(42, result?.cursorLine)
            assertEquals(7, result?.cursorColumn)
        }

        @Test
        @DisplayName("未保存阅读位置返回 null")
        fun get_noPosition_returnsNull() = runTest {
            coEvery { repository.getReadingPosition("uri1") } returns null

            val result = useCase.get("uri1")

            assertNull(result)
        }
    }
}
