/**
 * 撰码轻览 (DraftPeek) 设置界面 ViewModel。
 *
 * 本文件负责管理应用全局设置的状态，连接 UI 层与数据持久化层（Repository）。
 * 遵循 MVVM 架构模式，通过 Kotlin StateFlow 向 UI 层暴露响应式状态。
 *
 * 状态管理说明：
 * - 状态持有者：[settings] StateFlow，持有完整的 [EditorSettings] 不可变对象
 * - 状态共享策略：SharingStarted.WhileSubscribed(5_000)
 *   - 当最后一个订阅者消失后，保持流活跃 5 秒，避免配置变更（如屏幕旋转）时重新订阅导致的数据重新发射
 *   - 5 秒后如果仍无订阅者，上游 Flow 取消，节省资源
 * - 初始值：首次订阅时立即发射 EditorSettings() 默认值，避免 UI 出现空状态
 * - 更新机制：所有 setter 方法通过 viewModelScope.launch 在 IO 线程执行持久化操作，
 *   持久化成功后 DataStore 自动触发 settings Flow 发射新值，UI 通过 collectAsStateWithLifecycle()
 *   自动重组，实现单向数据流（Unidirectional Data Flow）
 * - 错误处理：所有更新操作包裹在 try-catch 中，失败时仅记录日志，不崩溃，
 *   因为设置保存失败不应阻断用户操作，下次启动仍使用原有设置
 *
 * 使用方式（在 Composable 中）：
 * ```
 * @Composable
 * fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
 *     val settings by viewModel.settings.collectAsStateWithLifecycle()
 *     // 使用 settings.fontSize 等
 * }
 * ```
 *
 * 模块：feature/settings
 * 生命周期：@HiltViewModel 由 Hilt 管理，绑定到 NavBackStackEntry 或 Activity
 */
