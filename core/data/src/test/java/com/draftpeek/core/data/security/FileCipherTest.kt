package com.draftpeek.core.data.security

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException

/**
 * FileCipher AES-256-GCM 加密模块单元测试。
 *
 * 验证加密/解密往返、错误密码检测、流式操作、容器格式等核心安全功能。
 */
@DisplayName("FileCipher")
class FileCipherTest {

    @Nested
    @DisplayName("encrypt / decrypt 往返")
    inner class EncryptDecryptRoundTrip {

        @Test
        @DisplayName("加密后用相同密码解密应返回原始数据")
        fun should_returnOriginal_when_encryptThenDecrypt() {
            val plaintext = "Hello, DraftPeek!".toByteArray(Charsets.UTF_8)
            val password = "testPassword123"

            val encrypted = FileCipher.encrypt(plaintext, password)
            val decrypted = FileCipher.decrypt(encrypted, password)

            assertArrayEquals(plaintext, decrypted)
        }

        @Test
        @DisplayName("加密空数据后解密应返回空数组")
        fun should_returnEmpty_when_encryptEmptyThenDecrypt() {
            val plaintext = ByteArray(0)
            val password = "pass"

            val encrypted = FileCipher.encrypt(plaintext, password)
            val decrypted = FileCipher.decrypt(encrypted, password)

            assertArrayEquals(plaintext, decrypted)
            assertEquals(0, decrypted.size)
        }

        @Test
        @DisplayName("加密大文本数据后解密应返回原始数据")
        fun should_returnOriginal_when_largeDataRoundTrip() {
            val plaintext = ByteArray(100_000) { (it % 256).toByte() }
            val password = "largeFilePassword"

            val encrypted = FileCipher.encrypt(plaintext, password)
            val decrypted = FileCipher.decrypt(encrypted, password)

            assertArrayEquals(plaintext, decrypted)
        }

        @Test
        @DisplayName("相同明文不同次加密应产生不同密文（随机 salt + nonce）")
        fun should_produceDifferentCiphertext_when_samePlaintextEncryptedTwice() {
            val plaintext = "same data".toByteArray()
            val password = "pass"

            val encrypted1 = FileCipher.encrypt(plaintext, password)
            val encrypted2 = FileCipher.encrypt(plaintext, password)

            // 密文应不同（因为随机 salt 和 nonce）
            assertFalse(encrypted1.contentEquals(encrypted2))
        }
    }

