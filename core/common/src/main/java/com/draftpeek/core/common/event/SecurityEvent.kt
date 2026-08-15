/**
 * 安全模块事件定义。
 *
 * 所有安全相关跨模块事件集中于此。
 * 本类实现 [AppEvent] 接口以通过 [AppEventBus] 分发。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.common.event

/**
 * 安全模块事件。所有安全相关跨模块事件集中于此。
 *
 * 本类实现 [AppEvent] 接口以通过 [AppEventBus] 分发。
 */
sealed class SecurityEvent : AppEvent {

    /**
     * 要求所有模块清空内存中的敏感缓存（文件内容 / 密码 / 凭证 / 撤销栈等）。
     *
     * @property preservePersistentFiles true 只清内存，不删磁盘文件；
     *                                     false 由订阅者自行决定是否擦除磁盘文件（默认 true）。
     */
    data class ClearAllSensitiveCaches(
        val preservePersistentFiles: Boolean = true,
    ) : SecurityEvent()
}
