@file:Suppress("UNUSED")

/**
 * 色盲模拟辅助工具。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 提供色盲模拟和色觉安全调色板功能，帮助设计无障碍配色方案。
 */
package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.ColorBlindnessHelper instead",
    replaceWith = ReplaceWith("ColorBlindnessHelper", "com.draftpeek.core.designsystem.theme")
)
typealias ColorBlindnessHelper = com.draftpeek.core.designsystem.theme.ColorBlindnessHelper

@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.ColorBlindSafePalette instead",
    replaceWith = ReplaceWith("ColorBlindSafePalette", "com.draftpeek.core.designsystem.theme")
)
typealias ColorBlindSafePalette = com.draftpeek.core.designsystem.theme.ColorBlindSafePalette
