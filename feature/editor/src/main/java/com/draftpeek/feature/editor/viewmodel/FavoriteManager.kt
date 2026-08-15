/**
 * 文件功能：管理当前活动文件的收藏状态
 * 
 * 主要类：
 * - [FavoriteManager]：收藏状态管理器，负责查询和切换文件的收藏状态
 * 
 * 模块依赖：
 * - core/data：RecentFilesRepository 用于读取和更新最近文件记录
 * - kotlinx-coroutines：用于协程和 Flow 响应式状态管理
 */
package com.draftpeek.feature.editor.viewmodel

import com.draftpeek.core.data.repository.RecentFilesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 收藏状态管理器
 * 
 * 从 [EditorViewModel] 中提取，使收藏相关的状态和逻辑可以独立测试和演进。
 * 负责监听指定 URI 文件的收藏状态，并提供切换收藏状态的功能。
 * 
 * 使用场景：
 * - 编辑器顶栏收藏按钮的状态显示
 * - 用户点击收藏按钮时切换收藏状态
 * - 切换文件时自动加载新文件的收藏状态
 */
@Singleton
class FavoriteManager @Inject constructor(
    private val recentFilesRepository: RecentFilesRepository,
) {

    private val _isFavorite = MutableStateFlow(false)
    /** 当前文件是否已收藏的状态流 */
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var currentUri: String = ""

    /**
     * 开始观察指定 URI 文件的收藏状态
     * 
     * 会替换之前的观察订阅。通过 Flow 实时监听数据库中收藏状态的变化。
     * 
     * @param scope 用于启动协程的作用域
     * @param uri 要观察的文件 URI，为空时将收藏状态设为 false
     */
    fun loadFavoriteStatus(scope: CoroutineScope, uri: String) {
        currentUri = uri
        if (uri.isEmpty()) {
            _isFavorite.value = false
            return
        }
        scope.launch {
            recentFilesRepository.getRecentFile(uri).collect { file ->
                _isFavorite.value = file?.isFavorite ?: false
            }
        }
    }

    /**
     * 切换当前文件的收藏状态
     * 
     * @param uri 要切换收藏状态的文件 URI
     */
    suspend fun toggleFavorite(uri: String) {
        if (uri.isEmpty()) return
        recentFilesRepository.toggleFavorite(uri)
    }
}
