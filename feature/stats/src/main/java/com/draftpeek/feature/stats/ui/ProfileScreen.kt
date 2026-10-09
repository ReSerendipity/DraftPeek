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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.draftpeek.core.ui.component.BrandDialog
import com.draftpeek.core.ui.component.BrandFilledButton
import com.draftpeek.core.ui.component.BrandOutlinedButton
import com.draftpeek.core.ui.component.BrandOutlinedTextField
import com.draftpeek.core.ui.component.BrandPill
import com.draftpeek.core.ui.component.BrandSettingRow
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.component.LocalToastHost
import com.draftpeek.core.ui.component.accessibilityEnhanced
import com.draftpeek.core.ui.layout.LayoutMode
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
import com.draftpeek.feature.stats.account.AccountTimeFormat
import com.draftpeek.feature.stats.account.AccountUiState
import com.draftpeek.feature.stats.account.RelativeUnit
import com.draftpeek.feature.stats.account.SyncUiStatus
import com.draftpeek.feature.stats.ui.component.DayDetailDialog
import com.draftpeek.feature.stats.ui.component.GreetingSection
import com.draftpeek.feature.stats.ui.component.PeriodChipRow
import com.draftpeek.feature.stats.ui.component.StatCardGrid
import com.draftpeek.feature.stats.ui.component.YearHeatmapNew
import com.draftpeek.feature.stats.util.AchievementDefinitions
import com.draftpeek.feature.stats.viewmodel.AccountViewModel
import com.draftpeek.feature.stats.viewmodel.StatsMessage
import com.draftpeek.feature.stats.viewmodel.StatsViewModel
import com.draftpeek.feature.stats.viewmodel.ThemeManageViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onNavigateToEditorSettings: () -> Unit = {},
    onNavigateToLab: () -> Unit = {},
    onNavigateToAboutSecurity: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
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

    // 身份区（设计回函 v3 · 阶段 A）：状态由 AccountViewModel 驱动，五态见 AccountUiState。
    val accountViewModel: AccountViewModel = hiltViewModel()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    // 「已同步 · N 分钟前」的相对时间：页面可见时每分钟刷新（设计回函 v3 §3）；
    // 以 accountState 为键 —— 状态变化（含同步完成）时立即重算，避免显示滞后一分钟。
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(accountState) {
        nowMillis = System.currentTimeMillis()
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val showToast = LocalToastHost.current
    val signInComingSoon = stringResource(R.string.account_sign_in_coming_soon)
    // 阶段 A 的验收开关只在可调试构建暴露（不引入 BuildConfig 依赖）
    val isDebuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val pageBg = PrototypeTokens.pageBackground
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }
    var showMirrorUrlDialog by remember { mutableStateOf(false) }
    var mirrorUrlText by remember { mutableStateOf(settings.githubMirrorUrl) }
    LaunchedEffect(settings.githubMirrorUrl) { mirrorUrlText = settings.githubMirrorUrl }
    var showMarkdownThemeDialog by remember { mutableStateOf(false) }
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
    var showThemeManageDialog by remember { mutableStateOf(false) }

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

    if (showSignOutDialog) {
        BrandDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.account_sign_out)) },
            content = { Text(stringResource(R.string.account_sign_out_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        accountViewModel.signOut()
                        showSignOutDialog = false
                    }
                ) {
                    Text(stringResource(R.string.profile_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.profile_dialog_cancel))
                }
            }
        )
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
                AccountIdentityHeader(
                    state = accountState,
                    nowMillis = nowMillis,
                    versionName = versionName,
                    avatarBitmap = avatarBitmap,
                    localName = settings.userName,
                    onPickAvatar = { pickImageLauncher.launch("image/*") },
                    onEditName = {
                        editNameText = settings.userName
                        showEditNameDialog = true
                    },
                    onSignIn = { showToast(signInComingSoon) },
                    onSyncNow = { accountViewModel.retrySync() },
                    onRetry = { accountViewModel.retrySync() },
                    onCancelSync = { accountViewModel.cancelSync() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 设置入口（实施指导书 §2.5 屏 20）：四个独立页面的入口归拢成一组。
            SectionHeader(label = stringResource(R.string.profile_section_settings))

            BrandSettingRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.profile_open_editor_settings),
                onClick = onNavigateToEditorSettings,
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Flag,
                label = stringResource(R.string.profile_open_lab),
                onClick = onNavigateToLab,
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Accessibility,
                label = stringResource(R.string.accessibility_page_title),
                onClick = onNavigateToAccessibility,
                showDivider = true
            )
            BrandSettingRow(
                icon = Icons.Filled.Security,
                label = stringResource(R.string.profile_open_about_security),
                onClick = onNavigateToAboutSecurity,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 数据区（只读）：活跃度与成就仅作展示，不在本页编辑。
            SectionHeader(label = stringResource(R.string.profile_section_stats))

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
            BrandSettingRow(
                icon = Icons.Filled.EmojiEvents,
                label = stringResource(R.string.stats_achievements_title),
                value = "${unlockedAchievements.size}/${AchievementDefinitions.allAchievements.size}",
                onClick = onNavigateToAchievements,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(label = stringResource(R.string.profile_section_editor))

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
                showDivider = false
            )

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
                showDivider = accountState is AccountUiState.SignedIn
            )
            // 退出登录移入危险区（设计回函 v2 §2）：红色 + 二次确认
            if (accountState is AccountUiState.SignedIn) {
                BrandSettingRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = stringResource(R.string.account_sign_out),
                    onClick = { showSignOutDialog = true },
                    isDanger = true,
                    showDivider = false
                )
            }

            // 阶段 A 验收开关：只在可调试构建出现，用于轮转身份区五态
            // （账号体系属阶段 B，五态无法自然产生）。阶段 B 落地后应删除。
            if (isDebuggable) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionHeader(label = stringResource(R.string.account_dev_section))

                BrandSettingRow(
                    icon = Icons.Filled.Flag,
                    label = stringResource(R.string.account_dev_simulate),
                    onClick = { accountViewModel.simulateNextState() },
                    showDivider = false
                )
            }

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

