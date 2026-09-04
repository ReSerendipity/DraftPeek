/**
 * 浮动终端窗口管理器文件。
 *
 * 使用Android WindowManager提供浮动终端覆盖层，允许用户在与应用其他部分交互时终端保持可见。
 * 支持拖拽移动、最小化到气泡、捏合缩放字体大小等功能。
 *
 * 安全注意：SYSTEM_ALERT_WINDOW权限必须由用户在系统设置中显式授予。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.draftpeek.core.ui.component.BrandIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.draftpeek.feature.terminal.R

/**
 * 浮动终端窗口管理器。
 *
 * 使用WindowManager TYPE_APPLICATION_OVERLAY创建悬浮窗口，
 * 需要SYSTEM_ALERT_WINDOW权限。
 *
 * 功能：
 * - 可拖拽浮动窗口
 * - 最小化为小图标气泡
 * - 边缘拖拽调整大小
 * - 关闭按钮
 * - 窗口位置/大小持久化
 * - 双指捏合缩放字体大小
 */
class FloatingTerminalManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isShowing = false
    private var isMinimized = false

    // 窗口位置和大小（通过SharedPreferences持久化）
    private var windowX = 100
    private var windowY = 100
    private var windowWidth = 800
    private var windowHeight = 600

    // 终端字体缩放（持久化）
    private var fontScale = 1.0f

    companion object {
        private const val TAG = "FloatingTerminal"
        private const val MIN_WIDTH = 400
        private const val MIN_HEIGHT = 300
        private const val BUBBLE_SIZE = 56 // dp

        // 窗口位置和大小持久化键
        private const val PREFS_NAME = "floating_terminal_prefs"
        private const val KEY_WINDOW_X = "window_x"
        private const val KEY_WINDOW_Y = "window_y"
        private const val KEY_WINDOW_WIDTH = "window_width"
        private const val KEY_WINDOW_HEIGHT = "window_height"
        private const val KEY_IS_MINIMIZED = "is_minimized"
        private const val KEY_FONT_SCALE = "font_scale"

        // 字体缩放限制
        private const val MIN_FONT_SCALE = 0.5f
        private const val MAX_FONT_SCALE = 3.0f
    }

    /** 获取当前字体缩放比例 */
    fun getFontScale(): Float = fontScale

    /**
     * 编程设置字体缩放比例。
     * @param scale 新的缩放比例，将被限制在[MIN_FONT_SCALE, MAX_FONT_SCALE]范围内
     */
    fun setFontScale(scale: Float) {
        fontScale = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        persistWindowState()
    }

    init {
        // 恢复持久化的窗口位置和大小
        restoreWindowState()
    }

    /**
     * 显示浮动终端窗口。
     *
     * 创建基于ComposeView的覆盖层并添加到WindowManager。
     * 如果已显示，将窗口置于前台。
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShowing) {
            bringToFront()
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val params = createLayoutParams()
        val composeView = createComposeView()

        // ScaleGestureDetector用于双指捏合缩放终端字体大小
        val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    fontScale = (fontScale * detector.scaleFactor).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
                    // 用新字体缩放重新渲染
                    try {
                        composeView.setContent {
                            FloatingTerminalContent(
                                isMinimized = isMinimized,
                                fontScale = fontScale,
                                onMinimize = { minimize() },
                                onRestore = { restore() },
                                onClose = { hide() }
                            )
                        }
                    } catch (_: Exception) { }
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    persistWindowState()
                }
            }
        )

        // 设置带手势检测的拖拽处理
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        composeView.setOnTouchListener { _, event ->
            // 先将所有触摸事件转发给缩放检测器
            scaleDetector.onTouchEvent(event)

            // 仅在非缩放时处理拖拽
            if (scaleDetector.isInProgress) {
                return@setOnTouchListener true
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(composeView, params)
                    windowX = params.x
                    windowY = params.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 拖拽后持久化窗口位置
                    persistWindowState()
                    false
                }
                else -> false
            }
        }

        try {
            wm.addView(composeView, params)
            floatingView = composeView
            isShowing = true
            Log.d(TAG, "浮动终端已显示")
        } catch (e: Exception) {
            Log.e(TAG, "显示浮动终端失败，是否缺少SYSTEM_ALERT_WINDOW权限？", e)
        }
    }

    /** 隐藏浮动终端窗口。 */
    fun hide() {
        if (!isShowing) return
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        floatingView = null
        isShowing = false
        isMinimized = false
        Log.d(TAG, "浮动终端已隐藏")
    }

    /** 将浮动窗口最小化为小气泡。 */
    fun minimize() {
        if (!isShowing || isMinimized) return
        isMinimized = true
        val params = createBubbleLayoutParams()
        try {
            floatingView?.let { windowManager?.updateViewLayout(it, params) }
            persistWindowState()
            Log.d(TAG, "浮动终端已最小化")
        } catch (e: Exception) {
            Log.e(TAG, "最小化浮动终端失败", e)
        }
    }

    /** 从最小化气泡恢复为完整窗口。 */
    fun restore() {
        if (!isShowing || !isMinimized) return
        isMinimized = false
        val params = createLayoutParams()
        try {
            floatingView?.let { windowManager?.updateViewLayout(it, params) }
            persistWindowState()
            Log.d(TAG, "浮动终端已恢复")
        } catch (e: Exception) {
            Log.e(TAG, "恢复浮动终端失败", e)
        }
    }

    /** 在最小化和恢复状态之间切换。 */
    fun toggleMinimize() {
        if (isMinimized) restore() else minimize()
    }

    /** 浮动窗口是否正在显示 */
    fun isShowing(): Boolean = isShowing

    /** 浮动窗口是否已最小化 */
    fun isMinimized(): Boolean = isMinimized

    private fun bringToFront() {
        if (!isShowing) return
        val params = if (isMinimized) createBubbleLayoutParams() else createLayoutParams()
        try {
            floatingView?.let { windowManager?.updateViewLayout(it, params) }
        } catch (_: Exception) {}
    }

    /** 创建完整窗口的布局参数 */
    private fun createLayoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        windowWidth.coerceAtLeast(MIN_WIDTH),
        windowHeight.coerceAtLeast(MIN_HEIGHT),
        // minSdk 26（O）起 TYPE_APPLICATION_OVERLAY 一直可用
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = windowX
        y = windowY
    }

    /** 创建最小化气泡的布局参数 */
    private fun createBubbleLayoutParams(): WindowManager.LayoutParams {
        val sizePx = (BUBBLE_SIZE * context.resources.displayMetrics.density).toInt()
        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            // minSdk 26（O）起 TYPE_APPLICATION_OVERLAY 一直可用
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX
            y = windowY
        }
    }

    @SuppressLint("SetWorldReadability")
    private fun createComposeView(): androidx.compose.ui.platform.ComposeView {
        val lifecycleOwner = FloatingLifecycleOwner()
        val composeView = androidx.compose.ui.platform.ComposeView(context)

        // 为Compose设置ViewTree生命周期和保存状态
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        composeView.setContent {
            FloatingTerminalContent(
                isMinimized = isMinimized,
                fontScale = fontScale,
                onMinimize = { minimize() },
                onRestore = { restore() },
                onClose = { hide() }
            )
        }

        return composeView
    }

    /**
     * 将当前窗口位置、大小和最小化状态持久化到SharedPreferences。
     * 在拖拽（ACTION_UP）和最小化/恢复操作后调用。
     */
    private fun persistWindowState() {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_WINDOW_X, windowX)
                .putInt(KEY_WINDOW_Y, windowY)
                .putInt(KEY_WINDOW_WIDTH, windowWidth)
                .putInt(KEY_WINDOW_HEIGHT, windowHeight)
                .putBoolean(KEY_IS_MINIMIZED, isMinimized)
                .putFloat(KEY_FONT_SCALE, fontScale)
                .apply()
            Log.d(TAG, "窗口状态已持久化: pos=($windowX,$windowY) size=${windowWidth}x$windowHeight fontScale=$fontScale")
        } catch (e: Exception) {
            Log.w(TAG, "持久化窗口状态失败", e)
        }
    }

    /**
     * 从SharedPreferences恢复窗口位置、大小和最小化状态。在init时调用。
     */
    private fun restoreWindowState() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            windowX = prefs.getInt(KEY_WINDOW_X, 100)
            windowY = prefs.getInt(KEY_WINDOW_Y, 100)
            windowWidth = prefs.getInt(KEY_WINDOW_WIDTH, 800)
            windowHeight = prefs.getInt(KEY_WINDOW_HEIGHT, 600)
            isMinimized = prefs.getBoolean(KEY_IS_MINIMIZED, false)
            fontScale = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
            Log.d(TAG, "窗口状态已恢复: pos=($windowX,$windowY) size=${windowWidth}x$windowHeight fontScale=$fontScale")
        } catch (e: Exception) {
            Log.w(TAG, "恢复窗口状态失败，使用默认值", e)
        }
    }

    /**
     * 浮动ComposeView的简单LifecycleOwner实现。
     */
    private class FloatingLifecycleOwner :
        LifecycleOwner,
        SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        init {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }
}

/**
 * 浮动终端窗口的Composable内容。
 *
 * @param isMinimized 是否最小化为气泡
 * @param fontScale 字体缩放比例（用于捏合缩放）
 * @param onMinimize 最小化回调
 * @param onRestore 恢复回调
 * @param onClose 关闭回调
 */
@Composable
private fun FloatingTerminalContent(
    isMinimized: Boolean,
    fontScale: Float = 1.0f,
    onMinimize: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    if (isMinimized) {
        // 气泡视图
        Surface(
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primary,
            onClick = onRestore
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = stringResource(R.string.terminal_restore),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    } else {
        // 完整浮动窗口
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        ) {
            Column {
                // 带拖拽手柄和控件的标题栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.terminal_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        BrandIconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Minimize,
                                contentDescription = stringResource(R.string.terminal_minimize),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        BrandIconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.terminal_floating_close),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 终端内容区域，应用字体缩放
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$ ",
                        color = Color.Green,
                        fontSize = (13f * fontScale).sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}
