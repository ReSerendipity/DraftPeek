/**
 * 原生 Compose 所见即所得（WYSIWYG）Markdown 富文本编辑器。
 *
 * 基于 richeditor-compose 库实现，提供完全原生的 Compose 富文本编辑体验，
 * 相比 WebView 方案具有更好的性能、更低的内存占用和更紧密的 Compose UI 集成。
 * 支持粗体、斜体、删除线、代码、列表等常用格式。
 *
 * **大文件保护**：当内容超过 [MAX_SAFE_CONTENT_SIZE] 时，不会加载到 RichTextState，
 * 而是显示提示信息。这是防止 RichTextEditor Constraints 溢出崩溃的最终防线——
 * 无论此 Composable 如何进入组合（导航转场动画、状态竞争等），都不会崩溃。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

/**
 * RichTextEditor 安全内容大小上限（字符数）。
 *
 * richeditor-compose 的 RichTextEditor 在 measure 阶段会尝试一次性测量全部内容，
 * 当内容高度超过 Compose Constraints 限制（约 65535px）时抛出
 * IllegalArgumentException: Can't represent a width of X and height of Y in Constraints。
 *
 * 此值设为 100K 字符（约 200KB），在常见字体大小下渲染高度远低于 65535px，
 * 留有充足安全余量。
 */
private const val MAX_SAFE_CONTENT_SIZE = 100_000

/**
 * 原生 Compose WYSIWYG Markdown 编辑器 Composable。
 *
 * 使用 richeditor-compose 库实现，替代基于 WebView 的 WYSIWYG 方案，
 * 提供完全原生的 Compose 实现，具有更好的性能、更低的内存占用，
 * 以及与 Compose UI 层更紧密的集成。
 *
 * **大文件保护**：内容超过 [MAX_SAFE_CONTENT_SIZE] 时显示降级 UI 而非崩溃。
 *
 * 参考: Ch6#2 P1 — "引入 richeditor-compose 增强 Markdown 编辑能力"
 *
 * @param markdownContent 当前要显示/编辑的 Markdown 源文本
 * @param onContentChanged 用户编辑内容时的回调，返回更新后的 Markdown 源字符串
 * @param isDarkTheme 是否使用深色主题（影响颜色）
 * @param modifier 应用于外层 Column 的修饰符
 */
@Composable
fun MarkdownRichEditor(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onContentChanged: ((String) -> Unit)? = null,
    isDarkTheme: Boolean = false
) {
    // 硬性内容大小守卫：超大内容绝不加载到 RichTextState，防止 Constraints 溢出崩溃。
    // 这是最终防线——无论此 Composable 如何进入组合（导航转场动画、状态竞争等），
    // 只要内容超限就显示降级 UI，绝不调用 RichTextEditor。
    if (markdownContent.length > MAX_SAFE_CONTENT_SIZE) {
        LargeContentFallback(
            contentLength = markdownContent.length,
            modifier = modifier
        )
        return
    }

    val richTextState = rememberRichTextState()

    // Track the last Markdown source we loaded to avoid infinite loops
    // when onContentChanged triggers a re-composition with the same content.
    var lastLoadedMarkdown = remember { markdownContent }
    // Suppress callback during programmatic content loading (setMarkdown)
    // to prevent false "modified" signals when switching view modes.
    var suppressCallback = remember { true }

    // Load Markdown content into the rich editor when it changes externally.
    LaunchedEffect(markdownContent) {
        if (markdownContent != lastLoadedMarkdown) {
            // 双重检查：防止在 LaunchedEffect 排队期间内容变大
            if (markdownContent.length > MAX_SAFE_CONTENT_SIZE) return@LaunchedEffect
            suppressCallback = true
            safeSetMarkdown(richTextState, markdownContent)
            lastLoadedMarkdown = markdownContent
            // Release suppression after a microtask to allow annotatedString
            // to settle before user edits can trigger callbacks again.
            kotlinx.coroutines.yield()
            suppressCallback = false
        }
    }

    // Initial load: first time entering composition, force setMarkdown and suppress callback.
    LaunchedEffect(Unit) {
        // 双重检查：防止初始加载时内容已经超限
        if (markdownContent.length > MAX_SAFE_CONTENT_SIZE) return@LaunchedEffect
        suppressCallback = true
        safeSetMarkdown(richTextState, markdownContent)
        lastLoadedMarkdown = markdownContent
        kotlinx.coroutines.yield()
        suppressCallback = false
    }

    // Propagate user edits back to the caller as Markdown source.
    LaunchedEffect(richTextState.annotatedString) {
        if (suppressCallback) return@LaunchedEffect
        val currentMd = richTextState.toMarkdown()
        if (currentMd != lastLoadedMarkdown) {
            lastLoadedMarkdown = currentMd
            onContentChanged?.invoke(currentMd)
        }
    }

    val fgColor = PrototypeTokens.fg
    val fgSoftColor = PrototypeTokens.fgSoft
    val surfaceColor = PrototypeTokens.surface

    Column(modifier = modifier.fillMaxSize()) {
        // Formatting toolbar
        MarkdownRichEditorToolbar(
            richTextState = richTextState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = fgSoftColor.copy(alpha = 0.2f)
        )

        // Rich text editor - use Box to constrain height and prevent overflow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = fgColor
                )
            )
        }
    }
}

