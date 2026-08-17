import com.android.build.api.dsl.CommonExtension
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.paparazzi) apply false
    jacoco
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/generated/**",
        )
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "indent_size" to "4",
                    "max_line_length" to "120",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
        )
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "indent_size" to "4",
                    "max_line_length" to "120",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// JaCoCo 代码覆盖率配置
tasks.register("jacocoTestReport", JacocoReport::class) {
    group = "verification"
    description = "Generate JaCoCo code coverage report"

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(file("$buildDir/reports/jacoco/report.xml"))
        html.outputLocation.set(file("$buildDir/reports/jacoco/html"))
    }

    // 收集所有模块的测试任务和源代码
    val classDirectories = fileTree(projectDir) {
        include("**/build/intermediates/classes/debug/**")
        include("**/build/tmp/kotlin-classes/debug/**")
        exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest.*")
        exclude("**/*_H.class", "**/*_Factory.class", "**/*_MembersInjector.class")
        exclude("**/*ComposableSingletons*.*", "**/*Preview*.*")
        exclude("**/build/**", "**/generated/**")
    }

    val sourceDirectories = fileTree(projectDir) {
        include("**/src/main/java/**", "**/src/main/kotlin/**")
        exclude("**/build/**", "**/generated/**")
    }

    val executionData = fileTree(projectDir) {
        include("**/build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("**/build/jacoco/*.exec")
        include("**/build/outputs/code_coverage/**/*.ec")
    }

    sourceDirectories.setFrom(sourceDirectories)
    classDirectories.setFrom(classDirectories)
    executionData.setFrom(executionData)

    dependsOn("testDebugUnitTest")
}
