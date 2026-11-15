package com.draftpeek.core.common.plugin.security

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/** DEX 读取期间的结构错误。 */
internal class DexParseException(message: String) : Exception(message)

/** 带边界检查的 DEX 字节来源。 */
internal interface DexSource : Closeable {
    val length: Long
    fun readByte(offset: Long): Byte
    fun readIntLe(offset: Long): Int
    fun readBytes(offset: Long, count: Int): ByteArray
}

/** 内存字节来源，用于 APK 内 DEX 与单测。 */
internal class ByteArrayDexSource(private val bytes: ByteArray) : DexSource {

    override val length: Long get() = bytes.size.toLong()

    override fun readByte(offset: Long): Byte = bytes[checkedIndex(offset, 1)]

    override fun readIntLe(offset: Long): Int {
        val index = checkedIndex(offset, 4)
        return (bytes[index].toInt() and 0xFF) or
            ((bytes[index + 1].toInt() and 0xFF) shl 8) or
            ((bytes[index + 2].toInt() and 0xFF) shl 16) or
            ((bytes[index + 3].toInt() and 0xFF) shl 24)
    }

    override fun readBytes(offset: Long, count: Int): ByteArray {
        val index = checkedIndex(offset, count)
        return bytes.copyOfRange(index, index + count)
    }

    override fun close() = Unit

    private fun checkedIndex(offset: Long, count: Int): Int {
        if (offset < 0 || count < 0 || offset > length - count.toLong()) {
            throw DexParseException("读取越界：offset=$offset, count=$count, length=$length")
        }
        if (offset > Int.MAX_VALUE) {
            throw DexParseException("偏移超出内存索引范围：$offset")
        }
        return offset.toInt()
    }
}

/** 文件字节来源，避免把整个 50 MB DEX 一次性读入内存。 */
internal class RandomAccessFileDexSource(file: File) : DexSource {

    private val randomAccessFile = RandomAccessFile(file, "r")

    override val length: Long = randomAccessFile.length()

    override fun readByte(offset: Long): Byte {
        checkRange(offset, 1)
        randomAccessFile.seek(offset)
        return randomAccessFile.readByte()
    }

    override fun readIntLe(offset: Long): Int {
        val bytes = readBytes(offset, 4)
        return (bytes[0].toInt() and 0xFF) or
            ((bytes[1].toInt() and 0xFF) shl 8) or
            ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 24)
    }

    override fun readBytes(offset: Long, count: Int): ByteArray {
        checkRange(offset, count)
        val result = ByteArray(count)
        randomAccessFile.seek(offset)
        randomAccessFile.readFully(result)
        return result
    }

    override fun close() {
        randomAccessFile.close()
    }

    private fun checkRange(offset: Long, count: Int) {
        if (offset < 0 || count < 0 || offset > length - count.toLong()) {
            throw DexParseException("读取越界：offset=$offset, count=$count, length=$length")
        }
    }
}
