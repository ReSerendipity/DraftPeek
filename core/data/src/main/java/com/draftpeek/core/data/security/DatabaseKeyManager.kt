/**
 * 数据库加密密钥管理器模块。
 *
 * 负责 SQLCipher 数据库加密密钥的安全生成、存储和获取，是 DraftPeek 数据库安全的核心组件。
 * 密钥优先使用 Android Keystore 硬件级保护，极端情况下（Keystore 不可用的新装设备）
 * 回退到 PBKDF2 从持久化随机种子派生。
 *
 * ## 加密策略
 * - 主加密算法：AES-256-GCM（认证加密，提供机密性和完整性校验）
 * - 密钥存储：Android Keystore（硬件安全模块支持时），密钥不可导出
 * - 回退方案：PBKDF2WithHmacSHA256（100,000 次迭代）
 * - 密码学随机数：SecureRandom（CSPRNG）
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * 数据库加密密钥管理器。
 *
 * 使用 Android Keystore 生成并存储 AES-256-GCM 密钥，
 * 用于加密 Room 数据库的 SQLCipher passphrase。
 *
 * 密钥不会离开 Keystore（硬件级保护），即使在 Root 设备上也无法提取。
 *
 * ## 密钥失效策略（D-01 修复）
 *
 * 当 Keystore 密钥被系统失效（如生物识别重置、Clear Credentials、MDM 擦除）时，
 * **绝不静默回退到 PBKDF2 派生密钥**——否则会用与原 DB 不匹配的密钥"成功"打开空数据库，
 * 造成用户数据被无声丢弃。
 *
 * 取而代之：抛出 [DatabaseLockedException]，调用方应：
 * 1. 提示用户"安全凭据已失效，需重新选择文件"；
 * 2. 删除旧 DB 文件（已不可恢复）后重建。
 *
 * PBKDF2 回退**仅**用于首次启动即无 Keystore 的新装设备（极少数 ROM 缺陷场景）。
 */
object DatabaseKeyManager {

    private const val TAG = "DatabaseKeyManager"
    private const val KEYSTORE_ALIAS = "draftpeek_db_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val PASSPHRASE_FILE = ".db_passphrase"
    private const val FALLBACK_SALT_FILE = ".db_salt"
    private const val FALLBACK_SEED_FILE = ".db_seed"
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_SIZE = 256

    /**
     * 标记当前是否使用了 Keystore 硬件级保护。
     * 在 [getOrCreatePassphrase] 首次调用后设置。
     */
    @Volatile
    private var usingKeystore: Boolean = false

    /**
     * 当 Keystore 中存在但密钥已失效时置为 true。
     * 调用方可据此区分"全新设备"与"凭据失效"两种回退场景。
     */
    @Volatile
    private var keystoreInvalidated: Boolean = false

    /**
     * 当数据库因 Keystore 失效而无法解密时抛出。
     * 调用方应捕获并向用户解释数据已不可恢复，引导重建数据库。
     */
    class DatabaseLockedException(val reason: Reason, cause: Throwable? = null) :
        RuntimeException("Database inaccessible: $reason", cause) {
        enum class Reason { KEY_INVALIDATED, DECRYPT_FAILED, KEYSTORE_UNAVAILABLE }
    }

    /** 返回当前是否使用了 Android Keystore 进行密钥保护。 */
    fun isUsingKeystore(): Boolean = usingKeystore

    /** 返回 Keystore 是否在已存在密钥的情况下失效。 */
    fun isKeystoreInvalidated(): Boolean = keystoreInvalidated

    /**
     * 获取或创建数据库加密 passphrase。
     *
     * E-04 修复：整个方法 `@Synchronized`，防止两个调用者同时进入
     * "文件不存在 → 生成 → 写入" 竞态，导致 passphrase 被相互覆盖。
     *
     * @throws DatabaseLockedException 当 Keystore 失效且已有加密 passphrase 文件时
     */
    @Synchronized
    fun getOrCreatePassphrase(context: Context): CharArray {
        val passphraseFile = File(context.filesDir, PASSPHRASE_FILE)

        // 路径 A：已存在加密 passphrase —— 必须用 Keystore 解密，失败即抛出
        if (passphraseFile.exists()) {
            return try {
                usingKeystore = true
                decryptPassphrase(passphraseFile)
            } catch (e: Exception) {
                // SECURITY: 不静默回退。passphrase 文件存在意味着 DB 已用此 passphrase 加密，
                // 任何其他派生路径都会产生不匹配的密钥 → 永久性数据丢失。
                Log.e(
                    TAG,
                    "Keystore key invalid while passphrase file exists. " +
                        "DB is permanently locked; refusing to fall back silently.",
                    e
                )
                usingKeystore = false
                keystoreInvalidated = true
                throw DatabaseLockedException(
                    reason = if (isKeyInvalidatedException(e)) {
                        DatabaseLockedException.Reason.KEY_INVALIDATED
                    } else {
                        DatabaseLockedException.Reason.DECRYPT_FAILED
                    },
                    cause = e
                )
            }
        }

        // 路径 B：全新设备，无 passphrase 文件 —— 优先 Keystore，失败可安全回退
        return try {
            val passphrase = generatePassphrase()
            encryptAndStorePassphrase(passphrase, passphraseFile)
            usingKeystore = true
            passphrase
        } catch (e: Exception) {
            // SECURITY: 全新设备无 DB，回退 PBKDF2 不会破坏既有数据
            Log.w(
                TAG,
                "Keystore unavailable on fresh install, falling back to PBKDF2. " +
                    "Sensitive features should be disabled.",
                e
            )
            usingKeystore = false
            generateFallbackPassphrase(context)
        }
    }

