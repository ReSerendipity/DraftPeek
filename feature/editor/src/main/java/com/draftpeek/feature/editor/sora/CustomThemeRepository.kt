package com.draftpeek.feature.editor.sora

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing custom (user-imported) TextMate themes.
 *
 * Custom themes are stored as JSON files in the app's internal files
 * directory under a "themes" subdirectory. All I/O operations must be
 * called on [Dispatchers.IO].
 */
interface CustomThemeRepository {

    /**
     * Observable stream of all custom theme metadata.
     * Emits a new list whenever a theme is added or removed.
     */
    val customThemes: Flow<List<ThemeMetadata>>

    /**
     * Get the current snapshot of custom themes.
     */
    fun getCustomThemes(): List<ThemeMetadata>

    /**
     * Import a custom theme from a JSON string.
     *
     * The JSON is validated for TextMate theme format (must contain "name"
     * and "type" fields). The file is written to internal storage and
     * the [customThemes] flow is updated.
     *
     * @param json The theme JSON content.
     * @param fileName Optional file name override. If null, the theme's
     *                 "name" field is used to generate the file name.
     * @return The metadata of the imported theme, or null if the JSON
     *         is invalid or writing fails.
     */
    suspend fun importTheme(json: String, fileName: String? = null): ThemeMetadata?

    /**
     * Delete a custom theme by its ID.
     *
     * Removes the JSON file from internal storage and updates the
     * [customThemes] flow. Bundled themes cannot be deleted.
     *
     * @param themeId The theme identifier to delete.
     * @return true if the theme was found and deleted.
     */
    suspend fun deleteTheme(themeId: String): Boolean

    /**
     * Read the JSON content of a custom theme.
     *
     * @param themeId The theme identifier.
     * @return The JSON string, or null if not found.
     */
    suspend fun readThemeJson(themeId: String): String?

    /**
     * Load all custom themes from disk into the reactive flow.
     * Called once at startup to discover existing custom themes.
     */
    suspend fun refreshCustomThemes()
}
