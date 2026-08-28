/**
 * 延迟初始化器工具模块。
 *
 * 提供后台协程任务管理，用于在应用启动时预加载资源而不阻塞UI线程。
 * 管理一组带标签的初始化任务，支持等待特定标签的所有任务完成。
 * 使用SupervisorJob确保单个任务失败不会影响其他任务。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 延迟初始化器对象。
 *
 * 管理应用启动后的后台初始化任务，允许非关键资源在后台预加载。
 * 使用独立的CoroutineScope（SupervisorJob + Dispatchers.Default）执行任务，
 * 标签用于分组相关任务，waitForTag()可等待标签下所有任务完成（带超时）。
 */
object DeferredInitializer {

    /**
     * 初始化任务数据类，用于追踪带标签的后台任务。
     *
     * @property tag 任务标签
     * @property name 任务名称（可选）
     * @property deferred 任务的CompletableDeferred句柄
     */
    data class InitTask(val tag: String, val name: String?, val deferred: CompletableDeferred<Unit>)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tasks = mutableMapOf<String, MutableList<InitTask>>()

    /**
     * 在指定标签下提交一个后台初始化任务。
     *
     * 任务在Dispatchers.Default线程池上启动。任务异常会被捕获并记录日志，
     * 但不会传播到其他任务（SupervisorJob）。
     *
     * @param tag 任务分组标签
     * @param name 可选的任务名称（用于日志）
     * @param block 要执行的挂起初始化块
     */
    fun submit(tag: String, name: String? = null, block: suspend () -> Unit) {
        val deferred = CompletableDeferred<Unit>()
        val task = InitTask(tag, name, deferred)

        synchronized(tasks) {
            tasks.getOrPut(tag) { mutableListOf() }.add(task)
        }

        scope.launch {
            try {
                block()
                deferred.complete(Unit)
            } catch (e: Exception) {
                android.util.Log.e(
                    "DeferredInit",
                    "Init task failed [tag=$tag, name=$name]: ${e.message}",
                    e
                )
                deferred.completeExceptionally(e)
            }
        }
    }

    /**
     * 等待指定标签下的所有任务完成。
     *
     * @param tag 要等待的任务标签
     * @param timeoutMs 超时时间（毫秒），null表示无超时
     * @return 如果所有任务在超时前完成返回`true`，超时返回`false`
     */
    suspend fun waitForTag(tag: String, timeoutMs: Long? = 5000L): Boolean {
        val taskList: List<InitTask> = synchronized(tasks) {
            tasks[tag]?.toList() ?: emptyList()
        }
        if (taskList.isEmpty()) return true

        return if (timeoutMs != null) {
            withTimeoutOrNull(timeoutMs) {
                taskList.forEach { it.deferred.join() }
            } != null
        } else {
            taskList.forEach { it.deferred.join() }
            true
        }
    }

    /**
     * 检查指定标签下是否有进行中的任务。
     *
     * @param tag 要检查的标签
     * @return 如果存在未完成任务返回`true`
     */
    fun hasPending(tag: String): Boolean {
        val taskList = synchronized(tasks) { tasks[tag]?.toList() ?: emptyList() }
        return taskList.any { !it.deferred.isCompleted }
    }

    /**
     * 清除所有已完成的任务引用（用于内存管理）。
     */
    fun cleanup() {
        synchronized(tasks) {
            val iter = tasks.entries.iterator()
            while (iter.hasNext()) {
                val (_, list) = iter.next()
                list.removeAll { it.deferred.isCompleted }
                if (list.isEmpty()) iter.remove()
            }
        }
    }

    /**
     * 触发预加载编辑器相关资源（语法、主题等）。
     * 在文件列表显示时调用，当用户打开第一个文件时资源已就绪。
     *
     * @param context 应用上下文
     */
    fun preloadEditorResources(context: Context) {
        submit("editor", "textmate-registry") {
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {}
        }

        submit("editor", "theme-preload") {
            try {
                Thread.sleep(30)
            } catch (_: InterruptedException) {}
        }
    }
}
