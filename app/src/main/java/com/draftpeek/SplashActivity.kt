/**
 * DraftPeek 应用启动屏 Activity。
 *
 * **文件功能**：应用启动时的闪屏界面，显示品牌动画，同时异步检查是否首次启动，
 *               动画结束后根据首次启动状态跳转到引导页或主界面。
 *
 * **主要类/接口**：
 * - [SplashActivity] - 启动屏 Activity
 * - [SplashContent] - 闪屏内容 Compose 组件
 *
 * **模块依赖**：
 * - `core/common`：使用 [FpsMonitor] 配置高刷新率
 * - `core/ui`：使用 [DraftPeekTheme] 应用主题
 * - `onboarding`：使用 [SplashAnimation] 显示启动动画、[OnboardingActivity] 引导页
 * - Hilt 依赖注入（@AndroidEntryPoint）
 * - DataStore 读取首次启动标记
 *
 * @see ComponentActivity
 */
package com.draftpeek

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.draftpeek.core.common.util.FpsMonitor
import com.draftpeek.core.ui.theme.DraftPeekTheme
import com.draftpeek.onboarding.OnboardingActivity
import com.draftpeek.onboarding.SplashAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

/**
 * 应用启动屏 Activity。
 *
 * **职责**：
 * 1. 显示品牌启动动画（SplashAnimation）
 * 2. 异步读取 DataStore 判断是否为首次启动
 * 3. 配置导航栏纯色背景，避免系统级毛玻璃效果
 * 4. 动画结束后根据首次启动状态跳转到引导页或主界面
 * 5. 处理配置变更（旋转、折叠屏等），保持动画连续性
 *
 * **使用场景**：应用进程冷启动或任务栈被回收后重新启动时显示。
 *
 * @see AndroidEntryPoint
 */
@AndroidEntryPoint
@SuppressLint("CustomSplashScreen") // 有意设计：自定义启动动画 + 首启引导流程（SplashAnimation + Onboarding）
class SplashActivity : ComponentActivity() {
    /**
     * Activity 创建时调用，设置启动屏 UI。
     *
     * @param savedInstanceState 之前保存的实例状态
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FpsMonitor.applyHighRefreshRate(window, this)

        // 防止系统在导航栏区域应用模糊/毛玻璃效果（常见于荣耀/华为/小米定制ROM）
        // 设置与闪屏背景匹配的纯色，避免系统自动添加透明度+模糊导致视觉不统一
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.parseColor("#F5F7FA")

        setContent {
            DraftPeekTheme {
                SplashContent(
                    onAnimationComplete = { isFirstLaunch ->
                        val targetIntent = if (isFirstLaunch) {
                            Intent(this@SplashActivity, OnboardingActivity::class.java)
                        } else {
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }
                        startActivity(targetIntent)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            overrideActivityTransition(
                                Activity.OVERRIDE_TRANSITION_OPEN,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        }
                        finish()
                    }
                )
            }
        }
    }

    /**
     * 处理配置变更（旋转、折叠屏铰链、字体缩放、语言切换等）。
     *
     * 这些配置变更已在 AndroidManifest.xml 中声明，避免 Activity 被重建。
     * 如果不自行处理，在折叠屏展开过程中 Activity 会被重建，导致启动动画从头播放。
     * 自行处理可保持动画视觉连续性。
     *
     * @param newConfig 新的配置信息
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 空实现：Compose 会通过 LocalConfiguration 自动重组
        // 动画状态保存在 remember { Animatable(...) } 中，视觉上平滑连续
    }
}

/**
 * 闪屏内容 Compose 组件。
 *
 * 负责异步读取首次启动标记，等待数据加载和动画完成两个条件都满足后触发跳转回调。
 * 用户点击屏幕可跳过动画立即跳转。
 *
 * @param onAnimationComplete 动画完成且数据加载后的回调，参数 isFirstLaunch 表示是否首次启动
 */
@Composable
private fun SplashContent(onAnimationComplete: (isFirstLaunch: Boolean) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DraftPeekApp
    var isFirstLaunch by remember { mutableStateOf<Boolean?>(null) }
    var animationFinished by remember { mutableStateOf(false) }

    // Async read DataStore without blocking main thread
    LaunchedEffect(Unit) {
        val prefs = app.onboardingDataStore.data.first()
        isFirstLaunch = !(prefs[booleanPreferencesKey("completed")] ?: false)
    }

    // Navigate once both conditions are met: data loaded and animation finished
    LaunchedEffect(isFirstLaunch, animationFinished) {
        if (isFirstLaunch != null && animationFinished) {
            onAnimationComplete(isFirstLaunch!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SplashAnimation(
            // minDurationMs of 1200 keeps the brand mark on-screen long
            // enough to register on a fast-startup device, while still
            // respecting the user's tap to skip.
            onComplete = { animationFinished = true },
            modifier = Modifier
                .fillMaxSize()
                .clickable { animationFinished = true }
        )
    }
}
