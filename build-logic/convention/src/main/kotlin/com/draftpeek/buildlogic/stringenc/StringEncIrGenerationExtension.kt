/**
 * B 层精选：字符串加密 Kotlin IR 插件（最小实现）。
 *
 * 仅处理 @EncryptedString 注解标记的 const val / val 字符串字段，
 * 编译期替换为 AES-256-CBC 密文 + IV，运行时通过 SecureStringResolver.decrypt() 解密。
 *
 * 当前默认关闭（gradle.properties: draftpeek.stringEnc.enabled=false）。
 * 启用时需在 app/build.gradle.kts 中注册此插件。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.buildlogic.stringenc

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * 字符串加密 IR 插件入口。
 *
 * 注册方式（在 build.gradle.kts 中）：
 * ```kotlin
 * kotlin {
 *     compilerOptions {
 *         freeCompilerArgs.addAll(listOf(
 *             "-P", "plugin:com.draftpeek.buildlogic.stringenc:StringEncEnabled=true"
 *         ))
 *     }
 * }
 * ```
 *
 * 当前为骨架实现，不执行实际加密。方案 C 启用时完善。
 */
class StringEncIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // 当前为 no-op，不执行任何转换
        // 方案 C 启用时：
        // 1. 遍历所有 @EncryptedString 标注的 val/const val
        // 2. 将字符串字面量替换为 AES-256-CBC 密文 + IV
        // 3. 在访问点插入 SecureStringResolver.decrypt(ciphertext, iv) 调用
        // moduleFragment.transform(StringEncTransformer(pluginContext), null)
    }
}

/**
 * 字符串加密 IR 转换器（骨架）。
 */
class StringEncTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoidWithContext() {

    // 方案 C 启用时实现：
    // override fun visitConst(expression: IrConst, data: Void?): IrExpression {
    //     if (expression.kind == IrConstKind.String) {
    //         val originalValue = expression.value as String
    //         // 加密 originalValue → 返回解密调用表达式
    //     }
    //     return super.visitConst(expression, data)
    // }
}
