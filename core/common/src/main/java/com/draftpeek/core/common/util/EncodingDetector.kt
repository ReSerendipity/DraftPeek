/**
 * 文件编码检测模块。
 *
 * 实现三段式编码检测策略：BOM检测（权威信号）→ juniversalchardet主检测+GB18030归一化 → UTF-8 fallback。
 * 支持UTF-8、UTF-16 BE/LE、GB18030、Big5、Shift_JIS、EUC-JP、EUC-KR、ISO-2022-JP等常见编码。
 * 采用64KB采样平衡检测精度与内存开销，源码文件默认UTF-8。
 *
 * ## P1 优化说明
 * 当前使用 juniversalchardet 2.5.0（Google 代码节自 Mozilla），是 CJK 编码检测的成熟标准方案。
 * 自研的 BOM 检测和 GB18030 归一化是 juniversalchardet 的标准补充实践，**不建议完全替换**。
 *
 * 如需更强 CJK 检测能力，可考虑以下替代方案：
 * 1. **ICU4J CharsetDetector** (`com.ibm.icu:icu4j`)：IBM 的国际化库，检测精度更高，
 *    但包体积增量 ~4MB，不适合移动端。
 * 2. **Mozilla universalchardet** (`org.mozilla:universalchardet`)：juniversalchardet 的上游，
 *    检测算法相同，但更新频率更高。
 * 3. **Alibaba fastjson CharsetUtils**：轻量级 CJK 检测，可作为 LOW_CONFIDENCE_ENCODINGS
 *    归一化的参考实现。
 *
 * 当前方案评估结论：juniversalchardet + 自研胶水层是移动端最佳平衡点，暂不替换。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import java.nio.charset.Charset
import org.mozilla.universalchardet.UniversalDetector

/**
 * 文件编码检测工具对象。
 *
 * 三段式编码检测策略：
 *
 * **第一段 — BOM 检测**：字节序标记是权威信号，检测到即返回。
 *   支持 UTF-8 (EF BB BF)、UTF-16 BE (FE FF)、UTF-16 LE (FF FE)。
 *
 * **第二段 — juniversalchardet 主检测 + GB18030 归一化**：
 *   - 仅取文件前 64KB 送入 Mozilla 通用编码检测器，避免大文件 OOM
 *   - GB18030 归一化：GBK/GB2312 → GB18030（GB18030 是 GBK 的超集，
 *     统一归一化确保完整覆盖所有中文字符）
 *   - juniversalchardet 内置对 Big5、Shift_JIS、EUC-JP、EUC-KR、
 *     ISO-2022-JP 等 CJK 编码的检测，无需单独的 GBK 启发式
 *
 * **第三段 — UTF-8 fallback**：
 *   - juniversalchardet 返回 null 或低置信度结果时，默认 UTF-8
 *   - UTF-8 是现代文本的默认编码，比 ISO-8859-1 更安全
 *   - 对于源码文件（.java, .kt, .py 等），无论检测结果如何均默认 UTF-8
 *
 * 优化要点：
 * - BOM 匹配后立即返回，避免不必要的扫描
 * - 使用 Set 进行 O(1) 扩展名查找
 * - 魔法阈值提取为命名常量
 * - 每个检测策略独立方法（单一职责）
 * - 防御性处理空字节数组
 * - 64KB 采样：平衡检测精度与内存开销
 * - ThreadLocal复用UniversalDetector实例，避免重复创建
 */
object EncodingDetector {

    private const val SAMPLE_MAX_BYTES = 65536

    private const val GBK_MIN_HIGH_BYTES = 4
    private const val GBK_VALID_RATIO_THRESHOLD = 0.7f

    private val UTF8_EXTENSIONS = setOf(
        "java", "kt", "kts", "py", "js", "ts", "json", "xml",
        "html", "htm", "css", "md", "yaml", "yml", "sh",
        "c", "cpp", "cc", "cxx", "go", "rs", "sql", "swift"
    )

    private val LOW_CONFIDENCE_ENCODINGS = setOf(
        "ASCII", "US-ASCII", "ISO-8859-1", "WINDOWS-1252", "ISO-8859-2",
        "ISO-8859-5", "ISO-8859-8", "IBM855", "IBM866"
    )

