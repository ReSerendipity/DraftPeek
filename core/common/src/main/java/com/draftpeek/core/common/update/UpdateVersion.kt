package com.draftpeek.core.common.update

/**
 * 版本号比较（纯逻辑，无 Android 依赖，可直接 JVM 单测）。
 *
 * 口径：去掉前导 `v`/`V`，按 `.` `-` `_` `+` 切段，每段只取开头的数字；
 * 空段或非数字段记 0（于是 `1.0.33-dev` 与 `1.0.33` 判等，符合「预发布不当作新版本」的预期）。
 */
object UpdateVersion {

    /**
     * 比较两个版本号。
     *
     * @return 1 表示 [v1] 更新，-1 表示 [v2] 更新，0 表示相同
     */
    fun compare(v1: String, v2: String): Int {
        val a = segments(v1)
        val b = segments(v2)
        val length = maxOf(a.size, b.size)
        for (i in 0 until length) {
            val left = a.getOrElse(i) { 0 }
            val right = b.getOrElse(i) { 0 }
            if (left != right) return if (left > right) 1 else -1
        }
        return 0
    }

    private fun segments(version: String): List<Int> {
        val trimmed = version.trim().removePrefix("v").removePrefix("V")
        if (trimmed.isEmpty()) return emptyList()
        return trimmed.split('.', '-', '_', '+').map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }
}
