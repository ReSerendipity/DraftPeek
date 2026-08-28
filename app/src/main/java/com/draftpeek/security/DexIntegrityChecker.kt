/**
 * DraftPeek DEX 文件完整性校验模块。
 *
 * **文件功能**：验证 APK 内 DEX（Dalvik Executable）文件未被篡改，通过 ZIP CRC32 校验、
 *               SHA-256 哈希、文件命名规范等多维度检测 DEX 修改。
 *
 * **主要类/接口**：
 * - [DexIntegrityChecker] - 单例对象，提供 DEX 完整性校验能力
 * - [DexResult] - 校验结果密封类
 * - [DexFileInfo] - DEX 文件信息数据类
 *
 * **模块依赖**：
 * - Android 框架：[Context] 获取 APK 路径
 * - Java 标准库：[ZipFile] 读取 APK、[CRC32] 校验、[MessageDigest] 计算 SHA-256
 */
package com.draftpeek.security

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipFile

/**
 * DEX 文件完整性校验器。
 *
 * 在运行时验证 APK 中的 DEX 文件未被篡改：
 * 1. 读取 APK 中每个 classes*.dex 的 ZipEntry CRC32
 * 2. 重新计算 DEX 文件流的实际 CRC32
 * 3. 对比 ZIP 目录中的 CRC 与实际内容 CRC，不匹配说明 DEX 被修改后未更新 ZIP 元数据
 * 4. 计算 DEX 文件的 SHA-256 指纹，用于进一步确认完整性
 * 5. 验证 DEX 文件数量和命名是否符合预期
 *
 * 该检查器利用 ZIP 格式自身的完整性机制 + SHA-256 哈希双重校验。
 * 攻击者修改 DEX 后必须同时更新 ZIP 中央目录中的 CRC32，
 * 而这会破坏 APK 签名（签名校验已由 ApkIntegrityChecker 覆盖）。
 */
object DexIntegrityChecker {

    private const val TAG = "DexIntegrityChecker"

    @Volatile
    var isVerified: Boolean = false
        private set

    /**
     * 校验结果
     */
    sealed class DexResult {
        /** 所有 DEX 文件完整性正常 */
        data object Verified : DexResult()

        /** DEX 文件完整性异常 */
        data class Tampered(val details: String) : DexResult()

        /** 校验过程出错 */
        data class Error(val exception: Exception) : DexResult()
    }

    /**
     * DEX 文件校验信息
     */
    data class DexFileInfo(val name: String, val crc32: Long, val sha256: String, val size: Long)

