/**
 * Git 凭据安全存储管理器。
 *
 * 使用 Android Keystore + 自研 SecurePreferences 安全存储 Git 认证凭据
 * （用户名/密码或 Personal Access Token）。实现 core/common 中的 CredentialProvider
 * 接口，供 GitManager 使用，避免循环依赖。
 *
 * SECURITY VULN-016: 替代 alpha 版 Jetpack Security 加密偏好存储。
 *
 * ## 加密策略
 * - 主密钥：Android Keystore 中的 AES-256-GCM 密钥（硬件保护，不可导出）
 * - 存储：SecurePreferences（值使用 AES-256-GCM 加密，Base64 存储在普通 SharedPreferences 中）
 * - 按主机隔离：凭据以 Git 主机（如 "github.com"）为键存储，支持多账户
 * - 线程安全：所有 I/O 操作在 Dispatchers.IO 上执行
 *
 * ## 密钥失效处理
 * 当 Keystore 主密钥失效（如生物识别重置、设备管理策略擦除）时，
 * 抛出 CredentialLockedException。与数据库密钥不同，凭据可由用户重新输入，
 * 因此不提供自动回退方案——调用方应引导用户重新输入凭据。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.security

import android.content.Context
import android.util.Log
import com.draftpeek.core.common.vcs.CredentialProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Git 凭据安全存储实现类。
 *
 * 提供按主机存储/查询/删除 Git 凭据的功能，支持 Token 和用户名/密码两种认证方式。
 * Token 认证优先：如果提供了 token，则清除该主机下的用户名/密码。
 *
 * @property context 应用上下文
 */
@Singleton
class CredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : CredentialProvider {

    companion object {
        private const val TAG = "CredentialManager"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "draftpeek_credential_master_key"
        private const val PREFS_FILE_NAME = "draftpeek_credentials"
        private const val KEY_USERNAME_SUFFIX = "_username"
        private const val KEY_PASSWORD_SUFFIX = "_password"
        private const val KEY_TOKEN_SUFFIX = "_token"
    }

    /**
     * 延迟初始化的 SecurePreferences 实例。
     *
     * SECURITY VULN-016: 替代 alpha 版 Jetpack Security 加密偏好存储。
     * 使用自研 SecurePreferences，基于 Android Keystore + AES-256-GCM 加密。
     *
     * 如果主密钥已失效，将抛出 [CredentialProvider.CredentialLockedException]，
     * 调用方应捕获并处理此异常。
     */
    private val securePrefs: SecurePreferences by lazy {
        try {
            SecurePreferences(context, PREFS_FILE_NAME, MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            if (isKeyInvalidatedException(e)) {
                throw CredentialProvider.CredentialLockedException(cause = e)
            }
            Log.e(TAG, "Failed to initialize SecurePreferences", e)
            throw e
        }
    }

    /**
     * 为指定 Git 主机保存凭据。
     *
     * 同一主机的已有凭据将被覆盖。优先使用 Token 认证：
     * 如果 [credential.token] 非空，则清除该主机的用户名/密码。
     *
     * @param credential 要存储的 Git 凭据
     * @throws CredentialProvider.CredentialLockedException 如果 Keystore 密钥失效
     */
    override suspend fun saveCredential(credential: CredentialProvider.GitCredential) {
        withContext(Dispatchers.IO) {
            try {
                val hostKey = sanitizeHost(credential.host)

                if (credential.hasToken) {
                    securePrefs.putString("$hostKey$KEY_TOKEN_SUFFIX", credential.token)
                    securePrefs.remove("$hostKey$KEY_USERNAME_SUFFIX")
                    securePrefs.remove("$hostKey$KEY_PASSWORD_SUFFIX")
                } else if (credential.hasBasicAuth) {
                    securePrefs.putString("$hostKey$KEY_USERNAME_SUFFIX", credential.username)
                    securePrefs.putString("$hostKey$KEY_PASSWORD_SUFFIX", credential.password)
                    securePrefs.remove("$hostKey$KEY_TOKEN_SUFFIX")
                }

                Log.i(TAG, "Credential saved for host: ${credential.host}")
            } catch (e: CredentialProvider.CredentialLockedException) {
                throw e
            } catch (e: Exception) {
                if (isKeyInvalidatedException(e)) {
                    throw CredentialProvider.CredentialLockedException(cause = e)
                }
                Log.e(TAG, "Failed to save credential for host: ${credential.host}", e)
                throw e
            }
        }
    }

    /**
     * 获取指定 Git 主机的存储凭据。
     *
     * @param host Git 主机地址（如 "github.com"）
     * @return 存储的 [CredentialProvider.GitCredential]，该主机无凭据时返回 null
     * @throws CredentialProvider.CredentialLockedException 如果 Keystore 密钥失效
     */
    override suspend fun getCredential(host: String): CredentialProvider.GitCredential? {
        return withContext(Dispatchers.IO) {
            try {
                val hostKey = sanitizeHost(host)

                val token = securePrefs.getString("$hostKey$KEY_TOKEN_SUFFIX", null)
                if (!token.isNullOrBlank()) {
                    return@withContext CredentialProvider.GitCredential(
                        host = host,
                        token = token,
                    )
                }

                val username = securePrefs.getString("$hostKey$KEY_USERNAME_SUFFIX", null)
                val password = securePrefs.getString("$hostKey$KEY_PASSWORD_SUFFIX", null)
                if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                    return@withContext CredentialProvider.GitCredential(
                        host = host,
                        username = username,
                        password = password,
                    )
                }

                null
            } catch (e: CredentialProvider.CredentialLockedException) {
                throw e
            } catch (e: Exception) {
                if (isKeyInvalidatedException(e)) {
                    throw CredentialProvider.CredentialLockedException(cause = e)
                }
                Log.e(TAG, "Failed to retrieve credential for host: $host", e)
                null
            }
        }
    }

