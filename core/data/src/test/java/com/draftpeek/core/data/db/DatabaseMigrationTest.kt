package com.draftpeek.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
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

/**
 * 数据库迁移测试（P1-5 重写）。
 *
 * 验证 Migration 对象的 migrate() 方法正确执行 DDL 语句。
 * 使用 SupportSQLiteOpenHelper 直接创建空数据库（非 Room 管理），
 * 执行迁移后验证表和索引是否正确创建。
 *
 * 注意：本类使用 JUnit4 + Robolectric 风格（遵循 AGENTS.md Gotcha #23）。
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var database: AppDatabase

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    // ===== 迁移 DDL 验证 =====
    // 使用空 SQLite 数据库执行 Migration.migrate()，验证 DDL 语句正确性

    private fun createEmptyDatabase(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // 空实现：不创建任何表
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(config)
        return helper.writableDatabase
    }

    @Test
    fun migrate_10_to_11_securityEventsTableCreated() {
        val db = createEmptyDatabase()

        // 执行迁移 10 → 11
        AppDatabase.MIGRATION_10_11.migrate(db)

        // 验证 security_events 表已创建
        val cursor = db.query("SELECT count(*) FROM security_events")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证索引已创建
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='security_events'"
        )
        indexCursor.use {
            val indexNames = mutableListOf<String>()
            while (it.moveToNext()) {
                indexNames.add(it.getString(0))
            }
            assertTrue("Should have timestamp index",
                indexNames.any { name -> name.contains("timestampEpochMs") })
            assertTrue("Should have eventType index",
                indexNames.any { name -> name.contains("eventType") })
        }

        // 验证可以插入数据
        db.execSQL(
            """INSERT INTO security_events
            (eventType, threatLevel, signalsMask, responseLevel, timestampEpochMs, anonymizedDeviceId, appVersionCode)
            VALUES ('DETECTION', 'SUSPICIOUS', 0, 'WARNING', 1000, 'device', 29)"""
        )
        val dataCursor = db.query("SELECT eventType FROM security_events WHERE anonymizedDeviceId = 'device'")
        dataCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("DETECTION", it.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate_11_to_12_linksTableCreated() {
        val db = createEmptyDatabase()

        // 执行迁移 11 → 12
        AppDatabase.MIGRATION_11_12.migrate(db)

        // 验证 links 表已创建
        val cursor = db.query("SELECT count(*) FROM links")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证索引已创建
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='links'"
        )
        indexCursor.use {
            val indexNames = mutableListOf<String>()
            while (it.moveToNext()) {
                indexNames.add(it.getString(0))
            }
            assertTrue("Should have index_links_sourceUri_targetTitle",
                indexNames.any { name -> name.contains("sourceUri") && name.contains("targetTitle") })
            assertTrue("Should have index_links_targetTitle",
                indexNames.any { name -> name.contains("targetTitle") && !name.contains("sourceUri") })
            assertTrue("Should have index_links_sourceUri",
                indexNames.any { name -> name.contains("sourceUri") && !name.contains("targetTitle") })
        }

        // 验证可以插入数据
        db.execSQL(
            """INSERT INTO links (sourceUri, targetTitle, updatedAt)
            VALUES ('content://test/doc.md', 'Target Title', 1000)"""
        )
        val dataCursor = db.query("SELECT targetTitle FROM links WHERE sourceUri = 'content://test/doc.md'")
        dataCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Target Title", it.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate_9_10_bookmarksTableCreated() {
        val db = createEmptyDatabase()

        // 执行迁移 9 → 10
        AppDatabase.MIGRATION_9_10.migrate(db)

        // 验证 bookmarks 表已创建
        val cursor = db.query("SELECT count(*) FROM bookmarks")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证索引已创建
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='bookmarks'"
        )
        indexCursor.use {
            val indexNames = mutableListOf<String>()
            while (it.moveToNext()) {
                indexNames.add(it.getString(0))
            }
            assertTrue("Should have index_bookmarks_uri",
                indexNames.any { name -> name.contains("uri") && name.contains("bookmarks") })
            assertTrue("Should have index_bookmarks_directoryUri",
                indexNames.any { name -> name.contains("directoryUri") })
        }

        // 验证可以插入数据
        db.execSQL(
            """INSERT INTO bookmarks (uri, fileName, directoryUri, addedAt)
            VALUES ('content://test/doc.md', 'doc.md', 'content://test/', 1000)"""
        )
        val dataCursor = db.query("SELECT fileName FROM bookmarks WHERE uri = 'content://test/doc.md'")
        dataCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("doc.md", it.getString(0))
        }

        db.close()
    }

    @Test
    fun migrate_fullChain_9_to_12_allTablesCreated() {
        val db = createEmptyDatabase()

        // 执行完整迁移链 9 → 10 → 11 → 12
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_10_11.migrate(db)
        AppDatabase.MIGRATION_11_12.migrate(db)

        // 验证 bookmarks 表存在
        val bookmarksCursor = db.query("SELECT count(*) FROM bookmarks")
        bookmarksCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证 security_events 表存在
        val secCursor = db.query("SELECT count(*) FROM security_events")
        secCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证 links 表存在
        val linksCursor = db.query("SELECT count(*) FROM links")
        linksCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证跨表数据操作不冲突
        db.execSQL(
            """INSERT INTO bookmarks (uri, fileName, directoryUri, addedAt)
            VALUES ('content://test/doc.md', 'doc.md', 'content://test/', 1000)"""
        )
        db.execSQL(
            """INSERT INTO security_events
            (eventType, threatLevel, signalsMask, responseLevel, timestampEpochMs, anonymizedDeviceId, appVersionCode)
            VALUES ('DETECTION', 'SAFE', 0, 'NONE', 2000, 'device2', 30)"""
        )
        db.execSQL(
            """INSERT INTO links (sourceUri, targetTitle, updatedAt)
            VALUES ('content://test/doc.md', 'Target', 3000)"""
        )

        val allBookmarks = db.query("SELECT count(*) FROM bookmarks")
        allBookmarks.use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }

        val allSecEvents = db.query("SELECT count(*) FROM security_events")
        allSecEvents.use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }

        val allLinks = db.query("SELECT count(*) FROM links")
        allLinks.use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }

        db.close()
    }

    // ===== DAO 交互验证（inMemoryDatabaseBuilder，保留原有测试） =====

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
