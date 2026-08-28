/**
 * 文件功能：编辑器硬件键盘快捷键处理器
 *
 * 主要类：
 * - [KeyboardShortcutHandler]：键盘快捷键处理类，将常用 Ctrl+key 组合映射到编辑器操作
 *
 * 模块依赖：
 * - android.view.KeyEvent：Android 按键事件
 * - androidx.compose.runtime：Compose 稳定注解
 * - feature.editor.ui.MarkdownFormatAction：Markdown 格式化操作
 *
 * 设计说明：
 * - sora-editor 原生处理的快捷键（Ctrl+S、Ctrl+Z、Ctrl+Y、Ctrl+C、Ctrl+V、Ctrl+X）不在这里拦截
 * - 当 [isMarkdownMode] 为 true 时，启用额外的 Markdown 专用快捷键（Ctrl+B 粗体、Ctrl+I 斜体等）
 * - 操作通过函数式接口注入，保持处理器与 UI 层解耦
 */
package com.draftpeek.feature.editor.input

import android.view.KeyEvent
import androidx.compose.runtime.Stable
import com.draftpeek.feature.editor.ui.MarkdownFormatAction

/**
 * 编辑器硬件键盘快捷键处理器
 *
 * 职责：映射物理键盘快捷键到编辑器操作，通过回调函数与 UI 层解耦。
 *
 * 支持的快捷键：
 * - Ctrl+S：保存文件
 * - Ctrl+Z：撤销
 * - Ctrl+Shift+Z / Ctrl+Y：重做
 * - Ctrl+F：打开搜索面板
 * - Ctrl+H：打开搜索替换面板
 * - Ctrl+G：转到行
 * - Ctrl+A：全选
 * - Ctrl+Shift+P：打开命令面板
 * - F3 / Shift+F3：查找下一个/上一个
 * - F8：切换专注模式
 * - F9：切换打字机模式
 * - Ctrl+Alt+Left：跳转到上次编辑位置
 * - Tab/Shift+Tab：Markdown 列表缩进/反缩进
 * - Markdown 模式：Ctrl+B 粗体、Ctrl+I 斜体、Ctrl+K 链接、Ctrl+Shift+K 代码块、Ctrl+Shift+M 数学公式
 *
 * @property onSave Ctrl+S – 保存当前文件
 * @property onUndo Ctrl+Z – 撤销（通常由编辑器处理，保留以完整性）
 * @property onRedo Ctrl+Shift+Z / Ctrl+Y – 重做
 * @property onOpenSearch Ctrl+F – 打开搜索面板
 * @property onOpenReplace Ctrl+H – 打开搜索替换面板
 * @property onGoToLine Ctrl+G – 显示转到行对话框
 * @property onSelectAll Ctrl+A – 全选文本
 * @property onOpenCommandPalette Ctrl+Shift+P – 打开命令面板
 * @property onMarkdownFormatAction Markdown 格式化操作回调
 * @property getCurrentLineText 返回当前行文本（用于 Tab/Shift+Tab 列表检测）
 * @property onFindNext F3 – 跳转到下一个搜索匹配
 * @property onFindPrevious Shift+F3 – 跳转到上一个搜索匹配
 * @property onToggleFocusMode F8 – 切换专注模式
 * @property onToggleTypewriterMode F9 – 切换打字机模式
 * @property onGoToLastEditLocation Ctrl+Alt+Left – 跳转到上次编辑位置
 */
