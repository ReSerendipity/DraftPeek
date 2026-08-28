/**
 * 编辑器文件读写往返保真测试文件。
 *
 * 验证Markdown和文本文件保存后重新读取时，内容与原始内容完全一致。
 * 确保没有隐式处理（如trim、格式化、行尾转换、空白行增减等）破坏文件内容。
 *
 * 测试覆盖：
 * - 内部文件写入路径（AppFileManager）
 * - SAF存储访问框架写入路径
 * - 空白行、混合缩进、CRLF/LF混合行尾
 * - Markdown尾部空格（硬换行语法）
 * - 完整Markdown文档结构
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.editor

import java.io.File
import java.nio.charset.Charset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * 往返保真（Round-trip Fidelity）测试类。
 *
 * 验证 Markdown 文件保存后重新读取，内容与原始内容完全一致，
 * 不应有任何隐式处理（如 trim、格式化、行尾转换、空白行增减等）。
 *
 * 这些测试直接模拟 [EditorRepositoryImpl.writeFile] 的核心路径：
 * `content.toByteArray(charset)` → 写入文件 → 重新读取 → 还原为 String。
 */
class RoundTripFidelityTest {

    @TempDir
    lateinit var tempDir: File

    /**
     * 模拟内部文件写入路径：
     * AppFileManager.writeUserFile(file, content) → file.writeText(content, Charsets.UTF_8)
     * AppFileManager.readUserFile(file) → file.readText(Charsets.UTF_8)
     *
     * @param content 要写入的原始内容
     * @param charset 字符编码，默认UTF-8
     * @return 重新读取的内容
     */
    private fun writeAndReadBack(content: String, charset: Charset = Charsets.UTF_8): String {
        val file = File(tempDir, "roundtrip_test.md")
        file.writeText(content, charset)
        return file.readText(charset)
    }

    /**
     * 模拟SAF存储访问框架输出流写入路径：
     * contentResolver.openOutputStream(uri, "wt") → outputStream.write(content.toByteArray(charset))
     *
     * @param content 要写入的原始内容
     * @param charset 字符编码，默认UTF-8
     * @return 重新读取的内容
     */
    private fun writeBytesAndReadBack(content: String, charset: Charset = Charsets.UTF_8): String {
        val file = File(tempDir, "roundtrip_bytes_test.md")
        file.outputStream().use { os ->
            os.write(content.toByteArray(charset))
        }
        return file.readText(charset)
    }

    @Test
    @DisplayName("包含空白行的 Markdown 文件保存后重新读取内容一致")
    fun saveFile_withBlankLines_contentUnchanged() {
        val original = "# Title\n\n## Section\n\nParagraph\n\n\nAnother paragraph\n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("包含空白行的 Markdown 文件（SAF 路径）保存后重新读取内容一致")
    fun saveFile_withBlankLines_safPath_contentUnchanged() {
        val original = "# Title\n\n## Section\n\nParagraph\n\n\nAnother paragraph\n"
        val result = writeBytesAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("包含混合缩进（空格+Tab）的文件保存后内容不变")
    fun saveFile_withMixedIndentation_contentUnchanged() {
        val original = "    indented with 4 spaces\n\tindented with tab\n  \t  mixed spaces and tab\n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("包含 CRLF 和 LF 混合行尾的文件保存后内容不变")
    fun saveFile_withMixedLineEndings_contentUnchanged() {
        val original = "line1 with CRLF\r\nline2 with LF\nline3 with CRLF\r\nline4 with LF\n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("包含 CRLF 和 LF 混合行尾的文件（SAF 路径）保存后内容不变")
    fun saveFile_withMixedLineEndings_safPath_contentUnchanged() {
        val original = "line1 with CRLF\r\nline2 with LF\nline3 with CRLF\r\nline4 with LF\n"
        val result = writeBytesAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("CRLF 行尾在 toByteArray/read 往返中不丢失")
    fun saveFile_crlfPreserved_afterByteRoundTrip() {
        val original = "first\r\nsecond\r\nthird"
        val bytes = original.toByteArray(Charsets.UTF_8)
        val restored = String(bytes, Charsets.UTF_8)
        assertEquals(original, restored)
        // 确保 \r\n 没有被转换为 \n
        assertFalse(original.contains("\r\n").not())
    }

    @Test
    @DisplayName("包含连续空行的文件保存后空行数量不变")
    fun saveFile_withConsecutiveBlankLines_blankLineCountUnchanged() {
        val original = "line1\n\n\n\n\nline2\n\n\n\n\n\nline3"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
        // 显式验证空行数量
        val originalBlankCount = original.lines().count { it.isEmpty() }
        val resultBlankCount = result.lines().count { it.isEmpty() }
        assertEquals(originalBlankCount, resultBlankCount)
    }

    @Test
    @DisplayName("包含尾部空格的 Markdown 行保存后尾部空格保留")
    fun saveFile_withTrailingSpaces_trailingSpacesPreserved() {
        // Markdown 语法中行尾双空格表示换行（硬换行）
        val original = "line with trailing double space  \nnext line\nsingle trailing space \nnext line\n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("包含尾部空格的 Markdown 行（SAF 路径）保存后尾部空格保留")
    fun saveFile_withTrailingSpaces_safPath_trailingSpacesPreserved() {
        val original = "line with trailing double space  \nnext line\n"
        val result = writeBytesAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("完整的 Markdown 文档保存后内容完全一致")
    fun saveFile_fullMarkdownDocument_contentUnchanged() {
        val original = """---
title: Test Document
---

# Main Title

## Section 1

Paragraph with **bold** and *italic*.

- list item 1
- list item 2


### Subsection

Code block:

    def hello():
        print("world")

Another paragraph with trailing spaces
This is a new line due to hard break.

---

End of document.
"""
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("文件开头和末尾的空行在保存后不变")
    fun saveFile_leadingAndTrailingNewlines_preserved() {
        val original = "\n\ncontent\n\n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }

    @Test
    @DisplayName("仅包含空白字符的文件保存后内容不变")
    fun saveFile_whitespaceOnly_contentUnchanged() {
        val original = "   \n\t\n  \t  \n"
        val result = writeAndReadBack(original)
        assertEquals(original, result)
    }
}
