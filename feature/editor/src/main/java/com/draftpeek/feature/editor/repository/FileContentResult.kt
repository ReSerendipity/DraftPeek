/**
 * 文件：FileContentResult.kt
 * 功能：离线优先数据层文件内容状态密封类
 * 主要类/接口：FileContentResult
 * 模块依赖：
 *   - androidx.compose.runtime：Compose 不可变注解
 */
package com.draftpeek.feature.editor.repository

import androidx.compose.runtime.Immutable

/**
 * 离线优先数据层中表示文件内容状态的密封类（Ch5#3 功能）。
 *
 * 允许 UI 区分不同的数据状态，并显示适当的加载指示器或过期标记。
 *
 * 使用流程：
 * 1. 打开文件时发射 [Loading]
 * 2. 如果存在缓存版本，立即发射 [Cached]
 * 3. 新版本加载完成后发射 [Fresh]
 * 4. 无缓存且出错时发射 [Error]
 */
@Immutable
sealed class FileContentResult {

    /** 文件内容正在加载中 */
    @Immutable
    data object Loading : FileContentResult()

    /**
     * 文件内容可从本地缓存获取。
     *
     * 可能已过期——UI 应指示此状态并允许刷新。
     *
     * @property content 缓存的文本内容
     * @property encoding 检测到的编码
     * @property cachedAtTimestamp 内容缓存时的纪元毫秒数
     * @property fileSize 缓存文件大小（字节）
     */
    @Immutable
    data class Cached(
        val content: String,
        val encoding: String = "UTF-8",
        val cachedAtTimestamp: Long = 0L,
        val fileSize: Long = 0L,
    ) : FileContentResult()

    /**
     * 从源成功读取的最新内容。
     *
     * @property content 最新的文本内容
     * @property encoding 检测到的编码
     * @property fileSize 文件大小（字节）
     */
    @Immutable
    data class Fresh(
        val content: String,
        val encoding: String = "UTF-8",
        val fileSize: Long = 0L,
    ) : FileContentResult()

    /**
     * 无法从任何源加载内容。
     *
     * @property message 人类可读的错误描述
     * @property cause 底层异常（如果有）
     */
    @Immutable
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : FileContentResult()
}
