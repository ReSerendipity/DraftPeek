/**
 * Git凭证安全存储抽象模块。
 *
 * 本文件定义了Git凭证存储的抽象接口，使[GitManager]可以依赖此接口而不会
 * 与`core/data`模块产生循环依赖。具体实现位于`core/data`模块，通过Hilt在
 * 功能模块层进行依赖注入。
 *
 * ## Keystore密钥失效处理
 *
 * 当底层Keystore密钥失效时（如生物识别重置、设备管理员擦除），
 * 会抛出[CredentialLockedException]。调用方应：
 * 1. 通知用户存储的凭证不再可访问；
 * 2. 提供重新输入凭证的选项。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.vcs

/**
 * Git凭证提供者接口。
 *
 * 定义安全存储和检索Git认证凭证的抽象操作，支持基于令牌（Token）和
 * 基本认证（用户名/密码）两种认证方式。
 */
interface CredentialProvider {

    /**
     * 当Keystore主密钥失效且凭证无法解密时抛出的异常。
     *
     * @property cause 原始异常原因，可为null
     */
    class CredentialLockedException(cause: Throwable? = null) :
        RuntimeException("Stored credentials are inaccessible due to Keystore key invalidation", cause)

    /**
     * 表示特定主机的Git凭证集合数据类。
     *
     * 两种认证方式二选一：
     * - [token]非null时使用基于令牌的认证（推荐）
     * - [username]和[password]非null时使用基本认证
     *
     * @property host 目标Git主机名（如 "github.com"）
     * @property username 用户名，基本认证时使用
     * @property password 密码，基本认证时使用
     * @property token 访问令牌，令牌认证时使用（推荐）
     */
    data class GitCredential(
        val host: String,
        val username: String? = null,
        val password: String? = null,
        val token: String? = null
    ) {
        /**
         * 是否存在有效的令牌。
         */
        val hasToken: Boolean get() = !token.isNullOrBlank()

        /**
         * 是否存在有效的基本认证信息（用户名和密码都非空）。
         */
        val hasBasicAuth: Boolean get() = !username.isNullOrBlank() && !password.isNullOrBlank()

        /**
         * 凭证是否有效（具有令牌或基本认证信息）。
         */
        val isValid: Boolean get() = hasToken || hasBasicAuth
    }

    /**
     * 保存特定主机的Git凭证。
     *
     * 同一主机的现有凭证将被覆盖。推荐使用基于令牌的认证：如果提供了[credential.token]，
     * 该主机的用户名/密码将被清除。
     *
     * @param credential 要存储的凭证
     * @throws CredentialLockedException Keystore密钥失效时抛出
     */
    suspend fun saveCredential(credential: GitCredential)

    /**
     * 获取特定主机存储的Git凭证。
     *
     * @param host Git主机名（如 "github.com"）
     * @return 存储的[GitCredential]，如果该主机没有存储凭证则返回null
     * @throws CredentialLockedException Keystore密钥失效时抛出
     */
    suspend fun getCredential(host: String): GitCredential?

    /**
     * 删除特定主机存储的Git凭证。
     *
     * @param host 要删除凭证的Git主机名
     * @return 找到并删除凭证返回true，不存在返回false
     * @throws CredentialLockedException Keystore密钥失效时抛出
     */
    suspend fun deleteCredential(host: String): Boolean

    /**
     * 列出所有存储了凭证的主机。
     *
     * @return 主机名字符串集合，没有则返回空集合
     * @throws CredentialLockedException Keystore密钥失效时抛出
     */
    suspend fun listHosts(): Set<String>

    /**
     * 删除所有存储的凭证并重置加密存储。
     * 这是一个破坏性操作。
     */
    suspend fun clearAllCredentials()

    /**
     * 检查给定主机是否存储了凭证。
     *
     * @param host 要检查的Git主机名
     * @return 该主机存在凭证返回true
     */
    suspend fun hasCredential(host: String): Boolean
}
