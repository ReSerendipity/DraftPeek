/**
 * 插件加载器（DexClassLoader 实现）。
 *
 * 将 [Pf4jPluginLoader] 实现为基于 Android [DexClassLoader] 的简单插件加载器，
 * 扫描应用私有插件目录下的 `.apk` / `.dex` 文件并反射实例化插件。
 *
 * ## 当前实现（任务26：简化实现 + 明确标注）
 *
 * - 扫描目录：`context.getDir("plugins", MODE_PRIVATE)`
 * - 加载器：[PolicyEnforcingDexClassLoader]（继承 Android `DexClassLoader`），
 *   optimizedDirectory 使用 [Context.getCodeCacheDir]（应用可写）
 * - 父 ClassLoader：当前线程的 contextClassLoader，使插件类能解析宿主的
 *   [DraftPeekPlugin] 接口（接口由宿主 ClassLoader 加载，保证类型一致）
 * - 插件类发现：优先由插件在 classpath 根放置清单文件声明实现类：
 *     1. `plugin.properties`，含 `pluginClass=com.example.MyPlugin`
 *     2. 或 ServiceLoader 风格的 `META-INF/services/com.draftpeek.core.common.plugin.DraftPeekPlugin`
 *   类级安全扫描由纯 Kotlin [com.draftpeek.core.common.plugin.security.DexParser]
 *   完成，不再依赖已废弃的 `DexFile` 枚举；实现类仍由清单声明，避免猜测入口。
 * - 实例化：要求插件类提供无参公开构造函数，强转为 [DraftPeekPlugin]
 * - 单个插件加载失败（ClassNotFoundException / InstantiationException / 类准入拒绝等）
 *   只记录日志，不影响其他插件
 *
 * ## 安全闸门（P0-2）
 *
 * 加载前由 [PluginSecurityVerifier] 对每个文件做 fail-closed 校验：
 * - APK：包名 + 全部签名证书 SHA-256 必须命中 `assets/plugin_trust.json`；
 * - DEX：文件 SHA-256 必须精确命中信任清单；
 * - 空清单/解析失败 = 全部拒绝；空文件、超过 50 MB、canonical path 不在插件目录内一律拒绝；
 * - 命中信任条目后，解析 DEX 的 class_defs/type_ids：定义类必须全部落在
 *   条目 `allowedClassPrefixes` 内，定义/引用类型不得命中全局 `restrictedClasses`；
 * - 运行时使用 [PolicyEnforcingDexClassLoader] 在 `loadClass` 复用同一套纯函数
 *   再次拒绝受限类与前缀外类（父 ClassLoader 命中宿主 API 时仍先拒绝 restrictedClasses）。
 *
 * 仍未提供进程内沙箱；DEX 扫描是类级结构检查，不覆盖方法体内的反射/动态调用。
 * 插件代码与宿主同进程、同 uid 运行，可访问应用全部私有数据。**仅应加载受信任来源的插件**。
 *
 * ## PF4J
 *
 * PF4J 依赖保留在工程中（`gradle/libs.versions.toml`）。当前运行时并未使用 PF4J 管理器，
 * 而是用上述 DexClassLoader 做最小可用加载；PF4J 的插件生命周期、扩展点、ClassLoader
 * 隔离能力作为未来完整插件体系的扩展点保留。
 *
 * @author DraftPeek Team
 * @since 1.0.24
 */
package com.draftpeek.core.common.plugin

import android.content.Context
import android.util.Log
import com.draftpeek.core.common.plugin.security.AndroidApkInspector
import com.draftpeek.core.common.plugin.security.PluginClassAccessDeniedException
import com.draftpeek.core.common.plugin.security.PluginRuntimePolicy
import com.draftpeek.core.common.plugin.security.PluginSecurityVerifier
import com.draftpeek.core.common.plugin.security.PluginTrustEntry
import com.draftpeek.core.common.plugin.security.PluginTrustStore
import com.draftpeek.core.common.plugin.security.PluginTrustStoreLoader
import com.draftpeek.core.common.plugin.security.PluginVerificationResult
import com.draftpeek.core.common.plugin.security.PolicyEnforcingDexClassLoader
import java.io.File
import java.util.Properties
import org.pf4j.PluginState
import org.pf4j.PluginWrapper

