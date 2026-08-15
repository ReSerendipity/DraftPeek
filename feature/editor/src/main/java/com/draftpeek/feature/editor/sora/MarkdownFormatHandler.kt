package com.draftpeek.feature.editor.sora

import android.util.Log
import androidx.annotation.UiThread
import com.draftpeek.feature.editor.ui.MarkdownFormatAction
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Handles Markdown formatting operations on a [CodeEditor].
 * Extracted from SoraEditorWrapper for single responsibility (B1/SRP).
 *
 * @param editor The sora-editor CodeEditor instance to operate on.
 */
class MarkdownFormatHandler(private val editor: CodeEditor) {

    companion object {
        private const val TAG = "MarkdownFormatHandler"
    }

    /**
     * Apply a Markdown format action to the editor content.
     * Handles wrapping selected text, inserting templates, and line prefixes.
     */
    @UiThread
    fun applyFormatAction(action: MarkdownFormatAction) {
        try {
            val cursor = editor.cursor
            if (cursor == null) {
                // No cursor, just insert at end
                val text = editor.text
                val end = text?.length ?: 0
                when (action) {
                    is MarkdownFormatAction.Wrap -> {
                        text?.replace(end, end, action.prefix + action.placeholder + action.suffix)
                    }
                    is MarkdownFormatAction.Insert -> {
                        text?.replace(end, end, action.template)
                    }
                    is MarkdownFormatAction.LinePrefix -> {
                        text?.replace(end, end, "\n" + action.prefix)
                    }
                    is MarkdownFormatAction.Custom -> { /* no-op without cursor */ }
                }
                return
            }

            val selectedText = editor.text?.subSequence(cursor.getLeft(), cursor.getRight())?.toString() ?: ""

            when (action) {
                is MarkdownFormatAction.Wrap -> {
                    if (selectedText.isNotEmpty()) {
                        // Wrap the selected text
                        editor.text?.replace(cursor.getLeft(), cursor.getRight(), action.prefix + selectedText + action.suffix)
                    } else {
                        // Insert template with placeholder
                        val insertText = action.prefix + action.placeholder + action.suffix
                        editor.text?.replace(cursor.getLeft(), cursor.getLeft(), insertText)
                        // Select the placeholder text — clamp to valid text bounds
                        val textLen = editor.text?.length ?: 0
                        val start = (cursor.getLeft() + action.prefix.length).coerceIn(0, textLen)
                        val end = (start + action.placeholder.length).coerceIn(start, textLen)
                        if (start < end) {
                            try {
                                editor.setSelection(start, end)
                            } catch (e: Exception) {
                                Log.w(TAG, "setSelection failed after wrap insert", e)
                            }
                        }
                    }
                }
                is MarkdownFormatAction.Insert -> {
                    editor.text?.replace(cursor.getLeft(), cursor.getLeft(), action.template)
                    // Move cursor to the offset position if specified
                    if (action.cursorOffset > 0) {
                        val textLen = editor.text?.length ?: 0
                        val newPos = (cursor.getLeft() + action.cursorOffset).coerceIn(0, textLen)
                        try {
                            editor.setSelection(newPos, newPos)
                        } catch (e: Exception) {
                            Log.w(TAG, "setSelection failed after insert", e)
                        }
                    }
                }
                is MarkdownFormatAction.LinePrefix -> {
                    // [Opt] Performance: 使用 lastIndexOf 替代手动 while 循环，底层为 native 实现
                    val text = editor.text?.toString() ?: ""
                    val lineStart = text.lastIndexOf('\n', cursor.getLeft() - 1) + 1
                    editor.text?.replace(lineStart, lineStart, action.prefix)
                }
                is MarkdownFormatAction.Custom -> {
                    when (action.action) {
                        "toggle_list_type" -> toggleListType()
                        else -> { /* ignore unknown custom action */ }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "applyFormatAction failed", e)
        }
    }

    /**
     * Get the text content of the current line (the line where the cursor is).
     *
     * @return 当前行的文本，失败时返回空字符串
     */
    @UiThread
    fun getCurrentLineText(): String {
        return try {
            val cursor = editor.cursor ?: return ""
            val text = editor.text?.toString() ?: return ""
            // [Opt] Performance: 使用 indexOf/lastIndexOf 替代手动 while 循环
            val lineStart = text.lastIndexOf('\n', cursor.getLeft() - 1) + 1
            val lineEnd = text.indexOf('\n', cursor.getLeft()).let { if (it == -1) text.length else it }
            text.substring(lineStart, lineEnd)
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLineText failed", e)
            ""
        }
    }

    /**
     * Decrease indentation of the current line by removing [removeCount] leading spaces.
     */
    @UiThread
    fun decreaseIndent(removeCount: Int) {
        try {
            val cursor = editor.cursor ?: return
            val text = editor.text ?: return
            val content = text.toString()
            // [Opt] Performance: 使用 lastIndexOf 替代手动 while 循环
            val lineStart = content.lastIndexOf('\n', cursor.getLeft() - 1) + 1
            val leadingSpaces = content.substring(lineStart).takeWhile { it == ' ' }
            val actualRemove = minOf(removeCount, leadingSpaces.length)
            if (actualRemove > 0) {
                text.delete(lineStart, lineStart + actualRemove)
            }
        } catch (e: Exception) {
            Log.w(TAG, "decreaseIndent failed", e)
        }
    }

    /**
     * Toggle the current line's list type:
     * - Unordered (- ) → Ordered (1. )
     * - Ordered (1. ) → Task (- [ ] )
     * - Task (- [ ] / - [x]) → Unordered (- )
     */
    @UiThread
    fun toggleListType() {
        try {
            val cursor = editor.cursor ?: return
            val text = editor.text?.toString() ?: return
            // [Opt] Performance: 使用 lastIndexOf 替代手动 while 循环
            val lineStart = text.lastIndexOf('\n', cursor.getLeft() - 1) + 1

            val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
            val lineText = text.substring(lineStart, lineEnd)

            val newLine = when {
                // Task list → Unordered
                lineText.trimStart().matches(Regex("[-*+] \\[[ xX]\\] .*")) -> {
                    val indent = lineText.length - lineText.trimStart().length
                    " ".repeat(indent) + "- " + lineText.trimStart().removeRange(0, 6)
                }
                // Ordered list → Task list
                lineText.trimStart().matches(Regex("\\d+\\. .*")) -> {
                    val indent = lineText.length - lineText.trimStart().length
                    " ".repeat(indent) + "- [ ] " + lineText.trimStart().replace(Regex("^\\d+\\. "), "")
                }
                // Unordered list → Ordered list
                lineText.trimStart().matches(Regex("[-*+] .*")) -> {
                    val indent = lineText.length - lineText.trimStart().length
                    " ".repeat(indent) + "1. " + lineText.trimStart().replace(Regex("^[-*+] "), "")
                }
                else -> return // Not a list item
            }

            editor.text?.replace(lineStart, lineEnd, newLine)
        } catch (e: Exception) {
            Log.w(TAG, "toggleListType failed", e)
        }
    }
}
