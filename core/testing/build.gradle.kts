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

    // androidTest APK 的 Java 资源合并默认「只允许一份」，而类路径上的
    // JUnit 5（META-INF/LICENSE.md）与 bouncycastle + jspecify
    // （META-INF/versions/9/OSGI-INF/MANIFEST.MF）都会触发
    // DuplicateRelativeFileException（PR #22 / #54 实测）。测试 APK 不对外
    // 分发，排除集直接对齐 app 模块的已知完备集合。
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/OSGI-INF/MANIFEST.MF",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
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
