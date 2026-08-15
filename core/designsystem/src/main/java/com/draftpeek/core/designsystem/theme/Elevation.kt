/**
 * DraftPeek 阴影/海拔系统定义文件。
 *
 * 提供5级阴影系统，通过Compose Modifier.shadow()实现，
 * 值对应HTML原型中的CSS box-shadow效果。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * DraftPeek 5级阴影系统，通过Compose Modifier.shadow()近似实现。
 * 值对应HTML原型中的CSS box-shadow等效值。
 */
object BrandElevation {

    /**
     * 细微阴影，用于标签、徽章、小型交互元素。
     * CSS: 0 1px 2px rgba(0,0,0,.02), 0 1px 2px rgba(0,0,0,.03)
     */
    fun Modifier.shadowSm(shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier =
        this.shadow(
            elevation = 1.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.02f),
            spotColor = Color.Black.copy(alpha = 0.03f),
        )

    /**
     * 小卡片/按钮阴影。
     * CSS: 0 1px 3px rgba(0,0,0,.03), 0 2px 6px rgba(0,0,0,.04)
     */
    fun Modifier.shadowMd(shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier =
        this.shadow(
            elevation = 2.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.03f),
            spotColor = Color.Black.copy(alpha = 0.04f),
        )

    /**
     * 标准卡片阴影。
     * CSS: 0 2px 8px rgba(0,0,0,.04), 0 4px 12px rgba(0,0,0,.05)
     */
    fun Modifier.shadowLg(shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier =
        this.shadow(
            elevation = 4.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.05f),
        )

    /**
     * 对话框/底部表单/悬浮表面阴影。
     * CSS: 0 4px 16px rgba(0,0,0,.05), 0 8px 24px rgba(0,0,0,.07)
     */
    fun Modifier.shadowXl(shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier =
        this.shadow(
            elevation = 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.05f),
            spotColor = Color.Black.copy(alpha = 0.07f),
        )

    /**
     * FAB浮动操作按钮阴影（含朱砂红强调色光晕）。
     * CSS: 0 6px 16px rgba(196,30,58,.30),0 2px 6px rgba(0,0,0,.12)
     */
    fun Modifier.shadowFab(shape: RoundedCornerShape = RoundedCornerShape(0.dp)): Modifier =
        this.shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0xFFC41E3A).copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.12f),
        )
}
