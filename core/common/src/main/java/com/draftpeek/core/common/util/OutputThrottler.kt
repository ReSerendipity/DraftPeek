package com.draftpeek.core.common.util

import kotlinx.coroutines.*

/**
 * 输出节流器，用于高频输出场景（如Git输出、日志流、AI流式生成等）。
 *
 * 按指定时间间隔（默认~60fps/16ms）合并输出，避免主线程因高频UI更新而卡顿。
 * 所有操作在绑定的[CoroutineScope]中按顺序执行，保证线程安全。
 *
 * 参考实现：CodeAssist OutputThrottler、KMP-Markdown render throttling
 */
class OutputThrottler(
    private val scope: CoroutineScope,
    private val onOutput: (String) -> Unit,
    private val throttleMs: Long = 16L,
) {
    private val buffer = StringBuilder()
    private var flushJob: Job? = null
    private var pendingFlush = false

    /**
     * 追加文本到缓冲区。如果没有正在进行的flush任务，将在[throttleMs]后触发一次flush。
     * 可从任何线程调用，内部自动切换到scope的上下文。
     */
    fun append(text: String) {
        if (text.isEmpty()) return
        scope.launch(Dispatchers.Main.immediate) {
            buffer.append(text)
            scheduleFlush()
        }
    }

    /**
     * 立即刷新缓冲区，取消等待中的节流任务。
     * 在任务完成、流结束等场景调用以确保所有输出都被处理。
     */
    fun flushNow() {
        scope.launch(Dispatchers.Main.immediate) {
            flushJob?.cancel()
            flushJob = null
            doFlush()
        }
    }

    /**
     * 清空缓冲区且不输出。用于重置状态。
     */
    fun reset() {
        flushJob?.cancel()
        flushJob = null
        buffer.setLength(0)
        pendingFlush = false
    }

    private fun scheduleFlush() {
        if (pendingFlush) return
        pendingFlush = true
        flushJob = scope.launch(Dispatchers.Main.immediate) {
            delay(throttleMs)
            pendingFlush = false
            doFlush()
        }
    }

    private fun doFlush() {
        if (buffer.isEmpty()) return
        val text = buffer.toString()
        buffer.setLength(0)
        onOutput(text)
    }
}

/**
 * 请求取消器，用于补全、搜索等场景：新请求自动取消旧请求，避免队列堆积。
 *
 * 当快速输入（如代码补全、搜索框）时，只处理最新的请求，旧的未完成请求被取消。
 */
class RequestCanceller(
    private val scope: CoroutineScope,
) {
    @Volatile
    private var currentJob: Job? = null

    /**
     * 启动新请求，自动取消上一个未完成的请求。
     *
     * @param dispatcher 请求执行的协程调度器
     * @param block 请求执行体
     * @return 新请求的Job
     */
    fun launch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        currentJob?.cancel()
        val newJob = scope.launch(dispatcher, block = block)
        currentJob = newJob
        return newJob
    }

    /**
     * 取消当前正在进行的请求（如果有）。
     */
    fun cancelCurrent() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * 是否有正在进行的请求。
     */
    fun isActive(): Boolean = currentJob?.isActive == true
}
