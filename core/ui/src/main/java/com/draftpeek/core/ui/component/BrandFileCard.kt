package com.draftpeek.core.ui.component

/** Design Tier: Molecule — Functional unit combining atoms */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.draftpeek.core.ui.icon.StrokeIcon
import com.draftpeek.core.ui.icon.StrokeIcons
import com.draftpeek.core.ui.modifier.pressScaleEffect
import com.draftpeek.core.ui.theme.BrandShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.FileMetaStyle
import com.draftpeek.core.ui.theme.MonoFileNameStyle
import com.draftpeek.core.ui.theme.MonoLabelStyle

/**
 * A branded file card composable for DraftPeek.
 *
 * Provides a consistent card layout across FileBrowserScreen, SampleFilesScreen,
 * and BrowseHistoryScreen, replacing duplicated inline card implementations.
 *
 * Layout structure:
 * ```
 * +------------------------------------------------------+
 * | [3dp indicator] [FileTypeIcon] fileName (bold)  [...] |
 * |                             metaText           [...] |
 * +------------------------------------------------------+
 * ```
 *
 * @param fileName Display name of the file.
 * @param extension File extension (e.g. "kt", "py") — drives icon and color indicator.
 * @param metaText Secondary information (file size, last modified date, etc.).
 * @param modifier Optional [Modifier] applied to the card.
 * @param onClick Called when the card is tapped.
 * @param onLongClick Optional long-press callback. When provided, uses
 *   [combinedClickable] instead of [clickable].
 * @param isPinned Whether this file is pinned (reserved for future pin indicator).
 * @param isSelected Whether this card is in a selected state (e.g. multi-select).
 *   Adds a subtle [primaryContainer] tint and an [outlineVariant] border.
 * @param leadingContent Optional composable rendered before the color indicator.
 *   Use for pinned indicators or checkboxes in multi-select mode.
 * @param trailingContent Optional composable rendered on the trailing side of the card.
 *   Use for action buttons, context menus, or overflow indicators.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrandFileCard(
    fileName: String,
    extension: String,
    metaText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    // Selected-state tint: subtle accent overlay
    val accent = PrototypeTokens.accent
    val selectedTint = remember(isSelected, accent) {
        if (isSelected) {
            Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.12f),
                    accent.copy(alpha = 0.04f),
                ),
            )
        } else {
            null
        }
    }

    // Border only when selected
    val border = if (isSelected) {
        BorderStroke(
            width = DraftPeekSpacing.BorderWidth,
            color = PrototypeTokens.border,
        )
    } else {
        null
    }

    val cardModifier = modifier
        .pressScaleEffect()
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            } else {
                Modifier.clickable(onClick = onClick)
            },
        )

    Card(
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(containerColor = PrototypeTokens.surface),
        border = border,
        modifier = cardModifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Optional selected-tint overlay
            if (selectedTint != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(BrandShapes.Card)
                        .background(selectedTint),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Optional leading content (pin indicator, checkbox, etc.)
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(DraftPeekSpacing.One))
                }

                // 3dp color indicator bar on the left edge
                FileTypeColorIndicator(
                    extension = extension,
                    modifier = Modifier.height(DraftPeekSpacing.FileTypeBadgeSize),
                )

                Spacer(modifier = Modifier.width(DraftPeekSpacing.One))

                // File type icon (28dp badge)
                FileTypeIcon(
                    extension = extension,
                    modifier = Modifier.size(DraftPeekSpacing.FileTypeBadgeSize),
                )

                Spacer(modifier = Modifier.width(DraftPeekSpacing.One))

                // File name and meta text column
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = fileName,
                        style = MonoFileNameStyle,
                        color = PrototypeTokens.fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(DraftPeekSpacing.Half))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Extension badge in JetBrains Mono
                        Text(
                            text = extension.uppercase(),
                            style = MonoLabelStyle,
                            color = PrototypeTokens.muted,
                        )

                        if (metaText.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(DraftPeekSpacing.Half))
                            Text(
                                text = metaText,
                                style = FileMetaStyle,
                                color = PrototypeTokens.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Trailing chevron indicator
                StrokeIcon(
                    icon = StrokeIcons.ChevronRight,
                    contentDescription = null,
                    tint = PrototypeTokens.mutedSoft.copy(alpha = 0.35f),
                    modifier = Modifier.size(14.dp),
                )

                // Optional trailing content (actions, overflow, etc.)
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(DraftPeekSpacing.Half))
                    trailingContent()
                }
            }
        }
    }
}

/**
 * A simplified branded card for directory/folder items.
 *
 * Uses the same card shape and spacing as [BrandFileCard] but replaces the
 * [FileTypeIcon] with a folder icon. Intended for directory rows in the
 * file browser.
 *
 * @param directoryName Display name of the directory.
 * @param modifier Optional [Modifier] applied to the card.
 * @param onClick Called when the directory card is tapped.
 * @param itemCount Optional number of items inside the directory.
 *   When provided, shown as "N items" in the meta text area.
 */
@Composable
fun BrandDirectoryCard(
    directoryName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    itemCount: Int? = null,
) {
    val folderTint = PrototypeTokens.folderContainer
    val folderIconTint = PrototypeTokens.folder

    Card(
        onClick = onClick,
        shape = BrandShapes.Card,
        colors = CardDefaults.cardColors(containerColor = PrototypeTokens.surface),
        modifier = modifier
            .pressScaleEffect()
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Folder icon
            Box(
                modifier = Modifier
                    .size(DraftPeekSpacing.FileTypeBadgeSize)
                    .clip(BrandShapes.Badge)
                    .background(folderTint),
                contentAlignment = Alignment.Center,
            ) {
                StrokeIcon(
                    icon = StrokeIcons.FolderFilled,
                    contentDescription = null,
                    tint = folderIconTint,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(DraftPeekSpacing.One))

            // Directory name and optional item count
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = directoryName,
                    style = MonoFileNameStyle,
                    color = PrototypeTokens.fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (itemCount != null) {
                    Spacer(modifier = Modifier.height(DraftPeekSpacing.Half))
                    Text(
                        text = "$itemCount item${if (itemCount != 1) "s" else ""}",
                        style = FileMetaStyle,
                        color = PrototypeTokens.muted,
                    )
                }
            }

            // Trailing chevron indicator
            StrokeIcon(
                icon = StrokeIcons.ChevronRight,
                contentDescription = null,
                tint = PrototypeTokens.mutedSoft.copy(alpha = 0.35f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
