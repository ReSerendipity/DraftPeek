/**
 * 功能开关枚举模块。
 *
 * 定义DraftPeek的所有功能开关，每个开关控制运行时是否启用某个功能。
 * 开关状态通过[FeatureToggleManager]持久化在SharedPreferences中，支持运行时动态切换。
 *
 * ## Kill Switch 机制
 *
 * 带有 `killSwitch = true` 的功能开关可通过 BuildConfig 或远程配置强制禁用，
 * 用于发布后发现严重问题时快速回滚特定功能模块，无需发布新版本。
 *
 * 使用方式：
 * 1. 在 `gradle.properties` 中设置 `draftpeek.killswitch.{key}=true`
 * 2. BuildConfig 注入后通过 `FeatureToggleManager.applyKillSwitches()` 生效
 * 3. 用户侧功能立即禁用，UI 隐藏对应入口
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.feature

/**
 * 功能开关枚举类。
 *
 * 每个枚举常量定义一个功能开关，包含持久化键名、默认启用状态和描述信息。
 *
 * @property key SharedPreferences中使用的键名
 * @property defaultEnabled 默认是否启用
 * @property description 功能描述
 * @property killSwitch 是否支持 Kill Switch 远程强制禁用
 */
enum class FeatureFlag(
    val key: String,
    val defaultEnabled: Boolean,
    val description: String,
    val killSwitch: Boolean = false
) {
    LSP_CLIENT("lsp_client", false, "Language Server Protocol client for code intelligence", killSwitch = true),
    TERMINAL("terminal", false, "Built-in terminal emulator", killSwitch = true),
    TREE_SITTER("tree_sitter", true, "Tree-sitter incremental syntax highlighting", killSwitch = true),
    MARKDOWN_WYSIWYG("markdown_wysiwyg", true, "WYSIWYG Markdown editing mode", killSwitch = true),
    MARKDOWN_EDITOR("markdown_editor", true, "Markdown editing toolbar and formatting"),
    GIT_UI("git_ui", true, "Git operations UI", killSwitch = true),
    COMMONMARK_PARSER("commonmark_parser", true, "CommonMark native Markdown parser"),
    FEATURE_TOGGLE("feature_toggle", true, "Feature toggle system for runtime feature control")
}