/**
 * PF4J 插件状态 → DraftPeek 启用状态 转换。
 */
fun PluginState.toDraftPeekEnabled(): Boolean = this == PluginState.STARTED

/**
 * PF4J PluginWrapper → DraftPeek PluginInfo 转换。
 * （保留用于未来接入完整 PF4J 管理器时桥接其描述符。）
 */
fun PluginWrapper.toPluginInfo(): PluginInfo = PluginInfo(
    id = pluginId,
    name = descriptor?.pluginId ?: pluginId,
    version = descriptor?.version ?: "1.0.0",
    description = descriptor?.pluginDescription ?: "",
    author = descriptor?.provider ?: ""
)

/**
 * 插件加载器。
 *
 * 使用 [DexClassLoader] 从应用私有目录加载插件 APK/DEX。
 *
 * @property context 应用上下文
 */
class Pf4jPluginLoader(private val context: Context) {

    companion object {
        private const val TAG = "Pf4jPluginLoader"
        private const val PLUGINS_DIR = "plugins"

        /** classpath 上声明插件实现类的 properties 文件 */
        private const val PROPS_RESOURCE = "plugin.properties"

        /** ServiceLoader 风格的服务声明资源路径 */
        private const val SERVICES_RESOURCE =
            "META-INF/services/com.draftpeek.core.common.plugin.DraftPeekPlugin"
    }

    /** 本次 discoverPlugins 成功实例化的插件，供 PluginManager 注册生命周期 */
    private val loadedPlugins = mutableListOf<DraftPeekPlugin>()

    /**
     * 插件信任清单与校验器。
     *
     * 构造时加载 `assets/plugin_trust.json`；加载失败按空清单 fail-closed。
     * 信任清单同时提供全局 restrictedClasses，供运行时 ClassLoader 复用。
     */
    private val trustStore: PluginTrustStore =
        PluginTrustStoreLoader.load(context.applicationContext ?: context)

    private val securityVerifier: PluginSecurityVerifier = PluginSecurityVerifier(
        trustStore = trustStore,
        apkInspector = AndroidApkInspector(context.applicationContext ?: context),
    )

    /**
     * 扫描插件目录并加载发现的插件。
     *
     * 对每个 `.apk` / `.dex` 文件：
     * 1. 由 [PluginSecurityVerifier] 校验信任清单、签名/哈希、DEX 类级准入与路径约束（失败跳过）
     * 2. 用 [PolicyEnforcingDexClassLoader] 加载（parent = 当前线程 contextClassLoader，
     *    loadClass 时再次执行 restrictedClasses/前缀校验）
     * 3. 从 `plugin.properties` 或 `META-INF/services/...` 读取实现类名
     * 4. 反射加载并实例化（无参构造），校验实现 [DraftPeekPlugin]
     *
     * 单个插件失败不中断整体扫描。
     *
     * @return 加载成功的插件 [PluginInfo] 列表
     */
    fun discoverPlugins(): List<PluginInfo> {
        loadedPlugins.clear()

        val pluginsDir = context.getDir(PLUGINS_DIR, Context.MODE_PRIVATE)
        if (!pluginsDir.exists()) {
            Log.d(TAG, "Plugins directory does not exist: ${pluginsDir.absolutePath}")
            return emptyList()
        }

        val pluginsDirCanonical = runCatching { pluginsDir.canonicalFile }.getOrNull()
        if (pluginsDirCanonical == null) {
            Log.w(TAG, "Cannot resolve plugins directory: ${pluginsDir.absolutePath}")
            return emptyList()
        }

        val pluginFiles = pluginsDir.listFiles { file ->
            file.extension.equals("apk", ignoreCase = true) ||
                file.extension.equals("dex", ignoreCase = true)
        } ?: emptyArray()

        if (pluginFiles.isEmpty()) {
            Log.d(TAG, "No plugin files found in ${pluginsDir.absolutePath}")
            return emptyList()
        }

        val infos = mutableListOf<PluginInfo>()
        for (file in pluginFiles) {
            val canonical = runCatching { file.canonicalFile }.getOrNull()
            if (canonical == null || canonical.parentFile != pluginsDirCanonical) {
                Log.w(TAG, "Rejected plugin outside plugins dir: ${file.name}")
                continue
            }

            val entry = when (val verification = securityVerifier.verify(canonical)) {
                is PluginVerificationResult.Denied -> {
                    Log.w(TAG, "Rejected plugin ${file.name}: ${verification.reason}")
                    continue
                }

                is PluginVerificationResult.Allowed -> {
                    Log.d(
                        TAG,
                        "Plugin ${file.name} verified by trust entry ${verification.entry.id}",
                    )
                    verification.entry
                }
            }

            val plugin = loadPlugin(canonical, entry)
            if (plugin != null) {
                loadedPlugins += plugin
                infos += plugin.info
                Log.d(TAG, "Loaded dynamic plugin: ${plugin.info.name} v${plugin.info.version}")
            }
        }
        return infos
    }

