@file:Suppress("UNUSED")

package com.draftpeek.core.ui.theme

import androidx.compose.runtime.Composable

/**
 * DraftPeek 应用主题配置。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含主题 CompositionLocal 和主题 Composable 函数，用于配置应用的深色/浅色模式、
 * 无障碍状态和字体设置。
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.LocalDarkTheme instead", ReplaceWith("LocalDarkTheme", "com.draftpeek.core.designsystem.theme"))
val LocalDarkTheme = com.draftpeek.core.designsystem.theme.LocalDarkTheme

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrototypeTokens instead", ReplaceWith("PrototypeTokens", "com.draftpeek.core.designsystem.theme"))
val PrototypeTokens = com.draftpeek.core.designsystem.theme.PrototypeTokens

/**
 * DraftPeek 应用主题 Composable。
 *
 * 配置 Material3 主题、颜色方案、排版和形状，支持深色模式、无障碍功能和自定义字体。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param accessibilityState 无障碍状态配置，用于高对比度等辅助功能
 * @param appFonts 应用字体配置，包含界面字体和代码字体
 * @param content 主题包裹的内容 Composable
 */
@Deprecated(
    message = "Use com.draftpeek.core.designsystem.theme.DraftPeekTheme instead",
    replaceWith = ReplaceWith("DraftPeekTheme", "com.draftpeek.core.designsystem.theme")
)
@Composable
fun DraftPeekTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    accessibilityState: com.draftpeek.core.designsystem.theme.AccessibilityState = com.draftpeek.core.designsystem.theme.AccessibilityState.Default,
    appFonts: com.draftpeek.core.designsystem.theme.AppFonts = com.draftpeek.core.designsystem.theme.AppFonts(),
    content: @Composable () -> Unit,
) {
    com.draftpeek.core.designsystem.theme.DraftPeekTheme(
        darkTheme = darkTheme,
        accessibilityState = accessibilityState,
        appFonts = appFonts,
        content = content,
    )
}
