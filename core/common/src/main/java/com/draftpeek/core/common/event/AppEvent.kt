/**
 * 应用事件标记接口模块。
 *
 * 定义所有通过[AppEventBus]分发的应用事件的标记接口。事件用于跨模块通信，
 * 避免直接依赖注入产生循环依赖（如editor → browser）。
 * 所有事件实现应为不可变数据类或对象，事件是一次性的——由收集器消费后不重放。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.event

/**
 * 应用事件标记接口。
 *
 * 所有通过AppEventBus分发的事件必须实现此接口。事件用于跨模块松耦合通信，
 * 避免功能模块间的直接依赖。实现类应为不可变数据类或单例对象。
 */
interface AppEvent
