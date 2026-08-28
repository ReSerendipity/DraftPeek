package com.draftpeek.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bookmarkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createBookmark(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        directoryUri: String = "content://test/"
    ) = BookmarkEntity(
        uri = uri,
        fileName = fileName,
        directoryUri = directoryUri
    )

    @Test
    fun insert_thenFindByUri() = runTest {
        val bookmark = createBookmark()
        dao.insert(bookmark)

        val result = dao.findByUri(bookmark.uri)
        assertNotNull(result)
        assertEquals(bookmark.fileName, result!!.fileName)
    }

    @Test
    fun findByUri_nonExistent_returnsNull() = runTest {
        val result = dao.findByUri("content://nonexistent")
        assertNull(result)
    }

    @Test
    fun insert_duplicateUri_replacesExisting() = runTest {
        val bookmark = createBookmark(fileName = "original.kt")
        dao.insert(bookmark)

        val updated = bookmark.copy(fileName = "updated.kt")
        dao.insert(updated)

        val all = dao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("updated.kt", all[0].fileName)
    }

    @Test
    fun getAllBookmarks_sortedByFileNameAsc() = runTest {
        dao.insert(createBookmark(uri = "uri_c", fileName = "c_file.kt"))
        dao.insert(createBookmark(uri = "uri_a", fileName = "a_file.kt"))
        dao.insert(createBookmark(uri = "uri_b", fileName = "b_file.kt"))

        val all = dao.getAllBookmarks().first()
        assertEquals(3, all.size)
        assertEquals("a_file.kt", all[0].fileName)
        assertEquals("b_file.kt", all[1].fileName)
        assertEquals("c_file.kt", all[2].fileName)
    }

    @Test
    fun getAllBookmarks_emptyDb_returnsEmptyList() = runTest {
        val all = dao.getAllBookmarks().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun getBookmarksByDirectory_filtersCorrectly() = runTest {
        dao.insert(createBookmark(uri = "uri1", directoryUri = "content://dir1/"))
        dao.insert(createBookmark(uri = "uri2", directoryUri = "content://dir2/"))
        dao.insert(createBookmark(uri = "uri3", directoryUri = "content://dir1/"))

        val dir1Bookmarks = dao.getBookmarksByDirectory("content://dir1/").first()
        assertEquals(2, dir1Bookmarks.size)
    }

    @Test
    fun getBookmarksByDirectory_nonExistentDir_returnsEmpty() = runTest {
        dao.insert(createBookmark(directoryUri = "content://dir1/"))

        val result = dao.getBookmarksByDirectory("content://nonexistent/").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteByUri_removesBookmark() = runTest {
        val bookmark = createBookmark()
        dao.insert(bookmark)

        dao.deleteByUri(bookmark.uri)

        val result = dao.findByUri(bookmark.uri)
        assertNull(result)
    }

    @Test
    fun deleteByUri_nonExistent_noError() = runTest {
        dao.deleteByUri("content://nonexistent")
        val all = dao.getAllBookmarks().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun isBookmarked_existingUri_returnsTrue() = runTest {
        val bookmark = createBookmark()
        dao.insert(bookmark)

        assertTrue(dao.isBookmarked(bookmark.uri))
    }

    @Test
    fun isBookmarked_nonExistentUri_returnsFalse() = runTest {
        assertFalse(dao.isBookmarked("content://nonexistent"))
    }

    @Test
    fun getAllBookmarkUris_returnsAllUris() = runTest {
        dao.insert(createBookmark(uri = "uri1"))
        dao.insert(createBookmark(uri = "uri2"))

        val uris = dao.getAllBookmarkUris().first()
        assertEquals(2, uris.size)
        assertTrue(uris.contains("uri1"))
        assertTrue(uris.contains("uri2"))
    }
}
