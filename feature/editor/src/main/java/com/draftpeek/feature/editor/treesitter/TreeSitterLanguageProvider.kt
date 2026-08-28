/**
 * 文件功能：Tree-sitter 语法语言提供者，为支持的文件类型提供基于 Tree-sitter 的 Language 实例
 *
 * 主要对象/类：
 * - [TreeSitterLanguageProvider]：Tree-sitter 语言提供者单例对象
 * - [JavaLanguageSpec]：Java 专用的 TsLanguageSpec 实现
 *
 * 模块依赖：
 * - com.itsaky.androidide.treesitter：Tree-sitter Android 绑定
 * - io.github.rosemoe.sora.editor.ts：sora-editor Tree-sitter 集成
 * - io.github.rosemoe.sora.lang：sora-editor 语言接口
 *
 * 当前状态：试点实现，仅支持 Java 语言。其他语言应回退到 TextMate 语法高亮。
 *
 * 架构设计：
 * - 每种支持的语言映射到一个 TsLanguageSpec（语法 + 查询文件）
 * - 主题规则将 Tree-sitter capture 名称映射到 sora-editor 配色方案槽位
 * - 如果 Tree-sitter 初始化因任何原因失败，调用方回退到 TextMate
 * - 使用缓存复用语言规范，避免重复加载原生库
 */
package com.draftpeek.feature.editor.treesitter

import android.content.Context
import android.util.Log
import com.itsaky.androidide.treesitter.java.TSLanguageJava
import io.github.rosemoe.sora.editor.ts.LocalsCaptureSpec
import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.editor.ts.TsLanguageSpec
import io.github.rosemoe.sora.editor.ts.TsThemeBuilder
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Tree-sitter 语言提供者（单例对象）
 *
 * 职责：
 * - 管理 Tree-sitter 原生库的加载状态检测
 * - 提供语言支持性检查
 * - 创建和缓存 TsLanguageSpec 实例
 * - 构建主题映射规则
 *
 * 失败处理：如果原生库加载失败，设置标志位避免后续重复尝试，防止日志泛滥。
 */
object TreeSitterLanguageProvider {

    private const val TAG = "TreeSitterProvider"

    /** 启用 Tree-sitter 支持的语言（试点阶段） */
    private val SUPPORTED_LANGUAGES = setOf("java")

    /** 缓存的语言规范——同一语言在多个编辑器实例间复用 */
    private val specCache = mutableMapOf<String, TsLanguageSpec>()

    /**
     * 全局开关：是否启用 Tree-sitter 增量语法高亮。
     * 由 FeatureFlag.TREE_SITTER 控制，在 Composable 中通过 LaunchedEffect 同步。
     */
    @Volatile
    var enabled: Boolean = true

    /**
     * 跟踪 Tree-sitter 原生库是否可用
     * - null = 尚未检查（懒加载初始化）
     * - true = 原生库已加载并正常工作
     * - false = 原生库加载失败；跳过所有后续尝试以避免日志泛滥
     */
    @Volatile
    private var treeSitterAvailable: Boolean? = null

    /**
     * 检查给定语言是否支持 Tree-sitter 高亮
     *
     * @param language 语言标识符（如 "java"）
     * @return 如果支持返回 true，否则返回 false
     */
    fun isSupported(language: String): Boolean {
        if (!enabled) return false
        if (treeSitterAvailable == false) return false
        return language.lowercase() in SUPPORTED_LANGUAGES
    }

    /**
     * 为给定语言标识符创建 Tree-sitter Language 实例
     *
     * 创建流程：
     * 1. 检查 Tree-sitter 是否可用（快速失败路径）
     * 2. 检查语言是否在支持列表中
     * 3. 获取或创建缓存的 TsLanguageSpec
     * 4. 创建 TsLanguage 实例并应用主题
     * 5. 处理异常：如果首次加载失败，标记 treeSitterAvailable 为 false 并清空缓存
     *
     * @param context ApplicationContext 用于加载资产文件
     * @param language 语言标识符（如 "java"）
     * @return TsLanguage 实例，如果语言不支持或初始化失败返回 null
     */
    fun createLanguage(context: Context, language: String): Language? {
        if (!enabled) return null
        if (treeSitterAvailable == false) return null
        if (!isSupported(language)) return null

        return try {
            val spec = getOrCreateSpec(context, language)
            val tsLanguage = TsLanguage(spec, tab = true) { buildJavaTheme() }
            if (treeSitterAvailable == null) {
                treeSitterAvailable = true
                Log.i(TAG, "TreeSitter native libraries loaded successfully")
            }
            tsLanguage
        } catch (e: Throwable) {
            if (treeSitterAvailable == null) {
                treeSitterAvailable = false
                Log.w(
                    TAG,
                    "TreeSitter native libraries unavailable: ${e.message}. " +
                        "Falling back to TextMate for all languages."
                )
            }
            specCache.clear()
            null
        }
    }

