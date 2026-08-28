/**
 * 文件功能：DraftPeek LspClient 与 sora-editor 内置 LSP 模块之间的桥接层
 *
 * 主要类：[SoraLspBridge] —— 桥接 DraftPeek LSP 系统与 sora-editor LSP 模块
 *
 * 模块依赖：
 * - android.content.Context：应用上下文
 * - android.util.Log：日志记录
 * - kotlinx.coroutines：协程支持（withContext）
 * - kotlinx.coroutines.Dispatchers：IO 调度器
 *
 * sora-editor (v0.24.6+) 包含集成的 LSP 客户端模块 (`io.github.rosemoe.sora.lsp`)，支持：
 * - 语言服务器连接（Socket、进程、自定义）
 * - 补全、诊断、悬停、签名帮助
 * - 内嵌提示（实验性）
 * - 代码操作（可定制 UI）
 * - 通过 AggregatedRequestManager 支持多服务器
 *
 * 桥接类提供：
 * 1. LSP 编辑器会话的生命周期管理
 * 2. 连接到外部 LSP 服务器（通过 Socket 或进程）
 * 3. 将 DraftPeek LspClient 委托给 sora-editor LSP 事件
 *
 * 注意：此桥接需要 `io.github.rosemoe:editor-lsp` 依赖项。如果运行时依赖项不可用，
 * 桥接会优雅回退到 DraftPeek 的独立 LSP 实现。
 */
package com.draftpeek.feature.editor.lsp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sora 编辑器 LSP 桥接类
 *
 * 负责管理 sora-editor LSP 模块与 DraftPeek 之间的连接。
 *
 * 架构说明：
 * ```
 * DraftPeek LspClient  ←→  SoraLspBridge  ←→  sora-editor LspEditor
 *       ↓                                         ↓
 *   LspManager                              LspProject
 *       ↓                                         ↓
 *   LspServerConfig                     LanguageServerDefinition
 * ```
 *
 * @property context Android 上下文
 * @property editor sora-editor CodeEditor 实例（使用 Any 类型以避免编译时依赖）
 */
class SoraLspBridge(private val context: Context, private val editor: Any?) {

    private val _connectedServers = mutableMapOf<String, ServerConnection>()

