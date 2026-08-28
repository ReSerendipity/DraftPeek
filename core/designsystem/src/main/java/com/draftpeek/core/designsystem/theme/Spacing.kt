/**
 * DraftPeek 间距与尺寸系统定义文件。
 *
 * 定义基于8dp网格系统的间距常量、组件尺寸、圆角和海拔值。
 * 使用这些常量代替硬编码的dp值以确保设计一致性。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DraftPeek设计系统 - 间距与尺寸变量
 * 所有值遵循8dp网格系统（8的倍数）。
 * 使用这些常量代替硬编码的dp值以确保一致性。
 */
object DraftPeekSpacing {
    // 8dp网格 - 基本单位
    val Unit = 8.dp // 1网格单位

    // 间距刻度
    val Half = 4.dp // 0.5网格 - 紧凑内边距
    val One = 8.dp // 1网格 - 标准内边距
    val OneHalf = 12.dp // 1.5网格 - 列表项垂直内边距
    val Two = 16.dp // 2网格 - 标准外边距
    val TwoHalf = 20.dp // 2.5网格 - 分区间距
    val Three = 24.dp // 3网格 - 大分区内边距
    val Four = 32.dp // 4网格 - 平板屏幕边缘
    val Five = 40.dp // 5网格 - 折叠屏避障区域
    val Six = 48.dp // 6网格 - 最小触摸目标尺寸
    val Eight = 64.dp // 8网格 - 超大间距

    // 对话框专用间距
    val DialogPadding = 24.dp

    // 卡片项内间距
    val CardItemVerticalPadding = 12.dp

    // 列表分区间距
    val ListSectionGap = 16.dp

    // 组件专用间距
    val CardPadding = 16.dp
    val CardCornerRadius = 8.dp
    val ListItemImageSize = 40.dp
    val ListItemVerticalPadding = 8.dp
    val ListItemHorizontalPadding = 16.dp
    val SectionVerticalGap = 24.dp
    val ButtonHeight = 48.dp
    val SmallButtonHeight = 40.dp
    val InputFieldHeight = 56.dp
    val BadgeSize = 32.dp
    val FileTypeBadgeSize = 28.dp
    val DividerThickness = 1.dp
    val BorderWidth = 1.dp

    // 圆角刻度
    object Corner {
        val Small = 4.dp
        val Medium = 8.dp
        val Large = 16.dp
        val XLarge = 24.dp
    }

    // 海拔阴影刻度
    object Elevation {
        val None = 0.dp
        val Low = 1.dp
        val Medium = 3.dp
        val High = 6.dp
    }
}

/**
 * UI重设计中原型专用间距值。
 * 精确匹配HTML原型中的像素值。
 */
object PrototypeSpacing {
    // Screen layout
    val ScreenHorizontal = 20.dp // .pad { padding-inline: 20px }
    val TabBarPadding = 6.dp // tabbar padding-top
    val TabBarHeight = 56.dp // tabbar total height (icons + labels)
    val TabBarIconSize = 24.dp // tab icon size (upgraded from 22dp)
    val TabBarFontSize = 10.sp // tab label font size
    val HomeIndicatorHeight = 28.dp // home indicator bar area

    // Quick action pills
    val QuickActionHeight = 36.dp // quick action pill button height

    // Search & header
    val SearchBarHeight = 40.dp // search bar height
    val SearchBarPaddingH = 14.dp // search bar horizontal padding
    val HistoryButtonSize = 40.dp // history/recent button (screen 01)
    val HistoryButtonSizeB = 36.dp // history/recent button (screen 01-B)
    val BadgeMinWidth = 15.dp // notification badge min width
    val BadgeHeight = 15.dp // notification badge height
    val PillFontSize = 9.sp // pill badge font size
    val PillPaddingH = 10.dp // pill horizontal padding
    val PillPaddingV = 3.dp // pill vertical padding

    // FAB
    val FABSize = 56.dp // FAB size 56x56
    val FABRadius = 16.dp // FAB corner radius
    val FABBottom = 16.dp // FAB bottom offset
    val FABRight = 16.dp // FAB right offset

