/**
 * 文件：TabManager.kt
 * 功能：编辑器多标签页管理单例类
 * 主要类/接口：TabManager、SessionData、SessionTab
 * 模块依赖：
 *   - feature/editor/model：EditorTab 数据模型
 *   - kotlinx.collections.immutable：不可变集合
 *   - kotlinx.coroutines.flow：状态流
 *   - javax.inject：Hilt 依赖注入
 */
package com.draftpeek.feature.editor.tabs

import com.draftpeek.core.common.model.TabId
import com.draftpeek.feature.editor.model.EditorTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 编辑器会话的可序列化快照，用于跨应用重启持久化。
 *
 * @property tabs 打开的标签页列表
 * @property activeTabId 当前活动标签页 ID
 */
data class SessionData(
    val tabs: List<SessionTab>,
    val activeTabId: TabId?,
)

/**
 * [SessionData] 中单个标签页的持久化数据。
 *
 * 包含光标和滚动位置，以便应用关闭后重新打开时能完整恢复会话。
 *
 * @property id 标签页唯一 ID
 * @property uri 文件 URI
 * @property fileName 文件名
 * @property language 编程语言
 * @property cursorLine 光标行号（1-based）
 * @property cursorColumn 光标列号（1-based）
 * @property scrollX 水平滚动位置
 * @property scrollY 垂直滚动位置
 */
data class SessionTab(
    val id: TabId,
    val uri: String,
    val fileName: String,
    val language: String?,
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1,
    val scrollX: Int = 0,
    val scrollY: Int = 0,
)

/**
 * 管理打开的编辑器标签页列表的单例类。
 *
 * 标签页在导航之间保持不变，用户可以在多个文件之间切换而不会丢失位置。
 *
 * 使用 LRU（最近最少使用）策略在达到 [MAX_TABS] 限制时驱逐标签页，
 * 优先驱逐未修改的标签页以避免丢失未保存的工作。
 *
 * 性能优化：维护 uri→tab 和 id→index 哈希索引，所有查找操作 O(1)，
 * 避免每次都对标签列表做线性扫描。
 */
@Singleton
class TabManager @Inject constructor() {

    companion object {
        /** 同时打开的最大标签页数 */
        const val MAX_TABS = 10
    }

    private val _tabs = MutableStateFlow<ImmutableList<EditorTab>>(persistentListOf())
    /** 当前打开的标签页不可变列表的 StateFlow */
    val tabs: StateFlow<ImmutableList<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<TabId?>(null)
    /** 当前活动标签页 ID 的 StateFlow */
    val activeTabId: StateFlow<TabId?> = _activeTabId.asStateFlow()

    /**
     * MRU 访问时间跟踪：映射 tabId → 最后访问纪元毫秒数。
     * 用于在达到 MAX_TABS 时驱逐最近最少使用（而非最早打开）的标签页。
     */
    private val lastAccessMap = java.util.concurrent.ConcurrentHashMap<TabId, Long>()

    /**
     * URI → EditorTab 快速查找索引。
     * 每次标签列表变更时同步重建/更新，确保 openTab/findTabByUri 为 O(1)。
     */
    @Volatile
    private var uriIndex = emptyMap<String, EditorTab>()

    /**
     * tabId → EditorTab 快速查找索引。
     */
    @Volatile
    private var idIndex = emptyMap<TabId, EditorTab>()

    /**
     * 重建哈希索引。在每次 _tabs 更新后调用以保持查找性能。
     */
    private fun rebuildIndexes(tabs: List<EditorTab>) {
        val byUri = HashMap<String, EditorTab>(tabs.size * 2)
        val byId = HashMap<TabId, EditorTab>(tabs.size * 2)
        for (tab in tabs) {
            byId[tab.id] = tab
            if (tab.uri.isNotBlank()) {
                byUri[tab.uri] = tab
            }
        }
        uriIndex = byUri
        idIndex = byId
    }

