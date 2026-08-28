package com.draftpeek.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * AntiDebug.assess() 综合安全评估单元测试。
 *
 * 验证威胁分数累加、安全等级跃迁（SAFE → SUSPICIOUS → HOSTILE）、
 * 滑窗衰减机制等核心安全决策逻辑。
 *
 * 使用 Robolectric 提供 android.os.Build 真实字段值，
 * 避免纯 JVM 下 Build.BRAND 为 null 导致的 NPE。
 *
 * 注意：本类使用 JUnit4 + Robolectric 风格（扁平方法，无 @Nested/@DisplayName），
 * 遵循 AGENTS.md Gotcha #23 的范式，避免 JUnit5 注解与 JUnit4 Runner 混用。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AntiDebugAssessTest {

    /**
     * 直接重置 AntiDebug 的内部状态（threatScore、lastScoreDecayMs、
     * integrityChecked、integrityVerified、currentLevel）。
     * 必须在每个测试前调用，确保测试间状态隔离。
     *
     * 注意：AntiDebug 的字段已标记为 @JvmField + internal，可直接访问。
     */
    private fun resetAntiDebugState() {
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(0)
        AntiDebug.lastScoreDecayMs = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
        AntiDebug.integrityChecked = false
        AntiDebug.integrityVerified = false
        AntiDebug.currentLevel = AntiDebug.SecurityLevel.SAFE
    }

    /**
     * 设置非模拟器 Build 字段，确保 isRunningOnEmulator() 返回 false。
     */
    private fun setRealDeviceBuildFields() {
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "BRAND", "samsung")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "DEVICE", "beyond1")
        ReflectionHelpers.setStaticField(
            android.os.Build::class.java,
            "FINGERPRINT",
            "samsung/beyond1/beyond1:14/UP1A.231005.007:user/release-keys"
        )
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "HARDWARE", "qcom")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "MODEL", "SM-G973F")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", "samsung")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "PRODUCT", "beyond1")
    }

    @Test
    fun `assess on clean environment returns SAFE`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 直接设置 threatScore 为 0，模拟干净环境（不依赖 isRooted() 的实际返回值）
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(0)

        val level = AntiDebug.assess()

        // 在干净环境中（无调试器、无模拟器、无 Frida 等），应返回 SAFE
        // 注意：NativeSecurityChecker.performSecurityCheck() 在 JVM 测试中返回 0（降级）
        assertEquals(
            "Clean environment should be assessed as SAFE",
            AntiDebug.SecurityLevel.SAFE,
            level
        )
    }

    @Test
    fun `assess returns SUSPICIOUS when threat score accumulates`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 直接设置 threatScore 为 SUSPICIOUS 阈值下限（>= 3）
        // 不依赖 isRooted() 的实际返回值，确保测试环境无关
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(3)

        val level = AntiDebug.assess()
        // 累积分数 3 + 当前分数 0（干净环境）= 3，>= 3 → SUSPICIOUS
        assertEquals(
            "Accumulated threat score >= 3 should be SUSPICIOUS",
            AntiDebug.SecurityLevel.SUSPICIOUS,
            level
        )
    }

    @Test
    fun `assess returns HOSTILE when threat score is high`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 通过反射设置威胁分数到 HOSTILE 阈值（>= 8）
        val threatScoreField = AntiDebug::class.java.getDeclaredField("threatScore")
        threatScoreField.isAccessible = true
        val atomicInt = threatScoreField.get(AntiDebug) as java.util.concurrent.atomic.AtomicInteger
        atomicInt.set(8) // HOSTILE 阈值下限

        val level = AntiDebug.assess()
        // 累积分数 8 + 当前分数 0（干净环境）= 8，>= 8 → HOSTILE
        assertEquals(
            "Accumulated threat score >= 8 should be HOSTILE",
            AntiDebug.SecurityLevel.HOSTILE,
            level
        )
    }

    @Test
    fun `assess currentThreat gte 5 triggers HOSTILE immediately`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 直接设置 threatScore 为 5，验证 currentThreat >= 5 时触发 HOSTILE
        // 不依赖 isRooted() 的实际返回值，确保测试环境无关
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(5)

        val level = AntiDebug.assess()
        // 累积分数 5 >= 5 → HOSTILE
        assertEquals(
            "Accumulated threat score >= 5 should be HOSTILE",
            AntiDebug.SecurityLevel.HOSTILE,
            level
        )
    }

    @Test
    fun `assess updates currentLevel after evaluation`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 确保初始 currentLevel 是 SAFE
        assertEquals(AntiDebug.SecurityLevel.SAFE, AntiDebug.currentLevel)

        // 通过反射设置高威胁分数
        val threatScoreField = AntiDebug::class.java.getDeclaredField("threatScore")
        threatScoreField.isAccessible = true
        val atomicInt = threatScoreField.get(AntiDebug) as java.util.concurrent.atomic.AtomicInteger
        atomicInt.set(10)

        AntiDebug.assess()

        // assess 后 currentLevel 应被更新为 HOSTILE
        assertEquals(
            "currentLevel should be updated to HOSTILE after assess with high threat",
            AntiDebug.SecurityLevel.HOSTILE,
            AntiDebug.currentLevel
        )
    }

    @Test
    fun `assess threat score accumulates across calls`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 直接设置 threatScore 为 2，模拟已累积分数
        // 不依赖 isRooted() 的实际返回值，确保测试环境无关
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(2)

        // 第一次调用：累积分=2
        AntiDebug.assess()
        val afterFirstCall = AntiDebug.threatScore.get()

        // 第二次调用：累积分应保持不变（因为 assess 只累加 currentThreat）
        AntiDebug.assess()
        val afterSecondCall = AntiDebug.threatScore.get()

        assertEquals(
            "Threat score should not increase in clean environment",
            afterFirstCall,
            afterSecondCall
        )
    }

    @Test
    fun `assess with emulator detection accumulates threat score`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 直接设置 threatScore 为 0，确保测试环境无关
        AntiDebug.threatScore = java.util.concurrent.atomic.AtomicInteger(0)

        // 第一次 assess：干净环境
        AntiDebug.assess()
        val scoreAfterClean = AntiDebug.threatScore.get()

        // 切换为模拟器 Build 字段
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "BRAND", "generic")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "DEVICE", "generic")
        ReflectionHelpers.setStaticField(
            android.os.Build::class.java,
            "FINGERPRINT",
            "generic/test/test:14/test/test-keys"
        )
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "HARDWARE", "goldfish")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "MODEL", "google_sdk")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "MANUFACTURER", "unknown")
        ReflectionHelpers.setStaticField(android.os.Build::class.java, "PRODUCT", "sdk_google")

        // 第二次 assess：检测到模拟器（+3）
        AntiDebug.assess()
        val scoreAfterEmulator = AntiDebug.threatScore.get()

        assertTrue(
            "Threat score should increase after emulator detection: before=$scoreAfterClean, after=$scoreAfterEmulator",
            scoreAfterEmulator > scoreAfterClean
        )
    }

    @Test
    fun `assess returns a non-null SecurityLevel`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        val level = AntiDebug.assess()
        assertNotNull("assess() must return a non-null SecurityLevel", level)
        assertTrue(
            "Returned level must be one of SAFE/SUSPICIOUS/HOSTILE",
            AntiDebug.SecurityLevel.entries.contains(level)
        )
    }

    @Test
    fun `quickCheck returns true in clean environment`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 在没有调试器附加的测试环境中，quickCheck 应返回 true
        val result = AntiDebug.quickCheck()
        assertTrue("quickCheck should return true when no debugger is attached", result)
    }

    @Test
    fun `assess does not throw on multiple consecutive calls`() {
        resetAntiDebugState()
        setRealDeviceBuildFields()

        // 验证连续调用不会抛异常或导致状态不一致
        repeat(10) {
            val level = AntiDebug.assess()
            assertNotNull(level)
        }
    }
}
