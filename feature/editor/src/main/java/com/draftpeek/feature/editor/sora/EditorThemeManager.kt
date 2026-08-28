package com.draftpeek.feature.editor.sora

import android.content.Context
import android.util.Log
import com.draftpeek.core.ui.theme.ThemeLoader
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * Manages available editor themes and theme selection.
 *
 * Provides a centralized registry of all bundled TextMate themes
 * and custom (user-imported) themes. Supports runtime theme switching.
 * Inspired by Acode's theme management approach but adapted for
 * sora-editor's ThemeRegistry.
 *
 * Custom themes are discovered from [CustomThemeRepository] and merged
 * with bundled themes. The combined list is exposed as a reactive
 * [StateFlow] via [allThemes].
 */
object EditorThemeManager {

    private const val TAG = "EditorThemeManager"

    /** Subdirectory name under app's internal filesDir for custom themes. */
    const val THEMES_DIR = "themes"

    /** Set of bundled theme IDs that custom themes must not collide with. */
    private val BUNDLED_THEME_IDS = setOf(
        "draftpeek-dark", "draftpeek-light", "nord", "monokai", "dracula",
        "solarized_dark", "solarized_light", "ayu-dark", "darcula",
        "quietlight", "tokyo-night"
    )

    /**
     * Represents a bundled editor theme with metadata.
     *
     * Kept for backward compatibility with existing code that
     * references [ThemeEntry]. New code should prefer [ThemeMetadata].
     *
     * @param id Unique identifier (matches the JSON file name without extension).
     * @param displayName Human-readable name shown in the theme picker.
     * @param isDark Whether this is a dark theme.
     * @param assetPath Path to the JSON file in assets/textmate/.
     */
    data class ThemeEntry(val id: String, val displayName: String, val isDark: Boolean, val assetPath: String)

    /** All bundled themes available in the application. */
    val bundledThemes: List<ThemeEntry> = listOf(
        ThemeEntry("draftpeek-dark", "DraftPeek Dark", true, "textmate/draftpeek-dark.json"),
        ThemeEntry("draftpeek-light", "DraftPeek Light", false, "textmate/draftpeek-light.json"),
        ThemeEntry("nord", "Nord", true, "textmate/nord.json"),
        ThemeEntry("monokai", "Monokai", true, "textmate/monokai.json"),
        ThemeEntry("dracula", "Dracula", true, "textmate/dracula.json"),
        ThemeEntry("solarized_dark", "Solarized Dark", true, "textmate/solarized_dark.json"),
        ThemeEntry("solarized_light", "Solarized Light", false, "textmate/solarized_light.json"),
        ThemeEntry("ayu-dark", "Ayu Dark", true, "textmate/ayu-dark.json"),
        ThemeEntry("darcula", "Darcula", true, "textmate/darcula.json"),
        ThemeEntry("quietlight", "Quiet Light", false, "textmate/quietlight.json"),
        ThemeEntry("tokyo-night", "Tokyo Night", true, "textmate/tokyo-night.json")
    )

    /** Bundled themes as [ThemeMetadata] for the unified list. */
    private val bundledMetadata: List<ThemeMetadata> = bundledThemes.map { entry ->
        ThemeMetadata(
            id = entry.id,
            displayName = entry.displayName,
            isDark = entry.isDark,
            isBundled = true,
            filePath = entry.assetPath
        )
    }

    // ── Reactive combined theme list ──

    private val _allThemes = MutableStateFlow<List<ThemeMetadata>>(bundledMetadata)

    /**
     * Observable stream of all themes (bundled + custom), sorted by
     * display name. Emits a new list whenever custom themes change.
     */
    val allThemes: Flow<List<ThemeMetadata>> = _allThemes.asStateFlow()

    /**
     * Current snapshot of all themes (bundled + custom).
     */
    fun getAllThemes(): List<ThemeMetadata> = _allThemes.value

