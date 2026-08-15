/**
 * DraftPeek 分区标题组件。
 *
 * 设计层级：原子组件（Atom）— 最小可复用 UI 基元。
 *
 * 提供统一的分区标题组件，替代各界面中重复的 SectionHeader 实现
 * （如 SettingsScreen、BrowseHistoryScreen），采用令牌驱动的单一原子组件。
 *
 * 支持三种视觉样式：
 * - [SectionHeaderStyle.SETTINGS]：设置风格，大写字母标签
 * - [SectionHeaderStyle.PRIMARY]：主标题风格，带强调色竖条
 * - [SectionHeaderStyle.SECONDARY]：副标题风格，柔和颜色
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.core.ui.theme.SectionLabelStyle

/**
 * 定义 [SectionHeader] 的视觉样式。
 *
 * 每种样式对应特定的产品场景：
 * - [SETTINGS]：设置风格分组界面中使用的大写标签
 * - [PRIMARY]：带强调色竖条的醒目标题，用于主分区标题
 * - [SECONDARY]：柔和的标题，用于二级或三级分区
 */
enum class SectionHeaderStyle {
    /** 设置风格：大写字母，Inter SemiBold 11sp，字间距 1sp，onSurfaceVariant 颜色 */
    SETTINGS,
    /** 主标题风格：Inter SemiBold 14sp，主色调，左侧带强调色竖条 */
    PRIMARY,
    /** 副标题风格：Inter Medium 13sp，onSurfaceVariant 颜色 */
    SECONDARY,
}

/**
 * DraftPeek 统一分区标题组件。
 *
 * 替代各界面中重复的 SectionHeader 实现（如 SettingsScreen、BrowseHistoryScreen），
 * 采用令牌驱动的单一原子组件。
 *
 * @param title 要显示的标题文本
 * @param modifier 应用于根 Composable 的可选 [Modifier]
 * @param style 要应用的视觉样式，默认为 [SectionHeaderStyle.SETTINGS]
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    style: SectionHeaderStyle = SectionHeaderStyle.SETTINGS,
) {
    when (style) {
        SectionHeaderStyle.SETTINGS -> {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SectionLabelStyle.fontFamily,
                    fontWeight = SectionLabelStyle.fontWeight,
                    fontSize = SectionLabelStyle.fontSize,
                    letterSpacing = SectionLabelStyle.letterSpacing,
                    lineHeight = SectionLabelStyle.lineHeight,
                ),
                color = PrototypeTokens.fgSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier.padding(
                    start = DraftPeekSpacing.Two,
                    end = DraftPeekSpacing.Two,
                    top = DraftPeekSpacing.Two,
                    bottom = DraftPeekSpacing.One,
                ),
            )
        }
        SectionHeaderStyle.PRIMARY -> {
            Row(
                modifier = modifier.padding(
                    horizontal = DraftPeekSpacing.Two,
                    vertical = DraftPeekSpacing.One,
                ),
            ) {
                Spacer(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrototypeTokens.accent)
                        .width(3.dp)
                        .padding(top = 2.dp), // optical centering within the text line
                )
                Spacer(Modifier.width(DraftPeekSpacing.Half))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = PrototypeTokens.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SectionHeaderStyle.SECONDARY -> {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = PrototypeTokens.fgSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier.padding(
                    start = DraftPeekSpacing.Two,
                    end = DraftPeekSpacing.Two,
                    top = DraftPeekSpacing.OneHalf,
                    bottom = DraftPeekSpacing.Half,
                ),
            )
        }
    }
}
