/**
 * 文件：SessionManager.kt
 * 功能：编辑器会话持久化管理器
 * 主要类/接口：SessionManager
 * 模块依赖：
 *   - android.content：Context
 *   - androidx.datastore：DataStore 偏好设置
 *   - kotlinx.coroutines：协程
 *   - org.json：JSON 序列化
 */
package com.draftpeek.feature.editor.tabs

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.draftpeek.core.common.model.TabId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "editor_session")
private val KEY_SESSION_JSON = stringPreferencesKey("session_json")

/**
 * 使用 DataStore 管理编辑器会话持久化（打开的标签页、活动标签页 ID、光标/滚动位置）。
 * 在标签页更改时保存，在应用启动时恢复。
 *
 * 会话以 JSON 字符串存储，结构如下：
 * ```json
 * {
 *   "activeTabId": "uuid-xxx",
 *   "tabs": [
 *     {
 *       "id": "uuid-1",
 *       "uri": "content://...",
 *       "fileName": "MainActivity.kt",
 *       "language": "kotlin",
 *       "cursorLine": 42,
 *       "cursorColumn": 10,
 *       "scrollX": 0,
 *       "scrollY": 350
 *     }
 *   ]
 * }
 * ```
 */
@Singleton
class SessionManager @Inject constructor(
    private val tabManager: TabManager,
    private val tabStateManager: TabStateManager,
) {

    companion object {
        private const val TAG = "SessionManager"

        private const val KEY_ACTIVE_TAB_ID = "activeTabId"
        private const val KEY_TABS = "tabs"
        private const val KEY_ID = "id"
        private const val KEY_URI = "uri"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_CURSOR_LINE = "cursorLine"
        private const val KEY_CURSOR_COLUMN = "cursorColumn"
        private const val KEY_SCROLL_X = "scrollX"
        private const val KEY_SCROLL_Y = "scrollY"
    }

    private var scope: CoroutineScope? = null
    private var context: Context? = null
    private var saveJob: Job? = null

    private val _isRestored = MutableStateFlow(false)
    /** 会话是否已恢复的 StateFlow */
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

    /**
     * 使用应用上下文和协程作用域初始化。
     * 必须在应用启动期间调用一次（如在 EditorViewModel init 中）。
     *
     * @param appContext 应用上下文
     * @param coroutineScope 协程作用域
     */
    fun initialize(appContext: Context, coroutineScope: CoroutineScope) {
        context = appContext
        scope = coroutineScope
    }

    /**
     * 清理 SessionManager 资源，取消待保存任务并清除引用。
     * 应在 ViewModel.onCleared() 或应用终止时调用，防止协程泄漏。
     */
    fun cleanup() {
        saveJob?.cancel()
        saveJob = null
        scope = null
        context = null
        Log.d(TAG, "SessionManager resources cleaned up")
    }

    /**
     * 从 DataStore 恢复之前保存的会话。
     * 应在应用启动时调用一次，在加载默认文件之前。
     *
     * @return 恢复的 [SessionData]，未保存会话时返回 null
     */
    suspend fun restoreSession(): SessionData? {
        val ctx = context ?: return null
        try {
            val prefs = ctx.sessionDataStore.data.first()
            val json = prefs[KEY_SESSION_JSON] ?: return null
            val data = deserializeSessionData(json)
            if (data != null && data.tabs.isNotEmpty()) {
                tabManager.restoreSession(data)
                data.tabs.forEach { tab ->
                    tabStateManager.saveRestoredPosition(
                        tabId = tab.id,
                        position = RestoredPosition(
                            cursorLine = tab.cursorLine,
                            cursorColumn = tab.cursorColumn,
                            scrollX = tab.scrollX,
                            scrollY = tab.scrollY,
                        ),
                    )
                }
                Log.i(TAG, "Restored session: ${data.tabs.size} tabs, active=${data.activeTabId}")
            }
            _isRestored.value = true
            return data
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore session", e)
            _isRestored.value = true
            return null
        }
    }

    /**
     * 将当前会话保存到 DataStore。
     * 防抖——快速连续调用合并为一次。
     *
     * 光标和滚动位置从 [TabStateManager] 为每个标签页拉取。
     * 调用方（通常是 EditorViewModel）必须确保在调用此方法之前
     * 将活动标签页的状态保存到 [TabStateManager]，
     * 否则活动标签页的光标/滚动不会包含在持久化会话中。
     */
    fun saveSession() {
        val scope = scope ?: return
        val ctx = context ?: return
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            try {
                val baseData = tabManager.serializeSession()
                val enrichedData = baseData.copy(
                    tabs = baseData.tabs.map { tab ->
                        val state = tabStateManager.getTabState(tab.id)
                        if (state != null) {
                            tab.copy(
                                cursorLine = state.cursorLine,
                                cursorColumn = state.cursorColumn,
                                scrollX = state.scrollX,
                                scrollY = state.scrollY,
                            )
                        } else {
                            tab
                        }
                    },
                )
                val json = serializeSessionData(enrichedData)
                ctx.sessionDataStore.edit { prefs ->
                    prefs[KEY_SESSION_JSON] = json
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save session", e)
            }
        }
    }

    /**
     * 清除持久化的会话（如关闭所有标签页时）。
     */
    fun clearSession() {
        val scope = scope ?: return
        val ctx = context ?: return
        scope.launch(Dispatchers.IO) {
            try {
                ctx.sessionDataStore.edit { prefs ->
                    prefs.remove(KEY_SESSION_JSON)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear session", e)
            }
        }
    }

    /**
     * 将 [SessionData] 序列化为 JSON 字符串。
     *
     * @param data 会话数据
     * @return JSON 字符串
     */
    internal fun serializeSessionData(data: SessionData): String {
        val root = JSONObject()
        root.put(KEY_ACTIVE_TAB_ID, data.activeTabId?.value ?: "")
        val tabsArray = JSONArray()
        data.tabs.forEach { tab ->
            val tabObj = JSONObject().apply {
                put(KEY_ID, tab.id.value)
                put(KEY_URI, tab.uri)
                put(KEY_FILE_NAME, tab.fileName)
                put(KEY_LANGUAGE, tab.language ?: "")
                put(KEY_CURSOR_LINE, tab.cursorLine)
                put(KEY_CURSOR_COLUMN, tab.cursorColumn)
                put(KEY_SCROLL_X, tab.scrollX)
                put(KEY_SCROLL_Y, tab.scrollY)
            }
            tabsArray.put(tabObj)
        }
        root.put(KEY_TABS, tabsArray)
        return root.toString()
    }

    /**
     * 从 JSON 字符串反序列化 [SessionData]。
     * 处理新 JSON 格式并在解析错误时优雅回退。
     *
     * @param json JSON 字符串
     * @return 反序列化的 SessionData，失败时返回 null
     */
    internal fun deserializeSessionData(json: String): SessionData? {
        if (json.isBlank()) return null
        try {
            val root = JSONObject(json)
            val activeTabIdStr = root.optString(KEY_ACTIVE_TAB_ID, "").ifBlank { null }
            val activeTabId = activeTabIdStr?.let { TabId(it) }
            val tabsArray = root.optJSONArray(KEY_TABS) ?: return null
            val tabs = (0 until tabsArray.length()).mapNotNull { i ->
                val tabObj = tabsArray.getJSONObject(i)
                val idStr = tabObj.optString(KEY_ID, "")
                val uri = tabObj.optString(KEY_URI, "")
                val fileName = tabObj.optString(KEY_FILE_NAME, "")
                if (idStr.isBlank() || uri.isBlank()) return@mapNotNull null
                SessionTab(
                    id = TabId(idStr),
                    uri = uri,
                    fileName = fileName,
                    language = tabObj.optString(KEY_LANGUAGE, "").ifBlank { null },
                    cursorLine = tabObj.optInt(KEY_CURSOR_LINE, 1),
                    cursorColumn = tabObj.optInt(KEY_CURSOR_COLUMN, 1),
                    scrollX = tabObj.optInt(KEY_SCROLL_X, 0),
                    scrollY = tabObj.optInt(KEY_SCROLL_Y, 0),
                )
            }
            return SessionData(tabs = tabs, activeTabId = activeTabId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize session data", e)
            return null
        }
    }
}
