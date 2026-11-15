package com.draftpeek.core.common.plugin.security

import java.io.File

/** 插件文件 → 类级扫描结果；测试与 APK/DEX 分发用同一接口。 */
fun interface PluginDexScanner {
    fun scan(file: File): DexScanResult
}

/** 按扩展名分发：`.apk` 扫描 zip 内 DEX，`.dex` 直接扫描。 */
object DefaultPluginDexScanner : PluginDexScanner {

    override fun scan(file: File): DexScanResult = when (file.extension.lowercase()) {
        "apk" -> ApkDexScanner.scan(file)
        "dex" -> DexFileScanner.scan(file)
        else -> DexScanResult.Failure("不支持的插件格式：.${file.extension}")
    }
}
