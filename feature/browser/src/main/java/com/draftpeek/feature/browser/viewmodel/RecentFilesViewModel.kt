/**
 * 最近文件与收藏管理 ViewModel
 *
 * ## 功能职责
 * 管理用户最近打开的文件列表和收藏（书签）功能：
 * - 添加/删除最近文件记录
 * - 切换文件收藏状态
 * - 批量删除最近文件
 * - 清理无效（stale）的文件记录（URI 已失效）
 *
 * ## 状态管理与数据流
 *
 * ### StateFlow 状态
 * - [recentFiles]: 最近打开文件列表（按访问时间倒序）
 * - [favorites]: 已收藏文件列表
 *
 * 数据流：
 * - 两个列表均通过 Room DAO 的 Flow 直接暴露
 * - 使用 [stateIn] 转换为热流，5秒无订阅者时停止上游（节省数据库监听资源）
 * - UI 层通过 collectAsStateWithLifecycle() 收集
 *
 * ## 数据持久化
 * - 数据存储在 Room 数据库（RecentFile 实体）
 * - 通过 RecentFilesRepository 访问
 *
 * ## 协程
 * - 所有写操作在 viewModelScope 中启动
 * - 数据库操作由 Repository 层负责调度到 IO 线程
 */
package com.draftpeek.feature.browser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.data.entity.RecentFile
import com.draftpeek.core.data.repository.RecentFilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecentFilesViewModel @Inject constructor(private val repository: RecentFilesRepository) : ViewModel() {

    /**
     * 最近打开文件列表（StateFlow）
     *
     * 通过 SharingStarted.WhileSubscribed(5000) 实现：
     * - 有订阅者时持续监听数据库变化
     * - 最后一个订阅者离开后 5 秒停止监听，避免不必要的数据库开销
     */
    val recentFiles: StateFlow<List<RecentFile>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 已收藏文件列表（StateFlow）
     */
    val favorites: StateFlow<List<RecentFile>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 添加文件到最近打开列表
     *
     * @param uri 文件 URI 字符串
     * @param fileName 文件名
     * @param language 编程语言标识（可为 null）
     * @param fileSize 文件大小（字节）
     */
    fun addRecentFile(uri: String, fileName: String, language: String?, fileSize: Long) {
        viewModelScope.launch {
            repository.addRecentFile(uri, fileName, language, fileSize)
        }
    }

    /**
     * 从最近列表中移除单个文件
     *
     * @param uri 文件 URI 字符串
     */
    fun removeRecentFile(uri: String) {
        viewModelScope.launch {
            repository.removeRecentFile(uri)
        }
    }

    /**
     * 批量从最近列表中移除文件
     *
     * @param uris 要移除的文件 URI 字符串列表
     */
    fun removeRecentFiles(uris: List<String>) {
        viewModelScope.launch {
            repository.removeRecentFiles(uris)
        }
    }

    /**
     * 切换文件收藏状态
     *
     * 如果文件已收藏则取消收藏，未收藏则添加收藏
     *
     * @param uri 文件 URI 字符串
     */
    fun toggleFavorite(uri: String) {
        viewModelScope.launch {
            repository.toggleFavorite(uri)
        }
    }

    /**
     * 清空所有最近文件记录
     *
     * 注意：此操作不影响收藏文件
     */
    fun clearAllRecentFiles() {
        viewModelScope.launch {
            repository.clearAllRecentFiles()
        }
    }

    /**
     * 清理无效（stale）记录
     *
     * 检测并移除 URI 已失效（文件被删除/权限被撤销）的记录。
     * 通常在应用启动时调用。
     */
    fun cleanupStaleRecords() {
        viewModelScope.launch {
            val staleUris = repository.getStaleUris()
            if (staleUris.isNotEmpty()) {
                repository.removeRecentFiles(staleUris)
            }
        }
    }
}
