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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.draftpeek.core.designsystem.theme.DraftPeekTheme

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
private fun PreviewFrame(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
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
private fun BrandOutlinedTextFieldPreview(
    @PreviewParameter(DarkModeProvider::class) darkTheme: Boolean
) {
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
