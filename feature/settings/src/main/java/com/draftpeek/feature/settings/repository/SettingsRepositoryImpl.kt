/**
 * 撰码轻览 (DraftPeek) 设置数据仓库实现类。
 *
 * 本文件实现 [SettingsRepository] 接口，使用 Jetpack DataStore (Preferences) 进行设置持久化。
 *
 * 数据持久化实现细节：
 * - 存储机制：基于 DataStore Preferences，键值对存储在 Protocol Buffer 二进制文件中
 * - 原子写入：所有写入操作通过 dataStore.edit{} 事务执行，保证数据一致性
 * - 协程安全：suspend 函数自动切换到 IO 线程，不会阻塞主线程
 * - 数据迁移：处理 fontFamily 旧字段到 codeFontFamilyId/uiFontFamilyId 的迁移逻辑
 * - 范围校验：fontSize 限制在 10-24sp，textScale 限制在 0.85-1.5 倍，URL 自动去除末尾斜杠
 * - 异常容错：枚举值解析失败时回退到默认值（SYSTEM 主题、RED 颜色等）
 * - 动态键：目录排序偏好使用 "dir_sort_" + URI 作为动态键名，支持任意数量目录
 *
 * 模块：feature/settings
 * 依赖注入：通过 Hilt 注入，单例生命周期（@Singleton）
 */
package com.draftpeek.feature.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.draftpeek.core.designsystem.theme.ColorBlindMode
import com.draftpeek.core.designsystem.theme.RainbowColor
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.model.EditorSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 设置仓库实现类，负责将 SettingsRepository 接口调用映射到 DataStore 操作。
 *
 * @property dataStore DataStore Preferences 实例，由 Hilt 注入，单例
 */
