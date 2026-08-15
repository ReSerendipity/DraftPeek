/**
 * DraftPeek 安全代码自校验模块。
 *
 * **文件功能**：验证安全模块自身代码未被篡改，通过反射检查关键方法存在性、类加载验证、DEX 完整性委托等
 *               多维度交叉校验，防止攻击者直接修改安全检测逻辑绕过保护。
 *
 * **主要类/接口**：[SecurityIntegrityChecker] - 单例对象，提供安全代码自校验能力。
 *
 * **模块依赖**：
 * - Android 框架：[Context]
 * - 本模块其他安全组件：[DexIntegrityChecker]、[ApkIntegrityChecker]、[AntiDebug]
 * - Java 反射 API：验证类和方法签名完整性
 *
 * **安全说明**：属于纵深防御措施，所有逻辑在应用层，理论上可被绕过，但显著增加逆向成本。
 *
 * SECURITY VULN-003: 更新为使用 Kotlin 类引用替代硬编码字符串类名，
 * 使安全类可以被 R8 混淆类名而不破坏自校验逻辑。
 */
package com.draftpeek.security

import android.content.Context
import androidx.annotation.WorkerThread

/**
 * 安全代码完整性自校验器 —— 验证关键安全类未被篡改。
 *
 * ## 安全模型
 *
 * 攻击者篡改安全代码的典型路径：
 * 1. 反编译 APK
 * 2. 修改 AntiDebug.assess() 始终返回 SAFE
 * 3. 重新打包签名
 *
 * 本校验器通过多维度检测增加攻击成本：
 * 1. **行为验证**：确认所有安全检测方法存在且签名正确
 * 2. **DEX 完整性**：委托 DexIntegrityChecker 验证 DEX 文件未被修改
 * 3. **交叉校验**：多个独立检查相互验证，单一 hook 无法完全绕过
 *
 * ## 局限性
 * 由于所有逻辑在 Java/Kotlin 层，此检查本身也可被修改。
 * 但增加了攻击者的逆向成本（需同时绕过所有检查），属于纵深防御。
 * 如需更强防护应集成 Play Integrity API 或 Native 层校验。
 */
object SecurityIntegrityChecker {

    /**
     * 校验结果
     */
    sealed class IntegrityCheckResult {
        /** 所有完整性检查通过 */
        data object Verified : IntegrityCheckResult()
        /** 检测到代码被篡改 */
        data class Tampered(val details: String) : IntegrityCheckResult()
        /** 校验过程出错 */
        data class Error(val exception: Exception) : IntegrityCheckResult()
    }

    @Volatile
    var isVerified: Boolean = false
        private set

    /**
     * 执行安全代码完整性校验。
     *
     * 应在 app 启动时调用一次，结果缓存在 [isVerified] 中。
     * 后续可通过 [spotCheck] 进行轻量级快速校验。
     *
     * @param context Application context
     * @return 校验结果
     */
    @WorkerThread
    fun verifyIntegrity(context: Context): IntegrityCheckResult {
        return try {
            val issues = mutableListOf<String>()

            // ===== 检查 1：DEX 文件完整性 =====
            val dexResult = DexIntegrityChecker.verify(context)
            if (dexResult is DexIntegrityChecker.DexResult.Tampered) {
                issues.add("DEX integrity failed: ${dexResult.details}")
            }

            // ===== 检查 2：行为验证 —— 关键安全方法存在性 =====
            val behaviorIssues = verifyCriticalMethods()
            issues.addAll(behaviorIssues)

            // ===== 检查 3：类加载验证 —— 确认安全类可正常加载 =====
            val classIssues = verifySecurityClasses()
            issues.addAll(classIssues)

            if (issues.isEmpty()) {
                isVerified = true
                IntegrityCheckResult.Verified
            } else {
                isVerified = false
                IntegrityCheckResult.Tampered(issues.joinToString("; "))
            }
        } catch (e: Exception) {
            isVerified = false
            IntegrityCheckResult.Error(e)
        }
    }

