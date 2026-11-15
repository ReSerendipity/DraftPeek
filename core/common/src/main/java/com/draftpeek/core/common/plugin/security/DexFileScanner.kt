package com.draftpeek.core.common.plugin.security

import java.io.File

/** 扫描 `.dex` 文件。 */
internal object DexFileScanner {

    fun scan(file: File): DexScanResult {
        if (!file.isFile) {
            return DexScanResult.Failure("插件路径不是普通文件")
        }
        val length = file.length()
        if (length <= 0L) {
            return DexScanResult.Failure("DEX 文件为空")
        }
        if (length > DexParser.MAX_DEX_BYTES) {
            return DexScanResult.Failure(
                "DEX 超过 ${DexParser.MAX_DEX_BYTES / 1024 / 1024} MB 上限",
            )
        }

        return try {
            RandomAccessFileDexSource(file).use { source ->
                DexParser.parse(source, file.name)
            }
        } catch (e: Exception) {
            DexScanResult.Failure(
                "${file.name}: 无法读取 DEX: ${e.message ?: e.javaClass.simpleName}",
            )
        }
    }
}
