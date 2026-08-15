/**
 * DraftPeek 形状系统定义文件。
 *
 * 定义应用的圆角、形状配置，包括Material 3 Shapes和品牌自定义形状。
 * 设计理念：内角大于外角以营造层次感，小元素使用较大圆角（亲切友好），
 * 大元素使用较小圆角（稳重专业），打造温暖亲切的视觉风格。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DraftPeek Material 3 形状方案 - "柔和圆角"
 *
 * 设计理念：
 * - 嵌套元素内角 > 外角，营造层次感
 * - 小元素圆角更大（亲切友好）
 * - 中等元素圆角适中（专业感）
 * - 大元素圆角更小（稳重扎实）
 *
 * 这种设计创造了温暖、亲切的美学，使DraftPeek区别于典型的"冰冷"开发工具。
 */
val Shapes = Shapes(
    // 小元素：8dp圆角
    extraSmall = RoundedCornerShape(8.dp),       // 图标、标签
    small = RoundedCornerShape(10.dp),           // 按钮、小卡片

    // 中等元素：14dp卡片圆角（原型 --radius-card）
    medium = RoundedCornerShape(14.dp),          // 列表项、卡片、对话框

    // 大元素：16dp圆角
    large = RoundedCornerShape(DraftPeekSpacing.Corner.Large),        // 底部表单、模态框
    extraLarge = RoundedCornerShape(20.dp),      // 页面级容器、底部表单顶部
)

/**
 * 非Material组件的品牌专用形状。
 * 用于需要统一样式的自定义UI元素。
 */
object BrandShapes {
    // 徽章形状（文件类型图标、标签）
    val Badge = RoundedCornerShape(6.dp)
    val BadgeSmall = RoundedCornerShape(6.dp)

    // 卡片形状
    val Card = RoundedCornerShape(12.dp)
    val CardElevated = RoundedCornerShape(DraftPeekSpacing.Corner.Large)

    // 菜单形状
    val Menu = RoundedCornerShape(18.dp)
    val MenuLarge = RoundedCornerShape(DraftPeekSpacing.Corner.Large)

    // 状态指示器形状
    val StatusPill = CircleShape
    val StatusCircle = CircleShape
    val Dialog = RoundedCornerShape(18.dp)
    val Chip = RoundedCornerShape(10.dp)
    val IconButton = RoundedCornerShape(10.dp)
    val SettingIcon = RoundedCornerShape(9.dp)
    val Pill = CircleShape
    val TopBarBack = CircleShape
    // 原型 .fab: 56x56, border-radius 16px — 圆角方块而非正圆
    val FAB = RoundedCornerShape(16.dp)
    val Switch = CircleShape
}

/**
 * UI重设计中的原型专用形状。
 * 用于精确匹配原型的新自定义组件。
 */
object PrototypeShapes {
    val Card = RoundedCornerShape(12.dp)
    val Pill = CircleShape
    val Small = RoundedCornerShape(6.dp)
    val Medium = RoundedCornerShape(10.dp)
    val Large = RoundedCornerShape(16.dp)
    // 原型 .fab: border-radius 16px
    val FAB = RoundedCornerShape(16.dp)
    val BottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val ModeSwitch = RoundedCornerShape(8.dp)
    val FileTreeItem = RoundedCornerShape(8.dp)
    val InputField = RoundedCornerShape(8.dp)
    val EditorTab = RoundedCornerShape(0.dp)
    val GuideCard = RoundedCornerShape(14.dp)
    val ConflictWarning = RoundedCornerShape(8.dp)
    val StatusCircle = CircleShape
}
