package com.draftpeek.core.common.plugin.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PluginTrustStore")
class PluginTrustStoreTest {

    private val certSha256 = "a".repeat(64)
    private val fileSha256 = "b".repeat(64)

    @Test
    @DisplayName("parses an APK entry and keeps SHA-256 values")
    fun parsesApkEntry() {
        val store = PluginTrustStore.parse(
            """
            {
              "version": 1,
              "plugins": [
                {
                  "id": "demo.apk",
                  "packageName": "com.example.plugin",
                  "certSha256": ["$certSha256"],
                  "fileSha256": "$fileSha256"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, store.entries.size)
        val entry = store.entries.single()
        assertEquals("demo.apk", entry.id)
        assertEquals("com.example.plugin", entry.packageName)
        assertEquals(setOf(certSha256), entry.certificateSha256)
        assertEquals(fileSha256, entry.fileSha256)
        assertEquals(listOf(entry), store.findByPackage("com.example.plugin"))
    }

    @Test
    @DisplayName("parses a DEX entry without a package name")
    fun parsesDexEntry() {
        val store = PluginTrustStore.parse(
            """
            {
              "version": 1,
              "plugins": [
                { "id": "demo.dex", "fileSha256": "$fileSha256" }
              ]
            }
            """.trimIndent(),
        )

        val entry = store.entries.single()
        assertNull(entry.packageName)
        assertTrue(entry.certificateSha256.isEmpty())
        assertEquals(listOf(entry), store.findDexByHash(fileSha256))
    }

    @Test
    @DisplayName("empty plugin list yields an empty store")
    fun emptyPluginList() {
        val store = PluginTrustStore.parse("""{"version":1,"plugins":[]}""")
        assertTrue(store.entries.isEmpty())
    }

    @Test
    @DisplayName("rejects a malformed certificate hash")
    fun rejectsMalformedCertificateHash() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                """
                {
                  "version": 1,
                  "plugins": [
                    {
                      "id": "bad",
                      "packageName": "com.example.plugin",
                      "certSha256": ["not-a-sha256"]
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    @DisplayName("rejects an APK entry without certificates")
    fun rejectsApkEntryWithoutCertificates() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                """
                {
                  "version": 1,
                  "plugins": [
                    { "id": "bad", "packageName": "com.example.plugin" }
                  ]
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    @DisplayName("rejects a DEX entry without a file hash")
    fun rejectsDexEntryWithoutFileHash() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                """
                {
                  "version": 1,
                  "plugins": [
                    { "id": "bad" }
                  ]
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    @DisplayName("rejects duplicate entry ids")
    fun rejectsDuplicateIds() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse(
                """
                {
                  "version": 1,
                  "plugins": [
                    { "id": "dup", "fileSha256": "$fileSha256" },
                    { "id": "dup", "fileSha256": "$fileSha256" }
                  ]
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    @DisplayName("rejects an unsupported manifest version")
    fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            PluginTrustStore.parse("""{"version":2,"plugins":[]}""")
        }
    }
}
