plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.paparazzi)
    jacoco
}

android {
    namespace = "com.draftpeek.core.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.windowmanager)
    implementation(libs.kotlinx.collections.immutable)

    // Screenshot testing
    testImplementation(libs.paparazzi)
}

// ===== Paparazzi / compileSdk 36 兼容说明 =====
// 在 compileSdk 36 上，Paparazzi 1.3.5 的 Renderer.configureBuildProperties 会反射已移除的
// android.os._Original_Build.VERSION_CODES_FULL 而崩溃，并连锁导致 sessionParamsBuilder
// 未初始化（Paparazzi issue #1877）。
// 曾尝试强制钉住 LayoutLib 15.2.2 作为临时方案（经 CI run 33541705813 验证未能解决：
// 崩溃仅从 NoSuchElementException 变为 IllegalStateException at Renderer.kt:96），
// 因为真正的修复在 Paparazzi 自身的 Renderer.kt 中，且官方未向 1.3.x 回移植。
// 故改为升级到 Paparazzi 2.0.0-alpha02（自带 LayoutLib 15.2.3，含 SDK 36 修复），
// 并配套升级 AGP 8.10.1 / Gradle 8.14.2 以满足其版本矩阵要求。
