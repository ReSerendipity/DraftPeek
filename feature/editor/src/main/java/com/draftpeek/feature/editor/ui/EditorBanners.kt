/**
 * 编辑器主界面模块。
 *
 * 提供代码编辑、Markdown预览、多文档查看（PDF/Office/媒体）、多标签页管理、
 * 搜索替换、快捷设置、代码片段等核心编辑功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

private const val TAG = "EditorScreen"

/**
 * 大文件警告横幅。
 *
 * 当打开的文件过大时，在编辑器内容区域顶部显示警告信息，
 * 提示用户可能存在性能问题。
 *
 * @param message 警告消息文本
 * @param modifier 修饰符
 * @param onDismiss 关闭横幅的回调
 */
@Composable
internal fun FileSizeWarningBanner(message: String, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val warning = PrototypeTokens.warning
    val warningContainer = warning.copy(alpha = 0.1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(warningContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = DraftPeekTypography.bodyMedium,
                color = warning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 大文件预览提示横幅。
 *
 * 在编辑模式下对大 Markdown 文件显示，提示用户可以切换到预览模式查看渲染效果。
 * 使用品牌强调色而非警告色，因为这不是错误状态，而是功能提示。
 *
 * @param onPreviewClick 点击"预览"按钮的回调
 * @param modifier 修饰符
 */
@Composable
internal fun LargeFilePreviewHintBanner(onPreviewClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = PrototypeTokens.accent
    val accentContainer = accent.copy(alpha = 0.1f)
    val fg = PrototypeTokens.fg
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(accentContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.editor_large_file_preview_hint),
                style = DraftPeekTypography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onPreviewClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.editor_preview),
                    style = DraftPeekTypography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 只读文件提示横幅。
 *
 * 在编辑器内容区域顶部显示，告知用户当前文件为只读状态无法编辑，
 * 并提示如何通过文件浏览器打开可编辑文件。用户可点击关闭按钮隐藏此提示。
 *
 * @param modifier 修饰符
 * @param onDismiss 关闭横幅的回调
 */
@Composable
internal fun ReadOnlyBanner(modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val info = PrototypeTokens.info
    val infoContainer = info.copy(alpha = 0.1f)
    val fg = PrototypeTokens.fg
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(infoContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "此文件为只读，不能修改。\n如需编辑，请复制内容后新建文件。",
                style = DraftPeekTypography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(PrototypeShapes.Medium)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                StrokeIcon(
                    icon = StrokeIcons.Close,
                    contentDescription = stringResource(R.string.editor_close_hint),
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
