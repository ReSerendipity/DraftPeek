/**
 * 模糊效果 Modifier。
 *
 * 提供高斯模糊效果的 Modifier 扩展，在 Android S（API 31）及以上版本使用
 * [RenderEffect] 实现模糊；在旧 API 级别上此 Modifier 为空操作，Composable 保持不模糊。
 *
 * 注意：模糊操作开销较大。建议使用较小的半径（8-20dp），避免应用于
 * 大面积或频繁失效的区域。
 */
package com.draftpeek.core.ui.modifier

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 在 Android S（API 31）及以上版本为 Composable 应用高斯模糊，
 * 该版本支持基于 [RenderEffect] 的模糊。在旧 API 级别上此 Modifier
 * 为空操作，Composable 保持不模糊——需要回退方案（例如半透明遮罩）的调用方
 * 应单独叠加该层。
 *
 * 用法：
 * ```
 * Box(Modifier.applyBlurIfSupported(16.dp)) { ... }
 * ```
 *
 * 注意：模糊操作开销较大。建议使用较小半径（8-20dp），避免应用于
 * 大面积或频繁失效的区域。
 *
 * @param radius 模糊半径（Dp），X 和 Y 轴应用相同值，默认为 12dp
 */
fun Modifier.applyBlurIfSupported(radius: Dp = 12.dp): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            val radiusPx = radius.toPx()
            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                radiusPx,
                radiusPx,
                android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    } else {
        Modifier
    }
)
