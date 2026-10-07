package com.draftpeek.feature.settings.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandSettingRow
import com.draftpeek.core.ui.component.BrandSwitchSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.theme.FontOptions
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SubPageTopBarTitleStyle
import com.draftpeek.feature.settings.R
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel

/**
 * 编辑器设置页（实施指导书 §2.5 屏 21）。
 *
 * 阶段 5 第二批：行与对话框自 `feature/stats/ProfileScreen.kt` 逐行搬移，未改写逻辑。
 * 仍留在 Me 页的编辑器相关项只有「Markdown 预览主题」与「主题管理」：
 * 前者依赖 `feature/editor` 的 `MarkdownTheme`，而 `feature/editor` 已依赖 `feature:settings`，
 * 反向依赖会成环；后者依赖 stats 的 `ThemeManageViewModel`。
 *
 * @param onNavigateUp 返回回调
 * @param onLanguageApplied 语言已落库后的「立即应用」回调（应用 locale 需 AppCompatDelegate，
 *   属应用级职责，由 app 层注入；feature 模块不引 appcompat）
 * @param settingsViewModel 设置 ViewModel（编辑器设置状态与写入口）
 */
@Composable
fun EditorSettingsScreen(
    onNavigateUp: () -> Unit,
    onLanguageApplied: (AppLanguage) -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val muted = PrototypeTokens.muted
    val accent = PrototypeTokens.accent
    val border = PrototypeTokens.border
    val surface = PrototypeTokens.surface

    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showTabWidthDialog by remember { mutableStateOf(false) }
    var showRecentLimitDialog by remember { mutableStateOf(false) }
    var showFontSelectionDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEncodingDialog by remember { mutableStateOf(false) }
    var showCustomCssDialog by remember { mutableStateOf(false) }
    var customCssText by remember { mutableStateOf(settings.customMarkdownCss) }
    LaunchedEffect(settings.customMarkdownCss) { customCssText = settings.customMarkdownCss }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        BrandTopBar(
            onBack = onNavigateUp,
            title = stringResource(R.string.settings_editor_page_title),
            titleStyle = SubPageTopBarTitleStyle
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            BrandSettingRow(
                icon = Icons.Filled.FormatSize,
                label = stringResource(R.string.profile_setting_font_size),
                value = "${settings.fontSize}sp",
                onClick = { showFontSizeDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.History,
                label = stringResource(R.string.profile_setting_recent_files_limit),
                value = stringResource(R.string.profile_recent_files_limit_count, settings.recentFilesLimit),
                onClick = { showRecentLimitDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.DarkMode,
                label = stringResource(R.string.profile_setting_theme),
                value = when (settings.theme) {
                    AppTheme.LIGHT -> stringResource(R.string.profile_setting_theme_light)
                    AppTheme.DARK -> stringResource(R.string.profile_setting_theme_dark)
                    AppTheme.SYSTEM -> stringResource(R.string.profile_setting_theme_system)
                },
                onClick = { showThemeDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.FontDownload,
                label = stringResource(R.string.profile_setting_font_family),
                value =
                stringResource(FontOptions.getCodeFontById(settings.codeFontFamilyId).displayNameResId) + " / " +
                    stringResource(FontOptions.getUiFontById(settings.uiFontFamilyId).displayNameResId),
                onClick = { showFontSelectionDialog = true },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.AutoMirrored.Filled.WrapText,
                label = stringResource(R.string.profile_setting_line_wrapping),
                checked = settings.lineWrapping,
                onCheckedChange = { settingsViewModel.updateLineWrapping(it) },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.Filled.FormatListNumbered,
                label = stringResource(R.string.profile_setting_show_line_numbers),
                checked = settings.showLineNumbers,
                onCheckedChange = { settingsViewModel.updateShowLineNumbers(it) },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.Filled.Save,
                label = stringResource(R.string.profile_setting_auto_save),
                checked = settings.autoSave,
                onCheckedChange = { settingsViewModel.updateAutoSave(it) },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.profile_setting_highlight_current_line),
                checked = settings.highlightCurrentLine,
                onCheckedChange = { settingsViewModel.updateHighlightCurrentLine(it) },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                label = stringResource(R.string.profile_setting_auto_indent),
                checked = settings.autoIndent,
                onCheckedChange = { settingsViewModel.updateAutoIndent(it) },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.SpaceBar,
                label = stringResource(R.string.profile_setting_tab_width),
                value = String.format(stringResource(R.string.profile_tab_width_spaces), settings.tabWidth),
                onClick = { showTabWidthDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.TextFields,
                label = stringResource(R.string.profile_setting_encoding),
                value = settings.defaultEncoding,
                onClick = { showEncodingDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Language,
                label = stringResource(R.string.profile_setting_language),
                value = when (settings.language) {
                    AppLanguage.SYSTEM -> stringResource(R.string.profile_setting_language_system)
                    AppLanguage.ZH -> stringResource(R.string.profile_setting_language_zh)
                    AppLanguage.ZH_TW -> stringResource(R.string.profile_setting_language_zh_tw)
                    AppLanguage.EN -> stringResource(R.string.profile_setting_language_en)
                    AppLanguage.JA -> stringResource(R.string.profile_setting_language_ja)
                    AppLanguage.KO -> stringResource(R.string.profile_setting_language_ko)
                },
                onClick = { showLanguageDialog = true },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.Filled.Reorder,
                label = stringResource(R.string.profile_setting_indent_guides),
                checked = settings.showIndentGuides,
                onCheckedChange = { settingsViewModel.updateShowIndentGuides(it) },
                showDivider = true
            )
            BrandSwitchSettingRow(
                icon = Icons.Filled.PushPin,
                label = stringResource(R.string.profile_setting_sticky_scroll),
                checked = settings.stickyScroll,
                onCheckedChange = { settingsViewModel.updateStickyScroll(it) },
                showDivider = true
            )

            BrandSwitchSettingRow(
                icon = Icons.Filled.Map,
                label = stringResource(R.string.profile_setting_show_minimap),
                checked = settings.showMinimap,
                onCheckedChange = { settingsViewModel.updateShowMinimap(it) },
                showDivider = true
            )

            BrandSettingRow(
                icon = Icons.Filled.Code,
                label = stringResource(R.string.profile_setting_custom_css),
                value = settings.customMarkdownCss.ifEmpty { stringResource(R.string.profile_setting_custom_css_hint) },
                onClick = {
                    customCssText = settings.customMarkdownCss
                    showCustomCssDialog = true
                },
                showDivider = false
            )
        }
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = settings.fontSize,
            onSizeChange = { settingsViewModel.updateFontSize(it) },
            onDismiss = { showFontSizeDialog = false }
        )
    }

    if (showTabWidthDialog) {
        TabWidthDialog(
            currentTabWidth = settings.tabWidth,
            onTabWidthSelected = { settingsViewModel.updateTabWidth(it) },
            onDismiss = { showTabWidthDialog = false }
        )
    }

    if (showRecentLimitDialog) {
        RecentFilesLimitDialog(
            currentLimit = settings.recentFilesLimit,
            onLimitSelected = { settingsViewModel.updateRecentFilesLimit(it) },
            onDismiss = { showRecentLimitDialog = false }
        )
    }

    if (showFontSelectionDialog) {
        FontSelectionDialog(
            currentCodeFontId = settings.codeFontFamilyId,
            currentUiFontId = settings.uiFontFamilyId,
            onCodeFontSelected = { settingsViewModel.updateCodeFontFamilyId(it) },
            onUiFontSelected = { settingsViewModel.updateUiFontFamilyId(it) },
            onDismiss = { showFontSelectionDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = settings.theme,
            onThemeSelected = { theme ->
                settingsViewModel.updateTheme(theme)
                showThemeDialog = false
                (context as? Activity)?.recreate()
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = settings.language,
            onLanguageSelected = { language ->
                settingsViewModel.updateLanguage(language)
                onLanguageApplied(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showEncodingDialog) {
        EncodingDialog(
            currentEncoding = settings.defaultEncoding,
            onEncodingSelected = { encoding ->
                settingsViewModel.updateDefaultEncoding(encoding)
            },
            onDismiss = { showEncodingDialog = false }
        )
    }
    if (showCustomCssDialog) {
        BrandDialog(
            onDismissRequest = { showCustomCssDialog = false },
            title = { Text(stringResource(R.string.profile_custom_css_dialog_title)) },
            content = {
                Column {
                    BrandOutlinedTextField(
                        value = customCssText,
                        onValueChange = { customCssText = it },
                        label = { Text(stringResource(R.string.profile_custom_css_dialog_hint)) },
                        minLines = 4,
                        maxLines = 12,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.profile_custom_css_dialog_description),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.profile_dialog_confirm),
                    onClick = {
                        settingsViewModel.updateCustomMarkdownCss(customCssText)
                        showCustomCssDialog = false
                    }
                )
            },
            dismissButton = {
                BrandOutlinedButton(text = stringResource(R.string.profile_dialog_cancel), onClick = {
                    showCustomCssDialog =
                        false
                })
            }
        )
    }
}

@Composable
private fun FontSizeDialog(currentSize: Int, onSizeChange: (Int) -> Unit, onDismiss: () -> Unit) {
    var sliderValue by remember { mutableStateOf(currentSize.toFloat()) }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_font_size_dialog_title)) },
        content = {
            Column {
                // 当前值大号显示
                Text(
                    text = "${sliderValue.toInt()}sp",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = PrototypeTokens.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 预览区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrototypeTokens.surface)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "中华人民共和国\n字体大小预览 AaBbCc 123\nThe quick brown fox",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = sliderValue.toInt().sp,
                        color = PrototypeTokens.fg,
                        lineHeight = (sliderValue.toInt() * 1.5f).sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Slider 行：最小值标签 + Slider + 最大值标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "8",
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            onSizeChange(it.toInt())
                        },
                        valueRange = 8f..32f,
                        steps = 23,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrototypeTokens.accent,
                            inactiveTrackColor = PrototypeTokens.accentSoft,
                            thumbColor = PrototypeTokens.accent
                        )
                    )
                    Text(
                        text = "32",
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        },
        dismissButton = {
            BrandOutlinedButton(text = stringResource(R.string.profile_dialog_cancel), onClick = onDismiss)
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabWidthDialog(currentTabWidth: Int, onTabWidthSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(2, 4, 8)

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_tab_width_dialog_title)) },
        content = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { width ->
                    FilterChip(
                        selected = width == currentTabWidth,
                        onClick = { onTabWidthSelected(width) },
                        label = { Text(String.format(stringResource(R.string.profile_tab_width_spaces), width)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrototypeTokens.accentSoft,
                            selectedLabelColor = PrototypeTokens.fg
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = PrototypeTokens.border,
                            selectedBorderColor = PrototypeTokens.accent,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp,
                            enabled = true,
                            selected = width == currentTabWidth
                        )
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}

@Composable
private fun RecentFilesLimitDialog(currentLimit: Int, onLimitSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    var sliderValue by remember { mutableStateOf(currentLimit.toFloat()) }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_recent_files_limit_dialog_title)) },
        content = {
            Column {
                Text(
                    text = stringResource(R.string.profile_recent_files_limit_count, sliderValue.toInt()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = PrototypeTokens.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0",
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            onLimitSelected(it.toInt())
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrototypeTokens.accent,
                            inactiveTrackColor = PrototypeTokens.accentSoft,
                            thumbColor = PrototypeTokens.accent
                        )
                    )
                    Text(
                        text = "10",
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EncodingDialog(currentEncoding: String, onEncodingSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf("UTF-8", "UTF-16", "GBK", "GB2312", "ISO-8859-1", "US-ASCII")

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setting_encoding)) },
        content = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { encoding ->
                    FilterChip(
                        selected = encoding == currentEncoding,
                        onClick = { onEncodingSelected(encoding) },
                        label = { Text(encoding) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrototypeTokens.accentSoft,
                            selectedLabelColor = PrototypeTokens.fg
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = PrototypeTokens.border,
                            selectedBorderColor = PrototypeTokens.accent,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp,
                            enabled = true,
                            selected = encoding == currentEncoding
                        )
                    )
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}

@Composable
private fun FontSelectionDialog(
    currentCodeFontId: String,
    currentUiFontId: String,
    onCodeFontSelected: (String) -> Unit,
    onUiFontSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.font_tab_code),
        stringResource(R.string.font_tab_ui)
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_dialog_font_family_title)) },
        content = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrototypeTokens.accentSoft else PrototypeTokens.surface)
                                .border(
                                    1.dp,
                                    if (selected) PrototypeTokens.accent else PrototypeTokens.border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) PrototypeTokens.accent else PrototypeTokens.fg,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                val options = if (selectedTab == 0) FontOptions.codeFonts else FontOptions.uiFonts
                val currentId = if (selectedTab == 0) currentCodeFontId else currentUiFontId

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { font ->
                        val isSelected = font.id == currentId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrototypeTokens.accentSoft else PrototypeTokens.surface)
                                .border(
                                    1.5.dp,
                                    if (isSelected) PrototypeTokens.accent else PrototypeTokens.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (selectedTab == 0) {
                                        onCodeFontSelected(font.id)
                                    } else {
                                        onUiFontSelected(font.id)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = PrototypeTokens.accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(font.displayNameResId),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = font.fontFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = PrototypeTokens.fg
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}

@Composable
private fun ThemePickerDialog(currentTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        AppTheme.LIGHT to stringResource(R.string.profile_setting_theme_light),
        AppTheme.DARK to stringResource(R.string.profile_setting_theme_dark),
        AppTheme.SYSTEM to stringResource(R.string.profile_setting_theme_system)
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setting_theme)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (theme, label) ->
                    val selected = theme == currentTheme
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PrototypeTokens.accentSoft else PrototypeTokens.surface)
                            .border(
                                1.5.dp,
                                if (selected) PrototypeTokens.accent else PrototypeTokens.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onThemeSelected(theme) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PrototypeTokens.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) PrototypeTokens.accent else PrototypeTokens.fg
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}

@Composable
private fun LanguagePickerDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        AppLanguage.SYSTEM to stringResource(R.string.profile_setting_language_system),
        AppLanguage.ZH to stringResource(R.string.profile_setting_language_zh),
        AppLanguage.ZH_TW to stringResource(R.string.profile_setting_language_zh_tw),
        AppLanguage.EN to stringResource(R.string.profile_setting_language_en),
        AppLanguage.JA to stringResource(R.string.profile_setting_language_ja),
        AppLanguage.KO to stringResource(R.string.profile_setting_language_ko)
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setting_language)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (language, label) ->
                    val selected = language == currentLanguage
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) PrototypeTokens.accentSoft else PrototypeTokens.surface)
                            .border(
                                1.5.dp,
                                if (selected) PrototypeTokens.accent else PrototypeTokens.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onLanguageSelected(language) }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = PrototypeTokens.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) PrototypeTokens.accent else PrototypeTokens.fg
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(text = stringResource(R.string.profile_dialog_confirm), onClick = onDismiss)
        }
    )
}
