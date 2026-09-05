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

// JaCoCo 版本统一钉死（CI 事故修复）
// WHY：Gradle 8.14.1 内置的 jacoco 插件默认 toolVersion 为 0.8.13，而
// app/gradle.lockfile 的依赖锁定把 org.jacoco:org.jacoco.agent 钉在
// strictly 0.8.12。app 模块只声明了 `jacoco` 插件、未显式设置 toolVersion，
// 于是解析出 0.8.13 与锁定冲突，:app:testDevBetaUnitTest 在写配置缓存时
// 直接失败，Build & Test 每次推送都报红。
// 此处以回调方式对**所有**模块（app 与各 library）统一注入目录版本，既修掉
// 当前冲突，也避免今后新增模块重蹈覆辙；子模块里散落的硬编码 toolVersion
// 会写入同一个值，无副作用。
allprojects {
    plugins.withId("jacoco") {
        extensions.configure<org.gradle.testing.jacoco.plugins.JacocoPluginExtension>("jacoco") {
            toolVersion = libs.versions.jacoco.get()
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/generated/**"
        )
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_standard_max-line-length" to "disabled",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_standard_backing-property-naming" to "disabled",
                    "ktlint_standard_value-parameter-comment" to "disabled",
                    "ktlint_standard_no-empty-file" to "disabled",
                    "ktlint_standard_property-naming" to "disabled"
                )
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude(
            "**/build/**",
            "**/.gradle/**"
        )
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "indent_size" to "4",
                    "max_line_length" to "120"
                )
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

    // 禁用 MissingTranslation 和 ExtraTranslation lint 检查
    // 因为默认语言是中文，不需要所有字符串都翻译成所有语言
    // 同时为所有子模块添加 beta 构建类型，以匹配 app 模块的构建类型
    plugins.withId("com.android.library") {
        configure<com.android.build.gradle.LibraryExtension> {
            lint {
                disable += setOf("MissingTranslation", "ExtraTranslation")
            }

            // 添加 beta 构建类型（如果不存在）
            buildTypes {
                if (findByName("beta") == null) {
                    create("beta") {
                        initWith(getByName("release"))
                        isMinifyEnabled = false
                    }
                }
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
    // P1-7: 将 project 引用提到配置期捕获，避免 doLast 执行期捕获 project
    // 触发配置缓存（configuration-cache）违反。
    val bumpType = (project.findProperty("bump") as? String) ?: "patch"
    val propsFile = rootProject.file("gradle.properties")
    doLast {
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
        println(
            "Version bumped: $currentMajor.$currentMinor.$currentPatch → $newVersionName (versionCode=$newVersionCode)"
        )
        println("Remember to:")
        println("  1. Update CHANGELOG.md with the new version entry")
        println("  2. Commit the changes: git add gradle.properties CHANGELOG.md")
        println("  3. Tag the release: git tag v$newVersionName")
        // SECURITY(红线): tag 必须推到 private 远程。若照旧推 origin，tag 会把
        // 私有 main 的完整提交历史带入公开仓库（与 2026-09-02 误推事故同源）。
        println("  4. Push the tag to the PRIVATE remote: git push private v$newVersionName")
        println("     (NEVER 'git push origin v...' — it would leak main history to the public repo)")
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
        // P1-4: STRICT 模式 —— 缺锁文件 / 引入未锁定依赖一律构建失败，
        // 防止新依赖绕过 libs.versions.toml 静默溜入。
        // 前提：全部 15 个项目的 gradle.lockfile 已生成并入库（2026-09-04 完成）。
        // 依赖变更后的更新流程：./gradlew <project>:dependencies --write-locks
        lockMode.set(org.gradle.api.artifacts.dsl.LockMode.STRICT)
    }
}

// ===== CHANGELOG Generation Task =====
// Usage: ./gradlew generateChangelog -Pversion=1.0.31
// Parses conventional commits from git log and inserts a new CHANGELOG.md entry.
// Requires Python 3 on PATH. Run after bumpVersion, before tagging.
tasks.register("generateChangelog") {
    group = "draftpeek"
    description = "Generate CHANGELOG.md entry from conventional commits. Usage: -Pversion=1.0.31"
    // P1-7: 将 project / rootDir 引用提到配置期捕获，避免 doLast 执行期捕获
    // project 触发配置缓存（configuration-cache）违反。
    val version = project.findProperty("version") as? String
    val scriptFile = rootProject.file("scripts/generate_changelog.py")
    val rootDirectory = rootDir
    doLast {
        if (version == null) {
            throw GradleException("Usage: ./gradlew generateChangelog -Pversion=1.0.31")
        }
        val scriptPath = scriptFile.absolutePath
        val process = ProcessBuilder("python3", scriptPath, "--version", version, "--insert")
            .directory(rootDirectory)
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
