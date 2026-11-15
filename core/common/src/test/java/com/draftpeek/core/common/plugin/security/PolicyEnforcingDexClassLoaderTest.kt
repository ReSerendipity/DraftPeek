package com.draftpeek.core.common.plugin.security

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("PolicyEnforcingDexClassLoader")
class PolicyEnforcingDexClassLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    private val entry = PluginTrustEntry(
        id = "demo.dex",
        packageName = null,
        certificateSha256 = emptySet(),
        fileSha256 = "b".repeat(64),
        allowedClassPrefixes = listOf("com.example.plugin"),
    )
    private val policy = PluginRuntimePolicy(
        entry = entry,
        restrictedClasses = setOf("java.lang.Runtime"),
    )

    @Test
    @DisplayName("denies a class outside allowedClassPrefixes")
    fun deniesClassOutsidePrefix() {
        val loader = newLoader()

        assertThrows(PluginClassAccessDeniedException::class.java) {
            loader.loadClass("com.evil.Main")
        }
    }

    @Test
    @DisplayName("denies a restricted class even when the parent can load it")
    fun deniesRestrictedClassBeforeParent() {
        val loader = newLoader()

        assertThrows(PluginClassAccessDeniedException::class.java) {
            loader.loadClass("java.lang.Runtime")
        }
    }

    @Test
    @DisplayName("denies a restricted array type")
    fun deniesRestrictedArrayType() {
        val loader = newLoader()

        assertThrows(PluginClassAccessDeniedException::class.java) {
            loader.loadClass("[Ljava.lang.Runtime;")
        }
    }

    @Test
    @DisplayName("still allows host classes through the parent loader")
    fun allowsHostClass() {
        val loader = newLoader()

        val clazz = loader.loadClass("java.lang.String")

        assertEquals("java.lang.String", clazz.name)
    }

    @Test
    @DisplayName("does not raise a policy denial for an allowed plugin class")
    fun allowedPluginClassIsNotPolicyDenied() {
        val loader = newLoader()

        val error = runCatching {
            loader.loadClass("com.example.plugin.Main")
        }.exceptionOrNull()

        if (error != null) {
            assertFalse(error is PluginClassAccessDeniedException)
        }
    }

    private fun newLoader(): PolicyEnforcingDexClassLoader {
        val dexFile = tempDir.resolve("plugin.dex").toFile().apply {
            writeBytes(DexTestData.minimalDex())
        }
        val optimizedDir = tempDir.resolve("opt").toFile().apply { mkdirs() }
        return PolicyEnforcingDexClassLoader(
            dexPath = dexFile.absolutePath,
            optimizedDirectory = optimizedDir.absolutePath,
            librarySearchPath = null,
            parent = javaClass.classLoader,
            policy = policy,
        )
    }
}
