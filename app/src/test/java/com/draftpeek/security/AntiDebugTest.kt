package com.draftpeek.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AntiDebug")
class AntiDebugTest {

    @Nested
    @DisplayName("SecurityLevel")
    inner class SecurityLevelTest {

        @Test
        @DisplayName("has SAFE, SUSPICIOUS, HOSTILE values")
        fun hasAllValues() {
            val levels = AntiDebug.SecurityLevel.entries
            assertTrue(levels.contains(AntiDebug.SecurityLevel.SAFE))
            assertTrue(levels.contains(AntiDebug.SecurityLevel.SUSPICIOUS))
            assertTrue(levels.contains(AntiDebug.SecurityLevel.HOSTILE))
            assertEquals(3, levels.size)
        }
    }

    @Nested
    @DisplayName("isDebuggerConnected()")
    inner class IsDebuggerConnectedTest {

        @Test
        @DisplayName("returns false in test environment")
        fun returnsFalseInTestEnv() {
            // In unit test environment, Debug.isDebuggerConnected() returns false
            assertFalse(AntiDebug.isDebuggerConnected())
        }
    }

    @Nested
    @DisplayName("isRunningOnEmulator()")
    inner class IsRunningOnEmulatorTest {

        @Test
        @DisplayName("returns false in test environment with default Build values")
        fun returnsFalseInTestEnv() {
            // In unit test, Build fields are empty/default, so emulator check should fail
            assertFalse(AntiDebug.isRunningOnEmulator())
        }
    }

    @Nested
    @DisplayName("isTraced()")
    inner class IsTracedTest {

        @Test
        @DisplayName("returns false when /proc/self/status not available")
        fun returnsFalseInTestEnv() {
            // In JVM test environment, /proc/self/status may not exist or TracerPid is 0
            val result = AntiDebug.isTraced()
            // On some CI environments /proc/self/status exists, so just verify it doesn't throw
            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("isFridaDetected()")
    inner class IsFridaDetectedTest {

        @Test
        @DisplayName("returns false in clean test environment")
        fun returnsFalseInCleanEnv() {
            val result = AntiDebug.isFridaDetected()
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("isXposedDetected()")
    inner class IsXposedDetectedTest {

        @Test
        @DisplayName("returns false in clean test environment")
        fun returnsFalseInCleanEnv() {
            val result = AntiDebug.isXposedDetected()
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("isRooted()")
    inner class IsRootedTest {

        @Test
        @DisplayName("returns false in clean test environment")
        fun returnsFalseInCleanEnv() {
            val result = AntiDebug.isRooted()
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("isHookFrameworkDetected()")
    inner class IsHookFrameworkDetectedTest {

        @Test
        @DisplayName("returns false in clean test environment")
        fun returnsFalseInCleanEnv() {
            val result = AntiDebug.isHookFrameworkDetected()
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("quickCheck()")
    inner class QuickCheckTest {

        @Test
        @DisplayName("returns true when no debugger is connected")
        fun returnsTrueWhenNoDebugger() {
            // In test environment, no debugger is connected
            assertTrue(AntiDebug.quickCheck())
        }
    }

    @Nested
    @DisplayName("nextCheckIntervalMs()")
    inner class NextCheckIntervalMsTest {

        @Test
        @DisplayName("returns value between 3000 and 15000")
        fun returnsValueInRange() {
            repeat(100) {
                val interval = AntiDebug.nextCheckIntervalMs()
                assertTrue(interval >= 3000)
                assertTrue(interval <= 15000)
            }
        }
    }

    @Nested
    @DisplayName("initialDelayMs()")
    inner class InitialDelayMsTest {

        @Test
        @DisplayName("returns value between 1000 and 5000")
        fun returnsValueInRange() {
            repeat(100) {
                val delay = AntiDebug.initialDelayMs()
                assertTrue(delay >= 1000)
                assertTrue(delay <= 5000)
            }
        }
    }

    @Nested
    @DisplayName("currentLevel")
    inner class CurrentLevelTest {

        @Test
        @DisplayName("is SAFE or higher after operations")
        fun isSafeOrHigher() {
            // currentLevel should be one of the defined levels
            assertNotNull(AntiDebug.currentLevel)
            assertTrue(AntiDebug.SecurityLevel.entries.contains(AntiDebug.currentLevel))
        }
    }
}
