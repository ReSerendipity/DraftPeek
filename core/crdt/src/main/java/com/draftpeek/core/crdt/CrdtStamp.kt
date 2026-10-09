package com.draftpeek.core.crdt

/**
 * 混合逻辑时钟戳（Hybrid Logical Clock）。
 *
 * 用途：给每个写入打一个**全局可比**的戳，合并时「大者胜」。
 *
 * **为什么不用纯物理时间**：设备时钟会漂移、会被用户改，只靠 wall clock 会让
 * 「后写的数据输给先写的」。这里用 `(物理毫秒, 计数器, 节点 id)` 三元组按字典序比较：
 * - 物理毫秒：尽量贴近真实时间，便于人读（数据存在用户自己的仓库里，可读性有意义）；
 * - 计数器：同一毫秒内的多次写入靠它区分；
 * - 节点 id：完全并发（同毫秒同计数）时**确定性地**打破平局 —— 保证所有设备合并出同一结果。
 */
data class CrdtStamp(val wallMillis: Long, val counter: Int, val nodeId: String) : Comparable<CrdtStamp> {

    override fun compareTo(other: CrdtStamp): Int {
        val byWall = wallMillis.compareTo(other.wallMillis)
        if (byWall != 0) return byWall
        val byCounter = counter.compareTo(other.counter)
        if (byCounter != 0) return byCounter
        return nodeId.compareTo(other.nodeId)
    }
}

/**
 * 本节点的时钟：产生严格递增的戳，并在看到远端戳后把自己推到远端之后。
 *
 * 线程安全（`@Synchronized`）：写入可能来自多个协程。
 *
 * @param nodeId 本设备标识（首次生成后持久化；同一设备重装后应重新生成）
 * @param wallClock 物理时间源，测试可注入
 */
class HybridLogicalClock(val nodeId: String, private val wallClock: () -> Long = System::currentTimeMillis) {

    private var last: CrdtStamp = CrdtStamp(0L, 0, nodeId)

    /** 本地下一次写入使用的戳。 */
    @Synchronized
    fun next(): CrdtStamp {
        val now = wallClock()
        last = if (now > last.wallMillis) {
            CrdtStamp(now, 0, nodeId)
        } else {
            // 时钟未前进（同毫秒）或发生回拨：靠计数器前进，保证戳严格递增
            CrdtStamp(last.wallMillis, last.counter + 1, nodeId)
        }
        return last
    }

    /**
     * 观察到远端戳后推进本地时钟。
     *
     * 不这么做的话，本机时钟落后时产生的戳会**永远小于**远端已存在的戳，
     * 导致本地新写入在合并中持续落败（表现为「改了但同步不上去」）。
     */
    @Synchronized
    fun observe(remote: CrdtStamp) {
        val now = wallClock()
        val maxWall = maxOf(now, last.wallMillis, remote.wallMillis)
        val counter = when {
            maxWall == last.wallMillis && maxWall == remote.wallMillis ->
                maxOf(last.counter, remote.counter) + 1

            maxWall == last.wallMillis -> last.counter + 1
            maxWall == remote.wallMillis -> remote.counter + 1
            else -> 0
        }
        last = CrdtStamp(maxWall, counter, nodeId)
    }

    /** 观察一份文档里的所有戳（合并远端后调用）。 */
    @Synchronized
    fun observeAll(document: CrdtDocument) {
        document.entries.values.forEach { observe(it.stamp) }
    }
}
