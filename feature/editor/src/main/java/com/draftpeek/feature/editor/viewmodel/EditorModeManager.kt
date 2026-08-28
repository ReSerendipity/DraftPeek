/**
 * 文件功能：编辑器模式切换和配置管理
 *
 * 主要类/枚举：
 * - [EditorModeManager]：编辑器模式管理器（单例）
 * - [EditorFeature]：可按模式启用/禁用的编辑器功能枚举
 *
 * 模块依赖：
 * - feature/editor/model：EditorMode 和 EditorModeConfig 定义模式和配置
 * - kotlinx-coroutines：Flow 响应式状态管理
 * - Hilt：依赖注入
 */
package com.draftpeek.feature.editor.viewmodel

import com.draftpeek.feature.editor.model.EditorMode
import com.draftpeek.feature.editor.model.EditorModeConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 编辑器模式管理器
 *
 * 负责编辑器模式切换和配置管理，支持三种模式：
 * - **标准模式 (STANDARD)**：全功能模式，启用所有语法高亮、LSP、Tree-sitter 等功能
 * - **轻量模式 (LITE)**：性能优先模式，禁用重型功能以提升大文件编辑体验
 * - **所见即所得 (WYSIWYG)**：Markdown 富文本编辑模式，仅支持 Markdown 文件
 *
 * **功能特性**：
 * - 模式切换和配置管理
 * - 按模式切换功能开关
 * - 根据文件特征自动推荐模式
 * - 提供各模式的内存和启动时间估算
 *
 * 参考设计：OpenNote-Compose 的 Lite/Standard 双模式设计
 */
@Singleton
class EditorModeManager @Inject constructor() {

    private val _currentMode = MutableStateFlow(EditorMode.STANDARD)

    /** 当前编辑器模式状态流 */
    val currentMode: StateFlow<EditorMode> = _currentMode.asStateFlow()

    private val _currentConfig = MutableStateFlow(EditorModeConfig.STANDARD_CONFIG)

    /** 当前模式的配置状态流 */
    val currentConfig: StateFlow<EditorModeConfig> = _currentConfig.asStateFlow()

    private val _isModeSwitching = MutableStateFlow(false)

    /** 是否正在切换模式（用于显示过渡 UI） */
    val isModeSwitching: StateFlow<Boolean> = _isModeSwitching.asStateFlow()

    /**
     * 切换到新的编辑器模式
     *
     * @param mode 要切换到的目标模式
     * @param force 是否强制切换（即使已经在该模式也重新加载配置）
     */
    fun switchMode(mode: EditorMode, force: Boolean = false) {
        if (_currentMode.value == mode && !force) return

        _isModeSwitching.value = true
        _currentMode.value = mode
        _currentConfig.value = EditorModeConfig.forMode(mode)
        _isModeSwitching.value = false
    }

    /**
     * 获取当前模式的配置
     *
     * @return 当前模式的 EditorModeConfig
     */
    fun getCurrentConfig(): EditorModeConfig = _currentConfig.value

    /**
     * 检查指定功能在当前模式下是否启用
     *
     * @param feature 要检查的编辑器功能
     * @return 功能已启用返回 true，否则返回 false
     */
    fun isFeatureEnabled(feature: EditorFeature): Boolean {
        val config = _currentConfig.value
        return when (feature) {
            EditorFeature.SYNTAX_HIGHLIGHTING -> config.enableSyntaxHighlighting
            EditorFeature.LSP -> config.enableLsp
            EditorFeature.TREE_SITTER -> config.enableTreeSitter
            EditorFeature.AUTO_COMPLETE -> config.enableAutoComplete
            EditorFeature.MINIMAP -> config.enableMinimap
            EditorFeature.LINE_NUMBERS -> config.enableLineNumbers
            EditorFeature.WORD_WRAP -> config.enableWordWrap
            EditorFeature.CODE_FOLDING -> config.enableCodeFolding
            EditorFeature.BRACKET_MATCHING -> config.enableBracketMatching
            EditorFeature.AUTO_INDENT -> config.enableAutoIndent
            EditorFeature.TAB_INSERTION -> config.enableTabInsertion
        }
    }

