/**
 * 文件：EditorTab.kt
 * 功能：编辑器标签页数据模型定义
 * 主要类/接口：EditorTab
 * 模块依赖：
 *   - androidx.compose.runtime：Compose 不可变注解
 */
package com.draftpeek.feature.editor.model

import androidx.compose.runtime.Immutable
import com.draftpeek.core.common.model.TabId

/**
 * 表示单个打开的编辑器标签页。
 *
 * 使用 @Immutable 注解标记，因为所有属性均为 val 且为基本类型/String 类型，
 * 允许 Compose 在重新发射相同标签列表时跳过重组，提升多标签页切换性能。
 *
 * 使用场景：TabManager 管理多个打开的文件标签，每个标签对应一个 EditorTab 实例。
 *
 * @property id 标签页唯一标识符（通常使用 UUID 或 URI 哈希）
 * @property uri 文件的 URI 字符串，用于定位和读写文件
 * @property fileName 文件名（含扩展名），用于标签页显示
 * @property language 编程语言标识符，用于语法高亮；null 表示纯文本
 * @property isModified 内容是否已修改（未保存），用于显示修改标记（*）
 */
@Immutable
data class EditorTab(
    val id: TabId,
    val uri: String,
    val fileName: String,
    val language: String?,
    val isModified: Boolean = false,
)
