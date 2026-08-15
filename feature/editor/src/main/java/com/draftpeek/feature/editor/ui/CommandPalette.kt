/**
 * 命令面板组件。
 *
 * 全屏覆盖的命令面板，提供搜索框和可滚动的命令列表（按类别分组）。
 * 支持模糊搜索（按标签、类别、ID 匹配）、键盘导航（上下箭头选择、Enter 执行、Esc 关闭）。
 * 选中命令后执行其 action 并关闭面板。快捷键显示在命令右侧。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.MetaStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * 命令面板中的单个可执行命令项。
 *
 * @property id 稳定标识符，同时用作模糊搜索的匹配项
 * @property label 用户可见的命令名称
 * @property category 分组标签（如"文件"、"编辑"、"视图"）
 * @property shortcut 可选的键盘快捷键显示文本（仅用于展示，如 "Ctrl+S"）
 * @property action 选中命令时执行的回调
 */
data class CommandItem(
    val id: String,
    val label: String,
    val category: String,
    val shortcut: String? = null,
    val action: () -> Unit,
)

/**
 * 编辑器可暴露给命令面板的所有可执行操作集合。
 *
 * 使用单个参数对象封装，方便后续添加命令时保持构建器签名稳定。
 */
data class EditorCommandActions(
    val onSave: () -> Unit,
    val onNewFile: () -> Unit,
    val onOpenFile: () -> Unit,
    val onExport: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onFind: () -> Unit,
    val onReplace: () -> Unit,
    val onSelectAll: () -> Unit,
    val onTogglePreview: () -> Unit,
    val onToggleFocusMode: () -> Unit,
    val onToggleLineNumbers: () -> Unit,
    val onToggleWordWrap: () -> Unit,
    val onToggleStickyScroll: () -> Unit,
    val onGoToLine: () -> Unit,
    val onToggleOutline: () -> Unit,
    val onNextTab: () -> Unit,
    val onPreviousTab: () -> Unit,
    val onDiff: () -> Unit,
    val onSnippet: () -> Unit,
    val onToggleTheme: () -> Unit,
    val onCloseTab: () -> Unit,
    val onCloseAllTabs: () -> Unit,
    val onToggleMinimap: () -> Unit,
    /** Ch1 Item 18 (P3): Jump to the last edit location. */
    val onGoToLastEditLocation: () -> Unit,
    /** Ch1 Item 19 (P3): Toggle the symbol panel. */
    val onToggleSymbolPanel: () -> Unit,
    /** Ch6 Item 16 (P3): Show Markdown syntax reference. */
    val onShowMarkdownCheatSheet: () -> Unit,
)

/**
 * 构建完整的编辑器命令列表，按类别分组。
 *
 * 从字符串资源解析本地化标签，包含文件、编辑、视图、导航、工具、帮助等类别的常用命令。
 *
 * @param actions 编辑器命令动作集合
 * @return 命令项列表
 */
