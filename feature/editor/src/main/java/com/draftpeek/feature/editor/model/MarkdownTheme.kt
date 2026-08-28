/**
 * 文件：MarkdownTheme.kt
 * 功能：Markdown 预览主题枚举定义
 * 主要类/接口：MarkdownTheme
 * 模块依赖：
 *   - androidx.annotation：字符串资源注解
 *   - feature/editor/R：编辑器模块资源
 */
package com.draftpeek.feature.editor.model

import androidx.annotation.StringRes
import com.draftpeek.feature.editor.R

/**
 * Markdown 预览可用主题枚举。
 *
 * 提供多种 CSS 主题以适配不同阅读场景和用户偏好：
 * - DEFAULT：默认主题，简洁清爽
 * - GITHUB：GitHub 风格，开发者熟悉
 * - NEWSPRINT：报纸风格，适合长文阅读
 * - NIGHT：夜间暗色主题，护眼
 * - PIXY：可爱风格主题
 * - ACADEMIC：学术风格，适合论文/文档
 *
 * @property displayNameResId 主题显示名称的字符串资源 ID
 */
enum class MarkdownTheme(@param:StringRes val displayNameResId: Int) {
    /** 默认主题 */
    DEFAULT(R.string.editor_theme_default),

    /** GitHub 风格主题 */
    GITHUB(R.string.editor_theme_github),

    /** 报纸风格主题 */
    NEWSPRINT(R.string.editor_theme_newsprint),

    /** 夜间暗色主题 */
    NIGHT(R.string.editor_theme_night),

    /** Pixy 可爱风格主题 */
    PIXY(R.string.editor_theme_pixy),

    /** 学术风格主题 */
    ACADEMIC(R.string.editor_theme_academic)
}
