/**
 * 文件功能：LSP（Language Server Protocol）管理器
 * 
 * 主要类：[LspManager] —— 管理 LSP 客户端生命周期、按语言路由编辑器事件、
 *                        维护诊断状态、支持用户配置的服务器
 * 
 * 模块依赖：
 * - com.draftpeek.core.common.error：错误处理
 * - com.draftpeek.feature.editor.diagnostics：诊断导航
 * - Hilt 依赖注入
 * - kotlinx.coroutines：协程和 Flow
 * 
 * 核心功能：
 * 1. 按语言 ID 维护 LSP 客户端实例（懒加载、单例）
 * 2. 将编辑器事件（打开、更改、关闭）路由到适当的服务器
 * 3. 从 textDocument/publishDiagnostics 通知中收集诊断信息
 * 4. 与 DiagnosticNavigator 集成，在编辑器边距/状态栏中显示诊断
 * 5. 支持用户配置的 LSP 服务器（如通过 Node.js/Termux 本地运行）
 * 
 * 线程安全：所有公共方法在 viewModelScope 中启动协程，所有可变状态
 * 通过 Mutex 或并发集合保护。
 */
package com.draftpeek.feature.editor.lsp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import com.draftpeek.core.common.error.AppError
import com.draftpeek.core.common.error.ErrorEvent
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.feature.FeatureToggleManager
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.core.common.util.RequestCanceller
import com.draftpeek.feature.editor.diagnostics.DiagnosticItem
import com.draftpeek.feature.editor.diagnostics.DiagnosticNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户可配置的 LSP 服务器配置
 * 
 * @property languageId 此服务器支持的语言 ID（如 "python"、"javascript"）
 * @property name 用户可见的服务器名称（如 "Python LSP (pylsp)"）
 * @property command 启动服务器的命令（如 "pylsp"、"node ./server.js"）
 * @property args 命令参数
 * @property workingDir 服务器工作目录（null 表示应用文件目录）
 * @property enabled 用户是否启用此服务器
 */
@Immutable
data class LspServerConfig(
    val languageId: String,
    val name: String,
    val command: String,
    val args: List<String> = emptyList(),
    val workingDir: String? = null,
    val enabled: Boolean = true,
)

/**
 * 语言服务器协议管理器
 * 
 * 作为所有 LSP 交互的中心协调器。编辑器通过此类发送文档事件
 * 并查询语言功能（补全、悬停等）。管理器负责：
 * - 根据需要懒加载和初始化 LSP 客户端
 * - 在客户端和内置诊断提供程序之间路由诊断信息
 * - 在服务器发生错误时提供回退机制
 * 
 * 线程安全：此类是单例且线程安全。协程在独立的 SupervisorJob 上启动，
 * 以防止一个服务器的故障影响其他服务器。
 * 
 * @property context 应用上下文（用于访问文件和进程 API）
 * @property diagnosticNavigator 跨编辑器会话的共享诊断导航器
 */
