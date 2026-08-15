/**
 * 帧率监控工具模块。
 *
 * 基于Choreographer.FrameCallback计算实际渲染帧率，提供设备最高刷新率获取和高刷新率应用功能。
 * 使用滚动1秒窗口（最近60帧时间戳）计算FPS，仅在Debug构建下启用。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Choreographer
import android.view.Display

private const val TAG = "FpsMonitor"

/**
 * 帧率监控工具对象。
 *
 * 基于Choreographer.FrameCallback计算实际渲染帧率，并提供获取设备最高刷新率的工具方法。
 * 使用最近60个帧时间戳的滚动窗口估算FPS，避免瞬时波动。
 * 注意：仅在Debug构建下启用，Release构建应避免调用。
 */
object FpsMonitor {

    private val choreographer = Choreographer.getInstance()
    private var isMonitoring = false
    private var frameTimestamps = mutableListOf<Long>()
    private var lastFrameTimeNanos = 0L
    private var callback: ((fps: Float) -> Unit)? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return

            if (lastFrameTimeNanos != 0L) {
                frameTimestamps.add(frameTimeNanos)
                if (frameTimestamps.size > 61) {
                    frameTimestamps.removeAt(0)
                }

                if (frameTimestamps.size >= 2) {
                    val durationNanos =
                        frameTimestamps.last() - frameTimestamps.first()
                    if (durationNanos > 0) {
                        val fps =
                            (frameTimestamps.size - 1).toFloat() / (durationNanos / 1_000_000_000f)
                        callback?.invoke(fps)
                    }
                }
            }

            lastFrameTimeNanos = frameTimeNanos
            choreographer.postFrameCallback(this)
        }
    }

    /**
     * 开始帧率监控。
     *
     * @param onFpsUpdate 每帧回调，参数为当前估算帧率
     */
    fun start(onFpsUpdate: ((fps: Float) -> Unit)? = null) {
        if (isMonitoring) return
        isMonitoring = true
        frameTimestamps.clear()
        lastFrameTimeNanos = 0L
        callback = onFpsUpdate
        choreographer.postFrameCallback(frameCallback)
        Log.d(TAG, "FPS monitoring started")
    }

    /**
     * 停止帧率监控。
     */
    fun stop() {
        if (!isMonitoring) return
        isMonitoring = false
        choreographer.removeFrameCallback(frameCallback)
        frameTimestamps.clear()
        lastFrameTimeNanos = 0L
        callback = null
        Log.d(TAG, "FPS monitoring stopped")
    }

    /**
     * 获取设备支持的最高显示刷新率（Hz）。
     *
     * @param context 应用上下文
     * @return 最高刷新率，如果无法获取则返回60Hz
     */
    fun getMaxRefreshRate(context: Context): Float {
        // minSdk 26（M）起 getDisplay 一直可用
        val display = context.getSystemService(Context.DISPLAY_SERVICE)
            as? android.hardware.display.DisplayManager
            ?: return 60f
        val defaultDisplay = display.getDisplay(Display.DEFAULT_DISPLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val modes = defaultDisplay.supportedModes
            return modes.maxOfOrNull { it.refreshRate } ?: 60f
        }
        return defaultDisplay.refreshRate
    }

    /**
     * 获取设备最高刷新率对应的Display.Mode ID。
     *
     * 仅API 30+有效，低版本返回0。
     *
     * @param context 应用上下文
     * @return modeId，用于WindowManager.LayoutParams.preferredDisplayModeId
     */
    fun getPreferredDisplayModeId(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayManager =
                context.getSystemService(Context.DISPLAY_SERVICE)
                        as? android.hardware.display.DisplayManager
                    ?: return 0
            val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val modes = defaultDisplay.supportedModes
            val maxMode = modes.maxByOrNull { it.refreshRate }
            Log.d(TAG, "Preferred display mode: ${maxMode?.modeId}, refreshRate=${maxMode?.refreshRate}Hz")
            return maxMode?.modeId ?: 0
        }
        return 0
    }

    /**
     * 为Activity Window设置最高刷新率。
     *
     * 在API 30+设备上通过WindowManager.LayoutParams.preferredDisplayModeId
     * 请求系统将窗口分配到最高刷新率的显示模式。
     *
     * @param window 需要设置的Window
     * @param context 应用上下文
     */
    fun applyHighRefreshRate(window: android.view.Window, context: Context) {
        val modeId = getPreferredDisplayModeId(context)
        if (modeId != 0) {
            val attrs = window.attributes
            attrs.preferredDisplayModeId = modeId
            window.attributes = attrs
            Log.d(TAG, "Applied high refresh rate mode: $modeId")
        }
    }
}
