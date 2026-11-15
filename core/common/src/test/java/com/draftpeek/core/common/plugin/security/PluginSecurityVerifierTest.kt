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

    private val fakeSuccessScan = DexScanResult.Success(
        definedClasses = listOf("Lcom/example/plugin/Main;"),
        referencedTypes = setOf("Lcom/example/plugin/Main;")
    )

    @TempDir
    lateinit var tempDir: Path

    private fun writePlugin(name: String, content: String): File = tempDir.resolve(name).toFile().apply {
        writeText(content)
    }

    private fun writeBytes(name: String, bytes: ByteArray): File = tempDir.resolve(name).toFile().apply {
        writeBytes(bytes)
    }

    private fun fakeScanner(): PluginDexScanner = PluginDexScanner { fakeSuccessScan }

    private fun verifier(
        store: PluginTrustStore,
        inspector: ApkInspector,
        scanner: PluginDexScanner = fakeScanner()
    ): PluginSecurityVerifier = PluginSecurityVerifier(store, inspector, scanner)

    private fun trustStoreWithApk(
        packageName: String = "com.example.plugin",
        cert: String = trustedCert,
        fileSha256: String? = null
    ): PluginTrustStore {
        val fileLine = fileSha256?.let { ",\"fileSha256\":\"$it\"" } ?: ""
        return PluginTrustStore.parse(
            """
            {
              "version": 2,
              "restrictedClasses": [],
              "plugins": [
                {
                  "id": "demo.apk",
                  "packageName": "$packageName",
                  "certSha256": ["$cert"]$fileLine,
                  "allowedClassPrefixes": ["com.example.plugin"]
                }
              ]
            }
            """.trimIndent()
        )
    }

    private fun trustStoreWithDex(
        fileSha256: String,
        prefixes: List<String> = listOf("com.example.plugin"),
        restricted: List<String> = emptyList()
    ): PluginTrustStore {
        val restrictedJson = restricted.joinToString(",") { "\"$it\"" }
        val prefixesJson = prefixes.joinToString(",") { "\"$it\"" }
        return PluginTrustStore.parse(
            """
            {
              "version": 2,
              "restrictedClasses": [$restrictedJson],
              "plugins": [
                {
                  "id": "demo.dex",
                  "fileSha256": "$fileSha256",
                  "allowedClassPrefixes": [$prefixesJson]
                }
              ]
            }
            """.trimIndent()
        )
    }

    @Test
    @DisplayName("allows an APK whose package, signer and hash all match")
    fun allowsTrustedApk() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val hash = PluginSecurityVerifier.sha256Hex(apk)
        val store = trustStoreWithApk(fileSha256 = hash)
        val verifier = verifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(trustedCert))
            }
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Allowed)
    }

    @Test
    @DisplayName("denies an APK with an unknown signer")
    fun deniesUnknownSigner() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = verifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(otherCert))
            }
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK carrying an extra untrusted signer")
    fun deniesExtraSigner() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = verifier(
            store,
            ApkInspector {
                PluginIdentity.Apk(
                    "com.example.plugin",
                    setOf(trustedCert, otherCert)
                )
            }
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK with an unlisted package name")
    fun deniesUnlistedPackage() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = verifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.other.plugin", setOf(trustedCert))
            }
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK whose file hash is pinned differently")
    fun deniesHashMismatch() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk(fileSha256 = wrongHash)
        val verifier = verifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(trustedCert))
            }
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK when the inspector cannot read signatures")
    fun deniesUnreadableApk() {
        val apk = writePlugin("demo.apk", "trusted-apk-content")
        val store = trustStoreWithApk()
        val verifier = verifier(store, ApkInspector { null })

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("allows a DEX when trust hash and fake class scan both pass")
    fun allowsTrustedDexWithScanner() {
        val dex = writePlugin("demo.dex", "trusted-dex-content")
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = trustStoreWithDex(hash)
        val verifier = verifier(store, ApkInspector { null })

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Allowed)
    }

    @Test
    @DisplayName("allows a real DEX whose classes stay under allowedClassPrefixes")
    fun allowsDexWithAllowedClass() {
        val dex = writeBytes("demo.dex", DexTestData.minimalDex())
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = trustStoreWithDex(hash)
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector { null },
            DefaultPluginDexScanner
        )

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Allowed)
    }

    @Test
    @DisplayName("denies a real DEX defining a class outside allowedClassPrefixes")
    fun deniesDexWithClassOutsidePrefix() {
        val dex = writeBytes(
            "demo.dex",
            DexTestData.minimalDex(definedClass = "Lcom/evil/Main;")
        )
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = trustStoreWithDex(hash)
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector { null },
            DefaultPluginDexScanner
        )

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies a real DEX referencing a restricted class")
    fun deniesDexReferencingRestrictedClass() {
        val dex = writeBytes(
            "demo.dex",
            DexTestData.minimalDex(extraType = "Ljava/lang/Runtime;")
        )
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = trustStoreWithDex(
            fileSha256 = hash,
            restricted = listOf("java.lang.Runtime")
        )
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector { null },
            DefaultPluginDexScanner
        )

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies malformed DEX bytes after trust hash matches")
    fun deniesMalformedDex() {
        val dex = writePlugin("demo.dex", "not a dex")
        val hash = PluginSecurityVerifier.sha256Hex(dex)
        val store = trustStoreWithDex(hash)
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector { null },
            DefaultPluginDexScanner
        )

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an APK whose DEX entries cannot be scanned")
    fun deniesApkWhenDexScanFails() {
        val apk = writePlugin("demo.apk", "not a zip")
        val store = trustStoreWithApk()
        val verifier = PluginSecurityVerifier(
            store,
            ApkInspector {
                PluginIdentity.Apk("com.example.plugin", setOf(trustedCert))
            },
            DefaultPluginDexScanner
        )

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an unlisted DEX")
    fun deniesUnlistedDex() {
        val dex = writePlugin("demo.dex", "untrusted-dex-content")
        val verifier = verifier(PluginTrustStore.EMPTY, ApkInspector { null })

        assertTrue(verifier.verify(dex) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies an empty plugin file")
    fun deniesEmptyFile() {
        val apk = writePlugin("empty.apk", "")
        val verifier = verifier(PluginTrustStore.EMPTY, ApkInspector { null })

        assertTrue(verifier.verify(apk) is PluginVerificationResult.Denied)
    }

    @Test
    @DisplayName("denies unsupported plugin extensions")
    fun deniesUnsupportedExtension() {
        val jar = writePlugin("demo.jar", "not-a-plugin-format")
        val verifier = verifier(PluginTrustStore.EMPTY, ApkInspector { null })

        assertTrue(verifier.verify(jar) is PluginVerificationResult.Denied)
    }
}
