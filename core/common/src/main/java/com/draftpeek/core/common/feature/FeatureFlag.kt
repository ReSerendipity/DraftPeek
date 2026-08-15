/**
 * 功能开关枚举模块。
 *
 * 定义DraftPeek的所有功能开关，每个开关控制运行时是否启用某个功能。
 * 开关状态通过[FeatureToggleManager]持久化在SharedPreferences中，支持运行时动态切换。
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
 */
enum class FeatureFlag(val key: String, val defaultEnabled: Boolean, val description: String) {
    LSP_CLIENT("lsp_client", false, "Language Server Protocol client for code intelligence"),
    TERMINAL("terminal", false, "Built-in terminal emulator"),
    TREE_SITTER("tree_sitter", true, "Tree-sitter incremental syntax highlighting"),
    MARKDOWN_WYSIWYG("markdown_wysiwyg", true, "WYSIWYG Markdown editing mode"),
    MARKDOWN_EDITOR("markdown_editor", true, "Markdown editing toolbar and formatting"),
    GIT_UI("git_ui", true, "Git operations UI"),
    COMMONMARK_PARSER("commonmark_parser", true, "CommonMark native Markdown parser"),
    FEATURE_TOGGLE("feature_toggle", true, "Feature toggle system for runtime feature control"),
}