/**
 * 大内容降级 UI。
 *
 * 当 Markdown 内容超过 [MAX_SAFE_CONTENT_SIZE] 时，替代 RichTextEditor 显示。
 * 展示一条提示信息，告知用户内容过大无法使用 WYSIWYG 模式。
 *
 * @param contentLength 实际内容长度（字符数）
 * @param modifier 修饰符
 */
@Composable
private fun LargeContentFallback(contentLength: Int, modifier: Modifier = Modifier) {
    val fgSoftColor = PrototypeTokens.fgSoft
    val accentColor = PrototypeTokens.accent
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "内容过大（${contentLength / 1000}K 字符），WYSIWYG 模式不可用。\n请使用编辑模式或预览模式。",
            style = MaterialTheme.typography.bodyMedium,
            color = fgSoftColor,
            modifier = Modifier.padding(24.dp)
        )
    }
}

/**
 * 安全地将 Markdown 内容加载到 RichTextState 中。
 *
 * richeditor-compose 内部使用 org.intellij.markdown 解析 AST，
 * 当输入包含 CRLF 换行时，AST 节点偏移量会超出实际字符串长度，
 * 导致 StringIndexOutOfBoundsException（begin > length）。
 *
 * 修复策略：
 * 1. 将 CRLF / CR 统一为 LF，消除偏移量计算错误的根源；
 * 2. 若解析仍因其他边界情况抛出异常，降级为纯文本加载，避免闪退。
 * 3. 最终安全网：内容超过 [MAX_SAFE_CONTENT_SIZE] 时拒绝加载。
 */
private fun safeSetMarkdown(state: RichTextState, markdown: String) {
    // 最终安全网：绝不加载超大内容
    if (markdown.length > MAX_SAFE_CONTENT_SIZE) {
        Log.w("MarkdownRichEditor", "Content too large (${markdown.length} chars), refusing to load into RichTextState")
        return
    }
    // Normalize line endings: CRLF / lone CR → LF
    val normalized = markdown.replace("\r\n", "\n").replace("\r", "\n")
    try {
        state.setMarkdown(normalized)
    } catch (e: Exception) {
        Log.w("MarkdownRichEditor", "setMarkdown failed, falling back to plain text", e)
        // Fallback: load as plain text so the user can still see/edit content
        state.setText(normalized)
    }
}

/**
 * 富文本编辑器紧凑格式工具栏。
 *
 * 提供一键访问最常用的 Markdown 格式化操作：
 * 粗体、斜体、删除线、代码、无序列表、有序列表。
 *
 * @param richTextState 富文本状态对象
 * @param modifier 修饰符
 */
@Composable
private fun MarkdownRichEditorToolbar(richTextState: RichTextState, modifier: Modifier = Modifier) {
    val fgSoftColor = PrototypeTokens.fgSoft
    val accentColor = PrototypeTokens.accent

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Bold
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_bold),
            isActive = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
            onClick = {
                richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
        )
        // Italic
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_italic),
            isActive = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
            onClick = {
                richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
        )
        // Strikethrough
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_strikethrough),
            isActive = richTextState.currentSpanStyle.textDecoration
                ?.contains(TextDecoration.LineThrough) == true,
            onClick = {
                richTextState.toggleSpanStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
                )
            }
        )
        // Code span
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_code),
            isActive = richTextState.isCodeSpan,
            onClick = { richTextState.toggleCodeSpan() }
        )
        // Unordered list
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_unordered_list),
            isActive = richTextState.isUnorderedList,
            onClick = { richTextState.toggleUnorderedList() }
        )
        // Ordered list
        ToolbarButton(
            label = stringResource(R.string.editor_rich_editor_ordered_list),
            isActive = richTextState.isOrderedList,
            onClick = { richTextState.toggleOrderedList() }
        )
    }
}

/**
 * 富文本编辑器工具栏按钮。
 *
 * 文本按钮，根据是否激活状态显示不同颜色和字重。
 *
 * @param label 按钮显示文本
 * @param isActive 按钮是否处于激活状态（当前格式已应用）
 * @param onClick 点击回调
 */
@Composable
private fun ToolbarButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    val fgSoftColor = PrototypeTokens.fgSoft
    val accentColor = PrototypeTokens.accent

    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(0.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isActive) accentColor else fgSoftColor
        )
    }
}
