/**
 * 编程语言配置模块。
 *
 * 提供统一的编程语言配置映射，将显示名称映射到文件扩展名和MIME类型，同时包含文件名验证逻辑。
 * 被文件创建对话框和文件导出/查看模型共用。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

/**
 * 文件名验证错误类型枚举。
 *
 * UI层使用此枚举显示本地化的错误消息。
 */
enum class FilenameValidationError {
    /** 文件名为空 */
    EMPTY,

    /** 文件名过长 */
    TOO_LONG,

    /** 包含非法字符 */
    ILLEGAL_CHARS,

    /** 以数字开头（某些语言不允许） */
    DIGIT_START
}

/**
 * 编程语言配置对象。
 *
 * 统一管理语言显示名称到扩展名和MIME类型的映射，提供文件名验证功能。
 * 支持C/C++/C#/Java/Kotlin/Python/JavaScript/TypeScript/HTML/CSS/Markdown/
 * JSON/XML/YAML/Go/Rust/PHP/Ruby/Swift/Shell/SQL/Dart/纯文本/加密导出等语言类型。
 *
 * 同时提供文件扩展名到编辑器语言ID的映射，供语法高亮使用。
 */
object LanguageConfig {

    /**
     * 语言信息数据类。
     *
     * @property displayName 语言显示名称
     * @property extension 文件扩展名（不含点）
     * @property mimeType MIME类型
     */
    data class LanguageInfo(val displayName: String, val extension: String, val mimeType: String)

    private val languages = listOf(
        // ── 最常用：Web 前端 + 通用编程语言 ──
        LanguageInfo("JavaScript", "js", "text/javascript"),
        LanguageInfo("Python", "py", "text/x-python"),
        LanguageInfo("HTML", "html", "text/html"),
        LanguageInfo("CSS", "css", "text/css"),
        LanguageInfo("TypeScript", "ts", "text/typescript"),
        LanguageInfo("Java", "java", "text/x-java-source"),
        LanguageInfo("SQL", "sql", "text/x-sql"),
        LanguageInfo("C#", "cs", "text/x-csharp"),
        LanguageInfo("Markdown", "md", "text/markdown"),
        LanguageInfo("C++", "cpp", "text/x-c++src"),
        LanguageInfo("Go", "go", "text/x-go"),
        LanguageInfo("PHP", "php", "text/x-php"),
        LanguageInfo("C", "c", "text/x-csrc"),
        LanguageInfo("Shell", "sh", "text/x-shellscript"),
        // ── 常用：移动 / 客户端 / 配置 ──
        LanguageInfo("JSON", "json", "application/json"),
        LanguageInfo("Kotlin", "kt", "text/x-kotlin"),
        LanguageInfo("Swift", "swift", "text/x-swift"),
        LanguageInfo("YAML", "yml", "text/x-yaml"),
        LanguageInfo("Rust", "rs", "text/x-rust"),
        LanguageInfo("Ruby", "rb", "text/x-ruby"),
        LanguageInfo("Dart", "dart", "text/x-dart"),
        LanguageInfo("R", "r", "text/x-r"),
        LanguageInfo("XML", "xml", "application/xml"),
        LanguageInfo("TOML", "toml", "text/x-toml"),
        // ── 其余编程语言 + 配置 / 特殊格式 ──
        LanguageInfo("INI", "ini", "text/x-ini"),
        LanguageInfo("Dockerfile", "dockerfile", "text/x-dockerfile"),
        LanguageInfo("LaTeX", "tex", "text/x-tex"),
        LanguageInfo("Scala", "scala", "text/x-scala"),
        LanguageInfo("Lua", "lua", "text/x-lua"),
        LanguageInfo("Groovy", "groovy", "text/x-groovy"),
        LanguageInfo("Perl", "pl", "text/x-perl"),
        LanguageInfo("Haskell", "hs", "text/x-haskell"),
        LanguageInfo("Elixir", "ex", "text/x-elixir"),
        LanguageInfo("Julia", "jl", "text/x-julia"),
        LanguageInfo("Clojure", "clj", "text/x-clojure"),
        LanguageInfo("Erlang", "erl", "text/x-erlang"),
        LanguageInfo("OCaml", "ml", "text/x-ocaml"),
        LanguageInfo("Lisp", "lisp", "text/x-lisp"),
        LanguageInfo("Vim Script", "vim", "text/x-vim"),
        // ── 特殊 ──
        LanguageInfo("Plain Text", "txt", "text/plain"),
        LanguageInfo("Encrypted Export", "jenc", "application/octet-stream")
    )

