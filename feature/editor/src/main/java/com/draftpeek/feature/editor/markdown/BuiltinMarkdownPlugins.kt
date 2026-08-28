/**
 * 文件功能：DraftPeek Markdown 渲染管道的内置插件
 *
 * 主要类：
 * - [KaTeXAutoDetectPlugin]：自动检测 LaTeX 表达式并启用 KaTeX 渲染
 * - [MermaidAutoDetectPlugin]：自动检测 Mermaid 代码块并启用图表渲染
 * - [TocExtractorPlugin]：从标题提取目录
 * - [FrontmatterPlugin]：解析 YAML 前置元数据并暴露为元数据
 *
 * 模块依赖：
 * - 无外部依赖，使用 Kotlin 标准库正则表达式
 */
package com.draftpeek.feature.editor.markdown

/**
 * KaTeX 自动检测插件
 *
 * 自动检测 LaTeX 表达式（由 $...$ 或 $$...$$ 分隔），
 * 检测到时启用 KaTeX 数学公式渲染。
 */
class KaTeXAutoDetectPlugin : MarkdownRendererPlugin {
    override val id = "katex-auto-detect"
    override val priority = 100

    companion object {
        /** 匹配 $...$（行内）和 $$...$$（块级）数学表达式 */
        private val LATEX_PATTERN = Regex("""\$\$.+?\$\$|\$.+?\$""", RegexOption.DOT_MATCHES_ALL)
    }

    /**
     * 转换 Markdown 内容，自动检测并启用 KaTeX
     *
     * @param content 当前管道阶段的内容
     * @return 转换后的内容
     */
    override fun transform(content: MarkdownContent): MarkdownContent {
        val hasLatex = LATEX_PATTERN.containsMatchIn(content.markdown)
        return if (hasLatex && !content.enableKaTeX) {
            content.copy(enableKaTeX = true)
        } else {
            content
        }
    }
}

/**
 * Mermaid 自动检测插件
 *
 * 自动检测 Mermaid 代码块（```mermaid），检测到时启用 Mermaid 图表渲染。
 */
class MermaidAutoDetectPlugin : MarkdownRendererPlugin {
    override val id = "mermaid-auto-detect"
    override val priority = 101

    companion object {
        private val MERMAID_PATTERN = Regex("""```mermaid\s*\n""", RegexOption.DOT_MATCHES_ALL)
    }

    /**
     * 转换 Markdown 内容，自动检测并启用 Mermaid
     *
     * @param content 当前管道阶段的内容
     * @return 转换后的内容
     */
    override fun transform(content: MarkdownContent): MarkdownContent {
        val hasMermaid = MERMAID_PATTERN.containsMatchIn(content.markdown)
        return if (hasMermaid && !content.enableMermaid) {
            content.copy(enableMermaid = true)
        } else {
            content
        }
    }
}

/**
 * 目录提取插件
 *
 * 从 Markdown 标题中提取目录（Table of Contents）。
 *
 * 生成与 marked.js 输出兼容的锚点 ID：
 * 标题文本 → 小写，空格→连字符，去除标点符号。
 */
class TocExtractorPlugin : MarkdownRendererPlugin {
    override val id = "toc-extractor"
    override val priority = 200

    companion object {
        private val HEADING_PATTERN = Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)

        /**
         * 生成标题锚点 ID
         *
         * 算法步骤：
         * 1. 转换为小写
         * 2. 移除非单词字符（保留字母、数字、空格、连字符）
         * 3. 将一个或多个空格替换为连字符
         * 4. 去除首尾连字符
         *
         * @param text 标题文本
         * @return 锚点 ID 字符串
         */
        fun generateAnchor(text: String): String = text.lowercase()
            .replace(Regex("[^\\w\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }

    /**
     * 转换 Markdown 内容，提取目录
     *
     * @param content 当前管道阶段的内容
     * @return 包含目录的转换后内容
     */
    override fun transform(content: MarkdownContent): MarkdownContent {
        val entries = HEADING_PATTERN.findAll(content.markdown).map { match ->
            val level = match.groupValues[1].length
            val text = match.groupValues[2].trim()
            TocEntry(level = level, text = text, anchor = generateAnchor(text))
        }.toList()

        return if (entries != content.tableOfContents) {
            content.copy(tableOfContents = entries)
        } else {
            content
        }
    }
}

/**
 * YAML 前置元数据解析插件
 *
 * 解析文档开头的 YAML 前置元数据（--- 分隔的块），
 * 并将键值对暴露为元数据。
 *
 * 前置元数据格式示例：
 * ```yaml
 * ---
 * title: 文档标题
 * author: 作者名
 * date: 2024-01-01
 * ---
 * ```
 */
class FrontmatterPlugin : MarkdownRendererPlugin {
    override val id = "frontmatter"
    override val priority = 50

    companion object {
        /** 匹配 YAML 前置元数据块 */
        private val FRONTMATTER_PATTERN = Regex(
            """^---\s*\n(.*?)\n---\s*\n""",
            RegexOption.DOT_MATCHES_ALL
        )

        /** 匹配键值对：key: value */
        private val KV_PATTERN = Regex("""^(\w[\w\s]*):\s*(.+)$""", RegexOption.MULTILINE)
    }

    /**
     * 转换 Markdown 内容，解析并剥离前置元数据
     *
     * 算法步骤：
     * 1. 使用正则匹配开头的 --- 分隔块
     * 2. 提取 YAML 内容
     * 3. 使用键值对正则解析为 Map
     * 4. 从渲染的 Markdown 中剥离前置元数据
     * 5. 将元数据合并到 content.metadata
     *
     * @param content 当前管道阶段的内容
     * @return 转换后的内容（元数据已解析，前置元数据已剥离）
     */
    override fun transform(content: MarkdownContent): MarkdownContent {
        val match = FRONTMATTER_PATTERN.find(content.markdown) ?: return content
        val yamlBlock = match.groupValues[1]
        val metadata = KV_PATTERN.findAll(yamlBlock).associate { match ->
            match.groupValues[1].trim() to match.groupValues[2].trim()
        }

        // 从渲染的 Markdown 中剥离前置元数据
        val strippedMarkdown = content.markdown.removeRange(match.range)

        return content.copy(
            markdown = strippedMarkdown,
            metadata = content.metadata + metadata
        )
    }
}
