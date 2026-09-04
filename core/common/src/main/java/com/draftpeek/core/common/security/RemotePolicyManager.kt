/**
 * 远程安全策略管理器（Remote Kill Switch + 策略热更新框架）。
 *
 * SECURITY IMPROVEMENT P1-1 / R1 整改：远程策略 Ed25519 验签（fail-closed）。
 *
 * 整改前（v1.0.30）：`verifySignature()` 恒返回 `true`，任何能控制 `policy.draftpeek.com`
 * （DNS 劫持 / MITM / 服务端被控）的一方都可下发 `{"kill_switch":true}` 或
 * `{"ai_protection_enabled":false}`，**无声关闭含"签名篡改杀进程"在内的全部 AI 防护**。
 *
 * 整改后（本文件）：
 * 1. 策略必须由构建期注入的 Ed25519 公钥验签；公钥缺失 / 验签失败 / 缺签名 → 一律忽略策略（fail-closed）。
 * 2. kill switch 仅在「签名有效 且 策略未过期」时生效（`currentPolicy` 只从已验真来源更新）。
 * 3. 本地缓存策略同样验签；旧格式（无签名）缓存直接丢弃。
 * 4. Ed25519 公钥即「服务端身份预共享绑定」，天然抵御 MITM 伪造（私钥仅服务端持有，绝不进仓库）。
 *
 * 服务端部署约束（R1 验收前置）：`policy.draftpeek.com` 对每个响应都须带
 * `X-Policy-Signature: <base64(Ed25519(body))>` 头；否则客户端忽略该响应、沿用安全默认值。
 * 公钥注入：`gradle.properties#policyEd25519PublicKey`（X.509 DER base64）→
 * `core/common` 的 `BuildConfig.POLICY_ED25519_PUBLIC_KEY`。
 *
 * @author DraftPeek Team
 * @since 1.2.0
 */
package com.draftpeek.core.common.security

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.draftpeek.core.common.BuildConfig
import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "RemotePolicyManager"

object RemotePolicyManager {

    /** 策略服务器地址 */
    private const val POLICY_SERVER_URL = "https://policy.draftpeek.com/ai-protection/policy-v1.json"

    /** 本地缓存文件名 */
    private const val CACHE_FILE_NAME = "security_policy.json"

    /** 策略拉取超时（秒） */
    private const val FETCH_TIMEOUT_S = 10L

    /**
     * 远程策略数据模型。
     */
    data class SecurityPolicy(
        /** Kill switch：远程禁用整个应用的安全检测（紧急回滚用） */
        val killSwitchEnabled: Boolean = false,
        /** AI 防护开关 */
        val aiProtectionEnabled: Boolean = true,
        /** 模拟器检测分数阈值 */
        val emulatorDetectionThreshold: Int = 5,
        /** 类加载爆发阈值 */
        val classloadingBurstThreshold: Int = 20,
        /** 反射异常阈值（标准差 ms） */
        val reflectionTimingThreshold: Double = 0.5,
        /** 额外 Frida 特征路径（可被远程更新） */
        val extraFridaPaths: List<String> = emptyList(),
        /** 额外 Xposed 特征类名（可被远程更新） */
        val extraXposedClasses: List<String> = emptyList(),
        /** 策略版本号 */
        val version: Long = 0L,
        /** 策略过期时间（epoch ms） */
        val expiresAt: Long = 0L
    )

    @Volatile
    private var currentPolicy: SecurityPolicy = SecurityPolicy()

    /**
     * 获取当前策略（内存缓存）。
     */
    fun getCurrentPolicy(): SecurityPolicy = currentPolicy

    /**
     * 判断 kill switch 是否激活。
     *
     * 由于 `currentPolicy` 只可能来自「验签通过且未过期」的远程策略或安全的本地默认值，
     * 此处无需再单独判签名——若从未成功拉取过可信策略，默认值 `killSwitchEnabled=false`。
     */
    fun isKillSwitchActive(): Boolean = currentPolicy.killSwitchEnabled

    /**
     * 初始化：从本地缓存加载策略，然后尝试从服务端更新。
     *
     * @param context 应用上下文
     */
    @WorkerThread
    suspend fun initialize(context: Context) {
        // Step 1: 加载本地缓存（已验签才采用）
        loadCachedPolicy(context)
        // Step 2: 尝试从服务端更新（非阻塞，失败不影响启动）
        try {
            fetchPolicyFromServer(context)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch policy from server, using cached version", e)
        }
    }

    /**
     * 从本地缓存加载策略（R1：缓存策略同样必须验签）。
     */
    private fun loadCachedPolicy(context: Context) {
        val envelope = readCacheEnvelope(context) ?: return
        val (body, sig) = envelope
        if (!verifySignature(body, sig)) {
            Log.w(TAG, "Cached policy signature invalid, discarding cache")
            return
        }
        val parsed = parsePolicy(body)
        if (parsed != null && !isExpired(parsed)) {
            currentPolicy = parsed
            Log.i(TAG, "Loaded verified cached policy v${parsed.version}")
        } else {
            Log.w(TAG, "Cached policy expired or invalid, using defaults")
        }
    }

