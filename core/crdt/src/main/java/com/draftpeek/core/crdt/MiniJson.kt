package com.draftpeek.core.crdt

/**
 * 极简 JSON 值模型。
 *
 * 只保留同步文档用得到的类型；数字统一按 [Long] 处理（时间戳与计数器都是整数）。
 */
internal sealed interface JsonValue {

    data class Obj(val fields: Map<String, JsonValue>) : JsonValue

    data class Arr(val items: List<JsonValue>) : JsonValue

    data class Str(val value: String) : JsonValue

    data class Num(val value: Long) : JsonValue

    data class Bool(val value: Boolean) : JsonValue

    data object Null : JsonValue
}

/** 解析失败时抛出，携带出错位置，便于定位（数据存在用户仓库里，出问题时要能看懂）。 */
internal class JsonParseException(message: String, val position: Int) :
    IllegalArgumentException("$message (位置 $position)")

/**
 * 极简 JSON 读写（本模块自用，不对外暴露）。
 *
 * **为什么不引入 kotlinx.serialization**：本仓库依赖锁定是 `LockMode.STRICT`，
 * 而版本目录是**多会话共享文件**；只为「一个形状固定的文档」引入新依赖族
 * （版本目录 + 编译器插件 + 全模块依赖锁重新生成）代价过高、且会与其他会话冲突。
 * 因此这里实现一个**只覆盖 JSON 子集**的严格解析器，并配 round-trip 与畸形输入的用例。
 *
 * 支持：object / array / string（含 `\uXXXX` 与代理对）/ 整数 / true / false / null。
 * 不支持（按错误处理）：注释、尾随逗号、浮点、NaN / Infinity。
 */
internal object MiniJson {

    fun parse(text: String): JsonValue {
        val cursor = Cursor(text)
        cursor.skipWhitespace()
        val value = cursor.readValue()
        cursor.skipWhitespace()
        if (!cursor.atEnd()) cursor.fail("末尾有多余内容")
        return value
    }

    fun write(value: JsonValue): String = buildString { appendValue(value, this) }

    // ── 写入 ────────────────────────────────────────────────────────────

    private fun appendValue(value: JsonValue, out: StringBuilder) {
        when (value) {
            is JsonValue.Obj -> {
                out.append('{')
                value.fields.entries.forEachIndexed { index, (key, child) ->
                    if (index > 0) out.append(',')
                    appendString(key, out)
                    out.append(':')
                    appendValue(child, out)
                }
                out.append('}')
            }

            is JsonValue.Arr -> {
                out.append('[')
                value.items.forEachIndexed { index, child ->
                    if (index > 0) out.append(',')
                    appendValue(child, out)
                }
                out.append(']')
            }

            is JsonValue.Str -> appendString(value.value, out)
            is JsonValue.Num -> out.append(value.value)
            is JsonValue.Bool -> out.append(if (value.value) "true" else "false")
            JsonValue.Null -> out.append("null")
        }
    }

    private fun appendString(raw: String, out: StringBuilder) {
        out.append('"')
        for (ch in raw) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (ch < ' ') out.append("\\u%04x".format(ch.code)) else out.append(ch)
            }
        }
        out.append('"')
    }

    // ── 解析 ────────────────────────────────────────────────────────────

    private class Cursor(private val text: String) {
        private var index = 0

        fun atEnd(): Boolean = index >= text.length

        fun fail(message: String): Nothing = throw JsonParseException(message, index)

        fun skipWhitespace() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun readValue(): JsonValue {
            if (atEnd()) fail("内容意外结束")
            return when (val ch = text[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> JsonValue.Str(readString())
                't' -> readLiteral("true", JsonValue.Bool(true))
                'f' -> readLiteral("false", JsonValue.Bool(false))
                'n' -> readLiteral("null", JsonValue.Null)
                else -> if (ch == '-' || ch.isDigit()) readNumber() else fail("意外的字符 '$ch'")
            }
        }

        private fun readObject(): JsonValue.Obj {
            index++ // '{'
            val fields = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[index] == '}') {
                index++
                return JsonValue.Obj(fields)
            }
            while (true) {
                skipWhitespace()
                if (atEnd() || text[index] != '"') fail("对象的键必须是字符串")
                val key = readString()
                skipWhitespace()
                if (atEnd() || text[index] != ':') fail("键之后缺少 ':'")
                index++
                skipWhitespace()
                fields[key] = readValue()
                skipWhitespace()
                if (atEnd()) fail("对象未闭合")
                when (text[index]) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return JsonValue.Obj(fields)
                    }

                    else -> fail("对象里出现意外字符 '${text[index]}'")
                }
            }
        }

        private fun readArray(): JsonValue.Arr {
            index++ // '['
            val items = mutableListOf<JsonValue>()
            skipWhitespace()
            if (!atEnd() && text[index] == ']') {
                index++
                return JsonValue.Arr(items)
            }
            while (true) {
                skipWhitespace()
                items += readValue()
                skipWhitespace()
                if (atEnd()) fail("数组未闭合")
                when (text[index]) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return JsonValue.Arr(items)
                    }

                    else -> fail("数组里出现意外字符 '${text[index]}'")
                }
            }
        }

        private fun readString(): String {
            index++ // 开引号
            val builder = StringBuilder()
            while (true) {
                if (atEnd()) fail("字符串未闭合")
                when (val ch = text[index]) {
                    '"' -> {
                        index++
                        return builder.toString()
                    }

                    '\\' -> {
                        index++
                        if (atEnd()) fail("转义序列不完整")
                        when (val escape = text[index]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 4 >= text.length) fail("\\u 需要 4 位十六进制")
                                val hex = text.substring(index + 1, index + 5)
                                val code = hex.toIntOrNull(16) ?: fail("非法 \\u 转义：$hex")
                                builder.append(code.toChar())
                                index += 4
                            }

                            else -> fail("未知转义 '\\$escape'")
                        }
                        index++
                    }

                    else -> {
                        if (ch < ' ') fail("字符串里出现未转义的控制字符")
                        builder.append(ch)
                        index++
                    }
                }
            }
        }

        private fun readNumber(): JsonValue.Num {
            val start = index
            if (!atEnd() && text[index] == '-') index++
            while (!atEnd() && text[index].isDigit()) index++
            if (!atEnd() && (text[index] == '.' || text[index] == 'e' || text[index] == 'E')) {
                fail("本模块只接受整数（时间戳/计数器）")
            }
            val raw = text.substring(start, index)
            val parsed = raw.toLongOrNull() ?: fail("非法整数 '$raw'")
            return JsonValue.Num(parsed)
        }

        private fun readLiteral(literal: String, value: JsonValue): JsonValue {
            if (!text.startsWith(literal, index)) fail("期望 '$literal'")
            index += literal.length
            return value
        }
    }
}
