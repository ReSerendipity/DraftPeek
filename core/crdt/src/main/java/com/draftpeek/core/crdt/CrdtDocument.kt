package com.draftpeek.core.crdt

/**
 * 一个键的 LWW 条目。
 *
 * [value] 为 `null` 表示**墓碑**（该键已被删除）。用墓碑而不是直接删键，
 * 是为了让「删除」也能参与戳比较 —— 否则远端会把已删的键当成「本地缺失」而复活它。
 */
data class CrdtEntry(val value: String?, val stamp: CrdtStamp)

/**
 * 同步文档：**扁平的「键 → LWW 条目」映射**。
 *
 * 为什么用扁平结构而不是嵌套的业务模型：
 * - 合并规则**统一**（逐键取戳大者），不需要为每种数据类型写一套合并逻辑；
 * - 文档形状稳定，未来加一类数据只是**换键前缀**，不用改格式与合并代码；
 * - 键用前缀约定区分数据类别（如 `pos:` 阅读进度、`snippet:` 片段、`stat:` 统计），
 *   由上层决定语义，本模块不关心。
 *
 * 语义边界（**已知取舍，需上层知情**）：
 * - 这是 **Last-Write-Wins**，并发写同一个键会**丢掉一方**（而非合并内容）。
 *   对阅读进度、片段这类「整条替换」的数据是合适的；
 * - **统计类累加值不适合直接 LWW**：两台设备各 +1 会丢掉一次。若统计要精确，
 *   应改用可加计数器（G-Counter），或由上层在写入前先做 max 归并。此点已在回函中说明。
 *
 * **为什么没有「文档级 nodeId」字段**：每个条目的戳里已经带了写入者，
 * 文档级再放一份不仅冗余，还会让 [merge] **不可结合**（取谁的 nodeId 取决于合并顺序）。
 * 单测 `可结合：三种合并顺序得到同一结果` 正是靠这一点抓出来的。
 */
data class CrdtDocument(val entries: Map<String, CrdtEntry> = emptyMap()) {

    /** 读取某键的值（已删除返回 null）。 */
    fun value(key: String): String? = entries[key]?.value

    /** 该键是否存在（未删除）。 */
    fun isPresent(key: String): Boolean = entries[key]?.value != null

    /** 本地写入，返回新文档（不修改原对象）。 */
    fun put(key: String, value: String, stamp: CrdtStamp): CrdtDocument =
        copy(entries = entries + (key to CrdtEntry(value, stamp)))

    /** 本地删除：写墓碑，保证删除在后续合并中也能胜出。 */
    fun remove(key: String, stamp: CrdtStamp): CrdtDocument = copy(entries = entries + (key to CrdtEntry(null, stamp)))

    /**
     * 合并另一份文档：**逐键取戳更大者**。
     *
     * 幂等、可交换、可结合 —— 因此「重复同步」「顺序不同的多设备同步」都收敛到同一结果。
     */
    fun merge(other: CrdtDocument): CrdtDocument {
        if (other.entries.isEmpty()) return this
        val merged = LinkedHashMap(entries)
        for ((key, incoming) in other.entries) {
            val current = merged[key]
            if (current == null || incoming.stamp > current.stamp) {
                merged[key] = incoming
            }
        }
        return CrdtDocument(entries = merged)
    }

    /** 只保留未删除的键值，供业务层读取。 */
    fun visibleValues(): Map<String, String> = entries
        .mapNotNull { (key, entry) -> entry.value?.let { key to it } }
        .toMap()

    /** 已删除的键（墓碑），供上层做「本地清理」时参考。 */
    fun tombstones(): Set<String> = entries.filterValues { it.value == null }.keys
}
