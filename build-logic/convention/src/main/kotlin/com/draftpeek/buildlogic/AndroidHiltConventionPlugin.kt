/**
 * Hilt 依赖注入约定插件。
 *
 * 本插件为启用 Hilt 依赖注入的模块提供统一的 Gradle 配置，避免在每个模块中重复配置。
 *
 * 自动应用的配置：
 * - Hilt Gradle 插件
 * - KSP 注解处理器
 * - Hilt Android 核心依赖
 * - Hilt Navigation Compose 依赖
 */
package com.draftpeek.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Android Hilt 约定插件。
 *
 * 为需要使用 Hilt 依赖注入的 Android 模块提供标准化配置。
 * 应用此插件后，模块即可直接使用 @HiltAndroidApp、@AndroidEntryPoint 等 Hilt 注解。
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    /**
     * 将插件应用到目标项目。
     *
     * @param target 要应用插件的 Gradle 项目
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.dagger.hilt.android")
                apply("com.google.devtools.ksp")
            }

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
            }
        }
    }
}
