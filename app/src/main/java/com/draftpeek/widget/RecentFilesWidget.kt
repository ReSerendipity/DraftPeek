/**
 * DraftPeek 桌面最近文件小部件。
 *
 * **文件功能**：提供 Android 桌面小部件，显示最近打开的文件列表，方便用户快速访问。
 *
 * **主要类/接口**：
 * - [RecentFilesWidget] - Glance 小部件主类
 * - [RecentFileInfo] - 最近文件信息数据类
 * - [WidgetContent] - 小部件内容 Composable
 *
 * **模块依赖**：
 * - Jetpack Glance：基于 Compose 的 AppWidget 框架
 * - Android 框架：[Context]、SharedPreferences 读取最近文件列表
 */
package com.draftpeek.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.draftpeek.R

/**
 * 桌面最近文件小部件，继承自 [GlanceAppWidget]。
 *
 * 使用 Glance + Material You 风格显示最多 4 个最近文件，点击文件可直接在编辑器中打开。
 * 数据来源：从 SharedPreferences 读取最近文件列表。
 */
class RecentFilesWidget : GlanceAppWidget() {

    /**
     * 提供小部件的 Glance 内容。
     *
     * @param context 应用上下文
     * @param id 小部件 ID
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recentFiles = loadRecentFiles(context)

        provideContent {
            WidgetContent(context = context, recentFiles = recentFiles)
        }
    }

    companion object {
        /** 小部件显示的最大文件数 */
        const val MAX_FILES = 4
        private const val PREFS_NAME = "recent_files_widget"
        private const val KEY_RECENT_FILES = "recent_files"
    }

    /**
     * 最近文件信息数据类。
     *
     * @property uri 文件 URI 字符串
     * @property fileName 文件名
     * @property language 编程语言标识，可为 null
     */
    internal data class RecentFileInfo(val uri: String, val fileName: String, val language: String?)

    /**
     * 从 SharedPreferences 加载最近文件列表。
     *
     * @param context 应用上下文
     * @return 解析后的最近文件信息列表
     */
    private fun loadRecentFiles(context: Context): List<RecentFileInfo> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val files = prefs.getStringSet(KEY_RECENT_FILES, emptySet()) ?: emptySet()
        return files.mapNotNull { entry ->
            val parts = entry.split("|", limit = 3)
            if (parts.size >= 2) {
                RecentFileInfo(
                    uri = parts[0],
                    fileName = parts[1],
                    language = parts.getOrNull(2)
                )
            } else {
                null
            }
        }.sortedByDescending { it.fileName }
    }
}

/**
 * 小部件内容 Composable 函数（基于 Glance）。
 *
 * 显示小部件标题和最近文件列表，若列表为空则显示提示文本。
 *
 * @param context 应用上下文，用于获取字符串资源
 * @param recentFiles 要显示的最近文件列表
 */
@Composable
private fun WidgetContent(context: Context, recentFiles: List<RecentFilesWidget.RecentFileInfo>) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = context.getString(R.string.widget_recent_files_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (recentFiles.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_no_recent_files),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            } else {
                recentFiles.take(RecentFilesWidget.MAX_FILES).forEach { file ->
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.fileName,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (!file.language.isNullOrBlank()) {
                            Text(
                                text = file.language,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
