/**
 * Android Library 模块约定插件。
 *
 * 本插件为所有 Android Library 模块提供统一的 Gradle 配置，避免在每个模块中重复配置。
 *
 * 自动应用的配置：
 * - 从版本目录读取 SDK 版本配置
 * - Java 17 字节码兼容性
 * - Consumer ProGuard 规则文件配置
 * - Android 测试 Instrumentation Runner
 * - 核心库脱糖（Core Library Desugaring）支持
 * - Kotlin JVM 目标版本设置
 */
package com.draftpeek.buildlogic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Android Library 约定插件。
 *
 * 为所有 Android Library 模块提供标准化配置，确保所有库模块使用一致的编译设置和依赖配置。
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    /**
     * 将插件应用到目标项目。
     *
     * @param target 要应用插件的 Gradle 项目
     */
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                compileSdkVersion(libs.findVersion("compileSdk").get().requiredVersion.toInt())
                buildToolsVersion = libs.findVersion("buildTools").get().requiredVersion

                defaultConfig {
                    minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                    consumerProguardFiles("consumer-rules.pro")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    isCoreLibraryDesugaringEnabled = true
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                testOptions {
                    unitTests.isIncludeAndroidResources = true
                }
            }

            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "17"
                }
            }

            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
            }
        }
    }
}
