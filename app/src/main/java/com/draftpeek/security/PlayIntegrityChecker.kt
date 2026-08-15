/**
 * DraftPeek Google Play Integrity API 集成模块。
 *
 * SECURITY VULN-009: 集成 Google Play Integrity API 进行设备完整性验证。
 *
 * Play Integrity API 提供以下验证：
 * - 设备完整性（Device Integrity）：设备是否通过 Google 认证、是否被 Root
 * - 应用完整性（App Integrity）：APP 是否被篡改、是否从官方渠道安装
 * - 账户完整性（Account Details）：账户活动风险
 *
 * 注意：完整的 Play Integrity 验证需要后端服务器解密 token。
 * 本实现作为客户端信号使用：
 * - 如果 API 可用且请求成功 → 设备有 Google Play 服务（正面信号）
 * - 如果 API 不可用 → 中性信号（不是所有设备都有 Play 服务）
 * - 如果请求意外失败 → 可能表示环境异常（可疑信号）
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.security

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "PlayIntegrity"

/**
 * Google Play Integrity API 客户端。
 *
 * 提供 Play Integrity token 请求和设备完整性检查功能。
 * 由于完整验证需要后端服务器，本实现仅作为客户端信号使用。
 */
object PlayIntegrityChecker {

    /**
     * Play Integrity 检查结果
     */
    sealed class IntegrityCheckResult {
        /** Play 服务可用且 token 请求成功 */
        data class Success(val token: String) : IntegrityCheckResult()
        /** 设备没有 Google Play 服务（中性信号，不一定表示威胁） */
        data object NoPlayServices : IntegrityCheckResult()
        /** API 请求失败（可疑信号） */
        data class Error(val message: String) : IntegrityCheckResult()
    }

    @Volatile
    private var lastResult: IntegrityCheckResult? = null

    /**
     * 请求 Play Integrity token。
     *
     * 此方法是 suspend 函数，应在 IO 线程调用。
     * 生成一个随机 nonce 并请求 integrity token。
     *
     * @param context 应用上下文
     * @return 检查结果
     */
    @WorkerThread
    suspend fun requestIntegrityToken(context: Context): IntegrityCheckResult {
        return try {
            val integrityManager: IntegrityManager =
                IntegrityManagerFactory.create(context)

            // 生成随机 nonce（32 字节十六进制字符串）
            val nonce = generateNonce()

            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            suspendCancellableCoroutine { continuation ->
                integrityManager.requestIntegrityToken(request)
                    .addOnSuccessListener { response ->
                        val token = response.token()
                        lastResult = IntegrityCheckResult.Success(token)
                        continuation.resume(IntegrityCheckResult.Success(token))
                    }
                    .addOnFailureListener { e ->
                        // 判断是否是因为没有 Play 服务
                        val message = e.message ?: "Unknown error"
                        if (message.contains("Play Services") || message.contains("SERVICE")) {
                            lastResult = IntegrityCheckResult.NoPlayServices
                            continuation.resume(IntegrityCheckResult.NoPlayServices)
                        } else {
                            lastResult = IntegrityCheckResult.Error(message)
                            continuation.resume(IntegrityCheckResult.Error(message))
                        }
                    }
            }
        } catch (e: Exception) {
            // 如果创建 IntegrityManager 失败，可能设备没有 Play 服务
            val message = e.message ?: "Unknown error"
            if (message.contains("Play Services") || message.contains("SERVICE") ||
                message.contains("ClassNotFound") || message.contains("NoClassDefFound")) {
                lastResult = IntegrityCheckResult.NoPlayServices
                IntegrityCheckResult.NoPlayServices
            } else {
                lastResult = IntegrityCheckResult.Error(message)
                IntegrityCheckResult.Error(message)
            }
        }
    }

    /**
     * 获取上次检查结果（缓存）。
     */
    fun getLastResult(): IntegrityCheckResult? = lastResult

    /**
     * 判断设备是否通过 Play Integrity 检查（基于上次结果）。
     *
     * @return true 如果检查成功（有 Play 服务且 token 获取成功）
     */
    fun isDeviceTrusted(): Boolean {
        return lastResult is IntegrityCheckResult.Success
    }

    /**
     * 生成随机 nonce（32 字节十六进制字符串）。
     * nonce 用于防止重放攻击，每次请求使用不同的 nonce。
     */
    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
