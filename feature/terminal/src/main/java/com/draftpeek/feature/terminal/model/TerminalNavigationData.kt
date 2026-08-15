/**
 * 终端导航数据传递单例。
 *
 * 用于跨模块传递终端工作目录（cwd）。入口（文件页/编辑器）写入 cwd，
 * 终端页消费后清空，避免路由参数 URL 编码与脏参数问题。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.feature.terminal.model

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 终端导航数据单例。
 *
 * 使用全局 [MutableStateFlow] 持有待消费的 cwd，入口写入、终端页消费后清空。
 * 避免了路由参数 URL 编码问题（路径含 `/` 与空格）。
 */
object TerminalNavigationData {

    /** 待消费的终端工作目录，null 表示使用默认目录 */
    val pendingCwd = MutableStateFlow<String?>(null)

    /**
     * 设置待消费的终端工作目录。
     * @param cwd 工作目录路径，null 表示使用默认目录
     */
    fun setCwd(cwd: String?) {
        pendingCwd.value = cwd
    }

    /**
     * 消费并清空 pendingCwd。
     * @return 之前设置的 cwd，或 null
     */
    fun consumeCwd(): String? {
        val cwd = pendingCwd.value
        pendingCwd.value = null
        return cwd
    }
}
