package com.draftpeek.core.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.dao.BookmarkDao
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.repository.BookmarkRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * BookmarkRepository → BookmarkDao → Room → SQLite 集成测试。
 *
 * 与 BookmarkRepositoryImplTest（mock DAO）不同，此测试使用真实 in-memory Room 数据库，
 * 验证完整的 SQL→Repository→Flow 数据链路，确保 Repository 委托与 DAO SQL 语句正确配合。
 *
 * 注意：使用 JUnit4 风格（非 JUnit5），因为 Robolectric 需要 @RunWith(RobolectricTestRunner)。
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkRepositoryDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BookmarkDao
    private lateinit var repository: BookmarkRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bookmarkDao()
        repository = BookmarkRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ===== addBookmark → allBookmarks flow =====

    @Test
    fun addBookmark_emitsInAllBookmarks() = runTest {
        repository.addBookmark("content://file.kt", "file.kt", "content://dir/")

        val bookmarks = repository.allBookmarks.first()
        assertEquals(1, bookmarks.size)
        assertEquals("file.kt", bookmarks[0].fileName)
    }

    @Test
    fun multipleBookmarks_sortedByFileName() = runTest {
        repository.addBookmark("uri_c", "c_file.kt", "dir")
        repository.addBookmark("uri_a", "a_file.kt", "dir")
        repository.addBookmark("uri_b", "b_file.kt", "dir")

        val bookmarks = repository.allBookmarks.first()
        assertEquals(3, bookmarks.size)
        assertEquals("a_file.kt", bookmarks[0].fileName)
        assertEquals("b_file.kt", bookmarks[1].fileName)
        assertEquals("c_file.kt", bookmarks[2].fileName)
    }

    // ===== isBookmarked =====

    @Test
    fun existingBookmark_isBookmarkedReturnsTrue() = runTest {
        repository.addBookmark("content://file.kt", "file.kt", "dir")

        assertTrue(repository.isBookmarked("content://file.kt"))
    }

    @Test
    fun nonExistentBookmark_isBookmarkedReturnsFalse() = runTest {
        assertFalse(repository.isBookmarked("content://nonexistent"))
    }

    // ===== removeBookmark =====

    @Test
    fun removeBookmark_removesFromList() = runTest {
        repository.addBookmark("content://file.kt", "file.kt", "dir")
        repository.removeBookmark("content://file.kt")

        val bookmarks = repository.allBookmarks.first()
        assertTrue(bookmarks.isEmpty())
    }

    @Test
    fun removeNonExistentBookmark_noError() = runTest {
        repository.removeBookmark("content://nonexistent")
        assertTrue(repository.allBookmarks.first().isEmpty())
    }

    // ===== getBookmarksByDirectory =====

    @Test
    fun filterByDirectory_returnsCorrectBookmarks() = runTest {
        repository.addBookmark("uri1", "f1.kt", "content://dir1/")
        repository.addBookmark("uri2", "f2.kt", "content://dir2/")
        repository.addBookmark("uri3", "f3.kt", "content://dir1/")

        val dir1Bookmarks = repository.getBookmarksByDirectory("content://dir1/").first()
        assertEquals(2, dir1Bookmarks.size)
    }

    // ===== allBookmarkUris =====

    @Test
    fun returnsAllBookmarkUris() = runTest {
        repository.addBookmark("uri1", "f1.kt", "dir")
        repository.addBookmark("uri2", "f2.kt", "dir")

        val uris = repository.allBookmarkUris.first()
        assertEquals(2, uris.size)
        assertTrue(uris.contains("uri1"))
        assertTrue(uris.contains("uri2"))
    }

    // ===== Duplicate handling =====

    @Test
    fun duplicateUri_replacesExisting() = runTest {
        repository.addBookmark("content://file.kt", "original.kt", "dir")
        repository.addBookmark("content://file.kt", "updated.kt", "dir")

        val bookmarks = repository.allBookmarks.first()
        assertEquals(1, bookmarks.size)
        assertEquals("updated.kt", bookmarks[0].fileName)
    }
}
