/**
 * 文件功能：简单代码诊断提供者，通过正则和启发式规则扫描代码问题
 * 
 * 主要类/对象：
 * - [DiagnosticItem]：单个诊断项数据类
 * - [SimpleDiagnosticProvider]：诊断提供者单例对象
 * 
 * 模块依赖：
 * - androidx.compose.runtime：Compose 不可变注解
 * 
 * 检查项：
 * 1. 括号不匹配：( 无 )、[ 无 ]、{ 无 }
 * 2. 缩进不匹配（反缩进而无闭合括号）
 * 3. TODO/FIXME/HACK/XXX 注释标记
 * 4. 行尾尾随空白
 * 5. 超过 [LONG_LINE_THRESHOLD] 字符的长行
 * 6. 缩进中混用 Tab 和空格
 * 
 * 性能设计：所有扫描都是 O(n) 单遍操作，足够轻量可在每次内容变更时运行（带防抖）。
 */
package com.draftpeek.feature.editor.diagnostics

import androidx.compose.runtime.Immutable

/**
 * 单个诊断项（由 [SimpleDiagnosticProvider] 产生）
 * 
 * 标记为 @Immutable 因为所有属性都是 val，允许 Compose 在重新发射相同实例时
 * 跳过重组（利用 data class 的相等性）。
 * 
 * @property startIndex 诊断起始字符偏移（0-based）
 * @property endIndex 诊断结束字符偏移（0-based，不包含）
 * @property severity 严重级别，取值为 [SEVERITY_TYPO]、[SEVERITY_WARNING]、[SEVERITY_ERROR] 之一
 * @property message 在工具提示中显示的人类可读描述
 */
@Immutable
data class DiagnosticItem(
    val startIndex: Int,
    val endIndex: Int,
    val severity: Short,
    val message: String,
) {
    companion object {
        /** 提示级别：TODO、尾随空白、风格建议 */
        const val SEVERITY_TYPO: Short = 1
        /** 警告级别：缩进问题、长行、混用缩进 */
        const val SEVERITY_WARNING: Short = 2
        /** 错误级别：括号不匹配、语法错误 */
        const val SEVERITY_ERROR: Short = 3
    }
}

/**
 * 简单诊断提供者（单例对象）
 * 
 * 通过正则表达式和启发式规则扫描代码内容，提供基础的代码问题诊断。
 * 设计为足够轻量，可在每次内容变更时运行（带防抖）。
 */
object SimpleDiagnosticProvider {

    private const val MAX_DIAGNOSTICS = 50

    /** 超过此字符数的行将触发警告 */
    private const val LONG_LINE_THRESHOLD = 120

    /**
     * 扫描 [content] 并返回 [DiagnosticItem] 列表
     * 
     * 扫描流程：
     * 1. 首先检查括号平衡
     * 2. 如果诊断数量未达上限，继续检查行级问题
     * 3. 按 startIndex 排序
     * 4. 截断到 [MAX_DIAGNOSTICS] 个
     * 
     * @param content 编辑器完整文本
     * @param language 可选语言提示（如 "python"）——缩进和注释规则因语言而异
     * @return 按 startIndex 排序的诊断列表，上限为 [MAX_DIAGNOSTICS]
     */
    fun analyze(content: String, language: String? = null): List<DiagnosticItem> {
        val diagnostics = mutableListOf<DiagnosticItem>()

        checkBracketBalance(content, diagnostics)
        if (diagnostics.size < MAX_DIAGNOSTICS) {
            checkLineIssues(content, language, diagnostics)
        }

        diagnostics.sortBy { it.startIndex }
        return diagnostics.take(MAX_DIAGNOSTICS)
    }

    // ------------------------------------------------------------------
    // 括号平衡检查
    // ------------------------------------------------------------------

