plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    jacoco
}

android {
    namespace = "com.draftpeek.core.sync"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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
    // 传输抽象用 suspend 函数（Git 实现在后续批次）
    implementation(libs.kotlinx.coroutines.android)
    // Git 传输的 HTTP 实现（版本目录已有 okhttp，无需改共享的版本目录）
    implementation(libs.okhttp)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.kotlinx.coroutines.test)
    // 用真实本地 HTTP 服务器测 OkHttp 实现（不走外网）
    testImplementation(libs.mockwebserver)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// jacocoTestReport 由根 build.gradle.kts 统一为所有 Library 模块注册（勿在此重复注册）
jacoco {
    toolVersion = "0.8.12"
}
