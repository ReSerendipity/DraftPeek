package com.draftpeek.feature.browser.model

import com.draftpeek.core.common.vcs.GitHubFileEntry

/**
 * GitHub 导入流程 UI 状态密封类
 *
 * 管理从 GitHub 仓库导入文件的完整流程状态，包括空闲、加载中、文件列表展示、
 * 克隆中、成功、失败等状态。状态转换流程：
 * ```
 * Idle → Loading → FileList → Cloning → Success
 *                    ↓          ↓
 *                  Error      Error
 * ```
 */
sealed class GitHubImportState {
    /** 空闲状态，未进行任何导入操作 */
    data object Idle : GitHubImportState()

    /** 正在从 GitHub API 获取文件树 */
    data object Loading : GitHubImportState()

    /**
     * 文件树已加载，展示文件列表供用户选择
     *
     * @property owner 仓库所有者用户名
     * @property repo 仓库名称
     * @property files 仓库文件列表
     */
    data class FileList(val owner: String, val repo: String, val files: List<GitHubFileEntry>) : GitHubImportState()

    /**
     * 正在克隆用户选择的文件
     *
     * @property progressMessage 最新的克隆进度消息（由OutputThrottler节流），可为null表示无进度
     */
    data class Cloning(val progressMessage: String? = null) : GitHubImportState()

    /**
     * 导入成功完成
     *
     * @property message 成功提示消息
     */
    data class Success(val message: String) : GitHubImportState()

    /**
     * 导入过程中发生错误
     *
     * @property message 错误提示消息
     */
    data class Error(val message: String) : GitHubImportState()
}
