package com.draftpeek.feature.terminal.viewmodel

import android.content.Context
import com.draftpeek.feature.terminal.emulator.ProotSessionManager
import com.draftpeek.feature.terminal.emulator.ProotSetupState
import com.draftpeek.feature.terminal.emulator.TerminalKey
import com.draftpeek.feature.terminal.emulator.TerminalSessionManager
import com.draftpeek.feature.terminal.model.TerminalConfig
import com.draftpeek.feature.terminal.model.TerminalNavigationData
import com.draftpeek.feature.terminal.model.TerminalSession
import com.draftpeek.feature.terminal.model.TerminalTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TerminalViewModel")
class TerminalViewModelTest {

    private lateinit var sessionManager: TerminalSessionManager
    private lateinit var prootSessionManager: ProotSessionManager
    private lateinit var context: Context
    private lateinit var viewModel: TerminalViewModel

    private val sessionsFlow = MutableStateFlow<List<TerminalSession>>(emptyList())
    private val activeSessionFlow = MutableStateFlow<TerminalSession?>(null)
    private val prootSetupStateFlow = MutableStateFlow<ProotSetupState>(ProotSetupState.NotChecked)

    @BeforeEach
    fun setUp() {
        sessionManager = mockk(relaxed = true)
        prootSessionManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { sessionManager.sessions } returns sessionsFlow
        every { sessionManager.activeSession } returns activeSessionFlow
        every { sessionManager.getOutputBuffer(any()) } returns MutableStateFlow("")
        every { sessionManager.prootManager } returns prootSessionManager
        every { prootSessionManager.setupState } returns prootSetupStateFlow

        // Reset TerminalNavigationData before each test
        TerminalNavigationData.setCwd(null)

        viewModel = TerminalViewModel(sessionManager, context)
    }

    @Test
    @DisplayName("inputBuffer starts empty")
    fun inputBufferStartsEmpty() {
        assertEquals("", viewModel.inputBuffer.value)
    }

    @Test
    @DisplayName("theme starts with default TerminalTheme")
    fun themeStartsWithDefault() {
        val theme = viewModel.theme.value
        assertEquals(TerminalTheme().background, theme.background)
        assertEquals(TerminalTheme().foreground, theme.foreground)
    }

    @Test
    @DisplayName("updateInput updates inputBuffer")
    fun updateInputUpdatesBuffer() {
        viewModel.updateInput("ls -la")
        assertEquals("ls -la", viewModel.inputBuffer.value)
    }

    @Test
    @DisplayName("submitInput sends input with newline and clears buffer")
    fun submitInputSendsAndClears() {
        viewModel.updateInput("echo hello")
        viewModel.submitInput()

        verify { sessionManager.sendInput("echo hello\n") }
        assertEquals("", viewModel.inputBuffer.value)
    }

    @Test
    @DisplayName("submitInput does nothing when buffer is empty")
    fun submitInputDoesNothingWhenEmpty() {
        viewModel.updateInput("")
        viewModel.submitInput()

        verify(exactly = 0) { sessionManager.sendInput(any<String>()) }
    }

    @Test
    @DisplayName("sendInput delegates to sessionManager")
    fun sendInputDelegates() {
        viewModel.sendInput("test")
        verify { sessionManager.sendInput("test") }
    }

    @Test
    @DisplayName("sendKey delegates to sessionManager")
    fun sendKeyDelegates() {
        viewModel.sendKey(TerminalKey.ENTER)
        verify { sessionManager.sendKeyEvent(TerminalKey.ENTER) }
    }

    @Test
    @DisplayName("switchSession delegates to sessionManager")
    fun switchSessionDelegates() {
        viewModel.switchSession("session-123")
        verify { sessionManager.switchSession("session-123") }
    }

    @Test
    @DisplayName("setTheme updates theme state")
    fun setThemeUpdatesState() {
        val newTheme = TerminalTheme(
            background = 0xFF000000,
            foreground = 0xFFFFFFFF,
        )
        viewModel.setTheme(newTheme)
        assertEquals(0xFF000000, viewModel.theme.value.background)
        assertEquals(0xFFFFFFFF, viewModel.theme.value.foreground)
    }

    @Test
    @DisplayName("destroySession delegates to sessionManager")
    fun destroySessionDelegates() {
        every { sessionManager.sessions } returns MutableStateFlow(emptyList())
        viewModel.destroySession("session-123")
        verify { sessionManager.destroySession("session-123") }
    }

    @Test
    @DisplayName("sessions flow is empty initially")
    fun sessionsFlowEmptyInitially() {
        assertTrue(viewModel.sessions.value.isEmpty())
    }

    @Test
    @DisplayName("activeSession is null initially")
    fun activeSessionNullInitially() {
        assertEquals(null, viewModel.activeSession.value)
    }

    @Test
    @DisplayName("outputBuffer is empty initially")
    fun outputBufferEmptyInitially() {
        assertEquals("", viewModel.outputBuffer.value)
    }

    // ---- New tests for integrated terminal ----

    @Test
    @DisplayName("consumePendingCwd returns config with default cwd when no pendingCwd")
    fun consumePendingCwdDefault() {
        TerminalNavigationData.setCwd(null)
        val config = viewModel.consumePendingCwd()
        assertNotNull(config)
        assertNull(viewModel.currentCwd.value)
        assertTrue(viewModel.cwdConsumed.value)
    }

    @Test
    @DisplayName("consumePendingCwd returns config with cwd when pendingCwd is set")
    fun consumePendingCwdWithCwd() {
        TerminalNavigationData.setCwd("/sdcard/myproject")
        val config = viewModel.consumePendingCwd()
        assertEquals("/sdcard/myproject", config.workingDirectory)
        assertEquals("/sdcard/myproject", viewModel.currentCwd.value)
    }

    @Test
    @DisplayName("consumePendingCwd clears pendingCwd after consumption")
    fun consumePendingCwdClearsAfterConsumption() {
        TerminalNavigationData.setCwd("/sdcard/myproject")
        viewModel.consumePendingCwd()
        // Second call should return default config
        assertNull(TerminalNavigationData.pendingCwd.value)
    }

    @Test
    @DisplayName("executeQuickCommand sends command with newline")
    fun executeQuickCommandSendsCommand() {
        viewModel.executeQuickCommand("git status")
        verify { sessionManager.sendInput("git status\n") }
    }

    @Test
    @DisplayName("prootSetupState reflects ProotSessionManager state")
    fun prootSetupStateReflectsManager() {
        prootSetupStateFlow.value = ProotSetupState.Ready
        assertEquals(ProotSetupState.Ready, viewModel.prootSetupState.value)
    }

    @Test
    @DisplayName("currentCwd is null initially")
    fun currentCwdNullInitially() {
        assertNull(viewModel.currentCwd.value)
    }

    @Test
    @DisplayName("quickCommands is empty initially")
    fun quickCommandsEmptyInitially() {
        assertTrue(viewModel.quickCommands.value.isEmpty())
    }

    @Test
    @DisplayName("cwdConsumed is false initially")
    fun cwdConsumedFalseInitially() {
        assertFalse(viewModel.cwdConsumed.value)
    }
}
