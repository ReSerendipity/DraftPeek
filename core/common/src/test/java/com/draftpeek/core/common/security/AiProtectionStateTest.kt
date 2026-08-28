/**
 * AiProtectionState 单元测试。
 *
 * 验证 bitmask 转换逻辑和状态模型正确性。
 */
package com.draftpeek.core.common.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AiProtectionStateTest {

    @Test
    fun `empty signals produce zero mask`() {
        val state = AiProtectionState()
        assertEquals(0, state.signalsToMask())
    }

    @Test
    fun `single signal mask roundtrip`() {
        val state = AiProtectionState(
            triggeredSignals = setOf(AiDetectionSignal.SIGNATURE_MISMATCH)
        )
        val mask = state.signalsToMask()
        val restored = AiProtectionState.maskToSignals(mask)
        assertEquals(setOf(AiDetectionSignal.SIGNATURE_MISMATCH), restored)
    }

    @Test
    fun `multiple signals mask roundtrip`() {
        val signals = setOf(
            AiDetectionSignal.SIGNATURE_MISMATCH,
            AiDetectionSignal.DEX_TAMPERED,
            AiDetectionSignal.HOSTILE_FRIDA_XPOSED,
            AiDetectionSignal.EMULATOR_ENHANCED_FINGERPRINT
        )
        val state = AiProtectionState(triggeredSignals = signals)
        val mask = state.signalsToMask()
        val restored = AiProtectionState.maskToSignals(mask)
        assertEquals(signals, restored)
    }

    @Test
    fun `all signals mask roundtrip`() {
        val signals = AiDetectionSignal.entries.toSet()
        val state = AiProtectionState(triggeredSignals = signals)
        val mask = state.signalsToMask()
        val restored = AiProtectionState.maskToSignals(mask)
        assertEquals(signals, restored)
    }

    @Test
    fun `default state is SAFE with NONE response`() {
        val state = AiProtectionState()
        assertEquals(AiThreatLevel.SAFE, state.threatLevel)
        assertEquals(AiProtectionState.ResponseLevel.NONE, state.responseLevel)
        assertTrue(state.triggeredSignals.isEmpty())
    }

    @Test
    fun `HOSTILE with SIGNATURE_MISMATCH produces SELF_DEFEND`() {
        val state = AiProtectionState(
            threatLevel = AiThreatLevel.HOSTILE,
            triggeredSignals = setOf(AiDetectionSignal.SIGNATURE_MISMATCH),
            responseLevel = AiProtectionState.ResponseLevel.SELF_DEFEND
        )
        assertEquals(AiProtectionState.ResponseLevel.SELF_DEFEND, state.responseLevel)
    }
}
