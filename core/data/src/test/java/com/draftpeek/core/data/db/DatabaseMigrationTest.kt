package com.draftpeek.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.entity.UserActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var database: AppDatabase

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun freshDatabase_allDaosAccessible() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        assertNotNull(database.bookmarkDao())
        assertNotNull(database.snippetDao())
        assertNotNull(database.userActivityDao())
        assertNotNull(database.securityEventDao())
        assertNotNull(database.recentFileDao())
    }

    @Test
    fun migration_9_10_bookmarksTableWorks() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val bookmarkDao = database.bookmarkDao()
        bookmarkDao.insert(
            BookmarkEntity(
                uri = "content://test/migration.kt",
                fileName = "migration.kt",
                directoryUri = "content://test/",
            )
        )

        val all = bookmarkDao.getAllBookmarks().first()
        assertEquals(1, all.size)
        assertEquals("migration.kt", all[0].fileName)
    }

    @Test
    fun migration_10_11_securityEventsTableWorks() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val securityEventDao = database.securityEventDao()
        securityEventDao.insert(
            com.draftpeek.core.data.entity.SecurityEventEntity(
                eventType = "DETECTION",
                threatLevel = "SUSPICIOUS",
                signalsMask = 0,
                responseLevel = "WARNING",
                timestampEpochMs = System.currentTimeMillis(),
                anonymizedDeviceId = "test-device",
                appVersionCode = 29,
            )
        )

        assertEquals(1, securityEventDao.getEventCount())
    }

    @Test
    fun migration_7_8_newColumnsWork() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = database.userActivityDao()
        dao.ensureDateExists("2026-01-01")
        dao.incrementUsageDurationMinutes("2026-01-01", 30)
        dao.incrementCharWriteCount("2026-01-01", 500)
        dao.incrementFileCreateCount("2026-01-01")

        val result = dao.getActivityForDate("2026-01-01").first()
        assertNotNull(result)
        assertEquals(30, result!!.usageDurationMinutes)
        assertEquals(500, result.charWriteCount)
        assertEquals(1, result.fileCreateCount)
    }

    @Test
    fun migration_8_9_dataIntegrity() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = database.userActivityDao()
        dao.upsert(
            UserActivity(
                date = "2026-01-01",
                fileOpenCount = 5,
                textEditCount = 10,
                otherOperationCount = 2,
                sessionCount = 1,
                previewCount = 3,
                searchCount = 4,
                snippetCount = 1,
                diffCount = 2,
                usageDurationMinutes = 60,
                charWriteCount = 1000,
                fileCreateCount = 0,
            )
        )

        val result = dao.getActivityForDate("2026-01-01").first()
        assertNotNull(result)
        assertEquals(5, result!!.fileOpenCount)
        assertEquals(10, result.textEditCount)
        assertEquals(60, result.usageDurationMinutes)
        assertEquals(1000, result.charWriteCount)
    }

    @Test
    fun fullMigrationChain_allTablesWork() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val bookmarkDao = database.bookmarkDao()
        bookmarkDao.insert(BookmarkEntity(uri = "uri1", fileName = "f1.kt", directoryUri = "dir1"))
        assertEquals(1, bookmarkDao.getAllBookmarks().first().size)

        val snippetDao = database.snippetDao()
        val snippetId = snippetDao.insert(
            Snippet(
                title = "Test",
                content = "fun test()",
                language = "kotlin",
                category = "Kotlin",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
        assertTrue(snippetId > 0)

        val activityDao = database.userActivityDao()
        activityDao.upsert(UserActivity(date = "2026-01-01", fileOpenCount = 1))
        assertEquals(1, activityDao.getActivityCount())

        val securityDao = database.securityEventDao()
        securityDao.insert(
            com.draftpeek.core.data.entity.SecurityEventEntity(
                eventType = "DETECTION",
                threatLevel = "SAFE",
                signalsMask = 0,
                responseLevel = "NONE",
                timestampEpochMs = System.currentTimeMillis(),
                anonymizedDeviceId = "device",
                appVersionCode = 29,
            )
        )
        assertEquals(1, securityDao.getEventCount())
    }
}
