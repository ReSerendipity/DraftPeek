/**
 * DraftPeek CompositionLocal 提供器。
 *
 * 提供用于共享应用状态的 CompositionLocal，允许深层嵌套的 Composable 访问公共状态，
 * 而无需通过 Composable 层次结构中的每个组件逐层传递参数（prop drilling）。
 *
 * 这些 Local 在顶层（DraftPeekApp / MainActivity）提供，可在 Composable 树的任何位置消费。
 *
 * 包含的 CompositionLocal：
 * - [LocalFeatureToggle]：功能开关管理器
 * - [LocalEditorSettings]：编辑器设置
 * - [LocalIsLandscape]：是否横屏
 * - [LocalIsWideScreen]：是否宽屏设备
 * - [LocalFoldInfo]：折叠屏状态
 */
package com.draftpeek.core.ui.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.draftpeek.core.common.feature.FeatureFlag
import com.draftpeek.core.common.feature.FeatureToggleManager

/**
 * 功能开关管理器，在应用级别提供。
 * 允许任何 Composable 检查功能标志，无需直接进行 Hilt 注入。
 *
 * 用法：
 * `val featureToggle = LocalFeatureToggle.current`
 * `if (featureToggle.isEnabled(FeatureFlag.LSP_CLIENT)) { ... }`
 */
val LocalFeatureToggle = compositionLocalOf<FeatureToggleManager?> { null }

/**
 * 当前编辑器设置，在应用级别提供。
 *
 * 允许编辑器层次结构中的任何 Composable 读取设置（字体大小、制表符宽度、行号等），
 * 而无需通过调用链中的每个 Composable 作为显式参数接收。
 *
 * 由从 [com.draftpeek.feature.settings.viewmodel.SettingsViewModel] 收集设置的顶层 Composable 提供。
 *
 * 用法：
 * `val settings = LocalEditorSettings.current`
 * `Text(style = TextStyle(fontSize = settings.fontSize.sp))`
 */
val LocalEditorSettings = compositionLocalOf {
    EditorSettingsDefaults()
}

/**
 * 默认编辑器设置值，在未设置提供器时使用（例如在 @Preview 中）。
 * 镜像 EditorSettings 数据类的默认值。
 *
 * @param fontSize 字体大小（sp），默认为 16
 * @param tabWidth 制表符宽度（空格数），默认为 4
 * @param showLineNumbers 是否显示行号，默认为 true
 * @param lineWrapping 是否自动换行，默认为 false
 * @param autoIndent 是否自动缩进，默认为 true
 * @param highlightCurrentLine 是否高亮当前行，默认为 true
 * @param showIndentGuides 是否显示缩进参考线，默认为 false
 * @param stickyScroll 是否粘性滚动，默认为 true
 * @param showMinimap 是否显示小地图，默认为 false
 * @param autoPairCompletion 是否自动配对补全，默认为 true
 * @param autoSave 是否自动保存，默认为 false
 * @param defaultEncoding 默认文件编码，默认为 "UTF-8"
 */
data class EditorSettingsDefaults(
    val fontSize: Int = 16,
    val tabWidth: Int = 4,
    val showLineNumbers: Boolean = true,
    val lineWrapping: Boolean = false,
    val autoIndent: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val showIndentGuides: Boolean = false,
    val stickyScroll: Boolean = true,
    val showMinimap: Boolean = false,
    val autoPairCompletion: Boolean = true,
    val autoSave: Boolean = false,
    val defaultEncoding: String = "UTF-8",
)

/**
 * 设备是否处于横屏方向。
 * 由 Activity 根据配置更改更新。
 */
val LocalIsLandscape = compositionLocalOf { false }

/**
 * 设备是否具有宽屏（sw600dp+）。
 * 用于在单窗格和双窗格布局之间做决策。
 */
val LocalIsWideScreen = compositionLocalOf { false }

/**
 * 折叠设备的当前折叠状态。
 * 由 WindowSize/FoldableStateProvider 提供。
 */
val LocalFoldInfo = staticCompositionLocalOf { FoldInfo() }

/**
 * 表示折叠状态的简单数据类。
 * 为方便起见，从 core.ui.layout 重新导出。
 *
 * @param isFolded 是否处于折叠状态
 * @param isSeparating 折叠是否将屏幕物理分隔
 * @param foldPositionRatio 折叠位置比例（0.0-1.0）
 */
data class FoldInfo(
    val isFolded: Boolean = false,
    val isSeparating: Boolean = false,
    val foldPositionRatio: Float = 0.5f,
)

/**
 * 安全检查功能是否启用，当 [LocalFeatureToggle] 未提供时返回 false
 * （例如在预览中）。
 *
 * @param flag 要检查的功能标志
 * @return 功能是否启用
 */
@Composable
fun isFeatureEnabled(flag: FeatureFlag): Boolean {
    return LocalFeatureToggle.current?.isEnabled(flag) ?: flag.defaultEnabled
}
