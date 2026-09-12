/**
 * 文件: ThemeManageViewModel.kt
 * 功能: 统计模块 ViewModel - 自定义主题导入管理
 * 描述: 桥接 CustomThemeRepository，供 ProfileScreen 的"主题管理"对话框使用：
 *       1. 暴露已导入主题列表 customThemes Flow
 *       2. 协程中执行主题导入（JSON -> 文件）
 *       3. 协程中执行主题删除
 *
 * 所有 I/O 操作均在 Dispatchers.IO 上执行。
 */
package com.draftpeek.feature.stats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.feature.editor.sora.CustomThemeRepository
import com.draftpeek.feature.editor.sora.ThemeMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * 管理用户自定义主题的导入、删除与列表观察。
 *
 * 通过 Hilt 注入 [CustomThemeRepository]（其绑定位于 feature/editor 的 EditorModule）。
 */
@HiltViewModel
class ThemeManageViewModel @Inject constructor(
    private val customThemeRepository: CustomThemeRepository
) : ViewModel() {

    /** 已导入自定义主题的响应式列表。 */
    val customThemes: StateFlow<List<ThemeMetadata>> =
        customThemeRepository.customThemes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    /**
     * 从 JSON 内容导入主题。在 [Dispatchers.IO] 上执行。
     *
     * @return 导入成功的主题元数据；JSON 非法或写入失败时返回 null。
     */
    suspend fun importTheme(json: String, fileName: String?): ThemeMetadata? =
        withContext(Dispatchers.IO) {
            customThemeRepository.importTheme(json, fileName)
        }

    /**
     * 按 ID 删除已导入主题。在 [Dispatchers.IO] 上执行。
     *
     * @return true 表示主题存在且已删除。
     */
    suspend fun deleteTheme(themeId: String): Boolean =
        withContext(Dispatchers.IO) {
            customThemeRepository.deleteTheme(themeId)
        }
}
