/**
 * DraftPeek APK 签名完整性校验模块。
 *
 * **文件功能**：校验当前 APK 的签名证书指纹是否与发布密钥一致，检测 APK 是否被重新打包或篡改。
 *
 * **主要类/接口**：[ApkIntegrityChecker] - 单例对象，提供 APK 签名校验能力。
 *
 * **模块依赖**：
 * - Android 框架：[PackageManager] 获取签名信息，[Build.VERSION.SDK_INT] 版本兼容
 * - Java 安全库：[MessageDigest] 计算 SHA-256 指纹
 *
 * **安全说明**：合法指纹使用 XOR 0x5A 编码存储，增加逆向定位成本；使用常量时间比较防止时序侧信道。
 */
package com.draftpeek.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import java.security.MessageDigest

/**
 * APK 完整性校验器 —— 校验当前 APK 的签名证书指纹是否与合法指纹一致。
 * 用于检测 APK 是否被重新打包或篡改。
 *
 * ## 安全模型说明（D-02 修复后）
 *
 * 本校验器的**主要**防护来自：
 * 1. Android 系统的 `PackageManager.GET_SIGNING_CERTIFICATES` API 本身
 *    （由系统签名机制背书，攻击者无法在不破坏签名的情况下替换 APK 内容）；
 * 2. R8 混淆（本包未 keep，类名/方法名被混淆，增加静态分析成本）。
 *
 * XOR 编码指纹**仅作为防御纵深**：增加逆向工程师在 smali 层定位指纹常量的
 * 时间成本（从 30 秒增至约 5 分钟）。**不应**视为对抗 hooking 的有效手段——
 * 运行时 hook `decodeFingerprint()` 即可拿到明文。如需更强防护应集成
 * SafetyNet/Play Integrity API。
 *
 * 指纹比较使用 [MessageDigest.isEqual]（常量时间），消除理论 timing 侧信道。
 */
object ApkIntegrityChecker {

    private const val TAG = "ApkIntegrityChecker"

    // ===== 多段滚动密钥编码的合法签名证书 SHA-256 指纹 =====
    // SECURITY VULN-006: 从单字节 XOR 0x5A 升级为 4 段滚动密钥编码。
    // 原始指纹: B9431FE64129965ABFC292C41C4ED6F3AA6731AD4AEBE25B97E4288B91A19E56
    //   ↑ 此指纹为构建期校验值（release.jks 签名证书的 SHA-256，keytool/apksigner 可查），
    //   非密钥/凭据：证书指纹属于公开信息，任何已安装 APK 均可提取，不构成泄露面。
    //   保留明文仅为签名密钥轮换时重编码 ENCODED_FINGERPRINT 的参考；
    //   运行时实际参与校验的是下方 XOR 编码字节数组，非本行明文。
    // 指纹分为 4 段（每段 16 字节），每段使用不同的 XOR 密钥：
    //   段 0 (pos 0-15):  密钥 0x5A — B9431FE64129965A
    //   段 1 (pos 16-31): 密钥 0x3C — BFC292C41C4ED6F3
    //   段 2 (pos 32-47): 密钥 0x7E — AA6731AD4AEBE25B
    //   段 3 (pos 48-63): 密钥 0x29 — 97E4288B91A19E56
    // 密钥字节在运行时通过算术运算派生，避免字节码中出现直接密钥字面量。
    // 频率分析需要同时破解 4 个不同的密钥，难度显著提升。
    // 获取方式: keytool -list -v -keystore release.jks -alias draftpeek
    // 注意: 签名密钥一旦轮换，必须同步更新此处编码字节数组。
    private val ENCODED_FINGERPRINT = byteArrayOf(
        // 段 0 (key=0x5A): B9431FE64129965A
        0x18.toByte(), 0x63.toByte(), 0x6E.toByte(), 0x69.toByte(), 0x6B.toByte(), 0x1C.toByte(), 0x1F.toByte(), 0x6C.toByte(),
        0x6E.toByte(), 0x6B.toByte(), 0x68.toByte(), 0x63.toByte(), 0x63.toByte(), 0x6C.toByte(), 0x6F.toByte(), 0x1B.toByte(),
        // 段 1 (key=0x3C): BFC292C41C4ED6F3
        0x7E.toByte(), 0x7A.toByte(), 0x7F.toByte(), 0x0E.toByte(), 0x05.toByte(), 0x0E.toByte(), 0x7F.toByte(), 0x08.toByte(),
        0x0D.toByte(), 0x7F.toByte(), 0x08.toByte(), 0x79.toByte(), 0x78.toByte(), 0x0A.toByte(), 0x7A.toByte(), 0x0F.toByte(),
        // 段 2 (key=0x7E): AA6731AD4AEBE25B
        0x3F.toByte(), 0x3F.toByte(), 0x48.toByte(), 0x49.toByte(), 0x4D.toByte(), 0x4F.toByte(), 0x3F.toByte(), 0x3A.toByte(),
        0x4A.toByte(), 0x3F.toByte(), 0x3B.toByte(), 0x3C.toByte(), 0x3B.toByte(), 0x4C.toByte(), 0x4B.toByte(), 0x3C.toByte(),
        // 段 3 (key=0x29): 97E4288B91A19E56
        0x10.toByte(), 0x1E.toByte(), 0x6C.toByte(), 0x1D.toByte(), 0x1B.toByte(), 0x11.toByte(), 0x11.toByte(), 0x6B.toByte(),
        0x10.toByte(), 0x18.toByte(), 0x68.toByte(), 0x18.toByte(), 0x10.toByte(), 0x6C.toByte(), 0x1C.toByte(), 0x1F.toByte()
    )

