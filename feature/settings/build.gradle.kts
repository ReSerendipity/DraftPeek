plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

android {
    namespace = "com.draftpeek.feature.settings"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// WHY：本模块的 Robolectric 测试（DataStoreRoundTripTest / SettingsRepositoryRoundTripTest）
// 之前对 JaCoCo 完全不可见 —— Robolectric 用自己的 SandboxClassLoader 重新加载类，
// 绕过了默认按「类路径位置」匹配的插桩，导致 SettingsRepositoryImpl 等
// 明明被测到的类在报告里 covered=0。isIncludeNoLocationClasses 让 agent 不再按位置过滤；
// excludes 掉 jdk.internal.* 是 JDK 9+ 上的标准配套（否则 agent 会在模块化 JDK 内部类上崩）。
// 无障碍页迁入后分母被 Compose 代码生成撑大，靠这批仓储测试把覆盖率拉回棘轮阈值之上。
tasks.withType<Test>().configureEach {
    configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric + AndroidX Test 支撑 JUnit4 风格测试（如 DataStoreRoundTripTest，
    // 依靠下方 junit-vintage-engine 在 useJUnitPlatform() 下被发现）。缺这些依赖会导致
    // beta 等变体的单元测试编译失败、整个 Android CI 假红。配置与 core/data 模块保持一致；
    // 平台/注解一致性规则由仓内 scripts/check_junit_platform.py 静态拦截。
    testImplementation(libs.robolectric)
    // AndroidX test core 提供 ApplicationProvider（Robolectric 测试需要）
    testImplementation(libs.androidx.test.core)
    // 提供 org.junit.* (JUnit4) 注解，供 Robolectric @RunWith 测试使用
    testImplementation(libs.androidx.test.ext.junit)
    // JUnit Vintage 引擎：在 useJUnitPlatform() 下运行 JUnit4 @RunWith 测试
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:6.1.3")

    // Android instrumented test dependencies (Compose UI Test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
