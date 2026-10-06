package com.draftpeek.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.ui.component.BrandSwitchSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.composition.LocalFeatureToggle
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SubPageTopBarTitleStyle
import com.draftpeek.feature.settings.R

/**
 * 实验室开关分组（决策点 D2：语言与补全 / Markdown / 版本与运行时）。
 *
 * 分组只影响展示，不影响开关语义；[FeatureFlag.FEATURE_TOGGLE]（总开关）不参与分组，
 * 固定展示在最上方，关闭时其余开关一并隐藏。
 */
private enum class LabGroup(val labelRes: Int) {
    LANGUAGE(R.string.settings_lab_group_language),
    MARKDOWN(R.string.settings_lab_group_markdown),
    RUNTIME(R.string.settings_lab_group_runtime)
}

private fun FeatureFlag.labGroup(): LabGroup = when (this) {
    FeatureFlag.LSP_CLIENT, FeatureFlag.TREE_SITTER -> LabGroup.LANGUAGE
    FeatureFlag.MARKDOWN_WYSIWYG, FeatureFlag.MARKDOWN_EDITOR,
    FeatureFlag.COMMONMARK_PARSER -> LabGroup.MARKDOWN
    // FEATURE_TOGGLE 由调用方过滤掉，这里给它一个归处避免 when 不穷尽
    FeatureFlag.GIT_UI, FeatureFlag.TERMINAL, FeatureFlag.FEATURE_TOGGLE -> LabGroup.RUNTIME
}

@Composable
private fun FeatureFlag.displayName(): String = when (this) {
    FeatureFlag.LSP_CLIENT -> stringResource(R.string.feature_flag_lsp_client)
    FeatureFlag.TERMINAL -> stringResource(R.string.feature_flag_terminal)
    FeatureFlag.TREE_SITTER -> stringResource(R.string.feature_flag_tree_sitter)
    FeatureFlag.MARKDOWN_WYSIWYG -> stringResource(R.string.feature_flag_markdown_wysiwyg)
    FeatureFlag.MARKDOWN_EDITOR -> stringResource(R.string.feature_flag_markdown_editor)
    FeatureFlag.GIT_UI -> stringResource(R.string.feature_flag_git_ui)
    FeatureFlag.COMMONMARK_PARSER -> stringResource(R.string.feature_flag_commonmark_parser)
    FeatureFlag.FEATURE_TOGGLE -> stringResource(R.string.feature_flag_feature_toggle)
}

/**
 * 实验室页（实施指导书 §2.5 屏 22）。
 *
 * 阶段 5 第三批：原 `ProfileScreen` 的「功能开关」分区迁入并按 D2 分三组展示。
 * 开关状态来自 [LocalFeatureToggle]（core/ui 的 CompositionLocal），
 * 因此本页无需自己的 ViewModel。
 *
 * @param onNavigateUp 返回回调
 */
@Composable
fun LabScreen(onNavigateUp: () -> Unit) {
    val featureToggleManager = LocalFeatureToggle.current
    val flagStates by featureToggleManager?.flagStates?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(FeatureFlag.entries.associateWith { it.defaultEnabled }) }

    val masterEnabled = flagStates[FeatureFlag.FEATURE_TOGGLE]
        ?: FeatureFlag.FEATURE_TOGGLE.defaultEnabled

    BrandTopBar(
        onBack = onNavigateUp,
        title = stringResource(R.string.settings_lab_page_title),
        titleStyle = SubPageTopBarTitleStyle
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 总开关常驻最上方；关闭时其余开关隐藏（沿用原 ProfileScreen 的行为）
        BrandSwitchSettingRow(
            icon = Icons.Filled.Flag,
            label = FeatureFlag.FEATURE_TOGGLE.displayName(),
            checked = masterEnabled,
            onCheckedChange = { featureToggleManager?.setEnabled(FeatureFlag.FEATURE_TOGGLE, it) },
            showDivider = false
        )

        if (!masterEnabled) {
            return@Column
        }

        LabGroup.entries.forEach { group ->
            val flags = FeatureFlag.entries.filter {
                it != FeatureFlag.FEATURE_TOGGLE && it.labGroup() == group
            }
            if (flags.isEmpty()) {
                return@forEach
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(group.labelRes),
                style = MonoLabelStyle.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
                color = PrototypeTokens.muted,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            flags.forEachIndexed { index, flag ->
                BrandSwitchSettingRow(
                    icon = Icons.Filled.Flag,
                    label = flag.displayName(),
                    checked = flagStates[flag] ?: flag.defaultEnabled,
                    onCheckedChange = { featureToggleManager?.setEnabled(flag, it) },
                    showDivider = index < flags.lastIndex
                )
            }
        }
    }
}
