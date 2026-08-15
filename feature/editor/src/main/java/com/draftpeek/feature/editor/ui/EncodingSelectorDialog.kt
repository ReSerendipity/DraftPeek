/**
 * 文件编码选择对话框组件。
 *
 * 提供常用编码列表（UTF-8、GB18030、GBK、GB2312、UTF-16BE、UTF-16LE、ISO-8859-1、Big5、Shift_JIS、EUC-KR），
 * 用户选择编码后回调。包含完整版和快速选择版（仅显示最常用编码）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.feature.editor.R
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 用户可选择的常用编码列表。
 */
val COMMON_ENCODINGS = listOf(
    "UTF-8",
    "GB18030",
    "GBK",
    "GB2312",
    "UTF-16BE",
    "UTF-16LE",
    "ISO-8859-1",
    "Big5",
    "Shift_JIS",
    "EUC-KR",
)

/**
 * 文件编码选择对话框 Composable。
 *
 * 当文件编码检测失败或需要手动指定编码时显示，使用单选按钮列表让用户选择编码。
 *
 * @param encodings 要显示的可用编码列表
 * @param defaultEncoding 当前检测到/建议的默认编码
 * @param onDismissRequest 对话框关闭回调
 * @param onEncodingSelected 用户选择编码后的回调，参数为选中的编码名称
 */
@Composable
fun EncodingSelectorDialog(
    encodings: List<String> = COMMON_ENCODINGS,
    defaultEncoding: String = "UTF-8",
    onDismissRequest: () -> Unit,
    onEncodingSelected: (String) -> Unit,
) {
    var selectedEncoding by rememberSaveable { mutableStateOf(defaultEncoding) }

    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    BrandDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.editor_encoding_select_title), style = DraftPeekTypography.titleLarge.copy(color = fg)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "文件编码检测失败或需要手动指定编码。请选择正确的编码格式重新打开文件。",
                    style = DraftPeekTypography.bodyMedium,
                    color = muted,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "可用编码：",
                    style = DraftPeekTypography.labelMedium,
                    color = fg,
                )
                Spacer(modifier = Modifier.height(8.dp))
                encodings.forEach { encoding ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEncoding = encoding }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedEncoding == encoding,
                            onClick = { selectedEncoding = encoding },
                            colors = RadioButtonDefaults.colors(selectedColor = PrototypeTokens.accent),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = encoding,
                            style = DraftPeekTypography.bodyLarge,
                            color = fg,
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = "确定", onClick = { onEncodingSelected(selectedEncoding) })
        },
        dismissButton = {
            BrandOutlinedButton(text = "取消", onClick = onDismissRequest)
        },
    )
}

/**
 * 快速编码选择器 Composable。
 *
 * 简化版编码选择对话框，仅显示最常用的 7 种编码（UTF-8、GB18030、GBK、GB2312、UTF-16BE、UTF-16LE、ISO-8859-1），供快速访问使用。
 *
 * @param defaultEncoding 默认选中的编码
 * @param onDismissRequest 对话框关闭回调
 * @param onEncodingSelected 用户选择编码后的回调
 */
@Composable
fun QuickEncodingSelector(
    defaultEncoding: String = "UTF-8",
    onDismissRequest: () -> Unit,
    onEncodingSelected: (String) -> Unit,
) {
    val quickEncodings = listOf("UTF-8", "GB18030", "GBK", "GB2312", "UTF-16BE", "UTF-16LE", "ISO-8859-1")
    EncodingSelectorDialog(
        encodings = quickEncodings,
        defaultEncoding = defaultEncoding,
        onDismissRequest = onDismissRequest,
        onEncodingSelected = onEncodingSelected,
    )
}
