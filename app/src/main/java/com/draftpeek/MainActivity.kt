/**
 * DraftPeek 应用主 Activity。
 *
 * **文件功能**：应用主界面入口，承载 Jetpack Compose 导航宿主、主题设置、折叠屏适配等核心 UI 逻辑。
 *
 * **主要类/接口**：
 * - [MainActivity] - 应用主 Activity
 * - [DraftPeekContent] - Compose 主内容可组合函数
 *
 * **模块依赖**：
 * - `core/ui`：使用 [DraftPeekTheme]、[CustomScaffold]、[BrandToastHost] 等 UI 组件
 * - `core/common`：使用 [FpsMonitor]、[FeatureToggleManager] 等工具
 * - `core/data`：使用 [UserActivityRepository] 记录用户活动
 * - `feature/settings`：使用 [SettingsViewModel] 获取用户设置
 * - `navigation`：使用 [DraftPeekNavHost] 和 [Route] 进行导航管理
 * - Jetpack Compose + Navigation Compose + Hilt 依赖注入
 *
 * @see ComponentActivity
 */
package com.draftpeek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.draftpeek.R
import com.draftpeek.core.common.util.FpsMonitor
import com.draftpeek.core.ui.component.BrandToastHost
import com.draftpeek.core.ui.component.CustomScaffold
import com.draftpeek.core.ui.component.TabBarItem
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.layout.FoldableState
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.layout.layoutMode
import com.draftpeek.core.ui.layout.rememberFoldableState
import com.draftpeek.core.common.feature.FeatureToggleManager
import com.draftpeek.core.ui.composition.LocalFeatureToggle
import com.draftpeek.core.ui.composition.LocalIsLandscape
import com.draftpeek.core.ui.composition.LocalFoldInfo
import com.draftpeek.core.ui.composition.LocalIsWideScreen
import com.draftpeek.core.ui.composition.FoldInfo as CompositionFoldInfo
import com.draftpeek.core.ui.theme.AccessibilityState
import com.draftpeek.core.ui.theme.AppFonts
import com.draftpeek.core.ui.theme.DraftPeekTheme
import com.draftpeek.core.ui.theme.FontOptions
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.feature.settings.model.AppTheme
import androidx.window.layout.FoldingFeature
import com.draftpeek.feature.settings.viewmodel.SettingsViewModel
import com.draftpeek.navigation.DraftPeekNavHost
import com.draftpeek.navigation.Route
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 应用主 Activity，用户启动后非首次启动直接进入此界面。
 *
 * **职责**：
 * 1. 设置 Compose 内容和主题
 * 2. 管理导航控制器和底部导航栏
 * 3. 处理折叠屏/大屏适配（分屏模式）
 * 4. 配置系统栏颜色和高刷新率
 * 5. 记录应用启动事件
 *
 * **使用场景**：用户正常使用应用时的主界面入口，包含文件浏览和个人/设置两个标签页。
 *
 * @see AndroidEntryPoint
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userActivityRepository: UserActivityRepository

    @Inject
    lateinit var featureToggleManager: FeatureToggleManager

    /**
     * Activity 创建时调用，初始化 UI 和相关组件。
     *
     * @param savedInstanceState 之前保存的实例状态，若为全新启动则为 null
     */
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FpsMonitor.applyHighRefreshRate(window, this)

        // 启用沉浸式边到边显示模式
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            // 使用 STARTED 而非 CREATED，确保只在 UI 可见时记录启动事件
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userActivityRepository.recordAppLaunch()
            }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val accessibilityState = AccessibilityState(
                colorBlindMode = settings.colorBlindMode,
                highContrastMode = settings.highContrastMode,
                textScale = settings.textScale,
                screenReaderOptimized = settings.screenReaderOptimized,
                vibrationFeedback = settings.vibrationFeedback,
                nonColorIndicators = settings.nonColorIndicators,
            )

            val appFonts = AppFonts(
                uiFontFamily = FontOptions.getUiFontById(settings.uiFontFamilyId).fontFamily,
                codeFontFamily = FontOptions.getCodeFontById(settings.codeFontFamilyId).fontFamily,
            )

            DraftPeekTheme(
                darkTheme = darkTheme,
                accessibilityState = accessibilityState,
                appFonts = appFonts,
            ) {
                val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

                // 设置系统状态栏和导航栏外观
                SideEffect {
                    val window = window ?: return@SideEffect
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme

                    @Suppress("DEPRECATION")
                    window.navigationBarColor = surfaceColor
                }
                val windowSizeClass = calculateWindowSizeClass(activity = this)
                val layoutMode = windowSizeClass.layoutMode()
                val foldInfo by rememberFoldableState()
                // 将布局层 FoldInfo 转换为 Composition 层 FoldInfo 以便通过 CompositionLocal 共享
                val compositionFoldInfo = CompositionFoldInfo(
                    isFolded = foldInfo.state == FoldableState.CLOSED,
                    isSeparating = foldInfo.isSeparating,
                    foldPositionRatio = if (foldInfo.isSeparating && foldInfo.bounds.height() > 0) {
                        foldInfo.bounds.exactCenterY() / resources.displayMetrics.heightPixels
                    } else {
                        0.5f
                    },
                )
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val isWideScreen = layoutMode != LayoutMode.COMPACT

                CompositionLocalProvider(
                    LocalFeatureToggle provides featureToggleManager,
                    LocalIsLandscape provides isLandscape,
                    LocalIsWideScreen provides isWideScreen,
                    LocalFoldInfo provides compositionFoldInfo,
                ) {
                    BrandToastHost {
                        DraftPeekContent(
                            layoutMode = layoutMode,
                            foldInfo = foldInfo,
                            darkTheme = darkTheme,
                        )
                    }
                }
            }
        }
    }
}

