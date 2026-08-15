/**
 * Markdown WebView 预览组件。
 *
 * 使用 WebView + marked.js/KaTeX/Mermaid/highlight.js 渲染 Markdown 内容，
 * 支持数学公式、流程图、代码高亮、任务列表复选框等功能。
 * 内容通过临时文件加载以避免 evaluateJavascript() 字符串大小限制。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.core.common.util.ContentChecksum
import com.draftpeek.core.common.util.MarkdownSanitizer
import com.draftpeek.core.designsystem.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import com.draftpeek.feature.editor.model.MarkdownTheme
import kotlinx.coroutines.delay
import java.io.File
import java.net.URLEncoder

private const val TAG = "MarkdownWebView"
// Stage 2: WebView 可以处理大内容，将截断阈值提高到 2MB。
// 超过此大小的文件由 LazyMarkdownPreview 流式渲染处理。
private const val MAX_PREVIEW_SIZE = 2_000_000
private const val MD_TEMP_FILE_NAME = "md_preview_current.md"
private const val LARGE_FILE_DEBOUNCE_MS = 500L
private const val NORMAL_FILE_DEBOUNCE_MS = 200L
private const val LARGE_FILE_THRESHOLD_CHARS = 100_000
private const val CRASH_RECOVERY_DELAY_MS = 1000L

/**
 * Markdown WebView 预览 Composable。
 *
 * 使用 WebView 渲染 Markdown 内容，支持多种主题和自定义 CSS。
 * 内容写入临时文件后通过 XMLHttpRequest 由 JS 读取，
 * 避免 evaluateJavascript() 字符串大小限制导致的静默失败。
 * 支持渲染进程崩溃自动恢复。
 *
 * @param markdownContent 要预览的 Markdown 文本内容
 * @param onContentChanged 内容变更回调（复选框状态同步时触发）
 * @param onPreviewClick 预览区域点击回调
 * @param onHeadingClick 标题点击回调，参数为标题所在行索引（用于跳转编辑）
 * @param isDarkTheme 是否使用深色主题
 * @param theme Markdown 预览主题
 * @param customCss 自定义 CSS 样式（可选）
 * @param modifier 修饰符
 */
