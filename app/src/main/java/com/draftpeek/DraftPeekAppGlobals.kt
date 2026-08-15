/**
 * 全局引用持有者。
 *
 * 在 DraftPeekApp.onCreate 中初始化，避免所有安全模块 object 都要走 Hilt EntryPoint。
 * 提供 AppEventBus 实例和 appScope 协程作用域的缓存引用。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek

import com.draftpeek.core.common.event.AppEventBus
import kotlinx.coroutines.CoroutineScope

/**
 * 全局引用单例。
 *
 * 在 [DraftPeekApp.onCreate] 中初始化：
 * - 从 Hilt EntryPoint 获取 AppEventBus 实例后赋值
 * - appScope 由 DraftPeekApp 提供
 */
object DraftPeekAppGlobals {

    /** 全局事件总线（由 Hilt 提供 Singleton 实例） */
    @Volatile
    lateinit var eventBus: AppEventBus
        private set

    /** 应用级协程作用域 */
    @Volatile
    lateinit var appScope: CoroutineScope
        private set

    /**
     * 初始化全局引用。由 DraftPeekApp.onCreate 调用。
     */
    fun init(eventBus: AppEventBus, appScope: CoroutineScope) {
        this.eventBus = eventBus
        this.appScope = appScope
    }

    /**
     * 清空运行时敏感引用（自保护响应时调用）。
     */
    fun clearRuntimeSensitiveRefs() {
        // 目前没有需要清空的额外引用
        // eventBus 和 appScope 保持不变，因为清空事件已通过 bus 发送
    }
}
