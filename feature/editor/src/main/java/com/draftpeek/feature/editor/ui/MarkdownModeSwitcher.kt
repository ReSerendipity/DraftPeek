/**
 * Markdown 视图模式分段切换器（MarkdownModeSwitcher）。
 *
 * 对应 HTML 原型 index.html 中的 `.mode-switch` 组件：
 * 以分段按钮组形式提供 编辑 / 预览 / 分屏（以及可选的 富文本）模式，
 * 点击哪个按钮就直接进入哪个模式，不再需要循环切换。
 *
 * 视觉规范（来自原型）：
 * - 容器：8dp 圆角，背景 --bg，2dp 内边距
 * - 按钮：6dp 圆角，未选中 muted 色，选中 surface 背景 + accent 色
 * - 字号 11sp，字重 600
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.designsystem.theme.DraftPeekTypography
import com.draftpeek.core.designsystem.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.MarkdownViewMode

/**
 * Markdown 视图模式分段切换器。
 *
 * @param currentMode 当前模式，决定哪个按钮高亮
 * @param showWysiwyg 是否显示"富文本"（WYSIWYG）按钮，大文件或功能关闭时不显示
 * @param onSelect 用户点击某个模式时回调（直接切换到该模式）
 * @param modifier 布局修饰符
 * @param compact 紧凑模式：顶栏使用更小的内边距和字号
 */
@Composable
fun MarkdownModeSwitcher(
    currentMode: MarkdownViewMode,
    showWysiwyg: Boolean,
    onSelect: (MarkdownViewMode) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val options = buildList {
        add(MarkdownViewMode.EDIT to stringResource(R.string.editor_edit))
        add(MarkdownViewMode.PREVIEW to stringResource(R.string.editor_preview))
        add(MarkdownViewMode.SPLIT to stringResource(R.string.editor_split_view))
        if (showWysiwyg) {
            add(MarkdownViewMode.WYSIWYG to stringResource(R.string.editor_rich_editor_mode))
        }
    }

    val horizontalPadding = if (compact) 6.dp else 10.dp
    val verticalPadding = if (compact) 3.dp else 5.dp
    val outerPadding = if (compact) 4.dp else 16.dp
    val outerVertical = if (compact) 2.dp else 6.dp
    val fontSize = if (compact) 10.sp else 11.sp

    Row(
        modifier = modifier.padding(horizontal = outerPadding, vertical = outerVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrototypeTokens.bg)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (mode, label) ->
                val isSelected = mode == currentMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PrototypeTokens.surface else Color.Transparent)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                        .semantics {
                            role = Role.Button
                            contentDescription = label
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = DraftPeekTypography.labelMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) PrototypeTokens.accent else PrototypeTokens.muted,
                        ),
                    )
                }
            }
        }
    }
}
