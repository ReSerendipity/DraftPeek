/**
 * Android Application 模块约定插件。
 *
 * 本插件为 Android Application 模块（app 壳模块）提供统一的 Gradle 配置，避免重复配置。
 *
 * 自动应用的配置：
 * - 从版本目录读取 SDK 版本配置（compileSdk、minSdk、targetSdk）
 * - Java 17 字节码兼容性
 * - 核心库脱糖（Core Library Desugaring）支持
 * - Android 测试 Instrumentation Runner
 * - Kotlin JVM 目标版本设置
 */
package com.draftpeek.buildlogic

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Android Application 约定插件。
 *
 * 为 Android Application 模块提供标准化配置，确保应用模块使用正确的编译设置和依赖配置。
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    /**
     * 将插件应用到目标项目。
     *
     * @param target 要应用插件的 Gradle 项目
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<AppExtension> {
                compileSdkVersion(libs.findVersion("compileSdk").get().requiredVersion.toInt())
                buildToolsVersion = libs.findVersion("buildTools").get().requiredVersion

                defaultConfig {
                    minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                    targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    isCoreLibraryDesugaringEnabled = true
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "17"
                }
            }

            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
                // 自定义 Lint 规则 JAR（build-logic/lint）
                add("lintChecks", files("${rootDir.absolutePath}/build-logic/lint/build/libs/lint.jar"))
            }

            // 运行 lint 前先构建自定义 lint JAR
            tasks.matching { it.name.startsWith("lint") }.configureEach {
                dependsOn(gradle.includedBuild("build-logic").task(":lint:jar"))
            }
        }
    }
}
