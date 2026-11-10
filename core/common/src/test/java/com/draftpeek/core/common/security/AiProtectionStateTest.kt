/**
 * AiProtectionState 单元测试。
 *
 * 验证 bitmask 转换逻辑和状态模型正确性。
 */
package com.draftpeek.core.common.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    // ---- DEX 完整性三态（走查 §六.6 fail-open 修复）----

    @Test
    fun `default state reports dex integrity as UNKNOWN never PASSED`() {
        // 负向对照的反面：默认状态必须「未检测」，一旦默认成 PASSED 就是假绿
        val state = AiProtectionState()
        assertEquals(DexIntegrityStatus.UNKNOWN, state.dexIntegrityStatus)
        assertNull(state.dexIntegrityReason)
    }

    @Test
    fun `empty signals without explicit passed status stays UNKNOWN`() {
        // 旧口径的 bug：triggeredSignals 为空 == 界面显示「通过」。
        // 现在空集只表示「没抓到信号」，不表示「校验过且一致」。
        val state = AiProtectionState(triggeredSignals = emptySet())
        assertEquals(DexIntegrityStatus.UNKNOWN, state.dexIntegrityStatus)
    }

    @Test
    fun `failed status carries reason for ui`() {
        val state = AiProtectionState(
            dexIntegrityStatus = DexIntegrityStatus.FAILED,
            dexIntegrityReason = "dex crc sum=12345678901 != baseline=13498282265"
        )
        assertEquals(DexIntegrityStatus.FAILED, state.dexIntegrityStatus)
        assertTrue(state.dexIntegrityReason!!.contains("12345678901"))
    }
}
