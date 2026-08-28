/**
 * DraftPeek Google Play Integrity API 集成模块。
 *
 * SECURITY VULN-009 FIXED: 集成 Google Play Integrity API 进行设备完整性验证。
 *
 * Play Integrity API 提供以下验证：
 * - 设备完整性（Device Integrity）：设备是否通过 Google 认证、是否被 Root
 * - 应用完整性（App Integrity）：APP 是否被篡改、是否从官方渠道安装
 * - 账户完整性（Account Details）：账户活动风险
 *
 * 实现包含完整的客户端→服务端验证流程：
 * 1. 客户端请求 Play Integrity token（nonce 防重放）
 * 2. 将 token 发送到 DraftPeek Integrity Server 进行服务端解密验证
 * 3. 服务端调用 Google Play Integrity API 解密 token 并返回 verdict
 * 4. 客户端根据 verdict 判断设备是否可信
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private const val TAG = "PlayIntegrity"

/**
 * Google Play Integrity API 客户端。
 *
 * 提供：
 * 1. Play Integrity token 请求（客户端→Google）
 * 2. 服务端验证（客户端→DraftPeek Server→Google API→verdict）
 *
 * 服务端验证确保 token 不可被伪造（攻击者无法在客户端 mock token）。
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

    /**
     * 服务端验证结果
     */
    sealed class ServerVerificationResult {
        /** 设备通过服务端验证 */
        data class Trusted(val deviceIntegrity: String, val appIntegrity: String) : ServerVerificationResult()

        /** 设备未通过验证 */
        data class NotTrusted(val deviceIntegrity: String, val appIntegrity: String, val reason: String) :
            ServerVerificationResult()

        /** 服务端不可达或验证失败 */
        data class Unavailable(val reason: String) : ServerVerificationResult()
    }

    @Volatile
    private var lastResult: IntegrityCheckResult? = null

    /** 上次请求使用的 nonce，供服务端验证时携带 */
    @Volatile
    private var lastNonce: String = ""

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
    suspend fun requestIntegrityToken(context: Context): IntegrityCheckResult = try {
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
        if (message.contains("Play Services") ||
            message.contains("SERVICE") ||
            message.contains("ClassNotFound") ||
            message.contains("NoClassDefFound")
        ) {
            lastResult = IntegrityCheckResult.NoPlayServices
            IntegrityCheckResult.NoPlayServices
        } else {
            lastResult = IntegrityCheckResult.Error(message)
            IntegrityCheckResult.Error(message)
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
    fun isDeviceTrusted(): Boolean = lastResult is IntegrityCheckResult.Success

    /**
     * 执行完整的服务端验证流程：
     * 1. 请求 Play Integrity token
     * 2. 将 token 发送到 DraftPeek Integrity Server 进行服务端解密验证
     * 3. 返回验证结果
     *
     * 此方法是 suspend 函数，应在 IO 线程调用。
     * 如果设备没有 Google Play 服务，返回 [ServerVerificationResult.Unavailable]。
     *
     * @param context 应用上下文
     * @param serverUrl DraftPeek Integrity Server URL（默认为生产环境地址）
     * @return 服务端验证结果
     */
    @WorkerThread
    suspend fun verifyWithServer(context: Context, serverUrl: String = DEFAULT_SERVER_URL): ServerVerificationResult {
        // Step 1: 获取 token 和 nonce
        val tokenResult = requestIntegrityToken(context)
        val token = when (tokenResult) {
            is IntegrityCheckResult.Success -> tokenResult.token
            is IntegrityCheckResult.NoPlayServices ->
                return ServerVerificationResult.Unavailable("No Google Play Services")
            is IntegrityCheckResult.Error ->
                return ServerVerificationResult.Unavailable(tokenResult.message)
        }

        // Step 2: 将 token 发送到服务端验证
        return sendTokenToServer(token, lastNonce, serverUrl)
    }

    /**
     * 将 Play Integrity token 发送到服务端进行验证。
     */
    @WorkerThread
    private suspend fun sendTokenToServer(token: String, nonce: String, serverUrl: String): ServerVerificationResult =
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = org.json.JSONObject().apply {
                    put("token", token)
                    put("nonce", nonce)
                    put("package_name", "com.draftpeek")
                }

                val url = java.net.URL("$serverUrl/integrity/verify")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                conn.outputStream.use { it.write(jsonBody.toString().toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    conn.disconnect()
                    return@withContext ServerVerificationResult.Unavailable(
                        "Server returned HTTP $responseCode"
                    )
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = org.json.JSONObject(body)
                val verified = json.optBoolean("verified", false)
                val deviceIntegrity = json.optString("device_integrity", "UNKNOWN")
                val appIntegrity = json.optString("app_integrity", "UNKNOWN")

                if (verified) {
                    ServerVerificationResult.Trusted(deviceIntegrity, appIntegrity)
                } else {
                    ServerVerificationResult.NotTrusted(
                        deviceIntegrity,
                        appIntegrity,
                        "Server verification failed"
                    )
                }
            } catch (e: Exception) {
                ServerVerificationResult.Unavailable(e.message ?: "Network error")
            }
        }

    /** DraftPeek Integrity Server 默认地址 */
    private const val DEFAULT_SERVER_URL = "https://integrity.draftpeek.com"

    /**
     * 生成随机 nonce（32 字节十六进制字符串）。
     * nonce 用于防止重放攻击，每次请求使用不同的 nonce。
     */
    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        val nonce = bytes.joinToString("") { "%02x".format(it) }
        lastNonce = nonce
        return nonce
    }
}
