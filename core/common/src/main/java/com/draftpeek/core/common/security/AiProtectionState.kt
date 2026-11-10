/**
 * AI 对抗防护状态模型。
 *
 * 定义 AI 威胁等级、检测信号和响应级别。供 [com.draftpeek.security.AiDetector] 使用，
 * 由 [AiProtectionStateHolder] 全局持有当前状态供 UI / SecurityGate 读取。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.common.security

/**
 * AI 威胁等级。
 *
 * 与 [com.draftpeek.security.AntiDebug.SecurityLevel] 并列，专门描述 AI 辅助逆向威胁。
 */
enum class AiThreatLevel {
    /** 未检测到 AI 辅助逆向特征 */
    SAFE,

    /** 存在轻微可疑迹象（触发初级响应） */
    SUSPICIOUS,

    /** 明确 AI 逆向行为（触发中/高级响应） */
    HOSTILE
}

/**
 * 所有可检测的 AI 逆向信号。
 *
 * A 层信号在 Kotlin 代码中实现；B 层信号需要 IR 埋点后才会触发。
 */
enum class AiDetectionSignal {
    // --- A 层检测信号 ---
    /** 增强模拟器指纹命中 */
    EMULATOR_ENHANCED_FINGERPRINT,

    /** 短时间大量类加载（沙箱批量分析） */
    CLASSLOADING_BURST,

    /** 反射调用间隔异常均匀（脚本/AI驱动） */
    REFLECTION_TIMING_ANOMALY,

    /** 触控时间戳过于规律（非人操作） */
    SANDBOX_TOUCH_TIMING,

    /** 复用 AntiDebug 检测到 Frida/Xposed */
    HOSTILE_FRIDA_XPOSED,

    /** DEX CRC 不匹配 */
    DEX_TAMPERED,

    /** 签名不匹配（二次打包） */
    SIGNATURE_MISMATCH,

    // --- B 层精选补充（ASM/IR 埋点后才会触发） ---
    /** B 层埋点检测到 Class.forName 超阈值 */
    ASM_CLASS_FORNAME_BURST,

    /** B 层埋点检测到 Method.invoke 超阈值 */
    ASM_METHOD_INVOKE_BURST,

    /** B 层埋点检测到安全类异常实例化时机 */
    ASM_SENSITIVE_CLASS_CREATION
}

/**
 * DEX 完整性校验的三态结论。
 *
 * 只有 [PASSED] 表示「已校验且与构建期基线一致」。默认值必须是 [UNKNOWN]：
 * 检测尚未执行、基线缺失导致跳过校验、校验过程异常时都停在或回到该值，
 * 不允许用「没发现 DEX_TAMPERED 信号」推定通过（安全状态不 fail-open）。
 */
enum class DexIntegrityStatus {
    /** 未检测：自检尚未执行、缺少基线跳过校验，或校验过程无法判定 */
    UNKNOWN,

    /** 通过：校验完成且 CRC 求和与构建期基线一致 */
    PASSED,

    /** 失败：CRC 与基线不一致（DEX 被 patch / 二次打包），或自检整体中断 */
    FAILED
}

/**
 * AI 对抗防护状态数据类。
 *
 * @property threatLevel 当前威胁等级
 * @property triggeredSignals 已触发的检测信号集合
 * @property responseLevel 当前响应级别
 * @property dexIntegrityStatus DEX 完整性三态结论（默认未检测）
 * @property dexIntegrityReason 非 [DexIntegrityStatus.PASSED] 时的技术细节（供 UI 展示原因）
 */
data class AiProtectionState(
    val threatLevel: AiThreatLevel = AiThreatLevel.SAFE,
    val triggeredSignals: Set<AiDetectionSignal> = emptySet(),
    val responseLevel: ResponseLevel = ResponseLevel.NONE,
    val dexIntegrityStatus: DexIntegrityStatus = DexIntegrityStatus.UNKNOWN,
    val dexIntegrityReason: String? = null
) {
    /**
     * 响应级别枚举。
     */
    enum class ResponseLevel {
        /** 无响应 */
        NONE,

        /** 初级：法律警告 */
        WARNING,

        /** 中级：功能锁定 */
        LOCKED,

        /** 高级：自保护 */
        SELF_DEFEND
    }

    /**
     * 将触发信号集合转换为 bitmask（用于 Room 存储）。
     */
    fun signalsToMask(): Int {
        var mask = 0
        for (signal in triggeredSignals) {
            mask = mask or (1 shl signal.ordinal)
        }
        return mask
    }

    companion object {
        /**
         * 从 bitmask 恢复触发信号集合。
         */
        fun maskToSignals(mask: Int): Set<AiDetectionSignal> {
            val result = mutableSetOf<AiDetectionSignal>()
            for (signal in AiDetectionSignal.entries) {
                if (mask and (1 shl signal.ordinal) != 0) {
                    result.add(signal)
                }
            }
            return result
        }
    }
}