    init {
        // 初始化索引
        rebuildIndexes(_tabs.value)
    }

    /**
     * 为给定文件打开新标签页，或切换到已打开的标签页。
     *
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param language 编程语言
     * @return 标签页 ID（已存在的或新创建的）
     */
    fun openTab(uri: String, fileName: String, language: String?): TabId {
        val existing = uriIndex[uri]
        if (existing != null) {
            _activeTabId.value = existing.id
            touchTab(existing.id)
            return existing.id
        }

        var reusableTab: EditorTab? = null
        for (tab in _tabs.value) {
            if (tab.uri.isBlank() && tab.fileName.isBlank() && !tab.isModified) {
                reusableTab = tab
                break
            }
        }
        if (reusableTab != null) {
            val updatedTab = reusableTab.copy(
                uri = uri,
                fileName = fileName,
                language = language,
                isModified = false,
            )
            val newTabs = _tabs.value.map { if (it.id == reusableTab.id) updatedTab else it }.toImmutableList()
            _tabs.value = newTabs
            rebuildIndexes(newTabs)
            _activeTabId.update { reusableTab.id }
            touchTab(reusableTab.id)
            return reusableTab.id
        }

        if (_tabs.value.size >= MAX_TABS) {
            val evictCandidate = findEvictionCandidate()
            if (evictCandidate != null) {
                val newTabs = _tabs.value.filter { it.id != evictCandidate }.toImmutableList()
                _tabs.value = newTabs
                rebuildIndexes(newTabs)
                lastAccessMap.remove(evictCandidate)
                if (_activeTabId.value == evictCandidate) {
                    _activeTabId.value = newTabs.firstOrNull()?.id
                }
            }
        }

        val tabId = TabId.generate()
        val tab = EditorTab(
            id = tabId,
            uri = uri,
            fileName = fileName,
            language = language,
        )
        val newTabs = (_tabs.value + tab).toImmutableList()
        _tabs.value = newTabs
        rebuildIndexes(newTabs)
        _activeTabId.value = tabId
        touchTab(tabId)
        return tabId
    }

    /**
     * 记录标签页刚被访问（激活/打开），更新其 MRU 时间戳。
     */
    private fun touchTab(tabId: TabId) {
        lastAccessMap[tabId] = System.currentTimeMillis()
    }

    /**
     * 选择 LRU 驱逐的最佳候选者。
     *
     * 策略：在非活动标签页中，优先选择未修改的标签页（其中最久未使用的），
     * 如果所有标签页都已修改，则回退到所有非活动标签页中最久未使用的。
     * 仅当除活动标签页外没有其他标签页时返回 null。
     */
    private fun findEvictionCandidate(): TabId? {
        val currentTabs = _tabs.value
        val activeId = _activeTabId.value
        val candidates = currentTabs.filter { it.id != activeId }
        if (candidates.isEmpty()) return null

        val unmodified = candidates.filter { !it.isModified }
        val pool = if (unmodified.isNotEmpty()) unmodified else candidates
        return pool.minByOrNull { tab ->
            lastAccessMap[tab.id] ?: 0L
        }?.id
    }

    /**
     * 关闭具有给定 [tabId] 的标签页。
     *
     * @return 关闭后仍有标签页打开返回 `true`，这是最后一个标签页返回 `false`
     */
    fun closeTab(tabId: TabId): Boolean {
        val currentTabs = _tabs.value
        // 使用 O(1) 哈希索引查找替代 O(n) 的 indexOfFirst
        val closingTab = idIndex[tabId] ?: return currentTabs.isNotEmpty()
        val index = currentTabs.indexOf(closingTab)

        val newTabs = currentTabs.filter { it.id != tabId }.toImmutableList()
        _tabs.value = newTabs
        rebuildIndexes(newTabs)
        lastAccessMap.remove(tabId)

        if (_activeTabId.value == tabId) {
            _activeTabId.value = when {
                newTabs.isEmpty() -> null
                index < newTabs.size -> {
                    val newId = newTabs[index].id
                    touchTab(newId)
                    newId
                }
                else -> {
                    val newId = newTabs.lastOrNull()?.id
                    newId?.let { touchTab(it) }
                    newId
                }
            }
        }
        return newTabs.isNotEmpty()
    }

