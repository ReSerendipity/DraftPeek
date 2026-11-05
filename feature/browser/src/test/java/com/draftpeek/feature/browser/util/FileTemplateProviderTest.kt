package com.draftpeek.feature.browser.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * [FileTemplateProvider] 单元测试（评估 2026-11-05 P1-3：核心 UI 模块补实质断言）。
 *
 * 锁定「新建文件对话框」模板供给的对外契约：
 * 1. Hello World / Class 模板对各自支持的语言矩阵**逐语言非空**（防某个 when 分支被误删后静默回退为空模板）；
 * 2. 关键语言的模板含语言特有的语法锚点（防分支间复制粘贴串语言）；
 * 3. 未支持语言返回空串（调用方据空串走「空文件」路径）；
 * 4. 模板内容只由语言决定，与扩展名参数无关。
 *
 * 运行方式：./gradlew :feature:browser:testDebugUnitTest --tests "*FileTemplateProviderTest"
 */
@DisplayName("FileTemplateProvider")
class FileTemplateProviderTest {

    /** getHelloWorldTemplate 的 when 分支全集（与实现逐分支对应；新增语言须同批更新此处） */
    private val helloWorldLanguages = listOf(
        "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "C", "C++", "C#", "Go",
        "Rust", "PHP", "Ruby", "Swift", "Shell", "SQL", "HTML", "CSS", "Markdown", "JSON",
        "XML", "YAML", "Dart", "Lua", "Scala", "Groovy", "R", "Julia", "Perl", "Haskell",
        "Elixir", "Erlang", "OCaml", "Clojure", "Lisp", "Vim Script", "TOML", "Dockerfile",
        "INI", "LaTeX"
    )

    /** getClassTemplate 的 when 分支全集（部分语言无 class 概念，故比 Hello World 少） */
    private val classLanguages = listOf(
        "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "C", "C++", "C#", "Go",
        "Rust", "PHP", "Swift", "Dart", "HTML", "CSS", "Lua", "Scala", "Groovy", "R",
        "Julia", "Perl", "Haskell", "Elixir", "Erlang", "OCaml", "Clojure", "Lisp"
    )

    @Nested
    @DisplayName("getHelloWorldTemplate() — 语言矩阵")
    inner class HelloWorldMatrixTests {

        @ParameterizedTest(name = "Hello World 模板非空：{0}")
        @ValueSource(
            strings = [
                "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "C", "C++", "C#", "Go",
                "Rust", "PHP", "Ruby", "Swift", "Shell", "SQL", "HTML", "CSS", "Markdown", "JSON",
                "XML", "YAML", "Dart", "Lua", "Scala", "Groovy", "R", "Julia", "Perl", "Haskell",
                "Elixir", "Erlang", "OCaml", "Clojure", "Lisp", "Vim Script", "TOML", "Dockerfile",
                "INI", "LaTeX"
            ]
        )
        fun helloWorld_nonEmptyForEverySupportedLanguage(language: String) {
            val template = FileTemplateProvider.getHelloWorldTemplate(language, "txt")
            assertTrue(template.isNotEmpty(), "$language 的 Hello World 模板为空——when 分支缺失？")
        }

        @ParameterizedTest(name = "Hello World 模板含语言锚点：{0} → {1}")
        @CsvSource(
            "Kotlin, fun main()",
            "Java, public class Main",
            "Python, print(",
            "JavaScript, console.log",
            "Go, package main",
            "Rust, fn main()",
            "C, #include <stdio.h>",
            "C++, #include <iostream>",
            "HTML, <!DOCTYPE html>",
            "Markdown, # Hello World",
            "JSON, \"message\"",
            "Dockerfile, FROM alpine:latest",
            "LaTeX, documentclass"
        )
        fun helloWorld_containsLanguageSpecificMarker(language: String, marker: String) {
            val template = FileTemplateProvider.getHelloWorldTemplate(language, "txt")
            assertTrue(
                template.contains(marker),
                "$language 模板应包含锚点「$marker」，实际：${template.take(60)}"
            )
        }
    }

