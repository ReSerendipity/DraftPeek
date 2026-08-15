@file:Suppress("DEPRECATION") // Intentionally deprecated; kept for runtime compatibility

/**
 * 文件: StatsStyle.kt
 * 功能: 统计模块 UI 样式 - 已废弃的设计系统定义
 * 描述: 该文件定义了一套并行的设计系统（StatsColors/StatsTypography/StatsSpacing/StatsRadius），
 *       这些令牌已被 core/ui/theme/ 中的 MaterialTheme + DraftPeekSpacing + BrandShapes 取代。
 *       保留此文件是为了运行时兼容性，防止遗漏的引用导致崩溃。
 *       所有 @Deprecated 注解指向新的替代方案，未来验证运行时无引用后可安全删除。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.BackgroundDark
import com.draftpeek.core.ui.theme.BackgroundLight
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PageBackgroundLight
import com.draftpeek.core.ui.theme.SurfaceDark
import com.draftpeek.core.ui.theme.SurfaceLight
import com.draftpeek.core.ui.theme.SurfaceVariantLight

// ============================================================
// DEPRECATED: This file defines a parallel design system that
// duplicates tokens already in core/ui/theme/. All consumers
// have been migrated to MaterialTheme + DraftPeekSpacing +
// BrandShapes. Kept temporarily to avoid breaking any runtime
// references we may have missed. Safe to delete in a future
// cleanup pass once the module is verified at runtime.
// ============================================================

/**
 * 统计页面颜色令牌（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.colorScheme 替代
 */
@Deprecated(
    message = "Use MaterialTheme.colorScheme instead of StatsColors",
    replaceWith = ReplaceWith(
        expression = "MaterialTheme.colorScheme",
        imports = ["androidx.compose.material3.MaterialTheme"],
    ),
)
@Immutable
data class StatsColors(
    /** 卡片背景色 */
    val cardBackground: Color,
    /** 卡片边框色 */
    val cardBorder: Color,
    /** 主要文字颜色 */
    val textPrimary: Color,
    /** 次要文字颜色 */
    val textSecondary: Color,
    /** 三级文字颜色（辅助说明） */
    val textTertiary: Color,
    /** 统计卡片背景色 */
    val statCardBackground: Color,
    /** Tab 指示器颜色 */
    val tabIndicator: Color,
    /** 选中芯片背景色 */
    val chipSelectedBackground: Color,
    /** 选中芯片文字颜色 */
    val chipSelectedLabel: Color,
    /** 未选中芯片文字颜色 */
    val chipUnselectedLabel: Color,
    /** 年份按钮背景色 */
    val yearButtonBackground: Color,
    /** 年份按钮文字颜色 */
    val yearButtonLabel: Color,
    /** 空状态图标颜色 */
    val emptyStateIcon: Color,
    /** 屏幕背景色 */
    val screenBackground: Color,
) {
    companion object {
        /** 亮色主题颜色配置 */
        val Light = StatsColors(
            cardBackground = SurfaceLight,
            cardBorder = Color(0xFFC3C7D0),
            textPrimary = Color(0xFF1B2838),
            textSecondary = Color(0xFF434A58),
            textTertiary = Color(0xFF737A88),
            statCardBackground = SurfaceVariantLight,
            tabIndicator = Color(0xFFC41E3A),
            chipSelectedBackground = Color(0xFFFCE8EC),
            chipSelectedLabel = Color(0xFFC41E3A),
            chipUnselectedLabel = Color(0xFF737A88),
            yearButtonBackground = Color(0xFFDBE3F6),
            yearButtonLabel = Color(0xFF3178C6),
            emptyStateIcon = Color(0xFFB0BEC5),
            screenBackground = PageBackgroundLight,
        )

        /** 暗色主题颜色配置 */
        val Dark = StatsColors(
            cardBackground = SurfaceDark,
            cardBorder = Color(0xFF354359),
            textPrimary = Color(0xFFE8EDF2),
            textSecondary = Color(0xFFBFC6D2),
            textTertiary = Color(0xFF8A919D),
            statCardBackground = BackgroundDark,
            tabIndicator = Color(0xFFE05555),
            chipSelectedBackground = Color(0xFF5E131F),
            chipSelectedLabel = Color(0xFFE05555),
            chipUnselectedLabel = Color(0xFF8A919D),
            yearButtonBackground = Color(0xFF1B3A5C),
            yearButtonLabel = Color(0xFF5A9BD5),
            emptyStateIcon = Color(0xFF6E7681),
            screenBackground = BackgroundDark,
        )
    }
}