    /**
     * 检查括号匹配平衡（单遍扫描优化）
     * 
     * 优化点：原实现对 ()、[]、{} 分别做3次完整字符串遍历（O(3n)），
     * 优化后单遍遍历同时跟踪三种括号（O(n)），减少2次完整字符串扫描，
     * 对大文件（500KB）约节省约100万次字符比较。
     * 
     * 算法步骤：
     * 1. 单遍遍历所有字符
     * 2. 为每种括号类型维护独立的栈（ArrayDeque）
     * 3. 遇到开括号：压入对应栈
     * 4. 遇到闭括号：如果对应栈为空，记录"不匹配的闭括号"错误；否则弹出栈顶
     * 5. 遍历结束后，三个栈中剩余的开括号全部记录为"不匹配的开括号"错误
     * 
     * @param content 要检查的文本内容
     * @param out 输出诊断列表
     */
    private fun checkBracketBalance(content: String, out: MutableList<DiagnosticItem>) {
        val parenStack = ArrayDeque<Int>()
        val bracketStack = ArrayDeque<Int>()
        val braceStack = ArrayDeque<Int>()

        var i = 0
        val len = content.length
        while (i < len && out.size < MAX_DIAGNOSTICS) {
            when (val ch = content[i]) {
                '(' -> parenStack.addLast(i)
                ')' -> {
                    if (parenStack.isEmpty()) {
                        out.add(DiagnosticItem(
                            startIndex = i,
                            endIndex = i + 1,
                            severity = DiagnosticItem.SEVERITY_ERROR,
                            message = "不匹配的 ')'"
                        ))
                    } else {
                        parenStack.removeLast()
                    }
                }
                '[' -> bracketStack.addLast(i)
                ']' -> {
                    if (bracketStack.isEmpty()) {
                        out.add(DiagnosticItem(
                            startIndex = i,
                            endIndex = i + 1,
                            severity = DiagnosticItem.SEVERITY_ERROR,
                            message = "不匹配的 ']'"
                        ))
                    } else {
                        bracketStack.removeLast()
                    }
                }
                '{' -> braceStack.addLast(i)
                '}' -> {
                    if (braceStack.isEmpty()) {
                        out.add(DiagnosticItem(
                            startIndex = i,
                            endIndex = i + 1,
                            severity = DiagnosticItem.SEVERITY_ERROR,
                            message = "不匹配的 '}'"
                        ))
                    } else {
                        braceStack.removeLast()
                    }
                }
            }
            i++
        }

        // 报告未闭合的开括号
        fun reportUnclosed(stack: ArrayDeque<Int>, ch: Char) {
            for (openIndex in stack) {
                if (out.size >= MAX_DIAGNOSTICS) return
                out.add(DiagnosticItem(
                    startIndex = openIndex,
                    endIndex = openIndex + 1,
                    severity = DiagnosticItem.SEVERITY_ERROR,
                    message = "不匹配的 '$ch'"
                ))
            }
        }
        reportUnclosed(parenStack, '(')
        reportUnclosed(bracketStack, '[')
        reportUnclosed(braceStack, '{')
    }

    // ------------------------------------------------------------------
    // 行级问题检查：尾随空白、长行、TODO、混用缩进
    // ------------------------------------------------------------------

