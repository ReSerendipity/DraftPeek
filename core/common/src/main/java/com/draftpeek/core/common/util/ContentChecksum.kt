/**
 * 内容校验和工具模块。
 *
 * 基于CRC32计算字符串内容的32位校验和，用于高效检测内容变更。
 * 避免昂贵的字符串比较，特别适用于撤销到原始状态场景——编辑器内容可能在中间状态循环后返回原始内容，
 * 简单的脏标记会错误地将文件标记为已修改。
 *
 * 性能优化：使用 ThreadLocal 缓存 CRC32 实例，避免每次调用都创建新对象和字节数组。
 * 对高频调用路径（每次内容变更防抖回调）显著减少 GC 压力。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import java.util.zip.CRC32

/**
 * CRC32内容校验和工具对象。
 *
 * 使用CRC32算法计算UTF-8编码字符串内容的校验和，用于高效检测内容是否发生实际变更。
 * 相比直接字符串比较，CRC32计算更快且内存占用更小。
 *
 * 使用 ThreadLocal<CRC32> 复用实例而非每次 new，减少频繁输入时的 GC 开销。
 */
object ContentChecksum {

    /**
     * ThreadLocal 缓存的 CRC32 实例。每个线程持有一个独立实例，线程安全且无锁竞争。
     * CRC32.reset() 可以重置状态，无需每次创建新对象。
     */
    private val threadLocalCrc32 = object : ThreadLocal<CRC32>() {
        override fun initialValue(): CRC32 = CRC32()
    }

    /**
     * 计算内容的CRC32校验和（UTF-8编码）。
     *
     * 优化：复用 ThreadLocal 中的 CRC32 实例，避免每次调用分配新对象。
     * 使用 getBytes(Charsets.UTF_8) 但 Kotlin 中 toByteArray(UTF_8) 已优化；
     * CRC32.update 后 reset 以便下次复用。
     *
     * @param content 待计算校验和的字符串内容
     * @return CRC32值（Long类型）
     */
    fun crc32(content: String): Long {
        val crc = threadLocalCrc32.get() ?: CRC32().also { threadLocalCrc32.set(it) }
        crc.reset()
        val bytes = content.toByteArray(Charsets.UTF_8)
        crc.update(bytes, 0, bytes.size)
        return crc.value
    }

    /**
     * 检查内容相对于之前的校验和是否发生了变化。
     *
     * @param content 当前待检查的内容
     * @param previousChecksum 之前存储的CRC32校验和
     * @return 如果内容与基线不同返回`true`，否则返回`false`
     */
    fun hasChanged(content: String, previousChecksum: Long): Boolean {
        return crc32(content) != previousChecksum
    }
}
