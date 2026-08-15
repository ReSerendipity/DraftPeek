@file:Suppress("UNUSED", "TopLevelPropertyNaming", "ObjectPropertyName")

package com.draftpeek.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DraftPeek 应用颜色定义。
 *
 * 本文件提供从 [com.draftpeek.core.designsystem.theme] 的向后兼容重新导出。
 * 新代码应直接从 designsystem 模块导入。
 *
 * 包含浅色/深色主题的完整 Material3 颜色方案、语义化颜色、文件类型颜色、
 * 热力图颜色以及其他原型设计令牌。
 *
 * 颜色分组：
 * - 浅色主题颜色（Light Theme）：Primary、Secondary、Tertiary、Error、Background、Surface 等
 * - 深色主题颜色（Dark Theme）：对应的深色模式颜色变体
 * - 对象与语义颜色：文件类型颜色、文件夹颜色、语义化状态颜色
 * - 原型令牌：页面背景、柔和强调色、悬停态、边框、热力图等级等
 */

// ---- 浅色主题颜色 ----

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrimaryLight instead", ReplaceWith("PrimaryLight", "com.draftpeek.core.designsystem.theme"))
val PrimaryLight: Color get() = com.draftpeek.core.designsystem.theme.PrimaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnPrimaryLight instead", ReplaceWith("OnPrimaryLight", "com.draftpeek.core.designsystem.theme"))
val OnPrimaryLight: Color get() = com.draftpeek.core.designsystem.theme.OnPrimaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrimaryContainerLight instead", ReplaceWith("PrimaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val PrimaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.PrimaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnPrimaryContainerLight instead", ReplaceWith("OnPrimaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val OnPrimaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.OnPrimaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SecondaryLight instead", ReplaceWith("SecondaryLight", "com.draftpeek.core.designsystem.theme"))
val SecondaryLight: Color get() = com.draftpeek.core.designsystem.theme.SecondaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSecondaryLight instead", ReplaceWith("OnSecondaryLight", "com.draftpeek.core.designsystem.theme"))
val OnSecondaryLight: Color get() = com.draftpeek.core.designsystem.theme.OnSecondaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SecondaryContainerLight instead", ReplaceWith("SecondaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val SecondaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.SecondaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSecondaryContainerLight instead", ReplaceWith("OnSecondaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val OnSecondaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.OnSecondaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.TertiaryLight instead", ReplaceWith("TertiaryLight", "com.draftpeek.core.designsystem.theme"))
val TertiaryLight: Color get() = com.draftpeek.core.designsystem.theme.TertiaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnTertiaryLight instead", ReplaceWith("OnTertiaryLight", "com.draftpeek.core.designsystem.theme"))
val OnTertiaryLight: Color get() = com.draftpeek.core.designsystem.theme.OnTertiaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.TertiaryContainerLight instead", ReplaceWith("TertiaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val TertiaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.TertiaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnTertiaryContainerLight instead", ReplaceWith("OnTertiaryContainerLight", "com.draftpeek.core.designsystem.theme"))
val OnTertiaryContainerLight: Color get() = com.draftpeek.core.designsystem.theme.OnTertiaryContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.ErrorLight instead", ReplaceWith("ErrorLight", "com.draftpeek.core.designsystem.theme"))
val ErrorLight: Color get() = com.draftpeek.core.designsystem.theme.ErrorLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnErrorLight instead", ReplaceWith("OnErrorLight", "com.draftpeek.core.designsystem.theme"))
val OnErrorLight: Color get() = com.draftpeek.core.designsystem.theme.OnErrorLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.ErrorContainerLight instead", ReplaceWith("ErrorContainerLight", "com.draftpeek.core.designsystem.theme"))
val ErrorContainerLight: Color get() = com.draftpeek.core.designsystem.theme.ErrorContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnErrorContainerLight instead", ReplaceWith("OnErrorContainerLight", "com.draftpeek.core.designsystem.theme"))
val OnErrorContainerLight: Color get() = com.draftpeek.core.designsystem.theme.OnErrorContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.BackgroundLight instead", ReplaceWith("BackgroundLight", "com.draftpeek.core.designsystem.theme"))
val BackgroundLight: Color get() = com.draftpeek.core.designsystem.theme.BackgroundLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnBackgroundLight instead", ReplaceWith("OnBackgroundLight", "com.draftpeek.core.designsystem.theme"))
val OnBackgroundLight: Color get() = com.draftpeek.core.designsystem.theme.OnBackgroundLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceLight instead", ReplaceWith("SurfaceLight", "com.draftpeek.core.designsystem.theme"))
val SurfaceLight: Color get() = com.draftpeek.core.designsystem.theme.SurfaceLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSurfaceLight instead", ReplaceWith("OnSurfaceLight", "com.draftpeek.core.designsystem.theme"))
val OnSurfaceLight: Color get() = com.draftpeek.core.designsystem.theme.OnSurfaceLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceVariantLight instead", ReplaceWith("SurfaceVariantLight", "com.draftpeek.core.designsystem.theme"))
val SurfaceVariantLight: Color get() = com.draftpeek.core.designsystem.theme.SurfaceVariantLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSurfaceVariantLight instead", ReplaceWith("OnSurfaceVariantLight", "com.draftpeek.core.designsystem.theme"))
val OnSurfaceVariantLight: Color get() = com.draftpeek.core.designsystem.theme.OnSurfaceVariantLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OutlineLight instead", ReplaceWith("OutlineLight", "com.draftpeek.core.designsystem.theme"))
val OutlineLight: Color get() = com.draftpeek.core.designsystem.theme.OutlineLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OutlineVariantLight instead", ReplaceWith("OutlineVariantLight", "com.draftpeek.core.designsystem.theme"))
val OutlineVariantLight: Color get() = com.draftpeek.core.designsystem.theme.OutlineVariantLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.InverseSurfaceLight instead", ReplaceWith("InverseSurfaceLight", "com.draftpeek.core.designsystem.theme"))
val InverseSurfaceLight: Color get() = com.draftpeek.core.designsystem.theme.InverseSurfaceLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.InverseOnSurfaceLight instead", ReplaceWith("InverseOnSurfaceLight", "com.draftpeek.core.designsystem.theme"))
val InverseOnSurfaceLight: Color get() = com.draftpeek.core.designsystem.theme.InverseOnSurfaceLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.InversePrimaryLight instead", ReplaceWith("InversePrimaryLight", "com.draftpeek.core.designsystem.theme"))
val InversePrimaryLight: Color get() = com.draftpeek.core.designsystem.theme.InversePrimaryLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceTintLight instead", ReplaceWith("SurfaceTintLight", "com.draftpeek.core.designsystem.theme"))
val SurfaceTintLight: Color get() = com.draftpeek.core.designsystem.theme.SurfaceTintLight

