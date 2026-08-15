/**
 * 撰码轻览 (DraftPeek) 设置数据仓库接口。
 *
 * 本文件定义了设置持久化层的抽象接口，遵循 Repository 设计模式。
 * 所有设置的读写操作都通过此接口暴露，底层使用 Jetpack DataStore (Preferences) 实现，
 * 确保数据在应用重启后持久保存，并通过 Kotlin Flow 提供响应式数据更新。
 *
 * 数据持久化说明：
 * - 存储介质：Jetpack DataStore (Preferences)，基于 Protocol Buffers 的键值对存储
 * - 文件位置：应用内部存储的 `datastore/settings.preferences_pb`
 * - 线程安全：所有写入操作通过 DataStore.edit{} 在 IO 线程执行，保证原子性
 * - 响应式：读取操作返回 Flow<T>，数据变化时自动通知订阅者
 * - 默认值：首次读取时若键不存在，返回 EditorSettings 中定义的默认值
 *
 * 模块：feature/settings
 * 实现类：[SettingsRepositoryImpl]
 */
package com.draftpeek.feature.settings.repository

import com.draftpeek.core.designsystem.theme.ColorBlindMode
import com.draftpeek.core.designsystem.theme.RainbowColor
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.model.EditorSettings
import kotlinx.coroutines.flow.Flow

/**
 * 设置仓库接口，定义所有配置项的读写契约。
 *
 * 接口遵循单一职责原则，按功能模块分组方法：
 * 1. 编辑器基础设置（字体、主题、行号等）
 * 2. 编辑器行为设置（自动缩进、高亮、缩进参考线等）
 * 3. 文件相关设置（编码、自动保存）
 * 4. 字体设置
 * 5. 语言设置
 * 6. 固定文件（pinned files）
 * 7. 活动统计颜色
 * 8. 用户信息
 * 9. 无障碍功能设置
 * 10. 网络设置（GitHub 镜像）
 * 11. 编辑器主题
 * 12. Markdown 预览设置
 * 13. 目录排序偏好
 */
interface SettingsRepository {
    /**
     * 完整设置数据流，Combined Flow 一次性发射所有配置项。
     * UI 层通过此 Flow 一次性获取全部设置，避免多次订阅。
     */
    val settings: Flow<EditorSettings>

    /** 更新编辑器字体大小 */
    suspend fun updateFontSize(size: Int)

    /** 更新应用主题模式 */
    suspend fun updateTheme(theme: AppTheme)

    /** 更新自动换行开关状态 */
    suspend fun updateLineWrapping(enabled: Boolean)

    /** 更新行号显示开关状态 */
    suspend fun updateShowLineNumbers(enabled: Boolean)

    /** 获取 Tab 宽度设置的响应式流 */
    fun getTabWidth(): Flow<Int>

    /** 设置 Tab 宽度 */
    suspend fun setTabWidth(width: Int)

    /** 获取自动缩进设置的响应式流 */
    fun getAutoIndent(): Flow<Boolean>

    /** 设置自动缩进开关 */
    suspend fun setAutoIndent(enabled: Boolean)

    /** 获取当前行高亮设置的响应式流 */
    fun getHighlightCurrentLine(): Flow<Boolean>

    /** 设置当前行高亮开关 */
    suspend fun setHighlightCurrentLine(enabled: Boolean)

    /** 获取缩进参考线设置的响应式流 */
    fun getShowIndentGuides(): Flow<Boolean>

    /** 设置缩进参考线开关 */
    suspend fun setShowIndentGuides(enabled: Boolean)

    /** 获取默认文件编码的响应式流 */
    fun getDefaultEncoding(): Flow<String>

    /** 设置默认文件编码 */
    suspend fun setDefaultEncoding(encoding: String)

    /** 获取自动保存开关的响应式流 */
    fun getAutoSave(): Flow<Boolean>

    /** 设置自动保存开关 */
    suspend fun setAutoSave(enabled: Boolean)

    /** 获取自动保存间隔时间的响应式流 */
    fun getAutoSaveIntervalMs(): Flow<Long>

    /** 设置自动保存间隔时间（毫秒） */
    suspend fun setAutoSaveIntervalMs(intervalMs: Long)

    /** @deprecated 已废弃，使用 getCodeFontFamilyId/getUiFontFamilyId 替代 */
    fun getFontFamily(): Flow<String>

    /** @deprecated 已废弃 */
    suspend fun setFontFamily(family: String)

    /** 获取代码字体 ID 的响应式流 */
    fun getCodeFontFamilyId(): Flow<String>

    /** 设置代码字体 ID */
    suspend fun setCodeFontFamilyId(id: String)

    /** 获取 UI 字体 ID 的响应式流 */
    fun getUiFontFamilyId(): Flow<String>

    /** 设置 UI 字体 ID */
    suspend fun setUiFontFamilyId(id: String)

    /** 获取应用语言设置的响应式流 */
    fun getLanguage(): Flow<AppLanguage>

    /** 设置应用语言 */
    suspend fun setLanguage(language: AppLanguage)

    /** 获取固定文件 URI 集合的响应式流 */
    fun getPinnedFiles(): Flow<Set<String>>

    /** 设置固定文件 URI 集合 */
    suspend fun setPinnedFiles(pinnedUris: Set<String>)