    /** Dark themes only (snapshot). */
    val darkThemes: List<ThemeEntry>
        get() = bundledThemes.filter { it.isDark }

    /** Light themes only (snapshot). */
    val lightThemes: List<ThemeEntry>
        get() = bundledThemes.filter { !it.isDark }

    /**
     * Get the default dark theme entry.
     */
    fun getDefaultDarkTheme(): ThemeEntry = bundledThemes.first { it.id == "draftpeek-dark" }

    /**
     * Get the default light theme entry.
     */
    fun getDefaultLightTheme(): ThemeEntry = bundledThemes.first { it.id == "draftpeek-light" }

    /**
     * Find a theme entry by its ID.
     *
     * Searches bundled themes first, then custom themes.
     *
     * @param themeId The unique theme identifier.
     * @return The matching [ThemeEntry] for bundled themes, or null.
     */
    fun findThemeById(themeId: String): ThemeEntry? = bundledThemes.find { it.id == themeId }

    /**
     * Find theme metadata by ID across both bundled and custom themes.
     *
     * @param themeId The unique theme identifier.
     * @return The matching [ThemeMetadata], or null if not found.
     */
    fun findThemeMetadataById(themeId: String): ThemeMetadata? = _allThemes.value.find { it.id == themeId }

    // ── Custom theme management ──

    /**
     * Update the custom themes list from the repository.
     * Called by the initialization code after custom themes are discovered.
     *
     * @param customThemes List of custom theme metadata from the repository.
     */
    fun setCustomThemes(customThemes: List<ThemeMetadata>) {
        val combined = (bundledMetadata + customThemes).sortedBy { it.displayName }
        _allThemes.value = combined
        Log.d(TAG, "Theme list updated: ${bundledMetadata.size} bundled + ${customThemes.size} custom")
    }

