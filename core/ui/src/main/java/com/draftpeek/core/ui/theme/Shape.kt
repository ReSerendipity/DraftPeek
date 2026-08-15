@file:Suppress("UNUSED")

/**
 * DraftPeek 应用形状定义。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含 Material3 Shapes 配置、品牌形状和原型形状定义，统一控制组件圆角样式。
 */
package com.draftpeek.core.ui.theme

import androidx.compose.material3.Shapes

/**
 * Backward-compatible re-exports from [com.draftpeek.core.designsystem.theme].
 * New code should import directly from the designsystem module.
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.Shapes instead", ReplaceWith("Shapes", "com.draftpeek.core.designsystem.theme"))
val Shapes: Shapes get() = com.draftpeek.core.designsystem.theme.Shapes

@Deprecated("Use com.draftpeek.core.designsystem.theme.BrandShapes instead", ReplaceWith("BrandShapes", "com.draftpeek.core.designsystem.theme"))
val BrandShapes = com.draftpeek.core.designsystem.theme.BrandShapes

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrototypeShapes instead", ReplaceWith("PrototypeShapes", "com.draftpeek.core.designsystem.theme"))
val PrototypeShapes = com.draftpeek.core.designsystem.theme.PrototypeShapes
