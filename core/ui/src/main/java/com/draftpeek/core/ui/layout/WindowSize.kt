/**
 * 窗口尺寸检测与布局模式定义。
 *
 * 提供基于 Material3 WindowSizeClass 的便捷封装，将窗口宽度尺寸类映射到简化的
 * [LayoutMode] 枚举，方便应用在不同屏幕尺寸和设备形态上自适应布局。
 *
 * 布局模式分为三类：
 * - COMPACT：手机竖屏（宽度 < 600dp）
 * - MEDIUM：手机横屏/折叠屏展开（600dp ≤ 宽度 < 840dp）
 * - EXPANDED：平板/桌面（宽度 ≥ 840dp）
 */
package com.draftpeek.core.ui.layout

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/**
 * 布局模式枚举，表示当前窗口的布局适配模式。
 *
 * 基于 Material3 [WindowWidthSizeClass] 简化而来，用于在整个应用中进行布局决策。
 */
enum class LayoutMode {
    /** 手机竖屏 — 宽度 < 600dp */
    COMPACT,

    /** 手机横屏 / 折叠屏展开 — 600dp ≤ 宽度 < 840dp */
    MEDIUM,

    /** 平板 / 桌面 — 宽度 ≥ 840dp */
    EXPANDED
}

/**
 * 根据 [WindowWidthSizeClass] 将 [WindowSizeClass] 转换为 [LayoutMode]。
 *
 * @return 对应的 [LayoutMode] 布局模式
 */
fun WindowSizeClass.layoutMode(): LayoutMode = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> LayoutMode.COMPACT
    WindowWidthSizeClass.Medium -> LayoutMode.MEDIUM
    WindowWidthSizeClass.Expanded -> LayoutMode.EXPANDED
    else -> LayoutMode.COMPACT
}
