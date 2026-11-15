package com.draftpeek.core.common.plugin.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Android 实现的 APK 签名读取器。
 *
 * API 28+ 使用 `GET_SIGNING_CERTIFICATES` / `apkContentsSigners`；
 * API 26-27 回退到 `GET_SIGNATURES` / `signatures`。
 * 任何解析失败都返回 null，由 [PluginSecurityVerifier] 按 fail-closed 拒绝。
 */
@Suppress("DEPRECATION")
class AndroidApkInspector(private val context: Context) : ApkInspector {

    override fun inspect(apk: File): PluginIdentity.Apk? {
        return try {
            val packageManager = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val info = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
                ?: return null

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                info.signatures
            } ?: return null
            if (signatures.isEmpty()) return null

            val hashes = signatures
                .map { PluginSecurityVerifier.sha256Hex(it.toByteArray()) }
                .toSet()
            PluginIdentity.Apk(
                packageName = info.packageName,
                signerSha256 = hashes
            )
        } catch (e: Exception) {
            null
        }
    }
}