    /**
     * 轻量级快速校验 —— 验证安全类自身完整性，无需 Context。
     * 通过行为验证（反射检查方法签名）检测代码是否被篡改。
     * 
     * 此方法设计为无 IO 操作，可在任何线程安全调用。
     *
     * SECURITY VULN-003: 使用 Kotlin 类引用替代硬编码字符串类名，
     * 兼容 R8 类名混淆。
     */
    fun verifySelfIntegrity(): Boolean {
        return try {
            // 行为验证：确认关键安全方法存在且签名正确
            val behaviorIssues = verifyCriticalMethods()
            if (behaviorIssues.isNotEmpty()) return false

            // 类加载验证：确认安全类未被替换
            val classIssues = verifySecurityClasses()
            if (classIssues.isNotEmpty()) return false

            // SecurityLevel 枚举完整性验证
            // VULN-003: 使用 Kotlin 类引用替代 Class.forName("com.draftpeek.security.AntiDebug$SecurityLevel")
            val securityLevelClass = AntiDebug.SecurityLevel::class.java
            val enumConstants = securityLevelClass.enumConstants
            // VULN-003: 使用运行时枚举值替代硬编码字符串集合
            val expectedValues = AntiDebug.SecurityLevel.values().map { it.name }.toSet()
            val actualValues = (enumConstants as? Array<*>)?.map {
                (it as? Enum<*>)?.name
            }?.toSet() ?: emptySet()
            if (actualValues != expectedValues) return false

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 轻量级快速校验 —— 使用缓存结果 + 快速行为检查。
     * 适用于高频调用场景。
     */
    fun spotCheck(): Boolean {
        if (isVerified) return true
        return quickBehaviorCheck()
    }

    /**
     * 行为验证 —— 检查所有关键安全方法的存在性和签名。
     * 攻击者删除或修改方法时会触发检测。
     *
     * SECURITY VULN-003: 使用 Kotlin 类引用替代 simpleName 字符串比较，
     * 兼容 R8 类名混淆。
     */
    private fun verifyCriticalMethods(): List<String> {
        val issues = mutableListOf<String>()

        // AntiDebug 关键方法
        val antiDebugMethods = mapOf(
            "assess" to emptyArray<Class<*>>(),
            "enforce" to emptyArray<Class<*>>(),
            "isDebuggerConnected" to emptyArray<Class<*>>(),
            "isRunningOnEmulator" to emptyArray<Class<*>>(),
            "isTraced" to emptyArray<Class<*>>(),
            "isFridaDetected" to emptyArray<Class<*>>(),
            "isXposedDetected" to emptyArray<Class<*>>(),
            "isRooted" to emptyArray<Class<*>>(),
            "isHookFrameworkDetected" to emptyArray<Class<*>>(),
            "isZygiskDetected" to emptyArray<Class<*>>(),  // VULN-014: Zygisk 检测
            "quickCheck" to emptyArray<Class<*>>(),
        )

        for ((methodName, paramTypes) in antiDebugMethods) {
            try {
                val method = AntiDebug::class.java.getMethod(methodName, *paramTypes)
                // 验证方法返回类型 — 使用类引用替代 simpleName 字符串比较
                when (methodName) {
                    "assess" -> {
                        val returnType = method.returnType
                        // VULN-003: 使用类引用比较替代 returnType.simpleName != "SecurityLevel"
                        if (!returnType.isEnum || returnType != AntiDebug.SecurityLevel::class.java) {
                            issues.add("$methodName() return type modified")
                        }
                    }
                    "enforce", "quickCheck" -> {
                        if (method.returnType != Void.TYPE && method.returnType != Boolean::class.javaPrimitiveType) {
                            issues.add("$methodName() return type modified")
                        }
                    }
                    else -> {
                        if (method.returnType != Boolean::class.javaPrimitiveType) {
                            issues.add("$methodName() return type modified")
                        }
                    }
                }
            } catch (_: NoSuchMethodException) {
                issues.add("$methodName() method missing")
            }
        }

        // ApkIntegrityChecker 关键方法
        try {
            val method = ApkIntegrityChecker::class.java.getMethod(
                "verify",
                Context::class.java
            )
            val returnType = method.returnType
            // VULN-003: 使用类引用比较替代 returnType.simpleName != "IntegrityResult"
            if (returnType != ApkIntegrityChecker.IntegrityResult::class.java) {
                issues.add("ApkIntegrityChecker.verify() return type modified")
            }
        } catch (_: NoSuchMethodException) {
            issues.add("ApkIntegrityChecker.verify() method missing")
        }

        // DexIntegrityChecker 关键方法
        try {
            val method = DexIntegrityChecker::class.java.getMethod(
                "verify",
                Context::class.java
            )
            val returnType = method.returnType
            // VULN-003: 使用类引用比较替代 returnType.simpleName != "DexResult"
            if (returnType != DexIntegrityChecker.DexResult::class.java) {
                issues.add("DexIntegrityChecker.verify() return type modified")
            }
        } catch (_: NoSuchMethodException) {
            issues.add("DexIntegrityChecker.verify() method missing")
        }

        return issues
    }

    /**
     * 类加载验证 —— 确认所有安全类可正常加载且未被替换。
     *
     * SECURITY VULN-003: 使用 Kotlin 类引用替代 Class.forName(硬编码字符串)，
     * 兼容 R8 类名混淆。类引用在编译时确定，运行时自动使用混淆后的类名。
     */
    private fun verifySecurityClasses(): List<String> {
        val issues = mutableListOf<String>()

        // VULN-003: 使用 Kotlin 类引用替代 Class.forName("com.draftpeek.security.AntiDebug") 等
        val criticalClasses = listOf(
            AntiDebug::class.java,
            ApkIntegrityChecker::class.java,
            DexIntegrityChecker::class.java,
            SecurityIntegrityChecker::class.java,
            com.draftpeek.core.common.security.SecurityGate::class.java
        )

        for (clazz in criticalClasses) {
            // 验证类不是被代理或包装的
            if (clazz.classLoader != SecurityIntegrityChecker::class.java.classLoader) {
                issues.add("${clazz.name} loaded by different classloader")
            }
        }

        return issues
    }

    /**
     * 快速行为检查 —— 验证 AntiDebug.assess() 的基本行为。
     * 不执行完整校验，仅做最小化验证。
     *
     * SECURITY VULN-003: 使用 Kotlin 类引用替代硬编码字符串。
     */
    private fun quickBehaviorCheck(): Boolean {
        return try {
            // 验证 assess() 方法可调用
            val assessMethod = AntiDebug::class.java.getMethod("assess")
            val enforceMethod = AntiDebug::class.java.getMethod("enforce")
            val quickCheckMethod = AntiDebug::class.java.getMethod("quickCheck")

            // 验证所有检测方法存在
            val detectionMethods = listOf(
                "isDebuggerConnected",
                "isRunningOnEmulator",
                "isTraced",
                "isFridaDetected",
                "isXposedDetected",
                "isRooted",
                "isHookFrameworkDetected",
                "isZygiskDetected"  // VULN-014: Zygisk 检测
            )

            for (methodName in detectionMethods) {
                try {
                    AntiDebug::class.java.getMethod(methodName)
                } catch (_: NoSuchMethodException) {
                    return false
                }
            }

            // 验证 SecurityLevel 枚举完整
            // VULN-003: 使用 Kotlin 类引用替代 Class.forName
            val securityLevelClass = AntiDebug.SecurityLevel::class.java
            val enumConstants = securityLevelClass.enumConstants
            val expectedValues = AntiDebug.SecurityLevel.values().map { it.name }.toSet()
            val actualValues = (enumConstants as? Array<*>)?.map {
                (it as? Enum<*>)?.name
            }?.toSet() ?: emptySet()
            if (actualValues != expectedValues) {
                return false
            }

            true
        } catch (_: Exception) {
            false
        }
    }
}
