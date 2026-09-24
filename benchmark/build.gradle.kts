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
        // CI 上的模拟器只作 smoke test（用例跑不跑得起来），不作性能基线：
        // 硬基线只认专用设备 runner（真机 / Cuttlefish），模拟器读数没有可比性。
        // androidx.benchmark 默认会用断言拒绝在受污染的测量环境下出数，三条各自的来源：
        //   EMULATOR        —— 5 条 macro 用例（Startup×3 / EditorScroll / EditorInput）
        //   ACTIVITY-MISSING —— 8 条 FileRead*：:benchmark 是自 instrument 的 library，
        //                       microbenchmark 全程不启动 Activity
        //   DEBUGGABLE      —— 同上 8 条：未配 android.testBuildType，androidTest 只能跟
        //                       debug 变体走，被测进程必然带 FLAG_DEBUGGABLE
        // 只抑制 EMULATOR 的话那 8 条仍会红，故三条一起列。本行不改变任何被测代码路径，
        // 也不放宽发布产物口径（release 包仍走 release.yml 的 apksigner 校验）。
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,DEBUGGABLE,ACTIVITY-MISSING"
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
