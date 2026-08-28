/**
 * Git 操作面板 ViewModel
 *
 * ## 功能职责
 * 为 Git 操作面板提供完整的状态管理和操作入口，包括：
 * - Git 仓库状态加载与刷新（分支、远程、文件状态）
 * - 提交（commit）选中文件
 * - 推送（push）到远程仓库
 * - 拉取（pull）从远程仓库更新
 * - 获取（fetch）远程更新但不合并
 * - 切换分支（checkout）
 * - 分支列表查询
 *
 * ## 状态管理与数据流
 *
 * ### UI 状态 ([GitUiState])
 * - `branchName`: 当前分支名
 * - `remoteUrl`: 远程仓库 URL
 * - `lastCommitMessage`: 最后一次提交消息
 * - `fileStatuses`: 文件 Git 状态列表
 * - `modifiedCount`: 已修改文件数量
 * - `isGitRepo`: 是否为 Git 仓库
 * - `isCommitting/isPushing/isPulling/isFetching`: 各操作进行中标志（用于加载指示器）
 *
 * ### 事件 ([GitEvent])
 * - `ShowMessage`: 成功提示消息
 * - `ShowError`: 错误提示消息
 *
 * ### 数据流
 * ```
 * 用户操作（提交/推送/拉取等）
 *   → ViewModel 方法
 *   → withContext(Dispatchers.IO) 切到 IO 线程
 *   → gitRepoMutex.withLock 确保线程安全
 *   → GitRepository 执行 JGit 操作
 *   → 成功：loadGitStatusInternal 刷新状态 + 发送 ShowMessage
 *   → 失败：发送 ShowError
 *   → finally：重置操作进行中标志
 * ```
 *
 * ## 线程安全
 * - 所有 JGit 操作均在 [Dispatchers.IO] 执行
 * - [gitRepoMutex] 协程互斥锁保护 [currentGitRepo] 的访问和关闭
 * - [currentGitRepo] 标记 @Volatile 保证可见性
 *
 * ## 异常处理说明
 * - 所有 Git 操作包裹 try-catch，CancellationException 正常向上抛出
 * - 异常信息通过 [GitEvent.ShowError] 发送给 UI 显示
 * - 操作中状态在 finally 块中重置，确保异常时也能恢复 UI
 *
 * ## 资源释放
 * - ViewModel 销毁时将 [currentGitRepo] 置 null（注意：此 ViewModel 未调用 close()，
 *   实际仓库关闭由 FileBrowserViewModel 管理，此处仅持有引用）
 */
package com.draftpeek.feature.browser.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitManager
import com.draftpeek.core.common.vcs.GitRepository
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.feature.browser.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GitViewModel"

/**
 * Git 面板 UI 状态
 *
 * 使用 @Immutable 标记（所有属性均为 val），允许 Compose 在相同实例重复发射时跳过重组
 *
 * @property branchName 当前分支名称
 * @property remoteUrl 远程仓库 URL（如 https://github.com/owner/repo.git）
 * @property lastCommitMessage 最后一次提交的提交消息
 * @property fileStatuses 工作区文件 Git 状态列表
 * @property modifiedCount 已修改（未提交）文件数量
 * @property isGitRepo 当前目录是否为 Git 仓库
 * @property isCommitting 是否正在执行提交操作
 * @property isPushing 是否正在执行推送操作
 * @property isPulling 是否正在执行拉取操作
 * @property isFetching 是否正在执行获取操作
 */
@Immutable
data class GitUiState(
    val branchName: String = "",
    val remoteUrl: String? = null,
    val lastCommitMessage: String? = null,
    val fileStatuses: List<GitFileStatus> = emptyList(),
    val modifiedCount: Int = 0,
    val isGitRepo: Boolean = false,
    val isCommitting: Boolean = false,
    val isPushing: Boolean = false,
    val isPulling: Boolean = false,
    val isFetching: Boolean = false
)

/**
 * Git 一次性事件（用于 Snackbar/Toast 提示）
 */
sealed class GitEvent {
    /**
     * 显示成功/提示消息
     * @property message 消息文本
     */
    data class ShowMessage(val message: String) : GitEvent()

    /**
     * 显示错误消息
     * @property message 错误文本
     */
    data class ShowError(val message: String) : GitEvent()
}

