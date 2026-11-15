package com.draftpeek.core.common.plugin.security

import java.io.File

/** 插件在加载前可被识别的身份。 */
sealed interface PluginIdentity {

    /** APK 插件：包名 + 全部签名证书 SHA-256。 */
    data class Apk(val packageName: String, val signerSha256: Set<String>) : PluginIdentity

    /** DEX 插件：无包名/签名，只能靠文件哈希固定。 */
    data object Dex : PluginIdentity
}

/** 从 APK 读取包名与签名证书；返回 null 表示无法安全解析。 */
fun interface ApkInspector {
    fun inspect(apk: File): PluginIdentity.Apk?
}

/** 插件校验结果。 */
sealed interface PluginVerificationResult {

    /** 命中信任清单。 */
    data class Allowed(val entry: PluginTrustEntry) : PluginVerificationResult

    /** 拒绝加载，并给出可日志化的原因。 */
    data class Denied(val reason: String) : PluginVerificationResult
}
