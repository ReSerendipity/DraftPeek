/**
 * DraftPeek 品牌开关组件。
 *
 * 设计层级：原子组件（Atom）— 最小可复用 UI 基元。
 *
 * 自定义实现的开关组件，替代 Material3 Switch。
 * 使用品牌 accent 色作为激活状态轨道颜色，白色圆形滑块，带弹簧动画效果，
 * 999dp 圆角（胶囊形）轨道。
 * **禁止直接使用 Material3 Switch，必须使用本组件。**
 */
package com.draftpeek.core.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * DraftPeek 品牌开关组件。
 *
 * 自定义实现的开关，品牌 accent 色轨道，白色圆形滑块，弹簧动画，胶囊形圆角。
 * **所有场景应优先使用此组件而非直接使用 Material3 Switch。**
 *
 * @param checked 开关是否选中
 * @param onCheckedChange 开关状态切换时的回调
 * @param modifier 应用于组件的 Modifier
 */
@Composable
fun BrandSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val trackColor = if (checked) PrototypeTokens.accent else PrototypeTokens.border
    val borderColor = if (checked) PrototypeTokens.accent.copy(alpha = 0.3f) else Color.Transparent
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (PrototypeSpacing.SwitchWidth - PrototypeSpacing.SwitchHeight + 2.dp) else 2.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "switchThumb"
    )

    Box(
        modifier = modifier
            .size(width = PrototypeSpacing.SwitchWidth, height = PrototypeSpacing.SwitchHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 2.dp)
                .size(PrototypeSpacing.SwitchThumbSize)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
