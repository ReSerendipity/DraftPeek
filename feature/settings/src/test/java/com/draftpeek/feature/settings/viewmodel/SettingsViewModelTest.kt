package com.draftpeek.feature.settings.viewmodel

import app.cash.turbine.test
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.model.EditorSettings
import com.draftpeek.feature.settings.repository.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SettingsViewModel].
 *
 * Uses MockK to mock [SettingsRepository] and verifies that ViewModel
 * correctly delegates update calls to the repository.
 */
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    private val settingsFlow = MutableStateFlow(EditorSettings())

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true) {
            every { settings } returns settingsFlow
        }
        viewModel = SettingsViewModel(mockRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------
    // 更新字体大小
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("更新字体大小")
    inner class UpdateFontSizeTests {

        @Test
        @DisplayName("updateFontSize 应调用 repository.updateFontSize")
        fun updateFontSize_callsRepository() = testScope.runTest {
            viewModel.updateFontSize(18)

            advanceUntilIdle()

            advanceUntilIdle()

            coVerify { mockRepository.updateFontSize(18) }
        }
    }

    // ------------------------------------------------------------------
    // 切换主题
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("切换主题")
    inner class UpdateThemeTests {

        @Test
        @DisplayName("updateTheme(DARK) 应调用 repository.updateTheme")
        fun updateTheme_dark_callsRepository() = testScope.runTest {
            viewModel.updateTheme(AppTheme.DARK)

            advanceUntilIdle()

            advanceUntilIdle()

            coVerify { mockRepository.updateTheme(AppTheme.DARK) }
        }

        @Test
        @DisplayName("updateTheme(LIGHT) 应调用 repository.updateTheme")
        fun updateTheme_light_callsRepository() = testScope.runTest {
            viewModel.updateTheme(AppTheme.LIGHT)

            advanceUntilIdle()

            advanceUntilIdle()

            coVerify { mockRepository.updateTheme(AppTheme.LIGHT) }
        }

        @Test
        @DisplayName("updateTheme(SYSTEM) 应调用 repository.updateTheme")
        fun updateTheme_system_callsRepository() = testScope.runTest {
            viewModel.updateTheme(AppTheme.SYSTEM)

            advanceUntilIdle()

            advanceUntilIdle()

            coVerify { mockRepository.updateTheme(AppTheme.SYSTEM) }
        }
    }

    // ------------------------------------------------------------------
    // 切换语言
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("切换语言")
    inner class UpdateLanguageTests {

        @Test
        @DisplayName("updateLanguage(ZH) 应调用 repository.setLanguage")
        fun updateLanguage_zh_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.ZH)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.ZH) }
        }

        @Test
        @DisplayName("updateLanguage(ZH_TW) 应调用 repository.setLanguage")
        fun updateLanguage_zh_tw_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.ZH_TW)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.ZH_TW) }
        }

        @Test
        @DisplayName("updateLanguage(EN) 应调用 repository.setLanguage")
        fun updateLanguage_en_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.EN)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.EN) }
        }

        @Test
        @DisplayName("updateLanguage(JA) 应调用 repository.setLanguage")
        fun updateLanguage_ja_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.JA)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.JA) }
        }

        @Test
        @DisplayName("updateLanguage(KO) 应调用 repository.setLanguage")
        fun updateLanguage_ko_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.KO)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.KO) }
        }

        @Test
        @DisplayName("updateLanguage(SYSTEM) 应调用 repository.setLanguage")
        fun updateLanguage_system_callsRepository() = testScope.runTest {
            viewModel.updateLanguage(AppLanguage.SYSTEM)

            advanceUntilIdle()

            coVerify { mockRepository.setLanguage(AppLanguage.SYSTEM) }
        }
    }

    // ------------------------------------------------------------------
    // 更新自动保存设置
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("更新自动保存设置")
    inner class UpdateAutoSaveTests {

        @Test
        @DisplayName("updateAutoSave(true) 应调用 repository.setAutoSave(true)")
        fun updateAutoSave_enable_callsRepository() = testScope.runTest {
            viewModel.updateAutoSave(true)

            advanceUntilIdle()

            coVerify { mockRepository.setAutoSave(true) }
        }

        @Test
        @DisplayName("updateAutoSave(false) 应调用 repository.setAutoSave(false)")
        fun updateAutoSave_disable_callsRepository() = testScope.runTest {
            viewModel.updateAutoSave(false)

            advanceUntilIdle()

            coVerify { mockRepository.setAutoSave(false) }
        }

        @Test
        @DisplayName("updateAutoSaveIntervalMs 应调用 repository.setAutoSaveIntervalMs")
        fun updateAutoSaveIntervalMs_callsRepository() = testScope.runTest {
            viewModel.updateAutoSaveIntervalMs(60000)

            advanceUntilIdle()

            coVerify { mockRepository.setAutoSaveIntervalMs(60000) }
        }
    }

    // ------------------------------------------------------------------
    // 设置 Flow 正确传播
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("设置 Flow 正确传播")
    inner class SettingsFlowTests {

        @Test
        @DisplayName("settings StateFlow 初始值为 EditorSettings 默认值")
        fun settings_initialValue_isDefault() = testScope.runTest {
            val defaultSettings = EditorSettings()
            assertEquals(defaultSettings, viewModel.settings.value)
        }

        @Test
        @DisplayName("settings StateFlow 反映 repository 的更新")
        fun settings_reflectsRepositoryUpdates() = testScope.runTest {
            val updatedSettings = EditorSettings(
                fontSize = 20,
                theme = AppTheme.DARK,
                language = AppLanguage.ZH,
                autoSave = true,
            )

            viewModel.settings.test {
                assertEquals(EditorSettings(), awaitItem())

                settingsFlow.value = updatedSettings
                advanceUntilIdle()

                assertEquals(updatedSettings, awaitItem())
            }
        }
    }

    // ------------------------------------------------------------------
    // 其他设置更新
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("其他设置更新")
    inner class OtherSettingsTests {

        @Test
        @DisplayName("updateLineWrapping 应调用 repository")
        fun updateLineWrapping_callsRepository() = testScope.runTest {
            viewModel.updateLineWrapping(true)
            advanceUntilIdle()

            coVerify { mockRepository.updateLineWrapping(true) }
        }

        @Test
        @DisplayName("updateShowLineNumbers 应调用 repository")
        fun updateShowLineNumbers_callsRepository() = testScope.runTest {
            viewModel.updateShowLineNumbers(false)
            advanceUntilIdle()

            coVerify { mockRepository.updateShowLineNumbers(false) }
        }

        @Test
        @DisplayName("updateTabWidth 应调用 repository")
        fun updateTabWidth_callsRepository() = testScope.runTest {
            viewModel.updateTabWidth(2)
            advanceUntilIdle()

            coVerify { mockRepository.setTabWidth(2) }
        }

        @Test
        @DisplayName("updateFontFamily 应调用 repository")
        fun updateFontFamily_callsRepository() = testScope.runTest {
            viewModel.updateFontFamily("serif")
            advanceUntilIdle()

            coVerify { mockRepository.setFontFamily("serif") }
        }

        @Test
        @DisplayName("updateDefaultEncoding 应调用 repository")
        fun updateDefaultEncoding_callsRepository() = testScope.runTest {
            viewModel.updateDefaultEncoding("GBK")
            advanceUntilIdle()

            coVerify { mockRepository.setDefaultEncoding("GBK") }
        }
    }
}
