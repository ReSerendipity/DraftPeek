/**
 * 文件：EditorFileReadOutcome.kt
 * 功能：离线优先文件读取结果类型密封类
 * 主要类/接口：EditorFileReadOutcome
 * 模块依赖：
 *   - androidx.compose.runtime：Compose 不可变注解
 */
package com.draftpeek.feature.editor.repository

import androidx.compose.runtime.Immutable

/**
 * 文件读取操作的离线优先结果类型（Ch5#3 功能）。
 *
 * 编码数据来源，以便 UI 在获取新副本时显示适当的过期指示器。
 *
 * 设计遵循 nowinandroid 的离线优先模式：
 * - 存在本地数据时首先发射 [Cached]
 * - 远程/源读取成功后用 [Fresh] 替换
 * - 无法获取本地和远程数据时发射 [Unavailable]
 */
@Immutable
sealed class EditorFileReadOutcome {

    /**
     * 文件内容从本地缓存/最近文件快照加载。
     *
     * UI 应显示内容并带有"过期"指示器，并触发刷新。
     *
     * @property result 缓存的文件数据
     * @property ageMs 缓存数据的近似年龄（毫秒），刚写入为 0，未知为 -1
     */
    @Immutable
    data class Cached(
        val result: FileReadResult,
        val ageMs: Long = -1L,
    ) : EditorFileReadOutcome()

    /**
     * 文件内容从源（SAF URI、Asset 等）最新读取。
     *
     * 这是权威的最新版本。
     *
     * @property result 最新读取的文件数据
     */
    @Immutable
    data class Fresh(
        val result: FileReadResult,
    ) : EditorFileReadOutcome()

    /**
     * 无法获取数据——缓存和源均不可用。
     *
     * @property error 导致读取失败的异常
     * @property message 人类可读的错误描述
     */
    @Immutable
    data class Unavailable(
        val error: Throwable,
        val message: String = error.message ?: "File unavailable",
    ) : EditorFileReadOutcome()
}