// ---- 深色主题颜色 ----

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrimaryDark instead", ReplaceWith("PrimaryDark", "com.draftpeek.core.designsystem.theme"))
val PrimaryDark: Color get() = com.draftpeek.core.designsystem.theme.PrimaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnPrimaryDark instead", ReplaceWith("OnPrimaryDark", "com.draftpeek.core.designsystem.theme"))
val OnPrimaryDark: Color get() = com.draftpeek.core.designsystem.theme.OnPrimaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.PrimaryContainerDark instead", ReplaceWith("PrimaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val PrimaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.PrimaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnPrimaryContainerDark instead", ReplaceWith("OnPrimaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val OnPrimaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.OnPrimaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SecondaryDark instead", ReplaceWith("SecondaryDark", "com.draftpeek.core.designsystem.theme"))
val SecondaryDark: Color get() = com.draftpeek.core.designsystem.theme.SecondaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSecondaryDark instead", ReplaceWith("OnSecondaryDark", "com.draftpeek.core.designsystem.theme"))
val OnSecondaryDark: Color get() = com.draftpeek.core.designsystem.theme.OnSecondaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SecondaryContainerDark instead", ReplaceWith("SecondaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val SecondaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.SecondaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSecondaryContainerDark instead", ReplaceWith("OnSecondaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val OnSecondaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.OnSecondaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.TertiaryDark instead", ReplaceWith("TertiaryDark", "com.draftpeek.core.designsystem.theme"))
val TertiaryDark: Color get() = com.draftpeek.core.designsystem.theme.TertiaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnTertiaryDark instead", ReplaceWith("OnTertiaryDark", "com.draftpeek.core.designsystem.theme"))
val OnTertiaryDark: Color get() = com.draftpeek.core.designsystem.theme.OnTertiaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.TertiaryContainerDark instead", ReplaceWith("TertiaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val TertiaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.TertiaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnTertiaryContainerDark instead", ReplaceWith("OnTertiaryContainerDark", "com.draftpeek.core.designsystem.theme"))
val OnTertiaryContainerDark: Color get() = com.draftpeek.core.designsystem.theme.OnTertiaryContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.ErrorDark instead", ReplaceWith("ErrorDark", "com.draftpeek.core.designsystem.theme"))
val ErrorDark: Color get() = com.draftpeek.core.designsystem.theme.ErrorDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnErrorDark instead", ReplaceWith("OnErrorDark", "com.draftpeek.core.designsystem.theme"))
val OnErrorDark: Color get() = com.draftpeek.core.designsystem.theme.OnErrorDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.ErrorContainerDark instead", ReplaceWith("ErrorContainerDark", "com.draftpeek.core.designsystem.theme"))
val ErrorContainerDark: Color get() = com.draftpeek.core.designsystem.theme.ErrorContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnErrorContainerDark instead", ReplaceWith("OnErrorContainerDark", "com.draftpeek.core.designsystem.theme"))
val OnErrorContainerDark: Color get() = com.draftpeek.core.designsystem.theme.OnErrorContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.BackgroundDark instead", ReplaceWith("BackgroundDark", "com.draftpeek.core.designsystem.theme"))
val BackgroundDark: Color get() = com.draftpeek.core.designsystem.theme.BackgroundDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnBackgroundDark instead", ReplaceWith("OnBackgroundDark", "com.draftpeek.core.designsystem.theme"))
val OnBackgroundDark: Color get() = com.draftpeek.core.designsystem.theme.OnBackgroundDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceDark instead", ReplaceWith("SurfaceDark", "com.draftpeek.core.designsystem.theme"))
val SurfaceDark: Color get() = com.draftpeek.core.designsystem.theme.SurfaceDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSurfaceDark instead", ReplaceWith("OnSurfaceDark", "com.draftpeek.core.designsystem.theme"))
val OnSurfaceDark: Color get() = com.draftpeek.core.designsystem.theme.OnSurfaceDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceVariantDark instead", ReplaceWith("SurfaceVariantDark", "com.draftpeek.core.designsystem.theme"))
val SurfaceVariantDark: Color get() = com.draftpeek.core.designsystem.theme.SurfaceVariantDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnSurfaceVariantDark instead", ReplaceWith("OnSurfaceVariantDark", "com.draftpeek.core.designsystem.theme"))
val OnSurfaceVariantDark: Color get() = com.draftpeek.core.designsystem.theme.OnSurfaceVariantDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OutlineDark instead", ReplaceWith("OutlineDark", "com.draftpeek.core.designsystem.theme"))
val OutlineDark: Color get() = com.draftpeek.core.designsystem.theme.OutlineDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OutlineVariantDark instead", ReplaceWith("OutlineVariantDark", "com.draftpeek.core.designsystem.theme"))
val OutlineVariantDark: Color get() = com.draftpeek.core.designsystem.theme.OutlineVariantDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.InverseSurfaceDark instead", ReplaceWith("InverseSurfaceDark", "com.draftpeek.core.designsystem.theme"))
val InverseSurfaceDark: Color get() = com.draftpeek.core.designsystem.theme.InverseSurfaceDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.InverseOnSurfaceDark instead", ReplaceWith("InverseOnSurfaceDark", "com.draftpeek.core.designsystem.theme"))
val InverseOnSurfaceDark: Color get() = com.draftpeek.core.designsystem.theme.InverseOnSurfaceDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.InversePrimaryDark instead", ReplaceWith("InversePrimaryDark", "com.draftpeek.core.designsystem.theme"))
val InversePrimaryDark: Color get() = com.draftpeek.core.designsystem.theme.InversePrimaryDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceTintDark instead", ReplaceWith("SurfaceTintDark", "com.draftpeek.core.designsystem.theme"))
val SurfaceTintDark: Color get() = com.draftpeek.core.designsystem.theme.SurfaceTintDark

