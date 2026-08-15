/**
 * Git 状态监控 ViewModel
 *
 * ## 功能职责
 * 提供轻量级的 Git 状态查询功能，用于编辑器状态栏等场景显示 Git 分支和文件状态。
 * 与 [GitViewModel] 不同，此 ViewModel 只负责状态读取，不提供提交/推送等写操作。
 *
 * ## 状态管理
 * - [gitRepoInfo]: Git 仓库基本信息（分支名、远程 URL 等）
 * - [gitFileStatuses]: 工作区所有文件的 Git 状态
 * - [isGitRepo]: 当前目录是否为 Git 仓库
 *
 * ## 典型使用场景
 * - 编辑器状态栏显示当前分支名
 * - 文件标签页显示 Git 修改状态标记
 * - 获取单个文件的 diff 内容
 *
 * ## 线程安全
 * - 所有 JGit 操作在 [Dispatchers.IO] 执行
 * - [currentRepo] 在 ViewModel 生命周期内持有，切换目录时先关闭旧仓库
 *
 * ## 异常处理说明
 * - Git 操作异常统一捕获并记录警告日志，失败时清空 Git 状态返回空结果
 * - [getFileDiff] 通过回调返回结果，异常时回调 null
 */
package com.draftpeek.feature.browser.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitManager
import com.draftpeek.core.common.vcs.GitRepoInfo
import com.draftpeek.core.common.vcs.GitRepository
import com.draftpeek.core.common.vcs.GitStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "GitStatusVM"

@HiltViewModel
class GitStatusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gitManager: GitManager,
) : ViewModel() {

    private val _gitRepoInfo = MutableStateFlow<GitRepoInfo?>(null)
    /** Git 仓库信息（分支名、远程、最后提交） */
    val gitRepoInfo: StateFlow<GitRepoInfo?> = _gitRepoInfo.asStateFlow()

    private val _gitFileStatuses = MutableStateFlow<List<GitFileStatus>>(emptyList())
    /** Git 文件状态列表 */
    val gitFileStatuses: StateFlow<List<GitFileStatus>> = _gitFileStatuses.asStateFlow()

    private val _isGitRepo = MutableStateFlow(false)
    /** 当前目录是否为 Git 仓库 */
    val isGitRepo: StateFlow<Boolean> = _isGitRepo.asStateFlow()

    /** 当前持有的 GitRepository 实例 */
    private var currentRepo: GitRepository? = null
    /** 当前加载的目录文件系统路径 */
    private var currentDirectoryPath: String? = null

    /**
     * 加载指定文件系统目录的 Git 状态
     *
     * - 路径为空时清空 Git 状态
     * - 相同路径已加载时跳过（避免重复加载）
     * - 切换目录时先关闭之前打开的仓库
     *
     * @param directoryPath 目录绝对路径，null 或空表示禁用 Git 功能
     */
    fun loadGitStatus(directoryPath: String?) {
        if (directoryPath.isNullOrEmpty()) {
            clearGitState()
            return
        }

        if (directoryPath == currentDirectoryPath && _isGitRepo.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentRepo?.close()
                currentRepo = null

                val repo = gitManager.findRepository(directoryPath)
                if (repo == null) {
                    clearGitState()
                    return@launch
                }

                currentRepo = repo
                currentDirectoryPath = directoryPath
                _isGitRepo.value = true
                _gitRepoInfo.value = repo.getRepoInfo()
                _gitFileStatuses.value = repo.getFileStatuses()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load Git status for $directoryPath", e)
                clearGitState()
            }
        }
    }

    /**
     * 刷新当前目录 Git 状态
     */
    fun refresh() {
        val repo = currentRepo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _gitRepoInfo.value = repo.getRepoInfo()
                _gitFileStatuses.value = repo.getFileStatuses()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh Git status", e)
            }
        }
    }

    /**
     * 获取单个文件的 Git 状态
     *
     * @param filePath 文件相对于仓库根目录的路径
     * @return GitStatus 枚举值，未找到或非 Git 仓库返回 null
     */
    fun getFileStatus(filePath: String): GitStatus? {
        return _gitFileStatuses.value.find { it.filePath == filePath }?.status
    }

    /**
     * 获取单个文件的 diff 内容
     *
     * @param filePath 文件相对于仓库根目录的路径
     * @param onResult 结果回调，diff 为字符串格式，异常或无 diff 时回调 null
     */
    fun getFileDiff(filePath: String, onResult: (String?) -> Unit) {
        val repo = currentRepo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val diff = repo.getFileDiff(filePath)
                onResult(diff)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get diff for $filePath", e)
                onResult(null)
            }
        }
    }

    /**
     * 清空所有 Git 状态并关闭仓库
     */
    private fun clearGitState() {
        currentRepo?.close()
        currentRepo = null
        currentDirectoryPath = null
        _isGitRepo.value = false
        _gitRepoInfo.value = null
        _gitFileStatuses.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        currentRepo?.close()
    }
}
