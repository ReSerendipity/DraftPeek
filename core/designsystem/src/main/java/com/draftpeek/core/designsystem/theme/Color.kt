/**
 * DraftPeek 品牌颜色系统定义文件。
 *
 * 提供浅色/深色两套完整的 Material 3 色彩方案、文件类型图标颜色、
 * 语义化颜色（成功/警告/错误/信息）、热力图颜色以及彩虹色选择器。
 *
 * 设计理念：现代极简风格 + 朱砂红(#C41E3A)功能强调色，
 * 冷灰色中性基调，确保长时间代码编辑时的视觉舒适度。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// DraftPeek Brand Color System — Modern Minimal + Cinnabar Accent
// Cold gray neutral base + Cinnabar red (#C41E3A) functional accent
// ============================================================

// ---- Light Theme ----

val PrimaryLight = Color(0xFFC41E3A)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFCE8EC)
val OnPrimaryContainerLight = Color(0xFF7F0F1F)

val SecondaryLight = Color(0xFF3178C6)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFDBE3F6)
val OnSecondaryContainerLight = Color(0xFF0A2540)

val TertiaryLight = Color(0xFF059669)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD2F5E2)
val OnTertiaryContainerLight = Color(0xFF002110)

val ErrorLight = Color(0xFFFF3B30)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFEBEE)
val OnErrorContainerLight = Color(0xFF7F0000)

val BackgroundLight = Color(0xFFF5F5F5)
val OnBackgroundLight = Color(0xFF18181B)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF15151A)
val SurfaceVariantLight = Color(0xFFFAFAFA)
val OnSurfaceVariantLight = Color(0xFF3A3A42)
val OutlineLight = Color(0xFFE5E5E5)
val OutlineVariantLight = Color(0xFFEDEDED)
val InverseSurfaceLight = Color(0xFF303848)
val InverseOnSurfaceLight = Color(0xFFF0F2F5)
val InversePrimaryLight = Color(0xFFFFB3B8)
val SurfaceTintLight = PrimaryLight

// ---- Dark Theme ----

val PrimaryDark = Color(0xFFE8A838)
val OnPrimaryDark = Color(0xFFFFFFFF)
val PrimaryContainerDark = Color(0xFF3D2D15)
val OnPrimaryContainerDark = Color(0xFFFFD9D9)

val SecondaryDark = Color(0xFF5A9BD5)
val OnSecondaryDark = Color(0xFF00335C)
val SecondaryContainerDark = Color(0xFF0D3E70)
val OnSecondaryContainerDark = Color(0xFFD3E5F9)

val TertiaryDark = Color(0xFF4ADE80)
val OnTertiaryDark = Color(0xFF00391D)
val TertiaryContainerDark = Color(0xFF005229)
val OnTertiaryContainerDark = Color(0xFFB9F5C8)

val ErrorDark = Color(0xFFFF6961)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF0A0A0A)
val OnBackgroundDark = Color(0xFFEDEDED)
val SurfaceDark = Color(0xFF141416)
val OnSurfaceDark = Color(0xFFF0F0F2)
val SurfaceVariantDark = Color(0xFF1C1C1C)
val OnSurfaceVariantDark = Color(0xFFC0C0C8)
val OutlineDark = Color(0xFF282828)
val OutlineVariantDark = Color(0xFF1F1F1F)
val InverseSurfaceDark = Color(0xFFE8EDF2)
val InverseOnSurfaceDark = Color(0xFF1B2838)
val InversePrimaryDark = Color(0xFFC41E3A)
val SurfaceTintDark = PrimaryDark

// ============================================================
// File Type Colors — Light Mode
// ============================================================

/**
 * 浅色主题下的文件类型图标颜色定义。
 * 为每种编程语言和文档格式提供品牌识别色。
 */
