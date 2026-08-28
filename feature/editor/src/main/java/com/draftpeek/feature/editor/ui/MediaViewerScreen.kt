/**
 * 媒体文件查看器组件。
 *
 * 支持图片（双指缩放/平移）、音频（播放/暂停/进度条）、视频（VideoView + MediaController）
 * 三种媒体类型的预览播放。自动根据 DocumentType 选择对应的查看器。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor.ui

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.draftpeek.core.common.util.DocumentType
import com.draftpeek.core.ui.theme.DraftPeekTypography
import com.draftpeek.core.ui.theme.PrototypeShapes
import com.draftpeek.core.ui.theme.PrototypeTokens
import com.draftpeek.feature.editor.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val TAG = "MediaViewer"

/**
 * 媒体文件查看器 Composable。
 *
 * 根据文档类型自动分发到对应的查看器：图片查看器、音频播放器、视频播放器。
 *
 * @param fileUri 媒体文件 URI
 * @param documentType 文档类型（IMAGE/AUDIO/VIDEO）
 * @param modifier 修饰符
 */
@Composable
fun MediaViewerScreen(fileUri: Uri, documentType: DocumentType, modifier: Modifier = Modifier) {
    val pageBg = PrototypeTokens.pageBackground
    val fgSoft = PrototypeTokens.fgSoft

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        when (documentType) {
            DocumentType.IMAGE -> ImageViewer(fileUri = fileUri, modifier = Modifier.fillMaxSize())
            DocumentType.AUDIO -> AudioPlayer(fileUri = fileUri, modifier = Modifier.fillMaxSize())
            DocumentType.VIDEO -> VideoPlayer(fileUri = fileUri, modifier = Modifier.fillMaxSize())
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.editor_unsupported_media),
                        style = DraftPeekTypography.bodyLarge,
                        color = fgSoft
                    )
                }
            }
        }
    }
}

/**
 * 图片查看器 Composable。
 *
 * 使用 Coil 异步加载图片，支持双指缩放和平移手势，支持从 assets、file://、content:// URI 加载。
 * Coil 自动管理内存/磁盘缓存，生命周期感知，无需手动释放 Bitmap。
 * 包含加载状态和错误状态显示。
 *
 * @param fileUri 图片文件 URI
 * @param modifier 修饰符
 */
