@file:Suppress("UNUSED")

/**
 * 高对比度配色方案。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 提供高对比度主题配色，满足无障碍访问需求。
 */

package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.HighContrastScheme instead",
    replaceWith = ReplaceWith("HighContrastScheme", "com.draftpeek.core.designsystem.theme")
)
typealias HighContrastScheme = com.draftpeek.core.designsystem.theme.HighContrastScheme