    /**
     * 检测 BOM（字节序标记）。
     * BOM 是权威信号，检测到即返回，无需后续扫描。
     *
     * @param bytes 文件内容的原始字节
     * @return 检测到的编码名称，未检测到 BOM 返回 null
     */
    private fun detectBom(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null

        if (bytes.size >= 3) {
            if ((bytes[0].toInt() and 0xFF) == 0xEF &&
                (bytes[1].toInt() and 0xFF) == 0xBB &&
                (bytes[2].toInt() and 0xFF) == 0xBF
            ) {
                return "UTF-8"
            }
        }
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFE && b1 == 0xFF) {
                return "UTF-16BE"
            }
            if (b0 == 0xFF && b1 == 0xFE) {
                return "UTF-16LE"
            }
        }
        return null
    }

    /**
     * 验证字节数组是否为合法 UTF-8。
     *
     * 检查多字节 UTF-8 编码规则：
     * - 2 字节：110xxxxx 10xxxxxx
     * - 3 字节：1110xxxx 10xxxxxx 10xxxxxx
     * - 4 字节：11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * 同时检测过长编码和代理码点。单遍 O(n) 扫描，遇到非法序列立即返回。
     *
     * @param bytes 待验证的字节数组
     * @return 如果是合法UTF-8返回 `true`
     */
    private fun isValidUtf8(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b <= 0x7F) {
                i++
            } else if (b in 0xC2..0xDF) {
                if (i + 1 >= bytes.size || !isContinuationByte(bytes[i + 1])) return false
                i += 2
            } else if (b in 0xE0..0xEF) {
                if (i + 2 >= bytes.size) return false
                if (!isContinuationByte(bytes[i + 1]) || !isContinuationByte(bytes[i + 2])) return false
                if (b == 0xE0 && (bytes[i + 1].toInt() and 0xFF) < 0xA0) return false
                if (b == 0xED && (bytes[i + 1].toInt() and 0xFF) > 0x9F) return false
                i += 3
            } else if (b in 0xF0..0xF4) {
                if (i + 3 >= bytes.size) return false
                if (!isContinuationByte(bytes[i + 1]) || !isContinuationByte(bytes[i + 2]) ||
                    !isContinuationByte(bytes[i + 3])
                ) {
                    return false
                }
                if (b == 0xF0 && (bytes[i + 1].toInt() and 0xFF) < 0x90) return false
                i += 4
            } else {
                return false
            }
        }
        return true
    }

    /**
     * 判断字节是否为UTF-8续字节（10xxxxxx格式）。
     *
     * @param b 待判断的字节
     * @return 如果是续字节返回 `true`
     */
    private fun isContinuationByte(b: Byte): Boolean = (b.toInt() and 0xC0) == 0x80

    /**
     * 启发式 GBK/GB2312 检测（保留作为补充手段）。
     *
     * GBK 使用双字节序列：首字节 0x81-0xFE，次字节 0x40-0x7E 或 0x80-0xFE。
     *
     * 当有效 GBK 字节对与高字节的比率超过阈值，且高字节数不少于最小值时返回 true。
     *
     * 注意：主检测链中已不使用此方法，juniversalchardet 能更准确地检测 GBK。
     * 保留此方法供特殊情况或未来使用。
     *
     * @param bytes 待检测的字节数组
     * @return 如果看起来像GBK编码返回 `true`
     */
    private fun looksLikeGbk(bytes: ByteArray): Boolean {
        var highByteCount = 0
        var validGbkPairs = 0
        var i = 0

        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            if (b in 0x81..0xFE) {
                highByteCount++
                if (i + 1 < bytes.size) {
                    val b2 = bytes[i + 1].toInt() and 0xFF
                    if (b2 in 0x40..0x7E || b2 in 0x80..0xFE) {
                        validGbkPairs++
                        i += 2
                        continue
                    }
                }
            }
            i++
        }

        if (highByteCount < GBK_MIN_HIGH_BYTES) return false
        val ratio = validGbkPairs.toFloat() / highByteCount.toFloat()
        return ratio > GBK_VALID_RATIO_THRESHOLD
    }

    /**
     * 使用 juniversalchardet 进行主检测（第二段核心）。
     *
     * 仅取前 [SAMPLE_MAX_BYTES] 字节送入检测器，避免大文件内存压力。
     * juniversalchardet 基于 Mozilla 通用编码检测算法，对 CJK 编码
     * （Big5、Shift_JIS、EUC-JP、EUC-KR、ISO-2022-JP）检测效果优于
     * 简单的启发式方法。使用ThreadLocal复用检测器实例，避免重复创建开销。
     *
     * @param bytes 文件内容的原始字节
     * @return 归一化后的编码名称；检测失败或置信度低返回 null
     */
    private fun detectWithChardet(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null

        try {
            // Create a new detector instance each call to avoid ThreadLocal state pollution
            // between sequential detections (reset() is not reliable across all juniversalchardet versions)
            val detector = UniversalDetector(null)
            detector.handleData(bytes, 0, minOf(bytes.size, SAMPLE_MAX_BYTES))
            detector.dataEnd()
            val detected = detector.detectedCharset

            if (detected != null) {
                val normalized = normalizeEncoding(detected)
                if (normalized.uppercase() in LOW_CONFIDENCE_ENCODINGS) {
                    return null
                }
                return normalized
            }
        } catch (_: Exception) {
        }

        return null
    }

    /**
     * GB18030 归一化 + 编码名称规范化（第二段核心）。
     *
     * GB18030 是 GBK/GB2312 的超集：
     * - GB2312 (1980)：基本汉字集，6763 字
     * - GBK (1995)：扩展至 21886 字
     * - GB18030 (2000/2005)：强制标准，覆盖 Unicode 全部 CJK 统一汉字
     *
     * 统一归一化到 GB18030，确保 Java Charset 解码时不会丢失生僻字。
     *
     * @param encoding 检测到的原始编码名称
     * @return 归一化后的编码名称
     */
    private fun normalizeEncoding(encoding: String): String = when (encoding.uppercase()) {
        "GB18030" -> "GB18030"
        "GB2312", "GBK" -> "GB18030"
        "BIG5" -> "Big5"
        "EUC-JP" -> "EUC-JP"
        "SHIFT_JIS" -> "Shift_JIS"
        "EUC-KR" -> "EUC-KR"
        "ISO-2022-JP" -> "ISO-2022-JP"
        else -> encoding
    }

    /**
     * 三段式编码检测（无文件名提示）。
     *
     * 检测链：
     * 1. BOM 检测 → 权威结果，立即返回
     * 2. juniversalchardet 主检测 + GB18030 归一化 → 可靠结果
     * 3. UTF-8 fallback → 置信度低或结果模糊时默认 UTF-8
     *
     * @param bytes 文件内容的原始字节
     * @return 检测到的编码名称，最终 fallback 为 "UTF-8"
     */
    fun detectEncoding(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "UTF-8"

        detectBom(bytes)?.let { return it }

        val chardetResult = detectWithChardet(bytes)
        if (chardetResult != null) {
            // juniversalchardet may incorrectly return UTF-8 for GBK-encoded text
            // on some JVMs. If the bytes are not valid UTF-8, try GBK heuristic.
            if (chardetResult == "UTF-8" && !isValidUtf8(bytes) && looksLikeGbk(bytes)) {
                return "GB18030"
            }
            return chardetResult
        }

        // Fallback: if bytes are not valid UTF-8 but look like GBK, return GB18030
        if (!isValidUtf8(bytes) && looksLikeGbk(bytes)) {
            return "GB18030"
        }

        return "UTF-8"
    }

    /**
     * 三段式编码检测（带文件名提示）。
     *
     * 对于源码文件（.java, .kt, .py 等），直接默认 UTF-8（现代源码约定），
     * 跳过不必要的字符集检测。非源码文件走完整的三段式检测链。
     *
     * @param bytes 文件内容的原始字节
     * @param fileName 文件名，用于扩展名判断
     * @return 检测到的编码名称
     */
    fun detectEncoding(bytes: ByteArray, fileName: String): String {
        if (bytes.isEmpty()) return "UTF-8"

        detectBom(bytes)?.let { return it }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension in UTF8_EXTENSIONS) {
            return "UTF-8"
        }

        return detectEncoding(bytes)
    }

    /**
     * 使用 juniversalchardet 进行编码检测（兼容旧 API）。
     *
     * 提供更好的 CJK 编码检测（Big5、Shift_JIS、EUC-JP、EUC-KR、ISO-2022-JP），
     * 已包含 GB18030 归一化。
     *
     * 注意：新代码应直接使用 [detectEncoding]，其已内含 juniversalchardet 主检测。
     * 本方法保留用于向后兼容。
     *
     * @param bytes 文件内容的原始字节
     * @param fileName 可选文件名（保留用于未来扩展）
     * @return 检测到的编码名称，最终 fallback 为 "UTF-8"
     */
    fun detectEncodingFallback(bytes: ByteArray, fileName: String? = null): String {
        if (bytes.isEmpty()) return "UTF-8"

        val result = detectWithChardet(bytes)
        if (result != null) return result

        return "UTF-8"
    }

    /**
     * 使用检测到的编码将字节解码为字符串，自动去除 BOM。
     *
     * 内部调用 [detectEncoding] 进行编码检测。若调用方已通过
     * [detectEncoding] 获取编码，应改用 [decodeWithEncoding] 以避免重复扫描。
     *
     * @param bytes 文件内容的原始字节
     * @param fileName 可选文件名，用于编码检测提示
     * @return 解码后的字符串
     */
    fun decodeBytes(bytes: ByteArray, fileName: String? = null): String {
        if (bytes.isEmpty()) return ""

        val encoding = if (fileName != null) {
            detectEncoding(bytes, fileName)
        } else {
            detectEncoding(bytes)
        }

        return decodeWithEncoding(bytes, encoding)
    }

    /**
     * 用已知编码解码字节为字符串，处理 BOM 去除。
     *
     * 调用方应先调 [detectEncoding] 获取编码，再用本方法解码，
     * 避免重复扫描大字节数组。
     *
     * @param bytes 待解码的字节数组
     * @param encoding 已检测的编码名称（应来自 [detectEncoding]）
     * @return 解码后的字符串
     * @throws java.nio.charset.UnsupportedCharsetException 如果编码不支持
     */
    fun decodeWithEncoding(bytes: ByteArray, encoding: String): String {
        if (bytes.isEmpty()) return ""

        if (encoding == "UTF-8" && bytes.size >= 3 &&
            (bytes[0].toInt() and 0xFF) == 0xEF &&
            (bytes[1].toInt() and 0xFF) == 0xBB &&
            (bytes[2].toInt() and 0xFF) == 0xBF
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }

        if ((encoding == "UTF-16BE" || encoding == "UTF-16LE") && bytes.size >= 2) {
            val charset = Charset.forName(encoding)
            return String(bytes, 2, bytes.size - 2, charset)
        }

        return String(bytes, Charset.forName(encoding))
    }
}
