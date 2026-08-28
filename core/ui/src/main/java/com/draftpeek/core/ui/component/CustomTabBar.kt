/**
 * DraftPeek 自定义底部标签栏组件。
 *
 * 完全自定义的底部导航栏，匹配 HTML 原型设计。
 *
 * 设计特性（来自原型）：
 * - 24dp 图标，10sp 标签
 * - 选中项使用 accent 色高亮 + SemiBold 字重
 * - 细微的顶部分割线
 * - 底部 8dp 内边距 + 系统导航栏适配
 *
 * 包含 [TabBarItem] 数据类定义标签项，以及 [CustomTabBar] 和 [CustomTabBarItem] Composable。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.TabLabelStyle

/**
 * 描述自定义底部导航栏中的单个标签项。
 *
 * @property label 标签显示文本
 * @property icon 标签图标
 * @property route 标签对应的导航路由
 */
data class TabBarItem(val label: String, val icon: StrokeIconDef, val route: String)

/**
 * 完全自定义的底部标签栏，匹配 HTML 原型设计。
 *
 * 设计说明（来自原型）：
 * - 24dp 图标，10sp 标签
 * - 选中图标后方 56x28dp 胶囊指示器（药丸），使用 accentSoft 填充
 * - 细微顶部分割线
 * - API 31+ 上的模糊表面背景实现玻璃效果
 *
 * @param items 要显示的标签列表（通常为 3 个）
 * @param selectedIndex 当前活动标签的索引
 * @param onTabClick 点击标签时的回调
 * @param modifier 可选 Modifier
 */
@Composable
fun CustomTabBar(
    items: List<TabBarItem>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = PrototypeTokens.surface
    val borderColor = PrototypeTokens.border

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = borderColor
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                CustomTabBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onTabClick(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 单个标签栏项 Composable。
 *
 * HTML 原型设计：选中项仅通过 accent 颜色和加粗字体表示，无药丸指示器。
 * - 24dp 图标
 * - 10sp 标签，选中时 font-weight 650，未选中 500
 * - 选中: accent 色，未选中: muted 色
 *
 * @param item 标签项数据
 * @param isSelected 是否选中
 * @param onClick 点击回调
 * @param modifier 可选 Modifier
 */
@Composable
private fun CustomTabBarItem(
    item: TabBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = PrototypeTokens.accent
    val muted = PrototypeTokens.muted

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StrokeIcon(
            icon = item.icon,
            contentDescription = item.label,
            tint = if (isSelected) accent else muted,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = item.label,
            style = TabLabelStyle.copy(
                color = if (isSelected) accent else muted,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}
