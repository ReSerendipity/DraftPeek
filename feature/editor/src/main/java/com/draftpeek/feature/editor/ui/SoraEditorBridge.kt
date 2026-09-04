/**
 * sora-editor（View 体系）↔ Jetpack Compose 的统一桥接封装。
 *
 * ## 设计动机（评估报告 P0② 缺陷）
 * `EditorScreen` 中共有 3 处 [androidx.compose.ui.viewinterop.AndroidView] 各自内联了
 * **完全相同** 的 factory / update 逻辑：复用同一个 `wrapper.editor` 视图、设置 key listener、
 * MATCH_PARENT 布局、以及「首行可见行 > 5」的滚动阈值计算。重复代码带来维护负担与状态
 * 不一致风险（例如三处阈值逻辑今后若需调整必须同步修改三遍），且同一 View 在多个宿主页
 * 之间争用宿主时，主题与焦点状态缺少统一同步点。
 *
 * 本文件将这部分 View-Compose 互操作逻辑收敛到单点 [SoraEditorBridge]，统一负责：
 * 1. **复用挂载**：factory 内先 `(parent as? ViewGroup)?.removeView(this)`，保证同一
 *    sora 实例在 split / preview / 单栏三种槽位间切换时不被"双挂载"而丢失状态；
 * 2. **update 轻量化**：update 块只读取一个 Int（首行可见行）并写入布尔量，不做重活；
 * 3. **主题同步**：暗色开关变化时调用 `wrapper.setTheme(dark)`，并通过 [SoraBridgeState]
 *    做去重，避免每次重组都触发一次昂贵的重新着色；
 * 4. **IME 焦点**：可选的挂载后请求焦点并唤起软键盘。
 *
 * 调用方（EditorScreen）只需：
 * ```
 * SoraEditorBridge(
 *     wrapper = wrapper,
 *     shortcutHandler = shortcutHandler,
 *     darkTheme = darkTheme,
 *     errorLabel = "split",
 *     modifier = Modifier.fillMaxSize(),
 *     onScrollPastThresholdChanged = { hasScrolledPastThreshold = it }
 * )
 * ```
 *
 * 注：依据评估报告 Q2 结论，仅在本 editor 模块内部封装 `SoraEditorBridge`，
 * **不** 为 editor / terminal 建立统一的 View-Compose 桥接层（terminal 为纯 Compose，
 * 不存在互操作需求）。
 */
package com.draftpeek.feature.editor.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.draftpeek.feature.editor.input.KeyboardShortcutHandler
import com.draftpeek.feature.editor.sora.SoraEditorWrapper

private const val TAG = "SoraEditorBridge"

/**
 * 判定"已滚动超过阈值"的可见行号阈值（不含）。首行可见行 > [SCROLL_THRESHOLD_LINE] 时
 * 认为用户已向下滚动，用于决定是否展示「回到顶部」按钮。
 */
internal const val SCROLL_THRESHOLD_LINE = 5

/**
 * 桥接层在多次重组之间的可变状态容器。
 *
 * 使用普通类而非 Compose `mutableStateOf` 承载"上一次已应用的主题"是刻意为之：
 * `AndroidView` 的 `update` 块在组合阶段执行，在其中写入 Compose 状态会触发再次重组，
 * 形成"重组 → update → 写状态 → 重组"的回路。这里只需要一个跨重组存活的脏标记，
 * 用普通对象即可满足，且便于在无 Android 依赖的 JVM 单元测试中直接验证。
 */
internal class SoraBridgeState {

    /** 上一次实际应用到编辑器的暗色开关；`null` 表示尚未应用过。 */
    @Volatile
    var lastAppliedDark: Boolean? = null

    /** 上一次实际应用的焦点请求开关；`null` 表示尚未处理过。 */
    @Volatile
    var lastAppliedFocusRequest: Boolean? = null

    /**
     * 判断是否需要向编辑器重新下发主题。
     *
     * 返回 `true` 的同时会把 [dark] 记为已应用，保证同一暗色值只下发一次。
     */
    fun shouldApplyTheme(dark: Boolean): Boolean {
        val changed = lastAppliedDark != dark
        if (changed) lastAppliedDark = dark
        return changed
    }

    /** 判断是否需要在本次 update 中请求 IME 焦点（同样带去重）。 */
    fun shouldRequestFocus(requestFocus: Boolean): Boolean {
        val changed = lastAppliedFocusRequest != requestFocus
        if (changed) lastAppliedFocusRequest = requestFocus
        return changed && requestFocus
    }

    /** 编辑器实例被换掉（或宿主销毁重建）时重置脏标记，强制重新同步一次。 */
    fun reset() {
        lastAppliedDark = null
        lastAppliedFocusRequest = null
    }
}

/**
 * 将 sora 编辑器视图挂载进 Compose 树的统一封装。
 *
 * @param wrapper sora 编辑器封装，提供可复用的 [io.github.rosemoe.sora.widget.CodeEditor] 实例
 * @param shortcutHandler 键盘快捷键处理器（通过 `setOnKeyListener` 接入编辑器）
 * @param modifier 应用于 [AndroidView] 宿主的 Modifier（调用方通常传 `fillMaxSize()`）
 * @param darkTheme 当前是否暗色主题；变化时同步到编辑器配色方案
 * @param requestFocus 是否请求焦点并唤起软键盘（默认 false，避免预览态抢焦点）
 * @param errorLabel 日志标识，用于区分 split / preview / 单栏等不同槽位
 * @param onScrollPastThresholdChanged 当编辑器首行可见行 > [SCROLL_THRESHOLD_LINE] 的滚动状态变化时回调
 */