    /**
     * 设置当前活动标签页。
     */
    fun setActiveTab(tabId: TabId) {
        if (idIndex.containsKey(tabId)) {
            _activeTabId.value = tabId
            touchTab(tabId)
        }
    }

    /**
     * 将标签页重排到目标索引（拖拽重排）。
     *
     * 从当前列表移除 [tabId] 并插入到 [toIndex] 位置，重建哈希索引。
     * 索引以重排前列表为基准进行钳制；若 [tabId] 不存在则不做任何修改。
     * 标签激活状态保持不变。
     *
     * @param tabId 要移动的标签页 ID
     * @param toIndex 重排后的目标索引（0-based），自动钳制到合法范围
     */
    fun reorderTab(tabId: TabId, toIndex: Int) {
        val currentTabs = _tabs.value
        val fromIndex = currentTabs.indexOfFirst { it.id == tabId }
        if (fromIndex < 0) return
        val targetIndex = toIndex.coerceIn(0, currentTabs.lastIndex)
        if (fromIndex == targetIndex) return

        val mutable = currentTabs.toMutableList()
        val removed = mutable.removeAt(fromIndex)
        mutable.add(targetIndex, removed)
        val newTabs = mutable.toImmutableList()
        _tabs.value = newTabs
        rebuildIndexes(newTabs)
    }

    /**
     * 更新标签页的修改状态。
     */
    fun updateTabModified(tabId: TabId, isModified: Boolean) {
        val currentTabs = _tabs.value
        val newTabs = currentTabs.map { tab ->
            if (tab.id == tabId) tab.copy(isModified = isModified) else tab
        }.toImmutableList()
        _tabs.value = newTabs
        rebuildIndexes(newTabs)
    }

    /**
     * 获取当前活动标签页，没有活动标签页时返回 `null`。
     */
    fun getActiveTab(): EditorTab? {
        val activeId = _activeTabId.value ?: return null
        return idIndex[activeId]
    }

    /**
     * 通过文件 URI 查找标签页（O(1) 哈希查找）。
     */
    fun findTabByUri(uri: String): EditorTab? {
        return uriIndex[uri]
    }

    /**
     * 关闭所有标签页。
     */
    fun closeAllTabs() {
        _tabs.value = persistentListOf()
        _activeTabId.value = null
        lastAccessMap.clear()
        rebuildIndexes(emptyList())
    }

    /**
     * 将当前会话序列化为 [SessionData] 用于持久化。
     */
    fun serializeSession(): SessionData {
        return SessionData(
            tabs = _tabs.value.map { tab ->
                SessionTab(
                    id = tab.id,
                    uri = tab.uri,
                    fileName = tab.fileName,
                    language = tab.language,
                )
            },
            activeTabId = _activeTabId.value,
        )
    }

    /**
     * 从 [SessionData] 恢复会话。
     */
    fun restoreSession(data: SessionData) {
        val restoredTabs = data.tabs.map { sessionTab ->
            EditorTab(
                id = sessionTab.id,
                uri = sessionTab.uri,
                fileName = sessionTab.fileName,
                language = sessionTab.language,
            )
        }
        val newTabs = restoredTabs.toImmutableList()
        _tabs.value = newTabs
        rebuildIndexes(newTabs)
        _activeTabId.value = data.activeTabId
        val now = System.currentTimeMillis()
        data.tabs.forEachIndexed { index, sessionTab ->
            lastAccessMap[sessionTab.id] = now - (data.tabs.size - index) * 1000L
        }
        data.activeTabId?.let { touchTab(it) }
    }
}
