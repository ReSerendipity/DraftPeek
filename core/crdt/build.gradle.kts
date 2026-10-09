plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    jacoco
}

android {
    namespace = "com.draftpeek.core.crdt"
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
    // 文档层实现 core:sync 的接缝（SyncMerger / SyncSnapshot），故依赖它。
    // 反向依赖（sync → crdt）会让传输层被迫知道文档格式，故不成立。
    implementation(project(":core:sync"))

    // 纯逻辑模块：CRDT 合并与 JSON 编解码都不需要协程或网络依赖。
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    // SyncMerger.merge 是 suspend 函数，测试里需要协程构建器
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// jacocoTestReport 由根 build.gradle.kts 统一为所有 Library 模块注册（勿在此重复注册）
jacoco {
    toolVersion = "0.8.12"
}
