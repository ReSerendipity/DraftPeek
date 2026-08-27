/**
 * DraftPeek 应用导航宿主定义。
 *
 * **文件功能**：定义顶层导航图（NavGraph），注册所有页面 composable 路由，处理自适应布局（分屏/单屏），
 *               集成转场动画，并通过 Hilt EntryPoint 获取 TabManager 实例。
 *
 * **主要类/接口**：
 * - [TabManagerEntryPoint] - Hilt 入口点接口，用于在 Composable 中获取 TabManager
 * - [DraftPeekNavHost] - 顶层导航宿主 Composable 函数
 *
 * **模块依赖**：
 * - `core/ui`：使用 [SplitScreenLayout]、[LayoutMode]、[FoldInfo] 等布局组件
 * - `feature/browser`：使用 [FileBrowserScreen]、[SnippetScreen]、[SampleFilesScreen] 等页面
 * - `feature/editor`：使用 [EditorScreen]、[DiffScreen]、[TabManager]
 * - `feature/stats`：使用 [ProfileScreen]、[AccessibilityScreen]、[AchievementScreen]
 * - `security`：使用 [ApkIntegrityChecker]、[DexIntegrityChecker] 用于应用完整性验证
 * - Navigation Compose + Hilt EntryPoint
 */
package com.draftpeek.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.draftpeek.R
import com.draftpeek.core.ui.component.BrandTopBar
import com.draftpeek.core.ui.layout.FoldInfo
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.layout.SplitScreenLayout
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.browser.model.FileItem
import com.draftpeek.feature.browser.ui.BrowseHistoryScreen
import com.draftpeek.feature.browser.ui.FileBrowserScreen
import com.draftpeek.feature.browser.ui.GitScreen
import com.draftpeek.feature.browser.ui.SampleFilesScreen
import com.draftpeek.feature.browser.ui.SnippetScreen
import com.draftpeek.feature.editor.tabs.TabManager
import com.draftpeek.feature.editor.ui.DiffScreen
import com.draftpeek.feature.editor.ui.EditorScreen
import com.draftpeek.feature.stats.ui.AccessibilityScreen
import com.draftpeek.feature.stats.ui.AchievementScreen
import com.draftpeek.feature.stats.ui.ProfileScreen
import com.draftpeek.feature.stats.ui.VerifyAppState
import com.draftpeek.feature.terminal.ui.TerminalScreen
import com.draftpeek.security.ApkIntegrityChecker
import com.draftpeek.security.DexIntegrityChecker
import dagger.hilt.android.EntryPointAccessors

/**
 * Hilt 入口点接口，用于在 Composable 函数中获取 [TabManager] 实例。
 *
 * TabManager 是 Singleton 作用域，但无法直接通过 @Inject 在 Composable 中注入，
 * 因此需要通过 EntryPoint 从 Activity 组件中获取。
 */
@dagger.hilt.InstallIn(dagger.hilt.android.components.ActivityComponent::class)
@dagger.hilt.EntryPoint
interface TabManagerEntryPoint {
    /**
     * 获取 TabManager 单例实例。
     * @return 标签页管理器实例
     */
    fun tabManager(): TabManager
}

