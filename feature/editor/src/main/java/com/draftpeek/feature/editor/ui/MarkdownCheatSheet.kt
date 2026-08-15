/**
 * Markdown 语法速查表对话框。
 *
 * 按分类展示 Markdown 常用语法示例，包括标题、强调、链接图片、代码、列表、
 * 引用、表格、数学公式、Mermaid 图表等，帮助用户快速查阅语法。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 速查表单条条目，包含语法示例和描述。
 *
 * @property syntax Markdown 语法示例
 * @property descriptionResId 描述文本的字符串资源 ID
 */
private data class CheatEntry(
    val syntax: String,
    val descriptionResId: Int,
)

/**
 * 速查表分类，包含多个 [CheatEntry] 条目。
 *
 * @property titleResId 分类标题的字符串资源 ID
 * @property entries 该分类下的语法条目列表
 */
private data class CheatCategory(
    val titleResId: Int,
    val entries: List<CheatEntry>,
)

/**
 * 记住速查表数据，返回按分类组织的 Markdown 语法条目列表。
 */
@Composable
private fun rememberCheatSheetData(): List<CheatCategory> = listOf(
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_headers,
        entries = listOf(
            CheatEntry("# H1", R.string.editor_cheatsheet_heading1),
            CheatEntry("## H2", R.string.editor_cheatsheet_heading2),
            CheatEntry("### H3", R.string.editor_cheatsheet_heading3),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_emphasis,
        entries = listOf(
            CheatEntry("**bold**", R.string.editor_cheatsheet_bold),
            CheatEntry("*italic*", R.string.editor_cheatsheet_italic),
            CheatEntry("~~strikethrough~~", R.string.editor_cheatsheet_strikethrough),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_links_images,
        entries = listOf(
            CheatEntry("[text](url)", R.string.editor_cheatsheet_link),
            CheatEntry("![alt](url)", R.string.editor_cheatsheet_image),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_code,
        entries = listOf(
            CheatEntry("`inline`", R.string.editor_cheatsheet_inline_code),
            CheatEntry("```\nfenced\n```", R.string.editor_cheatsheet_code_block),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_lists,
        entries = listOf(
            CheatEntry("- item", R.string.editor_cheatsheet_unordered),
            CheatEntry("1. item", R.string.editor_cheatsheet_ordered),
            CheatEntry("- [ ] task", R.string.editor_cheatsheet_task),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_blockquotes,
        entries = listOf(
            CheatEntry("> quote", R.string.editor_cheatsheet_blockquote_desc),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_tables,
        entries = listOf(
            CheatEntry("| A | B |\n|---|---|\n| 1 | 2 |", R.string.editor_cheatsheet_table_desc),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_math,
        entries = listOf(
            CheatEntry("\$inline\$", R.string.editor_cheatsheet_inline_math),
            CheatEntry("\$\$\ndisplay\n\$\$", R.string.editor_cheatsheet_display_math),
        ),
    ),
    CheatCategory(
        titleResId = R.string.editor_cheatsheet_mermaid,
        entries = listOf(
            CheatEntry("```mermaid\ngraph LR\n```", R.string.editor_cheatsheet_mermaid_desc),
        ),
    ),
)

/**
 * Markdown 语法速查表对话框 Composable。
 *
 * 以对话框形式展示按分类组织的 Markdown 语法示例，左侧显示等宽字体语法，
 * 右侧显示中文描述，帮助用户快速查阅 Markdown 语法。
 *
 * @param onDismiss 用户关闭速查表时的回调
 * @param modifier 修饰符
 */
@Composable
fun MarkdownCheatSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent

    val categories = rememberCheatSheetData()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = BrandShapes.Dialog,
            color = surface,
            contentColor = fg,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .fillMaxHeight(0.8f),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title
                Text(
                    text = stringResource(R.string.editor_markdown_cheat_sheet),
                    style = DraftPeekTypography.headlineSmall.copy(
                        color = fg,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                )

                HorizontalDivider(
                    color = border,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    categories.forEach { category ->
                        item(key = "cat_${category.titleResId}") {
                            // Category header
                            Text(
                                text = stringResource(category.titleResId),
                                style = MetaStyle.copy(
                                    color = accent,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 16.dp,
                                    bottom = 6.dp,
                                ),
                            )
                        }
                        category.entries.forEachIndexed { idx, entry ->
                            item(key = "entry_${category.titleResId}_$idx") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    // Syntax (monospace)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(PrototypeShapes.Small)
                                            .background(border.copy(alpha = 0.3f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = entry.syntax,
                                            style = DraftPeekTypography.bodySmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = fg,
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Description
                                    Text(
                                        text = stringResource(entry.descriptionResId),
                                        style = DraftPeekTypography.bodySmall.copy(
                                            color = fgSoft,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .align(Alignment.CenterVertically),
                                    )
                                }
                            }
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BrandFilledButton(
                        text = stringResource(R.string.editor_close),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}