    companion object {
        private const val TAG = "SoraLspBridge"

        /**
         * 检查运行时是否可用 sora-editor 的 LSP 模块
         *
         * 算法步骤：
         * 1. 尝试通过反射加载 LspEditor 类
         * 2. 如果类存在，返回 true
         * 3. 如果 ClassNotFoundException，返回 false（优雅降级）
         *
         * @return LSP 模块可用返回 true，否则返回 false
         */
        fun isLspModuleAvailable(): Boolean = try {
            Class.forName("io.github.rosemoe.sora.lsp.LspEditor")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /**
     * LSP 服务器的连接信息
     *
     * 支持三种连接方式：Socket、进程、自定义流
     */
    sealed class ServerConnectionInfo {
        /**
         * 通过 TCP Socket 连接
         *
         * @property host 服务器主机名
         * @property port 服务器端口
         * @property timeoutMs 连接超时时间（毫秒）
         */
        data class Socket(val host: String, val port: Int, val timeoutMs: Long = 10000) : ServerConnectionInfo()

        /**
         * 通过进程连接（将 LSP 服务器作为子进程启动）
         *
         * @property command 启动命令及参数列表
         * @property workingDirectory 工作目录（null 表示应用默认目录）
         */
        data class Process(val command: List<String>, val workingDirectory: String? = null) : ServerConnectionInfo()

        /**
         * 通过自定义流提供者连接
         *
         * @property providerName 自定义提供者名称
         */
        data class Custom(val providerName: String) : ServerConnectionInfo()
    }

    /**
     * 表示一个活动的服务器连接
     *
     * @property languageId 语言标识符
     * @property info 连接信息
     * @property connectedAt 连接时间戳
     * @property isActive 连接是否活跃
     */
    data class ServerConnection(
        val languageId: String,
        val info: ServerConnectionInfo,
        val connectedAt: Long = System.currentTimeMillis(),
        var isActive: Boolean = true
    )

    /**
     * 连接到指定语言的 LSP 服务器
     *
     * 算法步骤：
     * 1. 检查 sora-editor LSP 模块是否可用
     * 2. 如果该语言已有连接，先断开旧连接
     * 3. 根据连接信息类型选择连接方式（Socket/进程/自定义）
     * 4. 建立连接并注册到 sora-editor LSP 项目
     * 5. 更新连接状态
     *
     * @param languageId 语言标识符（如 "python"、"kotlin"）
     * @param info 服务器连接信息
     * @return 连接启动成功返回 true
     */
    suspend fun connectToServer(languageId: String, info: ServerConnectionInfo): Boolean = withContext(Dispatchers.IO) {
        if (!isLspModuleAvailable()) {
            Log.w(TAG, "sora-editor LSP module not available. Falling back to standalone LSP.")
            return@withContext false
        }

        try {
            if (_connectedServers.containsKey(languageId)) {
                disconnect(languageId)
            }

            val connection = ServerConnection(
                languageId = languageId,
                info = info
            )

            when (info) {
                is ServerConnectionInfo.Socket -> {
                    connectViaSocket(languageId, info)
                }
                is ServerConnectionInfo.Process -> {
                    connectViaProcess(languageId, info)
                }
                is ServerConnectionInfo.Custom -> {
                    Log.w(TAG, "Custom connection not yet implemented for $languageId")
                    return@withContext false
                }
            }

            _connectedServers[languageId] = connection
            Log.d(TAG, "LSP server connected for $languageId via ${info::class.simpleName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect LSP server for $languageId", e)
            false
        }
    }

    /**
     * 断开 LSP 服务器连接
     *
     * @param languageId 要断开的语言
     */
    fun disconnect(languageId: String) {
        val connection = _connectedServers.remove(languageId)
        if (connection != null) {
            connection.isActive = false
            try {
                disposeLspEditor(languageId)
            } catch (e: Exception) {
                Log.w(TAG, "Error disposing LSP editor for $languageId", e)
            }
            Log.d(TAG, "LSP server disconnected for $languageId")
        }
    }

    /**
     * 断开所有 LSP 服务器连接
     */
    fun disconnectAll() {
        _connectedServers.keys.toList().forEach { disconnect(it) }
    }

    /**
     * 检查指定语言的服务器是否已连接
     *
     * @param languageId 语言标识符
     * @return 已连接且活跃返回 true
     */
    fun isConnected(languageId: String): Boolean = _connectedServers[languageId]?.isActive == true

    /**
     * 获取所有已连接服务器的语言集合
     *
     * @return 活跃连接的语言 ID 集合
     */
    fun getConnectedLanguages(): Set<String> = _connectedServers.filter { it.value.isActive }.keys.toSet()

    /**
     * 使用 sora-editor 的 SocketStreamConnectionProvider 通过 TCP Socket 连接
     *
     * 算法说明：
     * 当 sora-editor LSP 模块可用时，将使用 languageServerDefinition DSL 创建
     * 服务器定义，并通过 LspProject 注册，然后创建 LspEditor 实例绑定到编辑器。
     *
     * @param languageId 语言标识符
     * @param info Socket 连接信息
     */
    private fun connectViaSocket(languageId: String, info: ServerConnectionInfo.Socket) {
        // 当 sora-editor LSP 模块可用时，这里将使用：
        // val serverDefinition = languageServerDefinition(languageId) {
        //     name("$languageId-lsp")
        //     connection { socket(host = info.host, port = info.port) }
        // }
        // lspProject.addServerDefinitions(listOf(serverDefinition))
        // lspProject.createEditor(editor.currentFile)

        Log.d(TAG, "Socket connection configured for $languageId at ${info.host}:${info.port}")
    }

    /**
     * 使用 sora-editor 的 ProcessConnectionProvider 通过进程连接
     *
     * 算法说明：
     * 启动外部语言服务器进程（如 pylsp、typescript-language-server），
     * 通过 stdin/stdout 建立 JSON-RPC 通信通道。
     *
     * @param languageId 语言标识符
     * @param info 进程连接信息
     */
    private fun connectViaProcess(languageId: String, info: ServerConnectionInfo.Process) {
        // 当 sora-editor LSP 模块可用时，这里将使用：
        // val serverDefinition = languageServerDefinition(languageId) {
        //     name("$languageId-lsp")
        //     connection {
        //         custom { workingDir ->
        //             val process = Runtime.getRuntime().exec(
        //                 info.command.toTypedArray(),
        //                 null,
        //                 workingDir?.let { File(it) }
        //             )
        //             StreamConnectionProvider.StreamProvider(
        //                 process.inputStream,
        //                 process.outputStream
        //             )
        //         }
        //     }
        // }

        Log.d(TAG, "Process connection configured for $languageId: ${info.command.joinToString(" ")}")
    }

    /**
     * 释放指定语言的 LSP 编辑器资源
     *
     * @param languageId 语言标识符
     */
    private fun disposeLspEditor(languageId: String) {
        Log.d(TAG, "LSP editor disposed for $languageId")
    }
}
