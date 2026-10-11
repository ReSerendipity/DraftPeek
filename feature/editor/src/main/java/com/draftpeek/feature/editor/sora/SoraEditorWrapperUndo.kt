package com.draftpeek.feature.editor.sora

import android.util.Log
import com.draftpeek.feature.editor.data.CacheManager

/**
 * SoraEditorWrapper 撤销栈持久化顶层扩展（从 SoraEditorWrapper.kt 拆分，T2）。
 * 保持 public API 签名不变；updateUndoLimit 由成员改为 internal 扩展。
 */

// ------------------------------------------------------------------
// Undo stack persistence
// ------------------------------------------------------------------

/**
 * Save the undo stack to the cache directory via [CacheManager].
 *
 * @return true if saved successfully
 */
fun SoraEditorWrapper.saveUndoState(cacheManager: CacheManager, uri: String): Boolean {
    return try {
        val content = editor.text ?: return false
        val undoManager = content.undoManager
        cacheManager.saveUndoState(uri, undoManager)
    } catch (e: Exception) {
        Log.w(SoraEditorWrapper.TAG, "saveUndoState failed", e)
        false
    }
}

/**
 * Restore the undo stack from the cache directory via [CacheManager].
 *
 * @return true if restored successfully
 */
fun SoraEditorWrapper.restoreUndoState(cacheManager: CacheManager, uri: String): Boolean {
    return try {
        val content = editor.text ?: return false
        val undoManager = cacheManager.restoreUndoState(uri) ?: return false
        content.undoManager = undoManager
        true
    } catch (e: Exception) {
        Log.w(SoraEditorWrapper.TAG, "restoreUndoState failed", e)
        false
    }
}

/**
 * P0-6: Dynamically set the undo stack size limit based on current content size.
 *
 * Since sora-editor's [setMaxUndoStackSize] only supports count-based limits,
 * we compute an approximate operation count that corresponds to the 512 KB
 * size budget for the current file size.
 *
 * Heuristic: each undo entry stores the changed text region plus metadata.
 * For a file of size N bytes, the average change is roughly N/50 bytes,
 * so 512 KB / (N/50 + 100) gives a reasonable operation count, clamped
 * between 50 and [UNDO_MAX_OPERATIONS].
 */
internal fun SoraEditorWrapper.updateUndoLimit() {
    val editor = this.editor
    val text = editor.text ?: return
    val contentBytes = text.length.toLong() * 2L // UTF-16 chars = 2 bytes each
    val estimatedOps = if (contentBytes > 0) {
        (SoraEditorWrapper.UNDO_SIZE_LIMIT_BYTES / (contentBytes / 50 + 100)).toInt()
            .coerceIn(50, SoraEditorWrapper.UNDO_MAX_OPERATIONS)
    } else {
        500
    }
    text.setMaxUndoStackSize(estimatedOps)
    Log.d(SoraEditorWrapper.TAG, "Undo limit set to $estimatedOps operations (contentBytes=$contentBytes)")
}
