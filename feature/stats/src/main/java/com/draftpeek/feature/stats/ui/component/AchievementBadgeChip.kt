/**
 * 文件: AchievementBadgeChip.kt
 * 功能: 统计模块UI组件 - 成就徽章标签
 * 描述: 用于在个人资料页展示已解锁成就的小型标签组件，包含成就图标和名称，点击可跳转到成就页面。
 * 创建: 2024
 */
package com.draftpeek.feature.stats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * 成就徽章标签组件。
 *
 * 显示一个带图标的小标签，用于在个人资料页横向滚动区域展示最近解锁的成就。
 * 点击时触发回调，带有按压缩放动画效果。
 *
 * @param icon 成就图标
 * @param name 成就显示名称
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun AchievementBadgeChip(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .pressScaleEffect()
            .background(
                color = PrototypeTokens.accentSoft,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrototypeTokens.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PrototypeTokens.accent,
        )
    }
}
