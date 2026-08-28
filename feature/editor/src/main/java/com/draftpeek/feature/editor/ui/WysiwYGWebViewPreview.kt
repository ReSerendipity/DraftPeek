/**
 * WYSIWYG（所见即所得）Markdown WebView 编辑器组件。
 *
 * 使用 WebView 加载富文本编辑器 HTML，提供可视化 Markdown 编辑体验。
 * 支持实时内容同步、格式化操作回调、深色主题、自定义 CSS，
 * 并具备渲染进程崩溃自动恢复机制。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
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
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.core.common.util.MarkdownSanitizer
import com.draftpeek.core.designsystem.theme.PrototypeTokens
import com.draftpeek.feature.editor.model.MarkdownTheme
import java.io.File
import java.net.URLEncoder
import kotlinx.coroutines.delay

private const val TAG = "WysiwYGWebView"
private const val MAX_CONTENT_SIZE = 500_000
private const val TEMP_FILE_NAME = "wysiwg_preview_current.md"
private const val EDITING_RESET_DELAY_MS = 600L
private const val EDITING_IDLE_THRESHOLD_MS = 500L
private const val LARGE_FILE_DEBOUNCE_MS = 500L
private const val NORMAL_FILE_DEBOUNCE_MS = 200L
private const val LARGE_FILE_THRESHOLD_CHARS = 100_000
private const val CRASH_RECOVERY_DELAY_MS = 1000L

/**
 * WYSIWYG Markdown WebView 编辑器 Composable。
 *
 * 使用 WebView 加载富文本编辑器，支持可视化编辑 Markdown。
 * 编辑时暂停外部内容更新以避免光标跳转，空闲后自动同步。
 * 具备防抖渲染、崩溃恢复、临时文件加载等机制。
 *
 * @param markdownContent 当前 Markdown 文本内容
 * @param onContentChanged 内容变更回调
 * @param isDarkTheme 是否深色主题
 * @param theme Markdown 主题
 * @param customCss 自定义 CSS 样式
 * @param modifier 修饰符
 */
@Composable
fun WysiwYGWebViewPreview(
    markdownContent: String,
    modifier: Modifier = Modifier,
    onContentChanged: ((String) -> Unit)? = null,
    isDarkTheme: Boolean = false,
    theme: MarkdownTheme = MarkdownTheme.DEFAULT,
    customCss: String? = null
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var lastRenderedKey by remember { mutableStateOf("") }
    var pageLoaded by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var pendingFilePath by remember { mutableStateOf<String?>(null) }
    val surfaceColor = PrototypeTokens.surface
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val pendingRunnables = remember { mutableListOf<Runnable>() }

    var currentMarkdown by remember { mutableStateOf(markdownContent) }
    var isEditing by remember { mutableStateOf(false) }
    var lastEditTime by remember { mutableStateOf(0L) }

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

    LaunchedEffect(markdownContent) {
        if (!isEditing) {
            currentMarkdown = markdownContent
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingRunnables.toList().forEach { mainHandler.removeCallbacks(it) }
            pendingRunnables.clear()
            val ctx = webView?.context
            webView?.stopLoading()
            webView?.destroy()
            webView = null
            try {
                ctx?.cacheDir?.listFiles { f ->
                    f.name.startsWith("wysiwg_preview_") && f.name.endsWith(".md")
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

                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        allowFileAccess = true
                        allowContentAccess = false
                    }
                    setLayerType(View.LAYER_TYPE_NONE, null)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            pageLoaded = true
                            rendererCrashed = false
                            pendingFilePath?.let { filePath ->
                                pendingFilePath = null
                                val darkParam = if (isDarkTheme) "1" else "0"
                                val themeParam = theme.name.lowercase()
                                val encodedUrl = URLEncoder.encode(filePath, "UTF-8")
                                view?.loadUrl(
                                    "file:///android_asset/markdown/wysiwg-editor.html?file=$encodedUrl&dark=$darkParam&theme=$themeParam"
                                )
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return true
                            return !url.startsWith("file:///android_asset/") &&
                                !url.startsWith("file:///data/")
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            Log.w(TAG, "WebView renderer crashed (crashed=${detail?.didCrash()})")
                            rendererCrashed = true
                            pageLoaded = false
                            pendingFilePath = null
                            lastRenderedKey = ""
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
                    addJavascriptInterface(
                        WysiwYGCallback(
                            onContentChanged = { newMarkdown ->
                                isEditing = true
                                lastEditTime = System.currentTimeMillis()
                                currentMarkdown = newMarkdown
                                onContentChanged?.invoke(newMarkdown)
                                postDelayedSafely(EDITING_RESET_DELAY_MS) {
                                    if (System.currentTimeMillis() - lastEditTime > EDITING_IDLE_THRESHOLD_MS) {
                                        isEditing = false
                                    }
                                }
                            },
                            onFormatApplied = { format ->
                                Log.d(TAG, "Format applied: $format")
                            }
                        ),
                        "Android"
                    )
                    loadUrl("file:///android_asset/markdown/wysiwg-editor.html")
                    webView = this
                }
            },
            modifier = modifier.fillMaxSize(),
            update = { view ->
                view.setBackgroundColor(surfaceColor.toArgb())

                if (isEditing) return@AndroidView

                val key = "$markdownContent\u0000$isDarkTheme\u0000$theme"
                if (key != lastRenderedKey) {
                    lastRenderedKey = key
                    val debounceMs = if (markdownContent.length > LARGE_FILE_THRESHOLD_CHARS) {
                        LARGE_FILE_DEBOUNCE_MS
                    } else {
                        NORMAL_FILE_DEBOUNCE_MS
                    }

                    pendingRunnables.toList().forEach { mainHandler.removeCallbacks(it) }
                    pendingRunnables.clear()

                    postDelayedSafely(debounceMs) {
                        try {
                            val previewContent = if (markdownContent.length > MAX_CONTENT_SIZE) {
                                markdownContent.substring(0, MAX_CONTENT_SIZE) +
                                    "\n\n---\nContent too large for preview"
                            } else {
                                markdownContent
                            }

                            val ctx = view.context
                            val tmpFile = File(ctx.cacheDir, TEMP_FILE_NAME)
                            tmpFile.writeText(previewContent)
                            val fileUrl = "file://${tmpFile.absolutePath}"

                            if (pageLoaded) {
                                val encodedUrl = URLEncoder.encode(fileUrl, "UTF-8")
                                val darkParam = if (isDarkTheme) "1" else "0"
                                val themeParam = theme.name.lowercase()
                                view.loadUrl(
                                    "file:///android_asset/markdown/wysiwg-editor.html?file=$encodedUrl&dark=$darkParam&theme=$themeParam"
                                )
                            } else {
                                pendingFilePath = fileUrl
                            }

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
                            Log.w(TAG, "Failed to render markdown in WYSIWYG WebView", e)
                        }
                    }
                }
            }
        )
    }
}

private class WysiwYGCallback(
    private val onContentChanged: (String) -> Unit,
    private val onFormatApplied: (String) -> Unit
) {
    @JavascriptInterface
    fun onContentChanged(newContent: String) {
        onContentChanged(newContent)
    }

    @JavascriptInterface
    fun onFormatApplied(format: String) {
        onFormatApplied(format)
    }
}
