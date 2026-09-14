/**
 * 品牌组件功能性预览集合。
 *
 * 对应评估报告 P2⑨「全仓 0 个功能性 @Preview」。此前唯一的 @Preview 位于
 * DraftPeekLocals.kt（仅文档示意），设计迭代与 a11y 审查没有实时预览可用。
 *
 * 约定（docs/agents/CODE_STYLE.md §2.2）：
 * - 每个组件至少一组亮色 + 一组暗色（uiMode = UI_MODE_NIGHT_YES）；
 * - Screen 级必须带暗色预览；组件级同样补齐，便于比对双主题对比度；
 * - 预览必须走 DraftPeekTheme（Brand 组件依赖 PrototypeTokens，
 *   而它是通过 CompositionLocal 提供的 @Composable 属性，裸渲染会拿不到色板）。
 *
 * 本文件只做预览，不新增任何运行时行为。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.draftpeek.core.designsystem.theme.DraftPeekTheme
import com.draftpeek.core.ui.icon.StrokeIcons

/**
 * 暗色/亮色主题开关的预览参数。
 *
 * 让每个 @Composable 预览体只需写一次 DraftPeekTheme(darkTheme = darkTheme)，
 * 由同一份代码同时产出两套主题的预览图。
 */
private class DarkModeProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}

/**
 * 所有品牌组件预览的统一外框：主题 + Surface + 内边距。
 */
@Composable
private fun PreviewFrame(darkTheme: Boolean, content: @Composable () -> Unit) {
    DraftPeekTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

// ==================== BrandFilledButton ====================

/**
 * 品牌填充按钮：enabled 与 disabled 两态 × 亮/暗双主题。
 */
@Composable
@Preview(name = "BrandFilledButton", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandFilledButtonPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandFilledButton(text = "Save", onClick = {})
        BrandFilledButton(text = "Disabled", onClick = {}, enabled = false)
    }
}

// ==================== BrandButton (Outlined / Tonal) ====================

/**
 * 品牌按钮另外两种样式：描边按钮与色调按钮（含禁用态）。
 */
@Composable
@Preview(name = "BrandButton", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandButtonPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandOutlinedButton(text = "取消", onClick = {})
        BrandTonalButton(text = "次要操作", onClick = {})
        BrandTonalButton(text = "禁用", onClick = {}, enabled = false)
    }
}

// ==================== BrandIconButton ====================

/**
 * 图标按钮：普通态与带角标态。
 */
@Composable
@Preview(name = "BrandIconButton", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandIconButtonPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandIconButton(
            icon = Icons.Filled.Close,
            onClick = {},
            contentDescription = "关闭"
        )
        BrandIconButton(
            icon = Icons.Filled.Close,
            onClick = {},
            contentDescription = "关闭（带角标）",
            badge = 3
        )
    }
}

// ==================== BrandSwitch ====================

/**
 * 开关：开 / 关两态（状态由预览内部 remember 持有，属于 UI 临时状态，符合状态提升约定）。
 */
@Composable
@Preview(name = "BrandSwitch", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandSwitchPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    var checked by remember { mutableStateOf(true) }
    PreviewFrame(darkTheme = darkTheme) {
        BrandSwitch(checked = checked, onCheckedChange = { checked = it })
        BrandSwitch(checked = !checked, onCheckedChange = { checked = !it })
    }
}

// ==================== BrandChip ====================

/**
 * 筛选 Chip：选中 / 未选中。
 */
@Composable
@Preview(name = "BrandChip", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandChipPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    var selected by remember { mutableStateOf(true) }
    PreviewFrame(darkTheme = darkTheme) {
        BrandChip(text = "Markdown", selected = selected, onClick = { selected = !selected })
        BrandChip(text = "代码", selected = !selected, onClick = {})
    }
}

// ==================== BrandPill ====================

/**
 * 状态胶囊。
 */
@Composable
@Preview(name = "BrandPill", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandPillPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandPill(text = "已同步")
        BrandPill(text = "草稿")
    }
}

// ==================== BrandOutlinedTextField ====================

/**
 * 输入框：常规、错误态、禁用态。
 */
