/**
 * Markdown 文档大纲抽屉组件。
 *
 * 使用 ModalBottomSheet 显示文档目录（标题列表），点击标题可跳转到编辑器对应行。
 * 根据标题级别（H1-H6）自动缩进显示，支持多级标题层级展示。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.OutlineHeading

/**
 * Markdown 文档大纲抽屉 Composable。
 *
 * 使用 ModalBottomSheet 显示解析出的文档标题列表（目录），支持多级缩进。
 * 点击标题时回调行索引，供编辑器跳转使用。
 *
 * @param headings 从文档解析出的标题列表
 * @param onHeadingClick 标题点击回调，参数为标题所在行的 0-based 索引
 * @param onDismiss 请求关闭抽屉的回调
 * @param modifier 可选修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlineDrawer(
    headings: List<OutlineHeading>,
    onHeadingClick: (lineIndex: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = PrototypeTokens.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.editor_outline),
                style = MaterialTheme.typography.titleMedium,
                color = PrototypeTokens.fg,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (headings.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_no_headings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrototypeTokens.fgSoft,
                )
            } else {
                headings.forEach { heading ->
                    val indent = ((heading.level - 1) * 16).dp
                    val style = when (heading.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        3 -> MaterialTheme.typography.bodyLarge
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHeadingClick(heading.lineIndex) }
                            .padding(start = indent, top = 6.dp, bottom = 6.dp),
                    ) {
                        Text(
                            text = heading.text,
                            style = style,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
