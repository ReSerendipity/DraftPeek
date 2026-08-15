/**
 * 文件级 AES-256-GCM 加密模块。
 *
 * 提供文件导出加密功能，使用用户提供的密码进行加密/解密。
 * 加密容器自包含 salt 和 nonce，无需额外元数据即可在另一台设备上解密。
 *
 * ## 加密策略
 * - 加密算法：AES-256-GCM（认证加密，防篡改）
 * - 密钥派生：PBKDF2WithHmacSHA256（100,000 次迭代）
 * - Salt 长度：16 字节（随机生成）
 * - Nonce/IV 长度：12 字节（GCM 标准）
 * - GCM Tag 长度：128 位
 * - 密钥缓存：LRU 缓存最多 8 个派生密钥，避免重复计算
 *
 * ## 容器格式
 * ```
 * [version(4B)][salt(16B)][nonce(12B)][ciphertext + GCM tag]
 * ```
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.security

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.Collections
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 文件级 AES-256-GCM 加密工具。
 *
 * 用于将文件内容以加密容器格式导出。容器布局：
 * ```
 * [version(4)][salt(16)][nonce(12)][ciphertext + GCM tag]
 * ```
 *
 * 密钥不存储在 Android Keystore 中——因为这是基于密码的文件导出加密，
 * 用户需要在另一台设备上用相同密码解密。密钥通过 PBKDF2WithHmacSHA256
 * 从用户提供的密码派生。
 */
object FileCipher {

    private const val KEY_CACHE_SIZE = 8

