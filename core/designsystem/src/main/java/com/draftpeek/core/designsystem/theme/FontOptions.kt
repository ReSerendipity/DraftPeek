/**
 * 字体选项配置文件。
 *
 * 提供应用支持的字体列表，包括等宽代码字体和UI字体选项，
 * 支持JetBrains Mono、系统字体、Inter、Noto Sans CJK等多种字体。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * 字体选项数据类。
 * @property id 字体唯一标识符
 * @property displayNameResId 显示名称字符串资源ID
 * @property descriptionResId 描述字符串资源ID
 * @property fontFamily Compose字体族实例
 * @property isMonospace 是否为等宽字体
 * @property typefaceName 字体类型名称（如"monospace"、"sans-serif"）
 */
@Immutable
data class FontOption(
    val id: String,
    val displayNameResId: Int,
    val descriptionResId: Int,
    val fontFamily: FontFamily,
    val isMonospace: Boolean,
    val typefaceName: String? = null
)

/**
 * 字体选项管理器。
 * 提供所有可用字体的列表，以及根据ID查找字体的方法。
 */
object FontOptions {
    private val JetBrainsMono = FontFamily(
        androidx.compose.ui.text.font.Font(com.draftpeek.core.designsystem.R.font.jetbrains_mono_regular),
        androidx.compose.ui.text.font.Font(com.draftpeek.core.designsystem.R.font.jetbrains_mono_medium, FontWeight.Medium),
        androidx.compose.ui.text.font.Font(com.draftpeek.core.designsystem.R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    )

    val allOptions: List<FontOption> = listOf(
        FontOption(
            id = "jetbrains_mono",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_jetbrains_mono,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_jetbrains_mono_desc,
            fontFamily = JetBrainsMono,
            isMonospace = true,
            typefaceName = "monospace"
        ),
        FontOption(
            id = "system_mono",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_system_mono,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_system_mono_desc,
            fontFamily = FontFamily.Monospace,
            isMonospace = true,
            typefaceName = "monospace"
        ),
        FontOption(
            id = "inter",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_inter,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_inter_desc,
            fontFamily = InterFontFamily,
            isMonospace = false,
            typefaceName = "sans-serif"
        ),
        FontOption(
            id = "noto_sans_cjk",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_noto_sans_cjk,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_noto_sans_cjk_desc,
            fontFamily = FontFamily.SansSerif,
            isMonospace = false,
            typefaceName = "sans-serif"
        ),
        FontOption(
            id = "microsoft_yahei",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_microsoft_yahei,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_microsoft_yahei_desc,
            fontFamily = FontFamily.SansSerif,
            isMonospace = false,
            typefaceName = "sans-serif-medium"
        ),
        FontOption(
            id = "pingfang_sc",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_pingfang_sc,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_pingfang_sc_desc,
            fontFamily = FontFamily.SansSerif,
            isMonospace = false,
            typefaceName = "sans-serif"
        ),
        FontOption(
            id = "simsun",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_simsun,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_simsun_desc,
            fontFamily = FontFamily.Serif,
            isMonospace = false,
            typefaceName = "serif"
        ),
        FontOption(
            id = "kaiti",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_kaiti,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_kaiti_desc,
            fontFamily = FontFamily.Cursive,
            isMonospace = false,
            typefaceName = "cursive"
        ),
        FontOption(
            id = "system_sans",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_system_sans,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_system_sans_desc,
            fontFamily = FontFamily.SansSerif,
            isMonospace = false,
            typefaceName = "sans-serif"
        ),
        FontOption(
            id = "system_serif",
            displayNameResId = com.draftpeek.core.designsystem.R.string.font_system_serif,
            descriptionResId = com.draftpeek.core.designsystem.R.string.font_system_serif_desc,
            fontFamily = FontFamily.Serif,
            isMonospace = false,
            typefaceName = "serif"
        )
    )

    val codeFonts: List<FontOption> = allOptions.filter { it.isMonospace }
    val uiFonts: List<FontOption> = allOptions.filter { !it.isMonospace }

    /** 默认代码字体：JetBrains Mono */
    val defaultCodeFont: FontOption = allOptions.first { it.id == "jetbrains_mono" }
    /** 默认UI字体：Inter */
    val defaultUiFont: FontOption = allOptions.first { it.id == "inter" }

    /**
     * 根据ID查找字体选项。
     * @param id 字体ID
     * @return 找到的字体选项，未找到时返回默认代码字体
     */
    fun getById(id: String): FontOption {
        return allOptions.find { it.id == id } ?: defaultCodeFont
    }

    /**
     * 根据ID查找代码字体选项。
     * @param id 字体ID
     * @return 找到的字体选项，未找到时返回默认代码字体
     */
    fun getCodeFontById(id: String): FontOption {
        return codeFonts.find { it.id == id } ?: defaultCodeFont
    }

    /**
     * 根据ID查找UI字体选项。
     * @param id 字体ID
     * @return 找到的字体选项，未找到时返回默认UI字体
     */
    fun getUiFontById(id: String): FontOption {
        return uiFonts.find { it.id == id } ?: defaultUiFont
    }

    /**
     * 根据旧版ID兼容查找字体选项。
     * 用于从旧版本设置迁移时的兼容处理。
     * @param legacyId 旧版字体标识符（如"monospace"、"sans-serif"、"serif"）
     * @return 对应的字体选项
     */
    fun getByLegacyId(legacyId: String): FontOption {
        return when (legacyId) {
            "monospace" -> defaultCodeFont
            "sans-serif" -> allOptions.first { it.id == "system_sans" }
            "serif" -> allOptions.first { it.id == "system_serif" }
            else -> defaultCodeFont
        }
    }
}
