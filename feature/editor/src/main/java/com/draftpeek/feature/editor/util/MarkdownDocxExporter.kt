/**
 * 文件功能：Markdown 到 DOCX 格式导出工具（使用 Apache POI）
 * 
 * 主要对象：
 * - [MarkdownDocxExporter]：DOCX 导出器单例对象
 * 
 * 模块依赖：
 * - org.apache.poi.xwpf.usermodel.XWPFDocument：Apache POI Word 文档模型
 * - java.io.OutputStream：输出流
 * 
 * 注意：这是一个兼容性存根。完整的 Markdown 到 DOCX 渲染器正在重构中，
 * 以匹配 Apache POI 5.x API。目前它将原始 Markdown 内容写入 DOCX 文档，
 * 以保持导出管道可编译，消费者仍能收到可用文件。
 */
package com.draftpeek.feature.editor.util

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.OutputStream

/**
 * Markdown 到 DOCX 导出工具（单例对象）
 * 
 * 当前实现将原始 Markdown 文本作为单段落写入 DOCX 文档。
 * 完整的格式化导出（标题、列表、粗体等）正在重构中。
 */
object MarkdownDocxExporter {

    /**
     * 将 Markdown 内容导出为 DOCX 格式
     * 
     * @param markdownContent 原始 Markdown 文本
     * @param outputStream 写入 DOCX 的输出流
     * @param isDarkTheme 是否使用深色主题颜色（当前忽略）
     * @throws Exception 如果写入过程发生 IO 错误
     */
    fun exportToDocx(markdownContent: String, outputStream: OutputStream, isDarkTheme: Boolean = false) {
        val document = XWPFDocument()
        try {
            val paragraph = document.createParagraph()
            val run = paragraph.createRun()
            run.setText(markdownContent)
            document.write(outputStream)
        } finally {
            document.close()
        }
    }
}
