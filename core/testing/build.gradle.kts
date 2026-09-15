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

    // JUnit 5 各 jar 自带 META-INF/LICENSE.md，androidTest APK 的 Java 资源合并默认
    // 「只允许一份」会报 DuplicateRelativeFileException（实测 6 份冲突，PR #22 run
    // 34931393472）——测试 APK 不分发，直接排除即可（含经典配套 LICENSE-notice.md）。
    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
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
