package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("OfficeDocumentParser")
class OfficeDocumentParserTest {

    @Nested
    @DisplayName("parseWord()")
    inner class ParseWordTest {

        @Test
        @DisplayName("empty input returns an error HTML, does not throw")
        fun emptyInput() {
            val html = OfficeDocumentParser.parseWord(ByteArrayInputStream(ByteArray(0)))

            assertNotNull(html)
            assertTrue(html.contains("<"), "Expected HTML output, got: $html")
        }

        @Test
        @DisplayName("input shorter than OLE2 header (8 bytes) returns error HTML")
        fun tooShort() {
            // 4 random bytes — too short to be recognized as OLE2 or ZIP
            val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
            val html = OfficeDocumentParser.parseWord(ByteArrayInputStream(bytes))

            assertNotNull(html)
            assertTrue(html.contains("<"), "Expected HTML output, got: $html")
        }

        @Test
        @DisplayName("non-Office binary (random bytes) returns error HTML, does not throw")
        fun randomBytes() {
            // 16 bytes that match neither OLE2 (D0 CF 11 E0 ...) nor ZIP (PK) headers
            val bytes = byteArrayOf(
                0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte(),
                0x99.toByte(), 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
                0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(), 0x00,
            )
            val html = OfficeDocumentParser.parseWord(ByteArrayInputStream(bytes))

            assertNotNull(html)
            assertTrue(html.contains("<"), "Expected HTML output, got: $html")
        }

        @Test
        @DisplayName("does not propagate exceptions on garbage input")
        fun handlesExceptionsGracefully() {
            // Try a few pathological inputs that previously could crash
            // the parser chain (e.g. truncated OOXML).
            val truncated = byteArrayOf(
                0x50, 0x4B, // ZIP signature "PK"
                0x03, 0x04, // local file header signature
            )
            val html = OfficeDocumentParser.parseWord(ByteArrayInputStream(truncated))

            assertNotNull(html)
            // Truncated OOXML should be reported as an error, not crash.
            assertTrue(html.contains("<"), "Expected HTML output, got: $html")
        }
    }

    @Nested
    @DisplayName("parseExcel()")
    inner class ParseExcelTest {

        @Test
        @DisplayName("empty input returns error HTML, does not throw")
        fun emptyInput() {
            val html = OfficeDocumentParser.parseExcel(ByteArrayInputStream(ByteArray(0)))

            assertNotNull(html)
            assertTrue(html.contains("<"))
        }

        @Test
        @DisplayName("non-Excel binary returns error HTML, does not throw")
        fun randomBytes() {
            val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
            val html = OfficeDocumentParser.parseExcel(ByteArrayInputStream(bytes))

            assertNotNull(html)
            assertTrue(html.contains("<"))
        }
    }

    @Nested
    @DisplayName("parsePowerPoint()")
    inner class ParsePowerPointTest {

        @Test
        @DisplayName("empty input returns error HTML, does not throw")
        fun emptyInput() {
            val html = OfficeDocumentParser.parsePowerPoint(ByteArrayInputStream(ByteArray(0)))

            assertNotNull(html)
            assertTrue(html.contains("<"))
        }

        @Test
        @DisplayName("non-PowerPoint binary returns error HTML, does not throw")
        fun randomBytes() {
            val bytes = byteArrayOf(0x12, 0x34, 0x56, 0x78)
            val html = OfficeDocumentParser.parsePowerPoint(ByteArrayInputStream(bytes))

            assertNotNull(html)
            assertTrue(html.contains("<"))
        }
    }
}
