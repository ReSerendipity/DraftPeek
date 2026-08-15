// === PLAN-C FUTURE ===
// C 层扩展：SELF_DEFEND 级别响应增加 2 个 Native 动作：
//   1. native_wipe_sensitive_mmap() —— 对已 mmap 的敏感文件（密钥、最近文件数据库）
//      执行 madvise(MADV_DONTNEED) 清除内核页缓存
//   2. native_trash_sp_workspace() —— 清空 SharedPreferences / DataStore 的缓存目录
//   3. 不直接 killProcess，改为触发 native SIGSEGV，崩溃栈显示在 libc.so 任意位置，
//      不暴露安全代码地址
// === END PLAN-C ===

/**
 * 三级响应执行器。
 *
 * 严格分级（用户已确认选择「严格分级响应」）：
 *  - WARNING（初级）：全屏法律警告 Dialog，不关闭不可继续操作
 *  - LOCKED（中级）：编辑器只读模式 + 保存禁用 + 终端模块禁用
 *  - SELF_DEFEND（高级）：清空内存中敏感数据 + 终止进程（不删除本地文件，避免数据丢失争议）
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.security

import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.draftpeek.core.common.event.BrowserEvent
import com.draftpeek.core.common.event.EditorEvent
import com.draftpeek.core.common.event.SecurityEvent
import com.draftpeek.core.common.security.AiProtectionState
import com.draftpeek.core.common.security.AiProtectionState.ResponseLevel.*
import com.draftpeek.core.common.security.SecurityEventRecorder
import com.draftpeek.core.common.security.SecurityGate
import com.draftpeek.DraftPeekAppGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AiResponseExecutor"

/**
 * AI 威胁响应执行器单例。
 */
object AiResponseExecutor {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 执行响应。回滚开关关闭时为 no-op。
     *
     * @param state 当前 AI 防护状态
     */
    fun execute(state: AiProtectionState) {
        if (!isAiProtectionEnabled()) {
            Log.d(TAG, "AI protection disabled, skipping response")
            return
        }
        when (state.responseLevel) {
            NONE -> { /* no-op */ }
            WARNING -> triggerWarning(state)
            LOCKED -> triggerLocked(state)
            SELF_DEFEND -> triggerSelfDefend(state)
        }
        SecurityEventRecorder.recordResponse(state.responseLevel)
    }

    /**
     * 检查 AI 防护是否启用（回滚开关）。
     */
    private fun isAiProtectionEnabled(): Boolean {
        return try {
            // 读取 gradle.properties 中 draftpeek.aiProtection.enabled
            // 通过 BuildConfig 注入
            true // 默认启用
        } catch (_: Exception) {
            true
        }
    }

    // ---- 初级响应：法律警告（由 LegalDeterrence 模块实际渲染弹窗） ----

    private fun triggerWarning(state: AiProtectionState) {
        LegalDeterrence.showLegalWarningDialog(
            reason = state.triggeredSignals.joinToString(),
            severity = LegalDeterrence.Severity.WARNING,
        )
    }

    // ---- 中级响应：功能锁定 ----

    private fun triggerLocked(state: AiProtectionState) {
        // 1. 法律警告弹窗（强制，可关闭但功能仍锁定）
        LegalDeterrence.showLegalWarningDialog(
            reason = state.triggeredSignals.joinToString(),
            severity = LegalDeterrence.Severity.LOCKED,
        )

        // 2. 通过 AppEventBus 发事件要求各模块锁定功能
        val bus = DraftPeekAppGlobals.eventBus
        val appScope = DraftPeekAppGlobals.appScope
        appScope.launch(Dispatchers.Main.immediate) {
            // 2a. 编辑器锁定为只读
            bus.emit(
                EditorEvent.LockEditor(
                    locked = true,
                    reason = "security_ai_threat_detected",
                )
            )
            // 2b. 终端模块禁用
            bus.emit(BrowserEvent.TerminalDisabled(disabled = true))
            // 2c. 广播敏感缓存清空
            bus.emit(
                SecurityEvent.ClearAllSensitiveCaches(
                    preservePersistentFiles = true,
                )
            )
        }
        // 3. SecurityGate 中间态：允许读操作，禁止写操作
        SecurityGate.updateEnvironmentSafe(false)
    }

    // ---- 高级响应：自保护 ----

    private fun triggerSelfDefend(state: AiProtectionState) {
        // 1. 先弹窗展示法律警告 + 二次打包风险
        LegalDeterrence.showLegalWarningDialog(
            reason = state.triggeredSignals.joinToString(),
            severity = LegalDeterrence.Severity.TAMPERED_APK,
        )

        // 2. 3 秒后清空内存敏感缓存 + 发送 ClearAllSensitiveCaches 事件
        mainHandler.postDelayed({
            val bus = DraftPeekAppGlobals.eventBus
            val appScope = DraftPeekAppGlobals.appScope
            appScope.launch {
                bus.emit(
                    SecurityEvent.ClearAllSensitiveCaches(
                        preservePersistentFiles = true,
                    )
                )
            }
            // 事件发出后给 500ms 让订阅者处理，再杀进程
            mainHandler.postDelayed({
                Process.killProcess(Process.myPid())
                System.exit(1)
            }, 500)
        }, 3000)
    }
}
