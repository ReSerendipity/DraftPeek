package com.draftpeek.feature.editor.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LanguageMapper")
class LanguageMapperTest {

    @Nested
    @DisplayName("fromExtension()")
    inner class FromExtensionTests {

        @Test
        @DisplayName("常见编程语言扩展名正确映射")
        fun commonExtensions_mappedCorrectly() {
            assertEquals("kotlin", LanguageMapper.fromExtension("kt"))
            assertEquals("python", LanguageMapper.fromExtension("py"))
            assertEquals("java", LanguageMapper.fromExtension("java"))
            assertEquals("javascript", LanguageMapper.fromExtension("js"))
            assertEquals("typescript", LanguageMapper.fromExtension("ts"))
            assertEquals("go", LanguageMapper.fromExtension("go"))
            assertEquals("rust", LanguageMapper.fromExtension("rs"))
            assertEquals("cpp", LanguageMapper.fromExtension("c"))
            assertEquals("cpp", LanguageMapper.fromExtension("cpp"))
        }

        @Test
        @DisplayName("纯文本扩展名映射为 text")
        fun txtExtension_mappedToText() {
            assertEquals("text", LanguageMapper.fromExtension("txt"))
        }

        @Test
        @DisplayName("未知扩展名返回 null")
        fun unknownExtension_returnsNull() {
            assertNull(LanguageMapper.fromExtension("xyz123"))
        }

        @Test
        @DisplayName("空字符串返回 null")
        fun emptyExtension_returnsNull() {
            assertNull(LanguageMapper.fromExtension(""))
        }
    }

    @Nested
    @DisplayName("fromFileName()")
    inner class FromFileNameTests {

        @Test
        @DisplayName("从完整文件名提取扩展名并映射")
        fun fileName_extractsExtensionAndMaps() {
            assertEquals("kotlin", LanguageMapper.fromFileName("Main.kt"))
            assertEquals("python", LanguageMapper.fromFileName("script.py"))
            assertEquals("java", LanguageMapper.fromFileName("Application.java"))
        }

        @Test
        @DisplayName("无扩展名的文件返回 null")
        fun noExtension_returnsNull() {
            assertNull(LanguageMapper.fromFileName("Makefile"))
        }

        @Test
        @DisplayName("多圆点文件名取最后一个扩展名")
        fun multipleDots_lastExtensionUsed() {
            assertEquals("typescript", LanguageMapper.fromFileName("config.test.ts"))
        }

        @Test
        @DisplayName("隐藏文件（以点开头）正确处理")
        fun hiddenFile_correctExtension() {
            // .gitignore → extension is "gitignore", likely null
            assertNull(LanguageMapper.fromFileName(".gitignore"))
        }
    }

    @Nested
    @DisplayName("toScopeName()")
    inner class ToScopeNameTests {

        @Test
        @DisplayName("已知语言 ID 返回对应 scope name")
        fun knownLanguageId_returnsScopeName() {
            val scope = LanguageMapper.toScopeName("kotlin")
            assertNotNull(scope)
            assertEquals("source.kotlin", scope)
        }

        @Test
        @DisplayName("null 语言 ID 返回 null")
        fun nullLanguageId_returnsNull() {
            assertNull(LanguageMapper.toScopeName(null))
        }

        @Test
        @DisplayName("未知语言 ID 返回 null")
        fun unknownLanguageId_returnsNull() {
            assertNull(LanguageMapper.toScopeName("unknown_lang"))
        }
    }
}
