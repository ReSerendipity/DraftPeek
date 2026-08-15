package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.SnippetDao
import com.draftpeek.core.data.entity.Snippet
import io.mockk.coEvery
import io.mockk.coVerify
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

@DisplayName("SnippetRepositoryImpl")
class SnippetRepositoryImplTest {

    private lateinit var dao: SnippetDao
    private lateinit var repository: SnippetRepositoryImpl

    private val sampleSnippet = Snippet(
        title = "Test", content = "fun test()", language = "kotlin",
        category = "Kotlin", createdAt = 0L, updatedAt = 0L,
    )

    @BeforeEach
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = SnippetRepositoryImpl(dao)
    }

    @Nested
    @DisplayName("getAllSnippets")
    inner class GetAllSnippetsTests {

        @Test
        @DisplayName("委托给 dao.getAllSnippets")
        fun getAllSnippets_delegatesToDao() = runTest {
            every { dao.getAllSnippets() } returns flowOf(listOf(sampleSnippet))
            val result = repository.getAllSnippets().first()
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("searchSnippets")
    inner class SearchSnippetsTests {

        @Test
        @DisplayName("FTS4 搜索委托给 dao")
        fun searchSnippets_delegatesToDao() = runTest {
            every { dao.searchSnippets("query") } returns flowOf(listOf(sampleSnippet))
            val result = repository.searchSnippets("query").first()
            assertEquals(1, result.size)
        }

        @Test
        @DisplayName("子串搜索委托给 dao")
        fun searchSnippetsSubstring_delegatesToDao() = runTest {
            every { dao.searchSnippetsSubstring("partial") } returns flowOf(listOf(sampleSnippet))
            val result = repository.searchSnippetsSubstring("partial").first()
            assertEquals(1, result.size)
        }
    }

    @Nested
    @DisplayName("addSnippet & updateSnippet & deleteSnippet")
    inner class CrudTests {

        @Test
        @DisplayName("addSnippet 委托给 dao.insert 并返回 ID")
        fun addSnippet_delegatesToDao() = runTest {
            coEvery { dao.insert(sampleSnippet) } returns 42L
            val id = repository.addSnippet(sampleSnippet)
            assertEquals(42L, id)
        }

        @Test
        @DisplayName("updateSnippet 委托给 dao.update")
        fun updateSnippet_delegatesToDao() = runTest {
            repository.updateSnippet(sampleSnippet)
            coVerify { dao.update(sampleSnippet) }
        }

        @Test
        @DisplayName("deleteSnippet 委托给 dao.delete")
        fun deleteSnippet_delegatesToDao() = runTest {
            repository.deleteSnippet(sampleSnippet)
            coVerify { dao.delete(sampleSnippet) }
        }
    }
}