/**
 * 统计页面排版令牌（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.typography 替代
 */
@Deprecated(
    message = "Use MaterialTheme.typography instead of StatsTypography",
    replaceWith = ReplaceWith(
        expression = "MaterialTheme.typography",
        imports = ["androidx.compose.material3.MaterialTheme"],
    ),
)
@Immutable
data class StatsTypography(
    val sectionTitle: TextStyle,
    val tabLabel: TextStyle,
    val tabLabelSelected: TextStyle,
    val chipLabel: TextStyle,
    val chipLabelSelected: TextStyle,
    val statTitle: TextStyle,
    val statValue: TextStyle,
    val monthLabel: TextStyle,
    val dayLabel: TextStyle,
    val caption: TextStyle,
    val yearButton: TextStyle,
    val contributionTitle: TextStyle,
) {
    companion object {
        /** 默认排版配置 */
        val Default = StatsTypography(
            sectionTitle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
            ),
            tabLabel = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
            ),
            tabLabelSelected = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            ),
            chipLabel = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
            ),
            chipLabelSelected = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
            ),
            statTitle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
            ),
            statValue = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
            ),
            monthLabel = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
            ),
            dayLabel = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
            ),
            caption = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
            ),
            yearButton = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            ),
            contributionTitle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            ),
        )
    }
}

/**
 * 统计页面间距令牌（已废弃）。
 *
 * @deprecated 请使用 DraftPeekSpacing 替代
 */
@Deprecated(
    message = "Use DraftPeekSpacing from core/ui/theme instead of StatsSpacing",
    replaceWith = ReplaceWith(
        expression = "DraftPeekSpacing",
        imports = ["com.draftpeek.core.ui.theme.DraftPeekSpacing"],
    ),
)
object StatsSpacing {
    val cardPadding = 16.dp
    val cardGap = 12.dp
    val statCardGap = 8.dp
    val chipGap = 8.dp
    val tabGap = 20.dp
    val sectionGap = 12.dp
    val cellGap = 3.dp
    val labelGap = 6.dp
}

/**
 * 统计页面圆角令牌（已废弃）。
 *
 * @deprecated 请使用 BrandShapes 替代
 */
@Deprecated(
    message = "Use BrandShapes from core/ui/theme instead of StatsRadius",
    replaceWith = ReplaceWith(
        expression = "BrandShapes",
        imports = ["com.draftpeek.core.ui.theme.BrandShapes"],
    ),
)
object StatsRadius {
    val card = 12.dp
    val statCard = 10.dp
    val chip = 8.dp
    val button = 10.dp
    val cell = 2.dp
    val tabIndicator = 1.5.dp
}

/**
 * 统计页面颜色 CompositionLocal（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.colorScheme 替代
 */
@Deprecated("Use MaterialTheme.colorScheme instead", level = DeprecationLevel.WARNING)
val LocalStatsColors = staticCompositionLocalOf { StatsColors.Light }

/**
 * 统计页面排版 CompositionLocal（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.typography 替代
 */
@Deprecated("Use MaterialTheme.typography instead", level = DeprecationLevel.WARNING)
val LocalStatsTypography = staticCompositionLocalOf { StatsTypography.Default }

/**
 * 获取当前主题的 StatsColors（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.colorScheme 替代
 */
@Deprecated(
    message = "Use MaterialTheme.colorScheme instead of statsColors()",
    replaceWith = ReplaceWith(
        expression = "MaterialTheme.colorScheme",
        imports = ["androidx.compose.material3.MaterialTheme"],
    ),
)
@Composable
fun statsColors(): StatsColors {
    val isDark = LocalDarkTheme.current
    return if (isDark) StatsColors.Dark else StatsColors.Light
}

/**
 * 获取当前主题的 StatsTypography（已废弃）。
 *
 * @deprecated 请使用 MaterialTheme.typography 替代
 */
@Deprecated(
    message = "Use MaterialTheme.typography instead of statsTypography()",
    replaceWith = ReplaceWith(
        expression = "MaterialTheme.typography",
        imports = ["androidx.compose.material3.MaterialTheme"],
    ),
)
@Composable
fun statsTypography(): StatsTypography = LocalStatsTypography.current