class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    /**
     * DataStore 键名常量对象。
     *
     * 所有设置项对应的 Preferences Key 在此集中定义，避免硬编码字符串重复。
     * 使用 intPreferencesKey/booleanPreferencesKey 等类型安全的键创建函数。
     */
    private object Keys {
        /** 编辑器字体大小 */
        val FONT_SIZE = intPreferencesKey("font_size")

        /** 应用主题 */
        val THEME = stringPreferencesKey("theme")

        /** 自动换行开关 */
        val LINE_WRAPPING = booleanPreferencesKey("line_wrapping")

        /** 显示行号开关 */
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")

        /** Tab 宽度 */
        val TAB_WIDTH = intPreferencesKey("tab_width")

        /** 自动缩进开关 */
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")

        /** 高亮当前行开关 */
        val HIGHLIGHT_CURRENT_LINE = booleanPreferencesKey("highlight_current_line")

        /** 显示缩进参考线开关 */
        val SHOW_INDENT_GUIDES = booleanPreferencesKey("show_indent_guides")

        /** 默认文件编码 */
        val DEFAULT_ENCODING = stringPreferencesKey("default_encoding")

        /** 自动保存开关 */
        val AUTO_SAVE = booleanPreferencesKey("auto_save")

        /** 自动保存间隔 */
        val AUTO_SAVE_INTERVAL_MS = longPreferencesKey("auto_save_interval_ms")

        /** @deprecated 旧版字体设置，保留用于迁移 */
        val FONT_FAMILY = stringPreferencesKey("font_family")

        /** 代码字体 ID */
        val CODE_FONT_FAMILY_ID = stringPreferencesKey("code_font_family_id")

        /** UI 字体 ID */
        val UI_FONT_FAMILY_ID = stringPreferencesKey("ui_font_family_id")

        /** 应用语言 */
        val LANGUAGE = stringPreferencesKey("language")

        /** 固定文件集合 */
        val PINNED_FILES = stringSetPreferencesKey("pinned_files")

        /** 活动日历颜色 */
        val ACTIVITY_COLOR = stringPreferencesKey("activity_color")

        /** 用户名称 */
        val USER_NAME = stringPreferencesKey("user_name")

        /** 用户头像 URI */
        val USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")

        /** 首页最近打开显示数量 */
        val RECENT_FILES_LIMIT = intPreferencesKey("recent_files_limit")
        // 无障碍功能设置
        /** 色盲模式 */
        val COLOR_BLIND_MODE = stringPreferencesKey("color_blind_mode")

        /** 高对比度模式 */
        val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")

        /** UI 文字缩放 */
        val TEXT_SCALE = floatPreferencesKey("text_scale")

        /** 屏幕阅读器优化 */
        val SCREEN_READER_OPTIMIZED = booleanPreferencesKey("screen_reader_optimized")

        /** 振动反馈 */
        val VIBRATION_FEEDBACK = booleanPreferencesKey("vibration_feedback")

        /** 非色彩标识 */
        val NON_COLOR_INDICATORS = booleanPreferencesKey("non_color_indicators")

        /** 粘性滚动 */
        val STICKY_SCROLL = booleanPreferencesKey("sticky_scroll")

        /** 显示缩略图 */
        val SHOW_MINIMAP = booleanPreferencesKey("show_minimap")
        // 自动配对补全
        /** 自动配对补全 */
        val AUTO_PAIR_COMPLETION = booleanPreferencesKey("auto_pair_completion")

        /** GitHub 镜像 URL */
        val GITHUB_MIRROR_URL = stringPreferencesKey("github_mirror_url")

        /** 编辑器主题 ID */
        val EDITOR_THEME_ID = stringPreferencesKey("editor_theme_id")
        // Markdown 预览设置
        /** Markdown 预览主题名称 */
        val MARKDOWN_THEME_NAME = stringPreferencesKey("markdown_theme_name")

        /** 自定义 Markdown CSS */
        val CUSTOM_MARKDOWN_CSS = stringPreferencesKey("custom_markdown_css")
        // ===== 首页文件列表自定义排序 =====
        /** 收藏文件自定义排序（逗号分隔的 URI 字符串） */
        val PINNED_ORDER = stringPreferencesKey("pinned_order")

        /** 最近文件自定义排序（逗号分隔的 URI 字符串） */
        val RECENT_ORDER = stringPreferencesKey("recent_order")

        /** 内部存储文件自定义排序（逗号分隔的 URI 字符串） */
        val INTERNAL_FILES_ORDER = stringPreferencesKey("internal_files_order")

        /** 书签文件自定义排序（逗号分隔的 URI 字符串） */
        val BOOKMARK_ORDER = stringPreferencesKey("bookmark_order")
    }

    /** 将逗号分隔字符串解码为 URI 列表 */
    private fun decodeOrderList(raw: String?): List<String> = raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    /** 将 URI 列表编码为逗号分隔字符串 */
    private fun encodeOrderList(order: List<String>): String = order.joinToString(",")

    /** 目录排序偏好键前缀，实际键为 dir_sort_{directoryUri} */
    private val DIR_SORT_PREFIX = "dir_sort_"

    /**
     * 完整设置数据流，从 DataStore 读取所有键并映射为 EditorSettings 对象。
     *
     * 数据流转：
     * 1. dataStore.data 发射原始 Preferences
     * 2. map 转换读取每个键，缺失时使用默认值
     * 3. 处理旧版 fontFamily 字段迁移逻辑
     * 4. 枚举解析失败时安全回退到默认值
     * 5. 发射完整的 EditorSettings 不可变对象
     */
    override val settings: Flow<EditorSettings> = dataStore.data.map { prefs ->
        EditorSettings(
            fontSize = prefs[Keys.FONT_SIZE] ?: 16,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.SYSTEM.name)
            } catch (_: IllegalArgumentException) {
                AppTheme.SYSTEM
            },
            lineWrapping = prefs[Keys.LINE_WRAPPING] ?: false,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            tabWidth = prefs[Keys.TAB_WIDTH] ?: 4,
            autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
            highlightCurrentLine = prefs[Keys.HIGHLIGHT_CURRENT_LINE] ?: true,
            showIndentGuides = prefs[Keys.SHOW_INDENT_GUIDES] ?: false,
            defaultEncoding = prefs[Keys.DEFAULT_ENCODING] ?: "UTF-8",
            autoSave = prefs[Keys.AUTO_SAVE] ?: false,
            autoSaveIntervalMs = prefs[Keys.AUTO_SAVE_INTERVAL_MS] ?: 30000L,
            fontFamily = prefs[Keys.FONT_FAMILY] ?: "monospace",
            codeFontFamilyId = prefs[Keys.CODE_FONT_FAMILY_ID] ?: run {
                when (prefs[Keys.FONT_FAMILY]) {
                    "serif", "sans-serif" -> "jetbrains_mono"
                    else -> "jetbrains_mono"
                }
            },
            uiFontFamilyId = prefs[Keys.UI_FONT_FAMILY_ID] ?: run {
                when (prefs[Keys.FONT_FAMILY]) {
                    "serif" -> "system_serif"
                    "sans-serif" -> "system_sans"
                    else -> "inter"
                }
            },
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: ""),
            activityColor = RainbowColor.fromName(prefs[Keys.ACTIVITY_COLOR] ?: RainbowColor.RED.name),
            userName = prefs[Keys.USER_NAME] ?: "用户",
            userAvatarUri = prefs[Keys.USER_AVATAR_URI] ?: "",
            recentFilesLimit = prefs[Keys.RECENT_FILES_LIMIT] ?: 3,
            colorBlindMode = ColorBlindMode.fromName(prefs[Keys.COLOR_BLIND_MODE]),
            highContrastMode = prefs[Keys.HIGH_CONTRAST_MODE] ?: false,
            textScale = prefs[Keys.TEXT_SCALE] ?: 1.0f,
            screenReaderOptimized = prefs[Keys.SCREEN_READER_OPTIMIZED] ?: false,
            vibrationFeedback = prefs[Keys.VIBRATION_FEEDBACK] ?: false,
            nonColorIndicators = prefs[Keys.NON_COLOR_INDICATORS] ?: false,
            stickyScroll = prefs[Keys.STICKY_SCROLL] ?: true,
            showMinimap = prefs[Keys.SHOW_MINIMAP] ?: false,
            autoPairCompletion = prefs[Keys.AUTO_PAIR_COMPLETION] ?: true,
            githubMirrorUrl = prefs[Keys.GITHUB_MIRROR_URL] ?: "",
            editorThemeId = prefs[Keys.EDITOR_THEME_ID] ?: "",
            markdownThemeName = prefs[Keys.MARKDOWN_THEME_NAME] ?: "DEFAULT",
            customMarkdownCss = prefs[Keys.CUSTOM_MARKDOWN_CSS] ?: ""
        )
    }

    override suspend fun updateFontSize(size: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE] = size.coerceIn(10, 24)
        }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    override suspend fun updateLineWrapping(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.LINE_WRAPPING] = enabled
        }
    }

    override suspend fun updateShowLineNumbers(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_LINE_NUMBERS] = enabled
        }
    }

    override fun getTabWidth(): Flow<Int> = dataStore.data.map { it[Keys.TAB_WIDTH] ?: 4 }

    override suspend fun setTabWidth(width: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.TAB_WIDTH] = width
        }
    }

    override fun getAutoIndent(): Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_INDENT] ?: true }

    override suspend fun setAutoIndent(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_INDENT] = enabled
        }
    }

    override fun getHighlightCurrentLine(): Flow<Boolean> = dataStore.data.map {
        it[Keys.HIGHLIGHT_CURRENT_LINE] ?: true
    }

    override suspend fun setHighlightCurrentLine(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HIGHLIGHT_CURRENT_LINE] = enabled
        }
    }

    override fun getShowIndentGuides(): Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_INDENT_GUIDES] ?: false }

    override suspend fun setShowIndentGuides(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_INDENT_GUIDES] = enabled
        }
    }

    override fun getDefaultEncoding(): Flow<String> = dataStore.data.map { it[Keys.DEFAULT_ENCODING] ?: "UTF-8" }

    override suspend fun setDefaultEncoding(encoding: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_ENCODING] = encoding
        }
    }

    override fun getAutoSave(): Flow<Boolean> = dataStore.data.map { it[Keys.AUTO_SAVE] ?: false }

    override suspend fun setAutoSave(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE] = enabled
        }
    }

    override fun getAutoSaveIntervalMs(): Flow<Long> = dataStore.data.map { it[Keys.AUTO_SAVE_INTERVAL_MS] ?: 30000L }

    override suspend fun setAutoSaveIntervalMs(intervalMs: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE_INTERVAL_MS] = intervalMs
        }
    }

    override fun getFontFamily(): Flow<String> = dataStore.data.map { it[Keys.FONT_FAMILY] ?: "monospace" }

    override suspend fun setFontFamily(family: String) {
        dataStore.edit { prefs ->
            prefs[Keys.FONT_FAMILY] = family
        }
    }

    override fun getCodeFontFamilyId(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CODE_FONT_FAMILY_ID] ?: when (prefs[Keys.FONT_FAMILY]) {
            "serif", "sans-serif" -> "jetbrains_mono"
            else -> "jetbrains_mono"
        }
    }

    override suspend fun setCodeFontFamilyId(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CODE_FONT_FAMILY_ID] = id
            prefs[Keys.FONT_FAMILY] = "monospace"
        }
    }

    override fun getUiFontFamilyId(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.UI_FONT_FAMILY_ID] ?: when (prefs[Keys.FONT_FAMILY]) {
            "serif" -> "system_serif"
            "sans-serif" -> "system_sans"
            else -> "inter"
        }
    }

    override suspend fun setUiFontFamilyId(id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.UI_FONT_FAMILY_ID] = id
        }
    }

    override fun getLanguage(): Flow<AppLanguage> = dataStore.data.map {
        AppLanguage.fromCode(it[Keys.LANGUAGE] ?: "")
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    override fun getPinnedFiles(): Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.PINNED_FILES] ?: emptySet()
    }

    override suspend fun setPinnedFiles(pinnedUris: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.PINNED_FILES] = pinnedUris
        }
    }

    override fun getActivityColor(): Flow<RainbowColor> = dataStore.data.map { prefs ->
        RainbowColor.fromName(prefs[Keys.ACTIVITY_COLOR] ?: RainbowColor.RED.name)
    }

    override suspend fun setActivityColor(color: RainbowColor) {
        dataStore.edit { prefs ->
            prefs[Keys.ACTIVITY_COLOR] = color.name
        }
    }

    override suspend fun updateUserName(userName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = userName
        }
    }

    override suspend fun updateUserAvatar(avatarUri: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_AVATAR_URI] = avatarUri
        }
    }

    override fun getRecentFilesLimit(): Flow<Int> = dataStore.data.map { it[Keys.RECENT_FILES_LIMIT] ?: 3 }

    override suspend fun setRecentFilesLimit(limit: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.RECENT_FILES_LIMIT] = limit.coerceIn(0, 10)
        }
    }

    // ===== 无障碍功能设置实现 =====

    override fun getColorBlindMode(): Flow<ColorBlindMode> = dataStore.data.map { prefs ->
        ColorBlindMode.fromName(prefs[Keys.COLOR_BLIND_MODE])
    }

    override suspend fun setColorBlindMode(mode: ColorBlindMode) {
        dataStore.edit { prefs ->
            prefs[Keys.COLOR_BLIND_MODE] = mode.name
        }
    }

    override fun getHighContrastMode(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.HIGH_CONTRAST_MODE] ?: false
    }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.HIGH_CONTRAST_MODE] = enabled
        }
    }

    override fun getTextScale(): Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.TEXT_SCALE] ?: 1.0f
    }

    override suspend fun setTextScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.TEXT_SCALE] = scale.coerceIn(0.85f, 1.5f)
        }
    }

    override fun getScreenReaderOptimized(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SCREEN_READER_OPTIMIZED] ?: false
    }

    override suspend fun setScreenReaderOptimized(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SCREEN_READER_OPTIMIZED] = enabled
        }
    }

    override fun getVibrationFeedback(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.VIBRATION_FEEDBACK] ?: false
    }

    override suspend fun setVibrationFeedback(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.VIBRATION_FEEDBACK] = enabled
        }
    }

    override fun getNonColorIndicators(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NON_COLOR_INDICATORS] ?: false
    }

    override suspend fun setNonColorIndicators(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NON_COLOR_INDICATORS] = enabled
        }
    }

    override fun getStickyScroll(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.STICKY_SCROLL] ?: true
    }

    override suspend fun setStickyScroll(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.STICKY_SCROLL] = enabled
        }
    }

    override fun getShowMinimap(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SHOW_MINIMAP] ?: false
    }

    override suspend fun setShowMinimap(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_MINIMAP] = enabled
        }
    }

    // ===== 自动配对补全设置实现 =====

    override fun getAutoPairCompletion(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_PAIR_COMPLETION] ?: true
    }

    override suspend fun setAutoPairCompletion(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_PAIR_COMPLETION] = enabled
        }
    }

    // ===== 网络设置实现 =====

    override fun getGithubMirrorUrl(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.GITHUB_MIRROR_URL] ?: ""
    }

    override suspend fun setGithubMirrorUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[Keys.GITHUB_MIRROR_URL] = url.trimEnd('/')
        }
    }

    // ===== 编辑器主题设置实现 =====

    override fun getEditorThemeId(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.EDITOR_THEME_ID] ?: ""
    }

    override suspend fun setEditorThemeId(themeId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.EDITOR_THEME_ID] = themeId
        }
    }

    // ===== Markdown 预览设置实现 =====

    override fun getMarkdownThemeName(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.MARKDOWN_THEME_NAME] ?: "DEFAULT"
    }

    override suspend fun setMarkdownThemeName(themeName: String) {
        dataStore.edit { prefs ->
            prefs[Keys.MARKDOWN_THEME_NAME] = themeName
        }
    }

    override fun getCustomMarkdownCss(): Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_MARKDOWN_CSS] ?: ""
    }

    override suspend fun setCustomMarkdownCss(css: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_MARKDOWN_CSS] = css
        }
    }

    // ===== 目录排序偏好设置实现 =====

    override fun getDirectorySortOption(directoryUri: String): Flow<String?> = dataStore.data.map { prefs ->
        val key = stringPreferencesKey(DIR_SORT_PREFIX + directoryUri)
        prefs[key]
    }

    override suspend fun setDirectorySortOption(directoryUri: String, sortOption: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(DIR_SORT_PREFIX + directoryUri)] = sortOption
        }
    }

    // ===== 首页文件列表自定义排序实现 =====

    override fun getPinnedOrder(): Flow<List<String>> = dataStore.data.map { prefs ->
        decodeOrderList(prefs[Keys.PINNED_ORDER])
    }

    override suspend fun setPinnedOrder(order: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.PINNED_ORDER] = encodeOrderList(order)
        }
    }

    override fun getRecentOrder(): Flow<List<String>> = dataStore.data.map { prefs ->
        decodeOrderList(prefs[Keys.RECENT_ORDER])
    }

    override suspend fun setRecentOrder(order: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.RECENT_ORDER] = encodeOrderList(order)
        }
    }

    override fun getInternalFilesOrder(): Flow<List<String>> = dataStore.data.map { prefs ->
        decodeOrderList(prefs[Keys.INTERNAL_FILES_ORDER])
    }

    override suspend fun setInternalFilesOrder(order: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.INTERNAL_FILES_ORDER] = encodeOrderList(order)
        }
    }

    override fun getBookmarkOrder(): Flow<List<String>> = dataStore.data.map { prefs ->
        decodeOrderList(prefs[Keys.BOOKMARK_ORDER])
    }

    override suspend fun setBookmarkOrder(order: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.BOOKMARK_ORDER] = encodeOrderList(order)
        }
    }
}
