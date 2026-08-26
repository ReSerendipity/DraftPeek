/**
 * 反向链接对话框组件。
 *
 * 展示引用当前文档标题的其他文件（反向链接 / backlink 列表）。
 * 点击某一项可回调来源文件 URI，由调用方决定如何导航（如打开该文件）。
 * 无反向链接时显示空状态提示。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 反向链接对话框。
 *
 * @param backlinks 当前文档的反向链接来源文件 URI 列表
 * @param onBacklinkClick 反向链接点击回调（参数为来源文件 URI）
 * @param onDismiss 关闭对话框回调
 */
@Composable
fun BacklinksDialog(
    backlinks: List<String>,
    onBacklinkClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PrototypeTokens.surface,
        title = {
            Text(text = stringResource(R.string.editor_backlinks), color = PrototypeTokens.fg)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 320.dp)) {
                if (backlinks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_backlinks_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrototypeTokens.fgSoft,
                    )
                } else {
                    backlinks.forEachIndexed { index, source ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBacklinkClick(source) }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = source,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrototypeTokens.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index < backlinks.lastIndex) {
                            HorizontalDivider(color = PrototypeTokens.border)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok), color = PrototypeTokens.accent)
            }
        },
    )
}