    /** 获取活动日历颜色主题的响应式流 */
    fun getActivityColor(): Flow<RainbowColor>

    /** 设置活动日历颜色主题 */
    suspend fun setActivityColor(color: RainbowColor)

    /** 更新用户显示名称 */
    suspend fun updateUserName(userName: String)

    /** 更新用户头像 URI */
    suspend fun updateUserAvatar(avatarUri: String)

    // ===== 首页显示设置 =====

    /** 获取首页"最近打开"显示数量的响应式流 */
    fun getRecentFilesLimit(): Flow<Int>

    /** 设置首页"最近打开"显示数量 */
    suspend fun setRecentFilesLimit(limit: Int)

    // ===== 无障碍功能设置 =====

    /** 获取色盲模式设置的响应式流 */
    fun getColorBlindMode(): Flow<ColorBlindMode>

    /** 设置色盲模式 */
    suspend fun setColorBlindMode(mode: ColorBlindMode)

    /** 获取高对比度模式开关的响应式流 */
    fun getHighContrastMode(): Flow<Boolean>

    /** 设置高对比度模式开关 */
    suspend fun setHighContrastMode(enabled: Boolean)

    /** 获取 UI 文字缩放倍数的响应式流 */
    fun getTextScale(): Flow<Float>

    /** 设置 UI 文字缩放倍数 */
    suspend fun setTextScale(scale: Float)

    /** 获取屏幕阅读器优化开关的响应式流 */
    fun getScreenReaderOptimized(): Flow<Boolean>

    /** 设置屏幕阅读器优化开关 */
    suspend fun setScreenReaderOptimized(enabled: Boolean)

    /** 获取振动反馈开关的响应式流 */
    fun getVibrationFeedback(): Flow<Boolean>

    /** 设置振动反馈开关 */
    suspend fun setVibrationFeedback(enabled: Boolean)

    /** 获取非色彩标识开关的响应式流 */
    fun getNonColorIndicators(): Flow<Boolean>

    /** 设置非色彩标识开关 */
    suspend fun setNonColorIndicators(enabled: Boolean)

    /** 获取粘性滚动开关的响应式流 */
    fun getStickyScroll(): Flow<Boolean>

    /** 设置粘性滚动开关 */
    suspend fun setStickyScroll(enabled: Boolean)

    /** 获取缩略图（minimap）显示开关的响应式流 */
    fun getShowMinimap(): Flow<Boolean>

    /** 设置缩略图显示开关 */
    suspend fun setShowMinimap(enabled: Boolean)

    /** 获取自动配对补全开关的响应式流 */
    fun getAutoPairCompletion(): Flow<Boolean>

    /** 设置自动配对补全开关 */
    suspend fun setAutoPairCompletion(enabled: Boolean)

    // ===== 网络设置 =====

    /** 获取 GitHub 镜像 URL 的响应式流 */
    fun getGithubMirrorUrl(): Flow<String>

    /** 设置 GitHub 镜像 URL */
    suspend fun setGithubMirrorUrl(url: String)

    // ===== 编辑器主题设置 =====

    /** 获取编辑器代码高亮主题 ID 的响应式流 */
    fun getEditorThemeId(): Flow<String>

    /** 设置编辑器代码高亮主题 ID */
    suspend fun setEditorThemeId(themeId: String)

    // ===== Markdown 预览设置 =====

    /** 获取 Markdown 预览主题名称的响应式流 */
    fun getMarkdownThemeName(): Flow<String>

    /** 设置 Markdown 预览主题名称 */
    suspend fun setMarkdownThemeName(themeName: String)

    /** 获取自定义 Markdown 预览 CSS 的响应式流 */
    fun getCustomMarkdownCss(): Flow<String>

    /** 设置自定义 Markdown 预览 CSS */
    suspend fun setCustomMarkdownCss(css: String)

    // ===== 目录排序偏好设置 =====

    /** 获取指定目录的排序选项偏好，若无保存的偏好则返回 null */
    fun getDirectorySortOption(directoryUri: String): Flow<String?>

    /** 保存指定目录的排序选项偏好 */
    suspend fun setDirectorySortOption(directoryUri: String, sortOption: String)

    // ===== 首页文件列表自定义排序 =====

    /** 获取收藏（Pinned）文件的自定义排序 URI 列表 */
    fun getPinnedOrder(): Flow<List<String>>

    /** 保存收藏（Pinned）文件的自定义排序 URI 列表 */
    suspend fun setPinnedOrder(order: List<String>)

    /** 获取最近文件的自定义排序 URI 列表 */
    fun getRecentOrder(): Flow<List<String>>

    /** 保存最近文件的自定义排序 URI 列表 */
    suspend fun setRecentOrder(order: List<String>)

    /** 获取内部存储文件自定义排序的 URI 列表 */
    fun getInternalFilesOrder(): Flow<List<String>>

    /** 保存内部存储文件的自定义排序 URI 列表 */
    suspend fun setInternalFilesOrder(order: List<String>)

    /** 获取书签文件的自定义排序 URI 列表 */
    fun getBookmarkOrder(): Flow<List<String>>

    /** 保存书签文件的自定义排序 URI 列表 */
    suspend fun setBookmarkOrder(order: List<String>)
}
