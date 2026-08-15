package com.draftpeek.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.Snippet
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

@RunWith(RobolectricTestRunner::class)
class SnippetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SnippetDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.snippetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSnippet(
        title: String = "Test Snippet",
        content: String = "fun test() { }",
        language: String? = "kotlin",
        category: String = "Kotlin",
    ) = Snippet(
        title = title,
        content = content,
        language = language,
        category = category,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    @Test
    fun insert_thenGetById() = runTest {
        val snippet = createSnippet()
        val id = dao.insert(snippet)

        val result = dao.getSnippetById(id).first()
        assertNotNull(result)
        assertEquals(snippet.title, result!!.title)
    }

    @Test
    fun getById_nonExistent_returnsNull() = runTest {
        val result = dao.getSnippetById(999L).first()
        assertNull(result)
    }

    @Test
    fun getAllSnippets_sortedByUpdatedAtDesc() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(createSnippet(title = "old").copy(updatedAt = now - 1000))
        dao.insert(createSnippet(title = "new").copy(updatedAt = now))

        val all = dao.getAllSnippets().first()
        assertEquals(2, all.size)
        assertEquals("new", all[0].title)
        assertEquals("old", all[1].title)
    }

    @Test
    fun getAllSnippets_emptyDb_returnsEmpty() = runTest {
        val all = dao.getAllSnippets().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun getSnippetsByCategory_filtersCorrectly() = runTest {
        dao.insert(createSnippet(title = "k1", category = "Kotlin"))
        dao.insert(createSnippet(title = "p1", category = "Python"))
        dao.insert(createSnippet(title = "k2", category = "Kotlin"))

        val kotlinSnippets = dao.getSnippetsByCategory("Kotlin").first()
        assertEquals(2, kotlinSnippets.size)
    }

    @Test
    fun getSnippetsByLanguage_filtersCorrectly() = runTest {
        dao.insert(createSnippet(title = "k1", language = "kotlin"))
        dao.insert(createSnippet(title = "p1", language = "python"))

        val kotlinSnippets = dao.getSnippetsByLanguage("kotlin").first()
        assertEquals(1, kotlinSnippets.size)
        assertEquals("k1", kotlinSnippets[0].title)
    }

    @Test
    fun update_changesContent() = runTest {
        val id = dao.insert(createSnippet(title = "original"))
        val snippet = dao.getSnippetById(id).first()!!
        val updated = snippet.copy(title = "updated")
        dao.update(updated)

        val result = dao.getSnippetById(id).first()
        assertEquals("updated", result!!.title)
    }

    @Test
    fun delete_removesSnippet() = runTest {
        val id = dao.insert(createSnippet())
        val snippet = dao.getSnippetById(id).first()!!

        dao.delete(snippet)

        val result = dao.getSnippetById(id).first()
        assertNull(result)
    }

    @Test
    fun searchSnippetsSubstring_matchesTitle() = runTest {
        dao.insert(createSnippet(title = "HelloWorld", content = "x"))
        dao.insert(createSnippet(title = "Other", content = "y"))

        val results = dao.searchSnippetsSubstring("Hello").first()
        assertEquals(1, results.size)
        assertEquals("HelloWorld", results[0].title)
    }

    @Test
    fun searchSnippetsSubstring_matchesContent() = runTest {
        dao.insert(createSnippet(title = "snippet1", content = "fun helloWorld() {}"))
        dao.insert(createSnippet(title = "snippet2", content = "no match here"))

        val results = dao.searchSnippetsSubstring("helloWorld").first()
        assertEquals(1, results.size)
        assertEquals("snippet1", results[0].title)
    }

    @Test
    fun getAllCategories_returnsDistinct() = runTest {
        dao.insert(createSnippet(category = "Kotlin"))
        dao.insert(createSnippet(category = "Kotlin"))
        dao.insert(createSnippet(category = "Python"))

        val categories = dao.getAllCategories().first()
        assertEquals(2, categories.size)
        assertTrue(categories.contains("Kotlin"))
        assertTrue(categories.contains("Python"))
    }

    @Test
    fun getAllLanguages_returnsDistinctExcludingNull() = runTest {
        dao.insert(createSnippet(language = "kotlin"))
        dao.insert(createSnippet(language = "python"))
        dao.insert(createSnippet(language = null))

        val languages = dao.getAllLanguages().first()
        assertEquals(2, languages.size)
        assertTrue(languages.contains("kotlin"))
        assertTrue(languages.contains("python"))
    }
}
