package com.draftpeek.core.common.plugin.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PluginClassPolicy")
class PluginClassPolicyTest {

    private val entry = PluginTrustEntry(
        id = "demo.dex",
        packageName = null,
        certificateSha256 = emptySet(),
        fileSha256 = "b".repeat(64),
        allowedClassPrefixes = listOf("com.example.plugin"),
    )
    private val restricted = setOf("java.lang.Runtime", "java.lang.reflect.Method")

    @Test
    @DisplayName("allows a defined class under the configured prefix")
    fun allowsDefinedClassUnderPrefix() {
        val scan = DexScanResult.Success(
            definedClasses = listOf("Lcom/example/plugin/Main;"),
            referencedTypes = setOf("Lcom/example/plugin/Main;"),
        )

        val decision = PluginClassPolicy.evaluateDex(scan, entry, restricted)

        assertTrue(decision is ClassPolicyDecision.Allowed)
    }

    @Test
    @DisplayName("denies a defined class outside the configured prefix")
    fun deniesDefinedClassOutsidePrefix() {
        val scan = DexScanResult.Success(
            definedClasses = listOf("Lcom/evil/Main;"),
            referencedTypes = setOf("Lcom/evil/Main;"),
        )

        val decision = PluginClassPolicy.evaluateDex(scan, entry, restricted)

        assertTrue(decision is ClassPolicyDecision.Denied)
    }

    @Test
    @DisplayName("denies a defined class that is restricted")
    fun deniesDefinedRestrictedClass() {
        val scan = DexScanResult.Success(
            definedClasses = listOf("Ljava/lang/Runtime;"),
            referencedTypes = setOf("Ljava/lang/Runtime;"),
        )

        val decision = PluginClassPolicy.evaluateDex(scan, entry, restricted)

        assertTrue(decision is ClassPolicyDecision.Denied)
    }

    @Test
    @DisplayName("denies a DEX referencing a restricted type")
    fun deniesReferencedRestrictedType() {
        val scan = DexScanResult.Success(
            definedClasses = listOf("Lcom/example/plugin/Main;"),
            referencedTypes = setOf(
                "Lcom/example/plugin/Main;",
                "Ljava/lang/reflect/Method;",
            ),
        )

        val decision = PluginClassPolicy.evaluateDex(scan, entry, restricted)

        assertTrue(decision is ClassPolicyDecision.Denied)
    }

    @Test
    @DisplayName("denies empty allowedClassPrefixes")
    fun deniesEmptyPrefixes() {
        val scan = DexScanResult.Success(
            definedClasses = listOf("Lcom/example/plugin/Main;"),
            referencedTypes = emptySet(),
        )

        val decision = PluginClassPolicy.evaluateDex(
            scan,
            entry.copy(allowedClassPrefixes = emptyList()),
            restricted,
        )

        assertTrue(decision is ClassPolicyDecision.Denied)
    }

    @Test
    @DisplayName("denies a DEX without defined classes")
    fun deniesEmptyDefinedClasses() {
        val decision = PluginClassPolicy.evaluateDex(
            DexScanResult.Success(emptyList(), emptySet()),
            entry,
            restricted,
        )

        assertTrue(decision is ClassPolicyDecision.Denied)
    }

    @Test
    @DisplayName("runtime check denies a restricted class before parent lookup")
    fun runtimeDeniesRestrictedClass() {
        assertTrue(
            PluginClassPolicy.checkRestricted("java.lang.Runtime", restricted)
                is ClassPolicyDecision.Denied,
        )
    }

    @Test
    @DisplayName("runtime check treats array descriptor as its base type")
    fun runtimeDeniesRestrictedArray() {
        assertTrue(
            PluginClassPolicy.checkRestricted("[Ljava.lang.Runtime;", restricted)
                is ClassPolicyDecision.Denied,
        )
    }

    @Test
    @DisplayName("runtime check denies a class outside the prefix")
    fun runtimeDeniesOutsidePrefix() {
        assertTrue(
            PluginClassPolicy.checkAllowedClass("com.evil.Main", entry, restricted)
                is ClassPolicyDecision.Denied,
        )
    }

    @Test
    @DisplayName("runtime check allows an inner class under the prefix")
    fun runtimeAllowsInnerClass() {
        assertTrue(
            PluginClassPolicy.checkAllowedClass(
                "com.example.plugin.Main\$Inner",
                entry,
                restricted,
            ) is ClassPolicyDecision.Allowed,
        )
    }

    @Test
    @DisplayName("prefix matches on package/class boundaries only")
    fun prefixBoundary() {
        assertEquals(
            false,
            PluginClassNames.isUnderPrefixes(
                "com.example.pluginEvil.Main",
                listOf("com.example.plugin"),
            ),
        )
    }
}
