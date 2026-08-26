/**
 * DraftPeek 应用主数据库类。
 *
 * 使用 Room 持久化库 + SQLCipher 加密存储所有本地数据，包括：
 * - 最近打开的文件记录（[RecentFile]）
 * - 收藏的书签文件（[BookmarkEntity]）
 * - 用户代码片段库（[Snippet] + [SnippetFts] 全文搜索虚拟表）
 * - 用户活动统计数据（[UserActivity]）
 *
 * ## 加密策略
 * 数据库通过 SQLCipher 进行 AES-256 加密，密钥由 [com.draftpeek.core.data.security.DatabaseKeyManager]
 * 通过 Android Keystore 安全管理，密钥不会明文出现在内存或磁盘中。
 *
 * ## 版本历史
 * - v1-v10: 逐步添加数据表、索引、字段和 FTS 支持
 *
 * ## 迁移策略
 * 所有数据库升级均通过显式 [Migration] 完成，禁止使用 destructive migration，
 * 以确保用户数据在版本升级时不丢失。降级场景允许重建数据库。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.draftpeek.core.data.dao.BookmarkDao
import com.draftpeek.core.data.dao.LinkDao
import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.dao.SnippetDao
import com.draftpeek.core.data.dao.UserActivityDao
import com.draftpeek.core.data.entity.BookmarkEntity
import com.draftpeek.core.data.entity.LinkEntity
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.entity.SecurityEventEntity
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.entity.SnippetFts
import com.draftpeek.core.data.entity.UserActivity
import com.draftpeek.core.data.dao.SecurityEventDao

/**
 * 应用 Room 数据库主类。
 *
 * 包含所有数据表定义、DAO 访问接口、数据库迁移逻辑和初始数据填充回调。
 *
 * @property version 当前数据库版本号 = 12
 * @property exportSchema 是否导出 schema 到 JSON 文件（用于迁移测试）
 * @property entities 所有数据库实体类列表
 */
