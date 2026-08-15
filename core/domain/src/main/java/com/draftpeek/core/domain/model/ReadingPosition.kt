/**
 * 阅读位置领域模型文件。
 *
 * 定义文件阅读位置的领域层数据模型，用于保存和恢复文件打开时的
 * 光标位置和滚动状态，提升用户体验。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.domain.model

/**
 * 表示文件中保存的阅读位置的领域模型。
 *
 * 用于在重新打开文件时恢复光标和滚动状态。
 *
 * @property uri 文件URI字符串
 * @property line 光标所在行号（从0开始）
 * @property column 光标所在列号（从0开始）
 * @property scrollX 水平滚动偏移量
 * @property scrollY 垂直滚动偏移量
 */
data class ReadingPosition(
    val uri: String,
    val line: Int,
    val column: Int,
    val scrollX: Int,
    val scrollY: Int,
)