@Composable
fun SoraEditorBridge(
    wrapper: SoraEditorWrapper,
    shortcutHandler: KeyboardShortcutHandler,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    requestFocus: Boolean = false,
    errorLabel: String = SORA_SLOT_EDITOR,
    onScrollPastThresholdChanged: (Boolean) -> Unit = {}
) {
    if (!wrapper.isUsable()) return

    // 跨重组存活的状态容器；wrapper 变化时重建，保证新编辑器实例一定会被同步一次主题。
    val bridgeState = remember(wrapper) { SoraBridgeState() }

    // 用 rememberUpdatedState 捕获最新回调，避免 factory/update 闭包捕获到过期引用，
    // 同时不因回调变化而重启 AndroidView 的 factory。
    val currentShortcutHandler by rememberUpdatedState(shortcutHandler)
    val currentDarkTheme by rememberUpdatedState(darkTheme)
    val currentRequestFocus by rememberUpdatedState(requestFocus)
    val currentErrorLabel by rememberUpdatedState(errorLabel)
    val currentThresholdCallback by rememberUpdatedState(onScrollPastThresholdChanged)

    AndroidView(
        factory = { ctx ->
            mountSoraEditor(
                context = ctx,
                wrapper = wrapper,
                shortcutHandler = currentShortcutHandler,
                errorLabel = currentErrorLabel
            )
        },
        modifier = modifier,
        update = { view ->
            // ① 主题同步（带去重，避免每次重组都重新着色）
            if (bridgeState.shouldApplyTheme(currentDarkTheme)) {
                runCatching { wrapper.setTheme(currentDarkTheme) }
                    .onFailure { Log.w(TAG, "主题同步失败 [$currentErrorLabel]", it) }
            }

            // ② IME 焦点（仅在开关由 false→true 的首次触发）
            if (bridgeState.shouldRequestFocus(currentRequestFocus)) {
                runCatching { requestImeFocus(view) }
                    .onFailure { Log.w(TAG, "焦点请求失败 [$currentErrorLabel]", it) }
            }

            // ③ 滚动阈值——轻量读取，只写布尔量
            currentThresholdCallback(computeScrollPastThreshold(wrapper))
        }
    )
}

/** AndroidView factory 的默认槽位标识（单栏编辑态）。 */
internal const val SORA_SLOT_EDITOR = "editor"

/** 槽位标识：分栏 + 大文件流式预览。 */
internal const val SORA_SLOT_SPLIT_STREAM = "split-stream"

/** 槽位标识：分栏 + WebView 实时预览。 */
internal const val SORA_SLOT_SPLIT = "split"

/**
 * 执行实际的 View 挂载准备：解绑旧宿主 → 挂 key listener → 设置 MATCH_PARENT。
 *
 * 抽离为顶层纯函数，便于单元测试覆盖"复用同一 View"的核心契约。
 *
 * @return 挂载成功时返回编辑器视图本身；失败时返回兜底空 [View]，绝不返回 null，
 *         以保证 AndroidView 总能拿到一个合法 View（否则会抛出 IllegalStateException）。
 */
internal fun mountSoraEditor(
    context: Context,
    wrapper: SoraEditorWrapper,
    shortcutHandler: KeyboardShortcutHandler,
    errorLabel: String = SORA_SLOT_EDITOR
): View {
    return try {
        wrapper.editor.apply {
            // 关键：同一 sora 实例可能在 split/preview/单栏三个槽位间切换宿主，
            // 挂载前必须先从旧宿主解绑，否则会抛 "has already been added to a parent"。
            (parent as? ViewGroup)?.removeView(this)
            setOnKeyListener { _, _, event -> shortcutHandler.handleKeyEvent(event) }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "AndroidView factory configuration failed [$errorLabel]", e)
        try {
            wrapper.editor
        } catch (_: Exception) {
            View(context)
        }
    }
}

/**
 * 请求视图焦点并唤起软键盘（IME）。
 *
 * 仅当真正拿到焦点后才请求输入法，避免在视图不可聚焦时白白弹出键盘。
 */
internal fun requestImeFocus(view: View) {
    if (!view.requestFocus()) return
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * 计算编辑器是否已滚动超过阈值（首行可见行 > [SCROLL_THRESHOLD_LINE]）。
 *
 * 抽离为纯函数以便编写设备无关的单元测试（回归测试 [SoraEditorBridge] 的滚动状态逻辑），
 * 避免对 Android 设备 / 模拟器的依赖。任何异常一律降级为 `false`，
 * 保证桥接层不会因编辑器尚未就绪而崩溃。
 */
internal fun computeScrollPastThreshold(wrapper: SoraEditorWrapper): Boolean = try {
    wrapper.isUsable() && isScrolledPastThreshold(wrapper.editor.firstVisibleLine)
} catch (_: Exception) {
    false
}

/**
 * 阈值的纯判定逻辑（无 Android 依赖，可直接单测）。
 *
 * @param firstVisibleLine 编辑器当前首行可见行号（0-based）
 * @param isUsable 编辑器实例是否可用
 */
internal fun isScrolledPastThreshold(firstVisibleLine: Int, isUsable: Boolean = true): Boolean =
    isUsable && firstVisibleLine > SCROLL_THRESHOLD_LINE