object FileTypeColorsLight {
    val JavaScript = Color(0xFFF7DF1E)
    val TypeScript = Color(0xFF3178C6)
    val Python = Color(0xFF059669)
    val Ruby = Color(0xFFCC342D)
    val Php = Color(0xFF777BB4)
    val Dart = Color(0xFF00B4AB)
    val Java = Color(0xFFED8B00)
    val Kotlin = Color(0xFF7F52FF)
    val Swift = Color(0xFFF05138)
    val Go = Color(0xFF00ADD8)
    val Rust = Color(0xFFCE422B)
    val C = Color(0xFFA8B9CC)
    val Cpp = Color(0xFF00599C)
    val CSharp = Color(0xFF239120)
    val Scala = Color(0xFFDC322F)
    val Html = Color(0xFFE34F26)
    val Css = Color(0xFF42A5F5)
    val Shell = Color(0xFF4EAA25)
    val Lua = Color(0xFF000080)
    val Groovy = Color(0xFF4298B8)
    val R = Color(0xFF276DC3)
    val Json = Color(0xFFD97706)
    val Yaml = Color(0xFFCB171E)
    val Xml = Color(0xFFE3722B)
    val Toml = Color(0xFF9C4121)
    val Markdown = Color(0xFFC41E3A)
    val Sql = Color(0xFFE38C00)
    val Dockerfile = Color(0xFF2496ED)
    val Pdf = Color(0xFFEC1B24)
    val Word = Color(0xFF3178C6)
    val Excel = Color(0xFF059669)
    val PowerPoint = Color(0xFFD97706)
    val Jenc = Color(0xFF6B21A8)
    val Default = Color(0xFFC41E3A)
}

// ============================================================
// File Type Colors — Dark Mode (brightened for dark backgrounds)
// ============================================================

/**
 * 深色主题下的文件类型图标颜色定义。
 * 颜色经过提亮处理以确保在深色背景上有足够的可见度。
 */
object FileTypeColorsDark {
    val JavaScript = Color(0xFFFFF176)
    val TypeScript = Color(0xFF5A9BD5)
    val Python = Color(0xFF4ADE80)
    val Ruby = Color(0xFFFF8A80)
    val Php = Color(0xFFB39DDB)
    val Dart = Color(0xFF4DD0E1)
    val Java = Color(0xFFFFB74D)
    val Kotlin = Color(0xFFB388FF)
    val Swift = Color(0xFFFF8A65)
    val Go = Color(0xFF4DD0E1)
    val Rust = Color(0xFFFFAB91)
    val C = Color(0xFFB0BEC5)
    val Cpp = Color(0xFF64B5F6)
    val CSharp = Color(0xFF81C784)
    val Scala = Color(0xFFFF8A80)
    val Html = Color(0xFFFF8A65)
    val Css = Color(0xFF60A5FA)
    val Shell = Color(0xFFA5D610)
    val Lua = Color(0xFF7986CB)
    val Groovy = Color(0xFF80DEEA)
    val R = Color(0xFF64B5F6)
    val Json = Color(0xFFFBBF24)
    val Yaml = Color(0xFFEF9A9A)
    val Xml = Color(0xFFFF8A65)
    val Toml = Color(0xFFBCAAA4)
    val Markdown = Color(0xFFE05555)
    val Sql = Color(0xFFFFB74D)
    val Dockerfile = Color(0xFF64B5F6)
    val Pdf = Color(0xFFEF9A9A)
    val Word = Color(0xFF5A9BD5)
    val Excel = Color(0xFF4ADE80)
    val PowerPoint = Color(0xFFFBBF24)
    val Jenc = Color(0xFFA855F7)
    val Default = Color(0xFFE05555)
}

// ============================================================
// FileTypeColors — Extension lookup with light/dark support
// ============================================================

/**
 * 文件类型颜色查找工具。
 * 根据文件扩展名和当前主题模式返回对应的颜色。
 */
object FileTypeColors {
    /**
     * 根据文件扩展名获取浅色主题下的颜色。
     * @param ext 文件扩展名（不含点号）
     * @return 对应的文件类型颜色
     */
    fun forExtension(ext: String): Color = forExtension(ext, isDark = false)

    /**
     * 根据文件扩展名和主题模式获取颜色。
     * @param ext 文件扩展名（不含点号）
     * @param isDark 是否为深色主题
     * @return 对应的文件类型颜色，未找到时返回默认颜色
     */
    fun forExtension(ext: String, isDark: Boolean): Color {
        val map = if (isDark) darkMap else lightMap
        return map[ext.lowercase()] ?: (if (isDark) FileTypeColorsDark.Default else FileTypeColorsLight.Default)
    }

