import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

// REFACTOR: [H1] - 签名配置外置到 signing.gradle，避免凭据读取逻辑污染主构建脚本。
// 详见 app/signing.gradle（Fail Fast + 配置外置原则）。
apply(from = "signing.gradle")

android {
    namespace = "com.draftpeek"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "com.draftpeek"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // Version management: centralized in gradle.properties
        // versionCode: Integer, must increase monotonically with each release
        // versionName: Major.Minor.Patch format
        // Both versionCode and versionName must increment together
        // To bump: ./gradlew bumpVersion -Pbump=patch (or major/minor)
        val vMajor = (project.findProperty("draftpeek.version.major") as? String)?.toInt() ?: 1
        val vMinor = (project.findProperty("draftpeek.version.minor") as? String)?.toInt() ?: 0
        val vPatch = (project.findProperty("draftpeek.version.patch") as? String)?.toInt() ?: 0
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        versionName = "$vMajor.$vMinor.$vPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK ABI filters: ensure native libraries (SQLCipher, tree-sitter) are packaged
        // for all supported ABIs. Without this, R8/APK packaging may exclude native libs,
        // causing UnsatisfiedLinkError at runtime.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        // ===== AI 对抗：构建期基线注入（零误报核心机制） =====
        // 所有基线值绝不硬编码在源码中，通过 Gradle 构建期注入。
        // CI 无签名时返回空字符串/0占位，所有校验自动跳过，零误报。
        buildConfigField(
            "String",
            "OFFICIAL_SIGNATURE_SHA256",
            "\"${fetchOfficialSignatureSha256()}\""
        )
        buildConfigField(
            "long",
            "DEX_CRC_BASELINE",
            "${computeDexCrcBaseline()}"
        )
        buildConfigField(
            "String",
            "LEGAL_NOTICE_HASH",
            "\"${computeLegalNoticeHash()}\""
        )
        // AI 防护回滚开关
        buildConfigField(
            "boolean",
            "AI_PROTECTION_ENABLED",
            "${project.findProperty("draftpeek.aiProtection.enabled") ?: "true"}"
        )

        // SECURITY VULN-005: Native C-layer anti-detection
        // CMake builds native_security.so with anti-debugging checks that
        // are harder to hook with Frida than Java/Kotlin methods.
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                cFlags("-Wall", "-Werror", "-O2")
                arguments("-DANDROID_STL=c++_static")
            }
        }
    }

    lint {
        disable += setOf("MissingTranslation", "ExtraTranslation")
        // 冻结存量 lint 债务（测试体系评估报告 P2-8）：基线中记录当前全部存量问题，
        // CI 的 lint 门禁此后只对新代码引入的问题报警——「存量冻结、增量收紧」。
        // 存量清偿后可用 ./gradlew :app:updateLintBaseline<Variant> 重新生成收窄基线。
        baseline = file("lint-baseline.xml")
    }

    buildTypes {
        debug {
            resValue("bool", "leak_canary_add_launcher_icon", "false")
            // 启用 debug 变体单元测试覆盖率采集（JaCoCo .exec），
            // 否则单元测试虽跑但通过，却不产出覆盖率数据，导致覆盖率门禁“无数据即绿”。
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            // SECURITY VULN-002: 启用 R8 代码收缩/优化/混淆。
            // proguard-rules.pro 已配置精细化的 keep 规则保护反射库
            // (Apache POI, JGit, sora-editor, tree-sitter) 和安全类。
            // -optimizations !field/marking/final 仅禁用导致 DirectAccessProps
            // IllegalAccessError 的特定优化，其他优化（内联、死代码删除）正常启用。
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "IS_BETA", "false")
            buildConfigField("String", "BUILD_CHANNEL", "\"production\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("beta") {
            // Pre-release build type for beta/rc testing.
            // Uses debug signing (no release keystore needed for QA builds).
            // Enables R8 minification to catch proguard issues before production release.
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "IS_BETA", "true")
            buildConfigField("String", "BUILD_CHANNEL", "\"beta\"")
        }
    }

    // ===== Multi-environment deployment via productFlavors =====
    // Supports three environments: dev (development), staging (QA/beta), production (release)
    // Each flavor has its own applicationId suffix and BuildConfig flags.
    // Usage: ./gradlew assembleDevDebug, assembleStagingRelease, assembleProductionRelease
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // Enable extra debugging features in dev
            buildConfigField("boolean", "IS_DEBUG_BUILD", "true")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"dev\"")
            resValue("string", "app_name", "DraftPeek Dev")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("boolean", "IS_DEBUG_BUILD", "false")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"staging\"")
            resValue("string", "app_name", "DraftPeek Staging")
        }
        create("production") {
            dimension = "environment"
            // No suffix — production uses the base applicationId
            buildConfigField("boolean", "IS_DEBUG_BUILD", "false")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"production\"")
            resValue("string", "app_name", "撰码轻览")
        }
    }

    // Baseline Profile: pre-compile AOT rules for critical user journeys
    // The baseline-prof.txt file is generated by running macrobenchmark tests
    // and placed in src/main/baseline-prof.txt (or src/release/baseline-prof.txt).
    // R8 uses these rules to pre-compile hot paths, reducing app startup time.
    // To generate: ./gradlew :benchmark:connectedBenchmarkAndroidTest
    // Then copy the generated baseline-prof.txt to src/main/baseline-prof.txt

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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

    // Compose Compiler metrics: generate stability configuration and recomposition
    // reports for performance optimization. Files are output to build/compose-metrics/.
    // Enabled by default in debug builds; disabled in release builds unless explicitly
    // requested. Override: -PcomposeMetrics=false to disable, -PcomposeMetrics=true to
    // force-enable (e.g. for release builds).
    // Output: build/compose-metrics/<module>-recomposition-report.txt
    kotlin {
        val isDebugBuild = gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) }
        val metricsEnabled = when (project.findProperty("composeMetrics")?.toString()) {
            "true" -> true
            "false" -> false
            else -> isDebugBuild // default: enabled for debug, disabled for release
        }
        if (metricsEnabled) {
            compilerOptions {
                val buildDir = project.layout.buildDirectory.asFile.get().absolutePath
                freeCompilerArgs.addAll(
                    listOf(
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                            buildDir + "/compose-metrics",
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                            buildDir + "/compose-reports"
                    )
                )
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // SECURITY VULN-005: Native C-layer anti-detection build configuration
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // jsch and jspecify both provide META-INF/versions/9/OSGI-INF/MANIFEST.MF
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            // Generic OSGI manifest duplicates from transitive dependencies
            excludes += "META-INF/OSGI-INF/MANIFEST.MF"
            // Additional common duplicate resources
            excludes += "META-INF/*.SF"
            excludes += "META-INF/*.DSA"
            excludes += "META-INF/*.RSA"
        }
    }
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
    // Exclude legacy Atlassian commonmark 0.13.0 — we use org.commonmark:0.24.0 directly
    exclude(group = "com.atlassian.commonmark")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:browser"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:terminal"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.windowmanager)

    // SECURITY VULN-009: Google Play Integrity API for device integrity verification
    implementation(libs.play.integrity)

    // Java 8+ API desugaring (required by tree-sitter native bindings)
    coreLibraryDesugaring(libs.desugar)

    // Baseline Profile Installer — runtime library for on-device profile installation.
    // Required alongside baseline-prof.txt for AOT compilation of critical user journeys.
    implementation(libs.profileinstaller)

    // Glance AppWidget — home screen widget for recent files quick access
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // ===== 全功能实施指南 v1.0: 新功能依赖 =====
    // Note: These dependencies are defined in libs.versions.toml.
    // Uncomment when the corresponding feature modules are integrated into Gradle.
    //
    // CRDT (real-time collaborative editing):
    // implementation("io.github.jan-tennert:simple-yjs:1.0.0")  // May not exist in Maven; see core/crdt/CrdtDocument.kt
    //
    // Knowledge Graph (optional graph database backend):
    // implementation(libs.neo4j.ogm.core)
    //
    // Cloud Sync (choose one or both storage backends):
    // implementation(libs.aws.java.sdk.s3)
    // implementation(libs.dropbox.core.sdk)

    debugImplementation(libs.leakcanary.android)
    debugImplementation(libs.compose.ui.tooling)

    // Logging: Timber 统一日志门面（debug 输出到 Logcat，release 输出到文件）
    implementation(libs.timber)

    // Unit test dependencies
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    // AndroidX test core for ApplicationProvider (Robolectric tests)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    // JUnit Vintage engine for JUnit 4 tests (Robolectric @RunWith)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// ===== JaCoCo 单元测试覆盖率报告 =====
// 注册 jacocoTestReport 任务，汇总所有变体单元测试产生的 .exec 执行数据，
// 生成 app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml，
// 供「Check Coverage Threshold」门禁读取。
// 修复前：未启用 enableUnitTestCoverage 且未注册该任务，报告从不生成，
// 覆盖率门禁因「无数据即绿」被绕过（exit 0）。
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "Verification"
    // 依赖全部单元测试任务，确保覆盖率执行数据已落盘
    dependsOn(tasks.withType<Test>())

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        )
        html.required.set(true)
    }

    // 采集所有变体单元测试产生的 JaCoCo .exec 执行数据
    executionData.setFrom(
        fileTree(layout.buildDirectory.asFile) { include("**/*.exec") }
    )

    // 源码目录（主源集 + 各 flavor 源集）
    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
            "src/debug/java",
            "src/debug/kotlin",
            "src/dev/java",
            "src/dev/kotlin",
            "src/devDebug/java",
            "src/devDebug/kotlin"
        )
    )

    // 编译后的 class 文件（Kotlin 输出到 tmp/kotlin-classes/<variant>）
    // 关键修复：必须限定到单一变体目录（devDebug）。
    // 若用 tmp/kotlin-classes/** 通配，会把 devDebug/stagingDebug/productionDebug
    // 等变体编译出的同名类（如 com/draftpeek/MainActivity）全部纳入，
    // JaCoCo 报 "Can't add different class with same name" 而失败。
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/devDebug")) {
            include("**/*.class")
        }
    )
}

