package com.draftpeek.core.common.plugin.security

/**
 * 纯 Kotlin DEX 类级解析器。
 *
 * 只读取 header / string_ids / type_ids / class_defs，不解析方法体、注解或代码；
 * 目标是回答“这个 DEX 定义了哪些类、引用了哪些类型”，而不是完整反编译。
 *
 * 安全边界：
 * - 文件 ≤ 50 MB；
 * - 所有 offset/count 做 Long 边界与溢出检查；
 * - 字符串读取有长度上限，缺失 NUL 终止符视为畸形；
 * - 拒绝 magic/endian/file_size/header_size 异常、截断、越界、重复类定义、空 class_defs。
 */
internal object DexParser {

    const val MAX_DEX_BYTES = 50L * 1024 * 1024

    private const val HEADER_SIZE = 0x70
    private const val MAX_STRING_IDS = 1_000_000
    private const val MAX_TYPE_IDS = 500_000
    private const val MAX_CLASS_DEFS = 200_000
    private const val MAX_STRING_BYTES = 4096
    private const val ENDIAN_CONSTANT = 0x12345678L
    private const val NO_INDEX = 0xFFFF_FFFFL

    private val DEX_VERSIONS = setOf("35", "37", "38", "39", "40")

    fun parse(bytes: ByteArray, sourceName: String = "dex"): DexScanResult = ByteArrayDexSource(bytes).use {
        parse(it, sourceName)
    }

    fun parse(source: DexSource, sourceName: String): DexScanResult = try {
        parseInternal(source, sourceName)
    } catch (e: DexParseException) {
        DexScanResult.Failure("$sourceName: ${e.message}")
    } catch (e: Exception) {
        DexScanResult.Failure(
            "$sourceName: 解析异常: ${e.message ?: e.javaClass.simpleName}"
        )
    }

    private fun parseInternal(source: DexSource, sourceName: String): DexScanResult.Success {
        val length = source.length
        if (length < HEADER_SIZE) {
            fail("文件小于 DEX header（$length < $HEADER_SIZE）")
        }
        if (length > MAX_DEX_BYTES) {
            fail("DEX 超过 ${MAX_DEX_BYTES / 1024 / 1024} MB 上限")
        }

        val header = source.readBytes(0, HEADER_SIZE)
        validateMagic(header)

        val fileSize = u32(header, 0x20)
        if (fileSize != length) {
            fail("file_size 与真实长度不一致：header=$fileSize, actual=$length")
        }

        val headerSize = u32(header, 0x24)
        if (headerSize < HEADER_SIZE.toLong() || headerSize > length) {
            fail("header_size 非法：$headerSize")
        }
        if (u32(header, 0x28) != ENDIAN_CONSTANT) {
            fail("endian_tag 非法：${u32(header, 0x28)}")
        }

        val stringTable = readTable(
            source,
            header,
            0x38,
            0x3C,
            4,
            MAX_STRING_IDS,
            "string_ids"
        )
        val typeTable = readTable(
            source,
            header,
            0x40,
            0x44,
            4,
            MAX_TYPE_IDS,
            "type_ids"
        )
        val classTable = readTable(
            source,
            header,
            0x60,
            0x64,
            32,
            MAX_CLASS_DEFS,
            "class_defs"
        )

        val dataSize = u32(header, 0x68)
        val dataOffset = u32(header, 0x6C)
        if (dataSize > 0L) {
            if (dataOffset < HEADER_SIZE || dataOffset + dataSize > length) {
                fail("data 区域越界：offset=$dataOffset, size=$dataSize")
            }
        }

        val stringOffsets = readU32Array(
            source,
            stringTable.offset,
            stringTable.count,
            "string_ids"
        )
        for (index in stringOffsets.indices) {
            val offset = stringOffsets[index]
            if (offset < HEADER_SIZE.toLong() || offset >= length) {
                fail("string_ids[$index] 越界：$offset")
            }
            if (dataSize <= 0L || offset < dataOffset || offset >= dataOffset + dataSize) {
                fail("string_ids[$index] 不在 data 区域：$offset")
            }
        }

        val typeDescriptorIndices = readU32Array(
            source,
            typeTable.offset,
            typeTable.count,
            "type_ids"
        )
        val typeDescriptors = arrayOfNulls<String>(typeTable.count)
        val referencedTypes = LinkedHashSet<String>(typeTable.count.coerceAtMost(1024))
        for (index in typeDescriptors.indices) {
            val stringIndex = typeDescriptorIndices[index]
            if (stringIndex >= stringTable.count.toLong()) {
                fail("type_ids[$index] 的 string index 越界：$stringIndex")
            }
            val descriptor = readStringAt(
                source,
                stringOffsets[stringIndex.toInt()],
                "type_ids[$index]"
            )
            if (!isValidTypeDescriptor(descriptor)) {
                fail("type_ids[$index] 不是合法类型描述符：$descriptor")
            }
            typeDescriptors[index] = descriptor
            if (descriptor.startsWith("L") || descriptor.startsWith("[")) {
                referencedTypes += descriptor
            }
        }

        if (classTable.count == 0) {
            fail("DEX 不含任何 class_defs")
        }
        val classBytes = source.readBytes(
            classTable.offset,
            classTable.count * 32
        )
        val definedClasses = LinkedHashSet<String>(classTable.count)
        for (index in 0 until classTable.count) {
            val base = index * 32
            val classIndex = u32(classBytes, base)
            if (classIndex >= typeTable.count.toLong()) {
                fail("class_defs[$index].class_idx 越界：$classIndex")
            }
            val descriptor = typeDescriptors[classIndex.toInt()]
                ?: fail("class_defs[$index] 指向空 type")
            if (!isClassDescriptor(descriptor)) {
                fail("class_defs[$index] 不是类描述符：$descriptor")
            }
            if (!definedClasses.add(descriptor)) {
                fail("重复的类定义：$descriptor")
            }

            val superclassIndex = u32(classBytes, base + 8)
            if (superclassIndex != NO_INDEX && superclassIndex >= typeTable.count.toLong()) {
                fail("class_defs[$index].superclass_idx 越界：$superclassIndex")
            }
            val sourceFileIndex = u32(classBytes, base + 16)
            if (sourceFileIndex != NO_INDEX && sourceFileIndex >= stringTable.count.toLong()) {
                fail("class_defs[$index].source_file_idx 越界：$sourceFileIndex")
            }
            validateOptionalDataOffset(
                u32(classBytes, base + 12),
                dataOffset,
                dataSize,
                "class_defs[$index].interfaces_off"
            )
            validateOptionalDataOffset(
                u32(classBytes, base + 20),
                dataOffset,
                dataSize,
                "class_defs[$index].annotations_off"
            )
            validateOptionalDataOffset(
                u32(classBytes, base + 24),
                dataOffset,
                dataSize,
                "class_defs[$index].class_data_off"
            )
            validateOptionalDataOffset(
                u32(classBytes, base + 28),
                dataOffset,
                dataSize,
                "class_defs[$index].static_values_off"
            )
        }

        return DexScanResult.Success(
            definedClasses = definedClasses.toList(),
            referencedTypes = referencedTypes.toSet()
        )
    }

