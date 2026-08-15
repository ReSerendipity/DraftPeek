/**
 * DataStore 偏好设置提供模块。
 *
 * 使用 Android Jetpack DataStore 存储 UI 相关的键值对偏好设置，
 * 通过 Context 扩展属性提供便捷访问。DataStore 基于 Protocol Buffers 或
 * Preferences 实现，提供一致的异步数据存储，替代传统 SharedPreferences。
 *
 * 当前仅提供 UI 偏好设置 DataStore（ui_prefs）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * UI 偏好设置 DataStore 实例。
 *
 * 通过 Context 扩展属性访问，文件名为 "ui_prefs"。
 * 用于存储界面相关的持久化设置，如主题、字体大小、视图模式等。
 *
 * 用法：
 * ```
 * context.uiDataStore.data.collect { prefs -> ... }
 * ```
 */
val Context.uiDataStore by preferencesDataStore(name = "ui_prefs")
