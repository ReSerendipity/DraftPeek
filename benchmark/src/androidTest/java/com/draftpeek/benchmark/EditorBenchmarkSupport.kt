/**
 * 编辑器 Macrobenchmark 公共脚手架。
 *
 * 提供 [EditorInputBenchmark] 与 [EditorScrollBenchmark] 共用的定位与投喂能力。
 *
 * ## 为什么按「类名」而不是按「文案」定位编辑器
 * DraftPeek 的编辑器是 sora-editor 的 [io.github.rosemoe.sora.widget.CodeEditor]，
 * 它是一个**真实的 Android View**（经 `AndroidView` 宿主接入 Compose），
 * 因此 UiAutomator 可以直接按类名命中，不受系统语言、字号、主题影响。
 *
 * 反之，纯 Compose 节点只能借助无障碍语义树（文案 / contentDescription）定位，
 * 而这些字符串会随系统语言漂移——本项目支持 zh/en/ja/ko 四语言，
 * 所以凡是必须按文案查找的地方，都改成遍历 [NEW_FILE_LABELS] 四语言候选。
 *
 * ## 运行前提（CI 与本地一致）
 * 1. 安装 production release 变体的应用（CI 由 `android.yml` 的 Macrobenchmark job
 *    `./gradlew :app:assembleRelease` + 单行 `adb install` 完成；`:app` 没有名为
 *    `benchmark` 的构建类型，旧文档里的 `:app:assembleBenchmark` 是不存在的任务）；
 * 2. 设备已解锁、屏幕常亮、已禁用动画（Macrobenchmark 会自行处理大部分）；
 * 3. 无需人工预备引导页 —— 首屏被引导页挡住时由 [ensureEditorOpen] 显式直启
 *    MainActivity 绕开（见其中注释）。直启失败仍以明确错误失败并附 `am start`
 *    原文与当前焦点组件，**不会静默通过**。
 */
package com.draftpeek.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** 被测应用包名，与 `app/build.gradle.kts` 的 applicationId 保持一致。 */
internal const val TARGET_PACKAGE = "com.draftpeek"

/** sora-editor 视图类全名。 */
internal const val SORA_EDITOR_CLASS = "io.github.rosemoe.sora.widget.CodeEditor"

/** 查找 UI 元素的超时时间（毫秒）。 */
internal const val FIND_OBJECT_TIMEOUT_MS = 15_000L

/** 单次测量中连续输入的字符数，模拟一次中等长度的连续键入。 */
internal const val INPUT_BURST = "fun benchmarkFrameTimingUnderTyping(): Int = 42\n"

/** 「新建文件」在四语言下的文案，用于跨语言定位 FAB 菜单项。 */
internal val NEW_FILE_LABELS = listOf("新建文件", "New file", "新規ファイル", "새 파일")

/**
 * 等待编辑器出现并返回它。
 *
 * @throws IllegalStateException 超时未找到时抛出，附可操作的排查指引
 */
internal fun UiDevice.requireEditor(): UiObject2 {
    wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), FIND_OBJECT_TIMEOUT_MS)
    return findObject(By.clazz(SORA_EDITOR_CLASS))
        ?: error(
            "在 ${FIND_OBJECT_TIMEOUT_MS}ms 内未找到 $SORA_EDITOR_CLASS。请确认：\n" +
                "1) 安装的是 production release 包（CI 由 android.yml 的 Macrobenchmark job 装）；\n" +
                "2) 设备已解锁且在桌面无锁屏遮挡；\n" +
                "3) 「新建文件」入口能点进编辑器（它是 FileBrowserScreen.kt:3056 起的 " +
                "speed-dial 菜单项，折叠态下按文案找不到）。"
        )
}

/**
 * 确保界面已进入编辑器；若停在文件浏览器，则通过 FAB 新建一个文件。
 *
 * 之所以要兼容「启动后已在编辑器」的情况：Macrobenchmark 以 WARM 模式重启 Activity，
 * 系统可能恢复到上次离开时的界面。
 */
