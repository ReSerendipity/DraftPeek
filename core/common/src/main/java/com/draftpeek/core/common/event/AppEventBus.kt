/**
 * 应用全局事件总线模块。
 *
 * 基于SharedFlow（replay=0，热流无重放）实现的应用级事件总线，用于跨模块通信。
 * 只有活跃的收集器能接收事件，避免功能模块间的循环依赖（如editor ↔ browser）。
 * 使用Hilt @Singleton注解确保全局单例。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用事件总线类（单例）。
 *
 * 使用SharedFlow实现热事件流，缓冲区容量64，溢出策略为DROP_OLDEST。
 * 提供emit（挂起函数，关键事件使用）和tryEmit（非挂起，低优先级通知使用）两种发送方式。
 * 线程安全：可从任意协程调用emit；events是冷Flow，支持多个订阅者同时收集。
 */
@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>(
        extraBufferCapacity = DEFAULT_BUFFER_CAPACITY,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    /**
     * 所有应用事件的冷Flow。
     * 收集器只会收到开始收集后发出的事件。
     */
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * 向所有活跃收集器发送事件。
     * 这是一个挂起函数——需在协程作用域中调用。
     *
     * @param event 要分发的事件
     */
    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }

    /**
     * 尝试非阻塞地发送事件。
     * 如果事件被缓冲或成功传递返回true；如果缓冲区已满导致事件被丢弃返回false。
     *
     * 关键事件优先使用[emit]；tryEmit仅用于低优先级或尽力而为的通知。
     *
     * @param event 要分发的事件
     * @return 事件是否成功缓冲/发送
     */
    fun tryEmit(event: AppEvent): Boolean {
        return _events.tryEmit(event)
    }

    companion object {
        /** SharedFlow缓冲区容量，满时DROP_OLDEST */
        private const val DEFAULT_BUFFER_CAPACITY = 64
    }
}
