plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    jacoco
}

android {
    namespace = "com.draftpeek.core.data"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
        // MigrationTestHelper 需要 Instrumentation，必须跑在 androidTest
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // MigrationTestHelper 需要从测试 APK 的 assets 读取 Room 导出的 schema JSON
    // （路径：<db canonical name 的 '.' 换成 '/'>/<version>.json）。
    // 这里把 schema 导出目录注入 androidTest assets，否则 schema 校验会被静默跳过。
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

// Room schema 导出目录配置（exportSchema = true 时需要）
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Room testing (MigrationTestHelper)
    testImplementation(libs.room.testing)

    // SQLCipher for encrypted Room database
    implementation(libs.sqlcipher)
    // SECURITY VULN-016: security-crypto (alpha) removed.
    // File encryption: SecureFileStorage (self-built Keystore AES-256-GCM)
    // Credential storage: SecurePreferences (self-built Keystore AES-256-GCM)

    implementation(libs.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.okhttp)

    implementation(libs.documentfile)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    // AndroidX test core for ApplicationProvider (Robolectric tests)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    // JUnit Vintage engine for JUnit 4 tests (Robolectric @RunWith)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")

    // ===== androidTest（插桩测试）=====
    // Room 迁移 schema 校验：MigrationTestHelper 依赖 Instrumentation，只能放 androidTest
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// JaCoCo coverage configuration
jacoco {
    toolVersion = "0.8.12"
}
