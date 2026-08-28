plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    jacoco
}

android {
    namespace = "com.draftpeek.core.common"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.documentfile)
    implementation(libs.jgit)
    implementation(libs.okhttp)
    // juniversalchardet: CJK encoding detection fallback
    implementation(libs.juniversalchardet)
    // JSR-330 annotations for constructor injection (B3/DIP).
    // Resolved by Hilt/Dagger at the app level; core module only needs the API.
    implementation(libs.javax.inject)

    // Hilt (for @ApplicationContext qualifier in FeatureToggleManager)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Apache POI (Office document parsing: Word, Excel, PowerPoint)
    api(libs.poi.ooxml)
    // poi-scratchpad: legacy OLE2 formats (.doc, .ppt)
    api(libs.poi.scratchpad)

    // java-diff-utils: Mature diff library replacing self-implemented Myers algorithm (P0)
    implementation(libs.java.diff.utils)

    // PF4J: Plugin framework for Java (P1 — replacing skeleton PluginManager)
    implementation(libs.pf4j)

    // CommonMark (native Markdown parsing)
    implementation(libs.commonmark.core)
    implementation(libs.commonmark.strikethrough)
    implementation(libs.commonmark.tables)
    implementation(libs.commonmark.autolink)
    implementation(libs.commonmark.tasklist)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation("org.json:json:20240303")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// JaCoCo 模块配置
plugins.withType<com.android.build.gradle.LibraryPlugin> {
    apply(plugin = "jacoco")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generate JaCoCo coverage report for this module"

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("$projectDir/src/main/java", "$projectDir/src/main/kotlin"))

    val classDirs = fileTree("$buildDir/intermediates/classes/debug") {
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest.*")
    }
    classDirectories.setFrom(classDirs)

    executionData.setFrom(files("$buildDir/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
}
