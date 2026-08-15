/**
 * 撰码轻览 (DraftPeek) 响应式缓存偏好设置工具类。
 *
 * 提供一种同步读取 + 响应式观察的设置访问模式，避免每次读取设置都需要调用
 * Flow.first() 挂起函数阻塞协程。内存中始终保持最新值的快照，可随时同步访问。
 *
 * 设计参考：Xed-Editor 的 CachedPreference 响应式设置模式。
 *
 * 模块：feature/settings
 */
package com.draftpeek.feature.settings.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 响应式缓存偏好设置，保持内存快照与持久化数据源（通常是 DataStore）同步。
 *
 * 设计说明：
 * - 首次访问时，[cachedValue] 立即返回初始默认值，直到持久化数据源发射第一个有效值
 * - 持久化源 [source] 的 Flow 被持续收集，实时更新 [cachedValue]，
 *   因此首次发射后，同步读取始终返回最新值
 * - 消除了仅为读取设置值而调用 .first() 阻塞协程的需要，提升读取性能
 *
 * 使用示例：
 * ```
 * val fontSize = CachedPreference(settingsRepository::getFontSize, 16)
 * // 协程中启动收集：
 * fontSize.startCollecting()
 * // 同步读取（快速，无需协程）：
 * val current = fontSize.cachedValue
 * // 响应式观察：
 * fontSize.state.collect { size -> updateUI(size) }
 * ```
 *
 * @param T 缓存值的类型，必须是非空类型（Any）
 * @property source 持久化数据源 Flow，通常来自 DataStore 或 Repository
 * @property initialValue 初始默认值，在数据源首次发射前使用
 */
@Singleton
class CachedPreference<T : Any>(
    private val source: Flow<T>,
    initialValue: T,
) {
    private val _state = MutableStateFlow(initialValue)

    /**
     * 响应式状态流，镜像持久化数据源的值。
     * UI 层可通过 collect 观察值变化，实现自动重组。
     */
    val state: StateFlow<T> = _state.asStateFlow()

    /**
     * 当前值的同步缓存读取。
     * 返回内存中保存的最新值，非挂起函数，可在任意线程直接调用。
     */
    val cachedValue: T get() = _state.value

    /**
     * 开始收集数据源 Flow 到缓存中。
     * 需在协程作用域（如 ViewModel init）中调用一次，启动持续收集。
     *
     * 收集行为：
     * - distinctUntilChanged()：值未变化时不发射，减少不必要的 UI 更新
     * - catch()：读取错误时静默忽略，保留最后已知值，避免崩溃
     */
    suspend fun startCollecting() {
        source
            .distinctUntilChanged()
            .catch { /* 忽略读取错误，保持最后已知值 */ }
            .collect { value ->
                _state.value = value
            }
    }
}

/**
 * CachedPreference 工厂类，用于 Hilt 依赖注入创建实例。
 *
 * 因为 CachedPreference 构造函数需要传入 Flow 源参数，无法直接通过 @Inject 构造，
 * 所以通过此工厂在运行时动态创建实例。
 *
 * 使用方式：
 * ```
 * @Inject lateinit var cachedPreferenceFactory: CachedPreferenceFactory
 * val fontSize = cachedPreferenceFactory.create(settingsRepository.getFontSize(), 16)
 * ```
 */
@Singleton
class CachedPreferenceFactory @Inject constructor() {
    /**
     * 创建一个 [CachedPreference] 实例。
     *
     * @param source 数据源 Flow
     * @param initialValue 初始默认值
     * @return 配置好的 CachedPreference 实例
     */
    fun <T : Any> create(source: Flow<T>, initialValue: T): CachedPreference<T> {
        return CachedPreference(source, initialValue)
    }
}
