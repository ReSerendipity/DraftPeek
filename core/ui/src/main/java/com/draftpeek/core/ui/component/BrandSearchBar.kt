package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SearchBarHintStyle

/**
 * A branded search bar with a leading search icon, placeholder text, and
 * focus-aware border color (accent when focused, default border otherwise).
 *
 * @param value The current search query text.
 * @param onValueChange Called when the user edits the search query.
 * @param placeholder Placeholder hint text shown when [value] is empty.
 */
@Composable
fun BrandSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = PrototypeTokens.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PrototypeSpacing.SearchBarHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, PrototypeTokens.border, RoundedCornerShape(10.dp))
            .padding(horizontal = PrototypeSpacing.SearchBarPaddingH),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StrokeIcon(
            icon = StrokeIcons.Search,
            contentDescription = null,
            tint = PrototypeTokens.muted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = SearchBarHintStyle,
                    color = PrototypeTokens.mutedSoft
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = PrototypeTokens.fg,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(PrototypeTokens.accent),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
