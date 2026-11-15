package com.draftpeek.core.common.plugin.security

/** DEX 类级扫描结果。 */
sealed interface DexScanResult {

    /**
     * @property definedClasses class_defs 中定义的类描述符（如 `Lcom/example/Foo;`）
     * @property referencedTypes type_ids 中出现过的类型描述符集合
     */
    data class Success(val definedClasses: List<String>, val referencedTypes: Set<String>) : DexScanResult

    /** 结构非法、截断、越界或没有类定义。 */
    data class Failure(val reason: String) : DexScanResult
}
