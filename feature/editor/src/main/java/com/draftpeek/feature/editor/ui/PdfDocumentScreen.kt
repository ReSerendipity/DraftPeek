/**
 * PDF 文档阅读器组件。
 *
 * 使用 Android 系统 PdfRenderer API 渲染 PDF 文件，支持多页翻页、双指缩放、
 * 错误处理（加密、损坏、权限等），从 assets、file:// 和 content:// URI 加载 PDF。
 *
 * 优势：
 * - 无外部依赖（Android 系统 API）
 * - 内存管理可控（按页渲染 Bitmap）
 * - 兼容所有 Android 5.0+ 设备
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeTokens
import java.io.File
import java.io.IOException

private const val TAG = "PdfDocumentScreen"
private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF

/**
 * 检查字节数组是否以指定前缀开头。
 */
private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (this.size < prefix.size) return false
    for (i in prefix.indices) {
        if (this[i] != prefix[i]) return false
    }
    return true
}

/**
 * PDF 文档阅读器 Composable。
 *
 * 使用 Android PdfRenderer 渲染 PDF 文件，支持手势缩放和滚动。
 * 包含完善的错误处理：文件不存在、空文件、格式错误、加密、权限拒绝等。
 *
 * @param fileUri PDF 文件的 URI
 * @param modifier 修饰符
 */
@Composable
fun PdfDocumentScreen(fileUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var loadError by remember { mutableStateOf<String?>(null) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val pageBg = PrototypeTokens.pageBackground
    val errorColor = PrototypeTokens.error
    val fgSoft = PrototypeTokens.fgSoft

    // Zoom and pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(fileUri) {
        isLoading = true
        loadError = null
        errorDetail = null
        pageBitmaps = emptyList()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            val resolvedUri = fileUri.toString()
            val file = when {
                resolvedUri.startsWith("file:///android_asset/") -> {
                    val assetPath = resolvedUri.removePrefix("file:///android_asset/")
                    val cacheFile = File(context.cacheDir, assetPath.substringAfterLast('/'))
                    if (!cacheFile.exists()) {
                        context.assets.open(assetPath).use { input ->
                            cacheFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    cacheFile
                }
                resolvedUri.startsWith("file://") -> {
                    File(Uri.parse(resolvedUri).path ?: "")
                }
                else -> {
                    val inputStream = context.contentResolver.openInputStream(fileUri)
                        ?: throw IllegalStateException("Cannot open URI: $fileUri")
                    val cacheFile = File(context.cacheDir, "temp_pdf.pdf")
                    cacheFile.outputStream().use { output ->
                        inputStream.use { input -> input.copyTo(output) }
                    }
                    cacheFile
                }
            }

            if (!file.exists()) {
                loadError = "文件不存在"
                errorDetail = "找不到指定的文件: ${file.name}"
                return@LaunchedEffect
            }

            if (file.length() == 0L) {
                loadError = "PDF 文件为空"
                errorDetail = "文件大小为 0 字节"
                return@LaunchedEffect
            }

            // Check PDF file header
            val header = ByteArray(4)
            try {
                file.inputStream().use { it.read(header) }
            } catch (e: IOException) {
                loadError = "无法读取文件"
                errorDetail = "文件可能已被损坏: ${e.message}"
                return@LaunchedEffect
            }

            if (!header.startsWith(PDF_MAGIC)) {
                loadError = "文件格式错误"
                errorDetail = "这不是有效的 PDF 文件（文件头不匹配）"
                return@LaunchedEffect
            }

            // Open PDF with PdfRenderer
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                try {
                    // Render at 150 DPI (scale = 150/72 ≈ 2.083)
                    val scale = 2.083f
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                } finally {
                    page.close()
                }
            }

            pageBitmaps = bitmaps
            isLoading = false
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied loading PDF: $fileUri", e)
            loadError = "无法访问文件"
            errorDetail = "没有足够的权限读取此文件"
            isLoading = false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load PDF: $fileUri", e)
            loadError = when {
                e.message.isNullOrEmpty() -> "无法加载 PDF 文件"
                else -> e.message!!
            }
            errorDetail = when {
                e.message?.contains("password", ignoreCase = true) == true ->
                    "此 PDF 文件已加密"
                e.message?.contains("corrupt", ignoreCase = true) == true ->
                    "PDF 文件已损坏"
                else -> null
            }
            isLoading = false
        } finally {
            renderer?.close()
            pfd?.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        if (loadError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = errorColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loadError ?: "加载失败",
                        style = DraftPeekTypography.bodyLarge,
                        color = errorColor
                    )
                    if (errorDetail != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorDetail ?: "",
                            style = DraftPeekTypography.bodySmall,
                            color = fgSoft,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pageBitmaps.isNotEmpty()) {
            // Use AndroidView with ScrollView + ImageView for PDF rendering
            // This avoids Compose canvas overhead for large bitmaps
            AndroidView(
                factory = { ctx ->
                    ScrollView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val container = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = android.view.Gravity.CENTER_HORIZONTAL
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPadding(0, 24, 0, 24)
                        }
                        pageBitmaps.forEach { bitmap ->
                            val imageView = ImageView(ctx).apply {
                                setImageBitmap(bitmap)
                                adjustViewBounds = true
                                layoutParams = LinearLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    bottomMargin = 16
                                }
                            }
                            container.addView(imageView)
                        }
                        addView(container)
                    }
                },
                update = { scrollView ->
                    // Update images if bitmaps change
                    val container = scrollView.getChildAt(0) as? LinearLayout
                    if (container != null && container.childCount != pageBitmaps.size) {
                        container.removeAllViews()
                        pageBitmaps.forEach { bitmap ->
                            val imageView = ImageView(scrollView.context).apply {
                                setImageBitmap(bitmap)
                                adjustViewBounds = true
                                layoutParams = LinearLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    bottomMargin = 16
                                }
                            }
                            container.addView(imageView)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = if (scale > 1f) {
                                Offset(
                                    offset.x + pan.x * scale,
                                    offset.y + pan.y * scale
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}
