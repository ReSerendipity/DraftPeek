/**
 * 文本差异比较引擎模块。
 *
 * 基于 java-diff-utils (io.github.java-diff-utils:java-diff-utils) 成熟库实现，
 * 替换原自研 Myers 算法。保留原有公共 API（[DiffLine]、[DiffType]、[DiffResult]），
 * 内部委托给 java-diff-utils 的 Myers/Histogram 算法引擎。
 *
 * 优势：
 * - 使用社区维护的 200+ 测试用例覆盖的成熟库
 * - 支持 Myers / Histogram / Patience 三种算法
 * - 保留原有 5000 行 OOM 防护
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.ChangeDelta
import com.github.difflib.patch.DeleteDelta
import com.github.difflib.patch.InsertDelta

/**
 * 并排比较中的单行差异数据类。
 *
 * @property lineNumber 行号（从1开始）
 * @property content 行内容
 * @property type 差异类型
 */
data class DiffLine(
    val lineNumber: Int,
    val content: String,
    val type: DiffType,
)

/**
 * 差异类型枚举。
 */
enum class DiffType {
    /** 两行相等 */
    EQUAL,
    /** 右侧新增行 */
    INSERT,
    /** 左侧删除行 */
    DELETE,
    /** 修改行（删除+新增配对） */
    MODIFY,
}

/**
 * 差异比较结果数据类。
 *
 * @property leftLines 左侧（原始文本）的差异行列表
 * @property rightLines 右侧（新文本）的差异行列表
 * @property diffCount 差异行数统计
 */
data class DiffResult(
    val leftLines: List<DiffLine>,
    val rightLines: List<DiffLine>,
    val diffCount: Int,
)

/**
 * 文本差异引擎单例对象。
 *
 * 委托给 java-diff-utils 库实现差异比较，保留 5000 行 OOM 防护。
 *
 * 优化要点：
 * - 使用 java-diff-utils 的 Myers 算法（含前缀/后缀修剪优化）
 * - MAX_DIFF_LINES 防护防止超大文件 OOM
 * - 保留原有 [DiffResult] / [DiffLine] / [DiffType] 公共 API
 * - 纯函数，完全可单元测试
 */
object DiffEngine {

    /** 最大差异比较行数；超过此限制的文件返回只读视图（全部标记为EQUAL） */
    private const val MAX_DIFF_LINES = 5000

