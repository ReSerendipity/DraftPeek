/**
 * 文件浏览器模块 ViewModel 层
 *
 * 本包包含文件浏览器功能的所有 ViewModel，负责：
 * - 文件列表加载与导航状态管理
 * - Git 仓库状态监控与操作
 * - 代码片段（Snippet）CRUD 与搜索
 * - 最近文件与收藏管理
 */
package com.draftpeek.feature.browser.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.event.AppEventBus
import com.draftpeek.core.common.event.BrowserEvent
import com.draftpeek.core.common.event.EditorEvent
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.util.OutputThrottler
import com.draftpeek.core.common.vcs.CloneResult
import com.draftpeek.core.common.vcs.GitFileStatus
import com.draftpeek.core.common.vcs.GitStatus
import com.draftpeek.core.common.vcs.GitHubApiClient
import com.draftpeek.core.common.vcs.GitHubTreeResult
import com.draftpeek.core.common.vcs.GitManager
import com.draftpeek.core.common.vcs.GitRepoInfo
import com.draftpeek.core.common.vcs.GitRepository
import com.draftpeek.feature.browser.model.BrowserUiState
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.model.FileSortOption
import com.draftpeek.feature.browser.model.GitHubImportState
import com.draftpeek.feature.browser.model.sortFiles
import com.draftpeek.feature.browser.R
import com.draftpeek.feature.browser.observer.DirectoryObserver
import com.draftpeek.feature.browser.repository.FileContentResult
import com.draftpeek.feature.browser.repository.FileRepository
import com.draftpeek.feature.settings.repository.SettingsRepository
import com.draftpeek.core.data.repository.BookmarkRepository
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.usecase.ManageBookmarksUseCase
import com.draftpeek.core.domain.usecase.RecordUserActivityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "FileBrowserVM"

/**
 * 文件浏览器主界面 ViewModel
 *
 * ## 状态管理与数据流
 *
 * ### UI 状态
 * - [uiState]: 浏览器主状态（Idle/Loading/Success/Error），通过 [BrowserUiState] 密封类管理
 * - [internalFiles]: 应用内部存储文件列表（app sandbox 目录）
 * - [currentSortOption]: 当前文件排序方式
 * - [pinnedFiles]: 置顶文件 URI 集合
 * - [bookmarkedUris]: 收藏文件 URI 集合
 *
 * ### 导航状态
 * - [currentTreeUri]: 当前浏览的目录 SAF URI
 * - [navigationStack]: 导航栈，支持返回上级目录
 * - [navigationDisplayPath]: 用户可见的路径显示文本
 *
 * ### Git 状态
 * - [isGitRepo]: 当前目录是否为 Git 仓库
 * - [gitRepoInfo]: Git 仓库信息（分支名、远程 URL、最后提交信息）
 * - [gitFileStatuses]: Git 文件状态列表（已修改/已暂存等）
 *
 * ### GitHub 导入状态
 * - [gitHubImportState]: GitHub 导入流程状态（Idle/Loading/FileList/Cloning/Success/Error）
 * - [gitHubSelectedFiles]: 用户选择导入的文件路径集合
 *
 * ## 数据流架构
 * ```
 * 用户操作 (SAF 选目录/导航/排序)
 *   → ViewModel 方法调用
 *   → FileRepository.listFiles() / GitManager
 *   → MutableStateFlow 更新
 *   → Compose collectAsStateWithLifecycle() 重组
 *
 * DirectoryObserver (文件系统变更监听)
 *   → refreshEvents Flow
 *   → 自动重新加载当前目录
 *
 * AppEventBus (编辑器保存事件)
 *   → EditorEvent.FileSaved
 *   → 刷新当前文件列表
 * ```
 *
 * ## 线程安全
 * - 所有文件 I/O 和 Git 操作均在 [Dispatchers.IO] 上执行
 * - [gitRepoMutex] 用于协程间安全访问 [currentGitRepo]，避免 JGit 并发问题
 * - [DirectoryObserver] 在 ViewModel 生命周期内注册/注销
 *
 * ## 异常处理说明
 * - SAF 文件列表加载失败通过 [catch] 操作符捕获，转换为 [BrowserUiState.Error]
 * - Git 操作异常统一捕获并记录日志，失败时清空 Git 状态而非崩溃
 * - 外部文件保存到内部存储时，文件名冲突重试上限为 [MAX_NAME_COLLISION_RETRIES]，超限则中止防止覆盖
 *
 * ## 资源释放
 * - [onCleared] 中关闭 JGit Repository 释放文件句柄和锁
 * - 使用守护线程 + 200ms join 超时，既避免 ANR 又尽量确保锁释放
 */
