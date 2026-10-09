package com.draftpeek.core.sync

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
 * - 实现内部负责把载体异常翻译成 [SyncFailureKind]（网络 / 授权 / 仓库 / 其他），
 *   因为只有载体知道「这个错误意味着 token 失效还是网络抖动」。
 */
interface SyncTransport {

    /** 拉取远端变更并合并到本地。 */
    suspend fun pull(): TransferResult

    /** 把本地未推送的变更推到远端。 */
    suspend fun push(): TransferResult
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