@Stable
class KeyboardShortcutHandler(
    val onSave: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onRedo: () -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onOpenReplace: () -> Unit = {},
    val onGoToLine: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onOpenCommandPalette: (() -> Unit)? = null,
    val onMarkdownFormatAction: ((MarkdownFormatAction) -> Unit)? = null,
    val getCurrentLineText: (() -> String)? = null,
    val onToggleFocusMode: (() -> Unit)? = null,
    val onToggleTypewriterMode: (() -> Unit)? = null,
    val onGoToLastEditLocation: (() -> Unit)? = null,
    val onFindNext: (() -> Unit)? = null,
    val onFindPrevious: (() -> Unit)? = null
) {

    /** 当前文件是否为 Markdown 文件（启用 Markdown 快捷键） */
    var isMarkdownMode: Boolean = false
        private set

    /**
     * 更新 Markdown 模式状态
     *
     * 当活动文件变化时调用。
     *
     * @param isMarkdown 是否为 Markdown 模式
     */
    fun setMarkdownMode(isMarkdown: Boolean) {
        isMarkdownMode = isMarkdown
    }

    /**
     * 处理按键事件，返回快捷键是否已处理
     *
     * 处理流程：
     * 1. 检查是否为 ACTION_DOWN 事件，否则返回 false
     * 2. 读取 Ctrl/Shift/Alt 修饰键状态
     * 3. 处理功能键（F3、F8、F9、Ctrl+Alt+Left）
     * 4. 处理 Tab/Shift+Tab（Markdown 列表缩进）
     * 5. 处理 Ctrl 组合键
     *
     * @param event 按键事件
     * @return 如果快捷键已处理返回 true，否则返回 false
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val ctrl = event.isCtrlPressed
        val shift = event.isShiftPressed
        val alt = event.isAltPressed

        // F8 - 专注模式
        if (event.keyCode == KeyEvent.KEYCODE_F8) {
            onToggleFocusMode?.invoke()
            return true
        }

        // F9 - 打字机模式
        if (event.keyCode == KeyEvent.KEYCODE_F9) {
            onToggleTypewriterMode?.invoke()
            return true
        }

        // F3 - 查找下一个 / Shift+F3 - 查找上一个
        if (event.keyCode == KeyEvent.KEYCODE_F3) {
            if (shift) {
                onFindPrevious?.invoke()
            } else {
                onFindNext?.invoke()
            }
            return true
        }

        // Ctrl+Alt+Left – 跳转到上次编辑位置
        if (ctrl && alt && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            onGoToLastEditLocation?.invoke()
            return true
        }

        // 处理 Tab/Shift+Tab 用于 Markdown 列表缩进
        if (event.keyCode == KeyEvent.KEYCODE_TAB && !ctrl) {
            if (isMarkdownMode && onMarkdownFormatAction != null && getCurrentLineText != null) {
                val lineText = getCurrentLineText.invoke()
                val trimmed = lineText.trimStart()
                val isListLine = trimmed.startsWith("- ") ||
                    trimmed.startsWith("* ") ||
                    trimmed.startsWith("+ ") ||
                    trimmed.matches(Regex("^\\d+[.)]\\s.*"))
                if (isListLine) {
                    if (shift) {
                        // Shift+Tab：减少缩进（移除前导空格）
                        val leadingSpaces = lineText.takeWhile { it == ' ' }
                        if (leadingSpaces.isNotEmpty()) {
                            val removeCount = minOf(2, leadingSpaces.length)
                            handleDecreaseIndent(removeCount)
                        }
                    } else {
                        // Tab：增加缩进
                        onMarkdownFormatAction.invoke(MarkdownFormatAction.LinePrefix("  "))
                    }
                    return true
                }
            }
            return false
        }

        if (!ctrl) return false

        return when (event.keyCode) {
            // Ctrl+S – 保存
            KeyEvent.KEYCODE_S -> {
                if (!shift) {
                    onSave()
                    true
                } else {
                    false
                }
            }
            // Ctrl+Z – 撤销
            KeyEvent.KEYCODE_Z -> {
                if (!shift) {
                    onUndo()
                    true
                } else {
                    onRedo()
                    true
                }
            }
            // Ctrl+Y – 重做
            KeyEvent.KEYCODE_Y -> {
                onRedo()
                true
            }
            // Ctrl+F – 搜索
            KeyEvent.KEYCODE_F -> {
                if (!shift) {
                    onOpenSearch()
                    true
                } else {
                    false
                }
            }
            // Ctrl+H – 替换
            KeyEvent.KEYCODE_H -> {
                if (!shift) {
                    onOpenReplace()
                    true
                } else {
                    false
                }
            }
            // Ctrl+G – 转到行
            KeyEvent.KEYCODE_G -> {
                if (!shift) {
                    onGoToLine()
                    true
                } else {
                    false
                }
            }
            // Ctrl+A – 全选
            KeyEvent.KEYCODE_A -> {
                if (!shift) {
                    onSelectAll()
                    true
                } else {
                    false
                }
            }
            // --- Markdown 快捷键（仅 isMarkdownMode 为 true 时生效） ---
            // Ctrl+B – 粗体
            KeyEvent.KEYCODE_B -> {
                if (isMarkdownMode && !shift && onMarkdownFormatAction != null) {
                    onMarkdownFormatAction(MarkdownFormatAction.Wrap("**", "**", tplBold))
                    true
                } else {
                    false
                }
            }
            // Ctrl+I – 斜体
            KeyEvent.KEYCODE_I -> {
                if (isMarkdownMode && !shift && onMarkdownFormatAction != null) {
                    onMarkdownFormatAction(MarkdownFormatAction.Wrap("*", "*", tplItalic))
                    true
                } else {
                    false
                }
            }
            // Ctrl+K / Ctrl+Shift+K – 插入链接 / 插入代码块
            KeyEvent.KEYCODE_K -> {
                if (isMarkdownMode && onMarkdownFormatAction != null) {
                    if (shift) {
                        onMarkdownFormatAction(MarkdownFormatAction.Insert(tplCodeBlock, 4))
                    } else {
                        onMarkdownFormatAction(MarkdownFormatAction.Insert(tplLink, 1))
                    }
                    true
                } else {
                    false
                }
            }
            // Ctrl+Shift+M – 插入数学公式
            KeyEvent.KEYCODE_M -> {
                if (isMarkdownMode && shift && onMarkdownFormatAction != null) {
                    onMarkdownFormatAction(MarkdownFormatAction.Insert(tplMathFormula, 4))
                    true
                } else {
                    false
                }
            }
            // Ctrl+Shift+P – 打开命令面板
            KeyEvent.KEYCODE_P -> {
                if (shift && onOpenCommandPalette != null) {
                    onOpenCommandPalette()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * 通过移除 [removeCount] 个前导空格来减少缩进
     *
     * 委托给 [onDecreaseIndent] 回调，该回调直接编辑编辑器文本。
     *
     * @param removeCount 要移除的前导空格数量
     */
    private fun handleDecreaseIndent(removeCount: Int) {
        onDecreaseIndent?.invoke(removeCount)
    }

    /** 可选回调：通过移除 [Int] 个前导空格来减少缩进 */
    var onDecreaseIndent: ((Int) -> Unit)? = null

    // Markdown 格式化模板字符串（由 UI 层使用本地化值设置）
    var tplBold: String = ""
    var tplItalic: String = ""
    var tplCodeBlock: String = ""
    var tplLink: String = ""
    var tplMathFormula: String = ""
}
