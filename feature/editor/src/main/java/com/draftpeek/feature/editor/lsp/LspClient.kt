/**
 * 文件功能：LSP 客户端接口定义
 * 
 * 主要接口：[LspClient] —— 定义与语言服务器通信的契约
 * 
 * 模块依赖：仅依赖同包下的数据模型 [CompletionItem]、[LspDiagnostic] 等
 * 
 * 接口方法覆盖了编辑器常用的 LSP 功能：
 * - 初始化与关闭服务器
 * - 文档打开/关闭/更新同步
 * - 自动补全、悬停提示、跳转定义、查找引用
 * - 签名帮助、格式化、内嵌提示、代码操作、重命名
 */
package com.draftpeek.feature.editor.lsp

/**
 * LSP 客户端接口
 * 
 * 定义与语言服务器通信的契约。每个语言服务器实例（pylsp、typescript-language-server 等）
 * 应实现此接口。实现负责：
 * 1. 管理服务器进程/连接的生命周期
 * 2. 发送 LSP JSON-RPC 请求/通知
 * 3. 将服务器响应解析为此文件中定义的模型类型
 * 
 * 所有挂起函数应在后台调度器上调用（IO 或默认），因为它们涉及网络/子进程 I/O。
 */
interface LspClient {

    /** 客户端是否已初始化并准备好处理请求 */
    val isRunning: Boolean

    // ── 生命周期 ────────────────────────────────────────────────────────────

    /**
     * 初始化 LSP 服务器
     * 
     * 在任何其他请求之前调用。发送 `initialize` 请求，随后发送 `initialized` 通知。
     * 
     * @param rootUri 工作区根目录的文件 URI
     * @return 初始化成功时返回 [Result.success]，失败时返回 [Result.failure]
     */
    suspend fun initialize(rootUri: String): Result<Unit>

    /**
     * 优雅关闭服务器
     * 
     * 发送 `shutdown` 请求，然后发送 `exit` 通知。如果服务器未能及时退出，
     * 实现应终止底层进程。
     */
    suspend fun shutdown()

    // ── 文档同步 ────────────────────────────────────────────────────────────

    /**
     * 通知服务器文档已打开
     * 
     * 发送 `textDocument/didOpen` 通知。此方法是文档同步必需的——服务器
     * 直到收到 didOpen 才会跟踪文档内容。
     * 
     * @param uri 打开的文档 URI
     * @param languageId 语言标识符（如 "python"、"kotlin"）
     * @param text 完整文档内容
     */
    suspend fun openDocument(uri: String, languageId: String, text: String)

    /**
     * 通知服务器文档已关闭
     * 
     * 发送 `textDocument/didClose` 通知。服务器将不再跟踪此文档。
     * 
     * @param uri 关闭的文档 URI
     */
    suspend fun closeDocument(uri: String)

    /**
     * 通知服务器文档内容已更改
     * 
     * 发送 `textDocument/didChange` 通知。当前实现使用完整同步（而非增量），
     * 这意味着每次更改都会发送整个文档文本。
     * 
     * @param uri 更改的文档 URI
     * @param text 新的完整文档内容
     * @param version 单调递增的文档版本号
     */
    suspend fun updateDocument(uri: String, text: String, version: Int)

    // ── 语言功能 ────────────────────────────────────────────────────────────

    /**
     * 请求给定位置的自动补全项
     * 
     * 发送 `textDocument/completion` 请求。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @return 在请求位置可用的补全项列表；无补全时返回空列表
     */
    suspend fun completions(uri: String, line: Int, column: Int): List<CompletionItem>

    /**
     * 请求给定位置的悬停信息
     * 
     * 发送 `textDocument/hover` 请求。悬停内容通常包含类型签名或文档。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @return 悬停结果，如果该位置无悬停信息则返回 null
     */
    suspend fun hover(uri: String, line: Int, column: Int): HoverResult?

    /**
     * 请求给定位置的定义位置
     * 
     * 发送 `textDocument/definition` 请求。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @return 定义位置列表；未找到定义时返回空列表
     */
    suspend fun gotoDefinition(uri: String, line: Int, column: Int): List<LocationLink>

    /**
     * 请求引用给定位置符号的位置
     * 
     * 发送 `textDocument/references` 请求。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @param includeDeclaration 是否在结果中包含声明位置
     * @return 引用位置列表
     */
    suspend fun findReferences(
        uri: String,
        line: Int,
        column: Int,
        includeDeclaration: Boolean,
    ): List<LocationLink>

    /**
     * 请求给定位置的函数/方法的签名帮助
     * 
     * 发送 `textDocument/signatureHelp` 请求。返回签名文档，用于参数提示。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @return 签名文档字符串（Markdown），不可用时返回 null
     */
    suspend fun signatureHelp(uri: String, line: Int, column: Int): String?

    /**
     * 请求整个文档的格式化文本编辑
     * 
     * 发送 `textDocument/formatting` 请求。
     * 
     * @param uri 文档 URI
     * @param tabSize 每个缩进级别的空格数
     * @param insertSpaces 用空格代替制表符
     * @return 应用格式化的文本编辑列表
     */
    suspend fun formatting(uri: String, tabSize: Int, insertSpaces: Boolean): List<TextEdit>

    /**
     * 从服务器推送的诊断中检索文档的最新诊断信息
     * 
     * 注意：这不会发送请求——它返回通过 `textDocument/publishDiagnostics` 通知
     * 已接收的缓存诊断信息。
     * 
     * @param uri 文档 URI
     * @return 该文档的当前已知诊断列表
     */
    suspend fun diagnostics(uri: String): List<LspDiagnostic>

    /**
     * 请求文档内指定范围的内嵌提示
     * 
     * 发送 `textDocument/inlayHint` 请求（LSP 3.17+）。内嵌提示是插入文本中的
     * 内联标签，显示类型注解或参数名。
     * 
     * @param uri 文档 URI
     * @param range 请求提示的文档范围
     * @return 该范围内的内嵌提示列表
     */
    suspend fun inlayHints(uri: String, range: LspRange): List<InlayHintItem>

    /**
     * 请求给定范围和上下文诊断的代码操作（快速修复、重构）
     * 
     * 发送 `textDocument/codeAction` 请求。代码操作包括用于解决诊断问题的
     * 快速修复和用于代码重构的源操作。
     * 
     * @param uri 文档 URI
     * @param range 请求操作的范围（通常是光标位置或选择内容）
     * @param diagnostics 上下文中的诊断，用于过滤相关操作
     * @return 可用代码操作列表
     */
    suspend fun codeActions(uri: String, range: LspRange, diagnostics: List<LspDiagnostic>): List<CodeActionItem>

    /**
     * 请求符号重命名
     * 
     * 发送 `textDocument/rename` 请求。返回一个工作区编辑，其中包含
     * 所有受影响文档中所有出现位置的替换。
     * 
     * @param uri 文档 URI
     * @param line 光标行号（0-based）
     * @param column 光标列号（0-based，UTF-16 代码单元）
     * @param newName 符号的新名称
     * @return 包含所有重命名编辑的工作区编辑，如果不支持重命名则返回 null
     */
    suspend fun rename(uri: String, line: Int, column: Int, newName: String): WorkspaceEdit?
}
