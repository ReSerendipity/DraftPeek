/**
 * 撰码轻览 (DraftPeek) 编辑器设置数据模型。
 *
 * 本文件定义了应用的所有可配置项，包括编辑器外观、行为、无障碍功能、网络设置等。
 * 所有设置通过 [com.draftpeek.feature.settings.repository.SettingsRepository] 持久化到
 * Jetpack DataStore (Preferences)，应用重启后自动恢复。
 *
 * 模块：feature/settings
 * 依赖：core/ui (主题、颜色、无障碍相关枚举)
 */
package com.draftpeek.feature.settings.model

import androidx.compose.runtime.Immutable
import com.draftpeek.core.designsystem.theme.ColorBlindMode
import com.draftpeek.core.designsystem.theme.RainbowColor

/**
 * 编辑器全局配置数据类。
 *
 * 这是一个不可变（@Immutable）的数据类，所有字段都有默认值，首次启动时使用默认配置。
 * 通过 [com.draftpeek.feature.settings.viewmodel.SettingsViewModel] 暴露为 StateFlow，
 * UI 层可通过 collectAsStateWithLifecycle() 观察设置变化。
 *
 * 新增设置项请遵循以下步骤：
 * 1. 在此处添加字段和默认值
 * 2. 在 SettingsRepository 接口添加对应 get/set 方法
 * 3. 在 SettingsRepositoryImpl 中实现 DataStore 读写逻辑
 * 4. 在 SettingsViewModel 中添加更新方法
 * 5. 在 ProfileScreen 添加 UI 控件
 */
@Immutable
data class EditorSettings(
    /** 编辑器字体大小（单位：sp），取值范围 10-24，默认 16 */
    val fontSize: Int = 16,

    /** 应用主题模式：浅色(LIGHT)、深色(DARK)、跟随系统(SYSTEM) */
    val theme: AppTheme = AppTheme.SYSTEM,

    /** 是否启用自动换行，开启后长行自动折行显示，默认关闭 */
    val lineWrapping: Boolean = false,

    /** 是否显示行号，默认开启 */
    val showLineNumbers: Boolean = true,

    /** Tab 键对应的空格数，默认 4 */
    val tabWidth: Int = 4,

    /** 是否启用自动缩进，换行时自动保持上一行缩进级别，默认开启 */
    val autoIndent: Boolean = true,

    /** 是否高亮当前光标所在行，默认开启 */
    val highlightCurrentLine: Boolean = true,

    /** 是否显示缩进参考线（竖线对齐缩进层级），默认关闭 */
    val showIndentGuides: Boolean = false,

    /** 文件默认编码，默认 UTF-8，可在保存时选择其他编码 */
    val defaultEncoding: String = "UTF-8",

    /** 是否启用自动保存，开启后按 [autoSaveIntervalMs] 间隔自动保存文件，默认关闭 */
    val autoSave: Boolean = false,

    /** 自动保存间隔时间（单位：毫秒），默认 30000ms（30秒） */
    val autoSaveIntervalMs: Long = 30000,

    /** @deprecated 已废弃，请使用 [codeFontFamilyId] 和 [uiFontFamilyId] 替代，保留用于旧版本迁移 */
    @Deprecated("Use codeFontFamilyId and uiFontFamilyId instead")
    val fontFamily: String = "monospace",

    /** 代码编辑器字体 ID，对应可加载的字体资源，默认 jetbrains_mono */
    val codeFontFamilyId: String = "jetbrains_mono",

    /** UI 界面字体 ID，对应可加载的字体资源，默认 inter */
    val uiFontFamilyId: String = "inter",

    /** 应用界面语言：跟随系统(SYSTEM)、中文(ZH)、英文(EN)、日文(JA)、韩文(KO) */
    val language: AppLanguage = AppLanguage.SYSTEM,

    /** 活动日历热力图的颜色主题，默认红色 */
    val activityColor: RainbowColor = RainbowColor.RED,

    /** 用户显示名称，默认 "用户" */
    val userName: String = "用户",

    /** 用户头像 URI 字符串，为空时使用默认头像 */
    val userAvatarUri: String = "",

    // ===== 首页显示设置 =====

    /** 首页"最近打开"区块显示的文件数量，默认 3 */
    val recentFilesLimit: Int = 3,

    // ===== 无障碍功能设置 =====

    /** 色盲模式：NONE(关闭)/PROTANOPIA(红色盲)/DEUTERANOPIA(绿色盲)/TRITANOPIA(蓝色盲)，默认关闭 */
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,

    /** 是否启用高对比度模式，增强文字与背景对比度，默认关闭 */
    val highContrastMode: Boolean = false,

    /** UI 文字缩放倍数，取值范围 0.85-1.5，默认 1.0（不缩放） */
    val textScale: Float = 1.0f,

    /** 是否启用屏幕阅读器优化，为 TalkBack 等服务提供增强语义信息，默认关闭 */
    val screenReaderOptimized: Boolean = false,

    /** 是否启用振动反馈，操作时触发触觉反馈，默认关闭 */
    val vibrationFeedback: Boolean = false,

    /** 是否启用非色彩标识，为依赖颜色区分的 UI 元素添加图标/形状/文本标签，默认关闭 */
    val nonColorIndicators: Boolean = false,

    /** 是否启用粘性滚动，将当前作用域起始行固定在编辑器顶部，默认开启 */
    val stickyScroll: Boolean = true,

    /** 是否显示缩略图（minimap），sora-editor 实验性功能，默认关闭 */
    val showMinimap: Boolean = false,

    /** 是否启用自动配对补全，输入开括号/引号时自动插入闭括号/引号，默认开启 */
    val autoPairCompletion: Boolean = true,

    // ===== 网络设置 =====

    /** GitHub 镜像 URL 前缀，为空则直接访问 github.com，用于加速 GitHub 资源访问 */
    val githubMirrorUrl: String = "",

    // ===== 编辑器主题设置 =====

    /** 编辑器代码高亮主题 ID，为空则跟随系统深浅色自动选择对应默认主题 */
    val editorThemeId: String = "",

    // ===== Markdown 预览设置 =====

    /** Markdown 预览主题名称，对应 MarkdownTheme 枚举：academic/github/newsprint/night/pixy/DEFAULT，默认 DEFAULT */
    val markdownThemeName: String = "DEFAULT",

    /** 自定义 Markdown 预览 CSS 样式，为空则不注入，用于自定义预览外观 */
    val customMarkdownCss: String = ""
)

/**
 * 应用主题枚举。
 *
 * - [LIGHT]: 强制浅色主题
 * - [DARK]: 强制深色主题
 * - [SYSTEM]: 跟随系统设置自动切换
 */
enum class AppTheme { LIGHT, DARK, SYSTEM }

/**
 * 应用界面语言枚举。
 *
 * @property code 语言代码，用于 AppCompatDelegate.setApplicationLocales()
 */
enum class AppLanguage(val code: String) {
    /** 跟随系统语言设置 */
    SYSTEM(""),

    /** 简体中文 */
    ZH("zh"),

    /** 繁体中文 */
    ZH_TW("zh-rTW"),

    /** 英文 */
    EN("en"),

    /** 日文 */
    JA("ja"),

    /** 韩文 */
    KO("ko");

    companion object {
        /**
         * 根据语言代码查找对应的枚举值，找不到则返回 [SYSTEM]。
         *
         * @param code 语言代码字符串
         * @return 对应的 [AppLanguage] 枚举值
         */
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: SYSTEM
    }
}
