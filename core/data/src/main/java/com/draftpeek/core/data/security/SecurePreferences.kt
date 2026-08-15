/**
 * 自研安全键值存储模块。
 *
 * SECURITY VULN-016: 替代 alpha 版 Jetpack Security 的加密偏好存储 API。
 *
 * 使用 Android Keystore + AES-256-GCM 直接实现键值对加密存储，
 * 不依赖 alpha 版本的 Jetpack Security 库。
 *
 * 加密方案：
 * - 密钥来源：Android Keystore（硬件级保护，密钥不可导出）
 * - 加密算法：AES-256-GCM（认证加密，防篡改）
 * - 键存储：明文键存储在普通 SharedPreferences 中（键为非敏感的主机名等标识符）
 * - 值存储：Base64 编码的 [nonce(12B)][ciphertext + GCM tag] 存储在普通 SharedPreferences 中
 *
 * @author DraftPeek Team
 * @since 1.0.29
 */
package com.draftpeek.core.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 基于 Android Keystore 的安全键值存储。
 *
 * 替代 Jetpack Security 的加密偏好存储 API。
 * 密钥由 Android Keystore 硬件级保护，不可导出。
 * 值使用 AES-256-GCM 加密后以 Base64 存储在普通 SharedPreferences 中。
 *
 * @param context 应用上下文
 * @param prefsName SharedPreferences 文件名（实际文件名会加 "secure_" 前缀）
 * @param keyAlias Keystore 密钥别名（不同用途应使用不同别名以实现密钥隔离）
 */
class SecurePreferences(
    private val context: Context,
    prefsName: String,
    private val keyAlias: String,
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val NONCE_LENGTH = 12 // GCM standard nonce
        private const val BASE64_FLAGS = Base64.NO_WRAP
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("secure_$prefsName", Context.MODE_PRIVATE)

    /**
     * 获取或创建 Android Keystore 中的 AES-256-GCM 密钥。
     * 密钥不可导出，绑定设备硬件。
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 加密字符串值，返回 Base64 编码的 [nonce][ciphertext + GCM tag]。
     */
    private fun encryptValue(plaintext: String): String {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val nonce = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 组合 nonce + ciphertext
        val combined = ByteArray(NONCE_LENGTH + ciphertext.size)
        System.arraycopy(nonce, 0, combined, 0, NONCE_LENGTH)
        System.arraycopy(ciphertext, 0, combined, NONCE_LENGTH, ciphertext.size)

        return Base64.encodeToString(combined, BASE64_FLAGS)
    }

    /**
     * 解密 Base64 编码的 [nonce][ciphertext + GCM tag]，返回明文字符串。
     */
    private fun decryptValue(encrypted: String): String? {
        return try {
            val combined = Base64.decode(encrypted, BASE64_FLAGS)
            if (combined.size <= NONCE_LENGTH) return null

            val nonce = combined.copyOfRange(0, NONCE_LENGTH)
            val ciphertext = combined.copyOfRange(NONCE_LENGTH, combined.size)

            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取指定键的解密字符串值。
     *
     * @param key 存储键名
     * @param default 默认值（键不存在时返回）
     * @return 解密后的字符串值，或默认值
     */
    fun getString(key: String, default: String? = null): String? {
        val encrypted = prefs.getString(key, null) ?: return default
        return decryptValue(encrypted) ?: default
    }

    /**
     * 存储加密后的字符串值。
     *
     * 如果 value 为 null，则移除该键（与 SharedPreferences.Editor.putString 行为一致）。
     *
     * @param key 存储键名
     * @param value 待加密存储的字符串值，null 时移除键
     */
    fun putString(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
            return
        }
        val encrypted = encryptValue(value)
        prefs.edit().putString(key, encrypted).apply()
    }

    /**
     * 移除指定键。
     *
     * @param key 要移除的键名
     */
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    /**
     * 批量移除多个键。
     *
     * @param keys 要移除的键名集合
     */
    fun removeAll(keys: Set<String>) {
        val editor = prefs.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }

    /**
     * 获取所有键名集合（不解密值，仅返回键名）。
     *
     * @return 所有存储键名的集合
     */
    fun keys(): Set<String> = prefs.all.keys

    /**
     * 清除所有存储的键值对。
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * 删除底层 SharedPreferences 文件（完全清除）。
     */
    fun deleteFile() {
        clearAll()
    }
}
