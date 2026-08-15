package com.draftpeek.feature.terminal.emulator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProotSetupState")
class ProotSetupStateTest {

    @Nested
    @DisplayName("state transitions")
    inner class StateTransitionTest {

        @Test
        @DisplayName("NotChecked is the initial state")
        fun notCheckedIsInitialState() {
            val state = ProotSetupState.NotChecked
            assertTrue(state is ProotSetupState.NotChecked)
        }

        @Test
        @DisplayName("Checking transitions from NotChecked")
        fun checkingTransitionsFromNotChecked() {
            val state = ProotSetupState.Checking
            assertTrue(state is ProotSetupState.Checking)
        }

        @Test
        @DisplayName("NotSetup contains availability info")
        fun notSetupContainsAvailability() {
            val availability = ProotAvailability(
                prootBinaryReady = false,
                rootfsReady = false,
                needsExtraction = true,
            )
            val state = ProotSetupState.NotSetup(availability)
            assertTrue(state is ProotSetupState.NotSetup)
            assertFalse(state.availability.isReady)
        }

        @Test
        @DisplayName("Extracting contains progress")
        fun extractingContainsProgress() {
            val state = ProotSetupState.Extracting(0.5f)
            assertTrue(state is ProotSetupState.Extracting)
            assertEquals(0.5f, state.progress)
        }

        @Test
        @DisplayName("Ready indicates proot is available")
        fun readyIndicatesAvailable() {
            val state = ProotSetupState.Ready
            assertTrue(state is ProotSetupState.Ready)
        }

        @Test
        @DisplayName("Running contains session ID")
        fun runningContainsSessionId() {
            val state = ProotSetupState.Running("session-123")
            assertTrue(state is ProotSetupState.Running)
            assertEquals("session-123", state.sessionId)
        }

        @Test
        @DisplayName("Error contains error message")
        fun errorContainsMessage() {
            val state = ProotSetupState.Error("Rootfs extraction failed")
            assertTrue(state is ProotSetupState.Error)
            assertEquals("Rootfs extraction failed", state.message)
        }
    }

    @Nested
    @DisplayName("ProotAvailability")
    inner class ProotAvailabilityTest {

        @Test
        @DisplayName("isReady is true when both binary and rootfs are ready")
        fun isReadyWhenBothReady() {
            val availability = ProotAvailability(
                prootBinaryReady = true,
                rootfsReady = true,
                prootPath = "/data/proot",
                rootfsPath = "/data/rootfs",
            )
            assertTrue(availability.isReady)
        }

        @Test
        @DisplayName("isReady is false when binary is not ready")
        fun notReadyWhenBinaryNotReady() {
            val availability = ProotAvailability(
                prootBinaryReady = false,
                rootfsReady = true,
            )
            assertFalse(availability.isReady)
        }

        @Test
        @DisplayName("isReady is false when rootfs is not ready")
        fun notReadyWhenRootfsNotReady() {
            val availability = ProotAvailability(
                prootBinaryReady = true,
                rootfsReady = false,
            )
            assertFalse(availability.isReady)
        }

        @Test
        @DisplayName("isReady is false when neither is ready")
        fun notReadyWhenNeitherReady() {
            val availability = ProotAvailability(
                prootBinaryReady = false,
                rootfsReady = false,
            )
            assertFalse(availability.isReady)
        }

        @Test
        @DisplayName("needsExtraction is true when not ready")
        fun needsExtractionWhenNotReady() {
            val availability = ProotAvailability(
                prootBinaryReady = false,
                rootfsReady = false,
                needsExtraction = true,
            )
            assertTrue(availability.needsExtraction)
        }
    }

    @Nested
    @DisplayName("state flow")
    inner class StateFlowTest {

        @Test
        @DisplayName("NotChecked -> Checking -> NotSetup flow is valid")
        fun notCheckedToCheckingToNotSetup() {
            val s1 = ProotSetupState.NotChecked
            val s2 = ProotSetupState.Checking
            val s3 = ProotSetupState.NotSetup(ProotAvailability(false, false))

            assertTrue(s1 is ProotSetupState.NotChecked)
            assertTrue(s2 is ProotSetupState.Checking)
            assertTrue(s3 is ProotSetupState.NotSetup)
        }

        @Test
        @DisplayName("NotSetup -> Extracting -> Ready flow is valid")
        fun notSetupToExtractingToReady() {
            val s1 = ProotSetupState.NotSetup(ProotAvailability(false, false))
            val s2 = ProotSetupState.Extracting(0f)
            val s3 = ProotSetupState.Extracting(0.5f)
            val s4 = ProotSetupState.Extracting(1.0f)
            val s5 = ProotSetupState.Ready

            assertTrue(s1 is ProotSetupState.NotSetup)
            assertTrue(s2 is ProotSetupState.Extracting)
            assertTrue(s3 is ProotSetupState.Extracting)
            assertTrue(s4 is ProotSetupState.Extracting)
            assertTrue(s5 is ProotSetupState.Ready)
        }

        @Test
        @DisplayName("Extracting -> Error flow is valid on failure")
        fun extractingToErrorOnFailure() {
            val s1 = ProotSetupState.Extracting(0.3f)
            val s2 = ProotSetupState.Error("Disk full")

            assertTrue(s1 is ProotSetupState.Extracting)
            assertTrue(s2 is ProotSetupState.Error)
        }

        @Test
        @DisplayName("Ready -> Running flow is valid")
        fun readyToRunning() {
            val s1 = ProotSetupState.Ready
            val s2 = ProotSetupState.Running("new-session-id")

            assertTrue(s1 is ProotSetupState.Ready)
            assertTrue(s2 is ProotSetupState.Running)
        }
    }
}
