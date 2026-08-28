package com.draftpeek.core.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.dao.SnippetDao
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * SnippetRepository → SnippetDao → Room → SQLite + FTS4 集成测试。
 *
 * 验证完整的 SQL→Repository→Flow 数据链路，包含 FTS4 全文搜索功能。
 * 与 SnippetRepositoryImplTest（mock DAO）不同，此测试使用真实 in-memory Room 数据库。
 *
 * 注意：使用 JUnit4 风格（非 JUnit5），因为 Robolectric 需要 @RunWith(RobolectricTestRunner)。
 */
@RunWith(RobolectricTestRunner::class)
class SnippetRepositoryDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SnippetDao
    private lateinit var repository: SnippetRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.snippetDao()
        repository = SnippetRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ===== addSnippet → getAllSnippets flow =====

    @Test
    fun addSnippet_emitsInAllSnippets() = runTest {
        val snippet = Snippet(
            title = "Hello World",
            content = "println(\"Hello\")",
            language = "kotlin",
            category = "Kotlin",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = repository.addSnippet(snippet)
        assertTrue(id > 0)

        val all = repository.getAllSnippets().first()
        assertEquals(1, all.size)
        assertEquals("Hello World", all[0].title)
    }

    @Test
    fun getAllSnippets_sortedByUpdatedAtDescending() = runTest {
        val base = System.currentTimeMillis()
        val s1 =
            Snippet(
                title = "First",
                content = "1",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        val s2 = Snippet(
            title = "Second",
            content = "2",
            language = "kotlin",
            category = "Kotlin",
            createdAt =
            base + 1000,
            updatedAt = base + 1000
        )
        val s3 = Snippet(
            title = "Third",
            content = "3",
            language = "kotlin",
            category = "Kotlin",
            createdAt =
            base + 2000,
            updatedAt = base + 2000
        )

        repository.addSnippet(s1)
        repository.addSnippet(s2)
        repository.addSnippet(s3)

        val all = repository.getAllSnippets().first()
        assertEquals(3, all.size)
        assertEquals("Third", all[0].title)
        assertEquals("Second", all[1].title)
        assertEquals("First", all[2].title)
    }

    // ===== getSnippetById =====

    @Test
    fun existingSnippet_getSnippetByIdReturnsIt() = runTest {
        val snippet =
            Snippet(
                title = "GetMe",
                content = "data",
                language = null,
                category = "Other",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        val id = repository.addSnippet(snippet)

        val result = repository.getSnippetById(id).first()
        assertNotNull(result)
        assertEquals("GetMe", result!!.title)
    }

    @Test
    fun nonExistentId_getSnippetByIdReturnsNull() = runTest {
        val result = repository.getSnippetById(999L).first()
        assertNull(result)
    }

    // ===== updateSnippet =====

    @Test
    fun updateSnippet_changesTitleAndContent() = runTest {
        val original =
            Snippet(
                title = "Original",
                content = "val x = 1",
                language = "kotlin",
                category = "Kotlin",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        val id = repository.addSnippet(original)

        val updated = Snippet(
            id = id,
            title = "Updated Title",
            content = "val y = 2",
            language = "kotlin",
            category = "Kotlin",
            createdAt = original.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateSnippet(updated)

        val result = repository.getSnippetById(id).first()
        assertNotNull(result)
        assertEquals("Updated Title", result!!.title)
        assertEquals("val y = 2", result.content)
    }

    // ===== deleteSnippet =====

    @Test
    fun deleteSnippet_removesFromList() = runTest {
        val snippet =
            Snippet(
                title = "ToDelete",
                content = "code",
                language = "kotlin",
                category = "Kotlin",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        val id = repository.addSnippet(snippet)
        // Create entity with actual ID from database
        val snippetWithId = snippet.copy(id = id)
        repository.deleteSnippet(snippetWithId)

        val all = repository.getAllSnippets().first()
        assertTrue(all.isEmpty())
    }

    // ===== getSnippetsByCategory =====

    @Test
    fun getSnippetsByCategory_filtersCorrectly() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "K1",
                content = "k",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "P1",
                content = "p",
                language = "python",
                category = "Python",
                createdAt = base,
                updatedAt = base
            )
        )

        val kotlin = repository.getSnippetsByCategory("Kotlin").first()
        assertEquals(1, kotlin.size)
        assertEquals("K1", kotlin[0].title)
    }

    // ===== getSnippetsByLanguage =====

    @Test
    fun getSnippetsByLanguage_filtersCorrectly() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "K1",
                content = "k",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "P1",
                content = "p",
                language = "python",
                category = "Python",
                createdAt = base,
                updatedAt = base
            )
        )

        val kotlin = repository.getSnippetsByLanguage("kotlin").first()
        assertEquals(1, kotlin.size)
    }

    // ===== FTS4 searchSnippets =====

    @Test
    fun searchSnippets_ftsSearchFindsByTitle() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "Hello World",
                content = "greeting",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "Data Class",
                content = "data",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )

        val results = repository.searchSnippets("Hello").first()
        assertEquals(1, results.size)
        assertEquals("Hello World", results[0].title)
    }

    @Test
    fun searchSnippetsByCategory_withQueryAndCategory_returnsMatchingSnippets() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "Hello Kotlin",
                content = "greeting",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "Hello Python",
                content = "greeting",
                language = "python",
                category = "Python",
                createdAt = base,
                updatedAt = base
            )
        )

        // searchSnippetsByCategory requires both query and category parameters
        val results = repository.searchSnippetsByCategory("Hello", "Kotlin").first()
        assertEquals(1, results.size)
        assertEquals("Hello Kotlin", results[0].title)
    }

    @Test
    fun searchSnippetsByLanguage_returnsMatchingSnippets() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "Coroutine",
                content = "launch",
                language = "kotlin",
                category = "Async",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "Thread",
                content = "run",
                language = "python",
                category = "Async",
                createdAt = base,
                updatedAt = base
            )
        )

        // searchSnippetsByLanguage requires both query and language parameters
        val results = repository.searchSnippetsByLanguage("Coroutine", "kotlin").first()
        assertEquals(1, results.size)
    }

    // ===== searchSnippetsSubstring (LIKE fallback) =====

    @Test
    fun searchSnippetsSubstring_findsPartialMatch() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "Coroutine Example",
                content = "launch { ... }",
                language = "kotlin",
                category = "Async",
                createdAt = base,
                updatedAt = base
            )
        )

        val results = repository.searchSnippetsSubstring("outine").first()
        assertTrue(results.isNotEmpty())
    }

    // ===== getAllCategories =====

    @Test
    fun getAllCategories_returnsUniqueCategories() = runTest {
        val base = System.currentTimeMillis()
        repository.addSnippet(
            Snippet(
                title = "K1",
                content = "k",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "K2",
                content = "k2",
                language = "kotlin",
                category = "Kotlin",
                createdAt = base,
                updatedAt = base
            )
        )
        repository.addSnippet(
            Snippet(
                title = "P1",
                content = "p",
                language = "python",
                category = "Python",
                createdAt = base,
                updatedAt = base
            )
        )

        val categories = repository.getAllCategories().first()
        assertEquals(2, categories.size)
        assertTrue(categories.contains("Kotlin"))
        assertTrue(categories.contains("Python"))
    }

    @Test
    fun noSnippets_getAllCategoriesReturnsEmpty() = runTest {
        val categories = repository.getAllCategories().first()
        assertTrue(categories.isEmpty())
    }
}
