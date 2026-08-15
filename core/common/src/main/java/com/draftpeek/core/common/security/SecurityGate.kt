/**
 * 安全门控模块。
 *
 * 敏感操作（保存、导出、删除、创建）的安全门控。DraftPeekApp在启动检查后更新完整性和环境状态，
 * 功能模块在执行敏感操作前调用[isOperationAllowed]进行检查。
 *
 * Debug构建：两个标志默认为true，允许所有操作。
 * Release构建：DraftPeekApp调用[updateIntegrity]和[updateEnvironmentSafe]反映实际安全状态。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.security

/**
 * 安全门控对象。
 *
 * 集中管理应用完整性和环境安全状态，作为所有敏感操作的前置检查。
 * 使用@Volatile注解保证多线程可见性，状态由应用层（DraftPeekApp）更新。
 */
object SecurityGate {

    @Volatile
    private var integrityOk: Boolean = true

    @Volatile
    private var environmentSafe: Boolean = true

    /**
     * 当前操作是否被允许。
     * 同时检查完整性和环境安全性。
     *
     * @return 如果允许操作返回true，否则返回false
     */
    fun isOperationAllowed(): Boolean = integrityOk && environmentSafe

    /**
     * 更新完整性状态。由DraftPeekApp在APK/DEX验证后调用。
     *
     * @param verified 完整性验证是否通过
     */
    fun updateIntegrity(verified: Boolean) {
        integrityOk = verified
    }

    /**
     * 更新环境安全状态。由DraftPeekApp定期反调试检查调用。
     *
     * @param safe 环境是否安全
     */
    fun updateEnvironmentSafe(safe: Boolean) {
        environmentSafe = safe
    }
}