    // SECURITY VULN-008: keyCache 使用 Collections.synchronizedMap 包装，
    // 确保所有操作（get/put/removeEldestEntry）在并发访问时线程安全。
    // LinkedHashMap 的 accessOrder=true 模式下 get() 会修改内部结构，
    // 必须同步访问。synchronizedMap 在包装器对象上加锁，
    // removeEldestEntry 在 put() 内部调用时也受同一锁保护。
    private val keyCache: MutableMap<String, SecretKeySpec> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, SecretKeySpec>(KEY_CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SecretKeySpec>?): Boolean {
                    return size > KEY_CACHE_SIZE
                }
            }
        )

    /** P1-4: File extension convention for encrypted export containers. */
    const val ENCRYPTED_FILE_EXTENSION = ".jenc"

    /** P1-4: Check if a file name represents an encrypted export. */
    fun isEncryptedExport(fileName: String): Boolean = fileName.endsWith(ENCRYPTED_FILE_EXTENSION, ignoreCase = true)

    /** P1-4: Derive the original file name from an encrypted export name.
     *  e.g. "document.txt.jenc" → "document.txt" */
    fun originalNameFromEncrypted(fileName: String): String {
        return if (isEncryptedExport(fileName)) {
            fileName.dropLast(ENCRYPTED_FILE_EXTENSION.length)
        } else {
            fileName
        }
    }

    private const val VERSION = 1
    private const val SALT_LENGTH = 16
    private const val NONCE_LENGTH = 12 // GCM standard nonce
    private const val KEY_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 100000
    private const val GCM_TAG_LENGTH = 128

    private const val HEADER_LENGTH = 4 + SALT_LENGTH + NONCE_LENGTH
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

    // OPTIMIZE: [C-03] - 流式加密缓冲区大小，平衡内存占用与系统调用次数
    private const val STREAM_BUFFER_SIZE = 16 * 1024 // 16 KB

    /**
     * 加密明文数据。
     *
     * 生成随机 salt 和 nonce，从 [password] 派生 AES-256-GCM 密钥并加密 [plaintext]。
     *
     * @param plaintext 待加密的明文字节
     * @param password 用户提供的密码
     * @return 加密容器：[version][salt][nonce][ciphertext + GCM tag]
     */
    fun encrypt(plaintext: ByteArray, password: String): ByteArray {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val nonce = ByteArray(NONCE_LENGTH).also { secureRandom.nextBytes(it) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        val ciphertext = cipher.doFinal(plaintext)

        // version(4) + salt(16) + nonce(12) + ciphertext
        return ByteArray(HEADER_LENGTH + ciphertext.size).also { out ->
            writeVersion(out, 0)
            System.arraycopy(salt, 0, out, 4, SALT_LENGTH)
            System.arraycopy(nonce, 0, out, 4 + SALT_LENGTH, NONCE_LENGTH)
            System.arraycopy(ciphertext, 0, out, HEADER_LENGTH, ciphertext.size)
        }
    }

    /**
     * 解密容器数据。
     *
     * 从容器头部读取 version、salt、nonce，用 [password] 派生密钥并解密。
     * 若密码错误或数据被篡改，将抛出 [javax.crypto.AEADBadTagException]。
     *
     * @param ciphertext 加密容器字节
     * @param password 用户提供的密码
     * @return 解密后的明文字节
     */
    fun decrypt(ciphertext: ByteArray, password: String): ByteArray {
        require(ciphertext.size > HEADER_LENGTH) { "Invalid container: too short" }
        val version = readVersion(ciphertext, 0)
        require(version == VERSION) { "Unsupported container version: $version" }

        val salt = ciphertext.copyOfRange(4, 4 + SALT_LENGTH)
        val nonce = ciphertext.copyOfRange(4 + SALT_LENGTH, HEADER_LENGTH)
        val encrypted = ciphertext.copyOfRange(HEADER_LENGTH, ciphertext.size)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        return cipher.doFinal(encrypted)
    }

    /**
     * 加密字符串内容并写入输出流。
     *
     * @param content 待加密的文本内容
     * @param password 用户提供的密码
     * @param outputStream 加密数据写入的目标流
     */
    fun encryptFile(content: String, password: String, outputStream: OutputStream) {
        val encrypted = encrypt(content.toByteArray(Charsets.UTF_8), password)
        outputStream.write(encrypted)
        outputStream.flush()
    }

    /**
     * 从输入流读取加密容器并解密为字符串。
     *
     * @param inputStream 包含加密容器的输入流
     * @param password 用户提供的密码
     * @return 解密后的文本内容
     */
    fun decryptFile(inputStream: InputStream, password: String): String {
        val encrypted = inputStream.readBytes()
        val plaintext = decrypt(encrypted, password)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * 流式加密：从 [input] 读取明文，加密后写入 [output]。
     *
     * OPTIMIZE: [C-03] - 修复大文件加密内存放大 3 倍问题。
     * 原 encrypt(ByteArray) 在内存中持有：plaintext + ciphertext + 输出容器 = 3× 文件大小。
     * 对 100MB 文件，峰值内存达 300MB+，可能 OOM。
     *
     * 流式加密使用 CipherInputStream，仅 16KB 缓冲区，峰值内存 ≈ 64KB（与文件大小无关）。
     * 100MB 文件加密内存从 300MB → 64KB（5000× 降耗，估算值）。
     *
     * 容器布局与 encrypt(ByteArray) 一致：
     * [version(4)][salt(16)][nonce(12)][ciphertext + GCM tag]
     *
     * @param input 明文输入流（调用方负责关闭）
     * @param password 用户提供的密码
     * @param output 加密容器输出流（调用方负责关闭）
     */
    fun encryptStream(input: InputStream, password: String, output: OutputStream) {
        val secureRandom = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val nonce = ByteArray(NONCE_LENGTH).also { secureRandom.nextBytes(it) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        // 写入容器头部
        val header = ByteArray(HEADER_LENGTH)
        writeVersion(header, 0)
        System.arraycopy(salt, 0, header, 4, SALT_LENGTH)
        System.arraycopy(nonce, 0, header, 4 + SALT_LENGTH, NONCE_LENGTH)
        output.write(header)

        // 流式加密主体：CipherInputStream 自动处理 GCM tag 写入
        CipherInputStream(input, cipher).use { cipherInput ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                val read = cipherInput.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        }
        output.flush()
    }

    /**
     * 流式解密：从 [input] 读取加密容器，解密后写入 [output]。
     *
     * OPTIMIZE: [C-03] - 配套 encryptStream 的流式解密。
     * 原 decrypt(ByteArray) 要求整个密文在内存，大文件场景不适用。
     * 流式解密峰值内存 ≈ 64KB（与文件大小无关）。
     *
     * 注意：GCM tag 校验在流末尾进行，如果 tag 不匹配会在最后 read() 抛
     * AEADBadTagException（包装为 IOException）。已写入 output 的数据无法回滚
     * （流式特性），调用方应在确认 stream 完整读取后才信任 output 内容，
     * 或写入临时文件后重命名。
     *
     * @param input 包含加密容器的输入流（调用方负责关闭）
     * @param password 用户提供的密码
     * @param output 明文输出流（调用方负责关闭）
     */
    fun decryptStream(input: InputStream, password: String, output: OutputStream) {
        val header = ByteArray(HEADER_LENGTH)
        val bytesRead = readFully(input, header)
        require(bytesRead == HEADER_LENGTH) {
            "Invalid container: header truncated (expected $HEADER_LENGTH, got $bytesRead)"
        }

        val version = readVersion(header, 0)
        require(version == VERSION) { "Unsupported container version: $version" }

        val salt = header.copyOfRange(4, 4 + SALT_LENGTH)
        val nonce = header.copyOfRange(4 + SALT_LENGTH, HEADER_LENGTH)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        CipherInputStream(input, cipher).use { cipherInput ->
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            while (true) {
                val read = cipherInput.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
            }
        }
        output.flush()
    }

    /**
     * 从输入流读取精确 [buffer.size] 字节，返回实际读取的字节数。
     * E3: 处理 EOF 提前到达的边界。
     */
    private fun readFully(input: InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) break
            offset += read
        }
        return offset
    }

    /**
     * 使用 PBKDF2WithHmacSHA256 从密码和 salt 派生 AES-256 密钥。
     * 结果会被 LRU 缓存以避免重复执行 100,000 次迭代。
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val cacheKey = password + ":" + salt.joinToString("") { "%02x".format(it) }
        synchronized(keyCache) {
            keyCache[cacheKey]?.let { return it }
        }

        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val key = try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val derivedKey = factory.generateSecret(spec).encoded
            SecretKeySpec(derivedKey, "AES")
        } finally {
            spec.clearPassword()
        }

        synchronized(keyCache) {
            keyCache[cacheKey] = key
        }
        return key
    }

    private fun writeVersion(out: ByteArray, offset: Int) {
        out[offset] = (VERSION shr 24).toByte()
        out[offset + 1] = (VERSION shr 16).toByte()
        out[offset + 2] = (VERSION shr 8).toByte()
        out[offset + 3] = VERSION.toByte()
    }

    private fun readVersion(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    // ===== Keystore-bound File Encryption (VULN-016) =====
    // SECURITY VULN-016: Replaced Jetpack Security's legacy file encryption with self-built SecureFileStorage.
    // Uses Android Keystore-backed AES-256-GCM directly, no alpha library dependency.
    // The password-based API above is retained for cross-device file export;
    // SecureFileStorage is for device-local secure storage.

    /**
     * 使用 Android Keystore 加密文件到指定路径。
     *
     * 基于 [SecureFileStorage]（自研 Keystore AES-256-GCM）进行加密，
     * 无需用户提供密码。密钥由硬件级 Keystore 保护。
     *
     * SECURITY VULN-016: 替代 alpha 版 Jetpack Security 文件加密 API。
     *
     * @param context 应用上下文
     * @param content 待加密的文本内容
     * @param outputFile 输出文件
     */
    fun encryptWithKeystore(context: Context, content: String, outputFile: File) {
        SecureFileStorage.encryptToFile(context, content, outputFile)
    }

    /**
     * 使用 Android Keystore 从文件解密文本。
     *
     * 基于 [SecureFileStorage]（自研 Keystore AES-256-GCM）进行解密。
     *
     * SECURITY VULN-016: 替代 alpha 版 Jetpack Security 文件加密 API。
     *
     * @param context 应用上下文
     * @param inputFile 加密文件
     * @return 解密后的文本内容
     */
    fun decryptWithKeystore(context: Context, inputFile: File): String {
        return SecureFileStorage.decryptFromFile(context, inputFile)
    }
}
