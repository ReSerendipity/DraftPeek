package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SubPageTopBarTitleStyle
import com.draftpeek.core.ui.theme.TopBarTitleStyle

/**
 * Top app bar for root-level screens (optional back button).
 * Displays a bold title with optional action icons on the right.
 * Pass [onBack] to show a back arrow (e.g. when navigating subdirectories).
 */
@Composable
fun BrandTopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = TopBarTitleStyle,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PrototypeSpacing.TopBarHeight)
                .background(PrototypeTokens.surface)
                .padding(horizontal = if (onBack != null) 12.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(PrototypeSpacing.TopBarBackSize)
                        .pressScaleEffect()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    StrokeIcon(
                        icon = StrokeIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = PrototypeTokens.fgSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = title,
                style = titleStyle,
                color = PrototypeTokens.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (onBack != null) 4.dp else 0.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = PrototypeTokens.border,
        )
    }
}


