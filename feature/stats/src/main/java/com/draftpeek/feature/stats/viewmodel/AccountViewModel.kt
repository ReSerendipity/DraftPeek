/**
 * 文件: AccountViewModel.kt
 * 功能: 统计模块 ViewModel - 「我的」页身份区的展示状态（设计回函 v3 · 阶段 A）
 * 描述: 阶段 A 只交付**身份区五态的展示层**：账号体系与同步引擎属阶段 B
 *       （`core/sync` 目前仅有模块骨架、未接入构建），因此这里不伪造后端，
 *       只维护 [AccountUiState] 并把动作暴露给 UI。
 *
 *       阶段 B 到位时：把 [_uiState] 的来源从「默认未登录 + debug 模拟」换成真实
 *       账号/同步数据源即可，UI 层无需改动。
 */
package com.draftpeek.feature.stats.viewmodel

import androidx.lifecycle.ViewModel
import com.draftpeek.feature.stats.account.AccountStateSimulation
import com.draftpeek.feature.stats.account.AccountUiState
import com.draftpeek.feature.stats.account.SyncUiStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 身份区状态。
 *
 * **默认值必须是 [AccountUiState.SignedOut]**：这是当前唯一真实的业务状态（无账号体系）。
 */
@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.SignedOut)

    /** 身份区展示状态。 */
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    /**
     * 「重试」：把失败/离线态拉回同步中。
     *
     * 阶段 A 无真实引擎，这里只驱动状态；阶段 B 需同时触发真实重试请求。
     */
    fun retrySync() {
        val current = _uiState.value
        if (current is AccountUiState.SignedIn) {
            _uiState.value = current.copy(sync = SyncUiStatus.Syncing)
        }
    }

    /**
     * 「取消」：同步中超时后取消，回到上一次成功态。
     *
     * 阶段 A 无真实引擎，回到模拟的「已同步」；阶段 B 需同时取消真实请求。
     */
    fun cancelSync() {
        val current = _uiState.value
        if (current is AccountUiState.SignedIn && current.sync is SyncUiStatus.Syncing) {
            _uiState.value = current.copy(
                sync = SyncUiStatus.Synced(
                    System.currentTimeMillis() - AccountStateSimulation.SIMULATED_SYNCED_AGO_MS
                )
            )
        }
    }

    /**
     * 退出登录。
     *
     * 按产品定稿（设计回函 v3 §2.3）：**本地片段 / 统计 / 设置 / 缓存全部保留**，
     * 仅清除云端登录状态。因此这里只重置身份区状态，不触碰任何本地数据。
     */
    fun signOut() {
        _uiState.value = AccountUiState.SignedOut
    }

    /**
     * **debug 验收专用**：按固定顺序轮转五态，使阶段 A 的五态可真机验收。
     *
     * release 构建不暴露入口（UI 层按 debuggable 判断隐藏），阶段 B 落地后应删除。
     */
    fun simulateNextState() {
        _uiState.value = AccountStateSimulation.next(_uiState.value, System.currentTimeMillis())
    }
}
