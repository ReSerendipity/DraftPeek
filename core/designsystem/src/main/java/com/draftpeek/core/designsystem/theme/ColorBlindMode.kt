/**
 * 色盲类型枚举定义文件。
 *
 * 定义红色盲、绿色盲、蓝色盲三种色盲类型，用于无障碍色彩适配。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

/**
 * 色盲类型枚举，用于无障碍色彩适配。
 *
 * - [NONE] 正常视觉，不做色彩调整
 * - [PROTANOPIA] 红色盲（缺乏 L 锥细胞，红色与绿色难以区分）
 * - [DEUTERANOPIA] 绿色盲（缺乏 M 锥细胞，红绿色难以区分，最常见的色盲类型）
 * - [TRITANOPIA] 蓝色盲（缺乏 S 锥细胞，蓝色与黄色难以区分，较罕见）
 */
enum class ColorBlindMode {
    NONE,
    PROTANOPIA,
    DEUTERANOPIA,
    TRITANOPIA;

    companion object {
        fun fromName(name: String?): ColorBlindMode = entries.find { it.name == name } ?: NONE
    }
}
