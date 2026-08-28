package com.draftpeek.feature.editor.sora

/**
 * Holds pre-computed ARGB int values for UI-relevant sora-editor color slots.
 *
 * Slot categories:
 * - **Base editor** (1–15): background, text, line numbers, selection, scrollbar, block lines
 * - **Editor chrome** (29–31): search match highlights, selection text, visible whitespace
 * - **Syntax highlighting** (21–28, 32–34): handled by TextMate themes, NOT overridden here
 * - **UI chrome** (16–20, 35–83): completion, diagnostics, hints, snippets, minimap, etc.
 *
 * We map every UI slot that TextMate themes do NOT set, plus provide M3-consistent
 * fallbacks for base editor slots so the editor looks correct even if a TextMate
 * theme fails to load.
 *
 * Extracted from [SoraEditorWrapper] to reduce file size and improve maintainability.
 */
internal data class SoraEditorColors(
    // ── Base editor slots (1–15) ──────────────────────────────────────
    val lineDivider: Int, // 1
    val lineNumber: Int, // 2
    val lineNumberBackground: Int, // 3
    val wholeBackground: Int, // 4
    val textNormal: Int, // 5
    val selectedTextBackground: Int, // 6
    val selectionInsert: Int, // 7  (cursor color)
    val selectionHandle: Int, // 8
    val currentLine: Int, // 9
    val underline: Int, // 10
    val scrollBarThumb: Int, // 11
    val scrollBarThumbPressed: Int, // 12
    val scrollBarTrack: Int, // 13
    val blockLine: Int, // 14
    val blockLineCurrent: Int, // 15

    // ── UI chrome (16–20) ─────────────────────────────────────────────
    val lineNumberPanel: Int, // 16
    val lineNumberPanelText: Int, // 17
    val lineBlockLabel: Int, // 18
    val completionWndBackground: Int, // 19
    val completionWndCorner: Int, // 20

    // ── Editor UI chrome (29–31) ──────────────────────────────────────
    val matchedTextBackground: Int, // 29 (search match highlights)
    val textSelected: Int, // 30 (0 = no text color change for selections)
    val nonPrintableChar: Int, // 31 (visible whitespace: tabs, spaces, EOL)

    // ── UI chrome (35–45) ─────────────────────────────────────────────
    val problemError: Int, // 35
    val problemWarning: Int, // 36
    val problemTypo: Int, // 37
    val sideBlockLine: Int, // 38
    val highlightedDelimitersForeground: Int, // 39
    val highlightedDelimitersUnderline: Int, // 40
    val highlightedDelimitersBackground: Int, // 41
    val completionWndTextPrimary: Int, // 42
    val completionWndTextSecondary: Int, // 43
    val completionWndItemCurrent: Int, // 44
    val lineNumberCurrent: Int, // 45

    // ── UI chrome (46–67) ─────────────────────────────────────────────
    val snippetBackgroundInactive: Int, // 46
    val snippetBackgroundRelated: Int, // 47
    val snippetBackgroundEditing: Int, // 48
    val textInlayHintBackground: Int, // 49
    val textInlayHintForeground: Int, // 50
    val hardWrapMarker: Int, // 51
    val functionCharBackgroundStroke: Int, // 52
    val diagnosticTooltipBackground: Int, // 53
    val diagnosticTooltipBriefMsg: Int, // 54
    val diagnosticTooltipDetailedMsg: Int, // 55
    val diagnosticTooltipAction: Int, // 56
    val strikethrough: Int, // 57
    val signatureTextNormal: Int, // 58
    val signatureTextHighlightedParameter: Int, // 59
    val signatureBackground: Int, // 60
    val stickyScrollDivider: Int, // 62
    val staticSpanBackground: Int, // 63
    val staticSpanForeground: Int, // 64
    val textActionWindowBackground: Int, // 65
    val textActionWindowIconColor: Int, // 66
    val completionWndTextMatched: Int, // 67

    // ── UI chrome (68–83) ─────────────────────────────────────────────
    val hoverTextNormal: Int, // 68
    val hoverBackground: Int, // 69
    val hoverBorder: Int, // 70
    val signatureBorder: Int, // 71
    val hoverTextHighlighted: Int, // 72
    val textHighlightStrongBackground: Int, // 73
    val textHighlightBackground: Int, // 74
    val highlightedDelimitersBorder: Int, // 75
    val textHighlightStrongBorder: Int, // 76
    val textHighlightBorder: Int, // 77
    val matchedTextBorder: Int, // 78
    val selectedTextBorder: Int, // 79
    val currentRowBorder: Int, // 80
    val minimapBackground: Int, // 81
    val minimapViewport: Int, // 82
    val minimapViewportBorder: Int // 83
)

