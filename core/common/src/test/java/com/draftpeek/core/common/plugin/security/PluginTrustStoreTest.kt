package com.draftpeek.core.common.plugin.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PluginTrustStore")
class PluginTrustStoreTest {

    private val certSha256 = "a".repeat(64)
    private val fileSha256 = "b".repeat(64)

    private fun manifest(plugins: String, restricted: String = "[]"): String = """
            {
              "version": 2,
              "restrictedClasses": $restricted,
              "plugins": [$plugins]
            }
    """.trimIndent()

    @Test
    @DisplayName("parses an APK entry with allowed class prefixes")
    fun parsesApkEntry() {
        val store = PluginTrustStore.parse(
            manifest(
                """
                {
                  "id": "demo.apk",
                  "packageName": "com.example.plugin",
                  "certSha256": ["$certSha256"],
                  "fileSha256": "$fileSha256",
                  "allowedClassPrefixes": ["com.example.plugin"]
                }
                """.trimIndent()
            )
        )

        assertEquals(1, store.entries.size)
        val entry = store.entries.single()
        assertEquals("demo.apk", entry.id)
        assertEquals("com.example.plugin", entry.packageName)
        assertEquals(setOf(certSha256), entry.certificateSha256)
        assertEquals(fileSha256, entry.fileSha256)
        assertEquals(listOf("com.example.plugin"), entry.allowedClassPrefixes)
        assertTrue(store.restrictedClasses.isEmpty())
        assertEquals(listOf(entry), store.findByPackage("com.example.plugin"))
    }

    @Test
    @DisplayName("parses a DEX entry and normalizes slash prefixes")
    fun parsesDexEntry() {
        val store = PluginTrustStore.parse(
            manifest(
                """
                {
                  "id": "demo.dex",
                  "fileSha256": "$fileSha256",
                  "allowedClassPrefixes": ["com/example/plugin"]
                }
                """.trimIndent()
            )
        )

        val entry = store.entries.single()
        assertNull(entry.packageName)
        assertTrue(entry.certificateSha256.isEmpty())
        assertEquals(listOf("com.example.plugin"), entry.allowedClassPrefixes)
        assertEquals(listOf(entry), store.findDexByHash(fileSha256))
    }

    @Test
    @DisplayName("parses and normalizes global restrictedClasses")
    fun parsesRestrictedClasses() {
        val store = PluginTrustStore.parse(
            manifest(
                plugins = "",
                restricted = """
                    [
                      "java.lang.Runtime",
                      "java/lang/ProcessBuilder",
                      "Ljava/lang/reflect/Method;"
                    ]
                """.trimIndent()
            )
        )

        assertEquals(
            setOf(
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.lang.reflect.Method"
            ),
            store.restrictedClasses
        )
    }

    @Test
    @DisplayName("empty plugin list yields an empty store")
    fun emptyPluginList() {
        val store = PluginTrustStore.parse(
            """{"version":2,"restrictedClasses":[],"plugins":[]}"""
        )
        assertTrue(store.entries.isEmpty())
        assertTrue(store.restrictedClasses.isEmpty())
    }

    @Test
    @DisplayName("requires restrictedClasses")
    fun rejectsMissingRestrictedClasses() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse("""{"version":2,"plugins":[]}""")
        }
    }

    @Test
    @DisplayName("requires allowedClassPrefixes for every entry")
    fun rejectsMissingAllowedPrefixes() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """{"id":"bad","fileSha256":"$fileSha256"}"""
                )
            )
        }
    }

    @Test
    @DisplayName("rejects an empty allowedClassPrefixes array")
    fun rejectsEmptyAllowedPrefixes() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {
                      "id": "bad",
                      "fileSha256": "$fileSha256",
                      "allowedClassPrefixes": []
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects wildcard class prefixes")
    fun rejectsWildcardPrefix() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {
                      "id": "bad",
                      "fileSha256": "$fileSha256",
                      "allowedClassPrefixes": ["com.example.*"]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects a malformed certificate hash")
    fun rejectsMalformedCertificateHash() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {
                      "id": "bad",
                      "packageName": "com.example.plugin",
                      "certSha256": ["not-a-sha256"],
                      "allowedClassPrefixes": ["com.example.plugin"]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects an APK entry without certificates")
    fun rejectsApkEntryWithoutCertificates() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {
                      "id": "bad",
                      "packageName": "com.example.plugin",
                      "allowedClassPrefixes": ["com.example.plugin"]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects a DEX entry without a file hash")
    fun rejectsDexEntryWithoutFileHash() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {
                      "id": "bad",
                      "allowedClassPrefixes": ["com.example.plugin"]
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects duplicate entry ids")
    fun rejectsDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    """
                    {"id":"dup","fileSha256":"$fileSha256","allowedClassPrefixes":["com.example.plugin"]},
                    {"id":"dup","fileSha256":"$fileSha256","allowedClassPrefixes":["com.example.plugin"]}
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    @DisplayName("rejects an unsupported manifest version")
    fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                """{"version":3,"restrictedClasses":[],"plugins":[]}"""
            )
        }
    }

    @Test
    @DisplayName("rejects an invalid restricted class")
    fun rejectsInvalidRestrictedClass() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                manifest(
                    plugins = "",
                    restricted = """["not a class"]"""
                )
            )
        }
    }
}
