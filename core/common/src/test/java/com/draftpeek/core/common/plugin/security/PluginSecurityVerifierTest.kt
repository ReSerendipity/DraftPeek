package com.draftpeek.core.common.plugin.security

import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("PluginSecurityVerifier")
class PluginSecurityVerifierTest {

    private val trustedCert = "a".repeat(64)
    private val otherCert = "b".repeat(64)
    private val wrongHash = "0".repeat(64)

    @TempDir
    lateinit var tempDir: Path

    private fun writePlugin(name: String, content: String): File {
        return tempDir.resolve(name).toFile().apply { writeText(content) }
    }

    private fun trustStoreWithApk(
        packageName: String = "com.example.plugin",
        cert: String = trustedCert,
        fileSha256: String? = null,
    ): PluginTrustStore {
        val fileLine = fileSha256?.let { ",\"fileSha256\":\"$it\"" } ?: ""
        return PluginTrustStore.parse(
            """
            {
              "version": 1,
              "plugins": [
                {
                  "id": "demo.apk",
                  "packageName": "$packageName",
                  "certSha256": ["$cert"]$fileLine
                }
              ]
            }
            """.trimIndent(),
        )
    }

    @Test
    @DisplayName("allows an APK whose package, signer and hash all match")
    fun allowsTrustedApk() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val hash = PluginSecurityVerifier.sha256Hex(apk)
        val store = trustStoreWithApk(fileSha256 = hash)
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(trustedCert))
            },
        )

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Allowed)
    }

    @Test
    @DisplayName("denies an APK with an unknown signer")
    fun deniesUnknownSigner() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(otherCert))
            },
        )

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK carrying an extra untrusted signer")
    fun deniesExtraSigner() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk(
                    "com.example.plugin",
                    setOf(trustedCert, otherCert),
                )
            },
        )

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK with an unlisted package name")
    fun deniesUnlistedPackage() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.other.plugin", setOf(trustedCert))
            },
        )

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK whose file hash is pinned differently")
    fun deniesHashMismatch() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk(fileSha256 = wrongHash)
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(trustedCert))
            },
        )

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK when the inspector cannot read signatures")
    fun deniesUnreadableApk() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = PluginSecurityVerifier(store, ApkInspector { null })

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("allows a DEX whose file hash is allow-listed")
    fun allowsTrustedDex() {
        val dex = writePlugin("demo.dex", "trusted-dex-content")
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = PluginTrustStore.parse(
            """
            {
              "version": 1,
              "plugins": [
                { "id": "demo.dex", "fileSha256": "$hash" }
              ]
            }
            """.trimIndent(),
        )
        val verifier = PluginSecurityVerifier(store, ApkInspector { null })

        val result = verifier.verify(dex)

        assertTrue(result is PluginVerificationResult.Allowed)
    }

    @Test
    @DisplayName("denies an unlisted DEX")
    fun deniesUnlistedDex() {
        val dex = writePlugin("demo.dex", "untrusted-dex-content")
        val verifier = PluginSecurityVerifier(PluginTrustStore.EMPTY, ApkInspector { null })

        val result = verifier.verify(dex)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an empty plugin file")
    fun deniesEmptyFile() {
        val apk = writePlugin("empty.apk", "")
        val verifier = PluginSecurityVerifier(PluginTrustStore.EMPTY, ApkInspector { null })

        val result = verifier.verify(apk)

        assertTrue(result is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies unsupported plugin extensions")
    fun deniesUnsupportedExtension() {
        val jar = writePlugin("demo.jar", "not-a-plugin-format")
        val verifier = PluginSecurityVerifier(PluginTrustStore.EMPTY, ApkInspector { null })

        val result = verifier.verify(jar)

        assertTrue(result is PluginVerificationResult.Denied)
    }
}
