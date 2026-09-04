package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.modifier.minimumTouchTarget
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.MonoLabelStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * A square icon button with rounded corners and an optional notification badge.
 * Uses [BrandShapes.IconButton] (10dp) corners and the press-scale effect for feedback.
 *
 * @param icon The icon [ImageVector] to display.
 * @param onClick Called when the button is tapped.
 * @param contentDescription Accessibility description for the icon.
 * @param tint Icon tint color. Defaults to [PrototypeTokens.fgSoft].
 * @param badge Optional notification count (shows a red dot with number when > 0).
 * @param iconSize Size of the icon inside the button. Defaults to 20dp.
 */
@Composable
fun BrandIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = PrototypeTokens.fgSoft,
    badge: Int? = null,
    iconSize: Dp = 20.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        // clickable 置于最外层使触摸区覆盖 48dp；minimumTouchTarget 只外扩触摸区，
        // 不改变 40dp 的图标视觉尺寸（图标按钮在工具栏中密度较高）。
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .minimumTouchTarget()
            .size(PrototypeSpacing.IconButtonSize)
            .clip(BrandShapes.IconButton)
            .pressScaleEffect(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        if (badge != null && badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(PrototypeTokens.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    style = MonoLabelStyle.copy(fontSize = 9.sp)
                )
            }
        }
    }
}
