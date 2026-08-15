/**
 * Office 文档预览组件。
 *
 * 使用 WebView 渲染由 Apache POI 解析生成的 HTML 内容，支持 Word、Excel、PPT 等
 * Office 文档格式的预览。支持深色主题样式注入，自动适配表格颜色。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.core.common.util.WebViewStyleHelper
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.ui.theme.PrototypeTokens

/**
 * Office 文档预览 Composable。
 *
 * 使用 WebView 渲染 Apache POI 解析后的 HTML 内容，支持 Word/Excel/PPT 预览。
 * 深色主题下自动注入 CSS 样式，适配表格边框颜色。
 *
 * @param htmlContent 由 POI 解析生成的 HTML 内容
 * @param documentType 文档类型（WORD/EXCEL/PPT）
 * @param darkTheme 是否深色主题
 * @param modifier 修饰符
 */
@Composable
fun OfficeDocumentScreen(
    htmlContent: String,
    documentType: DocumentType,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
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
            .background(pageBg),
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    WebViewStyleHelper.configureStaticHtmlWebView(this, surfaceColor.toArgb())
                    WebViewStyleHelper.loadHtml(this, styledHtml)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.setBackgroundColor(surfaceColor.toArgb())
                WebViewStyleHelper.loadHtml(view, styledHtml)
            },
        )
    }
}

/**
 * 根据主题生成带样式的 HTML（Office 文档专用）。
 *
 * 深色主题下注入 CSS 样式，适配背景色、文字色、链接色，以及表格边框颜色，
 * 确保 Office 文档（特别是 Excel 表格）在深色模式下显示正常。
 *
 * @param htmlContent 原始 HTML 内容
 * @param darkTheme 是否深色主题
 * @param surfaceColor 表面背景色
 * @param fgColor 前景文字色
 * @return 添加了样式的 HTML 字符串
 */
private fun rememberStyledHtml(
    htmlContent: String,
    darkTheme: Boolean,
    surfaceColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
): String {
    if (!darkTheme) return htmlContent
    val surfaceHex = surfaceColor.toHexString()
    val fgHex = fgColor.toHexString()
    val linkHex = "#5A9BD5"
    val css = """
        <style>
            html, body { background-color: $surfaceHex; color: $fgHex; }
            a { color: $linkHex; }
            table { background-color: $surfaceHex; color: $fgHex; border-color: #2A2A2E; }
            td, th { border-color: #2A2A2E; }
        </style>
    """.trimIndent()
    return css + htmlContent
}

/**
 * 将 Compose Color 转换为十六进制颜色字符串。
 *
 * @return 六位十六进制颜色字符串（如 #FFFFFF），不带 alpha 通道
 */
private fun androidx.compose.ui.graphics.Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}