    /**
     * 检查当前模式是否支持指定的编程语言
     *
     * @param language 编程语言标识符（如 "kotlin"、"markdown"），null 表示纯文本
     * @return 支持该语言返回 true
     */
    fun supportsLanguage(language: String?): Boolean {
        if (language == null) return true

        return when (_currentMode.value) {
            EditorMode.STANDARD -> true
            EditorMode.LITE -> true
            EditorMode.WYSIWYG -> language.lowercase() in MARKDOWN_LANGUAGES
        }
    }

    /**
     * 根据文件特征获取推荐的编辑器模式
     *
     * 推荐规则：
     * 1. Markdown 文件 → WYSIWYG 模式
     * 2. 文件大于 1MB → LITE 模式
     * 3. 其他 → STANDARD 模式
     *
     * @param language 编程语言标识符
     * @param fileSize 文件大小（字节）
     * @param isMarkdown 是否为 Markdown 文件
     * @return 推荐的编辑器模式
     */
    fun getRecommendedMode(language: String?, fileSize: Long, isMarkdown: Boolean): EditorMode {
        if (isMarkdown) {
            return EditorMode.WYSIWYG
        }

        if (fileSize > LITE_MODE_FILE_THRESHOLD) {
            return EditorMode.LITE
        }

        return EditorMode.STANDARD
    }

    /**
     * 获取当前模式的内存使用估算
     *
     * @return 估算内存使用量（MB）
     */
    fun getMemoryEstimate(): Float = when (_currentMode.value) {
        EditorMode.STANDARD -> STANDARD_MODE_MEMORY_MB
        EditorMode.LITE -> LITE_MODE_MEMORY_MB
        EditorMode.WYSIWYG -> WYSIWYG_MODE_MEMORY_MB
    }

    /**
     * 获取当前模式的启动时间估算
     *
     * @return 估算启动时间（毫秒）
     */
    fun getStartupTimeEstimate(): Long = when (_currentMode.value) {
        EditorMode.STANDARD -> STANDARD_MODE_STARTUP_MS
        EditorMode.LITE -> LITE_MODE_STARTUP_MS
        EditorMode.WYSIWYG -> WYSIWYG_MODE_STARTUP_MS
    }

    companion object {
        /** Markdown 语言标识符集合 */
        private val MARKDOWN_LANGUAGES = setOf("markdown", "md")

        /** 轻量模式文件大小阈值：1MB，超过此大小自动推荐 LITE 模式 */
        private const val LITE_MODE_FILE_THRESHOLD = 1024 * 1024L

        /** 标准模式内存估算（MB） */
        private const val STANDARD_MODE_MEMORY_MB = 50f

        /** 轻量模式内存估算（MB） */
        private const val LITE_MODE_MEMORY_MB = 20f

        /** WYSIWYG 模式内存估算（MB） */
        private const val WYSIWYG_MODE_MEMORY_MB = 30f

        /** 标准模式启动时间估算（ms） */
        private const val STANDARD_MODE_STARTUP_MS = 500L

        /** 轻量模式启动时间估算（ms） */
        private const val LITE_MODE_STARTUP_MS = 200L

        /** WYSIWYG 模式启动时间估算（ms） */
        private const val WYSIWYG_MODE_STARTUP_MS = 300L
    }
}

/**
 * 可按模式启用/禁用的编辑器功能枚举
 *
 * 每个枚举值代表一个可以在不同编辑器模式下独立开关的功能特性
 */
enum class EditorFeature {
    /** 语法高亮 */
    SYNTAX_HIGHLIGHTING,

    /** LSP 语言服务器支持 */
    LSP,

    /** Tree-sitter 增量解析 */
    TREE_SITTER,

    /** 自动补全 */
    AUTO_COMPLETE,

    /** 小地图 */
    MINIMAP,

    /** 行号显示 */
    LINE_NUMBERS,

    /** 自动换行 */
    WORD_WRAP,

    /** 代码折叠 */
    CODE_FOLDING,

    /** 括号匹配 */
    BRACKET_MATCHING,

    /** 自动缩进 */
    AUTO_INDENT,

    /** Tab 键插入 */
    TAB_INSERTION
}
