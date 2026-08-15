/**
 * 文件：TabStateManager.kt
 * 功能：标签页编辑器状态管理器
 * 主要类/接口：TabStateManager、RestoredPosition、SwitchResult
 * 模块依赖：
 *   - viewmodel/EditorSavedState：编辑器保存状态
 *   - javax.inject：Hilt 依赖注入
 */
package com.draftpeek.feature.editor.tabs

import com.draftpeek.core.common.model.TabId
import com.draftpeek.feature.editor.viewmodel.EditorSavedState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从会话持久化恢复的光标和滚动位置。
 *
 * 用于在会话恢复后从磁盘加载标签页时恢复位置，
 * 在完整的 [EditorSavedState]（包含内容）可用之前使用。
 *
 * @property cursorLine 光标行号（1-based）
 * @property cursorColumn 光标列号（1-based）
 * @property scrollX 水平滚动位置
 * @property scrollY 垂直滚动位置
 */
data class RestoredPosition(
    val cursorLine: Int,
    val cursorColumn: Int,
    val scrollX: Int,
    val scrollY: Int,
)

/**
 * 管理每个标签页的编辑器状态（光标、滚动、内容快照）。
 *
 * 此类集中管理之前分散在 [EditorViewModel] 和 [TabManager] 之间的状态，
 * 确保与标签页关联的编辑器状态有单一可信源。
 */
@Singleton
class TabStateManager @Inject constructor(
    private val tabManager: TabManager,
) {

    private val tabStates = ConcurrentHashMap<TabId, EditorSavedState>()
    private val restoredPositions = ConcurrentHashMap<TabId, RestoredPosition>()

    /**
     * 保存给定 [tabId] 的编辑器状态。
     *
     * @param tabId 标签页 ID
     * @param state 要保存的编辑器状态
     */
    fun saveTabState(tabId: TabId, state: EditorSavedState) {
        tabStates[tabId] = state
    }

    /**
     * 获取给定 [tabId] 的已保存编辑器状态。
     *
     * @param tabId 标签页 ID
     * @return 已保存的编辑器状态，未保存时返回 null
     */
    fun getTabState(tabId: TabId): EditorSavedState? = tabStates[tabId]

    /**
     * 移除给定 [tabId] 的已保存状态。
     * 应在关闭标签页时调用。
     *
     * @param tabId 标签页 ID
     */
    fun removeTabState(tabId: TabId) {
        tabStates.remove(tabId)
        restoredPositions.remove(tabId)
    }

    /**
     * 为给定 [tabId] 保存恢复的光标/滚动位置。
     * 在会话恢复期间调用，用于持久化稍后从磁盘加载标签页时使用的位置。
     *
     * @param tabId 标签页 ID
     * @param position 恢复的位置
     */
    fun saveRestoredPosition(tabId: TabId, position: RestoredPosition) {
        restoredPositions[tabId] = position
    }

    /**
     * 获取给定 [tabId] 的恢复位置。
     *
     * @param tabId 标签页 ID
     * @return 恢复的位置，不可用时返回 null
     */
    fun getRestoredPosition(tabId: TabId): RestoredPosition? = restoredPositions[tabId]

    /**
     * 移除给定 [tabId] 的恢复位置。
     * 应在位置被消费后调用（即标签页内容已加载且位置已应用后）。
     *
     * @param tabId 标签页 ID
     */
    fun removeRestoredPosition(tabId: TabId) {
        restoredPositions.remove(tabId)
    }

    /**
     * 保存当前标签页状态（如果有）并切换到目标标签页。
     *
     * @param tabId 要激活的标签页
     * @return 描述调用方接下来应做什么的 [SwitchResult]
     */
    fun switchTab(tabId: TabId): SwitchResult {
        val currentTab = tabManager.getActiveTab()
        tabManager.setActiveTab(tabId)

        val saved = tabStates[tabId]
        return if (saved != null) {
            SwitchResult.RestoreState(saved)
        } else {
            val targetTab = tabManager.tabs.value.find { it.id == tabId }
            if (targetTab != null) {
                val pos = restoredPositions[tabId]
                SwitchResult.LoadFromDisk(targetTab.uri, pos)
            } else {
                SwitchResult.Noop
            }
        }
    }

    /**
     * 关闭具有给定 [tabId] 的标签页并清理其已保存状态。
     *
     * @param tabId 标签页 ID
     * @return 关闭后仍有标签页打开返回 true
     */
    fun closeTab(tabId: TabId): Boolean {
        removeTabState(tabId)
        return tabManager.closeTab(tabId)
    }

    /**
     * 标签页切换操作结果密封类。
     */
    sealed class SwitchResult {
        /** 标签页有应恢复的已保存状态 */
        data class RestoreState(val state: EditorSavedState) : SwitchResult()

        /** 标签页没有已保存状态；从磁盘加载其内容 */
        data class LoadFromDisk(
            val uri: String,
            val restoredPosition: RestoredPosition? = null,
        ) : SwitchResult()

        /** 无需操作（标签页未找到） */
        data object Noop : SwitchResult()
    }
}
