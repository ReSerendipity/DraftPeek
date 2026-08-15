/**
 * 功能开关管理器模块。
 *
 * 运行时管理功能开关状态，开关持久化在SharedPreferences中以实现快速同步读取，
 * 并通过StateFlow暴露以支持响应式UI更新。使用Hilt @Singleton注解确保全局单例。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.feature

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 功能开关管理器类（单例）。
 *
 * 管理所有[FeatureFlag]的启用/禁用状态，提供同步快速读取（isEnabled）和
 * 响应式Flow观察（isEnabledFlow）两种访问方式。状态变更立即持久化到SharedPreferences。
 */
@Singleton
class FeatureToggleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _flagStates = MutableStateFlow(
        FeatureFlag.entries.associateWith { flag ->
            prefs.getBoolean(flag.key, flag.defaultEnabled)
        }
    )

    /** 所有功能开关状态的响应式StateFlow */
    val flagStates: StateFlow<Map<FeatureFlag, Boolean>> = _flagStates.asStateFlow()

    /**
     * 检查功能开关当前是否启用（同步快速读取）。
     *
     * @param flag 要检查的功能开关
     * @return 是否启用
     */
    fun isEnabled(flag: FeatureFlag): Boolean =
        _flagStates.value[flag] ?: flag.defaultEnabled

    /**
     * 以Flow形式观察单个功能开关状态。
     *
     * @param flag 要观察的功能开关
     * @return 开关状态的Flow
     */
    fun isEnabledFlow(flag: FeatureFlag): Flow<Boolean> =
        _flagStates.map { it[flag] ?: flag.defaultEnabled }

    /**
     * 启用或禁用功能开关，立即持久化。
     *
     * @param flag 要设置的功能开关
     * @param enabled 是否启用
     */
    fun setEnabled(flag: FeatureFlag, enabled: Boolean) {
        prefs.edit().putBoolean(flag.key, enabled).apply()
        _flagStates.value = _flagStates.value.toMutableMap().apply {
            this[flag] = enabled
        }
    }

    /**
     * 将功能开关重置为默认值。
     *
     * @param flag 要重置的功能开关
     */
    fun reset(flag: FeatureFlag) {
        prefs.edit().remove(flag.key).apply()
        _flagStates.value = _flagStates.value.toMutableMap().apply {
            this[flag] = flag.defaultEnabled
        }
    }

    /**
     * 将所有功能开关重置为默认值。
     */
    fun resetAll() {
        prefs.edit().clear().apply()
        _flagStates.value = FeatureFlag.entries.associateWith { it.defaultEnabled }
    }

    companion object {
        private const val PREFS_NAME = "draftpeek_feature_flags"
    }
}
