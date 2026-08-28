/**
 * Markdown 格式工具栏组件。
 *
 * 提供丰富的 Markdown 格式化按钮，包括粗体、斜体、删除线、高亮、标题、列表、
 * 链接、图片、引用、代码块、表格、数学公式、Mermaid 图表等。
 * 支持插入链接/图片/表格/代码块的对话框。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.icon.DraftPeekIcons
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 表示可应用于编辑器的 Markdown 格式化操作。
 */
sealed class MarkdownFormatAction {
    /**
     * 用格式标记包裹选中文本（如 **粗体**）。
     * @param prefix 前缀标记
     * @param suffix 后缀标记
     * @param placeholder 无选中文本时的占位符文本
     */
    data class Wrap(val prefix: String, val suffix: String, val placeholder: String) : MarkdownFormatAction()

    /**
     * 在光标位置插入模板文本。
     * @param template 要插入的模板
     * @param cursorOffset 插入后光标偏移量
     */
    data class Insert(val template: String, val cursorOffset: Int = 0) : MarkdownFormatAction()

    /**
     * 在当前行开头插入行前缀。
     * @param prefix 行前缀（如 "# ", "- ", "> "）
     */
    data class LinePrefix(val prefix: String) : MarkdownFormatAction()

    /**
     * 由字符串键标识的自定义操作。
     * @param action 自定义操作标识符
     */
    data class Custom(val action: String) : MarkdownFormatAction()
}

/**
 * Markdown 格式化工具栏 Composable。
 *
 * 水平滚动的工具栏，按功能分组提供格式化按钮。
 * 包含基础格式（粗体/斜体）、结构（标题/列表/引用）、高级（代码块/表格/公式）等分组。
 *
 * @param onFormatAction 格式化操作回调
 * @param onInsertEmoji 插入 Emoji 回调（可选）
 * @param onInsertImage 插入图片回调（可选）
 * @param modifier 修饰符
 */