@Composable
private fun ImageViewer(fileUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var loadError by remember { mutableStateOf<String?>(null) }

    val errorColor = PrototypeTokens.error
    val fgSoft = PrototypeTokens.fgSoft

    val imageRequest = remember(fileUri) {
        ImageRequest.Builder(context)
            .data(fileUri)
            .crossfade(true)
            .build()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.editor_image_preview),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y
                        )
                    }
                },
            onError = { state ->
                loadError = state.result.throwable?.message
                    ?: context.getString(R.string.editor_image_load_failed)
            }
        )

        if (loadError != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = errorColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.editor_image_load_failed),
                    style = DraftPeekTypography.bodyLarge,
                    color = errorColor
                )
                Text(
                    text = loadError ?: "",
                    style = DraftPeekTypography.bodySmall,
                    color = fgSoft,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 音频播放器 Composable。
 *
 * 使用 Android MediaPlayer 播放音频文件，显示文件名、进度条、播放/暂停按钮、当前/总时长。
 * 支持从 assets、file://、content:// URI 加载音频。组件销毁时自动释放 MediaPlayer。
 *
 * @param fileUri 音频文件 URI
 * @param modifier 修饰符
 */
@Composable
private fun AudioPlayer(fileUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val surface = PrototypeTokens.surface
    val border = PrototypeTokens.border
    val fg = PrototypeTokens.fg
    val fgSoft = PrototypeTokens.fgSoft
    val accent = PrototypeTokens.accent
    val errorColor = PrototypeTokens.error
    val onPrimary = PrototypeTokens.surface

    LaunchedEffect(fileUri) {
        try {
            val player = MediaPlayer().apply {
                setOnPreparedListener { mp ->
                    duration = mp.duration.toLong()
                    isPlaying = true
                    mp.start()
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, what, extra ->
                    loadError = context.getString(R.string.editor_player_error, what, extra)
                    true
                }
            }
            withContext(Dispatchers.IO) {
                val resolvedUri = fileUri.toString()
                when {
                    resolvedUri.startsWith("file:///android_asset/") -> {
                        val assetPath = resolvedUri.removePrefix("file:///android_asset/")
                        val fd = context.assets.openFd(assetPath)
                        player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                    resolvedUri.startsWith("file://") -> {
                        player.setDataSource(context, fileUri)
                    }
                    else -> {
                        player.setDataSource(context, fileUri)
                    }
                }
                player.prepare()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            loadError = e.message ?: context.getString(R.string.editor_cannot_load_audio)
        }
    }

    LaunchedEffect(mediaPlayer) {
        while (isActive && mediaPlayer != null) {
            try {
                mediaPlayer?.let { currentPosition = it.currentPosition.toLong() }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to poll media player position", e)
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        }
    }

    if (loadError != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = errorColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.editor_audio_load_failed),
                    style = DraftPeekTypography.bodyLarge,
                    color = errorColor
                )
                Text(
                    text = loadError ?: "",
                    style = DraftPeekTypography.bodySmall,
                    color = fgSoft
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .clip(PrototypeShapes.Card)
                .border(1.dp, border, PrototypeShapes.Card)
                .background(surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = accent
                )

                Spacer(modifier = Modifier.height(24.dp))

                val fileName =
                    fileUri.lastPathSegment?.substringAfterLast('/') ?: stringResource(R.string.editor_audio_file)
                Text(
                    text = fileName,
                    style = DraftPeekTypography.titleMedium.copy(color = fg)
                )

                Spacer(modifier = Modifier.height(24.dp))

                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                    trackColor = border
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = DraftPeekTypography.bodySmall,
                        color = fgSoft
                    )
                    Text(
                        text = formatDuration(duration),
                        style = DraftPeekTypography.bodySmall,
                        color = fgSoft
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable {
                            val player = mediaPlayer ?: return@clickable
                            if (isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(32.dp),
                        tint = onPrimary
                    )
                }
            }
        }
    }
}

/**
 * 视频播放器 Composable。
 *
 * 使用 Android VideoView + MediaController 播放视频文件，支持播放控制。
 * 组件销毁时自动停止播放。
 *
 * @param fileUri 视频文件 URI
 * @param modifier 修饰符
 */
@Composable
private fun VideoPlayer(fileUri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var loadError by remember { mutableStateOf<String?>(null) }

    val errorColor = PrototypeTokens.error
    val fgSoft = PrototypeTokens.fgSoft

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (loadError != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = errorColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "视频加载失败",
                    style = DraftPeekTypography.bodyLarge,
                    color = errorColor
                )
                Text(
                    text = loadError ?: "",
                    style = DraftPeekTypography.bodySmall,
                    color = fgSoft
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mediaController = MediaController(ctx)
                        setMediaController(mediaController)
                        mediaController.setAnchorView(this)

                        try {
                            val resolvedUri = fileUri.toString()
                            when {
                                resolvedUri.startsWith("file:///android_asset/") -> {
                                    val assetPath = resolvedUri.removePrefix("file:///android_asset/")
                                    val cacheFile =
                                        File(context.cacheDir, "temp_video_${assetPath.substringAfterLast('/')}")
                                    if (!cacheFile.exists()) {
                                        context.assets.open(assetPath).use { input ->
                                            cacheFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                    setVideoURI(Uri.fromFile(cacheFile))
                                }
                                else -> {
                                    setVideoURI(fileUri)
                                }
                            }
                            start()
                        } catch (e: Exception) {
                            loadError = e.message ?: "无法加载视频"
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 将毫秒时长格式化为 MM:SS 字符串。
 *
 * @param ms 时长（毫秒）
 * @return 格式化后的时间字符串，如 "03:45"
 */
private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