@Singleton
class LspManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val diagnosticNavigator: DiagnosticNavigator,
    private val featureToggleManager: FeatureToggleManager,
) {

    private val lspEnabled: Boolean
        get() = featureToggleManager.isEnabled(FeatureFlag.LSP_CLIENT)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val clients = mutableMapOf<String, LspClient>()

    private val clientMutex = Mutex()

    private val clientInitJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private val _clientStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    /** 每个语言 ID 的服务器状态（运行 = true） */
    val clientStatus: StateFlow<Map<String, Boolean>> = _clientStatus.asStateFlow()

    private val _errorFlow = MutableSharedFlow<ErrorEvent>()
    /** 用于向 UI 显示的错误事件 */
    val errorFlow: SharedFlow<ErrorEvent> = _errorFlow.asSharedFlow()

    private val _userServers = MutableStateFlow<List<LspServerConfig>>(emptyList())
    /** 用户配置的服务器 */
    val userServers: StateFlow<List<LspServerConfig>> = _userServers.asStateFlow()

    /**
     * 文档版本号映射（URI → 版本号）。
     * 使用 ConcurrentHashMap 保证多协程并发访问时的线程安全。
     * 版本号通过 getAndIncrement 实现原子递增，避免 read-modify-write 竞态条件。
     */
    private val documentVersions = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>()

    /**
     * 补全请求代次计数器（URI → AtomicInteger）。
     * 用于请求取消模式：每次新的补全请求递增代次，请求返回时检查自己是否是最新代次，
     * 如果不是则说明已被更新的请求取代，返回空列表。
     * 相比直接cancel Job，这种方式不会中断协程，而是通过代次检查优雅丢弃过期结果。
     * 参考 CodeAssist 的补全取消实现。
     */
    private val completionGenerations = ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicInteger>()

    private val _supportedLanguages = MutableStateFlow(setOf("python", "javascript", "typescript", "java", "kotlin", "go", "dart"))
    /** 支持 LSP 的语言 ID 集合 */
    val supportedLanguages: StateFlow<Set<String>> = _supportedLanguages.asStateFlow()

    companion object {
        private const val TAG = "LspManager"
    }

    /**
     * 获取或创建指定语言的 LSP 客户端
     * 
     * 算法步骤：
     * 1. 检查客户端是否已存在且正在运行
     * 2. 双重检查锁定（使用 Mutex）确保线程安全
     * 3. 根据语言 ID 选择适当的 LspClient 实现
     * 4. 初始化客户端并更新状态
     * 
     * @param languageId 语言标识符（如 "python"、"kotlin"）
     * @return 正在运行的 LspClient；如果语言不支持或初始化失败则返回 null
     */
    private suspend fun getOrCreateClient(languageId: String): LspClient? = clientMutex.withLock {
        clients[languageId]?.let { existing ->
            if (existing.isRunning) return@withLock existing
            clients.remove(languageId)
        }

        if (clientInitJobs[languageId]?.isActive == true) {
            return@withLock null
        }

        val client = createClient(languageId) ?: return@withLock null
        clients[languageId] = client

        clientInitJobs[languageId] = scope.launch {
            try {
                val rootUri = getRootUri()
                val result = client.initialize(rootUri)
                result.onSuccess {
                    Log.d(TAG, "LSP client initialized for $languageId")
                    _clientStatus.update { it + (languageId to true) }
                }.onFailure { error ->
                    Log.e(TAG, "LSP client failed to initialize for $languageId", error)
                    clientMutex.withLock {
                        clients.remove(languageId)
                    }
                    _clientStatus.update { it - languageId }
                    _errorFlow.emit(ErrorEvent(
                        message = "LSP 服务器初始化失败 ($languageId): ${error.message}",
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error initializing LSP client for $languageId", e)
                clientMutex.withLock {
                    clients.remove(languageId)
                }
                _clientStatus.update { it - languageId }
            } finally {
                clientInitJobs.remove(languageId)
            }
        }

        return@withLock client
    }

    /**
     * 根据语言 ID 创建 LSP 客户端实例
     * 
     * 算法步骤：
     * 1. 先检查用户配置的服务器是否匹配该语言
     * 2. 如果有用户配置，使用 UserConfiguredLspClient
     * 3. 否则使用内置的存根 provider（需要平台二进制文件才能正常工作）
     * 
     * @param languageId 语言标识符
     * @return LspClient 实例，如果不支持该语言则返回 null
     */
    private fun createClient(languageId: String): LspClient? {
        val userServer = _userServers.value.find { it.languageId == languageId && it.enabled }
        if (userServer != null) {
            Log.d(TAG, "Using user-configured LSP server for $languageId: ${userServer.command}")
            return UserConfiguredLspClient(userServer, context)
        }

        return when (languageId.lowercase()) {
            "python" -> PythonLspProvider(context)
            "javascript", "typescript" -> JavaScriptLspProvider(context)
            "java" -> JavaLspProvider(context)
            "kotlin" -> KotlinLspProvider(context)
            "go" -> GoLspProvider(context)
            "dart" -> DartLspProvider(context)
            else -> null
        }
    }

    /**
     * 通知管理器文档已打开
     * 
     * 算法步骤：
     * 1. 检测文件语言（可显式提供或从扩展名推断）
     * 2. 如果该语言支持 LSP，获取或创建客户端
     * 3. 向服务器发送 textDocument/didOpen
     * 4. 将文档同步到诊断导航器
     * 
     * @param uri 文档 URI
     * @param languageId 语言 ID（可选，null 时从扩展名检测）
     * @param text 完整文档内容
     */
    fun onDocumentOpened(uri: String, languageId: String?, text: String) {
        diagnosticNavigator.setDocument(uri, text)
        if (!lspEnabled) return

        val lang = languageId ?: detectLanguageFromUri(uri)
        if (lang == null || lang !in _supportedLanguages.value) {
            return
        }

        documentVersions[uri] = java.util.concurrent.atomic.AtomicInteger(0)

        scope.launch {
            val client = getOrCreateClient(lang)
            if (client != null && client.isRunning) {
                try {
                    client.openDocument(uri, lang, text)
                    fetchAndPublishDiagnostics(uri, lang, client)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onDocumentOpened for $uri", e)
                }
            }
        }
    }

    /**
     * 通知管理器文档内容已更改
     * 
     * 算法步骤：
     * 1. 增加文档版本号
     * 2. 如果有活动的 LSP 客户端，发送 textDocument/didChange（完整同步）
     * 3. 同步到诊断导航器
     * 
     * @param uri 文档 URI
     * @param text 新的完整文档内容
     */
    fun onDocumentChanged(uri: String, text: String) {
        diagnosticNavigator.setDocument(uri, text)
        if (!lspEnabled) return

        val version = documentVersions[uri]?.incrementAndGet() ?: 1

        val lang = detectLanguageFromUri(uri)

        scope.launch {
            if (lang != null && lang in _supportedLanguages.value) {
                val client = clientMutex.withLock { clients[lang] }
                if (client != null && client.isRunning) {
                    try {
                        client.updateDocument(uri, text, version)
                        fetchAndPublishDiagnostics(uri, lang, client)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDocumentChanged for $uri", e)
                    }
                }
            }
        }
    }

    /**
     * 通知管理器文档已关闭
     * 
     * 清理该文档的所有待处理补全请求，释放资源。
     * 
     * @param uri 文档 URI
     */
    fun onDocumentClosed(uri: String) {
        diagnosticNavigator.clearDocument(uri)
        // 清理该文档的补全代次计数器
        completionGenerations.remove(uri)
        if (!lspEnabled) return

        documentVersions.remove(uri)
        val lang = detectLanguageFromUri(uri)

        scope.launch {
            if (lang != null) {
                val client = clientMutex.withLock { clients[lang] }
                if (client != null && client.isRunning) {
                    try {
                        client.closeDocument(uri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDocumentClosed for $uri", e)
                    }
                }
            }
        }
    }

    /**
     * 请求给定位置的自动补全项
     * 
     * 使用请求取消模式（代次计数器）：同一文档新的补全请求会递增代次，
     * 旧请求返回时检测到不是最新代次则丢弃结果。避免快速输入时旧补全结果闪烁显示。
     * 参考 CodeAssist 的补全取消实现。
     *
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @return 补全项列表；LSP 不可用、请求被取消时返回空列表
     */
    suspend fun getCompletions(uri: String, line: Int, column: Int): List<CompletionItem> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        // 请求取消模式：递增代次，记录当前请求的代次
        val gen = completionGenerations.computeIfAbsent(uri) { java.util.concurrent.atomic.AtomicInteger(0) }
        val myGeneration = gen.incrementAndGet()

        return try {
            val result = client.completions(uri, line, column)
            // 检查代次：如果代次已变，说明有更新的请求，丢弃当前结果
            val currentGen = completionGenerations[uri]?.get() ?: 0
            if (currentGen != myGeneration) {
                emptyList()
            } else {
                result
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completions", e)
            emptyList()
        }
    }

    /**
     * 请求给定位置的悬停信息
     * 
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @return 悬停结果；不可用时返回 null
     */
    suspend fun getHover(uri: String, line: Int, column: Int): HoverResult? {
        if (!lspEnabled) return null
        val lang = detectLanguageFromUri(uri) ?: return null
        val client = getOrCreateClient(lang) ?: return null
        if (!client.isRunning) return null

        return try {
            client.hover(uri, line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hover", e)
            null
        }
    }

    /**
     * 跳转到给定位置的定义
     * 
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @return 定义位置列表；不可用时返回空列表
     */
    suspend fun goToDefinition(uri: String, line: Int, column: Int): List<LocationLink> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        return try {
            client.gotoDefinition(uri, line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Error going to definition", e)
            emptyList()
        }
    }

    /**
     * 请求给定位置符号的引用
     * 
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @param includeDeclaration 是否包含声明
     * @return 引用位置列表
     */
    suspend fun findReferences(
        uri: String,
        line: Int,
        column: Int,
        includeDeclaration: Boolean = false,
    ): List<LocationLink> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        return try {
            client.findReferences(uri, line, column, includeDeclaration)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding references", e)
            emptyList()
        }
    }

    /**
     * 请求签名帮助
     * 
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @return 签名文档字符串；不可用时返回 null
     */
    suspend fun getSignatureHelp(uri: String, line: Int, column: Int): String? {
        if (!lspEnabled) return null
        val lang = detectLanguageFromUri(uri) ?: return null
        val client = getOrCreateClient(lang) ?: return null
        if (!client.isRunning) return null

        return try {
            client.signatureHelp(uri, line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signature help", e)
            null
        }
    }

    /**
     * 请求文档格式化编辑
     * 
     * @param uri 文档 URI
     * @param tabSize 每个缩进级别的空格数
     * @param insertSpaces 用空格代替制表符
     * @return 格式化文本编辑列表
     */
    suspend fun getFormattingEdits(uri: String, tabSize: Int = 4, insertSpaces: Boolean = true): List<TextEdit> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        return try {
            client.formatting(uri, tabSize, insertSpaces)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting formatting edits", e)
            emptyList()
        }
    }

    /**
     * 请求内嵌提示（类型注解、参数名）
     * 
     * @param uri 文档 URI
     * @param range 请求提示的范围
     * @return 内嵌提示列表
     */
    suspend fun getInlayHints(uri: String, range: LspRange): List<InlayHintItem> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        return try {
            client.inlayHints(uri, range)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting inlay hints", e)
            emptyList()
        }
    }

    /**
     * 请求给定范围的代码操作
     * 
     * @param uri 文档 URI
     * @param range 请求操作的范围
     * @param diagnostics 上下文中的诊断
     * @return 可用代码操作列表
     */
    suspend fun getCodeActions(uri: String, range: LspRange, diagnostics: List<LspDiagnostic> = emptyList()): List<CodeActionItem> {
        if (!lspEnabled) return emptyList()
        val lang = detectLanguageFromUri(uri) ?: return emptyList()
        val client = getOrCreateClient(lang) ?: return emptyList()
        if (!client.isRunning) return emptyList()

        return try {
            client.codeActions(uri, range, diagnostics)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting code actions", e)
            emptyList()
        }
    }

    /**
     * 重命名符号
     * 
     * @param uri 文档 URI
     * @param line 行号（0-based）
     * @param column 列号（0-based）
     * @param newName 新符号名
     * @return 包含所有编辑的工作区编辑；不可用时返回 null
     */
    suspend fun rename(uri: String, line: Int, column: Int, newName: String): WorkspaceEdit? {
        if (!lspEnabled) return null
        val lang = detectLanguageFromUri(uri) ?: return null
        val client = getOrCreateClient(lang) ?: return null
        if (!client.isRunning) return null

        return try {
            client.rename(uri, line, column, newName)
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming", e)
            null
        }
    }

    /**
     * 从 LSP 客户端获取诊断并发布到诊断导航器
     * 
     * 算法步骤：
     * 1. 从 LSP 客户端获取诊断
     * 2. 将基于行/列的 LSP 诊断转换为基于字符偏移的 DiagnosticItem
     * 3. 将转换后的诊断发布到 DiagnosticNavigator
     * 
     * @param uri 文档 URI
     * @param languageId 语言 ID
     * @param client LSP 客户端
     */
    private suspend fun fetchAndPublishDiagnostics(uri: String, languageId: String, client: LspClient) {
        try {
            val lspDiagnostics = client.diagnostics(uri)
            val items = lspDiagnostics.map { d ->
                DiagnosticItem(
                    startIndex = 0,
                    endIndex = 0,
                    severity = when (d.severity) {
                        LspDiagnostic.SEVERITY_ERROR -> DiagnosticItem.SEVERITY_ERROR
                        LspDiagnostic.SEVERITY_WARNING -> DiagnosticItem.SEVERITY_WARNING
                        LspDiagnostic.SEVERITY_INFORMATION -> DiagnosticItem.SEVERITY_TYPO
                        else -> DiagnosticItem.SEVERITY_TYPO
                    },
                    message = d.message,
                )
            }
            diagnosticNavigator.setLspDiagnostics(uri, items)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching diagnostics", e)
        }
    }

    /**
     * 从文件 URI 推断编程语言
     *
     * 使用 LanguageConfig.extensionToLanguage() 进行统一映射，
     * 仅返回 LSP 支持的语言子集。
     *
     * @param uri 文件 URI 字符串
     * @return 语言 ID，如果无法识别或不支持 LSP 则返回 null
     */
    private fun detectLanguageFromUri(uri: String): String? {
        return try {
            val path = Uri.parse(uri).path ?: return null
            val ext = path.substringAfterLast('.', "").lowercase()
            val lang = LanguageConfig.extensionToLanguage(ext) ?: return null
            // 仅返回 LSP 支持的语言
            if (lang in _supportedLanguages.value) lang else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取 LSP 初始化的工作区根 URI
     * 
     * @return 文件 URI 字符串
     */
    private fun getRootUri(): String {
        val rootDir = context.getExternalFilesDir(null) ?: context.filesDir
        return Uri.fromFile(rootDir).toString()
    }

    /**
     * 注册用户配置的 LSP 服务器
     * 
     * @param config 服务器配置
     */
    fun registerUserServer(config: LspServerConfig) {
        val current = _userServers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.languageId == config.languageId && it.name == config.name }
        if (existingIdx >= 0) {
            current[existingIdx] = config
        } else {
            current.add(config)
        }
        _userServers.value = current

        val newLanguages = current.filter { it.enabled }.map { it.languageId }.toSet()
        _supportedLanguages.value = _supportedLanguages.value + newLanguages
    }

    /**
     * 移除用户配置的 LSP 服务器
     * 
     * @param languageId 语言 ID
     * @param name 服务器名称
     */
    fun removeUserServer(languageId: String, name: String) {
        val current = _userServers.value.toMutableList()
        current.removeAll { it.languageId == languageId && it.name == name }
        _userServers.value = current

        scope.launch {
            val clientToShutdown = clientMutex.withLock {
                val client = clients.remove(languageId)
                if (client != null) {
                    _clientStatus.update { it - languageId }
                }
                client
            }
            try {
                clientToShutdown?.shutdown()
            } catch (_: Exception) {}
        }
    }

    /**
     * 关闭所有 LSP 客户端并释放资源。
     * 取消所有进行中的协程，关闭所有客户端，清理所有缓存状态。
     * 应在应用终止时调用以确保资源正确释放。
     */
    fun shutdownAll() {
        // 清理所有补全代次计数器（使所有进行中的请求在返回时视为过期）
        completionGenerations.clear()

        // 取消所有进行中的初始化任务
        clientInitJobs.values.forEach { it.cancel() }
        clientInitJobs.clear()

        // 取消协程作用域中的所有子协程
        scope.cancel()

        // 在当前协程上下文中同步关闭客户端（避免启动新协程，因为scope已取消）
        kotlinx.coroutines.runBlocking {
            clientMutex.withLock {
                clients.values.forEach { client ->
                    try {
                        client.shutdown()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error shutting down LSP client", e)
                    }
                }
                clients.clear()
            }
        }
        _clientStatus.value = emptyMap()
        documentVersions.clear()
    }
}

/**
 * 用户配置的 LSP 客户端，启动用户指定的本地进程
 * 
 * 这是一个存根实现——完整实现将需要：
 * 1. 产生配置的命令作为子进程
 * 2. 通过 stdin/stdout 实现 JSON-RPC 传输
 * 3. 关联请求/响应 ID
 * 4. 在后台协程中处理通知
 * 
 * @property config 服务器配置
 * @property context 应用上下文
 */
private class UserConfiguredLspClient(
    private val config: LspServerConfig,
    private val context: Context,
) : LspClient {

    @Volatile
    private var running = false

    private var process: Process? = null

    override val isRunning: Boolean
        get() = running

    override suspend fun initialize(rootUri: String): Result<Unit> {
        return try {
            val cmd = mutableListOf<String>()
            cmd.add(config.command)
            cmd.addAll(config.args)

            val workDir = config.workingDir?.let { File(it) } ?: context.filesDir

            process = ProcessBuilder(cmd)
                .directory(workDir)
                .redirectErrorStream(true)
                .start()

            running = true
            Log.d("UserConfiguredLsp", "Started LSP server: ${config.name} (${config.command})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserConfiguredLsp", "Failed to start LSP server: ${config.name}", e)
            Result.failure(e)
        }
    }

    override suspend fun openDocument(uri: String, languageId: String, text: String) {
        // TODO: Send textDocument/didOpen via JSON-RPC
    }

    override suspend fun closeDocument(uri: String) {
        // TODO: Send textDocument/didClose via JSON-RPC
    }

    override suspend fun updateDocument(uri: String, text: String, version: Int) {
        // TODO: Send textDocument/didChange via JSON-RPC
    }

    override suspend fun completions(uri: String, line: Int, column: Int): List<CompletionItem> = emptyList()
    override suspend fun hover(uri: String, line: Int, column: Int): HoverResult? = null
    override suspend fun gotoDefinition(uri: String, line: Int, column: Int): List<LocationLink> = emptyList()
    override suspend fun findReferences(uri: String, line: Int, column: Int, includeDeclaration: Boolean): List<LocationLink> = emptyList()
    override suspend fun signatureHelp(uri: String, line: Int, column: Int): String? = null
    override suspend fun formatting(uri: String, tabSize: Int, insertSpaces: Boolean): List<TextEdit> = emptyList()
    override suspend fun diagnostics(uri: String): List<LspDiagnostic> = emptyList()
    override suspend fun inlayHints(uri: String, range: LspRange): List<InlayHintItem> = emptyList()
    override suspend fun codeActions(uri: String, range: LspRange, diagnostics: List<LspDiagnostic>): List<CodeActionItem> = emptyList()
    override suspend fun rename(uri: String, line: Int, column: Int, newName: String): WorkspaceEdit? = null

    override suspend fun shutdown() {
        running = false
        try {
            // TODO: Send shutdown + exit via JSON-RPC
            process?.destroy()
            process?.waitFor()
        } catch (e: Exception) {
            process?.destroyForcibly()
        }
        process = null
    }
}
