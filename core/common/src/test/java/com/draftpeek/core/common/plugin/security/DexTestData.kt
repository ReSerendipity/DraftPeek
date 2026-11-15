package com.draftpeek.core.common.plugin.security

import java.io.ByteArrayOutputStream

/**
 * 单测用最小 DEX 构造器。
 *
 * 只生成 header / string_ids / type_ids / class_defs / string_data 五个区域，
 * 足够让 [DexParser] 走完类级扫描路径；不追求可被 ART 执行。
 */
object DexTestData {

    private const val HEADER_SIZE = 0x70

    fun minimalDex(
        definedClass: String = "Lcom/example/plugin/Main;",
        extraType: String? = null,
        includeClassDef: Boolean = true
    ): ByteArray {
        val strings = mutableListOf(definedClass)
        if (extraType != null && extraType != definedClass) {
            strings += extraType
        }
        val typeDescriptors = mutableListOf(definedClass)
        if (extraType != null && extraType != definedClass) {
            typeDescriptors += extraType
        }
        val classDefCount = if (includeClassDef) 1 else 0

        val stringIdsOffset = HEADER_SIZE
        val typeIdsOffset = stringIdsOffset + strings.size * 4
        val classDefsOffset = typeIdsOffset + typeDescriptors.size * 4
        val dataOffset = classDefsOffset + classDefCount * 32

        val stringData = ByteArrayOutputStream()
        val stringOffsets = IntArray(strings.size)
        for (index in strings.indices) {
            stringOffsets[index] = dataOffset + stringData.size()
            val value = strings[index]
            stringData.write(uleb128(value.length))
            stringData.write(value.toByteArray(Charsets.UTF_8))
            stringData.write(0)
        }
        val dataBytes = stringData.toByteArray()
        val totalSize = dataOffset + dataBytes.size

        return ByteArray(totalSize).apply {
            // magic: dex\n035\0
            this[0] = 0x64
            this[1] = 0x65
            this[2] = 0x78
            this[3] = 0x0A
            this[4] = 0x30
            this[5] = 0x33
            this[6] = 0x35
            this[7] = 0x00

            putIntLe(0x20, totalSize)
            putIntLe(0x24, HEADER_SIZE)
            putIntLe(0x28, 0x12345678)

            putIntLe(0x38, strings.size)
            putIntLe(0x3C, stringIdsOffset)
            putIntLe(0x40, typeDescriptors.size)
            putIntLe(0x44, typeIdsOffset)
            putIntLe(0x60, classDefCount)
            putIntLe(0x64, if (classDefCount > 0) classDefsOffset else 0)
            putIntLe(0x68, dataBytes.size)
            putIntLe(0x6C, dataOffset)

            for (index in stringOffsets.indices) {
                putIntLe(stringIdsOffset + index * 4, stringOffsets[index])
            }
            for (index in typeDescriptors.indices) {
                val stringIndex = strings.indexOf(typeDescriptors[index])
                putIntLe(typeIdsOffset + index * 4, stringIndex)
            }

            if (classDefCount > 0) {
                putIntLe(classDefsOffset, 0) // class_idx
                putIntLe(classDefsOffset + 4, 1) // access_flags
                putIntLe(classDefsOffset + 8, -1) // superclass_idx
                putIntLe(classDefsOffset + 12, 0) // interfaces_off
                putIntLe(classDefsOffset + 16, -1) // source_file_idx
                putIntLe(classDefsOffset + 20, 0) // annotations_off
                putIntLe(classDefsOffset + 24, 0) // class_data_off
                putIntLe(classDefsOffset + 28, 0) // static_values_off
            }

            dataBytes.copyInto(this, dataOffset)
        }
    }

    fun withIntLe(bytes: ByteArray, offset: Int, value: Int): ByteArray = bytes.copyOf().apply {
        putIntLe(offset, value)
    }

    fun readIntLe(bytes: ByteArray, offset: Int): Int = (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    fun classDefsOffset(bytes: ByteArray): Int = readIntLe(bytes, 0x64)

    private fun ByteArray.putIntLe(offset: Int, value: Int) {
        this[offset] = (value and 0xFF).toByte()
        this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        this[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        this[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun uleb128(value: Int): ByteArray {
        var remaining = value
        val output = ByteArrayOutputStream()
        do {
            var byte = remaining and 0x7F
            remaining = remaining ushr 7
            if (remaining != 0) byte = byte or 0x80
            output.write(byte)
        } while (remaining != 0)
        return output.toByteArray()
    }
}
