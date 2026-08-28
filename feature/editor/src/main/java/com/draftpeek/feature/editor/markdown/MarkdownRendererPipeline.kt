/**
 * 文件功能：Markdown 渲染管道接口和实现，支持插件式内容转换
 *
 * 主要类/接口：
 * - [MarkdownRendererPlugin]：Markdown 渲染插件接口
 * - [MarkdownContent]：在渲染管道中流动的不可变内容数据类
 * - [TocEntry]：目录条目数据类
 * - [MarkdownRendererPipeline]：插件注册和执行器
 *
 * 模块依赖：
 * - androidx.compose.runtime：Compose 不可变注解
 *
 * 设计灵感：markdown-it 插件链和 HedgeDoc 渲染管道。
 * 插件按优先级顺序执行（数值越小越先执行）。
 */
package com.draftpeek.feature.editor.markdown

/**
 * Markdown 渲染插件接口
 *
 * 插件可以在渲染管道的各个阶段转换内容。
 * 插件按 [priority] 顺序执行（数值越小越先执行）。
 * 每个插件接收当前内容，可以转换它、添加元数据或注入额外的 HTML/CSS/JS。
 *
 * 典型优先级范围：
 * - 0-99：预处理（清洗、规范化）
 * - 100-199：内容转换（短代码扩展、模板注入）
 * - 200-299：后处理（目录、脚注）
 * - 300-399：样式/JS 注入
 */
interface MarkdownRendererPlugin {
    /** 插件唯一标识符 */
    val id: String

    /**
     * 执行优先级，数值越小越先执行。
     */
    val priority: Int

    /**
     * 转换 [MarkdownContent] 并返回修改后的版本
     *
     * 实现应该是纯函数——除了返回的内容之外没有副作用。
     * 这确保插件是可组合的，并且可以独立测试。
     *
     * @param content 当前管道阶段的内容
     * @return 下一阶段的转换后内容
     */
    fun transform(content: MarkdownContent): MarkdownContent
}

/**
 * 在 Markdown 渲染管道中流动的不可变内容数据类
 *
 * 标记为 @Immutable 用于 Compose 优化。
 * 每个插件可以修改任何字段。管道以最终加载到 WebView 的 HTML 结束。
 *
 * @property markdown 原始 Markdown 源文本
 * @property extraCss 注入到预览头部的额外 CSS
 * @property extraJs 渲染后执行的额外 JavaScript
 * @property bodyAttributes body 标签的额外 HTML 属性（如 "data-theme='night'"）
 * @property enableKaTeX 是否为此内容启用 KaTeX 渲染
 * @property enableMermaid 是否启用 Mermaid 图表渲染
 * @property enableHighlight 是否启用语法高亮（Prism.js）
 * @property tableOfContents 从标题提取的目录
 * @property metadata 从前置元数据或类似方式提取的自定义元数据
 */
@androidx.compose.runtime.Immutable
data class MarkdownContent(
    val markdown: String = "",
    val extraCss: String = "",
    val extraJs: String = "",
    val bodyAttributes: Map<String, String> = emptyMap(),
    val enableKaTeX: Boolean = false,
    val enableMermaid: Boolean = false,
    val enableHighlight: Boolean = false,
    val tableOfContents: List<TocEntry> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 目录中的单个条目
 *
 * @property level 标题级别（1-6）
 * @property text 标题文本
 * @property anchor 锚点 ID（用于跳转链接）
 */
@androidx.compose.runtime.Immutable
data class TocEntry(val level: Int, val text: String, val anchor: String)

/**
 * Markdown 渲染管道（插件注册器和执行器）
 *
 * 插件按 ID 注册并按 [priority] 顺序执行。
 * 重复 ID 被静默忽略（先注册的胜出）。
 *
 * 使用示例：
 * ```kotlin
 * val pipeline = MarkdownRendererPipeline()
 * pipeline.register(FrontmatterPlugin())
 * pipeline.register(TocPlugin())
 * pipeline.register(KaTeXAutoDetectPlugin())
 *
 * val result = pipeline.render(MarkdownContent(markdown = rawText))
 * // result.html → WebView
 * // result.tableOfContents → 大纲抽屉
 * ```
 */
class MarkdownRendererPipeline {
    private val plugins = mutableMapOf<String, MarkdownRendererPlugin>()

    /**
     * 注册插件
     *
     * 如果已存在相同 [id] 的插件，新插件被忽略并返回 false。
     *
     * @param plugin 要注册的插件
     * @return 注册成功返回 true，如果存在重复 ID 返回 false
     */
    fun register(plugin: MarkdownRendererPlugin): Boolean {
        if (plugin.id in plugins) return false
        plugins[plugin.id] = plugin
        return true
    }

    /**
     * 通过 ID 注销插件
     *
     * @param id 插件 ID
     * @return 移除成功返回 true，未找到 ID 返回 false
     */
    fun unregister(id: String): Boolean = plugins.remove(id) != null

    /**
     * 按优先级顺序对给定内容执行所有已注册插件
     *
     * 执行流程：
     * 1. 按 priority 排序所有插件（升序）
     * 2. 从 initial 内容开始
     * 3. 依次调用每个插件的 transform 方法
     * 4. 返回最终转换后的内容
     *
     * @param initial 初始 Markdown 内容
     * @return 所有插件处理后的完整转换内容
     */
    fun render(initial: MarkdownContent): MarkdownContent {
        val sorted = plugins.values.sortedBy { it.priority }
        var current = initial
        for (plugin in sorted) {
            current = plugin.transform(current)
        }
        return current
    }

    /**
     * 获取所有已注册插件的 ID 集合
     *
     * @return 插件 ID 集合
     */
    fun pluginIds(): Set<String> = plugins.keys.toSet()

    /**
     * 获取已注册插件的数量
     *
     * @return 插件数量
     */
    fun size(): Int = plugins.size
}
