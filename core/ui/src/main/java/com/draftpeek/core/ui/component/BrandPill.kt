package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SemanticColors

/**
 * A small status pill with an optional colored leading dot and monospace text.
 * Uses [BrandShapes.Pill] (50% / 999dp) shape with elevated background and a subtle border.
 *
 * @param text The label text to display (e.g. "Modified", "12 files").
 * @param dotColor Optional color for the leading status dot. Defaults to [SemanticColors.Success].
 *                 Pass `null` to hide the dot entirely.
 * @param textColor Color for the label text. Defaults to [PrototypeTokens.fgSoft].
 */
@Composable
fun BrandPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = SemanticColors.Success,
    textColor: Color = PrototypeTokens.fgSoft,
) {
    Row(
        modifier = modifier
            .clip(BrandShapes.Pill)
            .background(PrototypeTokens.elevated)
            .border(1.dp, PrototypeTokens.border, BrandShapes.Pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            lineHeight = 12.sp,
        )
    }
}
