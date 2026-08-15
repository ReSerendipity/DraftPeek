package com.draftpeek.feature.editor.sora

import com.draftpeek.core.ui.theme.ThemeLoader
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converter for importing VS Code theme files into DraftPeek's internal format.
 *
 * VS Code themes use a specific JSON format with:
 * - `colors`: UI-level color mappings (e.g., "editor.background", "editorLineNumber.foreground")
 * - `tokenColors`: Syntax highlighting rules with TextMate scopes
 * - `semanticTokenColors`: Semantic token overrides (LSP-provided highlighting)
 *
 * This converter maps VS Code color keys to DraftPeek's internal color slots,
 * ensuring that imported themes look correct in the sora-editor-based editor.
 *
 * Supported VS Code theme sources:
 * - Direct `.json` files (standard VS Code theme format)
 * - VS Code extension `.vsix` packages (requires extracting the theme JSON)
 * - Marketplace themes (requires downloading and extracting)
 */
object VsCodeThemeImporter {

    /**
     * Import a VS Code theme JSON string and convert it to DraftPeek format.
     *
     * @param vscodeJson The raw VS Code theme JSON string
     * @return DraftPeek-compatible theme JSON string, or null if parsing fails
     */
    fun importFromVsCodeTheme(vscodeJson: String): String? {
        val parsed = ThemeLoader.loadFromJson(vscodeJson) ?: return null
        val root = try { JSONObject(vscodeJson) } catch (_: Exception) { return null }

        // Convert VS Code color keys to DraftPeek color slots
        val convertedColors = convertVsCodeColors(root.optJSONObject("colors"))

        // Token colors are already TextMate-compatible — just pass through
        val tokenColors = root.optJSONArray("tokenColors") ?: JSONArray()

        // Build the DraftPeek theme JSON
        val draftpeekTheme = JSONObject().apply {
            put("name", "${parsed.name} (VS Code)")
            put("type", if (parsed.isDark) "dark" else "light")
            put("metadata", JSONObject().apply {
                put("version", 1)
                put("source", "vscode-import")
                put("originalName", parsed.name)
            })
            put("colors", convertedColors)
            put("tokenColors", tokenColors)
        }

        return draftpeekTheme.toString(2)
    }