    /**
     * 文件扩展名到编辑器语言ID的映射（用于语法高亮）。
     *
     * 使用 lazy 初始化避免类加载时的开销，一次性构建不可变 Map 提供 O(1) 查找。
     * 合并了所有别名扩展名（如 .htm/.html, .kts/.kt, .bash/.sh 等）。
     */
    private val extensionToLanguageMap: Map<String, String> by lazy {
        buildMap {
            // Kotlin
            put("kt", "kotlin")
            put("kts", "kotlin")
            // Java
            put("java", "java")
            put("class", "java")
            put("jar", "java")
            // Python
            put("py", "python")
            put("pyc", "python")
            put("pyw", "python")
            // JavaScript
            put("js", "javascript")
            put("mjs", "javascript")
            put("cjs", "javascript")
            put("jsx", "javascript")
            // TypeScript
            put("ts", "typescript")
            put("mts", "typescript")
            put("cts", "typescript")
            put("tsx", "typescript")
            // HTML
            put("html", "html")
            put("htm", "html")
            put("xhtml", "html")
            // CSS
            put("css", "css")
            put("scss", "css")
            put("sass", "css")
            put("less", "css")
            // JSON
            put("json", "json")
            put("json5", "json")
            put("jsonc", "json")
            // XML/SVG
            put("xml", "xml")
            put("svg", "xml")
            // YAML
            put("yaml", "yaml")
            put("yml", "yaml")
            // Markdown
            put("md", "markdown")
            put("markdown", "markdown")
            // Plain text
            put("txt", "text")
            put("text", "text")
            put("log", "text")
            // C/C++/C#
            put("c", "cpp")
            put("h", "cpp")
            put("cpp", "cpp")
            put("hpp", "cpp")
            put("cc", "cpp")
            put("cxx", "cpp")
            put("m", "cpp")
            put("cs", "csharp")
            // Go
            put("go", "go")
            // Rust
            put("rs", "rust")
            // Ruby
            put("rb", "ruby")
            // PHP
            put("php", "php")
            // Swift
            put("swift", "swift")
            // Dart
            put("dart", "dart")
            // Scala
            put("scala", "scala")
            // SQL
            put("sql", "sql")
            // Shell
            put("sh", "shell")
            put("bash", "shell")
            put("zsh", "shell")
            put("fish", "shell")
            put("ps1", "shell")
            put("bat", "shell")
            put("cmd", "shell")
            // Lua
            put("lua", "lua")
            // Groovy
            put("groovy", "groovy")
            put("gradle", "groovy")
            // Docker
            put("dockerfile", "dockerfile")
            // Config
            put("properties", "properties")
            put("toml", "toml")
            put("ini", "ini")
            put("cfg", "ini")
            put("conf", "ini")
            // Data
            put("csv", "csv")
            put("tsv", "tsv")
            // Other languages
            put("r", "r")
            put("pl", "perl")
            put("pm", "perl")
            put("ex", "elixir")
            put("exs", "elixir")
            put("erl", "erlang")
            put("hs", "haskell")
            put("ml", "ocaml")
            put("clj", "clojure")
            put("cljs", "clojure")
            put("lisp", "lisp")
            put("el", "lisp")
            put("vim", "vim")
            put("tex", "latex")
            put("jl", "julia")
        }
    }

