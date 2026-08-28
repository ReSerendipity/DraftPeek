/**
 * 触摸目标尺寸 Modifier。
 *
 * 遵循 Material Design 无障碍设计指南，确保可点击组件的最小触摸目标为 48×48dp。
 * 小于 48dp 的视觉元素将通过透明方式扩展其可点击区域。
 *
 * 使用时应在任何 `clickable()` Modifier 之前应用此 Modifier。
 */
package com.draftpeek.core.ui.modifier

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 确保 Composable 具有最小 48×48dp 的触摸目标，
 * 符合 Material Design 无障碍设计指南的推荐标准。
 *
 * 小于 48dp 的视觉元素将透明地扩展其可点击区域。
 * 请在任何 `clickable()` Modifier 之前应用此 Modifier。
 */
fun Modifier.minimumTouchTarget(): Modifier = this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