    /**
     * 从服务端拉取最新策略并验证签名（R1：fail-closed）。
     */
    @WorkerThread
    private suspend fun fetchPolicyFromServer(context: Context) = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(FETCH_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(FETCH_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url(POLICY_SERVER_URL)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Policy server returned HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body?.string() ?: return@withContext

                // R1：缺签名 → 直接忽略（fail-closed），杜绝"剥离签名头即可绕过"的 MITM 路径
                val signature = response.header("X-Policy-Signature")
                if (signature.isNullOrEmpty()) {
                    Log.e(TAG, "Policy response missing X-Policy-Signature, ignoring (fail-closed)")
                    return@withContext
                }
                if (!verifySignature(body, signature)) {
                    Log.e(TAG, "Policy signature verification FAILED, ignoring update")
                    return@withContext
                }

                val parsed = parsePolicy(body) ?: return@withContext
                if (isExpired(parsed)) {
                    Log.w(TAG, "Fetched policy is already expired")
                    return@withContext
                }

                currentPolicy = parsed
                // 缓存到本地（含签名，供离线复验）
                writeCache(context, body, signature)
                Log.i(TAG, "Updated policy to v${parsed.version} from server (signature verified)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch policy from server", e)
        }
    }

    /**
     * 解析策略 JSON。
     */
    private fun parsePolicy(json: String): SecurityPolicy? = try {
        val obj = JSONObject(json)
        SecurityPolicy(
            killSwitchEnabled = obj.optBoolean("kill_switch", false),
            aiProtectionEnabled = obj.optBoolean("ai_protection_enabled", true),
            emulatorDetectionThreshold = obj.optInt("emulator_threshold", 5),
            classloadingBurstThreshold = obj.optInt("classloading_burst_threshold", 20),
            reflectionTimingThreshold = obj.optDouble("reflection_timing_threshold", 0.5),
            extraFridaPaths = obj.optJSONArray("extra_frida_paths")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            extraXposedClasses = obj.optJSONArray("extra_xposed_classes")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            version = obj.optLong("version", 0L),
            expiresAt = obj.optLong("expires_at", 0L)
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse policy JSON", e)
        null
    }

    /**
     * 检查策略是否过期。
     */
    private fun isExpired(policy: SecurityPolicy): Boolean {
        if (policy.expiresAt == 0L) return false
        return System.currentTimeMillis() > policy.expiresAt
    }

    /**
     * 验证策略签名（Ed25519，RFC 8032）。
     *
     * 公钥来自构建期注入的 [BuildConfig.POLICY_ED25519_PUBLIC_KEY]（X.509 DER base64）。
     * 公钥缺失（未配置）→ 直接返回 `false`（fail-closed）：宁可忽略策略，也不应用未验真内容。
     */
    private fun verifySignature(payload: String, signatureBase64: String): Boolean {
        val publicKeyDerB64 = BuildConfig.POLICY_ED25519_PUBLIC_KEY
        if (publicKeyDerB64.isBlank()) {
            Log.w(TAG, "Policy Ed25519 public key not configured, rejecting policy (fail-closed)")
            return false
        }
        return verifySignatureWithKey(payload, signatureBase64, publicKeyDerB64)
    }

    /**
     * Ed25519 验签核心（内部可见，便于 JVM 单测覆盖，无需依赖 BuildConfig）。
     *
     * @param payload 待验明文字符串（UTF-8 字节）
     * @param signatureBase64 base64(Ed25519(payload))
     * @param publicKeyDerBase64 X.509 DER base64 公钥
     * @return 验签通过且参数齐备返回 `true`；任何异常 / 算法不可用 → `false`（fail-closed）
     */
    internal fun verifySignatureWithKey(
        payload: String,
        signatureBase64: String,
        publicKeyDerBase64: String
    ): Boolean = try {
        val keyBytes = Base64.getDecoder().decode(publicKeyDerBase64)
        val publicKey = KeyFactory.getInstance("Ed25519")
            .generatePublic(X509EncodedKeySpec(keyBytes))
        val sig = Signature.getInstance("Ed25519")
        sig.initVerify(publicKey)
        sig.update(payload.toByteArray(Charsets.UTF_8))
        sig.verify(Base64.getDecoder().decode(signatureBase64))
    } catch (e: Exception) {
        // 包括：公钥格式错误、Ed25519 算法在当前运行环境不可用（极低版本 Android 未提供 Conscrypt）等。
        // 一律 fail-closed：宁可忽略策略，不应用未验真内容。
        Log.e(TAG, "Policy signature verification error, rejecting policy", e)
        false
    }

    /**
     * 将策略写入本地缓存（信封格式，含签名供离线复验）。
     */
    private fun writeCache(context: Context, body: String, signature: String) {
        try {
            val envelope = JSONObject().apply {
                put("body", body)
                put("sig", signature)
            }
            File(context.filesDir, CACHE_FILE_NAME).writeText(envelope.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache policy", e)
        }
    }

    /**
     * 读取本地缓存信封。旧格式（裸 body 字符串）或无 body/sig 字段 → 返回 null（丢弃）。
     */
    private fun readCacheEnvelope(context: Context): Pair<String, String>? {
        // 注意：函数体不能用表达式体（= try { ... }）——表达式体内禁止裸 return，
        // 而下面的 early-return 需要块体才能编译。
        return try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return null
            val env = JSONObject(file.readText())
            val body = env.optString("body")
            val sig = env.optString("sig")
            if (body.isEmpty() || sig.isEmpty()) {
                Log.w(TAG, "Cache envelope missing body/sig, discarding")
                null
            } else {
                body to sig
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cached policy envelope, discarding", e)
            null
        }
    }
}