@Composable
fun MarkdownWebViewPreview(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onContentChanged: ((String) -> Unit)? = null,
    onPreviewClick: ((String) -> Unit)? = null,
    onHeadingClick: ((Int) -> Unit)? = null,
    isDarkTheme: Boolean = false,
    theme: MarkdownTheme = MarkdownTheme.DEFAULT,
    customCss: String? = null,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var lastRenderedChecksum by remember { mutableStateOf(0L) }
    var lastRenderedTheme by remember { mutableStateOf(MarkdownTheme.DEFAULT) }
    var lastRenderedDark by remember { mutableStateOf(false) }
    var pageLoaded by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var pendingFilePath by remember { mutableStateOf<String?>(null) }
    var savedScrollY by remember { mutableStateOf(0) }
    val truncationMessage = stringResource(R.string.editor_preview_truncated)
    val surfaceColor = PrototypeTokens.surface
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val pendingRunnables = remember { mutableListOf<Runnable>() }

    var currentMarkdown by remember { mutableStateOf(markdownContent) }
    LaunchedEffect(markdownContent) { currentMarkdown = markdownContent }

    fun removePendingRunnable(r: Runnable) {
        mainHandler.removeCallbacks(r)
        pendingRunnables.remove(r)
    }

    fun postDelayedSafely(delayMs: Long, action: () -> Unit): Runnable {
        val r = Runnable { action() }
        pendingRunnables.add(r)
        mainHandler.postDelayed(r, delayMs)
        return r
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingRunnables.toList().forEach { mainHandler.removeCallbacks(it) }
            pendingRunnables.clear()
            val ctx = webView?.context
            (webView?.webChromeClient as? VideoWebChromeClient)?.release()
            webView?.stopLoading()
            webView?.destroy()
            webView = null
            try {
                ctx?.cacheDir?.listFiles { f ->
                    f.name.startsWith("md_preview_") && f.name.endsWith(".md")
                }?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            delay(CRASH_RECOVERY_DELAY_MS)
            rendererCrashed = false
        }
    }

    if (!rendererCrashed) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setGeolocationEnabled(false)
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                    // Allow file access for temp file content loading
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    allowFileAccess = true
                    allowContentAccess = false
                }
                setLayerType(View.LAYER_TYPE_NONE, null)
                webChromeClient = VideoWebChromeClient(this)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        pageLoaded = true
                        rendererCrashed = false
                        // If there's a pending file, use loadUrl() to navigate (more reliable than evaluateJavascript)
                        pendingFilePath?.let { filePath ->
                            pendingFilePath = null
                            val darkParam = if (isDarkTheme) "1" else "0"
                            val themeParam = theme.name.lowercase()
                            val encodedUrl = URLEncoder.encode(filePath, "UTF-8")
                            view?.loadUrl("file:///android_asset/markdown/markdown-preview.html?file=$encodedUrl&dark=$darkParam&theme=$themeParam")
                            // 延迟恢复滚动位置，等待内容渲染完成
                            if (savedScrollY > 0) {
                                view?.postDelayed({
                                    view.scrollTo(0, savedScrollY)
                                }, 500)
                            }
                        } ?: run {
                            // 页面直接加载完成（无pending文件），恢复滚动位置
                            if (savedScrollY > 0) {
                                view?.postDelayed({
                                    view.scrollTo(0, savedScrollY)
                                }, 300)
                            }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return true
                        // Allow asset files AND temp files in cache dir
                        return !url.startsWith("file:///android_asset/") &&
                               !url.startsWith("file:///data/")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?, detail: RenderProcessGoneDetail?
                    ): Boolean {
                        Log.w(TAG, "WebView renderer crashed (crashed=${detail?.didCrash()})")
                        rendererCrashed = true
                        pageLoaded = false
                        pendingFilePath = null
                        lastRenderedChecksum = 0L
                        savedScrollY = 0
                        try {
                            view?.let {
                                it.stopLoading()
                                it.destroy()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error destroying crashed WebView", e)
                        }
                        return true
                    }
                }
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(surfaceColor.toArgb())
                addJavascriptInterface(WebViewCallback(
                    onCheckboxChanged = { statesJson ->
                        try {
                            val newContent = applyCheckboxStates(currentMarkdown, statesJson)
                            onContentChanged?.invoke(newContent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Checkbox sync failed", e)
                        }
                    },
                    onPreviewClick = onPreviewClick,
                    onHeadingClick = onHeadingClick,
                ), "Android")
                loadUrl("file:///android_asset/markdown/markdown-preview.html")
                webView = this
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            view.setBackgroundColor(surfaceColor.toArgb())

            // 使用CRC32校验和高效检测内容变化，避免大字符串直接比较
            val contentChecksum = ContentChecksum.crc32(markdownContent)
            val themeChanged = theme != lastRenderedTheme || isDarkTheme != lastRenderedDark
            val contentChanged = contentChecksum != lastRenderedChecksum

            if (contentChanged || themeChanged) {
                val debounceMs = if (markdownContent.length > LARGE_FILE_THRESHOLD_CHARS)
                    LARGE_FILE_DEBOUNCE_MS else NORMAL_FILE_DEBOUNCE_MS

                pendingRunnables.toList().forEach { mainHandler.removeCallbacks(it) }
                pendingRunnables.clear()

                postDelayedSafely(debounceMs) {
                    try {
                        // 渲染前保存当前滚动位置
                        savedScrollY = view.scrollY

                        val previewContent = if (markdownContent.length > MAX_PREVIEW_SIZE) {
                            markdownContent.substring(0, MAX_PREVIEW_SIZE) + "\n\n---\n$truncationMessage"
                        } else {
                            markdownContent
                        }
                        val sanitized = MarkdownSanitizer.sanitize(previewContent)

                        val ctx = view.context
                        val tmpFile = File(ctx.cacheDir, MD_TEMP_FILE_NAME)
                        tmpFile.writeText(sanitized)
                        val fileUrl = "file://${tmpFile.absolutePath}"

                        if (pageLoaded) {
                            val encodedUrl = URLEncoder.encode(fileUrl, "UTF-8")
                            val darkParam = if (isDarkTheme) "1" else "0"
                            val themeParam = theme.name.lowercase()
                            view.loadUrl("file:///android_asset/markdown/markdown-preview.html?file=$encodedUrl&dark=$darkParam&theme=$themeParam")
                            // 内容渲染后延迟恢复滚动位置
                            if (savedScrollY > 0) {
                                view.postDelayed({
                                    view.scrollTo(0, savedScrollY)
                                }, 500)
                            }
                        } else {
                            pendingFilePath = fileUrl
                        }

                        lastRenderedChecksum = contentChecksum
                        lastRenderedTheme = theme
                        lastRenderedDark = isDarkTheme

                        // Inject custom CSS if provided
                        if (pageLoaded && customCss != null) {
                            val escapedCss = MarkdownSanitizer.escapeForJsString(
                                MarkdownSanitizer.sanitizeCss(customCss)
                            )
                            view.evaluateJavascript(
                                "(function(){var s=document.getElementById('custom-css');if(!s){s=document.createElement('style');s.id='custom-css';document.head.appendChild(s)}s.textContent=\"$escapedCss\"})();",
                                null
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to render markdown in WebView", e)
                    }
                }
            }
        },
    )
    } // end if (!rendererCrashed)
}