/** Light-theme M3 editor color palette (matches Color.kt / DraftPeek light theme). */
internal val soraEditorColorsLight = SoraEditorColors(
    // Base palette
    // surface = 0xFFFFFFFF, surfaceVariant = 0xFFFAFAFA, onSurface = 0xFF15151A
    // outline = 0xFFE5E5E5, primary = 0xFFC41E3A, muted = 0xFF8E8E96
    // fgSoft = 0xFF3A3A42, accentSoft = 0x1AC41E3A
    // error = 0xFFFF3B30, warning = 0xFFFF9F0A

    // ── Base editor (1–15) ──
    lineDivider = 0xFFE5E5E5.toInt(), // 1  outline
    lineNumber = 0xFF8E8E96.toInt(), // 2  muted
    lineNumberBackground = 0xFFFAFAFA.toInt(), // 3  surfaceVariant
    wholeBackground = 0xFFFFFFFF.toInt(), // 4  surface
    textNormal = 0xFF15151A.toInt(), // 5  onSurface
    selectedTextBackground = 0x1AC41E3A.toInt(), // 6  accentSoft
    selectionInsert = 0xFFC41E3A.toInt(), // 7  primary (cursor)
    selectionHandle = 0xFFC41E3A.toInt(), // 8  primary
    currentLine = 0x0FC41E3A.toInt(), // 9  (6% accent highlight)
    underline = 0xFF3A3A42.toInt(), // 10 fgSoft
    scrollBarThumb = 0x3D15151A.toInt(), // 11
    scrollBarThumbPressed = 0xFF3A3A42.toInt(), // 12 fgSoft
    scrollBarTrack = 0x00000000.toInt(), // 13 (transparent)
    blockLine = 0xFFE5E5E5.toInt(), // 14 outline
    blockLineCurrent = 0x4D15151A.toInt(), // 15

    // ── UI chrome (16–20) ──
    lineNumberPanel = 0xFFFAFAFA.toInt(), // 16 surfaceVariant
    lineNumberPanelText = 0xFF8E8E96.toInt(), // 17 muted
    lineBlockLabel = 0xFF8E8E96.toInt(), // 18 muted
    completionWndBackground = 0xFFFFFFFF.toInt(), // 19 surface
    completionWndCorner = 0xFFE5E5E5.toInt(), // 20 outline

    // ── Editor UI chrome (29–31) ──
    matchedTextBackground = 0x33C41E3A.toInt(), // 29 (search matches: primary tint)
    textSelected = 0, // 30 (0 = keep text color in selections)
    nonPrintableChar = 0x668E8E96.toInt(), // 31 (visible whitespace: muted gray)

    // ── UI chrome (35–45) ──
    problemError = 0xFFFF3B30.toInt(), // 35 error
    problemWarning = 0xFFFF9F0A.toInt(), // 36 warning
    problemTypo = 0xFF8E8E96.toInt(), // 37 muted
    sideBlockLine = 0xFFE5E5E5.toInt(), // 38 outline
    highlightedDelimitersForeground = 0xFFC41E3A.toInt(), // 39 primary
    highlightedDelimitersUnderline = 0xFFC41E3A.toInt(), // 40 primary
    highlightedDelimitersBackground = 0x1AC41E3A.toInt(), // 41
    completionWndTextPrimary = 0xFF15151A.toInt(), // 42 onSurface
    completionWndTextSecondary = 0xFF8E8E96.toInt(), // 43 muted
    completionWndItemCurrent = 0x1AC41E3A.toInt(), // 44 accentSoft
    lineNumberCurrent = 0xFF15151A.toInt(), // 45 onSurface

    // ── UI chrome (46–67) ──
    snippetBackgroundInactive = 0x0F15151A.toInt(), // 46
    snippetBackgroundRelated = 0x1A15151A.toInt(), // 47
    snippetBackgroundEditing = 0x1AC41E3A.toInt(), // 48 accentSoft
    textInlayHintBackground = 0x1A15151A.toInt(), // 49
    textInlayHintForeground = 0xFF8E8E96.toInt(), // 50 muted
    hardWrapMarker = 0xFF8E8E96.toInt(), // 51 muted
    functionCharBackgroundStroke = 0x1AC41E3A.toInt(), // 52
    diagnosticTooltipBackground = 0xFFFFFFFF.toInt(), // 53 surface
    diagnosticTooltipBriefMsg = 0xFF15151A.toInt(), // 54 onSurface
    diagnosticTooltipDetailedMsg = 0xFF8E8E96.toInt(), // 55 muted
    diagnosticTooltipAction = 0xFFC41E3A.toInt(), // 56 primary
    strikethrough = 0xFFFF3B30.toInt(), // 57 error
    signatureTextNormal = 0xFF15151A.toInt(), // 58 onSurface
    signatureTextHighlightedParameter = 0xFFC41E3A.toInt(), // 59 primary
    signatureBackground = 0xFFFAFAFA.toInt(), // 60 surfaceVariant
    stickyScrollDivider = 0xFFE5E5E5.toInt(), // 62 outline
    staticSpanBackground = 0x1A15151A.toInt(), // 63
    staticSpanForeground = 0xFF8E8E96.toInt(), // 64 muted
    textActionWindowBackground = 0xFFFFFFFF.toInt(), // 65 surface
    textActionWindowIconColor = 0xFF15151A.toInt(), // 66 onSurface
    completionWndTextMatched = 0xFFC41E3A.toInt(), // 67 primary

    // ── UI chrome (68–83) ──
    hoverTextNormal = 0xFF15151A.toInt(), // 68 onSurface
    hoverBackground = 0xFFFFFFFF.toInt(), // 69 surface
    hoverBorder = 0xFFE5E5E5.toInt(), // 70 outline
    signatureBorder = 0xFFE5E5E5.toInt(), // 71 outline
    hoverTextHighlighted = 0xFFC41E3A.toInt(), // 72 primary
    textHighlightStrongBackground = 0x4DC41E3A.toInt(), // 73
    textHighlightBackground = 0x1AC41E3A.toInt(), // 74
    highlightedDelimitersBorder = 0x4DC41E3A.toInt(), // 75
    textHighlightStrongBorder = 0xFFC41E3A.toInt(), // 76 primary
    textHighlightBorder = 0x4DC41E3A.toInt(), // 77
    matchedTextBorder = 0x4DC41E3A.toInt(), // 78
    selectedTextBorder = 0xFFC41E3A.toInt(), // 79 primary
    currentRowBorder = 0xFFE5E5E5.toInt(), // 80 outline
    minimapBackground = 0xFFFFFFFF.toInt(), // 81 surface
    minimapViewport = 0x1A15151A.toInt(), // 82
    minimapViewportBorder = 0xFFE5E5E5.toInt() // 83 outline
)

