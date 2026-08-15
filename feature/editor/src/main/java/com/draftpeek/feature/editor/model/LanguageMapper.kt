/**
 * 文件：LanguageMapper.kt
 * 功能：文件扩展名到编程语言标识符映射工具
 * 主要类/接口：LanguageMapper
 * 模块依赖：core/common (LanguageConfig)
 */
package com.draftpeek.feature.editor.model

import com.draftpeek.core.common.util.LanguageConfig

/**
 * 文件扩展名到 TextMate 语言标识符的映射工具对象。
 *
 * 委托给 [LanguageConfig] 提供统一的语言映射，避免重复定义。
 * 无法识别的扩展名返回 `null`，表示纯文本模式。
 *
 * 使用场景：
 * - 打开文件时根据扩展名自动选择语法高亮语言
 * - TabManager 创建新标签页时设置语言类型
 */
object LanguageMapper {

    /**
     * 返回编辑器用于语法高亮的语言标识符。
     *
     * 委托给 [LanguageConfig.extensionToLanguage] 提供统一映射。
     *
     * 示例：
     * - `"kt"` -> `"kotlin"`
     * - `"py"` -> `"python"`
     * - `"txt"` -> `null`
     *
     * @param extension 文件扩展名（不含点号，如 "kt"、"py"）
     * @return 语言标识符字符串，无法识别时返回 null
     */
    fun fromExtension(extension: String): String? {
        return LanguageConfig.extensionToLanguage(extension)
    }

    /**
     * 便捷重载方法，接受完整文件名（如 "Main.kt"）并返回语言标识符。
     *
     * 自动提取最后一个点号后的扩展名进行映射。
     *
     * @param fileName 完整文件名（含扩展名）
     * @return 语言标识符字符串，无法识别时返回 null
     */
    fun fromFileName(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "")
        return fromExtension(extension)
    }

    /**
     * 根据语言ID获取对应的 TextMate scope name。
     *
     * 委托给 [LanguageConfig.languageToScopeName] 提供统一映射。
     *
     * @param languageId 语言ID（如 "kotlin"、"java"）
     * @return TextMate scope name（如 "source.kotlin"），无法识别时返回 null
     */
    fun toScopeName(languageId: String?): String? {
        return LanguageConfig.languageToScopeName(languageId)
    }
}
