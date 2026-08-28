/**
 * 代码片段（Snippet）管理 ViewModel
 *
 * ## 功能职责
 * - 代码片段的 CRUD 操作（增删改查）
 * - FTS4 全文搜索与分类过滤
 * - 片段导出为外部文件
 * - 从片段内容创建新文件
 * - 安全门控（SecurityGate）操作权限检查
 *
 * ## 状态管理与数据流
 *
 * ### StateFlow 状态
 * - [snippets]: 所有代码片段列表，通过 Room 持久化
 * - [categories]: 所有分类列表
 * - [searchQuery]: 当前搜索关键词
 * - [selectedCategory]: 当前选中分类过滤
 * - [searchMode]: 搜索模式（SMART 智能解析 / SUBSTRING 子串匹配）
 * - [searchResults]: 搜索结果（结合搜索词、分类、模式自动计算）
 *
 * ### SharedFlow 事件（一次性）
 * - [exportEvent]: 请求导出片段事件
 * - [createFileEvent]: 请求创建外部文件事件（SAF）
 * - [createInternalFileEvent]: 请求创建内部文件事件
 * - [errorEvent]: 错误提示事件（Snackbar 显示）
 *
 * ## 搜索策略
 * 1. **SMART 模式**：使用 [FtsQueryBuilder] 解析用户输入，支持 AND/OR/NOT/短语搜索
 * 2. **FTS4 MATCH 失败降级**：如果 FTS 查询语法错误，自动降级到 SUBSTRING 模式
 * 3. **SUBSTRING 模式**：使用 SQL LIKE 做子串匹配，支持部分单词但速度较慢
 *
 * ## 异常处理说明
 * - 所有数据库操作包裹在 try-catch 中
 * - 异常通过 [ErrorHandler] 转换为用户友好消息
 * - 安全门控检查失败时发出操作受限错误事件
 * - 使用 [emit] 发送错误事件（关键事件，确保送达）
 */
package com.draftpeek.feature.browser.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.error.ErrorEvent
import com.draftpeek.core.common.error.ErrorHandler
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.core.common.util.AppFileManager
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.usecase.RecordUserActivityUseCase
import com.draftpeek.core.data.util.FtsQueryBuilder
import com.draftpeek.core.domain.usecase.SearchSnippetsUseCase
import com.draftpeek.feature.browser.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 创建外部文件（SAF）所需的信息
 *
 * @property filename 文件名（不含扩展名）
 * @property language 编程语言标识符
 * @property mimeType MIME 类型
 * @property extension 文件扩展名（不含点）
 * @property initialContent 文件初始内容
 */
data class FileCreateInfo(
    val filename: String,
    val language: String,
    val mimeType: String,
    val extension: String,
    val initialContent: String = ""
)

/**
 * 创建应用内部存储文件所需的信息
 *
 * @property file 创建好的 File 对象
 * @property language 编程语言标识符
 * @property initialContent 文件初始内容
 */
data class InternalFileInfo(val file: java.io.File, val language: String, val initialContent: String = "")

