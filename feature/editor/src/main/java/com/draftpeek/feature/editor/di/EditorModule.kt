/**
 * 文件：EditorModule.kt
 * 功能：编辑器模块 Hilt 依赖注入配置
 * 主要类/接口：EditorModule、ThemeModule、DiagnosticModule
 * 模块依赖：
 *   - dagger：Hilt 依赖注入框架
 *   - repository/EditorRepository：编辑器仓库接口
 *   - sora/CustomThemeRepository：自定义主题仓库接口
 *   - diagnostics/DiagnosticNavigator：诊断导航器接口
 */
package com.draftpeek.feature.editor.di

import com.draftpeek.feature.editor.diagnostics.DefaultDiagnosticNavigator
import com.draftpeek.feature.editor.diagnostics.DiagnosticNavigator
import com.draftpeek.feature.editor.repository.EditorRepository
import com.draftpeek.feature.editor.repository.EditorRepositoryImpl
import com.draftpeek.feature.editor.sora.CustomThemeRepository
import com.draftpeek.feature.editor.sora.CustomThemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

/**
 * 编辑器模块 Hilt DI 配置。
 *
 * 绑定 EditorRepository 接口到 EditorRepositoryImpl 实现。
 * 使用 ViewModelComponent 生命周期，与 ViewModel 同生命周期。
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class EditorModule {

    /**
     * 绑定 EditorRepository 实现。
     *
     * @param impl EditorRepositoryImpl 实例
     * @return EditorRepository 接口实例
     */
    @Binds
    abstract fun bindEditorRepository(impl: EditorRepositoryImpl): EditorRepository
}

/**
 * 主题模块 Hilt DI 配置。
 *
 * 绑定 CustomThemeRepository 接口到 CustomThemeRepositoryImpl 实现。
 * 使用 SingletonComponent 生命周期，全局单例。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    /**
     * 绑定 CustomThemeRepository 实现。
     *
     * @param impl CustomThemeRepositoryImpl 实例
     * @return CustomThemeRepository 接口实例
     */
    @Binds
    abstract fun bindCustomThemeRepository(impl: CustomThemeRepositoryImpl): CustomThemeRepository
}

/**
 * 诊断模块 Hilt DI 配置。
 *
 * 绑定 DiagnosticNavigator 接口到 DefaultDiagnosticNavigator 实现。
 * 使用 SingletonComponent 生命周期，全局单例。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticModule {

    /**
     * 绑定 DiagnosticNavigator 实现。
     *
     * @param impl DefaultDiagnosticNavigator 实例
     * @return DiagnosticNavigator 接口实例
     */
    @Binds
    abstract fun bindDiagnosticNavigator(impl: DefaultDiagnosticNavigator): DiagnosticNavigator
}
