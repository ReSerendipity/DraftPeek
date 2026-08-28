package com.draftpeek.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.util.HapticFeedbackHelper
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandSettingRow
import com.draftpeek.core.ui.component.BrandSwitchSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.theme.ColorBlindMode
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SettingDescStyle
import com.draftpeek.core.ui.theme.SubPageTopBarTitleStyle
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel
import com.draftpeek.feature.stats.R

/**
 * 无障碍设置页面。
 *
 * 集中管理所有无障碍功能，包括：
 * - 色盲友好辅助（色盲模式、非色彩标识）
 * - 高对比度模式与文字缩放
 * - 屏幕阅读器优化
 * - 振动反馈
 *
 * 所有功能遵循 WCAG 2.1 AA 级标准。
 */
@Composable
fun AccessibilityScreen(onNavigateUp: () -> Unit, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticHelper = remember { HapticFeedbackHelper(context) }

    var showColorBlindDialog by remember { mutableStateOf(false) }
    var showTextScaleDialog by remember { mutableStateOf(false) }

    if (showColorBlindDialog) {
        ColorBlindModeDialog(
            currentMode = settings.colorBlindMode,
            onModeSelected = { mode ->
                settingsViewModel.updateColorBlindMode(mode)
                showColorBlindDialog = false
            },
            onDismiss = { showColorBlindDialog = false }
        )
    }

    if (showTextScaleDialog) {
        TextScaleDialog(
            currentScale = settings.textScale,
            onScaleChange = { settingsViewModel.updateTextScale(it) },
            onDismiss = { showTextScaleDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        BrandTopBar(
            onBack = onNavigateUp,
            title = stringResource(R.string.accessibility_page_title),
            titleStyle = SubPageTopBarTitleStyle
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // WCAG 信息横幅
            WcagInfoBanner()

            // ===== 视觉辅助 =====
            AccessibilitySectionHeader(
                text = stringResource(R.string.accessibility_section_visual)
            )

            BrandSettingRow(
                icon = Icons.Filled.Palette,
                label = stringResource(R.string.accessibility_color_blind_mode),
                value = colorBlindModeLabel(settings.colorBlindMode),
                onClick = { showColorBlindDialog = true },
                showDivider = true
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_color_blind_mode_desc)
            )

            BrandSwitchSettingRow(
                icon = Icons.Filled.HighQuality,
                label = stringResource(R.string.accessibility_high_contrast),
                checked = settings.highContrastMode,
                onCheckedChange = { settingsViewModel.updateHighContrastMode(it) },
                showDivider = true
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_high_contrast_desc)
            )

            BrandSwitchSettingRow(
                icon = Icons.Filled.GraphicEq,
                label = stringResource(R.string.accessibility_non_color_indicators),
                checked = settings.nonColorIndicators,
                onCheckedChange = { settingsViewModel.updateNonColorIndicators(it) },
                showDivider = true
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_non_color_indicators_desc)
            )

            BrandSettingRow(
                icon = Icons.Filled.FormatSize,
                label = stringResource(R.string.accessibility_text_scale),
                value = "${Math.round(settings.textScale * 100)}%",
                onClick = { showTextScaleDialog = true },
                showDivider = false
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_text_scale_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 屏幕阅读器 =====
            AccessibilitySectionHeader(
                text = stringResource(R.string.accessibility_section_screen_reader)
            )

            BrandSwitchSettingRow(
                icon = Icons.Filled.Accessibility,
                label = stringResource(R.string.accessibility_screen_reader_optimized),
                checked = settings.screenReaderOptimized,
                onCheckedChange = { settingsViewModel.updateScreenReaderOptimized(it) },
                showDivider = false
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_screen_reader_optimized_desc)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== 听觉辅助 =====
            AccessibilitySectionHeader(
                text = stringResource(R.string.accessibility_section_hearing)
            )

            BrandSwitchSettingRow(
                icon = Icons.Filled.Vibration,
                label = stringResource(R.string.accessibility_vibration_feedback),
                checked = settings.vibrationFeedback,
                onCheckedChange = { settingsViewModel.updateVibrationFeedback(it) },
                showDivider = true
            )
            SettingDescription(
                text = stringResource(R.string.accessibility_vibration_feedback_desc)
            )

            // 振动测试按钮
            if (settings.vibrationFeedback) {
                VibrationTestRow(
                    enabled = settings.vibrationFeedback && hapticHelper.hasVibrator,
                    onTest = {
                        hapticHelper.success()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================
// Sub-components
// ============================================================

@Composable
private fun AccessibilitySectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        ),
        color = PrototypeTokens.muted,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SettingDescription(text: String) {
    Text(
        text = text,
        style = SettingDescStyle,
        color = PrototypeTokens.muted,
        modifier = Modifier.padding(start = 64.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun WcagInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(PrototypeShapes.Card)
            .background(PrototypeTokens.infoContainer)
            .border(1.dp, PrototypeTokens.borderSoft, PrototypeShapes.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Accessibility,
            contentDescription = null,
            tint = PrototypeTokens.onInfoContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.accessibility_wcag_info),
            style = MaterialTheme.typography.bodySmall,
            color = PrototypeTokens.onInfoContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun VibrationTestRow(enabled: Boolean, onTest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) PrototypeTokens.accentSoft else PrototypeTokens.elevated)
                .clickable(enabled = enabled, onClick = onTest)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Vibration,
                contentDescription = null,
                tint = if (enabled) PrototypeTokens.accent else PrototypeTokens.muted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.accessibility_test_vibration),
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) PrototypeTokens.accent else PrototypeTokens.muted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================
// Dialogs
// ============================================================

@Composable
private fun ColorBlindModeDialog(
    currentMode: ColorBlindMode,
    onModeSelected: (ColorBlindMode) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        ColorBlindMode.NONE to stringResource(R.string.accessibility_color_blind_mode_none),
        ColorBlindMode.PROTANOPIA to stringResource(R.string.accessibility_color_blind_mode_protanopia),
        ColorBlindMode.DEUTERANOPIA to stringResource(R.string.accessibility_color_blind_mode_deuteranopia),
        ColorBlindMode.TRITANOPIA to stringResource(R.string.accessibility_color_blind_mode_tritanopia)
    )

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accessibility_color_blind_mode)) },
        content = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview circle to show how colors look in each mode
                        ColorBlindPreviewDot(mode = mode)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (mode == currentMode) PrototypeTokens.accent else PrototypeTokens.fg,
                            fontWeight = if (mode == currentMode) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (mode == currentMode) {
                            Text(
                                text = "✓",
                                color = PrototypeTokens.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.profile_dialog_confirm),
                    color = PrototypeTokens.accent
                )
            }
        }
    )
}