// ===== AI 对抗：构建期基线计算函数 =====

/**
 * 计算 Release 签名证书 SHA-256（从 release keystore 读取）。
 * 读取 local.properties 中 RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD /
 * RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD 四要素。
 * CI 环境未配置时返回空字符串，所有校验自动跳过。
 */
fun fetchOfficialSignatureSha256(): String {
    val localProps = project.rootProject.file("local.properties")
    val props = Properties()
    if (localProps.exists()) {
        localProps.inputStream().use { props.load(it) }
    }
    val storeFile = (project.findProperty("RELEASE_STORE_FILE") as? String)
        ?: props.getProperty("RELEASE_STORE_FILE") ?: ""
    val storePass = (project.findProperty("RELEASE_STORE_PASSWORD") as? String)
        ?: props.getProperty("RELEASE_STORE_PASSWORD") ?: ""
    val keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as? String)
        ?: props.getProperty("RELEASE_KEY_ALIAS") ?: ""
    val keyPass = (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
        ?: props.getProperty("RELEASE_KEY_PASSWORD") ?: ""
    if (storeFile.isEmpty() || !file(storeFile).exists()) return ""
    return try {
        val ks = KeyStore.getInstance("JKS")
        ks.load(file(storeFile).inputStream(), storePass.toCharArray())
        // keyPass is validated here to ensure signing config is complete
        require(keyPass.isNotEmpty()) { "RELEASE_KEY_PASSWORD must not be empty" }
        val cert = ks.getCertificate(keyAlias) ?: return ""
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(cert.encoded)
        hash.joinToString("") { byte -> "%02x".format(byte) }
    } catch (_: Exception) {
        ""
    }
}

/** 计算 DEX CRC 基线（配置期返回占位 0，实际值在构建后由后续步骤更新） */
fun computeDexCrcBaseline(): Long = 0L

/** 计算 legal_notice_zh.txt 的 SHA-256 哈希（用于运行时校验资源完整性） */
fun computeLegalNoticeHash(): String {
    val legalFile = file("src/main/res/raw/legal_notice_zh.txt")
    if (!legalFile.exists()) return ""
    return try {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(legalFile.readBytes())
        hash.joinToString("") { byte -> "%02x".format(byte) }
    } catch (_: Exception) {
        ""
    }
}
