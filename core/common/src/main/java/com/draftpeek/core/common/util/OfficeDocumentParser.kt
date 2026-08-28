/**
 * Office文档解析模块。
 *
 * 使用Apache POI将Office文档（Word/Excel/PowerPoint）解析为带样式的HTML，供WebView渲染。
 * 支持新旧两种格式：OOXML（.docx/.xlsx/.pptx，ZIP格式）和OLE2（.doc/.xls/.ppt，二进制格式）。
 * 包含stderr屏蔽（抑制Log4j SPI噪音）、统一错误映射、资源自动释放、并发安全锁等优化。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PrintStream
import java.io.SequenceInputStream
import java.util.concurrent.locks.ReentrantLock
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hslf.usermodel.HSLFTextShape
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xwpf.usermodel.XWPFDocument

private const val TAG = "OfficeDocumentParser"

/**
 * stderr屏蔽块的全局串行化锁。
 *
 * System.setErr是JVM进程级状态，两个线程并发执行withStderrSilenced时
 * 会相互覆盖finally中的stderr恢复。使用ReentrantLock串行化是最低成本修复，
 * POI解析不在热路径上，可接受。使用ReentrantLock而非kotlinx Mutex，
 * 因为withStderrSilenced是同步inline函数，不能是suspend。
 */
private val stderrLock = ReentrantLock()

/** OLE2（旧版Office二进制格式）魔数 */
private val OLE2_MAGIC = byteArrayOf(
    0xD0.toByte(),
    0xCF.toByte(),
    0x11.toByte(),
    0xE0.toByte(),
    0xA1.toByte(),
    0xB1.toByte(),
    0x1A.toByte(),
    0xE1.toByte()
)

/** ZIP/OOXML格式魔数（"PK"） */
private val ZIP_MAGIC = byteArrayOf(0x50.toByte(), 0x4B.toByte())

/**
 * 判断字节数组是否为OLE2格式头。
 */
private fun ByteArray.isOle2Header(): Boolean =
    size >= OLE2_MAGIC.size && (0 until OLE2_MAGIC.size).all { this[it] == OLE2_MAGIC[it] }

/**
 * 判断字节数组是否为ZIP/OOXML格式头。
 */
private fun ByteArray.isZipHeader(): Boolean = size >= 2 && this[0] == ZIP_MAGIC[0] && this[1] == ZIP_MAGIC[1]

/**
 * Office文档解析工具对象。
 *
 * 使用Apache POI库解析Word/Excel/PowerPoint文档为HTML，支持明暗主题、
 * 自动格式检测（OLE2/OOXML）、错误容错处理。所有POI操作包装在try-catch中，
 * 失败时返回友好的错误HTML页面。
 */
object OfficeDocumentParser {

    /**
     * 在POI初始化期间屏蔽stderr，抑制Log4j SPI噪音。
     * 原始stderr始终在finally块中恢复。stderrLock串行化整个屏蔽块，
     * 防止并发解析时stderr状态相互覆盖。
     */
    private inline fun <T> withStderrSilenced(block: () -> T): T {
        stderrLock.lock()
        val originalErr = System.err
        return try {
            System.setErr(
                PrintStream(object : java.io.OutputStream() {
                    override fun write(b: Int) {}
                })
            )
            block()
        } finally {
            System.setErr(originalErr)
            stderrLock.unlock()
        }
    }

    /**
     * 从流中精确读取n字节用于头部检测。
     * 如果流提前结束，返回实际读取的部分数组。
     */
    private fun InputStream.readExactly(n: Int): ByteArray {
        val buffer = ByteArray(n)
        var offset = 0
        while (offset < n) {
            val read = this.read(buffer, offset, n - offset)
            if (read == -1) break
            offset += read
        }
        return if (offset == n) buffer else buffer.copyOf(offset)
    }

