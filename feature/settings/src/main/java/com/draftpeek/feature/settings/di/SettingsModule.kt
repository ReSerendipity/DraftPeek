/**
 * 撰码轻览 (DraftPeek) 设置模块 Hilt 依赖注入配置。
 *
 * 本文件定义 settings 模块所需的依赖绑定，使用 Dagger Hilt 注解配置 DI 容器。
 *
 * 依赖注入说明：
 * - 模块安装范围：SingletonComponent，即应用全局单例生命周期，
 *   确保设置仓库和 DataStore 实例在整个应用进程中唯一，避免多实例数据不一致
 * - 绑定方式：
 *   1. @Binds 抽象方法：将 SettingsRepository 接口绑定到 SettingsRepositoryImpl 实现类，
 *      接口注入时 Hilt 自动提供实现类实例（比 @Provides 更高效，编译期生成代码）
 *   2. @Provides companion object 方法：提供 DataStore<Preferences> 实例，
 *      因为 DataStore 需要 Context 创建，无法通过构造函数直接注入
 * - DataStore 创建：通过 Context 扩展属性 preferencesDataStore 创建单例 DataStore，
 *   文件名为 "settings"，存储在应用内部 datastore/settings.preferences_pb
 * - 作用域注解：@Singleton 确保仓库和 DataStore 都是全局单例，所有注入点共享同一实例
 *
 * 依赖关系图：
 * ```
 * Application Context
 *       ↓ (provides)
 * DataStore<Preferences>
 *       ↓ (constructor inject)
 * SettingsRepositoryImpl
 *       ↓ (binds)
 * SettingsRepository (interface)
 *       ↓ (constructor inject)
 * SettingsViewModel (@HiltViewModel)
 *       ↓
 * UI 层 (Compose Screen)
 * ```
 *
 * 模块：feature/settings
 * 其他模块可通过 @Inject SettingsRepository 直接使用设置功能
 */
package com.draftpeek.feature.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.draftpeek.feature.settings.repository.SettingsRepository
import com.draftpeek.feature.settings.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore Preferences 扩展属性，单例委托。
 *
 * 通过 preferencesDataStore 委托创建 DataStore 实例，确保同一 Context 下
 * 只有一个名为 "settings" 的 DataStore 实例，避免多实例数据冲突。
 * 文件位置：/data/data/com.draftpeek/datastore/settings.preferences_pb
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Hilt 模块：设置依赖注入配置。
 *
 * 安装在 SingletonComponent 中，所有依赖全局唯一。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    /**
     * 绑定 SettingsRepository 接口到 SettingsRepositoryImpl 实现类。
     *
     * @Binds 注解告诉 Hilt 当需要注入 SettingsRepository 时，使用 SettingsRepositoryImpl 实例。
     * @Singleton 确保仓库是全局单例，多模块共享同一实例，避免重复订阅 DataStore。
     *
     * @param impl 实现类实例，由 Hilt 自动注入（通过 @Inject constructor）
     * @return SettingsRepository 接口实例
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        /**
         * 提供 DataStore<Preferences> 单例实例。
         *
         * @Provides 方法用于创建无法通过构造函数 @Inject 的类型（如需要 Context 的第三方类）。
         * @ApplicationContext 限定符注入应用 Context，避免内存泄漏。
         * @Singleton 确保 DataStore 单例，避免重复打开底层文件。
         *
         * @param context 应用上下文，由 Hilt 自动提供
         * @return 配置好的 DataStore<Preferences> 实例
         */
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
    }
}
