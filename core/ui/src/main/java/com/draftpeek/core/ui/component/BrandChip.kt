package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.ChipTextStyle
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * A selectable chip with solid accent fill when selected, outlined surface style otherwise.
 * Uses [BrandShapes.Chip] (10dp) corners and a 32dp height.
 *
 * @param text The label to display inside the chip.
 * @param selected Whether this chip is currently selected.
 * @param onClick Called when the chip is tapped.
 */
@Composable
fun BrandChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (selected) PrototypeTokens.accent else PrototypeTokens.surface
    val textColor = if (selected) Color.White else PrototypeTokens.fgSoft
    val borderColor = if (selected) PrototypeTokens.accent else PrototypeTokens.border

    Box(
        modifier = modifier
            .height(PrototypeSpacing.ChipHeight)
            .clip(BrandShapes.Chip)
            .background(bgColor)
            .border(1.dp, borderColor, BrandShapes.Chip)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = PrototypeSpacing.ChipPaddingH),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ChipTextStyle,
            color = textColor
        )
    }
}

/**
 * A softer filter-style chip with semi-transparent accent background when selected.
 * Uses a pill (999dp) shape and smaller 11sp text for dense filter rows.
 *
 * @param text The label to display inside the chip.
 * @param selected Whether this chip is currently selected.
 * @param onClick Called when the chip is tapped.
 */
@Composable
fun BrandFilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bgColor = if (selected) PrototypeTokens.accentSoft else PrototypeTokens.surface
    val textColor = if (selected) PrototypeTokens.accent else PrototypeTokens.muted
    val borderColor = if (selected) PrototypeTokens.accent else PrototypeTokens.border

    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ChipTextStyle.copy(fontSize = 11.sp),
            color = textColor,
            fontFamily = ChipTextStyle.fontFamily
        )
    }
}