@Composable
private fun ColorBlindPreviewDot(mode: ColorBlindMode) {
    val previewColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFEF5350), // Red
        androidx.compose.ui.graphics.Color(0xFF42A5F5), // Blue
        androidx.compose.ui.graphics.Color(0xFF66BB6A), // Green
        androidx.compose.ui.graphics.Color(0xFFFFB300) // Amber
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        previewColors.forEach { color ->
            val transformed = if (mode == ColorBlindMode.NONE) {
                color
            } else {
                com.draftpeek.core.ui.theme.ColorBlindnessHelper.transform(color, mode)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(transformed)
            )
        }
    }
}

// ============================================================
// Helpers
// ============================================================

@Composable
private fun colorBlindModeLabel(mode: ColorBlindMode): String = when (mode) {
    ColorBlindMode.NONE -> stringResource(R.string.accessibility_color_blind_mode_none)
    ColorBlindMode.PROTANOPIA -> stringResource(R.string.accessibility_color_blind_mode_protanopia)
    ColorBlindMode.DEUTERANOPIA -> stringResource(R.string.accessibility_color_blind_mode_deuteranopia)
    ColorBlindMode.TRITANOPIA -> stringResource(R.string.accessibility_color_blind_mode_tritanopia)
}

@Composable
private fun TextScaleDialog(currentScale: Float, onScaleChange: (Float) -> Unit, onDismiss: () -> Unit) {
    var sliderValue by remember { mutableStateOf(currentScale) }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accessibility_text_scale)) },
        content = {
            Column {
                Text(
                    text = "${Math.round(sliderValue * 100)}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = PrototypeTokens.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrototypeTokens.surface)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "中华人民共和国\n文字缩放预览 AaBbCc 123\nThe quick brown fox",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = (16 * sliderValue).sp,
                        color = PrototypeTokens.fg,
                        lineHeight = (16 * sliderValue * 1.5f).sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "80%",
                        fontSize = 12.sp,
                        color = PrototypeTokens.muted
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            val snapped = Math.round(it * 20) / 20f
                            sliderValue = snapped
                            onScaleChange(snapped)
                        },
                        valueRange = 0.8f..2.0f,
                        steps = 23,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = PrototypeTokens.accent,
                            inactiveTrackColor = PrototypeTokens.accentSoft,
                            thumbColor = PrototypeTokens.accent
                        )
                    )
                    Text(
                        text = "200%",
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
