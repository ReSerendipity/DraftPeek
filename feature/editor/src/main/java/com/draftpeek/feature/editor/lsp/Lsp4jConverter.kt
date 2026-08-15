/**
 * LSP4J 类型转换工具。
 *
 * 提供 DraftPeek 自研 LSP 数据模型与 Eclipse LSP4J 标准类型之间的双向转换。
 * 这是 LSP4J 集成的第一步（P1），为未来完全迁移到 LSP4J 的 LanguageServer/LanguageClient
 * 接口奠定基础。
 *
 * 当前策略：
 * - 保留自研 [LspClient] 接口和 [LspModels] 数据类（消费者无需改动）
 * - 新增 LSP4J 依赖作为协议层基础
 * - 通过本转换器实现两套类型系统的互通
 * - 未来迭代中将逐步将 LspClient 实现切换到 LSP4J 的 LanguageServer
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.feature.editor.lsp

import org.eclipse.lsp4j.CompletionItem as Lsp4jCompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Diagnostic as Lsp4jDiagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Hover as Lsp4jHover
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Position as Lsp4jPosition
import org.eclipse.lsp4j.Range as Lsp4jRange
import org.eclipse.lsp4j.TextEdit as Lsp4jTextEdit

/**
 * LSP4J 类型转换器对象。
 *
 * 提供 LSP4J 标准类型 → DraftPeek 自研模型的转换方法。
 * 反向转换将在完全迁移到 LSP4J 接口后添加。
 */
object Lsp4jConverter {

    // ── Position / Range ──────────────────────────────────────────────────

    /**
     * 将 LSP4J [Lsp4jPosition] 转换为自研 [LspPosition]。
     */
    fun toLspPosition(pos: Lsp4jPosition): LspPosition =
        LspPosition(line = pos.line, column = pos.character)

    /**
     * 将 LSP4J [Lsp4jRange] 转换为自研 [LspRange]。
     */
    fun toLspRange(range: Lsp4jRange): LspRange =
        LspRange(
            start = toLspPosition(range.start),
            end = toLspPosition(range.end),
        )

    // ── Completion ────────────────────────────────────────────────────────

    /**
     * 将 LSP4J [Lsp4jCompletionItem] 转换为自研 [CompletionItem]。
     */
    fun toCompletionItem(item: Lsp4jCompletionItem): CompletionItem =
        CompletionItem(
            label = item.label ?: "",
            kind = item.kind?.value ?: 0,
            detail = item.detail,
            documentation = item.documentation?.let { doc ->
                when (doc) {
                    is String -> doc
                    is MarkupContent -> doc.value
                    else -> null
                }
            },
            insertText = item.insertText,
        )

    /**
     * 批量转换 LSP4J 补全项列表。
     */
    fun toCompletionItems(items: List<Lsp4jCompletionItem>): List<CompletionItem> =
        items.map { toCompletionItem(it) }

    // ── Diagnostics ───────────────────────────────────────────────────────

    /**
     * 将 LSP4J [Lsp4jDiagnostic] 转换为自研 [LspDiagnostic]。
     */
    fun toLspDiagnostic(diag: Lsp4jDiagnostic): LspDiagnostic =
        LspDiagnostic(
            line = diag.range?.start?.line ?: 0,
            column = diag.range?.start?.character ?: 0,
            endLine = diag.range?.end?.line ?: 0,
            endColumn = diag.range?.end?.character ?: 0,
            severity = diag.severity?.value ?: LspDiagnostic.SEVERITY_ERROR,
            message = diag.message ?: "",
            source = diag.source,
        )

    /**
     * 批量转换 LSP4J 诊断列表。
     */
    fun toLspDiagnostics(diags: List<Lsp4jDiagnostic>): List<LspDiagnostic> =
        diags.map { toLspDiagnostic(it) }

    // ── Hover ─────────────────────────────────────────────────────────────

    /**
     * 将 LSP4J [Lsp4jHover] 转换为自研 [HoverResult]。
     */
    fun toHoverResult(hover: Lsp4jHover): HoverResult {
        val contents = hover.contents?.let { content ->
            when (content) {
                is String -> content
                is MarkupContent -> content.value
                else -> content.toString()
            }
        } ?: ""
        return HoverResult(
            contents = contents,
            range = hover.range?.let { toLspRange(it) },
        )
    }

    // ── TextEdit ──────────────────────────────────────────────────────────

    /**
     * 将 LSP4J [Lsp4jTextEdit] 转换为自研 [TextEdit]。
     */
    fun toTextEdit(edit: Lsp4jTextEdit): TextEdit =
        TextEdit(
            range = toLspRange(edit.range),
            newText = edit.newText ?: "",
        )

    /**
     * 批量转换 LSP4J 文本编辑列表。
     */
    fun toTextEdits(edits: List<Lsp4jTextEdit>): List<TextEdit> =
        edits.map { toTextEdit(it) }

    // ── Reverse conversions (for sending requests to LSP server) ──────────

    /**
     * 将自研 [LspPosition] 转换为 LSP4J [Lsp4jPosition]。
     */
    fun fromLspPosition(pos: LspPosition): Lsp4jPosition =
        Lsp4jPosition(pos.line, pos.column)

    /**
     * 将自研 [LspRange] 转换为 LSP4J [Lsp4jRange]。
     */
    fun fromLspRange(range: LspRange): Lsp4jRange =
        Lsp4jRange(
            fromLspPosition(range.start),
            fromLspPosition(range.end),
        )
}
