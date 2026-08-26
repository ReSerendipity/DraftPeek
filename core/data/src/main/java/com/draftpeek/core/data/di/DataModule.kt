/**
 * 数据层 Hilt 依赖注入模块。
 *
 * 负责提供数据层所有单例依赖：
 * - 加密的 Room 数据库实例（AppDatabase，使用 SQLCipher + Android Keystore）
 * - 各 DAO 实例
 * - Repository 接口与实现的绑定
 * - NetworkConnectivityChecker 网络连接检查器
 * - CredentialProvider（Git 凭据存储）
 *
 * ## 安全说明
 * 数据库通过 SQLCipher 进行 AES-256 加密，密钥由 DatabaseKeyManager 通过
 * Android Keystore 管理。当 Keystore 密钥失效时，会自动删除无法解密的旧数据库
 * 并重建（用户会丢失最近文件索引，但应用不会崩溃）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.draftpeek.core.data.dao.BookmarkDao
import com.draftpeek.core.data.dao.LinkDao
import com.draftpeek.core.data.dao.RecentFileDao
import com.draftpeek.core.data.dao.SnippetDao
import com.draftpeek.core.data.dao.UserActivityDao
import com.draftpeek.core.data.dao.SecurityEventDao
import com.draftpeek.core.data.db.AppDatabase
import com.draftpeek.core.data.repository.BookmarkRepository
import com.draftpeek.core.data.repository.BookmarkRepositoryImpl
import com.draftpeek.core.data.repository.LinkRepository
import com.draftpeek.core.data.repository.LinkRepositoryImpl
import com.draftpeek.core.data.repository.RecentFilesRepository
import com.draftpeek.core.data.repository.RecentFilesRepositoryImpl
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.data.repository.SnippetRepositoryImpl
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.repository.UserActivityRepositoryImpl
import com.draftpeek.core.data.repository.EditorFileRepository
import com.draftpeek.core.data.repository.EditorFileRepositoryImpl
import com.draftpeek.core.data.repository.SecurityEventRepository
import com.draftpeek.core.data.repository.SecurityEventRepositoryImpl
import com.draftpeek.core.common.util.NetworkConnectivityChecker
import com.draftpeek.core.common.vcs.CredentialProvider
import com.draftpeek.core.data.security.CredentialManager
import com.draftpeek.core.data.security.DatabaseKeyManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.draftpeek.core.data.security.DatabaseKeyManager.DatabaseLockedException
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindLinkRepository(impl: LinkRepositoryImpl): LinkRepository

    @Binds
    @Singleton
    abstract fun bindRecentFilesRepository(impl: RecentFilesRepositoryImpl): RecentFilesRepository

    @Binds
    @Singleton
    abstract fun bindSnippetRepository(impl: SnippetRepositoryImpl): SnippetRepository

    @Binds
    @Singleton
    abstract fun bindUserActivityRepository(impl: UserActivityRepositoryImpl): UserActivityRepository

    @Binds
    @Singleton
    abstract fun bindEditorFileRepository(impl: EditorFileRepositoryImpl): EditorFileRepository

    @Binds
    @Singleton
    abstract fun bindSecurityEventRepository(impl: SecurityEventRepositoryImpl): SecurityEventRepository

    /**
     * Binds [CredentialManager] as the [CredentialProvider] implementation.
     *
     * [CredentialManager] uses Android Keystore + SecurePreferences for
     * secure Git credential storage. The interface lives in `core/common` so that
     * [com.draftpeek.core.common.vcs.GitManager] can depend on it without creating
     * a circular dependency on `core/data`.
     */
    @Binds
    @Singleton
    abstract fun bindCredentialProvider(impl: CredentialManager): CredentialProvider

    companion object {
        private const val DB_NAME = "draftpeek.db"

        /**
         * 提供加密的 Room 数据库实例。
         *
         * [Opt] Security: 使用 SQLCipher 加密数据库，密钥通过 Keystore 管理。
         * passphraseBytes 在使用后不应在内存中残留，但 SQLCipher API 限制
         * 无法主动清零。这是已知的 Android 平台限制。
         *
         * D-01 修复联动：当 Keystore 凭据失效导致旧 DB 无法解密时，
         * 删除不可恢复的 DB 文件并重建空库，而非让应用启动崩溃。
         * 用户最近文件索引会丢失，但应用可用——优于"白屏打不开"。
         */
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
        ): AppDatabase {
            // SECURITY: 显式加载 SQLCipher native 库。
            // sqlcipher-android 4.6.0 的自动加载机制在某些设备/ROM 上可能失败,
            // 导致 UnsatisfiedLinkError。显式调用 System.loadLibrary 确保库已加载。
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                Log.w("DataModule", "Failed to load sqlcipher native library", e)
            }

            val passphrase = try {
                DatabaseKeyManager.getOrCreatePassphrase(context)
            } catch (e: DatabaseLockedException) {
                // SECURITY: 旧 DB 已永久不可读。删除数据库文件 + WAL + SHM 后重试。
                // 重试时 getOrCreatePassphrase 会走"全新设备"路径重新生成密钥。
                Log.w("DataModule", "Keystore invalidated; rebuilding database. " +
                    "User recent-file index will be lost.", e)
                deleteDatabaseFiles(context, DB_NAME)
                DatabaseKeyManager.getOrCreatePassphrase(context)
            }
            val passphraseBytes = String(passphrase).toByteArray(Charsets.UTF_8)
            val factory = SupportOpenHelperFactory(passphraseBytes)

            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addCallback(AppDatabase.PRE_POPULATE_CALLBACK)
                .addMigrations(
                    AppDatabase.MIGRATION_1_2,
                    AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8,
                    AppDatabase.MIGRATION_8_9,
                    AppDatabase.MIGRATION_9_10,
                    AppDatabase.MIGRATION_10_11,
                    AppDatabase.MIGRATION_11_12,
                )
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                // 不使用 fallbackToDestructiveMigration —— 升级迁移失败时应抛出异常
                // 暴露迁移 bug，而非静默删除用户数据。
                // 降级场景（用户从高版本回退）才允许重建数据库。
                .build()
        }

        /**
         * 删除 Room 数据库的所有相关文件（主库 + WAL + SHM + journal）。
         * 仅在 DB 永久不可读时调用。
         */
        private fun deleteDatabaseFiles(context: Context, dbName: String) {
            val dbFile = context.getDatabasePath(dbName)
            listOf(dbFile, File(dbFile.absolutePath + "-wal"), File(dbFile.absolutePath + "-shm"),
                File(dbFile.absolutePath + "-journal")).forEach { f ->
                runCatching { if (f.exists()) f.delete() }
            }
        }

        @Provides
        fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

        @Provides
        fun provideLinkDao(db: AppDatabase): LinkDao = db.linkDao()

        @Provides
        fun provideRecentFileDao(db: AppDatabase): RecentFileDao = db.recentFileDao()

        @Provides
        fun provideSnippetDao(db: AppDatabase): SnippetDao = db.snippetDao()

        @Provides
        fun provideUserActivityDao(db: AppDatabase): UserActivityDao = db.userActivityDao()

        @Provides
        fun provideSecurityEventDao(db: AppDatabase): SecurityEventDao = db.securityEventDao()

        /**
         * Provides a [NetworkConnectivityChecker] that checks the system's
         * active network connection via [ConnectivityManager].
         */
        @Provides
        @Singleton
        fun provideNetworkConnectivityChecker(
            @ApplicationContext context: Context,
        ): NetworkConnectivityChecker = NetworkConnectivityChecker {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return@NetworkConnectivityChecker false
            val network = cm.activeNetwork ?: return@NetworkConnectivityChecker false
            val caps = cm.getNetworkCapabilities(network) ?: return@NetworkConnectivityChecker false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