@SuppressLint("StaticFieldLeak") // context 为 @ApplicationContext，不构成泄漏（lint 对 ViewModel 注入误报）
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val repository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val userActivityRepository: UserActivityRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val manageBookmarks: ManageBookmarksUseCase,
    private val recordUserActivity: RecordUserActivityUseCase,
    private val appEventBus: AppEventBus,
    private val gitManager: GitManager,
    private val gitHubApiClient: GitHubApiClient,
    private val directoryObserver: DirectoryObserver,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BrowserUiState>(BrowserUiState.Idle)
    /** 浏览器主 UI 状态流 */
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _currentTreeUri = MutableStateFlow<Uri?>(null)
    /** 当前浏览的目录 SAF URI */
    val currentTreeUri: StateFlow<Uri?> = _currentTreeUri.asStateFlow()

    private val _gitRepoInfo = MutableStateFlow<GitRepoInfo?>(null)
    /** Git 仓库信息（分支、远程、最后提交） */
    val gitRepoInfo: StateFlow<GitRepoInfo?> = _gitRepoInfo.asStateFlow()

    private val _gitFileStatuses = MutableStateFlow<ImmutableList<GitFileStatus>>(persistentListOf())
    /** Git 文件状态不可变列表 */
    val gitFileStatuses: StateFlow<ImmutableList<GitFileStatus>> = _gitFileStatuses.asStateFlow()

    private val _isGitRepo = MutableStateFlow(false)
    /** 当前目录是否为 Git 仓库 */
    val isGitRepo: StateFlow<Boolean> = _isGitRepo.asStateFlow()

    private val _internalFiles = MutableStateFlow<ImmutableList<FileItem>>(persistentListOf())
    /** 应用内部存储文件列表 */
    val internalFiles: StateFlow<ImmutableList<FileItem>> = _internalFiles.asStateFlow()

    private val _currentSortOption = MutableStateFlow(FileSortOption.NAME_ASC)
    /** 当前文件排序选项 */
    val currentSortOption: StateFlow<FileSortOption> = _currentSortOption.asStateFlow()

    private val _pinnedFiles = MutableStateFlow<Set<String>>(emptySet())
    /** 置顶文件 URI 字符串集合 */
    val pinnedFiles: StateFlow<Set<String>> = _pinnedFiles.asStateFlow()

    private val _bookmarkedUris = MutableStateFlow<Set<String>>(emptySet())
    /** 收藏文件 URI 字符串集合 */
    val bookmarkedUris: StateFlow<Set<String>> = _bookmarkedUris.asStateFlow()

    // ===== 首页文件列表自定义顺序 =====
    private val _pinnedOrder = MutableStateFlow<List<String>>(emptyList())
    /** 置顶/收藏（Pinned）文件自定义排序 URI 列表 */
    val pinnedOrder: StateFlow<List<String>> = _pinnedOrder.asStateFlow()

    private val _recentOrder = MutableStateFlow<List<String>>(emptyList())
    /** 最近文件自定义排序 URI 列表 */
    val recentOrder: StateFlow<List<String>> = _recentOrder.asStateFlow()

    private val _internalFilesOrder = MutableStateFlow<List<String>>(emptyList())
    /** 内部存储文件自定义排序 URI 列表 */
    val internalFilesOrder: StateFlow<List<String>> = _internalFilesOrder.asStateFlow()

    private val _bookmarkOrder = MutableStateFlow<List<String>>(emptyList())
    /** 书签文件自定义排序 URI 列表 */
    val bookmarkOrder: StateFlow<List<String>> = _bookmarkOrder.asStateFlow()

    /** 首页"最近打开"区块显示数量（来自设置，默认 3） */
    val recentFilesLimit: StateFlow<Int> = settingsRepository.settings
        .map { it.recentFilesLimit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    private val _navigationStack = MutableStateFlow<List<Uri>>(emptyList())
    /** 导航栈（内部使用） */
    private val navigationStack: List<Uri> get() = _navigationStack.value

    private val _navigationDisplayPath = MutableStateFlow<String>("")
    /** 面包屑导航显示路径 */
    val navigationDisplayPath: StateFlow<String> = _navigationDisplayPath.asStateFlow()

    @Volatile
    private var currentGitRepo: GitRepository? = null
    /** Git 仓库访问互斥锁，防止 JGit 并发问题 */
    private val gitRepoMutex = Mutex()

    private val _gitHubImportState = MutableStateFlow<GitHubImportState>(GitHubImportState.Idle)
    /** GitHub 导入流程状态 */
    val gitHubImportState: StateFlow<GitHubImportState> = _gitHubImportState.asStateFlow()

    private val _gitHubSelectedFiles = MutableStateFlow<Set<String>>(emptySet())
    /** GitHub 导入中用户选中的文件路径集合 */
    val gitHubSelectedFiles: StateFlow<Set<String>> = _gitHubSelectedFiles.asStateFlow()

    /** DirectoryObserver 刷新事件收集协程 Job */
    private var observerCollectionJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            Log.e(TAG, "Uncaught exception in FileBrowserViewModel coroutine", throwable)
        }
    }

    /**
     * Launch a fire-and-forget IO coroutine with exception handling.
     */
    private fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            block()
        }
    }

    init {
        loadInternalFiles()
        loadPinnedFiles()
        loadBookmarks()
        collectObserverEvents()
        collectEditorEvents()
        collectListOrders()
    }

    /**
     * 从 SettingsRepository 收集首页各列表的自定义排序
     */
    private fun collectListOrders() {
        viewModelScope.launch {
            settingsRepository.getPinnedOrder().collect { order ->
                _pinnedOrder.value = order
            }
        }
        viewModelScope.launch {
            settingsRepository.getRecentOrder().collect { order ->
                _recentOrder.value = order
            }
        }
        viewModelScope.launch {
            settingsRepository.getInternalFilesOrder().collect { order ->
                _internalFilesOrder.value = order
                refreshInternalFiles()
            }
        }
        viewModelScope.launch {
            settingsRepository.getBookmarkOrder().collect { order ->
                _bookmarkOrder.value = order
            }
        }
    }

    /** 保存置顶/收藏文件自定义顺序 */
    fun savePinnedOrder(order: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _pinnedOrder.value = order
            settingsRepository.setPinnedOrder(order)
        }
    }

    /** 保存最近文件自定义顺序 */
    fun saveRecentOrder(order: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _recentOrder.value = order
            settingsRepository.setRecentOrder(order)
        }
    }

    /** 保存内部存储文件自定义顺序 */
    fun saveInternalFilesOrder(order: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _internalFilesOrder.value = order
            settingsRepository.setInternalFilesOrder(order)
        }
    }

    /** 保存书签文件自定义顺序 */
    fun saveBookmarkOrder(order: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _bookmarkOrder.value = order
            settingsRepository.setBookmarkOrder(order)
        }
    }

    /**
     * 收集 [DirectoryObserver] 的文件刷新事件，自动重新加载当前目录
     *
     * 在 ViewModel 生命周期内持续运行。根据 URI 类型自动选择：
     * - 应用内部 URI → [loadInternalDirectoryContents]
     * - SAF URI → [loadFiles]
     */
    private fun collectObserverEvents() {
        observerCollectionJob = viewModelScope.launch {
            directoryObserver.refreshEvents.collect {
                val currentUri = _currentTreeUri.value
                if (currentUri != null) {
                    if (AppFileManager.isInternalUri(currentUri.toString())) {
                        loadInternalDirectoryContents(currentUri)
                    } else {
                        loadFiles(currentUri)
                    }
                }
            }
        }
    }

    /**
     * 收集编辑器事件，文件保存时刷新当前目录
     */
    private fun collectEditorEvents() {
        viewModelScope.launch {
            appEventBus.events
                .collect { event ->
                    if (event is EditorEvent.FileSaved) {
                        val currentUri = _currentTreeUri.value
                        if (currentUri != null) {
                            loadFiles(currentUri)
                        } else {
                            refreshInternalFiles()
                        }
                    }
                }
        }
    }

    /**
     * 从 SettingsRepository 加载置顶文件列表，变更时刷新内部文件
     */
    private fun loadPinnedFiles() {
        viewModelScope.launch {
            settingsRepository.getPinnedFiles().collect { pinned ->
                _pinnedFiles.value = pinned
                refreshInternalFiles()
            }
        }
    }

    /**
     * 加载应用内部存储（user_files）的文件列表
     *
     * 在 [Dispatchers.IO] 上执行直接 java.io.File 访问（比 SAF DocumentFile 更快）
     */
    private fun loadInternalFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = AppFileManager.listUserFiles(context)
            val pinned = _pinnedFiles.value
            val bookmarked = _bookmarkedUris.value
            val order = _internalFilesOrder.value
            val fileItems = files.map { file ->
                val uri = AppFileManager.fileToInternalUri(file)
                FileItem(
                    name = file.name,
                    uri = Uri.parse(uri),
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    isPinned = pinned.contains(uri),
                    extension = file.name.substringAfterLast('.', ""),
                    isReadOnly = !file.canWrite(),
                    isBookmarked = bookmarked.contains(uri),
                )
            }.sortFiles(_currentSortOption.value)
                .applyCustomOrder(order) { it.uri.toString() }
            _internalFiles.value = fileItems.toImmutableList()
        }
    }

    /**
     * 对列表应用自定义顺序：出现在 order 中的元素按 order 的索引排到前面，其余保持原顺序。
     */
    private fun <T> List<T>.applyCustomOrder(
        order: List<String>,
        keyOf: (T) -> String,
    ): List<T> {
        if (order.isEmpty()) return this
        val orderMap = order.withIndex().associate { it.value to it.index }
        return this.sortedBy { t -> orderMap[keyOf(t)] ?: Int.MAX_VALUE }
    }

    /** 刷新内部存储文件列表 */
    fun refreshInternalFiles() { loadInternalFiles() }

    /**
     * 用户通过 SAF 选择目录后的入口
     *
     * 清空之前的 Git 状态防止脏数据，初始化导航栈，加载文件列表和 Git 状态，注册文件观察者
     *
     * @param treeUri 用户授予权限的 SAF 目录树 URI
     */
    fun onTreeUriSelected(treeUri: Uri) {
        viewModelScope.launch {
            repository.takeUriPermission(treeUri)
            clearGitState()
            _uiState.value = BrowserUiState.Loading
            _currentTreeUri.value = treeUri
            _navigationStack.value = listOf(treeUri)
            _navigationDisplayPath.value = safUriToDisplayPath(treeUri)
            recordUserActivity.recordFileOpen()
            applyDirectorySortPreference(treeUri)
            loadFiles(treeUri)
            loadGitStatus(treeUri)
            registerFileObserver(treeUri)
        }
    }

    /**
     * 导航到指定目录（文件夹点击事件）
     *
     * @param item 目标目录项
     */
    fun navigateTo(item: FileItem) {
        if (!item.isDirectory) return
        viewModelScope.launch {
            val newStack = _navigationStack.value + item.uri
            _navigationStack.value = newStack
            _currentTreeUri.value = item.uri
            _navigationDisplayPath.value = buildDisplayPath(newStack)
            applyDirectorySortPreference(item.uri)
            if (AppFileManager.isInternalUri(item.uri.toString())) {
                loadInternalDirectoryContents(item.uri)
            } else {
                loadFiles(item.uri)
                loadGitStatus(item.uri)
                registerFileObserver(item.uri)
            }
        }
    }

    /**
     * 加载应用内部目录内容（直接 java.io.File 访问）
     *
     * @param dirUri 内部目录 URI（content://com.draftpeek.fileprovider 或 file:// 格式）
     * @throws SecurityException 当无法访问目录时返回错误状态
     */
    private fun loadInternalDirectoryContents(dirUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BrowserUiState.Loading
            val path = dirUri.path ?: return@launch
            val dir = File(path)
            if (!dir.isDirectory) {
                _uiState.value = BrowserUiState.Error(context.getString(R.string.browser_error_not_directory))
                return@launch
            }
            val pinned = _pinnedFiles.value
            val bookmarked = _bookmarkedUris.value
            val fileItems = dir.listFiles()?.map { file ->
                val fileUri = AppFileManager.fileToInternalUri(file)
                FileItem(
                    name = file.name,
                    uri = Uri.parse(fileUri),
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    isPinned = pinned.contains(fileUri),
                    extension = file.name.substringAfterLast('.', ""),
                    isReadOnly = !file.canWrite(),
                    isBookmarked = bookmarked.contains(fileUri),
                )
            }?.sortFiles(_currentSortOption.value)?.toImmutableList() ?: persistentListOf()
            val displayPath = _navigationDisplayPath.value.ifEmpty {
                dir.absolutePath.substringAfterLast("/user_files/").ifEmpty { context.getString(R.string.browser_internal_storage) }
            }
            _uiState.value = BrowserUiState.Success(files = fileItems, currentPath = displayPath)
            registerFileObserver(dirUri)
        }
    }

    /**
     * 导航到上级目录
     *
     * @return true 表示发生了导航，false 表示已在根目录返回首页
     */
    fun navigateUp(): Boolean {
        val stack = _navigationStack.value
        if (stack.size <= 1) {
            _navigationStack.value = emptyList()
            _currentTreeUri.value = null
            _navigationDisplayPath.value = ""
            _uiState.value = BrowserUiState.Idle
            loadInternalFiles()
            return true
        }
        val parentUri = stack[stack.lastIndex - 1]
        val newStack = stack.dropLast(1)
        viewModelScope.launch {
            _navigationStack.value = newStack
            _currentTreeUri.value = parentUri
            _navigationDisplayPath.value = buildDisplayPath(newStack)
            applyDirectorySortPreference(parentUri)
            if (AppFileManager.isInternalUri(parentUri.toString())) {
                loadInternalDirectoryContents(parentUri)
            } else {
                loadFiles(parentUri)
                loadGitStatus(parentUri)
                registerFileObserver(parentUri)
            }
        }
        return true
    }

    /** 手动刷新当前目录内容和 Git 状态 */
    fun refresh() {
        val uri = _currentTreeUri.value ?: return
        viewModelScope.launch {
            loadFiles(uri)
            loadGitStatus(uri)
        }
    }

    /** 记录文件打开用户活动 */
    fun recordFileOpen() { launchIo { recordUserActivity.recordFileOpen() } }
    /** 记录其他操作用户活动 */
    fun recordOtherOperation() { launchIo { recordUserActivity.recordFileManagement() } }

    /**
     * 加载 SAF 目录的文件列表
     *
     * 异常处理：通过 [catch] 操作符捕获 SAF 访问异常，转换为用户友好的错误消息
     *
     * @param treeUri SAF 目录树 URI
     */
    private suspend fun loadFiles(treeUri: Uri) {
        _uiState.value = BrowserUiState.Loading
        val displayPath = safUriToDisplayPath(treeUri)
        repository.listFiles(treeUri)
            .catch { e ->
                _uiState.value = BrowserUiState.Error(
                    context.getString(R.string.browser_error_load_failed, displayPath)
                )
            }
            .collect { files ->
                val pinned = _pinnedFiles.value
                val bookmarked = _bookmarkedUris.value
                val gitStatusMap = buildGitStatusMap()
                val enrichedFiles = files.map { file ->
                    val withPinnedAndBookmark = file.copy(
                        isPinned = pinned.contains(file.uri.toString()),
                        isBookmarked = bookmarked.contains(file.uri.toString()),
                    )
                    enrichWithGitStatus(withPinnedAndBookmark, gitStatusMap)
                }
                val sortedFiles = enrichedFiles.sortFiles(_currentSortOption.value).toImmutableList()
                _uiState.value = BrowserUiState.Success(files = sortedFiles, currentPath = displayPath)
            }
    }

    /**
     * 构建 filePath → [GitStatus] 的查找表。
     *
     * **算法优化**：原实现在 [enrichWithGitStatus] 内对每个文件对 Git 状态列表做线性
     * `find` 扫描，形成 O(F×G) 嵌套查找（F=文件数，G=状态条目数）。改为一次性构建
     * 哈希表后，单个文件查找降为 O(1)，总体降为 O(F+G)。
     *
     * 使用 `putIfAbsent` 保留首个匹配，与原 `find` 的“首个命中”语义完全一致。
     */
    private fun buildGitStatusMap(): Map<String, GitStatus> {
        if (!_isGitRepo.value) return emptyMap()
        return buildMap {
            for (fileStatus in _gitFileStatuses.value) {
                putIfAbsent(fileStatus.filePath, fileStatus.status)
            }
        }
    }

    /**
     * 为文件项附加 Git 状态信息
     *
     * @param file 原始文件项
     * @param gitStatusMap 预先构建的 filePath → status 查找表（见 [buildGitStatusMap]）
     * @return 附加了 gitStatus 的文件项，如果不是 Git 仓库则原样返回
     */
    private fun enrichWithGitStatus(file: FileItem, gitStatusMap: Map<String, GitStatus>): FileItem {
        if (!_isGitRepo.value) return file
        val gitStatus = gitStatusMap[getRelativePath(file.name)] ?: gitStatusMap[file.name]
        return file.copy(gitStatus = gitStatus)
    }

    /**
     * 更改文件排序选项
     *
     * 同时保存该目录的排序偏好到 SettingsRepository，下次访问时自动恢复
     *
     * @param sortOption 目标排序选项
     */
    fun changeSortOption(sortOption: FileSortOption) {
        _currentSortOption.value = sortOption
        val currentUri = _currentTreeUri.value
        if (currentUri != null) {
            viewModelScope.launch {
                settingsRepository.setDirectorySortOption(currentUri.toString(), sortOption.name)
            }
        }
        _uiState.update { state ->
            if (state is BrowserUiState.Success) {
                state.copy(files = state.files.sortFiles(sortOption).toImmutableList())
            } else {
                state
            }
        }
    }

    /**
     * 恢复指定目录的排序偏好
     *
     * 如果没有保存的偏好或解析失败，保持当前排序选项不变
     */
    private suspend fun applyDirectorySortPreference(directoryUri: Uri) {
        settingsRepository.getDirectorySortOption(directoryUri.toString()).first()?.let { savedName ->
            try { _currentSortOption.value = FileSortOption.valueOf(savedName) } catch (_: IllegalArgumentException) { }
        }
    }

    /**
     * 加载当前目录的 Git 状态
     *
     * 在 [Dispatchers.IO] 上执行文件系统操作。使用 [gitRepoMutex] 保证线程安全。
     * 路径解析失败或非 Git 仓库时清空 Git 状态。
     *
     * @param treeUri 当前浏览目录的 SAF URI
     */
    private suspend fun loadGitStatus(treeUri: Uri) {
        withContext(Dispatchers.IO) {
            val fsPath = safUriToFilePath(treeUri)
            if (fsPath == null) {
                clearGitState()
                return@withContext
            }

            try {
                gitRepoMutex.withLock {
                    currentGitRepo?.close()
                    currentGitRepo = null
                }

                val repo = gitManager.findRepository(fsPath)
                if (repo == null) {
                    clearGitState()
                    return@withContext
                }

                gitRepoMutex.withLock {
                    currentGitRepo = repo
                }
                _isGitRepo.value = true
                _gitRepoInfo.value = repo.getRepoInfo()
                _gitFileStatuses.value = repo.getFileStatuses().toImmutableList()

                val currentState = _uiState.value
                if (currentState is BrowserUiState.Success) {
                    val gitStatusMap = buildGitStatusMap()
                    val enrichedFiles = currentState.files.map { enrichWithGitStatus(it, gitStatusMap) }
                    _uiState.value = currentState.copy(files = enrichedFiles.toImmutableList())
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enrich files with Git status", e)
                clearGitState()
            }
        }
    }

    /**
     * 将 SAF content URI 转换为文件系统绝对路径（内部使用）
     *
     * **仅用于 Git 操作和 DirectoryObserver，绝对不暴露给 UI 或错误消息**
     *
     * 安全处理：使用 [File.getCanonicalPath] 规范化路径，防止 `../` 路径遍历攻击
     * 支持的 URI 格式：
     * - content://com.android.externalstorage.documents（SAF 外置存储）
     * - file://（本地文件 URI）
     *
     * @param treeUri SAF 目录树 URI
     * @return 规范化后的文件系统路径，无法解析时返回 null
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
            val rawPath = when (volume) {
                "primary", "home" -> if (relativePath.isEmpty()) externalStorage else "$externalStorage/$relativePath"
                else -> if (relativePath.isEmpty()) "/storage/$volume" else "/storage/$volume/$relativePath"
            }
            return try {
                File(rawPath).canonicalPath
            } catch (e: Exception) {
                null
            }
        }

        if (uriString.startsWith("file://")) {
            val path = treeUri.path ?: return null
            return try {
                File(path).canonicalPath
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    /**
     * 将 SAF content URI 转换为用户友好的显示路径
     *
     * **绝不暴露** /storage/emulated/0/ 等系统路径
     *
     * @param treeUri SAF 目录树 URI
     * @return 用户可见的路径字符串（如"内部存储/Documents"）
     */
    private fun safUriToDisplayPath(treeUri: Uri): String {
        val uriString = treeUri.toString()

        if (AppFileManager.isInternalUri(uriString)) {
            val path = treeUri.path ?: return context.getString(R.string.browser_internal_storage)
            return path.substringAfterLast("/user_files/").ifEmpty { context.getString(R.string.browser_internal_storage) }
        }

        if (uriString.startsWith("content://com.android.externalstorage.documents")) {
            val lastSegment = treeUri.lastPathSegment ?: return context.getString(R.string.browser_storage)
            val colonIndex = lastSegment.indexOf(':')
            if (colonIndex < 0) return context.getString(R.string.browser_storage)

            val volume = lastSegment.substring(0, colonIndex)
            val relativePath = lastSegment.substring(colonIndex + 1)

            val volumeName = when (volume) {
                "primary", "home" -> context.getString(R.string.browser_internal_storage)
                else -> context.getString(R.string.browser_sd_card, volume)
            }

            return if (relativePath.isEmpty()) {
                volumeName
            } else {
                "$volumeName/$relativePath"
            }
        }

        if (uriString.startsWith("content://")) {
            val docId = treeUri.lastPathSegment ?: return context.getString(R.string.browser_storage)
            val colonIndex = docId.indexOf(':')
            if (colonIndex >= 0) {
                val relativePath = docId.substring(colonIndex + 1)
                return relativePath.ifEmpty { context.getString(R.string.browser_storage) }
            }
            return docId.substringAfterLast('/').ifEmpty { context.getString(R.string.browser_storage) }
        }

        if (uriString.startsWith("file://")) {
            val path = treeUri.path ?: return context.getString(R.string.browser_storage)
            return path.substringAfterLast('/').ifEmpty { context.getString(R.string.browser_storage) }
        }

        return context.getString(R.string.browser_storage)
    }

    /**
     * 根据导航栈构建完整的面包屑显示路径
     */
    private fun buildDisplayPath(stack: List<Uri>): String {
        if (stack.isEmpty()) return ""
        if (stack.size == 1) return safUriToDisplayPath(stack.first())

        val rootDisplay = safUriToDisplayPath(stack.first())
        val childSegments = stack.drop(1).map { uri ->
            getFolderDisplayNameFromUri(uri)
        }.filter { it.isNotEmpty() }
        return if (childSegments.isEmpty()) {
            rootDisplay
        } else {
            "$rootDisplay/${childSegments.joinToString("/")}"
        }
    }

    /**
     * 从文档 URI 中提取单纯的文件夹名称（用于面包屑）
     */
    private fun getFolderDisplayNameFromUri(uri: Uri): String {
        val uriString = uri.toString()
        if (AppFileManager.isInternalUri(uriString)) {
            return uri.path?.substringAfterLast('/') ?: ""
        }
        if (uriString.startsWith("content://")) {
            val lastSegment = uri.lastPathSegment ?: return ""
            return lastSegment.substringAfterLast('/').substringAfterLast(':')
        }
        if (uriString.startsWith("file://")) {
            return uri.path?.substringAfterLast('/') ?: ""
        }
        return ""
    }

    /** 获取文件相对于仓库根目录的路径 */
    private fun getRelativePath(fileName: String): String = fileName

    /**
     * 注册 [DirectoryObserver] 监听当前目录文件系统变更
     *
     * 文件事件经过 [DirectoryObserver.DEBOUNCE_MS] 防抖后触发 reload
     * 仅支持可解析为文件系统路径的目录（SAF URI 可映射到 /storage/...）
     */
    private fun registerFileObserver(treeUri: Uri) {
        directoryObserver.startWatching(
            uri = treeUri,
            scope = viewModelScope,
            pathResolver = { safUriToFilePath(it) },
        )
    }

    /** 注销当前的 [DirectoryObserver] */
    private fun unregisterFileObserver() {
        directoryObserver.stopWatching()
    }

    /**
     * 清空所有 Git 状态并关闭 GitRepository
     *
     * @throws Exception 关闭 JGit Repository 时的异常会被捕获并记录
     */
    private suspend fun clearGitState() {
        gitRepoMutex.withLock {
            currentGitRepo?.close()
            currentGitRepo = null
        }
        _isGitRepo.value = false
        _gitRepoInfo.value = null
        _gitFileStatuses.value = persistentListOf()
    }

    /**
     * 切换文件置顶状态
     *
     * @param fileUri 文件 URI 字符串
     */
    fun togglePin(fileUri: String) {
        viewModelScope.launch {
            _pinnedFiles.update { current ->
                if (fileUri in current) current - fileUri else current + fileUri
            }
            settingsRepository.setPinnedFiles(_pinnedFiles.value)
            val uri = _currentTreeUri.value
            if (uri != null) {
                loadFiles(uri)
            }
        }
    }

    /** 查询文件是否已置顶 */
    fun isFilePinned(fileUri: String): Boolean = _pinnedFiles.value.contains(fileUri)

    /** 从 BookmarksRepository 加载收藏列表 */
    private fun loadBookmarks() {
        viewModelScope.launch {
            manageBookmarks.allBookmarkUris().collect { uris ->
                _bookmarkedUris.value = uris.toSet()
            }
        }
    }

    /**
     * 切换文件收藏状态
     *
     * @param fileUri 文件 URI 字符串
     * @param fileName 文件名
     * @param directoryUri 所在目录 URI 字符串
     */
    fun toggleBookmark(fileUri: String, fileName: String, directoryUri: String) {
        viewModelScope.launch {
            if (_bookmarkedUris.value.contains(fileUri)) {
                manageBookmarks.removeBookmark(fileUri)
            } else {
                manageBookmarks.addBookmark(fileUri, fileName, directoryUri)
            }
        }
    }

    /** 查询文件是否已收藏 */
    fun isBookmarked(fileUri: String): Boolean = _bookmarkedUris.value.contains(fileUri)

    /**
     * ViewModel 销毁时释放资源
     *
     * 关键修复：
     * - 正确关闭 JGit Repository 释放 `.git` 目录文件句柄和锁，防止 [LockFailedException]
     * - 取消 DirectoryObserver 事件收集 Job
     * - 注销文件观察者
     *
     * JGit close 可能触发短暂 I/O，使用守护线程异步关闭避免 ANR；
     * 同时 200ms join 超时确保进程退出时锁文件有机会被清理
     */
    override fun onCleared() {
        super.onCleared()
        observerCollectionJob?.cancel()
        observerCollectionJob = null
        unregisterFileObserver()
        val repoToClose = currentGitRepo
        currentGitRepo = null
        if (repoToClose != null) {
            val closeThread = kotlin.concurrent.thread(start = true, isDaemon = true) {
                runCatching { repoToClose.close() }
                    .onFailure { Log.w(TAG, "Failed to close GitRepository on clear", it) }
            }
            try {
                closeThread.join(CLOSE_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    // ---- GitHub 导入功能 ----

    /**
     * 从 GitHub URL 开始导入流程
     *
     * URL 解析失败时直接进入 Error 状态。支持的 URL 格式：
     * - https://github.com/owner/repo
     * - https://github.com/owner/repo.git
     *
     * @param url GitHub 仓库 URL
     */
    fun importFromGitHub(url: String) {
        Log.i(TAG, "importFromGitHub: $url")
        val parsed = gitHubApiClient.parseGitHubUrl(url)
        if (parsed == null) {
            Log.w(TAG, "GitHub URL parsing failed for: $url")
            _gitHubImportState.value = GitHubImportState.Error(
                context.getString(R.string.browser_github_error_invalid_url)
            )
            return
        }

        val (owner, repo) = parsed
        Log.i(TAG, "GitHub parsed: owner=$owner, repo=$repo")
        _gitHubImportState.value = GitHubImportState.Loading
        _gitHubSelectedFiles.value = emptySet()

        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Fetching file tree: $owner/$repo")
            when (val result = gitHubApiClient.fetchFileTree(owner, repo)) {
                is GitHubTreeResult.Success -> {
                    Log.i(TAG, "GitHub API success: ${result.files.size} files, truncated=${result.truncated}")
                    if (result.files.isEmpty()) {
                        _gitHubImportState.value = GitHubImportState.Error(
                            context.getString(R.string.browser_github_error_network)
                        )
                    } else {
                        _gitHubImportState.value = GitHubImportState.FileList(
                            owner = owner,
                            repo = repo,
                            files = result.files
                        )
                    }
                }
                is GitHubTreeResult.Error -> {
                    Log.w(TAG, "GitHub API error: ${result.message}, rateLimited=${result.isRateLimited}")
                    val errorMessage = if (result.isRateLimited) {
                        context.getString(R.string.browser_github_error_rate_limited)
                    } else {
                        result.message
                    }
                    _gitHubImportState.value = GitHubImportState.Error(errorMessage)
                }
            }
        }
    }

    /**
     * 切换 GitHub 文件列表中单个文件的选中状态
     *
     * @param path 文件在仓库中的相对路径
     */
    fun toggleGitHubFileSelection(path: String) {
        _gitHubSelectedFiles.update { current ->
            if (path in current) current - path else current + path
        }
    }

    /** 全选 GitHub 文件列表中的所有文件 */
    fun selectAllGitHubFiles() {
        val state = _gitHubImportState.value
        if (state is GitHubImportState.FileList) {
            _gitHubSelectedFiles.value = state.files.map { it.path }.toSet()
        }
    }

    /** 清空 GitHub 文件选择 */
    fun clearGitHubFileSelection() {
        _gitHubSelectedFiles.value = emptySet()
    }

    /**
     * 确认 GitHub 导入，执行 sparse checkout
     *
     * 支持 GitHub 镜像加速（通过 SettingsRepository 配置）
     * 克隆成功后自动刷新内部文件列表
     *
     * @throws GitAPIException JGit 操作异常会被捕获并转换为错误消息
     */
    fun confirmGitHubImport() {
        val state = _gitHubImportState.value as? GitHubImportState.FileList ?: return
        val selectedPaths = _gitHubSelectedFiles.value
        if (selectedPaths.isEmpty()) return

        Log.i(TAG, "confirmGitHubImport: owner=${state.owner}, repo=${state.repo}, ${selectedPaths.size} files")
        _gitHubImportState.value = GitHubImportState.Cloning()

        viewModelScope.launch(Dispatchers.IO) {
            val githubUrl = "https://github.com/${state.owner}/${state.repo}.git"
            val mirrorUrl = settingsRepository.getGithubMirrorUrl().let { flow ->
                var url = ""
                flow.first { true }.also { url = it }
                url
            }
            val remoteUrl = if (mirrorUrl.isNotEmpty()) {
                "$mirrorUrl/$githubUrl"
            } else {
                githubUrl
            }
            val targetDir = File(AppFileManager.getUserFilesDir(context), state.repo)
            Log.i(TAG, "Cloning $remoteUrl to ${targetDir.absolutePath} (${selectedPaths.size} sparse paths)")

            val throttler = OutputThrottler(
                scope = viewModelScope,
                onOutput = { message ->
                    _gitHubImportState.update { current ->
                        if (current is GitHubImportState.Cloning) {
                            current.copy(progressMessage = message)
                        } else current
                    }
                },
            )

            val result = gitManager.cloneRepository(
                remoteUrl = remoteUrl,
                targetDirectory = targetDir,
                depth = 1,
                sparsePaths = selectedPaths.toList(),
                onProgress = { message -> throttler.append(message) },
            )

            throttler.flushNow()

            when (result) {
                is CloneResult.Success -> {
                    Log.i(TAG, "Clone successful")
                    _gitHubImportState.value = GitHubImportState.Success(
                        context.getString(R.string.browser_github_success)
                    )
                    loadInternalFiles()
                }
                is CloneResult.Error -> {
                    Log.w(TAG, "Clone failed: ${result.message}")
                    _gitHubImportState.value = GitHubImportState.Error(result.message)
                }
            }
        }
    }

    /** 重置 GitHub 导入状态到初始 Idle */
    fun resetGitHubImportState() {
        _gitHubImportState.value = GitHubImportState.Idle
        _gitHubSelectedFiles.value = emptySet()
    }

    /**
     * 将外部文件（SAF URI）保存到应用内部 user_files 目录
     *
     * 文件操作异常处理：
     * - 文件名冲突时自动追加 _1, _2 后缀，最多重试 [MAX_NAME_COLLISION_RETRIES] 次
     * - 重试次数耗尽后**中止操作返回 null**，绝不静默覆盖既有文件
     * - InputStream/OutputStream 使用 use{} 块确保正确关闭
     * - ContentResolver.openInputStream 返回 null 时安全处理
     *
     * @param item 外部文件项（包含 SAF URI）
     * @return 保存后的内部 File 对象，失败返回 null
     */
    suspend fun saveExternalFileToInternal(item: FileItem): File? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val dir = AppFileManager.getUserFilesDir(context)
                val targetFile = File(dir, item.name)
                var dest = targetFile
                var counter = 1
                while (dest.exists() && counter < MAX_NAME_COLLISION_RETRIES) {
                    val baseName = item.name.substringBeforeLast('.', item.name)
                    val ext = item.name.substringAfterLast('.', "")
                    dest = if (ext.isNotEmpty()) {
                        File(dir, "${baseName}_$counter.$ext")
                    } else {
                        File(dir, "${baseName}_$counter")
                    }
                    counter++
                }
                if (dest.exists()) {
                    Log.w(TAG, "Filename collision exhausted after $MAX_NAME_COLLISION_RETRIES attempts " +
                        "for ${item.name}; aborting save to prevent overwrite")
                    return@runCatching null
                }
                val inputStream = context.contentResolver.openInputStream(item.uri)
                    ?: return@runCatching null
                inputStream.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                refreshInternalFiles()
                dest
            }.getOrElse { e ->
                Log.w(TAG, "Failed to save external file to internal storage", e)
                null
            }
        }
    }

    // ===== 多选模式 =====
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems.asStateFlow()

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.update { !it }
        if (!_isMultiSelectMode.value) {
            _selectedItems.value = emptySet()
        }
    }

    fun toggleItemSelected(uri: String) {
        _selectedItems.update { current ->
            if (uri in current) current - uri else current + uri
        }
    }

    fun selectAllItems() {
        val currentFiles = (_uiState.value as? BrowserUiState.Success)?.files ?: return
        _selectedItems.value = currentFiles.map { it.uri.toString() }.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    // ===== 文件夹创建 =====
    fun createFolder(folderName: String): Boolean {
        return try {
            val folder = AppFileManager.createUserFolder(context, folderName)
            refreshInternalFiles()
            folder.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create folder: $folderName", e)
            false
        }
    }

    fun getInternalFolders(): List<File> {
        return AppFileManager.listUserFolders(context)
    }

    // ===== 文件移动 =====
    fun moveFile(sourceUri: String, targetDirUri: String): Boolean {
        return try {
            val moved = AppFileManager.moveInternalFile(context, sourceUri, targetDirUri)
            if (moved) {
                refreshInternalFiles()
            }
            moved
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move file", e)
            false
        }
    }

    fun moveSelectedFiles(targetDirUri: String): Int {
        val selected = _selectedItems.value
        var movedCount = 0
        for (uri in selected) {
            if (AppFileManager.moveInternalFile(context, uri, targetDirUri)) {
                movedCount++
            }
        }
        if (movedCount > 0) {
            refreshInternalFiles()
            _selectedItems.value = emptySet()
            _isMultiSelectMode.value = false
        }
        return movedCount
    }

    /**
     * 删除应用内部存储的文件。
     *
     * 仅允许删除位于 AppFileManager.getUserFilesDir() 目录下的内部文件，
     * 外部SAF文件不支持直接删除（需用户在系统文件管理器中操作）。
     * 删除操作在IO线程执行，包含路径安全校验防止路径遍历攻击。
     *
     * @param item 要删除的文件项（必须是内部file:// URI）
     * @return 删除成功返回true，失败返回false
     */
    suspend fun deleteInternalFile(item: FileItem): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val uriStr = item.uri.toString()
                if (!AppFileManager.isInternalUri(uriStr)) {
                    Log.w(TAG, "Refusing to delete non-internal file: $uriStr")
                    return@runCatching false
                }
                val file = AppFileManager.getInternalFileFromUri(context, uriStr)
                if (file == null) {
                    Log.w(TAG, "Cannot resolve internal file from URI: $uriStr")
                    return@runCatching false
                }
                val deleted = AppFileManager.deleteUserFile(file)
                if (deleted) {
                    refreshInternalFiles()
                }
                deleted
            }.getOrElse { e ->
                Log.w(TAG, "Failed to delete internal file: ${item.name}", e)
                false
            }
        }
    }

    /**
     * 读取文件开头内容，用于首页"最近打开"代码预览卡片。
     *
     * 只读取前 5 行并返回，避免大文件造成 IO 开销；读取失败返回 null，
     * 界面按"无预览"降级处理，不打扰用户。
     *
     * @param uriString 文件 URI 字符串
     * @return 前 5 行文本，失败或非文本文件返回 null
     */
    suspend fun readFilePreview(uriString: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val result = repository.readFile(Uri.parse(uriString))
            (result as? FileContentResult.Success)?.content?.content
        }.getOrNull()?.lines()?.take(5)?.joinToString("\n")
    }

    /**
     * 将外部文件（通过OpenDocument选择的文件）导入并复制到应用内部存储。
     *
     * 与saveExternalFileToInternal类似，但用于FAB直接选择文件的场景。
     *
     * @param uri 外部文件的SAF URI
     * @return 复制后的内部File对象，失败返回null
     */
    suspend fun importExternalFileToInternal(uri: Uri): File? {
        return withContext(Dispatchers.IO) {
            runCatching {
                // Resolve actual display name from ContentResolver (SAF URIs have unreliable lastPathSegment)
                val fileName = resolveFileName(uri)
                val dir = AppFileManager.getUserFilesDir(context)
                var dest = File(dir, fileName)
                var counter = 1
                while (dest.exists() && counter < MAX_NAME_COLLISION_RETRIES) {
                    val baseName = fileName.substringBeforeLast('.', fileName)
                    val ext = fileName.substringAfterLast('.', "")
                    dest = if (ext.isNotEmpty()) {
                        File(dir, "${baseName}_$counter.$ext")
                    } else {
                        File(dir, "${baseName}_$counter")
                    }
                    counter++
                }
                if (dest.exists()) {
                    Log.w(TAG, "Filename collision exhausted during import for $fileName")
                    return@runCatching null
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@runCatching null
                inputStream.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                refreshInternalFiles()
                dest
            }.getOrElse { e ->
                Log.w(TAG, "Failed to import external file", e)
                null
            }
        }
    }

    /**
     * Resolve display name from a content:// URI using ContentResolver,
     * falling back to lastPathSegment for file:// URIs.
     */
    private fun resolveFileName(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
        }
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
        } catch (_: Exception) {
            uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file"
        }
    }

    companion object {
        /**
         * 文件名冲突重试上限
         *
         * 从 1000 降至 100：1000 次 File.exists() 在 SD 卡上最坏需要 5-10 秒，造成明显卡顿
         * 实际冲突超过 10 次已极罕见，100 次提供足够余量且最坏仅 500ms-1s
         */
        private const val MAX_NAME_COLLISION_RETRIES = 100

        /** onCleared 中等待 JGit 关闭的超时时间（毫秒） */
        private const val CLOSE_TIMEOUT_MS = 200L
    }
}
