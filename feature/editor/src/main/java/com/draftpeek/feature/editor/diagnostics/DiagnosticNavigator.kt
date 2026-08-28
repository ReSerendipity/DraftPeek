/**
 * 文件功能：诊断导航系统，包含诊断导航状态管理和诊断导航器接口
 *
 * 主要类/接口：
 * - [DiagnosticNavigationState]：诊断项导航状态数据类
 * - [DiagnosticNavigator]：诊断导航器接口，管理跨编辑器会话的诊断状态
 * - [DefaultDiagnosticNavigator]：诊断导航器默认实现
 * - [moveToNext]：跳转到下一个诊断项的扩展函数
 * - [moveToPrevious]：跳转到上一个诊断项的扩展函数
 * - [fromDiagnostics]：从诊断列表创建导航状态的扩展函数
 *
 * 模块依赖：
 * - androidx.compose.runtime：Compose 状态注解
 * - kotlinx.collections.immutable：不可变集合
 * - kotlinx.coroutines.flow：状态流
 *
 * 内存保护：通过 MAX_TRACKED_DIAGNOSTICS 限制可导航的诊断数量，
 * 防止在有大量错误的文件（如压缩混淆后的文件）上发生 OOM。
 */
package com.draftpeek.feature.editor.diagnostics

import androidx.compose.runtime.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 诊断项导航状态
 *
 * 职责：管理可导航的诊断列表和当前焦点位置，支持用户通过键盘快捷键
 * （如 F8/Shift+F8 或 Ctrl+Period/Ctrl+Comma）在错误之间循环跳转。
 *
 * 同时跟踪当前聚焦的诊断项，以便在编辑器和诊断面板中进行视觉高亮。
 *
 * @property diagnostics 可导航的诊断项列表，上限为 [MAX_TRACKED_DIAGNOSTICS]
 * @property currentIndex 当前聚焦的诊断项索引，无焦点时为 -1
 */
@Immutable
data class DiagnosticNavigationState(
    val diagnostics: ImmutableList<DiagnosticItem> = persistentListOf(),
    val currentIndex: Int = -1
) {
    /** 当前聚焦的诊断项，无焦点时返回 null */
    val current: DiagnosticItem?
        get() = diagnostics.getOrNull(currentIndex)

    /** 是否存在下一个可跳转的诊断项 */
    val hasNext: Boolean
        get() = diagnostics.isNotEmpty() && currentIndex < diagnostics.lastIndex

    /** 是否存在上一个可跳转的诊断项 */
    val hasPrevious: Boolean
        get() = diagnostics.isNotEmpty() && currentIndex > 0

    /** 可导航诊断项的总数 */
    val count: Int
        get() = diagnostics.size

    companion object {
        /** 可跟踪导航的最大诊断数量，防止 OOM */
        const val MAX_TRACKED_DIAGNOSTICS = 200

        /**
         * 同时保留在内存中的最大文件诊断数量。
         * 超过此限制时，最旧文件的诊断将被驱逐。
         */
        const val MAX_DIAGNOSTIC_FILES = 10
    }
}

/**
 * 将导航状态移动到下一个诊断项
 *
 * 如果已在最后一个诊断项，则循环回到第一个。
 *
 * @return 更新后的导航状态
 */
fun DiagnosticNavigationState.moveToNext(): DiagnosticNavigationState {
    if (diagnostics.isEmpty()) return this
    val nextIndex = if (currentIndex >= diagnostics.lastIndex) 0 else currentIndex + 1
    return copy(currentIndex = nextIndex)
}

/**
 * 将导航状态移动到上一个诊断项
 *
 * 如果已在第一个诊断项，则循环回到最后一个。
 *
 * @return 更新后的导航状态
 */
fun DiagnosticNavigationState.moveToPrevious(): DiagnosticNavigationState {
    if (diagnostics.isEmpty()) return this
    val prevIndex = if (currentIndex <= 0) diagnostics.lastIndex else currentIndex - 1
    return copy(currentIndex = prevIndex)
}

/**
 * 从诊断项列表创建新的导航状态
 *
 * 当前索引重置为 -1（无焦点），诊断列表被截断到 [MAX_TRACKED_DIAGNOSTICS]。
 *
 * @param diagnostics 诊断项列表
 * @return 新的导航状态实例
 */
fun DiagnosticNavigationState.fromDiagnostics(diagnostics: List<DiagnosticItem>): DiagnosticNavigationState {
    val capped = diagnostics.take(DiagnosticNavigationState.MAX_TRACKED_DIAGNOSTICS)
    return DiagnosticNavigationState(
        diagnostics = capped.toImmutableList(),
        currentIndex = if (capped.isEmpty()) -1 else -1
    )
}

/**
 * 文档诊断数据类
 *
 * @property diagnostics 该文档的诊断项列表
 * @property text 文档的当前文本内容（用于诊断定位）
 */
data class DocumentDiagnostics(val diagnostics: List<DiagnosticItem> = emptyList(), val text: String = "")

/**
 * 诊断导航器接口
 *
 * 管理多个文档的诊断状态，作为LSP客户端和编辑器UI之间的桥梁。
 * 提供文档内容跟踪和诊断项更新方法。
 */
interface DiagnosticNavigator {
    /**
     * 所有文档诊断状态的只读流
     */
    val documentDiagnostics: StateFlow<Map<String, DocumentDiagnostics>>

    /**
     * 设置或更新文档的文本内容
     *
     * @param uri 文档URI标识符
     * @param text 文档的完整文本内容
     */
    fun setDocument(uri: String, text: String)

    /**
     * 清除指定文档的所有诊断和文本内容
     *
     * @param uri 文档URI标识符
     */
    fun clearDocument(uri: String)

    /**
     * 更新指定文档的LSP诊断项
     *
     * @param uri 文档URI标识符
     * @param diagnostics LSP服务器返回的诊断项列表
     */
    fun setLspDiagnostics(uri: String, diagnostics: List<DiagnosticItem>)
}

/**
 * 默认诊断导航器实现
 *
 * 使用内存中的ConcurrentHashMap存储文档诊断状态，线程安全。
 */
@Singleton
class DefaultDiagnosticNavigator @Inject constructor() : DiagnosticNavigator {

    private val _documentDiagnostics = MutableStateFlow<Map<String, DocumentDiagnostics>>(emptyMap())
    override val documentDiagnostics: StateFlow<Map<String, DocumentDiagnostics>> = _documentDiagnostics.asStateFlow()

    private val documents = java.util.concurrent.ConcurrentHashMap<String, DocumentDiagnostics>()

    override fun setDocument(uri: String, text: String) {
        val existing = documents[uri]
        val updated = if (existing != null) {
            existing.copy(text = text)
        } else {
            DocumentDiagnostics(text = text)
        }
        documents[uri] = updated
        _documentDiagnostics.value = documents.toMap()
    }

    override fun clearDocument(uri: String) {
        documents.remove(uri)
        _documentDiagnostics.value = documents.toMap()
    }

    override fun setLspDiagnostics(uri: String, diagnostics: List<DiagnosticItem>) {
        val existing = documents[uri]
        val updated = if (existing != null) {
            existing.copy(diagnostics = diagnostics)
        } else {
            DocumentDiagnostics(diagnostics = diagnostics)
        }
        documents[uri] = updated
        _documentDiagnostics.value = documents.toMap()
    }
}
