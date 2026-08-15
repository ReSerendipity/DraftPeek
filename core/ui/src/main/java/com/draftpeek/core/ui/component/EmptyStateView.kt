/**
 * DraftPeek 空状态视图组件。
 *
 * 设计层级：分子组件（Molecule）— 组合原子组件形成的功能单元。
 *
 * 提供两种视觉变体的空状态组件：
 * - [EmptyStateVariant.CALLIGRAPHY]（默认）：钢笔尖动画，墨水波纹效果，衬线字体排版
 * - [EmptyStateVariant.MINIMAL]：简约图标 + 文本，适用于次要空状态（历史、片段等）
 *
 * CALLIGRAPHY 变体包含：
 * - 钢笔尖 SVG 路径，轻柔上下浮动（3秒周期）
 * - 从笔尖扩散的墨水波纹圆圈
 * - 衬线风格排版，额外字间距
 */
package com.draftpeek.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * [EmptyStateView] 的视觉变体枚举。
 */
enum class EmptyStateVariant {
    /** 书法钢笔尖动画，带墨水波纹和衬线字体排版 */
    CALLIGRAPHY,
    /** 简约图标 + 文本，适用于次要空状态（历史记录、代码片段等） */
    MINIMAL,
}

/**
 * 具有两种视觉变体的空状态组件。
 *
 * **CALLIGRAPHY**（默认）：钢笔尖 SVG 路径轻柔上下浮动（3秒周期），
 * 从笔尖扩散的墨水波纹圆圈，带额外字间距的衬线风格排版。
 *
 * **MINIMAL**：单个图标带标题和副标题文本，无动画。
 * 用于浏览历史或代码片段列表等次要界面。
 *
 * @param title 主要消息（例如"此处留白"）
 * @param subtitle 次要说明（可选）
 * @param variant 视觉样式：[EmptyStateVariant.CALLIGRAPHY] 或 [EmptyStateVariant.MINIMAL]
 * @param icon MINIMAL 变体的自定义图标；CALLIGRAPHY 变体中忽略
 * @param action 操作按钮 Composable（可选）
 * @param modifier 可选 Modifier
 */
@Composable
fun EmptyStateView(
    title: String,
    subtitle: String? = null,
    variant: EmptyStateVariant = EmptyStateVariant.CALLIGRAPHY,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (variant == EmptyStateVariant.MINIMAL) {
        EmptyStateMinimal(
            title = title,
            subtitle = subtitle,
            icon = icon,
            action = action,
            modifier = modifier,
        )
    } else {
        EmptyStateCalligraphy(
            title = title,
            subtitle = subtitle,
            action = action,
            modifier = modifier,
        )
    }
}

/**
 * 简约变体：图标 + 标题 + 副标题 + 可选操作按钮。
 */
@Composable
private fun EmptyStateMinimal(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    action: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "minimal_alpha",
    )
    val contentOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 12f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "minimal_offset",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffsetY
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrototypeTokens.fgSoft.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            color = PrototypeTokens.fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f),
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                ),
                color = PrototypeTokens.fgSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 16.dp),
            )
        }

        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

/**
 * 书法变体：带动画钢笔尖、墨水波纹和衬线字体排版。
 */