package com.draftpeek.feature.settings.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.designsystem.theme.ColorBlindMode
import com.draftpeek.feature.settings.model.AppLanguage
import com.draftpeek.feature.settings.model.AppTheme
import com.draftpeek.feature.settings.model.EditorSettings
import com.draftpeek.feature.settings.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置 ViewModel，提供设置状态的响应式访问和更新方法。
 *
 * 所有 public 方法都是非 suspend 的，UI 层可直接在主线程调用，
 * ViewModel 内部负责协程调度和线程切换。
 *
 * @property repository 设置仓库接口，由 Hilt 注入
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {

    companion object {
        /** 日志标签，用于 Logcat 过滤设置相关错误 */
        private const val TAG = "SettingsViewModel"
    }

    /**
     * 完整编辑器设置状态流。
     *
     * 这是一个热流（Hot Flow），转换自仓库的冷流（Cold Flow）：
     * - stateIn 操作符将上游 Flow 转换为 StateFlow，确保始终有最新值可用
     * - 初始值为 EditorSettings() 默认配置，UI 订阅立即可渲染
     * - WhileSubscribed(5000) 策略平衡了响应性和资源消耗
     * - 值变化时自动通知所有活跃订阅者，触发 Compose 重组
     */
    val settings: StateFlow<EditorSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditorSettings()
        )

    /** 更新编辑器字体大小（自动限制在 10-24sp 范围） */
    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            try {
                repository.updateFontSize(size)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update fontSize", e)
            }
        }
    }

    /** 更新应用主题模式（浅色/深色/跟随系统） */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                repository.updateTheme(theme)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update theme", e)
            }
        }
    }

    /** 更新自动换行开关状态 */
    fun updateLineWrapping(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateLineWrapping(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update lineWrapping", e)
            }
        }
    }

    /** 更新行号显示开关状态 */
    fun updateShowLineNumbers(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateShowLineNumbers(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update showLineNumbers", e)
            }
        }
    }

    /** 更新 Tab 宽度（空格数） */
    fun updateTabWidth(width: Int) {
        viewModelScope.launch {
            try {
                repository.setTabWidth(width)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update tabWidth", e)
            }
        }
    }

    /** 更新自动缩进开关状态 */
    fun updateAutoIndent(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAutoIndent(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update autoIndent", e)
            }
        }
    }

    /** 更新当前行高亮开关状态 */
    fun updateHighlightCurrentLine(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setHighlightCurrentLine(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update highlightCurrentLine", e)
            }
        }
    }

    /** 更新缩进参考线显示开关状态 */
    fun updateShowIndentGuides(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setShowIndentGuides(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update showIndentGuides", e)
            }
        }
    }

    /** 更新默认文件编码 */
    fun updateDefaultEncoding(encoding: String) {
        viewModelScope.launch {
            try {
                repository.setDefaultEncoding(encoding)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update defaultEncoding", e)
            }
        }
    }

    /** 更新自动保存开关状态 */
    fun updateAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAutoSave(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update autoSave", e)
            }
        }
    }

    /** 更新自动保存间隔时间（毫秒） */
    fun updateAutoSaveIntervalMs(intervalMs: Long) {
        viewModelScope.launch {
            try {
                repository.setAutoSaveIntervalMs(intervalMs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update autoSaveIntervalMs", e)
            }
        }
    }

    /** @deprecated 已废弃，使用 updateCodeFontFamilyId/updateUiFontFamilyId 替代 */
    fun updateFontFamily(family: String) {
        viewModelScope.launch {
            try {
                repository.setFontFamily(family)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update fontFamily", e)
            }
        }
    }

    /** 更新代码编辑器字体 ID */
    fun updateCodeFontFamilyId(id: String) {
        viewModelScope.launch {
            try {
                repository.setCodeFontFamilyId(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update codeFontFamilyId", e)
            }
        }
    }

    /** 更新 UI 界面字体 ID */
    fun updateUiFontFamilyId(id: String) {
        viewModelScope.launch {
            try {
                repository.setUiFontFamilyId(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update uiFontFamilyId", e)
            }
        }
    }

    /** 更新应用界面语言，立即生效（通过 AppCompatDelegate） */
    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            try {
                repository.setLanguage(language)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language", e)
            }
        }
    }

    /** 更新用户显示名称 */
    fun updateUserName(userName: String) {
        viewModelScope.launch {
            try {
                repository.updateUserName(userName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update userName", e)
            }
        }
    }

    /** 更新用户头像 URI */
    fun updateUserAvatar(avatarUri: String) {
        viewModelScope.launch {
            try {
                repository.updateUserAvatar(avatarUri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update userAvatar", e)
            }
        }
    }

    /** 更新首页"最近打开"显示数量（限制 1-10 个） */
    fun updateRecentFilesLimit(limit: Int) {
        viewModelScope.launch {
            try {
                repository.setRecentFilesLimit(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update recentFilesLimit", e)
            }
        }
    }

    // ===== 无障碍功能设置 =====

    /** 更新色盲模式 */
    fun updateColorBlindMode(mode: ColorBlindMode) {
        viewModelScope.launch {
            try {
                repository.setColorBlindMode(mode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update colorBlindMode", e)
            }
        }
    }

    /** 更新高对比度模式开关状态 */
    fun updateHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setHighContrastMode(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update highContrastMode", e)
            }
        }
    }

    /** 更新 UI 文字缩放倍数（自动限制在 0.85-1.5 范围） */
    fun updateTextScale(scale: Float) {
        viewModelScope.launch {
            try {
                repository.setTextScale(scale)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update textScale", e)
            }
        }
    }

    /** 更新屏幕阅读器优化开关状态 */
    fun updateScreenReaderOptimized(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setScreenReaderOptimized(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update screenReaderOptimized", e)
            }
        }
    }

    /** 更新振动反馈开关状态 */
    fun updateVibrationFeedback(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setVibrationFeedback(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update vibrationFeedback", e)
            }
        }
    }

    /** 更新非色彩标识开关状态 */
    fun updateNonColorIndicators(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNonColorIndicators(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update nonColorIndicators", e)
            }
        }
    }

    /** 更新缩略图（minimap）显示开关状态 */
    fun updateShowMinimap(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setShowMinimap(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update showMinimap", e)
            }
        }
    }

    /** 更新编辑器代码高亮主题 ID */
    fun updateEditorThemeId(themeId: String) {
        viewModelScope.launch {
            try {
                repository.setEditorThemeId(themeId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update editorThemeId", e)
            }
        }
    }

    /** 更新粘性滚动开关状态 */
    fun updateStickyScroll(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setStickyScroll(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update stickyScroll", e)
            }
        }
    }

    /** 更新自动配对补全开关状态 */
    fun updateAutoPairCompletion(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAutoPairCompletion(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update autoPairCompletion", e)
            }
        }
    }

    // ===== 网络设置 =====

    /** 更新 GitHub 镜像 URL（自动去除末尾斜杠） */
    fun updateGithubMirrorUrl(url: String) {
        viewModelScope.launch {
            try {
                repository.setGithubMirrorUrl(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update githubMirrorUrl", e)
            }
        }
    }

    // ===== Markdown 预览设置 =====

    /** 更新 Markdown 预览主题名称 */
    fun updateMarkdownThemeName(themeName: String) {
        viewModelScope.launch {
            try {
                repository.setMarkdownThemeName(themeName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update markdownThemeName", e)
            }
        }
    }

    /** 更新自定义 Markdown 预览 CSS 样式 */
    fun updateCustomMarkdownCss(css: String) {
        viewModelScope.launch {
            try {
                repository.setCustomMarkdownCss(css)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update customMarkdownCss", e)
            }
        }
    }
}