private class WebViewCallback(
    private val onCheckboxChanged: (String) -> Unit,
    private val onPreviewClick: ((String) -> Unit)? = null,
    private val onHeadingClick: ((Int) -> Unit)? = null,
) {
    @JavascriptInterface
    fun onCheckboxChanged(statesJson: String) {
        onCheckboxChanged(statesJson)
    }

    @JavascriptInterface
    fun onPreviewClick(text: String) {
        onPreviewClick?.invoke(text)
    }

    @JavascriptInterface
    fun onHeadingClick(lineIndex: String) {
        val line = lineIndex.toIntOrNull()
        if (line != null) {
            onHeadingClick?.invoke(line)
        }
    }
}

private fun applyCheckboxStates(markdown: String, statesJson: String): String {
    val states = statesJson.trim()
        .removeSurrounding("[", "]")
        .split(",")
        .map { it.trim() == "true" }

    val lines = markdown.lines()
    var checkboxIdx = 0
    val result = lines.map { line ->
        val trimmed = line.trimStart()
        if ((trimmed.startsWith("- [") || trimmed.startsWith("* [") || trimmed.startsWith("+ ["))
            && trimmed.length > 4
            && (trimmed[3] == ']' || trimmed[3] == 'x' || trimmed[3] == 'X')
            && trimmed[4] == ' ') {
            if (checkboxIdx < states.size) {
                val isChecked = states[checkboxIdx]
                checkboxIdx++
                val newBox = if (isChecked) "[x]" else "[ ]"
                line.replace(Regex("""\[[ xX]\]"""), newBox)
            } else {
                checkboxIdx++
                line
            }
        } else {
            line
        }
    }
    return result.joinToString("\n")
}

private class VideoWebChromeClient(
    private val webView: WebView,
) : WebChromeClient() {
    private var customView: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback?) {
        if (customView != null) { callback?.onCustomViewHidden(); return }
        this.callback = callback
        customView = view
        val parent = webView.parent as? ViewGroup ?: return
        webView.visibility = View.GONE
        parent.addView(view, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    override fun onHideCustomView() {
        if (customView == null) return
        (webView.parent as? ViewGroup)?.removeView(customView)
        webView.visibility = View.VISIBLE
        customView = null
        callback?.onCustomViewHidden()
        callback = null
    }

    fun release() {
        customView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        customView = null
        callback?.onCustomViewHidden()
        callback = null
    }
}
