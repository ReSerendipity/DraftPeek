/**
 * 文件：ExportConfig.kt
 * 功能：Markdown 导出配置数据模型
 * 主要类/接口：ExportConfig、PageSize
 * 模块依赖：无外部依赖
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable

/**
 * Markdown 导出配置选项。
 *
 * 使用场景：MarkdownExporter 和 MarkdownDocxExporter 根据此配置生成导出文档。
 *
 * @property pageSize 页面尺寸，默认为 A4
 * @property includePageNumbers 是否包含页码
 * @property includeTableOfContents 是否包含目录
 */
@Immutable
data class ExportConfig(
    val pageSize: PageSize = PageSize.A4,
    val includePageNumbers: Boolean = false,
    val includeTableOfContents: Boolean = false,
)

/**
 * 页面尺寸枚举。
 *
 * 定义常见纸张规格及其毫米尺寸。
 *
 * @property displayName 页面尺寸显示名称
 * @property widthMm 页面宽度（毫米）
 * @property heightMm 页面高度（毫米）
 */
enum class PageSize(val displayName: String, val widthMm: Int, val heightMm: Int) {
    /** A4 纸张（210mm × 297mm），国际标准 */
    A4("A4", 210, 297),
    /** A3 纸张（297mm × 420mm），国际标准 */
    A3("A3", 297, 420),
    /** Letter 纸张（216mm × 279mm），北美标准 */
    LETTER("Letter", 216, 279),
    /** Legal 纸张（216mm × 356mm），北美法律用纸 */
    LEGAL("Legal", 216, 356),
}
