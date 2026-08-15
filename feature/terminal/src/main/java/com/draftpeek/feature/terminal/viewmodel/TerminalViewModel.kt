/**
 * 终端ViewModel文件。
 *
 * 管理终端UI状态、会话和用户输入，作为UI层与会话管理器之间的桥梁。
 * 使用Hilt依赖注入，生命周期感知。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.feature.terminal.emulator.ProotSetupState
import com.draftpeek.feature.terminal.emulator.TerminalKey
import com.draftpeek.feature.terminal.emulator.TerminalSessionManager
import com.draftpeek.feature.terminal.model.TerminalConfig
import com.draftpeek.feature.terminal.model.TerminalNavigationData
import com.draftpeek.feature.terminal.model.TerminalSession
import com.draftpeek.feature.terminal.model.TerminalTheme
import com.draftpeek.feature.terminal.service.TerminalService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 快捷命令数据类。
 *
 * @property label 显示标签
 * @property command 实际执行的命令
 */
data class QuickCommand(
    val label: String,
    val command: String,
)

/**
 * 终端ViewModel。
 *
 * 管理终端会话、输入缓冲区和主题状态，处理用户输入事件。
 * 自动管理前台服务的启动和停止。
 * 支持集成终端：消费 cwd、检测快捷命令、管理 Proot 引导状态。
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sessionManager: TerminalSessionManager,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val context = context.applicationContext

    /** 所有终端会话列表 */
    val sessions: StateFlow<List<TerminalSession>> = sessionManager.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前活动终端会话 */
    val activeSession: StateFlow<TerminalSession?> = sessionManager.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 当前活动会话的输出缓冲区 */
    val outputBuffer: StateFlow<String> = activeSession
        .flatMapLatest { session ->
            session?.let { sessionManager.getOutputBuffer(it.id) } ?: flowOf("")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _inputBuffer = MutableStateFlow("")
    /** 用户输入缓冲区 */
    val inputBuffer: StateFlow<String> = _inputBuffer.asStateFlow()

    private val _theme = MutableStateFlow(TerminalTheme())
    /** 终端颜色主题 */
    val theme: StateFlow<TerminalTheme> = _theme.asStateFlow()

    /** 当前工作目录 */
    private val _currentCwd = MutableStateFlow<String?>(null)
    val currentCwd: StateFlow<String?> = _currentCwd.asStateFlow()

    /** Proot 设置状态 */
    val prootSetupState: StateFlow<ProotSetupState> = sessionManager.prootManager.setupState

    /** 快捷命令列表 */
    private val _quickCommands = MutableStateFlow<List<QuickCommand>>(emptyList())
    val quickCommands: StateFlow<List<QuickCommand>> = _quickCommands.asStateFlow()

    /** 是否已尝试消费 pendingCwd */
    private val _cwdConsumed = MutableStateFlow(false)
    val cwdConsumed: StateFlow<Boolean> = _cwdConsumed.asStateFlow()

    /**
     * 消费来自入口的 pendingCwd，返回应使用的终端配置。
     * 如果有 pendingCwd，使用该目录作为工作目录；否则使用默认配置。
     * 消费后清空 pendingCwd。
     *
     * @return 终端配置
     */
    fun consumePendingCwd(): TerminalConfig {
        val cwd = TerminalNavigationData.consumeCwd()
        _currentCwd.value = cwd
        _cwdConsumed.value = true

        // 更新快捷命令
        updateQuickCommands(cwd)

        return if (cwd != null) {
            TerminalConfig(workingDirectory = cwd)
        } else {
            TerminalConfig()
        }
    }

    /**
     * 根据工作目录更新快捷命令。
     * 检测目录含 `.git` → git 相关命令；否则基础命令。
     */
    private fun updateQuickCommands(cwd: String?) {
        val dir = cwd ?: return
        val commands = if (File(dir, ".git").exists()) {
            listOf(
                QuickCommand("ls", "ls"),
                QuickCommand("git status", "git status"),
                QuickCommand("git diff", "git diff"),
                QuickCommand("git log", "git log --oneline -10"),
                QuickCommand("http.server", "python -m http.server"),
            )
        } else {
            listOf(
                QuickCommand("ls", "ls"),
                QuickCommand("pwd", "pwd"),
            )
        }
        _quickCommands.value = commands
    }

    /**
     * 检查 Proot 是否需要设置，如果需要则触发 rootfs 解压。
     */
    fun checkAndSetupProot() {
        viewModelScope.launch {
            val state = prootSetupState.value
            if (state is ProotSetupState.NotChecked || state is ProotSetupState.NotSetup) {
                val availability = sessionManager.prootManager.checkProotAvailable()
                if (!availability.isReady) {
                    sessionManager.prootManager.extractRootfs()
                }
            }
        }
    }

    /**
     * 创建新的终端会话并启动前台服务保持终端存活。
     * @param config 终端配置
     */
    fun createSession(config: TerminalConfig) {
        val title = config.workingDirectory.let { cwd ->
            val dirName = File(cwd).name
            if (dirName.isNotBlank()) dirName else "Shell"
        }

        viewModelScope.launch {
            sessionManager.createSession(config, title)
            TerminalService.start(context)
        }
    }

    /**
     * 向活动会话发送输入文本。
     * @param text 要发送的文本
     */
    fun sendInput(text: String) {
        sessionManager.sendInput(text)
    }

    /**
     * 向活动会话发送特殊按键事件。
     * @param key 终端按键
     */
    fun sendKey(key: TerminalKey) {
        sessionManager.sendKeyEvent(key)
    }

    /**
     * 提交输入缓冲区内容到终端并清空缓冲区。
     */
    fun submitInput() {
        val input = _inputBuffer.value
        if (input.isNotEmpty()) {
            sessionManager.sendInput(input + "\n")
            _inputBuffer.value = ""
        }
    }

    /**
     * 更新输入缓冲区内容。
     * @param text 新的输入文本
     */
    fun updateInput(text: String) {
        _inputBuffer.value = text
    }

    /**
     * 执行快捷命令。
     * @param command 要执行的命令
     */
    fun executeQuickCommand(command: String) {
        sessionManager.sendInput(command + "\n")
    }

    /**
     * 切换到指定会话。
     * @param sessionId 目标会话ID
     */
    fun switchSession(sessionId: String) {
        sessionManager.switchSession(sessionId)
    }

    /**
     * 销毁指定会话，所有会话关闭时停止前台服务。
     * @param sessionId 要销毁的会话ID
     */
    fun destroySession(sessionId: String) {
        sessionManager.destroySession(sessionId)
        if (sessionManager.sessions.value.isEmpty()) {
            TerminalService.stop(context)
        }
    }

    /**
     * 设置终端颜色主题。
     * @param theme 新的主题
     */
    fun setTheme(theme: TerminalTheme) {
        _theme.value = theme
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.destroyAll()
        TerminalService.stop(context)
    }
}
