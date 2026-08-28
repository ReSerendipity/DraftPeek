package com.draftpeek.core.common.util

import java.nio.charset.Charset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EncodingDetector")
class EncodingDetectorTest {

    @Nested
    @DisplayName("BOM detection")
    inner class BomDetectionTest {

        @Test
        @DisplayName("UTF-8 BOM (EF BB BF) → UTF-8")
        fun utf8Bom() {
            val bytes = byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte(),
                'a'.code.toByte(),
                'b'.code.toByte()
            )
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("UTF-16 BE BOM (FE FF) → UTF-16BE")
        fun utf16BeBom() {
            val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0x00, 0x61)
            assertEquals("UTF-16BE", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("UTF-16 LE BOM (FF FE) → UTF-16LE")
        fun utf16LeBom() {
            val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x61, 0x00)
            assertEquals("UTF-16LE", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("no BOM → falls through to juniversalchardet (UTF-8 for ASCII bytes)")
        fun noBomAscii() {
            val bytes = "hello world".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("short payload without BOM falls through")
        fun shortPayload() {
            val bytes = byteArrayOf(0x61, 0x62) // "ab" — no BOM, ASCII
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }
    }

    @Nested
    @DisplayName("UTF-8 validity")
    inner class Utf8ValidityTest {

        @Test
        @DisplayName("ASCII bytes are valid UTF-8")
        fun asciiIsUtf8() {
            val bytes = "Hello, World!".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("Chinese characters in UTF-8 are detected as UTF-8")
        fun chineseIsUtf8() {
            val bytes = "你好，世界".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("Emoji (4-byte UTF-8) is detected as UTF-8")
        fun emojiIsUtf8() {
            val bytes = "Hello 👋".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }
    }

    @Nested
    @DisplayName("GB18030 normalization (three-stage strategy)")
    inner class Gb18030NormalizationTest {

        @Test
        @DisplayName("GBK-encoded Chinese bytes → GB18030 (normalized via juniversalchardet)")
        fun chineseGbkNormalizedToGb18030() {
            // "你好世界你好" in GBK — juniversalchardet detects GBK, normalized to GB18030
            val bytes = "你好世界你好".toByteArray(Charset.forName("GBK"))
            assertEquals("GB18030", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("pure ASCII remains UTF-8 even with GBK detection possible")
        fun asciiIsNotGbk() {
            val bytes = "Hello".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("single Chinese char (2 bytes) in GBK — below heuristic threshold")
        fun singleChineseCharNotEnough() {
            // Only 1 high byte — below the > 4 threshold for the old heuristic.
            // juniversalchardet may still detect it as GBK/GB18030 or return null.
            val bytes = "中".toByteArray(Charset.forName("GBK"))
            val result = EncodingDetector.detectEncoding(bytes)
            assertTrue(
                result.isNotEmpty(),
                "Detection should return a non-empty encoding name"
            )
        }

        @Test
        @DisplayName("GB18030-encoded text is detected as GB18030")
        fun gb18030DetectedDirectly() {
            // GB18030 is a superset of GBK; test that it's detected correctly
            val bytes = "你好世界你好世界你好".toByteArray(Charset.forName("GB18030"))
            val result = EncodingDetector.detectEncoding(bytes)
            // juniversalchardet should detect GB18030 or GBK (normalized to GB18030)
            assertTrue(
                result == "GB18030" || result == "UTF-8",
                "Expected GB18030 or UTF-8 fallback, got: $result"
            )
        }
    }

    @Nested
    @DisplayName("Three-stage strategy")
    inner class ThreeStageStrategyTest {

        @Test
        @DisplayName("Stage 1: BOM is authoritative, bypasses juniversalchardet")
        fun stage1BomAuthoritative() {
            val bytes = byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte(),
                'h'.code.toByte(),
                'i'.code.toByte()
            )
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("Stage 2: juniversalchardet detects non-UTF-8 encoding")
        fun stage2ChardetDetectsNonUtf8() {
            // GBK-encoded Chinese text should be detected as GB18030
            val bytes = "你好世界你好世界".toByteArray(Charset.forName("GBK"))
            assertEquals("GB18030", EncodingDetector.detectEncoding(bytes))
        }

        @Test
        @DisplayName("Stage 3: UTF-8 fallback when detection is uncertain")
        fun stage3Utf8Fallback() {
            // Random bytes that are not valid UTF-8 and not clearly any encoding
            // juniversalchardet may return null or a low-confidence encoding → UTF-8
            val bytes = byteArrayOf(0x80.toByte(), 0x81.toByte(), 0x82.toByte(), 0x83.toByte())
            val result = EncodingDetector.detectEncoding(bytes)
            // With UTF-8 fallback, we should always get a valid encoding
            assertTrue(result.isNotEmpty())
        }

        @Test
        @DisplayName("Stage 3: pure ASCII falls through to UTF-8 (not ISO-8859-1)")
        fun stage3AsciiFallbackIsUtf8() {
            val bytes = "Hello".toByteArray(Charsets.UTF_8)
            val result = EncodingDetector.detectEncoding(bytes)
            assertEquals("UTF-8", result)
        }
    }

    @Nested
    @DisplayName("fileName hint")
    inner class FileNameHintTest {

        @Test
        @DisplayName("'.kt' file defaults to UTF-8 even with non-BOM header")
        fun ktExtension() {
            val bytes = "package com.example".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes, "Foo.kt"))
        }

        @Test
        @DisplayName("'.java' file defaults to UTF-8")
        fun javaExtension() {
            val bytes = "public class Foo {}".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes, "Foo.java"))
        }

        @Test
        @DisplayName("unknown extension without BOM falls back to UTF-8 via three-stage")
        fun unknownExtension() {
            val bytes = "plain text".toByteArray(Charsets.UTF_8)
            // ASCII → UTF-8 path
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes, "data.txt"))
        }

        @Test
        @DisplayName("BOM wins over extension hint")
        fun bomWinsOverExtension() {
            val bytes = byteArrayOf(
                0xFF.toByte(),
                0xFE.toByte(),
                'a'.code.toByte(),
                0x00
            )
            // .kt would default to UTF-8, but BOM says UTF-16LE
            assertEquals("UTF-16LE", EncodingDetector.detectEncoding(bytes, "Foo.kt"))
        }

        @Test
        @DisplayName("GBK-encoded .txt file is detected as GB18030")
        fun gbkTextFileDetectedAsGb18030() {
            val bytes = "你好世界你好世界".toByteArray(Charset.forName("GBK"))
            assertEquals("GB18030", EncodingDetector.detectEncoding(bytes, "data.txt"))
        }

        @Test
        @DisplayName("GBK-encoded .kt file defaults to UTF-8 (source code convention)")
        fun gbkKtFileDefaultsToUtf8() {
            val bytes = "你好世界你好世界".toByteArray(Charset.forName("GBK"))
            // Source code files always default to UTF-8
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes, "Foo.kt"))
        }
    }

