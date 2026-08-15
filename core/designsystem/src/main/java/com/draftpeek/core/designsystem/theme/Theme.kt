/**
 * DraftPeek 应用主题系统定义文件。
 *
 * 提供完整的Material 3主题支持，包括浅色/深色色彩方案、排版系统、形状系统，
 * 以及无障碍功能支持（色盲模式、高对比度模式、文字缩放）。
 *
 * 核心组件：
 * - [DraftPeekTheme]：应用主题Composable函数
 * - [PrototypeTokens]：原型设计专用颜色令牌
 * - [LocalDarkTheme]：暗色主题状态CompositionLocal
 * - [transformColorScheme]：色盲颜色转换工具
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceTint = SurfaceTintLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceTint = SurfaceTintDark,
)

/**
 * CompositionLocal for providing the app's dark theme state.
 * This allows PrototypeTokens to respond to the user's theme preference
 * rather than just the system theme.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * 批量计算后的 PrototypeTokens 颜色集合。
 *
 * 使用 @Immutable data class 确保 [remember] 的结构相等性比较有效：
 * 只有当至少一个颜色值实际改变时，依赖 [PrototypeTokens] 的 Composable 才会重组。
 */
@Immutable
private data class TokenColors(
    val pageBackground: Color,
    val accent: Color,
    val accentSoft: Color,
    val muted: Color,
    val fgSoft: Color,
    val border: Color,
    val elevated: Color,
    val bg: Color,
    val surface: Color,
    val fg: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
    val surfaceHover: Color,
    val mutedSoft: Color,
    val borderSoft: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val folder: Color,
    val folderContainer: Color,
)

/**
 * 根据无障碍状态和暗色主题一次性计算全部 21 个 token 颜色。
 *
 * 该函数为纯计算函数（非 @Composable），通过参数接收状态值，
 * 避免在 Compose 树中重复读取 CompositionLocal。
 *
 * @param state 当前无障碍状态
 * @param isDark 是否为暗色主题
 */
private fun computeTokenColors(state: AccessibilityState, isDark: Boolean): TokenColors {
    val colorBlindMode = state.colorBlindMode
    val highContrast = state.highContrastMode

    // 辅助：应用色盲变换（如启用）
    fun Color.blind(): Color = if (colorBlindMode == ColorBlindMode.NONE) this
        else ColorBlindnessHelper.transform(this, colorBlindMode)

    // 常规 token 取色：根据 highContrast + isDark 四选一，再叠加色盲变换
    fun pick(
        regularLight: Color,
        regularDark: Color,
        hcLight: Color,
        hcDark: Color,
    ): Color {
        val base = if (highContrast) {
            if (isDark) hcDark else hcLight
        } else {
            if (isDark) regularDark else regularLight
        }
        return base.blind()
    }

    // 语义色取色：常规色不区分深浅主题；高对比度模式下区分
    fun pickSemantic(
        regular: Color,
        hcLight: Color,
        hcDark: Color,
    ): Color {
        val base = if (highContrast) {
            if (isDark) hcDark else hcLight
        } else {
            regular
        }
        return base.blind()
    }

    return TokenColors(
        pageBackground = pick(
            PageBackgroundLight, PageBackgroundDark,
            HighContrastScheme.Light.background, HighContrastScheme.Dark.background,
        ),
        accent = pick(
            PrimaryLight, PrimaryDark,
            HighContrastScheme.Light.primary, HighContrastScheme.Dark.primary,
        ),
        accentSoft = pick(
            AccentSoftLight, AccentSoftDark,
            HighContrastScheme.Light.primaryContainer, HighContrastScheme.Dark.primaryContainer,
        ),
        muted = pick(
            MutedLight, MutedDark,
            HighContrastScheme.Light.onSurfaceVariant, HighContrastScheme.Dark.onSurfaceVariant,
        ),
        fgSoft = pick(
            FgSoftLight, FgSoftDark,
            HighContrastScheme.Light.onSurfaceVariant, HighContrastScheme.Dark.onSurfaceVariant,
        ),
        border = pick(
            OutlineLight, OutlineDark,
            HighContrastScheme.Light.outline, HighContrastScheme.Dark.outline,
        ),
        elevated = pick(
            SurfaceVariantLight, SurfaceVariantDark,
            HighContrastScheme.Light.surfaceVariant, HighContrastScheme.Dark.surfaceVariant,
        ),
        bg = pick(
            BackgroundLight, BackgroundDark,
            HighContrastScheme.Light.background, HighContrastScheme.Dark.background,
        ),
        surface = pick(
            SurfaceLight, SurfaceDark,
            HighContrastScheme.Light.surface, HighContrastScheme.Dark.surface,
        ),
        fg = pick(
            OnSurfaceLight, OnSurfaceDark,
            HighContrastScheme.Light.onSurface, HighContrastScheme.Dark.onSurface,
        ),
        success = pickSemantic(
            SemanticColors.Success,
            HighContrastScheme.Light.tertiary, HighContrastScheme.Dark.tertiary,
        ),
        error = pickSemantic(
            SemanticColors.Danger,
            HighContrastScheme.Light.error, HighContrastScheme.Dark.error,
        ),
        warning = pickSemantic(
            SemanticColors.Warning,
            Color(0xFFCC6600), Color(0xFFFF9900),
        ),
        info = pickSemantic(
            SemanticColors.Info,
            HighContrastScheme.Light.secondary, HighContrastScheme.Dark.secondary,
        ),
        surfaceHover = pick(
            SurfaceHoverLight, SurfaceHoverDark,
            HighContrastScheme.Light.surfaceVariant, HighContrastScheme.Dark.surfaceVariant,
        ),
        mutedSoft = pick(
            MutedSoftLight, MutedSoftDark,
            HighContrastScheme.Light.outlineVariant, HighContrastScheme.Dark.outlineVariant,
        ),
        borderSoft = pick(
            BorderSoftLight, BorderSoftDark,
            HighContrastScheme.Light.outlineVariant, HighContrastScheme.Dark.outlineVariant,
        ),
        infoContainer = pick(
            InfoContainerLight, InfoContainerDark,
            HighContrastScheme.Light.secondaryContainer, HighContrastScheme.Dark.secondaryContainer,
        ),
        onInfoContainer = pick(
            OnInfoContainerLight, OnInfoContainerDark,
            HighContrastScheme.Light.onSecondaryContainer, HighContrastScheme.Dark.onSecondaryContainer,
        ),
        folder = pick(
            FolderColorLight, FolderColorDark,
            HighContrastScheme.Light.tertiary, HighContrastScheme.Dark.tertiary,
        ),
        folderContainer = pick(
            FolderContainerLight, FolderContainerDark,
            HighContrastScheme.Light.tertiaryContainer, HighContrastScheme.Dark.tertiaryContainer,
        ),
    )
}

