package com.draftpeek.feature.terminal.emulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SecurityValidator")
class SecurityValidatorTest {

    @Nested
    @DisplayName("validate() — safe commands")
    inner class SafeCommandTests {

        @Test
        @DisplayName("空命令被允许")
        fun emptyCommand_allowed() {
            val result = SecurityValidator.validate("")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
        }

        @Test
        @DisplayName("纯空白命令被允许")
        fun whitespaceCommand_allowed() {
            val result = SecurityValidator.validate("   ")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
        }

        @Test
        @DisplayName("ls 命令被允许")
        fun lsCommand_allowed() {
            val result = SecurityValidator.validate("ls -la")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
            assertFalse((result as SecurityValidator.ValidationResult.Allowed).isDangerous)
        }

        @Test
        @DisplayName("echo 命令被允许")
        fun echoCommand_allowed() {
            val result = SecurityValidator.validate("echo hello")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
        }
    }

    @Nested
    @DisplayName("validate() — blocked commands")
    inner class BlockedCommandTests {

        @Test
        @DisplayName("reboot 被阻止")
        fun reboot_blocked() {
            val result = SecurityValidator.validate("reboot")
            assertTrue(result is SecurityValidator.ValidationResult.Blocked)
        }

        @Test
        @DisplayName("shutdown 被阻止")
        fun shutdown_blocked() {
            val result = SecurityValidator.validate("shutdown")
            assertTrue(result is SecurityValidator.ValidationResult.Blocked)
        }

        @Test
        @DisplayName("format 被阻止")
        fun format_blocked() {
            val result = SecurityValidator.validate("format")
            assertTrue(result is SecurityValidator.ValidationResult.Blocked)
        }

        @Test
        @DisplayName("通过完整路径调用被阻止命令也被阻止")
        fun blockedCommandWithPath_blocked() {
            val result = SecurityValidator.validate("/system/bin/reboot")
            assertTrue(result is SecurityValidator.ValidationResult.Blocked)
        }
    }

    @Nested
    @DisplayName("validate() — dangerous commands")
    inner class DangerousCommandTests {

        @Test
        @DisplayName("rm -rf 被标记为危险但允许")
        fun rmRf_dangerousButAllowed() {
            val result = SecurityValidator.validate("rm -rf /tmp/test")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
            assertTrue((result as SecurityValidator.ValidationResult.Allowed).isDangerous)
        }

        @Test
        @DisplayName("dd 写入设备被标记为危险")
        fun ddToDevice_dangerous() {
            val result = SecurityValidator.validate("dd if=/dev/zero of=/dev/sda")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
            assertTrue((result as SecurityValidator.ValidationResult.Allowed).isDangerous)
        }
    }

    @Nested
    @DisplayName("validate() — restricted paths")
    inner class RestrictedPathTests {

        @Test
        @DisplayName("访问 /system 被标记为危险")
        fun systemPath_dangerous() {
            val result = SecurityValidator.validate("ls /system/bin")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
            assertTrue((result as SecurityValidator.ValidationResult.Allowed).isDangerous)
        }

        @Test
        @DisplayName("访问 /proc 被标记为危险")
        fun procPath_dangerous() {
            val result = SecurityValidator.validate("cat /proc/cpuinfo")
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
            assertTrue((result as SecurityValidator.ValidationResult.Allowed).isDangerous)
        }
    }

    @Nested
    @DisplayName("validateArgs()")
    inner class ValidateArgsTests {

        @Test
        @DisplayName("空参数列表被允许")
        fun emptyArgs_allowed() {
            val result = SecurityValidator.validateArgs(emptyList())
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
        }

        @Test
        @DisplayName("安全参数列表被允许")
        fun safeArgs_allowed() {
            val result = SecurityValidator.validateArgs(listOf("ls", "-la"))
            assertTrue(result is SecurityValidator.ValidationResult.Allowed)
        }
    }
}
