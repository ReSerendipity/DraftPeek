package com.draftpeek.core.common.plugin.security

/**
 * 信任清单匹配策略。
 *
 * 纯函数实现，便于单测覆盖所有拒绝分支；不直接依赖 Android API。
 */
internal object PluginTrustPolicy {

    fun evaluate(
        store: PluginTrustStore,
        identity: PluginIdentity,
        fileSha256: String,
    ): PluginVerificationResult {
        val normalizedHash = fileSha256.lowercase()
        return when (identity) {
            is PluginIdentity.Apk -> evaluateApk(store, identity, normalizedHash)
            PluginIdentity.Dex -> evaluateDex(store, normalizedHash)
        }
    }

    private fun evaluateApk(
        store: PluginTrustStore,
        identity: PluginIdentity.Apk,
        fileSha256: String,
    ): PluginVerificationResult {
        if (identity.packageName.isBlank()) {
            return PluginVerificationResult.Denied("APK 包名为空")
        }
        if (identity.signerSha256.isEmpty()) {
            return PluginVerificationResult.Denied("APK 未签名")
        }

        val candidates = store.findByPackage(identity.packageName)
        if (candidates.isEmpty()) {
            return PluginVerificationResult.Denied(
                "包名 ${identity.packageName} 未在插件信任清单中",
            )
        }

        val signerSet = identity.signerSha256.map { it.lowercase() }.toSet()
        for (entry in candidates) {
            if (entry.fileSha256 != null && entry.fileSha256 != fileSha256) {
                continue
            }
            if (entry.certificateSha256.isEmpty()) {
                continue
            }
            // 要求 APK 的每一个签名者都在白名单内，避免“额外签名者”绕过固定。
            if (signerSet.all { it in entry.certificateSha256 }) {
                return PluginVerificationResult.Allowed(entry)
            }
        }

        return PluginVerificationResult.Denied(
            "APK 签名或文件哈希与信任清单不匹配",
        )
    }

    private fun evaluateDex(
        store: PluginTrustStore,
        fileSha256: String,
    ): PluginVerificationResult {
        val match = store.findDexByHash(fileSha256).firstOrNull()
            ?: return PluginVerificationResult.Denied(
                "DEX 文件哈希未在插件信任清单中",
            )
        return PluginVerificationResult.Allowed(match)
    }
}