// ---- 对象与语义颜色 ----

@Deprecated("Use com.draftpeek.core.designsystem.theme.FileTypeColorsLight instead", ReplaceWith("FileTypeColorsLight", "com.draftpeek.core.designsystem.theme"))
val FileTypeColorsLight = com.draftpeek.core.designsystem.theme.FileTypeColorsLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.FileTypeColorsDark instead", ReplaceWith("FileTypeColorsDark", "com.draftpeek.core.designsystem.theme"))
val FileTypeColorsDark = com.draftpeek.core.designsystem.theme.FileTypeColorsDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.FileTypeColors instead", ReplaceWith("FileTypeColors", "com.draftpeek.core.designsystem.theme"))
val FileTypeColors = com.draftpeek.core.designsystem.theme.FileTypeColors

@Deprecated("Use com.draftpeek.core.designsystem.theme.SemanticColors instead", ReplaceWith("SemanticColors", "com.draftpeek.core.designsystem.theme"))
val SemanticColors = com.draftpeek.core.designsystem.theme.SemanticColors

@Deprecated("Use com.draftpeek.core.designsystem.theme.FolderColorLight instead", ReplaceWith("FolderColorLight", "com.draftpeek.core.designsystem.theme"))
val FolderColorLight: Color get() = com.draftpeek.core.designsystem.theme.FolderColorLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.FolderColorDark instead", ReplaceWith("FolderColorDark", "com.draftpeek.core.designsystem.theme"))
val FolderColorDark: Color get() = com.draftpeek.core.designsystem.theme.FolderColorDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.FolderContainerLight instead", ReplaceWith("FolderContainerLight", "com.draftpeek.core.designsystem.theme"))
val FolderContainerLight: Color get() = com.draftpeek.core.designsystem.theme.FolderContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.FolderContainerDark instead", ReplaceWith("FolderContainerDark", "com.draftpeek.core.designsystem.theme"))
val FolderContainerDark: Color get() = com.draftpeek.core.designsystem.theme.FolderContainerDark