    /**
     * 语言ID到 TextMate scope name 的映射（用于语法高亮加载）。
     *
     * 使用 lazy 初始化，O(1) 哈希表查找。
     * 这是 SoraEditorWrapper.setLanguageForContent() 使用的统一映射，
     * 所有模块应通过 languageToScopeName() 访问，避免重复定义。
     */
    private val languageToScopeMap: Map<String, String> by lazy {
        buildMap {
            put("java", "source.java")
            put("kotlin", "source.kotlin")
            put("cpp", "source.cpp")
            put("csharp", "source.cs")
            put("python", "source.python")
            put("javascript", "source.js")
            put("typescript", "source.ts")
            put("html", "text.html.basic")
            put("css", "source.css")
            put("json", "source.json")
            put("xml", "text.xml")
            put("yaml", "source.yaml")
            put("markdown", "text.html.markdown")
            put("go", "source.go")
            put("rust", "source.rust")
            put("php", "source.php")
            put("ruby", "source.ruby")
            put("swift", "source.swift")
            put("shell", "source.shell")
            put("lua", "source.lua")
            put("dart", "source.dart")
            put("scala", "source.scala")
            put("sql", "source.sql")
            put("groovy", "source.groovy")
            put("toml", "source.toml")
            put("r", "source.r")
            put("julia", "source.julia")
            put("perl", "source.perl")
            put("clojure", "source.clojure")
            put("lisp", "source.lisp")
            put("latex", "text.tex")
            put("dockerfile", "source.dockerfile")
            put("ini", "source.ini")
            put("haskell", "source.haskell")
            put("elixir", "source.elixir")
            put("erlang", "source.erlang")
            put("ocaml", "source.ocaml")
            put("vim", "source.viml")
        }
    }

    /**
     * 获取所有支持的语言列表。
     *
     * @return LanguageInfo列表
     */
    fun getAllLanguages(): List<LanguageInfo> = languages

    /**
     * 获取所有语言的显示名称列表。
     *
     * @return 显示名称字符串列表
     */
    fun getDisplayNames(): List<String> = languages.map { it.displayName }

    /**
     * 根据语言显示名称获取对应的文件扩展名。
     *
     * @param displayName 语言显示名称
     * @return 文件扩展名（不含点），未找到时返回"txt"
     */
    fun languageToExtension(displayName: String): String =
        languages.find { it.displayName.equals(displayName, ignoreCase = true) }?.extension ?: "txt"

    /**
     * 根据文件扩展名获取对应的编辑器语言ID（用于语法高亮）。
     *
     * O(1) 哈希表查找，适用于大文件快速语言检测。
     *
     * @param extension 文件扩展名（不含点，大小写不敏感）
     * @return 语言ID字符串（如 "kotlin"、"java"），未识别时返回 null
     */
    fun extensionToLanguage(extension: String): String? {
        if (extension.isBlank()) return null
        return extensionToLanguageMap[extension.lowercase()]
    }

    /**
     * 根据语言ID获取对应的 TextMate scope name（用于语法高亮加载）。
     *
     * O(1) 哈希表查找。
     *
     * @param languageId 语言ID（如 "kotlin"、"java"），大小写不敏感
     * @return TextMate scope name（如 "source.kotlin"），未识别时返回 null
     */
    fun languageToScopeName(languageId: String?): String? {
        if (languageId.isNullOrBlank()) return null
        return languageToScopeMap[languageId.lowercase()]
    }

    /**
     * 根据语言显示名称获取对应的MIME类型。
     *
     * @param displayName 语言显示名称
     * @return MIME类型字符串，未找到时返回"text/plain"
     */
    fun languageToMimeType(displayName: String): String =
        languages.find { it.displayName.equals(displayName, ignoreCase = true) }?.mimeType ?: "text/plain"

    /**
     * 标识符不能以数字开头的语言集合（C系语言等）。
     */
    private val noDigitStartLanguages = setOf("C", "C++", "C#", "Go", "Rust", "Java", "Kotlin", "PHP", "Swift", "Dart")

    /**
     * 验证给定语言的文件名是否合法。
     *
     * 验证规则：
     * 1. 文件名不能为空
     * 2. 文件名长度不能超过200字符
     * 3. 不能包含非法字符（/ \ : * ? " < > |）
     * 4. 不能包含路径遍历序列（..）
     * 5. 对于C系等语言，文件名不能以数字开头
     *
     * @param filename 待验证的文件名（不含扩展名）
     * @param language 语言显示名称
     * @return 验证通过返回null，否则返回对应的[FilenameValidationError]
     */
    fun validateFilename(filename: String, language: String): FilenameValidationError? {
        if (filename.isBlank()) return FilenameValidationError.EMPTY

        if (filename.length > 200) {
            return FilenameValidationError.TOO_LONG
        }

        val illegalChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        if (filename.any { it in illegalChars }) {
            return FilenameValidationError.ILLEGAL_CHARS
        }

        if (filename.contains("..")) {
            return FilenameValidationError.ILLEGAL_CHARS
        }

        if (language in noDigitStartLanguages && filename.first().isDigit()) {
            return FilenameValidationError.DIGIT_START
        }

        return null
    }
}
