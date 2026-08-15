/**
 * 编辑器滚动相关组件。
 *
 * 提供 ScrollToTopButton：浮动"回到顶部"按钮，滚动超过阈值时显示。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.draftpeek.core.ui.theme.PrototypeTokens
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.feature.editor.R
import io.github.rosemoe.sora.widget.CodeEditor

private const val TAG = "EditorScroll"

/**
 * 浮动"回到顶部"按钮 Composable。
 *
 * 当编辑器滚动超过阈值时显示，带淡入/淡出+滑入/滑出动画。
 * 点击后跳转到文档第一行。
 *
 * @param editor sora-editor CodeEditor 实例，用于控制滚动
 * @param hasScrolled 用户是否已向下滚动超过阈值
 * @param modifier 修饰符
 */
@Composable
fun ScrollToTopButton(
    editor: CodeEditor,
    hasScrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = hasScrolled,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = {
                editor.jumpToLine(0)
            },
            containerColor = PrototypeTokens.accentSoft,
            contentColor = PrototypeTokens.accent,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
            ),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.editor_scroll_to_top),
            )
        }
    }
}