// ---- 原型设计令牌 ----

@Deprecated("Use com.draftpeek.core.designsystem.theme.PageBackgroundLight instead", ReplaceWith("PageBackgroundLight", "com.draftpeek.core.designsystem.theme"))
val PageBackgroundLight: Color get() = com.draftpeek.core.designsystem.theme.PageBackgroundLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.PageBackgroundDark instead", ReplaceWith("PageBackgroundDark", "com.draftpeek.core.designsystem.theme"))
val PageBackgroundDark: Color get() = com.draftpeek.core.designsystem.theme.PageBackgroundDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.AccentSoftLight instead", ReplaceWith("AccentSoftLight", "com.draftpeek.core.designsystem.theme"))
val AccentSoftLight: Color get() = com.draftpeek.core.designsystem.theme.AccentSoftLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.AccentSoftDark instead", ReplaceWith("AccentSoftDark", "com.draftpeek.core.designsystem.theme"))
val AccentSoftDark: Color get() = com.draftpeek.core.designsystem.theme.AccentSoftDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.FgSoftLight instead", ReplaceWith("FgSoftLight", "com.draftpeek.core.designsystem.theme"))
val FgSoftLight: Color get() = com.draftpeek.core.designsystem.theme.FgSoftLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.FgSoftDark instead", ReplaceWith("FgSoftDark", "com.draftpeek.core.designsystem.theme"))
val FgSoftDark: Color get() = com.draftpeek.core.designsystem.theme.FgSoftDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.MutedLight instead", ReplaceWith("MutedLight", "com.draftpeek.core.designsystem.theme"))
val MutedLight: Color get() = com.draftpeek.core.designsystem.theme.MutedLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.MutedDark instead", ReplaceWith("MutedDark", "com.draftpeek.core.designsystem.theme"))
val MutedDark: Color get() = com.draftpeek.core.designsystem.theme.MutedDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.MutedSoftLight instead", ReplaceWith("MutedSoftLight", "com.draftpeek.core.designsystem.theme"))
val MutedSoftLight: Color get() = com.draftpeek.core.designsystem.theme.MutedSoftLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.MutedSoftDark instead", ReplaceWith("MutedSoftDark", "com.draftpeek.core.designsystem.theme"))
val MutedSoftDark: Color get() = com.draftpeek.core.designsystem.theme.MutedSoftDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.BorderSoftLight instead", ReplaceWith("BorderSoftLight", "com.draftpeek.core.designsystem.theme"))
val BorderSoftLight: Color get() = com.draftpeek.core.designsystem.theme.BorderSoftLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.BorderSoftDark instead", ReplaceWith("BorderSoftDark", "com.draftpeek.core.designsystem.theme"))
val BorderSoftDark: Color get() = com.draftpeek.core.designsystem.theme.BorderSoftDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceHoverLight instead", ReplaceWith("SurfaceHoverLight", "com.draftpeek.core.designsystem.theme"))
val SurfaceHoverLight: Color get() = com.draftpeek.core.designsystem.theme.SurfaceHoverLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.SurfaceHoverDark instead", ReplaceWith("SurfaceHoverDark", "com.draftpeek.core.designsystem.theme"))
val SurfaceHoverDark: Color get() = com.draftpeek.core.designsystem.theme.SurfaceHoverDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL0Light instead", ReplaceWith("HeatL0Light", "com.draftpeek.core.designsystem.theme"))
val HeatL0Light: Color get() = com.draftpeek.core.designsystem.theme.HeatL0Light

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL1Light instead", ReplaceWith("HeatL1Light", "com.draftpeek.core.designsystem.theme"))
val HeatL1Light: Color get() = com.draftpeek.core.designsystem.theme.HeatL1Light

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL2Light instead", ReplaceWith("HeatL2Light", "com.draftpeek.core.designsystem.theme"))
val HeatL2Light: Color get() = com.draftpeek.core.designsystem.theme.HeatL2Light

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL3Light instead", ReplaceWith("HeatL3Light", "com.draftpeek.core.designsystem.theme"))
val HeatL3Light: Color get() = com.draftpeek.core.designsystem.theme.HeatL3Light

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL4Light instead", ReplaceWith("HeatL4Light", "com.draftpeek.core.designsystem.theme"))
val HeatL4Light: Color get() = com.draftpeek.core.designsystem.theme.HeatL4Light

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL0Dark instead", ReplaceWith("HeatL0Dark", "com.draftpeek.core.designsystem.theme"))
val HeatL0Dark: Color get() = com.draftpeek.core.designsystem.theme.HeatL0Dark

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL1Dark instead", ReplaceWith("HeatL1Dark", "com.draftpeek.core.designsystem.theme"))
val HeatL1Dark: Color get() = com.draftpeek.core.designsystem.theme.HeatL1Dark

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL2Dark instead", ReplaceWith("HeatL2Dark", "com.draftpeek.core.designsystem.theme"))
val HeatL2Dark: Color get() = com.draftpeek.core.designsystem.theme.HeatL2Dark

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL3Dark instead", ReplaceWith("HeatL3Dark", "com.draftpeek.core.designsystem.theme"))
val HeatL3Dark: Color get() = com.draftpeek.core.designsystem.theme.HeatL3Dark