    private val lightMap = mapOf(
        "kt" to FileTypeColorsLight.Kotlin, "kts" to FileTypeColorsLight.Kotlin,
        "java" to FileTypeColorsLight.Java,
        "js" to FileTypeColorsLight.JavaScript, "mjs" to FileTypeColorsLight.JavaScript,
        "cjs" to FileTypeColorsLight.JavaScript,
        "ts" to FileTypeColorsLight.TypeScript, "mts" to FileTypeColorsLight.TypeScript,
        "cts" to FileTypeColorsLight.TypeScript,
        "jsx" to FileTypeColorsLight.JavaScript, "tsx" to FileTypeColorsLight.TypeScript,
        "html" to FileTypeColorsLight.Html, "htm" to FileTypeColorsLight.Html,
        "css" to FileTypeColorsLight.Css, "scss" to FileTypeColorsLight.Css, "less" to FileTypeColorsLight.Css,
        "py" to FileTypeColorsLight.Python, "pyw" to FileTypeColorsLight.Python,
        "c" to FileTypeColorsLight.C, "h" to FileTypeColorsLight.C,
        "cpp" to FileTypeColorsLight.Cpp, "cc" to FileTypeColorsLight.Cpp, "cxx" to FileTypeColorsLight.Cpp,
        "hpp" to FileTypeColorsLight.Cpp,
        "cs" to FileTypeColorsLight.CSharp, "go" to FileTypeColorsLight.Go, "rs" to FileTypeColorsLight.Rust,
        "swift" to FileTypeColorsLight.Swift,
        "rb" to FileTypeColorsLight.Ruby, "php" to FileTypeColorsLight.Php,
        "sh" to FileTypeColorsLight.Shell, "bash" to FileTypeColorsLight.Shell, "zsh" to FileTypeColorsLight.Shell,
        "fish" to FileTypeColorsLight.Shell,
        "lua" to FileTypeColorsLight.Lua, "dart" to FileTypeColorsLight.Dart,
        "groovy" to FileTypeColorsLight.Groovy, "gradle" to FileTypeColorsLight.Groovy,
        "json" to FileTypeColorsLight.Json,
        "yaml" to FileTypeColorsLight.Yaml, "yml" to FileTypeColorsLight.Yaml,
        "xml" to FileTypeColorsLight.Xml, "xsl" to FileTypeColorsLight.Xml, "xsd" to FileTypeColorsLight.Xml,
        "toml" to FileTypeColorsLight.Toml,
        "md" to FileTypeColorsLight.Markdown, "markdown" to FileTypeColorsLight.Markdown,
        "sql" to FileTypeColorsLight.Sql, "scala" to FileTypeColorsLight.Scala, "r" to FileTypeColorsLight.R,
        "dockerfile" to FileTypeColorsLight.Dockerfile,
        "pdf" to FileTypeColorsLight.Pdf,
        "doc" to FileTypeColorsLight.Word, "docx" to FileTypeColorsLight.Word,
        "xls" to FileTypeColorsLight.Excel, "xlsx" to FileTypeColorsLight.Excel,
        "ppt" to FileTypeColorsLight.PowerPoint, "pptx" to FileTypeColorsLight.PowerPoint,
        "jenc" to FileTypeColorsLight.Jenc
    )

    private val darkMap = mapOf(
        "kt" to FileTypeColorsDark.Kotlin, "kts" to FileTypeColorsDark.Kotlin,
        "java" to FileTypeColorsDark.Java,
        "js" to FileTypeColorsDark.JavaScript, "mjs" to FileTypeColorsDark.JavaScript,
        "cjs" to FileTypeColorsDark.JavaScript,
        "ts" to FileTypeColorsDark.TypeScript, "mts" to FileTypeColorsDark.TypeScript,
        "cts" to FileTypeColorsDark.TypeScript,
        "jsx" to FileTypeColorsDark.JavaScript, "tsx" to FileTypeColorsDark.TypeScript,
        "html" to FileTypeColorsDark.Html, "htm" to FileTypeColorsDark.Html,
        "css" to FileTypeColorsDark.Css, "scss" to FileTypeColorsDark.Css, "less" to FileTypeColorsDark.Css,
        "py" to FileTypeColorsDark.Python, "pyw" to FileTypeColorsDark.Python,
        "c" to FileTypeColorsDark.C, "h" to FileTypeColorsDark.C,
        "cpp" to FileTypeColorsDark.Cpp, "cc" to FileTypeColorsDark.Cpp, "cxx" to FileTypeColorsDark.Cpp,
        "hpp" to FileTypeColorsDark.Cpp,
        "cs" to FileTypeColorsDark.CSharp, "go" to FileTypeColorsDark.Go, "rs" to FileTypeColorsDark.Rust,
        "swift" to FileTypeColorsDark.Swift,
        "rb" to FileTypeColorsDark.Ruby, "php" to FileTypeColorsDark.Php,
        "sh" to FileTypeColorsDark.Shell, "bash" to FileTypeColorsDark.Shell, "zsh" to FileTypeColorsDark.Shell,
        "fish" to FileTypeColorsDark.Shell,
        "lua" to FileTypeColorsDark.Lua, "dart" to FileTypeColorsDark.Dart,
        "groovy" to FileTypeColorsDark.Groovy, "gradle" to FileTypeColorsDark.Groovy,
        "json" to FileTypeColorsDark.Json,
        "yaml" to FileTypeColorsDark.Yaml, "yml" to FileTypeColorsDark.Yaml,
        "xml" to FileTypeColorsDark.Xml, "xsl" to FileTypeColorsDark.Xml, "xsd" to FileTypeColorsDark.Xml,
        "toml" to FileTypeColorsDark.Toml,
        "md" to FileTypeColorsDark.Markdown, "markdown" to FileTypeColorsDark.Markdown,
        "sql" to FileTypeColorsDark.Sql, "scala" to FileTypeColorsDark.Scala, "r" to FileTypeColorsDark.R,
        "dockerfile" to FileTypeColorsDark.Dockerfile,
        "pdf" to FileTypeColorsDark.Pdf,
        "doc" to FileTypeColorsDark.Word, "docx" to FileTypeColorsDark.Word,
        "xls" to FileTypeColorsDark.Excel, "xlsx" to FileTypeColorsDark.Excel,
        "ppt" to FileTypeColorsDark.PowerPoint, "pptx" to FileTypeColorsDark.PowerPoint,
        "jenc" to FileTypeColorsDark.Jenc
    )
}