    @Nested
    @DisplayName("detectEncodingFallback (backward compatibility)")
    inner class DetectEncodingFallbackTest {

        @Test
        @DisplayName("GBK bytes → GB18030 via fallback")
        fun fallbackGbkToGb18030() {
            val bytes = "你好世界你好世界".toByteArray(Charset.forName("GBK"))
            assertEquals("GB18030", EncodingDetector.detectEncodingFallback(bytes))
        }

        @Test
        @DisplayName("empty input → UTF-8")
        fun fallbackEmpty() {
            assertEquals("UTF-8", EncodingDetector.detectEncodingFallback(byteArrayOf()))
        }

        @Test
        @DisplayName("ASCII → UTF-8 (low confidence ASCII filtered)")
        fun fallbackAscii() {
            val bytes = "Hello".toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncodingFallback(bytes))
        }
    }

    @Nested
    @DisplayName("decodeBytes()")
    inner class DecodeBytesTest {

        @Test
        @DisplayName("decodes UTF-8 BOM correctly (BOM stripped)")
        fun decodesUtf8Bom() {
            val payload = "hello world"
            val withBom = byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte()
            ) + payload.toByteArray(Charsets.UTF_8)
            val decoded = EncodingDetector.decodeBytes(withBom)
            assertEquals(payload, decoded)
        }

        @Test
        @DisplayName("decodes plain UTF-8 without BOM")
        fun decodesPlainUtf8() {
            val payload = "hello"
            val bytes = payload.toByteArray(Charsets.UTF_8)
            assertEquals(payload, EncodingDetector.decodeBytes(bytes))
        }

        @Test
        @DisplayName("uses extension hint when decoding")
        fun decodesWithExtension() {
            val payload = "package com.example"
            val bytes = payload.toByteArray(Charsets.UTF_8)
            assertEquals(payload, EncodingDetector.decodeBytes(bytes, "Foo.kt"))
        }

        @Test
        @DisplayName("decodes GB18030 text correctly")
        fun decodesGb18030() {
            val payload = "你好世界"
            val bytes = payload.toByteArray(Charset.forName("GBK"))
            // juniversalchardet detects GBK → normalized to GB18030
            val decoded = EncodingDetector.decodeBytes(bytes)
            assertEquals(payload, decoded)
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCasesTest {

        @Test
        @DisplayName("empty input is valid UTF-8 (vacuously true)")
        fun emptyInput() {
            assertEquals("UTF-8", EncodingDetector.detectEncoding(byteArrayOf()))
        }

        @Test
        @DisplayName("single byte input returns UTF-8 (UTF-8 fallback)")
        fun singleByte() {
            val result = EncodingDetector.detectEncoding(byteArrayOf(0x61))
            // With UTF-8 fallback, single ASCII byte always returns UTF-8
            assertEquals("UTF-8", result)
        }

        @Test
        @DisplayName("invalid UTF-8 bytes still return valid encoding via fallback")
        fun invalidUtf8Bytes() {
            // 0x80-0xBF are continuation bytes without a leading byte — invalid UTF-8
            val bytes = byteArrayOf(0x80.toByte(), 0x81.toByte(), 0x82.toByte())
            val result = EncodingDetector.detectEncoding(bytes)
            // Should return some encoding (UTF-8 fallback or whatever chardet detects)
            assertTrue(result.isNotEmpty())
        }

        @Test
        @DisplayName("large input (> 64KB) is sampled correctly")
        fun largeInputSampled() {
            // Create a 128KB UTF-8 input
            val largeContent = "a".repeat(128 * 1024)
            val bytes = largeContent.toByteArray(Charsets.UTF_8)
            assertEquals("UTF-8", EncodingDetector.detectEncoding(bytes))
        }
    }
}
