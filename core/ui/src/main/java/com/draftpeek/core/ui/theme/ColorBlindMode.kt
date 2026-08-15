@file:Suppress("UNUSED")

/**
 * 色盲模式枚举定义。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 定义支持的色盲模拟模式，用于帮助开发者验证配色方案的可访问性。
 */
package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.ColorBlindMode instead",
    replaceWith = ReplaceWith("ColorBlindMode", "com.draftpeek.core.designsystem.theme")
)
typealias ColorBlindMode = com.draftpeek.core.designsystem.theme.ColorBlindMode
