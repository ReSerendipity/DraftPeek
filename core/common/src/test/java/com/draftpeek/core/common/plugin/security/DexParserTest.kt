package com.draftpeek.core.common.plugin.security

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("DexParser / Dex scanners")
class DexParserTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    @DisplayName("parses a minimal DEX and exports defined classes")
    fun parsesMinimalDex() {
        val result = DexParser.parse(DexTestData.minimalDex())

        assertTrue(result is DexScanResult.Success)
        val success = result as DexScanResult.Success
        assertEquals(
            listOf("Lcom/example/plugin/Main;"),
            success.definedClasses
        )
        assertTrue(success.referencedTypes.contains("Lcom/example/plugin/Main;"))
    }

    @Test
    @DisplayName("exports extra type_ids as referenced types")
    fun parsesReferencedTypes() {
        val result = DexParser.parse(
            DexTestData.minimalDex(extraType = "Ljava/lang/Runtime;")
        )

        assertTrue(result is DexScanResult.Success)
        val success = result as DexScanResult.Success
        assertTrue(success.referencedTypes.contains("Ljava/lang/Runtime;"))
    }

    @Test
    @DisplayName("rejects a truncated header")
    fun rejectsTruncatedHeader() {
        val result = DexParser.parse(DexTestData.minimalDex().copyOf(0x10))
        assertTrue(result is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects invalid magic")
    fun rejectsBadMagic() {
        val bytes = DexTestData.minimalDex().copyOf().apply { this[0] = 0x63 }
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects file_size mismatch")
    fun rejectsFileSizeMismatch() {
        val bytes = DexTestData.minimalDex()
        val mutated = DexTestData.withIntLe(bytes, 0x20, bytes.size + 1)
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects invalid endian tag")
    fun rejectsBadEndian() {
        val bytes = DexTestData.withIntLe(
            DexTestData.minimalDex(),
            0x28,
            0x12345679
        )
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects out-of-bounds string_ids offset")
    fun rejectsStringIdsOffsetOutOfBounds() {
        val bytes = DexTestData.withIntLe(
            DexTestData.minimalDex(),
            0x3C,
            0x7FFFFFFF
        )
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a string_data_off outside the data section")
    fun rejectsStringDataOffsetOutsideData() {
        val bytes = DexTestData.minimalDex()
        val stringIdsOffset = DexTestData.readIntLe(bytes, 0x3C)
        val mutated = DexTestData.withIntLe(bytes, stringIdsOffset, 0x70)
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects type_ids offset out-of-bounds")
    fun rejectsTypeIdsOffsetOutOfBounds() {
        val bytes = DexTestData.withIntLe(
            DexTestData.minimalDex(),
            0x44,
            0x7FFFFFFF
        )
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects class_idx outside type_ids")
    fun rejectsClassIndexOutOfBounds() {
        val bytes = DexTestData.minimalDex()
        val classDefsOffset = DexTestData.classDefsOffset(bytes)
        val mutated = DexTestData.withIntLe(bytes, classDefsOffset, 9)
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a superclass_idx outside type_ids")
    fun rejectsSuperclassIndexOutOfBounds() {
        val bytes = DexTestData.minimalDex()
        val classDefsOffset = DexTestData.classDefsOffset(bytes)
        val mutated = DexTestData.withIntLe(bytes, classDefsOffset + 8, 1)
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a source_file_idx outside string_ids")
    fun rejectsSourceFileIndexOutOfBounds() {
        val bytes = DexTestData.minimalDex()
        val classDefsOffset = DexTestData.classDefsOffset(bytes)
        val mutated = DexTestData.withIntLe(bytes, classDefsOffset + 16, 1)
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects an interfaces_off outside the data section")
    fun rejectsInterfacesOffsetOutOfData() {
        val bytes = DexTestData.minimalDex()
        val classDefsOffset = DexTestData.classDefsOffset(bytes)
        val dataOffset = DexTestData.readIntLe(bytes, 0x6C)
        val dataSize = DexTestData.readIntLe(bytes, 0x68)
        val mutated = DexTestData.withIntLe(
            bytes,
            classDefsOffset + 12,
            dataOffset + dataSize + 4
        )
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a class_data_off outside the data section")
    fun rejectsClassDataOffsetOutOfData() {
        val bytes = DexTestData.minimalDex()
        val classDefsOffset = DexTestData.classDefsOffset(bytes)
        val dataOffset = DexTestData.readIntLe(bytes, 0x6C)
        val dataSize = DexTestData.readIntLe(bytes, 0x68)
        val mutated = DexTestData.withIntLe(
            bytes,
            classDefsOffset + 24,
            dataOffset + dataSize + 4
        )
        assertTrue(DexParser.parse(mutated) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a string without a NUL terminator")
    fun rejectsMissingStringTerminator() {
        val bytes = DexTestData.minimalDex().copyOf()
        bytes[bytes.lastIndex] = 0x41
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a DEX without class_defs")
    fun rejectsEmptyClassDefs() {
        val bytes = DexTestData.minimalDex(includeClassDef = false)
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects an excessive string_ids count")
    fun rejectsExcessiveStringCount() {
        val bytes = DexTestData.withIntLe(
            DexTestData.minimalDex(),
            0x38,
            1_000_001
        )
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("rejects a malformed class descriptor")
    fun rejectsMalformedClassDescriptor() {
        val bytes = DexTestData.minimalDex(
            definedClass = "Lcom/example/plugin/Main"
        )
        assertTrue(DexParser.parse(bytes) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("APK scanner reads all .dex entries")
    fun apkScannerReadsDexEntries() {
        val apk = tempDir.resolve("plugin.apk").toFile()
        writeZip(
            apk,
            mapOf(
                "classes.dex" to DexTestData.minimalDex(),
                "classes2.dex" to DexTestData.minimalDex(
                    definedClass = "Lcom/example/plugin/Second;"
                )
            )
        )

        val result = ApkDexScanner.scan(apk)

        assertTrue(result is DexScanResult.Success)
        val success = result as DexScanResult.Success
        assertTrue(success.definedClasses.contains("Lcom/example/plugin/Main;"))
        assertTrue(success.definedClasses.contains("Lcom/example/plugin/Second;"))
    }

    @Test
    @DisplayName("APK scanner rejects an APK without .dex")
    fun apkScannerRejectsNoDex() {
        val apk = tempDir.resolve("empty.apk").toFile()
        writeZip(apk, mapOf("resources.arsc" to ByteArray(8)))
        assertTrue(ApkDexScanner.scan(apk) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("file scanner rejects non-DEX bytes")
    fun fileScannerRejectsNonDex() {
        val dex = tempDir.resolve("plain.dex").toFile()
        dex.writeText("not a dex")
        assertTrue(DexFileScanner.scan(dex) is DexScanResult.Failure)
    }

    @Test
    @DisplayName("class names/descriptors normalize consistently")
    fun classNamesNormalize() {
        assertEquals(
            "com.example.Foo",
            PluginClassNames.normalizeClassName("[Lcom/example/Foo;")
        )
        assertEquals(
            "com.example.plugin",
            PluginClassNames.normalizePrefix("Lcom/example/plugin;")
        )
        assertFalse(
            PluginClassNames.isUnderPrefixes(
                "com.example.pluginEvil.Main",
                listOf("com.example.plugin")
            )
        )
        assertTrue(
            PluginClassNames.isUnderPrefixes(
                "com.example.plugin.Main\$Inner",
                listOf("com.example.plugin")
            )
        )
    }

    private fun writeZip(file: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }
}
