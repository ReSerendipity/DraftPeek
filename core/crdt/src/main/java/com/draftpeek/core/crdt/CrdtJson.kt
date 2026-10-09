package com.draftpeek.core.crdt

/**
 * 同步文档 ⇄ JSON 的编解码。
 *
 * 文档形状（存进用户自己的仓库，**刻意保持人可读**）：
 * ```json
 * {
 *   "v": 1,
 *   "node": "a1b2c3",
 *   "entries": {
 *     "pos:notes/a.md": { "s": [1731000000000, 0, "a1b2c3"], "v": "42" },
 *     "snippet:todo":   { "s": [1731000000001, 0, "a1b2c3"], "v": null }
 *   }
 * }
 * ```
 * `"v": null` 是墓碑（已删除）。
 *
 * **解析策略：严格失败，不猜**。远端文档损坏时抛异常，由上层当作一次同步失败处理，
 * 绝不能「解析出一份空文档」—— 那会把本地数据当成待推送内容覆盖掉远端。
 */
internal object CrdtJson {

    /** 当前格式版本；未来不兼容变更时递增，读到更高版本应拒绝而非勉强解析。 */
    const val FORMAT_VERSION: Long = 1L

    private const val KEY_VERSION = "v"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_STAMP = "s"
    private const val KEY_VALUE = "v"

    fun write(document: CrdtDocument): String {
        val entries: Map<String, JsonValue> = document.entries.entries.associate { (key, entry) ->
            val fields: Map<String, JsonValue> = linkedMapOf(
                KEY_STAMP to stampToJson(entry.stamp),
                KEY_VALUE to (entry.value?.let { JsonValue.Str(it) } ?: JsonValue.Null)
            )
            key to JsonValue.Obj(fields)
        }
        val root: Map<String, JsonValue> = linkedMapOf(
            KEY_VERSION to JsonValue.Num(FORMAT_VERSION),
            KEY_ENTRIES to JsonValue.Obj(entries)
        )
        return MiniJson.write(JsonValue.Obj(root))
    }

    fun read(text: String): CrdtDocument {
        val root = MiniJson.parse(text) as? JsonValue.Obj
            ?: throw JsonParseException("文档根节点必须是对象", 0)

        val version = (root.fields[KEY_VERSION] as? JsonValue.Num)?.value
            ?: throw JsonParseException("缺少格式版本字段 '$KEY_VERSION'", 0)
        if (version > FORMAT_VERSION) {
            throw JsonParseException("文档格式版本 $version 高于本端支持的 $FORMAT_VERSION", 0)
        }

        val entriesObject = root.fields[KEY_ENTRIES] as? JsonValue.Obj ?: return CrdtDocument()

        val entries = entriesObject.fields.mapValues { (key, raw) ->
            val entry = raw as? JsonValue.Obj
                ?: throw JsonParseException("条目 '$key' 必须是对象", 0)
            val stamp = entry.fields[KEY_STAMP]?.let(::stampFromJson)
                ?: throw JsonParseException("条目 '$key' 缺少戳字段 '$KEY_STAMP'", 0)
            val value = when (val raw = entry.fields[KEY_VALUE]) {
                null, JsonValue.Null -> null
                is JsonValue.Str -> raw.value
                else -> throw JsonParseException("条目 '$key' 的值必须是字符串或 null", 0)
            }
            CrdtEntry(value, stamp)
        }
        return CrdtDocument(entries = entries)
    }

    private fun stampToJson(stamp: CrdtStamp): JsonValue.Arr = JsonValue.Arr(
        listOf(
            JsonValue.Num(stamp.wallMillis),
            JsonValue.Num(stamp.counter.toLong()),
            JsonValue.Str(stamp.nodeId)
        )
    )

    private fun stampFromJson(raw: JsonValue): CrdtStamp {
        val items = (raw as? JsonValue.Arr)?.items
            ?: throw JsonParseException("戳必须是数组 [毫秒, 计数, 节点]", 0)
        if (items.size != 3) throw JsonParseException("戳数组必须恰好 3 项", 0)
        val wall = (items[0] as? JsonValue.Num)?.value
            ?: throw JsonParseException("戳的毫秒必须是整数", 0)
        val counter = (items[1] as? JsonValue.Num)?.value
            ?: throw JsonParseException("戳的计数必须是整数", 0)
        val node = (items[2] as? JsonValue.Str)?.value
            ?: throw JsonParseException("戳的节点必须是字符串", 0)
        return CrdtStamp(wallMillis = wall, counter = counter.toInt(), nodeId = node)
    }
}