    /**
     * 判断异常是否表示 Keystore 密钥被系统失效。
     * 不同 OEM ROM 抛出的异常类型不一，按类名匹配以兜底。
     */
    private fun isKeyInvalidatedException(e: Throwable): Boolean {
        val name = e.javaClass.name
        return name.contains("KeyInvalidated") ||
            name.contains("KeyPermanentlyInvalidated") ||
            e is android.security.keystore.KeyPermanentlyInvalidatedException
    }

    /** 获取或创建 Android Keystore 中的 AES 密钥 */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        val key = keyStore.getKey(KEYSTORE_ALIAS, null)
        if (key is SecretKey) {
            return key
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * 使用 SecureRandom 生成随机 passphrase（32 字节 = 256 位）。
     * D9: 使用 CSPRNG (SecureRandom)。
     */
    private fun generatePassphrase(): CharArray {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP).toCharArray()
    }

    /**
     * 使用 Keystore 密钥加密 passphrase 并存储到文件。
     *
     * REFACTOR: 写入采用 .tmp + renameTo 原子替换，避免半写文件在崩溃后
     * 被路径 A 当作有效 passphrase 读取导致解密失败。
     */
    private fun encryptAndStorePassphrase(passphrase: CharArray, outputFile: File) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(String(passphrase).toByteArray(Charsets.UTF_8))

        // 格式: [IV (12 bytes)][encrypted data]
        val tmp = File(outputFile.parentFile, "${outputFile.name}.tmp")
        try {
            tmp.writeBytes(iv + encrypted)
            // SECURITY: 原子替换；旧文件若存在先删除（renameTo 在 Windows/某些 ROM 上不可覆盖）
            if (outputFile.exists()) outputFile.delete()
            if (!tmp.renameTo(outputFile)) {
                // 回退到非原子写入（tmp.renameTo 失败的 ROM）
                outputFile.writeBytes(iv + encrypted)
                tmp.delete()
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }

    /** 从加密文件中读取并解密 passphrase */
    private fun decryptPassphrase(inputFile: File): CharArray {
        val secretKey = getOrCreateKey()
        val data = inputFile.readBytes()

        if (data.size <= GCM_IV_SIZE) {
            throw IllegalStateException("Passphrase file truncated: ${data.size} bytes")
        }
        val iv = data.copyOfRange(0, GCM_IV_SIZE)
        val encrypted = data.copyOfRange(GCM_IV_SIZE, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE, iv))

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8).toCharArray()
    }

    /**
     * 回退方案：使用 PBKDF2 从持久化随机种子派生 passphrase。
     *
     * **仅在全新设备 + Keystore 不可用时调用**。
     * 种子通过 SecureRandom 首次生成后持久化存储，确保每次派生相同 passphrase。
     */
    private fun generateFallbackPassphrase(context: Context): CharArray {
        val saltFile = File(context.filesDir, FALLBACK_SALT_FILE)
        val salt = if (saltFile.exists()) {
            saltFile.readBytes()
        } else {
            val randomSalt = ByteArray(32)
            SecureRandom().nextBytes(randomSalt)
            saltFile.writeBytes(randomSalt)
            randomSalt
        }

        val seedFile = File(context.filesDir, FALLBACK_SEED_FILE)
        val seed = if (seedFile.exists()) {
            seedFile.readBytes()
        } else {
            val randomSeed = ByteArray(32)
            SecureRandom().nextBytes(randomSeed)
            seedFile.writeBytes(randomSeed)
            randomSeed
        }

        val spec = PBEKeySpec(
            Base64.encodeToString(seed, Base64.NO_WRAP).toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_SIZE
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedKey = factory.generateSecret(spec).encoded

        return Base64.encodeToString(derivedKey, Base64.NO_WRAP).toCharArray()
    }
}
