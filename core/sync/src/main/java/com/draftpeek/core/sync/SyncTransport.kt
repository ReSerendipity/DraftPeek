package com.draftpeek.core.sync

/**
 * 一次同步要传输的文档集合：**仓库内相对路径 → 文本内容**。
 *
 * 用「路径 → 内容」这种中性形态而不是具体业务模型，是为了让传输层不依赖
 * 业务数据的结构（阅读进度 / 片段 / 统计 / 主题）——后者由上层决定如何序列化。
 */
data class SyncSnapshot(val files: Map<String, String>) {

    companion object {
        val EMPTY = SyncSnapshot(emptyMap())
    }
}

/** 拉取结果：成功时带远端快照。 */
sealed interface PullOutcome {

    /** 远端快照（首次同步、远端为空时为 [SyncSnapshot.EMPTY]）。 */
    data class Success(val snapshot: SyncSnapshot) : PullOutcome

    data class Failure(val kind: SyncFailureKind, val cause: Throwable? = null) : PullOutcome
}

/** 一次传输的结果。 */
sealed interface TransferResult {

    /** 成功。 */
    data object Success : TransferResult

    /**
     * 失败。
     *
     * @param kind 失败分类 —— 决定状态机是否退避重试
     * @param cause 原始异常，仅用于日志
     */
    data class Failure(val kind: SyncFailureKind, val cause: Throwable? = null) : TransferResult
}

/**
 * 同步载体抽象。
 *
 * **当前实现方向（产品决策 2026-10-09，方案 C）**：用户**自己的 Git 仓库**——
 * 拉取/推送都是对用户私有仓库的提交与合并，我方不存储任何用户数据。
 *
 * **为什么保留这层抽象**：万一将来改判方案 A（第三方 BaaS）或 B（自建后端），
 * 只需换一个实现，上层的 [SyncMachine] 与身份区 UI 不需要重写。
 *
 * 实现约定：
 * - 两个方法都必须**可重入且幂等**（状态机会在退避后重复调用）；
 * - 实现内部负责把载体异常翻译成 [SyncFailureKind]（网络 / 授权 / 仓库 / 冲突 / 其他），
 *   因为只有载体知道「这个错误意味着 token 失效、仓库不可写，还是远端已被别人改动」。
 */
interface SyncTransport {

    /** 拉取远端快照。 */
    suspend fun pull(): PullOutcome

    /** 把本地快照推到远端。 */
    suspend fun push(snapshot: SyncSnapshot): TransferResult
}