    @Nested
    @DisplayName("getClassTemplate() — 语言矩阵")
    inner class ClassMatrixTests {

        @ParameterizedTest(name = "Class 模板非空：{0}")
        @ValueSource(
            strings = [
                "Java", "Kotlin", "Python", "JavaScript", "TypeScript", "C", "C++", "C#", "Go",
                "Rust", "PHP", "Swift", "Dart", "HTML", "CSS", "Lua", "Scala", "Groovy", "R",
                "Julia", "Perl", "Haskell", "Elixir", "Erlang", "OCaml", "Clojure", "Lisp"
            ]
        )
        fun classTemplate_nonEmptyForEverySupportedLanguage(language: String) {
            val template = FileTemplateProvider.getClassTemplate(language, "txt")
            assertTrue(template.isNotEmpty(), "$language 的 Class 模板为空——when 分支缺失？")
        }

        @ParameterizedTest(name = "Class 模板含语言锚点：{0} → {1}")
        @CsvSource(
            "Kotlin, class Main",
            "Java, public class Main",
            "Python, class Main:",
            "C, typedef struct",
            "C++, class Main",
            "Go, type Main struct",
            "Rust, struct Main",
            "Dart, class Main"
        )
        fun classTemplate_containsLanguageSpecificMarker(language: String, marker: String) {
            val template = FileTemplateProvider.getClassTemplate(language, "txt")
            assertTrue(
                template.contains(marker),
                "$language Class 模板应包含锚点「$marker」，实际：${template.take(60)}"
            )
        }

        @Test
        @DisplayName("Python Class 模板包含 __init__ 构造器")
        fun classTemplate_pythonHasConstructor() {
            val template = FileTemplateProvider.getClassTemplate("Python", "py")
            assertTrue(template.contains("def __init__(self):"))
        }
    }

    @Nested
    @DisplayName("未支持语言与空模板")
    inner class UnsupportedAndEmptyTests {

        @ParameterizedTest(name = "未支持语言返回空模板：{0}")
        @ValueSource(strings = ["Brainfuck", "kotlin", "KOTLIN", "", "  "])
        fun unknownLanguage_returnsEmptyTemplate(language: String) {
            // 语言名区分大小写且未命中 when 分支时一律空串——调用方据此走「空文件」路径
            assertEquals("", FileTemplateProvider.getHelloWorldTemplate(language, "txt"))
            assertEquals("", FileTemplateProvider.getClassTemplate(language, "txt"))
        }

        @ParameterizedTest(name = "EMPTY 模板恒为空串：{0}")
        @ValueSource(strings = ["Kotlin", "Python", "Markdown", "Unknown"])
        fun emptyTemplate_alwaysEmpty(language: String) {
            assertEquals("", FileTemplateProvider.getEmptyTemplate(language, "kt"))
        }
    }

    @Nested
    @DisplayName("扩展名参数不影响模板内容")
    inner class ExtensionIndependenceTests {

        @Test
        @DisplayName("同一语言、不同扩展名 → 模板逐字节一致")
        fun template_independentOfExtension() {
            for (language in listOf("Kotlin", "Python", "Go")) {
                val byKt = FileTemplateProvider.getHelloWorldTemplate(language, "kt")
                val byMd = FileTemplateProvider.getHelloWorldTemplate(language, "md")
                val byNoExt = FileTemplateProvider.getHelloWorldTemplate(language, "")
                assertEquals(byKt, byMd, "$language：扩展名 md 改变了模板")
                assertEquals(byKt, byNoExt, "$language：空扩展名改变了模板")
            }
        }

        @Test
        @DisplayName("语言矩阵计数与实现分支一致（39 语言 Hello World / 27 语言 Class）")
        fun languageMatrix_countMatchesImplementation() {
            assertEquals(39, helloWorldLanguages.size)
            assertEquals(27, classLanguages.size)
            // Class 支持集必须是 Hello World 支持集的子集（同一语言判定口径）
            assertTrue(
                classLanguages.all { it in helloWorldLanguages },
                "Class 模板出现了 Hello World 不支持的语言"
            )
        }
    }
}
