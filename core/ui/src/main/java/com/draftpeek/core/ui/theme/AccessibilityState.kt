@file:Suppress("UNUSED")

/**
 * DraftPeek 无障碍状态管理。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含无障碍状态数据类、CompositionLocal 和便捷访问属性，
 * 用于支持高对比度、大字体等辅助功能。
 */

package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.AccessibilityState instead",
    replaceWith = ReplaceWith("AccessibilityState", "com.draftpeek.core.designsystem.theme")
)
typealias AccessibilityState = com.draftpeek.core.designsystem.theme.AccessibilityState

@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.LocalAccessibilityState instead",
    replaceWith = ReplaceWith("LocalAccessibilityState", "com.draftpeek.core.designsystem.theme")
)
val LocalAccessibilityState = com.draftpeek.core.designsystem.theme.LocalAccessibilityState

@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.accessibilityState instead",
    replaceWith = ReplaceWith("accessibilityState", "com.draftpeek.core.designsystem.theme")
)
val accessibilityState: com.draftpeek.core.designsystem.theme.AccessibilityState
    @androidx.compose.runtime.Composable
    get() = com.draftpeek.core.designsystem.theme.accessibilityState
