/**
 * 编辑器状态栏组件。
 *
 * 位于编辑器底部，显示光标位置（行/列）、文件编码、编程语言、字数统计（Markdown），
 * 以及文件修改/已保存状态指示（彩色圆点+文字）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.EditorStatusBarStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors

/**
 * 编辑器状态栏 Composable。
 *
 * 匹配代码编辑器原型设计，左侧显示 "Ln X, Col X | 编码 | 语言"，
 * 右侧显示修改/已保存状态指示（彩色圆点+标签），Markdown 文件额外显示字数统计。
 *
 * @param line 当前光标行号（从 1 开始）
 * @param col 当前光标列号（从 1 开始）
 * @param encoding 文件编码（如 UTF-8）
 * @param language 编程语言名称
 * @param isModified 文件是否已修改未保存
 * @param words 字数统计（仅 Markdown 文件显示）
 * @param modifier 修饰符
 */
@Composable
fun EditorStatusBar(
    line: Int,
    col: Int,
    encoding: String,
    language: String,
    isModified: Boolean,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    words: Int? = null,
) {
    val surface = PrototypeTokens.surface
    val muted = PrototypeTokens.muted
    val borderColor = PrototypeTokens.border
    // Derived: only recomputes the color when isModified actually changes,
    // avoiding unnecessary recomposition for unrelated state reads.
    val indicatorColor = remember(isModified) {
        if (isModified) SemanticColors.Warning else SemanticColors.Success
    }

    Column(modifier = modifier) {
        HorizontalDivider(thickness = 1.dp, color = borderColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(surface)
                .padding(horizontal = PrototypeSpacing.EditorStatusBarPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Read-only indicator (leftmost, gray)
            if (isReadOnly) {
                Text("\u53ea\u8bfb", style = EditorStatusBarStyle.copy(color = muted))
                Spacer(modifier = Modifier.width(8.dp))
                Text("|", style = EditorStatusBarStyle.copy(color = muted))
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Line & column (HTML prototype: single text node "Ln X, Col X")
            Text("Ln $line, Col $col", style = EditorStatusBarStyle.copy(color = muted))
            Spacer(modifier = Modifier.width(8.dp))
            Text("|", style = EditorStatusBarStyle.copy(color = muted))
            Spacer(modifier = Modifier.width(8.dp))
            Text(encoding, style = EditorStatusBarStyle.copy(color = muted))
            Spacer(modifier = Modifier.width(8.dp))
            Text("|", style = EditorStatusBarStyle.copy(color = muted))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                language,
                style = EditorStatusBarStyle.copy(color = PrototypeTokens.accent, fontWeight = FontWeight.SemiBold),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Words count (for Markdown files)
            if (words != null) {
                Text("Words: $words", style = EditorStatusBarStyle.copy(color = muted))
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Modified / Saved indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(PrototypeSpacing.EditorModifiedDot)
                        .clip(PrototypeShapes.StatusCircle)
                        .background(indicatorColor),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isModified) "Modified" else "Saved",
                    style = EditorStatusBarStyle.copy(color = indicatorColor),
                )
            }
        }
    }
}
