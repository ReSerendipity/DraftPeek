/**
 * 全局 AI 防护状态持有者（供 UI / SecurityGate 读取）。
 *
 * 位于 core/common 以便 feature 模块直接读取当前 AI 威胁状态。
 * 由 [com.draftpeek.security.AiDetector] 更新。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.common.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局 AI 防护状态持有者。
 *
 * 状态以 [MutableStateFlow] 持有：`value` 的读写自带线程可见性（替代原先的
 * `@Volatile var`），且 Compose 侧可 `collectAsState()` 订阅——否则界面读的是一次性
 * 快照，后台协程写入后不会触发重组，安全状态行会一直停在首帧的值（fail-open 假绿）。
 */
object AiProtectionStateHolder {

    private val _state = MutableStateFlow(AiProtectionState())

    /** 可观察状态流；Compose 侧读状态必须走这里。 */
    val state: StateFlow<AiProtectionState> = _state.asStateFlow()

    /** 当前状态快照（非 Compose 调用方使用）。 */
    val current: AiProtectionState
        get() = _state.value

    /**
     * 更新当前状态并联动 SecurityGate + 记录事件。
     */
    fun update(state: AiProtectionState) {
        _state.value = state
        // 联动 SecurityGate：高级响应时门控关闭
        SecurityGate.updateEnvironmentSafe(
            state.responseLevel != AiProtectionState.ResponseLevel.SELF_DEFEND
        )
        // 记录事件
        SecurityEventRecorder.recordDetection(state)
    }

    /**
     * 自检整体中断（[com.draftpeek.security.AiDetector.assess] 抛异常）时落库的兜底状态。
     *
     * 此时 DEX 是否被改动并没有被证明，但「没能完成校验」绝不能显示成「通过」——
     * 按 fail-closed 口径记为 [DexIntegrityStatus.FAILED]，并保留异常细节供界面展示。
     *
     * @param error 让自检中断的异常
     */
    fun markAssessmentFailed(error: Throwable) {
        val detail = "assessment aborted: ${error::class.simpleName}: ${error.message ?: "no message"}"
        update(
            current.copy(
                dexIntegrityStatus = DexIntegrityStatus.FAILED,
                dexIntegrityReason = detail
            )
        )
    }

    /** 重置为默认（未检测）状态。仅测试使用。 */
    internal fun resetForTest() {
        _state.value = AiProtectionState()
    }
}
