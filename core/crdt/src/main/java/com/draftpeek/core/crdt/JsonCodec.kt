package com.draftpeek.core.crdt

/**
 * 供上层使用的**扁平字符串映射** JSON 编解码。
 *
 * ## 为什么需要这层封装
 *
 * [MiniJson] 与 [JsonValue] 是 `core:crdt` 的 `internal` 实现细节（不希望被跨模块依赖）。
 * 但上层（如 `core:data` 的同步载荷编解码）需要把「一组字段」写成 JSON 字符串放进 CRDT 文档，
 * 于是这里暴露一个**最小且稳定**的入口：只处理「扁平对象、值为字符串或 null」。
 *
 * 嵌套结构不在支持范围内 —— 需要嵌套时应先在 `core:crdt` 里扩展，
 * 而不是让调用方自己拼 JSON（手拼 JSON 是转义 bug 的温床）。
 */
object JsonCodec {

    /**
     * 把扁平映射编码成 JSON 对象文本。
     *
     * 值为 `null` 时写 JSON `null`（用于表示「该字段无值」，区别于空字符串）。
     */
    fun encodeStrings(fields: Map<String, String?>): String = MiniJson.write(
        JsonValue.Obj(
            fields.entries.associate { (key, value) ->
                key to (value?.let { JsonValue.Str(it) } ?: JsonValue.Null)
            }
        )
    )

    /**
     * 解析 JSON 对象文本为扁平映射。
     *
     * **严格失败不猜**（与 [CrdtJson] 同策略）：不是对象、或含嵌套值（数组/对象）时抛异常。
     * 上层应把它当作一次同步失败处理，绝不能「解析出空映射」—— 那会让合并结果丢掉整侧数据。
     *
     * 数字与布尔按字面量转成字符串（本层的契约是「扁平字符串映射」，调用方自行解析回类型）。
     */
    fun decodeStrings(text: String): Map<String, String?> {
        val obj = MiniJson.parse(text) as? JsonValue.Obj
            ?: throw JsonParseException("期望 JSON 对象", 0)
        return obj.fields.mapValues { (key, value) ->
            when (value) {
                JsonValue.Null -> null
                is JsonValue.Str -> value.value
                is JsonValue.Num -> value.value.toString()
                is JsonValue.Bool -> value.value.toString()
                else -> throw JsonParseException("字段 '$key' 是嵌套值，扁平映射不支持", 0)
            }
        }
    }
}