/**
 * DraftPeek 顶层导航宿主 Composable 函数。
 *
 * 注册所有页面路由并配置相应的转场动画。在展开布局模式下支持左右分屏：
 * 左侧为文件浏览器，右侧为编辑器。单屏模式下则采用全屏页面切换。
 *
 * 起始目的地为 [Route.Browser]（文件浏览页面）。
 *
 * @param navController 管理导航状态的 [NavHostController]
 * @param layoutMode 当前布局模式，用于自适应布局判断
 * @param foldInfo 折叠屏状态信息，传递给各子页面用于折叠屏适配
 * @param darkTheme 是否为深色主题，传递给编辑器页面
 * @param isSplitViewActive 是否启用分屏视图（浏览器+编辑器左右并排），仅在 [LayoutMode.EXPANDED] 时有效
 * @param splitViewFileUri 分屏视图编辑器窗格当前显示的文件 URI，若分屏中未打开文件则为 null
 * @param onSplitViewFileSelected 分屏视图激活时在浏览器中选择文件的回调
 * @param modifier 应用到 NavHost 的可选修饰符（例如来自 Scaffold 的内边距）
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DraftPeekNavHost(
    navController: NavHostController,
    layoutMode: LayoutMode,
    foldInfo: FoldInfo = FoldInfo(),
    darkTheme: Boolean = false,
    isSplitViewActive: Boolean = false,
    splitViewFileUri: String? = null,
    onSplitViewFileSelected: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    require(context is androidx.activity.ComponentActivity) {
        "DraftPeekNavHost must be hosted in ComponentActivity, got: ${context::class.java}"
    }
    val tabManager = remember {
        EntryPointAccessors.fromActivity(
            context,
            TabManagerEntryPoint::class.java,
        ).tabManager()
    }
    NavHost(
        navController = navController,
        startDestination = Route.Browser.route,
        modifier = modifier.background(PrototypeTokens.pageBackground),
    ) {
        // ---- Browser --------------------------------------------------
        composable(
            route = Route.Browser.route,
            enterTransition = { DraftPeekTransitions.topLevelEnter },
            exitTransition = { DraftPeekTransitions.topLevelExit },
            popEnterTransition = { DraftPeekTransitions.topLevelEnter },
            popExitTransition = { DraftPeekTransitions.topLevelExit },
        ) {
            if (layoutMode == LayoutMode.EXPANDED && isSplitViewActive) {
                // Split view: browser on the left, editor on the right
                SplitScreenLayout(
                    startContent = {
                        FileBrowserScreen(
                            onFileClick = onSplitViewFileSelected,
                            onCompareFiles = { leftUri, rightUri ->
                                navController.navigate(Route.Diff.createRoute(leftUri, rightUri))
                            },
                            onNavigateToSamples = {
                                navController.navigate(Route.Samples.route)
                            },
                            onNavigateToHistory = {
                                navController.navigate(Route.BrowseHistory.route)
                            },
                            onNavigateToSnippets = {
                                navController.navigate(Route.Snippets.route)
                            },
                            onNavigateToTerminal = { cwd ->
                                navController.navigate(Route.Terminal.createRoute(cwd))
                            },
                            layoutMode = layoutMode,
                            foldInfo = foldInfo,
                        )
                    },
                    endContent = {
                        if (splitViewFileUri != null) {
                            EditorScreen(
                                onNavigateUp = { /* In split view, closing editor just clears the file */ },
                                layoutMode = layoutMode,
                                foldInfo = foldInfo,
                                darkTheme = darkTheme,
                                fileUri = splitViewFileUri,
                            )
                        } else {
                            // No file selected yet -- show placeholder
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.app_hint_select_file_editor),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PrototypeTokens.fgSoft,
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    initialSplitRatio = 0.35f,
                    minSplitRatio = 0.25f,
                    maxSplitRatio = 0.6f,
                )
            } else {
                FileBrowserScreen(
                    onFileClick = { item: FileItem ->
                        val fileUri = item.uri.toString()
                        val existingTab = tabManager.findTabByUri(fileUri)
                        if (existingTab != null) {
                            // File already open in a tab -- switch to it and navigate
                            // to the editor if we're not already there.
                            tabManager.setActiveTab(existingTab.id)
                            navController.navigate(Route.Editor.createRoute(fileUri)) {
                                popUpTo(Route.Browser.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(Route.Editor.createRoute(fileUri))
                        }
                    },
                    onCompareFiles = { leftUri, rightUri ->
                        navController.navigate(Route.Diff.createRoute(leftUri, rightUri))
                    },
                    onNavigateToSamples = {
                        navController.navigate(Route.Samples.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Route.BrowseHistory.route)
                    },
                    onNavigateToSnippets = {
                        navController.navigate(Route.Snippets.route)
                    },
                    onNavigateToTerminal = { cwd ->
                        navController.navigate(Route.Terminal.createRoute(cwd))
                    },
                    layoutMode = layoutMode,
                    foldInfo = foldInfo,
                )
            }
        }

        // ---- Editor Home -----------------------------------------------
        // Removed: EditorHome tab has been deleted. Snippets entry migrated
        // to the FileBrowserScreen FAB menu.

        // ---- Editor ---------------------------------------------------
        composable(
            route = Route.Editor.route,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                },
            ),
            enterTransition = DraftPeekTransitions.sharedAxisHorizontalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisHorizontalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisHorizontalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisHorizontalPopExit,
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""

            EditorScreen(
                onNavigateUp = {
                    tabManager.closeAllTabs()
                    navController.navigateUp()
                },
                layoutMode = layoutMode,
                foldInfo = foldInfo,
                darkTheme = darkTheme,
                fileUri = uri.ifBlank { null },
                onNavigateToTerminal = { cwd ->
                    navController.navigate(Route.Terminal.createRoute(cwd))
                },
            )
        }

        // ---- Profile (Me) ---------------------------------------------
        composable(
            route = Route.Profile.route,
            enterTransition = { DraftPeekTransitions.topLevelEnter },
            exitTransition = { DraftPeekTransitions.topLevelExit },
            popEnterTransition = { DraftPeekTransitions.topLevelEnter },
            popExitTransition = { DraftPeekTransitions.topLevelExit },
        ) {
            ProfileScreen(
                onFileClick = { uri ->
                    val existingTab = tabManager.findTabByUri(uri)
                    if (existingTab != null) {
                        tabManager.setActiveTab(existingTab.id)
                        navController.navigate(Route.Editor.createRoute(uri)) {
                            popUpTo(Route.Profile.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Route.Editor.createRoute(uri))
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Route.BrowseHistory.route)
                },
                onNavigateToSnippets = {
                    navController.navigate(Route.Snippets.route)
                },
                onNavigateToAccessibility = {
                    navController.navigate(Route.Accessibility.route)
                },
                onNavigateToAchievements = {
                    navController.navigate(Route.Achievements.route)
                },
                onVerifyApp = { context ->
                    try {
                        val signatureResult = ApkIntegrityChecker.verify(context)
                        val dexResult = DexIntegrityChecker.verify(context)

                        when {
                            signatureResult is ApkIntegrityChecker.IntegrityResult.Verified &&
                            dexResult is DexIntegrityChecker.DexResult.Verified -> {
                                val fingerprint = DexIntegrityChecker.computeDexSha256(context)
                                VerifyAppState.Success(fingerprint ?: "计算中...")
                            }
                            signatureResult is ApkIntegrityChecker.IntegrityResult.Tampered -> {
                                VerifyAppState.Failed("签名证书不匹配")
                            }
                            dexResult is DexIntegrityChecker.DexResult.Tampered -> {
                                VerifyAppState.Failed("DEX 文件已被修改")
                            }
                            else -> {
                                VerifyAppState.Error("验证失败: $signatureResult")
                            }
                        }
                    } catch (e: Exception) {
                        VerifyAppState.Error(e.message ?: "未知错误")
                    }
                },
            )
        }

        // ---- Accessibility Settings -----------------------------------
        composable(
            route = Route.Accessibility.route,
            enterTransition = DraftPeekTransitions.sharedAxisVerticalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisVerticalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisVerticalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisVerticalPopExit,
        ) {
            AccessibilityScreen(
                onNavigateUp = { navController.navigateUp() },
            )
        }

        // ---- Snippets -------------------------------------------------
        composable(
            route = Route.Snippets.route,
            enterTransition = DraftPeekTransitions.sharedAxisVerticalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisVerticalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisVerticalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisVerticalPopExit,
        ) {
            SnippetScreen(navController = navController)
        }

        // ---- Samples --------------------------------------------------
        composable(
            route = Route.Samples.route,
            enterTransition = DraftPeekTransitions.sharedAxisVerticalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisVerticalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisVerticalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisVerticalPopExit,
        ) {
            SampleFilesScreen(
                onSampleClick = { uri ->
                    navController.navigate(Route.Editor.createRoute(uri))
                },
                onNavigateUp = { navController.navigateUp() },
            )
        }

        // ---- Browse History -------------------------------------------
        composable(
            route = Route.BrowseHistory.route,
            enterTransition = DraftPeekTransitions.sharedAxisVerticalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisVerticalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisVerticalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisVerticalPopExit,
        ) {
            BrowseHistoryScreen(
                onFileClick = { uri ->
                    val existingTab = tabManager.findTabByUri(uri)
                    if (existingTab != null) {
                        tabManager.setActiveTab(existingTab.id)
                        navController.navigate(Route.Editor.createRoute(uri)) {
                            popUpTo(Route.BrowseHistory.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(Route.Editor.createRoute(uri))
                    }
                },
                onBack = { navController.navigateUp() },
            )
        }

        // ---- Diff -----------------------------------------------------
        composable(
            route = Route.Diff.route,
            arguments = listOf(
                navArgument("leftUri") {
                    type = NavType.StringType
                },
                navArgument("rightUri") {
                    type = NavType.StringType
                },
            ),
            enterTransition = DraftPeekTransitions.sharedAxisHorizontalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisHorizontalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisHorizontalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisHorizontalPopExit,
        ) {
            DiffScreen(
                onNavigateUp = { navController.navigateUp() },
                layoutMode = layoutMode,
                foldInfo = foldInfo,
            )
        }

        // ---- Achievements ---------------------------------------------
        composable(
            route = Route.Achievements.route,
            enterTransition = DraftPeekTransitions.sharedAxisHorizontalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisHorizontalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisHorizontalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisHorizontalPopExit,
        ) {
            AchievementScreen(
                onNavigateUp = { navController.popBackStack() }
            )
        }

        // ---- Git Management ---------------------------------------------
        composable(
            route = Route.Git.route,
            arguments = listOf(navArgument("treeUri") { type = NavType.StringType }),
            enterTransition = DraftPeekTransitions.sharedAxisHorizontalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisHorizontalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisHorizontalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisHorizontalPopExit,
        ) { backStackEntry ->
            val treeUri = backStackEntry.arguments?.getString("treeUri")?.let {
                Uri.parse(Uri.decode(it))
            } ?: Uri.EMPTY
            GitScreen(
                treeUri = treeUri,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ---- Terminal ---------------------------------------------------
        composable(
            route = Route.Terminal.route,
            arguments = listOf(
                navArgument("cwd") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = DraftPeekTransitions.sharedAxisHorizontalEnter,
            exitTransition = DraftPeekTransitions.sharedAxisHorizontalExit,
            popEnterTransition = DraftPeekTransitions.sharedAxisHorizontalPopEnter,
            popExitTransition = DraftPeekTransitions.sharedAxisHorizontalPopExit,
        ) {
            Scaffold(
                topBar = {
                    BrandTopBar(
                        title = stringResource(R.string.app_title_terminal),
                        titleStyle = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        onBack = { navController.navigateUp() },
                    )
                },
                containerColor = PrototypeTokens.pageBackground,
            ) { innerPadding ->
                val cwd = it.arguments?.getString("cwd")
                TerminalScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    initialCwd = cwd,
                )
            }
        }
    }
}
