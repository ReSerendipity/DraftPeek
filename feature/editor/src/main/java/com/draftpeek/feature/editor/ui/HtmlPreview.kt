/**
 * HTML 预览组件。
 *
 * 使用沙箱化的只读 WebView 渲染原始 HTML 内容。
 * 禁用网络访问和 JavaScript，仅渲染本地 HTML 内容，
 * 支持深色主题样式注入，并提供一键转换为 Markdown 的功能。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.core.common.util.WebViewStyleHelper
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R

/**
 * HTML 预览 Composable。
 *
 * 在只读沙箱 WebView 中渲染原始 HTML 内容，禁用网络访问和 JavaScript，
 * 仅渲染本地 HTML 内容。用于编辑器在源代码视图和渲染 HTML 视图之间切换。
 * 右下角提供悬浮按钮，可一键将 HTML 转换为 Markdown 进行编辑。
 *
 * @param htmlContent 要渲染的原始 HTML 文本
 * @param darkTheme 当前是否为深色主题，用于注入深色基础样式表以匹配应用主题
 * @param onConvertToMarkdown 用户希望将此 HTML 转换为 Markdown 进行编辑时的回调
 * @param modifier 可选的修饰符
 */
@Composable
fun HtmlPreview(
    htmlContent: String,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    onConvertToMarkdown: (() -> Unit)? = null
) {
    val pageBg = PrototypeTokens.pageBackground
    val surfaceColor = PrototypeTokens.surface
    val fgColor = PrototypeTokens.fg
    val styledHtml = rememberStyledHtml(htmlContent, darkTheme, surfaceColor, fgColor)

    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    WebViewStyleHelper.configureStaticHtmlWebView(this, surfaceColor.toArgb())
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setBackgroundColor(surfaceColor.toArgb())
                WebViewStyleHelper.loadHtml(view, styledHtml)
            }
        )

        // Convert to Markdown FAB
        // 例外保留 M3 SmallFloatingActionButton：BrandFAB 是带展开速度拨号菜单的复合组件，
        // 固定展示 + 图标、56dp 尺寸且要求传入 List<FABMenuItem>，无法覆盖此场景的
        // 单动作小型 FAB（40dp、自定义 containerColor、自定义 Transform 图标）。
        // 详见 task rule 7：Brand* 完全无法覆盖时保留 M3 用法。
        if (onConvertToMarkdown != null) {
            SmallFloatingActionButton(
                onClick = onConvertToMarkdown,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = surfaceColor
            ) {
                Icon(
                    imageVector = Icons.Filled.Transform,
                    contentDescription = stringResource(R.string.editor_convert_html_to_markdown)
                )
            }
        }
    }
}

/**
 * 根据主题生成带样式的 HTML。
 *
 * 在深色主题下注入 CSS 样式，设置背景色、前景色和链接颜色，
 * 使 HTML 预览与应用主题保持一致。浅色主题直接返回原始 HTML。
 *
 * @param htmlContent 原始 HTML 内容
 * @param darkTheme 是否深色主题
 * @param surfaceColor 表面背景色
 * @param fgColor 前景文字色
 * @return 添加了样式的 HTML 字符串
 */
@Composable
private fun rememberStyledHtml(htmlContent: String, darkTheme: Boolean, surfaceColor: Color, fgColor: Color): String {
    if (!darkTheme) return htmlContent
    val surfaceHex = surfaceColor.toHexString()
    val fgHex = fgColor.toHexString()
    val linkHex = "#5A9BD5"
    val css = """
        <style>
            html, body { background-color: $surfaceHex; color: $fgHex; }
            a { color: $linkHex; }
        </style>
    """.trimIndent()
    return css + htmlContent
}

/**
 * 将 Compose Color 转换为十六进制颜色字符串（如 #FFFFFF）。
 *
 * @return 六位十六进制颜色字符串，不带 alpha 通道
 */
private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}