    private fun validateMagic(header: ByteArray) {
        val expected = byteArrayOf(
            0x64,
            0x65,
            0x78,
            0x0A,
            0x30,
            0x00,
            0x00,
            0x00
        )
        if (header[0] != expected[0] ||
            header[1] != expected[1] ||
            header[2] != expected[2] ||
            header[3] != expected[3] ||
            header[4] != expected[4] ||
            header[7] != expected[7]
        ) {
            fail("DEX magic 非法")
        }
        val version = buildString {
            append((header[5].toInt() and 0xFF).toChar())
            append((header[6].toInt() and 0xFF).toChar())
        }
        if (version !in DEX_VERSIONS) {
            fail("不支持的 DEX 版本：$version")
        }
    }

    private data class Table(val count: Int, val offset: Long)

    private fun validateOptionalDataOffset(value: Long, dataOffset: Long, dataSize: Long, path: String) {
        if (value == 0L) return
        if (dataSize <= 0L || value < dataOffset || value >= dataOffset + dataSize) {
            fail("$path 越界：$value")
        }
    }

    private fun readTable(
        source: DexSource,
        header: ByteArray,
        sizeOffset: Int,
        offsetOffset: Int,
        entrySize: Int,
        maxCount: Int,
        name: String
    ): Table {
        val count = u32(header, sizeOffset)
        if (count > maxCount.toLong()) {
            fail("$name 数量过大：$count > $maxCount")
        }
        if (count > Int.MAX_VALUE.toLong()) {
            fail("$name 数量超出 Int 范围：$count")
        }
        if (count == 0L) return Table(0, 0)

        val offset = u32(header, offsetOffset)
        if (offset < HEADER_SIZE.toLong() || offset % 4L != 0L) {
            fail("$name 偏移非法：$offset")
        }
        if (offset + count * entrySize > source.length) {
            fail("$name 区域越界：offset=$offset, count=$count, entrySize=$entrySize")
        }
        return Table(count.toInt(), offset)
    }

    private fun readU32Array(source: DexSource, offset: Long, count: Int, name: String): LongArray {
        if (count == 0) return LongArray(0)
        val bytes = source.readBytes(offset, count * 4)
        return LongArray(count) { index -> u32(bytes, index * 4) }
    }

