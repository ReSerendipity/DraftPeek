/**
 * Jetpack Compose 约定插件。
 *
 * 本插件为使用 Jetpack Compose 的模块提供统一的 Gradle 配置，避免在每个模块中重复配置。
 *
 * 自动应用的配置：
 * - Kotlin Compose 编译器插件
 * - Compose BOM 依赖版本对齐
 * - Compose 核心 UI 依赖
 * - Compose 调试工具依赖
 *
 * 注意：此插件必须在 draftpeek.android.library 或 draftpeek.android.application 之后应用。
 */
package com.draftpeek.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Android Compose 约定插件。
 *
 * 为使用 Jetpack Compose 的 Android 模块提供标准化配置。
 * 自动启用 Compose 构建特性并添加必要的 Compose 依赖。
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    /**
     * 将插件应用到目标项目。
     *
     * @param target 要应用插件的 Gradle 项目
     */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                add("implementation", platform(bom))
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-foundation").get())
                add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