    // Editor
    val EditorTopBarHeight = 44.dp // topbar min-height
    val EditorTopBarPadding = 16.dp // topbar horizontal padding
    val EditorIconBtnSize = 32.dp // back/menu button size
    val EditorTabPaddingH = 14.dp // editor-tab horizontal padding
    val EditorTabFontSize = 12.sp // editor-tab font size
    val EditorTabMiniBadge = 16.dp // mini file type badge in tabs
    val EditorTabCloseBtn = 14.dp // close button in tabs
    val EditorStatusBarHeight = 28.dp // editor-status bar area
    val EditorStatusBarPadding = 16.dp
    val EditorModifiedDot = 6.dp // modified indicator dot size
    val CodeFontSize = 13.sp // code area font size
    val CodeLineHeight = 1.65f // code area line height multiplier
    val LineNumWidth = 36.dp // line number column width
    val LineNumFontSize = 11.sp // line number font size
    val HighlightLineAlpha = 0.06f // highlight line background alpha

    // Markdown
    val MarkdownToolbarBtnSize = 32.dp // md-btn width
    val MarkdownToolbarBtnH = 28.dp // md-btn height
    val ModeSwitchRadius = 8.dp // mode-switch corner radius
    val ModeSwitchPadding = 2.dp // mode-switch inner padding

    // Bottom sheet
    val BottomSheetTopRadius = 20.dp // sheet container top radius
    val BottomSheetPadding = 20.dp // sheet horizontal padding
    val SheetHandleWidth = 36.dp // sheet handle width
    val SheetHandleHeight = 4.dp // sheet handle height
    val SheetHandleMarginBottom = 16.dp

    // Create file dialog
    val LanguageGridColumns = 4 // lang-grid columns
    val LangItemPadding = 10.dp // lang-item vertical padding
    val LangDotSize = 24.dp // language color dot size
    val LangDotRadius = 6.dp // language dot corner radius
    val LangDotFontSize = 8.sp // language dot text size
    val LangItemFontSize = 10.sp // language item label size

    // Continue editing card
    val HeroCodeMaxHeight = 88.dp // code preview max height
    val ContinueEditingBtnHeight = 36.dp // continue editing button min height

    // Input fields
    val InputFieldHeight = 56.dp // input field height

    // File card
    val FileCardPadding = 12.dp // file-card vertical padding
    val FileTypeBadgeSize = 28.dp // file-type-badge size (standard)
    val FileTypeBadgeSmall = 24.dp // file-type-badge (hero card)
    val FileTypeBadgeMini = 20.dp // file-type-badge (tree)
    val FileTypeBadgeMicro = 16.dp // file-type-badge (editor tab)
    val FileColorBarWidth = 3.dp // file-color-bar width
    val FileColorBarHeight = 24.dp // file-color-bar height

    // Directory
    val DirIconSize = 20.dp // directory icon size
    val DirItemPadding = 8.dp // dir-item vertical padding

    // Settings
    val SettingsRowPadding = 14.dp // settings-row vertical padding
    val SettingsCardPadding = 4.dp // settings card inner horizontal padding multiplier
    val SettingsCardPaddingH = 16.dp // settings card horizontal padding

    // Stats
    val StatValueFontSize = 24.sp // stat-value font size
    val StatLabelFontSize = 11.sp // stat-label font size
    val StatCardPadding = 14.dp // stat-card padding

    // Heatmap
    val HeatmapGap = 3.dp // heatmap cell gap
    val HeatmapCellCornerRadius = 2.dp // heatmap cell radius
    val HeatmapLegendSize = 10.dp // legend color box size

    // External file badge
    val ExtBadgePaddingH = 8.dp // EXT badge horizontal padding
    val ExtBadgePaddingV = 2.dp // EXT badge vertical padding
    val ExtBadgeRadius = 4.dp // EXT badge corner radius
    val ExtBadgeFontSize = 9.sp // EXT badge font size

    // Guide card
    val GuideIconSize = 48.dp // guide-card icon area size
    val GuideIconRadius = 14.dp // guide-card icon corner radius
    val GuideTitleFontSize = 15.sp // guide-card title font size
    val GuideDescFontSize = 12.sp // guide-card description font size

    // Input
    val InputFieldPaddingH = 14.dp // input-field horizontal padding
    val InputFieldPaddingV = 12.dp // input-field vertical padding
    val InputLabelFontSize = 12.sp // input-label font size
    val InputHintFontSize = 11.sp // input hint font size
    val TopBarHeight = 52.dp
    val TopBarBackSize = 36.dp
    val IconButtonSize = 40.dp
    val SettingIconSize = 32.dp
    val SwitchWidth = 50.dp
    val SwitchHeight = 30.dp
    val SwitchThumbSize = 26.dp
    val AvatarSize = 64.dp
    val TabIndicatorW = 56.dp
    val TabIndicatorH = 32.dp
    val ChipHeight = 32.dp
    val ChipPaddingH = 14.dp
}
