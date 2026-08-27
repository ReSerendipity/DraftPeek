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

// ===== Version Bump Task =====
// Usage: ./gradlew bumpVersion -Pbump=major|minor|patch
// Reads current version from gradle.properties, increments the specified
// component, writes back, and prints the new version.
tasks.register("bumpVersion") {
    group = "draftpeek"
    description = "Increment version number in gradle.properties. Usage: -Pbump=patch|minor|major"
    doLast {
        val bumpType = (project.findProperty("bump") as? String) ?: "patch"
        val propsFile = rootProject.file("gradle.properties")
        val content = propsFile.readText()

        val majorRegex = Regex("""^draftpeek\.version\.major=(\d+)$""", RegexOption.MULTILINE)
        val minorRegex = Regex("""^draftpeek\.version\.minor=(\d+)$""", RegexOption.MULTILINE)
        val patchRegex = Regex("""^draftpeek\.version\.patch=(\d+)$""", RegexOption.MULTILINE)

        val currentMajor = majorRegex.find(content)?.groupValues?.get(1)?.toInt() ?: 1
        val currentMinor = minorRegex.find(content)?.groupValues?.get(1)?.toInt() ?: 0
        val currentPatch = patchRegex.find(content)?.groupValues?.get(1)?.toInt() ?: 0

        val (newMajor, newMinor, newPatch) = when (bumpType) {
            "major" -> Triple(currentMajor + 1, 0, 0)
            "minor" -> Triple(currentMajor, currentMinor + 1, 0)
            "patch" -> Triple(currentMajor, currentMinor, currentPatch + 1)
            else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Use major, minor, or patch.")
        }

        var updated = content
            .replace(majorRegex, "draftpeek.version.major=$newMajor")
            .replace(minorRegex, "draftpeek.version.minor=$newMinor")
            .replace(patchRegex, "draftpeek.version.patch=$newPatch")
        propsFile.writeText(updated)

        val newVersionCode = newMajor * 10000 + newMinor * 100 + newPatch
        val newVersionName = "$newMajor.$newMinor.$newPatch"
        println("Version bumped: $currentMajor.$currentMinor.$currentPatch → $newVersionName (versionCode=$newVersionCode)")
        println("Remember to:")
        println("  1. Update CHANGELOG.md with the new version entry")
        println("  2. Commit the changes: git add gradle.properties CHANGELOG.md")
        println("  3. Tag the release: git tag v$newVersionName")
        println("  4. Push: git push origin v$newVersionName")
    }
}

// ===== Dependency Locking =====
// Ensures reproducible builds by locking transitive dependency versions.
// To generate lock files: ./gradlew dependencies --write-locks
// To update lock files after intentional dependency changes: ./gradlew dependencies --update-locks group:artifact
// Lock files are committed to git at gradle.lockfile in each module.
allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

// ===== CHANGELOG Generation Task =====
// Usage: ./gradlew generateChangelog -Pversion=1.0.31
// Parses conventional commits from git log and inserts a new CHANGELOG.md entry.
// Requires Python 3 on PATH. Run after bumpVersion, before tagging.
tasks.register("generateChangelog") {
    group = "draftpeek"
    description = "Generate CHANGELOG.md entry from conventional commits. Usage: -Pversion=1.0.31"
    doLast {
        val version = (project.findProperty("version") as? String)
            ?: throw GradleException("Usage: ./gradlew generateChangelog -Pversion=1.0.31")
        val scriptPath = rootProject.file("scripts/generate_changelog.py").absolutePath
        val process = ProcessBuilder("python3", scriptPath, "--version", version, "--insert")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException("generate_changelog.py failed (exit $exitCode):\n$output")
        }
        println(output)
    }
}
