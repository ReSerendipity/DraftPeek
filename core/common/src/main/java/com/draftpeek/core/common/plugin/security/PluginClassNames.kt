package com.draftpeek.core.common.plugin.security

/**
 * 插件类名/描述符的规范化与前缀匹配。
 *
 * 统一把 DEX 描述符（`Lcom/example/Foo;`、`[Lcom/example/Foo;`）与
 * 点分类名（`com.example.Foo`）转换成点分形式，供信任清单与运行时
 * ClassLoader 复用同一套判断。
 */
object PluginClassNames {

    private val PRIMITIVE_DESCRIPTORS = setOf("V", "Z", "B", "S", "C", "I", "J", "F", "D")

    /**
     * 把类名/描述符规范化为点分名；原生类型返回 null。
     */
    fun normalizeClassName(raw: String): String? {
        var value = raw.trim()
        if (value.isEmpty()) return null

        // 数组描述符：剥掉所有 '['，只校验基础类型。
        if (value.startsWith("[")) {
            var index = 0
            while (index < value.length && value[index] == '[') index++
            value = value.substring(index)
            if (value.length == 1 && value in PRIMITIVE_DESCRIPTORS) return null
        } else if (value.length == 1 && value in PRIMITIVE_DESCRIPTORS) {
            return null
        }

        if (value.startsWith("L") && value.endsWith(";")) {
            value = value.substring(1, value.length - 1).replace('/', '.')
        } else {
            if (value.startsWith("L") && value.contains('/')) return null
            value = value.replace('/', '.')
        }

        if (value.endsWith(";")) value = value.dropLast(1)
        if (value.startsWith(".") || value.endsWith(".") || value.contains("..")) return null
        if (value.isEmpty()) return null

        val segments = value.split('.')
        if (segments.any { it.isEmpty() }) return null
        if (!segments.all { isValidSegment(it) }) return null
        return value
    }

    /**
     * 规范化允许的类/包前缀；支持 `com.example.plugin`、`com/example/plugin`、
     * `Lcom/example/plugin;`，不接受 `*` 通配符。
     */
    fun normalizePrefix(raw: String): String? {
        var value = raw.trim().trimEnd('.')
        if (value.isEmpty() || value.contains('*') || value.startsWith("[")) return null

        if (value.startsWith("L") && value.endsWith(";")) {
            value = value.substring(1, value.length - 1)
        }
        value = value.replace('/', '.')
        if (value.isEmpty() || value.startsWith(".") || value.endsWith(".")) return null

        val segments = value.split('.')
        if (segments.any { it.isEmpty() }) return null
        if (!segments.all { isValidSegment(it) }) return null
        return value
    }

    /**
     * 规范化 restrictedClasses 条目；原生类型返回 null（不允许作为受限类）。
     */
    fun normalizeRestrictedClass(raw: String): String? = normalizeClassName(raw)

    /** DEX 类型描述符 → 点分类名；原生类型返回 null。 */
    fun descriptorToDotted(descriptor: String): String? = normalizeClassName(descriptor)

    /** 点分类名 → DEX 类描述符；非法名字返回 null。 */
    fun dottedToDescriptor(name: String): String? {
        val normalized = normalizeClassName(name) ?: return null
        return "L" + normalized.replace('.', '/') + ";"
    }

    /**
     * 类名是否在允许前缀内。
     *
     * 前缀按包/类边界匹配：`com.example.plugin` 允许
     * `com.example.plugin.Main`、`com.example.plugin.Main$Inner`，不允许
     * `com.example.plugin2.Main`。
     */
    fun isUnderPrefixes(className: String, prefixes: List<String>): Boolean {
        return prefixes.any { prefix ->
            className == prefix ||
                className.startsWith(prefix + ".") ||
                className.startsWith(prefix + "$")
        }
    }

    /**
     * 类名是否命中 restrictedClasses（精确类名；其内部类 `Outer$Inner` 也命中）。
     */
    fun isRestricted(className: String, restrictedClasses: Set<String>): Boolean {
        return restrictedClasses.any { restricted ->
            className == restricted || className.startsWith(restricted + "$")
        }
    }

    private fun isValidSegment(segment: String): Boolean {
        if (segment.isEmpty()) return false
        return segment.all { ch ->
            ch.isLetterOrDigit() || ch == '_' || ch == '$' || ch == '-'
        }
    }
}