@Composable
fun buildEditorCommands(actions: EditorCommandActions): List<CommandItem> {
    val file = stringResource(R.string.editor_file)
    val edit = stringResource(R.string.editor_category_edit)
    val view = stringResource(R.string.editor_category_view)
    val navigation = stringResource(R.string.editor_category_navigation)
    val tools = stringResource(R.string.editor_category_tools)
    val help = stringResource(R.string.editor_category_help)

    return listOf(
        // File
        CommandItem("file.save", stringResource(R.string.editor_save), file, "Ctrl+S", actions.onSave),
        CommandItem("file.new", stringResource(R.string.editor_action_new_file), file, "Ctrl+N", actions.onNewFile),
        CommandItem("file.open", stringResource(R.string.editor_action_open_file), file, "Ctrl+O", actions.onOpenFile),
        CommandItem("file.export", stringResource(R.string.editor_export), file, "Ctrl+E", actions.onExport),
        // Edit
        CommandItem("edit.undo", stringResource(R.string.editor_undo), edit, "Ctrl+Z", actions.onUndo),
        CommandItem("edit.redo", stringResource(R.string.editor_redo), edit, "Ctrl+Y", actions.onRedo),
        CommandItem("edit.find", stringResource(R.string.editor_find), edit, "Ctrl+F", actions.onFind),
        CommandItem("edit.replace", stringResource(R.string.editor_replace), edit, "Ctrl+H", actions.onReplace),
        CommandItem("edit.select_all", stringResource(R.string.editor_select_all), edit, "Ctrl+A", actions.onSelectAll),
        // View
        CommandItem("view.toggle_preview", stringResource(R.string.editor_toggle_preview), view, "Ctrl+P", actions.onTogglePreview),
        CommandItem("view.toggle_focus_mode", stringResource(R.string.editor_focus_mode), view, null, actions.onToggleFocusMode),
        CommandItem("view.toggle_line_numbers", stringResource(R.string.editor_show_line_numbers), view, null, actions.onToggleLineNumbers),
        CommandItem("view.toggle_word_wrap", stringResource(R.string.editor_word_wrap), view, null, actions.onToggleWordWrap),
        CommandItem("view.toggle_sticky_scroll", stringResource(R.string.editor_sticky_scroll), view, null, actions.onToggleStickyScroll),
        CommandItem("view.toggle_minimap", stringResource(R.string.editor_command_palette_toggle_minimap), view, null, actions.onToggleMinimap),
        CommandItem("view.toggle_symbol_panel", stringResource(R.string.editor_command_palette_toggle_symbol_panel), view, null, actions.onToggleSymbolPanel),
        // Navigation
        CommandItem("nav.go_to_line", stringResource(R.string.editor_go_to_line), navigation, "Ctrl+G", actions.onGoToLine),
        CommandItem("navigation.last_edit_location", stringResource(R.string.editor_command_palette_goto_last_edit), navigation, "Ctrl+Alt+Left", actions.onGoToLastEditLocation),
        CommandItem("nav.toggle_outline", stringResource(R.string.editor_outline), navigation, "Ctrl+Shift+O", actions.onToggleOutline),
        CommandItem("nav.next_tab", stringResource(R.string.editor_next_tab), navigation, "Ctrl+Tab", actions.onNextTab),
        CommandItem("nav.previous_tab", stringResource(R.string.editor_previous_tab), navigation, "Ctrl+Shift+Tab", actions.onPreviousTab),
        CommandItem("nav.close_tab", stringResource(R.string.editor_command_palette_close_tab), navigation, "Ctrl+W", actions.onCloseTab),
        CommandItem("nav.close_all_tabs", stringResource(R.string.editor_command_palette_close_all_tabs), navigation, null, actions.onCloseAllTabs),
        // Tools
        CommandItem("tools.diff", stringResource(R.string.editor_diff), tools, null, actions.onDiff),
        CommandItem("tools.snippet", stringResource(R.string.editor_action_snippets), tools, null, actions.onSnippet),
        CommandItem("tools.toggle_theme", stringResource(R.string.editor_toggle_theme), tools, null, actions.onToggleTheme),
        // Help
        CommandItem("help.markdown_cheat_sheet", stringResource(R.string.editor_command_markdown_cheat_sheet), help, null, actions.onShowMarkdownCheatSheet),
    )
}

/**
 * 全屏命令面板覆盖层 Composable。
 *
 * 顶部显示搜索框（使用 BrandOutlinedTextField），下方显示可滚动、按类别分组的命令列表。
 * 选中命令会执行其 action 并关闭面板。支持按标签、类别或 ID 模糊匹配。
 *
 * 键盘导航：
 * - 上/下箭头：移动选中项
 * - Enter：执行选中的命令
 * - Escape：关闭面板
 *
 * @param commands 可用命令的完整集合
 * @param onDismiss 用户关闭面板时的回调（返回键、点击遮罩、或执行命令后）
 * @param modifier 修饰符
 */
