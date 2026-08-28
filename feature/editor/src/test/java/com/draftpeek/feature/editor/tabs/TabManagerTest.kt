package com.draftpeek.feature.editor.tabs

import com.draftpeek.core.common.model.TabId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TabManager")
class TabManagerTest {

    private lateinit var tabManager: TabManager

    @BeforeEach
    fun setUp() {
        tabManager = TabManager()
    }

    @Nested
    @DisplayName("openTab()")
    inner class OpenTabTests {

        @Test
        @DisplayName("首次打开标签页返回非空 ID 并设为活动")
        fun openFirstTab_setsActive() {
            val tabId = tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")

            assertNotNull(tabId)
            assertEquals(tabId, tabManager.activeTabId.value)
            assertEquals(1, tabManager.tabs.value.size)
        }

        @Test
        @DisplayName("打开相同 URI 返回已存在标签页 ID 而非创建新标签")
        fun openSameUri_returnsExistingTab() {
            val firstId = tabManager.openTab("content://file.kt", "file.kt", "kotlin")
            val secondId = tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            assertEquals(firstId, secondId)
            assertEquals(1, tabManager.tabs.value.size)
        }

        @Test
        @DisplayName("打开不同 URI 创建新标签页")
        fun openDifferentUri_createsNewTab() {
            tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")
            tabManager.openTab("content://file2.kt", "file2.kt", "kotlin")

            assertEquals(2, tabManager.tabs.value.size)
        }

        @Test
        @DisplayName("达到 MAX_TABS 限制时驱逐最久未使用的标签页")
        fun reachMaxTabs_evictsLRUTab() {
            // Open MAX_TABS tabs
            for (i in 0 until TabManager.MAX_TABS) {
                tabManager.openTab("content://file$i.kt", "file$i.kt", "kotlin")
            }
            assertEquals(TabManager.MAX_TABS, tabManager.tabs.value.size)

            // Open one more — should evict the LRU (file0)
            tabManager.openTab("content://new.kt", "new.kt", "kotlin")
            assertEquals(TabManager.MAX_TABS, tabManager.tabs.value.size)
            assertNull(tabManager.findTabByUri("content://file0.kt"))
            assertNotNull(tabManager.findTabByUri("content://new.kt"))
        }
    }

    @Nested
    @DisplayName("closeTab()")
    inner class CloseTabTests {

        @Test
        @DisplayName("关闭活动标签页后自动切换到相邻标签页")
        fun closeActiveTab_switchesToAdjacent() {
            val id1 = tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")
            val id2 = tabManager.openTab("content://file2.kt", "file2.kt", "kotlin")

            // Close the active tab (id2)
            val hasRemaining = tabManager.closeTab(id2)

            assertTrue(hasRemaining)
            assertEquals(1, tabManager.tabs.value.size)
            assertEquals(id1, tabManager.activeTabId.value)
        }

        @Test
        @DisplayName("关闭最后一个标签页返回 false 且活动 ID 为 null")
        fun closeLastTab_returnsFalse() {
            val id = tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            val hasRemaining = tabManager.closeTab(id)

            assertFalse(hasRemaining)
            assertEquals(0, tabManager.tabs.value.size)
            assertNull(tabManager.activeTabId.value)
        }

        @Test
        @DisplayName("关闭不存在的标签页 ID 不影响现有标签")
        fun closeNonExistentId_noEffect() {
            tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            val hasRemaining = tabManager.closeTab(TabId.generate())

            assertTrue(hasRemaining)
            assertEquals(1, tabManager.tabs.value.size)
        }
    }

    @Nested
    @DisplayName("reorderTab()")
    inner class ReorderTabTests {

        @Test
        @DisplayName("将标签页移到列表中间位置")
        fun moveTab_toMiddle_reorders() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")
            val id3 = tabManager.openTab("content://f3.kt", "f3.kt", "kotlin")

            // Move first tab to index 2 (end)
            tabManager.reorderTab(id1, 2)

            val ids = tabManager.tabs.value.map { it.id }
            assertEquals(listOf(id2, id3, id1), ids)
        }

        @Test
        @DisplayName("将中间标签移到开头")
        fun moveMiddleTab_toFirst_reorders() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")
            val id3 = tabManager.openTab("content://f3.kt", "f3.kt", "kotlin")

            tabManager.reorderTab(id2, 0)