/**
 * Prototype-specific tokens for custom components.
 * These bypass MaterialTheme.colorScheme to ensure exact prototype matching.
 * Use these in all custom Compose components (CustomTabBar, CustomScaffold, etc.)
 *
 * 无障碍支持：所有颜色通过 [computeTokenColors] 批量计算后缓存：
 * - **高对比度模式**：切换至 [HighContrastScheme] 中对应字段，确保 WCAG AA 级对比度。
 * - **色盲模式**：通过 [ColorBlindnessHelper.transform] 进行 LMS 色彩空间模拟。
 * - **性能优化**：使用 [remember] 缓存计算结果，仅当 [LocalAccessibilityState] 或
 *   [LocalDarkTheme] 变化时才重新计算，避免每次重组都重算 21 个颜色。
 */
object PrototypeTokens {
    private val colors: TokenColors
        @Composable get() {
            val state = LocalAccessibilityState.current
            val isDark = LocalDarkTheme.current
            return remember(state, isDark) { computeTokenColors(state, isDark) }
        }

    val pageBackground: Color @Composable get() = colors.pageBackground
    val accent: Color @Composable get() = colors.accent
    val accentSoft: Color @Composable get() = colors.accentSoft
    val muted: Color @Composable get() = colors.muted
    val fgSoft: Color @Composable get() = colors.fgSoft
    val border: Color @Composable get() = colors.border
    val elevated: Color @Composable get() = colors.elevated
    val bg: Color @Composable get() = colors.bg
    val surface: Color @Composable get() = colors.surface
    val fg: Color @Composable get() = colors.fg
    val success: Color @Composable get() = colors.success
    val error: Color @Composable get() = colors.error
    val warning: Color @Composable get() = colors.warning
    val info: Color @Composable get() = colors.info
    val surfaceHover: Color @Composable get() = colors.surfaceHover
    val mutedSoft: Color @Composable get() = colors.mutedSoft
    val borderSoft: Color @Composable get() = colors.borderSoft
    val infoContainer: Color @Composable get() = colors.infoContainer
    val onInfoContainer: Color @Composable get() = colors.onInfoContainer
    val folder: Color @Composable get() = colors.folder
    val folderContainer: Color @Composable get() = colors.folderContainer
}

