package com.draftpeek.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * AntiDebug 安全模块单元测试（Robolectric 提供真实 android.os.Build 值）。
 *
 * 说明：JVM 下 android.os.Build 的 stub 字段为 null，`Build.BRAND.startsWith(...)`
 * 会触发 NPE，因此使用 Robolectric 运行以获得有效的 Build 字段。
 * 为控制模拟器判定结果，`isRunningOnEmulator` 前通过反射设置非模拟器字段值。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AntiDebugTest {

    private fun setBuildField(fieldName: String, value: String) {
        ReflectionHelpers.setStaticField(android.os.Build::class.java, fieldName, value)
    }

    @Test
    fun `has SAFE SUSPICIOUS HOSTILE security levels`() {
        val levels = AntiDebug.SecurityLevel.entries
        assertTrue(levels.contains(AntiDebug.SecurityLevel.SAFE))
        assertTrue(levels.contains(AntiDebug.SecurityLevel.SUSPICIOUS))
        assertTrue(levels.contains(AntiDebug.SecurityLevel.HOSTILE))
        assertEquals(3, levels.size)
    }

    @Test
    fun `isDebuggerConnected returns false without debugger`() {
        assertFalse(AntiDebug.isDebuggerConnected())
    }

    @Test
    fun `isRunningOnEmulator returns false for non emulator Build values`() {
        // 设置非模拟器的 Build 字段，确保判定为 false
        setBuildField("BRAND", "samsung")
        setBuildField("DEVICE", "beyond1")
        setBuildField("FINGERPRINT", "samsung/beyond1/beyond1:14/UP1A.231005.007:user/release-keys")
        setBuildField("HARDWARE", "qcom")
        setBuildField("MODEL", "SM-G973F")
        setBuildField("MANUFACTURER", "samsung")
        setBuildField("PRODUCT", "beyond1")

        assertFalse(AntiDebug.isRunningOnEmulator())
    }

    @Test
    fun `isTraced returns a Boolean without throwing`() {
        // 兼容 /proc/self/status 存在/不存在的环境，仅验证不抛异常
        val result = AntiDebug.isTraced()
        assertNotNull(result)
    }

    @Test
    fun `isFridaDetected returns false in clean environment`() {
        val result = AntiDebug.isFridaDetected()
        assertFalse(result)
    }

    @Test
    fun `isXposedDetected returns false in clean environment`() {
        val result = AntiDebug.isXposedDetected()
        assertFalse(result)
    }

    @Test
    fun `isRooted returns false in clean environment`() {
        val result = AntiDebug.isRooted()
        assertFalse(result)
    }

    @Test
    fun `isHookFrameworkDetected returns false in clean environment`() {
        val result = AntiDebug.isHookFrameworkDetected()
        assertFalse(result)
    }

    @Test
    fun `quickCheck returns true when no debugger attached`() {
        assertTrue(AntiDebug.quickCheck())
    }

    @Test
    fun `nextCheckIntervalMs stays within 3000 to 15000`() {
        repeat(100) {
            val interval = AntiDebug.nextCheckIntervalMs()
            assertTrue(interval >= 3000)
            assertTrue(interval <= 15000)
        }
    }

    @Test
    fun `initialDelayMs stays within 1000 to 5000`() {
        repeat(100) {
            val delay = AntiDebug.initialDelayMs()
            assertTrue(delay >= 1000)
            assertTrue(delay <= 5000)
        }
    }

    @Test
    fun `currentLevel is a defined security level`() {
        assertNotNull(AntiDebug.currentLevel)
        assertTrue(AntiDebug.SecurityLevel.entries.contains(AntiDebug.currentLevel))
    }
}