// ============================================================
// Semantic Colors
// ============================================================

/**
 * 语义化颜色集合。
 * 用于表示操作状态和特定含义（成功、警告、危险、信息等）。
 */
object SemanticColors {
    val Success = Color(0xFF34C759)
    val Warning = Color(0xFFFF9F0A)
    val Danger = Color(0xFFFF3B30)
    val Info = Color(0xFF0A84FF)
    val Favorite = Color(0xFFC41E3A)
    val Folder = Color(0xFF2E9E5E)
}

/**
 * Folder/directory icon tint colors for light and dark themes.
 */
val FolderColorLight = Color(0xFF2E9E5E)
val FolderColorDark = Color(0xFF3CB371)
val FolderContainerLight = Color(0xFFD6F5E3)
val FolderContainerDark = Color(0xFF0F3D24)

// ============================================================
// Prototype Design Tokens — Additional semantic colors
// ============================================================

// Page-level background (behind everything)
val PageBackgroundLight = Color(0xFFEFEFEF)
val PageBackgroundDark = Color(0xFF080808)

// Accent soft (for pill backgrounds, badges, highlighted states)
val AccentSoftLight = Color(0x1AC41E3A) // rgba(196,30,58,0.10)
val AccentSoftDark = Color(0x1FE8A838) // rgba(232,168,56,0.12)

// Fg-soft (secondary text color)
val FgSoftLight = Color(0xFF3A3A42)
val FgSoftDark = Color(0xFFC0C0C8)

// Muted (tertiary text, disabled icons)
val MutedLight = Color(0xFF8E8E96)
val MutedDark = Color(0xFF787886)
val MutedSoftLight = Color(0xFFB0B0B8)
val MutedSoftDark = Color(0xFF585866)
val BorderSoftLight = Color(0xFFEDEDED)
val BorderSoftDark = Color(0xFF1F1F1F)
val SurfaceHoverLight = Color(0xFFF0F0F0)
val SurfaceHoverDark = Color(0xFF222222)

// Heatmap accent-based levels (Light theme — cinnabar red base)
val HeatL0Light = Color(0xFFF4F4F5)
val HeatL1Light = Color(0x1FC41E3A)
val HeatL2Light = Color(0x4DC41E3A)
val HeatL3Light = Color(0x80C41E3A)
val HeatL4Light = Color(0xBFC41E3A)

// Heatmap accent-based levels (Dark theme — amber/gold base)
val HeatL0Dark = Color(0xFF1C1C1F)
val HeatL1Dark = Color(0x26E8A838)
val HeatL2Dark = Color(0x59E8A838)
val HeatL3Dark = Color(0x8CE8A838)
val HeatL4Dark = Color(0xCCE8A838)

// Info banner container colors (for informational banners, distinct from error/warning)
val InfoContainerLight = Color(0xFFE3F0FF)
val OnInfoContainerLight = Color(0xFF1E40AF)
val InfoContainerDark = Color(0xFF1A3050)
val OnInfoContainerDark = Color(0xFF93C5FD)

// Icon background tint (6% of on-surface)
val IconBackgroundLight = Color(0x0F1B2838) // 27,40,56 at 6% alpha
val IconBackgroundDark = Color(0x14E8EDF2) // 232,237,242 at 8% alpha

