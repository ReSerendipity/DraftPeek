package com.draftpeek.feature.editor.sora

import android.util.Log
import androidx.annotation.UiThread
import com.draftpeek.feature.editor.diagnostics.DiagnosticItem
import com.draftpeek.feature.editor.diagnostics.DiagnosticNavigationState
import com.draftpeek.feature.editor.diagnostics.moveToNext
import com.draftpeek.feature.editor.diagnostics.moveToPrevious
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer

/**
 * SoraEditorWrapper 诊断相关顶层扩展（从 SoraEditorWrapper.kt 拆分，T2）。
 * 保持 public API 签名不变；与撤销持久化扩展（SoraEditorWrapperUndo.kt）配套。
 */

// ------------------------------------------------------------------
// Diagnostics (wavy underlines + tooltip)
// ------------------------------------------------------------------

/**
 * Apply diagnostic markers to the editor.
 *
 * Each [DiagnosticItem] produces a wavy underline at the given character range
 * with the corresponding severity colour (error / warning / typo).
 * Hovering / clicking the marker shows [DiagnosticItem.message] in a tooltip.
 *
 * Must be called on the UI thread.
 */
@UiThread
fun SoraEditorWrapper.setDiagnostics(diagnostics: List<DiagnosticItem>) {
    val container = DiagnosticsContainer()
    for ((index, item) in diagnostics.withIndex()) {
        val region = DiagnosticRegion(
            item.startIndex,
            item.endIndex,
            item.severity,
            index.toLong(),
            DiagnosticDetail(item.message)
        )
        container.addDiagnostic(region)
    }
    editor.setDiagnostics(container)
}

/** Clear all diagnostic markers. Must be called on the UI thread. */
@UiThread
fun SoraEditorWrapper.clearDiagnostics() {
    editor.setDiagnostics(null)
}

/**
 * Ch2#5: Jump the cursor to the diagnostic at [startIndex].
 *
 * Moves the selection cursor to the beginning of the diagnostic
 * range and centers the viewport on that position.
 *
 * Must be called on the UI thread.
 */
@UiThread
fun SoraEditorWrapper.jumpToDiagnostic(startIndex: Int, endIndex: Int) {
    val content = editor.text ?: return
    if (startIndex < 0 || startIndex >= content.length) return
    // Convert character index to line/column position
    val position = content.indexer.getCharPosition(startIndex)
    val line = position.line
    val column = position.column
    // Set cursor to the start of the diagnostic
    editor.setSelection(line, column)
    // Ensure the position is visible
    editor.ensurePositionVisible(line, column)
}

/**
 * 把选区移到字符区间 `[startIndex, endIndex)` 上，并把视口滚到该处。
 *
 * 供搜索面板的「上一个 / 下一个」使用。**为什么不用 `searcher.gotoNext()`**：
 * 真机走查发现 sora 的 `gotoNext/gotoPrevious` 在**同一行内的相邻匹配**上不推进选区
 * （如 `bb` 里搜 `b`），序号会永远停在 1/N；由调用方按自己算出的匹配表显式移动
 * 可以完全避开该行为，且与面板显示的 `n/N` 天然一致。
 *
 * Must be called on the UI thread.
 *
 * @return 移动成功返回 true；区间非法或编辑器不可用时返回 false
 */
@UiThread
fun SoraEditorWrapper.selectMatchRange(startIndex: Int, endIndex: Int): Boolean {
    return try {
        val content = editor.text ?: return false
        if (startIndex < 0 || startIndex >= content.length) return false
        val start = content.indexer.getCharPosition(startIndex)
        val safeEnd = endIndex.coerceIn(startIndex + 1, content.length)
        val end = content.indexer.getCharPosition(safeEnd)
        editor.setSelectionRegion(start.line, start.column, end.line, end.column)
        editor.ensurePositionVisible(start.line, start.column)
        true
    } catch (e: Exception) {
        Log.w(SoraEditorWrapper.TAG, "selectMatchRange failed", e)
        false
    }
}

/**
 * Ch2#5: Jump to the next diagnostic in the navigation state.
 *
 * Returns the updated [DiagnosticNavigationState] after moving.
 * The caller is responsible for calling [jumpToDiagnostic] with
 * the focused item's position.
 */
fun SoraEditorWrapper.navigateToNextDiagnostic(state: DiagnosticNavigationState): DiagnosticNavigationState {
    val next = state.moveToNext()
    val current: DiagnosticItem? = next.current
    if (current != null) {
        jumpToDiagnostic(current.startIndex, current.endIndex)
    }
    return next
}

/**
 * Ch2#5: Jump to the previous diagnostic in the navigation state.
 */
fun SoraEditorWrapper.navigateToPreviousDiagnostic(state: DiagnosticNavigationState): DiagnosticNavigationState {
    val prev = state.moveToPrevious()
    val current: DiagnosticItem? = prev.current
    if (current != null) {
        jumpToDiagnostic(current.startIndex, current.endIndex)
    }
    return prev
}
