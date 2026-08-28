/**
 * 自适应列表-详情布局组件。
 *
 * 提供两种列表-详情布局实现，适配不同屏幕尺寸：
 * - [AdaptiveListDetailScaffold]：使用 Material3 SupportingPaneScaffold 的官方自适应实现
 * - [AdaptiveListDetailLayout]：简化版实现，在 Material3 自适应 Scaffold 不可用时回退到 Crossfade
 *
 * 布局行为：
 * - 紧凑屏幕：单窗格显示（列表或详情）
 * - 中等屏幕：列表为主窗格，详情为支持窗格
 * - 展开屏幕：列表和详情并排显示
 */
package com.draftpeek.core.ui.layout

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * 使用 Material3 SupportingPaneScaffold 的自适应列表-详情布局。
 *
 * 在紧凑屏幕上：显示列表或详情窗格（单窗格）
 * 在中等屏幕上：显示列表，详情作为支持窗格
 * 在展开屏幕上：列表和详情并排显示
 *
 * 布局自动适应窗口尺寸类和折叠状态。
 * 当选中详情项时，支持（详情）窗格滑入。
 * 返回导航在紧凑屏幕上关闭详情窗格。
 *
 * @param listContent 列表窗格内容（展开时始终可见，紧凑时为主窗格）
 * @param detailContent 详情窗格内容（支持窗格，选中项时显示）
 * @param selectedItemKey 当前选中项的键，未选中时为 null
 * @param modifier 应用于 Scaffold 的 Modifier
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveListDetailScaffold(
    listContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    selectedItemKey: Any?,
    modifier: Modifier = Modifier
) {
    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator()

    LaunchedEffect(selectedItemKey) {
        if (selectedItemKey != null &&
            scaffoldNavigator.currentDestination?.pane != SupportingPaneScaffoldRole.Supporting
        ) {
            scaffoldNavigator.navigateTo(
                SupportingPaneScaffoldRole.Supporting,
                selectedItemKey
            )
        }
    }

    SupportingPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        modifier = modifier,
        mainPane = {
            AnimatedPane {
                listContent()
            }
        },
        supportingPane = {
            AnimatedPane {
                if (selectedItemKey != null) {
                    detailContent()
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    )
}

/**
 * 简化版自适应布局，在 Material3 自适应 Scaffold 不可用时回退到 Crossfade
 * （例如在非常旧的 API 级别或更简单的用例中）。
 *
 * 在展开屏幕上：列表和详情并排显示。
 * 在紧凑/中等屏幕上：列表和详情之间交叉淡入淡出。
 *
 * @param layoutMode 当前布局模式（来自 WindowSizeClass）
 * @param selectedItemKey 当前选中项的键，未选中时为 null
 * @param listContent 列表窗格内容
 * @param detailContent 详情窗格内容
 * @param modifier 应用于布局的 Modifier
 * @param splitRatio 展开布局的初始分割比例（0.0-1.0），默认为 0.35
 */
@Composable
fun AdaptiveListDetailLayout(
    layoutMode: LayoutMode,
    selectedItemKey: Any?,
    listContent: @Composable () -> Unit,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    splitRatio: Float = 0.35f
) {
    when (layoutMode) {
        LayoutMode.EXPANDED -> {
            SplitScreenLayout(
                startContent = { listContent() },
                endContent = {
                    if (selectedItemKey != null) {
                        detailContent()
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                },
                initialSplitRatio = splitRatio,
                modifier = modifier
            )
        }
        LayoutMode.MEDIUM,
        LayoutMode.COMPACT -> {
            Crossfade(
                targetState = selectedItemKey != null,
                modifier = modifier,
                label = "list-detail-crossfade"
            ) { showDetail ->
                if (showDetail && selectedItemKey != null) {
                    detailContent()
                } else {
                    listContent()
                }
            }
        }
    }
}
