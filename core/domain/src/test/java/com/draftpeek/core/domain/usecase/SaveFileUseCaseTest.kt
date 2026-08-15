package com.draftpeek.core.domain.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SaveFileUseCase")
class SaveFileUseCaseTest {

    private lateinit var repository: EditorFileRepository
    private lateinit var useCase: SaveFileUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SaveFileUseCase(repository)
    }

    @Nested
    @DisplayName("invoke")
    inner class InvokeTests {

        @Test
        @DisplayName("成功保存返回 Result.success")
        fun invoke_success_returnsSuccess() = runTest {
            val uri = mockk<Uri>()
            coEvery { repository.writeFile(uri, "content", null) } returns Result.success(Unit)

            val result = useCase(uri, "content")

            assertTrue(result.isSuccess)
        }

        @Test
        @DisplayName("保存失败返回 Result.failure")
        fun invoke_failure_returnsFailure() = runTest {
            val uri = mockk<Uri>()
            val exception = RuntimeException("Write error")
            coEvery { repository.writeFile(uri, "content", null) } returns Result.failure(exception)

            val result = useCase(uri, "content")

            assertTrue(result.isFailure)
        }

        @Test
        @DisplayName("指定编码时传递给 repository")
        fun invoke_withEncoding_passesToRepository() = runTest {
            val uri = mockk<Uri>()
            coEvery { repository.writeFile(uri, "content", "GBK") } returns Result.success(Unit)

            useCase(uri, "content", "GBK")

            coVerify { repository.writeFile(uri, "content", "GBK") }
        }
    }
}
