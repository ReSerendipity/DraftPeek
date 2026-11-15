package com.draftpeek.core.common.plugin.security

/** 静态扫描与运行时 ClassLoader 共用的准入判定。 */
internal sealed interface ClassPolicyDecision {
    data object Allowed : ClassPolicyDecision
    data class Denied(val reason: String) : ClassPolicyDecision
}

/** 运行时 ClassLoader 需要的策略上下文。 */
data class PluginRuntimePolicy(
    val entry: PluginTrustEntry,
    val restrictedClasses: Set<String>,
)

/**
 * 插件类准入的纯函数策略。
 *
 * 静态侧：DEX 定义的类必须全部落在 `entry.allowedClassPrefixes` 内；
 * 定义类与 type_ids 引用类型都不得命中全局 `restrictedClasses`。
 * 运行时侧：先拒绝 restrictedClasses（即使父 ClassLoader 能加载），
 * 再对父加载器未命中的类检查 allowedClassPrefixes，最后交给 DexClassLoader。
 */
internal object PluginClassPolicy {

    fun evaluateDex(
        scan: DexScanResult.Success,
        entry: PluginTrustEntry,
        restrictedClasses: Set<String>,
    ): ClassPolicyDecision {
        if (entry.allowedClassPrefixes.isEmpty()) {
            return ClassPolicyDecision.Denied("插件未配置 allowedClassPrefixes")
        }
        if (scan.definedClasses.isEmpty()) {
            return ClassPolicyDecision.Denied("DEX 未定义任何类")
        }

        for (descriptor in scan.definedClasses) {
            val className = PluginClassNames.descriptorToDotted(descriptor)
                ?: return ClassPolicyDecision.Denied("非法类描述符：$descriptor")
            if (!PluginClassNames.isUnderPrefixes(className, entry.allowedClassPrefixes)) {
                return ClassPolicyDecision.Denied(
                    "类 $className 不在 allowedClassPrefixes 内",
                )
            }
            if (PluginClassNames.isRestricted(className, restrictedClasses)) {
                return ClassPolicyDecision.Denied("类 $className 命中 restrictedClasses")
            }
        }

        for (descriptor in scan.referencedTypes) {
            val className = PluginClassNames.descriptorToDotted(descriptor) ?: continue
            if (PluginClassNames.isRestricted(className, restrictedClasses)) {
                return ClassPolicyDecision.Denied(
                    "DEX 引用了受限类 $className",
                )
            }
        }

        return ClassPolicyDecision.Allowed
    }

    /** 仅检查 restrictedClasses；供 ClassLoader 在父加载之前调用。 */
    fun checkRestricted(
        className: String,
        restrictedClasses: Set<String>,
    ): ClassPolicyDecision {
        val normalized = PluginClassNames.normalizeClassName(className)
            ?: return ClassPolicyDecision.Denied("非法类名：$className")
        if (PluginClassNames.isRestricted(normalized, restrictedClasses)) {
            return ClassPolicyDecision.Denied("类 $normalized 命中 restrictedClasses")
        }
        return ClassPolicyDecision.Allowed
    }

    /** 检查 restrictedClasses + allowedClassPrefixes；供父加载器未命中时调用。 */
    fun checkAllowedClass(
        className: String,
        entry: PluginTrustEntry,
        restrictedClasses: Set<String>,
    ): ClassPolicyDecision {
        val normalized = PluginClassNames.normalizeClassName(className)
            ?: return ClassPolicyDecision.Denied("非法类名：$className")
        if (PluginClassNames.isRestricted(normalized, restrictedClasses)) {
            return ClassPolicyDecision.Denied("类 $normalized 命中 restrictedClasses")
        }
        if (!PluginClassNames.isUnderPrefixes(normalized, entry.allowedClassPrefixes)) {
            return ClassPolicyDecision.Denied(
                "类 $normalized 不在 allowedClassPrefixes 内",
            )
        }
        return ClassPolicyDecision.Allowed
    }
}