@Database(
    entities = [BookmarkEntity::class, LinkEntity::class, RecentFile::class, Snippet::class, SnippetFts::class, UserActivity::class, SecurityEventEntity::class],
    version = 12,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 获取书签数据访问对象。
     * @return [BookmarkDao] 书签表的 DAO 接口
     */
    abstract fun bookmarkDao(): BookmarkDao

    /**
     * 获取双向链接数据访问对象。
     * @return [LinkDao] 链接表的 DAO 接口
     */
    abstract fun linkDao(): LinkDao

    /**
     * 获取最近文件数据访问对象。
     * @return [RecentFileDao] 最近文件表的 DAO 接口
     */
    abstract fun recentFileDao(): RecentFileDao

    /**
     * 获取代码片段数据访问对象。
     * @return [SnippetDao] 代码片段表的 DAO 接口
     */
    abstract fun snippetDao(): SnippetDao

    /**
     * 获取用户活动数据访问对象。
     * @return [UserActivityDao] 用户活动表的 DAO 接口
     */
    abstract fun userActivityDao(): UserActivityDao

    /**
     * 获取安全事件数据访问对象。
     * @return [SecurityEventDao] 安全事件表的 DAO 接口
     */
    abstract fun securityEventDao(): SecurityEventDao

    companion object {
        /**
         * 数据库迁移：版本 1 → 2。
         *
         * 创建 `snippets` 代码片段表，用于存储用户保存的常用代码片段。
         * 表结构包含：id（自增主键）、title（标题）、content（内容）、
         * language（编程语言）、category（分类）、createdAt/updatedAt（时间戳）。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS snippets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        language TEXT,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * 数据库迁移：版本 2 → 3。
         *
         * 主要变更：
         * 1. 为 `snippets` 表创建查询优化索引（按分类、语言、更新时间）
         * 2. 为 `recent_files` 表创建索引（收藏状态、最后打开时间）
         * 3. 创建 `snippets_fts` FTS4 虚拟表，支持代码片段全文搜索
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snippets_category_updatedAt ON snippets(category, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snippets_language_updatedAt ON snippets(language, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snippets_updatedAt ON snippets(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_snippets_category ON snippets(category)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_files_isFavorite_lastOpenedAt ON recent_files(isFavorite, lastOpenedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recent_files_lastOpenedAt ON recent_files(lastOpenedAt)")

                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE snippets_fts USING fts4(
                        content=snippets,
                        title,
                        content
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * 数据库迁移：版本 3 → 4。
         *
         * 为 `recent_files` 表添加阅读位置持久化字段：
         * - cursorLine: 光标所在行（从 1 开始）
         * - cursorColumn: 光标所在列（从 1 开始）
         * - scrollX: 水平滚动偏移
         * - scrollY: 垂直滚动偏移
         *
         * 这些字段用于在重新打开文件时恢复用户上次的阅读位置。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recent_files ADD COLUMN cursorLine INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE recent_files ADD COLUMN cursorColumn INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE recent_files ADD COLUMN scrollX INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recent_files ADD COLUMN scrollY INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 数据库迁移：版本 4 → 5。
         *
         * 创建 `user_activity` 用户活动统计表，按日期聚合用户行为数据：
         * - date: 日期（yyyy-MM-dd，主键）
         * - fileOpenCount: 文件打开次数
         * - textEditCount: 文本编辑/保存次数
         * - otherOperationCount: 其他操作次数
         * - sessionCount: 会话启动次数
         * - updatedAt: 记录更新时间戳
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_activity (
                        date TEXT PRIMARY KEY NOT NULL,
                        fileOpenCount INTEGER NOT NULL,
                        textEditCount INTEGER NOT NULL,
                        otherOperationCount INTEGER NOT NULL,
                        sessionCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_activity_date ON user_activity(date)")
            }
        }

        /**
         * 数据库迁移：版本 5 → 6。
         *
         * 重建 `user_activity` 表以移除 DEFAULT 值约束，确保与 Room 实体类定义一致。
         * 使用"创建新表 → 复制数据 → 删除旧表 → 重命名"的安全迁移模式。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_activity_new (
                        date TEXT PRIMARY KEY NOT NULL,
                        fileOpenCount INTEGER NOT NULL,
                        textEditCount INTEGER NOT NULL,
                        otherOperationCount INTEGER NOT NULL,
                        sessionCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO user_activity_new (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
                    SELECT date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt FROM user_activity
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE user_activity")
                db.execSQL("ALTER TABLE user_activity_new RENAME TO user_activity")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_activity_date ON user_activity(date)")
            }
        }

        /**
         * 数据库迁移：版本 6 → 7。
         *
         * 为 `user_activity` 表添加更细粒度的操作计数字段：
         * - previewCount: Markdown/Office/PDF 预览次数
         * - searchCount: 搜索操作次数
         * - snippetCount: 代码片段创建次数
         * - diffCount: 差异对比次数
         *
         * 使用表重建模式安全添加带默认值的新列。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_activity_new (
                        date TEXT PRIMARY KEY NOT NULL,
                        fileOpenCount INTEGER NOT NULL,
                        textEditCount INTEGER NOT NULL,
                        otherOperationCount INTEGER NOT NULL,
                        sessionCount INTEGER NOT NULL,
                        previewCount INTEGER NOT NULL DEFAULT 0,
                        searchCount INTEGER NOT NULL DEFAULT 0,
                        snippetCount INTEGER NOT NULL DEFAULT 0,
                        diffCount INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO user_activity_new (date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt)
                    SELECT date, fileOpenCount, textEditCount, otherOperationCount, sessionCount, updatedAt FROM user_activity
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE user_activity")
                db.execSQL("ALTER TABLE user_activity_new RENAME TO user_activity")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_activity_date ON user_activity(date)")
            }
        }

        /**
         * 数据库迁移：版本 7 → 8。
         *
         * 为 `user_activity` 表添加使用时长和字符写入统计字段：
         * - usageDurationMinutes: 使用时长（分钟）
         * - charWriteCount: 写入字符数
         * - fileCreateCount: 文件创建次数
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_activity ADD COLUMN usageDurationMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_activity ADD COLUMN charWriteCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_activity ADD COLUMN fileCreateCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 数据库迁移：版本 8 → 9。
         *
         * 重建 `user_activity` 表以将列名规范化为 Room 默认的 camelCase 命名约定，
         * 同时为所有字段添加 DEFAULT 0 约束并使用 COALESCE 处理 NULL 值迁移。
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_activity_new (
                        date TEXT PRIMARY KEY NOT NULL,
                        fileOpenCount INTEGER NOT NULL DEFAULT 0,
                        textEditCount INTEGER NOT NULL DEFAULT 0,
                        otherOperationCount INTEGER NOT NULL DEFAULT 0,
                        sessionCount INTEGER NOT NULL DEFAULT 0,
                        previewCount INTEGER NOT NULL DEFAULT 0,
                        searchCount INTEGER NOT NULL DEFAULT 0,
                        snippetCount INTEGER NOT NULL DEFAULT 0,
                        diffCount INTEGER NOT NULL DEFAULT 0,
                        usageDurationMinutes INTEGER NOT NULL DEFAULT 0,
                        charWriteCount INTEGER NOT NULL DEFAULT 0,
                        fileCreateCount INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO user_activity_new (
                        date, fileOpenCount, textEditCount, otherOperationCount, sessionCount,
                        previewCount, searchCount, snippetCount, diffCount, updatedAt,
                        usageDurationMinutes, charWriteCount, fileCreateCount
                    )
                    SELECT
                        date,
                        COALESCE(fileOpenCount, 0),
                        COALESCE(textEditCount, 0),
                        COALESCE(otherOperationCount, 0),
                        COALESCE(sessionCount, 0),
                        COALESCE(previewCount, 0),
                        COALESCE(searchCount, 0),
                        COALESCE(snippetCount, 0),
                        COALESCE(diffCount, 0),
                        COALESCE(updatedAt, 0),
                        COALESCE(usageDurationMinutes, 0),
                        COALESCE(charWriteCount, 0),
                        COALESCE(fileCreateCount, 0)
                    FROM user_activity
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE user_activity")
                db.execSQL("ALTER TABLE user_activity_new RENAME TO user_activity")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_activity_date ON user_activity(date)")
            }
        }

        /**
         * 数据库迁移：版本 9 → 10。
         *
         * 创建 `bookmarks` 书签表，用于用户收藏常用文件：
         * - id: 自增主键
         * - uri: 文件 URI（唯一索引）
         * - fileName: 文件名
         * - directoryUri: 父目录 URI（用于按目录分组）
         * - addedAt: 添加时间戳
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uri TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        directoryUri TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarks_uri ON bookmarks(uri)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_directoryUri ON bookmarks(directoryUri)")
            }
        }

        /**
         * 数据库迁移：版本 10 → 11。
         *
         * 创建 `security_events` 安全事件日志表，用于记录 AI 对抗检测结果、
         * 响应执行和完整性校验事件（纯本地 SQLCipher 加密存储，不上传服务器）。
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS security_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventType TEXT NOT NULL,
                        threatLevel TEXT NOT NULL,
                        signalsMask INTEGER NOT NULL,
                        responseLevel TEXT NOT NULL,
                        timestampEpochMs INTEGER NOT NULL,
                        anonymizedDeviceId TEXT NOT NULL,
                        appVersionCode INTEGER NOT NULL,
                        extra TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_timestampEpochMs ON security_events(timestampEpochMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_eventType ON security_events(eventType)")
            }
        }

        /**
         * 数据库迁移：版本 11 → 12。
         *
         * 创建 `links` 双向链接表，存储 Markdown `[[目标标题]]` 双链语法产生的
         * 单向引用关系（sourceUri → targetTitle）。通过反向查询实现反向链接：
         * - 唯一索引 (sourceUri, targetTitle)：同一文档对同一目标去重
         * - 普通索引 targetTitle：反向链接查询（按被引用标题查来源）
         * - 普通索引 sourceUri：正向链接查询（按来源查目标）
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sourceUri TEXT NOT NULL,
                        targetTitle TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_links_sourceUri_targetTitle ON links(sourceUri, targetTitle)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_targetTitle ON links(targetTitle)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_links_sourceUri ON links(sourceUri)")
            }
        }

        /**
         * 默认代码片段数据类。
         *
         * 用于在数据库首次创建时预填充常用的代码模板。
         *
         * @property title 片段标题
         * @property content 片段代码内容
         * @property language 编程语言标识（如 "kotlin", "python"）
         * @property category 分类名称（如 "Kotlin", "Compose"）
         */
        private data class DefaultSnippet(
            val title: String,
            val content: String,
            val language: String?,
            val category: String,
        )

        /**
         * 默认代码片段列表。
         *
         * 首次启动应用时自动插入到数据库，为用户提供开箱即用的代码模板，
         * 涵盖 Kotlin、Compose、Room、Hilt、Python、HTML、JSON 等常见场景。
         */
        private val DEFAULT_SNIPPETS = listOf(
            DefaultSnippet(
                title = "Kotlin Hello World",
                content = """fun main() {
    println("Hello, World!")
}""",
                language = "kotlin",
                category = "Kotlin",
            ),
            DefaultSnippet(
                title = "Compose Screen Template",
                content = """@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> Content(state.data)
        is UiState.Error -> ErrorView(state.message)
    }
}""",
                language = "kotlin",
                category = "Compose",
            ),
            DefaultSnippet(
                title = "Room DAO Query",
                content = """@Dao
interface MyDao {
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    @Upsert
    suspend fun upsert(item: Item)

    @Query("SELECT * FROM items ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Item>>
}""",
                language = "kotlin",
                category = "Room",
            ),
            DefaultSnippet(
                title = "Hilt Module",
                content = """@Module
@InstallIn(SingletonComponent::class)
abstract class MyModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: RepositoryImpl): Repository
}""",
                language = "kotlin",
                category = "Hilt",
            ),
            DefaultSnippet(
                title = "Python FastAPI Endpoint",
                content = """from fastapi import FastAPI

app = FastAPI()

@app.get("/items/{item_id}")
async def read_item(item_id: int, q: str | None = None):
    return {"item_id": item_id, "q": q}""",
                language = "python",
                category = "Python",
            ),
            DefaultSnippet(
                title = "HTML5 Boilerplate",
                content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
</head>
<body>
    <h1>Hello, World!</h1>
</body>
</html>""",
                language = "html",
                category = "Web",
            ),
            DefaultSnippet(
                title = "JSON Structure",
                content = """{
    "name": "example",
    "version": "1.0.0",
    "items": [
        {"id": 1, "label": "First"},
        {"id": 2, "label": "Second"}
    ]
}""",
                language = "json",
                category = "Data",
            ),
        )

        /**
         * 数据库预填充回调。
         *
         * 在数据库首次创建时执行，将 [DEFAULT_SNIPPETS] 中的默认代码模板批量插入，
         * 并重建 FTS 索引以确保全文搜索立即可用。
         *
         * 注意：此回调仅在数据库首次创建时执行一次，后续打开数据库不会触发。
         * 使用原生 SQL 直接插入，避免回调执行时 DAO 依赖问题。
         */
        val PRE_POPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                DEFAULT_SNIPPETS.forEach { snippet ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO snippets (title, content, language, category, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf<Any?>(
                            snippet.title,
                            snippet.content,
                            snippet.language,
                            snippet.category,
                            now,
                            now,
                        )
                    )
                }
                db.execSQL("INSERT INTO snippets_fts(snippets_fts) VALUES('rebuild')")
            }
        }
    }
}
