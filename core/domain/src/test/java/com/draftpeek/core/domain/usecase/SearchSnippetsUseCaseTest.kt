package com.draftpeek.core.domain.usecase

import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SearchSnippetsUseCase")
class SearchSnippetsUseCaseTest {

    private lateinit var repository: SnippetRepository
    private lateinit var useCase: SearchSnippetsUseCase

    private val sampleSnippet = Snippet(
        id = 1,
        title = "Test",
        content = "fun test()",
        language = "kotlin",
        category = "Kotlin",
        createdAt = 0L,
        updatedAt = 0L,
    )

    @BeforeEach
    fun setUp() {
        repository = mockk()
        useCase = SearchSnippetsUseCase(repository)
    }

    @Nested
    @DisplayName("invoke (FTS4 search)")
    inner class FtsSearchTests {

        @Test
        @DisplayName("FTS4 搜索委托给 repository")
        fun invoke_delegatesToRepository() = runTest {
            every { repository.searchSnippets("test") } returns flowOf(listOf(sampleSnippet))

            val result = useCase("test").first()

            assertEquals(1, result.size)
            assertEquals("Test", result[0].title)
        }

        @Test
        @DisplayName("无匹配结果返回空列表")
        fun invoke_noMatch_returnsEmpty() = runTest {
            every { repository.searchSnippets("nonexistent") } returns flowOf(emptyList())

            val result = useCase("nonexistent").first()

            assertEquals(0, result.size)
        }
    }

    @Nested
    @DisplayName("byLanguage")
    inner class ByLanguageTests {

        @Test
        @DisplayName("按语言过滤搜索")
        fun byLanguage_delegatesToRepository() = runTest {
            every { repository.searchSnippetsByLanguage("test", "kotlin") } returns flowOf(listOf(sampleSnippet))

            val result = useCase.byLanguage("test", "kotlin").first()

            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("byCategory")
    inner class ByCategoryTests {

        @Test
        @DisplayName("按分类过滤搜索")
        fun byCategory_delegatesToRepository() = runTest {
            every { repository.searchSnippetsByCategory("test", "Kotlin") } returns flowOf(listOf(sampleSnippet))

            val result = useCase.byCategory("test", "Kotlin").first()

            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("substring")
    inner class SubstringTests {

        @Test
        @DisplayName("子串搜索委托给 repository")
        fun substring_delegatesToRepository() = runTest {
            every { repository.searchSnippetsSubstring("partial") } returns flowOf(listOf(sampleSnippet))

            val result = useCase.substring("partial").first()

            assertEquals(1, result.size)
        }
    }
}
