package com.draftpeek.feature.editor.sora

import android.content.Context
import android.util.Log
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeGitHub
import com.draftpeek.core.common.util.LanguageConfig
import com.draftpeek.feature.editor.treesitter.TreeSitterLanguageProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * Manages TextMate grammar/theme initialization, color scheme application,
 * and language selection (TreeSitter / TextMate) for the sora-editor.
 *
 * Extracted from [SoraEditorWrapper] to reduce file size and improve modularity.
 * This class encapsulates all syntax highlighting and theme-related state.
 *
 * @param appContext The application context (for asset access).
 * @param editorProvider Returns the current [CodeEditor] instance, or null if not available.
 * @param isReleased Returns true if the wrapper has been released (resources freed).
 * @param initScope Coroutine scope for background initialization.
 */
internal class SoraThemeManager(
    private val appContext: Context,
    private val editorProvider: () -> CodeEditor?,
    private val isReleased: () -> Boolean,
    private val initScope: CoroutineScope,
) {
    @Volatile
    var textMateInitialized = false
        private set

    @Volatile
    var textMateInitFailed = false
        private set

    @Volatile
    private var textMateThemeLoaded = false

    @Volatile
    var currentDarkTheme = false
        private set

    @Volatile
    var currentThemeId: String? = null
        private set

    @Volatile
    var currentLanguage: String? = null
        private set

    val initComplete = CompletableDeferred<Unit>()

    /**
     * Initialize TextMate grammar and theme registries on a background thread.
     * Safe to call from [SoraEditorWrapper.init]; uses a global guard to ensure
     * one-time initialization per process.
     */
    suspend fun initialize() {
        if (textMateInitialized) {
            Log.d(TAG, "TextMate already initialized for this wrapper")
            return
        }

        if (textMateInitFailed) {
            Log.d(TAG, "TextMate init previously failed, skipping retry")
            return
        }

        if (isReleased()) {
            Log.d(TAG, "Wrapper already released, skipping TextMate init")
            return
        }

        try {
            synchronized(SoraThemeManager::class.java) {
                if (globalTextMateInitialized) {
                    Log.d(TAG, "TextMate was already initialized by another wrapper instance, skipping")
                    textMateInitialized = true
                    textMateThemeLoaded = true
                    return
                }

                Log.d(TAG, "Initializing TextMate globally (first wrapper instance)...")

                try {
                    FileProviderRegistry.getInstance()
                        .addFileProvider(AssetsFileResolver(appContext.assets))
                    Log.d(TAG, "FileProviderRegistry initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize FileProviderRegistry, syntax highlighting disabled", e)
                    textMateInitFailed = true
                    return
                }

                try {
                    GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                    Log.d(TAG, "All grammars loaded successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "Grammar loading failed, some languages may lack highlighting", e)
                }

                loadTheme("textmate/draftpeek-light.json", "draftpeek-light", false)
                loadTheme("textmate/draftpeek-dark.json", "draftpeek-dark", true)

                try {
                    val customCount = EditorThemeManager.discoverAndLoadCustomThemes(appContext)
                    if (customCount > 0) {
                        Log.d(TAG, "$customCount custom themes loaded from internal storage")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to discover custom themes", e)
                }

                globalTextMateInitialized = true
                textMateInitialized = true
                Log.d(TAG, "Global TextMate initialization complete, themeLoaded=$textMateThemeLoaded")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error during TextMate initialization, falling back to plain text", t)
            textMateInitFailed = true
        }
    }

    private fun loadTheme(path: String, name: String, isDark: Boolean) {
        try {
            appContext.assets.open(path).use { input ->
                val source = IThemeSource.fromInputStream(input, path, null)
                val model = ThemeModel(source, name).apply { this.isDark = isDark }
                ThemeRegistry.getInstance().loadTheme(model)
                textMateThemeLoaded = true
                Log.d(TAG, "Theme loaded: $name from $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme $path", e)
        }
    }

    /**
     * Apply the current color scheme to the editor.
     * Falls back to built-in SchemeDarcula/SchemeGitHub if TextMate themes haven't loaded.
     */
    fun applyColorScheme() {
        val editor = editorProvider() ?: return
        try {
            if (!textMateThemeLoaded) {
                editor.colorScheme = if (currentDarkTheme) SchemeDarcula() else SchemeGitHub()
            } else {
                val themeId = currentThemeId ?: if (currentDarkTheme) "draftpeek-dark" else "draftpeek-light"
                try {
                    ThemeRegistry.getInstance().setTheme(themeId)
                    editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to apply TextMate theme $themeId, falling back to built-in", t)
                    try {
                        editor.colorScheme = if (currentDarkTheme) SchemeDarcula() else SchemeGitHub()
                    } catch (_: Throwable) {}
                }
            }

            try {
                applyM3EditorColors(editor, currentDarkTheme)
            } catch (t: Throwable) {
                Log.w(TAG, "applyM3EditorColors failed", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "applyColorScheme failed entirely", t)
        }
    }

    /**
     * Apply M3-inspired colors to ALL 83 sora-editor color slots.
     *
     * Strategy:
     * - Slots 21–28, 32–34 (syntax highlighting tokens) are NOT overridden here —
     *   they belong to TextMate themes. If a TextMate theme is loaded, its syntax
     *   colors will already be set by [TextMateColorScheme.create]. If TextMate fails
     *   to load, the built-in SchemeDarcula/SchemeGitHub defaults for these slots
     *   are acceptable.
     * - All other slots (1–20, 29–31, 35–83) get explicit M3-matched colors so the
     *   editor always looks consistent, regardless of TextMate loading state.
     */
    private fun applyM3EditorColors(editor: CodeEditor, dark: Boolean) {
        val scheme = editor.colorScheme
        val c = if (dark) soraEditorColorsDark else soraEditorColorsLight

        // ── Base editor (1–15) ──
        scheme.setColor(EditorColorScheme.LINE_DIVIDER, c.lineDivider)
        scheme.setColor(EditorColorScheme.LINE_NUMBER, c.lineNumber)
        scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, c.lineNumberBackground)
        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, c.wholeBackground)
        scheme.setColor(EditorColorScheme.TEXT_NORMAL, c.textNormal)
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, c.selectedTextBackground)
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, c.selectionInsert)
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, c.selectionHandle)
        scheme.setColor(EditorColorScheme.CURRENT_LINE, c.currentLine)
        scheme.setColor(EditorColorScheme.UNDERLINE, c.underline)
        scheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB, c.scrollBarThumb)
        scheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, c.scrollBarThumbPressed)
        scheme.setColor(EditorColorScheme.SCROLL_BAR_TRACK, c.scrollBarTrack)
        scheme.setColor(EditorColorScheme.BLOCK_LINE, c.blockLine)
        scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, c.blockLineCurrent)

        // ── UI chrome (16–20) ──
        scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL, c.lineNumberPanel)
        scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, c.lineNumberPanelText)
        scheme.setColor(EditorColorScheme.LINE_BLOCK_LABEL, c.lineBlockLabel)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, c.completionWndBackground)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_CORNER, c.completionWndCorner)

        // ── Syntax highlighting (21–28, 32–34): INTENTIONALLY SKIPPED ──
        // These are managed by TextMate themes and should NOT be overridden.

        // ── Editor UI chrome (29–31) ──
        scheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, c.matchedTextBackground)
        scheme.setColor(EditorColorScheme.TEXT_SELECTED, c.textSelected)
        scheme.setColor(EditorColorScheme.NON_PRINTABLE_CHAR, c.nonPrintableChar)

        // ── UI chrome (35–45) ──
        scheme.setColor(EditorColorScheme.PROBLEM_ERROR, c.problemError)
        scheme.setColor(EditorColorScheme.PROBLEM_WARNING, c.problemWarning)
        scheme.setColor(EditorColorScheme.PROBLEM_TYPO, c.problemTypo)
        scheme.setColor(EditorColorScheme.SIDE_BLOCK_LINE, c.sideBlockLine)
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, c.highlightedDelimitersForeground)
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, c.highlightedDelimitersUnderline)
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, c.highlightedDelimitersBackground)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, c.completionWndTextPrimary)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, c.completionWndTextSecondary)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT, c.completionWndItemCurrent)
        scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, c.lineNumberCurrent)

        // ── UI chrome (46–67) ──
        scheme.setColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE, c.snippetBackgroundInactive)
        scheme.setColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED, c.snippetBackgroundRelated)
        scheme.setColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING, c.snippetBackgroundEditing)
        scheme.setColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND, c.textInlayHintBackground)
        scheme.setColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND, c.textInlayHintForeground)
        scheme.setColor(EditorColorScheme.HARD_WRAP_MARKER, c.hardWrapMarker)
        scheme.setColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE, c.functionCharBackgroundStroke)
        scheme.setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BACKGROUND, c.diagnosticTooltipBackground)
        scheme.setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_BRIEF_MSG, c.diagnosticTooltipBriefMsg)
        scheme.setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_DETAILED_MSG, c.diagnosticTooltipDetailedMsg)
        scheme.setColor(EditorColorScheme.DIAGNOSTIC_TOOLTIP_ACTION, c.diagnosticTooltipAction)
        scheme.setColor(EditorColorScheme.STRIKETHROUGH, c.strikethrough)
        scheme.setColor(EditorColorScheme.SIGNATURE_TEXT_NORMAL, c.signatureTextNormal)
        scheme.setColor(EditorColorScheme.SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER, c.signatureTextHighlightedParameter)
        scheme.setColor(EditorColorScheme.SIGNATURE_BACKGROUND, c.signatureBackground)
        scheme.setColor(EditorColorScheme.STICKY_SCROLL_DIVIDER, c.stickyScrollDivider)
        scheme.setColor(EditorColorScheme.STATIC_SPAN_BACKGROUND, c.staticSpanBackground)
        scheme.setColor(EditorColorScheme.STATIC_SPAN_FOREGROUND, c.staticSpanForeground)
        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, c.textActionWindowBackground)
        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR, c.textActionWindowIconColor)
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_MATCHED, c.completionWndTextMatched)

        // ── UI chrome (68–83) ──
        scheme.setColor(EditorColorScheme.HOVER_TEXT_NORMAL, c.hoverTextNormal)
        scheme.setColor(EditorColorScheme.HOVER_BACKGROUND, c.hoverBackground)
        scheme.setColor(EditorColorScheme.HOVER_BORDER, c.hoverBorder)
        scheme.setColor(EditorColorScheme.SIGNATURE_BORDER, c.signatureBorder)
        scheme.setColor(EditorColorScheme.HOVER_TEXT_HIGHLIGHTED, c.hoverTextHighlighted)
        scheme.setColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND, c.textHighlightStrongBackground)
        scheme.setColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND, c.textHighlightBackground)
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER, c.highlightedDelimitersBorder)
        scheme.setColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BORDER, c.textHighlightStrongBorder)
        scheme.setColor(EditorColorScheme.TEXT_HIGHLIGHT_BORDER, c.textHighlightBorder)
        scheme.setColor(EditorColorScheme.MATCHED_TEXT_BORDER, c.matchedTextBorder)
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BORDER, c.selectedTextBorder)
        scheme.setColor(EditorColorScheme.CURRENT_ROW_BORDER, c.currentRowBorder)
        scheme.setColor(EditorColorScheme.MINIMAP_BACKGROUND, c.minimapBackground)
        scheme.setColor(EditorColorScheme.MINIMAP_VIEWPORT, c.minimapViewport)
        scheme.setColor(EditorColorScheme.MINIMAP_VIEWPORT_BORDER, c.minimapViewportBorder)

        Log.d(TAG, "M3 editor colors applied (dark=$dark, syntax slots 21-28/32-34 preserved)")
    }

    /**
     * Set the editor theme (dark/light). Clears the token color cache.
     * Must be called on the UI thread.
     */
    fun setTheme(dark: Boolean, tokenColorCache: TokenColorCache) {
        currentDarkTheme = dark
        currentThemeId = null
        tokenColorCache.clear()
        applyColorScheme()
    }

    /**
     * Set a custom theme by ID. Supports both bundled themes (from assets) and
     * custom themes (imported by the user, stored in internal files directory).
     *
     * @param themeId The theme ID (e.g., "nord", "dracula", or "custom-my-theme").
     * @param tokenColorCache Token color cache to clear on theme change.
     */
    fun setCustomTheme(themeId: String, tokenColorCache: TokenColorCache) {
        try {
            val bundledEntry = EditorThemeManager.findThemeById(themeId)
            if (bundledEntry != null) {
                EditorThemeManager.loadBundledTheme(appContext, bundledEntry)
                currentThemeId = themeId
                currentDarkTheme = bundledEntry.isDark
                tokenColorCache.clear()
                applyColorScheme()
                Log.d(TAG, "Custom theme set: ${bundledEntry.displayName} ($themeId)")
                return
            }

            val metadata = EditorThemeManager.findThemeMetadataById(themeId)
            if (metadata != null && !metadata.isBundled) {
                EditorThemeManager.loadCustomTheme(metadata)
                currentThemeId = themeId
                currentDarkTheme = metadata.isDark
                tokenColorCache.clear()
                applyColorScheme()
                Log.d(TAG, "Custom theme set: ${metadata.displayName} ($themeId)")
            } else {
                Log.w(TAG, "Theme not found: $themeId, falling back to default")
                currentThemeId = null
                tokenColorCache.clear()
                applyColorScheme()
            }
        } catch (e: Exception) {
            Log.w(TAG, "setCustomTheme failed", e)
        }
    }

    /**
     * Set the editor language for the given language identifier.
     *
     * Strategy: Try TreeSitter first for supported languages (pilot: Java only).
     * If TreeSitter is unavailable or fails, fall back to TextMate.
     * If neither works, use plain text mode.
     */
    fun setLanguageForContent(language: String?) {
        currentLanguage = language
        val editor = editorProvider() ?: return

        if (language.isNullOrBlank()) {
            try {
                editor.setEditorLanguage(null)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set null language", t)
            }
            return
        }

        if (TreeSitterLanguageProvider.isSupported(language)) {
            try {
                val tsLanguage = TreeSitterLanguageProvider.createLanguage(appContext, language)
                if (tsLanguage != null) {
                    editor.setEditorLanguage(tsLanguage)
                    Log.d(TAG, "TreeSitter language created for $language")
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "TreeSitter failed for $language, falling back to TextMate", t)
            }
        }

        val scopeName = LanguageConfig.languageToScopeName(language)
        Log.d(TAG, "setLanguageForContent: language=$language, scopeName=$scopeName (TextMate)")

        if (scopeName != null) {
            try {
                val tmLanguage = TextMateLanguage.create(scopeName, true)
                editor.setEditorLanguage(tmLanguage)
                Log.d(TAG, "TextMate language created for $scopeName")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to create TextMate language for $scopeName", t)
                try {
                    editor.setEditorLanguage(null)
                } catch (_: Throwable) {}
            }
        } else {
            try {
                editor.setEditorLanguage(null)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set null language (no TextMate scope)", t)
            }
            Log.d(TAG, "No TextMate language for $language, using plain text")
        }
    }

    /**
     * Wait for TextMate initialization to complete (with timeout), then apply
     * the given language. Called during [SoraEditorWrapper.loadContent] when
     * TextMate hasn't finished initializing yet.
     *
     * @param effectiveLanguage The language to apply after init completes.
     * @param onReady Callback invoked on the main thread after language is set.
     * @param onTimeout Callback invoked on the main thread if init times out.
     */
    suspend fun awaitInitAndApplyLanguage(
        effectiveLanguage: String?,
        onReady: () -> Unit,
        onTimeout: () -> Unit,
    ) {
        try {
            withTimeout(5000L) {
                initComplete.await()
            }
            withContext(Dispatchers.Main) {
                if (isReleased()) return@withContext
                try {
                    setLanguageForContent(effectiveLanguage)
                } catch (t: Throwable) {
                    Log.w(TAG, "setLanguageForContent failed after TextMate init", t)
                    try { editorProvider()?.setEditorLanguage(null) } catch (_: Throwable) {}
                }
                onReady()
            }
        } catch (e: Exception) {
            Log.w(TAG, "TextMate init timed out or failed, showing plain text", e)
            textMateInitFailed = true
            withContext(Dispatchers.Main) {
                if (isReleased()) return@withContext
                try {
                    editorProvider()?.setEditorLanguage(null)
                } catch (_: Throwable) {}
                onTimeout()
            }
        }
    }

    /** Mark initialization as complete (success or failure). */
    fun completeInit() {
        initComplete.complete(Unit)
    }

    /**
     * Global flag: TextMate registries (GrammarRegistry/ThemeRegistry/FileProviderRegistry)
     * are JVM singletons. Only initialize them once per process to avoid duplicate
     * provider registration and potential crashes.
     */
    companion object {
        private const val TAG = "SoraThemeManager"

        @Volatile
        private var globalTextMateInitialized = false
    }
}
