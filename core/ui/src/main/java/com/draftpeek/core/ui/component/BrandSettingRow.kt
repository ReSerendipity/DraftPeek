package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors
import com.draftpeek.core.ui.theme.SettingNameStyle
import com.draftpeek.core.ui.theme.SettingValueStyle

/**
 * A branded settings list row with a leading icon in a rounded tile,
 * label text, optional value/chevron, and an optional trailing slot.
 *
 * Matches the HTML prototype's `.settings-row` component: 52dp height,
 * 20dp horizontal padding, 32dp setting icon tile, 12dp gap, 1dp soft divider.
 */
@Composable
fun BrandSettingRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = PrototypeTokens.accentSoft,
    iconTint: Color = PrototypeTokens.accent,
    value: String? = null,
    isDanger: Boolean = false,
    showDivider: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    iconPainter: Painter? = null
) {
    val labelColor = if (isDanger) SemanticColors.Danger else PrototypeTokens.fg
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon container (32x32 rounded tile)
            Box(
                modifier = Modifier
                    .size(PrototypeSpacing.SettingIconSize)
                    .clip(BrandShapes.SettingIcon)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Label
            Text(
                text = label,
                style = SettingNameStyle,
                color = labelColor,
                modifier = Modifier.weight(1f)
            )
            // Trailing slot or value + chevron
            if (trailing != null) {
                trailing()
            } else {
                if (value != null) {
                    Text(
                        text = value,
                        style = SettingValueStyle,
                        color = PrototypeTokens.muted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = PrototypeTokens.mutedSoft,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = PrototypeTokens.borderSoft,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/**
 * A [BrandSettingRow] specialized for boolean toggles, with a [BrandSwitch]
 * as the trailing slot instead of a chevron. Tapping the row toggles the switch.
 */
@Composable
fun BrandSwitchSettingRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = PrototypeTokens.accentSoft,
    iconTint: Color = PrototypeTokens.accent,
    showDivider: Boolean = true
) {
    BrandSettingRow(
        icon = icon,
        label = label,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        iconBackgroundColor = iconBackgroundColor,
        iconTint = iconTint,
        showDivider = showDivider,
        trailing = {
            BrandSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