@Deprecated("Use com.draftpeek.core.designsystem.theme.HeatL4Dark instead", ReplaceWith("HeatL4Dark", "com.draftpeek.core.designsystem.theme"))
val HeatL4Dark: Color get() = com.draftpeek.core.designsystem.theme.HeatL4Dark

@Deprecated("Use com.draftpeek.core.designsystem.theme.InfoContainerLight instead", ReplaceWith("InfoContainerLight", "com.draftpeek.core.designsystem.theme"))
val InfoContainerLight: Color get() = com.draftpeek.core.designsystem.theme.InfoContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnInfoContainerLight instead", ReplaceWith("OnInfoContainerLight", "com.draftpeek.core.designsystem.theme"))
val OnInfoContainerLight: Color get() = com.draftpeek.core.designsystem.theme.OnInfoContainerLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.InfoContainerDark instead", ReplaceWith("InfoContainerDark", "com.draftpeek.core.designsystem.theme"))
val InfoContainerDark: Color get() = com.draftpeek.core.designsystem.theme.InfoContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.OnInfoContainerDark instead", ReplaceWith("OnInfoContainerDark", "com.draftpeek.core.designsystem.theme"))
val OnInfoContainerDark: Color get() = com.draftpeek.core.designsystem.theme.OnInfoContainerDark

