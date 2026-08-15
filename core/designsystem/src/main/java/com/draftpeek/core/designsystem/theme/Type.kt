/**
 * DraftPeek 排版系统定义文件。
 *
 * 定义应用的字体族（Inter用于UI，JetBrains Mono用于代码）、Material 3 Typography配置，
 * 以及各种自定义文本样式（标题、正文、代码、标签等）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.draftpeek.core.designsystem.R

/** Inter字体族，用于UI界面文本 */
val InterFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.inter_regular),
    androidx.compose.ui.text.font.Font(R.font.inter_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.inter_semibold, FontWeight.SemiBold),
    androidx.compose.ui.text.font.Font(R.font.inter_bold, FontWeight.Bold),
)

/** JetBrains Mono字体族，用于代码和等宽文本 */
val JetBrainsMonoFontFamily = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_regular),
    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    androidx.compose.ui.text.font.Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

/**
 * 应用字体配置数据类。
 * @property uiFontFamily UI界面使用的字体族，默认为Inter
 * @property codeFontFamily 代码编辑使用的等宽字体族，默认为JetBrains Mono
 */
@Immutable
data class AppFonts(
    val uiFontFamily: FontFamily = InterFontFamily,
    val codeFontFamily: FontFamily = JetBrainsMonoFontFamily
)

/** 应用字体配置的CompositionLocal */
val LocalAppFonts = compositionLocalOf { AppFonts() }

/**
 * 构建Material 3 Typography配置。
 * @param uiFontFamily UI字体族
 * @param codeFontFamily 代码字体族（用于代码样式）
 * @return 配置完成的Typography实例
 */
fun buildTypography(uiFontFamily: FontFamily, codeFontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.02).sp,
        ),
        titleLarge = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.01).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.01).sp,
        ),
        titleSmall = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = uiFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.06.sp,
        ),
    )
}

val DraftPeekTypography = buildTypography(InterFontFamily, JetBrainsMonoFontFamily)

/**
 * Text style for code / monospace content in the editor.
 * Uses the system monospace font family.
 */
val CodeTextStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
)

// Design system: Monospace label for file extension badges (32x32 boxes)
val MonoLabelStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 0.3.sp,
    lineHeight = 12.sp,
)

// Design system: Monospace file name for code files
val MonoFileNameStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = (-0.2).sp,
    lineHeight = 18.sp,
)

// Design system: Serif title for empty states
val SerifTitleStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Black,  // 900
    fontSize = 22.sp,
    letterSpacing = 6.sp,
    lineHeight = 28.sp,
)

// Design system: Serif subtitle for empty states
val SerifSubtitleStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 3.sp,
    lineHeight = 18.sp,
)

// Design system: Small section label (settings groups)
val SectionLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.sp,
    lineHeight = 16.sp,
)

// Design system: Setting item name
val SettingNameStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.sp,
    lineHeight = 20.sp,
)

// Design system: Setting description
val SettingDescStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.1.sp,
    lineHeight = 15.sp,
)

// Design system: File meta info (size, date)
val FileMetaStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.1.sp,
    lineHeight = 15.sp,
)

// Design system: Navigation bar label — active state
val NavigationBarLabelActive = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 0.5.sp,
    lineHeight = 16.sp,
)

// Design system: Navigation bar label — inactive state
val NavigationBarLabelInactive = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    letterSpacing = 0.5.sp,
    lineHeight = 16.sp,
)

// Design system: Dialog body text (slightly looser line height for readability)
val DialogBodyStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.25.sp,
    lineHeight = 22.sp,
)

// ============================================================
// Prototype Design Styles — Additional text styles from redesign
// ============================================================

// H1 page title (26sp/Bold, tight letter-spacing)
val H1Style = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    letterSpacing = (-0.02).sp,
    lineHeight = 32.sp,
)

// H2 section title (20sp/Bold, tight letter-spacing — matching prototype "DraftPeek" / "Me" headers)
val H2Style = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    letterSpacing = (-0.02).sp,
    lineHeight = 26.sp,
)

// Meta label (11sp/SemiBold, wide letter-spacing — for section labels like "DIRECTORIES")
val MetaStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 0.06.sp,
    lineHeight = 16.sp,
)

// Tab bar label (10sp/Medium)
val TabLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    letterSpacing = 0.02.sp,
    lineHeight = 14.sp,
)

// Editor status bar text (11sp mono)
val EditorStatusBarStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.sp,
    lineHeight = 16.sp,
)

// Top bar page title (22sp Bold)
val TopBarTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = (-0.03).sp,
    lineHeight = 28.sp,
)

// Top bar sub-page title (18sp Bold — slightly smaller for sub-screens with back button)
val SubPageTopBarTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = (-0.02).sp,
    lineHeight = 24.sp,
)

// Mono uppercase title for empty states (18sp Bold)
val MonoUppercaseTitleStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = 1.sp,
    lineHeight = 24.sp,
)

// Chip text (13sp Regular)
val ChipTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.sp,
    lineHeight = 18.sp,
)

// Tab label active state (10sp SemiBold)
val TabLabelActiveStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 0.02.sp,
    lineHeight = 14.sp,
)

// Search bar hint text (14sp, muted color applied at composable level)
val SearchBarHintStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.sp,
    lineHeight = 20.sp,
)

// Settings row value (13sp mono — for displaying current setting values)
val SettingValueStyle = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.sp,
    lineHeight = 18.sp,
)
