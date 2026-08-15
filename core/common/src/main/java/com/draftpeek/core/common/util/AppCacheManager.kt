/**
 * 应用缓存管理模块。
 *
 * 提供应用缓存清理功能，包括编辑器撤销缓存、内部/外部缓存目录、WebView缓存/Cookie/WebStorage等。
 * 所有清理操作在协程中执行，WebView相关操作切换到主线程执行。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用缓存管理工具对象。
 *
 * 负责清理各类应用缓存：编辑器撤销状态缓存、应用内部缓存目录、代码缓存目录、
 * 外部缓存目录、WebView缓存、WebView Cookie、WebStorage数据等。
 */
object AppCacheManager {

    private const val TAG = "AppCacheManager"
    private const val EDITOR_UNDO_CACHE_SUBDIR = "editor_undo"

    /**
     * 清除所有应用缓存（挂起函数）。
     *
     * 清理内容包括：编辑器撤销缓存、内部缓存目录、代码缓存目录、外部缓存目录、
     * WebView缓存、WebView Cookie、WebStorage数据。WebView相关操作在主线程执行。
     *
     * @param context 应用上下文
     */
    suspend fun clearAllAppCaches(context: Context) {
        Log.d(TAG, "Starting full app cache clearing...")
        val appContext = context.applicationContext

        clearEditorUndoCache(appContext)

        deleteDirContents(appContext.cacheDir)

        appContext.codeCacheDir?.let { codeCache ->
            deleteDirContents(codeCache)
        }

        appContext.externalCacheDir?.let { externalCache ->
            deleteDirContents(externalCache)
        }

        withContext(Dispatchers.Main) {
            try {
                val webView = WebView(appContext)
                webView.clearCache(true)
                webView.destroy()
                Log.d(TAG, "Cleared WebView cache")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear WebView cache", e)
            }

            try {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                Log.d(TAG, "Cleared WebView cookies")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear WebView cookies", e)
            }

            try {
                WebStorage.getInstance().deleteAllData()
                Log.d(TAG, "Cleared WebStorage data")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear WebStorage", e)
            }
        }

        Log.d(TAG, "Full app cache clearing completed")
    }

    /**
     * 清除编辑器撤销状态缓存。
     *
     * @param context 应用上下文
     */
    fun clearEditorUndoCache(context: Context) {
        try {
            val undoCacheDir = File(context.cacheDir, EDITOR_UNDO_CACHE_SUBDIR)
            if (undoCacheDir.exists() && undoCacheDir.isDirectory) {
                undoCacheDir.listFiles()?.forEach { it.delete() }
                Log.d(TAG, "Cleared all editor undo state caches")
            }
        } catch (e: Exception) {
            Log.w(TAG, "clearEditorUndoCache failed", e)
        }
    }

    /**
     * 获取编辑器撤销缓存目录。
     *
     * 如目录不存在则自动创建。
     *
     * @param context 应用上下文
     * @return 撤销缓存目录File对象
     */
    fun getEditorUndoCacheDir(context: Context): File {
        val dir = File(context.cacheDir, EDITOR_UNDO_CACHE_SUBDIR)
        dir.mkdirs()
        return dir
    }

    /**
     * 递归删除目录下的所有内容（不删除目录本身）。
     *
     * @param dir 要清空的目录
     */
    private fun deleteDirContents(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                try {
                    if (file.isDirectory) {
                        deleteDirContents(file)
                    }
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting file: ${file.absolutePath}", e)
                }
            }
        }
    }
}
