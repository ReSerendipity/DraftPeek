package com.draftpeek.core.common.model

import kotlin.jvm.JvmInline

/**
 * 类型安全ID封装，使用@JvmInline value class避免装箱开销。
 *
 * 参考CodeAssist的value class实践：防止不同类型的ID被错误地混用
 * （如将TabId传给需要SnippetId的方法），编译期即可发现类型错误。
 *
 * **注意**：Room数据库实体暂时保持原有的String/Long类型主键，
 * 避免触发schema版本升级和数据迁移。这些ID主要用于：
 * - 内存中的临时对象标识（标签页、LSP会话等）
 * - 跨模块传递的标识符
 * - 新业务代码中的ID类型
 */

/** 编辑器标签页ID */
@JvmInline
value class TabId(val value: String) {
    override fun toString(): String = value
    companion object {
        fun generate(): TabId = TabId("tab_${System.nanoTime()}")
    }
}

/** LSP会话ID */
@JvmInline
value class LspSessionId(val value: String) {
    override fun toString(): String = value
    companion object {
        fun generate(): LspSessionId = LspSessionId("lsp_${System.nanoTime()}")
    }
}

/** LSP请求ID */
@JvmInline
value class LspRequestId(val value: Int) {
    override fun toString(): String = value.toString()
}

/** 插件ID */
@JvmInline
value class PluginId(val value: String) {
    override fun toString(): String = value
}

/** 主题ID */
@JvmInline
value class ThemeId(val value: String) {
    override fun toString(): String = value
    companion object {
        val DEFAULT_LIGHT = ThemeId("default_light")
        val DEFAULT_DARK = ThemeId("default_dark")
    }
}

/** 字体ID */
@JvmInline
value class FontFamilyId(val value: String) {
    override fun toString(): String = value
    companion object {
        val DEFAULT_CODE = FontFamilyId("jetbrains_mono")
        val DEFAULT_UI = FontFamilyId("inter")
    }
}

/** 文件URI字符串封装 */
@JvmInline
value class FileUri(val value: String) {
    override fun toString(): String = value

    /** 获取文件名部分（最后一个/后的内容） */
    fun fileName(): String = value.substringAfterLast('/')
        .substringAfterLast('\\')
        .ifEmpty { value }

    /** 获取文件扩展名 */
    fun extension(): String = fileName().substringAfterLast('.', "")
}

/** 语言ID（用于语法高亮、LSP关联） */
@JvmInline
value class LanguageId(val value: String) {
    override fun toString(): String = value
    companion object {
        val KOTLIN = LanguageId("kotlin")
        val JAVA = LanguageId("java")
        val PYTHON = LanguageId("python")
        val JAVASCRIPT = LanguageId("javascript")
        val TYPESCRIPT = LanguageId("typescript")
        val MARKDOWN = LanguageId("markdown")
        val HTML = LanguageId("html")
        val CSS = LanguageId("css")
        val JSON = LanguageId("json")
        val XML = LanguageId("xml")
        val TEXT = LanguageId("text")
        val UNKNOWN = LanguageId("unknown")
    }
}

/** 诊断问题ID（LSP诊断） */
@JvmInline
value class DiagnosticId(val value: String) {
    override fun toString(): String = value
}
