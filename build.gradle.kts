import com.diffplug.spotless.LineEnding
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
    // 显式平台行尾策略：默认 GIT_ATTRIBUTES 策略在 Windows 上会扫描/哈希 .gradle 内部锁文件，
    // 导致 config-cache 存储阶段报 "Failed to create MD5 hash for checksums.lock"（CI Linux 不受影响）。
    lineEndings = LineEnding.PLATFORM_NATIVE
    kotlin {
        target("**/*.kt")
        targetExclude(
            "**/build/**",
            "**/.gradle/**",
            "**/generated/**",
            "_archive/**"
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

        // 启用 debug 变体单元测试覆盖率采集（JaCoCo .exec）。
        // 否则 testDebugUnitTest 虽跑但通过、不产出 .exec，
        // 下方 jacocoTestReport 因无执行数据被 SKIPPED（静默失效，BUILD 仍 SUCCESSFUL），
        // 正是本次要根治的历史问题。app 模块在 app/build.gradle.kts 已单独开启。
        configure<com.android.build.gradle.LibraryExtension> {
            buildTypes {
                getByName("debug") {
                    enableUnitTestCoverage = true
                }
            }
        }

        tasks.withType<JacocoReport> {
            group = "verification"
            description = "Generate JaCoCo coverage report"

            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        // 统一为所有库模块注册 jacocoTestReport（2026-09-05 修复静默失效）。
        //
        // 背景：此前只有 core/common 自建过一份，但它的 executionData 指向
        // build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec
        // —— 这是 AGP 4.x 时代的路径，AGP 8 的真实产物在
        // build/jacoco/<variant>UnitTest.exec。旧路径不存在 → JacocoReport 因无执行数据
        // 被 **SKIPPED**，而 Gradle 仍报 BUILD SUCCESSFUL：报告从未产出，
        // 覆盖率数字无从谈起（无任何报错，属静默失效）。其余库模块则根本没有报告任务。
        //
        // 现收敛为一份正确实现，覆盖全部库模块。
        tasks.register<JacocoReport>("jacocoTestReport") {
            group = "verification"
            description = "Generate JaCoCo coverage report for this module"

            // 保证 exec 数据是最新的，避免报告基于陈旧数据
            dependsOn("testDebugUnitTest")

            reports {
                xml.required.set(true)
                html.required.set(true)
            }

            sourceDirectories.setFrom(
                files("$projectDir/src/main/java", "$projectDir/src/main/kotlin")
            )

            // AGP 8：Kotlin 与 Java 类最终汇入 intermediates/classes/<variant>
            // （已过 ASM transform，是 tmp/kotlin-classes 的超集，实测 205 > 201）。
            classDirectories.setFrom(
                fileTree(layout.buildDirectory.dir("intermediates/classes/debug")) {
                    exclude(
                        "**/R.class",
                        "**/R\$*.class",
                        "**/BuildConfig.*",
                        "**/Manifest.*"
                    )
                }
            )

            executionData.setFrom(
                fileTree(layout.buildDirectory.dir("jacoco")) {
                    include("testDebugUnitTest.exec")
                }
            )
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

            // androidTest Java 资源合并排除。
            // WHY：JUnit 5 的 6 个 artifact（junit-jupiter / -api / -engine / -params、
            // junit-platform-commons / -engine）各自携带 META-INF/LICENSE.md 与
            // META-INF/LICENSE-notice.md，AGP 的 MergeJavaRes 遇到「同名不同源」会直接
            // 抛 DuplicateRelativeFileException 而失败。main c51f9bd 上
            // :feature:browser:mergeDebugAndroidTestJavaResource 即因此报红，
            // 并连带整个 instrumented 阶段（API 26/30/34）失败。
            // 放在根脚本统一注入，所有 library 模块自动继承；此前只在个别模块修过
            // （app 自带 packaging 块、core/* 单独加过），feature/* 因而漏网——
            // 逐模块复制正是这类回归的根因，故收敛到此处。
            packaging {
                resources {
                    excludes += setOf(
                        // JUnit 5 的每个 artifact 都随包一份同名许可文件
                        // （实测 5.8.2 版本由传递依赖引入，非版本目录声明的 5.11.3）。
                        "META-INF/LICENSE.md",
                        "META-INF/LICENSE-notice.md",
                        // 其余 6 条对齐 app 与 core/testing 的已知完备集合：
                        // bouncycastle / jspecify / jsch / jgit 会带上 OSGI 清单与签名文件。
                        // MergeJavaRes 是 fail-fast 的——只补 LICENSE.md 会在下一轮才暴露
                        // OSGI-INF/MANIFEST.MF 冲突（feature/browser 恰好依赖 jsch），故一次补齐。
                        "META-INF/{AL2.0,LGPL2.1}",
                        "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                        "META-INF/OSGI-INF/MANIFEST.MF",
                        "META-INF/*.SF",
                        "META-INF/*.DSA",
                        "META-INF/*.RSA"
                    )
                }
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

// 根级聚合任务：让 CI 的 `./gradlew jacocoTestReport`（未限定路径）能一次性触发
// 所有模块（库模块 + app）的覆盖率报告。否则未限定路径的 jacocoTestReport 会因
// 根项目无此任务而报 “Task 'jacocoTestReport' not found in root project”。
val jacocoAggregate = tasks.register("jacocoTestReport") {
    group = "verification"
    description = "Aggregate JaCoCo coverage reports across all modules"
}
// 注意：不能用 subprojects.forEach { it.tasks... } 急切遍历 —— 那会在配置期强制
// 物化所有子项目的 Task 容器（破坏配置缓存 / 拖慢配置），且库模块的
// jacocoTestReport 是在 plugins.withType<LibraryPlugin> 回调里**延迟**注册的，
// 急切遍历时可能还没注册上（漏依赖）。matching{}.configureEach{} 是 live collection，
// 对后续注册的任务同样生效。
// 另注：TaskProvider 没有 dependsOn（那是 Task 的方法），必须 .configure { } 内部调用。
subprojects {
    tasks.matching { it.name == "jacocoTestReport" }.configureEach {
        jacocoAggregate.configure { dependsOn(this@configureEach) }
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
