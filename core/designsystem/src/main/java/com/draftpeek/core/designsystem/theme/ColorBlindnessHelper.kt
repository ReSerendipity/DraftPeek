/**
 * 色盲色彩转换工具文件。
 *
 * 基于LMS色彩空间的模拟矩阵，实现红色盲、绿色盲、蓝色盲的颜色模拟，
 * 帮助开发者预览色盲用户的视觉效果，并提供色盲安全调色板。
 *
 * 算法来源：Vischeck / Brettel et al. (1997) 和 Machado et al. (2009)
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * 色盲友好色彩转换工具。
 *
 * 基于 LMS 色彩空间的模拟矩阵，将原始颜色转换为色盲用户所感知的颜色。
 * 这使得开发者可以预览界面在色盲用户眼中的效果，并据此调整配色方案。
 *
 * 矩阵来源：Vischeck / Brettel et al. (1997) 和 Machado et al. (2009) 的色盲模拟算法。
 */
object ColorBlindnessHelper {

    /**
     * 根据色盲模式转换颜色。
     *
     * @param color 原始颜色
     * @param mode 色盲模式
     * @return 色盲用户感知到的颜色
     */
    fun transform(color: Color, mode: ColorBlindMode): Color {
        if (mode == ColorBlindMode.NONE) return color

        val r = color.red
        val g = color.green
        val b = color.blue

        val (nr, ng, nb) = when (mode) {
            ColorBlindMode.PROTANOPIA -> {
                // 红色盲：L 锥缺失，红绿混淆
                Triple(
                    0.152286f * r + 1.052583f * g - 0.204868f * b,
                    0.114503f * r + 0.786281f * g + 0.099216f * b,
                    -0.003882f * r - 0.048116f * g + 1.051998f * b,
                )
            }
            ColorBlindMode.DEUTERANOPIA -> {
                // 绿色盲：M 锥缺失，红绿混淆（最常见）
                Triple(
                    0.367322f * r + 0.860646f * g - 0.227968f * b,
                    0.280085f * r + 0.672501f * g + 0.047413f * b,
                    -0.011820f * r - 0.042940f * g + 1.054760f * b,
                )
            }
            ColorBlindMode.TRITANOPIA -> {
                // 蓝色盲：S 锥缺失，蓝黄混淆
                Triple(
                    1.255528f * r - 0.076749f * g - 0.178779f * b,
                    -0.078411f * r + 0.930809f * g + 0.147602f * b,
                    0.004733f * r + 0.691367f * g + 0.303900f * b,
                )
            }
            else -> Triple(r, g, b)
        }

        return Color(
            red = nr.coerceIn(0f, 1f),
            green = ng.coerceIn(0f, 1f),
            blue = nb.coerceIn(0f, 1f),
            alpha = color.alpha,
        )
    }

    /**
     * 批量转换颜色列表。
     */
    fun transformAll(colors: List<Color>, mode: ColorBlindMode): List<Color> =
        colors.map { transform(it, mode) }
}

/**
 * 红绿色盲安全配色方案。
 * 当色盲模式启用时，提供替代的高区分度颜色对。
 */
object ColorBlindSafePalette {
    /** 成功状态色 — 蓝色系（替代绿色，红绿色盲可辨识） */
    val Success = Color(0xFF0072B2)

    /** 警告状态色 — 橙色系（红绿色盲可辨识） */
    val Warning = Color(0xFFE69F00)

    /** 危险/错误状态色 — 深红朱砂色（配合非色彩标识使用） */
    val Danger = Color(0xFFD55E00)

    /** 信息状态色 — 天蓝色 */
    val Info = Color(0xFF56B4E9)

    /**
     * 色盲友好的文件类型颜色映射。
     * 使用 Okabe-Ito 色盲安全调色板作为基础。
     */
    val safeFileColors = mapOf(
        "kotlin" to Color(0xFFCC79A7),
        "java" to Color(0xFFE69F00),
        "javascript" to Color(0xFFF0E442),
        "typescript" to Color(0xFF0072B2),
        "python" to Color(0xFF009E73),
        "html" to Color(0xFFD55E00),
        "css" to Color(0xFF56B4E9),
        "json" to Color(0xFFCC79A7),
        "markdown" to Color(0xFFD55E00),
        "default" to Color(0xFF0072B2),
    )

    /**
     * 根据文件扩展名获取色盲安全颜色。
     */
    fun colorForExtension(ext: String): Color {
        return safeFileColors[ext.lowercase()] ?: safeFileColors["default"]!!
    }
}
