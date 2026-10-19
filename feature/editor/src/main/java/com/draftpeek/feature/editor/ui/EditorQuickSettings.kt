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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.component.BrandSwitch
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.settings.model.EditorSettings

private const val TAG = "EditorScreen"

/**
 * 编辑器快速设置面板。
 *
 * 提供常用编辑器配置的快捷访问，包括字体大小、自动换行、行号显示、Tab宽度等。
 *
 * @param settings 当前编辑器设置
 * @param onFontSizeChange 字体大小变更回调
 * @param onWordWrapChange 自动换行开关变更回调
 * @param onLineNumbersChange 行号显示开关变更回调
 * @param onTabWidthChange Tab宽度变更回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickSettingsPanel(
    settings: EditorSettings,
    onFontSizeChange: (Int) -> Unit,
    onWordWrapChange: (Boolean) -> Unit,
    onLineNumbersChange: (Boolean) -> Unit,
    onTabWidthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(PrototypeSpacing.BottomSheetPadding)
    ) {
        Text(
            text = stringResource(R.string.editor_quick_settings_title),
            style = H2Style.copy(color = fg),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Font size slider
        Text(
            stringResource(R.string.editor_font_size, settings.fontSize),
            style = DraftPeekTypography.bodyMedium.copy(color = fg)
        )
        androidx.compose.material3.Slider(
            value = settings.fontSize.toFloat(),
            onValueChange = { onFontSizeChange(it.toInt()) },
            valueRange = 10f..24f,
            steps = 13,
            colors = SliderDefaults.colors(
                activeTrackColor = PrototypeTokens.accent,
                thumbColor = PrototypeTokens.accent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Word wrap switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.editor_word_wrap), style = DraftPeekTypography.bodyMedium.copy(color = fg))
            BrandSwitch(
                checked = settings.lineWrapping,
                onCheckedChange = onWordWrapChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Line numbers switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.editor_show_line_numbers),
                style = DraftPeekTypography.bodyMedium.copy(color = fg)
            )
            BrandSwitch(
                checked = settings.showLineNumbers,
                onCheckedChange = onLineNumbersChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab width chips
        Text(stringResource(R.string.editor_tab_width), style = DraftPeekTypography.bodyMedium.copy(color = fg))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 4, 8).forEach { width ->
                androidx.compose.material3.FilterChip(
                    selected = settings.tabWidth == width,
                    onClick = { onTabWidthChange(width) },
                    label = { Text("$width", style = DraftPeekTypography.labelMedium) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
