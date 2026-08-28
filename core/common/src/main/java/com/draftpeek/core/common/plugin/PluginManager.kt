/**
 * 插件管理器模块。
 *
 * 本文件实现了DraftPeek插件系统的中央管理器，负责插件的发现、注册、生命周期管理、
 * 扩展点分发以及插件状态持久化。使用ConcurrentHashMap确保线程安全操作，
 * 支持自动注册扩展和手动注册两种模式。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.plugin

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件管理器单例对象。
 *
 * DraftPeek插件系统的核心管理器，负责：
 * - 插件的发现和注册
 * - 生命周期管理（创建/销毁）
 * - 扩展点分发
 *
 * 当前为骨架实现，仅支持编程式插件注册。未来迭代将添加：
 * - 基于APK的插件加载（通过DexClassLoader）
 * - 插件市场集成
 * - 权限沙箱
 *
 * 线程安全：所有操作通过ConcurrentHashMap保证线程安全。
 */
object PluginManager {

    private const val TAG = "PluginManager"

    /** 已注册的插件，以插件唯一ID为键 */
    private val plugins = ConcurrentHashMap<String, DraftPeekPlugin>()

    /** 每个插件的启用状态，跨会话持久化 */
    private val enabledState = ConcurrentHashMap<String, Boolean>()

    /** 扩展注册中心实现实例 */
    private val registry = PluginExtensionRegistry()

    /** 插件系统是否已初始化的标志 */
    @Volatile
    private var initialized = false

    /**
     * 初始化插件系统。
     *
     * 应在[Application.onCreate]期间调用一次。
     * 从SharedPreferences恢复插件启用状态。
     *
     * @param context 应用上下文
     */
    fun initialize(context: Context) {
        if (initialized) {
            Log.w(TAG, "PluginManager already initialized")
            return
        }
        val prefs = context.getSharedPreferences("plugin_prefs", Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            if (value is Boolean) {
                enabledState[key] = value
            }
        }
        initialized = true
        Log.d(TAG, "PluginManager initialized with ${enabledState.size} persisted states")
    }

    /**
     * 编程式注册插件。
     *
     * 自动注册流程：
     * 1. 在映射表中注册插件
     * 2. 自动注册通过[DraftPeekPlugin.getAutoRegisteredExtensions]声明的扩展
     * 3. 调用[DraftPeekPlugin.onCreate]进行手动注册和初始化
     *
     * 如果自动注册或onCreate失败，插件将被完全回滚（从映射表移除并注销所有扩展）。
     *
     * @param plugin 要注册的插件实例
     * @param context 应用上下文
     * @return 注册成功返回true，如果已存在相同ID的插件则返回false
     */
    fun registerPlugin(plugin: DraftPeekPlugin, context: Context): Boolean {
        val id = plugin.info.id
        if (plugins.putIfAbsent(id, plugin) != null) {
            Log.w(TAG, "Plugin $id is already registered")
            return false
        }

        registry.setPluginId(id)
        try {
            val autoExtensions = plugin.getAutoRegisteredExtensions()
            if (autoExtensions.isNotEmpty()) {
                autoRegisterExtensions(plugin.info.id, autoExtensions)
                Log.d(TAG, "Auto-registered ${autoExtensions.size} extensions for plugin $id")
            }

            plugin.onCreate(context, registry)
            Log.d(TAG, "Plugin registered: ${plugin.info.name} v${plugin.info.version}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin $id", e)
            plugins.remove(id)
            registry.unregisterAll(id)
            return false
        } finally {
            registry.clearPluginId()
        }
        return true
    }