/**
 * 返回原型设计中基于强调色的5级热力图颜色列表。
 * 使用当前主题的强调色在不同透明度下的值。
 * @param isDark 是否为深色主题
 * @return 5级热力图颜色列表，索引0为最浅（无活动），索引4为最深
 */
fun accentHeatMapColors(isDark: Boolean): List<Color> = if (isDark) {
    listOf(HeatL0Dark, HeatL1Dark, HeatL2Dark, HeatL3Dark, HeatL4Dark)
} else {
    listOf(HeatL0Light, HeatL1Light, HeatL2Light, HeatL3Light, HeatL4Light)
}

/**
 * 根据贡献级别返回对应的热力图颜色。
 * @param level 贡献活动级别
 * @param isDark 是否为深色主题
 * @return 对应级别的热力图颜色
 */
fun accentHeatMapColor(level: ContributionLevel, isDark: Boolean): Color {
    val colors = accentHeatMapColors(isDark)
    return when (level) {
        ContributionLevel.NONE -> colors[0]
        ContributionLevel.LOW -> colors[1]
        ContributionLevel.MEDIUM -> colors[2]
        ContributionLevel.HIGH -> colors[3]
        ContributionLevel.VERY_HIGH -> colors[4]
    }
}

// ============================================================
// Rainbow Activity Colors — User-selectable heatmap themes
// Provides 7 standard rainbow colors with light/dark variants.
// ============================================================

/**
 * 彩虹热力图颜色枚举，提供用户可选择的热力图主题色。
 * 包含8种标准彩虹颜色，每种颜色都有浅色和深色变体。
 *
 * @property displayName 颜色的中文显示名称
 * @property baseLight 浅色主题下的基础颜色
 * @property baseDark 深色主题下的基础颜色
 */
enum class RainbowColor(val displayName: String, val baseLight: Color, val baseDark: Color) {
    RED("红", Color(0xFFEF5350), Color(0xFFFF8A80)),
    AMBER("琥珀", Color(0xFFFFB300), Color(0xFFFFD54F)),
    ORANGE("橙", Color(0xFFFF9800), Color(0xFFFFB74D)),
    YELLOW("黄", Color(0xFFFFCA28), Color(0xFFFFE082)),
    GREEN("绿", Color(0xFF66BB6A), Color(0xFF81C784)),
    BLUE("蓝", Color(0xFF42A5F5), Color(0xFF90CAF9)),
    INDIGO("靛", Color(0xFF5C6BC0), Color(0xFF9FA8DA)),
    VIOLET("紫", Color(0xFFAB47BC), Color(0xFFCE93D8));

    companion object {
        /**
         * 根据名称查找对应的彩虹颜色。
         * @param name 颜色名称（枚举名）
         * @return 找到的颜色，未找到时返回RED
         */
        fun fromName(name: String): RainbowColor = entries.find { it.name == name } ?: RED
    }
}

/**
 * 为指定的[RainbowColor]返回5级热力图颜色刻度。
 * 索引0为最浅（无活动），索引4为基础颜色。
 * @receiver 彩虹颜色枚举值
 * @param isDark 是否为深色主题
 * @return 5级热力图颜色列表
 */
fun RainbowColor.heatMapColors(isDark: Boolean): List<Color> {
    val base = if (isDark) baseDark else baseLight
    return listOf(
        base.copy(alpha = 0.08f),
        base.copy(alpha = 0.28f),
        base.copy(alpha = 0.50f),
        base.copy(alpha = 0.75f),
        base
    )
}

/**
 * 返回特定贡献级别对应的活动颜色。
 * @receiver 彩虹颜色枚举值
 * @param level 贡献活动级别
 * @param isDark 是否为深色主题
 * @return 对应级别的热力图颜色
 */
fun RainbowColor.heatMapColor(level: ContributionLevel, isDark: Boolean): Color {
    val colors = heatMapColors(isDark)
    return when (level) {
        ContributionLevel.NONE -> colors[0]
        ContributionLevel.LOW -> colors[1]
        ContributionLevel.MEDIUM -> colors[2]
        ContributionLevel.HIGH -> colors[3]
        ContributionLevel.VERY_HIGH -> colors[4]
    }
}

/**
 * 用户活动贡献级别枚举。
 * 用于热力图显示，表示不同强度的用户活动量。
 *
 * - [NONE] 无活动
 * - [LOW] 低活动量
 * - [MEDIUM] 中等活动量
 * - [HIGH] 高活动量
 * - [VERY_HIGH] 极高活动量
 */
enum class ContributionLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}
