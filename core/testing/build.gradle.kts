plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.draftpeek.core.testing"
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
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:common"))

    // Room testing
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // Coroutines test
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)

    // MockK
    implementation(libs.mockk)

    // JUnit 5
    implementation(libs.junit5.api)
    implementation(libs.junit5.params)

    // Robolectric
    implementation(libs.robolectric)
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.ext.junit)
}
