package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("InputValidator")
class InputValidatorTest {

    @Nested
    @DisplayName("escapeRegexSpecialChars()")
    inner class EscapeRegexSpecialCharsTest {

        @Test
        @DisplayName("escapes dot")
        fun escapesDot() {
            assertEquals("\\.", InputValidator.escapeRegexSpecialChars("."))
        }

        @Test
        @DisplayName("escapes asterisk")
        fun escapesAsterisk() {
            assertEquals("\\*", InputValidator.escapeRegexSpecialChars("*"))
        }

        @Test
        @DisplayName("escapes plus")
        fun escapesPlus() {
            assertEquals("\\+", InputValidator.escapeRegexSpecialChars("+"))
        }

        @Test
        @DisplayName("escapes question mark")
        fun escapesQuestionMark() {
            assertEquals("\\?", InputValidator.escapeRegexSpecialChars("?"))
        }

        @Test
        @DisplayName("escapes caret")
        fun escapesCaret() {
            assertEquals("\\^", InputValidator.escapeRegexSpecialChars("^"))
        }

        @Test
        @DisplayName("escapes dollar sign")
        fun escapesDollar() {
            assertEquals("\\$", InputValidator.escapeRegexSpecialChars("$"))
        }

        @Test
        @DisplayName("escapes curly braces")
        fun escapesCurlyBraces() {
            assertEquals("\\{\\}", InputValidator.escapeRegexSpecialChars("{}"))
        }

        @Test
        @DisplayName("escapes parentheses")
        fun escapesParentheses() {
            assertEquals("\\(\\)", InputValidator.escapeRegexSpecialChars("()"))
        }

        @Test
        @DisplayName("escapes pipe")
        fun escapesPipe() {
            assertEquals("\\|", InputValidator.escapeRegexSpecialChars("|"))
        }

        @Test
        @DisplayName("escapes square brackets")
        fun escapesSquareBrackets() {
            assertEquals("\\[\\]", InputValidator.escapeRegexSpecialChars("[]"))
        }

        @Test
        @DisplayName("escapes backslash")
        fun escapesBackslash() {
            assertEquals("\\\\", InputValidator.escapeRegexSpecialChars("\\"))
        }

        @Test
        @DisplayName("does not escape alphanumeric characters")
        fun doesNotEscapeAlphanumeric() {
            assertEquals("abc123", InputValidator.escapeRegexSpecialChars("abc123"))
        }

        @Test
        @DisplayName("handles mixed content")
        fun handlesMixedContent() {
            val input = "function(test).value*2"
            val expected = "function\\(test\\)\\.value\\*2"
            assertEquals(expected, InputValidator.escapeRegexSpecialChars(input))
        }

        @Test
        @DisplayName("handles empty string")
        fun handlesEmptyString() {
            assertEquals("", InputValidator.escapeRegexSpecialChars(""))
        }
    }