            val ids = tabManager.tabs.value.map { it.id }
            assertEquals(listOf(id2, id1, id3), ids)
        }

        @Test
        @DisplayName("移到相同的索引时不改变顺序")
        fun moveToSameIndex_noChange() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")

            tabManager.reorderTab(id1, 0)

            val ids = tabManager.tabs.value.map { it.id }
            assertEquals(listOf(id1, id2), ids)
        }

        @Test
        @DisplayName("索引越界时自动钳制到合法范围")
        fun outOfBoundsIndex_clamped() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")
            val id3 = tabManager.openTab("content://f3.kt", "f3.kt", "kotlin")

            tabManager.reorderTab(id1, 99)

            val ids = tabManager.tabs.value.map { it.id }
            assertEquals(listOf(id2, id3, id1), ids)
        }

        @Test
        @DisplayName("移动不存在的标签 ID 时不改变任何状态")
        fun moveNonExistentId_noEffect() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")
            val before = tabManager.tabs.value

            tabManager.reorderTab(TabId.generate(), 0)

            assertEquals(before, tabManager.tabs.value)
            assertEquals(id2, tabManager.activeTabId.value)
        }

        @Test
        @DisplayName("重排后重建索引，可通过 URI 查找")
        fun reorder_rebuildsIndex() {
            val id1 = tabManager.openTab("content://f1.kt", "f1.kt", "kotlin")
            val id2 = tabManager.openTab("content://f2.kt", "f2.kt", "kotlin")

            tabManager.reorderTab(id2, 0)

            assertEquals(id2, tabManager.findTabByUri("content://f2.kt")?.id)
            assertEquals(id1, tabManager.findTabByUri("content://f1.kt")?.id)
        }
    }

    @Nested
    @DisplayName("setActiveTab()")
    inner class SetActiveTabTests {

        @Test
        @DisplayName("切换活动标签页更新 activeTabId")
        fun switchActiveTab_updatesId() {
            val id1 = tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")
            val id2 = tabManager.openTab("content://file2.kt", "file2.kt", "kotlin")

            tabManager.setActiveTab(id1)

            assertEquals(id1, tabManager.activeTabId.value)
        }

        @Test
        @DisplayName("设置不存在的 ID 不改变活动标签")
        fun setNonExistentId_noChange() {
            val id1 = tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")

            tabManager.setActiveTab(TabId.generate())

            assertEquals(id1, tabManager.activeTabId.value)
        }
    }

    @Nested
    @DisplayName("updateTabModified()")
    inner class UpdateModifiedTests {

        @Test
        @DisplayName("更新标签页修改状态")
        fun updateModified_changesFlag() {
            val id = tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            tabManager.updateTabModified(id, true)

            val tab = tabManager.tabs.value.find { it.id == id }
            assertTrue(tab?.isModified == true)
        }
    }

    @Nested
    @DisplayName("findTabByUri()")
    inner class FindByUriTests {

        @Test
        @DisplayName("通过 URI 查找已打开的标签页")
        fun findExistingTab() {
            tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            val tab = tabManager.findTabByUri("content://file.kt")
            assertNotNull(tab)
            assertEquals("file.kt", tab?.fileName)
        }

        @Test
        @DisplayName("查找不存在的 URI 返回 null")
        fun findNonExistent_returnsNull() {
            assertNull(tabManager.findTabByUri("content://nonexistent"))
        }
    }

    @Nested
    @DisplayName("getActiveTab()")
    inner class GetActiveTabTests {

        @Test
        @DisplayName("无标签页时返回 null")
        fun noTabs_returnsNull() {
            assertNull(tabManager.getActiveTab())
        }

        @Test
        @DisplayName("返回当前活动标签页")
        fun returnsActiveTab() {
            tabManager.openTab("content://file.kt", "file.kt", "kotlin")

            val active = tabManager.getActiveTab()
            assertNotNull(active)
            assertEquals("file.kt", active?.fileName)
        }
    }

    @Nested
    @DisplayName("closeAllTabs()")
    inner class CloseAllTests {

        @Test
        @DisplayName("关闭所有标签页后列表为空")
        fun closeAll_clearsList() {
            tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")
            tabManager.openTab("content://file2.kt", "file2.kt", "kotlin")

            tabManager.closeAllTabs()

            assertEquals(0, tabManager.tabs.value.size)
            assertNull(tabManager.activeTabId.value)
        }
    }

    @Nested
    @DisplayName("serializeSession() / restoreSession()")
    inner class SessionPersistenceTests {

        @Test
        @DisplayName("序列化后恢复保持标签页信息")
        fun serializeThenRestore_preservesTabs() {
            val id1 = tabManager.openTab("content://file1.kt", "file1.kt", "kotlin")
            val id2 = tabManager.openTab("content://file2.py", "file2.py", "python")

            val sessionData = tabManager.serializeSession()
            assertEquals(2, sessionData.tabs.size)
            assertEquals(id2, sessionData.activeTabId)

            // Create new TabManager and restore
            val restoredManager = TabManager()
            restoredManager.restoreSession(sessionData)

            assertEquals(2, restoredManager.tabs.value.size)
            assertEquals(id2, restoredManager.activeTabId.value)
            assertNotNull(restoredManager.findTabByUri("content://file1.kt"))
            assertNotNull(restoredManager.findTabByUri("content://file2.py"))
        }

        @Test
        @DisplayName("空会话序列化后恢复为空")
        fun emptySession_serializeAndRestore() {
            val sessionData = tabManager.serializeSession()
            assertEquals(0, sessionData.tabs.size)
            assertNull(sessionData.activeTabId)

            val restoredManager = TabManager()
            restoredManager.restoreSession(sessionData)
            assertEquals(0, restoredManager.tabs.value.size)
        }
    }
}