@Deprecated("Use com.draftpeek.core.designsystem.theme.IconBackgroundLight instead", ReplaceWith("IconBackgroundLight", "com.draftpeek.core.designsystem.theme"))
val IconBackgroundLight: Color get() = com.draftpeek.core.designsystem.theme.IconBackgroundLight

@Deprecated("Use com.draftpeek.core.designsystem.theme.IconBackgroundDark instead", ReplaceWith("IconBackgroundDark", "com.draftpeek.core.designsystem.theme"))
val IconBackgroundDark: Color get() = com.draftpeek.core.designsystem.theme.IconBackgroundDark

// ---- 枚举与函数 ----

/** 彩虹颜色枚举，用于颜色选择器等组件 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.RainbowColor instead", ReplaceWith("RainbowColor", "com.draftpeek.core.designsystem.theme"))
typealias RainbowColor = com.draftpeek.core.designsystem.theme.RainbowColor

/** 贡献等级枚举，用于 GitHub 风格热力图 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.ContributionLevel instead", ReplaceWith("ContributionLevel", "com.draftpeek.core.designsystem.theme"))
typealias ContributionLevel = com.draftpeek.core.designsystem.theme.ContributionLevel

/**
 * 获取强调色热力图颜色列表。
 * @param isDark 是否为深色主题
 * @return 五个等级的热力图颜色列表
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.accentHeatMapColors instead", ReplaceWith("accentHeatMapColors", "com.draftpeek.core.designsystem.theme"))
fun accentHeatMapColors(isDark: Boolean): List<Color> = com.draftpeek.core.designsystem.theme.accentHeatMapColors(isDark)

/**
 * 根据贡献等级获取热力图颜色。
 * @param level 贡献等级（L0-L4）
 * @param isDark 是否为深色主题
 * @return 对应等级的颜色
 */
@Deprecated("Use com.draftpeek.core.designsystem.theme.accentHeatMapColor instead", ReplaceWith("accentHeatMapColor", "com.draftpeek.core.designsystem.theme"))
fun accentHeatMapColor(level: com.draftpeek.core.designsystem.theme.ContributionLevel, isDark: Boolean): Color =
    com.draftpeek.core.designsystem.theme.accentHeatMapColor(level, isDark)
