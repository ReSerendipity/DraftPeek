/**
 * DraftPeek 自研安全文件存储模块。
 *
 * SECURITY VULN-016: 替代 alpha 版 Jetpack Security (security-crypto:1.1.0-alpha06)。
 *
 * 使用 Android Keystore + AES-256-GCM 直接实现文件加密存储，
 * 不依赖 alpha 版本的 Jetpack Security 库，避免长期 alpha 状态带来的
 * 兼容性风险和安全补丁不确定问题。
 *
 * 加密方案：
 * - 密钥来源：Android Keystore（硬件级保护，密钥不可导出）
 * - 加密算法：AES-256-GCM（认证加密，防篡改）
 * - 密钥别名：draftpeek_secure_file_key
 * - 文件格式：[nonce(12B)][ciphertext + GCM tag]
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.core.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 Android Keystore 的安全文件存储。
 *
 * 提供 Keystore 绑定的文件加密/解密功能，替代 Jetpack Security 的遗留文件加密 API。
 * 密钥由 Android Keystore 硬件级保护，不可导出。
 */
object SecureFileStorage {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "draftpeek_secure_file_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val NONCE_LENGTH = 12 // GCM standard nonce

    /**
     * 获取或创建 Android Keystore 中的 AES-256-GCM 密钥。
     * 密钥不可导出，绑定设备硬件。
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        // 检查密钥是否已存在
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        // 创建新密钥
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true) // 强制使用随机 IV
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 加密字符串内容并写入文件。
     *
     * @param context 应用上下文（未使用，保留用于未来扩展）
     * @param content 待加密的文本内容
     * @param outputFile 输出文件
     */
    fun encryptToFile(context: Context, content: String, outputFile: File) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val nonce = cipher.iv
        val ciphertext = cipher.doFinal(content.toByteArray(Charsets.UTF_8))

        // 文件格式：[nonce(12B)][ciphertext + GCM tag]
        if (outputFile.exists()) outputFile.delete()
        outputFile.outputStream().use { output ->
            output.write(nonce)
            output.write(ciphertext)
            output.flush()
        }
    }

    /**
     * 从加密文件解密为字符串。
     *
     * @param context 应用上下文（未使用，保留用于未来扩展）
     * @param inputFile 加密文件
     * @return 解密后的文本内容
     */
    fun decryptFromFile(context: Context, inputFile: File): String {
        val key = getOrCreateKey()
        val fileBytes = inputFile.readBytes()

        require(fileBytes.size > NONCE_LENGTH) {
            "Invalid encrypted file: too short (expected at least ${NONCE_LENGTH + 1} bytes)"
        }

        val nonce = fileBytes.copyOfRange(0, NONCE_LENGTH)
        val ciphertext = fileBytes.copyOfRange(NONCE_LENGTH, fileBytes.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * 流式加密：从输入流读取明文，加密后写入输出流。
     *
     * @param context 应用上下文
     * @param input 明文输入流
     * @param output 加密数据输出流
     */
    fun encryptStream(context: Context, input: InputStream, output: OutputStream) {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val nonce = cipher.iv
        output.write(nonce)

        // GCM 模式不需要 padding，可以直接处理
        val plaintext = input.readBytes()
        val ciphertext = cipher.doFinal(plaintext)
        output.write(ciphertext)
        output.flush()
    }

    /**
     * 流式解密：从输入流读取加密数据，解密后写入输出流。
     *
     * @param context 应用上下文
     * @param input 加密数据输入流
     * @param output 明文输出流
     */
    fun decryptStream(context: Context, input: InputStream, output: OutputStream) {
        val key = getOrCreateKey()

        val nonce = ByteArray(NONCE_LENGTH)
        var bytesRead = 0
        while (bytesRead < NONCE_LENGTH) {
            val read = input.read(nonce, bytesRead, NONCE_LENGTH - bytesRead)
            if (read == -1) break
            bytesRead += read
        }
        require(bytesRead == NONCE_LENGTH) {
            "Invalid encrypted stream: nonce truncated"
        }

        val ciphertext = input.readBytes()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        val plaintext = cipher.doFinal(ciphertext)
        output.write(plaintext)
        output.flush()
    }
}
