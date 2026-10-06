package com.draftpeek.feature.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import com.draftpeek.core.designsystem.theme.ColorBlindMode
import com.draftpeek.core.designsystem.theme.RainbowColor
import com.draftpeek.feature.settings.model.AppLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// 独立 DataStore 名：preferencesDataStore 委托是按「文件名」的单例，
// 与 DataStoreRoundTripTest 的 test_settings 同名会触发
// "There are multiple DataStores active for the same file"。
private val android.content.Context.roundTripDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "test_settings_round_trip")

/**
 * SettingsRepositoryImpl 各设置项的写入 → 读回往返测试。
 *
 * 动机：无障碍页自 feature/stats 迁入本模块后，模块分母被 Compose 代码生成指令撑大，
 * 覆盖率被被动稀释（14.7% → 10.29%），击穿 coverage ratchet。
 * 本文件补齐仓储层一批此前无测试覆盖的 setter/getter 往返（数据层逻辑，正是本模块
 * 测试该覆盖的地方），把覆盖率拉回阈值之上。
 *
 * 平台说明与 [DataStoreRoundTripTest] 相同：JUnit4 + Robolectric + junit-vintage-engine。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryRoundTripTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dataStore = context.roundTripDataStore
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() = runTest {
        dataStore.edit { it.clear() }
    }

    @Test
    fun `tab width round trip`() = runTest {
        repository.setTabWidth(8)
        assertEquals(8, repository.getTabWidth().first())
    }

    @Test
    fun `auto indent round trip`() = runTest {
        repository.setAutoIndent(false)
        assertEquals(false, repository.getAutoIndent().first())
    }

    @Test
    fun `highlight current line round trip`() = runTest {
        repository.setHighlightCurrentLine(true)
        assertEquals(true, repository.getHighlightCurrentLine().first())
    }

    @Test
    fun `show indent guides round trip`() = runTest {
        repository.setShowIndentGuides(true)
        assertEquals(true, repository.getShowIndentGuides().first())
    }

    @Test
    fun `default encoding round trip`() = runTest {
        repository.setDefaultEncoding("GBK")
        assertEquals("GBK", repository.getDefaultEncoding().first())
    }

    @Test
    fun `auto save round trip`() = runTest {
        repository.setAutoSave(true)
        assertEquals(true, repository.getAutoSave().first())
    }

    @Test
    fun `auto save interval round trip`() = runTest {
        repository.setAutoSaveIntervalMs(60000L)
        assertEquals(60000L, repository.getAutoSaveIntervalMs().first())
    }

    @Test
    fun `font family round trip`() = runTest {
        repository.setFontFamily("JetBrains Mono")
        assertEquals("JetBrains Mono", repository.getFontFamily().first())
    }

    @Test
    fun `code font family id round trip`() = runTest {
        repository.setCodeFontFamilyId("jetbrains_mono")
        assertEquals("jetbrains_mono", repository.getCodeFontFamilyId().first())
    }

    @Test
    fun `ui font family id round trip`() = runTest {
        repository.setUiFontFamilyId("inter")
        assertEquals("inter", repository.getUiFontFamilyId().first())
    }

    @Test
    fun `language round trip`() = runTest {
        repository.setLanguage(AppLanguage.ZH_TW)
        assertEquals(AppLanguage.ZH_TW, repository.getLanguage().first())
    }

    @Test
    fun `pinned files round trip`() = runTest {
        val pinned = setOf("file:///a.txt", "file:///b.md")
        repository.setPinnedFiles(pinned)
        assertEquals(pinned, repository.getPinnedFiles().first())
    }

    @Test
    fun `activity color round trip`() = runTest {
        repository.setActivityColor(RainbowColor.INDIGO)
        assertEquals(RainbowColor.INDIGO, repository.getActivityColor().first())
    }

    @Test
    fun `recent files limit round trip`() = runTest {
        repository.setRecentFilesLimit(10)
        assertEquals(10, repository.getRecentFilesLimit().first())
    }

    @Test
    fun `color blind mode round trip`() = runTest {
        repository.setColorBlindMode(ColorBlindMode.DEUTERANOPIA)
        assertEquals(ColorBlindMode.DEUTERANOPIA, repository.getColorBlindMode().first())
    }

    @Test
    fun `high contrast mode round trip`() = runTest {
        repository.setHighContrastMode(true)
        assertEquals(true, repository.getHighContrastMode().first())
    }

    @Test
    fun `text scale round trip`() = runTest {
        repository.setTextScale(1.3f)
        assertEquals(1.3f, repository.getTextScale().first())
    }

    @Test
    fun `github mirror url round trip`() = runTest {
        // setGithubMirrorUrl 会 trimEnd('/')，这里同时锁定该归一化行为
        repository.setGithubMirrorUrl("https://mirror.example.com/")
        assertEquals("https://mirror.example.com", repository.getGithubMirrorUrl().first())
    }

    @Test
    fun `second write overwrites first for same key`() = runTest {
        repository.setTabWidth(2)
        repository.setTabWidth(6)
        assertEquals(6, repository.getTabWidth().first())
    }

    @Test
    fun `independent keys do not interfere`() = runTest {
        repository.setTabWidth(4)
        repository.setDefaultEncoding("UTF-16")
        repository.setLanguage(AppLanguage.EN)
        assertEquals(4, repository.getTabWidth().first())
        assertEquals("UTF-16", repository.getDefaultEncoding().first())
        assertEquals(AppLanguage.EN, repository.getLanguage().first())
    }

    @Test
    fun `auto pair completion round trip`() = runTest {
        repository.setAutoPairCompletion(false)
        assertEquals(false, repository.getAutoPairCompletion().first())
    }

    @Test
    fun `show minimap round trip`() = runTest {
        repository.setShowMinimap(true)
        assertEquals(true, repository.getShowMinimap().first())
    }

    @Test
    fun `sticky scroll round trip`() = runTest {
        repository.setStickyScroll(true)
        assertEquals(true, repository.getStickyScroll().first())
    }

    @Test
    fun `screen reader optimized round trip`() = runTest {
        repository.setScreenReaderOptimized(true)
        assertEquals(true, repository.getScreenReaderOptimized().first())
    }

    @Test
    fun `non color indicators round trip`() = runTest {
        repository.setNonColorIndicators(true)
        assertEquals(true, repository.getNonColorIndicators().first())
    }

    @Test
    fun `vibration feedback round trip`() = runTest {
        repository.setVibrationFeedback(false)
        assertEquals(false, repository.getVibrationFeedback().first())
    }

    @Test
    fun `custom markdown css round trip`() = runTest {
        repository.setCustomMarkdownCss("body { color: red; }")
        assertEquals("body { color: red; }", repository.getCustomMarkdownCss().first())
    }

    @Test
    fun `editor theme id round trip`() = runTest {
        repository.setEditorThemeId("dracula")
        assertEquals("dracula", repository.getEditorThemeId().first())
    }

    @Test
    fun `markdown theme name round trip`() = runTest {
        repository.setMarkdownThemeName("GITHUB")
        assertEquals("GITHUB", repository.getMarkdownThemeName().first())
    }

    @Test
    fun `pinned order round trip`() = runTest {
        val order = listOf("file:///b.txt", "file:///a.txt")
        repository.setPinnedOrder(order)
        assertEquals(order, repository.getPinnedOrder().first())
    }

    @Test
    fun `recent order round trip`() = runTest {
        val order = listOf("file:///c.md")
        repository.setRecentOrder(order)
        assertEquals(order, repository.getRecentOrder().first())
    }

    @Test
    fun `internal files order round trip`() = runTest {
        val order = listOf("dir:///x", "dir:///y")
        repository.setInternalFilesOrder(order)
        assertEquals(order, repository.getInternalFilesOrder().first())
    }

    @Test
    fun `bookmark order round trip`() = runTest {
        val order = listOf("file:///z.kt")
        repository.setBookmarkOrder(order)
        assertEquals(order, repository.getBookmarkOrder().first())
    }

    @Test
    fun `directory sort option round trip`() = runTest {
        repository.setDirectorySortOption("content://tree/1", "NAME_ASC")
        assertEquals("NAME_ASC", repository.getDirectorySortOption("content://tree/1").first())
    }
}
