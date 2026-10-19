/**
 * 文件: ProfileScreen.kt
 * 功能: 统计模块UI - 个人资料/统计/设置主页面
 * 描述: DraftPeek 应用的"我的"页面，整合了以下功能模块：
 *       1. 用户资料展示（头像、用户名、版本号）
 *       2. 统计数据展示（周期选择器、统计卡片网格、年度热力图）
 *       3. 编辑器设置（字体大小、主题、字体、自动换行、行号等）
 *       4. Markdown 预览设置（主题、自定义 CSS）
 *       5. 无障碍设置入口
 *       6. 功能开关（Feature Flags）
 *       7. 网络设置（GitHub 镜像）
 *       8. 反馈功能（带截图附件的邮件反馈）
 *       9. 社交平台链接
 *       10. 应用验证与版本信息
 *       11. 危险操作（清除缓存、重置设置）
 *
 * 页面结构：使用垂直滚动布局，按分区组织设置项，每个分区有 SectionHeader 标识。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.stats.R
import java.util.Locale

@Composable
internal fun SectionHeader(label: String, isDanger: Boolean = false) {
    Text(
        text = label,
        style = MonoLabelStyle.copy(
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        ),
        color = if (isDanger) SemanticColors.Danger else PrototypeTokens.muted,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
internal fun SocialPlatformItem(
    label: String,
    iconRes: Int?,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrototypeTokens.surface),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconRes != null -> Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
                iconVector != null -> Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
                else -> Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = PrototypeTokens.muted
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = PrototypeTokens.fgSoft,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (isDanger) SemanticColors.Danger else PrototypeTokens.fg
            )
        },
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PrototypeTokens.fgSoft
            )
        },
        confirmButton = {
            BrandFilledButton(
                text = confirmText,
                onClick = onConfirm
            )
        },
        dismissButton = {
            BrandOutlinedButton(
                text = stringResource(R.string.profile_dialog_cancel),
                onClick = onDismiss
            )
        }
    )
}

@SuppressLint("AppBundleLocaleChanges") // 本地切换 5 种语言，不接入 Play Core 语言包分发
private fun applyLanguage(language: AppLanguage) {
    // 与 DraftPeekApp.applySavedLanguage() 保持一致（全仓语言切换统一走同一入口）：
    // 使用 AppCompatDelegate.setApplicationLocales 替代已废弃的
    // Resources.updateConfiguration，该 API 兼容 API 26+ 且能正确触发 Activity 重建。
    val localeList = when (language) {
        AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.ZH -> LocaleListCompat.create(Locale.CHINA)
        AppLanguage.ZH_TW -> LocaleListCompat.create(Locale.TAIWAN)
        AppLanguage.EN -> LocaleListCompat.create(Locale.US)
        AppLanguage.JA -> LocaleListCompat.create(Locale.JAPAN)
        AppLanguage.KO -> LocaleListCompat.create(Locale.KOREA)
    }
    AppCompatDelegate.setApplicationLocales(localeList)
}

/**
 * 应用验证状态
 */

@Composable
internal fun MarkdownThemePickerDialog(
    currentThemeName: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTheme = try {
        MarkdownTheme.valueOf(currentThemeName)
    } catch (_: Exception) {
        MarkdownTheme.DEFAULT
    }

    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_setting_markdown_theme)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MarkdownTheme.entries.forEach { theme ->
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
                            .clickable { onThemeSelected(theme.name) }
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
                                text = stringResource(theme.displayNameResId),
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
