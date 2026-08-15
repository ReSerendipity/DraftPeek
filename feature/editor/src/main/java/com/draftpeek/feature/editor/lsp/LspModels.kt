/**
 * 文件功能：LSP（Language Server Protocol）数据模型定义
 * 
 * 主要数据类：
 * - [CompletionItem]：自动补全项
 * - [LspDiagnostic]：LSP 诊断项（使用行/列位置）
 * - [HoverResult]：悬停信息结果
 * - [LspPosition]：文档位置（行/列）
 * - [LspRange]：文档范围（起止位置）
 * - [LocationLink]：位置链接（URI + 范围）
 * - [TextEdit]：文本编辑
 * - [InlayHintItem]：内嵌提示（类型注解、参数名等）
 * - [CodeActionItem]：代码操作（快速修复、重构等）
 * - [WorkspaceEdit]：工作区编辑（跨文件文本修改）
 * 
 * 模块依赖：无外部依赖，纯数据类定义
 * 
 * 注意：与 diagnostics/DiagnosticItem 不同，LSP 诊断使用基于 0 的行/列位置，
 * 而 DiagnosticItem 使用字符偏移，这是 LSP 规范定义的标准格式。
 */
package com.draftpeek.feature.editor.lsp

/**
 * 语言服务器返回的自动补全项
 * 
 * @property label 补全弹出窗口中显示的文本
 * @property kind LSP CompletionItemKind 值（如 3 = 函数，6 = 变量）
 * @property detail 标签旁边显示的简短类型/签名（可选）
 * @property documentation 完整文档字符串（可选）
 * @property insertText 选中时插入的文本；为 null 时回退到 [label]
 */
data class CompletionItem(
    val label: String,
    val kind: Int,
    val detail: String? = null,
    val documentation: String? = null,
    val insertText: String? = null,
)

/**
 * 语言服务器返回的诊断项
 * 
 * 与 [com.draftpeek.feature.editor.diagnostics.DiagnosticItem] 使用字符偏移不同，
 * LSP 诊断使用 LSP 规范定义的基于 0 的行/列位置。
 * 
 * @property line 起始行（0-based）
 * @property column 起始列（0-based，UTF-16 代码单元）
 * @property endLine 结束行（0-based）
 * @property endColumn 结束列（0-based，UTF-16 代码单元）
 * @property severity 严重级别：1 = 错误，2 = 警告，3 = 信息，4 = 提示
 * @property message 人类可读描述
 * @property source 诊断来源（如 "pylsp"、"pyflakes"）
 */
data class LspDiagnostic(
    val line: Int,
    val column: Int,
    val endLine: Int,
    val endColumn: Int,
    val severity: Int,
    val message: String,
    val source: String? = null,
) {
    companion object {
        /** 错误级别 */
        const val SEVERITY_ERROR = 1
        /** 警告级别 */
        const val SEVERITY_WARNING = 2
        /** 信息级别 */
        const val SEVERITY_INFORMATION = 3
        /** 提示级别 */
        const val SEVERITY_HINT = 4
    }
}

// ── 悬停信息 ───────────────────────────────────────────────────────────────

/**
 * textDocument/hover 请求的结果
 * 
 * @property contents 悬停内容（Markdown 字符串）
 * @property range 文档中的悬停范围（如果可用）
 */
data class HoverResult(
    val contents: String,
    val range: LspRange? = null,
)

// ── 位置 / 定义 / 引用 ─────────────────────────────────────────────────────

/**
 * 文档中基于 0 的行/列位置
 * 
 * @property line 行号（0-based）
 * @property column 列号（0-based，UTF-16 代码单元偏移）
 */
data class LspPosition(
    val line: Int,
    val column: Int,
)

/**
 * 文档中的范围，由起始和结束位置定义
 * 
 * @property start 起始位置
 * @property end 结束位置
 */
data class LspRange(
    val start: LspPosition,
    val end: LspPosition,
)

/**
 * 位置引用：URI + 文档内范围
 * 
 * 用于 gotoDefinition、findReferences 和 typeDefinition 响应。
 * 
 * @property uri 文档 URI
 * @property range 该文档内的范围
 * @property targetRange 触发此位置的符号范围（如函数名），可选
 */
data class LocationLink(
    val uri: String,
    val range: LspRange,
    val targetRange: LspRange? = null,
)

// ── 文档格式化 ─────────────────────────────────────────────────────────────

/**
 * 应用到文档的文本编辑
 * 
 * @property range 要替换的范围
 * @property newText 要插入的文本
 */
data class TextEdit(
    val range: LspRange,
    val newText: String,
)

// ── 内嵌提示 ───────────────────────────────────────────────────────────────

/**
 * 编辑器中内联显示的提示项
 * 
 * 内嵌提示用于显示类型注解、参数名等内联信息。
 * 
 * @property position 提示应显示的位置
 * @property label 提示文本（可能包含多个部分用于样式设置）
 * @property kind 提示类型：1 = 类型，2 = 参数（LSP InlayHintKind）
 * @property tooltip 悬停时显示的可选工具提示
 */
data class InlayHintItem(
    val position: LspPosition,
    val label: String,
    val kind: Int = 1,
    val tooltip: String? = null,
) {
    companion object {
        /** 类型提示 */
        const val KIND_TYPE = 1
        /** 参数提示 */
        const val KIND_PARAMETER = 2
    }
}

// ── 代码操作 ───────────────────────────────────────────────────────────────

/**
 * 代码操作（快速修复、重构或源操作）
 * 
 * @property title 用户可见的操作标题
 * @property kind 代码操作类型（如 "quickfix"、"refactor"、"source"）
 * @property edit 触发操作时要应用的工作区编辑
 * @property isPreferred 这是否是该类型的首选操作
 * @property command 应用编辑后执行的可选命令
 */
data class CodeActionItem(
    val title: String,
    val kind: String? = null,
    val edit: WorkspaceEdit? = null,
    val isPreferred: Boolean = false,
    val command: String? = null,
) {
    companion object {
        /** 快速修复 */
        const val KIND_QUICK_FIX = "quickfix"
        /** 重构 */
        const val KIND_REFACTOR = "refactor"
        /** 提取重构 */
        const val KIND_REFACTOR_EXTRACT = "refactor.extract"
        /** 内联重构 */
        const val KIND_REFACTOR_INLINE = "refactor.inline"
        /** 重写重构 */
        const val KIND_REFACTOR_REWRITE = "refactor.rewrite"
        /** 源操作 */
        const val KIND_SOURCE = "source"
        /** 组织导入源操作 */
        const val KIND_SOURCE_ORGANIZE_IMPORTS = "source.organizeImports"
    }
}

// ── 工作区编辑 ─────────────────────────────────────────────────────────────

/**
 * 跨多个文档的文本编辑集合
 * 
 * 用于 codeAction 和 rename 响应。
 * 
 * @property changes 文档 URI 到该文档编辑列表的映射
 */
data class WorkspaceEdit(
    val changes: Map<String, List<TextEdit>> = emptyMap(),
)