internal fun UiDevice.ensureEditorOpen() {
    if (wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), 3_000L)) return

    // 全新安装的首屏是引导页：路由只在 SplashActivity 上做拦截
    // （SplashActivity.kt:142-143 读 DataStore onboarding/completed 决定去
    // OnboardingActivity 还是 MainActivity，:87-89 执行跳转），MainActivity 自身不拦。
    // 所以显式直启 MainActivity 即可绕开引导，既不用点 UI 也不用伪造 protobuf 文件。
    val launchOutput = launchMainActivityDirectly()

    val newFile = NEW_FILE_LABELS.firstNotNullOfOrNull { label -> findByTextOrDesc(label) }
        ?: error(diagnoseInaccessible(launchOutput))

    newFile.click()
    waitForIdle()
    requireEditor()
}

/**
 * 显式启动 `com.draftpeek/com.draftpeek.MainActivity`，返回 `am start` 的原始输出。
 *
 * 依赖 CI 镜像是 `google_apis`（userdebug，shell 持有 `START_ANY_ACTIVITY`）才能启动
 * 非 exported 的 Activity；这一前提若被破坏，[diagnoseInaccessible] 会把 `am start`
 * 的拒绝原文连同当前 focus 组件一起报出来，不静默降级。
 */
private fun UiDevice.launchMainActivityDirectly(): String = try {
    executeShellCommand("am start -n $TARGET_PACKAGE/.MainActivity")
} catch (t: Throwable) {
    "executeShellCommand 异常: ${t.message}"
}

/**
 * 组装失败诊断：`am start` 原文 + 当前 focus/ resumed 组件。
 *
 * 有了这两项就能一步区分三种可能：Permission Denial（镜像不满足前提）、
 * 已进 MainActivity 但「新建文件」藏在折叠的 speed-dial FAB 里
 * （FileBrowserScreen.kt:3056 起是 expandable speed-dial 菜单）、以及仍停在引导页。
 */
private fun UiDevice.diagnoseInaccessible(launchOutput: String): String =
    "既未发现编辑器，也未找到「新建文件」入口（已尝试 ${NEW_FILE_LABELS.joinToString()}）。\n" +
        "直启输出: ${launchOutput.replace("\n", " ").trim().take(300)}\n" +
        "窗口焦点: ${focusedWindow()}\n" +
        "判读: 输出含 Permission Denial ⇒ 镜像 shell 无权启动非 exported 组件；" +
        "焦点已是 $TARGET_PACKAGE/.MainActivity ⇒ 入口被折叠在 speed-dial FAB 内，需先展开；" +
        "焦点是 OnboardingActivity ⇒ 仍被引导页挡住，直启未生效。"

private fun UiDevice.focusedWindow(): String = try {
    executeShellCommand("dumpsys window windows")
        .lines()
        .filter { "mCurrentFocus" in it || "mFocusedApp" in it }
        .joinToString(" | ")
        .trim()
        .take(240)
        .ifBlank { "(dumpsys window 无 mCurrentFocus 行)" }
} catch (t: Throwable) {
    "dumpsys 异常: ${t.message}"
}

/**
 * 按文案或内容描述查找元素。
 *
 * Compose 节点的可见名称可能落在 `text` 也可能落在 `content-description` 上
 * （取决于是否设置了 `contentDescription`），因此两者都要试。
 * 注意 [BySelector] 没有 `or()` 方法，只能分别查询。
 *
 * @param label 目标文案，等待超时为 2 秒
 * @return 命中的元素，未命中返回 null
 */
internal fun UiDevice.findByTextOrDesc(label: String): UiObject2? {
    wait(Until.hasObject(By.text(label)), 2_000L)
    return findObject(By.text(label)) ?: findObject(By.desc(label))
}

/**
 * 向当前获得焦点的视图发送整段文本。
 *
 * 走 `Instrumentation.sendStringSync` 而不是逐次 `pressKeyCode`：
 * 后者每次按键都是一次跨进程调用，在 40 字符量级上会引入数十毫秒的测试框架噪声，
 * 污染 FrameTimingMetric 的读数。
 */
internal fun sendText(text: String) {
    InstrumentationRegistry.getInstrumentation().sendStringSync(text)
}

/**
 * 生成用于滚动基准的样本文档。
 *
 * @param lineCount 行数，默认 120 行，足以让编辑器产生多屏可滚动内容
 * @return 组装好的 Markdown 文本
 */
internal fun seedDocument(lineCount: Int = 120): String = buildString {
    repeat(lineCount) { index ->
        append("## Section ${index + 1}\n")
        append("DraftPeek scroll benchmark line ${index + 1}: some prose long enough to wrap.\n")
    }
}