    private fun readStringAt(source: DexSource, offset: Long, path: String): String {
        if (offset < HEADER_SIZE.toLong() || offset >= source.length) {
            fail("$path string_data_off 越界：$offset")
        }

        var position = offset
        var utf16Length = 0L
        var shift = 0
        var terminated = false
        for (index in 0 until 5) {
            val value = source.readByte(position++).toInt() and 0xFF
            utf16Length = utf16Length or ((value and 0x7F).toLong() shl shift)
            if ((value and 0x80) == 0) {
                terminated = true
                break
            }
            shift += 7
        }
        if (!terminated) {
            fail("$path 的 uleb128 长度缺少终止位")
        }
        if (utf16Length > MAX_STRING_BYTES.toLong()) {
            fail("$path 字符串过长：$utf16Length")
        }

        val buffer = ByteArray(MAX_STRING_BYTES + 1)
        var count = 0
        while (true) {
            val value = source.readByte(position++).toInt() and 0xFF
            if (value == 0) break
            if (count >= MAX_STRING_BYTES) {
                fail("$path 字符串超过 $MAX_STRING_BYTES 字节")
            }
            buffer[count++] = value.toByte()
        }

        return decodeMutf8(buffer.copyOf(count), path).also { decoded ->
            if (decoded.length != utf16Length.toInt()) {
                fail("$path 的 MUTF-8 长度与 uleb128 不一致：${decoded.length} != $utf16Length")
            }
        }
    }

    private fun decodeMutf8(bytes: ByteArray, path: String): String {
        val builder = StringBuilder(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xFF
            when {
                first == 0 -> fail("$path 包含未编码的 NUL")
                first < 0x80 -> {
                    builder.append(first.toChar())
                    index += 1
                }
                first in 0xC0..0xDF -> {
                    if (index + 1 >= bytes.size) fail("$path 的 2 字节 MUTF-8 被截断")
                    val second = bytes[index + 1].toInt() and 0xFF
                    if ((second and 0xC0) != 0x80) fail("$path 的 2 字节 MUTF-8 非法")
                    builder.append((((first and 0x1F) shl 6) or (second and 0x3F)).toChar())
                    index += 2
                }
                first in 0xE0..0xEF -> {
                    if (index + 2 >= bytes.size) fail("$path 的 3 字节 MUTF-8 被截断")
                    val second = bytes[index + 1].toInt() and 0xFF
                    val third = bytes[index + 2].toInt() and 0xFF
                    if ((second and 0xC0) != 0x80 || (third and 0xC0) != 0x80) {
                        fail("$path 的 3 字节 MUTF-8 非法")
                    }
                    val code = ((first and 0x0F) shl 12) or
                        ((second and 0x3F) shl 6) or
                        (third and 0x3F)
                    builder.append(code.toChar())
                    index += 3
                }
                else -> fail("$path 包含不支持的 4 字节 UTF-8/MUTF-8")
            }
        }
        return builder.toString()
    }

    private fun isValidTypeDescriptor(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.any { it < ' ' || it == '\u007F' }) return false
        if (value.length == 1) {
            return value[0] == 'V' ||
                value[0] == 'Z' ||
                value[0] == 'B' ||
                value[0] == 'S' ||
                value[0] == 'C' ||
                value[0] == 'I' ||
                value[0] == 'J' ||
                value[0] == 'F' ||
                value[0] == 'D'
        }

        var index = 0
        while (index < value.length && value[index] == '[') index++
        if (index > 255) return false
        val base = value.substring(index)
        if (base.length < 3 || base[0] != 'L' || base[base.length - 1] != ';') {
            return false
        }
        return isValidClassBody(base.substring(1, base.length - 1))
    }

    private fun isClassDescriptor(value: String): Boolean =
        value.startsWith("L") && value.endsWith(";") && isValidTypeDescriptor(value)

    private fun isValidClassBody(body: String): Boolean {
        if (body.isEmpty()) return false
        if (body.startsWith("/") || body.endsWith("/") || body.contains("//")) {
            return false
        }
        return body.all { ch ->
            ch.isLetterOrDigit() || ch == '_' || ch == '$' || ch == '-' || ch == '/'
        }
    }

    private fun u32(bytes: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + 4 > bytes.size) {
            fail("读取 32 位字段越界：offset=$offset")
        }
        return (
            (bytes[offset].toLong() and 0xFF) or
                ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
                ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
                ((bytes[offset + 3].toLong() and 0xFF) shl 24)
            )
    }

    private fun fail(message: String): Nothing = throw DexParseException(message)
}