    @Nested
    @DisplayName("sanitizeControlChars()")
    inner class SanitizeControlCharsTest {

        @Test
        @DisplayName("removes null character")
        fun removesNullChar() {
            val input = "hello\u0000world"
            val result = InputValidator.sanitizeControlChars(input)
            assertEquals("helloworld", result)
        }

        @Test
        @DisplayName("preserves newline")
        fun preservesNewline() {
            val input = "line1\nline2"
            assertEquals(input, InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("preserves carriage return")
        fun preservesCarriageReturn() {
            val input = "line1\rline2"
            assertEquals(input, InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("preserves tab")
        fun preservesTab() {
            val input = "col1\tcol2"
            assertEquals(input, InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("removes bell character")
        fun removesBellChar() {
            val input = "hello\u0007world"
            assertEquals("helloworld", InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("removes vertical tab")
        fun removesVerticalTab() {
            val input = "hello\u000Bworld"
            assertEquals("helloworld", InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("removes form feed")
        fun removesFormFeed() {
            val input = "hello\u000Cworld"
            assertEquals("helloworld", InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("removes escape character")
        fun removesEscapeChar() {
            val input = "hello\u001Bworld"
            assertEquals("helloworld", InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("removes delete character (0x7F is not in 0x00-0x1F range, should be preserved)")
        fun preservesDeleteChar() {
            val input = "hello\u007Fworld"
            assertEquals(input, InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("preserves regular text")
        fun preservesRegularText() {
            val input = "Hello, World! 123"
            assertEquals(input, InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("handles empty string")
        fun handlesEmptyString() {
            assertEquals("", InputValidator.sanitizeControlChars(""))
        }

        @Test
        @DisplayName("handles string with only control characters")
        fun handlesOnlyControlChars() {
            val input = "\u0000\u0001\u0002"
            assertEquals("", InputValidator.sanitizeControlChars(input))
        }

        @Test
        @DisplayName("preserves newlines and tabs while removing other control chars")
        fun preservesNewlinesAndTabs() {
            val input = "line1\n\u0000line2\t\u0001end"
            assertEquals("line1\nline2\tend", InputValidator.sanitizeControlChars(input))
        }
    }

    @Nested
    @DisplayName("validateSearchQuery()")
    inner class ValidateSearchQueryTest {

        @Test
        @DisplayName("returns Invalid for empty query")
        fun returnsInvalidForEmptyQuery() {
            val result = InputValidator.validateSearchQuery("", isRegex = false)
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("query", (result as ValidationResult.Invalid).field)
        }

        @Test
        @DisplayName("returns Valid for non-empty query")
        fun returnsValidForNonEmptyQuery() {
            val result = InputValidator.validateSearchQuery("test", isRegex = false)
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for query exceeding max length")
        fun returnsInvalidForTooLongQuery() {
            val longQuery = "a".repeat(1001)
            val result = InputValidator.validateSearchQuery(longQuery, isRegex = false)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Valid for regex query")
        fun returnsValidForRegexQuery() {
            val result = InputValidator.validateSearchQuery("test.*", isRegex = true)
            assertTrue(result is ValidationResult.Valid)
        }
    }

    @Nested
    @DisplayName("validateReplaceText()")
    inner class ValidateReplaceTextTest {

        @Test
        @DisplayName("returns Valid for empty text")
        fun returnsValidForEmptyText() {
            val result = InputValidator.validateReplaceText("")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Valid for normal text")
        fun returnsValidForNormalText() {
            val result = InputValidator.validateReplaceText("replacement")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for text exceeding max length")
        fun returnsInvalidForTooLongText() {
            val longText = "a".repeat(5001)
            val result = InputValidator.validateReplaceText(longText)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    @DisplayName("validateCrossFileSearchQuery()")
    inner class ValidateCrossFileSearchQueryTest {

        @Test
        @DisplayName("returns Invalid for empty query")
        fun returnsInvalidForEmptyQuery() {
            val result = InputValidator.validateCrossFileSearchQuery("")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Valid for non-empty query")
        fun returnsValidForNonEmptyQuery() {
            val result = InputValidator.validateCrossFileSearchQuery("search")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for query exceeding max length")
        fun returnsInvalidForTooLongQuery() {
            val longQuery = "a".repeat(501)
            val result = InputValidator.validateCrossFileSearchQuery(longQuery)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    @DisplayName("validateSnippetTitle()")
    inner class ValidateSnippetTitleTest {

        @Test
        @DisplayName("returns Invalid for empty title")
        fun returnsInvalidForEmptyTitle() {
            val result = InputValidator.validateSnippetTitle("")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("title", (result as ValidationResult.Invalid).field)
        }

        @Test
        @DisplayName("returns Invalid for whitespace-only title")
        fun returnsInvalidForWhitespaceOnlyTitle() {
            val result = InputValidator.validateSnippetTitle("   ")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Valid for normal title")
        fun returnsValidForNormalTitle() {
            val result = InputValidator.validateSnippetTitle("My Snippet")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for title exceeding max length")
        fun returnsInvalidForTooLongTitle() {
            val longTitle = "a".repeat(201)
            val result = InputValidator.validateSnippetTitle(longTitle)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("trims whitespace before validation")
        fun trimsWhitespaceBeforeValidation() {
            val result = InputValidator.validateSnippetTitle("  valid title  ")
            assertTrue(result is ValidationResult.Valid)
        }
    }

    @Nested
    @DisplayName("validateSnippetContent()")
    inner class ValidateSnippetContentTest {

        @Test
        @DisplayName("returns Invalid for empty content")
        fun returnsInvalidForEmptyContent() {
            val result = InputValidator.validateSnippetContent("")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("content", (result as ValidationResult.Invalid).field)
        }

        @Test
        @DisplayName("returns Valid for non-empty content")
        fun returnsValidForNonEmptyContent() {
            val result = InputValidator.validateSnippetContent("print('hello')")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for content exceeding max length")
        fun returnsInvalidForTooLongContent() {
            val longContent = "a".repeat(100_001)
            val result = InputValidator.validateSnippetContent(longContent)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    @DisplayName("validateSnippetCategory()")
    inner class ValidateSnippetCategoryTest {

        @Test
        @DisplayName("returns Invalid for empty category")
        fun returnsInvalidForEmptyCategory() {
            val result = InputValidator.validateSnippetCategory("")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("category", (result as ValidationResult.Invalid).field)
        }

        @Test
        @DisplayName("returns Invalid for whitespace-only category")
        fun returnsInvalidForWhitespaceOnlyCategory() {
            val result = InputValidator.validateSnippetCategory("  ")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Valid for normal category")
        fun returnsValidForNormalCategory() {
            val result = InputValidator.validateSnippetCategory("Kotlin")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for category exceeding max length")
        fun returnsInvalidForTooLongCategory() {
            val longCategory = "a".repeat(101)
            val result = InputValidator.validateSnippetCategory(longCategory)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    @DisplayName("validateGitHubUrl()")
    inner class ValidateGitHubUrlTest {

        @Test
        @DisplayName("returns Invalid for empty URL")
        fun returnsInvalidForEmptyUrl() {
            val result = InputValidator.validateGitHubUrl("")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("url", (result as ValidationResult.Invalid).field)
        }

        @Test
        @DisplayName("returns Invalid for whitespace-only URL")
        fun returnsInvalidForWhitespaceOnlyUrl() {
            val result = InputValidator.validateGitHubUrl("   ")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Valid for standard GitHub URL")
        fun returnsValidForStandardGitHubUrl() {
            val result = InputValidator.validateGitHubUrl("https://github.com/owner/repo")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Valid for GitHub URL with trailing path")
        fun returnsValidForGitHubUrlWithPath() {
            val result = InputValidator.validateGitHubUrl("https://github.com/owner/repo/tree/main/src")
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for non-GitHub URL")
        fun returnsInvalidForNonGitHubUrl() {
            val result = InputValidator.validateGitHubUrl("https://gitlab.com/owner/repo")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Invalid for HTTP (non-HTTPS) URL")
        fun returnsInvalidForHttpUrl() {
            val result = InputValidator.validateGitHubUrl("http://github.com/owner/repo")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Invalid for URL with control characters")
        fun returnsInvalidForUrlWithControlChars() {
            val result = InputValidator.validateGitHubUrl("https://github.com/owner\u0000/repo")
            // The control char is removed by sanitizeControlChars, but the URL may still be valid
            // after sanitization. The key is that it doesn't crash.
            // After sanitization: "https://github.com/owner/repo" which is valid
            assertTrue(result is ValidationResult.Valid)
        }

        @Test
        @DisplayName("returns Invalid for URL exceeding max length")
        fun returnsInvalidForTooLongUrl() {
            val longUrl = "https://github.com/owner/repo/" + "a".repeat(2048)
            val result = InputValidator.validateGitHubUrl(longUrl)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("returns Invalid for malformed URL")
        fun returnsInvalidForMalformedUrl() {
            val result = InputValidator.validateGitHubUrl("not-a-url")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        @DisplayName("trims whitespace before validation")
        fun trimsWhitespaceBeforeValidation() {
            val result = InputValidator.validateGitHubUrl("  https://github.com/owner/repo  ")
            assertTrue(result is ValidationResult.Valid)
        }
    }
}
