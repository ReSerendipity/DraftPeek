/**
 * Markdown 预览界面组件。
 *
 * 提供多种 Markdown 视图模式：编辑、预览、分屏、所见即所得（WYSIWYG）。
 * Markwon 已移除（P0），改用 CommonMark 原生渲染（NativeMarkdownRenderer）+ WebView 显示。
 *
 * 渲染策略：
 * - CommonMark (org.commonmark 0.24.0) 解析 Markdown → HTML
 * - WebView 显示带主题 CSS 的 HTML
 * - 支持 GFM 表格、删除线、任务列表、自动链接
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.model.MarkdownViewMode

private const val TAG = "MarkdownPreview"

/**
 * Compose `testTag` 标识，生产代码与 androidTest 共用同一份常量，
 * 避免测试断言的 tag 与被测组件漂移。
 */
object EditorTestTags {
    const val MARKDOWN_PREVIEW = "test_tag_markdown_preview"
    const val MARKDOWN_RICH_EDITOR = "test_tag_markdown_rich_editor"
    const val LARGE_CONTENT_FALLBACK = "test_tag_large_content_fallback"
}

/**
 * 增强型 Markdown 预览 Composable，支持多种视图模式。
 *
 * 使用 CommonMark 原生渲染 + WebView 显示，替代已移除的 Markwon。
 *
 * @param markdownContent Markdown 文本内容
 * @param onContentChanged 内容变更回调
 * @param viewMode 视图模式
 * @param isDarkTheme 是否深色主题
 * @param modifier 修饰符
 */
@Composable
fun MarkdownPreview(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onContentChanged: ((String) -> Unit)? = null,
    viewMode: MarkdownViewMode = MarkdownViewMode.EDIT,
    isDarkTheme: Boolean = LocalDarkTheme.current
) {
    val pageBg = PrototypeTokens.pageBackground
    val surfaceColor = PrototypeTokens.surface
    val onSurfaceColor = PrototypeTokens.fg

    when (viewMode) {
        MarkdownViewMode.EDIT, MarkdownViewMode.PREVIEW -> {
            // Read-only preview using CommonMark → WebView
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(EditorTestTags.MARKDOWN_PREVIEW)
            ) {
                MarkdownWebViewPreview(
                    markdownContent = markdownContent,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        MarkdownViewMode.SPLIT -> {
            // Split view: edit on left, preview on right
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(EditorTestTags.MARKDOWN_PREVIEW)
                    .background(pageBg)
            ) {
                // Edit pane (plain text)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(surfaceColor)
                ) {
                    MarkdownWebViewPreview(
                        markdownContent = markdownContent,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(0.01f),
                    color = onSurfaceColor.copy(alpha = 0.2f)
                )

                // Preview pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(pageBg)
                ) {
                    MarkdownWebViewPreview(
                        markdownContent = markdownContent,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        MarkdownViewMode.WYSIWYG -> {
            // WYSIWYG mode: native Compose rich text editing (Ch6#2 P1)
            MarkdownRichEditor(
                markdownContent = markdownContent,
                onContentChanged = onContentChanged,
                isDarkTheme = isDarkTheme,
                modifier = modifier
            )
        }
    }
}