@Composable
@Preview(name = "BrandOutlinedTextField", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandOutlinedTextFieldPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    var value by remember { mutableStateOf("draft-peek.md") }
    PreviewFrame(darkTheme = darkTheme) {
        BrandOutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("文件名") },
            modifier = Modifier.width(240.dp)
        )
        BrandOutlinedTextField(
            value = "已存在的文件名",
            onValueChange = {},
            label = { Text("文件名") },
            isError = true,
            supportingText = { Text("该名称已被占用") },
            modifier = Modifier.width(240.dp)
        )
        BrandOutlinedTextField(
            value = "只读",
            onValueChange = {},
            label = { Text("文件名") },
            enabled = false,
            modifier = Modifier.width(240.dp)
        )
    }
}

// ==================== BrandDialog ====================

/**
 * 品牌对话框：破坏性确认对话框（便捷字符串重载）。
 * 预览中 [androidx.compose.ui.window.Dialog] 会以独立悬浮窗形态渲染。
 */
@Composable
@Preview(name = "BrandDialog", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandDialogPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandDialog(
            onDismissRequest = {},
            title = "删除文件",
            message = "此操作将永久删除 draft-peek.md，且无法撤销。",
            isDestructive = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

// ==================== BrandFAB ====================

/**
 * 品牌悬浮按钮（speed-dial）：折叠态主按钮 + 两个可展开菜单项。
 */
@Composable
@Preview(name = "BrandFAB", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandFABPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandFAB(
            items = listOf(
                FABMenuItem(icon = StrokeIcons.File, label = "新建笔记", onClick = {}),
                FABMenuItem(icon = StrokeIcons.FolderOutline, label = "导入目录", onClick = {})
            )
        )
    }
}

// ==================== BrandFileCard ====================

/**
 * 文件卡片：普通态与多选选中态。
 */
@Composable
@Preview(name = "BrandFileCard", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandFileCardPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandFileCard(
            fileName = "draft-peek.md",
            extension = "md",
            metaText = "12 KB · 2 分钟前",
            onClick = {}
        )
        BrandFileCard(
            fileName = "todo-list.md",
            extension = "md",
            metaText = "3 KB · 昨天",
            isSelected = true,
            onClick = {}
        )
    }
}

// ==================== BrandSearchBar ====================

/**
 * 搜索栏：占位提示态与已输入态。
 */
@Composable
@Preview(name = "BrandSearchBar", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandSearchBarPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    var query by remember { mutableStateOf("") }
    PreviewFrame(darkTheme = darkTheme) {
        BrandSearchBar(
            value = query,
            onValueChange = { query = it },
            placeholder = "搜索文件…"
        )
        BrandSearchBar(
            value = "draft",
            onValueChange = {},
            placeholder = "搜索文件…"
        )
    }
}

// ==================== BrandSettingRow ====================

/**
 * 设置行：带值行、开关行（remember 持有开关状态）、危险操作行。
 */
@Composable
@Preview(name = "BrandSettingRow", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandSettingRowPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    var darkMode by remember { mutableStateOf(false) }
    PreviewFrame(darkTheme = darkTheme) {
        BrandSettingRow(
            icon = Icons.Filled.Settings,
            label = "外观",
            value = "浅色",
            onClick = {}
        )
        BrandSwitchSettingRow(
            icon = Icons.Filled.Warning,
            label = "深色模式",
            checked = darkMode,
            onCheckedChange = { darkMode = it }
        )
        BrandSettingRow(
            icon = Icons.Filled.Warning,
            label = "清除缓存",
            onClick = {},
            isDanger = true
        )
    }
}

// ==================== BrandToast ====================

/**
 * Toast 宿主：组合进入时自动通过 LocalToastHost 触发一条示例 toast，
 * 便于静态预览直接看到胶囊浮层（预览环境会执行 LaunchedEffect）。
 */
@Composable
@Preview(name = "BrandToast", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandToastPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandToastHost {
            val showToast = LocalToastHost.current
            LaunchedEffect(Unit) { showToast("文件已保存") }
            Box(modifier = Modifier.size(width = 220.dp, height = 64.dp))
        }
    }
}

// ==================== BrandTopBar ====================

/**
 * 顶栏：无返回键的根级形态与带返回键 + 右侧动作的子级形态。
 */
@Composable
@Preview(name = "BrandTopBar", showBackground = true, backgroundColor = 0xFFFFFFFF)
private fun BrandTopBarPreview(@PreviewParameter(DarkModeProvider::class) darkTheme: Boolean) {
    PreviewFrame(darkTheme = darkTheme) {
        BrandTopBar(title = "DraftPeek")
        BrandTopBar(
            title = "Documents",
            onBack = {},
            actions = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置"
                )
            }
        )
    }
}