    /**
     * 比较两个文本并生成并排差异结果。
     *
     * @param leftText 左侧（原始）文本
     * @param rightText 右侧（新）文本
     * @return 包含左右两侧差异行和差异统计的DiffResult
     */
    fun diff(leftText: String, rightText: String): DiffResult {
        // 快速路径：完全相同的文本无需运行差异算法
        if (leftText == rightText) {
            val lines = leftText.lines()
            val effectiveLines = if (leftText.isEmpty()) emptyList() else lines
            return DiffResult(
                leftLines = effectiveLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.EQUAL) },
                rightLines = effectiveLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.EQUAL) },
                diffCount = 0,
            )
        }

        // Kotlin's String.lines() 对于空字符串返回 listOf("")，这不符合差异算法预期——空文本应为空行列表
        val leftLines = when {
            leftText.isEmpty() -> emptyList()
            leftText == "\n" || leftText == "\r\n" -> listOf("")
            else -> leftText.lines().let {
                if (leftText.endsWith('\n')) it + "" else it
            }
        }
        val rightLines = when {
            rightText.isEmpty() -> emptyList()
            rightText == "\n" || rightText == "\r\n" -> listOf("")
            else -> rightText.lines().let {
                if (rightText.endsWith('\n')) it + "" else it
            }
        }

        if (leftLines.size > MAX_DIFF_LINES || rightLines.size > MAX_DIFF_LINES) {
            return DiffResult(
                leftLines = leftLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.EQUAL) },
                rightLines = rightLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.EQUAL) },
                diffCount = 0,
            )
        }

        // 快速路径：一侧为空
        if (leftLines.isEmpty()) {
            val right = rightLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.INSERT) }
            val padding = List(right.size) { DiffLine(0, "", DiffType.EQUAL) }
            return DiffResult(
                leftLines = padding,
                rightLines = right,
                diffCount = right.size,
            )
        }
        if (rightLines.isEmpty()) {
            val left = leftLines.mapIndexed { i, line -> DiffLine(i + 1, line, DiffType.DELETE) }
            val padding = List(left.size) { DiffLine(0, "", DiffType.EQUAL) }
            return DiffResult(
                leftLines = left,
                rightLines = padding,
                diffCount = left.size,
            )
        }

        // 委托给 java-diff-utils 进行差异比较
        val patch = DiffUtils.diff(leftLines, rightLines)

        // 将 java-diff-utils 的 Patch 转换为并排 DiffResult
        val (rawLeft, rawRight) = buildRawDiff(patch.getDeltas(), leftLines, rightLines)
        padToEqualLength(rawLeft, rawRight)
        val (resultLeft, resultRight) = assignLineNumbers(rawLeft, rawRight)

        val diffCount = resultLeft.count { it.type != DiffType.EQUAL }

        return DiffResult(leftLines = resultLeft, rightLines = resultRight, diffCount = diffCount)
    }

    /**
     * 将 java-diff-utils 的 Delta 列表转换为配对的 [DiffLine] 行。
     *
     * 策略：ChangeDelta 转换为 MODIFY 行（取 min(delete, insert) 对），
     * DeleteDelta 转换为 DELETE 行，InsertDelta 转换为 INSERT 行。
     * 相等行在 Delta 之间的间隙自动填充。
     *
     * @param deltas java-diff-utils 的 Delta 列表
     * @param leftLines 左侧文本行
     * @param rightLines 右侧文本行
     * @return (rawLeft, rawRight) 配对的可变列表
     */
    private fun buildRawDiff(
        deltas: List<AbstractDelta<String>>,
        leftLines: List<String>,
        rightLines: List<String>,
    ): Pair<MutableList<DiffLine>, MutableList<DiffLine>> {
        val rawLeft = mutableListOf<DiffLine>()
        val rawRight = mutableListOf<DiffLine>()

        var leftPos = 0  // 下一个待处理的左侧行索引
        var rightPos = 0 // 下一个待处理的右侧行索引

        for (delta in deltas) {
            val sourceStart = delta.source.position
            val sourceEnd = sourceStart + delta.source.size()
            val targetStart = delta.target.position
            val targetEnd = targetStart + delta.target.size()

            // 填充 Delta 之前的相等行
            while (leftPos < sourceStart && rightPos < targetStart) {
                rawLeft.add(DiffLine(lineNumber = 0, content = leftLines[leftPos], type = DiffType.EQUAL))
                rawRight.add(DiffLine(lineNumber = 0, content = rightLines[rightPos], type = DiffType.EQUAL))
                leftPos++
                rightPos++
            }

            when (delta) {
                is ChangeDelta -> {
                    // ChangeDelta: 既有删除又有新增，配对为 MODIFY
                    val deleteLines = delta.source.lines
                    val insertLines = delta.target.lines
                    val pairCount = minOf(deleteLines.size, insertLines.size)
                    for (i in 0 until pairCount) {
                        rawLeft.add(DiffLine(lineNumber = 0, content = deleteLines[i], type = DiffType.MODIFY))
                        rawRight.add(DiffLine(lineNumber = 0, content = insertLines[i], type = DiffType.MODIFY))
                    }
                    for (i in pairCount until deleteLines.size) {
                        rawLeft.add(DiffLine(lineNumber = 0, content = deleteLines[i], type = DiffType.DELETE))
                        rawRight.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
                    }
                    for (i in pairCount until insertLines.size) {
                        rawLeft.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
                        rawRight.add(DiffLine(lineNumber = 0, content = insertLines[i], type = DiffType.INSERT))
                    }
                }
                is DeleteDelta -> {
                    for (i in 0 until delta.source.size()) {
                        rawLeft.add(DiffLine(lineNumber = 0, content = delta.source.lines[i], type = DiffType.DELETE))
                        rawRight.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
                    }
                }
                is InsertDelta -> {
                    for (i in 0 until delta.target.size()) {
                        rawLeft.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
                        rawRight.add(DiffLine(lineNumber = 0, content = delta.target.lines[i], type = DiffType.INSERT))
                    }
                }
            }

            leftPos = sourceEnd
            rightPos = targetEnd
        }

        // 填充最后一个 Delta 之后的相等行
        while (leftPos < leftLines.size && rightPos < rightLines.size) {
            rawLeft.add(DiffLine(lineNumber = 0, content = leftLines[leftPos], type = DiffType.EQUAL))
            rawRight.add(DiffLine(lineNumber = 0, content = rightLines[rightPos], type = DiffType.EQUAL))
            leftPos++
            rightPos++
        }

        // 填充剩余行（一侧比另一侧长）
        while (leftPos < leftLines.size) {
            rawLeft.add(DiffLine(lineNumber = 0, content = leftLines[leftPos], type = DiffType.DELETE))
            rawRight.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
            leftPos++
        }
        while (rightPos < rightLines.size) {
            rawLeft.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
            rawRight.add(DiffLine(lineNumber = 0, content = rightLines[rightPos], type = DiffType.INSERT))
            rightPos++
        }

        return rawLeft to rawRight
    }

    /**
     * 填充左右差异列表使它们长度相等。
     */
    private fun padToEqualLength(left: MutableList<DiffLine>, right: MutableList<DiffLine>) {
        while (left.size < right.size) {
            left.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
        }
        while (right.size < left.size) {
            right.add(DiffLine(lineNumber = 0, content = "", type = DiffType.EQUAL))
        }
    }

    /**
     * 为原始差异行分配正确的行号。
     */
    private fun assignLineNumbers(
        rawLeft: List<DiffLine>,
        rawRight: List<DiffLine>,
    ): Pair<List<DiffLine>, List<DiffLine>> {
        var leftNum = 0
        var rightNum = 0
        val resultLeft = ArrayList<DiffLine>(rawLeft.size)
        val resultRight = ArrayList<DiffLine>(rawRight.size)

        for (idx in rawLeft.indices) {
            val ll = rawLeft[idx]
            val rl = rawRight[idx]

            if (ll.type != DiffType.INSERT && ll.content.isNotEmpty()) leftNum++
            if (rl.type != DiffType.DELETE && rl.content.isNotEmpty()) rightNum++

            resultLeft.add(ll.copy(lineNumber = leftNum))
            resultRight.add(rl.copy(lineNumber = rightNum))
        }
        return resultLeft to resultRight
    }
}