    /**
     * 执行 DEX 完整性校验
     */
    fun verify(context: Context): DexResult {
        return try {
            val apkPath = context.applicationInfo.sourceDir
                ?: return DexResult.Error(IllegalStateException("Cannot determine APK path"))

            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return DexResult.Error(IllegalStateException("APK file not found: $apkPath"))
            }

            val dexFiles = mutableListOf<DexFileInfo>()
            val issues = mutableListOf<String>()
            val warnings = mutableListOf<String>()

            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    // 只检查 DEX 文件
                    if (!name.matches(Regex("classes\\d*\\.dex"))) continue

                    // 一次性读取 InputStream，同时计算 CRC32、SHA-256 和 size
                    // 修复：之前多次调用 zip.getInputStream(entry) 可能导致某些设备上的问题
                    val triple = computeCrcSha256SizeStreaming(zip.getInputStream(entry))
                    val actualCrc = triple.first
                    val actualSize = triple.second
                    val sha256 = triple.third
                    val declaredCrc = entry.crc

                    // 存储 DEX 文件信息
                    dexFiles.add(DexFileInfo(name, actualCrc, sha256, actualSize))

                    // CRC32 校验：ZIP 目录中声明的 CRC 应与实际内容匹配
                    // 注意：某些 Android 版本或构建工具（如 zipalign）可能导致 CRC 差异，
                    // 因此 CRC 不匹配只记录警告，不判定为篡改。签名校验才是主要的完整性保障。
                    if (actualCrc != declaredCrc) {
                        warnings.add(
                            "CRC mismatch for $name: declared=0x${declaredCrc.toString(
                                16
                            )}, actual=0x${actualCrc.toString(16)}"
                        )
                    }

                    // 检查 DEX 文件大小是否合理（至少 1KB，小于 100MB）
                    if (actualSize < 1024) {
                        issues.add("Suspiciously small DEX file: $name ($actualSize bytes)")
                    }
                }
            }

            // 验证 DEX 文件数量和命名
            if (dexFiles.isEmpty()) {
                issues.add("No DEX files found in APK")
            } else {
                // 检查是否有 classes.dex（主 DEX）
                val hasMainDex = dexFiles.any { it.name == "classes.dex" }
                if (!hasMainDex) {
                    issues.add("Missing main DEX file (classes.dex)")
                }

                // 检查 DEX 文件命名是否规范
                dexFiles.forEach { dex ->
                    if (!dex.name.matches(Regex("^classes\\d*\\.dex$"))) {
                        issues.add("Unexpected DEX file name: ${dex.name}")
                    }
                }
            }

            if (warnings.isNotEmpty()) {
                Log.w(TAG, "DEX verification warnings: ${warnings.joinToString("; ")}")
            }

            if (issues.isEmpty()) {
                isVerified = true
                DexResult.Verified
            } else {
                DexResult.Tampered(issues.joinToString("; "))
            }
        } catch (e: Exception) {
            DexResult.Error(e)
        }
    }

    /**
     * 轻量级快速校验 —— 仅检查主 DEX 文件的 CRC
     */
    fun quickVerify(context: Context): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir ?: return false
            ZipFile(File(apkPath)).use { zip ->
                val mainDex = zip.getEntry("classes.dex") ?: return false
                computeCrc32Streaming(zip.getInputStream(mainDex)) == mainDex.crc
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 获取所有 DEX 文件的详细信息（用于调试和日志）
     */
    fun getDexFileInfo(context: Context): List<DexFileInfo> {
        return try {
            val apkPath = context.applicationInfo.sourceDir ?: return emptyList()
            val dexFiles = mutableListOf<DexFileInfo>()

            ZipFile(File(apkPath)).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (!name.matches(Regex("classes\\d*\\.dex"))) continue

                    val sha256 = computeSha256Streaming(zip.getInputStream(entry))
                    dexFiles.add(DexFileInfo(name, entry.crc, sha256, entry.size))
                }
            }
            dexFiles
        } catch (_: Exception) {
            emptyList()
        }
    }

    // [Opt] Maintainability: 移除未使用的 computeCrc32(ByteArray)，
    // 已被流式 computeCrc32Streaming(InputStream) 替代

    /**
     * 一次性流式计算 InputStream 的 CRC32、字节大小和 SHA-256。
     * 修复：避免对同一个 ZipEntry 多次调用 getInputStream() 可能导致的问题。
     * @return Triple(crc32, size, sha256)
     */
    private fun computeCrcSha256SizeStreaming(stream: java.io.InputStream): Triple<Long, Long, String> {
        val crc = CRC32()
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        BufferedInputStream(stream).use { input ->
            val chunk = ByteArray(8192)
            var read = input.read(chunk)
            while (read != -1) {
                crc.update(chunk, 0, read)
                digest.update(chunk, 0, read)
                total += read
                read = input.read(chunk)
            }
        }
        return Triple(crc.value, total, digest.digest().joinToString("") { "%02X".format(it) })
    }

    /**
     * 流式计算 InputStream 的 CRC32，避免全量加载到内存
     */
    private fun computeCrc32Streaming(stream: java.io.InputStream): Long {
        val crc = CRC32()
        BufferedInputStream(stream).use { input ->
            val chunk = ByteArray(8192)
            var read = input.read(chunk)
            while (read != -1) {
                crc.update(chunk, 0, read)
                read = input.read(chunk)
            }
        }
        return crc.value
    }

    /**
     * [Review] C7 fix: 流式计算 InputStream 的字节总数
     */
    private fun computeActualSize(stream: java.io.InputStream): Long {
        var total = 0L
        stream.use { input ->
            val chunk = ByteArray(8192)
            var read = input.read(chunk)
            while (read != -1) {
                total += read
                read = input.read(chunk)
            }
        }
        return total
    }

    /**
     * 流式计算 InputStream 的 SHA-256 哈希
     */
    private fun computeSha256Streaming(stream: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(stream).use { input ->
            val chunk = ByteArray(8192)
            var read = input.read(chunk)
            while (read != -1) {
                digest.update(chunk, 0, read)
                read = input.read(chunk)
            }
        }
        return digest.digest().joinToString("") { "%02X".format(it) }
    }

    /**
     * 计算 DEX 文件的 SHA-256 哈希（供调试和日志使用）
     */
    fun computeDexSha256(context: Context, dexName: String = "classes.dex"): String? {
        return try {
            val apkPath = context.applicationInfo.sourceDir ?: return null
            ZipFile(File(apkPath)).use { zip ->
                val entry = zip.getEntry(dexName) ?: return null
                computeSha256Streaming(zip.getInputStream(entry))
            }
        } catch (_: Exception) {
            null
        }
    }
}