    /**
     * 自动注册插件声明的扩展。
     *
     * 处理每个[AutoRegisteredExtension]并委托给相应的注册中心方法。
     * 验证扩展ID不与已注册的扩展冲突。
     *
     * @param pluginId 插件ID
     * @param extensions 需要自动注册的扩展列表
     */
    private fun autoRegisterExtensions(pluginId: String, extensions: List<AutoRegisteredExtension>) {
        for (extension in extensions) {
            try {
                when (extension) {
                    is AutoRegisteredExtension.Language -> {
                        if (registry.hasLanguage(extension.languageId)) {
                            Log.w(
                                TAG,
                                "Language ${extension.languageId} already registered, skipping auto-registration from $pluginId"
                            )
                        } else {
                            registry.registerLanguage(extension.languageId, extension.provider)
                            Log.d(TAG, "Auto-registered language: ${extension.languageId} from $pluginId")
                        }
                    }
                    is AutoRegisteredExtension.Theme -> {
                        if (registry.hasTheme(extension.themeId)) {
                            Log.w(
                                TAG,
                                "Theme ${extension.themeId} already registered, skipping auto-registration from $pluginId"
                            )
                        } else {
                            registry.registerTheme(extension.themeId, extension.provider)
                            Log.d(TAG, "Auto-registered theme: ${extension.themeId} from $pluginId")
                        }
                    }
                    is AutoRegisteredExtension.Command -> {
                        registry.registerCommand(extension.commandId, extension.label, extension.handler)
                        Log.d(TAG, "Auto-registered command: ${extension.commandId} from $pluginId")
                    }
                    is AutoRegisteredExtension.Panel -> {
                        registry.registerPanel(extension.panelId, extension.provider)
                        Log.d(TAG, "Auto-registered panel: ${extension.panelId} from $pluginId")
                    }
                    is AutoRegisteredExtension.ExportHandler -> {
                        registry.registerExportHandler(extension.formatId, extension.provider)
                        Log.d(TAG, "Auto-registered export handler: ${extension.formatId} from $pluginId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-register extension from $pluginId", e)
            }
        }
    }

    /**
     * 通过ID注销插件。
     *
     * 调用插件的onDestroy生命周期方法，然后从注册中心移除该插件的所有扩展。
     *
     * @param pluginId 要移除的插件ID
     */
    fun unregisterPlugin(pluginId: String) {
        plugins.remove(pluginId)?.let { plugin ->
            try {
                plugin.onDestroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying plugin $pluginId", e)
            }
            registry.unregisterAll(pluginId)
            Log.d(TAG, "Plugin unregistered: $pluginId")
        }
    }

    /**
     * 获取扩展注册中心以读取已注册的扩展。
     * 在功能模块中使用此方法发现和使用插件贡献项。
     *
     * @return 扩展注册中心实例
     */
    fun getRegistry(): ExtensionRegistry = registry

    /**
     * 获取类型化扩展注册中心以查询特定扩展类型。
     * 暴露内部注册中心用于语言/主题查找。
     *
     * 在需要按ID、文件扩展名等查询扩展时使用此方法。
     *
     * @return 类型化的插件扩展注册中心实例
     */
    internal fun getTypedRegistry(): PluginExtensionRegistry = registry

    /**
     * 根据文件扩展名查找语言提供者。
     * 便捷方法，用于从文件类型解析语言支持。
     *
     * @param extension 文件扩展名（不带点，如 "rs"、"kt"）
     * @return 语言提供者，如果没有插件处理此扩展名则返回null
     */
    fun findLanguageForExtension(extension: String): LanguageExtensionProvider? =
        registry.findLanguageByExtension(extension)?.second

    /**
     * 通过主题ID查找主题提供者。
     * 便捷方法，用于按ID解析主题。
     *
     * @param themeId 主题标识符
     * @return 主题提供者，如果未找到则返回null
     */
    fun findThemeById(themeId: String): ThemeExtensionProvider? = registry.getThemeProviders()[themeId]

    /**
     * 获取所有已注册插件的元数据信息。
     *
     * @return 所有插件的PluginInfo列表
     */
    fun getPluginInfos(): List<PluginInfo> = plugins.values.map { it.info }

    /**
     * 检查具有给定ID的插件是否已注册。
     *
     * @param pluginId 插件ID
     * @return 已注册返回true，否则返回false
     */
    fun isPluginRegistered(pluginId: String): Boolean = plugins.containsKey(pluginId)

    /**
     * 检查插件是否启用。
     * 如果未设置显式状态，插件默认为启用状态。
     *
     * @param pluginId 插件ID
     * @return 启用返回true，禁用返回false
     */
    fun isEnabled(pluginId: String): Boolean = enabledState[pluginId] ?: true

    /**
     * 获取所有当前启用的插件。
     *
     * @return 启用插件的PluginInfo列表
     */
    fun getEnabledPlugins(): List<PluginInfo> = plugins.values.filter { isEnabled(it.info.id) }.map { it.info }

    /**
     * 获取所有当前禁用的插件。
     *
     * @return 禁用插件的PluginInfo列表
     */
    fun getDisabledPlugins(): List<PluginInfo> = plugins.values.filter { !isEnabled(it.info.id) }.map { it.info }

    /**
     * 设置插件的启用状态。
     * 持久化状态并通知插件。
     *
     * @param pluginId 插件ID
     * @param enabled 是否启用
     * @param persist 是否持久化状态到SharedPreferences，默认为true
     */
    fun setPluginEnabled(pluginId: String, enabled: Boolean, persist: Boolean = true) {
        enabledState[pluginId] = enabled
        plugins[pluginId]?.onStateChanged(enabled)
        Log.d(TAG, "Plugin $pluginId ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * 将当前启用状态持久化到SharedPreferences。
     * 应在批量更改后或应用进入后台时调用。
     *
     * @param context 应用上下文
     */
    fun persistEnabledStates(context: Context) {
        val prefs = context.getSharedPreferences("plugin_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        enabledState.forEach { (pluginId, enabled) ->
            editor.putBoolean(pluginId, enabled)
        }
        editor.apply()
        Log.d(TAG, "Persisted ${enabledState.size} plugin states")
    }

    /**
     * 通知所有插件配置值已改变。
     *
     * @param configKey 改变的配置键
     */
    fun notifyConfigChanged(configKey: String) {
        plugins.forEach { (id, plugin) ->
            if (isEnabled(id)) {
                try {
                    plugin.onConfigChanged(configKey)
                } catch (e: Exception) {
                    Log.w(TAG, "Plugin $id failed to handle config change: $configKey", e)
                }
            }
        }
    }

    /**
     * 释放所有插件资源。在Application.onTerminate期间调用。
     */
    fun release() {
        plugins.values.forEach { plugin ->
            try {
                plugin.onDestroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying plugin ${plugin.info.id}", e)
            }
        }
        plugins.clear()
        registry.clear()
        initialized = false
        Log.d(TAG, "PluginManager released")
    }
}

/**
 * [ExtensionRegistry]的内部实现类。
 *
 * 跟踪每个扩展是由哪个插件贡献的，以便[unregisterAll]可以干净地移除它们。
 *
 * 可见性为internal以允许[PluginManager.getTypedRegistry]暴露查询方法用于语言/主题解析。
 */
internal class PluginExtensionRegistry : ExtensionRegistry {

    /** 当前插件ID上下文——在registerPlugin期间设置，用于标记扩展的归属 */
    @Volatile
    private var currentPluginId: String = ""

    /** 已注册语言映射：languageId → (pluginId, LanguageExtensionProvider) */
    private val languages = ConcurrentHashMap<String, Pair<String, LanguageExtensionProvider>>()

    /** 已注册命令映射：commandId → (pluginId, (label, handler)) */
    private val commands = ConcurrentHashMap<String, Pair<String, Pair<String, () -> Unit>>>()

    /** 已注册主题映射：themeId → (pluginId, ThemeExtensionProvider) */
    private val themes = ConcurrentHashMap<String, Pair<String, ThemeExtensionProvider>>()

    /** 已注册面板映射：panelId → (pluginId, PanelExtensionProvider) */
    private val panels = ConcurrentHashMap<String, Pair<String, PanelExtensionProvider>>()

    /** 已注册导出处理器映射：formatId → (pluginId, ExportHandlerProvider) */
    private val exportHandlers = ConcurrentHashMap<String, Pair<String, ExportHandlerProvider>>()

    /**
     * 设置后续注册操作使用的插件ID上下文。
     *
     * @param pluginId 当前正在注册的插件ID
     */
    fun setPluginId(pluginId: String) {
        currentPluginId = pluginId
    }

    /**
     * 注册完成后清除插件ID上下文。
     */
    fun clearPluginId() {
        currentPluginId = ""
    }

    override fun registerLanguage(languageId: String, provider: LanguageExtensionProvider) {
        languages[languageId] = currentPluginId to provider
    }

    override fun registerCommand(commandId: String, label: String, handler: () -> Unit) {
        commands[commandId] = Pair(currentPluginId, Pair(label, handler))
    }

    override fun registerTheme(themeId: String, provider: ThemeExtensionProvider) {
        themes[themeId] = currentPluginId to provider
    }

    override fun registerPanel(panelId: String, provider: PanelExtensionProvider) {
        panels[panelId] = currentPluginId to provider
    }

    override fun registerExportHandler(formatId: String, provider: ExportHandlerProvider) {
        exportHandlers[formatId] = currentPluginId to provider
    }

    /**
     * 注销特定插件贡献的所有扩展。
     * 仅移除属于给定pluginId的扩展。
     *
     * @param pluginId 要清除其扩展的插件ID
     */
    override fun unregisterAll(pluginId: String) {
        languages.entries.removeIf { it.value.first == pluginId }
        commands.entries.removeIf { it.value.first == pluginId }
        themes.entries.removeIf { it.value.first == pluginId }
        panels.entries.removeIf { it.value.first == pluginId }
        exportHandlers.entries.removeIf { it.value.first == pluginId }
        Log.d("PluginExtRegistry", "Cleared all extensions for plugin: $pluginId")
    }

    /**
     * 检查具有给定ID的语言是否已注册。
     *
     * @param languageId 语言ID
     * @return 已注册返回true
     */
    fun hasLanguage(languageId: String): Boolean = languages.containsKey(languageId)

    /**
     * 检查具有给定ID的主题是否已注册。
     *
     * @param themeId 主题ID
     * @return 已注册返回true
     */
    fun hasTheme(themeId: String): Boolean = themes.containsKey(themeId)

    /**
     * 通过文件扩展名查找语言提供者（如 "rs" → rust语言）。
     *
     * @param extension 文件扩展名
     * @return (pluginId, LanguageExtensionProvider)对，未找到返回null
     */
    fun findLanguageByExtension(extension: String): Pair<String, LanguageExtensionProvider>? =
        languages.values.find { it.second.fileExtensions.contains(extension) }

    /**
     * 通过语言ID获取语言提供者。
     *
     * @param languageId 语言ID
     * @return LanguageExtensionProvider，未找到返回null
     */
    fun getLanguageProvider(languageId: String): LanguageExtensionProvider? = languages[languageId]?.second

    /**
     * 获取所有已注册的语言扩展。
     *
     * @return languageId → LanguageExtensionProvider映射
     */
    fun getLanguageProviders(): Map<String, LanguageExtensionProvider> = languages.mapValues { it.value.second }

    /**
     * 获取所有已注册的命令。
     *
     * @return commandId → (label, handler)映射
     */
    fun getCommands(): Map<String, Pair<String, () -> Unit>> = commands.mapValues { it.value.second }

    /**
     * 获取所有已注册的主题提供者。
     *
     * @return themeId → ThemeExtensionProvider映射
     */
    fun getThemeProviders(): Map<String, ThemeExtensionProvider> = themes.mapValues { it.value.second }

    /**
     * 获取所有已注册的面板提供者。
     *
     * @return panelId → PanelExtensionProvider映射
     */
    fun getPanelProviders(): Map<String, PanelExtensionProvider> = panels.mapValues { it.value.second }

    /**
     * 获取所有已注册的导出处理器。
     *
     * @return formatId → ExportHandlerProvider映射
     */
    fun getExportHandlers(): Map<String, ExportHandlerProvider> = exportHandlers.mapValues { it.value.second }

    /**
     * 获取所有类型的已注册扩展总数。
     *
     * @return 扩展总数
     */
    fun getExtensionCount(): Int = languages.size + commands.size + themes.size + panels.size + exportHandlers.size

    /**
     * 清除所有已注册的扩展。
     */
    fun clear() {
        languages.clear()
        commands.clear()
        themes.clear()
        panels.clear()
        exportHandlers.clear()
    }
}
