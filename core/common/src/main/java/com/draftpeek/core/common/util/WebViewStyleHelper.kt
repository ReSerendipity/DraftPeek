/**
 * WebView样式配置工具模块。
 *
 * 集中管理WebView实例的配置，提供一致的安全加固设置，避免OfficeDocumentScreen、HtmlPreview、
 * MarkdownWebViewPreview等场景中的重复配置。支持静态HTML（禁用JS）和需要JS的内容（如Markdown预览）两种配置。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView

/**
 * WebView配置工具对象。
 *
 * 集中配置WebView的安全设置，包括JavaScript开关、网络加载阻断、地理位置禁用、文件访问限制等。
 * 提供两种配置模式：静态HTML模式（禁用JS）和JavaScript模式（启用JS但限制网络/文件访问）。
 * 使用about:blank作为base URL加载HTML，防止WebView访问本地文件或网络资源。
 */
object WebViewStyleHelper {

    /**
     * 配置WebView用于渲染静态HTML内容（不需要JavaScript）。
     *
     * 安全加固：
     * - 禁用JavaScript
     * - 阻断网络加载
     * - 阻断网络图片
     * - 禁用地理位置
     *
     * @param webView 要配置的WebView实例
     * @param backgroundColor 背景颜色（ARGB整数），默认透明
     */
    fun configureStaticHtmlWebView(webView: WebView, backgroundColor: Int = Color.TRANSPARENT) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = false
                blockNetworkLoads = true
                blockNetworkImage = true
                setGeolocationEnabled(false)
                defaultTextEncodingName = "UTF-8"
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(backgroundColor)
        }
    }

    /**
     * 配置WebView用于渲染需要JavaScript的内容（如Markdown预览）。
     *
     * 安全加固：
     * - 阻断网络加载（仅离线渲染）
     * - 禁用地理位置
     * - 限制file://URL的文件访问
     *
     * @param webView 要配置的WebView实例
     * @param backgroundColor 背景颜色（ARGB整数），默认透明
     */
    @SuppressLint("SetJavaScriptEnabled") // 渲染 Markdown 预览需 JS；已通过 blockNetworkLoads/禁文件访问/HTML 消毒（jsoup, VULN-004）加固
    fun configureJavascriptWebView(webView: WebView, backgroundColor: Int = Color.TRANSPARENT) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                blockNetworkLoads = true
                setGeolocationEnabled(false)
                allowFileAccess = false
                allowContentAccess = false
                defaultTextEncodingName = "UTF-8"
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(backgroundColor)
        }
    }

    /**
     * 使用about:blank作为base URL将HTML内容加载到WebView中。
     * 使用about:blank可防止WebView访问本地文件或网络资源。
     *
     * @param webView 要加载内容的WebView实例
     * @param htmlContent 要显示的HTML内容
     * @param mimeType MIME类型，默认"text/html"
     * @param encoding 字符编码，默认"UTF-8"
     */
    fun loadHtml(webView: WebView, htmlContent: String, mimeType: String = "text/html", encoding: String = "UTF-8") {
        webView.loadDataWithBaseURL(
            "about:blank",
            htmlContent,
            "$mimeType; charset=$encoding",
            encoding,
            null
        )
    }
}
