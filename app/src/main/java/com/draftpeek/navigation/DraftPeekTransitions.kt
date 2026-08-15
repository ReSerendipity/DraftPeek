/**
 * DraftPeek 应用导航转场动画定义。
 *
 * **文件功能**：集中定义各页面之间切换时的进入/退出动画，统一应用内导航视觉风格。
 *
 * **主要类/接口**：[DraftPeekTransitions] - 单例对象，包含所有转场动画常量。
 *
 * **模块依赖**：
 * - Jetpack Compose Animation：使用 fadeIn/fadeOut/slideIn/slideOut 等动画 API
 * - Navigation Compose：适配 [NavBackStackEntry] 作为动画作用域接收者
 *
 * 动画风格参考 Material Design 共享轴（Shared Axis）和容器变换模式。
 */
package com.draftpeek.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * DraftPeek 导航转场动画集合。
 *
 * 提供三类动画：
 * 1. 同级底部导航标签切换：使用轻量级淡入淡出，无位移
 * 2. 父子层级下钻/全屏详情：使用水平共享轴（从右侧滑入）
 * 3. 模态/辅助页面：使用垂直共享轴（从底部滑入，类似 BottomSheet）
 *
 * 所有动画使用 Material Design 标准缓动曲线：
 * - 进入：FastOutSlowInEasing（减速曲线）
 * - 退出：FastOutLinearInEasing（加速曲线）
 */
object DraftPeekTransitions {
    /** 同级底部导航标签：进入淡入动画，时长 150ms */
    val topLevelEnter: EnterTransition = fadeIn(tween(150, easing = FastOutSlowInEasing))

    /** 同级底部导航标签：退出淡出动画，时长 150ms */
    val topLevelExit: ExitTransition = fadeOut(tween(150, easing = FastOutLinearInEasing))

    /**
     * 水平共享轴：进入动画（目标页从右侧 30% 位置滑入，同时淡入）。
     * 适用于下钻到详情页、打开编辑器等正向导航。
     * 时长：滑动 250ms + 淡入 200ms
     */
    val sharedAxisHorizontalEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth * 30 / 100 },
            animationSpec = tween(250, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(200, easing = FastOutSlowInEasing))
    }

    /**
     * 水平共享轴：退出动画（当前页向左侧 15% 位置滑出，同时淡出）。
     * 配合下钻动画，当前页轻微后退。
     * 时长：滑动 150ms + 淡出 150ms
     */
    val sharedAxisHorizontalExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth * 15 / 100 },
            animationSpec = tween(150, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(150, easing = FastOutLinearInEasing))
    }

    /**
     * 水平共享轴：返回时的进入动画（上一级从左侧 15% 位置滑回）。
     * 用户按下返回键时使用，与下钻动画方向相反。
     * 时长：滑动 250ms + 淡入 200ms
     */
    val sharedAxisHorizontalPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth * 15 / 100 },
            animationSpec = tween(250, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(200, easing = FastOutSlowInEasing))
    }

    /**
     * 水平共享轴：返回时的退出动画（当前页向右侧 30% 位置滑出）。
     * 用户按下返回键时使用。
     * 时长：滑动 150ms + 淡出 150ms
     */
    val sharedAxisHorizontalPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth * 30 / 100 },
            animationSpec = tween(150, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(150, easing = FastOutLinearInEasing))
    }

    /**
     * 垂直共享轴：进入动画（模态页从底部 20% 位置滑入，类似 BottomSheet）。
     * 适用于示例文件、代码片段、历史记录、无障碍设置等辅助页面。
     * 时长：滑动 220ms + 淡入 180ms
     */
    val sharedAxisVerticalEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight * 20 / 100 },
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(180, easing = FastOutSlowInEasing))
    }

    /**
     * 垂直共享轴：退出动画（当前页向上侧 10% 位置轻微滑出）。
     * 打开模态页时，底层页面轻微向上移动。
     * 时长：滑动 180ms + 淡出 150ms
     */
    val sharedAxisVerticalExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight * 10 / 100 },
            animationSpec = tween(180, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(150, easing = FastOutLinearInEasing))
    }

    /**
     * 垂直共享轴：返回时的进入动画（底层页面淡入）。
     * 关闭模态页时，底层页面不需要滑动，仅淡入即可。
     * 时长：淡入 180ms
     */
    val sharedAxisVerticalPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(180, easing = FastOutSlowInEasing))
    }

    /**
     * 垂直共享轴：返回时的退出动画（模态页向底部 25% 位置滑出）。
     * 关闭模态页时使用。
     * 时长：滑动 180ms + 淡出 150ms
     */
    val sharedAxisVerticalPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight * 25 / 100 },
            animationSpec = tween(180, easing = FastOutLinearInEasing),
        ) + fadeOut(tween(150, easing = FastOutLinearInEasing))
    }
}
