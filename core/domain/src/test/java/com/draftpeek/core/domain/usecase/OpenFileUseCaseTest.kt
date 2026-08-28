package com.draftpeek.core.domain.usecase

import android.net.Uri
import com.draftpeek.core.data.repository.EditorFileReadOutcome
import com.draftpeek.core.data.repository.EditorFileReadResult
import com.draftpeek.core.data.repository.EditorFileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OpenFileUseCase")
class OpenFileUseCaseTest {

    private lateinit var repository: EditorFileRepository
    private lateinit var useCase: OpenFileUseCase

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = OpenFileUseCase(repository)
    }

    @Nested
    @DisplayName("invoke")
    inner class InvokeTests {

        @Test
        @DisplayName("成功读取文件返回 Success")
        fun invoke_success_returnsSuccessOutcome() = runTest {
            val uri = mockk<Uri>()
            val expectedResult = EditorFileReadResult(
                content = "fun main()",
                language = "kotlin",
                fileName = "main.kt"
            )
            coEvery { repository.readFile(uri, null) } returns EditorFileReadOutcome.Success(expectedResult)

            val result = useCase(uri)

            assertTrue(result is EditorFileReadOutcome.Success)
            assertEquals("main.kt", (result as EditorFileReadOutcome.Success).result.fileName)
        }

        @Test
        @DisplayName("文件不存在返回 Error")
        fun invoke_fileNotFound_returnsError() = runTest {
            val uri = mockk<Uri>()
            coEvery { repository.readFile(uri, null) } returns EditorFileReadOutcome.Error(
                message = "File not found",
                isFileNotFound = true
            )

            val result = useCase(uri)

            assertTrue(result is EditorFileReadOutcome.Error)
            assertTrue((result as EditorFileReadOutcome.Error).isFileNotFound)
        }

        @Test
        @DisplayName("指定编码时传递给 repository")
        fun invoke_withEncoding_passesToRepository() = runTest {
            val uri = mockk<Uri>()
            coEvery { repository.readFile(uri, "GBK") } returns EditorFileReadOutcome.Success(
                EditorFileReadResult(content = "", language = null, fileName = "")
            )

            useCase(uri, "GBK")

            coVerify { repository.readFile(uri, "GBK") }
        }
    }

    @Nested
    @DisplayName("isInternalFile & deleteInternalFile")
    inner class InternalFileTests {

        @Test
        @DisplayName("isInternalFile 委托给 repository")
        fun isInternalFile_delegatesToRepository() {
            every { repository.isInternalFile("content://internal/file") } returns true

            assertTrue(useCase.isInternalFile("content://internal/file"))
        }

        @Test
        @DisplayName("deleteInternalFile 委托给 repository")
        fun deleteInternalFile_delegatesToRepository() = runTest {
            coEvery { repository.deleteInternalFile("content://internal/file") } returns true

            assertTrue(useCase.deleteInternalFile("content://internal/file"))
        }
    }
}
