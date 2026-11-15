package com.draftpeek.core.common.plugin.security

import dalvik.system.DexClassLoader

/** 插件类被策略拒绝时抛出；属于 SecurityException，宿主可统一捕获。 */
class PluginClassAccessDeniedException(message: String) : SecurityException(message)

/**
 * 在 DexClassLoader 的 loadClass 上做运行时二次校验。
 *
 * 顺序很重要：
 * 1. 先拒绝 restrictedClasses，即使父 ClassLoader 能加载（防止
 *    `Class.forName("java.lang.Runtime")` 借宿主类绕过）。
 * 2. 已加载类 / 父 ClassLoader 命中则放行（宿主 API 与系统类）。
 * 3. 父未命中时，类名必须落在插件清单的 allowedClassPrefixes 内。
 * 4. 最后交给 DexClassLoader 从插件 DEX 查找。
 */
class PolicyEnforcingDexClassLoader(
    dexPath: String,
    optimizedDirectory: String?,
    librarySearchPath: String?,
    parent: ClassLoader?,
    private val policy: PluginRuntimePolicy,
) : DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val restricted = PluginClassPolicy.checkRestricted(name, policy.restrictedClasses)
        if (restricted is ClassPolicyDecision.Denied) {
            throw PluginClassAccessDeniedException(restricted.reason)
        }

        val alreadyLoaded = findLoadedClass(name)
        if (alreadyLoaded != null) {
            if (resolve) resolveClass(alreadyLoaded)
            return alreadyLoaded
        }

        val fromParent = try {
            parent?.loadClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
        if (fromParent != null) {
            if (resolve) resolveClass(fromParent)
            return fromParent
        }

        val allowed = PluginClassPolicy.checkAllowedClass(
            name,
            policy.entry,
            policy.restrictedClasses,
        )
        if (allowed is ClassPolicyDecision.Denied) {
            throw PluginClassAccessDeniedException(allowed.reason)
        }

        return super.loadClass(name, resolve)
    }
}
