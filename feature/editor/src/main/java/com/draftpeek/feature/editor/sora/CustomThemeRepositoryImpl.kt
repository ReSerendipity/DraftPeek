package com.draftpeek.feature.editor.sora

import android.content.Context
import android.util.Log
import com.draftpeek.core.ui.theme.ThemeLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CustomThemeRepository] that stores custom themes
 * as JSON files in the app's internal files directory under "themes/".
 *
 * All file I/O is performed on [Dispatchers.IO].
 */
@Singleton
class CustomThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : CustomThemeRepository {

    companion object {
        private const val TAG = "CustomThemeRepo"
        private const val THEMES_DIR = "themes"
    }

    private val _customThemes = MutableStateFlow<List<ThemeMetadata>>(emptyList())
    override val customThemes: Flow<List<ThemeMetadata>> = _customThemes.asStateFlow()

    override fun getCustomThemes(): List<ThemeMetadata> = _customThemes.value

    override suspend fun refreshCustomThemes() = withContext(Dispatchers.IO) {
        val themesDir = getThemesDir()
        val themes = mutableListOf<ThemeMetadata>()

        val jsonFiles = themesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        }

        if (jsonFiles != null) {
            for (file in jsonFiles) {
                val metadata = parseThemeMetadata(file)
                if (metadata != null) {
                    themes.add(metadata)
                }
            }
        }

        _customThemes.value = themes.sortedBy { it.displayName }
        Log.d(TAG, "Refreshed custom themes: ${themes.size} found")
        Unit
    }

    override suspend fun importTheme(json: String, fileName: String?): ThemeMetadata? =
        withContext(Dispatchers.IO) {
            // Try VS Code theme import first if it looks like a VS Code theme
            val themeJson = if (isVsCodeTheme(json)) {
                val converted = VsCodeThemeImporter.importFromVsCodeTheme(json)
                converted ?: json // Fall back to original if conversion fails
            } else {
                json
            }

            // Validate JSON is a valid TextMate theme
            val parsed = ThemeLoader.loadFromJson(themeJson)
            if (parsed == null) {
                Log.e(TAG, "Invalid TextMate theme JSON")
                return@withContext null
            }

            // Generate theme ID from name
            val themeId = parsed.name.lowercase().replace(' ', '-').replaceRegex()
            val safeFileName = (fileName ?: "$themeId.json").ensureJsonExtension()
            val themesDir = getThemesDir()
            val file = File(themesDir, safeFileName)

            // Avoid overwriting bundled theme IDs by prefixing with "custom-"
            val finalId = if (isBundledThemeId(themeId)) "custom-$themeId" else themeId

            try {
                file.writeText(themeJson, Charsets.UTF_8)
                val metadata = ThemeMetadata(
                    id = finalId,
                    displayName = parsed.name,
                    isDark = parsed.isDark,
                    isBundled = false,
                    filePath = file.absolutePath,
                )
                // Update the reactive list
                val updated = (_customThemes.value.filter { it.id != finalId } + metadata)
                    .sortedBy { it.displayName }
                _customThemes.value = updated
                Log.d(TAG, "Imported custom theme: ${parsed.name} ($finalId)")
                metadata
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write custom theme file", e)
                null
            }
        }

    override suspend fun deleteTheme(themeId: String): Boolean =
        withContext(Dispatchers.IO) {
            val themes = _customThemes.value
            val target = themes.find { it.id == themeId }
            if (target == null || target.isBundled) {
                Log.w(TAG, "Cannot delete theme: $themeId (not found or bundled)")
                return@withContext false
            }

            val file = File(target.filePath)
            val deleted = file.delete()
            if (deleted) {
                _customThemes.value = themes.filter { it.id != themeId }
                Log.d(TAG, "Deleted custom theme: $themeId")
            } else {
                Log.e(TAG, "Failed to delete theme file: ${target.filePath}")
            }
            deleted
        }

    override suspend fun readThemeJson(themeId: String): String? =
        withContext(Dispatchers.IO) {
            val target = _customThemes.value.find { it.id == themeId }
            if (target == null) return@withContext null

            try {
                File(target.filePath).readText(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read theme JSON: $themeId", e)
                null
            }
        }

    // ── Internal helpers ──

    private fun getThemesDir(): File {
        val dir = File(context.filesDir, THEMES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Parse a theme JSON file and extract metadata without fully
     * parsing token colors (cheaper than ThemeLoader.loadFromJson
     * for discovery purposes, but we reuse it for consistency).
     */
    private fun parseThemeMetadata(file: File): ThemeMetadata? {
        return try {
            val json = file.readText(Charsets.UTF_8)
            val parsed = ThemeLoader.loadFromJson(json) ?: return null
            val themeId = parsed.name.lowercase().replace(' ', '-').replaceRegex()
            val finalId = if (isBundledThemeId(themeId)) "custom-$themeId" else themeId
            ThemeMetadata(
                id = finalId,
                displayName = parsed.name,
                isDark = parsed.isDark,
                isBundled = false,
                filePath = file.absolutePath,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse theme metadata from ${file.name}", e)
            null
        }
    }

    /**
     * Check if a theme ID conflicts with a bundled theme.
     */
    private fun isBundledThemeId(id: String): Boolean {
        return id in BUNDLED_THEME_IDS
    }

    /**
     * Heuristic check: does this JSON look like a VS Code theme?
     *
     * VS Code themes typically have specific keys like "contributes"
     * or use VS Code-specific color keys like "editor.background".
     */
    private fun isVsCodeTheme(json: String): Boolean {
        return try {
            val root = org.json.JSONObject(json)
            // If it has semanticTokenColors or contributes, it's definitely VS Code
            root.has("semanticTokenColors") ||
                root.has("contributes") ||
                // Check for VS Code-specific color keys
                run {
                    val colors = root.optJSONObject("colors") ?: return@run false
                    colors.has("editor.background") &&
                        (colors.has("editorCursor.foreground") || colors.has("editor.selectionBackground"))
                }
        } catch (_: Exception) {
            false
        }
    }

    private fun String.replaceRegex(): String =
        replace(Regex("[^a-z0-9_-]"), "")

    private fun String.ensureJsonExtension(): String =
        if (endsWith(".json", ignoreCase = true)) this else "$this.json"
}

/**
 * Set of bundled theme IDs that custom themes must not collide with.
 */
private val BUNDLED_THEME_IDS = setOf(
    "draftpeek-dark", "draftpeek-light", "nord", "monokai", "dracula",
    "solarized_dark", "solarized_light", "ayu-dark", "darcula",
    "quietlight", "tokyo-night",
)
