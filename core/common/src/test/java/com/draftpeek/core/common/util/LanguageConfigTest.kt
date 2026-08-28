package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LanguageConfig")
class LanguageConfigTest {

    @Nested
    @DisplayName("getAllLanguages()")
    inner class GetAllLanguagesTest {

        @Test
        @DisplayName("returns non-empty list")
        fun returnsNonEmptyList() {
            val languages = LanguageConfig.getAllLanguages()
            assertTrue(languages.isNotEmpty())
        }

        @Test
        @DisplayName("includes common languages")
        fun includesCommonLanguages() {
            val displayNames = LanguageConfig.getDisplayNames()
            assertTrue(displayNames.contains("Kotlin"))
            assertTrue(displayNames.contains("Java"))
            assertTrue(displayNames.contains("Python"))
            assertTrue(displayNames.contains("Markdown"))
            assertTrue(displayNames.contains("Plain Text"))
        }

        @Test
        @DisplayName("each language has unique extension")
        fun uniqueExtensions() {
            val extensions = LanguageConfig.getAllLanguages().map { it.extension }
            assertEquals(extensions.size, extensions.toSet().size)
        }

        @Test
        @DisplayName("each language has non-blank fields")
        fun nonBlankFields() {
            for (lang in LanguageConfig.getAllLanguages()) {
                assertTrue(lang.displayName.isNotBlank(), "displayName blank for $lang")
                assertTrue(lang.extension.isNotBlank(), "extension blank for $lang")
                assertTrue(lang.mimeType.isNotBlank(), "mimeType blank for $lang")
            }
        }
    }

    @Nested
    @DisplayName("languageToExtension()")
    inner class LanguageToExtensionTest {

        @Test
        @DisplayName("Kotlin → kt")
        fun kotlinExtension() {
            assertEquals("kt", LanguageConfig.languageToExtension("Kotlin"))
        }

        @Test
        @DisplayName("Python → py")
        fun pythonExtension() {
            assertEquals("py", LanguageConfig.languageToExtension("Python"))
        }

        @Test
        @DisplayName("case-insensitive match")
        fun caseInsensitive() {
            assertEquals("kt", LanguageConfig.languageToExtension("kotlin"))
            assertEquals("kt", LanguageConfig.languageToExtension("KOTLIN"))
        }

        @Test
        @DisplayName("unknown language defaults to txt")
        fun unknownLanguage() {
            assertEquals("txt", LanguageConfig.languageToExtension("Unknown"))
        }

        @Test
        @DisplayName("empty string defaults to txt")
        fun emptyString() {
            assertEquals("txt", LanguageConfig.languageToExtension(""))
        }
    }

    @Nested
    @DisplayName("languageToMimeType()")
    inner class LanguageToMimeTypeTest {

        @Test
        @DisplayName("HTML → text/html")
        fun htmlMimeType() {
            assertEquals("text/html", LanguageConfig.languageToMimeType("HTML"))
        }

        @Test
        @DisplayName("JSON → application/json")
        fun jsonMimeType() {
            assertEquals("application/json", LanguageConfig.languageToMimeType("JSON"))
        }

        @Test
        @DisplayName("unknown language defaults to text/plain")
        fun unknownMimeType() {
            assertEquals("text/plain", LanguageConfig.languageToMimeType("Unknown"))
        }
    }

    @Nested
    @DisplayName("validateFilename()")
    inner class ValidateFilenameTest {

        @Test
        @DisplayName("null = valid filename")
        fun validFilename() {
            val result = LanguageConfig.validateFilename("myfile", "Kotlin")
            assertNull(result)
        }

        @Test
        @DisplayName("empty filename → EMPTY")
        fun emptyFilename() {
            assertEquals(FilenameValidationError.EMPTY, LanguageConfig.validateFilename("", "Kotlin"))
        }

        @Test
        @DisplayName("blank filename → EMPTY")
        fun blankFilename() {
            assertEquals(FilenameValidationError.EMPTY, LanguageConfig.validateFilename("   ", "Kotlin"))
        }

        @Test
        @DisplayName("filename with / → ILLEGAL_CHARS")
        fun slashInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("path/file", "Kotlin"))
        }

        @Test
        @DisplayName("filename with .. → ILLEGAL_CHARS")
        fun doubleDotInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("../etc", "Kotlin"))
        }

        @Test
        @DisplayName("filename with * → ILLEGAL_CHARS")
        fun asteriskInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file*", "Kotlin"))
        }

        @Test
        @DisplayName("filename with ? → ILLEGAL_CHARS")
        fun questionMarkInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file?", "Kotlin"))
        }

        @Test
        @DisplayName("filename with \" → ILLEGAL_CHARS")
        fun quoteInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file\"name", "Kotlin"))
        }

        @Test
        @DisplayName("filename with < → ILLEGAL_CHARS")
        fun lessThanInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file<name", "Kotlin"))
        }

        @Test
        @DisplayName("filename with > → ILLEGAL_CHARS")
        fun greaterThanInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file>name", "Kotlin"))
        }

        @Test
        @DisplayName("filename with | → ILLEGAL_CHARS")
        fun pipeInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file|name", "Kotlin"))
        }

        @Test
        @DisplayName("filename with : → ILLEGAL_CHARS")
        fun colonInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file:name", "Kotlin"))
        }

        @Test
        @DisplayName("filename with \\ → ILLEGAL_CHARS")
        fun backslashInFilename() {
            assertEquals(FilenameValidationError.ILLEGAL_CHARS, LanguageConfig.validateFilename("file\\name", "Kotlin"))
        }

        @Test
        @DisplayName("filename > 200 chars → TOO_LONG")
        fun tooLongFilename() {
            val longName = "a".repeat(201)
            assertEquals(FilenameValidationError.TOO_LONG, LanguageConfig.validateFilename(longName, "Kotlin"))
        }

        @Test
        @DisplayName("filename exactly 200 chars is valid")
        fun maxLengthFilename() {
            val name = "a".repeat(200)
            assertNull(LanguageConfig.validateFilename(name, "Kotlin"))
        }

        @Test
        @DisplayName("digit-start filename for C → DIGIT_START")
        fun digitStartForC() {
            assertEquals(FilenameValidationError.DIGIT_START, LanguageConfig.validateFilename("1file", "C"))
        }

        @Test
        @DisplayName("digit-start filename for Kotlin → DIGIT_START")
        fun digitStartForKotlin() {
            assertEquals(FilenameValidationError.DIGIT_START, LanguageConfig.validateFilename("1file", "Kotlin"))
        }

        @Test
        @DisplayName("digit-start filename for Python is valid (not in noDigitStart set)")
        fun digitStartForPython() {
            assertNull(LanguageConfig.validateFilename("1file", "Python"))
        }

        @Test
        @DisplayName("digit-start filename for Plain Text is valid")
        fun digitStartForPlainText() {
            assertNull(LanguageConfig.validateFilename("1file", "Plain Text"))
        }

        @Test
        @DisplayName("letter-start filename for C is valid")
        fun letterStartForC() {
            assertNull(LanguageConfig.validateFilename("myfile", "C"))
        }
    }
}
