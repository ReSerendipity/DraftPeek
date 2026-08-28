/**
 * DraftPeek 品牌对话框组件。
 *
 * 设计层级：分子组件（Molecule）— 组合原子组件形成的功能单元。
 *
 * 提供品牌风格的对话框组件，统一应用 DraftPeek 设计语言。
 * 包含两种使用方式：
 * - [BrandDialog]：支持自定义标题、内容和按钮插槽的通用对话框
 * - 便捷重载：接受字符串参数，快速构建确认/取消对话框
 *
 * 对话框统一使用 18dp 圆角和品牌配色方案。
 * **禁止直接使用 Material3 AlertDialog，必须使用本文件中的品牌组件。**
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DialogBodyStyle
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * DraftPeek 品牌对话框 Composable。
 *
 * 使用自定义 [Dialog] + [Surface] 布局，强制应用原型设计规范：
 * 18dp 圆角，内容内边距（上 24dp、左右 20dp、下 16dp）。
 *
 * @param onDismissRequest 用户点击对话框外部或按返回键时的回调
 * @param title 对话框标题区域的 Composable 内容
 * @param content 对话框正文区域的可选 Composable 内容
 * @param confirmButton 确认按钮区域的 Composable 内容
 * @param dismissButton 取消按钮区域的可选 Composable 内容
 * @param shape 对话框圆角形状，默认为 [BrandShapes.Dialog]（18dp）
 */
@Composable
fun BrandDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit),
    content: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = BrandShapes.Dialog
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = shape,
            color = PrototypeTokens.surface,
            contentColor = PrototypeTokens.fg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 24.dp,
                        bottom = 16.dp
                    )
            ) {
                title()

                if (content != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                    }
                    confirmButton()
                }
            }
        }
    }
}

/**
 * [BrandDialog] 的便捷重载版本，接受简单的字符串参数用于常见的确认/取消工作流。
 *
 * 提供双按钮布局：
 * - **确认按钮**：[BrandFilledButton]。当 [isDestructive] 为 true 时，使用错误色样式表示风险操作。
 * - **取消按钮**：[BrandOutlinedButton]，文字为"取消"。
 *
 * @param onDismissRequest 用户点击对话框外部或按返回键时的回调
 * @param title 对话框标题文本
 * @param message 解释操作后果的对话框正文文本
 * @param confirmLabel 确认按钮文字，默认为"确认"
 * @param isDestructive 是否为破坏性操作，为 true 时确认按钮使用错误色样式
 * @param onConfirm 用户点击确认按钮时的回调
 * @param onDismiss 用户点击取消按钮时的回调
 */
@Composable
fun BrandDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String = "确认",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BrandDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = PrototypeTokens.fg
            )
        },
        content = {
            Text(
                text = message,
                style = DialogBodyStyle,
                color = PrototypeTokens.fgSoft
            )
        },
        confirmButton = {
            BrandFilledButton(
                text = confirmLabel,
                onClick = onConfirm
            )
        },
        dismissButton = {
            BrandOutlinedButton(
                text = "取消",
                onClick = onDismiss
            )
        }
    )
}
