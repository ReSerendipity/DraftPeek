package com.draftpeek.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 同步协调器：把 [SyncMachine]（决策）+ [SyncTransport]（搬运）+ 连通性（离线/恢复）
 * 串成一个**可运行的同步流程**。
 *
 * 一次同步 = 读本地 → 拉远端 → [SyncMerger] 合并 → 写本地 → 推远端。
 *
 * 职责边界：
 * - **只管流程与状态**：不关心数据是什么、存哪、怎么合并（分别由 [SyncTransport]、
 *   [LocalSnapshotSource]、[SyncMerger] 承担）；
 * - **不做业务判断**：失败分类由传输层给出，退避与阈值由 [SyncMachine] 决定。
 *
 * 线程模型：所有状态迁移都在 [mutex] 内串行执行，避免「读状态 → 算新状态 → 写状态」被并发穿插。
 *
 * @param connectivity 连通性流（true = 在线）。用 `Flow` 而不是轮询式检查，是为了让协调器
 *   可被单测（测试直接发射即可）；把 `NetworkConnectivityChecker` 适配成流的活由调用方做。
 * @param clock 时间源，测试可注入固定值
 */
class SyncCoordinator(
    private val transport: SyncTransport,
    private val localSource: LocalSnapshotSource,
    private val merger: SyncMerger,
    private val connectivity: Flow<Boolean>,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private val mutex = Mutex()
    private val _state = MutableStateFlow(SyncMachineState())

    /** 当前同步状态，供 UI 渲染身份区的同步状态行。 */
    val state: StateFlow<SyncMachineState> = _state.asStateFlow()

    private var transferJob: Job? = null
    private var retryJob: Job? = null
    private var tickerJob: Job? = null
    private var connectivityJob: Job? = null

    /** 订阅连通性并启动「取消阈值」计时器。幂等。 */
    fun start() {
        if (connectivityJob == null) {
            connectivityJob = scope.launch {
                connectivity.collect { online ->
                    dispatch(if (online) SyncEvent.WentOnline(clock()) else SyncEvent.WentOffline)
                }
            }
        }
    }

    /** 触发同步（数据变更自动触发，或用户点「立即同步」）。 */
    fun requestSync() {
        scope.launch { dispatch(SyncEvent.Requested(clock())) }
    }

    /** 用户点「重试」（失败态）。 */
    fun retry() {
        scope.launch { dispatch(SyncEvent.Requested(clock())) }
    }

    /** 用户点「取消」（同步中超阈值后可见）。 */
    fun cancel() {
        scope.launch { dispatch(SyncEvent.CancelRequested) }
    }

    /** 释放：取消全部内部协程。 */
    fun stop() {
        connectivityJob?.cancel()
        tickerJob?.cancel()
        retryJob?.cancel()
        transferJob?.cancel()
        connectivityJob = null
    }

    private suspend fun dispatch(event: SyncEvent) {
        mutex.withLock {
            val (next, effects) = SyncMachine.reduce(_state.value, event)
            _state.value = next
            effects.forEach { runEffect(it) }
        }
    }

    private fun runEffect(effect: SyncEffect) {
        when (effect) {
            SyncEffect.StartTransfer -> startTransfer()
            is SyncEffect.ScheduleRetry -> scheduleRetry(effect.delayMillis)
            SyncEffect.CancelTransfer -> cancelTransfer()
        }
    }

    private fun startTransfer() {
        transferJob?.cancel()
        ensureTicker()
        transferJob = scope.launch {
            val result = runTransfer()
            mutex.withLock {
                when (result) {
                    TransferResult.Success -> applyEvent(SyncEvent.Succeeded(clock()))
                    is TransferResult.Failure -> applyEvent(SyncEvent.Failed(result.kind))
                }
            }
        }
    }

    /** 一次传输：读本地 → 拉远端 → 合并 → 写本地 → 推远端。任何异常都归一成失败分类。 */
    private suspend fun runTransfer(): TransferResult = try {
        val local = localSource.read()
        when (val pulled = transport.pull()) {
            is PullOutcome.Failure -> TransferResult.Failure(pulled.kind, pulled.cause)
            is PullOutcome.Success -> {
                val merged = merger.merge(local, pulled.snapshot)
                localSource.write(merged)
                transport.push(merged)
            }
        }
    } catch (cancellation: CancellationException) {
        // ⚠️ 协程取消**必须原样抛出**：若被下面的 catch(Throwable) 吞掉，
        // 「用户点取消」就会被当成一次失败上报，状态错变成 FAILED（单测抓到过）。
        throw cancellation
    } catch (throwable: Throwable) {
        TransferResult.Failure(SyncFailureMapper.fromThrowable(throwable), throwable)
    }

    private fun scheduleRetry(delayMillis: Long) {
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMillis)
            // 到点后走显式事件：若期间已被取消或转为离线，状态机会忽略它
            dispatch(SyncEvent.RetryDue)
        }
    }

    private fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        stopTicker()
    }

    /**
     * 「正在同步…」超时计时器。
     *
     * 用独立计时器而不是让 UI 自己数秒，是为了让「超 30 秒才出现取消」这条规格
     * 由引擎保证（UI 只读 [SyncMachineState.cancelAvailable]）。
     */
    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                mutex.withLock { applyEvent(SyncEvent.Tick(clock())) }
                if (_state.value.phase != SyncPhase.SYNCING) break
            }
            tickerJob = null
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /** 在已持锁的前提下应用事件（不再重复加锁）。 */
    private fun applyEvent(event: SyncEvent) {
        val (next, effects) = SyncMachine.reduce(_state.value, event)
        _state.value = next
        effects.forEach { runEffect(it) }
    }

    companion object {
        /** 取消阈值的检查间隔（阈值本身由 [SyncMachine.CANCEL_AFTER_MS] 决定）。 */
        const val TICK_INTERVAL_MS = 1_000L
    }
}
