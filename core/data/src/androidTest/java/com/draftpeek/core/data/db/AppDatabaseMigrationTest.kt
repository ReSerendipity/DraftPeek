/**
 * Room 迁移回归测试（R6 整改，评估报告 2026-09-04 §4-R6）。
 *
 * 依赖前置（core/data/build.gradle.kts 已就绪）：
 * - `androidTest.assets.srcDirs(schemas)`：MigrationTestHelper 从测试 APK assets 读取
 *   Room 导出的 schema JSON（core/data/schemas/AppDatabase/7-12.json）；
 * - `androidTestImplementation(libs.room.testing)` + test runner + ext junit。
 *
 * 覆盖内容：
 * - 7→12 全链路迁移：每一步使用 AppDatabase 中真实的 Migration 对象执行，
 *   最终 schema 与 12.json 严格比对（validateDroppedTables=true，多余/缺失表均报错）；
 * - 10→12 近期迁移：覆盖最近两步（含 11→12 links 表与唯一索引）。
 *
 * 说明：
 * - 早期版本（1-6）schema 未导出，最早可创建的版本为 7；后续新增迁移时
 *   必须同时提交 schema JSON，并在本测试追加对应 Migration 对象；
 * - 本测试校验 schema 正确性；「迁移不丢数据」的数据保持性断言需按各版本
 *   真实建表结构 seed 数据，留待后续增强；
 * - `migrations/001_create_knowledge_graph.sql` 为独立 SQL 文档（未绑定 Room
 *   版本步骤），Room 实际迁移以 AppDatabase.MIGRATION_* 为准。
 *
 * 运行：`./gradlew :core:data:connectedDevDebugAndroidTest`（需设备/模拟器）
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate7To12_chainValidatesAgainstExportedSchemas() {
        val dbName = "migration-test-7-12"

        // 以版本 7 的导出 schema 创建空库
        val dbV7 = helper.createDatabase(dbName, 7)
        dbV7.close()

        // 逐版迁移至 12，最终 schema 与 12.json 严格校验
        val dbV12 = helper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12,
        )
        dbV12.close()
    }

    @Test
    fun migrate10To12_recentStepsValidate() {
        val dbName = "migration-test-10-12"

        val dbV10 = helper.createDatabase(dbName, 10)
        dbV10.close()

        val dbV12 = helper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12,
        )
        dbV12.close()
    }

    @Test
    fun migrate11To12_createsLinksTableWithUniqueIndex() {
        val dbName = "migration-test-11-12"

        val dbV11 = helper.createDatabase(dbName, 11)
        dbV11.close()

        val dbV12 = helper.runMigrationsAndValidate(
            dbName,
            12,
            true,
            AppDatabase.MIGRATION_11_12,
        )

        // 显式断言 11→12 的产物：links 表存在，且唯一索引可用
        val linkCount = dbV12.query("SELECT COUNT(*) FROM links").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
        check(linkCount == 0L) { "fresh migrated DB should have empty links table" }

        dbV12.close()
    }
}