    /**
     * 获取或创建给定语言的缓存 TsLanguageSpec
     *
     * 使用双重检查模式（虽然 Kotlin 中 synchronized 保护更简单）。
     *
     * @param context ApplicationContext
     * @param language 语言标识符
     * @return TsLanguageSpec 实例
     * @throws IllegalArgumentException 如果语言不支持
     */
    private fun getOrCreateSpec(context: Context, language: String): TsLanguageSpec {
        specCache[language]?.let { return it }

        val spec = when (language.lowercase()) {
            "java" -> createJavaSpec(context)
            else -> throw IllegalArgumentException("Unsupported TreeSitter language: $language")
        }

        specCache[language] = spec
        return spec
    }

    /**
     * 通过从 assets 加载 scm 查询文件创建 Java TsLanguageSpec
     *
     * 加载的查询文件：
     * - highlights.scm：语法高亮规则
     * - blocks.scm：代码块识别
     * - brackets.scm：括号匹配
     * - locals.scm：局部变量/作用域识别
     *
     * @param context ApplicationContext
     * @return Java 语言的 TsLanguageSpec
     */
    private fun createJavaSpec(context: Context): TsLanguageSpec {
        val highlights = loadAsset(context, "tree-sitter-queries/java/highlights.scm")
        val blocks = loadAsset(context, "tree-sitter-queries/java/blocks.scm")
        val brackets = loadAsset(context, "tree-sitter-queries/java/brackets.scm")
        val locals = loadAsset(context, "tree-sitter-queries/java/locals.scm")

        return JavaLanguageSpec(highlights, blocks, brackets, locals)
    }

    /**
     * 从 assets 读取文本文件内容
     *
     * @param context ApplicationContext
     * @param path 资产文件路径（相对于 assets/ 目录）
     * @return 文件内容字符串
     * @throws java.io.IOException 如果读取失败
     */
    private fun loadAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    /**
     * 释放所有缓存的语言规范
     *
     * 应用终止时调用以释放 Tree-sitter 语法持有的原生内存。
     * 遍历并关闭所有 spec，然后清空缓存。
     */
    fun release() {
        specCache.values.forEach { spec ->
            try {
                spec.close()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to close TsLanguageSpec", e)
            }
        }
        specCache.clear()
    }
}

/**
 * Java 专用 TsLanguageSpec
 *
 * 使用 TSLanguageJava 和自定义的局部变量捕获规范。
 *
 * @param highlightScmSource 高亮查询源代码
 * @param codeBlocksScmSource 代码块查询源代码
 * @param bracketsScmSource 括号查询源代码
 * @param localsScmSource 局部变量查询源代码
 */
private class JavaLanguageSpec(
    highlightScmSource: String,
    codeBlocksScmSource: String = "",
    bracketsScmSource: String = "",
    localsScmSource: String = ""
) : TsLanguageSpec(
    TSLanguageJava.getInstance(),
    highlightScmSource,
    codeBlocksScmSource,
    bracketsScmSource,
    localsScmSource,
    JAVA_LOCALS_CAPTURE_SPEC
)

/**
 * Java Tree-sitter 查询的局部变量捕获规范
 *
 * 将 locals.scm 中的 capture 名称映射到语义类别：
 * - "scope"：作用域
 * - "reference"：引用
 * - "definition.var"/"definition.field"：变量/字段定义
 * - "scope.members"：成员作用域
 */
private val JAVA_LOCALS_CAPTURE_SPEC = object : LocalsCaptureSpec() {
    override fun isScopeCapture(captureName: String) = captureName == "scope"
    override fun isReferenceCapture(captureName: String) = captureName == "reference"
    override fun isDefinitionCapture(captureName: String) =
        captureName == "definition.var" || captureName == "definition.field"
    override fun isMembersScopeCapture(captureName: String) = captureName == "scope.members"
}

/**
 * 构建 Tree-sitter 主题规则，将 capture 名称映射到 sora-editor 配色方案槽位
 *
 * 这些槽位与 TextMate 主题使用的槽位相同，确保在 Tree-sitter 和 TextMate 模式切换时视觉一致性。
 *
 * 映射规则：
 * - comment → 注释（斜体）
 * - keyword → 关键字（粗体）
 * - constant.builtin/string/number → 字面量
 * - variable.builtin/variable/constant → 变量标识符
 * - type.builtin/type/attribute → 类型名称
 * - function.method/function.builtin/variable.field → 函数名
 * - operator → 操作符
 */
private fun TsThemeBuilder.buildJavaTheme() {
    textStyle(EditorColorScheme.COMMENT, italic = true) applyTo "comment"
    textStyle(EditorColorScheme.KEYWORD, bold = true) applyTo "keyword"
    TextStyle.makeStyle(EditorColorScheme.LITERAL) applyTo arrayOf(
        "constant.builtin",
        "string",
        "number"
    )
    TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_VAR) applyTo arrayOf(
        "variable.builtin",
        "variable",
        "constant"
    )
    TextStyle.makeStyle(EditorColorScheme.IDENTIFIER_NAME) applyTo arrayOf(
        "type.builtin",
        "type",
        "attribute"
    )
    TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME) applyTo arrayOf(
        "function.method",
        "function.builtin",
        "variable.field"
    )
    TextStyle.makeStyle(EditorColorScheme.OPERATOR) applyTo "operator"
}
