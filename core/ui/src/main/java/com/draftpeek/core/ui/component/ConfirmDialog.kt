/**
 * DraftPeek 确认对话框组件。
 *
 * 设计层级：分子组件（Molecule）— 组合原子组件形成的功能单元。
 *
 * 用于高风险操作（删除、覆盖等）的可复用确认对话框。
 * 遵循 Material Design 3 错误预防指南，要求用户显式确认。
 *
 * 使用 DraftPeek 品牌样式：18dp 圆角，24dp/20dp/16dp 内容内边距，
 * 填充式确认按钮和 10dp 圆角的描边式取消按钮。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draftpeek.core.ui.theme.DialogBodyStyle
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 用于高风险操作的可复用确认对话框。
 *
 * 遵循 MD3 错误预防指南，要求用户显式确认。
 * 使用 DraftPeek 品牌样式：18dp 圆角，24dp/20dp/16dp 内容内边距，
 * 填充式确认按钮和描边式取消按钮（10dp 圆角）。
 *
 * @param title 对话框标题（例如"确认删除"）
 * @param message 解释操作后果的对话框消息
 * @param confirmLabel 确认按钮的标签（例如"删除"），默认为"确认"
 * @param isDestructive 操作是否具有破坏性（保留用于未来的错误样式）
 * @param onConfirm 用户确认时调用的回调
 * @param onDismiss 用户取消或关闭对话框时调用的回调
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "确认",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PrototypeTokens.surface,
            contentColor = PrototypeTokens.fg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 24.dp,
                        bottom = 16.dp,
                    ),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrototypeTokens.fg,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = DialogBodyStyle,
                    color = PrototypeTokens.fgSoft,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandOutlinedButton(
                        text = "取消",
                        onClick = onDismiss,
                    )
                    BrandFilledButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}
