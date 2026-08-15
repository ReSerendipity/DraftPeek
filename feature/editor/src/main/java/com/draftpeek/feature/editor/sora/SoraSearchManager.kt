package com.draftpeek.feature.editor.sora

import android.util.Log
import androidx.annotation.UiThread
import com.draftpeek.core.common.util.InputValidator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher

/**
 * Manages all search-related operations for the sora-editor.
 *
 * Extracted from [SoraEditorWrapper] to reduce file size and improve modularity.
 * Handles text search, replace, word highlighting, and cursor-based word retrieval.
 *
 * @param editorProvider Returns the current [CodeEditor] instance, or null if not available.
 * @param isReleased Returns true if the wrapper has been released (resources freed).
 */
internal class SoraSearchManager(
    private val editorProvider: () -> CodeEditor?,
    private val isReleased: () -> Boolean,
) {

    /**
     * Search for [query] in the editor.
     *
     * @param query Search text
     * @param regex Whether to treat the query as a regular expression
     * @param matchCase Whether to match case-sensitively (true = case-sensitive)
     * @param wholeWord Whether to match only whole words
     * @return true if the search was successfully started
     */
    @UiThread
    fun search(query: String, regex: Boolean = false, matchCase: Boolean = false, wholeWord: Boolean = false): Boolean {
        return try {
            if (query.isBlank()) return false
            val editor = editorProvider() ?: return false
            val options = buildSearchOptions(regex, matchCase, wholeWord)
            val actualQuery = buildSearchQuery(query, regex, wholeWord)
            editor.searcher.search(actualQuery, options)
            true
        } catch (e: Exception) {
            Log.w(TAG, "search failed", e)
            false
        }
    }

    /** Replace the current search match. Must be called on the main thread. */
    @UiThread
    fun replaceCurrent(replacement: String) {
        try {
            val editor = editorProvider() ?: return
            editor.searcher.replaceCurrentMatch(replacement)
        } catch (e: Exception) {
            Log.w(TAG, "replaceCurrent failed", e)
        }
    }

    /** Replace all search matches. Must be called on the main thread. */
    @UiThread
    fun replaceAll(query: String, replacement: String, regex: Boolean = false, matchCase: Boolean = false, wholeWord: Boolean = false) {
        try {
            if (query.isBlank()) return
            val editor = editorProvider() ?: return
            val options = buildSearchOptions(regex, matchCase, wholeWord)
            val actualQuery = buildSearchQuery(query, regex, wholeWord)
            editor.searcher.search(actualQuery, options)
            editor.searcher.replaceAll(replacement)
        } catch (e: Exception) {
            Log.w(TAG, "replaceAll failed", e)
        }
    }

    /** Stop the current search. Must be called on the main thread. */
    @UiThread
    fun stopSearch() {
        try {
            val editor = editorProvider() ?: return
            editor.searcher.stopSearch()
        } catch (e: Exception) {
            Log.w(TAG, "stopSearch failed", e)
        }
    }

    /**
     * Jump to the next search match.
     * @return true if successfully moved to next match, false if no matches or at end
     */
    @UiThread
    fun gotoNext(): Boolean {
        return try {
            val editor = editorProvider() ?: return false
            editor.searcher.gotoNext()
        } catch (e: Exception) {
            Log.w(TAG, "gotoNext failed", e)
            false
        }
    }

    /**
     * Jump to the previous search match.
     * @return true if successfully moved to previous match, false if no matches or at start
     */
    @UiThread
    fun gotoPrevious(): Boolean {
        return try {
            val editor = editorProvider() ?: return false
            editor.searcher.gotoPrevious()
        } catch (e: Exception) {
            Log.w(TAG, "gotoPrevious failed", e)
            false
        }
    }

    /**
     * Highlight all occurrences of [word] in the editor.
     *
     * Uses the sora-editor searcher API (the same engine used for Find) to
     * search for the word with case-insensitive, whole-word matching.
     * The matched ranges are rendered as highlighted backgrounds.
     *
     * Must be called on the UI thread.
     */
    @UiThread
    fun highlightWord(word: String) {
        if (word.isBlank()) return
        try {
            val editor = editorProvider() ?: return
            val options = EditorSearcher.SearchOptions(
                EditorSearcher.SearchOptions.TYPE_WHOLE_WORD,
                true, // caseInsensitive
            )
            val escapedQuery = InputValidator.escapeRegexSpecialChars(word)
            editor.searcher.search(escapedQuery, options)
        } catch (e: Exception) {
            Log.w(TAG, "highlightWord failed", e)
        }
    }

    /**
     * Clear the word highlight created by [highlightWord].
     * Must be called on the UI thread.
     */
    @UiThread
    fun clearWordHighlight() {
        try {
            val editor = editorProvider() ?: return
            editor.searcher.stopSearch()
        } catch (e: Exception) {
            Log.w(TAG, "clearWordHighlight failed", e)
        }
    }

    /**
     * Get the word at the current cursor position.
     *
     * Returns null if the cursor is not on a word character.
     * Must be called on the UI thread.
     */
    @UiThread
    fun getWordAtCursor(): String? {
        return try {
            val editor = editorProvider() ?: return null
            val cursor = editor.cursor ?: return null
            val line = cursor.leftLine
            val column = cursor.leftColumn
            val text = editor.text ?: return null
            val lineText = text.getLine(line).toString()
            if (column < 0 || column > lineText.length) return null

            // Find word boundaries around the cursor position
            val isWordChar: (Char) -> Boolean = { it.isLetterOrDigit() || it == '_' }
            var start = column.coerceAtMost(lineText.length)
            var end = column.coerceAtMost(lineText.length)

            // If cursor is on a non-word character, return null
            if (start < lineText.length && !isWordChar(lineText[start])) {
                // Try one position to the left (cursor may be right after a word)
                if (start > 0 && isWordChar(lineText[start - 1])) {
                    start--
                    end = start + 1
                } else {
                    return null
                }
            }

            // Expand left
            while (start > 0 && isWordChar(lineText[start - 1])) start--
            // Expand right
            while (end < lineText.length && isWordChar(lineText[end])) end++

            if (start >= end) return null
            lineText.substring(start, end)
        } catch (e: Exception) {
            Log.w(TAG, "getWordAtCursor failed", e)
            null
        }
    }

    /**
     * Build [EditorSearcher.SearchOptions] from search toggle states.
     *
     * Priority: regex > wholeWord > normal.
     * When regex and wholeWord are both enabled, regex takes precedence and the caller
     * is responsible for wrapping the pattern with `\b` word boundaries in [buildSearchQuery].
     */
    private fun buildSearchOptions(regex: Boolean, matchCase: Boolean, wholeWord: Boolean): EditorSearcher.SearchOptions {
        val caseInsensitive = !matchCase
        val type = when {
            regex -> EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION
            wholeWord -> EditorSearcher.SearchOptions.TYPE_WHOLE_WORD
            else -> EditorSearcher.SearchOptions.TYPE_NORMAL
        }
        return EditorSearcher.SearchOptions(type, caseInsensitive)
    }

    /**
     * Build the actual search query string from user input and toggle states.
     *
     * - Non-regex, non-whole-word: escape regex special chars for literal matching
     * - Non-regex, whole-word: escape regex special chars (sora-editor wraps with `\b` + `Pattern.quote`)
     * - Regex, non-whole-word: use raw query as regex
     * - Regex, whole-word: wrap the raw regex with `\b` word boundaries
     */
    private fun buildSearchQuery(query: String, regex: Boolean, wholeWord: Boolean): String {
        return when {
            regex && wholeWord -> "\\b(?:$query)\\b"
            regex -> query
            else -> InputValidator.escapeRegexSpecialChars(query)
        }
    }

    companion object {
        private const val TAG = "SoraSearchManager"
    }
}
