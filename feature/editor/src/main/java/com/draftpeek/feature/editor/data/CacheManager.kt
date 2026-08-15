/**
 * 文件功能：编辑器缓存管理器，负责持久化和恢复编辑器的撤销/重做状态
 * 
 * 主要类：
 * - [CacheManager]：单例缓存管理器，使用 Parcel 序列化 UndoManager 状态到本地文件
 * 
 * 模块依赖：
 * - core.common.util.AppCacheManager：应用缓存目录管理
 * - dagger.hilt：依赖注入
 * - io.github.rosemoe.sora.text.UndoManager：sora-editor 撤销管理器
 * 
 * 缓存策略：
 * - 使用 URI 的 SHA-256 哈希前8位作为文件名，避免特殊字符问题
 * - 所有方法加 @Synchronized 保证线程安全
 * - 异常捕获保证缓存失败不影响主流程
 */
package com.draftpeek.feature.editor.data

import android.os.Parcel
import android.util.Log
import com.draftpeek.core.common.util.AppCacheManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.rosemoe.sora.text.UndoManager
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

/**
 * 编辑器撤销状态缓存管理器（单例）
 * 
 * 职责：将 sora-editor 的 UndoManager 状态序列化保存到本地缓存文件，
 * 并在重新打开文件时恢复，使用户可以在应用重启后继续撤销/重做操作。
 * 
 * 使用场景：
 * - 文件切换时保存当前文件的撤销历史
 * - 重新打开已缓存的文件时恢复撤销历史
 * - 应用退出或清理缓存时删除所有撤销状态
 * 
 * 线程安全：所有公开方法使用 @Synchronized 注解，确保多线程访问安全。
 * 性能优化：使用 ThreadLocal 缓存 SHA-256 MessageDigest 实例，避免每次
 * hashUri 都走 Security Provider 查找和对象分配（高频标签切换路径）。
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cacheDir = AppCacheManager.getEditorUndoCacheDir(context)

    /**
     * ThreadLocal 缓存的 SHA-256 MessageDigest 实例。
     * MessageDigest 不是线程安全的，但 ThreadLocal 确保每个线程一个独立实例。
     * 使用 reset() 复用实例而非每次 getInstance()，减少 Provider 查找开销。
     */
    private val threadLocalDigest = object : ThreadLocal<java.security.MessageDigest>() {
        override fun initialValue(): java.security.MessageDigest =
            java.security.MessageDigest.getInstance("SHA-256")
    }

    /**
     * 保存指定 URI 的撤销管理器状态到缓存文件
     * 
     * 算法步骤：
     * 1. 根据 URI 生成缓存文件名（使用 SHA-256 哈希）
     * 2. 获取 Parcel 对象并将 UndoManager 写入
     * 3. 将 Parcel 序列化为字节数组
     * 4. 写入文件输出流
     * 5. 无论成功与否，回收 Parcel 对象
     * 
     * @param uri 文件的 URI 字符串
     * @param undoManager 要保存的撤销管理器实例
     * @return 保存成功返回 true，失败返回 false
     */
    @Synchronized
    fun saveUndoState(uri: String, undoManager: UndoManager): Boolean {
        return try {
            val file = cacheFileForUri(uri)
            val parcel = Parcel.obtain()
            try {
                undoManager.writeToParcel(parcel, 0)
                val bytes = parcel.marshall()
                FileOutputStream(file).use { fos ->
                    fos.write(bytes)
                }
                Log.d(TAG, "Saved undo state for ${hashUri(uri)} (${bytes.size} bytes)")
                true
            } finally {
                parcel.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveUndoState failed for $uri", e)
            false
        }
    }

    /**
     * 从缓存文件恢复指定 URI 的撤销管理器状态
     * 
     * 算法步骤：
     * 1. 检查缓存文件是否存在，不存在返回 null
     * 2. 读取文件字节数组
     * 3. 获取 Parcel 对象并反序列化字节数组
     * 4. 设置 Parcel 数据位置到起始位置
     * 5. 使用 CREATOR 从 Parcel 创建 UndoManager 实例
     * 6. 无论成功与否，回收 Parcel 对象
     * 
     * @param uri 文件的 URI 字符串
     * @return 恢复成功返回 UndoManager 实例，失败或无缓存返回 null
     */
    @Synchronized
    fun restoreUndoState(uri: String): UndoManager? {
        return try {
            val file = cacheFileForUri(uri)
            if (!file.exists()) {
                Log.d(TAG, "No cached undo state for ${hashUri(uri)}")
                return null
            }
            val bytes = file.readBytes()
            val parcel = Parcel.obtain()
            try {
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                val undoManager = UndoManager.CREATOR.createFromParcel(parcel)
                Log.d(TAG, "Restored undo state for ${hashUri(uri)} (${bytes.size} bytes)")
                undoManager
            } finally {
                parcel.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "restoreUndoState failed for $uri", e)
            null
        }
    }

    /**
     * 删除指定 URI 的撤销状态缓存文件
     * 
     * @param uri 文件的 URI 字符串
     */
    @Synchronized
    fun deleteUndoState(uri: String) {
        try {
            val file = cacheFileForUri(uri)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted undo state for ${hashUri(uri)}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteUndoState failed for $uri", e)
        }
    }

    /**
     * 清空所有编辑器撤销缓存
     * 
     * 委托给 AppCacheManager 清除整个编辑器撤销缓存目录。
     */
    @Synchronized
    fun clearAll() {
        AppCacheManager.clearEditorUndoCache(context)
    }

    /**
     * 根据 URI 生成对应的缓存文件对象
     * 
     * @param uri 文件的 URI 字符串
     * @return 缓存文件对象
     */
    private fun cacheFileForUri(uri: String): File {
        return File(cacheDir, "undo_${hashUri(uri)}.bin")
    }

    /**
     * 对 URI 进行 SHA-256 哈希，取前 8 位十六进制字符作为文件名
     * 
     * 目的：
     * - 避免 URI 中包含特殊字符（如 /、:、? 等）导致文件名非法
     * - 统一文件名长度，便于管理
     * - 部分哈希足以避免冲突（8 位十六进制 = 32 位，碰撞概率极低）
     * 
     * 优化：使用 ThreadLocal 缓存的 MessageDigest 实例，reset() 后复用，
     * 避免每次调用都触发 Security Provider 查找和新对象分配。
     * 
     * @param uri 文件的 URI 字符串
     * @return URI 的 SHA-256 哈希前 8 位十六进制字符串
     */
    private fun hashUri(uri: String): String {
        val digest = threadLocalDigest.get() ?: MessageDigest.getInstance("SHA-256").also { threadLocalDigest.set(it) }
        digest.reset()
        val hash = digest.digest(uri.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "CacheManager"
    }
}