    /**
     * 删除指定 Git 主机的存储凭据。
     *
     * @param host 要删除凭据的 Git 主机
     * @return true 表示找到并删除了凭据，false 表示该主机无存储凭据
     * @throws CredentialProvider.CredentialLockedException 如果 Keystore 密钥失效
     */
    override suspend fun deleteCredential(host: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existing = getCredential(host)
                if (existing == null) {
                    return@withContext false
                }

                val hostKey = sanitizeHost(host)
                securePrefs.remove("$hostKey$KEY_TOKEN_SUFFIX")
                securePrefs.remove("$hostKey$KEY_USERNAME_SUFFIX")
                securePrefs.remove("$hostKey$KEY_PASSWORD_SUFFIX")

                Log.i(TAG, "Credential deleted for host: $host")
                true
            } catch (e: CredentialProvider.CredentialLockedException) {
                throw e
            } catch (e: Exception) {
                if (isKeyInvalidatedException(e)) {
                    throw CredentialProvider.CredentialLockedException(cause = e)
                }
                Log.e(TAG, "Failed to delete credential for host: $host", e)
                false
            }
        }
    }

    /**
     * 列出所有存储了凭据的 Git 主机。
     *
     * @return 主机地址集合，无凭据或出错时返回空集
     * @throws CredentialProvider.CredentialLockedException 如果 Keystore 密钥失效
     */
    override suspend fun listHosts(): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                securePrefs.keys()
                    .mapNotNull { key ->
                        when {
                            key.endsWith(KEY_TOKEN_SUFFIX) ->
                                key.removeSuffix(KEY_TOKEN_SUFFIX)
                            key.endsWith(KEY_USERNAME_SUFFIX) ->
                                key.removeSuffix(KEY_USERNAME_SUFFIX)
                            key.endsWith(KEY_PASSWORD_SUFFIX) ->
                                key.removeSuffix(KEY_PASSWORD_SUFFIX)
                            else -> null
                        }
                    }
                    .toSet()
            } catch (e: CredentialProvider.CredentialLockedException) {
                throw e
            } catch (e: Exception) {
                if (isKeyInvalidatedException(e)) {
                    throw CredentialProvider.CredentialLockedException(cause = e)
                }
                Log.e(TAG, "Failed to list credential hosts", e)
                emptySet()
            }
        }
    }

    /**
     * 清除所有存储的凭据并重置 SecurePreferences。
     *
     * 这是一个破坏性操作。应在用户要求清除所有 Git 凭据时调用，
     * 或在捕获 [CredentialProvider.CredentialLockedException] 且用户同意重新输入凭据时调用。
     *
     * 清除后会重新生成 Keystore 主密钥以确保状态干净。
     */
    override suspend fun clearAllCredentials() {
        withContext(Dispatchers.IO) {
            try {
                securePrefs.clearAll()

                try {
                    val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
                    keyStore.load(null)
                    if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                        keyStore.deleteEntry(MASTER_KEY_ALIAS)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete master key from Keystore", e)
                }

                Log.i(TAG, "All credentials cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all credentials", e)
            }
        }
    }

    /**
     * 检查指定主机是否存储了凭据。
     *
     * @param host Git 主机地址
     * @return true 表示该主机有存储的凭据
     */
    override suspend fun hasCredential(host: String): Boolean = getCredential(host) != null

    /**
     * 清理主机字符串使其适合作为 SharedPreferences 键前缀。
     *
     * 将非字母数字字符替换为下划线，确保键名在 SharedPreferences 中安全存储。
     *
     * @param host 原始主机字符串
     * @return 清理后的主机键名
     */
    private fun sanitizeHost(host: String): String {
        return host.lowercase().trim()
            .replace(Regex("[^a-z0-9]"), "_")
            .trim('_')
    }

    /**
     * 从异常类型检测 Keystore 密钥失效。
     *
     * 不同 OEM ROM 抛出的异常类型不同，通过类名匹配进行兜底检测。
     *
     * @param e 捕获的异常
     * @return true 表示密钥已失效
     */
    private fun isKeyInvalidatedException(e: Throwable): Boolean {
        val name = e.javaClass.name
        return name.contains("KeyInvalidated") ||
            name.contains("KeyPermanentlyInvalidated") ||
            e is android.security.keystore.KeyPermanentlyInvalidatedException
    }
}
