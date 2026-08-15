/**
 * DraftPeek 快捷设置底部表单组件。
 *
 * 设计层级：分子组件（Molecule）— 组合原子组件形成的功能单元。
 *
 * 样式匹配 HTML 原型的模态底部表单：遮罩背景、20dp 顶部圆角、居中拖拽手柄、
 * 标题、分割线和调用方提供的内容插槽（通常是 [BrandSettingRow] 列或快捷操作按钮）。
 *
 * 使用全宽 [Dialog] 而非 ModalBottomSheet，以避免拖拽手柄冲突，并保持表单内容与原型完全一致。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 样式匹配 HTML 原型的模态底部表单：遮罩背景，20dp 顶部圆角，
 * 居中拖拽手柄，标题，分割线，以及调用方提供的内容插槽（通常是 [BrandSettingRow] 列或快捷操作按钮）。
 *
 * 使用全宽 [Dialog] 而非 ModalBottomSheet 以避免拖拽手柄冲突，
 * 并保持表单内容严格符合原型所示。
 *
 * @param onDismiss 用户点击表单外部或按返回键时调用
 * @param content 表单主体内容，在 [ColumnScope] 中布局
 */
@Composable
fun QuickSettingsBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume clicks so taps inside don't dismiss */ },
                    ),
                shape = RoundedCornerShape(
                    topStart = PrototypeSpacing.BottomSheetTopRadius,
                    topEnd = PrototypeSpacing.BottomSheetTopRadius,
                ),
                color = PrototypeTokens.surface,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 32.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(PrototypeSpacing.SheetHandleWidth)
                            .height(PrototypeSpacing.SheetHandleHeight)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PrototypeTokens.border)
                            .align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Quick Settings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrototypeTokens.fg,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp),
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = PrototypeTokens.borderSoft,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    content()
                }
            }
        }
    }
}
