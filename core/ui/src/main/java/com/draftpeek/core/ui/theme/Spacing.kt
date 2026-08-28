@file:Suppress("UNUSED")

/**
 * DraftPeek 应用间距常量。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 定义应用中统一使用的间距值，包括按钮高度、内边距、组件间距等设计令牌。
 */

package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    "Use com.draftpeek.core.designsystem.theme.DraftPeekSpacing instead",
    ReplaceWith("DraftPeekSpacing", "com.draftpeek.core.designsystem.theme")
)
val DraftPeekSpacing = com.draftpeek.core.designsystem.theme.DraftPeekSpacing

@Deprecated(
    "Use com.draftpeek.core.designsystem.theme.PrototypeSpacing instead",
    ReplaceWith("PrototypeSpacing", "com.draftpeek.core.designsystem.theme")
)
val PrototypeSpacing = com.draftpeek.core.designsystem.theme.PrototypeSpacing