    // VULN-006: 多段滚动密钥 — 4 个不同的 XOR 密钥，运行时通过算术派生
    // 避免字节码中出现连续的密钥字面量
    private val ROLLING_KEYS = intArrayOf(
        (0x2D + 0x2D), // 0x5A — 段 0
        (0x1E + 0x1E), // 0x3C — 段 1
        (0x3F + 0x3F), // 0x7E — 段 2
        (0x14 + 0x15) // 0x29 — 段 3
    )
    private val SEGMENT_SIZE = ENCODED_FINGERPRINT.size / ROLLING_KEYS.size

    /**
     * 运行时解码合法指纹为字节形式。
     * SECURITY: 返回 ByteArray（而非 String）以便用 [MessageDigest.isEqual]
     * 进行常量时间比较，避免 String.equals 的 timing 侧信道。
     *
     * VULN-006: 使用多段滚动密钥解码，每个段使用不同的 XOR 密钥。
     */
    private fun decodeFingerprintBytes(): ByteArray = ByteArray(ENCODED_FINGERPRINT.size) { i ->
        val keyIndex = i / SEGMENT_SIZE
        (ENCODED_FINGERPRINT[i].toInt() xor ROLLING_KEYS[keyIndex]).toByte()
    }

    /**
     * 完整性校验结果状态，供外部模块查询
     */
    @Volatile
    var isVerified: Boolean = false
        private set

    /**
     * 校验结果
     */
    sealed class IntegrityResult {
        /** 校验通过：签名一致 */
        data object Verified : IntegrityResult()

        /** 校验失败：签名不一致，可能被篡改 */
        data class Tampered(val currentHash: String) : IntegrityResult()

        /** 校验异常：无法读取签名信息 */
        data class Error(val exception: Exception) : IntegrityResult()
    }

    /**
     * 校验当前 APK 的签名证书。
     *
     * SECURITY: 指纹比较使用 [MessageDigest.isEqual]（常量时间），
     * 消除理论 timing 侧信道。本地校验场景下风险等级低，但遵循 D9 纵深防御。
     */
    @WorkerThread
    fun verify(context: Context): IntegrityResult {
        return try {
            // ===== 增强 1：Debuggable 标志独立检测 =====
            // 不依赖 BuildConfig.DEBUG（可被攻击者修改），直接检查 ApplicationInfo 标志
            val appInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            ).applicationInfo
            if (appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                isVerified = false
                return IntegrityResult.Tampered("FLAG_DEBUGGABLE set on release build")
            }

            // ===== 增强 2：获取所有签名并逐一验证 =====
            val allSignatures = getAllSignatures(context)
            if (allSignatures.isEmpty()) {
                isVerified = false
                return IntegrityResult.Error(IllegalStateException("No signatures found"))
            }

            // 预期签名者数量为 1（release.jks 单一签名）
            val expectedSignerCount = 1
            if (allSignatures.size != expectedSignerCount) {
                isVerified = false
                return IntegrityResult.Tampered(
                    "Unexpected signer count: ${allSignatures.size} (expected $expectedSignerCount)"
                )
            }

            // ===== 增强 3：对每个签名者进行指纹验证 =====
            val expected = decodeFingerprintBytes()
            for (signature in allSignatures) {
                val currentFingerprint = computeSha256Fingerprint(signature)
                val verified = MessageDigest.isEqual(
                    currentFingerprint.toByteArray(Charsets.US_ASCII),
                    expected
                )
                if (!verified) {
                    isVerified = false
                    return IntegrityResult.Tampered(currentFingerprint)
                }
            }

            // ===== 增强 4：安装来源验证 =====
            val installerPkg = getInstallerPackageName(context)
            val suspiciousInstallers = setOf(
                "com.android.r8",
                "de.robv.android.xposed.installer",
                "com.android.shellms"
            )
            if (installerPkg != null && installerPkg in suspiciousInstallers) {
                isVerified = false
                return IntegrityResult.Tampered("Suspicious installer: $installerPkg")
            }

            isVerified = true
            IntegrityResult.Verified
        } catch (e: Exception) {
            isVerified = false
            IntegrityResult.Error(e)
        }
    }

    /**
     * 轻量级签名校验点 —— 用于在关键操作前进行快速校验。
     *
     * 增强：不再无条件信任缓存结果，每次以小概率触发重新验证，
     * 防止攻击者 hook 一次 verify() 后永久绕过。
     */
    @WorkerThread
    fun spotCheck(context: Context): Boolean {
        if (isVerified) {
            // 10% 概率触发重新验证，防止一次性 hook 永久绕过
            if (System.nanoTime() % 10 == 0L) {
                return verify(context) is IntegrityResult.Verified
            }
            return true
        }
        return when (verify(context)) {
            is IntegrityResult.Verified -> true
            else -> false
        }
    }

    /**
     * 获取当前 APK 的所有签名证书列表。
     *
     * 增强：返回所有签名者，而非仅第一个，防止多签名绕过攻击。
     */
    @WorkerThread
    private fun getAllSignatures(context: Context): List<Signature> {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        return signatures?.toList() ?: emptyList()
    }

    /**
     * 计算单个签名证书的 SHA-256 指纹（大写十六进制字符串）。
     */
    @WorkerThread
    private fun computeSha256Fingerprint(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(signature.toByteArray())
        return hash.joinToString("") { "%02X".format(it) }
    }

    /**
     * 获取安装来源包名。
     *
     * 增强：检测可疑安装来源（Xposed、R8 等），增加重打包检测维度。
     */
    private fun getInstallerPackageName(context: Context): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(
                context.packageName
            ).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(
                context.packageName
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to get installer package name", e)
        null
    }
}
