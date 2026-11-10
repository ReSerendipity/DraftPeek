/**
 * AiProtectionStateHolder 单元测试。
 *
 * 覆盖走查缺陷 §六.6 的 holder 侧：默认必须是「未检测」，写入必须对 UI 可见，
 * 自检中断必须落 FAILED（不得停在默认值装作通过）。
 */
package com.draftpeek.core.common.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiProtectionStateHolderTest {

    @AfterEach
    fun tearDown() {
        AiProtectionStateHolder.resetForTest()
    }

    @Test
    fun `fresh holder is not assessed yet`() {
        assertEquals(DexIntegrityStatus.UNKNOWN, AiProtectionStateHolder.current.dexIntegrityStatus)
        assertEquals(DexIntegrityStatus.UNKNOWN, AiProtectionStateHolder.state.value.dexIntegrityStatus)
    }

    @Test
    fun `update writes passed status through to the observable state`() {
        AiProtectionStateHolder.update(
            AiProtectionState(dexIntegrityStatus = DexIntegrityStatus.PASSED)
        )

        // current 与 state 必须同源：界面用 state 订阅，非 Compose 调用方用 current
        assertEquals(DexIntegrityStatus.PASSED, AiProtectionStateHolder.state.value.dexIntegrityStatus)
        assertEquals(DexIntegrityStatus.PASSED, AiProtectionStateHolder.current.dexIntegrityStatus)
    }

    @Test
    fun `markAssessmentFailed turns unknown into FAILED with detail`() {
        // 模拟 assess() 抛异常：以前 catch 里什么都不写，界面停在默认「通过」
        AiProtectionStateHolder.markAssessmentFailed(
            IllegalStateException("dex baseline unreadable")
        )

        val state = AiProtectionStateHolder.current
        assertEquals(DexIntegrityStatus.FAILED, state.dexIntegrityStatus)
        assertNotNull(state.dexIntegrityReason)
        assertTrue(state.dexIntegrityReason!!.contains("dex baseline unreadable"))
    }

    @Test
    fun `markAssessmentFailed keeps previously detected signals`() {
        AiProtectionStateHolder.update(
            AiProtectionState(triggeredSignals = setOf(AiDetectionSignal.SIGNATURE_MISMATCH))
        )
        AiProtectionStateHolder.markAssessmentFailed(RuntimeException("boom"))

        val state = AiProtectionStateHolder.current
        assertEquals(DexIntegrityStatus.FAILED, state.dexIntegrityStatus)
        assertTrue(AiDetectionSignal.SIGNATURE_MISMATCH in state.triggeredSignals)
    }
}