@Composable
fun MarkdownToolbar(
    onFormatAction: (MarkdownFormatAction) -> Unit,
    modifier: Modifier = Modifier,
    onInsertEmoji: (() -> Unit)? = null,
    onInsertImage: (() -> Unit)? = null
) {
    // --- Action content descriptions ---
    val bold = stringResource(R.string.editor_action_bold)
    val italic = stringResource(R.string.editor_action_italic)
    val strikethrough = stringResource(R.string.editor_action_strikethrough)
    val highlight = stringResource(R.string.editor_action_highlight)
    val underline = stringResource(R.string.editor_action_underline)
    val inlineCode = stringResource(R.string.editor_action_inline_code)
    val inlineFormula = stringResource(R.string.editor_action_inline_formula)
    val superscript = stringResource(R.string.editor_action_superscript)
    val subscript = stringResource(R.string.editor_action_subscript)

    val heading1 = stringResource(R.string.editor_action_heading_1)
    val heading2 = stringResource(R.string.editor_action_heading_2)
    val heading3 = stringResource(R.string.editor_action_heading_3)
    val unorderedList = stringResource(R.string.editor_action_unordered_list)
    val orderedList = stringResource(R.string.editor_action_ordered_list)
    val taskList = stringResource(R.string.editor_action_task_list)
    val toggleList = stringResource(R.string.editor_action_toggle_list)
    val link = stringResource(R.string.editor_action_link)
    val imageLink = stringResource(R.string.editor_action_image_link)
    val insertImage = stringResource(R.string.editor_action_insert_image)
    val blockquote = stringResource(R.string.editor_action_blockquote)
    val horizontalRule = stringResource(R.string.editor_action_horizontal_rule)

    val codeBlock = stringResource(R.string.editor_action_code_block)
    val codeBlockLang = stringResource(R.string.editor_action_code_block_lang)
    val table = stringResource(R.string.editor_action_table)
    val mathFormula = stringResource(R.string.editor_action_math_formula)
    val mermaid = stringResource(R.string.editor_action_mermaid)
    val insertLinkDialog = stringResource(R.string.editor_action_insert_link_dialog)
    val insertImageDialog = stringResource(R.string.editor_action_insert_image_dialog)
    val insertTable = stringResource(R.string.editor_action_insert_table)

    // --- Template strings ---
    val boldTpl = stringResource(R.string.editor_tpl_bold)
    val italicTpl = stringResource(R.string.editor_tpl_italic)
    val strikethroughTpl = stringResource(R.string.editor_tpl_strikethrough)
    val highlightTpl = stringResource(R.string.editor_tpl_highlight)
    val underlineTpl = stringResource(R.string.editor_tpl_underline)
    val codeTpl = stringResource(R.string.editor_tpl_code)
    val formulaTpl = stringResource(R.string.editor_tpl_formula)
    val superscriptTpl = stringResource(R.string.editor_tpl_superscript)
    val subscriptTpl = stringResource(R.string.editor_tpl_subscript)

    val linkTpl = stringResource(R.string.editor_tpl_link)
    val imageTpl = stringResource(R.string.editor_tpl_image)
    val codeBlockTpl = stringResource(R.string.editor_tpl_code_block)
    val tableTpl = stringResource(R.string.editor_tpl_table)
    val mathTpl = stringResource(R.string.editor_tpl_math)
    val mermaidTpl = stringResource(R.string.editor_tpl_mermaid)

    // --- Dialog state ---
    var showLinkDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showCodeBlockDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // --- Dialog composables ---
    if (showLinkDialog) {
        InsertLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { text, url ->
                onFormatAction(MarkdownFormatAction.Insert("[$text]($url)", text.length + 3))
                showLinkDialog = false
            }
        )
    }

    if (showImageDialog) {
        InsertImageDialog(
            onDismiss = { showImageDialog = false },
            onConfirm = { alt, url ->
                onFormatAction(MarkdownFormatAction.Insert("![$alt]($url)", alt.length + 4))
                showImageDialog = false
            }
        )
    }

    if (showTableDialog) {
        InsertTableDialog(
            onDismiss = { showTableDialog = false },
            onConfirm = { rows, cols ->
                onFormatAction(MarkdownFormatAction.Insert(generateTableTemplate(rows, cols), 2))
                showTableDialog = false
            }
        )
    }

    if (showCodeBlockDialog) {
        InsertCodeBlockDialog(
            onDismiss = { showCodeBlockDialog = false },
            onConfirm = { language ->
                val template = "```$language\n\n```"
                val offset = language.length + 5
                onFormatAction(MarkdownFormatAction.Insert(template, offset))
                showCodeBlockDialog = false
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = PrototypeTokens.border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrototypeTokens.surface)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ── Basic group: Bold, Italic, Strikethrough, Code inline ──
                FormatButton(
                    label = "B",
                    contentDescription = bold,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("**", "**", boldTpl)) }
                )
                FormatButton(
                    label = "I",
                    contentDescription = italic,
                    italic = true,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("*", "*", italicTpl)) }
                )
                FormatButton(
                    label = "S",
                    contentDescription = strikethrough,
                    strikethrough = true,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("~~", "~~", strikethroughTpl)) }
                )
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("`", "`", codeTpl)) },
                    contentDescription = inlineCode
                ) {
                    Icon(
                        DraftPeekIcons.CodeBrackets,
                        contentDescription = inlineCode,
                        modifier = Modifier.size(17.dp),
                        tint = PrototypeTokens.muted
                    )
                }

                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("- ")) },
                    contentDescription = unorderedList
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.ListBullet,
                        contentDescription = unorderedList,
                        modifier = Modifier.size(17.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                FormatButton(
                    label = "1.",
                    contentDescription = orderedList,
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("1. ")) }
                )

                ToolbarDivider(Modifier.align(Alignment.CenterVertically))

                // ── Structure group: Heading, Blockquote, HR, Link, Image ──
                FormatButton(
                    label = "H1",
                    contentDescription = heading1,
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("# ")) }
                )
                FormatButton(
                    label = "H2",
                    contentDescription = heading2,
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("## ")) }
                )
                FormatButton(
                    label = "H3",
                    contentDescription = heading3,
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("### ")) }
                )

                ToolbarDivider(Modifier.align(Alignment.CenterVertically))

                FormatButton(
                    label = "\u9ad8\u4eae",
                    contentDescription = highlight,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("==", "==", highlightTpl)) }
                )
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("- [ ] ")) },
                    contentDescription = taskList
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.TaskList,
                        contentDescription = taskList,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.Custom("toggle_list_type")) },
                    contentDescription = toggleList
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.SwapHoriz,
                        contentDescription = toggleList,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }

                ToolbarDivider(Modifier.align(Alignment.CenterVertically))

                ToolbarIconButton(
                    onClick = { showLinkDialog = true },
                    contentDescription = insertLinkDialog
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.Link,
                        contentDescription = insertLinkDialog,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                ToolbarIconButton(
                    onClick = { showImageDialog = true },
                    contentDescription = insertImageDialog
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.Image,
                        contentDescription = insertImageDialog,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                if (onInsertImage != null) {
                    ToolbarIconButton(
                        onClick = { onInsertImage() },
                        contentDescription = insertImage
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.Image,
                            contentDescription = insertImage,
                            modifier = Modifier.size(16.dp),
                            tint = PrototypeTokens.muted
                        )
                    }
                }
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.LinePrefix("> ")) },
                    contentDescription = blockquote
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.Quote,
                        contentDescription = blockquote,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.Insert("---\n", 0)) },
                    contentDescription = horizontalRule
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.HorizontalRule,
                        contentDescription = horizontalRule,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }

                ToolbarDivider(Modifier.align(Alignment.CenterVertically))

                // ── Advanced group: Code block, Table, Math, Mermaid ──
                ToolbarIconButton(
                    onClick = { showCodeBlockDialog = true },
                    contentDescription = codeBlockLang
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.CodeBlock,
                        contentDescription = codeBlockLang,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                ToolbarIconButton(
                    onClick = { showTableDialog = true },
                    contentDescription = insertTable
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.Table,
                        contentDescription = insertTable,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }
                ToolbarIconButton(
                    onClick = { onFormatAction(MarkdownFormatAction.Insert(mathTpl, 4)) },
                    contentDescription = mathFormula
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.Formula,
                        contentDescription = mathFormula,
                        modifier = Modifier.size(16.dp),
                        tint = PrototypeTokens.muted
                    )
                }

                ToolbarDivider(Modifier.align(Alignment.CenterVertically))

                // ── More overflow menu ──
                Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                    ToolbarIconButton(
                        onClick = { showMoreMenu = true },
                        contentDescription = stringResource(R.string.editor_toolbar_more)
                    ) {
                        StrokeIcon(
                            icon = StrokeIcons.MoreVert,
                            contentDescription = stringResource(R.string.editor_toolbar_more),
                            modifier = Modifier.size(16.dp),
                            tint = PrototypeTokens.muted
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        // Underline
                        DropdownMenuItem(
                            text = { Text(underline, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(underlineTpl, 3))
                                showMoreMenu = false
                            }
                        )
                        // Inline formula
                        DropdownMenuItem(
                            text = { Text(inlineFormula, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Wrap("$", "$", formulaTpl))
                                showMoreMenu = false
                            }
                        )
                        // Superscript
                        DropdownMenuItem(
                            text = { Text(superscript, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Wrap("^", "^", superscriptTpl))
                                showMoreMenu = false
                            }
                        )
                        // Subscript
                        DropdownMenuItem(
                            text = { Text(subscript, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Wrap("~", "~", subscriptTpl))
                                showMoreMenu = false
                            }
                        )
                        HorizontalDivider()
                        // Quick link (inline template)
                        DropdownMenuItem(
                            text = { Text(link, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(linkTpl, 1))
                                showMoreMenu = false
                            }
                        )
                        // Quick image link (inline template)
                        DropdownMenuItem(
                            text = { Text(imageLink, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(imageTpl, 2))
                                showMoreMenu = false
                            }
                        )
                        // Quick code block (no language)
                        DropdownMenuItem(
                            text = { Text(codeBlock, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(codeBlockTpl, 4))
                                showMoreMenu = false
                            }
                        )
                        // Quick table (default template)
                        DropdownMenuItem(
                            text = { Text(table, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(tableTpl, 2))
                                showMoreMenu = false
                            }
                        )
                        HorizontalDivider()
                        // Mermaid diagram
                        DropdownMenuItem(
                            text = { Text(mermaid, style = DraftPeekTypography.bodySmall) },
                            onClick = {
                                onFormatAction(MarkdownFormatAction.Insert(mermaidTpl, 16))
                                showMoreMenu = false
                            }
                        )
                    }
                }

                if (onInsertEmoji != null) {
                    ToolbarIconButton(
                        onClick = { onInsertEmoji() },
                        contentDescription = "Emoji"
                    ) {
                        Text(text = "\uD83D\uDE00", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 对话框 Composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 插入链接对话框。
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认插入回调，参数为链接文本和 URL
 */
@Composable
private fun InsertLinkDialog(onDismiss: () -> Unit, onConfirm: (text: String, url: String) -> Unit) {
    var linkText by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_insert_link_title)) },
        confirmButton = {
            BrandFilledButton(
                onClick = { if (linkText.isNotBlank() && linkUrl.isNotBlank()) onConfirm(linkText, linkUrl) },
                enabled = linkText.isNotBlank() && linkUrl.isNotBlank()
            ) {
                Text(stringResource(R.string.editor_confirm))
            }
        },
        dismissButton = {
            BrandOutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.editor_cancel))
            }
        },
        content = {
            BrandOutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                label = { Text(stringResource(R.string.editor_insert_link_text)) },
                placeholder = { Text(stringResource(R.string.editor_insert_link_text_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            BrandOutlinedTextField(
                value = linkUrl,
                onValueChange = { linkUrl = it },
                label = { Text(stringResource(R.string.editor_insert_link_url)) },
                placeholder = { Text(stringResource(R.string.editor_insert_link_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/**
 * 插入图片对话框。
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认插入回调，参数为替代文本和图片 URL
 */
@Composable
private fun InsertImageDialog(onDismiss: () -> Unit, onConfirm: (alt: String, url: String) -> Unit) {
    var altText by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_insert_image_title)) },
        confirmButton = {
            BrandFilledButton(
                onClick = { if (altText.isNotBlank() && imageUrl.isNotBlank()) onConfirm(altText, imageUrl) },
                enabled = altText.isNotBlank() && imageUrl.isNotBlank()
            ) {
                Text(stringResource(R.string.editor_confirm))
            }
        },
        dismissButton = {
            BrandOutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.editor_cancel))
            }
        },
        content = {
            BrandOutlinedTextField(
                value = altText,
                onValueChange = { altText = it },
                label = { Text(stringResource(R.string.editor_insert_image_alt)) },
                placeholder = { Text(stringResource(R.string.editor_insert_image_alt_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            BrandOutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text(stringResource(R.string.editor_insert_image_url)) },
                placeholder = { Text(stringResource(R.string.editor_insert_image_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/**
 * 插入表格对话框。
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认插入回调，参数为行数和列数
 */
@Composable
private fun InsertTableDialog(onDismiss: () -> Unit, onConfirm: (rows: Int, cols: Int) -> Unit) {
    var rowsText by remember { mutableStateOf("3") }
    var colsText by remember { mutableStateOf("3") }
    val rows = rowsText.toIntOrNull() ?: 3
    val cols = colsText.toIntOrNull() ?: 3
    val valid = rows in 1..20 && cols in 1..10

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_insert_table_title)) },
        confirmButton = {
            BrandFilledButton(
                onClick = { if (valid) onConfirm(rows, cols) },
                enabled = valid
            ) {
                Text(stringResource(R.string.editor_confirm))
            }
        },
        dismissButton = {
            BrandOutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.editor_cancel))
            }
        },
        content = {
            BrandOutlinedTextField(
                value = rowsText,
                onValueChange = { rowsText = it },
                label = { Text(stringResource(R.string.editor_insert_table_rows)) },
                placeholder = { Text(stringResource(R.string.editor_insert_table_rows_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            BrandOutlinedTextField(
                value = colsText,
                onValueChange = { colsText = it },
                label = { Text(stringResource(R.string.editor_insert_table_cols)) },
                placeholder = { Text(stringResource(R.string.editor_insert_table_cols_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/**
 * 插入代码块对话框。
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认插入回调，参数为编程语言标识符
 */
@Composable
private fun InsertCodeBlockDialog(onDismiss: () -> Unit, onConfirm: (language: String) -> Unit) {
    var language by remember { mutableStateOf("") }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_action_code_block_lang)) },
        confirmButton = {
            BrandFilledButton(
                onClick = { onConfirm(language.trim()) },
                enabled = true
            ) {
                Text(stringResource(R.string.editor_confirm))
            }
        },
        dismissButton = {
            BrandOutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.editor_cancel))
            }
        },
        content = {
            BrandOutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text(stringResource(R.string.editor_code_block_language)) },
                placeholder = { Text(stringResource(R.string.editor_code_block_language_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 辅助函数
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 生成指定行列数的 Markdown 表格模板。
 *
 * 生成包含表头（| Col1 | Col2 |）、分隔符行和数据行的 Markdown 表格。
 *
 * @param rows 数据行数
 * @param cols 列数
 * @return Markdown 表格字符串
 */
private fun generateTableTemplate(rows: Int, cols: Int): String {
    val header = (1..cols).joinToString(" | ", "| ", " |") { "Col$it" }
    val separator = (1..cols).joinToString(" | ", "| ", " |") { "---" }
    val dataRow = (1..cols).joinToString(" | ", "| ", " |") { "Content" }
    val dataRows = (1..rows).joinToString("\n") { dataRow }
    return "$header\n$separator\n$dataRows"
}

// ─────────────────────────────────────────────────────────────────────────────
// 私有 Composable 辅助组件
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 格式化文本按钮（用于工具栏）。
 *
 * 显示带样式的文本标签（支持粗体、斜体、删除线、下划线），用于粗体、斜体等格式化按钮。
 *
 * @param label 按钮显示文本
 * @param contentDescription 无障碍描述
 * @param italic 是否使用斜体样式
 * @param strikethrough 是否使用删除线样式
 * @param underline 是否使用下划线样式
 * @param onClick 点击回调
 */
@Composable
private fun FormatButton(
    label: String,
    contentDescription: String,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false,
    onClick: () -> Unit
) {
    val muted = PrototypeTokens.muted
    val isChinese = label.any { it.code > 0x2E80 }
    Box(
        modifier = Modifier
            .then(
                if (isChinese) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                } else {
                    Modifier
                        .size(32.dp, 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onClick() }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (isChinese) {
                DraftPeekTypography.labelMedium.copy(
                    color = muted,
                    fontSize = 12.sp
                )
            } else {
                DraftPeekTypography.bodySmall.copy(
                    color = muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    fontStyle = if (italic) FontStyle.Italic else null,
                    textDecoration = when {
                        strikethrough -> TextDecoration.LineThrough
                        underline -> TextDecoration.Underline
                        else -> null
                    }
                )
            }
        )
    }
}

/**
 * 工具栏分隔符。
 *
 * 在工具栏按钮组之间显示垂直分隔线。
 *
 * @param modifier 修饰符
 */
@Composable
private fun ToolbarDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(24.dp)
            .width(1.dp),
        color = PrototypeTokens.border
    )
}

/**
 * 工具栏图标按钮（32x28，圆角，柔和色调）。
 *
 * 紧凑的图标按钮，符合原型工具栏设计风格。
 *
 * @param onClick 点击回调
 * @param contentDescription 无障碍描述
 * @param modifier 修饰符
 * @param content 按钮图标内容 Composable
 */
@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp, 28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
