@file:Suppress("UNUSED")

/**
 * DraftPeek 应用海拔（阴影）定义。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 定义不同层级组件的阴影高度，用于表达 UI 元素的立体层次关系。
 */

package com.draftpeek.core.ui.theme

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated(
    "Use com.draftpeek.core.designsystem.theme.BrandElevation instead",
    ReplaceWith("BrandElevation", "com.draftpeek.core.designsystem.theme")
)
val BrandElevation = com.draftpeek.core.designsystem.theme.BrandElevation