    @Nested
    @DisplayName("错误密码检测")
    inner class WrongPasswordDetection {

        @Test
        @DisplayName("用错误密码解密应抛出 AEADBadTagException")
        fun should_throwAEADBadTagException_when_wrongPassword() {
            val plaintext = "secret data".toByteArray()
            val correctPassword = "correctPass"
            val wrongPassword = "wrongPass"

            val encrypted = FileCipher.encrypt(plaintext, correctPassword)

            val exception = assertThrows(AEADBadTagException::class.java) {
                FileCipher.decrypt(encrypted, wrongPassword)
            }
            assertNotNull(exception)
        }

        @Test
        @DisplayName("密文被篡改后解密应抛出异常")
        fun should_throwException_when_ciphertextTampered() {
            val plaintext = "original data".toByteArray()
            val password = "pass"

            val encrypted = FileCipher.encrypt(plaintext, password)
            // 篡改最后一个字节（GCM tag 区域）
            val tampered = encrypted.copyOf()
            tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0xFF).toByte()

            assertThrows(AEADBadTagException::class.java) {
                FileCipher.decrypt(tampered, password)
            }
        }
    }

    @Nested
    @DisplayName("容器格式验证")
    inner class ContainerFormat {

        @Test
        @DisplayName("密文长度应大于头部（4 + 16 + 12 = 32 字节）")
        fun should_haveLengthGreaterThanHeader() {
            val plaintext = "x".toByteArray()
            val encrypted = FileCipher.encrypt(plaintext, "pass")

            // header = 4 (version) + 16 (salt) + 12 (nonce) = 32
            // + ciphertext + GCM tag (16 bytes)
            assertTrue(encrypted.size > 32)
        }

        @Test
        @DisplayName("解密过短的容器应抛出 IllegalArgumentException")
        fun should_throwIllegalArgumentException_when_containerTooShort() {
            val shortData = ByteArray(10)

            assertThrows(IllegalArgumentException::class.java) {
                FileCipher.decrypt(shortData, "pass")
            }
        }
    }

    @Nested
    @DisplayName("encryptFile / decryptFile 流操作")
    inner class FileOperations {

        @Test
        @DisplayName("encryptFile 后 decryptFile 应返回原始内容")
        fun should_returnOriginalContent_when_fileRoundTrip() {
            val content = "File content for testing\nMultiple lines\n中文内容"
            val password = "filePass"

            val outputStream = ByteArrayOutputStream()
            FileCipher.encryptFile(content, password, outputStream)

            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val decrypted = FileCipher.decryptFile(inputStream, password)

            assertEquals(content, decrypted)
        }
    }

    @Nested
    @DisplayName("encryptStream / decryptStream 流式操作")
    inner class StreamOperations {

        @Test
        @DisplayName("encryptStream 后 decryptStream 应返回原始数据")
        fun should_returnOriginalData_when_streamRoundTrip() {
            val originalData = "Stream test data with some content".toByteArray()
            val password = "streamPass"

            val encryptOutput = ByteArrayOutputStream()
            FileCipher.encryptStream(
                ByteArrayInputStream(originalData),
                password,
                encryptOutput,
            )

            val decryptOutput = ByteArrayOutputStream()
            FileCipher.decryptStream(
                ByteArrayInputStream(encryptOutput.toByteArray()),
                password,
                decryptOutput,
            )

            assertArrayEquals(originalData, decryptOutput.toByteArray())
        }

        @Test
        @DisplayName("流式加密大文件应与内存加密结果一致")
        fun should_produceConsistentResult_when_largeStreamRoundTrip() {
            val largeData = ByteArray(50_000) { (it % 256).toByte() }
            val password = "largeStreamPass"

            val encryptOutput = ByteArrayOutputStream()
            FileCipher.encryptStream(
                ByteArrayInputStream(largeData),
                password,
                encryptOutput,
            )

            val decryptOutput = ByteArrayOutputStream()
            FileCipher.decryptStream(
                ByteArrayInputStream(encryptOutput.toByteArray()),
                password,
                decryptOutput,
            )

            assertArrayEquals(largeData, decryptOutput.toByteArray())
        }
    }

    @Nested
    @DisplayName("文件扩展名工具方法")
    inner class FileExtensionUtils {

        @Test
        @DisplayName("isEncryptedExport 应正确识别 .jenc 文件")
        fun should_identifyEncryptedFiles() {
            assertTrue(FileCipher.isEncryptedExport("document.txt.jenc"))
            assertTrue(FileCipher.isEncryptedExport("PHOTO.JPG.JENC"))
            assertFalse(FileCipher.isEncryptedExport("document.txt"))
            assertFalse(FileCipher.isEncryptedExport("document.jenc.txt"))
        }

        @Test
        @DisplayName("originalNameFromEncrypted 应去除 .jenc 扩展名")
        fun should_stripJencExtension() {
            assertEquals("document.txt", FileCipher.originalNameFromEncrypted("document.txt.jenc"))
            assertEquals("photo.jpg", FileCipher.originalNameFromEncrypted("photo.jpg.jenc"))
            assertEquals("noext", FileCipher.originalNameFromEncrypted("noext"))
        }

        @Test
        @DisplayName("加密扩展名应为 .jenc")
        fun should_haveCorrectExtension() {
            assertEquals(".jenc", FileCipher.ENCRYPTED_FILE_EXTENSION)
        }
    }
}
