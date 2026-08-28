/**
 * DraftPeek 自定义脚手架组件。
 *
 * 设计层级：有机体组件（Organism）— 组合多个分子组件形成的复杂 UI 区域。
 *
 * 替代 Material3 Scaffold 的自定义脚手架，提供自适应布局，不使用 Material 导航组件。
 * 根据 [LayoutMode] 自动选择三种布局模式：
 * - COMPACT（紧凑）：底部标签栏布局
 * - MEDIUM（中等）：侧边导航栏布局
 * - EXPANDED（展开）：侧边抽屉导航布局
 *
 * 支持可选的浮动操作按钮，正确处理系统栏（状态栏、导航栏）边距。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.R
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.layout.LayoutMode
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.H2Style
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.TabLabelStyle

/**
 * 替代 DraftPeekScaffold 的自定义脚手架。
 * 提供自适应布局，不使用任何 Material 导航组件。
 *
 * @param currentRoute 当前导航路由，用于标签高亮
 * @param onNavigate 点击导航标签时的回调
 * @param navItems 要显示的导航项列表
 * @param layoutMode 当前自适应布局模式
 * @param showNavigation 是否显示导航（标签栏/导航栏/抽屉）
 * @param fab 可选的浮动操作按钮 Composable，放置在标签栏上方
 * @param modifier 可选 Modifier
 * @param content 主内容区域
 */
@Composable
fun CustomScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    navItems: List<TabBarItem>,
    layoutMode: LayoutMode = LayoutMode.COMPACT,
    showNavigation: Boolean = true,
    fab: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    when (layoutMode) {
        LayoutMode.COMPACT -> CompactCustomScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            navItems = navItems,
            showNavigation = showNavigation,
            fab = fab,
            modifier = modifier,
            content = content
        )
        LayoutMode.MEDIUM -> MediumCustomScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            navItems = navItems,
            showNavigation = showNavigation,
            modifier = modifier,
            content = content
        )
        LayoutMode.EXPANDED -> ExpandedCustomScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            navItems = navItems,
            showNavigation = showNavigation,
            modifier = modifier,
            content = content
        )
    }
}

// ---- COMPACT: Bottom Tab Bar ----

@Composable
private fun CompactCustomScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    navItems: List<TabBarItem>,
    showNavigation: Boolean,
    fab: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val statusBarInsets = WindowInsets.statusBars
    val navBarInsets = WindowInsets.navigationBars
    val topPadding = with(density) { statusBarInsets.getTop(this).toDp() }
    val navBarBottom = with(density) { navBarInsets.getBottom(this).toDp() }

    // Tab bar visual height = 1dp divider + 56dp row + 8dp bottom padding.
    // CustomTabBar applies navigationBarsPadding() internally for the system nav area.
    // Content bottom padding = tab bar height (65dp) so content doesn't go under it.
    // When navigation is hidden, still pad for the system navigation bar.
    val tabBarVisualHeight = 1.dp + 56.dp + 8.dp // divider + bar content + bottom padding
    val bottomContentPadding = if (showNavigation) {
        tabBarVisualHeight + navBarBottom
    } else {
        navBarBottom
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        // Content
        content(
            PaddingValues(
                top = topPadding,
                bottom = bottomContentPadding
            )
        )

        // FAB — placed above the tab bar with adequate clearance (~80dp from bottom)
        if (showNavigation) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = PrototypeSpacing.FABRight,
                        bottom = tabBarVisualHeight + navBarBottom + PrototypeSpacing.FABBottom
                    )
            ) {
                fab()
            }
        }

        // Tab bar at bottom
        if (showNavigation) {
            val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            CustomTabBar(
                items = navItems,
                selectedIndex = selectedIndex,
                onTabClick = { index ->
                    if (index in navItems.indices) {
                        onNavigate(navItems[index].route)
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---- MEDIUM: Side Rail ----

@Composable
private fun MediumCustomScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    navItems: List<TabBarItem>,
    showNavigation: Boolean,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val railWidth = 72.dp
    val surface = PrototypeTokens.surface
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted
    val borderColor = PrototypeTokens.border
    val density = LocalDensity.current
    val statusBarInsets = WindowInsets.statusBars
    val navBarInsets = WindowInsets.navigationBars
    val topPadding = with(density) { statusBarInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { navBarInsets.getBottom(this).toDp() }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        if (showNavigation) {
            // Custom Navigation Rail
            Column(
                modifier = Modifier
                    .width(railWidth)
                    .fillMaxHeight()
                    .background(surface),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    Column(
                        modifier = Modifier
                            .width(railWidth)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StrokeIcon(
                            icon = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) accent else muted,
                            modifier = Modifier.size(PrototypeSpacing.TabBarIconSize)
                        )
                        Text(
                            text = item.label,
                            style = TabLabelStyle.copy(
                                color = if (isSelected) accent else muted,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider(
                thickness = 1.dp,
                color = borderColor
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            content(
                PaddingValues(
                    top = topPadding,
                    bottom = bottomPadding,
                    start = if (showNavigation) 0.dp else 0.dp
                )
            )
        }
    }
}

// ---- EXPANDED: Side Drawer ----

@Composable
private fun ExpandedCustomScaffold(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    navItems: List<TabBarItem>,
    showNavigation: Boolean,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerWidth = 240.dp
    val surface = PrototypeTokens.surface
    val accent = PrototypeTokens.accent
    val accentSoft = PrototypeTokens.accentSoft
    val muted = PrototypeTokens.muted
    val fg = PrototypeTokens.fg
    val borderColor = PrototypeTokens.border
    val density = LocalDensity.current
    val statusBarInsets = WindowInsets.statusBars
    val navBarInsets = WindowInsets.navigationBars
    val topPadding = with(density) { statusBarInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { navBarInsets.getBottom(this).toDp() }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(PrototypeTokens.pageBackground)
    ) {
        if (showNavigation) {
            // Custom Navigation Drawer
            Column(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .background(surface)
            ) {
                // Brand title
                Text(
                    text = stringResource(R.string.core_ui_brand_title),
                    style = H2Style.copy(color = accent),
                    modifier = Modifier.padding(
                        top = 32.dp,
                        start = 32.dp,
                        end = 32.dp,
                        bottom = 16.dp
                    )
                )

                // Nav items
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PrototypeShapes.Small)
                            .then(
                                if (isSelected) {
                                    Modifier.background(accentSoft)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            )
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            // Left accent indicator bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .clip(PrototypeShapes.Pill)
                                    .background(accent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Spacer(modifier = Modifier.width(15.dp))
                        }
                        StrokeIcon(
                            icon = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) accent else muted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.label,
                            style = DraftPeekTypography.bodyMedium.copy(
                                color = if (isSelected) accent else fg,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider(
                thickness = 1.dp,
                color = borderColor
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            content(
                PaddingValues(
                    top = topPadding,
                    bottom = bottomPadding
                )
            )
        }
    }
}
