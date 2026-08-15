@file:Suppress("UNUSED")

/**
 * DraftPeek 应用排版定义。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含字体族定义（Inter 界面字体、JetBrains Mono 代码字体）、排版构建函数，
 * 以及各种预定义文本样式（代码样式、标题样式、设置项样式、标签样式等）。
 */
package com.draftpeek.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.InterFontFamily instead", ReplaceWith("InterFontFamily", "com.draftpeek.core.designsystem.theme"))
val InterFontFamily: FontFamily get() = com.draftpeek.core.designsystem.theme.InterFontFamily

@Deprecated("Use com.draftpeek.core.designsystem.theme.JetBrainsMonoFontFamily instead", ReplaceWith("JetBrainsMonoFontFamily", "com.draftpeek.core.designsystem.theme"))
val JetBrainsMonoFontFamily: FontFamily get() = com.draftpeek.core.designsystem.theme.JetBrainsMonoFontFamily

@Deprecated("Use com.draftpeek.core.designsystem.theme.AppFonts instead", ReplaceWith("AppFonts", "com.draftpeek.core.designsystem.theme"))
typealias AppFonts = com.draftpeek.core.designsystem.theme.AppFonts

@Deprecated("Use com.draftpeek.core.designsystem.theme.LocalAppFonts instead", ReplaceWith("LocalAppFonts", "com.draftpeek.core.designsystem.theme"))
val LocalAppFonts = com.draftpeek.core.designsystem.theme.LocalAppFonts

@Deprecated("Use com.draftpeek.core.designsystem.theme.buildTypography instead", ReplaceWith("buildTypography", "com.draftpeek.core.designsystem.theme"))
fun buildTypography(uiFontFamily: FontFamily, codeFontFamily: FontFamily): Typography =
    com.draftpeek.core.designsystem.theme.buildTypography(uiFontFamily, codeFontFamily)

@Deprecated("Use com.draftpeek.core.designsystem.theme.DraftPeekTypography instead", ReplaceWith("DraftPeekTypography", "com.draftpeek.core.designsystem.theme"))
val DraftPeekTypography: Typography get() = com.draftpeek.core.designsystem.theme.DraftPeekTypography

@Deprecated("Use com.draftpeek.core.designsystem.theme.CodeTextStyle instead", ReplaceWith("CodeTextStyle", "com.draftpeek.core.designsystem.theme"))
val CodeTextStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.CodeTextStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.MonoLabelStyle instead", ReplaceWith("MonoLabelStyle", "com.draftpeek.core.designsystem.theme"))
val MonoLabelStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.MonoLabelStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.MonoFileNameStyle instead", ReplaceWith("MonoFileNameStyle", "com.draftpeek.core.designsystem.theme"))
val MonoFileNameStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.MonoFileNameStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SerifTitleStyle instead", ReplaceWith("SerifTitleStyle", "com.draftpeek.core.designsystem.theme"))
val SerifTitleStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SerifTitleStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SerifSubtitleStyle instead", ReplaceWith("SerifSubtitleStyle", "com.draftpeek.core.designsystem.theme"))
val SerifSubtitleStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SerifSubtitleStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SectionLabelStyle instead", ReplaceWith("SectionLabelStyle", "com.draftpeek.core.designsystem.theme"))
val SectionLabelStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SectionLabelStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SettingNameStyle instead", ReplaceWith("SettingNameStyle", "com.draftpeek.core.designsystem.theme"))
val SettingNameStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SettingNameStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SettingDescStyle instead", ReplaceWith("SettingDescStyle", "com.draftpeek.core.designsystem.theme"))
val SettingDescStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SettingDescStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.FileMetaStyle instead", ReplaceWith("FileMetaStyle", "com.draftpeek.core.designsystem.theme"))
val FileMetaStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.FileMetaStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.NavigationBarLabelActive instead", ReplaceWith("NavigationBarLabelActive", "com.draftpeek.core.designsystem.theme"))
val NavigationBarLabelActive: TextStyle get() = com.draftpeek.core.designsystem.theme.NavigationBarLabelActive

@Deprecated("Use com.draftpeek.core.designsystem.theme.NavigationBarLabelInactive instead", ReplaceWith("NavigationBarLabelInactive", "com.draftpeek.core.designsystem.theme"))
val NavigationBarLabelInactive: TextStyle get() = com.draftpeek.core.designsystem.theme.NavigationBarLabelInactive

@Deprecated("Use com.draftpeek.core.designsystem.theme.DialogBodyStyle instead", ReplaceWith("DialogBodyStyle", "com.draftpeek.core.designsystem.theme"))
val DialogBodyStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.DialogBodyStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.H1Style instead", ReplaceWith("H1Style", "com.draftpeek.core.designsystem.theme"))
val H1Style: TextStyle get() = com.draftpeek.core.designsystem.theme.H1Style

@Deprecated("Use com.draftpeek.core.designsystem.theme.H2Style instead", ReplaceWith("H2Style", "com.draftpeek.core.designsystem.theme"))
val H2Style: TextStyle get() = com.draftpeek.core.designsystem.theme.H2Style

@Deprecated("Use com.draftpeek.core.designsystem.theme.MetaStyle instead", ReplaceWith("MetaStyle", "com.draftpeek.core.designsystem.theme"))
val MetaStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.MetaStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.TabLabelStyle instead", ReplaceWith("TabLabelStyle", "com.draftpeek.core.designsystem.theme"))
val TabLabelStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.TabLabelStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.EditorStatusBarStyle instead", ReplaceWith("EditorStatusBarStyle", "com.draftpeek.core.designsystem.theme"))
val EditorStatusBarStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.EditorStatusBarStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.TopBarTitleStyle instead", ReplaceWith("TopBarTitleStyle", "com.draftpeek.core.designsystem.theme"))
val TopBarTitleStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.TopBarTitleStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SubPageTopBarTitleStyle instead", ReplaceWith("SubPageTopBarTitleStyle", "com.draftpeek.core.designsystem.theme"))
val SubPageTopBarTitleStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SubPageTopBarTitleStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.MonoUppercaseTitleStyle instead", ReplaceWith("MonoUppercaseTitleStyle", "com.draftpeek.core.designsystem.theme"))
val MonoUppercaseTitleStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.MonoUppercaseTitleStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.ChipTextStyle instead", ReplaceWith("ChipTextStyle", "com.draftpeek.core.designsystem.theme"))
val ChipTextStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.ChipTextStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.TabLabelActiveStyle instead", ReplaceWith("TabLabelActiveStyle", "com.draftpeek.core.designsystem.theme"))
val TabLabelActiveStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.TabLabelActiveStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SearchBarHintStyle instead", ReplaceWith("SearchBarHintStyle", "com.draftpeek.core.designsystem.theme"))
val SearchBarHintStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SearchBarHintStyle

@Deprecated("Use com.draftpeek.core.designsystem.theme.SettingValueStyle instead", ReplaceWith("SettingValueStyle", "com.draftpeek.core.designsystem.theme"))
val SettingValueStyle: TextStyle get() = com.draftpeek.core.designsystem.theme.SettingValueStyle
