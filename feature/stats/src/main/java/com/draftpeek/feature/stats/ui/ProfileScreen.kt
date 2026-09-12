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
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.security.AiProtectionStateHolder
import com.draftpeek.core.common.security.AiThreatLevel
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandPill
import com.draftpeek.core.ui.component.BrandSettingRow
import com.draftpeek.core.ui.component.BrandSwitchSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.composition.LocalFeatureToggle
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.theme.FontOptions
import com.draftpeek.core.ui.theme.JetBrainsMonoFontFamily
import com.draftpeek.core.ui.theme.LocalDarkTheme
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel
import com.draftpeek.feature.stats.R
import com.draftpeek.feature.stats.ui.component.DayDetailDialog
import com.draftpeek.feature.stats.ui.component.GreetingSection
import com.draftpeek.feature.stats.ui.component.PeriodChipRow
import com.draftpeek.feature.stats.ui.component.StatCardGrid
import com.draftpeek.feature.stats.ui.component.YearHeatmapNew
import com.draftpeek.feature.stats.util.AchievementDefinitions
import com.draftpeek.feature.stats.viewmodel.StatsMessage
import com.draftpeek.feature.stats.viewmodel.StatsViewModel
import com.draftpeek.feature.stats.viewmodel.ThemeManageViewModel
import java.util.Locale
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    onOpenEditor: () -> Unit = {},
    onFileClick: (String) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSnippets: () -> Unit = {},
    onNavigateToAccessibility: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onVerifyApp: (suspend (Context) -> VerifyAppState)? = null,
    viewModel: StatsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val featureToggleManager = LocalFeatureToggle.current
    val flagStates by featureToggleManager?.flagStates?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(FeatureFlag.entries.associateWith { it.defaultEnabled }) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val periodStats by viewModel.periodStats.collectAsStateWithLifecycle()
    val firstUseDate by viewModel.firstUseDate.collectAsStateWithLifecycle()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val yearActivities by viewModel.yearActivities.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val themeManageViewModel: ThemeManageViewModel = hiltViewModel()
    val importedThemes by themeManageViewModel.customThemes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current

    val pageBg = PrototypeTokens.pageBackground
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border

    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showTabWidthDialog by remember { mutableStateOf(false) }
    var showRecentLimitDialog by remember { mutableStateOf(false) }
    var showFontSelectionDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEncodingDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }
    var showMirrorUrlDialog by remember { mutableStateOf(false) }
    var mirrorUrlText by remember { mutableStateOf(settings.githubMirrorUrl) }
    LaunchedEffect(settings.githubMirrorUrl) { mirrorUrlText = settings.githubMirrorUrl }
    var showMarkdownThemeDialog by remember { mutableStateOf(false) }
    var showCustomCssDialog by remember { mutableStateOf(false) }
    var customCssText by remember { mutableStateOf(settings.customMarkdownCss) }
    LaunchedEffect(settings.customMarkdownCss) { customCssText = settings.customMarkdownCss }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var feedbackScreenshots by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            feedbackScreenshots = (feedbackScreenshots + uris).distinct().take(5)
        }
    }
    var showVerifyAppDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }
    var showThemeManageDialog by remember { mutableStateOf(false) }
    var verifyAppState by remember { mutableStateOf<VerifyAppState>(VerifyAppState.Idle) }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { message ->
            when (message) {
                is StatsMessage.CacheCleared -> {
                    showClearCacheDialog = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.profile_cache_cleared),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is StatsMessage.CacheClearFailed -> {
                    showClearCacheDialog = false
                    Toast.makeText(
                        context,
                        message.error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "v1.0.0"
        } catch (_: Exception) {
            "v1.0.0"
        }
    }

    val avatarBitmap = remember(settings.userAvatarUri) {
        if (settings.userAvatarUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(settings.userAvatarUri)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            settingsViewModel.updateUserAvatar(it.toString())
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
                applyLanguage(language)
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

    if (showResetDialog) {
        ConfirmActionDialog(
            title = stringResource(R.string.profile_dialog_reset_title),
            message = stringResource(R.string.profile_dialog_reset_message),
            confirmText = stringResource(R.string.profile_dialog_reset_confirm),
            isDanger = true,
            onConfirm = {
                settingsViewModel.updateFontSize(16)
                settingsViewModel.updateTabWidth(4)
                settingsViewModel.updateLineWrapping(false)
                settingsViewModel.updateShowLineNumbers(true)
                settingsViewModel.updateTheme(AppTheme.SYSTEM)
                settingsViewModel.updateFontFamily("monospace")
                settingsViewModel.updateCodeFontFamilyId("jetbrains_mono")
                settingsViewModel.updateUiFontFamilyId("inter")
                settingsViewModel.updateLanguage(AppLanguage.SYSTEM)
                settingsViewModel.updateAutoSave(false)
                settingsViewModel.updateAutoIndent(true)
                settingsViewModel.updateHighlightCurrentLine(true)
                settingsViewModel.updateShowIndentGuides(false)
                settingsViewModel.updateDefaultEncoding("UTF-8")
                settingsViewModel.updateColorBlindMode(com.draftpeek.core.ui.theme.ColorBlindMode.NONE)
                settingsViewModel.updateHighContrastMode(false)
                settingsViewModel.updateTextScale(1.0f)
                settingsViewModel.updateScreenReaderOptimized(false)
                settingsViewModel.updateVibrationFeedback(false)
                settingsViewModel.updateNonColorIndicators(false)
                settingsViewModel.updateGithubMirrorUrl("")
                settingsViewModel.updateMarkdownThemeName("DEFAULT")
                settingsViewModel.updateCustomMarkdownCss("")
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showClearCacheDialog) {
        ConfirmActionDialog(
            title = stringResource(R.string.profile_dialog_clear_cache_title),
            message = stringResource(R.string.profile_dialog_clear_cache_message),
            confirmText = stringResource(R.string.profile_dialog_clear_cache_confirm),
            isDanger = true,
            onConfirm = { viewModel.clearCache() },
            onDismiss = { showClearCacheDialog = false }
        )
    }

    if (showEditNameDialog) {
        BrandDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.profile_edit_user_name_title)) },
            content = {
                Column {
                    BrandOutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text(stringResource(R.string.profile_edit_user_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.profile_dialog_confirm),
                    onClick = {
                        val newName = editNameText.trim()
                        if (newName.isNotEmpty() && newName.length <= 20) {
                            settingsViewModel.updateUserName(newName)
                        }
                        showEditNameDialog = false
                    }
                )
            },
            dismissButton = {
                BrandOutlinedButton(text = stringResource(R.string.profile_dialog_cancel), onClick = {
                    showEditNameDialog =
                        false
                })
            }
        )
    }

    if (showMirrorUrlDialog) {
        BrandDialog(
            onDismissRequest = { showMirrorUrlDialog = false },
            title = { Text(stringResource(R.string.profile_github_mirror_dialog_title)) },
            content = {
                Column {
                    BrandOutlinedTextField(
                        value = mirrorUrlText,
                        onValueChange = { mirrorUrlText = it },
                        label = { Text(stringResource(R.string.profile_github_mirror_dialog_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.profile_dialog_confirm),
                    onClick = {
                        settingsViewModel.updateGithubMirrorUrl(mirrorUrlText.trim())
                        showMirrorUrlDialog = false
                    }
                )
            },
            dismissButton = {
                BrandOutlinedButton(text = stringResource(R.string.profile_dialog_cancel), onClick = {
                    showMirrorUrlDialog =
                        false
                })
            }
        )
    }

    if (showMarkdownThemeDialog) {
        MarkdownThemePickerDialog(
            currentThemeName = settings.markdownThemeName,
            onThemeSelected = { themeName ->
                settingsViewModel.updateMarkdownThemeName(themeName)
            },
            onDismiss = { showMarkdownThemeDialog = false }
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

    fun sendFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("f*********@*********"))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.profile_feedback_email_subject))
            val deviceInfo = buildString {
                appendLine("应用版本: $versionName")
                appendLine("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("设备型号: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine()
                appendLine("--- 问题描述 ---")
                appendLine()
            }
            val body = deviceInfo + feedbackText
            putExtra(Intent.EXTRA_TEXT, body)
            if (feedbackScreenshots.isNotEmpty()) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(feedbackScreenshots))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.profile_feedback_submit)))
            showFeedbackDialog = false
            feedbackText = ""
            feedbackScreenshots = emptyList()
            Toast.makeText(context, context.getString(R.string.profile_feedback_thanks), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.profile_feedback_failed), Toast.LENGTH_SHORT).show()
        }
    }

    data class SocialPlatform(
        val name: String,
        val url: String,
        val iconRes: Int?,
        val iconVector: androidx.compose.ui.graphics.vector.ImageVector?,
        val accentColor: Color,
        val packageName: String? = null,
        val appScheme: String? = null
    )

    val socialPlatforms = remember {
        listOf(
            SocialPlatform(
                "GitHub",
                "https://github.com/ReSerendipity/DraftPeek",
                R.drawable.ic_social_github,
                null,
                Color(0xFF24292E),
                null,
                null
            ),
            SocialPlatform(
                "B站",
                "https://space.bilibili.com/499527473",
                R.drawable.ic_social_bilibili,
                null,
                Color(0xFFFB7299),
                "tv.danmaku.bili",
                "bilibili://"
            ),
            SocialPlatform(
                "抖音",
                "https://www.douyin.com/user/MS4wLjABAAAAcEdOoxVlfk3Ulx_usqR-3PHW4xxp6wYzRmsuRI_-fHBigPETTKLsv4fknIpFq6sP",
                R.drawable.ic_social_douyin,
                null,
                Color(0xFF000000),
                "com.ss.android.ugc.aweme",
                "snssdk1128://"
            ),
            SocialPlatform(
                "小红书",
                "https://www.xiaohongshu.com/user/profile/6a606c140000000010000801",
                R.drawable.ic_social_xiaohongshu,
                null,
                Color(0xFFFF2442),
                "com.xingin.xhs",
                null
            ),
            SocialPlatform(
                "快手",
                "https://www.kuaishou.com/profile/3x2sk6hj48i2mhs",
                R.drawable.ic_social_kuaishou,
                null,
                Color(0xFFFF4906),
                "com.smile.gifmaker",
                null
            )
        )
    }

    fun openSocialUrl(platform: SocialPlatform) {
        if (platform.url.isBlank()) {
            Toast.makeText(context, context.getString(R.string.profile_social_coming_soon), Toast.LENGTH_SHORT).show()
            return
        }

        val httpUri = Uri.parse(platform.url)
        val tag = "SocialUrlLauncher"

        fun tryIntent(intent: Intent, strategyName: String): Boolean = try {
            // 不再用 resolveActivity 预判：Android 11+ 包可见性受限时常量返回 null，
            // 直接 startActivity，由系统真正解析；无匹配 Activity 时抛 ActivityNotFoundException
            context.startActivity(intent)
            Log.d(tag, "Successfully opened ${platform.name} via: $strategyName")
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(tag, "Failed to open ${platform.name} via $strategyName: no activity resolved")
            false
        } catch (e: Exception) {
            Log.w(tag, "Failed to open ${platform.name} via $strategyName", e)
            false
        }

        try {
            // 策略1（优先）: 纯 ACTION_VIEW Intent，不指定 package
            // 让 Android 系统 Intent Resolver 根据已安装 APP 的 intent-filter 自动选择：
            // - 如果 APP 注册了该域名的 App Links / Deep Links，会直接跳转到 APP 内对应页面
            // - 如果没有 APP 处理，会让用户选择浏览器或其他APP打开
            // 这是 Android 官方推荐的标准做法，兼容性最好
            val viewIntent = Intent(Intent.ACTION_VIEW, httpUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryIntent(viewIntent, "system VIEW (no package)")) {
                return
            }

            // 策略2: App Scheme（仅作为fallback）
            // 注意：静态 scheme（如 snssdk1128://、bilibili://）通常只能打开 APP，
            // 能否导航到特定内容取决于 scheme 是否包含路径参数
            if (platform.appScheme != null) {
                val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(platform.appScheme)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (platform.packageName != null) {
                        setPackage(platform.packageName)
                    }
                }
                if (tryIntent(schemeIntent, "native scheme (${platform.appScheme})")) {
                    return
                }
            }

            // 策略3: 显式指定 package 的 HTTPS intent（最后尝试）
            // 某些 APP 可能需要显式指定 package 才能捕获 HTTPS 链接
            if (platform.packageName != null) {
                val appHttpIntent = Intent(Intent.ACTION_VIEW, httpUri).apply {
                    setPackage(platform.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryIntent(appHttpIntent, "HTTPS with package (${platform.packageName})")) {
                    return
                }
            }

            // 所有策略都失败，提示用户
            Toast.makeText(context, context.getString(R.string.profile_social_open_failed), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error opening ${platform.name}", e)
            Toast.makeText(context, context.getString(R.string.profile_social_open_failed), Toast.LENGTH_SHORT).show()
        }
    }

    if (showFeedbackDialog) {
        BrandDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text(stringResource(R.string.profile_feedback_title)) },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrandOutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text(stringResource(R.string.profile_feedback_description_hint)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (feedbackScreenshots.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.profile_feedback_screenshot_attached,
                                feedbackScreenshots.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = PrototypeTokens.accent
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            feedbackScreenshots.forEachIndexed { index, uri ->
                                Box {
                                    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                                    LaunchedEffect(uri) {
                                        bitmap = try {
                                            context.contentResolver.openInputStream(uri)?.use {
                                                BitmapFactory.decodeStream(it)?.asImageBitmap()
                                            }
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrototypeTokens.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        bitmap?.let {
                                            Image(
                                                bitmap = it,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } ?: Icon(
                                            Icons.Filled.Image,
                                            contentDescription = null,
                                            tint = PrototypeTokens.fgSoft,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                feedbackScreenshots =
                                                    feedbackScreenshots.toMutableList().apply { removeAt(index) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(
                                                R.string.profile_feedback_screenshot_remove
                                            ),
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    BrandOutlinedButton(
                        onClick = { screenshotPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_feedback_upload_screenshot))
                    }
                    Text(
                        text = stringResource(R.string.profile_feedback_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            },
            confirmButton = {
                BrandFilledButton(
                    text = stringResource(R.string.profile_feedback_submit),
                    onClick = { sendFeedbackEmail() }
                )
            },
            dismissButton = {
                BrandOutlinedButton(
                    text = stringResource(R.string.profile_dialog_cancel),
                    onClick = { showFeedbackDialog = false }
                )
            }
        )
    }

    if (showVerifyAppDialog) {
        VerifyAppDialog(
            verifyAppState = verifyAppState,
            onVerifyClick = {
                if (onVerifyApp != null) {
                    verifyAppState = VerifyAppState.Loading
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        verifyAppState = try {
                            onVerifyApp(context)
                        } catch (e: Exception) {
                            VerifyAppState.Error(e.message ?: "未知错误")
                        }
                    }
                }
            },
            onDismiss = {
                showVerifyAppDialog = false
                verifyAppState = VerifyAppState.Idle
            }
        )
    }

    if (showOpenSourceDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showOpenSourceDialog = false }
        )
    }

    if (showThemeManageDialog) {
        ThemeManageDialog(
            themeManageViewModel = themeManageViewModel,
            currentEditorThemeId = settings.editorThemeId,
            onThemeSelected = { themeId -> settingsViewModel.updateEditorThemeId(themeId) },
            onDismiss = { showThemeManageDialog = false }
        )
    }

    if (uiState.selectedDay != null) {
        val detail = viewModel.getDayDetail(uiState.selectedDay!!)
        if (detail != null) {
            DayDetailDialog(
                detail = detail,
                onDismiss = { viewModel.dismissDayDetail() }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            BrandTopBar(
                title = stringResource(R.string.profile_page_title),
                titleStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 1.2.sp,
                    lineHeight = 28.sp
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(PrototypeSpacing.AvatarSize + 8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(PrototypeSpacing.AvatarSize)
                            .clip(CircleShape)
                            .background(PrototypeTokens.accentSoft)
                            .clickable {
                                pickImageLauncher.launch("image/*")
                            }
                            .accessibilityEnhanced(
                                contentDescription = stringResource(R.string.profile_select_avatar)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(PrototypeSpacing.AvatarSize)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = getInitials(settings.userName),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = PrototypeTokens.accent
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrototypeTokens.accent)
                            .clickable {
                                pickImageLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.profile_select_avatar),
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = settings.userName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = fg,
                    lineHeight = 24.sp,
                    modifier = Modifier.clickable {
                        editNameText = settings.userName
                        showEditNameDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                BrandPill(
                    text = versionName,
                    dotColor = SemanticColors.Success
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            PeriodChipRow(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.selectPeriod(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatCardGrid(
                stats = periodStats,
                formatDuration = viewModel::formatDuration,
                formatNumber = viewModel::formatNumber
            )

            Spacer(modifier = Modifier.height(20.dp))

            GreetingSection(
                userName = settings.userName,
                daysSinceFirstUse = viewModel.getTotalDaysSinceFirstUse(),
                unlockedAchievements = unlockedAchievements,
                onAchievementClick = onNavigateToAchievements
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(label = stringResource(R.string.profile_section_activity))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(PrototypeShapes.Card)
                    .background(PrototypeTokens.surface)
                    .border(1.dp, border, PrototypeShapes.Card)
                    .padding(14.dp)
            ) {
                YearHeatmapNew(
                    activities = yearActivities,
                    onDayClick = { date -> viewModel.onDaySelected(date) },
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_editor))

            BrandSettingRow(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.stats_achievements_title),
                value = "${unlockedAchievements.size}/${AchievementDefinitions.allAchievements.size}",
                onClick = onNavigateToAchievements,
                showDivider = true
            )
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
                icon = Icons.Filled.Palette,
                label = stringResource(R.string.profile_setting_theme_manage),
                value = stringResource(R.string.profile_theme_import_count, importedThemes.size),
                onClick = { showThemeManageDialog = true },
                showDivider = true
            )
            val resolvedMarkdownTheme = try {
                MarkdownTheme.valueOf(settings.markdownThemeName)
            } catch (_: Exception) {
                MarkdownTheme.DEFAULT
            }
            BrandSettingRow(
                icon = Icons.Filled.Visibility,
                label = stringResource(R.string.profile_setting_markdown_theme),
                value = stringResource(resolvedMarkdownTheme.displayNameResId),
                onClick = { showMarkdownThemeDialog = true },
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

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_accessibility))

            BrandSettingRow(
                icon = Icons.Filled.Accessibility,
                label = stringResource(R.string.accessibility_page_title),
                onClick = onNavigateToAccessibility,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.stats_feature_flags_section))

            // Feature toggle master switch (always shown)
            val featureToggleEnabled =
                flagStates[FeatureFlag.FEATURE_TOGGLE] ?: FeatureFlag.FEATURE_TOGGLE.defaultEnabled
            val visibleFlags = if (featureToggleEnabled) {
                FeatureFlag.entries.toList()
            } else {
                listOf(FeatureFlag.FEATURE_TOGGLE)
            }
            visibleFlags.forEachIndexed { index, flag ->
                BrandSwitchSettingRow(
                    icon = Icons.Filled.Flag,
                    label = flag.displayName(),
                    checked = flagStates[flag] ?: flag.defaultEnabled,
                    onCheckedChange = { featureToggleManager?.setEnabled(flag, it) },
                    showDivider = index < visibleFlags.lastIndex
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_network))

            BrandSettingRow(
                icon = Icons.Filled.Code,
                label = stringResource(R.string.profile_setting_github_mirror),
                value = settings.githubMirrorUrl.ifEmpty {
                    stringResource(R.string.profile_setting_github_mirror_hint)
                },
                onClick = {
                    mirrorUrlText = settings.githubMirrorUrl
                    showMirrorUrlDialog = true
                },
                showDivider = false,
                iconPainter = painterResource(id = R.drawable.ic_social_github)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_feedback))

            BrandSettingRow(
                icon = Icons.Filled.Feedback,
                label = stringResource(R.string.profile_feedback_title),
                onClick = { showFeedbackDialog = true },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_social))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(PrototypeShapes.Card)
                    .background(PrototypeTokens.surface)
                    .border(1.dp, border, PrototypeShapes.Card)
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    socialPlatforms.forEach { platform ->
                        SocialPlatformItem(
                            label = platform.name,
                            iconRes = platform.iconRes,
                            iconVector = platform.iconVector,
                            onClick = { openSocialUrl(platform) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== AI Protection: Security Status Section =====
            SectionHeader(label = stringResource(R.string.security_status_section_title))

            val aiState = AiProtectionStateHolder.current
            val isSignatureMismatch =
                com.draftpeek.core.common.security.AiDetectionSignal.SIGNATURE_MISMATCH in aiState.triggeredSignals
            val isDexTampered =
                com.draftpeek.core.common.security.AiDetectionSignal.DEX_TAMPERED in aiState.triggeredSignals

            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(
                    if (isSignatureMismatch) {
                        R.string.security_status_signature_unverified
                    } else {
                        R.string.security_status_signature_verified
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(
                    if (isDexTampered) {
                        R.string.security_status_dex_failed
                    } else {
                        R.string.security_status_dex_passed
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(
                    when (aiState.threatLevel) {
                        AiThreatLevel.SAFE -> R.string.security_status_environment_safe
                        AiThreatLevel.SUSPICIOUS -> R.string.security_status_environment_suspicious
                        AiThreatLevel.HOSTILE -> R.string.security_status_environment_hostile
                    }
                ),
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.security_status_recheck),
                onClick = {
                    // Re-verify is handled at app level; this triggers a toast
                    Toast.makeText(context, R.string.security_status_recheck, Toast.LENGTH_SHORT).show()
                },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.security_status_export_report),
                onClick = {
                    Toast.makeText(context, R.string.security_status_export_report, Toast.LENGTH_SHORT).show()
                },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.DeleteSweep,
                label = stringResource(R.string.security_status_clear_logs),
                onClick = {
                    Toast.makeText(context, R.string.security_status_clear_logs, Toast.LENGTH_SHORT).show()
                },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_about))

            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(R.string.profile_verify_app),
                onClick = { showVerifyAppDialog = true },
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.profile_setting_version),
                value = versionName,
                onClick = {},
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Link,
                label = stringResource(R.string.profile_open_source_license),
                onClick = { showOpenSourceDialog = true },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_danger), isDanger = true)

            BrandSettingRow(
                icon = Icons.Filled.DeleteSweep,
                label = stringResource(R.string.profile_setting_clear_cache),
                onClick = { showClearCacheDialog = true },
                isDanger = true,
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Restore,
                label = stringResource(R.string.profile_setting_reset_settings),
                onClick = { showResetDialog = true },
                isDanger = true,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.profile_footer_made_with),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = muted,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_footer_for_devs),
                    fontSize = 11.sp,
                    color = muted.copy(alpha = 0.7f),
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

private fun getInitials(name: String): String {
    if (name.isEmpty()) return "用"
    val firstChar = name.trim().first()
    return if (firstChar.isLetter()) {
        firstChar.uppercaseChar().toString()
    } else {
        firstChar.toString()
    }
}

@Composable
private fun SectionHeader(label: String, isDanger: Boolean = false) {
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
private fun SocialPlatformItem(
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

@Composable
private fun ConfirmActionDialog(
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
    // 与 DraftPeekApp.applySavedLanguage() 保持一致（见 AGENTS.md 规范）：
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
sealed class VerifyAppState {
    data object Idle : VerifyAppState()
    data object Loading : VerifyAppState()
    data class Success(val fingerprint: String) : VerifyAppState()
    data class Failed(val reason: String) : VerifyAppState()
    data class Error(val message: String) : VerifyAppState()
}

@Composable
private fun VerifyAppDialog(verifyAppState: VerifyAppState, onVerifyClick: () -> Unit, onDismiss: () -> Unit) {
    BrandDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_verify_app_title)) },
        content = {
            Column {
                Text(
                    text = stringResource(R.string.profile_verify_app_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = PrototypeTokens.fgSoft
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (verifyAppState) {
                    is VerifyAppState.Idle -> {
                        BrandFilledButton(
                            text = stringResource(R.string.profile_verify_app_button),
                            onClick = onVerifyClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is VerifyAppState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrototypeTokens.accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "验证中...",
                                color = PrototypeTokens.fg
                            )
                        }
                    }
                    is VerifyAppState.Success -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = SemanticColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.profile_verify_app_success),
                                    color = SemanticColors.Success,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_fingerprint_label),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PrototypeTokens.muted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = verifyAppState.fingerprint,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = PrototypeTokens.fg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrototypeTokens.surface)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_fingerprint_hint),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PrototypeTokens.muted
                            )
                        }
                    }
                    is VerifyAppState.Failed -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = SemanticColors.Danger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_app_failed),
                                color = SemanticColors.Danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verifyAppState.reason,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = PrototypeTokens.fgSoft
                        )
                    }
                    is VerifyAppState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = SemanticColors.Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profile_verify_app_error),
                                color = SemanticColors.Warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verifyAppState.message,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = PrototypeTokens.fgSoft
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandFilledButton(
                text = stringResource(R.string.profile_dialog_confirm),
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun MarkdownThemePickerDialog(
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
