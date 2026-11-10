package com.draftpeek.feature.browser.model

/**
 * SAF 目录树访问状态，与 [BrowserUiState] 由同一处（FileBrowserViewModel）持有。
 *
 * 拆分动机（WO-B22「文件页列表恒空」）：旧实现把「是否已授权」放在
 * `FileBrowserScreen` 的 `rememberSaveable` 里，靠组合期副作用置真，与 `uiState`
 * 的发布时机彼此独立 —— 授权后第一次 `listFiles` 若返回空列表，界面就会把
 * 「未授权 / 未加载完」渲染成「暂无文件」。
 *
 * 三态语义固定如下：
 * - [NONE]：没有已授权的目录树。列表区应走首页（内部存储 + 授权引导卡）；
 *   该状态下不允许出现 `BrowserUiState.Success(emptyList())`。
 * - [PENDING]：用户已选定目录树，正在取持久授权并做首次加载。
 * - [GRANTED]：授权已落地；此后 `BrowserUiState.Success` 中的空列表才表示
 *   「目录确实为空」（已加载空），而不是「未授权空」。
 */
enum class SafAccessState {
    NONE,
    PENDING,
    GRANTED
}
