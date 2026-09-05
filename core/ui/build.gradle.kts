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

        // 必须显式声明：AGP 默认使用 android.test.InstrumentationTestRunner，
        // 它不识别 JUnit4 注解，会让 BrandComponentsBehaviorTest 等用例无法执行。
        // core:ui 此前是全仓唯一漏配该 runner 的模块（其余 8 个模块均已配置）。
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // @Preview / @PreviewParameter 注解所在 artifact（BrandComponentPreviews.kt 位于 src/main）。
    // ui-tooling-preview 在 release 构建里是 no-op 存根，可安全用 implementation 常驻；
    // 预览的实际渲染工具另用 debugImplementation。
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Screenshot testing
    testImplementation(libs.paparazzi)

    // 纯 Kotlin 单元测试（RecompositionTracker 等）。
    // 与 Paparazzi 共处同一 test source set，故统一用 JUnit4 —— 本模块刻意不配置
    // useJUnitPlatform()，否则 JUnit Platform 在没有 vintage 引擎时会静默跳过 Paparazzi 用例。
    testImplementation(libs.junit4)

    // Behavior testing (Compose UI Test — 行为/交互验证，需 Android 设备/模拟器运行)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)
    // AndroidJUnit4 runner 与 runner/rules 需显式声明，否则 createComposeRule 无可用 runner
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
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
