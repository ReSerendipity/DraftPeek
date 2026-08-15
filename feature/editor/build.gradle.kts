plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

android {
    namespace = "com.draftpeek.feature.editor"
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
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // 与 app 模块保持一致：纯 JVM 单元测试中 Log 等 Android API 返回默认值而非抛异常
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":feature:settings"))

    // Immutable collections for @Immutable State optimization
    implementation(libs.kotlinx.collections.immutable)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.datastore.preferences)
    implementation(libs.documentfile)
    implementation(libs.windowmanager)

    // Native Android code editor replacing WebView + CodeMirror
    implementation("io.github.rosemoe:editor:0.24.6")
    implementation("io.github.rosemoe:language-textmate:0.24.6")

    // TreeSitter incremental parsing (Phase 4.1 — pilot: Java only)
    implementation(libs.sora.language.treesitter)
    implementation(libs.tree.sitter)
    implementation(libs.tree.sitter.java)

    // Markdown rendering: Markwon removed (P0) — replaced by CommonMark native (via :core:common)
    // + WebView pipeline (MarkdownWebViewPreview). CommonMark provides GFM tables,
    // strikethrough, task lists, autolink. WebView handles KaTeX/Mermaid/highlight.js.

    // LSP4J: Eclipse LSP protocol layer replacing self-implemented LSP (P1)
    implementation(libs.lsp4j)

    // Compose Rich Editor (WYSIWYG Markdown editing — Ch6#2 P1)
    implementation(libs.richeditor.compose)

    // Apache POI is provided transitively via :core:common dependency

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    // 真实 org.json 实现：JVM 单元测试中 android.jar 的 org.json 是桩（length() 恒为 0）
    testImplementation("org.json:json:20240303")

    // Android instrumented test dependencies (Compose UI Test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
