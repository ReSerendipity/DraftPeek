/**
 * DraftPeek 桌面小部件广播接收器。
 *
 * **文件功能**：Glance AppWidget 的 BroadcastReceiver 实现，向系统注册最近文件小部件，
 *               使其出现在系统小部件选择器中。
 *
 * **主要类/接口**：[RecentFilesWidgetReceiver] - 继承自 [GlanceAppWidgetReceiver]。
 *
 * **模块依赖**：
 * - Jetpack Glance：[GlanceAppWidgetReceiver] 基类
 */
package com.draftpeek.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 最近文件桌面小部件的广播接收器。
 *
 * 向 Android 系统注册 [RecentFilesWidget]，使其出现在桌面小部件选择列表中。
 * 系统通过此 Receiver 实例化和管理小部件生命周期。
 */
class RecentFilesWidgetReceiver : GlanceAppWidgetReceiver() {
    /** 提供此 Receiver 对应的 GlanceAppWidget 实例 */
    override val glanceAppWidget: GlanceAppWidget = RecentFilesWidget()
}