/**
 * DraftPeek 主内容可组合函数。
 *
 * 包含导航控制器、底部导航栏、导航宿主等核心 UI 结构，支持折叠屏半开状态下自动启用分屏。
 *
 * @param layoutMode 当前布局模式（紧凑/中等/展开），决定是否显示分屏
 * @param foldInfo 折叠屏状态信息，用于检测半开姿势
 * @param darkTheme 是否为深色主题，用于分屏编辑器
 */
@Composable
private fun DraftPeekContent(layoutMode: LayoutMode, foldInfo: FoldInfo, darkTheme: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Split view state -- only applicable in EXPANDED mode
    var isSplitViewActive by rememberSaveable { mutableStateOf(false) }
    var splitViewFileUri by rememberSaveable { mutableStateOf<String?>(null) }

    // Auto-enable split view in book/half-opened posture
    if (foldInfo.state == FoldableState.HALF_OPENED && foldInfo.orientation == FoldingFeature.Orientation.HORIZONTAL && layoutMode != LayoutMode.COMPACT) {
        if (!isSplitViewActive) {
            isSplitViewActive = true
        }
    }

    // Navigation items: Files / Me (2-Tab structure)
    val navItems = listOf(
        TabBarItem(
            label = stringResource(R.string.app_nav_files),
            icon = StrokeIcons.FolderOutline,
            route = Route.Browser.route,
        ),
        TabBarItem(
            label = stringResource(R.string.app_nav_me),
            icon = StrokeIcons.Person,
            route = Route.Profile.route,
        ),
    )

    val isInEditor = currentRoute == Route.Editor.route

    val onSplitViewFileSelected: (FileItem) -> Unit = { item ->
        splitViewFileUri = item.uri.toString()
    }

    CustomScaffold(
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        navItems = navItems,
        layoutMode = layoutMode,
        showNavigation = !isInEditor,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        DraftPeekNavHost(
            navController = navController,
            layoutMode = layoutMode,
            foldInfo = foldInfo,
            darkTheme = darkTheme,
            isSplitViewActive = isSplitViewActive,
            splitViewFileUri = splitViewFileUri,
            onSplitViewFileSelected = onSplitViewFileSelected,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