    /**
     * 获取最近一次 [discoverPlugins] 成功实例化的插件实例。
     *
     * PluginManager 在发现后调用此方法拿到实例并走 registerPlugin 生命周期。
     */
    fun getLoadedPlugins(): List<DraftPeekPlugin> = loadedPlugins.toList()

    /**
     * 加载单个插件文件；[entry] 是 [PluginSecurityVerifier] 已放行的信任条目。
     * 失败返回 null。
     */
    private fun loadPlugin(file: File, entry: PluginTrustEntry): DraftPeekPlugin? {
        return try {
            val parent = Thread.currentThread().contextClassLoader
                ?: Pf4jPluginLoader::class.java.classLoader
                ?: ClassLoader.getSystemClassLoader()

            val optimizedDir = File(context.codeCacheDir, "dex").apply { mkdirs() }
            val loader = PolicyEnforcingDexClassLoader(
                dexPath = file.absolutePath,
                optimizedDirectory = optimizedDir.absolutePath,
                librarySearchPath = null,
                parent = parent,
                policy = PluginRuntimePolicy(
                    entry = entry,
                    restrictedClasses = trustStore.restrictedClasses,
                ),
            )

            val className = readPluginClassName(loader)
            if (className.isNullOrBlank()) {
                Log.w(TAG, "No pluginClass declared for ${file.name}; skipped")
                return null
            }

            val clazz = try {
                loader.loadClass(className)
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "Plugin class not found: $className in ${file.name}", e)
                return null
            }

            if (!DraftPeekPlugin::class.java.isAssignableFrom(clazz)) {
                Log.w(TAG, "$className does not implement DraftPeekPlugin; skipped")
                return null
            }

            val instance = try {
                clazz.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to instantiate $className (needs public no-arg ctor)", e)
                return null
            }

            instance as? DraftPeekPlugin
        } catch (e: PluginClassAccessDeniedException) {
            Log.w(TAG, "Plugin class access denied for ${file.name}: ${e.message}")
            null
        } catch (e: Exception) {
            // 覆盖 DexClassLoader 创建失败、IO 异常等，单个插件失败不影响其他
            Log.w(TAG, "Failed to load plugin file: ${file.name}", e)
            null
        }
    }

    /**
     * 从插件 classpath 读取插件实现类名。
     *
     * 优先级：
     * 1. `plugin.properties` 中的 `pluginClass` 键
     * 2. `META-INF/services/com.draftpeek.core.common.plugin.DraftPeekPlugin`（ServiceLoader 风格）
     */
    private fun readPluginClassName(loader: ClassLoader): String? {
        // 1) plugin.properties
        loader.getResourceAsStream(PROPS_RESOURCE)?.use { stream ->
            runCatching {
                Properties().apply { load(stream) }.getProperty("pluginClass")?.trim()
            }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        // 2) ServiceLoader 风格服务声明文件
        loader.getResourceAsStream(SERVICES_RESOURCE)?.use { stream ->
            runCatching {
                stream.bufferedReader().useLines { lines ->
                    lines.map { it.trim() }
                        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
                }
            }.getOrNull()?.let { return it }
        }

        return null
    }

    /**
     * 检查 PF4J 是否在 classpath 上可用（保留作未来扩展判断）。
     */
    fun isAvailable(): Boolean = try {
        Class.forName("org.pf4j.PluginManager")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}
