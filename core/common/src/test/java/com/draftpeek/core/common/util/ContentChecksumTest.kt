package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ContentChecksum")
class ContentChecksumTest {

    @Test
    @DisplayName("same content produces same checksum")
    fun sameContent_producesSameChecksum() {
        val content = "Hello, World!"
        val checksum1 = ContentChecksum.crc32(content)
        val checksum2 = ContentChecksum.crc32(content)
        assertEquals(checksum1, checksum2)
    }

    @Test
    @DisplayName("different content produces different checksum")
    fun differentContent_producesDifferentChecksum() {
        val content1 = "Hello, World!"
        val content2 = "Hello, DraftPeek!"
        val checksum1 = ContentChecksum.crc32(content1)
        val checksum2 = ContentChecksum.crc32(content2)
        assertTrue(checksum1 != checksum2)
    }

    @Test
    @DisplayName("empty string returns consistent checksum")
    fun emptyString_returnsConsistentChecksum() {
        val checksum1 = ContentChecksum.crc32("")
        val checksum2 = ContentChecksum.crc32("")
        assertEquals(checksum1, checksum2)
    }

    @Test
    @DisplayName("hasChanged returns true for different content")
    fun hasChanged_returnsTrue_forDifferentContent() {
        val original = "original content"
        val baseline = ContentChecksum.crc32(original)
        val modified = "modified content"
        assertTrue(ContentChecksum.hasChanged(modified, baseline))
    }

    @Test
    @DisplayName("hasChanged returns false for same content")
    fun hasChanged_returnsFalse_forSameContent() {
        val content = "unchanged content"
        val baseline = ContentChecksum.crc32(content)
        assertFalse(ContentChecksum.hasChanged(content, baseline))
    }

    @Test
    @DisplayName("hasChanged returns false when undo restores original")
    fun hasChanged_returnsFalse_whenUndoRestoresOriginal() {
        val original = "line one\nline two\nline three"
        val baseline = ContentChecksum.crc32(original)

        val modified = "line one\nline CHANGED\nline three"
        assertTrue(ContentChecksum.hasChanged(modified, baseline))

        assertFalse(ContentChecksum.hasChanged(original, baseline))
    }

    @Test
    @DisplayName("unicode content produces consistent checksum")
    fun unicodeContent_producesConsistentChecksum() {
        val content = "你好世界 🌍 こんにちは 한국어"
        val checksum1 = ContentChecksum.crc32(content)
        val checksum2 = ContentChecksum.crc32(content)
        assertEquals(checksum1, checksum2)
    }

    @Test
    @DisplayName("unicode content differs from ascii content")
    fun unicodeContent_differsFromAsciiContent() {
        val ascii = "Hello World"
        val unicode = "你好世界"
        assertTrue(ContentChecksum.crc32(ascii) != ContentChecksum.crc32(unicode))
    }

    @Test
    @DisplayName("large content produces consistent checksum")
    fun largeContent_producesConsistentChecksum() {
        val baseLine = "The quick brown fox jumps over the lazy dog.\n"
        val content = baseLine.repeat(20_000)
        val checksum1 = ContentChecksum.crc32(content)
        val checksum2 = ContentChecksum.crc32(content)
        assertEquals(checksum1, checksum2)
    }

    @Test
    @DisplayName("single character change detected by checksum")
    fun singleCharacterChange_detectedByChecksum() {
        val original = "aaaaaaaaaa"
        val modified = "aaaaaaaaba"
        val baseline = ContentChecksum.crc32(original)
        assertTrue(ContentChecksum.hasChanged(modified, baseline))
    }

    @Test
    @DisplayName("whitespace difference detected by checksum")
    fun whitespaceDifference_detectedByChecksum() {
        val original = "hello world"
        val modified = "hello  world"
        val baseline = ContentChecksum.crc32(original)
        assertTrue(ContentChecksum.hasChanged(modified, baseline))
    }
}
