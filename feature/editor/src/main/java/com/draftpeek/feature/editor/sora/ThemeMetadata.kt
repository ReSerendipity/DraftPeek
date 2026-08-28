package com.draftpeek.feature.editor.sora

/**
 * Metadata for an editor theme, whether bundled (shipped in assets) or
 * custom (imported by the user and stored in internal files directory).
 *
 * @param id Unique identifier. For bundled themes this matches the JSON
 *           file name without extension (e.g. "nord"). For custom themes
 *           it is derived from the imported file name.
 * @param displayName Human-readable name shown in the theme picker.
 * @param isDark Whether this is a dark theme.
 * @param isBundled true if this theme ships with the APK (assets/textmate/).
 *                  false if it was imported by the user.
 * @param filePath Source path for the theme JSON. For bundled themes this is
 *                 the asset path (e.g. "textmate/nord.json"). For custom themes
 *                 this is the absolute file path in internal storage
 *                 (e.g. "/data/.../files/themes/my-theme.json").
 */
data class ThemeMetadata(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val isBundled: Boolean,
    val filePath: String
)