/**
 * 「我的」页身份区（设计回函 v3 · 阶段 A 五态）。
 *
 * 完全由 [AccountUiState] 驱动：未登录（占位 + 价值主张 + 登录按钮）／已登录（昵称 + 邮箱 +
 * 同步状态行）。阶段 B 接入真实账号与同步后本组件无需改动。
 *
 */
@Composable
private fun AccountIdentityHeader(
    state: AccountUiState,
    nowMillis: Long,
    versionName: String,
    avatarBitmap: ImageBitmap?,
    localName: String,
    onPickAvatar: () -> Unit,
    onEditName: () -> Unit,
    onSignIn: () -> Unit,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit,
    onCancelSync: () -> Unit
) {
    val fg = PrototypeTokens.fg
    val muted = PrototypeTokens.muted
    val border = PrototypeTokens.border
    val signedIn = state as? AccountUiState.SignedIn

    // 设计回函 v4 §1：**可编辑的是本地资料**（DataStore），「未登录」描述的是**账户/同步状态**，
    // 两者各司其职。因此本地资料的编辑入口在「未登录」态保留（这是设计侧的底线要求）；
    // 已登录态的资料合并/迁移策略由阶段 B 设计稿给出，本批**不预支**，只读展示。
    val localProfileEditable = signedIn == null
    val displayName = if (localProfileEditable) localName else signedIn?.displayName ?: localName

    Column(
        modifier = Modifier.fillMaxWidth(),
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
                    .then(
                        if (localProfileEditable) {
                            Modifier.clickable(onClick = onPickAvatar)
                        } else {
                            Modifier
                        }
                    )
                    .accessibilityEnhanced(
                        // 未登录态头像可点换 → 播报操作名；已登录态头像是装饰（昵称紧邻下方）→ 不播报
                        contentDescription = if (localProfileEditable) {
                            stringResource(R.string.profile_select_avatar)
                        } else {
                            null
                        }
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
                        text = getInitials(displayName),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = PrototypeTokens.accent
                    )
                }
            }
            if (localProfileEditable) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrototypeTokens.accent)
                        .clickable(onClick = onPickAvatar),
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 本地资料昵称（未登录态可点改名；编辑徽标在头像上）
        Text(
            text = displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = fg,
            lineHeight = 24.sp,
            modifier = if (localProfileEditable) {
                Modifier.clickable(onClick = onEditName)
            } else {
                Modifier
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 分隔：把「本地资料」与「账户状态」两块分开（设计回函 v4 §1）
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 1.dp)
                .background(border)
        )

        Spacer(modifier = Modifier.height(10.dp))

        when (state) {
            AccountUiState.SignedOut -> {
                // 账户状态行：状态 + 价值主张同一行（v4 §1：「未登录」不再是身份区标题）
                Text(
                    text = stringResource(R.string.account_signed_out_title) +
                        ACCOUNT_STATUS_SEPARATOR +
                        stringResource(R.string.account_signed_out_desc),
                    fontSize = 13.sp,
                    color = muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                BrandFilledButton(
                    text = stringResource(R.string.account_sign_in),
                    onClick = onSignIn
                )
            }

            is AccountUiState.SignedIn -> {
                Text(
                    text = state.email,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = muted
                )
                Spacer(modifier = Modifier.height(10.dp))
                SyncStatusRow(
                    sync = state.sync,
                    nowMillis = nowMillis,
                    onSyncNow = onSyncNow,
                    onRetry = onRetry,
                    onCancelSync = onCancelSync
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        BrandPill(
            text = versionName,
            dotColor = SemanticColors.Success
        )
    }
}

/** 账户状态行里「状态 · 价值主张」的分隔符（标点，非可翻译文案）。 */
private const val ACCOUNT_STATUS_SEPARATOR = " · "

/**
 * 同步状态行（设计回函 v3 §3）：**一律行内提示，不弹窗**。
 *
 * 「正在同步…」超过 [SyncUiStatus.SYNC_CANCEL_AFTER_MS] 后行内出现「取消」。
 */
@Composable
private fun SyncStatusRow(
    sync: SyncUiStatus,
    nowMillis: Long,
    onSyncNow: () -> Unit,
    onRetry: () -> Unit,
    onCancelSync: () -> Unit
) {
    // 同步中超 30 秒才给「取消」；用独立计时器而非 60 秒的页面 ticker，保证阈值准确
    var cancelAvailable by remember(sync) { mutableStateOf(false) }
    LaunchedEffect(sync) {
        cancelAvailable = false
        if (sync is SyncUiStatus.Syncing) {
            delay(SyncUiStatus.SYNC_CANCEL_AFTER_MS)
            cancelAvailable = true
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (sync) {
            is SyncUiStatus.Synced -> {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SemanticColors.Success
                )
                Text(
                    text = syncedAgoText(sync.lastSuccessAtMillis, nowMillis),
                    style = MonoLabelStyle,
                    color = PrototypeTokens.fgSoft
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSyncNow) {
                    Text(text = stringResource(R.string.account_sync_now))
                }
            }

            SyncUiStatus.Syncing -> {
                Text(
                    text = stringResource(R.string.account_syncing),
                    style = MonoLabelStyle,
                    color = PrototypeTokens.fgSoft
                )
                if (cancelAvailable) {
                    TextButton(onClick = onCancelSync) {
                        Text(text = stringResource(R.string.account_sync_cancel))
                    }
                }
            }

            SyncUiStatus.Failed -> Text(
                text = stringResource(R.string.account_sync_failed),
                style = MonoLabelStyle,
                color = SemanticColors.Danger,
                modifier = Modifier.clickable(onClick = onRetry)
            )

            SyncUiStatus.Offline -> Text(
                text = stringResource(R.string.account_offline),
                style = MonoLabelStyle,
                color = PrototypeTokens.muted
            )
        }
    }
}

/** 「已同步 · N 分钟前」；不足 1 分钟显示「刚刚同步」。 */
@Composable
private fun syncedAgoText(lastSuccessAtMillis: Long, nowMillis: Long): String {
    val relative = AccountTimeFormat.relativeTime(nowMillis, lastSuccessAtMillis)
    return when (relative.unit) {
        RelativeUnit.JUST_NOW -> stringResource(R.string.account_synced_just_now)
        RelativeUnit.MINUTES -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_minutes, relative.value)
        )

        RelativeUnit.HOURS -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_hours, relative.value)
        )

        RelativeUnit.DAYS -> stringResource(
            R.string.account_synced_ago,
            stringResource(R.string.account_ago_days, relative.value)
        )
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
