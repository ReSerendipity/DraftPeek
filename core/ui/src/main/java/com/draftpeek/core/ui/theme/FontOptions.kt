@file:Suppress("UNUSED")

/**
 * DraftPeek 字体选项配置。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含字体选项枚举和配置，支持用户自定义应用字体。
 */
package com.draftpeek.core.ui.theme



/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.FontOption instead", ReplaceWith("FontOption", "com.draftpeek.core.designsystem.theme"))
typealias FontOption = com.draftpeek.core.designsystem.theme.FontOption

@Deprecated("Use com.draftpeek.core.designsystem.theme.FontOptions instead", ReplaceWith("FontOptions", "com.draftpeek.core.designsystem.theme"))
val FontOptions = com.draftpeek.core.designsystem.theme.FontOptions
