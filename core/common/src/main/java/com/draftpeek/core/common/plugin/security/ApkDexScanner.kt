package com.draftpeek.core.common.plugin.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * 扫描 APK 内的所有 `.dex` 条目。
 *
 * 不依赖 dexlib2：把每个 DEX 当作独立字节流交给 [DexParser]。
 * 对 zip bomb 做两层限制：单条目 ≤ 50 MB、所有 DEX 解压后总量 ≤ 50 MB。
 */
internal object ApkDexScanner {

    fun scan(apk: File): DexScanResult {
        if (!apk.isFile) {
            return DexScanResult.Failure("插件路径不是普通文件")
        }
        if (apk.length() <= 0L) {
            return DexScanResult.Failure("APK 文件为空")
        }
        if (apk.length() > DexParser.MAX_DEX_BYTES) {
            return DexScanResult.Failure(
                "APK 超过 ${DexParser.MAX_DEX_BYTES / 1024 / 1024} MB 上限"
            )
        }

        return try {
            ZipFile(apk).use { zip ->
                val entries = zip.entries()
                val dexEntries = mutableListOf<ZipEntry>()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.endsWith(".dex", ignoreCase = true)) {
                        dexEntries += entry
                    }
                }
                if (dexEntries.isEmpty()) {
                    return DexScanResult.Failure("APK 不含 .dex 条目")
                }

                var totalBytes = 0L
                val definedClasses = LinkedHashSet<String>()
                val referencedTypes = LinkedHashSet<String>()

                for (entry in dexEntries) {
                    if (entry.size > DexParser.MAX_DEX_BYTES) {
                        return DexScanResult.Failure(
                            "APK 条目 ${entry.name} 超过 50 MB 上限"
                        )
                    }
                    val bytes = zip.getInputStream(entry).use { input ->
                        readLimited(input)
                    }
                    totalBytes += bytes.size.toLong()
                    if (totalBytes > DexParser.MAX_DEX_BYTES) {
                        return DexScanResult.Failure(
                            "APK 内 DEX 解压后总量超过 50 MB 上限"
                        )
                    }

                    when (val result = DexParser.parse(bytes, entry.name)) {
                        is DexScanResult.Failure -> return result
                        is DexScanResult.Success -> {
                            definedClasses += result.definedClasses
                            referencedTypes += result.referencedTypes
                        }
                    }
                }

                if (definedClasses.isEmpty()) {
                    return DexScanResult.Failure("APK 内的 DEX 没有定义任何类")
                }
                DexScanResult.Success(
                    definedClasses = definedClasses.toList(),
                    referencedTypes = referencedTypes.toSet()
                )
            }
        } catch (e: Exception) {
            DexScanResult.Failure(
                "${apk.name}: 无法扫描 APK 内 DEX: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (read == 0) continue
            total += read.toLong()
            if (total > DexParser.MAX_DEX_BYTES) {
                throw DexParseException("DEX 解压后超过 50 MB 上限")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
