/**
 * 文件：EditorMode.kt
 * 功能：编辑器模式枚举与配置类定义
 * 主要类/接口：EditorMode、EditorModeConfig
 * 模块依赖：无外部依赖
 */
package com.draftpeek.feature.editor.model

/**
 * 编辑器模式枚举，提供不同的编辑体验。
 *
 * 参考：OpenNote-Compose 的 Lite/Standard 双模式设计。
 *
 * 各模式资源需求和功能集差异：
 * - STANDARD：完整功能，语法高亮（TextMate + Tree-sitter）、LSP 支持、全文件类型支持，内存占用较高
 * - LITE：轻量编辑，基本文本编辑、无语法高亮、启动更快、内存占用低，适合快速编辑小文件
 * - WYSIWYG：富文本编辑，基于 WebView 的 Markdown 实时渲染，仅适用于 Markdown
 */
enum class EditorMode {
    /**
     * 标准编辑器模式，提供完整功能。
     *
     * 特性：
     * - 语法高亮（TextMate + Tree-sitter）
     * - LSP 语言服务器支持（如语言可用）
     * - 完整文件类型支持
     * - 较高内存使用
     */
    STANDARD,

    /**
     * 轻量编辑器模式，用于快速编辑。
     *
     * 特性：
     * - 仅基本文本编辑
     * - 无语法高亮
     * - 更快启动速度和更低内存使用
     * - 适合小文件或快速编辑场景
     */
    LITE,

    /**
     * 所见即所得（WYSIWYG）模式。
     *
     * 特性：
     * - 富文本编辑与实时预览
     * - 仅支持 Markdown
     * - 基于 WebView 渲染
     * - 最适合 Markdown 文档编写
     */
    WYSIWYG
}

/**
 * 编辑器模式配置类。
 *
 * 包含特定于每种编辑器模式的功能开关设置。
 *
 * @property mode 关联的编辑器模式
 * @property enableSyntaxHighlighting 是否启用语法高亮
 * @property enableLsp 是否启用 LSP 语言服务器
 * @property enableTreeSitter 是否启用 Tree-sitter 语法解析
 * @property enableAutoComplete 是否启用自动补全
 * @property enableMinimap 是否启用缩略图（代码地图）
 * @property enableLineNumbers 是否显示行号
 * @property enableWordWrap 是否启用自动换行
 * @property enableCodeFolding 是否启用代码折叠
 * @property enableBracketMatching 是否启用括号匹配高亮
 * @property enableAutoIndent 是否启用自动缩进
 * @property enableTabInsertion 是否启用 Tab 键插入
 * @property tabSize Tab 键对应的空格数
 * @property maxFileSizeForHighlighting 启用语法高亮的最大文件大小（字节），0 表示禁用
 * @property maxFileSizeForLsp 启用 LSP 的最大文件大小（字节），0 表示禁用
 */
data class EditorModeConfig(
    val mode: EditorMode,
    val enableSyntaxHighlighting: Boolean = true,
    val enableLsp: Boolean = true,
    val enableTreeSitter: Boolean = true,
    val enableAutoComplete: Boolean = true,
    val enableMinimap: Boolean = true,
    val enableLineNumbers: Boolean = true,
    val enableWordWrap: Boolean = false,
    val enableCodeFolding: Boolean = true,
    val enableBracketMatching: Boolean = true,
    val enableAutoIndent: Boolean = true,
    val enableTabInsertion: Boolean = true,
    val tabSize: Int = 4,
    val maxFileSizeForHighlighting: Long = 1024 * 1024,
    val maxFileSizeForLsp: Long = 512 * 1024
) {
    companion object {
        /**
         * STANDARD 模式的默认配置。
         *
         * 启用所有功能，适合正常代码编辑场景。
         */
        val STANDARD_CONFIG = EditorModeConfig(
            mode = EditorMode.STANDARD,
            enableSyntaxHighlighting = true,
            enableLsp = true,
            enableTreeSitter = true,
            enableAutoComplete = true,
            enableMinimap = true,
            enableLineNumbers = true,
            enableWordWrap = false,
            enableCodeFolding = true,
            enableBracketMatching = true,
            enableAutoIndent = true,
            enableTabInsertion = true,
            tabSize = 4,
            maxFileSizeForHighlighting = 1024 * 1024,
            maxFileSizeForLsp = 512 * 1024
        )

        /**
         * LITE 模式的默认配置。
         *
         * 禁用重量级功能（语法高亮、LSP、Tree-sitter 等），
         * 保留基本编辑功能以获得最佳性能。
         */
        val LITE_CONFIG = EditorModeConfig(
            mode = EditorMode.LITE,
            enableSyntaxHighlighting = false,
            enableLsp = false,
            enableTreeSitter = false,
            enableAutoComplete = false,
            enableMinimap = false,
            enableLineNumbers = true,
            enableWordWrap = true,
            enableCodeFolding = false,
            enableBracketMatching = false,
            enableAutoIndent = true,
            enableTabInsertion = true,
            tabSize = 4,
            maxFileSizeForHighlighting = 0,
            maxFileSizeForLsp = 0
        )

        /**
         * WYSIWYG 模式的默认配置。
         *
         * 禁用所有代码编辑功能，启用自动换行以适配富文本编辑体验。
         */
        val WYSIWYG_CONFIG = EditorModeConfig(
            mode = EditorMode.WYSIWYG,
            enableSyntaxHighlighting = false,
            enableLsp = false,
            enableTreeSitter = false,
            enableAutoComplete = false,
            enableMinimap = false,
            enableLineNumbers = false,
            enableWordWrap = true,
            enableCodeFolding = false,
            enableBracketMatching = false,
            enableAutoIndent = true,
            enableTabInsertion = true,
            tabSize = 4,
            maxFileSizeForHighlighting = 0,
            maxFileSizeForLsp = 0
        )

        /**
         * 获取指定模式的配置。
         *
         * @param mode 编辑器模式
         * @return 对应模式的配置对象
         */
        fun forMode(mode: EditorMode): EditorModeConfig = when (mode) {
            EditorMode.STANDARD -> STANDARD_CONFIG
            EditorMode.LITE -> LITE_CONFIG
            EditorMode.WYSIWYG -> WYSIWYG_CONFIG
        }
    }
}

/**
 * 将 EditorMode 转换为显示用字符串。
 *
 * @return 模式的英文显示名称（"Standard"、"Lite"、"WYSIWYG"）
 */
fun EditorMode.toDisplayString(): String = when (this) {
    EditorMode.STANDARD -> "Standard"
    EditorMode.LITE -> "Lite"
    EditorMode.WYSIWYG -> "WYSIWYG"
}

/**
 * 获取 EditorMode 的描述文本。
 *
 * @return 模式功能的英文描述字符串
 */
fun EditorMode.getDescription(): String = when (this) {
    EditorMode.STANDARD -> "Full-featured editor with syntax highlighting and LSP support"
    EditorMode.LITE -> "Lightweight editor for quick edits, better performance"
    EditorMode.WYSIWYG -> "Rich text editing with real-time preview (Markdown only)"
}

/**
 * 判断 EditorMode 是否仅支持 Markdown 文件。
 *
 * @return 如果是 WYSIWYG 模式返回 true，否则返回 false
 */
fun EditorMode.isMarkdownOnly(): Boolean = this == EditorMode.WYSIWYG
