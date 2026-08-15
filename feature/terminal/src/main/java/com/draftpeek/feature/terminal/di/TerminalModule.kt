/**
 * 终端功能依赖注入模块文件。
 *
 * 使用Hilt提供终端会话管理器和命令执行器的单例依赖注入配置。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.di

import android.content.Context
import com.draftpeek.feature.terminal.emulator.CommandExecutor
import com.draftpeek.feature.terminal.emulator.ProotSessionManager
import com.draftpeek.feature.terminal.emulator.TerminalSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * 终端功能Hilt依赖注入模块。
 * 提供TerminalSessionManager和CommandExecutor的单例实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object TerminalModule {

    /**
     * 提供TerminalSessionManager单例。
     * @return 终端会话管理器实例
     */
    @Provides
    @Singleton
    fun provideProotSessionManager(
        @ApplicationContext context: Context,
    ): ProotSessionManager {
        return ProotSessionManager(context)
    }

    @Provides
    @Singleton
    fun provideTerminalSessionManager(
        prootSessionManager: ProotSessionManager,
    ): TerminalSessionManager {
        return TerminalSessionManager(prootSessionManager)
    }

    /**
     * 提供带安全验证管道的CommandExecutor单例。
     * @param sessionManager 终端会话管理器
     * @return 命令执行器实例
     */
    @Provides
    @Singleton
    fun provideCommandExecutor(
        sessionManager: TerminalSessionManager,
    ): CommandExecutor {
        return CommandExecutor(sessionManager)
    }
}
