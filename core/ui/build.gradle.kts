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

// ===== Paparazzi / compileSdk 36 兼容修复 =====
// WHY：DraftPeek 已将 compileSdk/targetSdk 升到 36（Android 16）。Paparazzi 1.3.5
// 内置的 LayoutLib 仅支持到 SDK 35；在 SDK 36 上 Renderer.configureBuildProperties
// 通过反射访问已移除的 android.os._Original_Build.VERSION_CODES_FULL 时抛
// NoSuchElementException，导致 sessionParamsBuilder 未能初始化，截图测试
// （BrandComponentScreenshotTest）随即抛 UninitializedPropertyAccessException，
// 使整条 CI 报红（Paparazzi issue #1877）。
// 官方临时修复：将 LayoutLib 强制钉到 15.2.2（含 SDK 36 反射修复）。本模块没有
// gradle.lockfile，故不会与依赖锁定冲突。待升级到 Paparazzi 2.0.0-alpha02+ 后即可移除。
configurations.all {
    resolutionStrategy {
        force(
            "com.android.tools.layoutlib:layoutlib:15.2.2",
            "com.android.tools.layoutlib:layoutlib-resources:15.2.2",
            "com.android.tools.layoutlib:layoutlib-runtime:15.2.2"
        )
    }
}