/**
 * DraftPeek application theme.
 *
 * @param darkTheme Whether to use the dark color scheme.
 *                  Defaults to system setting via [isSystemInDarkTheme].
 * @param accessibilityState 无障碍状态，控制色盲模式、高对比度、文字缩放等。
 * @param content The content to be themed.
 */
@Composable
fun DraftPeekTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accessibilityState: AccessibilityState = AccessibilityState.Default,
    appFonts: AppFonts = AppFonts(),
    content: @Composable () -> Unit,
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // 高对比度模式：使用专门的高对比度配色方案
    val colorScheme = if (accessibilityState.highContrastMode) {
        HighContrastScheme.scheme(darkTheme)
    } else {
        baseScheme
    }

    // 色盲模式：对颜色方案中的所有颜色进行色盲模拟转换
    val finalScheme = if (accessibilityState.colorBlindMode != ColorBlindMode.NONE) {
        transformColorScheme(colorScheme, accessibilityState.colorBlindMode)
    } else {
        colorScheme
    }

    val finalSchemeWithScrim = finalScheme.copy(scrim = Color.Black)

    // 动态构建Typography
    val typography = remember(appFonts.uiFontFamily, appFonts.codeFontFamily) {
        buildTypography(appFonts.uiFontFamily, appFonts.codeFontFamily)
    }

    // 文字缩放：通过 Density 覆盖实现全局文字大小调整
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * accessibilityState.textScale,
    )

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAccessibilityState provides accessibilityState,
        LocalDensity provides scaledDensity,
        LocalAppFonts provides appFonts,
    ) {
        MaterialTheme(
            colorScheme = finalSchemeWithScrim,
            typography = typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/**
 * 对 ColorScheme 中的所有颜色应用色盲转换。
 */
private fun transformColorScheme(
    scheme: androidx.compose.material3.ColorScheme,
    mode: ColorBlindMode,
): androidx.compose.material3.ColorScheme {
    return scheme.copy(
        primary = ColorBlindnessHelper.transform(scheme.primary, mode),
        onPrimary = ColorBlindnessHelper.transform(scheme.onPrimary, mode),
        primaryContainer = ColorBlindnessHelper.transform(scheme.primaryContainer, mode),
        onPrimaryContainer = ColorBlindnessHelper.transform(scheme.onPrimaryContainer, mode),
        secondary = ColorBlindnessHelper.transform(scheme.secondary, mode),
        onSecondary = ColorBlindnessHelper.transform(scheme.onSecondary, mode),
        secondaryContainer = ColorBlindnessHelper.transform(scheme.secondaryContainer, mode),
        onSecondaryContainer = ColorBlindnessHelper.transform(scheme.onSecondaryContainer, mode),
        tertiary = ColorBlindnessHelper.transform(scheme.tertiary, mode),
        onTertiary = ColorBlindnessHelper.transform(scheme.onTertiary, mode),
        tertiaryContainer = ColorBlindnessHelper.transform(scheme.tertiaryContainer, mode),
        onTertiaryContainer = ColorBlindnessHelper.transform(scheme.onTertiaryContainer, mode),
        error = ColorBlindnessHelper.transform(scheme.error, mode),
        onError = ColorBlindnessHelper.transform(scheme.onError, mode),
        errorContainer = ColorBlindnessHelper.transform(scheme.errorContainer, mode),
        onErrorContainer = ColorBlindnessHelper.transform(scheme.onErrorContainer, mode),
        background = ColorBlindnessHelper.transform(scheme.background, mode),
        onBackground = ColorBlindnessHelper.transform(scheme.onBackground, mode),
        surface = ColorBlindnessHelper.transform(scheme.surface, mode),
        onSurface = ColorBlindnessHelper.transform(scheme.onSurface, mode),
        surfaceVariant = ColorBlindnessHelper.transform(scheme.surfaceVariant, mode),
        onSurfaceVariant = ColorBlindnessHelper.transform(scheme.onSurfaceVariant, mode),
        outline = ColorBlindnessHelper.transform(scheme.outline, mode),
        outlineVariant = ColorBlindnessHelper.transform(scheme.outlineVariant, mode),
        inverseSurface = ColorBlindnessHelper.transform(scheme.inverseSurface, mode),
        inverseOnSurface = ColorBlindnessHelper.transform(scheme.inverseOnSurface, mode),
        inversePrimary = ColorBlindnessHelper.transform(scheme.inversePrimary, mode),
        surfaceTint = ColorBlindnessHelper.transform(scheme.surfaceTint, mode),
    )
}