@Composable
private fun EmptyStateCalligraphy(
    title: String,
    subtitle: String?,
    action: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "content_alpha",
    )

    val contentOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "content_offset",
    )

    // Pen bob animation: translateY oscillates 0 -> -5dp over 3 seconds, ease-in-out
    val infiniteTransition = rememberInfiniteTransition(label = "pen_bob")
    val penOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -1f, // normalized; mapped to dp in layout
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pen_bob_offset",
    )

    // Ink ripple 1: scale from 0 to 5x with fading opacity, 3s cycle
    val inkRipple1Scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ink_ripple1_scale",
    )
    val inkRipple1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ink_ripple1_alpha",
    )

    // Ink ripple 2: same cycle but staggered by 0.5s (start delay)
    val inkRipple2Scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ink_ripple2_scale",
    )
    val inkRipple2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ink_ripple2_alpha",
    )

    val penColor = PrototypeTokens.fgSoft.copy(alpha = 0.4f)
    val rippleColor = PrototypeTokens.fgSoft.copy(alpha = 0.15f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffsetY
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Pen nib with ink ripples
        Box(
            modifier = Modifier.size(width = 96.dp, height = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Ink ripple circles expanding from pen tip area
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(width = 96.dp, height = 120.dp),
            ) {
                val center = Offset(size.width / 2f, size.height * 0.65f)
                val baseRadius = size.width * 0.06f

                // Ripple 1
                if (inkRipple1Scale > 0f) {
                    drawCircle(
                        color = rippleColor,
                        radius = baseRadius * inkRipple1Scale * 5f,
                        center = center,
                        alpha = inkRipple1Alpha,
                    )
                }

                // Ripple 2
                if (inkRipple2Scale > 0f) {
                    drawCircle(
                        color = rippleColor,
                        radius = baseRadius * inkRipple2Scale * 5f,
                        center = center,
                        alpha = inkRipple2Alpha,
                    )
                }
            }

            // Pen nib drawn on Canvas with bob animation
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(width = 48.dp, height = 80.dp)
                    .graphicsLayer {
                        translationY = penOffsetY * 5f // maps -1..0 to -5..0 dp
                    },
            ) {
                drawPenNib(penColor)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title - serif style, 22sp, letter spacing 6sp, weight 900
        Text(
            text = title,
            style = com.draftpeek.core.ui.theme.SerifTitleStyle,
            color = PrototypeTokens.fg,
            textAlign = TextAlign.Center,
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle - serif style, 13sp, letter spacing 3sp
            Text(
                text = subtitle,
                style = com.draftpeek.core.ui.theme.SerifSubtitleStyle,
                color = PrototypeTokens.fgSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 16.dp),
            )
        }

        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

/**
 * 使用 SVG 风格路径命令绘制钢笔尖轮廓。
 *
 * 笔身：M25,10 L35,10 L35,65 L30,95 L25,65 Z
 * 笔身文字线：y=20、25、30 处的水平线
 * 笔尖细节：y=70、78 处的水平线
 *
 * 所有坐标在 0-100 逻辑空间中，缩放到画布大小。
 *
 * @param color 钢笔尖的绘制颜色
 */
private fun DrawScope.drawPenNib(color: Color) {
    val w = size.width
    val h = size.height
    // Scale from logical 60x100 space to actual canvas size
    val scaleX = w / 60f
    val scaleY = h / 100f

    // Pen body path
    val penPath = Path().apply {
        moveTo(25f * scaleX, 10f * scaleY)
        lineTo(35f * scaleX, 10f * scaleY)
        lineTo(35f * scaleX, 65f * scaleY)
        lineTo(30f * scaleX, 95f * scaleY)
        lineTo(25f * scaleX, 65f * scaleY)
        close()
    }

    drawPath(
        path = penPath,
        color = color,
        style = Stroke(
            width = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )

    // Text lines on pen body
    val lineLeft = 26f * scaleX
    val lineRight = 34f * scaleX

    for (y in listOf(20f, 25f, 30f)) {
        drawLine(
            color = color,
            start = Offset(lineLeft, y * scaleY),
            end = Offset(lineRight, y * scaleY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Tip detail lines
    for (y in listOf(70f, 78f)) {
        val centerX = 30f * scaleX
        val halfWidth = when (y) {
            70f -> 4f * scaleX
            78f -> 2f * scaleX
            else -> 3f * scaleX
        }
        drawLine(
            color = color,
            start = Offset(centerX - halfWidth, y * scaleY),
            end = Offset(centerX + halfWidth, y * scaleY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
