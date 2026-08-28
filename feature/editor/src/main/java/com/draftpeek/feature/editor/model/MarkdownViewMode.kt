/**
 * 文件：MarkdownViewMode.kt
 * 功能：Markdown 视图模式枚举定义
 * 主要类/接口：MarkdownViewMode
 * 模块依赖：无外部依赖
 */
package com.draftpeek.feature.editor.model

/**
 * Markdown 文件的视图模式枚举。
 *
 * 参考：OpenNote-Compose 的 Lite/Standard 双模式设计。
 *
 * 四种视图模式满足不同的编辑/阅读场景：
 * - EDIT：标准文本编辑模式（当前默认），直接编辑 Markdown 源码
 * - PREVIEW：只读渲染预览模式，查看渲染后的 HTML 效果
 * - SPLIT：分屏模式，左侧编辑右侧预览，实时同步
 * - WYSIWYG：轻量模式，带有行内格式化提示（未来功能）
 */
enum class MarkdownViewMode {
    /** 编辑模式：直接编辑 Markdown 源码 */
    EDIT,

    /** 预览模式：只读渲染预览 */
    PREVIEW,

    /** 分屏模式：左右分屏，编辑与预览同步 */
    SPLIT,

    /** 所见即所得模式：带行内格式化提示 */
    WYSIWYG
}
