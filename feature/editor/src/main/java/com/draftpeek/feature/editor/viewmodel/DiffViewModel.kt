/**
 * 文件功能：文件差异对比界面的 ViewModel
 * 
 * 主要类/数据类：
 * - [DiffUiState]：差异对比界面的 UI 状态数据类
 * - [DiffViewModel]：差异对比 ViewModel，负责加载两个文件并执行差异比较
 * 
 * 模块依赖：
 * - core/common：DiffEngine 提供 LCS 差异比较算法
 * - core/data：UserActivityRepository 和 RecordUserActivityUseCase 用于用户行为统计
 * - feature/editor/repository：EditorRepository 用于文件读取
 * - Hilt：依赖注入
 */
package com.draftpeek.feature.editor.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.draftpeek.core.common.util.DiffEngine
import com.draftpeek.core.common.util.DiffResult
import com.draftpeek.core.common.util.DiffType
import com.draftpeek.core.data.repository.UserActivityRepository
import com.draftpeek.core.data.usecase.RecordUserActivityUseCase
import com.draftpeek.feature.editor.repository.EditorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 差异对比界面的 UI 状态
 * 
 * @property isLoading 是否正在加载文件
 * @property leftFileName 左侧文件名
 * @property rightFileName 右侧文件名
 * @property diffResult 差异比较结果，为 null 表示尚未完成比较
 * @property error 错误信息，为 null 表示无错误
 */
data class DiffUiState(
    val isLoading: Boolean = true,
    val leftFileName: String = "",
    val rightFileName: String = "",
    val diffResult: DiffResult? = null,
    val error: String? = null,
)

/**
 * 文件差异对比 ViewModel
 * 
 * **状态管理**：
 * - [uiState]：暴露加载状态、文件名、差异结果和错误信息
 * - [currentDiffIndex]：当前选中的差异项索引，用于上下导航
 * 
 * **数据流**：
 * 1. 初始化时从 SavedStateHandle 获取左右两个文件的 URI
 * 2. 在 Dispatchers.Default 线程上并发读取两个文件
 * 3. 使用 DiffEngine 执行 LCS 差异比较算法
 * 4. 将结果更新到 uiState，UI 层订阅状态更新
 * 
 * **使用场景**：
 * - 用户选择两个文件进行对比时
 * - 从 Git 状态查看文件变更时
 */
@HiltViewModel
class DiffViewModel @Inject constructor(
    private val repository: EditorRepository,
    private val userActivityRepository: UserActivityRepository,
    private val recordUserActivity: RecordUserActivityUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val leftUri: String = savedStateHandle["leftUri"] ?: ""
    private val rightUri: String = savedStateHandle["rightUri"] ?: ""

    private val _uiState = MutableStateFlow(DiffUiState())
    /** 差异对比界面的 UI 状态流 */
    val uiState: StateFlow<DiffUiState> = _uiState.asStateFlow()

    private val _currentDiffIndex = MutableStateFlow(0)
    /** 当前选中的差异项索引（0-based） */
    val currentDiffIndex: StateFlow<Int> = _currentDiffIndex.asStateFlow()

    init {
        loadFiles()
    }

    /**
     * 跳转到下一个差异项
     * 
     * 循环导航：到达最后一个差异项后回到第一个
     */
    fun nextDiff() {
        val result = _uiState.value.diffResult ?: return
        if (result.diffCount == 0) return
        val next = (_currentDiffIndex.value + 1) % result.diffCount
        _currentDiffIndex.value = next
    }

    /**
     * 跳转到上一个差异项
     * 
     * 循环导航：到达第一个差异项后跳到最后一个
     */
    fun prevDiff() {
        val result = _uiState.value.diffResult ?: return
        if (result.diffCount == 0) return
        val prev = (_currentDiffIndex.value - 1 + result.diffCount) % result.diffCount
        _currentDiffIndex.value = prev
    }

    /**
     * 获取当前差异项对应的滚动行号
     * 
     * 遍历左侧文件的所有行，统计非相等行的数量，找到第 N 个差异的位置。
     * 用于 UI 层自动滚动到当前选中的差异位置。
     * 
     * @return 左侧文件中当前差异所在的行索引（0-based），无差异时返回 0
     */
    fun getScrollLineForCurrentDiff(): Int {
        val result = _uiState.value.diffResult ?: return 0
        val idx = _currentDiffIndex.value
        var count = 0
        for (i in result.leftLines.indices) {
            if (result.leftLines[i].type != DiffType.EQUAL) {
                if (count == idx) return i
                count++
            }
        }
        return 0
    }

    /**
     * 加载两个文件并执行差异比较
     * 
     * 算法步骤：
     * 1. 验证两个 URI 都不为空且不相同
     * 2. 在后台线程使用 EditorRepository 读取两个文件内容
     * 3. 调用 DiffEngine.diff() 执行 LCS 差异比较
     * 4. 更新 UI 状态为成功，重置当前差异索引为 0
     * 5. 记录用户行为统计（diff 操作）
     * 6. 发生异常时更新错误状态
     */
    private fun loadFiles() {
        if (leftUri.isBlank() || rightUri.isBlank()) {
            _uiState.value = DiffUiState(
                isLoading = false,
                error = "未提供文件 URI",
            )
            return
        }

        if (leftUri == rightUri) {
            _uiState.value = DiffUiState(
                isLoading = false,
                error = "请选择两个不同的文件进行比较",
            )
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val leftResult = repository.readFile(Uri.parse(leftUri))
                val rightResult = repository.readFile(Uri.parse(rightUri))

                val diffResult = DiffEngine.diff(leftResult.content, rightResult.content)

                _uiState.value = DiffUiState(
                    isLoading = false,
                    leftFileName = leftResult.fileName,
                    rightFileName = rightResult.fileName,
                    diffResult = diffResult,
                )
                _currentDiffIndex.value = 0
                recordUserActivity.recordDiff()
            } catch (e: Exception) {
                _uiState.value = DiffUiState(
                    isLoading = false,
                    error = e.message ?: "加载文件失败",
                )
            }
        }
    }
}
