/**
 * 编辑器模块事件定义模块。
 *
 * 定义编辑器模块发出的所有事件，允许其他功能模块（如browser、stats）响应编辑器状态变化，
 * 无需直接模块依赖。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.event

import android.net.Uri

/**
 * 编辑器事件密封类。
 *
 * 所有编辑器相关事件的基类，实现[AppEvent]接口可通过[AppEventBus]分发。
 * 包含文件保存、文件打开、标签页切换、脏状态变更等事件。
 */
sealed class EditorEvent : AppEvent {

    /**
     * 文件成功保存后发出的事件。
     *
     * @property uri 已保存文件的URI
     * @property fileName 已保存文件的显示名称
     */
    data class FileSaved(
        val uri: String,
        val fileName: String = "",
    ) : EditorEvent()

    /**
     * 在编辑器中打开新文件时发出的事件。
     *
     * @property uri 已打开文件的URI
     * @property language 检测到的文件语言
     */
    data class FileOpened(
        val uri: String,
        val language: String? = null,
    ) : EditorEvent()

    /**
     * 当前标签页切换时发出的事件。
     *
     * @property tabId 新活跃标签页的ID
     * @property uri 新标签页中文件的URI
     */
    data class TabChanged(
        val tabId: String,
        val uri: String,
    ) : EditorEvent()

    /**
     * 编辑器脏状态（未保存更改）变更时发出的事件。
     *
     * @property uri 脏状态变更的文件URI
     * @property isDirty 如果文件现在有未保存的更改则为true
     */
    data class DirtyStateChanged(
        val uri: String,
        val isDirty: Boolean,
    ) : EditorEvent()

    /**
     * 安全响应要求编辑器切换只读/解除只读。
     *
     * 订阅者：feature/editor 模块 EditorViewModel
     *
     * @property locked true 表示切换为只读模式，false 表示解除只读
     * @property reason 锁定原因标识（如 "security_ai_threat_detected"）
     */
    data class LockEditor(
        val locked: Boolean,
        val reason: String = "",
    ) : EditorEvent()
}