    /**
     * 检查行级问题（性能优化版）
     * 
     * 优化点：
     * - 内联括号计数，避免 countChar 对每行做两次遍历
     * - 前导空白分析单次扫描同时检测 Tab/Space 混合和长度计算
     * - 减少不必要的中间字符串创建
     * 
     * 检查项：
     * - 行尾尾随空白
     * - 超长行警告
     * - TODO/FIXME 等注释标记
     * - 缩进中混用 Tab 和空格
     * - 括号深度/缩进不匹配（仅适用于基于括号的语言）
     * 
     * @param content 要检查的文本内容
     * @param language 语言标识符
     * @param out 输出诊断列表
     */
    private fun checkLineIssues(
        content: String,
        language: String?,
        out: MutableList<DiagnosticItem>,
    ) {
        val isIndentSignificant = language in INDENT_SIGNIFICANT_LANGUAGES
        val lineCommentPrefixes = getLineCommentPrefixes(language)

        var charOffset = 0
        var braceDepth = 0

        val lines = content.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            if (out.size >= MAX_DIAGNOSTICS) break

            val lineLength = line.length
            val lineEnd = charOffset + lineLength

            // --- 尾随空白（从末尾反向扫描，避免创建trimmedEnd字符串） ---
            if (lineLength > 0) {
                var wsEnd = lineLength - 1
                while (wsEnd >= 0 && line[wsEnd].isWhitespace()) { wsEnd-- }
                val wsStart = wsEnd + 1
                if (wsStart < lineLength) {
                    out.add(DiagnosticItem(
                        startIndex = charOffset + wsStart,
                        endIndex = lineEnd,
                        severity = DiagnosticItem.SEVERITY_TYPO,
                        message = "行尾尾随空白"
                    ))
                }
            }

            // --- 长行 ---
            if (lineLength > LONG_LINE_THRESHOLD) {
                out.add(DiagnosticItem(
                    startIndex = charOffset + LONG_LINE_THRESHOLD,
                    endIndex = lineEnd,
                    severity = DiagnosticItem.SEVERITY_WARNING,
                    message = "行长度为 $lineLength 字符（超过 $LONG_LINE_THRESHOLD）"
                ))
            }

            // --- 注释中的 TODO/FIXME/HACK/XXX ---
            checkTodoComments(line, charOffset, lineCommentPrefixes, out)
            if (out.size >= MAX_DIAGNOSTICS) break

            // --- 缩进分析（单次扫描同时计算长度和检测Tab/Space混合） ---
            var leadingWsLen = 0
            var hasTab = false
            var hasSpace = false
            while (leadingWsLen < lineLength) {
                val ch = line[leadingWsLen]
                if (ch == ' ') hasSpace = true
                else if (ch == '\t') hasTab = true
                else break
                leadingWsLen++
            }

            if (leadingWsLen > 0 && leadingWsLen < lineLength) {
                // 缩进中混用 Tab 和空格
                if (hasTab && hasSpace) {
                    out.add(DiagnosticItem(
                        startIndex = charOffset,
                        endIndex = charOffset + leadingWsLen,
                        severity = DiagnosticItem.SEVERITY_WARNING,
                        message = "缩进中混用 Tab 和空格"
                    ))
                }

                // 括号深度/缩进不匹配（仅基于括号的语言）
                if (!isIndentSignificant) {
                    val trimmed = line.substring(leadingWsLen)
                    if (trimmed.isNotBlank() && !isCommentLine(trimmed, lineCommentPrefixes)) {
                        // 内联括号计数，避免两次countChar遍历
                        var openBraces = 0
                        var closeBraces = 0
                        for (c in trimmed) {
                            when (c) {
                                '{' -> openBraces++
                                '}' -> closeBraces++
                            }
                        }

                        val prevDepth = braceDepth
                        braceDepth -= closeBraces
                        if (braceDepth < 0) braceDepth = 0
                        braceDepth += openBraces

                        if (closeBraces > 0 && braceDepth < prevDepth) {
                            val expectedIndent = braceDepth * 4
                            if (leadingWsLen != expectedIndent && lineIdx > 0) {
                                out.add(DiagnosticItem(
                                    startIndex = charOffset,
                                    endIndex = charOffset + minOf(leadingWsLen, line.length),
                                    severity = DiagnosticItem.SEVERITY_WARNING,
                                    message = "缩进不匹配：期望 $expectedIndent 个空格"
                                ))
                            }
                        }
                    }
                }
            }

            charOffset += lineLength + 1 // +1 for '\n'
        }
    }

    /**
     * 检测行注释中的 TODO/FIXME/HACK/XXX/BUG/NB 标记
     *
     * 扫描行中的这些关键字（不区分大小写）并将它们标记为提示级别。
     * 仅标记关键字本身，不标记整行。
     *
     * 优化：使用 regionMatches(ignoreCase = true) 进行匹配，避免为每行创建
     * uppercase() 副本字符串，减少大文件扫描时的 GC 压力。
     *
     * 单词边界检查：关键字前后的字符不能是字母或数字。
     *
     * @param line 当前行文本
     * @param charOffset 当前行在全文中的字符偏移
     * @param commentPrefixes 行注释前缀列表
     * @param out 输出诊断列表
     */
    private fun checkTodoComments(
        line: String,
        charOffset: Int,
        commentPrefixes: List<String>,
        out: MutableList<DiagnosticItem>,
    ) {
        val todoKeywords = listOf("TODO", "FIXME", "HACK", "XXX", "BUG", "NB", "NOTE", "OPTIMIZE")

        // 首先检查行是否以注释前缀开头
        val trimmedStart = line.trimStart()
        val isCommentLine = commentPrefixes.any { trimmedStart.startsWith(it) }

        for (keyword in todoKeywords) {
            if (out.size >= MAX_DIAGNOSTICS) break
            var searchFrom = 0
            while (searchFrom < line.length) {
                val idx = indexOfIgnoreCase(line, keyword, searchFrom)
                if (idx < 0) break
                val afterPos = idx + keyword.length
                // 单词边界检查：关键字前后的字符不能是字母/数字（isLetterOrDigit 大小写无关）
                val beforeOk = idx == 0 || !line[idx - 1].isLetterOrDigit()
                val afterOk = afterPos >= line.length || !line[afterPos].isLetterOrDigit()
                if (beforeOk && afterOk) {
                    // 在非注释行中，仅当 TODO 类关键字出现在字符串类上下文中时才标记；
                    // 为简单起见，始终标记它们但使用 TYPO 级别。
                    out.add(DiagnosticItem(
                        startIndex = charOffset + idx,
                        endIndex = charOffset + afterPos,
                        severity = DiagnosticItem.SEVERITY_TYPO,
                        message = "$keyword 注释标记"
                    ))
                }
                searchFrom = afterPos
            }
        }
    }

    /**
     * 在 [source] 中从 [startIndex] 开始不区分大小写地查找 [target]，
     * 使用 regionMatches(ignoreCase = true) 逐位置比较，不创建额外字符串。
     *
     * @return 匹配位置的起始索引，未找到返回 -1
     */
    private fun indexOfIgnoreCase(source: String, target: String, startIndex: Int): Int {
        val sourceLen = source.length
        val targetLen = target.length
        if (startIndex >= sourceLen) return -1
        if (targetLen == 0) return startIndex
        val maxStart = sourceLen - targetLen
        if (startIndex > maxStart) return -1

        var i = startIndex
        while (i <= maxStart) {
            if (source.regionMatches(i, target, 0, targetLen, ignoreCase = true)) {
                return i
            }
            i++
        }
        return -1
    }

    // ------------------------------------------------------------------
    // 辅助函数
    // ------------------------------------------------------------------

    /**
     * 判断修剪后的行是否是注释行
     *
     * @param trimmed 修剪后的行文本
     * @param prefixes 注释前缀列表
     * @return 如果是注释行返回 true
     */
    private fun isCommentLine(trimmed: String, prefixes: List<String>): Boolean {
        return prefixes.any { trimmed.startsWith(it) }
    }

    /**
     * 返回给定语言的行注释前缀
     * 
     * 用于在缩进检查中识别纯注释行。
     * 
     * @param language 语言标识符
     * @return 行注释前缀列表
     */
    private fun getLineCommentPrefixes(language: String?): List<String> {
        return when (language?.lowercase()) {
            "python", "bash", "shell", "sh", "ruby", "perl", "yaml", "yml", "coffeescript", "r" ->
                listOf("#")
            "sql", "lua", "ada", "haskell", "--" ->
                listOf("--")
            "php" -> listOf("//", "#")
            "html", "xml", "htm" -> listOf("<!--")
            "matlab" -> listOf("%")
            "lisp", "clojure", "scheme" -> listOf(";")
            else -> listOf("//") // C 风格：Kotlin、Java、C++、C#、JS、TS、Go、Rust、Swift、Dart、Scala 等
        }
    }

    /** 缩进具有语义意义的语言（不使用基于括号的块） */
    private val INDENT_SIGNIFICANT_LANGUAGES = setOf(
        "python", "yaml", "yml", "coffeescript", "haml", "slim", "jade", "pug"
    )
}
