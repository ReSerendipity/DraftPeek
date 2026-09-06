plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    jacoco
}

android {
    namespace = "com.draftpeek.core.common"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    // R1: 生成 BuildConfig，用于注入远程策略 Ed25519 公钥
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")

        // R1: 远程策略验签公钥（X.509 DER base64）。为空 → verifySignature fail-closed。
        buildConfigField(
            "String",
            "POLICY_ED25519_PUBLIC_KEY",
            "\"${project.findProperty("policyEd25519PublicKey") ?: ""}\""
        )
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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.documentfile)
    implementation(libs.jgit)
    implementation(libs.okhttp)
    // juniversalchardet: CJK encoding detection fallback
    implementation(libs.juniversalchardet)
    // JSR-330 annotations for constructor injection (B3/DIP).
    // Resolved by Hilt/Dagger at the app level; core module only needs the API.
    implementation(libs.javax.inject)

    // Hilt (for @ApplicationContext qualifier in FeatureToggleManager)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Apache POI (Office document parsing: Word, Excel, PowerPoint)
    api(libs.poi.ooxml)
    // poi-scratchpad: legacy OLE2 formats (.doc, .ppt)
    api(libs.poi.scratchpad)

    // java-diff-utils: Mature diff library replacing self-implemented Myers algorithm (P0)
    implementation(libs.java.diff.utils)

    // PF4J: Plugin framework for Java (P1 — replacing skeleton PluginManager)
    implementation(libs.pf4j)

    // CommonMark (native Markdown parsing)
    implementation(libs.commonmark.core)
    implementation(libs.commonmark.strikethrough)
    implementation(libs.commonmark.tables)
    implementation(libs.commonmark.autolink)
    implementation(libs.commonmark.tasklist)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.json)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// JaCoCo 模块配置
plugins.withType<com.android.build.gradle.LibraryPlugin> {
    apply(plugin = "jacoco")
}

// jacocoTestReport 任务已上收至根 build.gradle.kts（subprojects + LibraryPlugin），
// 为所有库模块统一注册。
//
// 移除原因（2026-09-05）：本模块原先自建的 executionData 指向
// build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec
// （AGP 4.x 路径），AGP 8 实际产物在 build/jacoco/，旧路径不存在导致任务被
// SKIPPED 却仍 BUILD SUCCESSFUL —— 覆盖率报告从未真正产出过。
// 保留此处注释以免有人按旧写法复原。