@SuppressLint("StaticFieldLeak") // context 为 @ApplicationContext，不构成泄漏（lint 对 ViewModel 注入误报）
@HiltViewModel
class GitViewModel @Inject constructor(
    private val gitManager: GitManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitUiState())

    /** Git 面板 UI 状态流 */
    val uiState: StateFlow<GitUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GitEvent>()

    /** Git 一次性事件流（成功/错误提示） */
    val events: SharedFlow<GitEvent> = _events.asSharedFlow()

    @Volatile
    private var currentGitRepo: GitRepository? = null

    /** Git 仓库访问互斥锁 */
    private val gitRepoMutex = Mutex()

    /** 当前浏览目录的文件系统绝对路径 */
    private var currentDirectoryPath: String? = null

    /**
     * 加载指定 SAF URI 对应目录的 Git 状态
     *
     * 入口方法：将 SAF URI 解析为文件系统路径后调用 [loadGitStatusInternal]
     *
     * @param treeUri 当前浏览目录的 SAF URI
     */
    fun loadGitStatus(treeUri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val fsPath = safUriToFilePath(treeUri) ?: return@withContext
                currentDirectoryPath = fsPath
                loadGitStatusInternal(fsPath)
            }
        }
    }

    /**
     * 内部方法：加载指定文件系统路径的 Git 状态
     *
     * 流程：
     * 1. 通过互斥锁安全关闭之前打开的 GitRepository
     * 2. 使用 [GitManager.findRepository] 向上查找 .git 目录
     * 3. 找到则读取仓库信息和文件状态并更新 UI
     * 4. 未找到则清空 Git 状态
     *
     * @param fsPath 目录文件系统绝对路径
     */
    private suspend fun loadGitStatusInternal(fsPath: String) {
        try {
            gitRepoMutex.withLock {
                currentGitRepo?.close()
                currentGitRepo = null
            }

            val repo = gitManager.findRepository(fsPath)
            if (repo == null) {
                _uiState.value = GitUiState(isGitRepo = false)
                return
            }

            gitRepoMutex.withLock {
                currentGitRepo = repo
            }

            val repoInfo = repo.getRepoInfo()
            val statuses = repo.getFileStatuses()
            val modifiedCount = statuses.count { it.status != GitStatus.UNMODIFIED }

            _uiState.value = GitUiState(
                branchName = repoInfo.branchName,
                remoteUrl = repoInfo.remoteUrl,
                lastCommitMessage = repoInfo.lastCommitMessage,
                fileStatuses = statuses,
                modifiedCount = modifiedCount,
                isGitRepo = true
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Git status", e)
            _uiState.value = GitUiState(isGitRepo = false)
        }
    }

    /**
     * 刷新当前目录 Git 状态
     */
    fun refresh() {
        val path = currentDirectoryPath ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadGitStatusInternal(path)
            }
        }
    }

    /**
     * 执行 Git 提交操作
     *
     * @param message 提交消息（不能为空）
     * @param selectedFilePaths 要提交的文件相对路径集合（暂存并提交）
     *
     * 异常处理：
     * - 空消息：直接发送错误事件不执行
     * - Git 操作异常：捕获后发送 ShowError 事件
     * - finally 块确保 isCommitting 被重置
     */
    fun commit(message: String, selectedFilePaths: Set<String>) {
        if (message.isBlank()) {
            viewModelScope.launch {
                _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_empty_message)))
            }
            return
        }
        _uiState.value = _uiState.value.copy(isCommitting = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val repo = gitRepoMutex.withLock { currentGitRepo }
                    if (repo == null) {
                        _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_no_repo)))
                        return@withContext
                    }

                    repo.commit(message, selectedFilePaths.toList())

                    _events.emit(GitEvent.ShowMessage(context.getString(R.string.browser_git_commit_success)))
                    loadGitStatusInternal(currentDirectoryPath ?: return@withContext)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Commit failed", e)
                    _events.emit(
                        GitEvent.ShowError(
                            context.getString(R.string.browser_git_commit_failed, e.message ?: "")
                        )
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(isCommitting = false)
                }
            }
        }
    }

    /**
     * 执行 Git 推送操作（git push）
     *
     * 将本地提交推送到远程仓库。操作期间 isPushing 为 true，UI 显示加载状态
     */
    fun push() {
        _uiState.value = _uiState.value.copy(isPushing = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val repo = gitRepoMutex.withLock { currentGitRepo }
                    if (repo == null) {
                        _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_no_repo)))
                        return@withContext
                    }

                    repo.push()

                    _events.emit(GitEvent.ShowMessage(context.getString(R.string.browser_git_push_success)))
                    loadGitStatusInternal(currentDirectoryPath ?: return@withContext)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Push failed", e)
                    _events.emit(
                        GitEvent.ShowError(
                            context.getString(R.string.browser_git_push_failed, e.message ?: "")
                        )
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(isPushing = false)
                }
            }
        }
    }

    /**
     * 执行 Git 拉取操作（git pull）
     *
     * 从远程仓库拉取并合并更新。操作期间 isPulling 为 true
     */
    fun pull() {
        _uiState.value = _uiState.value.copy(isPulling = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val repo = gitRepoMutex.withLock { currentGitRepo }
                    if (repo == null) {
                        _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_no_repo)))
                        return@withContext
                    }

                    repo.pull()

                    _events.emit(GitEvent.ShowMessage(context.getString(R.string.browser_git_pull_success)))
                    loadGitStatusInternal(currentDirectoryPath ?: return@withContext)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Pull failed", e)
                    _events.emit(
                        GitEvent.ShowError(
                            context.getString(R.string.browser_git_pull_failed, e.message ?: "")
                        )
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(isPulling = false)
                }
            }
        }
    }

    /**
     * 执行 Git 获取操作（git fetch）
     *
     * 从远程仓库获取更新但不自动合并。操作期间 isFetching 为 true
     */
    fun fetch() {
        _uiState.value = _uiState.value.copy(isFetching = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val repo = gitRepoMutex.withLock { currentGitRepo }
                    if (repo == null) {
                        _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_no_repo)))
                        return@withContext
                    }

                    repo.fetch()

                    _events.emit(GitEvent.ShowMessage(context.getString(R.string.browser_git_fetch_success)))
                    loadGitStatusInternal(currentDirectoryPath ?: return@withContext)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Fetch failed", e)
                    _events.emit(
                        GitEvent.ShowError(
                            context.getString(R.string.browser_git_fetch_failed, e.message ?: "")
                        )
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(isFetching = false)
                }
            }
        }
    }

    /**
     * 切换 Git 分支（git checkout）
     *
     * @param branch 目标分支名称
     */
    fun checkout(branch: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val repo = gitRepoMutex.withLock { currentGitRepo }
                    if (repo == null) {
                        _events.emit(GitEvent.ShowError(context.getString(R.string.browser_git_error_no_repo)))
                        return@withContext
                    }

                    repo.checkout(branch)

                    _events.emit(
                        GitEvent.ShowMessage(
                            context.getString(R.string.browser_git_checkout_success, branch)
                        )
                    )
                    loadGitStatusInternal(currentDirectoryPath ?: return@withContext)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Checkout failed", e)
                    _events.emit(
                        GitEvent.ShowError(
                            context.getString(R.string.browser_git_checkout_failed, e.message ?: "")
                        )
                    )
                }
            }
        }
    }

    /**
     * 获取所有本地分支名称列表
     *
     * @return 分支名列表，异常时返回空列表
     */
    fun getBranches(): List<String> {
        val repo = currentGitRepo ?: return emptyList()
        return try {
            repo.listBranches()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list branches", e)
            emptyList()
        }
    }

    /**
     * 将 SAF URI 转换为文件系统路径（内部使用）
     *
     * 支持的 URI 格式：
     * - content://com.android.externalstorage.documents（SAF 外置存储）
     * - file://（本地文件 URI）
     *
     * @param treeUri SAF 目录树 URI
     * @return 文件系统绝对路径，无法解析时返回 null
     */
    private fun safUriToFilePath(treeUri: Uri): String? {
        val uriString = treeUri.toString()

        if (uriString.startsWith("content://com.android.externalstorage.documents")) {
            val lastSegment = treeUri.lastPathSegment ?: return null
            val colonIndex = lastSegment.indexOf(':')
            if (colonIndex < 0) return null

            val volume = lastSegment.substring(0, colonIndex)
            val relativePath = lastSegment.substring(colonIndex + 1)

            val externalStorage = Environment.getExternalStorageDirectory().absolutePath
            return when (volume) {
                "primary", "home" -> "$externalStorage/$relativePath"
                else -> "/storage/$volume/$relativePath"
            }
        }

        if (uriString.startsWith("file://")) {
            return treeUri.path
        }

        return null
    }

    override fun onCleared() {
        super.onCleared()
        currentGitRepo = null
    }
}
