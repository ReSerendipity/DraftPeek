package com.draftpeek.core.common.plugin.security

import java.io.File
import java.security.MessageDigest

/**
 * 插件加载前的安全校验器。
 *
 * 校验顺序：
 * 1. 必须是应用私有插件目录内的普通文件（由调用方做目录约束）；
 * 2. 文件非空且不超过 [MAX_PLUGIN_FILE_BYTES]；
 * 3. 计算文件 SHA-256；
 * 4. APK 读取包名与签名证书，DEX 仅按文件哈希；
 * 5. 与 [PluginTrustStore] 做 fail-closed 匹配。
 *
 * 该类不依赖 Android API，APK 信息由 [ApkInspector] 注入，便于单测。
 */
class PluginSecurityVerifier(
    private val trustStore: PluginTrustStore,
    private val apkInspector: ApkInspector,
) {

    fun verify(file: File): PluginVerificationResult {
        if (!file.isFile) {
            return PluginVerificationResult.Denied("插件路径不是普通文件")
        }
        val length = file.length()
        if (length <= 0L) {
            return PluginVerificationResult.Denied("插件文件为空")
        }
        if (length > MAX_PLUGIN_FILE_BYTES) {
            return PluginVerificationResult.Denied(
                "插件文件超过 ${MAX_PLUGIN_FILE_BYTES / 1024 / 1024} MB 上限",
            )
        }

        val fileSha256 = try {
            sha256Hex(file)
        } catch (e: Exception) {
            return PluginVerificationResult.Denied(
                "无法计算插件文件 SHA-256: ${e.message ?: e.javaClass.simpleName}",
            )
        }

        return when (file.extension.lowercase()) {
            "apk" -> {
                val identity = try {
                    apkInspector.inspect(file)
                } catch (e: Exception) {
                    null
                } ?: return PluginVerificationResult.Denied(
                    "无法读取 APK 包名或签名信息",
                )
                PluginTrustPolicy.evaluate(trustStore, identity, fileSha256)
            }

            "dex" -> PluginTrustPolicy.evaluate(
                trustStore,
                PluginIdentity.Dex,
                fileSha256,
            )

            else -> PluginVerificationResult.Denied("仅支持 .apk / .dex 插件")
        }
    }

    companion object {
        /** 单插件文件上限：50 MB。 */
        const val MAX_PLUGIN_FILE_BYTES = 50L * 1024 * 1024

        private const val HEX = "0123456789abcdef"

        fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    if (read > 0) digest.update(buffer, 0, read)
                }
            }
            return toHex(digest.digest())
        }

        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return toHex(digest.digest(bytes))
        }

        private fun toHex(bytes: ByteArray): String {
            val builder = StringBuilder(bytes.size * 2)
            for (byte in bytes) {
                val value = byte.toInt() and 0xFF
                builder.append(HEX[value ushr 4])
                builder.append(HEX[value and 0x0F])
            }
            return builder.toString()
        }
    }
}
