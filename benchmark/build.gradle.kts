plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.draftpeek.benchmark"
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
        // 必须开启：本模块依赖 :feature:editor，后者传递引入
        // com.itsaky.androidide.treesitter（android-tree-sitter / tree-sitter-java 4.3.2），
        // 这两个 AAR 的元数据要求启用 core library desugaring。
        // 未开启时 checkDebugAndroidTestAarMetadata 直接失败，
        // 导致本模块全部 Macrobenchmark 无法编译（此前长期未被发现——
        // StartupBenchmark / FileReadBenchmark 也因此从未真正跑起来）。
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    // This is important for benchmarks - you need to disable some optimizations
    // to get accurate measurements
    buildTypes {
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            // org.jspecify:jspecify 与 com.github.mwiede:jsch 都提供
            // META-INF/versions/9/OSGI-INF/MANIFEST.MF；本模块合并 androidTest
            // 资源时报 DuplicateRelativeFileException，导致 :benchmark:mergeDebug
            // AndroidTestJavaResource 失败、Instrumented Tests / Macrobenchmark 全红。
            // 与 :app 的 packaging 排除口径保持一致（app/build.gradle.kts）。
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:browser"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    // Hilt (required by feature modules that depend on Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Hilt testing support (used in FileReadBenchmark)
    implementation(libs.hilt.android.testing)

    // Java 8+ API desugaring (required by tree-sitter AARs pulled in via :feature:editor)
    coreLibraryDesugaring(libs.desugar)

    // Benchmark dependencies
    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.benchmark.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)

    // Macro-benchmark for startup and UI benchmarks
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)

    // Test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