@Composable
fun CommandPalette(
    commands: List<CommandItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    val pageBg = PrototypeTokens.pageBackground
    val surface = PrototypeTokens.surface
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val accent = PrototypeTokens.accent
    val surfaceHover = PrototypeTokens.surfaceHover

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Filter by label/category/id (case-insensitive contains), preserving order.
    val filtered = remember(query, commands) {
        if (query.isBlank()) {
            commands
        } else {
            val q = query.trim().lowercase()
            commands.filter {
                it.label.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.id.lowercase().contains(q)
            }
        }
    }

    // Group by category while preserving first-appearance order.
    val grouped = remember(filtered) { filtered.groupBy { it.category } }

    // Build a flat list of display items for indexed access in the LazyColumn.
    data class PaletteItem(val category: String?, val command: CommandItem?)
    val flatItems = remember(grouped) {
        val items = mutableListOf<PaletteItem>()
        grouped.forEach { (category, cmds) ->
            items.add(PaletteItem(category, null))
            cmds.forEach { cmd -> items.add(PaletteItem(null, cmd)) }
        }
        items.toList()
    }

    // Count only command items (not category headers) for selection bounds.
    val commandCount = filtered.size

    // Clamp selectedIndex when filtered list changes.
    LaunchedEffect(commandCount) {
        if (commandCount == 0) {
            selectedIndex = 0
        } else if (selectedIndex >= commandCount) {
            selectedIndex = commandCount - 1
        }
    }

    // Scroll the LazyColumn to keep the selected item visible.
    LaunchedEffect(selectedIndex, flatItems) {
        if (flatItems.isEmpty() || commandCount == 0) return@LaunchedEffect
        // Find the LazyColumn index of the selectedIndex-th command.
        var commandCounter = 0
        for ((lazyIdx, item) in flatItems.withIndex()) {
            if (item.command != null) {
                if (commandCounter == selectedIndex) {
                    lazyListState.animateScrollToItem(lazyIdx)
                    break
                }
                commandCounter++
            }
        }
    }

    fun runCommand(command: CommandItem) {
        focusManager.clearFocus()
        onDismiss()
        command.action()
    }

    // Handle hardware key events on the search field.
    fun handleKeyEvent(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        return when (keyEvent.key) {
            Key.DirectionUp -> {
                if (selectedIndex > 0) selectedIndex-- else selectedIndex = 0
                true
            }
            Key.DirectionDown -> {
                if (selectedIndex < commandCount - 1) selectedIndex++
                true
            }
            Key.Enter -> {
                if (commandCount > 0 && selectedIndex in 0 until commandCount) {
                    filtered.getOrNull(selectedIndex)?.let { runCommand(it) }
                }
                true
            }
            Key.Escape -> {
                onDismiss()
                true
            }
            else -> false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(pageBg.copy(alpha = 0.92f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                shape = BrandShapes.Dialog,
                color = surface,
                contentColor = fg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 48.dp)
                    .fillMaxHeight(0.7f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search field using BrandOutlinedTextField
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.editor_command_palette),
                            tint = muted,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BrandOutlinedTextField(
                            value = query,
                            onValueChange = {
                                if (it.length <= 100) {
                                    query = it
                                    selectedIndex = 0
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { event -> handleKeyEvent(event) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.editor_search_commands),
                                    color = muted,
                                )
                            },
                            singleLine = true,
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(min = 1.dp, max = 1.dp)
                            .background(border),
                    )

                    // Command list
                    if (grouped.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.editor_no_match_found),
                                style = DraftPeekTypography.bodyMedium.copy(color = muted),
                            )
                        }
                    } else {
                        // Track which flat index corresponds to each command for selection highlight.
                        var commandIndexCounter = 0
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                vertical = 8.dp,
                            ),
                        ) {
                            flatItems.forEachIndexed { flatIdx, item ->
                                if (item.category != null) {
                                    item(key = "header_${item.category}") {
                                        Text(
                                            text = item.category,
                                            style = MetaStyle.copy(color = muted),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 16.dp,
                                                    vertical = 8.dp,
                                                ),
                                        )
                                    }
                                } else if (item.command != null) {
                                    val cmdIndex = commandIndexCounter
                                    commandIndexCounter++
                                    item(key = item.command.id) {
                                        CommandRow(
                                            command = item.command,
                                            isSelected = cmdIndex == selectedIndex,
                                            onClick = { runCommand(item.command) },
                                            surfaceHover = surfaceHover,
                                            border = border,
                                            fg = fg,
                                            muted = muted,
                                            fgSoft = fgSoft,
                                            accent = accent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 命令面板中的单行命令项 Composable。
 *
 * 显示命令名称，如有快捷键则在右侧显示快捷键标签。选中项高亮显示。
 *
 * @param command 命令项数据
 * @param isSelected 是否为当前选中项
 * @param onClick 点击回调
 * @param surfaceHover 悬停/选中背景色
 * @param border 边框颜色
 * @param fg 前景文字色
 * @param muted 次要文字色
 * @param fgSoft 柔和文字色
 * @param accent 强调色
 */
@Composable
private fun CommandRow(
    command: CommandItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    surfaceHover: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
    fgSoft: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
) {
    val rowBg = if (isSelected) surfaceHover else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(rowBg, PrototypeShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = command.label,
            style = DraftPeekTypography.bodyMedium.copy(
                color = if (isSelected) accent else fg,
            ),
            modifier = Modifier.weight(1f),
        )
        if (command.shortcut != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(PrototypeShapes.Small)
                    .background(surfaceHover)
                    .border(1.dp, border, PrototypeShapes.Small)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = command.shortcut,
                    style = DraftPeekTypography.labelSmall.copy(
                        color = fgSoft,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
