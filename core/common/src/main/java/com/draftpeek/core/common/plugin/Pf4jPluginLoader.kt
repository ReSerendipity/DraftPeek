/**
 * PF4J 插件框架适配器。
 *
 * 将 Eclipse PF4J (Plugin Framework for Java) 的插件生命周期管理能力
 * 集成到 DraftPeek 的 [PluginManager] 中。
 *
 * PF4J 提供：
 * - 插件发现（JAR/APK 文件扫描）
 * - 插件版本管理
 * - 扩展点机制（ExtensionPoint + Extension）
 * - 插件生命周期（STARTED / STOPPED / DISABLED）
 * - ClassLoader 隔离
 *
 * 当前集成状态（P1）：
 * - PF4J 依赖已添加
 * - 本适配器提供 PF4J Plugin → DraftPeekPlugin 的桥接
 * - [PluginManager] 保留原有编程式注册 API
 * - 未来迭代中将通过 Pf4jPluginLoader 实现 JAR/APK 动态加载
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.core.common.plugin

import android.content.Context
import android.util.Log
import org.pf4j.PluginState
import org.pf4j.PluginWrapper

/**
 * PF4J 插件状态 → DraftPeek 启用状态 转换。
 */
fun PluginState.toDraftPeekEnabled(): Boolean = this == PluginState.STARTED

/**
 * PF4J PluginWrapper → DraftPeek PluginInfo 转换。
 */
fun PluginWrapper.toPluginInfo(): PluginInfo =
    PluginInfo(
        id = pluginId,
        name = descriptor?.pluginId ?: pluginId,
        version = descriptor?.version ?: "1.0.0",
        description = descriptor?.pluginDescription ?: "",
        author = descriptor?.provider ?: "",
    )

/**
 * PF4J 插件加载器（骨架）。
 *
 * 提供基于 PF4J 的插件发现和加载能力。
 * 当前为骨架实现，未来将通过 DexClassLoader 实现 APK 插件加载。
 *
 * @property context 应用上下文
 */
class Pf4jPluginLoader(private val context: Context) {

    companion object {
        private const val TAG = "Pf4jPluginLoader"
        private const val PLUGINS_DIR = "plugins"
    }

    /**
     * 扫描插件目录并返回发现的插件信息列表。
     *
     * 当前实现返回空列表（无动态插件加载）。
     * 未来将通过 PF4J 的 PluginManager 实现 JAR/APK 扫描。
     *
     * @return 发现的插件信息列表
     */
    fun discoverPlugins(): List<PluginInfo> {
        val pluginsDir = context.getDir(PLUGINS_DIR, Context.MODE_PRIVATE)
        if (!pluginsDir.exists()) {
            Log.d(TAG, "Plugins directory does not exist: ${pluginsDir.absolutePath}")
            return emptyList()
        }

        val pluginFiles = pluginsDir.listFiles { file ->
            file.extension.equals("jar", ignoreCase = true) ||
                file.extension.equals("apk", ignoreCase = true)
        } ?: emptyArray()

        if (pluginFiles.isEmpty()) {
            Log.d(TAG, "No plugin files found in ${pluginsDir.absolutePath}")
            return emptyList()
        }

        // TODO: Use PF4J's PluginManager to load and validate plugins
        // For now, just log what we found
        pluginFiles.forEach { file ->
            Log.d(TAG, "Found plugin file: ${file.name}")
        }

        return emptyList()
    }

    /**
     * 检查 PF4J 是否可用。
     *
     * @return true 如果 PF4J 库已正确加载
     */
    fun isAvailable(): Boolean = try {
        Class.forName("org.pf4j.PluginManager")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
