package com.draftpeek.feature.editor.sora

import android.util.Log
import com.draftpeek.core.ui.theme.ThemeLoader
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONObject

/**
 * Applies a loaded [ThemeLoader.EditorTheme] to a sora-editor [CodeEditor].
 *
 * This extension lives in feature/editor because core/ui cannot depend
 * on sora-editor's CodeEditor type.
 */
object ThemeLoaderExt {

    private const val TAG = "ThemeLoaderExt"

    /**
     * Apply an [ThemeLoader.EditorTheme] to the given [CodeEditor].
     *
     * Registers the theme with sora-editor's ThemeRegistry (if not already
     * registered) and applies it as the current color scheme.
     *
     * If a [tokenColorCache] is provided, it is cleared before applying the
     * new theme so that stale resolved colors are not reused.
     *
     * Must be called on the UI thread.
     */
    @androidx.annotation.UiThread
    fun applyTheme(editor: CodeEditor, theme: ThemeLoader.EditorTheme, tokenColorCache: TokenColorCache? = null) {
        try {
            val themeName = theme.name.lowercase().replace(' ', '-')
            val json = ThemeLoader.toJson(theme)

            // Ch10 Item 16 (P2): Invalidate token color cache when theme changes
            tokenColorCache?.clear()

            // Register with ThemeRegistry if not already present
            try {
                ThemeRegistry.getInstance().setTheme(themeName)
            } catch (_: Exception) {
                // Theme not registered yet, register it
                val source = IThemeSource.fromInputStream(json.byteInputStream(), "$themeName.json", null)
                val model = ThemeModel(source, themeName).apply { isDark = theme.isDark }
                ThemeRegistry.getInstance().loadTheme(model)
            }

            ThemeRegistry.getInstance().setTheme(themeName)
            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            Log.d(TAG, "Applied custom theme: $themeName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply theme: ${theme.name}", e)
        }
    }
}