    /**
     * Load a bundled theme into the sora-editor ThemeRegistry.
     *
     * This loads the JSON from assets and registers it with the
     * ThemeRegistry so it can be applied to any editor instance.
     *
     * @param context Application context for asset access.
     * @param entry The theme entry to load.
     * @return true if the theme was loaded successfully.
     */
    fun loadBundledTheme(context: Context, entry: ThemeEntry): Boolean = try {
        context.assets.open(entry.assetPath).use { input ->
            val source = IThemeSource.fromInputStream(input, entry.assetPath, null)
            val model = ThemeModel(source, entry.id).apply { isDark = entry.isDark }
            ThemeRegistry.getInstance().loadTheme(model)
            Log.d(TAG, "Loaded bundled theme: ${entry.displayName} (${entry.id})")
            true
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bundled theme: ${entry.displayName}", e)
        false
    }

    /**
     * Load a custom theme from a file into the sora-editor ThemeRegistry.
     *
     * This reads the JSON from the file path and registers it with
     * the ThemeRegistry so it can be applied to any editor instance.
     *
     * @param metadata The custom theme metadata (must have isBundled=false).
     * @return true if the theme was loaded successfully.
     */
    fun loadCustomTheme(metadata: ThemeMetadata): Boolean {
        if (metadata.isBundled) {
            Log.w(TAG, "loadCustomTheme called with bundled theme: ${metadata.id}")
            return false
        }
        return try {
            val file = File(metadata.filePath)
            file.inputStream().use { input ->
                val source = IThemeSource.fromInputStream(input, metadata.filePath, null)
                val model = ThemeModel(source, metadata.id).apply { isDark = metadata.isDark }
                ThemeRegistry.getInstance().loadTheme(model)
                Log.d(TAG, "Loaded custom theme: ${metadata.displayName} (${metadata.id})")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom theme: ${metadata.displayName}", e)
            false
        }
    }

    /**
     * Load all bundled themes into the ThemeRegistry.
     *
     * @param context Application context for asset access.
     * @return Number of themes loaded successfully.
     */
    fun loadAllBundledThemes(context: Context): Int {
        var loaded = 0
        for (entry in bundledThemes) {
            if (loadBundledTheme(context, entry)) {
                loaded++
            }
        }
        Log.d(TAG, "Loaded $loaded/${bundledThemes.size} bundled themes")
        return loaded
    }

    /**
     * Load all custom themes into the ThemeRegistry.
     *
     * @param customThemes List of custom theme metadata to load.
     * @return Number of themes loaded successfully.
     */
    fun loadAllCustomThemes(customThemes: List<ThemeMetadata>): Int {
        var loaded = 0
        for (metadata in customThemes) {
            if (loadCustomTheme(metadata)) {
                loaded++
            }
        }
        Log.d(TAG, "Loaded $loaded/${customThemes.size} custom themes")
        return loaded
    }

    /**
     * Discover custom themes from the app's internal "themes/" directory,
     * load them into the ThemeRegistry, and update the reactive theme list.
     *
     * This is the primary entry point for custom theme initialization.
     * It scans the `themes/` subdirectory in the app's internal files
     * directory for `.json` files, parses their metadata, loads them
     * into sora-editor's ThemeRegistry, and merges them with the
     * bundled themes in [allThemes].
     *
     * Must be called on a background thread (Dispatchers.IO) as it
     * performs file I/O.
     *
     * @param context Application context for file access.
     * @return Number of custom themes loaded successfully.
     */
    fun discoverAndLoadCustomThemes(context: Context): Int {
        val themesDir = File(context.filesDir, THEMES_DIR)
        if (!themesDir.exists()) {
            themesDir.mkdirs()
            Log.d(TAG, "Created themes directory: ${themesDir.absolutePath}")
            // No custom themes yet; just ensure the combined list is published
            setCustomThemes(emptyList())
            return 0
        }

        val customMetadata = mutableListOf<ThemeMetadata>()
        val jsonFiles = themesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json", ignoreCase = true)
        }

        if (jsonFiles != null) {
            for (file in jsonFiles) {
                try {
                    val json = file.readText(Charsets.UTF_8)
                    val parsed = ThemeLoader.loadFromJson(json)
                    if (parsed != null) {
                        val themeId = parsed.name.lowercase()
                            .replace(' ', '-')
                            .replace(Regex("[^a-z0-9_-]"), "")
                        val finalId = if (themeId in BUNDLED_THEME_IDS) "custom-$themeId" else themeId
                        val metadata = ThemeMetadata(
                            id = finalId,
                            displayName = parsed.name,
                            isDark = parsed.isDark,
                            isBundled = false,
                            filePath = file.absolutePath
                        )
                        customMetadata.add(metadata)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse custom theme: ${file.name}", e)
                }
            }
        }

        // Update the reactive list
        setCustomThemes(customMetadata)

        // Load into ThemeRegistry
        return loadAllCustomThemes(customMetadata)
    }

    /**
     * Apply a theme to the ThemeRegistry and set it as active.
     *
     * Works for both bundled and custom themes as long as they
     * have been loaded into the ThemeRegistry.
     *
     * @param themeId The ID of the theme to apply.
     * @return true if the theme was found and applied.
     */
    fun applyTheme(themeId: String): Boolean = try {
        ThemeRegistry.getInstance().setTheme(themeId)
        Log.d(TAG, "Applied theme: $themeId")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to apply theme: $themeId", e)
        false
    }

    /**
     * Get the display name for a theme ID.
     *
     * Searches both bundled and custom themes.
     *
     * @param themeId The theme identifier.
     * @return The display name, or the raw ID if not found.
     */
    fun getThemeDisplayName(themeId: String): String = findThemeMetadataById(themeId)?.displayName ?: themeId

    /**
     * Check if a theme ID refers to a dark theme.
     *
     * Searches both bundled and custom themes.
     *
     * @param themeId The theme identifier.
     * @return true if the theme is dark, false if light or unknown.
     */
    fun isDarkTheme(themeId: String): Boolean = findThemeMetadataById(themeId)?.isDark ?: true
}
