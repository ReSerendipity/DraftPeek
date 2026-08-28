package com.draftpeek.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIconDef
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * A single entry in the [BrandFAB]'s expandable speed-dial menu.
 *
 * @param icon    Icon shown on the mini circular button next to the label.
 * @param label   Text label displayed in the bubble next to the icon button.
 * @param onClick Called when the user taps the item; the FAB automatically collapses.
 */
data class FABMenuItem(val icon: StrokeIconDef, val label: String, val onClick: () -> Unit)

/**
 * A branded Floating Action Button with an expandable speed-dial menu.
 *
 * In collapsed state shows a single 56x56 accent circle with a "+" icon.
 * When tapped, it rotates 45 degrees (turning into a close icon) and reveals
 * [items] as label+icon pairs stacked above the FAB, each with a staggered
 * fade+scale animation matching the HTML prototype.
 *
 * The caller is responsible for anchoring this FAB in its layout (usually with
 * `Modifier.align(Alignment.BottomEnd).padding(16.dp)` inside a Box).
 *
 * @param items     Menu items to show when the FAB is expanded. The first item
 *                  appears at the top (farthest from the FAB), the last item is
 *                  closest to the FAB.
 * @param onDismiss Optional callback invoked when the FAB collapses (e.g. after
 *                  selecting a menu item).
 */
@Composable
fun BrandFAB(items: List<FABMenuItem>, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "fabRotation"
    )

    fun collapse() {
        expanded = false
        onDismiss?.invoke()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Menu items: reversed so first-defined item is at the top (farthest from FAB),
        // which matches the HTML prototype stacking order.
        items.reversed().forEachIndexed { idx, item ->
            // Stagger the entrance so items closest to the FAB appear first.
            val delayMs = (items.size - 1 - idx) * 40
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(durationMillis = 150, delayMillis = delayMs)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 200, delayMillis = delayMs),
                        initialScale = 0.98f
                    ),
                exit = fadeOut(tween(durationMillis = 100))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Label bubble (surface-colored pill with border)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrototypeTokens.surface.copy(alpha = 0.96f))
                            .border(
                                width = 1.dp,
                                color = PrototypeTokens.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = PrototypeTokens.fg
                        )
                    }
                    // Mini icon button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(BrandShapes.IconButton)
                            .background(PrototypeTokens.surface.copy(alpha = 0.96f))
                            .border(
                                width = 1.dp,
                                color = PrototypeTokens.border,
                                shape = BrandShapes.IconButton
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    collapse()
                                    item.onClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StrokeIcon(
                            icon = item.icon,
                            contentDescription = item.label,
                            tint = PrototypeTokens.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Main FAB button
        Box(
            modifier = Modifier
                .size(PrototypeSpacing.FABSize)
                .shadow(
                    elevation = 6.dp,
                    shape = BrandShapes.FAB,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(BrandShapes.FAB)
                .background(PrototypeTokens.accent)
                .pressScaleEffect()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Close menu" else "Add",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
        }
    }
}
