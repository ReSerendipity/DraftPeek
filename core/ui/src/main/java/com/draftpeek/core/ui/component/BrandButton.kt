/**
 * DraftPeek 品牌按钮组件。
 *
 * 设计层级：原子组件（Atom）— 最小可复用 UI 基元。
 *
 * 提供三种品牌按钮样式，统一封装 Material3 按钮组件：
 * - [BrandFilledButton]：填充按钮，使用品牌强调色背景
 * - [BrandOutlinedButton]：描边按钮，透明背景带边框
 * - [BrandTonalButton]：色调按钮，柔和背景色
 *
 * 所有按钮统一使用 10dp 圆角和 48dp 高度，符合品牌设计规范。
 * **禁止直接使用 Material3 的 Button/OutlinedButton/FilledTonalButton，必须使用本文件中的品牌组件。**
 *
 * ## P1 设计系统文档化说明
 * Brand 组件体系是 DraftPeek 设计语言的载体，Material3 是底层设计 token。
 * 封装量合理（每个组件 < 50 行），**不应替换为其他库**。
 * 设计规范：
 * - 圆角：10dp（BrandShapes.Medium）
 * - 高度：48dp（触控目标最小值）
 * - 强调色：PrototypeTokens.accent
 * - 间距：DraftPeekSpacing
 * 后续优化方向：接入 Figma-to-Compose 自动化流程，保持设计稿与代码同步。
 */
package com.draftpeek.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.draftpeek.core.ui.theme.DraftPeekSpacing
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * DraftPeek 品牌填充按钮，封装 Material3 [Button]。
 *
 * 使用品牌 accent 色作为背景，白色文字，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 Button。**
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun BrandFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BrandFilledButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * DraftPeek 品牌填充按钮，封装 Material3 [Button]。
 *
 * 使用品牌 accent 色作为背景，白色文字，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 Button。**
 *
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param content 按钮内容
 */
@Composable
fun BrandFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(DraftPeekSpacing.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrototypeTokens.accent,
            contentColor = Color.White,
        ),
        content = content,
    )
}

/**
 * DraftPeek 品牌描边按钮，封装 Material3 [OutlinedButton]。
 *
 * 透明背景，品牌前景色文字，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 OutlinedButton。**
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun BrandOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BrandOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * DraftPeek 品牌描边按钮，封装 Material3 [OutlinedButton]。
 *
 * 透明背景，品牌前景色文字，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 OutlinedButton。**
 *
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param content 按钮内容
 */
@Composable
fun BrandOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(DraftPeekSpacing.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PrototypeTokens.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = PrototypeTokens.fg,
        ),
        content = content,
    )
}

/**
 * DraftPeek 品牌色调按钮，封装 Material3 [FilledTonalButton]。
 *
 * 使用品牌 accentSoft 背景色，accent 前景色，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 FilledTonalButton。**
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 */
@Composable
fun BrandTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BrandTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * DraftPeek 品牌色调按钮，封装 Material3 [FilledTonalButton]。
 *
 * 使用品牌 accentSoft 背景色，accent 前景色，10dp 圆角，48dp 高度。
 * **所有场景应优先使用此组件而非直接使用 Material3 FilledTonalButton。**
 *
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param enabled 是否启用
 * @param content 按钮内容
 */
@Composable
fun BrandTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(DraftPeekSpacing.ButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = PrototypeTokens.accentSoft,
            contentColor = PrototypeTokens.accent,
        ),
        content = content,
    )
}
