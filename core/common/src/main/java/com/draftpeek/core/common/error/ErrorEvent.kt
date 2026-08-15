/**
 * 一次性错误事件定义模块。
 *
 * 本文件定义了用于UI层显示的一次性错误事件，通常通过Snackbar或Toast展示给用户。
 * 采用data class设计，支持携带错误消息和重试标识。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.error

/**
 * 一次性错误事件数据类。
 *
 * 用于在ViewModel与UI层之间传递需要显示给用户的错误信息。
 * 此类事件设计为一次性消费，避免配置变更（如屏幕旋转）时重复显示。
 *
 * @property message 需要显示给用户的错误消息文本（已本地化）
 * @property isRetryable 标识该错误是否支持重试操作，默认为false
 */
data class ErrorEvent(
    val message: String,
    val isRetryable: Boolean = false,
)
