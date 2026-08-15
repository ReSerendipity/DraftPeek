plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    jacoco
}

android {
    namespace = "com.draftpeek.core.domain"
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
    // Domain layer depends only on core:data interfaces (not implementations)
    implementation(project(":core:data"))
    implementation(project(":core:common"))

    // JSR-330 for @Inject, @Singleton annotations
    implementation(libs.javax.inject)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Collection (ImmutableList)
    implementation(libs.kotlinx.collections.immutable)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// JaCoCo coverage configuration
jacoco {
    toolVersion = "0.8.12"
}


