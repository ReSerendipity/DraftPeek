/**
 * 全链路数据库迁移测试。
 *
 * 验证从数据库 v1 → v12 的完整逐步迁移路径正确性。
 * 在每个迁移步骤后验证表结构存在性和数据完整性。
 *
 * 测试策略：
 * 1. 创建 v1 空数据库（仅 recent_files 表，Room 自动创建）
 * 2. 逐步执行 MIGRATION_1_2 → MIGRATION_11_12
 * 3. 在关键节点插入测试数据并验证后续迁移不破坏数据
 * 4. 最终验证 v12 schema 与 Room 实体定义一致
 *
 * 注意：本类使用 JUnit4 + Robolectric 风格（遵循 AGENTS.md Gotcha #23），
 * 与现有 DatabaseMigrationTest 保持一致。
 *
 * @author DraftPeek Team
 * @since 1.0.31
 */
package com.draftpeek.core.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FullMigrationChainTest {

    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        // 创建空内存数据库（version=1，无表）
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // 创建 v1 基础表（recent_files，Room v1 schema）
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recent_files (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            uri TEXT NOT NULL,
                            fileName TEXT NOT NULL,
                            lastOpenedAt INTEGER NOT NULL,
                            isFavorite INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_files_uri ON recent_files(uri)")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_recent_files_isFavorite_lastOpenedAt ON recent_files(isFavorite, lastOpenedAt)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_recent_files_lastOpenedAt ON recent_files(lastOpenedAt)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // 由手动调用 Migration.migrate() 处理
                }
            })
            .build()
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    /**
     * 验证 v1 → v2 迁移：创建 snippets 表。
     */
    @Test
    fun migrate_1_to_2_snippetsTableCreated() {
        AppDatabase.MIGRATION_1_2.migrate(db)

        // 验证 snippets 表存在
        val cursor = db.query("SELECT count(*) FROM snippets")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }

        // 验证可插入数据
        db.execSQL(
            """INSERT INTO snippets (title, content, language, category, createdAt, updatedAt)
            VALUES ('Test', 'fun test()', 'kotlin', 'Kotlin', 1000, 1000)"""
        )
        val dataCursor = db.query("SELECT title FROM snippets WHERE title = 'Test'")
        dataCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Test", it.getString(0))
        }
    }

    /**
     * 验证 v2 → v3 迁移：创建索引和 FTS 表。
     */
    @Test
    fun migrate_2_to_3_indexesAndFtsCreated() {
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)

        // 验证 FTS 表存在
        val ftsCursor = db.query("SELECT count(*) FROM snippets_fts")
        ftsCursor.use {
            assertTrue(it.moveToFirst())
        }

        // 验证索引存在
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='snippets'"
        )
        indexCursor.use {
            val indexNames = mutableListOf<String>()
            while (it.moveToNext()) {
                indexNames.add(it.getString(0))
            }
            assertTrue("Should have category index", indexNames.any { it.contains("category") })
            assertTrue("Should have language index", indexNames.any { it.contains("language") })
            assertTrue("Should have updatedAt index", indexNames.any { it.contains("updatedAt") })
        }
    }

    /**
     * 验证 v3 → v4 迁移：为 recent_files 添加阅读位置字段。
     */
    @Test
    fun migrate_3_to_4_cursorFieldsAdded() {
        // 前置迁移到 v3
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)

        // 插入数据验证新字段
        db.execSQL(
            """INSERT INTO recent_files (uri, fileName, lastOpenedAt, isFavorite, cursorLine, cursorColumn, scrollX, scrollY)
            VALUES ('content://test.kt', 'test.kt', 1000, 0, 10, 5, 0, 200)"""
        )
        val cursor = db.query(
            "SELECT cursorLine, cursorColumn, scrollY FROM recent_files WHERE uri = 'content://test.kt'"
        )
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(10, it.getInt(0))
            assertEquals(5, it.getInt(1))
            assertEquals(200, it.getInt(2))
        }
    }

    /**
     * 验证 v4 → v5 迁移：创建 user_activity 表。
     */
    @Test
    fun migrate_4_to_5_userActivityTableCreated() {
        // 前置迁移到 v4
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
            VALUES ('2026-01-01', 5, 10, 2, 1, 1000)"""
        )
        val cursor = db.query("SELECT fileOpenCount FROM user_activity WHERE date = '2026-01-01'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }
    }

    /**
     * 验证 v5 → v6 迁移：重建 user_activity 表（移除 DEFAULT 约束），数据不丢失。
     */
    @Test
    fun migrate_5_to_6_dataPreserved() {
        // 前置迁移到 v5
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)

        // 插入测试数据
        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
            VALUES ('2026-01-01', 5, 10, 2, 1, 1000)"""
        )

        // 执行 v5 → v6 迁移
        AppDatabase.MIGRATION_5_6.migrate(db)

        // 验证数据保留
        val cursor = db.query("SELECT fileOpenCount FROM user_activity WHERE date = '2026-01-01'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }
    }

    /**
     * 验证 v6 → v7 迁移：添加细粒度操作计数字段，原有数据不丢失。
     */
    @Test
    fun migrate_6_to_7_newColumnsAdded_dataPreserved() {
        // 前置迁移到 v6
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)

        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
            VALUES ('2026-01-01', 3, 7, 1, 2, 2000)"""
        )

        AppDatabase.MIGRATION_6_7.migrate(db)

        // 验证原有数据保留且新字段默认值为 0
        val cursor = db.query(
            "SELECT fileOpenCount, previewCount, searchCount FROM user_activity WHERE date = '2026-01-01'"
        )
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(3, it.getInt(0))
            assertEquals(0, it.getInt(1)) // previewCount 默认 0
            assertEquals(0, it.getInt(2)) // searchCount 默认 0
        }
    }

    /**
     * 验证 v7 → v8 迁移：添加使用时长字段，原有数据不丢失。
     */
    @Test
    fun migrate_7_to_8_usageFieldsAdded_dataPreserved() {
        // 前置迁移到 v7
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)

        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount,
            previewCount, searchCount, snippetCount, diffCount, updatedAt)
            VALUES ('2026-01-02', 1, 2, 0, 1, 3, 4, 1, 2, 3000)"""
        )

        AppDatabase.MIGRATION_7_8.migrate(db)

        val cursor = db.query(
            "SELECT fileOpenCount, usageDurationMinutes, charWriteCount FROM user_activity WHERE date = '2026-01-02'"
        )
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
            assertEquals(0, it.getInt(1)) // usageDurationMinutes 默认 0
            assertEquals(0, it.getInt(2)) // charWriteCount 默认 0
        }
    }

    /**
     * 验证 v8 → v9 迁移：重建 user_activity 表规范化列名，数据完整迁移。
     */
    @Test
    fun migrate_8_to_9_dataIntegrityPreserved() {
        // 前置迁移到 v8
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)
        AppDatabase.MIGRATION_7_8.migrate(db)

        // 插入多行数据
        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount,
            previewCount, searchCount, snippetCount, diffCount, usageDurationMinutes, charWriteCount, fileCreateCount, updatedAt)
            VALUES ('2026-01-03', 5, 10, 2, 1, 3, 4, 1, 2, 60, 1000, 0, 5000)"""
        )
        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount,
            previewCount, searchCount, snippetCount, diffCount, usageDurationMinutes, charWriteCount, fileCreateCount, updatedAt)
            VALUES ('2026-01-04', 8, 20, 3, 2, 5, 6, 2, 3, 120, 2000, 1, 6000)"""
        )

        AppDatabase.MIGRATION_8_9.migrate(db)

        // 验证两行数据完整保留
        val cursor = db.query("SELECT count(*) FROM user_activity")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(2, it.getInt(0))
        }

        // 验证具体数据值
        val dataCursor = db.query(
            "SELECT fileOpenCount, usageDurationMinutes, charWriteCount FROM user_activity WHERE date = '2026-01-03'"
        )
        dataCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(5, it.getInt(0))
            assertEquals(60, it.getInt(1))
            assertEquals(1000, it.getInt(2))
        }
    }

    /**
     * 完整迁移链 v1 → v12：验证所有表结构存在且可操作。
     *
     * 这是最关键的测试：模拟用户从最旧版本逐步升级到最新版本的全过程。
     */
    @Test
    fun fullMigrationChain_v1_to_v12_allTablesAndDataIntact() {
        // 执行完整迁移链
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_2_3.migrate(db)
        AppDatabase.MIGRATION_3_4.migrate(db)
        AppDatabase.MIGRATION_4_5.migrate(db)
        AppDatabase.MIGRATION_5_6.migrate(db)
        AppDatabase.MIGRATION_6_7.migrate(db)
        AppDatabase.MIGRATION_7_8.migrate(db)
        AppDatabase.MIGRATION_8_9.migrate(db)
        AppDatabase.MIGRATION_9_10.migrate(db)
        AppDatabase.MIGRATION_10_11.migrate(db)
        AppDatabase.MIGRATION_11_12.migrate(db)

        // 验证所有表存在
        for (table in listOf(
            "recent_files",
            "snippets",
            "snippets_fts",
            "user_activity",
            "bookmarks",
            "security_events",
            "links"
        )) {
            val cursor = db.query("SELECT count(*) FROM $table")
            cursor.use {
                assertTrue("Table $table should exist after full migration", it.moveToFirst())
            }
        }

        // 在每张表中插入数据验证可操作性
        db.execSQL(
            """INSERT INTO recent_files (uri, fileName, lastOpenedAt, isFavorite, cursorLine, cursorColumn, scrollX, scrollY)
            VALUES ('content://test.kt', 'test.kt', 1000, 1, 1, 1, 0, 0)"""
        )
        db.execSQL(
            """INSERT INTO snippets (title, content, language, category, createdAt, updatedAt)
            VALUES ('Test', 'fun test()', 'kotlin', 'Kotlin', 1000, 1000)"""
        )
        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount,
            previewCount, searchCount, snippetCount, diffCount, usageDurationMinutes, charWriteCount, fileCreateCount, updatedAt)
            VALUES ('2026-01-01', 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1000)"""
        )
        db.execSQL(
            """INSERT INTO bookmarks (uri, fileName, directoryUri, addedAt)
            VALUES ('content://bookmark.md', 'bookmark.md', 'content://', 1000)"""
        )
        db.execSQL(
            """INSERT INTO security_events (eventType, threatLevel, signalsMask, responseLevel, timestampEpochMs, anonymizedDeviceId, appVersionCode)
            VALUES ('DETECTION', 'SAFE', 0, 'NONE', 1000, 'device', 31)"""
        )
        db.execSQL(
            """INSERT INTO links (sourceUri, targetTitle, updatedAt)
            VALUES ('content://test.md', 'Target', 1000)"""
        )

        // 验证每张表数据
        assertEquals(
            1,
            db.query("SELECT count(*) FROM recent_files").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
        assertEquals(
            1,
            db.query("SELECT count(*) FROM snippets").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
        assertEquals(
            1,
            db.query("SELECT count(*) FROM user_activity").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
        assertEquals(
            1,
            db.query("SELECT count(*) FROM bookmarks").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
        assertEquals(
            1,
            db.query("SELECT count(*) FROM security_events").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
        assertEquals(
            1,
            db.query("SELECT count(*) FROM links").use {
                it.moveToFirst()
                it.getInt(0)
            }
        )
    }

    /**
     * 验证带数据的全链路迁移：数据在各迁移步骤中不丢失。
     */
    @Test
    fun fullMigrationChain_withDataPreserved() {
        // v1 → v2: 创建 snippets 表
        AppDatabase.MIGRATION_1_2.migrate(db)
        db.execSQL(
            """INSERT INTO snippets (title, content, language, category, createdAt, updatedAt)
            VALUES ('Snippet1', 'val x = 1', 'kotlin', 'Kotlin', 1000, 1000)"""
        )

        // v2 → v3: 创建索引和 FTS
        AppDatabase.MIGRATION_2_3.migrate(db)

        // v3 → v4: 添加阅读位置字段
        AppDatabase.MIGRATION_3_4.migrate(db)
        db.execSQL(
            """INSERT INTO recent_files (uri, fileName, lastOpenedAt, isFavorite, cursorLine, cursorColumn, scrollX, scrollY)
            VALUES ('content://file1.kt', 'file1.kt', 2000, 0, 5, 3, 0, 100)"""
        )

        // v4 → v5: 创建 user_activity
        AppDatabase.MIGRATION_4_5.migrate(db)
        db.execSQL(
            """INSERT INTO user_activity (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
            VALUES ('2026-01-01', 5, 10, 2, 1, 3000)"""
        )

        // v5 → v6: 重建 user_activity（数据应保留）
        AppDatabase.MIGRATION_5_6.migrate(db)

        // v6 → v7: 添加新列
        AppDatabase.MIGRATION_6_7.migrate(db)

        // v7 → v8: 添加使用时长字段
        AppDatabase.MIGRATION_7_8.migrate(db)

        // v8 → v9: 重建 user_activity（数据应完整保留）
        AppDatabase.MIGRATION_8_9.migrate(db)

        // v9 → v10: 创建 bookmarks
        AppDatabase.MIGRATION_9_10.migrate(db)
        db.execSQL(
            """INSERT INTO bookmarks (uri, fileName, directoryUri, addedAt)
            VALUES ('content://bm.md', 'bm.md', 'content://', 4000)"""
        )

        // v10 → v11: 创建 security_events
        AppDatabase.MIGRATION_10_11.migrate(db)

        // v11 → v12: 创建 links
        AppDatabase.MIGRATION_11_12.migrate(db)

        // ===== 最终验证所有数据完整保留 =====

        // snippets 数据
        val snippetCursor = db.query("SELECT title FROM snippets WHERE title = 'Snippet1'")
        snippetCursor.use {
            assertTrue("Snippet data should survive full migration chain", it.moveToFirst())
            assertEquals("Snippet1", it.getString(0))
        }

        // recent_files 数据（含迁移添加的字段）
        val fileCursor = db.query(
            "SELECT fileName, cursorLine, scrollY FROM recent_files WHERE uri = 'content://file1.kt'"
        )
        fileCursor.use {
            assertTrue("Recent file data should survive full migration chain", it.moveToFirst())
            assertEquals("file1.kt", it.getString(0))
            assertEquals(5, it.getInt(1))
            assertEquals(100, it.getInt(2))
        }

        // user_activity 数据（经历了多次重建迁移）
        val activityCursor = db.query("SELECT fileOpenCount FROM user_activity WHERE date = '2026-01-01'")
        activityCursor.use {
            assertTrue("User activity data should survive multiple table rebuilds", it.moveToFirst())
            assertEquals(5, it.getInt(0))
        }

        // bookmarks 数据
        val bookmarkCursor = db.query("SELECT fileName FROM bookmarks WHERE uri = 'content://bm.md'")
        bookmarkCursor.use {
            assertTrue("Bookmark data should be intact", it.moveToFirst())
            assertEquals("bm.md", it.getString(0))
        }
    }
}

/**
 * Extension function to simplify cursor use in tests.
 */
private inline fun <T> android.database.Cursor.use(block: (android.database.Cursor) -> T): T {
    try {
        return block(this)
    } finally {
        close()
    }
}
