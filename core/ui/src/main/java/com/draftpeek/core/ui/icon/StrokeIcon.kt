/**
 * 描边图标渲染组件（StrokeIcon）。
 *
 * 用原生描边（[androidx.compose.ui.graphics.drawscope.Stroke]）绘制 [StrokeIcons] 中定义的图标：
 * - 折线 → `drawPath(style = Stroke(cap = Round, join = Round))`，圆头/圆角由渲染器计算，
 *   从根上避免"填充几何拼接"方案在子路径接缝处的抗锯齿裂缝（小白点/断点）；
 * - 圆环 → `drawCircle(style = Stroke)`，不再需要 EvenOdd 挖孔；
 * - 实心元素（圆点、文件夹、笔尖等） → 正常填充。
 *
 * 用法与 Material [androidx.compose.material3.Icon] 一致：
 * `StrokeIcon(icon = StrokeIcons.Search, contentDescription = "搜索", tint = ...)`
 */
package com.draftpeek.core.ui.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 绘制一个描边风格图标。
 *
 * @param icon 图标定义（见 [StrokeIcons]）
 * @param modifier 图标大小/布局修饰符（图标按 24 视口等比缩放）
 * @param tint 图标颜色
 * @param contentDescription 无障碍描述（null 表示纯装饰）
 */
@Composable
fun StrokeIcon(
    icon: StrokeIconDef,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    // 与 Material Icon 行为一致：未显式指定大小时默认 24dp。
    // `modifier.then(size(24.dp))` 保证调用方显式传入的 size 仍优先生效。
    Canvas(modifier = modifier.then(Modifier.size(24.dp)).then(semanticsModifier)) {
        // 24 视口 → 实际像素
        val s = size.minDimension / 24f
        val scaleMatrix = Matrix()
        scaleMatrix.scale(s, s, 1f)
        for (item in icon.items) {
            when (item) {
                is StrokeItem.Stroke -> {
                    val path = Path()
                    item.points.forEachIndexed { i, p ->
                        if (i == 0) {
                            path.moveTo(p.x * s, p.y * s)
                        } else {
                            path.lineTo(p.x * s, p.y * s)
                        }
                    }
                    drawPath(
                        path = path,
                        color = tint,
                        style = Stroke(
                            width = item.width * s,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
                is StrokeItem.CircleFill -> drawCircle(
                    color = tint,
                    radius = item.radius * s,
                    center = Offset(item.center.x * s, item.center.y * s),
                )
                is StrokeItem.CircleStroke -> drawCircle(
                    color = tint,
                    radius = item.radius * s,
                    center = Offset(item.center.x * s, item.center.y * s),
                    style = Stroke(width = item.width * s),
                )
                is StrokeItem.PathFill -> {
                    val scaled = Path().apply {
                        addPath(item.path, Offset.Zero)
                        transform(scaleMatrix)
                    }
                    drawPath(path = scaled, color = tint)
                }
            }
        }
    }
}
