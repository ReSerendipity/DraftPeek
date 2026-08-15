package com.draftpeek.core.common.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 安全门控攻击模拟测试。
 *
 * 模拟各种攻击场景，验证 SecurityGate 正确阻断敏感操作：
 * - APK 篡改场景（完整性校验失败）
 * - 反调试检测触发（环境不安全）
 * - 组合攻击场景（完整性 + 环境同时异常）
 * - 恢复场景（攻击解除后操作恢复）
 *
 * **注意（反模式修复）**：SecurityGate 使用静态可变状态（伴生对象字段），
 * 测试间通过 `@BeforeEach` 重置为安全状态。JUnit5 默认同线程串行执行，
 * 因此当前配置下不会出现状态污染。若未来启用并行测试执行，
 * 需添加 `@Execution(ExecutionMode.SAME_THREAD)` 确保隔离。
 */
@DisplayName("SecurityGate Attack Simulation")
class SecurityGateAttackSimulationTest {

    @BeforeEach
    fun setUp() {
        // Reset to safe state before each test
        SecurityGate.updateIntegrity(true)
        SecurityGate.updateEnvironmentSafe(true)
    }

    @Nested
    @DisplayName("Normal operation (no attack)")
    inner class NormalOperationTests {

        @Test
        @DisplayName("正常状态下操作被允许")
        fun normalState_operationAllowed() {
            assertTrue(SecurityGate.isOperationAllowed())
        }
    }

    @Nested
    @DisplayName("APK tampering attack")
    inner class ApkTamperingTests {

        @Test
        @DisplayName("APK 完整性校验失败时操作被阻断")
        fun apkTampered_operationBlocked() {
            // Simulate APK tampering detected
            SecurityGate.updateIntegrity(false)

            assertFalse(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("APK 完整性恢复后操作恢复")
        fun apkRestored_operationRestored() {
            SecurityGate.updateIntegrity(false)
            assertFalse(SecurityGate.isOperationAllowed())

            // Simulate APK integrity restored
            SecurityGate.updateIntegrity(true)
            assertTrue(SecurityGate.isOperationAllowed())
        }
    }

    @Nested
    @DisplayName("Anti-debug detection")
    inner class AntiDebugTests {

        @Test
        @DisplayName("检测到调试器附加时操作被阻断")
        fun debuggerDetected_operationBlocked() {
            SecurityGate.updateEnvironmentSafe(false)

            assertFalse(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("调试器分离后操作恢复")
        fun debuggerDetached_operationRestored() {
            SecurityGate.updateEnvironmentSafe(false)
            assertFalse(SecurityGate.isOperationAllowed())

            SecurityGate.updateEnvironmentSafe(true)
            assertTrue(SecurityGate.isOperationAllowed())
        }
    }

    @Nested
    @DisplayName("Combined attack scenarios")
    inner class CombinedAttackTests {

        @Test
        @DisplayName("APK 篡改 + 调试器同时存在时操作被阻断")
        fun combinedAttack_operationBlocked() {
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateEnvironmentSafe(false)

            assertFalse(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("仅修复完整性但环境仍不安全时操作仍被阻断")
        fun partialFix_stillBlocked() {
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateEnvironmentSafe(false)

            // Fix only integrity
            SecurityGate.updateIntegrity(true)
            assertFalse(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("仅修复环境但完整性仍异常时操作仍被阻断")
        fun partialFixEnvironment_stillBlocked() {
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateEnvironmentSafe(false)

            // Fix only environment
            SecurityGate.updateEnvironmentSafe(true)
            assertFalse(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("两个问题都修复后操作恢复")
        fun allFixed_operationRestored() {
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateEnvironmentSafe(false)

            SecurityGate.updateIntegrity(true)
            SecurityGate.updateEnvironmentSafe(true)
            assertTrue(SecurityGate.isOperationAllowed())
        }
    }

    @Nested
    @DisplayName("State transition resilience")
    inner class StateTransitionTests {

        @Test
        @DisplayName("快速切换状态不影响最终一致性")
        fun rapidStateChanges_finalStateConsistent() {
            // Simulate rapid state changes (e.g., race condition attempt)
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateIntegrity(true)
            SecurityGate.updateIntegrity(false)
            SecurityGate.updateIntegrity(true)

            assertTrue(SecurityGate.isOperationAllowed())
        }

        @Test
        @DisplayName("环境安全状态快速切换不影响最终一致性")
        fun rapidEnvironmentChanges_finalStateConsistent() {
            SecurityGate.updateEnvironmentSafe(false)
            SecurityGate.updateEnvironmentSafe(true)
            SecurityGate.updateEnvironmentSafe(false)
            SecurityGate.updateEnvironmentSafe(true)

            assertTrue(SecurityGate.isOperationAllowed())
        }
    }
}