    /**
     * 解析Word文档（.doc/.docx）为HTML。
     * 通过文件头自动分发：ZIP→OOXML，OLE2→旧版二进制。
     *
     * @param inputStream 文档输入流
     * @param isDark 是否使用暗色主题
     * @return HTML字符串
     */
    fun parseWord(inputStream: InputStream, isDark: Boolean = false): String {
        val header = inputStream.readExactly(OLE2_MAGIC.size)
        if (header.size < OLE2_MAGIC.size) {
            return getErrorHtml(isDark, "文件格式错误", "文件太小或为空")
        }
        val wrappedStream: InputStream = SequenceInputStream(ByteArrayInputStream(header), inputStream)

        return when {
            header.isOle2Header() -> parseWordLegacy(wrappedStream, isDark)
            header.isZipHeader() -> parseWordOoxml(wrappedStream, isDark)
            else -> getErrorHtml(isDark, "文件格式错误", "这不是有效的 Word 文件（文件头不匹配）")
        }
    }

    /**
     * 解析OOXML格式Word文档（.docx）为HTML。
     */
    private fun parseWordOoxml(wrappedStream: InputStream, isDark: Boolean): String = try {
        withStderrSilenced {
            XWPFDocument(wrappedStream).use { doc ->
                val html = StringBuilder()
                html.append(buildHtmlHeader(isDark))

                val hasContent = doc.paragraphs.any { it.text.isNotBlank() } || doc.tables.isNotEmpty()
                if (!hasContent) {
                    html.appendLine(emptyContentHtml("文档内容为空", isDark))
                } else {
                    for (paragraph in doc.paragraphs) {
                        val text = paragraph.text
                        if (text.isNotBlank()) {
                            val style = paragraph.style
                            val tag = when {
                                style?.contains("Heading") == true -> "h2"
                                style?.contains("Title") == true -> "h1"
                                else -> "p"
                            }
                            html.appendLine("<$tag>${escapeHtml(text)}</$tag>")
                        }
                    }

                    for (table in doc.tables) {
                        html.appendLine("<table>")
                        for (row in table.rows) {
                            html.appendLine("<tr>")
                            for (cell in row.tableCells) {
                                html.appendLine("<td>${escapeHtml(cell.text)}</td>")
                            }
                            html.appendLine("</tr>")
                        }
                        html.appendLine("</table>")
                    }
                }

                html.append(getHtmlFooter())
                html.toString()
            }
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to parse Word (.docx) document", e)
        getErrorHtml(isDark, "无法解析 Word 文件", mapWordErrorMessage(e))
    }

    /**
     * 解析旧版二进制格式Word文档（.doc）为HTML。
     */
    private fun parseWordLegacy(wrappedStream: InputStream, isDark: Boolean): String = try {
        withStderrSilenced {
            HWPFDocument(wrappedStream).use { doc ->
                val html = StringBuilder()
                html.append(buildHtmlHeader(isDark))

                val range = doc.range
                val styleSheet = doc.styleSheet
                val numParagraphs = range.numParagraphs()
                var i = 0
                var hasContent = false

                while (i < numParagraphs) {
                    val paragraph = try {
                        range.getParagraph(i)
                    } catch (e: Exception) {
                        i++
                        continue
                    }

                    if (paragraph.isInTable) {
                        try {
                            val table = range.getTable(paragraph)
                            html.appendLine("<table>")
                            for (rowIdx in 0 until table.numRows()) {
                                val row = table.getRow(rowIdx)
                                html.appendLine("<tr>")
                                for (cellIdx in 0 until row.numCells()) {
                                    val cellText = try {
                                        row.getCell(cellIdx).text()
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    val tag = if (rowIdx == 0) "th" else "td"
                                    html.appendLine("<$tag>${escapeHtml(cellText)}</$tag>")
                                }
                                html.appendLine("</tr>")
                            }
                            html.appendLine("</table>")
                            hasContent = true
                            val tableParagraphs = table.numParagraphs().coerceAtLeast(1)
                            i += tableParagraphs
                            continue
                        } catch (e: Exception) {
                            Log.w(TAG, "HWPF table extraction failed, falling back", e)
                        }
                    }

                    val text = try {
                        paragraph.text()
                    } catch (e: Exception) {
                        ""
                    }
                    if (text.isNotBlank()) {
                        val tag = try {
                            resolveHwpfParagraphTag(paragraph, styleSheet)
                        } catch (e: Exception) {
                            "p"
                        }
                        html.appendLine("<$tag>${escapeHtml(text)}</$tag>")
                        hasContent = true
                    }
                    i++
                }

                if (!hasContent) {
                    html.appendLine(emptyContentHtml("文档内容为空", isDark))
                }

                html.append(getHtmlFooter())
                html.toString()
            }
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to parse Word (.doc) document", e)
        getErrorHtml(isDark, "无法解析 Word 文件", mapWordErrorMessage(e))
    }

    /**
     * 解析HWPF段落样式，确定对应的HTML标签（h1/h2/p）。
     */
    private fun resolveHwpfParagraphTag(
        paragraph: org.apache.poi.hwpf.usermodel.Paragraph,
        styleSheet: org.apache.poi.hwpf.model.StyleSheet?
    ): String {
        if (styleSheet == null) return "p"
        val styleIdx = paragraph.styleIndex
        if (styleIdx < 0) return "p"
        val description = try {
            styleSheet.getStyleDescription(styleIdx.toInt())
        } catch (e: Exception) {
            null
        } ?: return "p"
        val name = description.name ?: return "p"
        return when {
            name.contains("Heading", ignoreCase = true) -> "h2"
            name.contains("Title", ignoreCase = true) -> "h1"
            else -> "p"
        }
    }

    /**
     * 统一POI异常错误消息映射。
     * 消除Word/Excel/PowerPoint解析器中重复的错误消息逻辑。
     */
    private fun mapPoiErrorMessage(docTypeLabel: String, e: Throwable, vararg additionalKeywords: String): String =
        when {
            e is OutOfMemoryError -> "内存不足，文件过大"
            e.message?.contains("password", ignoreCase = true) == true ->
                "${docTypeLabel}已加密，无法打开"
            e.message?.contains("corrupt", ignoreCase = true) == true ||
                e.message?.contains("invalid", ignoreCase = true) == true ||
                additionalKeywords.any { kw -> e.message?.contains(kw, ignoreCase = true) == true } ->
                "文件已损坏或格式无效"
            e is NoClassDefFoundError ||
                e is ExceptionInInitializerError ||
                e.message?.contains("Class", ignoreCase = true) == true ->
                "库初始化失败，请尝试更新应用"
            else -> "解析失败: ${e.message}"
        }

    private fun mapWordErrorMessage(e: Throwable): String = mapPoiErrorMessage("文档", e, "ZIP")

    private fun mapPresentationErrorMessage(e: Throwable): String = mapPoiErrorMessage("演示文稿", e)

    /**
     * 解析Excel文档（.xlsx）为HTML表格。
     *
     * @param inputStream 文档输入流
     * @param isDark 是否使用暗色主题
     * @return HTML字符串
     */
    fun parseExcel(inputStream: InputStream, isDark: Boolean = false): String {
        val header = inputStream.readExactly(ZIP_MAGIC.size)
        if (header.size < ZIP_MAGIC.size) {
            return getErrorHtml(isDark, "文件格式错误", "文件太小或为空")
        }
        val wrappedStream: InputStream = SequenceInputStream(ByteArrayInputStream(header), inputStream)

        return try {
            withStderrSilenced {
                WorkbookFactory.create(wrappedStream).use { workbook ->
                    val html = StringBuilder()
                    html.append(buildHtmlHeader(isDark))

                    if (workbook.numberOfSheets == 0) {
                        html.appendLine(emptyContentHtml("工作簿没有工作表", isDark))
                    } else {
                        for (sheetIdx in 0 until workbook.numberOfSheets) {
                            val sheet = workbook.getSheetAt(sheetIdx)
                            html.appendLine("<h2>${escapeHtml(sheet.sheetName)}</h2>")
                            html.appendLine("<table>")

                            for (row in sheet) {
                                html.appendLine("<tr>")
                                for (cellIdx in 0 until row.lastCellNum) {
                                    val cell = row.getCell(cellIdx) ?: continue
                                    val value = when (cell.cellType) {
                                        CellType.STRING -> cell.stringCellValue
                                        CellType.NUMERIC -> {
                                            val num = cell.numericCellValue
                                            if (num ==
                                                num.toLong().toDouble()
                                            ) {
                                                num.toLong().toString()
                                            } else {
                                                num.toString()
                                            }
                                        }
                                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                        CellType.FORMULA -> try {
                                            cell.stringCellValue
                                        } catch (e: Exception) {
                                            try {
                                                cell.numericCellValue.toString()
                                            } catch (e2: Exception) {
                                                ""
                                            }
                                        }
                                        else -> ""
                                    }
                                    val tag = if (row.rowNum == 0) "th" else "td"
                                    html.appendLine("<$tag>${escapeHtml(value)}</$tag>")
                                }
                                html.appendLine("</tr>")
                            }
                            html.appendLine("</table>")
                        }
                    }

                    html.append(getHtmlFooter())
                    html.toString()
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to parse Excel document", e)
            getErrorHtml(
                isDark,
                "无法解析 Excel 文件",
                mapPoiErrorMessage("工作簿", e, "encrypt", "Record")
            )
        }
    }

    /**
     * 解析PowerPoint文档（.ppt/.pptx）为HTML。
     * 通过文件头自动分发：ZIP→OOXML，OLE2→旧版二进制。
     *
     * @param inputStream 文档输入流
     * @param isDark 是否使用暗色主题
     * @return HTML字符串
     */
    fun parsePowerPoint(inputStream: InputStream, isDark: Boolean = false): String {
        val header = inputStream.readExactly(OLE2_MAGIC.size)
        if (header.size < OLE2_MAGIC.size) {
            return getErrorHtml(isDark, "文件格式错误", "文件太小或为空")
        }
        val wrappedStream: InputStream = SequenceInputStream(ByteArrayInputStream(header), inputStream)

        return when {
            header.isOle2Header() -> parsePowerPointLegacy(wrappedStream, isDark)
            header.isZipHeader() -> parsePowerPointOoxml(wrappedStream, isDark)
            else -> getErrorHtml(isDark, "文件格式错误", "这不是有效的 PowerPoint 文件（文件头不匹配）")
        }
    }

    /**
     * 解析OOXML格式PowerPoint文档（.pptx）为HTML。
     */
    private fun parsePowerPointOoxml(wrappedStream: InputStream, isDark: Boolean): String = try {
        withStderrSilenced {
            XMLSlideShow(wrappedStream).use { slideShow ->
                renderSlides(
                    slides = slideShow.slides,
                    isDark = isDark,
                    getShapes = { it.shapes },
                    extractTextLines = { shape ->
                        (shape as? XSLFTextShape)?.textParagraphs?.map { p -> p.text }
                    }
                )
            }
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to parse PowerPoint (.pptx) document", e)
        getErrorHtml(isDark, "无法解析 PowerPoint 文件", mapPresentationErrorMessage(e))
    }

    /**
     * 解析旧版二进制格式PowerPoint文档（.ppt）为HTML。
     */
    private fun parsePowerPointLegacy(wrappedStream: InputStream, isDark: Boolean): String = try {
        withStderrSilenced {
            HSLFSlideShow(wrappedStream).use { slideShow ->
                renderSlides(
                    slides = slideShow.slides.toList(),
                    isDark = isDark,
                    getShapes = { slide -> slide.shapes.toList() },
                    extractTextLines = { shape ->
                        (shape as? HSLFTextShape)?.textParagraphs?.map { para ->
                            para.textRuns.joinToString("") { it.rawText }
                        }
                    }
                )
            }
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to parse PowerPoint (.ppt) document", e)
        getErrorHtml(isDark, "无法解析 PowerPoint 文件", mapPresentationErrorMessage(e))
    }

    /**
     * 通用幻灯片渲染模板（OOXML和旧版PPT共用）。
     * 使用泛型避免slide和shape类型的代码重复。
     */
    private inline fun <S, SH> renderSlides(
        slides: List<S>,
        isDark: Boolean,
        getShapes: (S) -> List<SH>,
        extractTextLines: (SH) -> List<String>?
    ): String {
        val html = StringBuilder()
        html.append(buildHtmlHeader(isDark))

        if (slides.isEmpty()) {
            html.appendLine(emptyContentHtml("演示文稿没有幻灯片", isDark))
        } else {
            var slideNum = 1
            for (slide in slides) {
                html.appendLine("<div class='slide'>")
                html.appendLine("<h3>Slide $slideNum</h3>")

                var hasContent = false
                for (shape in getShapes(slide)) {
                    val lines = try {
                        extractTextLines(shape)
                    } catch (e: Exception) {
                        null
                    }
                    if (lines != null) {
                        for (text in lines) {
                            if (text.isNotBlank()) {
                                html.appendLine("<p>${escapeHtml(text)}</p>")
                                hasContent = true
                            }
                        }
                    }
                }

                if (!hasContent) {
                    html.appendLine(emptyContentHtml("（此幻灯片无文本内容）", isDark))
                }

                html.appendLine("</div>")
                html.appendLine("<hr>")
                slideNum++
            }
        }

        html.append(getHtmlFooter())
        return html.toString()
    }

    /**
     * 生成空内容提示HTML。
     */
    private fun emptyContentHtml(text: String, isDark: Boolean): String {
        val color = if (isDark) "#888" else "#666"
        return "<p style='color: $color'>$text</p>"
    }

    /**
     * 构建HTML头部（包含CSS样式，支持明暗主题）。
     */
    private fun buildHtmlHeader(isDark: Boolean): String {
        val colors = if (isDark) {
            listOf("#1a1a1a", "#e0e0e0", "#fff", "#e0e0e0", "#aaa", "#60a5fa", "#333", "#444", "#2a2a2a", "#444")
        } else {
            listOf("#ffffff", "#333", "#1a1a1a", "#333", "#555", "#2563eb", "#f5f5f5", "#ddd", "#fafafa", "#eee")
        }
        val bg = colors[0]
        val text = colors[1]
        val h1 = colors[2]
        val h2 = colors[3]
        val h3 = colors[4]
        val border = colors[5]
        val thBg = colors[6]
        val thBorder = colors[7]
        val slideBg = colors[8]
        val hrColor = colors[9]
        return """<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<style>
    body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: $text; background: $bg; }
    h1 { color: $h1; border-bottom: 2px solid $border; padding-bottom: 8px; }
    h2 { color: $h2; margin-top: 24px; }
    h3 { color: $h3; }
    p { margin: 8px 0; }
    table { border-collapse: collapse; width: 100%; margin: 12px 0; }
    th, td { border: 1px solid $thBorder; padding: 8px; text-align: left; }
    th { background: $thBg; font-weight: bold; }
    .slide { padding: 16px; background: $slideBg; border-radius: 8px; margin: 12px 0; }
    hr { border: none; border-top: 1px solid $hrColor; margin: 24px 0; }
</style></head><body>"""
    }

    private val HTML_TEMPLATE_FOOTER = "</body></html>"

    private fun getHtmlFooter(): String = HTML_TEMPLATE_FOOTER

    /**
     * 生成错误提示HTML页面。
     */
    private fun getErrorHtml(isDark: Boolean, title: String, detail: String): String {
        val textColor = if (isDark) "#e0e0e0" else "#333"
        val bgColor = if (isDark) "#1a1a1a" else "#ffffff"
        val subtextColor = if (isDark) "#aaa" else "#666"
        return """
<!DOCTYPE html>
<html><head><meta charset="UTF-8">
<style>
    body { font-family: sans-serif; padding: 48px 16px; line-height: 1.6; color: $textColor; background: $bgColor; text-align: center; }
    h2 { color: #ef4444; margin-bottom: 8px; }
    p { color: $subtextColor; margin-top: 8px; }
</style></head><body>
<h2>⚠ ${escapeHtml(title)}</h2>
<p>${escapeHtml(detail)}</p>
</body></html>
        """.trimIndent()
    }

    private fun getErrorHtml(isDark: Boolean, message: String): String = getErrorHtml(isDark, "文件解析失败", message)

    /**
     * HTML转义特殊字符，防止XSS。
     *
     * 使用单次遍历的 StringBuilder 实现，避免多次 .replace() 调用产生的中间字符串。
     * 预先估算容量（原始长度 + 10%）减少 StringBuilder 扩容次数。
     * 性能：O(n) 单次扫描，比 5 次链式 replace（每次 O(n) 创建新字符串）快约 3-5 倍。
     */
    private fun escapeHtml(text: String): String {
        val sb = StringBuilder(text.length + (text.length shr 3) + 16)
        var i = 0
        val len = text.length
        while (i < len) {
            when (val c = text[i]) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}