@HiltViewModel
class SnippetViewModel @Inject constructor(
    private val repository: SnippetRepository,
    private val userActivityRepository: UserActivityRepository,
    private val searchSnippets: SearchSnippetsUseCase,
    private val recordUserActivity: RecordUserActivityUseCase,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    /**
     * 所有代码片段列表
     *
     * 通过 [stateIn] 转换为热流，5秒无订阅者时停止上游（节省资源）
     */
    val snippets: StateFlow<List<Snippet>> = repository.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 所有分类名称列表（去重）
     */
    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchQuery = MutableStateFlow("")

    /** 当前搜索关键词 */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)

    /** 当前选中的分类过滤（null 表示不过滤） */
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _exportEvent = MutableSharedFlow<Snippet>()

    /** 导出片段事件（消费后触发 SAF 创建文档 Intent） */
    val exportEvent: SharedFlow<Snippet> = _exportEvent.asSharedFlow()

    private val _createFileEvent = MutableSharedFlow<FileCreateInfo>()

    /** 创建外部文件事件（消费后触发 SAF 创建文档 Intent） */
    val createFileEvent: SharedFlow<FileCreateInfo> = _createFileEvent.asSharedFlow()

    private val _createInternalFileEvent = MutableSharedFlow<InternalFileInfo>()

    /** 创建内部文件事件 */
    val createInternalFileEvent: SharedFlow<InternalFileInfo> = _createInternalFileEvent.asSharedFlow()

    /**
     * 一次性错误事件流（用于 Snackbar 提示）
     *
     * 配置：缓冲区容量 1，溢出时丢弃最旧的，确保不阻塞发送者
     */
    private val _errorEvent = MutableSharedFlow<ErrorEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent.asSharedFlow()

    /**
     * 搜索模式
     *
     * 控制如何将用户输入构建为 FTS4 查询
     */
    enum class SearchMode {
        /**
         * 智能模式（默认）
         *
         * 使用 [FtsQueryBuilder] 解析原始输入，支持 AND/OR/NOT/精确短语等高级语法
         */
        SMART,

        /**
         * 子串匹配模式
         *
         * 使用 SQL LIKE 做降级匹配，支持部分单词但无法利用 FTS 索引，速度较慢
         */
        SUBSTRING
    }

    private val _searchMode = MutableStateFlow(SearchMode.SMART)

    /** 当前搜索模式 */
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    /**
     * 搜索结果（自动计算）
     *
     * 结合三个输入流：
     * 1. 搜索关键词 [_searchQuery]
     * 2. 选中分类 [_selectedCategory]
     * 3. 搜索模式 [_searchMode]
     *
     * 搜索策略：
     * - 空查询：仅分类过滤
     * - SMART 模式：尝试 FTS4 MATCH，语法错误降级到子串
     * - SUBSTRING 模式：直接 LIKE 子串匹配
     */
    val searchResults: StateFlow<List<Snippet>> = combine(
        _searchQuery,
        _selectedCategory,
        _searchMode
    ) { query, category, mode ->
        Triple(query, category, mode)
    }.combine(repository.getAllSnippets()) { (query, category, mode), allSnippets ->
        if (query.isBlank()) {
            var result = allSnippets
            if (!category.isNullOrEmpty()) {
                result = result.filter { it.category == category }
            }
            return@combine result
        }

        val ftsQuery = when (mode) {
            SearchMode.SMART -> FtsQueryBuilder.buildFromRawInput(query)
            SearchMode.SUBSTRING -> ""
        }

        if (ftsQuery.isNotBlank()) {
            try {
                val ftsResults = if (!category.isNullOrEmpty()) {
                    searchSnippets.byCategory(ftsQuery, category).first()
                } else {
                    searchSnippets(query).first()
                }
                return@combine ftsResults
            } catch (_: Exception) {
                // FTS4 MATCH 可能因查询格式错误失败，降级到子串匹配
            }
        }

        var result = searchSnippets.substring(query).first()
        if (!category.isNullOrEmpty()) {
            result = result.filter { it.category == category }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 更新搜索关键词
     *
     * @param query 搜索关键词
     */
    fun search(query: String) {
        _searchQuery.value = query
    }

    /**
     * 切换搜索模式
     *
     * @param mode 目标搜索模式
     */
    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
    }

    /**
     * 请求导出代码片段
     *
     * 首先通过 SecurityGate 检查操作权限，权限不足时发送错误事件
     *
     * @param snippet 要导出的代码片段
     * @see createExportIntent 配套的 Intent 创建方法
     */
    fun requestExport(snippet: Snippet) {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch {
                _errorEvent.emit(
                    ErrorEvent(
                        message = appContext.getString(R.string.security_operation_restricted)
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _exportEvent.emit(snippet)
        }
    }

    /**
     * 请求在外部存储创建新文件（通过 SAF）
     *
     * 根据语言自动映射 MIME 类型和扩展名
     *
     * @param filename 文件名（不含扩展名）
     * @param language 编程语言标识符
     * @param initialContent 文件初始内容（默认为空）
     */
    fun requestCreateFile(filename: String, language: String, initialContent: String = "") {
        val ext = LanguageConfig.languageToExtension(language)
        val mime = LanguageConfig.languageToMimeType(language)
        viewModelScope.launch {
            _createFileEvent.emit(FileCreateInfo(filename, language, mime, ext, initialContent))
        }
    }

    /**
     * 直接在应用内部存储创建文件
     *
     * 安全门控检查后通过 [AppFileManager.createUserFile] 创建文件
     *
     * @param filename 文件名（不含扩展名）
     * @param language 编程语言标识符
     * @param initialContent 文件初始内容（默认为空）
     * @throws SecurityException 当安全门控拒绝操作时
     */
    fun createFileInInternalStorage(filename: String, language: String, initialContent: String = "") {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch {
                _errorEvent.emit(
                    ErrorEvent(
                        message = appContext.getString(R.string.security_operation_restricted)
                    )
                )
            }
            return
        }
        val ext = LanguageConfig.languageToExtension(language)
        val file = AppFileManager.createUserFile(appContext, filename, ext, initialContent)
        viewModelScope.launch {
            _createInternalFileEvent.emit(InternalFileInfo(file, language, initialContent))
            recordUserActivity.recordFileManagement()
            recordUserActivity.recordFileCreate()
        }
    }

    /**
     * 添加新代码片段
     *
     * @param title 片段标题
     * @param content 片段内容
     * @param language 编程语言（可为 null）
     * @param category 分类名称，空字符串归为"未分类"
     *
     * 异常处理：数据库操作异常通过 [ErrorHandler] 转换为用户友好消息
     */
    fun addSnippet(title: String, content: String, language: String?, category: String) {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch {
                _errorEvent.emit(
                    ErrorEvent(
                        message = appContext.getString(R.string.security_operation_restricted)
                    )
                )
            }
            return
        }
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                repository.addSnippet(
                    Snippet(
                        title = title,
                        content = content,
                        language = language,
                        category = category.ifBlank {
                            appContext.getString(R.string.browser_snippet_category_uncategorized)
                        },
                        createdAt = now,
                        updatedAt = now
                    )
                )
                recordUserActivity.recordSnippetCreated()
                recordUserActivity.recordFileCreate()
            } catch (e: Exception) {
                val appError = ErrorHandler.fromException(e, appContext)
                _errorEvent.emit(
                    ErrorEvent(
                        message = ErrorHandler.getUserMessage(appError, appContext),
                        isRetryable = ErrorHandler.getRetryAction(appError)
                    )
                )
            }
        }
    }

    /**
     * 更新现有代码片段
     *
     * 自动更新 updatedAt 时间戳为当前时间
     *
     * @param snippet 要更新的片段对象（必须包含 id）
     */
    fun updateSnippet(snippet: Snippet) {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch {
                _errorEvent.emit(
                    ErrorEvent(
                        message = appContext.getString(R.string.security_operation_restricted)
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.updateSnippet(snippet.copy(updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                val appError = ErrorHandler.fromException(e, appContext)
                _errorEvent.emit(
                    ErrorEvent(
                        message = ErrorHandler.getUserMessage(appError, appContext),
                        isRetryable = ErrorHandler.getRetryAction(appError)
                    )
                )
            }
        }
    }

    /**
     * 删除代码片段
     *
     * @param snippet 要删除的片段对象
     */
    fun deleteSnippet(snippet: Snippet) {
        if (!SecurityGate.isOperationAllowed()) {
            viewModelScope.launch {
                _errorEvent.emit(
                    ErrorEvent(
                        message = appContext.getString(R.string.security_operation_restricted)
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.deleteSnippet(snippet)
            } catch (e: Exception) {
                val appError = ErrorHandler.fromException(e, appContext)
                _errorEvent.emit(
                    ErrorEvent(
                        message = ErrorHandler.getUserMessage(appError, appContext),
                        isRetryable = ErrorHandler.getRetryAction(appError)
                    )
                )
            }
        }
    }

    /**
     * 按分类过滤片段
     *
     * @param category 分类名称，null 表示清除过滤显示全部
     */
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    companion object {
        /**
         * 创建导出代码片段的 SAF Intent
         *
         * 根据片段语言自动推断 MIME 类型和文件扩展名
         *
         * @param snippet 要导出的代码片段
         * @return ACTION_CREATE_DOCUMENT Intent，可直接用于 startActivityForResult
         */
        fun createExportIntent(snippet: Snippet): Intent {
            val mimeType = LanguageConfig.languageToMimeType(snippet.language ?: "")
            val extension = LanguageConfig.languageToExtension(snippet.language ?: "")
            val fileName = "${snippet.title}.$extension"
            return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
        }

        /**
         * 创建新文件的 SAF Intent
         *
         * @param info 文件创建信息
         * @return ACTION_CREATE_DOCUMENT Intent
         */
        fun createFileIntent(info: FileCreateInfo): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = info.mimeType
            putExtra(Intent.EXTRA_TITLE, "${info.filename}.${info.extension}")
        }
    }
}
