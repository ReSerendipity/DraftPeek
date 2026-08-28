/**
 * 高对比度配色方案定义文件。
 *
 * 提供符合WCAG 2.1 AA级标准的高对比度浅色/深色色彩方案，
 * 确保视障用户能够清晰区分界面元素（对比度≥4.5:1用于正常文本）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 高对比度颜色方案。
 *
 * 符合 WCAG 2.1 AA 级标准（对比度 ≥ 4.5:1 用于正常文本，≥ 3:1 用于大文本）。
 * 在高对比度模式下替代标准配色方案，确保视障用户可以清晰区分界面元素。
 */
object HighContrastScheme {

    /** 高对比度浅色方案 */
    val Light = lightColorScheme(
        primary = Color(0xFF0000FF), // 纯蓝，与白色背景对比度极高
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD0D0FF),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFF0066CC),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD0E8FF),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFF006600),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD0FFD0),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFFCC0000),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFD0D0),
        onErrorContainer = Color(0xFF000000),
        background = Color(0xFFFFFFFF), // 纯白背景
        onBackground = Color(0xFF000000), // 纯黑文字
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color(0xFF000000),
        outline = Color(0xFF666666), // 更深的边框线
        outlineVariant = Color(0xFF999999),
        inverseSurface = Color(0xFF000000),
        inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF9999FF),
        surfaceTint = Color(0xFF0000FF)
    )

    /** 高对比度深色方案 */
    val Dark = darkColorScheme(
        primary = Color(0xFFFFFF00), // 纯黄，与黑色背景对比度极高
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF555500),
        onPrimaryContainer = Color(0xFFFFFF00),
        secondary = Color(0xFF66CCFF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF003355),
        onSecondaryContainer = Color(0xFF66CCFF),
        tertiary = Color(0xFF00FF00),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF003300),
        onTertiaryContainer = Color(0xFF00FF00),
        error = Color(0xFFFF6666),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF550000),
        onErrorContainer = Color(0xFFFF9999),
        background = Color(0xFF000000), // 纯黑背景
        onBackground = Color(0xFFFFFFFF), // 纯白文字
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF222222),
        onSurfaceVariant = Color(0xFFFFFFFF),
        outline = Color(0xFF999999),
        outlineVariant = Color(0xFF666666),
        inverseSurface = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFF666600),
        surfaceTint = Color(0xFFFFFF00)
    )

    /**
     * 根据是否为暗色主题返回对应的高对比度方案。
     */
    fun scheme(isDark: Boolean): ColorScheme = if (isDark) Dark else Light
}
