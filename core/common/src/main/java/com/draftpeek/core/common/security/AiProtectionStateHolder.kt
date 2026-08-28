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

/**
 * 全局 AI 防护状态持有者。
 *
 * 使用 @Volatile 保证多线程可见性。
 */
object AiProtectionStateHolder {

    @Volatile
    var current: AiProtectionState = AiProtectionState()

    /**
     * 更新当前状态并联动 SecurityGate + 记录事件。
     */
    fun update(state: AiProtectionState) {
        current = state
        // 联动 SecurityGate：高级响应时门控关闭
        SecurityGate.updateEnvironmentSafe(
            state.responseLevel != AiProtectionState.ResponseLevel.SELF_DEFEND
        )
        // 记录事件
        SecurityEventRecorder.recordDetection(state)
    }
}
