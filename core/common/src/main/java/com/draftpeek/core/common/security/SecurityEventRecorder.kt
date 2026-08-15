// === PLAN-C FUTURE ===
// C 层扩展：新增策略热更新框架
//   C1. 新增 app/src/main/assets/default_ai_protection_policy.json（打包时附带）
//   C2. 新增 PolicyUpdater：启动时检查配置服务器
//        https://policy.draftpeek.com/ai-protection/policy-v1.sig
//        文件内容 = BASE64(JSON 策略体) + "." + Ed25519 签名 BASE64
//   C3. 本地 Ed25519 公钥 = 硬编码 32 字节（build.gradle 注入）
//   C4. 验签通过后写 SQLCipher policy 表覆盖默认设置
//   C5. 策略体包含：
//        - 新的 AI 检测信号阈值
//        - 新的 Frida/Xposed/Root 黑名单路径正则
//        - 新的虚假类种子（下次启动 ASM 动态加载）
//   好处：出现新 AI 逆向工具时无需发布新版 APK，在线更新策略即可。
//   当前阶段暂不实现，本注释作为未来扩展点。
// === END PLAN-C ===

/**
 * 安全事件记录入口。
 *
 * 被 AiDetector / AiResponseExecutor / LegalDeterrence 调用。
 * 所有写操作走 Dispatchers.IO，数据库通过 Hilt 获取 SecurityEventRepository。
 *
 * 由于 core/common 不能直接依赖 core/data，本对象使用回调模式：
 * app 层在 DraftPeekApp.onCreate 中通过 [setSink] 注入实际的数据库写入回调。
 * 未注入时为 no-op，不影响功能。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.common.security

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 安全事件记录器单例。
 *
 * 使用回调模式解耦 core/common → core/data 的依赖方向。
 * app 层初始化时调用 [setSink] 注入数据库写入回调。
 */
object SecurityEventRecorder {

    private const val TAG = "SecEventRecorder"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 事件写入回调接口。
     * 由 app 层实现并注入。
     */
    fun interface Sink {
        suspend fun record(
            eventType: String,
            threatLevel: String,
            signalsMask: Int,
            responseLevel: String,
        )
    }

    @Volatile
    private var sink: Sink? = null

    /**
     * 注入数据库写入回调。由 app 层在启动时调用。
     */
    fun setSink(sink: Sink) {
        this.sink = sink
    }

    /**
     * 记录检测结果事件。
     */
    fun recordDetection(state: AiProtectionState) {
        val s = sink ?: return
        scope.launch {
            try {
                s.record(
                    eventType = "DETECTION",
                    threatLevel = state.threatLevel.name,
                    signalsMask = state.signalsToMask(),
                    responseLevel = state.responseLevel.name,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record detection event", e)
            }
        }
    }

    /**
     * 记录响应执行事件。
     */
    fun recordResponse(level: AiProtectionState.ResponseLevel) {
        val s = sink ?: return
        scope.launch {
            try {
                s.record(
                    eventType = "RESPONSE",
                    threatLevel = "",
                    signalsMask = 0,
                    responseLevel = level.name,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record response event", e)
            }
        }
    }

    /**
     * 记录完整性校验事件。
     */
    fun recordIntegrityVerify(passed: Boolean) {
        val s = sink ?: return
        scope.launch {
            try {
                s.record(
                    eventType = "INTEGRITY_VERIFY",
                    threatLevel = if (passed) "SAFE" else "HOSTILE",
                    signalsMask = 0,
                    responseLevel = "NONE",
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record integrity verify event", e)
            }
        }
    }

    /**
     * 记录用户确认事件。
     */
    fun recordUserAck(threatLevel: AiThreatLevel, acceptedRisk: Boolean) {
        val s = sink ?: return
        scope.launch {
            try {
                s.record(
                    eventType = "USER_ACK",
                    threatLevel = threatLevel.name,
                    signalsMask = 0,
                    responseLevel = if (acceptedRisk) "WARNING" else "NONE",
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record user ack event", e)
            }
        }
    }
}
