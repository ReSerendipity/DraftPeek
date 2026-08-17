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
subprojects {
    plugins.withType<com.android.build.gradle.LibraryPlugin> {
        apply(plugin = "jacoco")
        
        tasks.withType<JacocoReport> {
            group = "verification"
            description = "Generate JaCoCo coverage report"
            
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
    }
}
