/**
 * 文件浏览器模块事件定义模块。
 *
 * 定义文件浏览器模块发出的所有事件，允许其他功能模块响应浏览器状态变化
 * （如目录导航、文件创建、Git操作完成等），无需直接模块依赖。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.event

/**
 * 文件浏览器事件密封类。
 *
 * 所有浏览器相关事件的基类，实现[AppEvent]接口可通过[AppEventBus]分发。
 * 包含目录变更、文件创建、文件删除、Git操作完成等事件。
 */
sealed class BrowserEvent : AppEvent {

    /**
     * 用户导航到不同目录时发出的事件。
     *
     * @property directoryUri 新目录的URI
     */
    data class DirectoryChanged(
        val directoryUri: String,
    ) : BrowserEvent()

    /**
     * 从浏览器创建新文件时发出的事件。
     *
     * @property uri 新创建文件的URI
     * @property fileName 新文件的名称
     */
    data class FileCreated(
        val uri: String,
        val fileName: String,
    ) : BrowserEvent()

    /**
     * 从浏览器删除文件时发出的事件。
     *
     * @property uri 被删除文件的URI
     */
    data class FileDeleted(
        val uri: String,
    ) : BrowserEvent()

    /**
     * Git操作完成时发出的事件。
     *
     * @property operation Git操作类型（commit、push、pull等）
     * @property success 操作是否成功
     * @property message 可选的状态或错误消息
     */
    data class GitOperationCompleted(
        val operation: String,
        val success: Boolean,
        val message: String? = null,
    ) : BrowserEvent()

    /**
     * 安全响应要求禁用 / 恢复终端模块。
     *
     * 订阅者：feature/terminal 模块 TerminalViewModel / TerminalService
     *
     * @property disabled true 表示禁用终端，false 表示恢复
     */
    data class TerminalDisabled(
        val disabled: Boolean,
    ) : BrowserEvent()
}