/** Dark-theme M3 editor color palette (matches Color.kt / DraftPeek dark theme). */
internal val soraEditorColorsDark = SoraEditorColors(
    // Base palette
    // surface = 0xFF141416, surfaceVariant = 0xFF1C1C1C, onSurface = 0xFFF0F0F2
    // outline = 0xFF282828, primary = 0xFFE8A838, muted = 0xFF787886
    // fgSoft = 0xFFC0C0C8, accentSoft = 0x1FE8A838
    // error = 0xFFFF6961, warning = 0xFFFF9F0A

    // ── Base editor (1–15) ──
    lineDivider = 0xFF282828.toInt(), // 1  outline
    lineNumber = 0xFF787886.toInt(), // 2  muted
    lineNumberBackground = 0xFF1C1C1C.toInt(), // 3  surfaceVariant
    wholeBackground = 0xFF141416.toInt(), // 4  surface
    textNormal = 0xFFF0F0F2.toInt(), // 5  onSurface
    selectedTextBackground = 0x1FE8A838.toInt(), // 6  accentSoft
    selectionInsert = 0xFFE8A838.toInt(), // 7  primary (cursor)
    selectionHandle = 0xFFE8A838.toInt(), // 8  primary
    currentLine = 0x0FF0F0F2.toInt(), // 9  (subtle highlight)
    underline = 0xFFC0C0C8.toInt(), // 10 fgSoft
    scrollBarThumb = 0x3DF0F0F2.toInt(), // 11
    scrollBarThumbPressed = 0xFFC0C0C8.toInt(), // 12 fgSoft
    scrollBarTrack = 0x00000000.toInt(), // 13 (transparent)
    blockLine = 0xFF282828.toInt(), // 14 outline
    blockLineCurrent = 0x4DF0F0F2.toInt(), // 15

    // ── UI chrome (16–20) ──
    lineNumberPanel = 0xFF1C1C1C.toInt(), // 16 surfaceVariant
    lineNumberPanelText = 0xFF787886.toInt(), // 17 muted
    lineBlockLabel = 0xFF787886.toInt(), // 18 muted
    completionWndBackground = 0xFF141416.toInt(), // 19 surface
    completionWndCorner = 0xFF282828.toInt(), // 20 outline

    // ── Editor UI chrome (29–31) ──
    matchedTextBackground = 0x33E8A838.toInt(), // 29 (search matches: gold tint)
    textSelected = 0, // 30 (0 = keep text color in selections)
    nonPrintableChar = 0x66787886.toInt(), // 31 (visible whitespace: muted gray)

    // ── UI chrome (35–45) ──
    problemError = 0xFFFF6961.toInt(), // 35 error
    problemWarning = 0xFFFF9F0A.toInt(), // 36 warning
    problemTypo = 0xFF787886.toInt(), // 37 muted
    sideBlockLine = 0xFF282828.toInt(), // 38 outline
    highlightedDelimitersForeground = 0xFFE8A838.toInt(), // 39 primary
    highlightedDelimitersUnderline = 0xFFE8A838.toInt(), // 40 primary
    highlightedDelimitersBackground = 0x1FE8A838.toInt(), // 41
    completionWndTextPrimary = 0xFFF0F0F2.toInt(), // 42 onSurface
    completionWndTextSecondary = 0xFF787886.toInt(), // 43 muted
    completionWndItemCurrent = 0x1FE8A838.toInt(), // 44 accentSoft
    lineNumberCurrent = 0xFFF0F0F2.toInt(), // 45 onSurface

    // ── UI chrome (46–67) ──
    snippetBackgroundInactive = 0x0FF0F0F2.toInt(), // 46
    snippetBackgroundRelated = 0x1AF0F0F2.toInt(), // 47
    snippetBackgroundEditing = 0x1FE8A838.toInt(), // 48 accentSoft
    textInlayHintBackground = 0x1AF0F0F2.toInt(), // 49
    textInlayHintForeground = 0xFF787886.toInt(), // 50 muted
    hardWrapMarker = 0xFF787886.toInt(), // 51 muted
    functionCharBackgroundStroke = 0x1FE8A838.toInt(), // 52
    diagnosticTooltipBackground = 0xFF141416.toInt(), // 53 surface
    diagnosticTooltipBriefMsg = 0xFFF0F0F2.toInt(), // 54 onSurface
    diagnosticTooltipDetailedMsg = 0xFF787886.toInt(), // 55 muted
    diagnosticTooltipAction = 0xFFE8A838.toInt(), // 56 primary
    strikethrough = 0xFFFF6961.toInt(), // 57 error
    signatureTextNormal = 0xFFF0F0F2.toInt(), // 58 onSurface
    signatureTextHighlightedParameter = 0xFFE8A838.toInt(), // 59 primary
    signatureBackground = 0xFF1C1C1C.toInt(), // 60 surfaceVariant
    stickyScrollDivider = 0xFF282828.toInt(), // 62 outline
    staticSpanBackground = 0x1AF0F0F2.toInt(), // 63
    staticSpanForeground = 0xFF787886.toInt(), // 64 muted
    textActionWindowBackground = 0xFF141416.toInt(), // 65 surface
    textActionWindowIconColor = 0xFFF0F0F2.toInt(), // 66 onSurface
    completionWndTextMatched = 0xFFE8A838.toInt(), // 67 primary

    // ── UI chrome (68–83) ──
    hoverTextNormal = 0xFFF0F0F2.toInt(), // 68 onSurface
    hoverBackground = 0xFF141416.toInt(), // 69 surface
    hoverBorder = 0xFF282828.toInt(), // 70 outline
    signatureBorder = 0xFF282828.toInt(), // 71 outline
    hoverTextHighlighted = 0xFFE8A838.toInt(), // 72 primary
    textHighlightStrongBackground = 0x4DE8A838.toInt(), // 73
    textHighlightBackground = 0x1FE8A838.toInt(), // 74
    highlightedDelimitersBorder = 0x4DE8A838.toInt(), // 75
    textHighlightStrongBorder = 0xFFE8A838.toInt(), // 76 primary
    textHighlightBorder = 0x1FE8A838.toInt(), // 77
    matchedTextBorder = 0x4DE8A838.toInt(), // 78
    selectedTextBorder = 0xFFE8A838.toInt(), // 79 primary
    currentRowBorder = 0xFF282828.toInt(), // 80 outline
    minimapBackground = 0xFF141416.toInt(), // 81 surface
    minimapViewport = 0x1AF0F0F2.toInt(), // 82
    minimapViewportBorder = 0xFF282828.toInt() // 83 outline
)
