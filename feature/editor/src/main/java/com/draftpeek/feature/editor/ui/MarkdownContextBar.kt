/**
 * Markdown 选中文字上下文浮动工具栏。
 *
 * 当在 Markdown 编辑模式下选中文本时，在选区附近显示的浮动格式工具栏，
 * 提供快速格式化操作（粗体、斜体、删除线、高亮、代码、引用、链接、上下标）。
 * 支持滑入/滑出动画。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * Markdown 上下文浮动工具栏 Composable。
 *
 * 选中文本时显示的浮动工具栏，提供快速格式化操作。
 * 按功能分组：
 * - 行内格式：粗体、斜体、删除线、高亮、行内代码
 * - 结构操作：引用、链接
 * - 上下标：上标、下标
 *
 * @param visible 是否显示工具栏
 * @param onFormatAction 格式化操作回调
 * @param modifier 修饰符
 */
@Composable
fun MarkdownContextBar(
    visible: Boolean,
    onFormatAction: (MarkdownFormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrototypeTokens.elevated)
                .border(1.dp, PrototypeTokens.border, PrototypeShapes.Medium)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline formatting group（与 MarkdownToolbar 一致的文本/描边样式）
                ContextBarLabelButton(
                    label = "B",
                    contentDescription = stringResource(R.string.editor_action_bold),
                    fontWeight = FontWeight.Bold,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("**", "**", "")) }
                )
                ContextBarLabelButton(
                    label = "I",
                    contentDescription = stringResource(R.string.editor_action_italic),
                    fontStyle = FontStyle.Italic,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("*", "*", "")) }
                )
                ContextBarLabelButton(
                    label = "S",
                    contentDescription = stringResource(R.string.editor_action_strikethrough),
                    textDecoration = TextDecoration.LineThrough,
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("~~", "~~", "")) }
                )
                BrandIconButton(onClick = {
                    onFormatAction(MarkdownFormatAction.Wrap("==", "==", ""))
                }, modifier = Modifier.size(36.dp)) {
                    StrokeIcon(
                        icon = StrokeIcons.Highlighter,
                        contentDescription = stringResource(R.string.editor_action_highlight),
                        modifier = Modifier.size(18.dp)
                    )
                }
                BrandIconButton(onClick = {
                    onFormatAction(MarkdownFormatAction.Wrap("`", "`", ""))
                }, modifier = Modifier.size(36.dp)) {
                    StrokeIcon(
                        icon = StrokeIcons.Code,
                        contentDescription = stringResource(R.string.editor_action_inline_code),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Structure group
                BrandIconButton(onClick = {
                    onFormatAction(MarkdownFormatAction.LinePrefix("> "))
                }, modifier = Modifier.size(36.dp)) {
                    StrokeIcon(
                        icon = StrokeIcons.Quote,
                        contentDescription = stringResource(R.string.editor_action_blockquote),
                        modifier = Modifier.size(18.dp)
                    )
                }
                BrandIconButton(onClick = {
                    onFormatAction(MarkdownFormatAction.Insert("[", 1))
                }, modifier = Modifier.size(36.dp)) {
                    StrokeIcon(
                        icon = StrokeIcons.Link,
                        contentDescription = stringResource(R.string.editor_action_link),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Script group (using text labels since Superscript/Subscript icons require material-icons-extended)
                ContextBarLabelButton(
                    label = "X\u00B2",
                    contentDescription = stringResource(R.string.editor_action_superscript),
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("^", "^", "")) }
                )
                ContextBarLabelButton(
                    label = "X\u2082",
                    contentDescription = stringResource(R.string.editor_action_subscript),
                    onClick = { onFormatAction(MarkdownFormatAction.Wrap("~", "~", "")) }
                )
            }
        }
    }
}

/**
 * 上下文栏文本标签按钮。
 *
 * 用于没有对应 Material 图标的操作（如上标、下标），显示紧凑的文本标签。
 *
 * @param label 按钮显示文本（如 "X²", "X₂"）
 * @param contentDescription 无障碍描述
 * @param onClick 点击回调
 */
@Composable
private fun ContextBarLabelButton(
    label: String,
    contentDescription: String,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = DraftPeekTypography.bodySmall.copy(
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textDecoration = textDecoration,
                fontSize = 14.sp,
                color = PrototypeTokens.fg
            )
        )
    }
}