    /**
     * Convert VS Code's `colors` object to DraftPeek's internal color slots.
     *
     * VS Code uses keys like "editor.background" while DraftPeek uses
     * a specific set of color slot names. This mapping ensures that
     * the most important UI colors are preserved during import.
     */
    private fun convertVsCodeColors(vscodeColors: JSONObject?): JSONObject {
        val result = JSONObject()
        if (vscodeColors == null) return result

        // Map VS Code color keys to DraftPeek color slots
        // Priority: exact matches first, then close approximations
        val colorMappings = mapOf(
            // Editor core colors
            "editor.background" to "editor.background",
            "editor.foreground" to "editor.foreground",
            "editor.lineHighlightBackground" to "editor.lineHighlightBackground",
            "editor.selectionBackground" to "editor.selectionBackground",
            "editor.selectionForeground" to "editor.selectionForeground",
            "editor.inactiveSelectionBackground" to "editor.inactiveSelectionBackground",
            "editorCursor.foreground" to "editorCursor.foreground",
            "editorWhitespace.foreground" to "editorWhitespace.foreground",
            "editorIndentGuide.background" to "editorIndentGuide.background",
            "editorIndentGuide.activeBackground" to "editorIndentGuide.activeBackground",
            "editorLineNumber.foreground" to "editorLineNumber.foreground",
            "editorLineNumber.activeForeground" to "editorLineNumber.activeForeground",
            "editor.findMatchBackground" to "editor.findMatchBackground",
            "editor.findMatchHighlightBackground" to "editor.findMatchHighlightBackground",
            "editor.findRangeHighlightBackground" to "editor.findRangeHighlightBackground",
            "editor.wordHighlightBackground" to "editor.wordHighlightBackground",
            "editor.wordHighlightStrongBackground" to "editor.wordHighlightStrongBackground",

            // Sidebar and UI colors
            "sideBar.background" to "sideBar.background",
            "sideBar.foreground" to "sideBar.foreground",
            "sideBarTitle.foreground" to "sideBarTitle.foreground",
            "activityBar.background" to "activityBar.background",
            "activityBar.foreground" to "activityBar.foreground",
            "statusBar.background" to "statusBar.background",
            "statusBar.foreground" to "statusBar.foreground",
            "titleBar.activeBackground" to "titleBar.activeBackground",
            "titleBar.activeForeground" to "titleBar.activeForeground",

            // Input and widget colors
            "input.background" to "input.background",
            "input.foreground" to "input.foreground",
            "input.border" to "input.border",
            "dropdown.background" to "dropdown.background",
            "dropdown.foreground" to "dropdown.foreground",
            "dropdown.border" to "dropdown.border",

            // Button colors
            "button.background" to "button.background",
            "button.foreground" to "button.foreground",
            "button.hoverBackground" to "button.hoverBackground",

            // List and tree colors
            "list.activeSelectionBackground" to "list.activeSelectionBackground",
            "list.activeSelectionForeground" to "list.activeSelectionForeground",
            "list.hoverBackground" to "list.hoverBackground",
            "list.highlightForeground" to "list.highlightForeground",

            // Tab colors
            "tab.activeBackground" to "tab.activeBackground",
            "tab.activeForeground" to "tab.activeForeground",
            "tab.inactiveBackground" to "tab.inactiveBackground",
            "tab.inactiveForeground" to "tab.inactiveForeground",
            "tab.border" to "tab.border",

            // Minimap colors
            "minimap.background" to "minimap.background",
            "minimap.selectionHighlight" to "minimap.selectionHighlight",

            // Bracket pair colors
            "editorBracketMatch.background" to "editorBracketMatch.background",
            "editorBracketMatch.border" to "editorBracketMatch.border",

            // Gutter colors
            "editorGutter.background" to "editorGutter.background",
            "editorGutter.modifiedBackground" to "editorGutter.modifiedBackground",
            "editorGutter.addedBackground" to "editorGutter.addedBackground",
            "editorGutter.deletedBackground" to "editorGutter.deletedBackground",
        )

        // Apply direct mappings
        for ((vscodeKey, draftpeekKey) in colorMappings) {
            val color = vscodeColors.optString(vscodeKey, null as String?)
            if (color != null) {
                result.put(draftpeekKey, color)
            }
        }

        // Also preserve any VS Code colors that don't have a direct mapping
        // (these may be used by custom editor components)
        val keys = vscodeColors.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!result.has(key)) {
                result.put(key, vscodeColors.getString(key))
            }
        }

        return result
    }

    /**
     * Validate that a JSON string looks like a valid VS Code theme.
     *
     * @param json The JSON string to validate
     * @return ValidationResult with either success or specific error messages
     */
    fun validateVsCodeTheme(json: String): ValidationResult {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            return ValidationResult.Error("Invalid JSON: ${e.message}")
        }

        val name = root.optString("name", "")
        if (name.isBlank()) {
            return ValidationResult.Error("Missing required field: 'name'")
        }

        val type = root.optString("type", "")
        if (type !in listOf("light", "dark")) {
            return ValidationResult.Error("Invalid or missing 'type' field (must be 'light' or 'dark')")
        }

        // Check for at least some theme content
        val hasColors = root.has("colors")
        val hasTokenColors = root.has("tokenColors")
        if (!hasColors && !hasTokenColors) {
            return ValidationResult.Error("Theme must have 'colors' and/or 'tokenColors'")
        }

        return ValidationResult.Valid(name, type == "dark")
    }

    sealed class ValidationResult {
        data class Valid(val name: String, val isDark: Boolean) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
