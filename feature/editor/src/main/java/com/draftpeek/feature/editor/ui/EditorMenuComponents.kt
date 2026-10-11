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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandIconButton
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.EditorStatusBarStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.viewmodel.CursorPosition

private const val TAG = "EditorScreen"


/**
 * 带工具提示的图标按钮。
 *
 * 对 [BrandIconButton] 的封装，在长按（或支持悬停的设备上鼠标悬停）时显示纯文本提示，
 * 用于帮助用户理解工具栏图标的功能含义。
 *
 * @param tooltip 提示文本内容
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param content 按钮内的图标内容 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        BrandIconButton(onClick = onClick, modifier = modifier) {
            content()
        }
    }
}

/**
 * 统一搜索面板（查找 / 替换 / 跳转到行三页签）。
 *
 * 替代原来三个独立的 BrandDialog，在单一对话框内通过页签切换。
 * 引擎侧复用 SoraSearchManager 已有的 search/replace/replaceAll/goToLine 能力。
 */
/** 搜索匹配计数的防抖窗口（毫秒）。 */
internal const val SEARCH_COUNT_DEBOUNCE_MS = 200L


/**
 * 溢出菜单内的分组标题（实施指导书 §2.3 屏 15–17）。
 *
 * 用弱化的小号文字，避免与菜单项争夺注意力；与 [HorizontalDivider] 一起形成组边界。
 */
@Composable
internal fun EditorMenuSectionLabel(text: String) {
    Text(
        text = text,
        style = DraftPeekTypography.labelSmall,
        color = PrototypeTokens.muted,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp)
    )
}


/**
 * 溢出菜单内的「一行双按钮」动作（撤销 / 重做）。
 *
 * 与 [DropdownMenuItem] 不同：它是等宽的「图标 + 文字」竖排块，两个并排占满一行，
 * 比两行菜单项更紧凑。
 */
@Composable
internal fun EditorMenuActionButton(
    icon: StrokeIconDef,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(PrototypeShapes.Small)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StrokeIcon(
            icon = icon,
            contentDescription = null,
            tint = PrototypeTokens.fgSoft,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = DraftPeekTypography.labelSmall,
            color = PrototypeTokens.fgSoft
        )
    }
}


/**
 * 编辑器底部状态栏。
 *
 * 显示当前光标位置（行:列）、文件编码、编程语言、只读状态、修改标记等信息。
 *
 * @param cursorPosition 当前光标位置
 * @param detectedEncoding 检测到的文件编码
 * @param uiState 编辑器 UI 状态
 * @param isModified 文件是否已修改
 * @param tabWidth 缩进宽度（来自设置）
 * @param selectionCount 选中字符数（0 表示无选区）
 * @param isReadOnlyOverride 手动只读覆盖状态
 * @param onLanguageClick 语言点击回调
 * @param onReadOnlyToggle 只读/编辑切换回调
 * @param onLineEndingClick 行尾符点击回调
 * @param onEncodingClick 编码段点击回调（直达编码选择面板）
 * @param modifier 修饰符
 */
@Composable
internal fun EditorBottomStatusBar(
    cursorPosition: CursorPosition,
    detectedEncoding: String?,
    uiState: EditorUiState,
    isModified: Boolean,
    changedLineCount: Int = 0,
    tabWidth: Int = 4,
    selectionCount: Int = 0,
    isReadOnlyOverride: Boolean = false,
    onLanguageClick: () -> Unit = {},
    onReadOnlyToggle: () -> Unit = {},
    onLineEndingClick: () -> Unit = {},
    onEncodingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent

    // Detect line ending style from content (CRLF vs LF)
    val detectedLineEnding = remember(uiState) {
        val content = (uiState as? EditorUiState.Success)?.content ?: ""
        when {
            content.contains("\r\n") -> "CRLF"
            content.contains("\n") -> "LF"
            content.contains("\r") -> "CR"
            else -> "LF"
        }
    }

    // Effective read-only state (file property OR manual override)
    val fileReadOnly = (uiState as? EditorUiState.Success)?.isReadOnly == true
    val effectiveReadOnly = fileReadOnly || isReadOnlyOverride

    val positionText = "Ln ${cursorPosition.line}, Col ${cursorPosition.column}"
    val encodingText = detectedEncoding ?: "UTF-8"
    val indentText = stringResource(R.string.editor_status_indent_spaces, tabWidth)

    HorizontalDivider(thickness = 1.dp, color = border)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surface)
            .padding(horizontal = PrototypeSpacing.EditorStatusBarPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Read-only / Edit toggle (clickable)
        val readOnlyLabel = if (effectiveReadOnly) {
            stringResource(R.string.editor_read_only)
        } else {
            stringResource(R.string.editor_status_editable)
        }
        Text(
            text = readOnlyLabel,
            style = EditorStatusBarStyle,
            color = if (effectiveReadOnly) muted else accent,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReadOnlyToggle
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        // 未保存提示（实施指导书 §2.3 屏 15）：由「● + 已修改」改为文字，保存后消失。
        // 变更行数来自 EditorStateManager.changedLineCount（相对上次保存基线做行级 diff，
        // 与脏标记同走防抖）；-1 表示文件过大未统计，此时只显示「未保存」。
        if (isModified) {
            Text(
                text = if (changedLineCount >= 0) {
                    stringResource(R.string.editor_status_unsaved_lines, changedLineCount)
                } else {
                    stringResource(R.string.editor_status_unsaved)
                },
                style = EditorStatusBarStyle,
                color = accent
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        // Selection count (only when there is a selection)
        if (selectionCount > 0) {
            Text(
                text = stringResource(R.string.editor_status_selection_count, selectionCount),
                style = EditorStatusBarStyle,
                color = muted
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        // Language (clickable)
        if (uiState is EditorUiState.Success) {
            val lang = uiState.language
            if (lang != null) {
                val displayLang = if (lang.equals("markdown", ignoreCase = true)) "Markdown" else lang.uppercase()
                Text(
                    text = "$displayLang ▾",
                    style = EditorStatusBarStyle,
                    color = accent,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLanguageClick
                    )
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        // Indent (Spaces: N)
        Text(
            text = indentText,
            style = EditorStatusBarStyle,
            color = muted
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Encoding (UTF-8, etc.) — 可点击直达编码面板（实施指导书 §2.3 屏 15）
        Text(
            text = "$encodingText ▾",
            style = EditorStatusBarStyle,
            color = muted,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEncodingClick
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Line ending (LF / CRLF) — clickable
        Text(
            text = detectedLineEnding,
            style = EditorStatusBarStyle,
            color = muted,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onLineEndingClick
            )
        )
        Spacer(modifier = Modifier.width(14.dp))
        // Cursor position (Ln X, Col Y) — rightmost in VSCode style
        Text(
            text = positionText,
            style = EditorStatusBarStyle,
            color = muted
        )
    }
}


/** Small pill-style label used in the status bar (e.g. "Read Only"). */
@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = EditorStatusBarStyle,
        color = color,
        modifier = Modifier
            .background(
                PrototypeTokens.elevated,
                PrototypeShapes.Small
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
