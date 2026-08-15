package com.draftpeek.core.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.entity.RecentFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Instrumented unit tests for [RecentFileDao].
 *
 * Uses an in-memory Room database (no SQLCipher) to test DAO operations
 * without needing the encryption key infrastructure.
 *
 * Robolectric is configured to run these tests on the JVM without a device.
 */
@RunWith(RobolectricTestRunner::class)
class RecentFileDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RecentFileDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.recentFileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRecentFile(
        uri: String = "content://test/file.kt",
        fileName: String = "file.kt",
        language: String? = "kotlin",
        isFavorite: Boolean = false,
    ) = RecentFile(
        uri = uri,
        fileName = fileName,
        language = language,
        lastOpenedAt = System.currentTimeMillis(),
        isFavorite = isFavorite,
        fileSize = 100L,
    )

    @Test
    fun insert_newFile_canBeQueried() = runTest {
        val file = createRecentFile()
        dao.insertOrUpdate(file)

        val result = dao.getReadingPosition(file.uri)
        assertEquals(file.fileName, result?.fileName)
    }

    @Test
    fun insert_duplicateUri_updatesExisting() = runTest {
        val file = createRecentFile(fileName = "original.kt")
        dao.insertOrUpdate(file)

        val updated = file.copy(fileName = "updated.kt")
        dao.insertOrUpdate(updated)

        val all = dao.getAllRecentFilesOneShot()
        assertEquals(1, all.size)
        assertEquals("updated.kt", all[0].fileName)
    }

    @Test
    fun toggleFavorite_falseToTrue() = runTest {
        val file = createRecentFile(isFavorite = false)
        dao.insertOrUpdate(file)

        dao.toggleFavorite(file.uri)

        val result = dao.getReadingPosition(file.uri)
        assertTrue(result?.isFavorite == true)
    }

    @Test
    fun toggleFavorite_trueToFalse() = runTest {
        val file = createRecentFile(isFavorite = true)
        dao.insertOrUpdate(file)

        dao.toggleFavorite(file.uri)

        val result = dao.getReadingPosition(file.uri)
        assertFalse(result?.isFavorite == true)
    }

    @Test
    fun updateReadingPosition_updatesCorrectly() = runTest {
        val file = createRecentFile()
        dao.insertOrUpdate(file)

        dao.updateReadingPosition(file.uri, line = 42, column = 7, scrollX = 100, scrollY = 200)

        val result = dao.getReadingPosition(file.uri)
        assertEquals(42, result?.cursorLine)
        assertEquals(7, result?.cursorColumn)
        assertEquals(100, result?.scrollX)
        assertEquals(200, result?.scrollY)
    }

    @Test
    fun delete_byUri_removesFile() = runTest {
        val file = createRecentFile()
        dao.insertOrUpdate(file)

        dao.delete(file.uri)

        val result = dao.getReadingPosition(file.uri)
        assertNull(result)
    }

    @Test
    fun deleteOlderThan_removesOldNonFavorite() = runTest {
        val now = System.currentTimeMillis()
        val oldTime = now - 86400000 // 1 day ago
        val oldFile = createRecentFile(uri = "old", isFavorite = false).copy(lastOpenedAt = oldTime)
        val newFile = createRecentFile(uri = "new", isFavorite = false).copy(lastOpenedAt = now)
        dao.insertOrUpdate(oldFile)
        dao.insertOrUpdate(newFile)

        dao.deleteOlderThan(now)

        val all = dao.getAllRecentFilesOneShot()
        assertEquals(1, all.size)
        assertEquals("new", all[0].uri)
    }

    @Test
    fun deleteOlderThan_keepsFavorite() = runTest {
        val oldTime = System.currentTimeMillis() - 86400000
        val oldFavorite = createRecentFile(uri = "favorite", isFavorite = true).copy(lastOpenedAt = oldTime)
        dao.insertOrUpdate(oldFavorite)

        dao.deleteOlderThan(System.currentTimeMillis())

        val all = dao.getAllRecentFilesOneShot()
        assertEquals(1, all.size)
    }

    @Test
    fun getFavorites_returnsOnlyFavorites() = runTest {
        val favorite = createRecentFile(uri = "fav", isFavorite = true)
        val nonFavorite = createRecentFile(uri = "non-fav", isFavorite = false)
        dao.insertOrUpdate(favorite)
        dao.insertOrUpdate(nonFavorite)

        val favorites = dao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("fav", favorites[0].uri)
    }
}
